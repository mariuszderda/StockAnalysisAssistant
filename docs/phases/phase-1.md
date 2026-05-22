---
description: Phase 1 — bootstrap Android project (Gradle, Hilt, package structure, placeholder UI, CI)
allowed-tools: Read, Write, Edit, Bash, WebSearch, WebFetch, Glob, Grep
model: claude-opus-4-7
---

You are executing **Phase 1 of 5** of the Stock Analysis Assistant (SAA) project.

## Step 0 — Load context (do this FIRST, in order)

1. Read `CLAUDE.md` at the repository root. Treat it as hard constraints.
2. Read `docs/phases/01_bootstrap.md` — the full spec for this phase.
3. Run `ls -la` to confirm what's already in the working directory.

## Step 1 — Verify versions (CRITICAL)

Before writing any Gradle file, run these WebSearch queries and record the
**latest stable** version of each. Do NOT use your training data for these:

- `android gradle plugin AGP latest stable 8.x 2026`
- `kotlin latest stable version 2.x`
- `gradle distribution version compatible with AGP <found-version>`
- `compose bom latest stable maven`
- `hilt android latest stable maven central`
- `retrofit latest stable maven central`
- `moshi latest stable maven central`
- `room latest stable androidx`
- `vico compose chart library latest stable`
- `gemini generativeai android sdk maven central`

Write each verified version into your plan with a comment showing the source URL.
**If you find yourself about to write a version ending in -RC, -alpha, -beta, or
-SNAPSHOT — stop, search again, find the stable.**

## Step 2 — Enter plan mode

Use `/plan` to propose what you will create:

- Files to create (paths, brief description each)
- Verified versions table (library → version → source URL)
- Order of operations
- What you will NOT do in this phase (out of scope items from the spec)

Stop and wait for explicit user approval before writing any file.

## Step 3 — Execute

Only after the plan is approved:

1. Create files in the order proposed.
2. After every 3-4 file creations, run `./gradlew help --no-daemon` to confirm
   Gradle config is still valid (fast sanity check).
3. After all files: run the full verification checklist from
   `docs/phases/01_bootstrap.md` § "Jak zweryfikować".

## Step 4 — Hand off

When complete:

1. Summarize what was built (one paragraph).
2. List any decisions you made that weren't in the spec.
3. Run `git status` and propose a commit message.
4. **Do not start Phase 2.** Stop, wait for review.

## Hard constraints from CLAUDE.md

- NO business logic in this phase. Scaffolding only.
- BuildConfig wired from `local.properties` (NOT in code).
- Package structure exactly as in CLAUDE.md § "Package structure".
- Verify every version against Maven Central before committing to it.
- If you're uncertain about an API or a version — search first, ask second,
  code third.
