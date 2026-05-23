package pl.gwsh.stockanalysis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.gwsh.stockanalysis.data.local.dao.CandleDao
import pl.gwsh.stockanalysis.data.local.dao.FavoriteDao
import pl.gwsh.stockanalysis.data.local.dao.StockMetaDao
import pl.gwsh.stockanalysis.data.local.entity.CandleEntity
import pl.gwsh.stockanalysis.data.local.entity.FavoriteEntity
import pl.gwsh.stockanalysis.data.local.entity.StockMetaEntity

@Database(
    entities = [CandleEntity::class, StockMetaEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SaaDatabase : RoomDatabase() {
    abstract fun candleDao(): CandleDao
    abstract fun stockMetaDao(): StockMetaDao
    abstract fun favoriteDao(): FavoriteDao
}
