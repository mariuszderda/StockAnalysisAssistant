# CLAUDE.md — Stock Analysis Assistant

> **Memory file dla Claude Code.** Czytane automatycznie przy starcie każdej sesji
> w tym repo. Wszystko poniżej to twarde ograniczenia — nie odstępuj bez wyraźnej
> zgody w prompcie.
>
> Aktualizuj ten plik przez `/memory` albo edycję ręczną. Nie usuwaj sekcji
> "Patterns to defend" — komisja będzie pytać o te wzorce na obronie.

---

## Project context

**Stock Analysis Assistant (SAA)** — aplikacja Android do analizy technicznej akcji.
Użytkownik wpisuje ticker (np. `AAPL`, `CDR.WA`), ogląda wykres świecowy/liniowy,
włącza wskaźniki (RSI, MACD, SMA, EMA), pyta asystenta AI (Gemini) o interpretację
po polsku.

To projekt **inżynierski**, broniony przed komisją ~czerwiec 2026. Komisja będzie
pytać o decyzje architektoniczne i wzorce projektowe. Każda decyzja musi być
uzasadniona — nie tylko "tak wyszło".

## Stack — pinned, nie dodawaj nic bez zgody

- **Język:** Kotlin (najnowsza stabilna 2.x; aktualnie 2.1.x — zweryfikuj WebSearch przy starcie Fazy 1)
- **AGP:** najnowsza **stabilna 8.x** (nie 9.x — zbyt nowe, brakuje stabilnego wsparcia w IDE). Zweryfikuj przed konfiguracją Gradle.
- **Gradle:** kompatybilny z wybraną wersją AGP — zweryfikuj na https://developer.android.com/build/releases/gradle-plugin (sekcja "Update Gradle")
- **UI:** Jetpack Compose + Material 3
- **Architektura:** MVVM + Clean Architecture (3 warstwy: data, domain, presentation)
- **DI:** Hilt (najnowsza stabilna 2.5x)
- **Sieć:** Retrofit + OkHttp + Moshi (Kotlin codegen via KSP)
- **Persistence:** Room
- **Async:** Coroutines + StateFlow
- **Nawigacja:** Navigation Compose
- **Wykresy:** Vico — sprawdź aktualną majorkę (2.x ma inne API niż 1.x; preferuj 2.x jeśli stabilne)
- **AI:** Gemini API (`com.google.ai.client.generativeai`)
- **Dane rynkowe:** Twelve Data API (`api.twelvedata.com`)
- **Testy:** JUnit 5 + Truth + MockK + Turbine + MockWebServer
- **minSdk 26, targetSdk 34, JDK 17, Gradle Kotlin DSL**

**KRYTYCZNE — weryfikacja wersji.** Modele LLM często halucynują numery wersji
pakietów Maven. Przed każdym wpisaniem nowej zależności do `libs.versions.toml`:

1. Otwórz https://central.sonatype.com/artifact/<group>/<artifact>
2. Skopiuj wersję z pola "Latest version"
3. Jeśli nie ma dostępu do sieci, użyj WebSearch z zapytaniem
   "<artifact> latest stable version maven central"
4. **Nigdy** nie wpisuj wersji "RC", "alpha", "beta", "SNAPSHOT" bez wyraźnego
   uzasadnienia w komentarzu

Nie używaj `kotlinCompilerExtensionVersion` w `composeOptions` — od Kotlin 2.0
pluginem `org.jetbrains.kotlin.plugin.compose` zarządza tym automatycznie.

## Package structure

Pakiet bazowy: `pl.gwsh.stockanalysis`

```
pl.gwsh.stockanalysis/
├── data/
│   ├── remote/          # Retrofit API, DTO
│   ├── local/           # Room: entities, DAO, database
│   ├── repository/      # implementacje repository
│   ├── mapper/          # DTO ↔ Entity ↔ Domain
│   └── ai/              # GeminiClient impl
├── domain/
│   ├── model/           # czyste klasy domenowe (Stock, Candle, Range, DataError)
│   ├── repository/      # interfejsy repository
│   ├── indicator/       # IndicatorStrategy, konkretne strategie, IndicatorFactory, util/
│   └── ai/              # GeminiClient (interfejs), AnalysisContextBuilder
├── presentation/
│   ├── theme/
│   ├── navigation/
│   ├── common/
│   └── screens/
│       ├── search/
│       ├── favorites/
│       ├── chart/
│       └── chat/
└── di/                  # NetworkModule, DatabaseModule, RepositoryModule, AiModule
```

