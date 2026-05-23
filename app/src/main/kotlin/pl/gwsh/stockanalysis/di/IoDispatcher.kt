package pl.gwsh.stockanalysis.di

import javax.inject.Qualifier

/**
 * Kwalifikator dla `Dispatchers.IO`. Pozwala testom wstrzyknac
 * `UnconfinedTestDispatcher` zamiast IO bez zaburzania innych Singletonow.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
