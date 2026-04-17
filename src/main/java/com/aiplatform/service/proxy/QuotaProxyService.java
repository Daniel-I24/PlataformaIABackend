package com.aiplatform.service.proxy;

import com.aiplatform.dto.GenerationRequest;
import com.aiplatform.dto.GenerationResponse;
import com.aiplatform.exception.QuotaExceededException;
import com.aiplatform.repository.UserQuotaRepository;
import com.aiplatform.service.AIGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy de Cuota Mensual — segundo eslabón de la cadena.
 *
 * Responsabilidad única: verificar que el usuario no haya agotado su cuota
 * mensual de tokens antes de pasar al servicio real de generación.
 *
 * Límites por plan:
 *   FREE       → 50 000 tokens/mes  → HTTP 402 si se agota
 *   PRO        → 500 000 tokens/mes → HTTP 402 si se agota
 *   ENTERPRISE → sin límite         → siempre pasa
 *
 * La verificación usa una estimación previa para evitar consumir recursos
 * del servicio real cuando la cuota ya está agotada. El consumo exacto
 * lo registra MockAIGenerationService después de generar la respuesta.
 *
 * Cadena: RateLimitProxy → QuotaProxy → MockAIGenerationService
 */
public class QuotaProxyService extends BaseProxyService {

    private static final Logger log = LoggerFactory.getLogger(QuotaProxyService.class);

    /** Tokens estimados de la respuesta (promedio de las respuestas predefinidas). */
    private static final int ESTIMATED_RESPONSE_TOKENS = 80;
    private static final int CHARS_PER_TOKEN           = 4;
    private static final int MIN_TOKENS                = 1;

    private final UserQuotaRepository userQuotaRepository;

    public QuotaProxyService(AIGenerationService next, UserQuotaRepository userQuotaRepository) {
        super(next);
        if (userQuotaRepository == null) throw new IllegalArgumentException("userQuotaRepository no puede ser nulo");
        this.userQuotaRepository = userQuotaRepository;
    }

    /**
     * Verifica la cuota mensual y delega al servicio real si hay tokens disponibles.
     *
     * @throws QuotaExceededException si el usuario agotó su cuota mensual
     */
    @Override
    public GenerationResponse generate(GenerationRequest request) {
        var quota           = userQuotaRepository.findOrCreate(request.userId());
        int estimatedTokens = estimateTokens(request.prompt());

        log.debug("Quota check user='{}': {}t remaining, ~{}t needed",
            request.userId(), quota.getRemainingTokens(), estimatedTokens);

        if (!quota.hasTokensAvailable(estimatedTokens)) {
            log.warn("Quota exceeded user='{}' plan={}: {}/{}t",
                request.userId(), quota.getPlan(),
                quota.getTokensUsedThisMonth(), quota.getPlan().getMaxTokensPerMonth());

            throw new QuotaExceededException(
                "Monthly quota exceeded for user: " + request.userId(),
                quota.getPlan().getMaxTokensPerMonth(),
                quota.getTokensUsedThisMonth()
            );
        }

        // Delega al servicio real — él registrará el consumo exacto
        return next.generate(request);
    }

    // ---- Auxiliares privados ------------------------------------------------

    /**
     * Estima los tokens totales del request (prompt + respuesta estimada).
     * Usa la misma fórmula que el frontend para consistencia en el estimador.
     */
    private int estimateTokens(String prompt) {
        int promptTokens = Math.max(MIN_TOKENS, prompt.length() / CHARS_PER_TOKEN);
        return promptTokens + ESTIMATED_RESPONSE_TOKENS;
    }
}
