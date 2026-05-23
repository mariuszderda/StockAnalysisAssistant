package pl.gwsh.stockanalysis.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TimeSeriesResponseDto(
    val meta: MetaDto? = null,
    val values: List<TimeSeriesValueDto>? = null,
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class MetaDto(
    val symbol: String? = null,
    val interval: String? = null,
    val currency: String? = null,
    val exchange: String? = null,
    @Json(name = "mic_code") val micCode: String? = null,
    val type: String? = null,
)

/**
 * Twelve Data zwraca wszystkie liczby jako stringi (`"172.45"`) — mapper musi
 * to przeparsowac, bo Moshi nie rzutuje string->Double bez customowego adaptera.
 */
@JsonClass(generateAdapter = true)
data class TimeSeriesValueDto(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
)
