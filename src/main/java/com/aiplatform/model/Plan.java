package com.aiplatform.model;

/**
 * Planes de suscripción disponibles en la plataforma.
 *
 * Cada plan define dos límites:
 * - maxRequestsPerMinute: cuántas llamadas puede hacer el usuario por minuto.
 * - maxTokensPerMonth:    cuántos tokens puede consumir en el mes.
 *
 * ENTERPRISE usa Integer.MAX_VALUE como señal de "sin límite"; el código
 * lo detecta con isUnlimited() para evitar comparaciones mágicas dispersas.
 */
public enum Plan {

    FREE      (10,              50_000),
    PRO       (60,             500_000),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int maxRequestsPerMinute;
    private final int maxTokensPerMonth;

    Plan(int maxRequestsPerMinute, int maxTokensPerMonth) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxTokensPerMonth    = maxTokensPerMonth;
    }

    public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    public int getMaxTokensPerMonth()    { return maxTokensPerMonth;    }

    /** Retorna true solo para ENTERPRISE (sin límites de ningún tipo). */
    public boolean isUnlimited() {
        return this == ENTERPRISE;
    }
}