**Inwarianty zależności:**

- `data` NIE importuje z `presentation`
- `domain` NIE importuje z `data` ani `presentation`
- `domain` NIE importuje z `androidx.*`, `retrofit2.*`, `androidx.room.*`,
  `com.squareup.moshi.*`. Sprawdzalne testem JVM bez `androidTest`.

## Patterns to defend

Wymaganie prowadzącego: co najmniej 2 świadomie zastosowane wzorce projektowe.
Aplikacja realizuje **trzy**:

### 1. Repository Pattern (warstwa data)
- Interfejs `StockRepository` w `domain.repository`
- Implementacja `StockRepositoryImpl` w `data.repository`
- Dwa źródła: Twelve Data API + Room cache (cache-first z TTL)
- ViewModel zna tylko interfejs

### 2. Strategy Pattern (wskaźniki)
- Interfejs `IndicatorStrategy` w `domain.indicator`
- Konkretne strategie: `RsiStrategy`, `MacdStrategy`, `SmaStrategy`, `EmaStrategy`
- Algorytmy autorskie (RSI: Wilder smoothing 1978, MACD: 12/26/9)

### 3. Factory Pattern (tworzenie strategii)
- `IndicatorFactory` (klasa `@Singleton` w Hilt)
- Wejście: sealed class `IndicatorSpec` (RSI/MACD/SMA/EMA z parametrami)
- Wyjście: konkretna `IndicatorStrategy`
- Exhaustive `when` na sealed class — kompilator wymusza aktualizację fabryki

**Pytania obronne** (przygotuj odpowiedzi):
1. Co daje Repository, skoro można wstrzyknąć API bezpośrednio do ViewModelu?
2. Co daje Strategy nad zwykłym `when (type)` w jednej funkcji?
3. Jak Strategy i Factory współpracują?
4. Dlaczego sealed `IndicatorSpec` a nie String?
5. Open/Closed Principle — gdzie tu jest?
6. Dlaczego Wilder smoothing a nie SMA w RSI?

Wzorce "darmowe" (wymień w pracy, ale broń głównie 3 powyżej):
- Observer — StateFlow w Compose
- Singleton — Hilt `@Singleton`
- Adapter/Mapper — DTO ↔ Domain

## Code conventions

- KDoc po polsku nad public API w `domain` (komisja czyta).
- Nazwy klas i zmiennych — angielskie (idiomatyczny Kotlin).
- Stringi UI — wyłącznie w `res/values/strings.xml`, po polsku.
- Funkcje matematyczne (mappers, indicator math) → top-level fun, NIE static
  helper w companion.
- Ciężkie obliczenia → `Dispatchers.Default`.
- I/O → `Dispatchers.IO` (Retrofit/Room same to robią).
- ViewModel: tylko `viewModelScope`, nigdy `runBlocking`/`GlobalScope`.
- `Result<T>` dla operacji sieciowych z repository.
- Sealed class lub data class dla UiState — trzymaj jedną konwencję w projekcie.
- Brak `!!` poza testami. `?.let`, `requireNotNull`, sealed states.

## IndicatorResult — kontrakt (Faza 4)

```kotlin
sealed class IndicatorResult {
    abstract val name: String
    data class SingleLine(
        override val name: String,
        val values: List<Double?>
    ) : IndicatorResult()
    data class MultiLine(
        override val name: String,
        val lines: Map<String, List<Double?>>
    ) : IndicatorResult()
}
```

**Inwariant:** indeks `i` w `values`/`lines[*]` odpowiada indeksowi świecy
`candles[i]`. Brak wartości (period nieosiągnięty) → `null`, nie pomijaj.

## API keys — never in repo

- `local.properties` (gitignored):
  ```
  TWELVE_DATA_API_KEY=...
  GEMINI_API_KEY=...
  ```
