package pl.gwsh.stockanalysis.domain.ai

/**
 * Granica miedzy warstwa domeny a klientem Gemini. Konkretny SDK / sposob
 * komunikacji to detal implementacyjny — zmiana z REST na Firebase AI w
 * przyszlosci nie wymaga zmian w ChatViewModelu.
 *
 * Zwracamy `Result<String>`; w `failure` siedzi [GeminiException] z
 * typowanym [GeminiError]. Patrz `docs/ADR/005-gemini-integration.md`.
 */
interface GeminiClient {

    /**
     * Wysyla zapytanie do Gemini.
     *
     * @param systemInstruction kontekst (rola asystenta + dane o spolce
     *                          + wskazniki) zbudowany przez
     *                          [buildSystemInstruction]. Nie zawiera
     *                          pytania uzytkownika.
     * @param userPrompt        wolny tekst od uzytkownika.
     */
    suspend fun ask(systemInstruction: String, userPrompt: String): Result<String>
}
