package com.aiplatform.exception;

/**
 * Lanzada cuando un usuario supera su límite de requests por minuto.
 * El GlobalExceptionHandler la convierte en HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    private final int maxRequests;
    private final int currentRequests;

    public RateLimitExceededException(String message, int maxRequests, int currentRequests) {
        super(message);
        this.maxRequests     = maxRequests;
        this.currentRequests = currentRequests;
    }

    public int getMaxRequests()     { return maxRequests;     }
    public int getCurrentRequests() { return currentRequests; }
}
