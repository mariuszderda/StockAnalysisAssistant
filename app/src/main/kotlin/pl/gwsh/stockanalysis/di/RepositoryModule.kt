package pl.gwsh.stockanalysis.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Stub. W Fazie 2 wiążemy `StockRepository` (interfejs z domain) → `StockRepositoryImpl` (data).
 * To jest punkt egzaminacyjny dla Repository Pattern — patrz CLAUDE.md § "Patterns to defend".
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
