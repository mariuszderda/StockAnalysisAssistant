package pl.gwsh.stockanalysis.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.gwsh.stockanalysis.data.local.SaaDatabase
import pl.gwsh.stockanalysis.data.local.dao.CandleDao
import pl.gwsh.stockanalysis.data.local.dao.FavoriteDao
import pl.gwsh.stockanalysis.data.local.dao.StockMetaDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "saa.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SaaDatabase =
        Room.databaseBuilder(context, SaaDatabase::class.java, DB_NAME).build()

    @Provides
    fun provideCandleDao(db: SaaDatabase): CandleDao = db.candleDao()

    @Provides
    fun provideStockMetaDao(db: SaaDatabase): StockMetaDao = db.stockMetaDao()

    @Provides
    fun provideFavoriteDao(db: SaaDatabase): FavoriteDao = db.favoriteDao()
}
