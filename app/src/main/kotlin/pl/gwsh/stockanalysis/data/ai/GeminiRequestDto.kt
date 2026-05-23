package pl.gwsh.stockanalysis.data.ai

import com.squareup.moshi.JsonClass

/**
 * Cialo zapytania do `models/{model}:generateContent`. Pelny schemat REST
 * ma wiecej pol — uzywamy tylko niezbednych dla MVP.
 *
 * Patrz https://ai.google.dev/api/generate-content i ADR-005.
 */
@JsonClass(generateAdapter = true)
data class GeminiRequestDto(
    val contents: List<GeminiContentDto>,
    val systemInstruction: GeminiContentDto? = null,
    val generationConfig: GeminiGenerationConfigDto? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    val role: String? = null,
    val parts: List<GeminiPartDto>,
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfigDto(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
)
