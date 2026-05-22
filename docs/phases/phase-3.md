---
description: Phase 3 — Compose screens (Search, Favorites, Chart with Vico), navigation, ViewModels
allowed-tools: Read, Write, Edit, Bash, WebSearch, WebFetch, Glob, Grep
model: claude-opus-4-7
---

You are executing **Phase 3 of 5** of the SAA project.

## Step 0 — Load context

1. Read `CLAUDE.md`. Hard constraints.
2. Read `docs/phases/03_search_and_chart.md`. Full spec.
3. Verify Phase 2 complete: `git tag | grep phase-2-complete` must return a tag.
4. Branch: `git checkout -b feature/phase-3-search-chart`.

## Step 1 — Verify Vico API

Vico has had API changes between 1.x and 2.x. Before writing any chart code:

- WebSearch: `vico chart compose latest version compose-m3`
- WebFetch the Vico wiki or docs for the major version you'll use
- Confirm the exact composable names and import paths for `CandlestickCartesianLayer`,
  `LineCartesianLayer`, `rememberMarker`. Document them in your plan.

If anything in `docs/phases/03_search_and_chart.md` references an API that
doesn't match current Vico, **flag it in your plan** before coding.

## Step 2 — Plan mode

Propose:

- Order: navigation skeleton → SearchScreen → FavoritesScreen → ChartScreen → tests
- For each ViewModel: state shape, public functions, what's mocked in tests
- Compose UI tests: which screen, what interaction, what assertion
- Vico API mapping (composable names confirmed in Step 1)
- ADR-003 (UiState + StateFlow convention) outline

Wait for approval.

## Step 3 — Execute, screen by screen

For each of the three screens, in this order (Search → Favorites → Chart):

1. UiState (data class).
2. ViewModel (with `@HiltViewModel`, `viewModelScope`, StateFlow).
3. Composable screen (Material 3, Polish strings via `stringResource`).
4. ViewModel unit test (Turbine + MainDispatcherRule).
5. Compose UI smoke test (one per screen, basic rendering + interaction).
6. Wire into `SaaNavHost`.
7. Run tests: `./gradlew testDebugUnitTest`.

After all three screens:
- Re-run on emulator. Manual checklist from spec § "Jak zweryfikować".
- Confirm offline mode works (airplane mode → cached data renders).

## Step 4 — Hand off

1. Summary + test counts.
2. Screenshots of the three screens (use `adb exec-out screencap` if running
   on emulator, save to `docs/screenshots/phase-3-*.png`).
3. Manual E2E verification result.
4. Propose commit + tag `phase-3-complete`.
5. **Do not start Phase 4.** Stop.

## Hard rules

- Zero hardcoded strings in Composables. Everything via `stringResource()`.
- No indicator math in this phase. `IndicatorPanel` is a placeholder.
- No Gemini in this phase.
- Each ViewModel testable with mocked `StockRepository` only — no Retrofit/Room
  in test dependencies.
