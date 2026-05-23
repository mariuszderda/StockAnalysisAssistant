package pl.gwsh.stockanalysis.presentation.screens.chat

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
import pl.gwsh.stockanalysis.domain.ai.AnalysisContext
import pl.gwsh.stockanalysis.domain.ai.GeminiClient
import pl.gwsh.stockanalysis.domain.ai.GeminiError
import pl.gwsh.stockanalysis.domain.ai.GeminiException
import pl.gwsh.stockanalysis.domain.ai.buildSystemInstruction
import pl.gwsh.stockanalysis.domain.indicator.IndicatorFactory
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.navigation.SaaDestinations
import javax.inject.Inject

/**
 * ViewModel ekranu czatu z asystentem AI. Pobiera kontekst (swiece +
 * wskazniki) niezaleznie od [pl.gwsh.stockanalysis.presentation.screens.chart.ChartViewModel]
 * — patrz `docs/ADR/005-gemini-integration.md` § "Reload via repository".
 *
 * Bledy z Gemini surfacuja jako wiadomosc asystenta (uzytkownik moze pytac
 * dalej), bledy kontekstu zmieniaja stan na ContextError (bez kontekstu
 * nie ma sensu pytac).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StockRepository,
    private val factory: IndicatorFactory,
    private val geminiClient: GeminiClient,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val symbol: String = requireNotNull(savedStateHandle[SaaDestinations.CHAT_ARG_SYMBOL]) {
        "Brak argumentu '${SaaDestinations.CHAT_ARG_SYMBOL}' w SavedStateHandle"
    }

    private val _state = MutableStateFlow<ChatUiState>(ChatUiState.LoadingContext(symbol))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var candles: List<Candle> = emptyList()
    private var indicators: List<IndicatorResult> = emptyList()
    private var nextMessageId: Long = 0L

    init {
        loadContext()
    }

    fun onRetryContext() {
        if (_state.value is ChatUiState.LoadingContext) return
        _state.value = ChatUiState.LoadingContext(symbol)
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            val result = repository.getCandles(symbol, Range.THREE_MONTHS, forceRefresh = false)
            result.fold(
                onSuccess = { fetched ->
                    candles = fetched
                    val specs = factory.availableIndicators()
                    indicators = withContext(defaultDispatcher) {
                        specs.map { factory.create(it).compute(fetched) }
                    }
                    _state.value = ChatUiState.Ready(symbol = symbol, messages = emptyList())
                },
                onFailure = { t ->
                    val err = (t as? DataErrorException)?.error ?: DataError.Unknown(t)
                    _state.value = ChatUiState.ContextError(symbol = symbol, error = err)
                },
            )
        }
    }

    fun onSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value as? ChatUiState.Ready ?: return
        if (current.isSending) return

        val userMsg = ChatMessage.User(id = ++nextMessageId, text = trimmed)
        _state.value = current.copy(
            messages = current.messages + userMsg,
            isSending = true,
        )

        viewModelScope.launch {
            val systemInstruction = withContext(defaultDispatcher) {
                buildSystemInstruction(
                    AnalysisContext(
                        symbol = symbol,
                        stock = null,
                        range = Range.THREE_MONTHS,
                        candles = candles,
                        indicators = indicators,
                    ),
                )
            }
            val result = geminiClient.ask(systemInstruction, trimmed)
            val assistantMsg: ChatMessage.Assistant = result.fold(
                onSuccess = { ChatMessage.Assistant.Text(id = ++nextMessageId, text = it) },
                onFailure = { t ->
                    val err = (t as? GeminiException)?.error ?: GeminiError.Unknown(t)
                    ChatMessage.Assistant.Error(id = ++nextMessageId, error = err)
                },
            )
            _state.update { latest ->
                if (latest is ChatUiState.Ready) {
                    latest.copy(messages = latest.messages + assistantMsg, isSending = false)
                } else latest
            }
        }
    }
}
