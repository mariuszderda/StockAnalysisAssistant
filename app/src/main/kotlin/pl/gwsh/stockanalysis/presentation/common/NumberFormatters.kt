package pl.gwsh.stockanalysis.presentation.common

import java.text.NumberFormat
import java.util.Locale

private val plLocale = Locale.forLanguageTag("pl-PL")

/**
 * Formatuje cene do `X,YZ` z dwoma miejscami po przecinku, bez waluty.
 * Walute pokazujemy obok osobnym labelem (zaleznie od ekranu), zeby
 * uniknac problemow z mieszanymi instrumentami USD / PLN / EUR w jednym
 * widoku ulubionych.
 */
fun formatPrice(value: Double): String =
    NumberFormat.getNumberInstance(plLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

/**
 * Formatuje wolumen w skroconej notacji "K / M / B" — wolumen akcji
 * potrafi byc rzedu setek milionow, surowa liczba w UI to scianka tekstu.
 */
fun formatVolume(volume: Long): String = when {
    volume >= 1_000_000_000L -> "%.2fB".format(plLocale, volume / 1_000_000_000.0)
    volume >= 1_000_000L -> "%.2fM".format(plLocale, volume / 1_000_000.0)
    volume >= 1_000L -> "%.1fK".format(plLocale, volume / 1_000.0)
    else -> volume.toString()
}
