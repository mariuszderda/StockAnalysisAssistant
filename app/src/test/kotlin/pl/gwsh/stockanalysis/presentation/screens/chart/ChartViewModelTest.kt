package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import pl.gwsh.stockanalysis.domain.indicator.IndicatorFactory
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec
import pl.gwsh.stockanalysis.domain.indicator.IndicatorType
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.common.MainDispatcherExtension
import pl.gwsh.stockanalysis.presentation.navigation.SaaDestinations
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ChartViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repo: StockRepository = mockk()
    private val factory = IndicatorFactory()
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun handle(symbol: String = "AAPL"): SavedStateHandle =
        SavedStateHandle(mapOf(SaaDestinations.CHART_ARG_SYMBOL to symbol))

    private fun vm(symbol: String = "AAPL"): ChartViewModel =
        ChartViewModel(handle(symbol), repo, factory, testDispatcher)

    private fun candle(day: Int): Candle = Candle(
        date = LocalDate.of(2026, 4, day),
        open = 100.0, high = 105.0, low = 99.0, close = 104.0, volume = 1_000L,
    )

    private fun risingCandles(n: Int): List<Candle> = (1..n).map { i ->
        Candle(LocalDate.of(2026, 4, 1).plusDays(i.toLong()),
            open = 100.0 + i, high = 110.0 + i, low = 99.0 + i, close = 100.0 + i, volume = 1_000L)
    }

    @Test
    fun `init loads ONE_MONTH and produces Success`() = runTest {
        val candles = (1..22).map(::candle)
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(candles)
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ChartUiState.Success::class.java)
        val success = s as ChartUiState.Success
        assertThat(success.symbol).isEqualTo("AAPL")
        assertThat(success.range).isEqualTo(Range.ONE_MONTH)
        assertThat(success.candles).hasSize(22)
        assertThat(success.isFavorite).isFalse()
        assertThat(success.chartType).isEqualTo(ChartType.CANDLE)
        assertThat(success.activeSpecs).isEmpty()
        assertThat(success.indicators).isEmpty()
    }

    @Test
    fun `onRangeChange triggers reload with new range`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.getCandles("AAPL", Range.ONE_YEAR, false) } returns Result.success(listOf(candle(2), candle(3)))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        vm.onRangeChange(Range.ONE_YEAR)
        advanceUntilIdle()

        val s = vm.state.value as ChartUiState.Success
        assertThat(s.range).isEqualTo(Range.ONE_YEAR)
        assertThat(s.candles).hasSize(2)
        coVerify { repo.getCandles("AAPL", Range.ONE_YEAR, false) }
    }

    @Test
    fun `onRangeChange to same range is no-op`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()
        vm.onRangeChange(Range.ONE_MONTH)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.getCandles("AAPL", Range.ONE_MONTH, false) }
    }

    @Test
    fun `onChartTypeChange flips type without reload`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        vm.onChartTypeChange(ChartType.LINE)
        advanceUntilIdle()

        assertThat(vm.state.value.chartType).isEqualTo(ChartType.LINE)
        coVerify(exactly = 1) { repo.getCandles("AAPL", Range.ONE_MONTH, false) }
    }

    @Test
    fun `onToggleFavorite calls repo and refreshes flag`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.isFavorite("AAPL") } returnsMany listOf(false, true)
        coEvery { repo.toggleFavorite("AAPL") } just Runs

        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value.isFavorite).isFalse()

        vm.onToggleFavorite()
        advanceUntilIdle()

        assertThat(vm.state.value.isFavorite).isTrue()
        coVerify { repo.toggleFavorite("AAPL") }
    }

    @Test
    fun `onRefresh forces fetch with forceRefresh true`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, true) } returns Result.success(listOf(candle(1), candle(2)))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        vm.onRefresh()
        advanceUntilIdle()

        coVerify { repo.getCandles("AAPL", Range.ONE_MONTH, true) }
        assertThat((vm.state.value as ChartUiState.Success).candles).hasSize(2)
    }

    @Test
    fun `repo failure becomes Error with DataError`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns
            Result.failure(DataErrorException(DataError.Network))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s).isInstanceOf(ChartUiState.Error::class.java)
        assertThat((s as ChartUiState.Error).error).isEqualTo(DataError.Network)
    }

    @Test
    fun `missing symbol in SavedStateHandle throws`() = runTest {
        val emptyHandle = SavedStateHandle(emptyMap())
        try {
            ChartViewModel(emptyHandle, repo, factory, testDispatcher)
            assert(false) { "expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains(SaaDestinations.CHART_ARG_SYMBOL)
        }
    }

    // --- Indicator toggling -------------------------------------------------

    @Test
    fun `onToggleIndicator adds spec and computes indicator`() = runTest {
        val candles = risingCandles(40)
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(candles)
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        vm.onToggleIndicator(IndicatorSpec.Sma(20))
        advanceUntilIdle()

        val s = vm.state.value as ChartUiState.Success
        assertThat(s.activeSpecs.map { it.key }).containsExactly("SMA:20")
        assertThat(s.indicators).hasSize(1)
        val result = s.indicators.single() as IndicatorResult.SingleLine
        assertThat(result.type).isEqualTo(IndicatorType.OVERLAY)
        assertThat(result.values).hasSize(40)
    }

    @Test
    fun `onToggleIndicator twice removes the spec`() = runTest {
        val candles = risingCandles(40)
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(candles)
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()

        vm.onToggleIndicator(IndicatorSpec.Sma(20))
        advanceUntilIdle()
        vm.onToggleIndicator(IndicatorSpec.Sma(20))
        advanceUntilIdle()

        val s = vm.state.value as ChartUiState.Success
        assertThat(s.activeSpecs).isEmpty()
        assertThat(s.indicators).isEmpty()
    }

    @Test
    fun `active indicators survive range change and get recomputed on new data`() = runTest {
        val first = risingCandles(40)
        val second = risingCandles(60)
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(first)
        coEvery { repo.getCandles("AAPL", Range.ONE_YEAR, false) } returns Result.success(second)
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()
        vm.onToggleIndicator(IndicatorSpec.Ema(20))
        advanceUntilIdle()

        vm.onRangeChange(Range.ONE_YEAR)
        advanceUntilIdle()

        val s = vm.state.value as ChartUiState.Success
        assertThat(s.range).isEqualTo(Range.ONE_YEAR)
        assertThat(s.activeSpecs.map { it.key }).containsExactly("EMA:20")
        assertThat(s.indicators).hasSize(1)
        val ema = s.indicators.single() as IndicatorResult.SingleLine
        assertThat(ema.values).hasSize(60)
    }

    @Test
    fun `onToggleIndicator on Error state updates activeSpecs but skips compute`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns
            Result.failure(DataErrorException(DataError.Network))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(ChartUiState.Error::class.java)

        vm.onToggleIndicator(IndicatorSpec.Rsi(14))
        advanceUntilIdle()

        val s = vm.state.value as ChartUiState.Error
        assertThat(s.activeSpecs.map { it.key }).containsExactly("RSI:14")
    }

    @Test
    fun `availableIndicators exposes factory list`() = runTest {
        coEvery { repo.getCandles("AAPL", Range.ONE_MONTH, false) } returns Result.success(listOf(candle(1)))
        coEvery { repo.isFavorite("AAPL") } returns false

        val vm = vm()
        advanceUntilIdle()
        assertThat(vm.availableIndicators.map { it.key })
            .containsExactly("SMA:20", "EMA:20", "RSI:14", "MACD:12/26/9").inOrder()
    }
}
