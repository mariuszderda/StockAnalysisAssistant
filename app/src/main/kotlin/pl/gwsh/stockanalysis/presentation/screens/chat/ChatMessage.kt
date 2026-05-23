package pl.gwsh.stockanalysis.presentation.screens.chat

import pl.gwsh.stockanalysis.domain.ai.GeminiError

/**
 * Pojedyncza wiadomosc w widoku czatu. `id` sluzy LazyColumn jako klucz —
 * monotonicznie rosnacy, generowany przez ChatViewModel.
 *
 * `Assistant` ma dwa warianty: zwykla odpowiedz tekstowa albo blad —
 * dzieki temu ViewModel pozostaje language-agnostic, a UI mapuje
 * [GeminiError] na lokalizowany string przez `Composable` mapper.
 */
sealed class ChatMessage {
    abstract val id: Long

    data class User(override val id: Long, val text: String) : ChatMessage()

    sealed class Assistant : ChatMessage() {
        data class Text(override val id: Long, val text: String) : Assistant()
        data class Error(override val id: Long, val error: GeminiError) : Assistant()
    }
}
