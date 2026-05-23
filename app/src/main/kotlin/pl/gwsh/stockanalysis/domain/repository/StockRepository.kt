package pl.gwsh.stockanalysis.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.model.Stock

/**
 * Jedyna granica miedzy warstwa domeny/prezentacji a danymi (sieć + cache).
 *
 * Repository Pattern — patrz `docs/ADR/001-repository-pattern.md`. ViewModel
 * dostaje ten interfejs przez Hilt i nigdy nie widzi Retrofitu ani Room.
 *
 * Wszystkie `suspend` zwracaja `Result<T>` — w `failure` siedzi
 * [pl.gwsh.stockanalysis.domain.model.DataErrorException] z typowanym
 * [pl.gwsh.stockanalysis.domain.model.DataError]. To pozwala warstwie UI
 * sterowac komunikatami exhaustive when'em na sealed class.
 */
interface StockRepository {

    /** Wyszukuje tickery po fragmencie nazwy/symbolu. */
    suspend fun searchSymbols(query: String): Result<List<Stock>>

    /**
     * Pobiera swiece OHLC dla tickera w danym zakresie.
     *
     * Strategia cache-first z TTL 24h:
     *  - cache wystarczajacy i swiezy → bez zapytania do API;
     *  - cache stale / niepelny → fetch z API, refresh cache;
     *  - `forceRefresh = true` pomija cache;
     *  - brak sieci a cache niepusty → graceful fallback do cache (success);
     *  - brak sieci a cache pusty → failure(Network).
     */
    suspend fun getCandles(
        symbol: String,
        range: Range,
        forceRefresh: Boolean = false,
    ): Result<List<Candle>>

    /** Strumien ulubionych — Room obserwuje zmiany tabeli `favorites`. */
    fun observeFavorites(): Flow<List<Stock>>

    /** Przelacza obecnosc tickera w ulubionych (insert/delete). */
    suspend fun toggleFavorite(symbol: String)

    /** Sprawdza, czy ticker jest w ulubionych. */
    suspend fun isFavorite(symbol: String): Boolean
}
