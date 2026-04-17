package com.aiplatform.service.proxy;

import com.aiplatform.dto.GenerationRequest;
import com.aiplatform.dto.GenerationResponse;
import com.aiplatform.exception.RateLimitExceededException;
import com.aiplatform.repository.UserQuotaRepository;
import com.aiplatform.service.AIGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy de Rate Limiting — primer eslabón de la cadena.
 *
 * Responsabilidad única: verificar que el usuario no supere el límite
 * de requests por minuto según su plan antes de pasar al siguiente proxy.
 *
 * Límites por plan:
 *   FREE       → 10 req/min  → HTTP 429 si se supera
 *   PRO        → 60 req/min  → HTTP 429 si se supera
 *   ENTERPRISE → sin límite  → siempre pasa
 *
 * Cadena: RateLimitProxy → QuotaProxy → MockAIGenerationService
 */
public class RateLimitProxyService extends BaseProxyService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitProxyService.class);

    private final UserQuotaRepository userQuotaRepository;

    public RateLimitProxyService(AIGenerationService next, UserQuotaRepository userQuotaRepository) {
        super(next);
        if (userQuotaRepository == null) throw new IllegalArgumentException("userQuotaRepository no puede ser nulo");
        this.userQuotaRepository = userQuotaRepository;
    }

    /**
     * Verifica el rate limit y delega al siguiente proxy si está dentro del límite.
     *
     * @throws RateLimitExceededException si el usuario superó su límite de req/min
     */
    @Override
    public GenerationResponse generate(GenerationRequest request) {
        var quota = userQuotaRepository.findOrCreate(request.userId());

        log.debug("Rate check user='{}': {}/{} req/min",
            request.userId(), quota.getRequestsThisMinute(), quota.getPlan().getMaxRequestsPerMinute());

        if (!quota.isWithinRateLimit()) {
            log.warn("Rate limit exceeded user='{}' plan={}: {}/{}",
                request.userId(), quota.getPlan(),
                quota.getRequestsThisMinute(), quota.getPlan().getMaxRequestsPerMinute());

            throw new RateLimitExceededException(
                "Rate limit exceeded for user: " + request.userId(),
                quota.getPlan().getMaxRequestsPerMinute(),
                quota.getRequestsThisMinute()
            );
        }

        // Incremento ANTES de delegar para evitar race conditions
        quota.incrementRequestCount();

        return next.generate(request);
    }
}
