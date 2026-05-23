package pl.gwsh.stockanalysis.domain.ai

import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult

/**
 * Buduje deterministyczna, polskojezyczna instrukcje systemowa dla Gemini
 * na podstawie [AnalysisContext]. Pure function — bez I/O, bez Clocka,
 * latwo testowalna.
 *
 * Konwencja CLAUDE.md § Gemini boundaries:
 *  - kontekst zawsze przez [buildSystemInstruction], nigdy klejone stringi
 *    w ViewModelu;
 *  - krotki: ostatnie 10 swiec + biezace wartosci wskaznikow;
 *  - jezyk: polski; ton: rzeczowy; disclaimer wymuszony w instrukcji.
 *
 * Format outputu jest stabilny (mockable), bo ChatViewModelTest opiera
 * sie na obecnosci konkretnych fragmentow w wyniku.
 */
fun buildSystemInstruction(ctx: AnalysisContext): String = buildString {
    appendLine("Jestes asystentem analizy technicznej akcji dla aplikacji Stock Analysis Assistant.")
    appendLine("Odpowiadaj po polsku, rzeczowo, w 3-6 zdaniach.")
    appendLine("Nie udzielaj porad inwestycyjnych. Nie zalecaj kupna ani sprzedazy.")
    appendLine("Nie zgaduj danych spoza dostarczonego kontekstu. Jezeli pytanie wykracza poza ten kontekst, powiedz to wprost.")
    appendLine()

    appendLine("KONTEKST INSTRUMENTU:")
    appendLine("- Symbol: ${ctx.symbol}")
    ctx.stock?.let { s ->
        if (s.name.isNotBlank()) appendLine("- Nazwa: ${s.name}")
        if (s.exchange.isNotBlank()) appendLine("- Gielda: ${s.exchange}")
        if (s.currency.isNotBlank()) appendLine("- Waluta: ${s.currency}")
    }
    appendLine("- Zakres danych: ${rangeLabel(ctx.range.name)}")
    appendLine("- Liczba swiec w kontekscie: ${ctx.candles.size}")
    appendLine()

    if (ctx.candles.isNotEmpty()) {
        appendLine("OSTATNIE SWIECE (data | open | high | low | close | volume):")
        val tail = ctx.candles.takeLast(LAST_CANDLES)
        for (c in tail) {
            appendLine(
                "- ${c.date} | ${fmt(c.open)} | ${fmt(c.high)} | ${fmt(c.low)} | ${fmt(c.close)} | ${c.volume}"
            )
        }
        appendLine()
    }

    if (ctx.indicators.isNotEmpty()) {
        appendLine("WSKAZNIKI TECHNICZNE (biezaca wartosc):")
        for (ind in ctx.indicators) {
            appendLine(formatIndicator(ind))
        }
        appendLine()
    }

    appendLine("Odpowiadaj odwolujac sie do liczb powyzej, jezeli to wlasciwe. Jezeli wartosc wskaznika jest pusta, powiedz, ze brak danych.")
}

private const val LAST_CANDLES = 10

private fun rangeLabel(rangeName: String): String = when (rangeName) {
    "ONE_MONTH" -> "1 miesiac"
    "THREE_MONTHS" -> "3 miesiace"
    "ONE_YEAR" -> "1 rok"
    else -> rangeName
}

private fun fmt(v: Double): String = "%.2f".format(v)
private fun fmt(v: Double?): String = v?.let { "%.2f".format(it) } ?: "brak"

private fun formatIndicator(result: IndicatorResult): String = when (result) {
    is IndicatorResult.SingleLine -> {
        val last = result.values.lastOrNull { it != null }
        "- ${result.name}: ${fmt(last)}"
    }
    is IndicatorResult.MultiLine -> {
        val parts = result.lines.entries.joinToString(separator = ", ") { (key, line) ->
            val last = line.lastOrNull { it != null }
            "$key=${fmt(last)}"
        }
        "- ${result.name}: $parts"
    }
}
