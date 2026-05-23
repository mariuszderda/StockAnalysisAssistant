package pl.gwsh.stockanalysis.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SymbolSearchResponseDto(
    val data: List<SymbolMatchDto>? = null,
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class SymbolMatchDto(
    val symbol: String,
    @Json(name = "instrument_name") val instrumentName: String? = null,
    val exchange: String? = null,
    @Json(name = "mic_code") val micCode: String? = null,
    val country: String? = null,
    val currency: String? = null,
    @Json(name = "instrument_type") val instrumentType: String? = null,
)
