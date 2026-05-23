package pl.gwsh.stockanalysis.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.data.remote.dto.SymbolMatchDto

class StockMapperTest {

    @Test
    fun `happy path - wszystkie pola obecne`() {
        val dto = SymbolMatchDto(
            symbol = "AAPL",
            instrumentName = "Apple Inc",
            exchange = "NASDAQ",
            micCode = "XNAS",
            country = "United States",
            currency = "USD",
            instrumentType = "Common Stock",
        )

        val stock = dto.toStock()

        assertThat(stock.symbol).isEqualTo("AAPL")
        assertThat(stock.name).isEqualTo("Apple Inc")
        assertThat(stock.exchange).isEqualTo("NASDAQ")
        assertThat(stock.micCode).isEqualTo("XNAS")
        assertThat(stock.currency).isEqualTo("USD")
        assertThat(stock.type).isEqualTo("Common Stock")
    }

    @Test
    fun `brak optional fields defaultuje do pustego stringa`() {
        val dto = SymbolMatchDto(
            symbol = "CDR.WA",
            instrumentName = null,
            exchange = null,
            micCode = null,
            country = null,
            currency = null,
            instrumentType = null,
        )

        val stock = dto.toStock()

        assertThat(stock.symbol).isEqualTo("CDR.WA")
        assertThat(stock.name).isEmpty()
        assertThat(stock.exchange).isEmpty()
        assertThat(stock.micCode).isEmpty()
        assertThat(stock.currency).isEmpty()
        assertThat(stock.type).isEmpty()
    }

    @Test
    fun `polski ticker z sufiksem WA jest przepuszczony bez zmian`() {
        val dto = SymbolMatchDto(symbol = "PKN.WA", currency = "PLN", micCode = "XWAR")
        val stock = dto.toStock()
        assertThat(stock.symbol).isEqualTo("PKN.WA")
        assertThat(stock.currency).isEqualTo("PLN")
        assertThat(stock.micCode).isEqualTo("XWAR")
    }
}
