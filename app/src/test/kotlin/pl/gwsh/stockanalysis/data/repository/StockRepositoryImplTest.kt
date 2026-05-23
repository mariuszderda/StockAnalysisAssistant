package pl.gwsh.stockanalysis.data.repository

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.data.local.dao.CandleDao
import pl.gwsh.stockanalysis.data.local.dao.FavoriteDao
import pl.gwsh.stockanalysis.data.local.dao.StockMetaDao
import pl.gwsh.stockanalysis.data.local.entity.CandleEntity
import pl.gwsh.stockanalysis.data.local.entity.StockMetaEntity
import pl.gwsh.stockanalysis.data.remote.TwelveDataApi
import pl.gwsh.stockanalysis.di.ApiKeys
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("OPT_IN_USAGE")
class StockRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TwelveDataApi

    private val candleDao: CandleDao = mockk(relaxUnitFun = true)
    private val metaDao: StockMetaDao = mockk(relaxUnitFun = true)
    private val favoriteDao: FavoriteDao = mockk(relaxUnitFun = true)
    private val apiKeys: ApiKeys = mockk { coEvery { twelveData } returns "TEST_KEY" }

    // Zegar zatrzymany na 2026-05-23T12:00:00Z (data z CLAUDE.md/currentDate).
    private val fixedNowMs: Long = Instant.parse("2026-05-23T12:00:00Z").toEpochMilli()
    private val clock = Clock.fixed(Instant.ofEpochMilli(fixedNowMs), ZoneOffset.UTC)

    private lateinit var repo: StockRepositoryImpl

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        api = retrofit.create(TwelveDataApi::class.java)

        repo = StockRepositoryImpl(
            api = api,
            candleDao = candleDao,
            metaDao = metaDao,
            favoriteDao = favoriteDao,
            apiKeys = apiKeys,
            clock = clock,
            io = UnconfinedTestDispatcher(),
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    // === getCandles =========================================================

    @Test
    fun `cache hit fresh — bez wywolania API`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns fullCachedMonth()
        coEvery { metaDao.getFetchedAt("AAPL") } returns fixedNowMs - 3_600_000L // 1h temu

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(22)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `cache hit stale — wywoluje API i refreshuje cache`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returnsMany listOf(fullCachedMonth(), fullCachedMonth())
        coEvery { metaDao.getFetchedAt("AAPL") } returns fixedNowMs - (25L * 60 * 60 * 1000) // 25h
        server.enqueue(MockResponse().setBody(timeSeriesBody(2)))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        assertThat(server.requestCount).isEqualTo(1)
        coVerify { candleDao.deleteForSymbol("AAPL") }
        coVerify { candleDao.upsertAll(any()) }
        coVerify { metaDao.upsert(any()) }
    }

    @Test
    fun `cache miss + network OK — fetch, insert, success`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        server.enqueue(MockResponse().setBody(timeSeriesBody(20)))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(20)
        val slot = slot<List<CandleEntity>>()
        coVerify { candleDao.upsertAll(capture(slot)) }
        assertThat(slot.captured).hasSize(20)
        assertThat(slot.captured.all { it.symbol == "AAPL" }).isTrue()
    }

    @Test
    fun `HTTP 200 z status error code 429 → RateLimited`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        server.enqueue(MockResponse().setBody(errorBody(code = 429, msg = "rate limit")))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isFailure).isTrue()
        assertThat(extractError(result)).isEqualTo(DataError.RateLimited)
    }

    @Test
    fun `HTTP 200 z status error code 404 → NotFound`() = runTest {
        coEvery { candleDao.getRange("BOGUS", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("BOGUS") } returns null
        server.enqueue(MockResponse().setBody(errorBody(code = 404, msg = "symbol not found")))

        val result = repo.getCandles("BOGUS", Range.ONE_MONTH)

        assertThat(extractError(result)).isEqualTo(DataError.NotFound)
    }

    @Test
    fun `HTTP 500 → Server(500)`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        val err = extractError(result)
        assertThat(err).isInstanceOf(DataError.Server::class.java)
        assertThat((err as DataError.Server).code).isEqualTo(500)
    }

    @Test
    fun `IOException + empty cache → Network`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(extractError(result)).isEqualTo(DataError.Network)
    }

    @Test
    fun `IOException + stale cache → success z cache (graceful fallback)`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns fullCachedMonth()
        coEvery { metaDao.getFetchedAt("AAPL") } returns fixedNowMs - (25L * 60 * 60 * 1000)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(22)
    }

    @Test
    fun `forceRefresh true wywoluje API mimo swiezego cache`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returnsMany listOf(fullCachedMonth(), fullCachedMonth())
        coEvery { metaDao.getFetchedAt("AAPL") } returns fixedNowMs - 60_000L // 1 min — definitely fresh
        server.enqueue(MockResponse().setBody(timeSeriesBody(22)))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH, forceRefresh = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `puste values w body + pusty cache → NotFound`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        server.enqueue(MockResponse().setBody("""{"meta":{},"values":[]}"""))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(extractError(result)).isEqualTo(DataError.NotFound)
    }

    @Test
    fun `puste values + niepusty cache → success z cache`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns fullCachedMonth()
        coEvery { metaDao.getFetchedAt("AAPL") } returns fixedNowMs - (25L * 60 * 60 * 1000)
        server.enqueue(MockResponse().setBody("""{"meta":{},"values":[]}"""))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(22)
    }

    @Test
    fun `malformed datetime w body → ParseError`() = runTest {
        coEvery { candleDao.getRange("AAPL", any()) } returns emptyList()
        coEvery { metaDao.getFetchedAt("AAPL") } returns null
        val bad = """
            {"meta":{"symbol":"AAPL"},
             "values":[{"datetime":"NOT-A-DATE","open":"1","high":"1","low":"1","close":"1","volume":"1"}]}
        """.trimIndent()
        server.enqueue(MockResponse().setBody(bad))

        val result = repo.getCandles("AAPL", Range.ONE_MONTH)

        assertThat(extractError(result)).isInstanceOf(DataError.ParseError::class.java)
    }

    // === searchSymbols ======================================================

    @Test
    fun `searchSymbols happy path`() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"data":[{"symbol":"AAPL","instrument_name":"Apple Inc","exchange":"NASDAQ"}]}"""
        ))

        val result = repo.searchSymbols("aapl")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().single().symbol).isEqualTo("AAPL")
    }

    @Test
    fun `searchSymbols pusta lista to success`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[]}"""))

        val result = repo.searchSymbols("zzzzz")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `searchSymbols rate-limited`() = runTest {
        server.enqueue(MockResponse().setBody(errorBody(code = 429, msg = "rate limit")))

        val result = repo.searchSymbols("aapl")

        assertThat(extractError(result)).isEqualTo(DataError.RateLimited)
    }

    @Test
    fun `searchSymbols IOException to Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repo.searchSymbols("aapl")

        assertThat(extractError(result)).isEqualTo(DataError.Network)
    }

    // === favorites =========================================================

    @Test
    fun `toggleFavorite dodaje gdy brak, usuwa gdy jest`() = runTest {
        coEvery { favoriteDao.exists("AAPL") } returnsMany listOf(false, true)

        repo.toggleFavorite("AAPL")
        coVerify { favoriteDao.insert(match { it.symbol == "AAPL" }) }

        repo.toggleFavorite("AAPL")
        coVerify { favoriteDao.delete("AAPL") }
    }

    @Test
    fun `isFavorite delegate do dao`() = runTest {
        coEvery { favoriteDao.exists("AAPL") } returns true
        assertThat(repo.isFavorite("AAPL")).isTrue()
    }

    // === helpers ===========================================================

    private fun extractError(r: Result<*>): DataError {
        val ex = r.exceptionOrNull() as? DataErrorException
            ?: error("Expected DataErrorException, got: ${r.exceptionOrNull()}")
        return ex.error
    }

    private fun fullCachedMonth(): List<CandleEntity> = (1..22).map {
        CandleEntity(
            symbol = "AAPL",
            date = "2026-05-%02d".format(it),
            open = 1.0, high = 1.0, low = 1.0, close = 1.0, volume = 1L,
        )
    }

    private fun timeSeriesBody(n: Int): String {
        val values = (1..n).joinToString(",") { i ->
            """{"datetime":"2026-04-%02d","open":"1","high":"1","low":"1","close":"1","volume":"1"}""".format(i)
        }
        return """{"meta":{"symbol":"AAPL","interval":"1day","currency":"USD","exchange":"NASDAQ","mic_code":"XNAS","type":"Common Stock"},"values":[$values],"status":"ok"}"""
    }

    private fun errorBody(code: Int, msg: String): String =
        """{"status":"error","code":$code,"message":"$msg"}"""
}
