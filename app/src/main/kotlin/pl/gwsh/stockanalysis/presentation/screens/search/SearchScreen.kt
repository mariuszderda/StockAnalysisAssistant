package pl.gwsh.stockanalysis.presentation.screens.search

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.model.Stock
import pl.gwsh.stockanalysis.presentation.common.toUserMessage

object SearchScreenTags {
    const val FIELD = "search_field"
    const val LIST = "search_results_list"
    const val EMPTY = "search_empty"
    const val IDLE = "search_idle"
    const val LOADING = "search_loading"
    const val ERROR = "search_error"
}

@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.currentQuery.collectAsStateWithLifecycle()

    SearchContent(
        query = query,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clear,
        onStockClick = onStockClick,
    )
}

@Composable
fun SearchContent(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onStockClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SearchScreenTags.FIELD),
            label = { Text(stringResource(R.string.search_field_label)) },
            placeholder = { Text(stringResource(R.string.search_field_placeholder)) },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.search_clear_cd),
                        )
                    }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        )

        when (state) {
            SearchUiState.Idle -> CenterText(
                text = stringResource(R.string.search_idle_hint),
                tag = SearchScreenTags.IDLE,
            )
            SearchUiState.Loading -> CenterLoading(
                text = stringResource(R.string.search_loading),
                tag = SearchScreenTags.LOADING,
            )
            is SearchUiState.Empty -> CenterText(
                text = stringResource(R.string.search_empty, state.query),
                tag = SearchScreenTags.EMPTY,
            )
            is SearchUiState.Error -> CenterText(
                text = state.error.toUserMessage(),
                tag = SearchScreenTags.ERROR,
            )
            is SearchUiState.Success -> SearchResultsList(
                items = state.results,
                onClick = onStockClick,
            )
        }
    }
}

@Composable
private fun SearchResultsList(items: List<Stock>, onClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchScreenTags.LIST),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items, key = { "${it.symbol}|${it.micCode}" }) { stock ->
            StockRow(stock = stock, onClick = { onClick(stock.symbol) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun StockRow(stock: Stock, onClick: () -> Unit) {
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
private fun CenterText(text: String, tag: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun CenterLoading(text: String, tag: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text(text = text, modifier = Modifier.testTag(tag))
        }
    }
}
