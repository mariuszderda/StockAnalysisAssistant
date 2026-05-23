package pl.gwsh.stockanalysis.domain.ai

/**
 * Typowane bledy klienta Gemini. Symetria do
 * [pl.gwsh.stockanalysis.domain.model.DataError] — UI mapuje sealed na
 * lokalizowane komunikaty bez parsowania stringow z exceptionow.
 */
sealed class GeminiError {

    /** Brak polaczenia / `IOException`. */
    data object Network : GeminiError()

    /** Klucz API pusty — local.properties bez wpisanego klucza. */
    data object MissingApiKey : GeminiError()

    /**
     * Odpowiedz zablokowana przez filtr bezpieczenstwa Gemini lub pusta
     * (brak kandydatow, brak tekstu). Dla uzytkownika jeden komunikat.
     */
    data object Blocked : GeminiError()

    /** HTTP non-2xx (najczesciej 429 rate limit lub 5xx). */
    data class Server(val code: Int) : GeminiError()

    /** Nieznane wyjatki — ostatnia galaz, do logu. */
    data class Unknown(val cause: Throwable) : GeminiError()
}

/**
 * Cienki wrapper na [Throwable] dla `Result.failure`. ChatViewModel czyta
 * przez `result.exceptionOrNull() as? GeminiException`.
 */
class GeminiException(val error: GeminiError) : Exception(error.toString())
