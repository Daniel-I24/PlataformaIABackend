package com.aiplatform.service;

import com.aiplatform.dto.GenerationRequest;
import com.aiplatform.dto.GenerationResponse;

/**
 * Contrato del servicio de generación de texto con IA.
 *
 * Es la interfaz central del Patrón Proxy: tanto el servicio real
 * (MockAIGenerationService) como los proxies (RateLimitProxyService,
 * QuotaProxyService) la implementan, lo que permite encadenarlos de
 * forma transparente para el controlador.
 */
public interface AIGenerationService {

    /**
     * Genera texto a partir de un prompt.
     *
     * @param request solicitud con userId y prompt
     * @return respuesta con texto generado y metadatos de consumo
     */
    GenerationResponse generate(GenerationRequest request);
}
