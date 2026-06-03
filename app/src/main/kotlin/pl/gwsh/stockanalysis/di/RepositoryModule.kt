package pl.gwsh.stockanalysis.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.gwsh.stockanalysis.data.repository.StockRepositoryImpl
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import javax.inject.Singleton

/**
 * Spina interfejs domenowy ze swoja implementacja (Repository Pattern, ADR-001).
 * Klasa `abstract`, nie `object`, bo `@Binds` wymaga abstract method.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideStockRepository(impl: StockRepositoryImpl): StockRepository = impl
}
