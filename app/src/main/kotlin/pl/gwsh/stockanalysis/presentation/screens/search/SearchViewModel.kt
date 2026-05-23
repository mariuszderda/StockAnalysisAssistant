package pl.gwsh.stockanalysis.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import javax.inject.Inject

/**
 * ViewModel ekranu wyszukiwania. Debounce 350 ms na pole zapytania,
 * pomija puste/krotsze niz 1 znak (=> [SearchUiState.Idle]).
 */
@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val currentQuery: StateFlow<String> = query.asStateFlow()

    init {
        query
            .debounce(DEBOUNCE_MS)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) flowOf(SearchUiState.Idle to q)
                else flowOf(SearchUiState.Loading to q)
            }
            .onEach { (state, q) ->
                _state.value = state
                if (state is SearchUiState.Loading) runSearch(q)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clear() {
        query.value = ""
        _state.value = SearchUiState.Idle
    }

    private suspend fun runSearch(q: String) {
        val result = repository.searchSymbols(q)
        _state.value = result.fold(
            onSuccess = { list ->
                if (list.isEmpty()) SearchUiState.Empty(q)
                else SearchUiState.Success(q, list)
            },
            onFailure = { t ->
                val err = (t as? DataErrorException)?.error ?: DataError.Unknown(t)
                SearchUiState.Error(err)
            },
        )
    }

    companion object {
        const val DEBOUNCE_MS: Long = 350L
    }
}
