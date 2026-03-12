package com.ctrlaltquest.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.Mission;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.application.Platform;

public class GameService {

    private static final GameService instance = new GameService();
    private GameService() {}
    public static GameService getInstance() { return instance; }

    /**
     * Cooldown por misión completada (ms).
     * Evita que la misma misión dispare múltiples toasts si el DAO
     * la devuelve en ticks consecutivos antes de marcarse completa.
     */
    private static final long MISSION_TOAST_COOLDOWN = 10_000; // 10 segundos
    private final Map<Integer, Long> missionToastTimestamps = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════
    // PROCESAMIENTO DE ACTIVIDAD
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Procesa un tick de actividad (llamado cada segundo por ActivityMonitorService).
     * Solo notifica a la UI cuando una misión se completa, no en cada tick.
     */
    public void processActivityEvent(int userId, String metricKey, int value) {
        List<Integer> completedMissions = MissionsDAO.actualizarProgreso(userId, metricKey, value);

        for (int missionId : completedMissions) {
            // Evitar toasts duplicados para la misma misión en un período corto
            long now = System.currentTimeMillis();
            Long lastToast = missionToastTimestamps.get(missionId);
            if (lastToast != null && now - lastToast < MISSION_TOAST_COOLDOWN) continue;

            missionToastTimestamps.put(missionId, now);
            completarYRecompensar(userId, missionId);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECOMPENSAS
    // ════════════════════════════════════════════════════════════════════════

    private void completarYRecompensar(int userId, int missionId) {
        Mission mission = MissionsDAO.getMisionById(missionId);

        if (mission == null) {
            System.err.println("❌ Misión ID " + missionId + " completada pero no encontrada en BD.");
            return;
        }

        MissionsDAO.reclamarMision(userId, missionId);
        boolean levelUp = UserDAO.otorgarRecompensas(userId, mission.getXpReward(), mission.getCoinReward());

        System.out.println("✅ Misión completada: " + mission.getTitle() +
                           " | +" + mission.getXpReward() + " XP | +" + mission.getCoinReward() + " monedas");

        Platform.runLater(() -> {
            try { SoundManager.playSuccessSound(); } catch (Exception ignored) {}

            Toast.gold("¡Misión Completada!",
                mission.getTitle() + " · +" + mission.getXpReward() +
                " XP  |  +" + mission.getCoinReward() + " 💰");

            if (levelUp) {
                try { SoundManager.playLevelUpSound(); } catch (Exception ignored) {}
                Toast.success("🎉 ¡LEVEL UP!", "¡Has subido de nivel! ¡Sigue adelante!");
            }
        });
    }
}