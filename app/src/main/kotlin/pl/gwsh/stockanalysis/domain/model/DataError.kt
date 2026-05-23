package pl.gwsh.stockanalysis.domain.model

/**
 * Typowane bledy warstwy danych. Repository zwraca je w [Result.failure]
 * opakowane w [DataErrorException], dzieki czemu warstwa prezentacji moze
 * sterowac UI exhaustive when'em na sealed class — bez parsowania komunikatow.
 */
sealed class DataError {

    /** Brak polaczenia / `IOException` na poziomie OkHttp. */
    data object Network : DataError()

    /** HTTP 5xx (badz inny twardy blad serwera). */
    data class Server(val code: Int, val message: String) : DataError()

    /** Limit Twelve Data wyczerpany (HTTP 429 lub HTTP 200 z `code: 429`). */
    data object RateLimited : DataError()

    /** Ticker nie istnieje lub API zwrocilo pusta liste swiec a cache jest pusty. */
    data object NotFound : DataError()

    /** Format odpowiedzi nieoczekiwany (zla data, NaN, brak pol). */
    data class ParseError(val reason: String) : DataError()

    /** Nieznane wyjatki — ostatnia gałąź `catch`, do logowania. */
    data class Unknown(val cause: Throwable) : DataError()
}

/**
 * Cienki wrapper na [Throwable] (wymagany przez [Result.failure]). Warstwa
 * prezentacji odczytuje przez `result.exceptionOrNull() as? DataErrorException`.
 */
class DataErrorException(val error: DataError) : Exception(error.toString())
