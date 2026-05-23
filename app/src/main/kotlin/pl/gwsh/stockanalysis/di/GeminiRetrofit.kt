package pl.gwsh.stockanalysis.di

import javax.inject.Qualifier

/**
 * Kwalifikator dla Retrofit instancji wskazujacej na
 * `https://generativelanguage.googleapis.com/`. Wspoldzielimy OkHttpClient
 * i Moshi z NetworkModule — rozne base URL'e wymagaja roznych Retrofitow.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit
