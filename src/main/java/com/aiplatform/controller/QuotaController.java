package com.aiplatform.controller;

import com.aiplatform.dto.DailyUsageRecord;
import com.aiplatform.dto.QuotaStatusResponse;
import com.aiplatform.dto.UpgradePlanRequest;
import com.aiplatform.service.QuotaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de gestión de cuotas.
 *
 * GET  /api/quota/status?userId=   → estado actual de cuota
 * GET  /api/quota/history?userId=  → historial de uso (últimos 7 días)
 * POST /api/quota/upgrade          → cambio de plan
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /**
     * Estado actual de la cuota: tokens usados, restantes, plan y fecha de reset.
     * Si el usuario no existe, se crea con plan FREE.
     */
    @GetMapping("/status")
    public ResponseEntity<QuotaStatusResponse> getQuotaStatus(@RequestParam String userId) {
        return ResponseEntity.ok(quotaService.getQuotaStatus(userId));
    }

    /**
     * Historial de uso de los últimos 7 días.
     * Siempre retorna 7 registros; días sin actividad tienen 0 tokens.
     */
    @GetMapping("/history")
    public ResponseEntity<List<DailyUsageRecord>> getUsageHistory(@RequestParam String userId) {
        return ResponseEntity.ok(quotaService.getUsageHistory(userId));
    }

    /**
     * Cambia el plan de un usuario.
     * Body: { "userId": "user-free", "newPlan": "PRO" }
     * Retorna el nuevo estado de cuota después del cambio.
     */
    @PostMapping("/upgrade")
    public ResponseEntity<QuotaStatusResponse> upgradePlan(@Valid @RequestBody UpgradePlanRequest request) {
        return ResponseEntity.ok(quotaService.upgradePlan(request.userId(), request.newPlan()));
    }
}
