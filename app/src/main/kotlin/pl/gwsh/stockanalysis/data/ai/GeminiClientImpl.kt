package pl.gwsh.stockanalysis.data.ai

import pl.gwsh.stockanalysis.di.ApiKeys
import pl.gwsh.stockanalysis.domain.ai.GeminiClient
import pl.gwsh.stockanalysis.domain.ai.GeminiError
import pl.gwsh.stockanalysis.domain.ai.GeminiException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [GeminiClient] przez REST. Mapuje wyniki Retrofit na
 * `Result<String>` + [GeminiException] z typowanym [GeminiError].
 *
 * Parametry generacji (`temperature`, `maxOutputTokens`) zgodnie z
 * CLAUDE.md § Gemini boundaries.
 */
@Singleton
class GeminiClientImpl @Inject constructor(
    private val api: GeminiApi,
    private val apiKeys: ApiKeys,
) : GeminiClient {

    override suspend fun ask(systemInstruction: String, userPrompt: String): Result<String> {
        val key = apiKeys.gemini
        if (key.isBlank()) return Result.failure(GeminiException(GeminiError.MissingApiKey))

        val body = GeminiRequestDto(
            contents = listOf(
                GeminiContentDto(
                    role = "user",
                    parts = listOf(GeminiPartDto(text = userPrompt)),
                ),
            ),
            systemInstruction = GeminiContentDto(
                parts = listOf(GeminiPartDto(text = systemInstruction)),
            ),
            generationConfig = GeminiGenerationConfigDto(
                temperature = TEMPERATURE,
                maxOutputTokens = MAX_OUTPUT_TOKENS,
            ),
        )

        return try {
            val response = api.generate(model = MODEL, apiKey = key, body = body)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString(separator = "") { it.text }
                ?.trim()
                .orEmpty()

            val blocked = response.promptFeedback?.blockReason != null
            when {
                blocked -> Result.failure(GeminiException(GeminiError.Blocked))
                text.isEmpty() -> Result.failure(GeminiException(GeminiError.Blocked))
                else -> Result.success(text)
            }
        } catch (e: IOException) {
            Result.failure(GeminiException(GeminiError.Network))
        } catch (e: HttpException) {
            Result.failure(GeminiException(GeminiError.Server(e.code())))
        } catch (e: Exception) {
            Result.failure(GeminiException(GeminiError.Unknown(e)))
        }
    }
}

private const val MODEL = "gemini-1.5-flash"
private const val TEMPERATURE = 0.6
private const val MAX_OUTPUT_TOKENS = 800
