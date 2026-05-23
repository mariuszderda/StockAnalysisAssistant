# ADR-005 — Integracja z Gemini przez REST (Retrofit)

**Status:** Accepted
**Data:** 2026-05-23
**Faza:** 5 (AI chat + polish)

## Kontekst

Faza 5 dokłada ekran czatu z asystentem AI (Google Gemini), który odpowiada
po polsku na pytania o instrument. Asystent dostaje deterministyczny kontekst
z `AnalysisContextBuilder` (ostatnie 10 świec + bieżące wartości wskaźników
z Fazy 4) — nie zgaduje danych, nie udziela porad inwestycyjnych.

Pierwotnie CLAUDE.md wskazywało SDK `com.google.ai.client.generativeai`.
Weryfikacja Web Search z maja 2026 pokazała trzy realne opcje:

1. **`com.google.ai.client.generativeai`** — **deprecated**. Repozytorium
   przemianowane na `deprecated-generative-ai-android`. Google kieruje
   developerów Androida do Firebase AI Logic.
2. **Firebase AI Logic** (`com.google.firebase:firebase-ai` przez BoM 34.4.0).
   Aktualne stanowisko Google, ale wymaga projektu Firebase, pliku
   `google-services.json` i runtime'u Firebase.
3. **`com.google.genai:google-genai`** (1.55.0) — server-side Java SDK,
   bez wsparcia dla Androida w dokumentacji.

## Decyzja

**Wywołujemy REST endpoint Gemini bezpośrednio przez Retrofit + Moshi —
te same biblioteki, których używamy do Twelve Data.**

Architektonicznie:

- `domain/ai/GeminiClient` — interfejs (jedyny punkt zależności dla ChatViewModel).
- `domain/ai/AnalysisContextBuilder` — top-level pure function budujący
  deterministyczną instrukcję systemową w języku polskim.
- `data/ai/GeminiApi` — Retrofit fasada (`@POST v1beta/models/{model}:generateContent`).
- `data/ai/GeminiClientImpl` — implementacja, mapuje `IOException` /
  `HttpException` / pusta odpowiedź na sealed `GeminiError`.
- `di/AiModule` — osobna instancja Retrofit z `baseUrl = "https://generativelanguage.googleapis.com/"`,
  współdzielony `OkHttpClient` + `Moshi` z `NetworkModule`.

Parametry generacji (CLAUDE.md § Gemini boundaries):
- `model = "gemini-1.5-flash"` (tańszy/szybszy)
- `temperature = 0.6`
- `maxOutputTokens = 800`
- brak streamingu w MVP — pojedyncze `suspend fun ask()` zwraca `Result<String>`.

Klucz API trafia do query stringu (`?key=…`); `HttpLoggingInterceptor` ma
custom logger redagujący wartości `key=` i `apikey=` regexem przed
zapisem do Logcat (CLAUDE.md § API keys — klucz nigdy w logach).

## Konsekwencje

- **Brak deprecated zależności.** Komisja nie ma podstaw do pytania
  „dlaczego używasz nieaktualnego SDK".
- **Brak konfiguracji Firebase.** Żadnego `google-services.json`, żadnego
  pluginu, żadnej puli wymagań plikowych. Setup = wpisanie klucza w
  `local.properties`.
- **Reuse istniejącego stacku HTTP.** OkHttp + Retrofit + Moshi już są
  w projekcie (Twelve Data). Dodajemy ~150 LoC (DTO + interface + impl +
  module). Brak nowych dependencies w `libs.versions.toml`.
- **Łatwa wymiana w v2.** Jeśli w przyszłości dojdzie streaming albo
  Firebase, wymieniamy tylko `GeminiClientImpl`. `ChatViewModel` nadal
  woła `geminiClient.ask(...)` i nic o tym nie wie.
- **Klucz API nie wycieka do logu.** Custom logger w `NetworkModule`
  zamienia wartości `key=` / `apikey=` na `REDACTED` — sprawdzalne grepem
  w `app/src/main/kotlin/.../di/NetworkModule.kt`.
- **Cienki kontrakt domain ↔ data.** `GeminiClient` ma jedną metodę i
  zwraca `Result<String>`. Bez `Flow`, bez stream'a, bez sesji — MVP-ready.

## Decyzja: kontekst przez repository (NIE shared state z `ChartViewModel`)

`ChatViewModel` jest instancjonowany pod innym `ViewModelStoreOwner`
(`NavBackStackEntry` dla `chat/{symbol}`). Mieliśmy dwie opcje:

1. **Współdzielić stan z `ChartViewModel`** przez NavGraph-scoped VM albo
   SavedStateHandle.
2. **Załadować dane ponownie** przez `StockRepository.getCandles()` +
   `IndicatorFactory.compute()`.

**Wybór: (2).** Powody:

