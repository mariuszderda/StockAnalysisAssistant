package pl.gwsh.stockanalysis.presentation.navigation

/**
 * Lista tras nawigacyjnych aplikacji. Trzymamy je jako stale w jednym miejscu,
 * zeby compose-typo nie rozjechalo sie po projekcie.
 *
 * `Chart` to trasa parametryzowana symbolem tickera; tworze ja przez [SaaDestinations.chartRoute].
 */
object SaaDestinations {
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val CHART_PATTERN = "chart/{symbol}"
    const val CHART_ARG_SYMBOL = "symbol"

    /** Buduje konkretny URL trasy wykresu dla danego tickera. */
    fun chartRoute(symbol: String): String = "chart/$symbol"
}
