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
import pl.gwsh.stockanalysis.data.local.dao.StockMetaDao
import pl.gwsh.stockanalysis.data.local.entity.StockMetaEntity

@RunWith(AndroidJUnit4::class)
class StockMetaDaoTest {

    private lateinit var db: SaaDatabase
    private lateinit var dao: StockMetaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SaaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.stockMetaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGet() = runTest {
        val meta = StockMetaEntity(
            symbol = "AAPL",
            name = "Apple Inc",
            exchange = "NASDAQ",
            currency = "USD",
            type = "Common Stock",
            fetchedAt = 1_700_000_000_000L,
        )

        dao.upsert(meta)

        assertThat(dao.get("AAPL")).isEqualTo(meta)
        assertThat(dao.getFetchedAt("AAPL")).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun getOnMissingReturnsNull() = runTest {
        assertThat(dao.get("CDR.WA")).isNull()
        assertThat(dao.getFetchedAt("CDR.WA")).isNull()
    }

    @Test
    fun upsertOverwritesFetchedAt() = runTest {
        val base = StockMetaEntity("AAPL", "Apple", "NASDAQ", "USD", "Common Stock", 1L)
        dao.upsert(base)
        dao.upsert(base.copy(fetchedAt = 2L))

        assertThat(dao.getFetchedAt("AAPL")).isEqualTo(2L)
    }
}
