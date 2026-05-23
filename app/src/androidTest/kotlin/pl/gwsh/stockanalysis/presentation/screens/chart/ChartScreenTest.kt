package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.Range
import java.time.LocalDate

class ChartScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun candle(day: Int) = Candle(
        date = LocalDate.of(2026, 4, day),
        open = 100.0, high = 105.0, low = 99.0, close = 104.0, volume = 1_000L,
    )

    private val available = listOf(
        IndicatorSpec.Sma(20),
        IndicatorSpec.Ema(20),
        IndicatorSpec.Rsi(14),
        IndicatorSpec.Macd(12, 26, 9),
    )

    @Test
    fun loading_state_shows_progress() {
        rule.setContent {
            ChartContent(
                state = ChartUiState.Loading("AAPL", Range.ONE_MONTH, ChartType.CANDLE, false),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = {},
                onRangeChange = {}, onTypeChange = {}, onToggleIndicator = {},
            )
        }
        rule.onNodeWithText("AAPL").assertIsDisplayed()
        rule.onNodeWithTag(ChartScreenTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun success_state_renders_chart_canvas() {
        rule.setContent {
            ChartContent(
                state = ChartUiState.Success(
                    symbol = "AAPL",
                    range = Range.ONE_MONTH,
                    chartType = ChartType.CANDLE,
                    isFavorite = true,
                    candles = (1..5).map(::candle),
                ),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = {},
                onRangeChange = {}, onTypeChange = {}, onToggleIndicator = {},
            )
        }
        rule.onNodeWithTag(ChartScreenTags.CHART).assertIsDisplayed()
    }

    @Test
    fun error_state_shows_error_message() {
        rule.setContent {
            ChartContent(
                state = ChartUiState.Error("AAPL", Range.ONE_MONTH, ChartType.CANDLE, false, error = DataError.Network),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = {},
                onRangeChange = {}, onTypeChange = {}, onToggleIndicator = {},
            )
        }
        rule.onNodeWithTag(ChartScreenTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun favorite_button_click_invokes_callback() {
        var toggled = 0
        rule.setContent {
            ChartContent(
                state = ChartUiState.Success("AAPL", Range.ONE_MONTH, ChartType.CANDLE, false, candles = listOf(candle(1))),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = { toggled++ },
                onRangeChange = {}, onTypeChange = {}, onToggleIndicator = {},
            )
        }
        rule.onNodeWithTag(ChartScreenTags.FAVORITE_BTN).performClick()
        assert(toggled == 1) { "expected 1 toggle call, got $toggled" }
    }

    @Test
    fun range_chip_click_invokes_callback_with_new_range() {
        var picked: Range? = null
        rule.setContent {
            ChartContent(
                state = ChartUiState.Success("AAPL", Range.ONE_MONTH, ChartType.CANDLE, false, candles = listOf(candle(1))),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = {},
                onRangeChange = { picked = it }, onTypeChange = {}, onToggleIndicator = {},
            )
        }
        rule.onNodeWithTag(ChartScreenTags.RANGE_1Y).performClick()
        assert(picked == Range.ONE_YEAR) { "expected ONE_YEAR, got $picked" }
    }

    @Test
    fun indicators_button_opens_panel_and_chip_click_invokes_toggle() {
        var toggled: IndicatorSpec? = null
        rule.setContent {
            ChartContent(
                state = ChartUiState.Success("AAPL", Range.ONE_MONTH, ChartType.CANDLE, false, candles = listOf(candle(1))),
                availableIndicators = available,
                onBack = {}, onRefresh = {}, onToggleFavorite = {},
                onRangeChange = {}, onTypeChange = {},
                onToggleIndicator = { toggled = it },
            )
        }
        rule.onNodeWithTag(ChartScreenTags.INDICATORS_BTN).performClick()
        rule.onNodeWithTag(INDICATOR_PANEL_SHEET_TAG).assertIsDisplayed()
        rule.onNodeWithTag(INDICATOR_CHIP_TAG_PREFIX + "RSI:14").performClick()
        assert(toggled?.key == "RSI:14") { "expected RSI:14 toggle, got ${toggled?.key}" }
    }
}
