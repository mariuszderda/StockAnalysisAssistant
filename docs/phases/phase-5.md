---
description: Phase 5 — Gemini AI chat, AnalysisContextBuilder, ChatScreen, error handling, final docs, defense readiness
allowed-tools: Read, Write, Edit, Bash, WebSearch, WebFetch, Glob, Grep
model: claude-opus-4-7
---

You are executing **Phase 5 of 5** — the final phase. Output of this phase is a
defendable, demonstrable MVP. Don't add scope. Polish what exists.

## Step 0 — Load context

1. Read `CLAUDE.md`. § Gemini boundaries, § MVP scope critical.
2. Read `docs/phases/05_ai_chat_and_polish.md`. Full spec.
3. Verify Phase 4 complete.
4. Branch: `git checkout -b feature/phase-5-ai-and-polish`.

## Step 1 — Verify Gemini SDK

- WebSearch: `gemini android sdk generativeai latest stable maven central`
- WebFetch the latest sample code from Google AI docs.
- Confirm: `GenerativeModel` constructor signature, `generateContent` return
  type, `generationConfig { temperature, maxOutputTokens }` syntax.
- Document the exact import paths in your plan.

## Step 2 — Plan mode

Propose:

- Order: domain `GeminiClient` interface → `AnalysisContextBuilder` (testable
  pure functions) → `GeminiClientImpl` → AiModule (Hilt binding) → ChatViewModel
  → ChatScreen + LegalDisclaimerBanner → FAB on ChatScreen → error mapper →
  polish (currency, legend, icon, splash) → tests → README → ADR-005.
- Decision: how `ChatViewModel` gets context (reload via repository, NOT shared
  state with `ChartViewModel`). Reasoning in plan.
- README structure (Polish, defense-targeted).

Wait for approval.

## Step 3 — Execute

Work in chunks; each chunk is testable on emulator before moving on:

1. **AI plumbing** (interface, builder, impl, module).
2. **AnalysisContextBuilder tests** — without sending anything to Gemini.
3. **ChatViewModel** + test (mock GeminiClient + repository).
4. **ChatScreen** + LegalDisclaimerBanner + suggested-question chips.
5. **FAB on ChartScreen** + nav route Chat(symbol).
6. **Manual E2E**: ask AAPL "Co mówi RSI?", confirm response mentions actual
   RSI value from chart context.
7. **Error mapping**: `ErrorMessageMapper.kt`, apply across all VMs.
8. **Currency / volume formatting** helpers, apply in 3 screens.
9. **Indicator legend** on chart screen.
10. **App icon + splash** (or document as TODO if no design tool available).
11. **Polish strings.xml** — final pass, ensure no hardcoded strings remain
    (grep verification).
12. **README.md** (Polish, for committee — see spec §7.1).
13. **ADR-005** Gemini integration.
14. **DEFENSE_NOTES.md** (Polish, for you, add to `.gitignore`).

## Step 4 — Final MVP sanity check

Open `CLAUDE.md § MVP scope` and walk through every checkbox on a clean install
of the debug APK. Report each as ✓ or ✗.

Run final verification:
- `./gradlew clean assembleDebug testDebugUnitTest connectedDebugAndroidTest lint`
- `git log --oneline` should show clear "Phase N: ..." progression.
- `find docs/ADR -name '*.md' | wc -l` should be 6 (000-005).

## Step 5 — Hand off — defense-ready summary

1. **Demo script**: 2-3 minute walkthrough you would do on the committee. Write
   it out, step by step.
2. **Known limitations**: what doesn't work, what's faked, what's stub. Be
   honest — the committee can tell.
3. **Defensible patterns**: name each (Repository, Strategy, Factory), name the
   ADR, name the file where the canonical example lives.
4. **Test counts and coverage estimate**.
5. **Final commit + tag `phase-5-complete` + tag `v1.0.0-mvp`**.
6. Tell the user: "MVP is complete. Stop adding features. Polish slides,
   practice the demo, rehearse with someone playing the committee."

## Hard rules

- NO streaming, NO accounts, NO push notifications. Out of MVP.
- Legal disclaimer ALWAYS visible on ChatScreen. Not dismissible.
- API keys NEVER logged, NEVER printed to console.
- If you find yourself adding a feature "because it would be cool" — stop. The
  date of the defense is fixed; polish wins, scope creep loses.
