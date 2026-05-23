package pl.gwsh.stockanalysis.presentation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.presentation.common.LegalDisclaimerBanner
import pl.gwsh.stockanalysis.presentation.common.toUserMessage as toUserMsg

object ChatScreenTags {
    const val TOP_BAR_TITLE = "chat_top_bar_title"
    const val LOADING = "chat_loading"
    const val ERROR = "chat_error"
    const val INPUT = "chat_input"
    const val SEND_BTN = "chat_send_btn"
    const val MESSAGE_LIST = "chat_message_list"
    const val RETRY_CONTEXT_BTN = "chat_retry_context_btn"
    const val SUGGESTION_PREFIX = "chat_suggestion_"
    const val MESSAGE_PREFIX = "chat_message_"
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatContent(
        state = state,
        onBack = onBack,
        onSend = viewModel::onSend,
        onRetryContext = viewModel::onRetryContext,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    state: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onRetryContext: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_top_bar_title, state.symbol),
                        modifier = Modifier.testTag(ChatScreenTags.TOP_BAR_TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chart_back_cd),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            // Disclaimer ZAWSZE widoczny — CLAUDE.md § Gemini boundaries.
            LegalDisclaimerBanner()

            when (val s = state) {
                is ChatUiState.LoadingContext -> LoadingBox()
                is ChatUiState.ContextError -> ContextErrorBox(s, onRetryContext)
                is ChatUiState.Ready -> ChatBody(state = s, onSend = onSend)
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.chat_loading_context),
                modifier = Modifier.testTag(ChatScreenTags.LOADING),
            )
        }
    }
}

@Composable
private fun ContextErrorBox(state: ChatUiState.ContextError, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.error.toUserMsg(),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ChatScreenTags.ERROR),
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag(ChatScreenTags.RETRY_CONTEXT_BTN),
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ChatBody(state: ChatUiState.Ready, onSend: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll do najnowszej wiadomosci.
    LaunchedEffect(state.messages.size, state.isSending) {
        val target = (state.messages.size + if (state.isSending) 1 else 0) - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(ChatScreenTags.MESSAGE_LIST),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
            if (state.isSending) {
                item(key = "sending") { TypingIndicator() }
            }
        }

        if (state.messages.isEmpty() && !state.isSending) {
            SuggestionChips(onClick = onSend)
        }

        InputRow(
            input = input,
            isSending = state.isSending,
            onInputChange = { input = it },
            onSendClicked = {
                if (input.isNotBlank()) {
                    onSend(input)
                    input = ""
                }
            },
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg is ChatMessage.User
    val isError = msg is ChatMessage.Assistant.Error
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayText: String = when (msg) {
        is ChatMessage.User -> msg.text
        is ChatMessage.Assistant.Text -> msg.text
        is ChatMessage.Assistant.Error -> msg.error.toUserMsg()
    }

    Column(modifier = Modifier.fillMaxWidth().testTag(ChatScreenTags.MESSAGE_PREFIX + msg.id), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(color = bubbleColor, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = displayText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.chat_thinking),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuggestionChips(onClick: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(R.string.chat_suggestion_rsi),
        stringResource(R.string.chat_suggestion_trend),
        stringResource(R.string.chat_suggestion_last_candle),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEachIndexed { index, label ->
            AssistChip(
                onClick = { onClick(label) },
                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.testTag(ChatScreenTags.SUGGESTION_PREFIX + index),
            )
        }
    }
}

@Composable
private fun InputRow(
    input: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    val sendEnabled by remember(input, isSending) {
        derivedStateOf { input.isNotBlank() && !isSending }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
            enabled = !isSending,
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.weight(1f).testTag(ChatScreenTags.INPUT),
        )
        IconButton(
            onClick = onSendClicked,
            enabled = sendEnabled,
            modifier = Modifier.testTag(ChatScreenTags.SEND_BTN),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.chat_send_cd),
                tint = if (sendEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
