# ADR-002 — Twelve Data jako źródło danych rynkowych

**Status:** Accepted
**Data:** 2026-05-23
**Faza:** 2 (data layer)

## Kontekst

SAA potrzebuje publicznego REST API dla dwóch operacji: wyszukiwanie tickerów
po nazwie (NASDAQ, NYSE, GPW) oraz pobieranie świec OHLC (1day, do roku
wstecz). Wymagania: darmowy plan akademicki, stabilny endpoint REST,
JSON-friendly, wsparcie dla polskich tickerów z sufiksem `.WA` (CDR.WA,
PKN.WA).

## Decyzja

Używamy **Twelve Data** (`https://api.twelvedata.com`), plan darmowy. Dwa
endpointy: `/time_series?symbol=...&interval=1day&outputsize=...` oraz
`/symbol_search?symbol=...`. Klucz API trzymany w `local.properties` (poza
repo, patrz `local.properties.example`) i czytany **wyłącznie** przez
wstrzykiwany `ApiKeys` — nigdy globalne `BuildConfig.X` w kodzie aplikacji.

## Konsekwencje

- Limit darmowy: **8 req/min, 800 req/dzień**. Testy używają MockWebServer i
  nie biją po realnym API — nie spalimy limitu w CI.
- Liczby w odpowiedzi (`open`, `high`, `low`, `close`, `volume`) są **stringami**
  — mapper `TimeSeriesValueDto.toCandle()` konwertuje jawnie i rzuca
  `NumberFormatException` / `DateTimeParseException` na złych danych,
  `StockRepositoryImpl` opakowuje to w `DataError.ParseError`.
- Rate-limit może przyjść jako **HTTP 429** *lub* jako **HTTP 200 z body**
  `{"status":"error","code":429,...}` — repository sprawdza obie ścieżki i
  mapuje na `DataError.RateLimited`. Identycznie 404 → `DataError.NotFound`.
- Polskie tickery wymagają sufiksu `.WA` (XWAR MIC). Nie modyfikujemy
  symbolu w mapperach — UI/ViewModel przekazuje to, co dostał z search.
- Data point w `meta` zawiera waluty (`currency`) — dla GPW będzie `PLN`,
  dla USA `USD`. Trzymane w `StockMetaEntity` razem ze świecami.
- Brak abstrakcji "DataSource" nad API — zmiana dostawcy oznaczałaby napisanie
  nowego `TwelveDataApi` i mapperów; akceptujemy to. Dwie implementacje
  równolegle nie są planowane.

## Odrzucone alternatywy

**Stooq.** Brak stabilnego REST API — tylko download CSV z URL-i bez
gwarancji formatu. Trudne do mockowania, łatwe do zepsucia.

**Yahoo Finance (unofficial).** Brak oficjalnego API; biblioteki community
(YQL, yfinance) łamią ToS przy programowym dostępie i bywają blokowane.
Niepoważne do pracy obronnej.

**IEX Cloud.** Płatne (brak free tier od 2024), poza budżetem projektu.

**Alpha Vantage.** Plan darmowy ma surowsze limity (5 req/min, 500/dzień) i
gorsze pokrycie GPW (część polskich tickerów wraca jako "Invalid API call").

**Cache obowiązkowy zamiast cache-first.** Wymuszałby refetch przy każdym
otwarciu ekranu — niezgodne z duchem darmowego API. Cache-first z TTL 24h
jest rozsądnym kompromisem dla horyzontu dziennych świec.
