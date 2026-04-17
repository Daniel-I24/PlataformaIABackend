package com.aiplatform.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado de cuota y rate-limit de un usuario en memoria.
 *
 * Decisiones de thread-safety:
 * - AtomicInteger     → contadores sin locks explícitos.
 * - ConcurrentHashMap → historial diario con acceso concurrente seguro.
 * - volatile          → campos leídos/escritos desde múltiples hilos.
 */
public class UserQuota {

    private final String userId;

    /** volatile: puede cambiar por upgrade desde otro hilo. */
    private volatile Plan plan;

    private final AtomicInteger tokensUsedThisMonth = new AtomicInteger(0);
    private final AtomicInteger requestsThisMinute  = new AtomicInteger(0);

    /** volatile: el scheduler lo actualiza desde otro hilo. */
    private volatile LocalDate monthlyResetDate;

    /**
     * Historial de tokens consumidos por día.
     * merge() en ConcurrentHashMap es atómico por clave.
     */
    private final Map<LocalDate, Integer> dailyUsageHistory = new ConcurrentHashMap<>();

    public UserQuota(String userId, Plan plan) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId no puede ser nulo o vacío");
        }
        if (plan == null) {
            throw new IllegalArgumentException("plan no puede ser nulo");
        }
        this.userId           = userId;
        this.plan             = plan;
        this.monthlyResetDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
    }

    // ---- Getters ------------------------------------------------------------

    public String    getUserId()              { return userId;                    }
    public Plan      getPlan()                { return plan;                      }
    public int       getTokensUsedThisMonth() { return tokensUsedThisMonth.get(); }
    public int       getRequestsThisMinute()  { return requestsThisMinute.get();  }
    public LocalDate getMonthlyResetDate()    { return monthlyResetDate;          }

    /** Vista no modificable del historial para evitar mutaciones externas. */
    public Map<LocalDate, Integer> getDailyUsageHistory() {
        return Collections.unmodifiableMap(dailyUsageHistory);
    }

    // ---- Operaciones de negocio ---------------------------------------------

    /**
     * Registra el consumo de tokens en el mes y en el historial diario.
     * merge() en ConcurrentHashMap es atómico por clave.
     */
    public void consumeTokens(int tokens) {
        if (tokens <= 0) return;
        tokensUsedThisMonth.addAndGet(tokens);
        dailyUsageHistory.merge(LocalDate.now(), tokens, Integer::sum);
    }

    /** Incrementa el contador de requests del minuto actual. */
    public int incrementRequestCount() {
        return requestsThisMinute.incrementAndGet();
    }

    /** Resetea el contador de requests por minuto (llamado por el scheduler). */
    public void resetMinuteCounter() {
        requestsThisMinute.set(0);
    }

    /** Resetea la cuota mensual de tokens (llamado el primer día del mes). */
    public void resetMonthlyQuota() {
        tokensUsedThisMonth.set(0);
        monthlyResetDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
    }

    /** Actualiza el plan del usuario. volatile garantiza visibilidad inmediata. */
    public void setPlan(Plan newPlan) {
        if (newPlan == null) throw new IllegalArgumentException("plan no puede ser nulo");
        this.plan = newPlan;
    }

    /** Tokens restantes en el mes. ENTERPRISE retorna Integer.MAX_VALUE. */
    public int getRemainingTokens() {
        if (plan.isUnlimited()) return Integer.MAX_VALUE;
        return Math.max(0, plan.getMaxTokensPerMonth() - tokensUsedThisMonth.get());
    }

    /** Verifica si hay suficientes tokens para el request estimado. */
    public boolean hasTokensAvailable(int requiredTokens) {
        return plan.isUnlimited() || getRemainingTokens() >= requiredTokens;
    }

    /** Verifica si el usuario está dentro del límite de requests/minuto. */
    public boolean isWithinRateLimit() {
        return plan.isUnlimited() || requestsThisMinute.get() < plan.getMaxRequestsPerMinute();
    }

    @Override
    public String toString() {
        return String.format("UserQuota{userId='%s', plan=%s, tokensUsed=%d, reqThisMin=%d}",
            userId, plan, tokensUsedThisMonth.get(), requestsThisMinute.get());
    }
}
