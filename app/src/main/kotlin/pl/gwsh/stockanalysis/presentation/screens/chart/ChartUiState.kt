package pl.gwsh.stockanalysis.presentation.screens.chart

import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.Range

/**
 * Stany ekranu wykresu. Sealed — exhaustive when w UI gwarantuje obsluge
 * wszystkich gałęzi.
 *
 * Pola wspolne (`symbol`, `range`, `chartType`, `isFavorite`, `activeSpecs`)
 * trzymamy w nadtypie, zeby TopBar / IndicatorPanel mogly je czytac bez
 * rozpakowywania wariantu. `activeSpecs` musi przezyc zmiane zakresu i
 * przeladowanie danych — uzytkownik wlaczyl RSI, oczekuje ze RSI nadal
 * bedzie wlaczone po zmianie z 1M na 3M.
 */
sealed interface ChartUiState {
    val symbol: String
    val range: Range
    val chartType: ChartType
    val isFavorite: Boolean
    val activeSpecs: Set<IndicatorSpec>

    data class Loading(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
        override val activeSpecs: Set<IndicatorSpec> = emptySet(),
    ) : ChartUiState

    data class Success(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
        override val activeSpecs: Set<IndicatorSpec> = emptySet(),
        val candles: List<Candle>,
        val indicators: List<IndicatorResult> = emptyList(),
    ) : ChartUiState

    data class Error(
        override val symbol: String,
        override val range: Range,
        override val chartType: ChartType,
        override val isFavorite: Boolean,
        override val activeSpecs: Set<IndicatorSpec> = emptySet(),
        val error: DataError,
    ) : ChartUiState
}
