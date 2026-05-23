# ADR-004 — Strategy + Factory dla wskaźników technicznych

**Status:** Accepted
**Data:** 2026-05-23
**Faza:** 4 (indicators)

## Kontekst

Faza 4 dokłada cztery wskaźniki techniczne (RSI, MACD, SMA, EMA) — autorska
implementacja z testami przeciw referencji (Wilder 1978 dla RSI, hand-calc
dla SMA/EMA, niezależna ścieżka EMA→diff→EMA z hand-computed wartością
asymptotyczną dla MACD). Każdy wskaźnik to jeden algorytm + zestaw parametrów
(np. RSI okres = 14, MACD = 12/26/9), a panel UI musi pozwalać użytkownikowi
włączać i wyłączać dowolny podzbiór. W tle są dwa wymagania komisji:

1. **Co najmniej dwa świadomie zastosowane wzorce projektowe** (CLAUDE.md §
   Patterns to defend). Faza 4 daje okazję na dwa naraz, bez ceremonii.
2. **Open/Closed Principle w praktyce**: dodanie piątego wskaźnika
   (Bollinger, Stochastic) nie może wymagać edycji istniejących algorytmów
   ani UI.

Najbardziej kuszącą alternatywą — zwłaszcza dla 4 wskaźników — jest jedna
duża funkcja `compute(type: String, candles, params): IndicatorResult` z
`when` na typie i całą matematyką w środku. To 200-300 linii intermixed
wzorów, brak per-algorytm test isolation, edycja jednego pliku przy każdej
zmianie. Odrzucone (sekcja niżej).

## Decyzja

Stosujemy dwa współpracujące wzorce.

**Strategy** — `IndicatorStrategy` (interface) w `domain/indicator/`. Cztery
implementacje: `RsiStrategy`, `MacdStrategy`, `SmaStrategy`, `EmaStrategy`.
Każda hermetyzuje jeden algorytm, ma swój test (`*StrategyTest.kt`) z własną
referencją matematyczną i wystawia dwie metadane: `type` (`IndicatorType.OVERLAY`
albo `OSCILLATOR`) i `displayName`. Klient widzi tylko interfejs.

**Factory** — `IndicatorFactory` (`@Singleton` w Hilt). Mapuje sealed
`IndicatorSpec` (intencja użytkownika: który wskaźnik z jakimi parametrami)
na konkretną `IndicatorStrategy`. Drugą funkcją Factory jest
`availableIndicators(): List<IndicatorSpec>` — **single source of truth**
o tym, jakie wskaźniki są oferowane w UI z jakimi domyślnymi parametrami.

Cały łańcuch wygląda tak:

```
User toggles chip
   │
   ▼
ChartViewModel.onToggleIndicator(spec)         ← IndicatorSpec, sealed
   │
   ├─► flip in activeSpecs (Set<IndicatorSpec>)
   │
   └─► recompute(candles, specs) on Dispatchers.Default
              │
              └─► factory.create(spec)               ← Factory: exhaustive when
                          │
                          ▼
                  IndicatorStrategy                  ← Strategy abstraction
                          │
                          └─► .compute(candles): IndicatorResult
                                        │
                                        ▼
                              ChartScreen partitions by IndicatorType
                                  ├─ OVERLAY    → VicoChart overlays
                                  └─ OSCILLATOR → OscillatorPanel per result
```

`IndicatorSpec` jest **sealed**, nie String. Konsekwencja: `when (spec)` w
`IndicatorFactory.create` jest exhaustive — kompilator zatrzyma build, gdy
ktoś doda nowy wariant a zapomni zaktualizować fabryki. To główna
warta-pieniędzy własność tej decyzji.

Matematyka żyje w `domain/indicator/util/` jako top-level funkcje (`sma`,
`ema`, `wilderSmooth`). Strategie są cienkimi adapterami nad nimi — wyciągają
closes, wywołują math, pakują w `IndicatorResult`. Top-level zamiast
`companion object` zgodnie z konwencją projektu (CLAUDE.md § Code conventions,
hard rule: NO companion objects w `domain/indicator/`).

## Konsekwencje

