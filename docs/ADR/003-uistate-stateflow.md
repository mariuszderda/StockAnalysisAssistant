# ADR-003 — UiState + StateFlow jako kontrakt prezentacji

**Status:** Accepted
**Data:** 2026-05-23
**Faza:** 3 (UI: Search/Favorites/Chart)

## Kontekst

Każdy ekran Compose potrzebuje pojedynczego źródła prawdy o tym, co aktualnie
narysować. Mamy dwie skrajne praktyki spotykane w MVVM/Compose:

1. **„Tłusta" `data class` ze wszystkim:** `data class State(val loading: Boolean,
   val error: String?, val data: List<X>?)`. Łatwe do napisania, ale dopuszcza
   niespójne kombinacje (`loading=true` + `error="X"` + `data=[…]`) — UI musi
   ręcznie pilnować priorytetów, testy ekspandują wykładniczo.
2. **`sealed class/interface UiState`:** dyskretne, wzajemnie wykluczające się
   stany (Idle/Loading/Success/Empty/Error). Exhaustive `when` w UI gwarantuje
   pokrycie. Cena: tworzenie nowego stanu wymaga zmiany typu i wszystkich
   konsumentów.

Po stronie wymiany danych z ViewModelem mamy `LiveData` vs `StateFlow` vs
`SharedFlow` — `LiveData` jest legacy w nowych projektach Compose, `SharedFlow`
nie ma początkowej wartości (UI musi rysować coś przed pierwszą emisją).

## Decyzja

**Każdy ViewModel eksponuje pojedyncze `StateFlow<UiState>` jako `val state`.**
Typ `UiState` dobieramy do kształtu ekranu:

- **`sealed interface`** dla ekranów z dyskretnymi stanami i wykluczającymi się
  ścieżkami renderowania:
  - `SearchUiState`: `Idle | Loading | Success | Empty | Error`.
  - `ChartUiState`: `Loading | Success | Error` (wspólne pola `symbol`, `range`,
    `chartType`, `isFavorite` w nadtypie — żeby TopBar mógł je czytać bez
    rozpakowywania wariantu).
- **`data class`** dla ekranów reaktywnych z jednym strumieniem prawdy, gdzie
  pusta lista to legalny "Success" a nie błąd:
  - `FavoritesUiState(items: List<Stock>, loading: Boolean)`. Source-of-truth =
    `repository.observeFavorites()` z Room. Sztuczne wymuszanie stanów
    `Empty`/`Success` byłoby ceremonią — pusta lista renderuje natywny empty
    state.

Każdy ViewModel używa `@HiltViewModel`, `viewModelScope`, oraz albo:
- `MutableStateFlow(...)` + `_state.value = ...` dla imperatywnych aktualizacji
  (Search, Chart), albo
- `flow.stateIn(viewModelScope, WhileSubscribed(5_000), initialValue)` dla
  ekranów reaktywnych (Favorites).

`WhileSubscribed(5_000)` — Flow utrzymuje się 5 s po znikającym ekranie
(rotacja, krótka nawigacja w bok), żeby nie restartować subskrypcji Room.

## Konsekwencje

- UI używa `when (state) { is X -> ... }` — kompilator wymusza obsługę nowych
  wariantów; brak ścieżki "co jeśli ktoś dodał nowy stan a UI o nim nie wie".
- Testy ViewModela sprowadzają się do "wstrzykuj fake repo → wywołaj akcję →
  `advanceUntilIdle()` → assertEquals na `state.value`". Stałe 6–8 scenariuszy
  per VM. **Nie testujemy stanu `Loading` osobno** — `StateFlow` konfluuje
  szybkie emisje gdy mock zwraca synchronicznie (Unconfined + już-rozwiązany
  `Result`), więc Loading jest zjadane przez Success. Testowanie tranzytu byłoby
  brittle; sprawdzamy stany **końcowe**.
- Współdzielone pola w nadtypie sealed (`ChartUiState.symbol`) są tańsze niż
  rozpakowywanie wariantów w UI dla rzeczy które nie zależą od stanu — i tańsze
  niż osobna `TopBarState` obok `BodyState`.
- Brak `LiveData`. Brak `Flow` poza ViewModelem (UI dostaje gotowy `StateFlow`,
  konsumuje przez `collectAsStateWithLifecycle()` z `androidx.lifecycle.runtime`).
- Konwencja: każdy ekran ma trzy pliki — `*UiState.kt`, `*ViewModel.kt`,
  `*Screen.kt`. Plus opcjonalny `*Content.kt` jeśli rozdzielamy stateful od
  stateless (Chart: tak — żeby Compose UI test mógł renderować bez Hilta).

## Odrzucone alternatywy

**„Tłusta" data class wszędzie.** Dopuszcza nielegalne stany; UI musi
przyporządkowywać priorytety. Łatwa pułapka: zapomniany reset `error = null`
przy nowym sukcesie i komunikat błędu pozostaje.

**`MutableState<UiState>` w ViewModelu (Compose State).** Wycieka detal
implementacji Compose poza warstwę prezentacji, blokuje test ViewModela bez
zależności od `androidx.compose.*`. `StateFlow` jest neutralny.

**Jeden `UiState<T>` z genericiem.** Brzmi DRY na papierze, w praktyce dodaje
warstwę bez wartości — każdy ekran ma inne akcje (search debounce, range
change, toggle favorite). Wspólny supertype kupuje nic, kosztuje czytelność.

**`SharedFlow<UiEvent>` jako kanał side-effectów (snackbar, nawigacja).** W MVP
nie jest potrzebne — nawigację robi sam UI przez przekazany callback (`onStockClick`),
snackbary nie istnieją. Wprowadzimy gdy zajdzie potrzeba (np. Faza 5 — błąd
Gemini).
