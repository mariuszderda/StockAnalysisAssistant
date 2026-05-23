package pl.gwsh.stockanalysis.presentation.screens.chat

import pl.gwsh.stockanalysis.domain.model.DataError

/**
 * Stany ekranu czatu — sealed dla exhaustive when'a w UI.
 *
 *  - [LoadingContext]: pobieramy swiece + liczymy wskazniki, zeby zbudowac
 *    kontekst dla Gemini. Bez tego nie da sie nic sensownie zapytac.
 *  - [Ready]: kontekst gotowy, mozna wysylac wiadomosci. Flaga
 *    `isSending` blokuje przycisk i pokazuje progress inline.
 *  - [ContextError]: nie udalo sie pobrac danych (np. brak sieci i pusty
 *    cache). Wyswietlamy DataError mapowany przez ErrorMessageMapper.
 *
 * Bledy z samego Gemini (Network, Blocked, Server) nie zmieniaja stanu —
 * lecisz jak assistant message w `messages`, zeby uzytkownik mogl pytac
 * dalej / sprobowac ponownie.
 */
sealed interface ChatUiState {
    val symbol: String
    val messages: List<ChatMessage>

    data class LoadingContext(
        override val symbol: String,
        override val messages: List<ChatMessage> = emptyList(),
    ) : ChatUiState

    data class Ready(
        override val symbol: String,
        override val messages: List<ChatMessage>,
        val isSending: Boolean = false,
    ) : ChatUiState

    data class ContextError(
        override val symbol: String,
        val error: DataError,
        override val messages: List<ChatMessage> = emptyList(),
    ) : ChatUiState
}