- Repository jest cache-first z TTL 24h (ADR-001) — drugi `getCandles`
  trafia w Room (<10 ms), bez requestu sieciowego.
- Obliczenia wskaźników: zmierzone 0.7 ms dla 365 świec × 5 wskaźników
  (Faza 4 perf test).
- Brak coupling: ChatScreen może być otwarty deep linkiem bez przejścia
  przez ChartScreen.
- Lifecycle: każdy ekran ma własny stan, bez ryzyka scope leak.

Koszt: drobna duplikacja logiki „load + compute" między `ChartViewModel`
i `ChatViewModel`. Akceptowalna.

## Bezpieczeństwo i UX

- **`LegalDisclaimerBanner`** zawsze widoczny w ChatScreen (`Surface` z ikoną
  ostrzeżenia, niedismissable). String: `R.string.chat_disclaimer_full`.
- **Suggested questions** widoczne tylko gdy lista wiadomości pusta —
  „Co mówi RSI?", „Jaki jest krótkoterminowy trend?", „Co oznacza ostatnia
  świeca?". Pomagają komisji szybko zobaczyć kontekst-aware odpowiedź.
- **`GeminiError`** sealed: `Network`, `MissingApiKey`, `Blocked` (safety
  filter), `Server(code)`, `Unknown`. Mapowane na `R.string.chat_error_*`
  przez `@Composable GeminiError.toUserMessage()` — wszystkie komunikaty
  w `strings.xml`, zero hardkodowanych polskich literałów w VM.

## Odrzucone alternatywy

**`com.google.ai.client.generativeai` (deprecated).** Działa, ale deprecated
stamp to czerwona flaga na obronie. Dodatkowo Google nie gwarantuje
sunset date; nie chcemy się obudzić tydzień przed obroną z 410 Gone
od endpointu.

**Firebase AI Logic.** Wymaga: założenia projektu Firebase, pobrania
`google-services.json`, dodania pluginu `com.google.gms.google-services` do
Gradle, BoM management. Dwa nowe „klucze" (Firebase config + Gemini key
w Firebase console) zamiast jednego. Wartość dodana zerowa dla MVP, koszt
konfiguracji jednorazowo niski ale wieczny w utrzymaniu.

**`com.google.genai:google-genai`.** Pure Java SDK, server-targeted.
Dokumentacja nie wymienia Androida; potencjalne problemy z
ProGuard/R8 i bibliotekami HTTP (Apache HttpComponents na Androidzie
to overhead).

**Streaming odpowiedzi.** Świadomie poza MVP (CLAUDE.md § Gemini boundaries).
Powód: `Flow<String>` na styku UI dodaje skomplikowania niespójnego z resztą
warstw, a percepcja użytkownika nie cierpi przy `maxOutputTokens = 800`
(~1-2 s).

## Pytania obronne (z odpowiedziami)

1. **Dlaczego nie Firebase AI Logic, skoro Google rekomenduje?**
   Scope creep — Firebase project + plugin Gradle + dwukrotne klucze.
   Wartość dodana zerowa dla MVP; w v2 wymieniamy `GeminiClientImpl`,
   reszta kodu się nie zmienia.

2. **Skąd Gemini wie, co policzyłeś w aplikacji?**
   `AnalysisContextBuilder` (`domain/ai/`) buduje system instruction
   z `AnalysisContext`: symbol, ostatnie 10 świec OHLCV, bieżące
   wartości wskaźników. Wstrzykiwany w `systemInstruction.parts[0].text`
   zapytania REST. Deterministyczny, czysty pure function, 8 testów
   jednostkowych.

3. **Dlaczego klucz w URL a nie w nagłówku?**
   Endpoint Gemini Developer API akceptuje key w query. Authorization
   Bearer byłby dla OAuth — niedostępne bez Firebase / Google Cloud.
   Klucz redagowany w logu Logcat custom loggerem w `NetworkModule`.

4. **Czemu disclaimer non-dismissable?**
   Wymóg projektu (CLAUDE.md § Gemini boundaries). Komisja będzie pytać
   o etykę AI — disclaimer to konkretny artefakt do pokazania w demo.

5. **Co jak Gemini odmówi (safety filter)?**
   `GeminiResponseDto.promptFeedback.blockReason != null` → mapowanie
   na `GeminiError.Blocked` → assistant message z `R.string.chat_error_blocked`.
   Użytkownik może spróbować ponownie z innym pytaniem.

6. **Co jak nie ma klucza?**
   `GeminiClientImpl` validuje `apiKey.isBlank()` PRZED requestem HTTP →
   `GeminiError.MissingApiKey` → komunikat z instrukcją wpisania klucza
   w `local.properties`. Testowane (`GeminiClientImplTest` — zero HTTP
   requestów do MockWebServer).
