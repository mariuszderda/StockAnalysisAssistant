package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec
import pl.gwsh.stockanalysis.domain.indicator.IndicatorType
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.presentation.common.toUserMessage

object ChartScreenTags {
    const val CHART = "chart_canvas"
    const val LOADING = "chart_loading"
    const val ERROR = "chart_error"
    const val FAVORITE_BTN = "chart_favorite_btn"
    const val INDICATORS_BTN = "chart_indicators_btn"
    const val ASK_ASSISTANT_FAB = "chart_ask_assistant_fab"
    const val RANGE_1M = "chart_range_1m"
    const val RANGE_3M = "chart_range_3m"
    const val RANGE_1Y = "chart_range_1y"
    const val TYPE_CANDLE = "chart_type_candle"
    const val TYPE_LINE = "chart_type_line"
}

@Composable
fun ChartScreen(
    onBack: () -> Unit,
    onAskAssistant: (String) -> Unit,
    viewModel: ChartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChartContent(
        state = state,
        availableIndicators = viewModel.availableIndicators,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onToggleFavorite = viewModel::onToggleFavorite,
        onRangeChange = viewModel::onRangeChange,
        onTypeChange = viewModel::onChartTypeChange,
        onToggleIndicator = viewModel::onToggleIndicator,
        onAskAssistant = { onAskAssistant(viewModel.symbol) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartContent(
    state: ChartUiState,
    availableIndicators: List<IndicatorSpec>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRangeChange: (Range) -> Unit,
    onTypeChange: (ChartType) -> Unit,
    onToggleIndicator: (IndicatorSpec) -> Unit,
    onAskAssistant: () -> Unit = {},
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
                    IconButton(
                        onClick = { sheetOpen = true },
                        modifier = Modifier.testTag(ChartScreenTags.INDICATORS_BTN),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = stringResource(R.string.chart_indicators_open_cd),
                        )
                    }
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
        floatingActionButton = {
            if (state is ChartUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = onAskAssistant,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.chart_ask_assistant)) },
                    modifier = Modifier.testTag(ChartScreenTags.ASK_ASSISTANT_FAB),
                )
            }
        },
    ) { inner ->
        val overlays = (state as? ChartUiState.Success)
            ?.indicators
            ?.filter { it.type == IndicatorType.OVERLAY }
            ?.filterIsInstance<IndicatorResult.SingleLine>()
            .orEmpty()

        val oscillators = (state as? ChartUiState.Success)
            ?.indicators
            ?.filter { it.type == IndicatorType.OSCILLATOR }
            .orEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
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
                        overlays = overlays,
                        modifier = Modifier.testTag(ChartScreenTags.CHART),
                    )
                }
            }

            IndicatorLegend(overlays = overlays)

            oscillators.forEach { osc ->
                OscillatorPanel(result = osc)
            }
        }
    }

    if (sheetOpen) {
        IndicatorPanel(
            available = availableIndicators,
            activeSpecs = state.activeSpecs,
            sheetState = sheetState,
            onDismiss = { sheetOpen = false },
            onToggle = { spec ->
                onToggleIndicator(spec)
            },
        )
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