- W `app/build.gradle.kts` przepuść do `BuildConfig` przez `buildConfigField`
- Czytaj przez wstrzykiwany `ApiKeys @Inject constructor()`, NIE przez globalne
  `BuildConfig.X`
- `local.properties.example` w repo z placeholderami

## Testing minimum

- Każda strategia wskaźnika: test z danymi referencyjnymi (TradingView lub ta4j
  jako oracle), tolerancja ±0.01.
- `IndicatorFactory`: test exhaustive na sealed class.
- Mappery DTO → Domain: 4 przypadki (happy, zła data, zła liczba, sortowanie).
- Repository: MockK + MockWebServer dla cache-hit, cache-miss-network-ok,
  cache-miss-network-fail.
- ViewModel: Turbine na StateFlow.
- Cel pokrycia: ~60% `domain`, ~40% `data`, smoke w `presentation`.

## Twelve Data — boundaries

- Plan darmowy: **8 req/min, 800 req/dzień**. Nie spal limitu w testach —
  MockWebServer.
- Endpoint: `/time_series?symbol=AAPL&interval=1day&outputsize=365&apikey=...`
- Liczby przychodzą jako **stringi**, daty `"yyyy-MM-dd"`.
- Rate-limit: HTTP 429 lub HTTP 200 z `status: "error"` + `code: 429`.
- Polskie tickery z GPW: sufiks `.WA` (np. `CDR.WA`, `PKN.WA`).

## Gemini — boundaries

- Model: `gemini-1.5-flash` (tańszy/szybszy)
- Brak streamingu w MVP — `suspend fun` zwraca pełną odpowiedź.
- Kontekst budowany WYŁĄCZNIE przez `AnalysisContextBuilder` — nigdy klejone
  stringi w ViewModelu.
- Disclaimer "to nie jest porada inwestycyjna" widoczny zawsze w UI czatu
  (wymagane + część obrony).
- `maxOutputTokens = 800`, `temperature = 0.6`.

## MVP scope (§11 — checklist na obronę)

W zakresie:
1. Wyszukiwarka tickerów (Twelve Data).
2. Wykres świecowy + liniowy, zakresy 1M/3M/1R.
3. RSI, MACD, SMA, EMA — autorska implementacja.
4. Cache w Room (offline fallback).
5. Ulubione tickery.
6. Czat z Gemini z konkretnym kontekstem.
7. Repository + Strategy + Factory udokumentowane w ADR.
8. Testy jednostkowe dla strategii, mapperów, ViewModeli.

Poza MVP — **nie rób, marnujesz czas**:
- Streaming Gemini
- Tryb dark/light
- Powiadomienia push
- Logowanie/konta
- Portfel / wycena pozycji
- Backtesting

## Workflow

- Każda faza = osobny branch (`feature/phase-N-<short-name>`).
- Po każdej fazie: `./gradlew build` + `./gradlew test` zielone.
- Po każdej fazie: commit + tag `phase-N-complete`, czekaj na review użytkownika.
- Każda znacząca decyzja architektoniczna → nowy plik w `docs/ADR/`.

## Slash commands

Custom commands żyją w `.claude/commands/`. Dostępne:

- `/phase-1` — bootstrap projektu (Gradle, Hilt, struktura, placeholder UI, CI)
- `/phase-2` — data layer (Twelve Data + Room + Repository)
- `/phase-3` — search + chart + favorites (UI)
- `/phase-4` — wskaźniki techniczne (Strategy + Factory) — kluczowa faza obronna
- `/phase-5` — Gemini + polish + dokumentacja końcowa
- `/check` — szybki sanity check (lint + test + grep konwencji)
- `/verify-deps` — weryfikacja wersji w `libs.versions.toml` przeciw Maven Central

**Każda komenda fazowa zaczyna się od `/plan` mode** — Claude proponuje plan,
użytkownik akceptuje, dopiero potem implementacja.

## ADR — Architecture Decision Records

`docs/ADR/000-uzycie-adr.md`, `001-repository-pattern.md`, `002-twelve-data.md`,
`003-uistate-stateflow.md`, `004-strategy-factory.md`, `005-gemini-integration.md`.

Format: kontekst (1 akapit), decyzja (1 zdanie), konsekwencje (3-5 punktów),
alternatywy odrzucone (jeśli istotne).
