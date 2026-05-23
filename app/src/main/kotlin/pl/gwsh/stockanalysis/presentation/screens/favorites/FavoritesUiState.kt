package pl.gwsh.stockanalysis.presentation.screens.favorites

import pl.gwsh.stockanalysis.domain.model.Stock

/**
 * Stan ekranu ulubionych. Trzymamy jako data class — zrodlem prawdy jest reaktywny
 * Flow z Room, ktory zawsze zwraca pelna liste (moze byc pusta). Brak dyskretnych
 * stanow "loading vs error vs success" upraszcza UI (patrz ADR-003).
 *
 * `loading` jest `true` tylko przy pierwszym subskrybowaniu, zanim Flow wyemituje
 * pierwsza wartosc.
 */
data class FavoritesUiState(
    val items: List<Stock> = emptyList(),
    val loading: Boolean = true,
)