- **Per-algorytm test isolation.** `RsiStrategyTest` może zwalić się bez
  wpływu na `SmaStrategyTest`; kazda strategia ma niezależny zestaw danych
  referencyjnych. Przykład: RSI testowany na zbiorze 33 cen z Wilder 1978
  (`70.46 ± 0.01` na indeksie 14), SMA na ramp [1..10] z period=3
  (`2.0, 3.0, ..., 9.0`), MACD na ramp [1..60] gdzie liniowe wejście
  daje EMA bez transientu i stałą `MACD = 7.0` od indeksu 25.
- **Open/Closed na poziomie kompilatora.** Dodanie Bollinger Bands wymaga:
  (a) nowej klasy `BollingerStrategy : IndicatorStrategy` (MultiLine,
  OVERLAY); (b) nowego wariantu `IndicatorSpec.Bollinger(period, k)`;
  (c) jednej linii w `IndicatorFactory.create` (kompilator wymusi)
  + jednej linii w `availableIndicators()`. **Zero zmian w UI** — `IndicatorType`
  prowadzi `IndicatorResult` na właściwy panel, a `IndicatorResult.MultiLine`
  jest już obsługiwany przez generic renderer w `OscillatorPanel` / VicoChart.
- **`availableIndicators` w Factory.** Lista oferowanych wskaźników jest w
  jednym miejscu (Factory). UI czyta ją przez ViewModel; nigdy nie hardkoduje
  `IndicatorSpec.Rsi()`. Dodanie wskaźnika do listy = pojawia się w panelu.
- **Hilt minimum boilerplate.** Factory ma `@Inject constructor()` + `@Singleton`
  — Hilt instancjonuje na żądanie, dzieli między wszystkie ViewModele, bez
  osobnego modułu DI.
- **Wykonanie na `Dispatchers.Default`.** Obliczenia są CPU-bound (kilka
  passów po N=365 wartościach z mnożeniem). `Default` ma pulę = #CPU,
  `IO` (Retrofit/Room) jest zarezerwowane dla blokujących I/O. Pomiar:
  365 świec × 5 wskaźników = **~0.7 ms** na desktopowym JVM (`IndicatorPerformanceTest`).
  W tle: jeden poziom pośredni przez `withContext(defaultDispatcher)` w
  `ChartViewModel.recompute` — przekazany przez `@DefaultDispatcher`, w
  testach podstawiamy `UnconfinedTestDispatcher`.
- **Wynik z gwarancją alignmentu.** `IndicatorResult.SingleLine.values.size == candles.size`
  zawsze (test per strategy). Index `i` w `values` to indeks `candles[i]` — bez
  pomijania, bez przesunięć. Wartości niedostępne (period nieosiągnięty,
  signal seed wcześniejszy niż MACD seed) reprezentowane jako `null`. To
  warunek konieczny dla nakładania serii na wykres ceny w Vico.

## Decyzje implementacyjne (ważne dla obrony)

- **RSI używa Wilder smoothing (RMA), nie SMA.** Wilder 1978 oryginalnie
  zdefiniował RSI z RMA: `avg = (prev_avg * (period-1) + new) / period`,
  co odpowiada EMA z `α = 1/period`. SMA dawałoby RSI bez pamięci poza oknem
  i wartości niezgodne z każdą platformą tradingową (TradingView, MetaTrader,
  Yahoo, ta4j). `wilderSmooth` jest osobną funkcją w `util/`, bo jest też
  używany przez ATR/ADX (jeśli kiedyś dojdą) — nie wciskać tego do RSI.
- **MACD signal liczone po stripowaniu wiodących nulli.** `signal = EMA(macd, 9)`
  — wymaga ciągłej listy bez nulli, bo EMA seed = SMA pierwszego okresu.
  Strategia drop'uje leading nulle z MACD, woła `ema()`, repaduje wynik —
  dzięki temu invariant `values.size == candles.size` nadal trzyma.
- **`IndicatorType` jako enum, nie polimorfizm.** Mogłoby być
  `interface OverlayIndicator : IndicatorStrategy` + `interface OscillatorIndicator`.
  Enum jest tańszy: pojedyncza wartość per instancja, UI partycjonuje
  `filter { it.type == OVERLAY }` zamiast filterIsInstance dwóch interfejsów.
  Klasyfikacja jest binarna i stabilna — overengineering by jej nie służył.

## Odrzucone alternatywy

