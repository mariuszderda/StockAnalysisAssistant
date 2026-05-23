package pl.gwsh.stockanalysis.data.remote

import pl.gwsh.stockanalysis.data.remote.dto.SymbolSearchResponseDto
import pl.gwsh.stockanalysis.data.remote.dto.TimeSeriesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit klient Twelve Data. Zwracamy [Response] (nie czysty body), zeby
 * w repository miec dostep do HTTP code — Twelve Data sygnalizuje rate-limit
 * zarowno HTTP 429 jak i HTTP 200 z `status: "error", code: 429` w body.
 */
interface TwelveDataApi {

    @GET("time_series")
    suspend fun timeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("outputsize") outputSize: Int,
        @Query("apikey") apiKey: String,
    ): Response<TimeSeriesResponseDto>

    @GET("symbol_search")
    suspend fun symbolSearch(
        @Query("symbol") query: String,
        @Query("apikey") apiKey: String,
    ): Response<SymbolSearchResponseDto>
}
