package pl.gwsh.stockanalysis.domain.indicator

/**
 * Klasyfikacja wskaznika ze wzgledu na sposob renderowania na wykresie.
 *
 * - [OVERLAY] — rysowany na tym samym kanwasie co cena (SMA, EMA, Bollinger).
 *   Skala wartosci pokrywa sie ze skala ceny instrumentu.
 * - [OSCILLATOR] — rysowany w osobnym panelu pod wykresem (RSI, MACD,
 *   Stochastic). Ma wlasna skale (np. RSI: 0-100), wiec nakladanie na
 *   wykres ceny dawaloby nieczytelny obraz.
 *
 * UI partycjonuje [IndicatorResult] po tym typie — bez `when` po nazwie,
 * bez `if (name == "RSI")`. Nowy oscylator dopisany w domenie sam
 * trafi w panel oscylatorow.
 */
enum class IndicatorType { OVERLAY, OSCILLATOR }
