package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null // e.g. "1K"
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null, // "user" or "model"
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)
