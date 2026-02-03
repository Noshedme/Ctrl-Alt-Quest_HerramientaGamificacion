package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.Mission; // Necesario para leer recompensas reales
import com.ctrlaltquest.ui.utils.SoundManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.StageStyle;

import java.util.List;

public class GameService {

    private static final GameService instance = new GameService();

    private GameService() {}

    public static GameService getInstance() {
        return instance;
    }

    /**
     * Procesa un evento de actividad (ej: 1 segundo programando).
     * Es el "cerebro" que conecta el monitoreo con la base de datos y la UI.
     * * @param userId ID del usuario
     * @param metricKey Clave de la métrica (ej: "time_coding")
     * @param value Cantidad a sumar al progreso
     */
    public void processActivityEvent(int userId, String metricKey, int value) {
        // 1. Actualizar progresos en BD y obtener lista de misiones que llegaron al 100%
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
        // A. Obtener datos reales de la misión (XP y Monedas) antes de cerrar
        Mission mission = MissionsDAO.getMisionById(missionId);
        
        if (mission == null) {
            System.err.println("❌ Error: Se completó la misión ID " + missionId + " pero no se pudo recuperar info.");
            return;
        }

        // B. Marcar como completa en BD (Auto-complete)
        // Nota: Si prefieres que el usuario deba dar click a "Reclamar" manualmente en la UI,
        // deberías mover la llamada a otorgarRecompensas() al controlador de la vista, no aquí.
        // Aquí asumimos un flujo automático o "Notificación de listo".
        MissionsDAO.completarMision(missionId);

        // C. Otorgar recompensas en la cuenta del usuario
        boolean levelUp = UserDAO.otorgarRecompensas(userId, mission.getXpReward(), mission.getCoinReward());

        System.out.println("✅ Misión completada: " + mission.getTitle());

        // D. Notificar a la UI (Debe ser en el hilo de JavaFX)
        Platform.runLater(() -> {
            // 1. Sonido
            SoundManager.playSuccessSound();
            
            // 2. Alerta Visual (Simple)
            // Idealmente esto sería una notificación tipo "Toast" no intrusiva.
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initStyle(StageStyle.UTILITY);
            alert.setTitle("¡Misión Completada!");
            alert.setHeaderText(mission.getTitle());
            alert.setContentText("Recompensa: +" + mission.getXpReward() + " XP, " + mission.getCoinReward() + " Monedas.");
            alert.show();

            // 3. Manejo de Subida de Nivel
            if (levelUp) {
                SoundManager.playLevelUpSound();
                
                Alert lvlAlert = new Alert(Alert.AlertType.INFORMATION);
                lvlAlert.setTitle("¡LEVEL UP!");
                lvlAlert.setHeaderText("¡HAS SUBIDO DE NIVEL!");
                lvlAlert.setContentText("Tus atributos han aumentado. ¡Sigue programando!");
                lvlAlert.show();
            }
        });
    }
}