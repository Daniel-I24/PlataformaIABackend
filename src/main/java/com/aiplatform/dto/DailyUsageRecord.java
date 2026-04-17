package com.aiplatform.dto;

import java.time.LocalDate;

/**
 * Registro de uso de tokens en un día específico.
 * Usado en el historial de los últimos 7 días.
 */
public record DailyUsageRecord(
    LocalDate date,
    int       tokensUsed
) {}
