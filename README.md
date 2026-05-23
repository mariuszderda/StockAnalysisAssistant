# Stock Analysis Assistant (SAA)

Aplikacja mobilna Android do analizy technicznej akcji giełdowych. Praca
inżynierska, broniona w ~czerwcu 2026.

Użytkownik wpisuje ticker (`AAPL`, `MSFT`, `CDR.WA`), ogląda wykres
świecowy/liniowy z konfigurowalnym zakresem (1M / 3M / 1R), włącza wskaźniki
techniczne (RSI, MACD, SMA, EMA) implementowane od zera, a następnie pyta
asystenta AI (Google Gemini) o interpretację po polsku. Asystent dostaje
deterministyczny kontekst (ostatnie świece + bieżące wartości wskaźników) —
nie zgaduje, nie udziela porad inwestycyjnych.

## Architektura

```
┌───────────────────────────────────────────────────────────────┐
│                       presentation                            │
│  Compose screens · ViewModels · UiState · navigation (Hilt)   │
└───────────────────────────────────────────────────────────────┘
            │  StateFlow                  ▲  domain interfaces
            ▼                             │
┌───────────────────────────────────────────────────────────────┐
│                          domain                               │
│  model/ · indicator/ (Strategy + Factory) · ai/ · repository/ │
│                  brak zależności od Androida                  │
└───────────────────────────────────────────────────────────────┘
            ▲                             │
            │  implements                 ▼
┌───────────────────────────────────────────────────────────────┐
│                           data                                │
│  remote (Twelve Data) · local (Room) · repository · ai (REST) │
└───────────────────────────────────────────────────────────────┘
```

**MVVM + Clean Architecture (3 warstwy).** `domain` zależy tylko od JVM
(testy bez emulatora); `data` i `presentation` zależą od `domain`. Reguła
sprawdzalna grepem — `domain/` nie importuje `androidx.`, `retrofit2.`,
`com.squareup.moshi.`.

## Wzorce projektowe (do obrony)

Komisja oczekuje co najmniej dwóch świadomie zastosowanych wzorców. SAA
ma trzy podstawowe + trzy „darmowe" (dziedzina, nie ozdoba):

| Wzorzec       | Zastosowanie                         | ADR                                            |
|---------------|--------------------------------------|------------------------------------------------|
| **Repository** | dane (API + Room) za jednym interfejsem `StockRepository` w `domain.repository` | [ADR-001](docs/ADR/001-repository-pattern.md) |
| **Strategy**   | algorytmy wskaźników: `IndicatorStrategy` + 4 implementacje (`RsiStrategy`, `MacdStrategy`, `SmaStrategy`, `EmaStrategy`) | [ADR-004](docs/ADR/004-strategy-factory.md) |
| **Factory**    | mapowanie `IndicatorSpec` → `IndicatorStrategy` (`IndicatorFactory`, exhaustive `when` na sealed class) | [ADR-004](docs/ADR/004-strategy-factory.md) |
| Observer (free)| `StateFlow<UiState>` w każdym ViewModelu, `collectAsStateWithLifecycle` w Compose | [ADR-003](docs/ADR/003-uistate-stateflow.md) |
| Singleton (free)| `@Singleton` w Hilt (Repository, Factory, ApiKeys, Gemini stack)        | —                                              |
| Adapter / Mapper (free)| DTO ↔ Entity ↔ Domain w `data/mapper/`                          | —                                              |

## Tech stack

- **Język:** Kotlin 2.1.x, JDK 17
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt 2.58
- **Sieć:** Retrofit 3.0.0 + OkHttp 5.3.2 + Moshi 1.15.2 (codegen via KSP)
- **Persistence:** Room 2.8.4 (cache OHLC 24h + ulubione)
- **Wykresy:** Vico 2.1.2 (Compose-M3)
- **Async:** Kotlin Coroutines + StateFlow
- **Nawigacja:** Navigation Compose 2.9.8
- **AI:** Google Gemini API (REST, `generativelanguage.googleapis.com`,
  model `gemini-1.5-flash`). Decyzja: REST przez Retrofit, **nie** deprecated
  SDK ani Firebase AI — [ADR-005](docs/ADR/005-gemini-integration.md).
- **Dane rynkowe:** Twelve Data API (free tier — 8 req/min, 800 req/dzień)
- **Testy:** JUnit 5 + Truth + MockK + Turbine + MockWebServer

`minSdk = 26`, `targetSdk = 34`, `compileSdk = 36`.

## Uruchomienie

### 1. Klucze API

Załóż konta i pobierz klucze:
- Twelve Data: <https://twelvedata.com/account/api-keys>
- Google AI Studio (Gemini): <https://aistudio.google.com/apikey>

Stwórz `local.properties` w roocie projektu (gitignored — patrz
`local.properties.example`):

```properties
sdk.dir=C:\\Users\\twoja-nazwa\\AppData\\Local\\Android\\Sdk
TWELVE_DATA_API_KEY=tutaj_klucz
GEMINI_API_KEY=tutaj_klucz
```

