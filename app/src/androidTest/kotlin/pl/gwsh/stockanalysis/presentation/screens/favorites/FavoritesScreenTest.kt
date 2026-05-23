package pl.gwsh.stockanalysis.presentation.screens.favorites

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.gwsh.stockanalysis.domain.model.Stock

class FavoritesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun stock(symbol: String) = Stock(
        symbol = symbol, name = "$symbol Inc", exchange = "NASDAQ",
        micCode = "XNAS", currency = "USD", type = "Common Stock",
    )

    @Test
    fun empty_state_shows_hint() {
        rule.setContent {
            FavoritesContent(
                state = FavoritesUiState(items = emptyList(), loading = false),
                onStockClick = {},
            )
        }
        rule.onNodeWithTag(FavoritesScreenTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun loading_state_shows_progress() {
        rule.setContent {
            FavoritesContent(
                state = FavoritesUiState(items = emptyList(), loading = true),
                onStockClick = {},
            )
        }
        rule.onNodeWithTag(FavoritesScreenTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun populated_state_lists_items_and_click_emits_symbol() {
        var clicked: String? = null
        rule.setContent {
            FavoritesContent(
                state = FavoritesUiState(items = listOf(stock("CDR.WA")), loading = false),
                onStockClick = { clicked = it },
            )
        }
        rule.onNodeWithText("CDR.WA").assertIsDisplayed()
        rule.onNodeWithText("CDR.WA").performClick()
        assert(clicked == "CDR.WA") { "expected CDR.WA, got $clicked" }
    }
}
