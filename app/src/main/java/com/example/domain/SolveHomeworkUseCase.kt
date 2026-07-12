package com.example.domain

import com.example.repository.ChatRepository

class SolveHomeworkUseCase(private val chatRepository: ChatRepository) {
    suspend fun execute(subject: String, question: String): String {
        if (question.isBlank()) {
            throw IllegalArgumentException("Question cannot be empty")
        }
        val systemInstruction = "You are Nova Academic Solver. Provide clear step-by-step educational explanations for any $subject question. Highlight formulas, theories, and final answers clearly."
        val userPrompt = "Explain step-by-step:\n$question"
        return chatRepository.generateAiResponse(
            history = listOf(com.example.model.ChatMessage(sessionId = 0, role = "user", text = userPrompt)),
            systemInstruction = systemInstruction
        )
    }
}
