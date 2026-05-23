package pl.gwsh.stockanalysis.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.gwsh.stockanalysis.R
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
