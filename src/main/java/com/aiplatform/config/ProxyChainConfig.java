package com.aiplatform.config;

import com.aiplatform.repository.UserQuotaRepository;
import com.aiplatform.service.AIGenerationService;
import com.aiplatform.service.MockAIGenerationService;
import com.aiplatform.service.proxy.QuotaProxyService;
import com.aiplatform.service.proxy.RateLimitProxyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Ensambla la cadena de proxies del Patrón Proxy.
 *
 * Cadena resultante (de afuera hacia adentro):
 *   RateLimitProxyService → QuotaProxyService → MockAIGenerationService
 *
 * @Primary hace que Spring inyecte este bean cuando se pide un AIGenerationService,
 * en lugar del MockAIGenerationService directamente. El controlador no sabe
 * que está hablando con proxies — solo conoce la interfaz AIGenerationService.
 *
 * Construcción de adentro hacia afuera:
 *   1. MockAIGenerationService (servicio real, ya es bean de Spring)
 *   2. QuotaProxyService       (envuelve al real)
 *   3. RateLimitProxyService   (envuelve al proxy de cuota → entrada de la cadena)
 */
@Configuration
public class ProxyChainConfig {

    @Bean
    @Primary
    public AIGenerationService aiGenerationServiceProxyChain(
            MockAIGenerationService realService,
            UserQuotaRepository     userQuotaRepository) {

        AIGenerationService quotaProxy     = new QuotaProxyService(realService,    userQuotaRepository);
        AIGenerationService rateLimitProxy = new RateLimitProxyService(quotaProxy, userQuotaRepository);

        return rateLimitProxy;
    }
}
