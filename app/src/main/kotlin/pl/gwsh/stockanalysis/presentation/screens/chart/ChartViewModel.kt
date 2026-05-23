package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.gwsh.stockanalysis.di.DefaultDispatcher
import pl.gwsh.stockanalysis.domain.indicator.IndicatorFactory
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.navigation.SaaDestinations
import javax.inject.Inject

/**
 * ViewModel ekranu wykresu. Symbol pobieramy z [SavedStateHandle] (argument trasy).
 * Reaguje na zmiane zakresu (1M/3M/1R), typu wykresu, toggle ulubionych
 * i toggle wskaznikow.
 *
 * Wskazniki — patrz [pl.gwsh.stockanalysis.domain.indicator.IndicatorFactory].
 * Sprawdzamy aktywne specs po kazdym sukcesie pobrania swiec i odsiewamy
 * wynik na [DefaultDispatcher] — algorytmy sa CPU-bound (`Dispatchers.Default`
 * jest pulą rozmiar=#CPU; `IO` rezerwujemy dla blokujacego I/O Retrofit/Room).
 */
@HiltViewModel
class ChartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StockRepository,
    private val factory: IndicatorFactory,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val symbol: String = requireNotNull(savedStateHandle[SaaDestinations.CHART_ARG_SYMBOL]) {
        "Brak argumentu '${SaaDestinations.CHART_ARG_SYMBOL}' w SavedStateHandle"
    }

    /** Lista wskaznikow oferowanych w UI — single source of truth = fabryka. */
    val availableIndicators: List<IndicatorSpec> = factory.availableIndicators()

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

    /**
     * Przelacza wskaznik w/wy. Jezeli stan jest [ChartUiState.Success] —
     * odswieza pole `indicators` przez przeliczenie zestawu.
     */
    fun onToggleIndicator(spec: IndicatorSpec) {
        val current = _state.value
        val newSpecs = current.activeSpecs.toggle(spec)
        when (current) {
            is ChartUiState.Loading -> _state.value = current.copy(activeSpecs = newSpecs)
            is ChartUiState.Error -> _state.value = current.copy(activeSpecs = newSpecs)
            is ChartUiState.Success -> {
                _state.value = current.copy(activeSpecs = newSpecs)
                viewModelScope.launch {
                    val results = recompute(current.candles, newSpecs)
                    // Stan mogl sie zmienic w trakcie (np. zmiana range)
                    _state.update { latest ->
                        if (latest is ChartUiState.Success && latest.symbol == current.symbol &&
                            latest.range == current.range && latest.activeSpecs == newSpecs
                        ) latest.copy(indicators = results) else latest
                    }
                }
            }
        }
    }

    private fun Set<IndicatorSpec>.toggle(spec: IndicatorSpec): Set<IndicatorSpec> =
        if (any { it.key == spec.key }) filterTo(mutableSetOf()) { it.key != spec.key } else this + spec

    private fun load(range: Range, forceRefresh: Boolean) {
        val current = _state.value
        _state.value = ChartUiState.Loading(
            symbol = symbol,
            range = range,
            chartType = current.chartType,
            isFavorite = current.isFavorite,
            activeSpecs = current.activeSpecs,
        )
        viewModelScope.launch {
            val result = repository.getCandles(symbol, range, forceRefresh)
            _state.value = result.fold(
                onSuccess = { candles ->
                    val indicators = recompute(candles, current.activeSpecs)
                    ChartUiState.Success(
                        symbol = symbol,
                        range = range,
                        chartType = current.chartType,
                        isFavorite = current.isFavorite,
                        activeSpecs = current.activeSpecs,
                        candles = candles,
                        indicators = indicators,
                    )
                },
                onFailure = { t ->
                    val err = (t as? DataErrorException)?.error ?: DataError.Unknown(t)
                    ChartUiState.Error(
                        symbol = symbol,
                        range = range,
                        chartType = current.chartType,
                        isFavorite = current.isFavorite,
                        activeSpecs = current.activeSpecs,
                        error = err,
                    )
                },
            )
        }
    }

    /**
     * Liczy wszystkie aktywne wskazniki na podanych swiecach. Zwraca pusty
     * `List` jezeli zestaw pusty. Wszystko na [defaultDispatcher] — operacja
     * CPU-bound (kilkaset iteracji array-walk dla EMA × N).
     */
    suspend fun recompute(
        candles: List<Candle>,
        specs: Set<IndicatorSpec>,
    ): List<IndicatorResult> {
        if (specs.isEmpty() || candles.isEmpty()) return emptyList()
        return withContext(defaultDispatcher) {
            specs.map { factory.create(it).compute(candles) }
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
