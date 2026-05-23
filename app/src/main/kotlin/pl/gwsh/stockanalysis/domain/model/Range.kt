package pl.gwsh.stockanalysis.domain.model

/**
 * Zakres czasowy wykresu. Wartosci `outputSize` to liczba swiec do pobrania z
 * Twelve Data (parametr API `outputsize`); `minExpectedCandles` to dolny prog,
 * przy ktorym uznajemy cache za "wystarczajacy" mimo brakow weekendowych /
 * swiatecznych — bez tego refetch byłby wymuszany praktycznie zawsze.
 *
 * Wartosci dobrane wg srednio ~21 sesji w miesiacu (GPW + NYSE).
 */
enum class Range(
    val outputSize: Int,
    val minExpectedCandles: Int,
) {
    ONE_MONTH(outputSize = 22, minExpectedCandles = 18),
    THREE_MONTHS(outputSize = 66, minExpectedCandles = 55),
    ONE_YEAR(outputSize = 252, minExpectedCandles = 200),
}
