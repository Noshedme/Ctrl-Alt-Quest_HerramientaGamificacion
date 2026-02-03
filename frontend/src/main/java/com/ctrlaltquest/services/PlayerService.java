package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.AuthDAO;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PlayerService {

    private final AuthDAO authDAO = new AuthDAO();
    
    // Mapa para definir los multiplicadores de XP por clase
    // ID 1: Programador, ID 2: Escritor, ID 3: Analista
    private static final Map<Integer, Double> CLASS_XP_BONUS = new HashMap<>();

    static {
        CLASS_XP_BONUS.put(1, 1.20); // +20% para Programadores
        CLASS_XP_BONUS.put(2, 1.15); // +15% para Escritores
        CLASS_XP_BONUS.put(3, 1.10); // +10% para Analistas
    }

    /**
     * Vincula la clase elegida al usuario y asigna el equipo/misiones iniciales.
     */
    public boolean finalizeCharacterSelection(int userId, int classId) {
        try {
            // 1. Actualizar la clase en la tabla users
            authDAO.updateUserClass(userId, classId);

            // 2. Asignar misiones iniciales dependiendo de la clase
            assignInitialMissions(userId, classId);

            AuditService.log(userId, "CLASS_SELECTED", "Clase ID: " + classId + " vinculada correctamente.");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error al finalizar selección de personaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula la XP final aplicando el bono de clase si la actividad corresponde.
     */
    public int calculateEarnedXP(int classId, int baseXP, String appCategory) {
        double multiplier = 1.0;

        // Lógica de bonificación:
        if (classId == 1 && "Development".equalsIgnoreCase(appCategory)) {
            multiplier = CLASS_XP_BONUS.get(1);
        } else if (classId == 2 && "Writing".equalsIgnoreCase(appCategory)) {
            multiplier = CLASS_XP_BONUS.get(2);
        } else if (classId == 3 && "Office".equalsIgnoreCase(appCategory)) {
            multiplier = CLASS_XP_BONUS.get(3);
        }

        return (int) (baseXP * multiplier);
    }

    /**
     * Inserta las misiones iniciales en la base de datos según la clase.
     */
    private void assignInitialMissions(int userId, int classId) throws SQLException {
        // Aquí llamarías a tu DAO de misiones. 
        // Ejemplo conceptual de misiones por clase:
        switch (classId) {
            case 1: // Programador
                insertMission(userId, "El Compilador Arcano", "Pasa 2 horas en un IDE", 200, 50, "Development");
                break;
            case 2: // Escritor
                insertMission(userId, "Escriba Real", "Escribe 1000 palabras en documentos", 150, 40, "Writing");
                break;
            case 3: // Analista
                insertMission(userId, "Maestro de Datos", "Analiza hojas de cálculo por 1 hora", 180, 60, "Office");
                break;
        }
    }

    private void insertMission(int userId, String title, String desc, int xp, int coins, String category) {
        // Aquí usarías el SQL: INSERT INTO missions (user_id, title, category, xp_reward, coin_reward...)
        System.out.println("⚔️ Misión asignada: " + title + " para el usuario " + userId);
    }
    
    /**
     * Verifica si el usuario sube de nivel tras ganar XP.
     */
    public boolean checkLevelUp(int currentXP, int currentLevel) {
        int nextLevelXP = currentLevel * 1000; // Ejemplo: Nivel 1 necesita 1000, Nivel 2 necesita 2000...
        return currentXP >= nextLevelXP;
    }
}