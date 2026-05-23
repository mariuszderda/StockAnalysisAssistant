package pl.gwsh.stockanalysis.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.gwsh.stockanalysis.R

/** testTag dla testow UI: disclaimer musi byc widoczny zawsze. */
const val DISCLAIMER_BANNER_TAG = "disclaimer_banner"

/**
 * Pasek z disclaimer'em "to nie jest porada inwestycyjna". CLAUDE.md
 * § Gemini boundaries — widoczny zawsze, niedismissable. Element ekranu
 * czatu, ale wydzielony zeby zaznaczyc niezmiennik.
 */
@Composable
fun LegalDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DISCLAIMER_BANNER_TAG),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.chat_disclaimer_full),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
