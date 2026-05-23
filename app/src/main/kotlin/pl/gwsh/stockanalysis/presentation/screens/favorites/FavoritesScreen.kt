package pl.gwsh.stockanalysis.presentation.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.model.Stock

object FavoritesScreenTags {
    const val LIST = "favorites_list"
    const val EMPTY = "favorites_empty"
    const val LOADING = "favorites_loading"
}

@Composable
fun FavoritesScreen(
    onStockClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoritesContent(state = state, onStockClick = onStockClick)
}

@Composable
fun FavoritesContent(
    state: FavoritesUiState,
    onStockClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        when {
            state.loading -> Center {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text(text = stringResource(R.string.favorites_loading), modifier = Modifier.testTag(FavoritesScreenTags.LOADING))
                }
            }
            state.items.isEmpty() -> Center {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(FavoritesScreenTags.EMPTY),
                )
            }
            else -> FavoritesList(state.items, onStockClick)
        }
    }
}

@Composable
private fun FavoritesList(items: List<Stock>, onClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(FavoritesScreenTags.LIST),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items, key = { it.symbol }) { stock ->
            FavoriteRow(stock, onClick = { onClick(stock.symbol) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun FavoriteRow(stock: Stock, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Text(stock.symbol, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${stock.name} · ${stock.exchange}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { content() }
}
