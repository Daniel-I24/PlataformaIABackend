package com.aiplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de generación de texto.
 * Usa Java record para inmutabilidad y eliminación de boilerplate.
 */
public record GenerationRequest(

    @NotBlank(message = "El userId no puede estar vacío")
    String userId,

    @NotBlank(message = "El prompt no puede estar vacío")
    @Size(max = 4000, message = "El prompt no puede superar los 4000 caracteres")
    String prompt
) {}
