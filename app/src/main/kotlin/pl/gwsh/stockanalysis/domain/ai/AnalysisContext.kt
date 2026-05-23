package pl.gwsh.stockanalysis.domain.ai

import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.model.Candle
import pl.gwsh.stockanalysis.domain.model.Range
import pl.gwsh.stockanalysis.domain.model.Stock

/**
 * Snapshot danych przekazywanych do [buildSystemInstruction]. Pojedyncze
 * miejsce, w ktorym ChatViewModel zbiera wszystko, co Gemini powinno
 * widziec o instrumencie: metadane, swiece, wskazniki.
 *
 * `stock` moze byc null jezeli wyszukiwarka nie zwrocila metadanych
 * (np. uzytkownik wszedl po deep linku, a Twelve Data search jest puste).
 * W kontekscie wpisujemy wtedy tylko symbol.
 */
data class AnalysisContext(
    val symbol: String,
    val stock: Stock?,
    val range: Range,
    val candles: List<Candle>,
    val indicators: List<IndicatorResult>,
)
