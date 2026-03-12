package com.ctrlaltquest.services;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoginAttemptService — Controla intentos fallidos de login y bloqueos temporales.
 *
 * Funciona completamente en memoria (sin BD) para máxima velocidad.
 * El bloqueo se libera automáticamente al pasar LOCKOUT_DURATION_MS.
 *
 * Uso desde AuthDAO:
 *   LoginAttemptService svc = LoginAttemptService.getInstance();
 *   if (svc.isBlocked(identifier)) { return BLOCKED; }
 *   // ... verificar contraseña ...
 *   if (exito) svc.resetAttempts(identifier);
 *   else       svc.registerFailedAttempt(identifier);
 */
public class LoginAttemptService {

    // ── Configuración ────────────────────────────────────────────────────────
    public static final int  MAX_ATTEMPTS         = 3;
    public static final long LOCKOUT_DURATION_MS  = 60_000; // 1 minuto (de prueba)

    // ── Singleton ────────────────────────────────────────────────────────────
    private static LoginAttemptService instance;
    public static synchronized LoginAttemptService getInstance() {
        if (instance == null) instance = new LoginAttemptService();
        return instance;
    }
    private LoginAttemptService() {}

    // ── Estado interno ────────────────────────────────────────────────────────
    private static class AttemptData {
        int  count     = 0;
        long lockedAt  = 0; // 0 = no bloqueado
    }

    private final Map<String, AttemptData> attempts = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════
    // API PÚBLICA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retorna true si el identificador está bloqueado en este momento.
     * Libera el bloqueo automáticamente si ya expiró.
     */
    public boolean isBlocked(String identifier) {
        AttemptData data = attempts.get(key(identifier));
        if (data == null || data.lockedAt == 0) return false;

        if (Instant.now().toEpochMilli() - data.lockedAt >= LOCKOUT_DURATION_MS) {
            // Bloqueo expirado → limpiar
            attempts.remove(key(identifier));
            return false;
        }
        return true;
    }

    /**
     * Milisegundos que quedan de bloqueo. 0 si no está bloqueado.
     */
    public long getRemainingLockMs(String identifier) {
        AttemptData data = attempts.get(key(identifier));
        if (data == null || data.lockedAt == 0) return 0;
        long elapsed = Instant.now().toEpochMilli() - data.lockedAt;
        long remaining = LOCKOUT_DURATION_MS - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Registra un intento fallido.
     * @return true si con este intento se alcanzó el límite y se bloqueó la cuenta.
     */
    public boolean registerFailedAttempt(String identifier) {
        AttemptData data = attempts.computeIfAbsent(key(identifier), k -> new AttemptData());
        data.count++;
        System.out.println("🔐 [LoginAttemptService] Intento fallido " + data.count +
                           "/" + MAX_ATTEMPTS + " para: " + identifier);

        if (data.count >= MAX_ATTEMPTS) {
            data.lockedAt = Instant.now().toEpochMilli();
            System.out.println("🔒 [LoginAttemptService] Cuenta bloqueada: " + identifier);
            return true; // recién bloqueado → disparar email
        }
        return false;
    }

    /**
     * Cuántos intentos fallidos lleva el identificador (sin bloqueo aún).
     */
    public int getAttemptCount(String identifier) {
        AttemptData data = attempts.get(key(identifier));
        return data == null ? 0 : data.count;
    }

    /**
     * Resetea el contador tras un login exitoso.
     */
    public void resetAttempts(String identifier) {
        attempts.remove(key(identifier));
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────
    private String key(String identifier) {
        return identifier == null ? "" : identifier.toLowerCase().trim();
    }
}