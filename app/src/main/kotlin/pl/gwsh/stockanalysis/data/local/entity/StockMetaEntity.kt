package pl.gwsh.stockanalysis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadane tickera. `fetchedAt` jest epoch-ms ostatniego udanego fetchu z
 * Twelve Data — repository czyta to do oceny TTL (24h, patrz
 * `StockRepositoryImpl.CACHE_TTL_MS`).
 */
@Entity(tableName = "stock_meta")
data class StockMetaEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val currency: String,
    val type: String,
    val fetchedAt: Long,
)
