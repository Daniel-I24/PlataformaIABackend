package com.aiplatform.dto;

/**
 * Respuesta de generación de texto con metadatos de consumo.
 */
public record GenerationResponse(
    String generatedText,
    int    tokensConsumed,
    int    tokensRemaining,
    int    requestsThisMinute,
    int    maxRequestsPerMinute,
    String plan
) {}
