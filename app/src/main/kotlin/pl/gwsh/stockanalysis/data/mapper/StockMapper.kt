package pl.gwsh.stockanalysis.data.mapper

import pl.gwsh.stockanalysis.data.remote.dto.SymbolMatchDto
import pl.gwsh.stockanalysis.domain.model.Stock

/**
 * Mapuje pojedynczy wynik `/symbol_search` na domenowy [Stock]. Pola nullable
 * w DTO (czesc tickerow GPW nie ma `currency`, niektore ETF nie maja
 * `instrument_type`) defaultuja do pustego stringa — domena nie modeluje
 * "nieznane" jako null, bo to upraszcza UI.
 */
fun SymbolMatchDto.toStock(): Stock = Stock(
    symbol = symbol,
    name = instrumentName.orEmpty(),
    exchange = exchange.orEmpty(),
    micCode = micCode.orEmpty(),
    currency = currency.orEmpty(),
    type = instrumentType.orEmpty(),
)
