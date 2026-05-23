package pl.gwsh.stockanalysis.domain.indicator

/**
 * Specyfikacja wskaznika — "intencja uzytkownika" zanim zostanie obliczony.
 * Rozdzielona od [IndicatorStrategy] swiadomie: ViewModel trzyma
 * `Set<IndicatorSpec>` (lekkie data class, latwe do (de)serializacji i
 * porownan), a IndicatorFactory mapuje spec → konkretna strategia na
 * zadanie.
 *
 * **Dlaczego sealed a nie String?** Exhaustive `when (spec)` w [IndicatorFactory]
 * gwarantuje, ze kompilator zatrzyma build, gdy ktos doda nowy wariant a
 * zapomni zaktualizowac fabryki. Z `String` skonczylibysmy z `else -> throw`,
 * ktory wybucha dopiero w runtime — i tylko jesli kod doszedl do tej linii.
 *
 * **Klucz [key]:** stabilna reprezentacja stringowa uzywana do porownan
 * zestawow ([equals]/[hashCode] na data class i tak dziala, ale `key`
 * pozwala dodatkowo serializowac stan UI np. do SavedStateHandle bez
 * polymorfizmu).
 */
sealed class IndicatorSpec {
    abstract val key: String

    data class Rsi(val period: Int = 14) : IndicatorSpec() {
        override val key: String = "RSI:$period"
    }

    data class Macd(
        val fast: Int = 12,
        val slow: Int = 26,
        val signal: Int = 9,
    ) : IndicatorSpec() {
        override val key: String = "MACD:$fast/$slow/$signal"
    }

    data class Sma(val period: Int = 20) : IndicatorSpec() {
        override val key: String = "SMA:$period"
    }

    data class Ema(val period: Int = 20) : IndicatorSpec() {
        override val key: String = "EMA:$period"
    }
}
