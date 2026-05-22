package pl.gwsh.stockanalysis.di

import pl.gwsh.stockanalysis.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jedyne miejsce w kodzie, które czyta klucze API z BuildConfig.
 *
 * Reszta aplikacji bierze klucze przez wstrzykiwany typ `ApiKeys`, dzięki czemu
 * test może wstrzyknąć fake'a bez tykania statycznego pola BuildConfig.X.
 * Konwencja wymuszona w CLAUDE.md § "API keys".
 */
@Singleton
class ApiKeys @Inject constructor() {
    val twelveData: String = BuildConfig.TWELVE_DATA_API_KEY
    val gemini: String = BuildConfig.GEMINI_API_KEY
}
