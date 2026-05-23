package pl.gwsh.stockanalysis.presentation.screens.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import pl.gwsh.stockanalysis.domain.model.Stock

class SearchScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun stock(symbol: String) = Stock(
        symbol = symbol, name = "$symbol Inc", exchange = "NASDAQ",
        micCode = "XNAS", currency = "USD", type = "Common Stock",
    )

    @Test
    fun idle_state_shows_hint() {
        rule.setContent {
            SearchContent(
                query = "",
                state = SearchUiState.Idle,
                onQueryChange = {},
                onClear = {},
                onStockClick = {},
            )
        }
        rule.onNodeWithTag(SearchScreenTags.IDLE).assertIsDisplayed()
    }

    @Test
    fun success_state_lists_results_and_click_emits_symbol() {
        var clicked: String? = null
        rule.setContent {
            SearchContent(
                query = "aapl",
                state = SearchUiState.Success("aapl", listOf(stock("AAPL"), stock("MSFT"))),
                onQueryChange = {},
                onClear = {},
                onStockClick = { clicked = it },
            )
        }
        rule.onNodeWithText("AAPL").assertIsDisplayed()
        rule.onNodeWithText("MSFT").assertIsDisplayed()
        rule.onNodeWithText("AAPL").performClick()
        assert(clicked == "AAPL") { "expected AAPL, got $clicked" }
    }

    @Test
    fun query_input_calls_callback() {
        val captured = mutableListOf<String>()
        rule.setContent {
            SearchContent(
                query = "",
                state = SearchUiState.Idle,
                onQueryChange = { captured += it },
                onClear = {},
                onStockClick = {},
            )
        }
        rule.onNodeWithTag(SearchScreenTags.FIELD).performTextInput("a")
        assert(captured.contains("a")) { "expected 'a' in callbacks, got $captured" }
    }
}
