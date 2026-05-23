package pl.gwsh.stockanalysis.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.ai.GeminiError
import pl.gwsh.stockanalysis.domain.model.DataError

/** Mapuje [DataError] na lokalizowany komunikat z `strings.xml`. */
@Composable
fun DataError.toUserMessage(): String = when (this) {
    DataError.Network -> stringResource(R.string.error_network)
    DataError.RateLimited -> stringResource(R.string.error_rate_limit)
    DataError.NotFound -> stringResource(R.string.error_not_found)
    is DataError.Server -> stringResource(R.string.error_server, code)
    is DataError.ParseError -> stringResource(R.string.error_parse)
    is DataError.Unknown -> stringResource(R.string.error_unknown)
}

/** Mapuje [GeminiError] na lokalizowany komunikat z `strings.xml`. */
@Composable
fun GeminiError.toUserMessage(): String = when (this) {
    GeminiError.Network -> stringResource(R.string.chat_error_network)
    GeminiError.MissingApiKey -> stringResource(R.string.chat_error_missing_key)
    GeminiError.Blocked -> stringResource(R.string.chat_error_blocked)
    is GeminiError.Server -> when (code) {
        429 -> stringResource(R.string.chat_error_rate_limit)
        else -> stringResource(R.string.chat_error_server, code)
    }
    is GeminiError.Unknown -> stringResource(R.string.chat_error_unknown)
}
