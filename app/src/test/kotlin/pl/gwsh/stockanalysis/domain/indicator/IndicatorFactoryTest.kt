package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IndicatorFactoryTest {

    private val factory = IndicatorFactory()

    @Test
    fun `create maps every IndicatorSpec variant to expected concrete strategy`() {
        assertThat(factory.create(IndicatorSpec.Sma(20))).isInstanceOf(SmaStrategy::class.java)
        assertThat(factory.create(IndicatorSpec.Ema(20))).isInstanceOf(EmaStrategy::class.java)
        assertThat(factory.create(IndicatorSpec.Rsi(14))).isInstanceOf(RsiStrategy::class.java)
        assertThat(factory.create(IndicatorSpec.Macd(12, 26, 9))).isInstanceOf(MacdStrategy::class.java)
    }

    @Test
    fun `created strategies expose displayName reflecting spec parameters`() {
        assertThat(factory.create(IndicatorSpec.Sma(50)).displayName).isEqualTo("SMA(50)")
        assertThat(factory.create(IndicatorSpec.Ema(7)).displayName).isEqualTo("EMA(7)")
        assertThat(factory.create(IndicatorSpec.Rsi(21)).displayName).isEqualTo("RSI(21)")
        assertThat(factory.create(IndicatorSpec.Macd(5, 35, 5)).displayName).isEqualTo("MACD(5/35/5)")
    }

    /**
     * **Exhaustive-when sentinel.** Glowna gwarancja siedzi w kompilatorze:
     * `when (spec)` w [IndicatorFactory.create] na sealed [IndicatorSpec]
     * jest exhaustive — kompilator zatrzyma build, gdy ktos doda wariant
     * bez branchu.
     *
     * Ten test dodaje runtime guard: gdy w przyszlosci dorzucony zostanie
     * nowy podtyp (np. Bollinger), ten `when` (rowniez exhaustive) wymusi
     * dolozenie galezi, ktora wywolala factory. Jezeli ktos zapomni
     * zaktualizowac fabryki — kompilacja `IndicatorFactory.create` padnie
     * pierwsza, jeszcze przed tym testem.
     */
    @Test
    fun `every IndicatorSpec variant is creatable by the factory`() {
        val specs: List<IndicatorSpec> = listOf(
            IndicatorSpec.Sma(),
            IndicatorSpec.Ema(),
            IndicatorSpec.Rsi(),
            IndicatorSpec.Macd(),
        )
        for (spec in specs) {
            val strategy: IndicatorStrategy = when (spec) {
                is IndicatorSpec.Sma  -> factory.create(spec)
                is IndicatorSpec.Ema  -> factory.create(spec)
                is IndicatorSpec.Rsi  -> factory.create(spec)
                is IndicatorSpec.Macd -> factory.create(spec)
            }
            assertThat(strategy.displayName).isNotEmpty()
        }
    }

    @Test
    fun `availableIndicators returns canonical default list`() {
        val list = factory.availableIndicators()
        assertThat(list).hasSize(4)
        val keys = list.map { it.key }
        assertThat(keys).containsExactly(
            "SMA:20",
            "EMA:20",
            "RSI:14",
            "MACD:12/26/9",
        ).inOrder()
    }

    @Test
    fun `availableIndicators are all instantiable via create`() {
        for (spec in factory.availableIndicators()) {
            val s = factory.create(spec)
            assertThat(s).isNotNull()
            assertThat(s.displayName).isNotEmpty()
        }
    }

    @Test
    fun `IndicatorSpec key is stable for equal data class instances`() {
        // Dla set membership w activeSpecs polegamy na equals/hashCode data class;
        // dodatkowo key musi byc deterministyczny.
        val a = IndicatorSpec.Rsi(14)
        val b = IndicatorSpec.Rsi(14)
        assertThat(a).isEqualTo(b)
        assertThat(a.key).isEqualTo(b.key)
        assertThat(setOf(a)).contains(b)
    }
}
