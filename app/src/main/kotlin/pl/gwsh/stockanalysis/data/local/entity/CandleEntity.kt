package pl.gwsh.stockanalysis.data.local.entity

import androidx.room.Entity

/**
 * Wiersz w cache OHLC. PK kompozytowy (symbol, date) — jeden ticker moze miec
 * tysiace swiec, ale (symbol, date) jest unikalne. Daty trzymamy jako ISO
 * stringi (`yyyy-MM-dd`) — sortowalne leksykograficznie, indeksowane przez SQLite
 * natywnie, i unikamy konwerterow Room.
 */
@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "date"],
)
data class CandleEntity(
    val symbol: String,
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)
