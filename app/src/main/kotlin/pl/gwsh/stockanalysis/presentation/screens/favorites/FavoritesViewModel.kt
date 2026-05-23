package pl.gwsh.stockanalysis.presentation.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import javax.inject.Inject

/**
 * ViewModel ekranu ulubionych. Mapuje reaktywny `observeFavorites()` repository
 * na [FavoritesUiState]. `stateIn` z `WhileSubscribed(5_000)` — gdy ekran znika
 * (np. rotacja), subskrypcja sie utrzymuje 5 s, zeby uniknac restartu Flow.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    val state: StateFlow<FavoritesUiState> = repository.observeFavorites()
        .map { FavoritesUiState(items = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = FavoritesUiState(),
        )

    companion object {
        const val STOP_TIMEOUT_MS: Long = 5_000L
    }
}
