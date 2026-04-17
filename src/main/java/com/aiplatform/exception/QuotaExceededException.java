package com.aiplatform.exception;

/**
 * Lanzada cuando un usuario agota su cuota mensual de tokens.
 * El GlobalExceptionHandler la convierte en HTTP 402 Payment Required.
 */
public class QuotaExceededException extends RuntimeException {

    private final int maxTokens;
    private final int tokensUsed;

    public QuotaExceededException(String message, int maxTokens, int tokensUsed) {
        super(message);
        this.maxTokens  = maxTokens;
        this.tokensUsed = tokensUsed;
    }

    public int getMaxTokens()  { return maxTokens;  }
    public int getTokensUsed() { return tokensUsed; }
}
