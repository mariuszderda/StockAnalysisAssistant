---
description: Phase 4 — Indicators (Strategy + Factory patterns, RSI/MACD/SMA/EMA, chart integration)
allowed-tools: Read, Write, Edit, Bash, WebSearch, WebFetch, Glob, Grep
model: claude-opus-4-7
---

You are executing **Phase 4 of 5** of the SAA project.

**This is THE defendable phase.** Two design patterns are implemented here
deliberately and must be defendable to a thesis committee. Treat this with extra
care — code quality matters more than speed.

## Step 0 — Load context

1. Read `CLAUDE.md`. Pay extreme attention to § Patterns to defend.
2. Read `docs/phases/04_indicators.md`. Full spec.
3. Verify Phase 3 complete.
4. Branch: `git checkout -b feature/phase-4-indicators`.

## Step 1 — Algorithm verification

Algorithms must be author-implemented. Before coding, verify formulas:

- **RSI** must use Wilder's smoothing (RMA), NOT simple moving average.
  WebFetch: https://en.wikipedia.org/wiki/Relative_strength_index
  Confirm: initial value = SMA of first `period` gains/losses, then
  `avg = (prev_avg * (period-1) + new_value) / period`.

- **MACD**: fast EMA(12) − slow EMA(26) → signal = EMA(9) of MACD line →
  histogram = MACD − signal.

- **EMA**: initial value = SMA of first `period` values, then
  `ema[i] = value[i] * k + ema[i-1] * (1-k)`, k = 2/(period+1).

- **SMA**: rolling arithmetic mean, `period-1` leading nulls.

Document formula source in KDoc on each function (Wilder 1978, etc.).

## Step 2 — Reference data acquisition

You need test fixtures. Options (in order of preference):

1. **TradingView screenshot/manual** — capture 30+ daily closes for AAPL, then
   read RSI(14), MACD(12,26,9), SMA(20), EMA(20) values from same chart for last
   ~10 candles. Hardcode in test. This is the gold standard.
2. **ta4j Java library values** — fetch via Maven/GitHub, run on same data,
   capture output. Useful if option 1 too time-consuming.
3. **Hand-calculated** — for SMA only, by hand on simple `[1..10]` input.

In your plan, state which option you'll use for each indicator. **Do not
proceed without reference values.** Tolerance is ±0.01.

## Step 3 — Plan mode

Propose:

- File creation order: IndicatorResult → IndicatorStrategy → util/MovingAverages →
  util/WilderSmoothing → 4 strategies → IndicatorFactory → tests → ChartScreen
  integration → ADR-004
- For each strategy: which reference data, where it comes from
- Hilt wiring (IndicatorFactory is @Singleton, no module needed)
- ChartScreen changes (rough diff from Phase 3)

Wait for approval.

## Step 4 — Execute, indicator by indicator

For each indicator:

1. Helper functions in `domain/indicator/util/` (if needed for this indicator).
2. Helper unit test.
3. Strategy class.
4. Strategy unit test with reference data (±0.01).
5. Run tests, confirm green.

After all 4 strategies:
6. `IndicatorFactory` + test (exhaustive on `IndicatorSpec`).
7. Wire into `ChartViewModel` (add `factory: IndicatorFactory`, `toggleIndicator`,
   `recomputeIndicators` on `Dispatchers.Default`).
8. Rewrite `IndicatorPanel` from placeholder to real ModalBottomSheet.
9. Render OVERLAY indicators on main chart, OSCILLATOR as separate composables
   under chart.
10. ADR-004.
11. Manual E2E on emulator: toggle each indicator on AAPL/MSFT, confirm renders.

## Step 5 — Self-defense check

Before committing, answer in your handoff summary (this is your dry run for the
committee):

1. Why Strategy instead of `when (type)` in one function?
2. What does exhaustive `when` on sealed class give the Factory?
3. Why Wilder smoothing in RSI, not SMA? What would change?
4. How does `IndicatorType` (OVERLAY/OSCILLATOR) flow through the UI?
5. How would you add Bollinger Bands? Point at files you'd change.
6. Why is `availableIndicators()` in Factory, not in UI?
7. Why is `recomputeIndicators` on `Dispatchers.Default`?

If you can't answer any of these crisply — go back, fix the code so the answer
becomes obvious.

## Step 6 — Hand off

1. Summary + test counts (RSI test count, MACD test count, etc.).
2. Reference data sources (per indicator).
3. ADR-004 written.
4. Performance: time `recomputeIndicators` for 365 candles + 5 indicators.
   Must be < 100ms. Report actual number.
5. Commit + tag `phase-4-complete`.
6. **Do not start Phase 5.** Stop.

## Hard rules

- All indicator math is **top-level functions** in `domain/indicator/util/`.
  NO companion objects. NO static helpers in classes.
- Each `IndicatorResult.SingleLine.values.size == candles.size`.
  Same for each list in `MultiLine.lines`. Verified by test.
- Leading nulls where indicator can't yet be calculated. NEVER skip indices.
- No "TODO" left in `domain/indicator/`. Grep proves it before commit.
