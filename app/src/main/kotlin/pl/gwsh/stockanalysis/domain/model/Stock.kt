package pl.gwsh.stockanalysis.domain.model

/**
 * Reprezentuje pojedynczy instrument finansowy zwracany przez wyszukiwarkę
 * tickerów (np. AAPL, CDR.WA). Pole `type` jest swobodnym stringiem z API
 * Twelve Data ("Common Stock", "ETF", "Index", ...) — w MVP nie modelujemy
 * tego jako enum, bo lista typow nie jest stabilna i nie wplywa na logike UI.
 */
data class Stock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val micCode: String,
    val currency: String,
    val type: String,
)
