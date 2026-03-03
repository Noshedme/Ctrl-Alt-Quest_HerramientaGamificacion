package com.ctrlaltquest.services;

import java.util.List;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.Mission; 
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.application.Platform;

public class GameService {

    private static final GameService instance = new GameService();

    private GameService() {}

    public static GameService getInstance() {
        return instance;
    }

    /**
     * Procesa un evento de actividad (ej: 1 segundo programando).
     * Es el "cerebro" que conecta el monitoreo con la base de datos y la UI.
     * @param userId ID del usuario
     * @param metricKey Clave de la métrica (ej: "time_coding")
     * @param value Cantidad a sumar al progreso
     */
    public void processActivityEvent(int userId, String metricKey, int value) {
        // 1. Actualizar progresos en BD y obtener lista de misiones que llegaron al 100%
        //    (El DAO ya maneja la lógica de buscar en mission_progress)
        List<Integer> completedMissions = MissionsDAO.actualizarProgreso(userId, metricKey, value);

        // 2. Si hubo misiones completadas en este "tick", procesar recompensas
        for (int missionId : completedMissions) {
            completarYRecompensar(userId, missionId);
        }
    }

    /**
     * Maneja la lógica de finalización de misión: BD, Recompensas y UI.
     */
    private void completarYRecompensar(int userId, int missionId) {
        // A. Obtener datos reales de la misión (XP y Monedas) para mostrar en la alerta
        Mission mission = MissionsDAO.getMisionById(missionId);
        
        if (mission == null) {
            System.err.println("❌ Error: Se completó la misión ID " + missionId + " pero no se pudo recuperar info.");
            return;
        }

        // B. Marcar como completa en BD (Auto-complete) para ESTE usuario
        // 🔥 CORRECCIÓN: Usamos reclamarMision pasando el userId
        MissionsDAO.reclamarMision(userId, missionId);

        // C. Otorgar recompensas en la cuenta del usuario
        boolean levelUp = UserDAO.otorgarRecompensas(userId, mission.getXpReward(), mission.getCoinReward());

        System.out.println("✅ Misión completada automáticamente: " + mission.getTitle());

        // D. Notificar a la UI (Debe ser en el hilo de JavaFX)
        Platform.runLater(() -> {
            // 1. Sonido
            SoundManager.playSuccessSound();
            
            // 2. Notificación visual con Toast
            Toast.success("¡Misión Completada!", mission.getTitle() + "\nRecompensa: +" + mission.getXpReward() + " XP, " + mission.getCoinReward() + " Monedas.");

            // 3. Manejo de Subida de Nivel
            if (levelUp) {
                SoundManager.playLevelUpSound();
                Toast.success("¡LEVEL UP!", "¡HAS SUBIDO DE NIVEL! Tus atributos han aumentado. ¡Sigue programando!");
            }
        });
    }
}