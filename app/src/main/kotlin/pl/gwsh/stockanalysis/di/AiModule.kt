package pl.gwsh.stockanalysis.di

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import pl.gwsh.stockanalysis.data.ai.GeminiApi
import pl.gwsh.stockanalysis.data.ai.GeminiClientImpl
import pl.gwsh.stockanalysis.domain.ai.GeminiClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Wiring dla warstwy AI. Decyzja architektoniczna — patrz
 * `docs/ADR/005-gemini-integration.md`: nie uzywamy deprecated
 * `com.google.ai.client.generativeai`, tylko REST endpoint przez Retrofit.
 *
 * Dziel sie OkHttpClient + Moshi z NetworkModule (singletony Hilt),
 * dodaje osobny Retrofit z innym `baseUrl`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideGeminiApi(@GeminiRetrofit retrofit: Retrofit): GeminiApi =
        retrofit.create(GeminiApi::class.java)
}

/**
 * `@Binds` wymaga abstract module — trzymamy osobno, zeby AiModule
 * pozostalo `object` z `@Provides`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindingsModule {

    @Binds
    @Singleton
    abstract fun bindGeminiClient(impl: GeminiClientImpl): GeminiClient
}
