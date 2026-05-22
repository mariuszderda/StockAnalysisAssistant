package pl.gwsh.stockanalysis.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Stub. W Fazie 5 podpinamy klienta Gemini (`GeminiClient` z domain.ai).
 * Wybór SDK (deprecated `com.google.ai.client.generativeai` vs Firebase AI Logic vs
 * `com.google.genai:google-genai`) finalizujemy przed Fazą 5.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule
