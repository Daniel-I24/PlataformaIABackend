package com.aiplatform.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la API REST.
 *
 * Centraliza el manejo de errores para:
 * 1. No repetir lógica en cada controlador.
 * 2. Garantizar un formato de respuesta consistente.
 * 3. Registrar errores inesperados sin exponer detalles internos al cliente.
 *
 * Jerarquía de handlers (más específico → más general):
 *   RateLimitExceededException              → 429
 *   QuotaExceededException                  → 402
 *   MethodArgumentNotValidException         → 400
 *   MissingServletRequestParameterException → 400
 *   MethodArgumentTypeMismatchException     → 400
 *   IllegalArgumentException                → 400
 *   Exception (catch-all)                   → 500
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Errores de negocio del Patrón Proxy --------------------------------

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "60")
            .body(buildErrorBody(429, "RATE_LIMIT_EXCEEDED",
                "Has superado el límite de requests por minuto de tu plan.",
                Map.of("maxRequestsPerMinute", ex.getMaxRequests(),
                       "currentRequests",      ex.getCurrentRequests(),
                       "retryAfterSeconds",    60)));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException ex) {
        log.warn("Monthly quota exceeded: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(buildErrorBody(402, "QUOTA_EXCEEDED",
                "Has agotado tu cuota mensual de tokens. Considera hacer upgrade de plan.",
                Map.of("maxTokensPerMonth", ex.getMaxTokens(),
                       "tokensUsed",        ex.getTokensUsed())));
    }

    // ---- Errores de validación y parámetros ---------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest()
            .body(buildErrorBody(400, "VALIDATION_ERROR",
                "Los datos de la solicitud no son válidos.",
                Map.of("fieldErrors", fieldErrors)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
            .body(buildErrorBody(400, "MISSING_PARAMETER",
                "Falta el parámetro requerido: " + ex.getParameterName(),
                Map.of("parameterName", ex.getParameterName(),
                       "parameterType", ex.getParameterType())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
            .body(buildErrorBody(400, "TYPE_MISMATCH",
                "El parámetro '" + ex.getName() + "' tiene un tipo incorrecto.",
                Map.of("parameterName", ex.getName(),
                       "receivedValue", String.valueOf(ex.getValue()))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(buildErrorBody(400, "INVALID_ARGUMENT", ex.getMessage(), Map.of()));
    }

    // ---- Catch-all ----------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
            .body(buildErrorBody(500, "INTERNAL_SERVER_ERROR",
                "Ocurrió un error interno. Por favor intenta de nuevo.", Map.of()));
    }

    // ---- Método auxiliar ----------------------------------------------------

    private Map<String, Object> buildErrorBody(
            int status, String errorCode, String message, Map<String, Object> details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status);
        body.put("error",     errorCode);
        body.put("message",   message);
        body.put("details",   details);
        return body;
    }
}
