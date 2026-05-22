# ADR-000 — Format Architecture Decision Records

**Status:** Accepted
**Data:** 2026-05-22
**Faza:** 1 (bootstrap)

## Kontekst

Projekt SAA jest broniony przed komisją inżynierską. Komisja oczekuje
udokumentowanych decyzji projektowych — nie tylko "tak wyszło", ale "tak
zdecydowaliśmy, z takich powodów, odrzucając takie alternatywy". CLAUDE.md
wymaga, by każda znacząca decyzja architektoniczna miała swój ADR. Tu definiujemy
format, żeby kolejne ADR-y były spójne.

## Decyzja

Każdy ADR żyje w `docs/ADR/NNN-krotki-tytul-kebab.md`, gdzie NNN to trzycyfrowy
numer monotonicznie rosnący (zero-padded). Format pliku:

1. **Nagłówek H1** — `ADR-NNN — Tytuł po polsku`
2. **Metadane** w jednym bloku: `Status` (Proposed / Accepted / Superseded by …),
   `Data` (ISO `YYYY-MM-DD`), `Faza` (numer fazy projektu).
3. **Sekcja Kontekst** — 1 akapit. Co się dzieje, jaki problem rozwiązujemy.
4. **Sekcja Decyzja** — 1 zdanie + opcjonalnie krótka lista konkretów.
5. **Sekcja Konsekwencje** — 3-5 punktów. Co zyskujemy, co tracimy, co teraz
   trzeba robić inaczej.
6. **Sekcja Odrzucone alternatywy** (jeśli były) — po jednym akapicie na każdą,
   z uzasadnieniem dlaczego nie.

Tekst po polsku (komisja czyta). Identyfikatory klas / pakietów / bibliotek
po angielsku (idiomatyczny Kotlin). Bez zbędnego marketingu — krótko, konkretnie.

## Konsekwencje

- Każda znacząca decyzja architektoniczna z CLAUDE.md § "Patterns to defend"
  dostaje własny ADR (Repository, Strategy, Factory, Twelve Data, Gemini,
  UiState/StateFlow — minimum sześć dokumentów do końca projektu).
- Numeracja zarezerwowana z góry: `001-repository-pattern`, `002-twelve-data`,
  `003-uistate-stateflow`, `004-strategy-factory`, `005-gemini-integration`.
- ADR nie jest commitowany razem z kodem, który go realizuje, tylko **przed** —
  najpierw decyzja, potem implementacja zgodna z ADR.
- Jeśli decyzja zostaje obalona — nie kasujemy ADR-a, dopisujemy nowy ze statusem
  "Supersedes ADR-NNN" i w starym aktualizujemy "Status: Superseded by ADR-MMM".

## Odrzucone alternatywy

**Notatki wbudowane w CLAUDE.md.** Plik główny rośnie poza obronność; ADR jako
osobne pliki łatwiej cytować w pracy inżynierskiej i przy obronie.

**Notion / Confluence.** Decyzje powinny być wersjonowane razem z kodem. Brak
internetu na obronie = brak dokumentacji = przegrana.
