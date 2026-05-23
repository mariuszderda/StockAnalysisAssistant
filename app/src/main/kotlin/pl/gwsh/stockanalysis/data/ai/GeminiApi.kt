package pl.gwsh.stockanalysis.data.ai

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit fasada na REST endpoint Gemini Developer API.
 * Model przekazywany jako parametr sciezki — domyslnie `gemini-1.5-flash`
 * (CLAUDE.md § Gemini boundaries).
 *
 * Klucz API trafia do query stringu zgodnie z dokumentacja;
 * `HttpLoggingInterceptor` ma `redactQueryParameter("key")` w
 * NetworkModule, zeby Logcat go nie ujawnial.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequestDto,
    ): GeminiResponseDto
}
