package pl.gwsh.stockanalysis.domain.ai

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.indicator.IndicatorType
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.model.Stock
import java.time.LocalDate

class AnalysisContextBuilderTest {

    private fun candle(day: Int, close: Double): Candle = Candle(
        date = LocalDate.of(2026, 4, day),
        open = close - 1.0, high = close + 1.5, low = close - 2.0, close = close, volume = 1_000_000L,
    )

    private val sampleStock = Stock(
        symbol = "AAPL", name = "Apple Inc.", exchange = "NASDAQ",
        micCode = "XNGS", currency = "USD", type = "Common Stock",
    )

    @Test
    fun `prompt always contains role and no-advice disclaimer`() {
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = sampleStock, range = Range.ONE_MONTH,
            candles = emptyList(), indicators = emptyList(),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("Jestes asystentem analizy technicznej")
        assertThat(out).contains("Nie udzielaj porad inwestycyjnych")
        assertThat(out).contains("po polsku")
    }

    @Test
    fun `prompt embeds stock metadata when stock is present`() {
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = sampleStock, range = Range.THREE_MONTHS,
            candles = emptyList(), indicators = emptyList(),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("AAPL")
        assertThat(out).contains("Apple Inc.")
        assertThat(out).contains("NASDAQ")
        assertThat(out).contains("USD")
        assertThat(out).contains("3 miesiace")
    }

    @Test
    fun `prompt skips stock fields when stock is null but keeps symbol`() {
        val ctx = AnalysisContext(
            symbol = "ZZZ", stock = null, range = Range.ONE_MONTH,
            candles = emptyList(), indicators = emptyList(),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("ZZZ")
        assertThat(out).doesNotContain("Nazwa:")
        assertThat(out).doesNotContain("Gielda:")
    }

    @Test
    fun `prompt includes at most the last 10 candles`() {
        val candles = (1..15).map { candle(it, 100.0 + it) }
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = sampleStock, range = Range.ONE_MONTH,
            candles = candles, indicators = emptyList(),
        )
        val out = buildSystemInstruction(ctx)
        // 1-5 powinny zostac obciete, 6-15 (ostatnie 10) zostaja.
        assertThat(out).doesNotContain("2026-04-01")
        assertThat(out).doesNotContain("2026-04-05")
        assertThat(out).contains("2026-04-06")
        assertThat(out).contains("2026-04-15")
        // Cena 115.00 (formatowanie %.2f) powinna byc w outpucie.
        assertThat(out).contains("115.00")
    }

    @Test
    fun `prompt renders SingleLine indicator with last non-null value`() {
        val rsi = IndicatorResult.SingleLine(
            name = "RSI(14)",
            type = IndicatorType.OSCILLATOR,
            values = listOf(null, null, 62.345, 63.7, null),
        )
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = null, range = Range.ONE_MONTH,
            candles = emptyList(), indicators = listOf(rsi),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("RSI(14): 63.70")
    }

    @Test
    fun `prompt renders MultiLine indicator with last non-null value per line`() {
        val macd = IndicatorResult.MultiLine(
            name = "MACD(12/26/9)",
            type = IndicatorType.OSCILLATOR,
            lines = linkedMapOf(
                "macd" to listOf(null, 1.234),
                "signal" to listOf(null, 0.987),
                "histogram" to listOf(null, 0.247),
            ),
        )
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = null, range = Range.ONE_MONTH,
            candles = emptyList(), indicators = listOf(macd),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("MACD(12/26/9):")
        assertThat(out).contains("macd=1.23")
        assertThat(out).contains("signal=0.99")
        assertThat(out).contains("histogram=0.25")
    }

    @Test
    fun `prompt represents indicator with no non-null values as brak`() {
        val sma = IndicatorResult.SingleLine(
            name = "SMA(20)",
            type = IndicatorType.OVERLAY,
            values = listOf(null, null, null),
        )
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = null, range = Range.ONE_MONTH,
            candles = emptyList(), indicators = listOf(sma),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).contains("SMA(20): brak")
    }

    @Test
    fun `prompt with empty indicators omits indicator section`() {
        val ctx = AnalysisContext(
            symbol = "AAPL", stock = sampleStock, range = Range.ONE_MONTH,
            candles = listOf(candle(1, 100.0)), indicators = emptyList(),
        )
        val out = buildSystemInstruction(ctx)
        assertThat(out).doesNotContain("WSKAZNIKI")
    }
}
