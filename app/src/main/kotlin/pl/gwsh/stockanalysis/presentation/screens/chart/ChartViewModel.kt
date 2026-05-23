package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.navigation.SaaDestinations
import javax.inject.Inject

/**
 * ViewModel ekranu wykresu. Symbol pobieramy z [SavedStateHandle] (argument trasy).
 * Reaguje na zmiane zakresu (1M/3M/1R), typu wykresu i toggle ulubionych.
 */
@HiltViewModel
class ChartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StockRepository,
) : ViewModel() {

    val symbol: String = requireNotNull(savedStateHandle[SaaDestinations.CHART_ARG_SYMBOL]) {
        "Brak argumentu '${SaaDestinations.CHART_ARG_SYMBOL}' w SavedStateHandle"
    }

    private val _state = MutableStateFlow<ChartUiState>(
        ChartUiState.Loading(
            symbol = symbol,
            range = Range.ONE_MONTH,
            chartType = ChartType.CANDLE,
            isFavorite = false,
        ),
    )
    val state: StateFlow<ChartUiState> = _state.asStateFlow()

    init {
        load(range = Range.ONE_MONTH, forceRefresh = false)
        refreshFavoriteFlag()
    }

    fun onRangeChange(range: Range) {
        if (range == _state.value.range) return
        load(range = range, forceRefresh = false)
    }

    fun onChartTypeChange(type: ChartType) {
        _state.update { current ->
            when (current) {
                is ChartUiState.Loading -> current.copy(chartType = type)
                is ChartUiState.Success -> current.copy(chartType = type)
                is ChartUiState.Error -> current.copy(chartType = type)
            }
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(symbol)
            refreshFavoriteFlag()
        }
    }

    fun onRefresh() {
        load(range = _state.value.range, forceRefresh = true)
    }

    private fun load(range: Range, forceRefresh: Boolean) {
        val current = _state.value
        _state.value = ChartUiState.Loading(
            symbol = symbol,
            range = range,
            chartType = current.chartType,
            isFavorite = current.isFavorite,
        )
        viewModelScope.launch {
            val result = repository.getCandles(symbol, range, forceRefresh)
            _state.value = result.fold(
                onSuccess = { candles ->
                    ChartUiState.Success(
                        symbol = symbol,
                        range = range,
                        chartType = current.chartType,
                        isFavorite = current.isFavorite,
                        candles = candles,
                    )
                },
                onFailure = { t ->
                    val err = (t as? DataErrorException)?.error ?: DataError.Unknown(t)
                    ChartUiState.Error(
                        symbol = symbol,
                        range = range,
                        chartType = current.chartType,
                        isFavorite = current.isFavorite,
                        error = err,
                    )
                },
            )
        }
    }

    private fun refreshFavoriteFlag() {
        viewModelScope.launch {
            val fav = repository.isFavorite(symbol)
            _state.update { current ->
                when (current) {
                    is ChartUiState.Loading -> current.copy(isFavorite = fav)
                    is ChartUiState.Success -> current.copy(isFavorite = fav)
                    is ChartUiState.Error -> current.copy(isFavorite = fav)
                }
            }
        }
    }
}
