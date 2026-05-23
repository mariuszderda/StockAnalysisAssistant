package pl.gwsh.stockanalysis.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.gwsh.stockanalysis.data.local.dao.CandleDao
import pl.gwsh.stockanalysis.data.local.dao.FavoriteDao
import pl.gwsh.stockanalysis.data.local.dao.StockMetaDao
import pl.gwsh.stockanalysis.data.local.entity.CandleEntity
import pl.gwsh.stockanalysis.data.local.entity.FavoriteEntity
import pl.gwsh.stockanalysis.data.local.entity.StockMetaEntity
import pl.gwsh.stockanalysis.data.mapper.toCandles
import pl.gwsh.stockanalysis.data.mapper.toStock
import pl.gwsh.stockanalysis.data.remote.TwelveDataApi
import pl.gwsh.stockanalysis.data.remote.dto.MetaDto
import pl.gwsh.stockanalysis.di.ApiKeys
import pl.gwsh.stockanalysis.di.IoDispatcher
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.model.Stock
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: TwelveDataApi,
    private val candleDao: CandleDao,
    private val metaDao: StockMetaDao,
    private val favoriteDao: FavoriteDao,
    private val apiKeys: ApiKeys,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StockRepository {

    companion object {
        const val CACHE_TTL_MS: Long = 24L * 60 * 60 * 1000
        const val BASE_INTERVAL: String = "1day"

        /** Mnoznik kalendarzowy nad trading-days (uwzglednia weekendy + swieta). */
        private const val CALENDAR_OVER_TRADING_DAYS = 1.6
    }

    override suspend fun searchSymbols(query: String): Result<List<Stock>> = withContext(io) {
        try {
            val response = api.symbolSearch(query, apiKeys.twelveData)
            if (!response.isSuccessful) {
                return@withContext fail(DataError.Server(response.code(), response.message()))
            }
            val body = response.body()
                ?: return@withContext fail(DataError.ParseError("empty body"))
            if (body.status.equals("error", ignoreCase = true)) {
                return@withContext fail(classifyApiError(body.code, body.message))
            }
            Result.success(body.data.orEmpty().map { it.toStock() })
        } catch (_: IOException) {
            fail(DataError.Network)
        } catch (e: Throwable) {
            fail(DataError.Unknown(e))
        }
    }

    override suspend fun getCandles(
        symbol: String,
        range: Range,
        forceRefresh: Boolean,
    ): Result<List<Candle>> = withContext(io) {
        val today = LocalDate.now(clock.withZone(ZoneId.of("UTC")))
        val cacheFromDate = today.minusDays((range.outputSize * CALENDAR_OVER_TRADING_DAYS).toLong())
        val cached = candleDao.getRange(symbol, cacheFromDate.toString())
        val fetchedAt = metaDao.getFetchedAt(symbol)
        val fresh = fetchedAt != null && (clock.millis() - fetchedAt) < CACHE_TTL_MS
        val enough = cached.size >= range.minExpectedCandles

        if (!forceRefresh && fresh && enough) {
            return@withContext Result.success(cached.map { it.toDomain() })
        }

        try {
            val response = api.timeSeries(
                symbol = symbol,
                interval = BASE_INTERVAL,
                outputSize = range.outputSize,
                apiKey = apiKeys.twelveData,
            )
            if (!response.isSuccessful) {
                return@withContext fail(DataError.Server(response.code(), response.message()))
            }
            val body = response.body()
                ?: return@withContext fail(DataError.ParseError("empty body"))
            if (body.status.equals("error", ignoreCase = true)) {
                return@withContext fail(classifyApiError(body.code, body.message))
            }

            val candles: List<Candle> = try {
                body.toCandles()
            } catch (e: DateTimeParseException) {
                return@withContext fail(DataError.ParseError(e.message ?: "bad date"))
            } catch (e: NumberFormatException) {
                return@withContext fail(DataError.ParseError(e.message ?: "bad number"))
            }

            if (candles.isEmpty()) {
                return@withContext if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDomain() })
                } else {
                    fail(DataError.NotFound)
                }
            }

            candleDao.deleteForSymbol(symbol)
            candleDao.upsertAll(candles.toEntities(symbol))
            metaDao.upsert(buildMeta(symbol, body.meta, clock.millis()))

            Result.success(candles)
        } catch (_: IOException) {
            // Graceful fallback: brak sieci → cache jesli niepusty.
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                fail(DataError.Network)
            }
        } catch (e: Throwable) {
            fail(DataError.Unknown(e))
        }
    }

    override fun observeFavorites(): Flow<List<Stock>> =
        favoriteDao.observeAll().map { rows ->
            rows.map { fav ->
                Stock(
                    symbol = fav.symbol,
                    name = "", exchange = "", micCode = "", currency = "", type = "",
                )
            }
        }

    override suspend fun toggleFavorite(symbol: String): Unit = withContext(io) {
        if (favoriteDao.exists(symbol)) {
            favoriteDao.delete(symbol)
        } else {
            favoriteDao.insert(FavoriteEntity(symbol = symbol, addedAt = clock.millis()))
        }
    }

    override suspend fun isFavorite(symbol: String): Boolean = withContext(io) {
        favoriteDao.exists(symbol)
    }

    private fun <T> fail(error: DataError): Result<T> = Result.failure(DataErrorException(error))

    private fun classifyApiError(code: Int?, message: String?): DataError = when (code) {
        429 -> DataError.RateLimited
        404 -> DataError.NotFound
        else -> DataError.Server(code ?: -1, message.orEmpty())
    }
}

internal fun CandleEntity.toDomain(): Candle = Candle(
    date = LocalDate.parse(date),
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume,
)

internal fun List<Candle>.toEntities(symbol: String): List<CandleEntity> = map {
    CandleEntity(
        symbol = symbol,
        date = it.date.toString(),
        open = it.open,
        high = it.high,
        low = it.low,
        close = it.close,
        volume = it.volume,
    )
}

internal fun buildMeta(symbol: String, meta: MetaDto?, fetchedAt: Long): StockMetaEntity =
    StockMetaEntity(
        symbol = symbol,
        name = meta?.symbol.orEmpty(),
        exchange = meta?.exchange.orEmpty(),
        currency = meta?.currency.orEmpty(),
        type = meta?.type.orEmpty(),
        fetchedAt = fetchedAt,
    )
