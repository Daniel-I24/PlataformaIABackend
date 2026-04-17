package com.aiplatform.repository;

import com.aiplatform.model.Plan;
import com.aiplatform.model.UserQuota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria para las cuotas de usuario.
 *
 * Usa ConcurrentHashMap para thread-safety en accesos concurrentes.
 * computeIfAbsent() es atómico: evita race conditions al crear usuarios nuevos.
 */
@Repository
public class UserQuotaRepository {

    private static final Logger log = LoggerFactory.getLogger(UserQuotaRepository.class);

    private final ConcurrentHashMap<String, UserQuota> store = new ConcurrentHashMap<>();

    public UserQuotaRepository() {
        initializeSampleUsers();
    }

    /**
     * Busca la cuota de un usuario; si no existe la crea con plan FREE.
     * computeIfAbsent es atómico: garantiza una sola instancia por userId.
     */
    public UserQuota findOrCreate(String userId) {
        return store.computeIfAbsent(userId, id -> {
            log.info("New user registered: '{}' with FREE plan", id);
            return new UserQuota(id, Plan.FREE);
        });
    }

    /** Guarda o actualiza la cuota de un usuario. */
    public void save(UserQuota userQuota) {
        if (userQuota == null) throw new IllegalArgumentException("userQuota no puede ser nulo");
        store.put(userQuota.getUserId(), userQuota);
    }

    /** Retorna todos los usuarios registrados (usado por los schedulers). */
    public Collection<UserQuota> findAll() {
        return store.values();
    }

    /** Número de usuarios registrados (usado en logs de los schedulers). */
    public int countUsers() {
        return store.size();
    }

    // ---- Datos de prueba ----------------------------------------------------

    private void initializeSampleUsers() {
        store.put("user-free",       new UserQuota("user-free",       Plan.FREE));
        store.put("user-pro",        new UserQuota("user-pro",        Plan.PRO));
        store.put("user-enterprise", new UserQuota("user-enterprise", Plan.ENTERPRISE));
        log.info("Sample users initialized: user-free (FREE), user-pro (PRO), user-enterprise (ENTERPRISE)");
    }
}