Bez klucza Gemini ekran czatu wyświetli komunikat „Brak klucza API Gemini".

### 2. Build i instalacja

```bash
./gradlew assembleDebug
./gradlew installDebug   # wymaga podłączonego urządzenia / emulatora API 26+
```

### 3. Testy

```bash
./gradlew testDebugUnitTest        # JVM unit tests
./gradlew connectedDebugAndroidTest # Compose UI + Room DAO (wymaga emulatora)
./gradlew lint                     # Android Lint
```

## Struktura pakietów

```
pl.gwsh.stockanalysis/
├── data/
│   ├── remote/          # Retrofit (Twelve Data), DTO
│   ├── local/           # Room: entities, DAO, database
│   ├── repository/      # implementacja StockRepository (cache-first)
│   ├── mapper/          # DTO ↔ Entity ↔ Domain
│   └── ai/              # GeminiClientImpl (REST), GeminiApi, DTO
├── domain/
│   ├── model/           # Stock, Candle, Range, DataError
│   ├── repository/      # StockRepository (interfejs)
│   ├── indicator/       # IndicatorStrategy + 4 strategie + IndicatorFactory
│   │   └── util/        # sma, ema, wilderSmooth (top-level fun)
│   └── ai/              # GeminiClient, AnalysisContextBuilder, GeminiError
├── presentation/
│   ├── theme/
│   ├── navigation/      # SaaNavHost, SaaDestinations, SaaBottomBar
│   ├── common/          # ErrorText, LegalDisclaimerBanner, NumberFormatters
│   └── screens/
│       ├── search/      # SearchScreen + ViewModel + UiState
│       ├── favorites/   # FavoritesScreen + ViewModel + UiState
│       ├── chart/       # ChartScreen + ViewModel + UiState + VicoChart +
│       │                # IndicatorPanel + OscillatorPanel + IndicatorLegend
│       └── chat/        # ChatScreen + ViewModel + UiState + ChatMessage
└── di/                  # NetworkModule, DatabaseModule, RepositoryModule,
                         # CoroutineModule, AiModule, ApiKeys, kwalifikatory
```

## ADR — Architecture Decision Records

| Numer  | Tytuł                                                                       |
|--------|-----------------------------------------------------------------------------|
| [000](docs/ADR/000-uzycie-adr.md) | Po co nam ADR-y                                  |
| [001](docs/ADR/001-repository-pattern.md) | Repository Pattern dla warstwy danych    |
| [002](docs/ADR/002-twelve-data.md) | Wybór dostawcy danych rynkowych                 |
| [003](docs/ADR/003-uistate-stateflow.md) | UiState (sealed) + StateFlow              |
| [004](docs/ADR/004-strategy-factory.md) | **Strategy + Factory dla wskaźników**      |
| [005](docs/ADR/005-gemini-integration.md) | **Gemini przez REST (Retrofit)**         |

## Ograniczenia (świadome)

- **Twelve Data free tier:** 8 req/min, 800 req/dzień. Repository ma
  cache-first z TTL 24h (ADR-001) — kolejne otwarcia tego samego tickera nie
  jadą po sieci. Demo komisji powinno trzymać się 2-3 tickerów.
- **Gemini free tier:** ~15 req/min (Google AI Studio). Demo: po jednym
  pytaniu na ticker.
- **Disclaimer „to nie jest porada inwestycyjna":** zawsze widoczny w
  ChatScreen (`LegalDisclaimerBanner`, non-dismissable). Wymóg projektu
  i CLAUDE.md § Gemini boundaries.

## Poza MVP (nie robione świadomie)

- streaming odpowiedzi Gemini,
- konta / logowanie,
- powiadomienia push,
- portfel / wycena pozycji,
- backtesting,
- tryb dark/light (Material 3 dynamic colors)
- powiadomienia o alertach cenowych.

## Workflow developerski

Praca chunkami po fazach (5 łącznie):

| Faza   | Branch                                | Tag                              |
|--------|---------------------------------------|----------------------------------|
| 1      | `feature/phase-1-bootstrap`           | `phase-1-complete`               |
| 2      | `feature/phase-2-data-layer`          | `phase-2-complete`               |
| 3      | `feature/phase-3-search-chart`        | `phase-3-complete`               |
| 4      | `feature/phase-4-indicators`          | `phase-4-complete`               |
| 5      | `feature/phase-5-ai-and-polish`       | `phase-5-complete`, `v1.0.0-mvp` |

## Testy — pokrycie

- `domain/indicator/` — 43 testy (algorytmy + Factory + perf < 100 ms)
- `domain/ai/AnalysisContextBuilder` — 8 testów
- `data/ai/GeminiClientImpl` — 7 testów (MockWebServer)
- `data/repository/StockRepositoryImpl` — 11 testów (MockK + MockWebServer)
- `data/mapper/` — 8 testów
- `presentation/screens/*/ViewModel` — 22 testy
- `presentation/common/NumberFormatters` — 6 testów

Razem: ~120 testów jednostkowych + UI Compose tests (`androidTest`).
