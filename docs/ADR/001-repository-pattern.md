# ADR-001 — Repository Pattern dla warstwy danych

**Status:** Accepted
**Data:** 2026-05-23
**Faza:** 2 (data layer)

## Kontekst

Aplikacja czyta dane OHLC i wyszukuje tickery przez Twelve Data (REST) oraz
trzyma cache w Room (offline fallback, ochrona przed wyczerpaniem 8 req/min /
800 req/dzień). ViewModel mógłby wstrzykiwać `TwelveDataApi` i DAO bezpośrednio
i sklejać logikę "najpierw cache, w razie braku — sieć". To rozjedzie się przy
trzecim ekranie i nie da się tego sensownie testować — każdy ViewModel musiałby
mockować cztery rzeczy. Komisja chce zobaczyć świadomie zastosowany wzorzec.

## Decyzja

Wystawiamy interfejs `StockRepository` w warstwie `domain.repository`. Jedyna
implementacja `StockRepositoryImpl` (`@Singleton`) żyje w `data.repository` i
łączy dwa źródła: `TwelveDataApi` (Retrofit) + trzy DAO Room (`CandleDao`,
`StockMetaDao`, `FavoriteDao`). Polityka: cache-first z TTL **24h**
(`CACHE_TTL_MS` w companion object), graceful offline fallback (przy
`IOException` a niepustym cache → zwracamy cache jako `Result.success`).
Wszystkie metody `suspend` zwracają `Result<T>` — w `failure` siedzi
`DataErrorException(error: DataError)` z typowanym sealed class, dzięki czemu
warstwa prezentacji może sterować UI exhaustive when'em.

## Konsekwencje

- ViewModel widzi tylko interfejs `StockRepository` (Faza 3) — w testach
  podstawia się fake bez stawiania Retrofitu i Room.
- Polityka cache (TTL, fallback, `forceRefresh`) jest w jednym pliku
  (`StockRepositoryImpl`). Zmiana TTL = jedna stała.
- Warstwa `domain` jest JVM-pure: zero importów z `retrofit2.*`,
  `androidx.room.*`, `com.squareup.moshi.*`. Sprawdzalne grepem.
- Testy repository używają **MockK** dla DAO + **MockWebServer** dla API +
  podstawionego `Clock.fixed(...)` dla TTL — bez zegara systemowego i bez
  prawdziwej sieci. 11 scenariuszy dla `getCandles` + 4 dla `searchSymbols`.
- `Result<T>` to standard biblioteczny Kotlina — nie wprowadzamy własnego
  `Outcome<T>`. Cena: `Result.failure` wymaga `Throwable`, więc dodaliśmy
  cienki `DataErrorException(val error: DataError)` jako wrapper.

## Odrzucone alternatywy

**UseCase per akcja (np. `GetCandlesUseCase`, `SearchSymbolsUseCase`).** W MVP
to dwie metody — wprowadzenie warstwy UseCase generuje sześć dodatkowych klas
i interfejsów, nic nie dając poza zwiększeniem powierzchni do mockowania.
Komisja zaakceptuje brak UseCase w pracy inżynierskiej, jeśli uzasadniony
prostotą domeny (CRUD nad tickerami).

**Osobne repository per ekran (`SearchRepository`, `ChartRepository`,
`FavoritesRepository`).** Duplikuje cache (każdy musiałby trzymać własny DAO),
rozjedzie się przy pierwszej zmianie polityki. Jeden `StockRepository` nad
jednym schematem Room jest spójniejszy.

**Brak repository — Retrofit + DAO wstrzykiwane wprost do ViewModelu.** Każdy
nowy ViewModel musi reimplementować cache-first; testy stają się sklejaniem
mocków pięciu klas; cała polityka cache rozjeżdża się po `viewModelScope`.
