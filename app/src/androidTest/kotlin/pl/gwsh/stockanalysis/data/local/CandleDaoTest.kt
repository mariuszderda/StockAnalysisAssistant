package pl.gwsh.stockanalysis.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.gwsh.stockanalysis.data.local.dao.CandleDao
import pl.gwsh.stockanalysis.data.local.entity.CandleEntity

@RunWith(AndroidJUnit4::class)
class CandleDaoTest {

    private lateinit var db: SaaDatabase
    private lateinit var dao: CandleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SaaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.candleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndQueryRange() = runTest {
        dao.upsertAll(
            listOf(
                candle("AAPL", "2026-05-18"),
                candle("AAPL", "2026-05-19"),
                candle("AAPL", "2026-05-20"),
            ),
        )

        val result = dao.getRange("AAPL", "2026-05-19")

        assertThat(result.map { it.date }).containsExactly("2026-05-19", "2026-05-20").inOrder()
    }

    @Test
    fun upsertReplacesOnConflict() = runTest {
        dao.upsertAll(listOf(candle("AAPL", "2026-05-20", close = 100.0)))
        dao.upsertAll(listOf(candle("AAPL", "2026-05-20", close = 200.0)))

        val result = dao.getRange("AAPL", "2026-05-20")

        assertThat(result).hasSize(1)
        assertThat(result.first().close).isEqualTo(200.0)
    }

    @Test
    fun deleteForSymbolOnlyAffectsTicker() = runTest {
        dao.upsertAll(
            listOf(
                candle("AAPL", "2026-05-20"),
                candle("MSFT", "2026-05-20"),
            ),
        )

        dao.deleteForSymbol("AAPL")

        assertThat(dao.countFor("AAPL")).isEqualTo(0)
        assertThat(dao.countFor("MSFT")).isEqualTo(1)
    }

    private fun candle(symbol: String, date: String, close: Double = 1.0) = CandleEntity(
        symbol = symbol,
        date = date,
        open = 1.0, high = 1.0, low = 1.0, close = close, volume = 1L,
    )
}
