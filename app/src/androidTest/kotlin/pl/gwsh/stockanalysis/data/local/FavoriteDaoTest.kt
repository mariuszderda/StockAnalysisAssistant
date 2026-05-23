package pl.gwsh.stockanalysis.data.local

import app.cash.turbine.test
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.gwsh.stockanalysis.data.local.dao.FavoriteDao
import pl.gwsh.stockanalysis.data.local.entity.FavoriteEntity

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var db: SaaDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SaaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.favoriteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeReactsToInsertAndDelete() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEmpty()

            dao.insert(FavoriteEntity("AAPL", addedAt = 1L))
            assertThat(awaitItem().map { it.symbol }).containsExactly("AAPL")

            dao.insert(FavoriteEntity("MSFT", addedAt = 2L))
            // ORDER BY addedAt DESC — MSFT pierwsze.
            assertThat(awaitItem().map { it.symbol }).containsExactly("MSFT", "AAPL").inOrder()

            dao.delete("AAPL")
            assertThat(awaitItem().map { it.symbol }).containsExactly("MSFT")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun existsReportsPresence() = runTest {
        assertThat(dao.exists("AAPL")).isFalse()
        dao.insert(FavoriteEntity("AAPL", addedAt = 1L))
        assertThat(dao.exists("AAPL")).isTrue()
    }
}
