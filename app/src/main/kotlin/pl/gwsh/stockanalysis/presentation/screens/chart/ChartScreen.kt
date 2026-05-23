package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.presentation.common.toUserMessage

object ChartScreenTags {
    const val CHART = "chart_canvas"
    const val LOADING = "chart_loading"
    const val ERROR = "chart_error"
    const val FAVORITE_BTN = "chart_favorite_btn"
    const val RANGE_1M = "chart_range_1m"
    const val RANGE_3M = "chart_range_3m"
    const val RANGE_1Y = "chart_range_1y"
    const val TYPE_CANDLE = "chart_type_candle"
    const val TYPE_LINE = "chart_type_line"
}

@Composable
fun ChartScreen(
    onBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChartContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onToggleFavorite = viewModel::onToggleFavorite,
        onRangeChange = viewModel::onRangeChange,
        onTypeChange = viewModel::onChartTypeChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartContent(
    state: ChartUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRangeChange: (Range) -> Unit,
    onTypeChange: (ChartType) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.symbol) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chart_back_cd),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.chart_refresh_cd),
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag(ChartScreenTags.FAVORITE_BTN),
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(
                                if (state.isFavorite) R.string.chart_favorite_remove_cd
                                else R.string.chart_favorite_add_cd,
                            ),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp)) {
            ChartControls(
                range = state.range,
                chartType = state.chartType,
                onRangeChange = onRangeChange,
                onTypeChange = onTypeChange,
            )

            Box(modifier = Modifier.fillMaxWidth().height(320.dp).padding(vertical = 8.dp)) {
                when (val s = state) {
                    is ChartUiState.Loading -> CenterColumn {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.chart_loading), modifier = Modifier.testTag(ChartScreenTags.LOADING))
                    }
                    is ChartUiState.Error -> Text(
                        text = s.error.toUserMessage(),
                        modifier = Modifier.align(Alignment.Center).testTag(ChartScreenTags.ERROR),
                    )
                    is ChartUiState.Success -> VicoChart(
                        candles = s.candles,
                        type = s.chartType,
                        modifier = Modifier.testTag(ChartScreenTags.CHART),
                    )
                }
            }

            IndicatorPanel(modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartControls(
    range: Range,
    chartType: ChartType,
    onRangeChange: (Range) -> Unit,
    onTypeChange: (ChartType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangeChip(
                label = stringResource(R.string.chart_range_1m),
                selected = range == Range.ONE_MONTH,
                onClick = { onRangeChange(Range.ONE_MONTH) },
                tag = ChartScreenTags.RANGE_1M,
            )
            RangeChip(
                label = stringResource(R.string.chart_range_3m),
                selected = range == Range.THREE_MONTHS,
                onClick = { onRangeChange(Range.THREE_MONTHS) },
                tag = ChartScreenTags.RANGE_3M,
            )
            RangeChip(
                label = stringResource(R.string.chart_range_1y),
                selected = range == Range.ONE_YEAR,
                onClick = { onRangeChange(Range.ONE_YEAR) },
                tag = ChartScreenTags.RANGE_1Y,
            )
        }

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                selected = chartType == ChartType.CANDLE,
                onClick = { onTypeChange(ChartType.CANDLE) },
                modifier = Modifier.testTag(ChartScreenTags.TYPE_CANDLE),
                icon = { Icon(Icons.Filled.CandlestickChart, contentDescription = null) },
                label = { Text(stringResource(R.string.chart_type_candle)) },
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                selected = chartType == ChartType.LINE,
                onClick = { onTypeChange(ChartType.LINE) },
                modifier = Modifier.testTag(ChartScreenTags.TYPE_LINE),
                icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                label = { Text(stringResource(R.string.chart_type_line)) },
            )
        }
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun BoxScope.CenterColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private typealias BoxScope = androidx.compose.foundation.layout.BoxScope
