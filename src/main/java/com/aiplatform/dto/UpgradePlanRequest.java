package com.aiplatform.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de cambio de plan.
 * Usado en POST /api/quota/upgrade
 */
public record UpgradePlanRequest(

    @NotBlank(message = "El userId no puede estar vacío")
    String userId,

    @NotBlank(message = "El nuevo plan no puede estar vacío")
    String newPlan
) {}
