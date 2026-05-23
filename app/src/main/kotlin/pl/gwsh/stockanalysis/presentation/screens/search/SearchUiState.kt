package pl.gwsh.stockanalysis.presentation.screens.search

import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.Stock

/**
 * Dyskretne stany ekranu wyszukiwania. Trzymamy je jako sealed class — UI
 * konsumuje exhaustive when'em (patrz ADR-003).
 */
sealed interface SearchUiState {
    /** Pole zapytania puste lub zbyt krotkie — pokazujemy hint. */
    data object Idle : SearchUiState

    /** Zapytanie w locie. */
    data object Loading : SearchUiState

    /** Sukces; lista moze byc niepusta. */
    data class Success(val query: String, val results: List<Stock>) : SearchUiState

    /** Zapytanie powiodlo sie, ale dostawca nie zwrocil dopasowan. */
    data class Empty(val query: String) : SearchUiState

    /** Blad sieciowy lub serwera; szczegoly w [error]. */
    data class Error(val error: DataError) : SearchUiState
}
