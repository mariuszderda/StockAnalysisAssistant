package pl.gwsh.stockanalysis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.gwsh.stockanalysis.data.local.entity.StockMetaEntity

@Dao
interface StockMetaDao {

    @Query("SELECT * FROM stock_meta WHERE symbol = :symbol")
    suspend fun get(symbol: String): StockMetaEntity?

    @Query("SELECT fetchedAt FROM stock_meta WHERE symbol = :symbol")
    suspend fun getFetchedAt(symbol: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: StockMetaEntity)
}
