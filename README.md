# SAA — Claude Code prompt pack

Plan promptów dla Claude Code do projektu Stock Analysis Assistant (Android,
projekt inżynierski).

## Co tu jest

```
.
├── CLAUDE.md                      # memory file — Claude Code czyta auto przy starcie
├── .claude/
│   └── commands/                  # slash-commands dla Claude Code
│       ├── phase-1.md             # /phase-1  — bootstrap
│       ├── phase-2.md             # /phase-2  — data layer
│       ├── phase-3.md             # /phase-3  — UI screens
│       ├── phase-4.md             # /phase-4  — wskaźniki (Strategy + Factory)
│       ├── phase-5.md             # /phase-5  — Gemini + polish
│       ├── check.md               # /check    — sanity check
│       └── verify-deps.md         # /verify-deps — weryfikacja wersji
└── docs/
    └── phases/                    # pełne specyfikacje (czytane z dysku przez agenta)
        ├── 01_bootstrap.md
        ├── 02_data_layer.md
        ├── 03_search_and_chart.md
        ├── 04_indicators.md
        └── 05_ai_chat_and_polish.md
```

## Jak zacząć

### 1. Wrzuć całość do swojego repo

```bash
# w pustym katalogu projektu (świeży git init)
# skopiuj zawartość tego pakietu do roota:
#   CLAUDE.md
#   .claude/
#   docs/

git add CLAUDE.md .claude/ docs/
git commit -m "Add Claude Code prompt pack"
```

### 2. Odpal Claude Code w katalogu projektu

```bash
cd /ścieżka/do/StockAnalysisAssistant
claude
```

Przy starcie Claude Code automatycznie wczyta `CLAUDE.md` — zobaczysz to w
banerze. Slash-komendy z `.claude/commands/` są dostępne natychmiast.

### 3. Sprawdź że komendy są widoczne

W Claude Code wpisz `/` i powinieneś zobaczyć:
- `/phase-1` — Phase 1 — bootstrap...
- `/phase-2` ...
- `/check`
- `/verify-deps`

Jeśli nie widać — upewnij się że jesteś w roocie repo (Claude Code szuka
`.claude/commands/` względem cwd) i że pliki mają rozszerzenie `.md`.

## Workflow

### Faza 1

```
> /phase-1
```

Claude Code:
1. Wczyta `CLAUDE.md` + `docs/phases/01_bootstrap.md`
2. Zweryfikuje aktualne wersje pakietów przez WebSearch (krytyczne — modele
   halucynują wersje)
3. Wejdzie w **plan mode** i przedstawi co stworzy
4. Czeka na Twoje `approve`
5. Tworzy pliki w kolejności + uruchamia `./gradlew help` co kilka kroków
6. Wykonuje pełną weryfikację z checklisty
7. Zatrzymuje się i czeka na review

**Po fazie:**
```
> /check
```
Szybki sanity check (lint + test + grep konwencji).

**Jeśli wszystko OK:**
```bash
git checkout -b feature/phase-1-bootstrap   # jeśli komenda nie zrobiła tego sama
git add .
git commit -m "Phase 1: bootstrap project skeleton"
git tag phase-1-complete
```

### Pozostałe fazy

Tak samo: `/phase-2`, `/phase-3`, `/phase-4`, `/phase-5`. Każda zaczyna się od
plan mode, każda kończy commitem + tagiem.

**Reguła:** nie zaczynaj fazy N+1 dopóki faza N nie ma zielonego buildu
i commita.

## Klucze API

Przed Fazą 2:

```bash
# w roocie projektu:
cat > local.properties <<EOF
sdk.dir=/twoja/ścieżka/do/Android/Sdk
TWELVE_DATA_API_KEY=...   # https://twelvedata.com/account/api-keys
GEMINI_API_KEY=...         # https://aistudio.google.com/apikey (potrzebne dopiero w Fazie 5)
EOF
```

`local.properties` jest w `.gitignore` — nigdy nie commituj.

## Komendy pomocnicze

### `/check`
Szybki status projektu: build, testy, lint, grep konwencji. Nie modyfikuje plików.

### `/verify-deps`
Sprawdza każdą wersję w `libs.versions.toml` przeciw Maven Central.
Łapie halucynacje (RC, alpha, nieistniejące wersje).

Uruchamiaj po każdej fazie albo zawsze gdy `./gradlew build` zaczyna pluć
"Could not find ...".

## Tipy

### Plan mode jako bezpiecznik

Każda komenda fazowa wymusza plan mode. **Nie pomijaj.** Czytaj plan,
ewentualnie odrzuć i poproś o korekty. To 5 minut czytania które uratuje
30 minut naprawiania złych decyzji.

### Małe chunki

Komendy każą Claude Code pracować chunk-by-chunk i uruchamiać testy między
nimi. Jeśli widzisz że próbuje zbudować wszystko naraz — przerwij (Esc) i
poproś o mniejsze kroki.

### Branching

Każda faza na osobnym branchu, każda faza tag-uje wynik:
```
feature/phase-1-bootstrap     → phase-1-complete
feature/phase-2-data-layer    → phase-2-complete
feature/phase-3-search-chart  → phase-3-complete
feature/phase-4-indicators    → phase-4-complete
feature/phase-5-ai-and-polish → phase-5-complete, v1.0.0-mvp
```

Tag = łatwy rollback gdyby kolejna faza rozwaliła wcześniejszą.

### Long-running session

Fazy 2 i 4 są długie (kilka godzin pracy). Jeśli context window się wypełnia,
Claude Code samo zasugeruje `/compact`. Jeśli sesja zostaje za długa — `/clear`
i wystartuj nową, agent przeczyta `CLAUDE.md` od nowa.

### Gdy coś idzie nie tak

- **Build fails po fazie:** uruchom `/verify-deps`. 80% problemów to złe wersje.
- **Wzorce źle zaimplementowane:** w Fazie 4 jest dedykowany self-defense check.
  Jeśli nie potrafisz odpowiedzieć na pytania z planu — wróć i popraw.
- **Vico API się zmieniło:** Faza 3 i 4 mają w Step 1 weryfikację Vico API
  z WebSearch. Trzymaj się tego co znajdzie, nie tego co pamiętasz.

## Co kiedy się skończy

Po Fazie 5:
1. **Nie dorzucaj featurów.** Scope zamrożony.
2. Slajdy do prezentacji.
3. Nagraj 2-3 min demo (backup gdyby na sali nie było WiFi).
4. Próbna obrona z kimś — niech pyta z `CLAUDE.md § Patterns to defend`.

## Co robić gdy AI mówi głupotę

Modele LLM (włącznie z Claude) **halucynują numery wersji pakietów** — to
nie jest "może czasem", to powtarzalny problem. Specjalne zabezpieczenia w
tym pakiecie:

- `CLAUDE.md § Stack` — eksplicytna instrukcja "weryfikuj przed użyciem"
- `/phase-1 Step 1` — wymuszona weryfikacja przed plan mode
- `/verify-deps` — narzędzie do post-hoc audytu

Jeśli mimo to coś przepuści — to nie jest Twoja wina, ale obowiązek poprawienia
jest Twój. Wklej `./gradlew build 2>&1 | Tee-Object build.log | select -Last 80`
(PowerShell) albo `tail -80` (bash) i wracaj do mnie / nowej sesji Claude.

---

**Powodzenia na obronie.** 🎯
