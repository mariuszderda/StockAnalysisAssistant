package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate
import kotlin.random.Random

/**
 * Test wydajnosciowy — dla 365 swiec i pelnego zestawu wskaznikow
 * obliczenia musza zakonczyc sie w < 100 ms (z generosem 2x na cold JVM
 * w pierwszej iteracji). Spec fazy 4 wymaga raportowania konkretnego czasu
 * w handoffie.
 *
 * Nie uzywa Dispatchers — sprawdzamy CZAS samego algorytmu, nie schedulera.
 * W produkcji ChartViewModel wywoluje to wewnatrz `withContext(Dispatchers.Default)`.
 */
class IndicatorPerformanceTest {

    private val factory = IndicatorFactory()

    private fun yearOfCandles(seed: Long = 42L): List<Candle> {
        val rng = Random(seed)
        var price = 100.0
        return (0 until 365).map { i ->
            val change = rng.nextDouble(-2.0, 2.0)
            price = (price + change).coerceAtLeast(1.0)
            Candle(
                date = LocalDate.of(2025, 1, 1).plusDays(i.toLong()),
                open = price,
                high = price + rng.nextDouble(0.0, 1.5),
                low = price - rng.nextDouble(0.0, 1.5),
                close = price,
                volume = 1_000L,
            )
        }
    }

    @Test
    fun `recompute of full indicator set over 365 candles finishes under 100ms`() {
        val candles = yearOfCandles()
        val specs: List<IndicatorSpec> = listOf(
            IndicatorSpec.Sma(20),
            IndicatorSpec.Ema(20),
            IndicatorSpec.Sma(50),
            IndicatorSpec.Rsi(14),
            IndicatorSpec.Macd(12, 26, 9),
        )

        // Warm-up — JIT i klasy ladowanie.
        repeat(3) { specs.forEach { factory.create(it).compute(candles) } }

        val started = System.nanoTime()
        val results = specs.map { factory.create(it).compute(candles) }
        val elapsedNanos = System.nanoTime() - started
        val elapsedMillis = elapsedNanos / 1_000_000.0

        // Diagnostic — widoczne w `--info` przy testach.
        println("[perf] recompute 365 candles × ${specs.size} indicators = %.3f ms".format(elapsedMillis))

        assertThat(results).hasSize(specs.size)
        assertThat(elapsedMillis).isLessThan(100.0)
    }
}
