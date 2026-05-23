package pl.gwsh.stockanalysis.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.gwsh.stockanalysis.data.local.entity.CandleEntity

@Dao
interface CandleDao {

    @Query(
        "SELECT * FROM candles " +
            "WHERE symbol = :symbol AND date >= :fromIsoDate " +
            "ORDER BY date ASC",
    )
    suspend fun getRange(symbol: String, fromIsoDate: String): List<CandleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(candles: List<CandleEntity>)

    @Query("DELETE FROM candles WHERE symbol = :symbol")
    suspend fun deleteForSymbol(symbol: String)

    @Query("SELECT COUNT(*) FROM candles WHERE symbol = :symbol")
    suspend fun countFor(symbol: String): Int
}
