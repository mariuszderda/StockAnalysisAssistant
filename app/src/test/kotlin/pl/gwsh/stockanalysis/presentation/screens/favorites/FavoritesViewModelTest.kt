package pl.gwsh.stockanalysis.presentation.screens.favorites

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import pl.gwsh.stockanalysis.domain.model.Stock
import pl.gwsh.stockanalysis.domain.repository.StockRepository
import pl.gwsh.stockanalysis.presentation.common.MainDispatcherExtension

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repo: StockRepository = mockk()

    private fun stock(symbol: String) = Stock(
        symbol = symbol, name = "$symbol Inc", exchange = "NASDAQ",
        micCode = "XNAS", currency = "USD", type = "Common Stock",
    )

    @Test
    fun `initial state is loading`() = runTest {
        every { repo.observeFavorites() } returns MutableStateFlow(emptyList())
        val vm = FavoritesViewModel(repo)
        // Przed jakimkolwiek collectorem stan to initialValue (loading = true).
        assertThat(vm.state.value.loading).isTrue()
        assertThat(vm.state.value.items).isEmpty()
    }

    @Test
    fun `empty flow emission yields not loading and empty items`() = runTest {
        every { repo.observeFavorites() } returns flowOf(emptyList())
        val vm = FavoritesViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.items).isEmpty()
    }

    @Test
    fun `flow emission populates items`() = runTest {
        every { repo.observeFavorites() } returns flowOf(listOf(stock("AAPL"), stock("CDR.WA")))
        val vm = FavoritesViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.items.map { it.symbol }).containsExactly("AAPL", "CDR.WA").inOrder()
    }
}
