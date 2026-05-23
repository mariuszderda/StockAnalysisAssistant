package pl.gwsh.stockanalysis.data.ai

import com.squareup.moshi.JsonClass

/**
 * Odpowiedz z `generateContent`. Pola opcjonalne, bo:
 *  - `candidates` puste przy zablokowaniu przez safety filter;
 *  - `promptFeedback.blockReason != null` rowniez sygnalizuje blok;
 *  - finishReason moze byc `SAFETY` / `RECITATION` / `MAX_TOKENS` itd.
 */
@JsonClass(generateAdapter = true)
data class GeminiResponseDto(
    val candidates: List<GeminiCandidateDto>? = null,
    val promptFeedback: GeminiPromptFeedbackDto? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
    val finishReason: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeminiPromptFeedbackDto(
    val blockReason: String? = null,
)
