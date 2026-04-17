package com.aiplatform.service;

import com.aiplatform.dto.DailyUsageRecord;
import com.aiplatform.dto.QuotaStatusResponse;
import com.aiplatform.model.Plan;
import com.aiplatform.model.UserQuota;
import com.aiplatform.repository.UserQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona las operaciones de cuota de los usuarios:
 * - Consulta de estado y historial.
 * - Upgrade/downgrade de plan.
 * - Tareas programadas de reset (rate limit y cuota mensual).
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private static final int HISTORY_DAYS = 7;

    private final UserQuotaRepository userQuotaRepository;

    public QuotaService(UserQuotaRepository userQuotaRepository) {
        this.userQuotaRepository = userQuotaRepository;
    }

    /**
     * Retorna el estado actual de la cuota de un usuario.
     * Si no existe, lo crea con plan FREE.
     */
    public QuotaStatusResponse getQuotaStatus(String userId) {
        validateUserId(userId);
        UserQuota quota = userQuotaRepository.findOrCreate(userId);

        return new QuotaStatusResponse(
            userId,
            quota.getPlan().name(),
            quota.getTokensUsedThisMonth(),
            quota.getRemainingTokens(),
            quota.getPlan().getMaxTokensPerMonth(),
            quota.getMonthlyResetDate(),
            quota.getRequestsThisMinute(),
            quota.getPlan().getMaxRequestsPerMinute()
        );
    }

    /**
     * Retorna el historial de uso de los últimos 7 días.
     * Siempre retorna exactamente 7 registros; días sin actividad tienen 0 tokens.
     * Orden: del día más antiguo al más reciente.
     */
    public List<DailyUsageRecord> getUsageHistory(String userId) {
        validateUserId(userId);
        UserQuota quota = userQuotaRepository.findOrCreate(userId);

        List<DailyUsageRecord> history = new ArrayList<>(HISTORY_DAYS);
        LocalDate today = LocalDate.now();

        for (int daysAgo = HISTORY_DAYS - 1; daysAgo >= 0; daysAgo--) {
            LocalDate date      = today.minusDays(daysAgo);
            int       tokens    = quota.getDailyUsageHistory().getOrDefault(date, 0);
            history.add(new DailyUsageRecord(date, tokens));
        }
        return history;
    }

    /**
     * Actualiza el plan de un usuario.
     *
     * @param userId     identificador del usuario
     * @param newPlanName nombre del plan (FREE, PRO, ENTERPRISE) — insensible a mayúsculas
     * @throws IllegalArgumentException si el plan no existe o el userId es inválido
     */
    public QuotaStatusResponse upgradePlan(String userId, String newPlanName) {
        validateUserId(userId);

        if (newPlanName == null || newPlanName.isBlank()) {
            throw new IllegalArgumentException("El nombre del plan no puede estar vacío");
        }

        Plan newPlan;
        try {
            newPlan = Plan.valueOf(newPlanName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Plan inválido: '" + newPlanName + "'. Planes disponibles: FREE, PRO, ENTERPRISE");
        }

        UserQuota quota       = userQuotaRepository.findOrCreate(userId);
        Plan      previousPlan = quota.getPlan();
        quota.setPlan(newPlan);
        userQuotaRepository.save(quota);

        log.info("Plan updated for user='{}': {} → {}", userId, previousPlan, newPlan);
        return getQuotaStatus(userId);
    }

    // ---- Tareas programadas -------------------------------------------------

    /**
     * Resetea el rate limit de todos los usuarios cada minuto.
     * Cron "0 * * * * *" → segundo 0 de cada minuto.
     */
    @Scheduled(cron = "0 * * * * *")
    public void resetAllRateLimits() {
        userQuotaRepository.findAll().forEach(UserQuota::resetMinuteCounter);
        log.debug("Rate limits reset for {} users", userQuotaRepository.countUsers());
    }

    /**
     * Resetea la cuota mensual el primer día de cada mes.
     * Cron "0 0 0 1 * *" → medianoche del día 1 de cada mes.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetAllMonthlyQuotas() {
        userQuotaRepository.findAll().forEach(UserQuota::resetMonthlyQuota);
        log.info("Monthly quotas reset for {} users", userQuotaRepository.countUsers());
    }

    // ---- Auxiliares privados ------------------------------------------------

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId no puede estar vacío");
        }
    }
}
