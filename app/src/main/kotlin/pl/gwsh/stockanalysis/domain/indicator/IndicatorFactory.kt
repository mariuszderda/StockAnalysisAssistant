package pl.gwsh.stockanalysis.domain.indicator

import javax.inject.Inject
import javax.inject.Singleton

/**
 * **Wzorzec Factory.** Mapuje deklaratywny [IndicatorSpec] (intencja
 * uzytkownika) na konkretna [IndicatorStrategy] (algorytm).
 *
 * Dlaczego nie pominac warstwy i nie tworzyc strategii bezposrednio w
 * ViewModelu?
 *  - Single source of truth: lista dostepnych wskaznikow i ich domyslnych
 *    parametrow zyje w jednym miejscu ([availableIndicators]). UI to czyta,
 *    nie duplikuje.
 *  - Exhaustive `when (spec)` na [IndicatorSpec] (sealed) gwarantuje, ze
 *    dodanie nowego wariantu zatrzyma kompilacje, dopoki branch nie pojawi
 *    sie w fabryce. Brak ryzyka "Strategy.kt rosnie a fabryka zostaje stara".
 *  - Hilt: jako `@Singleton` instancja jest dzielona przez wszystkie
 *    ViewModele i moze trzymac np. cache obliczen w przyszlosci.
 *
 * Wzorzec Strategy + Factory wspolpracuje tak: Factory wie *ktora* strategia,
 * Strategy wie *jak* policzyc. Klient (ChartViewModel) zalezy wylacznie od
 * abstrakcji [IndicatorStrategy] i sealed [IndicatorSpec], wiec implementacje
 * konkretnych wskaznikow mozna wymienic bez dotykania prezentacji.
 */
@Singleton
class IndicatorFactory @Inject constructor() {

    /**
     * Tworzy strategie dla danego [spec]. Exhaustive `when` — kompilator
     * wymusi nowy branch przy dodaniu wariantu [IndicatorSpec].
     */
    fun create(spec: IndicatorSpec): IndicatorStrategy = when (spec) {
        is IndicatorSpec.Rsi  -> RsiStrategy(spec.period)
        is IndicatorSpec.Macd -> MacdStrategy(spec.fast, spec.slow, spec.signal)
        is IndicatorSpec.Sma  -> SmaStrategy(spec.period)
        is IndicatorSpec.Ema  -> EmaStrategy(spec.period)
    }

    /**
     * Lista wskaznikow oferowanych uzytkownikowi z domyslnymi parametrami.
     * UI buduje na tej podstawie panel wyboru — nie hardkoduje typow.
     * Dodanie wskaznika tutaj = pojawia sie w panelu bez zmian w UI.
     */
    fun availableIndicators(): List<IndicatorSpec> = listOf(
        IndicatorSpec.Sma(period = 20),
        IndicatorSpec.Ema(period = 20),
        IndicatorSpec.Rsi(period = 14),
        IndicatorSpec.Macd(fast = 12, slow = 26, signal = 9),
    )
}
