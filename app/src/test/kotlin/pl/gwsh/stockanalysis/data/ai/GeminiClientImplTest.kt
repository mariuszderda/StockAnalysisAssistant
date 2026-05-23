package pl.gwsh.stockanalysis.data.ai

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.di.ApiKeys
import pl.gwsh.stockanalysis.domain.ai.GeminiError
import pl.gwsh.stockanalysis.domain.ai.GeminiException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GeminiClientImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GeminiApi
    private val apiKeys: ApiKeys = mockk()

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        val moshi = Moshi.Builder().build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): GeminiClientImpl = GeminiClientImpl(api, apiKeys)

    @Test
    fun `missing api key returns MissingApiKey without HTTP call`() = runTest {
        every { apiKeys.gemini } returns ""
        val result = client().ask("system", "pytanie")

        assertThat(result.isFailure).isTrue()
        val err = (result.exceptionOrNull() as GeminiException).error
        assertThat(err).isEqualTo(GeminiError.MissingApiKey)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `happy path returns text from first candidate first part`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "candidates":[{
                    "content":{"role":"model","parts":[{"text":"Witaj"},{"text":" swiecie"}]},
                    "finishReason":"STOP"
                  }]
                }
                """.trimIndent(),
            ),
        )
        val result = client().ask("system", "Powitaj")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("Witaj swiecie")
    }

    @Test
    fun `safety blocked response maps to Blocked`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "candidates":[],
                  "promptFeedback":{"blockReason":"SAFETY"}
                }
                """.trimIndent(),
            ),
        )
        val result = client().ask("system", "uzytkownik")
        val err = (result.exceptionOrNull() as GeminiException).error
        assertThat(err).isEqualTo(GeminiError.Blocked)
    }

    @Test
    fun `empty response text without explicit block reason still maps to Blocked`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "candidates":[{"content":{"parts":[{"text":""}]}, "finishReason":"STOP"}]
                }
                """.trimIndent(),
            ),
        )
        val result = client().ask("system", "uzytkownik")
        val err = (result.exceptionOrNull() as GeminiException).error
        assertThat(err).isEqualTo(GeminiError.Blocked)
    }

    @Test
    fun `HTTP 500 maps to Server with status code`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = client().ask("system", "uzytkownik")
        val err = (result.exceptionOrNull() as GeminiException).error
        assertThat(err).isInstanceOf(GeminiError.Server::class.java)
        assertThat((err as GeminiError.Server).code).isEqualTo(500)
    }

    @Test
    fun `HTTP 429 maps to Server 429`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(MockResponse().setResponseCode(429))
        val result = client().ask("system", "uzytkownik")
        val err = (result.exceptionOrNull() as GeminiException).error
        assertThat(err).isInstanceOf(GeminiError.Server::class.java)
        assertThat((err as GeminiError.Server).code).isEqualTo(429)
    }

    @Test
    fun `request body includes systemInstruction, contents, and generationConfig`() = runTest {
        every { apiKeys.gemini } returns "any-key"
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}""",
            ),
        )
        client().ask(systemInstruction = "Jestes asystentem.", userPrompt = "Pytanie?")

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertThat(body).contains("\"systemInstruction\"")
        assertThat(body).contains("\"Jestes asystentem.\"")
        assertThat(body).contains("\"Pytanie?\"")
        assertThat(body).contains("\"temperature\":0.6")
        assertThat(body).contains("\"maxOutputTokens\":800")
        assertThat(recorded.path).contains("models/gemini-1.5-flash:generateContent")
        assertThat(recorded.path).contains("key=any-key")
    }
}
