---
description: Phase 2 — data layer (Twelve Data API, DTO, Room cache, Repository pattern)
allowed-tools: Read, Write, Edit, Bash, WebSearch, WebFetch, Glob, Grep
model: claude-opus-4-7
---

You are executing **Phase 2 of 5** of the Stock Analysis Assistant (SAA) project.
This is the first of three **defendable** phases — Repository Pattern lives here
and the committee will ask about it.

## Step 0 — Load context

1. Read `CLAUDE.md`. Hard constraints.
2. Read `docs/phases/02_data_layer.md`. Full spec.
3. Verify that Phase 1 is complete: `./gradlew assembleDebug --no-daemon` must
   succeed. If not — stop, tell the user, do not proceed.
4. Confirm we're on a fresh branch: `git checkout -b feature/phase-2-data-layer`.

## Step 1 — Read key sections of CLAUDE.md

Pay critical attention to:

- § Package structure — `data/`, `domain/`, `mapper/` boundaries
- § Patterns to defend — Repository Pattern is implemented here
- § Twelve Data — boundaries (rate limits, string-encoded numbers, polish tickers)
- § IndicatorResult — not implemented yet but data shape matters
- § Testing minimum

## Step 2 — Enter plan mode

Use `/plan` to propose:

- File creation order (start with domain model → DTO → mappers → DAO → Repository
  → Hilt modules → tests)
- Sketch of `StockRepositoryImpl.getCandles()` cache-first decision tree
- Test strategy (which scenarios, MockWebServer or MockK for each)
- ADRs you will write (001-repository, 002-twelve-data)

Stop and wait for approval. Do not write files yet.

## Step 3 — Execute incrementally

After approval, work in **small reviewable chunks**:

1. Domain models (Stock, Candle, Range, DataError) + their JVM tests → run tests.
2. DTO + mappers + mapper tests → run tests.
3. Room entities + DAOs + DAO instrumented tests → run androidTest.
4. Repository interface in `domain` + impl in `data` + Hilt bindings.
5. Repository tests with MockWebServer + MockK.
6. ADRs.
7. Final smoke: `./gradlew assembleDebug` + `testDebugUnitTest` + `connectedDebugAndroidTest`.

After each chunk: run the relevant tests, fix issues, then proceed.

## Step 4 — Hand off

When complete:

1. Summary of what was built.
2. Test counts: unit tests passing / android tests passing.
3. Manual verification: with real API key in `local.properties`, fetch AAPL
   candles end-to-end and confirm data lands in Room. Document the result.
4. Propose commit + tag `phase-2-complete`.
5. **Do not start Phase 3.** Stop, wait for review.

## Hard rules

- ZERO `androidx.*` / `retrofit2.*` / `androidx.room.*` / `moshi.*` imports in
  `domain/`. Verify with grep after writing.
- ViewModel-level code does NOT belong in Phase 2 — that's Phase 3. If you find
  yourself writing one, stop.
- No UI in this phase.
- Use `Result<T>` from repository, not throw-and-catch.
- Cache TTL: 24h. Constants in companion object on `StockRepositoryImpl`.
