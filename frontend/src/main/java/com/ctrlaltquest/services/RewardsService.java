package com.ctrlaltquest.services;

import com.ctrlaltquest.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RewardsService - Servicio centralizado para otorgar XP, monedas y completar logros.
 * Se integra con el monitoreo de actividad para dar recompensas automáticas.
 */
public class RewardsService {
    
    private static RewardsService instance;
    
    private RewardsService() {}
    
    public static synchronized RewardsService getInstance() {
        if (instance == null) {
            instance = new RewardsService();
        }
        return instance;
    }

    /**
     * Otorga XP al usuario por período de actividad productiva.
     * Se llama cada 1 segundo si está en modo productivo.
     * Cada 10 segundos productivos = 1 XP
     * 
     * @param userId ID del usuario
     * @param isProductive ¿Es actividad productiva?
     * @return XP otorgado en esta acción
     */
    public int awardXPForActivity(int userId, boolean isProductive) {
        if (!isProductive) return 0;
        return awardXPForActivity(userId, isProductive, 1);
    }
    
    /**
     * Otorga cantidad específica de XP al usuario.
     * Versión overload para eventos que otorgan XP custom.
     * 
     * @param userId ID del usuario
     * @param isProductive ¿Es actividad productiva?
     * @param xpAmount Cantidad de XP a otorgar
     * @return XP otorgado
     */
    public int awardXPForActivity(int userId, boolean isProductive, int xpAmount) {
        if (!isProductive || xpAmount <= 0) return 0;
        
        int xpAwarded = 0;
        
        String sql = "UPDATE public.users SET current_xp = current_xp + ?, total_xp = total_xp + ? " +
                     "WHERE id = ? RETURNING current_xp, total_xp";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, xpAmount);
            pstmt.setInt(2, xpAmount);
            pstmt.setInt(3, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int currentXP = rs.getInt("current_xp");
                int totalXP = rs.getInt("total_xp");
                xpAwarded = xpAmount;
                
                System.out.println("✨ [RewardsService] XP Otorgado: +" + xpAmount + " XP | Total: " + totalXP);
                
                // Verificar si el usuario sube de nivel
                checkLevelUp(userId, currentXP);
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error otorgando XP: " + e.getMessage());
        }
        
