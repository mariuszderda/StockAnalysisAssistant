package pl.gwsh.stockanalysis.presentation.screens.search

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import pl.gwsh.stockanalysis.domain.model.DataError
import pl.gwsh.stockanalysis.domain.model.DataErrorException
import pl.gwsh.stockanalysis.domain.model.Stock
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.common.MainDispatcherExtension

/**
 * Testy ViewModelu szukania. StateFlow konfluuje szybkie emisje (Loading -> Success)
 * gdy Unconfined dispatcher i mock-success sa synchronozne — wiec asercja Loading
 * jest pominieta. Loading jest stanem przejsciowym wizualnym; sprawdzamy
 * koncowe stany (Idle/Success/Empty/Error), ktore opisuja kontrakt VM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repo: StockRepository = mockk()

    private fun makeVm(): SearchViewModel = SearchViewModel(repo)

    private fun stock(symbol: String) = Stock(
        symbol = symbol, name = "$symbol Inc", exchange = "NASDAQ",
        micCode = "XNAS", currency = "USD", type = "Common Stock",
    )

    @Test
    fun `initial state is Idle`() = runTest {
        val vm = makeVm()
        assertThat(vm.state.value).isEqualTo(SearchUiState.Idle)
    }

    @Test
    fun `blank query stays Idle and never hits repo`() = runTest {
        val vm = makeVm()
        vm.onQueryChange("   ")
        advanceTimeBy(500)
        assertThat(vm.state.value).isEqualTo(SearchUiState.Idle)
        coVerify(exactly = 0) { repo.searchSymbols(any()) }
    }

    @Test
    fun `non-blank query results in Success`() = runTest {
        coEvery { repo.searchSymbols("aapl") } returns Result.success(listOf(stock("AAPL")))
        val vm = makeVm()
        vm.onQueryChange("aapl")
        advanceUntilIdle()
        val s = vm.state.value
        assertThat(s).isInstanceOf(SearchUiState.Success::class.java)
        val success = s as SearchUiState.Success
        assertThat(success.query).isEqualTo("aapl")
        assertThat(success.results.single().symbol).isEqualTo("AAPL")
    }

    @Test
    fun `empty result list yields Empty`() = runTest {
        coEvery { repo.searchSymbols("zzzz") } returns Result.success(emptyList())
        val vm = makeVm()
        vm.onQueryChange("zzzz")
        advanceUntilIdle()
        assertThat(vm.state.value).isEqualTo(SearchUiState.Empty("zzzz"))
    }

    @Test
    fun `repository failure becomes Error with mapped DataError`() = runTest {
        coEvery { repo.searchSymbols("aapl") } returns
            Result.failure(DataErrorException(DataError.RateLimited))
        val vm = makeVm()
        vm.onQueryChange("aapl")
        advanceUntilIdle()
        assertThat(vm.state.value).isEqualTo(SearchUiState.Error(DataError.RateLimited))
    }

    @Test
    fun `clear resets to Idle`() = runTest {
        coEvery { repo.searchSymbols(any()) } returns Result.success(listOf(stock("AAPL")))
        val vm = makeVm()
        vm.onQueryChange("aapl")
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(SearchUiState.Success::class.java)

        vm.clear()
        advanceUntilIdle()
        assertThat(vm.state.value).isEqualTo(SearchUiState.Idle)
    }

    @Test
    fun `debounce coalesces rapid keystrokes into one search`() = runTest {
        coEvery { repo.searchSymbols("aapl") } returns Result.success(listOf(stock("AAPL")))
        val vm = makeVm()
        vm.onQueryChange("a")
        vm.onQueryChange("aa")
        vm.onQueryChange("aap")
        vm.onQueryChange("aapl")
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.searchSymbols("aapl") }
        coVerify(exactly = 0) { repo.searchSymbols("a") }
        coVerify(exactly = 0) { repo.searchSymbols("aa") }
        coVerify(exactly = 0) { repo.searchSymbols("aap") }
    }
}
