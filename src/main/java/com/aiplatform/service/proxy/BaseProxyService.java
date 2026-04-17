package com.aiplatform.service.proxy;

import com.aiplatform.service.AIGenerationService;

/**
 * Clase base abstracta para todos los proxies del sistema.
 *
 * Implementa el Patrón Proxy mediante herencia y composición:
 * - Hereda de AIGenerationService (mismo contrato que el servicio real).
 * - Contiene una referencia al siguiente eslabón de la cadena (next).
 *
 * Cadena de proxies:
 *   RateLimitProxyService → QuotaProxyService → MockAIGenerationService
 *
 * Cada proxy concreto aplica su lógica de control y luego delega a next.
 */
public abstract class BaseProxyService implements AIGenerationService {

    /** Siguiente servicio en la cadena (otro proxy o el servicio real). */
    protected final AIGenerationService next;

    protected BaseProxyService(AIGenerationService next) {
        if (next == null) throw new IllegalArgumentException("El siguiente servicio no puede ser nulo");
        this.next = next;
    }
}