        return xpAwarded;
    }

    /**
     * Otorga monedas al usuario por misión completada.
     * 
     * @param userId ID del usuario
     * @param missionId ID de la misión
     * @param coins Cantidad de monedas a otorgar
     */
    public void awardCoinsForMission(int userId, int missionId, int coins) {
        if (coins <= 0) return;
        
        String sql = "INSERT INTO public.coin_transactions (user_id, amount, reason, ref_type, ref_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP); " +
                     "UPDATE public.users SET coins = coins + ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // INSERT coin_transactions
            pstmt.setInt(1, userId);
            pstmt.setInt(2, coins);
            pstmt.setString(3, "Misión completada");
            pstmt.setString(4, "MISSION");
            pstmt.setInt(5, missionId);
            
            // UPDATE users
            pstmt.setInt(6, coins);
            pstmt.setInt(7, userId);
            
            pstmt.execute();
            System.out.println("💰 [RewardsService] Monedas Otorgadas: +" + coins + " | Misión #" + missionId);
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error otorgando monedas: " + e.getMessage());
        }
    }

    /**
     * Verifica si el usuario sube de nivel y otorga bonificaciones.
     * 
     * @param userId ID del usuario
     * @param currentXP XP actual del usuario
     */
    private void checkLevelUp(int userId, int currentXP) {
        // XP requerido por nivel: 100 * nivel
        // Nivel 1: 0-100, Nivel 2: 100-300, Nivel 3: 300-600, etc.
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT level FROM public.users WHERE id = ?")) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int currentLevel = rs.getInt("level");
                int xpRequiredForNext = (currentLevel + 1) * 100;
                
                if (currentXP >= xpRequiredForNext) {
                    // Subir de nivel
                    try (PreparedStatement upLevel = conn.prepareStatement(
                         "UPDATE public.users SET level = level + 1 WHERE id = ?")) {
                        
                        upLevel.setInt(1, userId);
                        upLevel.executeUpdate();
                        
                        System.out.println("⬆️  [RewardsService] ¡NIVEL SUBIDO! Nuevo nivel: " + (currentLevel + 1));
                        
                        // Bonus: 50 monedas por subir de nivel
                        awardCoinsForMission(userId, -1, 50);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando level up: " + e.getMessage());
        }
    }

    /**
     * Otorga un logro al usuario si aún no lo tiene.
     * 
     * @param userId ID del usuario
     * @param achievementId ID del logro
     * @return true si fue otorgado, false si ya lo tenía
     */
    public boolean awardAchievement(int userId, int achievementId) {
        // Verificar si ya tiene el logro
        String checkSql = "SELECT id FROM public.user_achievements WHERE user_id = ? AND achievement_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, achievementId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                // Ya tiene el logro
                return false;
            }
            
            // No lo tiene, otorgarlo
            String insertSql = "INSERT INTO public.user_achievements (user_id, achievement_id, unlocked_at) " +
                              "VALUES (?, ?, CURRENT_TIMESTAMP)";
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, achievementId);
                insertStmt.executeUpdate();
                
                System.out.println("🏆 [RewardsService] Logro Desbloqueado: ID #" + achievementId);
                
                // Obtener información del logro para recompensas
                String achievementSql = "SELECT xp_reward, coin_reward FROM public.achievements WHERE id = ?";
                try (PreparedStatement achStmt = conn.prepareStatement(achievementSql)) {
                    achStmt.setInt(1, achievementId);
                    ResultSet achRs = achStmt.executeQuery();
                    
                    if (achRs.next()) {
                        int xpReward = achRs.getInt("xp_reward");
                        int coinReward = achRs.getInt("coin_reward");
                        
                        // Otorgar recompensas del logro
                        if (xpReward > 0) {
                            String xpSql = "UPDATE public.users SET current_xp = current_xp + ?, total_xp = total_xp + ? WHERE id = ?";
                            try (PreparedStatement xpStmt = conn.prepareStatement(xpSql)) {
                                xpStmt.setInt(1, xpReward);
                                xpStmt.setInt(2, xpReward);
                                xpStmt.setInt(3, userId);
                                xpStmt.executeUpdate();
                                System.out.println("  └─ ✨ XP Bonus del Logro: +" + xpReward);
                            }
                        }
                        
                        if (coinReward > 0) {
                            awardCoinsForMission(userId, achievementId, coinReward);
                            System.out.println("  └─ 💰 Coin Bonus del Logro: +" + coinReward);
                        }
                    }
                }
                
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error otorgando logro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica condiciones de logros y los desbloquea automáticamente.
     * Se llama después de completar misiones o alcanzar hitos.
     * 
     * @param userId ID del usuario
     */
    public void checkAndAwardAchievements(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // Obtener logros no desbloqueados aún
            String sql = "SELECT a.id, a.condition " +
                        "FROM public.achievements a " +
                        "WHERE a.id NOT IN (SELECT achievement_id FROM public.user_achievements WHERE user_id = ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    int achievementId = rs.getInt("id");
                    String conditionJson = rs.getString("condition");
                    
                    // Evaluar condiciones (simplificado para ejemplo)
                    if (evaluateAchievementCondition(userId, conditionJson, conn)) {
                        awardAchievement(userId, achievementId);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando logros: " + e.getMessage());
        }
    }

    /**
     * Evalúa las condiciones de un logro usando JSON parsing seguro.
     * Las condiciones están almacenadas en formato JSONB.
     * 
     * @param userId ID del usuario
     * @param conditionJson JSON con condiciones
     * @param conn Conexión a BD
     * @return true si se cumplen las condiciones
     */
    private boolean evaluateAchievementCondition(int userId, String conditionJson, Connection conn) {
        // Ejemplos de condiciones:
        // {"type": "missions_completed", "count": 10}
        // {"type": "total_xp", "amount": 1000}
        // {"type": "consecutive_days", "days": 7}
        // {"type": "level_reached", "level": 5}
        
        try {
            // Parse seguro usando índices
            if (conditionJson.contains("missions_completed")) {
                int requiredCount = extractJsonIntValue(conditionJson, "count");
                String countSql = "SELECT COUNT(*) as cnt FROM public.missions WHERE user_id = ? AND completed = true";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("cnt") >= requiredCount;
                    }
                }
            }
            
            if (conditionJson.contains("total_xp")) {
                int requiredXp = extractJsonIntValue(conditionJson, "amount");
                String xpSql = "SELECT total_xp FROM public.users WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(xpSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("total_xp") >= requiredXp;
                    }
                }
            }
            
            if (conditionJson.contains("level_reached")) {
                int requiredLevel = extractJsonIntValue(conditionJson, "level");
                String levelSql = "SELECT level FROM public.users WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(levelSql)) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("level") >= requiredLevel;
                    }
                }
            }
            
            if (conditionJson.contains("consecutive_days")) {
                // Implementación simplificada: dias desde primer login
                int requiredDays = extractJsonIntValue(conditionJson, "days");
                return requiredDays <= 1; // Por ahora, simplificado
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Error evaluando condición: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Extrae un valor entero de un JSON string de forma segura.
     * @param jsonString JSON string
     * @param key Clave a buscar
     * @return Valor extraído
     */
    private int extractJsonIntValue(String jsonString, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = jsonString.indexOf(searchKey);
            if (keyIndex == -1) return 0;
            
            int colonIndex = jsonString.indexOf(":", keyIndex);
            int commaIndex = jsonString.indexOf(",", colonIndex);
            int braceIndex = jsonString.indexOf("}", colonIndex);
            
            int endIndex = commaIndex > 0 && commaIndex < braceIndex ? commaIndex : braceIndex;
            if (endIndex == -1) endIndex = jsonString.length();
            
            String valueStr = jsonString.substring(colonIndex + 1, endIndex).trim();
            return Integer.parseInt(valueStr);
        } catch (Exception e) {
            System.err.println("⚠️  Error extrayendo JSON value para key '" + key + "': " + e.getMessage());
            return 0;
        }
    }
}
