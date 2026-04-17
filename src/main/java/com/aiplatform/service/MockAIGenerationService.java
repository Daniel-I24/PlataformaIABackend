package com.aiplatform.service;

import com.aiplatform.dto.GenerationRequest;
import com.aiplatform.dto.GenerationResponse;
import com.aiplatform.repository.UserQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Servicio real de generación de IA (simulado).
 *
 * Es el "RealSubject" del Patrón Proxy. Su única responsabilidad es
 * generar texto — no conoce nada de rate limiting ni cuotas.
 *
 * Simula un modelo de IA real con:
 * - Delay de 1200 ms para imitar el tiempo de procesamiento.
 * - Respuestas predefinidas variadas sobre tecnología.
 * - Cálculo de tokens basado en longitud del texto (1 token ≈ 4 chars).
 */
@Service("mockAIGenerationService")
public class MockAIGenerationService implements AIGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MockAIGenerationService.class);

    private static final long PROCESSING_DELAY_MS = 1_200;
    private static final int  CHARS_PER_TOKEN     = 4;
    private static final int  MIN_TOKENS          = 1;

    private static final List<String> RESPONSES = List.of(
        "El aprendizaje automático permite a los sistemas aprender y mejorar automáticamente a partir de la experiencia sin ser programados explícitamente para cada tarea.",
        "Los patrones de diseño son soluciones reutilizables a problemas comunes. El patrón Proxy actúa como intermediario controlando el acceso a otro objeto, permitiendo agregar funcionalidad como caché, logging o control de acceso.",
        "La programación orientada a objetos se basa en cuatro pilares: encapsulamiento, herencia, polimorfismo y abstracción. Estos principios permiten crear código modular, reutilizable y fácil de mantener.",
        "Spring Boot simplifica el desarrollo Java con configuración automática y servidor embebido, reduciendo significativamente el tiempo de configuración inicial del proyecto.",
        "Los microservicios estructuran una aplicación como servicios pequeños e independientes. Cada servicio se ejecuta en su propio proceso y se comunica mediante APIs bien definidas.",
        "React permite construir interfaces de usuario reactivas con componentes reutilizables. El Virtual DOM optimiza las actualizaciones de la UI para mejor rendimiento.",
        "La IA generativa puede crear texto, imágenes y código a partir de patrones aprendidos durante el entrenamiento con grandes conjuntos de datos.",
        "El rate limiting controla la tasa de solicitudes que un usuario puede hacer a un servicio, protegiendo los recursos del servidor y garantizando equidad entre usuarios.",
        "Los algoritmos BFS y DFS son fundamentales en ciencias de la computación. BFS explora nivel por nivel con una cola; DFS profundiza en cada rama con una pila o recursión.",
        "La computación en la nube ofrece recursos de TI bajo demanda con pago por uso. Los modelos IaaS, PaaS y SaaS ofrecen diferentes niveles de abstracción y control.",
        "Docker empaqueta el código con todas sus dependencias en contenedores, garantizando que la aplicación funcione igual en cualquier entorno de ejecución.",
        "Las bases de datos NoSQL como Redis, MongoDB y Cassandra ofrecen alternativas flexibles a las relacionales, optimizadas para caché, documentos o series temporales."
    );

    private final UserQuotaRepository userQuotaRepository;
    private final Random random = new Random();

    public MockAIGenerationService(UserQuotaRepository userQuotaRepository) {
        this.userQuotaRepository = userQuotaRepository;
    }

    /**
     * Genera texto simulado:
     * 1. Simula el delay de procesamiento del modelo.
     * 2. Selecciona una respuesta aleatoria.
     * 3. Calcula y registra los tokens consumidos.
     * 4. Retorna la respuesta con metadatos de consumo.
     */
    @Override
    public GenerationResponse generate(GenerationRequest request) {
        log.debug("Generating for user='{}', promptLength={}", request.userId(), request.prompt().length());

        simulateProcessingDelay();

        String generatedText  = RESPONSES.get(random.nextInt(RESPONSES.size()));
        int    tokensConsumed = calculateTokens(request.prompt(), generatedText);

        var userQuota = userQuotaRepository.findOrCreate(request.userId());
        userQuota.consumeTokens(tokensConsumed);

        log.debug("Generated for user='{}': {}t consumed, {}t remaining",
            request.userId(), tokensConsumed, userQuota.getRemainingTokens());

        return new GenerationResponse(
            generatedText,
            tokensConsumed,
            userQuota.getRemainingTokens(),
            userQuota.getRequestsThisMinute(),
            userQuota.getPlan().getMaxRequestsPerMinute(),
            userQuota.getPlan().name()
        );
    }

    // ---- Métodos auxiliares privados ----------------------------------------

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(PROCESSING_DELAY_MS);
        } catch (InterruptedException e) {
            // Restauro el estado de interrupción según las buenas prácticas de Java
            Thread.currentThread().interrupt();
            log.warn("Processing delay interrupted on thread: {}", Thread.currentThread().getName());
        }
    }

    /** 1 token ≈ 4 caracteres (aproximación estándar GPT). */
    private int calculateTokens(String prompt, String response) {
        int promptTokens   = Math.max(MIN_TOKENS, prompt.length()   / CHARS_PER_TOKEN);
        int responseTokens = Math.max(MIN_TOKENS, response.length() / CHARS_PER_TOKEN);
        return promptTokens + responseTokens;
    }
}
