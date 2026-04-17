package com.aiplatform.controller;

import com.aiplatform.dto.GenerationRequest;
import com.aiplatform.dto.GenerationResponse;
import com.aiplatform.service.AIGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de generación de texto con IA.
 *
 * El controlador solo conoce la interfaz AIGenerationService.
 * Spring inyecta el proxy chain completo gracias a @Primary en ProxyChainConfig,
 * por lo que cada request pasa automáticamente por:
 *   RateLimitProxy → QuotaProxy → MockAIGenerationService
 */
@RestController
@RequestMapping("/api/ai")
public class AIGenerationController {

    private final AIGenerationService aiGenerationService;

    public AIGenerationController(AIGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    /**
     * Genera texto a partir de un prompt.
     *
     * POST /api/ai/generate
     * Body: { "userId": "user-free", "prompt": "Explica el machine learning" }
     *
     * Posibles respuestas de error (manejadas por GlobalExceptionHandler):
     *   400 → prompt vacío o userId vacío
     *   429 → rate limit excedido
     *   402 → cuota mensual agotada
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerationResponse> generate(@Valid @RequestBody GenerationRequest request) {
        return ResponseEntity.ok(aiGenerationService.generate(request));
    }
}