**Jedna duża funkcja `compute(type: IndicatorType, params, candles)` z `when`
na typie.** Łamie Single Responsibility (jedna funkcja zna wszystkie
algorytmy), uniemożliwia testowanie RSI bez kompilowania MACD, każda zmiana
algorytmu rusza wspólny plik. Open/Closed naruszone w dwóch miejscach: nowy
wskaźnik wymaga edycji tej funkcji, a nie tylko dodania kodu.

**Polimorfizm zamiast Factory — `class RsiSpec : IndicatorSpec() { fun
build() = RsiStrategy(period) }`.** Mieszanie "intencji" (lekka data class,
serializowana, porównywana) z "implementacją" (algorytm). Spec musiałby
importować strategie, a domain podział spec/strategy znika. Factory jako
oddzielny obiekt zostawia spec czystym i pozwala mieć jedno miejsce, gdzie
spec → strategy mapping żyje (testowalne, zmienialne, łatwe do mockowania
w testach VM).

**Skip Factory — `ChartViewModel` tworzy strategie wprost.** Każdy ViewModel
musiałby znać listę dostępnych wskaźników (duplikacja
`listOf(RsiStrategy(14), ...)`); dodanie wskaźnika do panelu wymagałoby
zmiany VM, nie tylko domeny. Factory wprowadza jednoz źródło prawdy o
"co istnieje + z jakimi domyślnymi parametrami".

**`IndicatorResult` jako Map<String, List<Double?>> tylko (bez SingleLine).**
Wymuszałby na UI dla SMA stale używać `result.lines["sma"]!!`. SingleLine
+ MultiLine + sealed daje UI exhaustive when i zero null assertion na keyu.

**MACD histogram jako kolumny (Vico ColumnLayer).** W Vico 2.1.2 stack
ColumnLayer+LineLayer w jednym CartesianChart wymaga współdzielonej osi Y;
histogram MACD zazwyczaj mieści się w innym rzędzie wielkości niż linie
MACD/signal, co zaburza skalę. W MVP renderujemy histogram jako trzecią
linię w `OscillatorPanel` — czytelne wizualnie, jeden layer, jeden axis.
Jeśli w Fazie 5+ pojawi się czas na dwa osobne axisy, można podmienić bez
zmian w domenie.

## Pytania obronne (z odpowiedziami)

1. **Strategy nad `when (type)`?** Per-algorytm test isolation, OCP w
   praktyce, brak mega-funkcji rosnącej z każdym wskaźnikiem.
2. **Co daje sealed `IndicatorSpec`?** Exhaustive `when` w Factory →
   kompilator zatrzyma build przy nowym wariancie bez branchu. Runtime
   `else -> throw` zamienione na compile-time check.
3. **Wilder vs SMA w RSI?** Wilder 1978 oryginalny wzór (RMA, α = 1/period).
   SMA dałoby inne wartości, niezgodne z każdą platformą tradingową; zmiana
   semantyczna, nie kosmetyka.
4. **Jak `IndicatorType` płynie do UI?** Strategy deklaruje `type` (enum) →
   `IndicatorResult.type` dziedziczy → `ChartScreen` partycjonuje
   `filter { it.type == OVERLAY }` na overlays do `VicoChart` i
   `filter { it.type == OSCILLATOR }` na panele pod wykresem. Type-driven,
   nie string-matching.
5. **Dodanie Bollinger Bands — gdzie?** (a) `BollingerStrategy.kt` w
   `domain/indicator/`; (b) `IndicatorSpec.Bollinger(period, k)` w
   `IndicatorSpec.kt`; (c) branch w `IndicatorFactory.create` (kompilator
   wymusi) + linia w `availableIndicators()`. UI bez zmian — MultiLine już
   obsługiwany, OVERLAY już renderowany.
6. **Czemu `availableIndicators()` siedzi w Factory?** Jedna własność "co
   istnieje z jakimi domyślnymi parametrami". UI nie powtarza `listOf(...)`,
   spec dodany w Factory pojawia się w panelu bez zmian w UI.
7. **Czemu `Dispatchers.Default` dla `recompute`?** CPU-bound math. `IO`
   (64+ wątków) marnowałby zasoby; `Main` zamroziłby UI. `Default` (#CPU
   wątków) jest dla algorytmów. Mierzone: 0.7 ms na 365 świec × 5
   wskaźników.
