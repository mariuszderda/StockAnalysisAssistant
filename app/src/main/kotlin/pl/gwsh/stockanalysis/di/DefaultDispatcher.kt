package pl.gwsh.stockanalysis.di

import javax.inject.Qualifier

/**
 * Kwalifikator dla `Dispatchers.Default`. Uzywany tam, gdzie zadanie jest
 * CPU-bound (np. obliczenia wskaznikow technicznych), wiec dispatcher I/O
 * (64+ watkow) bylby marnotrawstwem, a glowny watek zablokowalby UI.
 *
 * Testy moga wstrzyknac `UnconfinedTestDispatcher`, zeby `withContext`
 * w obliczeniach nie wymagal `runTest { advanceUntilIdle() }`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
