package pl.gwsh.stockanalysis.di

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.gwsh.stockanalysis.BuildConfig
import pl.gwsh.stockanalysis.data.remote.TwelveDataApi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Wystawia: Moshi, OkHttpClient (z BODY-logging tylko w debug), Retrofit i
 * [TwelveDataApi]. Wszystko singletonowe — Retrofit cieszy sie connection pooling.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TWELVE_DATA_BASE_URL = "https://api.twelvedata.com/"
    private const val HTTP_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(TWELVE_DATA_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideTwelveDataApi(retrofit: Retrofit): TwelveDataApi =
        retrofit.create(TwelveDataApi::class.java)
}
