package pl.gwsh.stockanalysis.presentation.screens.chart

import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.Range

/**
 * Stany ekranu wykresu. Sealed — exhaustive when w UI gwarantuje obsluge
 * wszystkich gałęzi.
 */
sealed interface ChartUiState {
    val symbol: String
    val range: Range
    val chartType: ChartType
    val isFavorite: Boolean

    data class Loading(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
    ) : ChartUiState

    data class Success(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
        val candles: List<Candle>,
    ) : ChartUiState

    data class Error(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
        val error: DataError,
    ) : ChartUiState
}
