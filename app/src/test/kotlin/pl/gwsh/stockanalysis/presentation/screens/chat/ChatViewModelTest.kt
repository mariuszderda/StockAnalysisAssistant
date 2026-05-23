package pl.gwsh.stockanalysis.presentation.screens.chat

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import pl.gwsh.stockanalysis.domain.ai.GeminiClient
import pl.gwsh.stockanalysis.domain.ai.GeminiError
import pl.gwsh.stockanalysis.domain.ai.GeminiException
import pl.gwsh.stockanalysis.domain.indicator.IndicatorFactory
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.common.MainDispatcherExtension
import pl.gwsh.stockanalysis.presentation.navigation.SaaDestinations
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repo: StockRepository = mockk()
    private val gemini: GeminiClient = mockk()
    private val factory = IndicatorFactory()
    private val dispatcher = UnconfinedTestDispatcher()

    private fun handle(symbol: String = "AAPL"): SavedStateHandle =
        SavedStateHandle(mapOf(SaaDestinations.CHAT_ARG_SYMBOL to symbol))

    private fun vm(symbol: String = "AAPL"): ChatViewModel =
        ChatViewModel(handle(symbol), repo, factory, gemini, dispatcher)

    private fun candles(n: Int): List<Candle> = (1..n).map { i ->
        Candle(
            date = LocalDate.of(2026, 4, 1).plusDays(i.toLong()),
            open = 100.0 + i, high = 110.0 + i, low = 99.0 + i, close = 100.0 + i, volume = 1_000L,
        )
    }

    @Test
    fun `init loads candles and computes indicators then becomes Ready`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns Result.success(candles(40))

        val vm = vm()
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ChatUiState.Ready::class.java)
        val ready = s as ChatUiState.Ready
        assertThat(ready.symbol).isEqualTo("AAPL")
        assertThat(ready.messages).isEmpty()
        assertThat(ready.isSending).isFalse()
        coVerify { repo.getCandles("AAPL", Range.THREE_MONTHS, false) }
    }

    @Test
    fun `init failure with DataError yields ContextError`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns
            Result.failure(DataErrorException(DataError.Network))

        val vm = vm()
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ChatUiState.ContextError::class.java)
        assertThat((s as ChatUiState.ContextError).error).isEqualTo(DataError.Network)
    }

    @Test
    fun `onSend appends user then assistant message in order on success`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns Result.success(candles(40))
        coEvery { gemini.ask(any(), "Co mowi RSI?") } returns Result.success("RSI(14) = 62.4 — neutralnie.")

        val vm = vm()
        advanceUntilIdle()
        vm.onSend("Co mowi RSI?")
        advanceUntilIdle()

        val msgs = (vm.state.value as ChatUiState.Ready).messages
        assertThat(msgs).hasSize(2)
        assertThat(msgs[0]).isInstanceOf(ChatMessage.User::class.java)
        assertThat((msgs[0] as ChatMessage.User).text).isEqualTo("Co mowi RSI?")
        assertThat(msgs[1]).isInstanceOf(ChatMessage.Assistant.Text::class.java)
        assertThat((msgs[1] as ChatMessage.Assistant.Text).text).contains("RSI(14) = 62.4")
        assertThat((vm.state.value as ChatUiState.Ready).isSending).isFalse()
    }

    @Test
    fun `onSend with Gemini failure surfaces assistant error message`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns Result.success(candles(40))
        coEvery { gemini.ask(any(), any()) } returns Result.failure(GeminiException(GeminiError.Network))

        val vm = vm()
        advanceUntilIdle()
        vm.onSend("Co mowi RSI?")
        advanceUntilIdle()

        val msgs = (vm.state.value as ChatUiState.Ready).messages
        assertThat(msgs).hasSize(2)
        val err = msgs[1] as ChatMessage.Assistant.Error
        assertThat(err.error).isEqualTo(GeminiError.Network)
    }

    @Test
    fun `onSend with blank text is no-op`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns Result.success(candles(40))

        val vm = vm()
        advanceUntilIdle()
        vm.onSend("   ")
        advanceUntilIdle()

        assertThat((vm.state.value as ChatUiState.Ready).messages).isEmpty()
        coVerify(exactly = 0) { gemini.ask(any(), any()) }
    }

    @Test
    fun `onSend in ContextError state is no-op`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns
            Result.failure(DataErrorException(DataError.Network))

        val vm = vm()
        advanceUntilIdle()
        vm.onSend("Cos")
        advanceUntilIdle()

        assertThat(vm.state.value).isInstanceOf(ChatUiState.ContextError::class.java)
        coVerify(exactly = 0) { gemini.ask(any(), any()) }
    }

    @Test
    fun `onRetryContext re-fetches candles`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returnsMany listOf(
            Result.failure(DataErrorException(DataError.Network)),
            Result.success(candles(40)),
        )

        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(ChatUiState.ContextError::class.java)

        vm.onRetryContext()
        advanceUntilIdle()

        assertThat(vm.state.value).isInstanceOf(ChatUiState.Ready::class.java)
        coVerify(exactly = 2) { repo.getCandles("AAPL", Range.THREE_MONTHS, false) }
    }

    @Test
    fun `Gemini Server 429 surfaces rate-limit message`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.THREE_MONTHS, false) } returns Result.success(candles(40))
        coEvery { gemini.ask(any(), any()) } returns Result.failure(GeminiException(GeminiError.Server(429)))

        val vm = vm()
        advanceUntilIdle()
        vm.onSend("Pytanie")
        advanceUntilIdle()

        val msgs = (vm.state.value as ChatUiState.Ready).messages
        val err = msgs[1] as ChatMessage.Assistant.Error
        assertThat(err.error).isEqualTo(GeminiError.Server(429))
    }
}
