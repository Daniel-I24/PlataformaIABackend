package com.aiplatform.dto;

import java.time.LocalDate;

/**
 * Estado actual de la cuota de un usuario.
 * Retornado por GET /api/quota/status
 */
public record QuotaStatusResponse(
    String    userId,
    String    plan,
    int       tokensUsed,
    int       tokensRemaining,
    int       maxTokensPerMonth,
    LocalDate resetDate,
    int       requestsThisMinute,
    int       maxRequestsPerMinute
) {}
