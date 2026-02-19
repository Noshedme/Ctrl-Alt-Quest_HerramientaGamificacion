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

    public int awardXPForActivity(int userId, boolean isProductive) {
        if (!isProductive) return 0;
        return awardXPForActivity(userId, isProductive, 1);
    }
    
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
                
                checkLevelUp(userId, currentXP);
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error otorgando XP: " + e.getMessage());
        }
        
        return xpAwarded;
    }

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

    private void checkLevelUp(int userId, int currentXP) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT level FROM public.users WHERE id = ?")) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int currentLevel = rs.getInt("level");
                int xpRequiredForNext = (currentLevel + 1) * 100;
                
                if (currentXP >= xpRequiredForNext) {
                    try (PreparedStatement upLevel = conn.prepareStatement(
                         "UPDATE public.users SET level = level + 1 WHERE id = ?")) {
                        
                        upLevel.setInt(1, userId);
                        upLevel.executeUpdate();
                        
                        System.out.println("⬆️  [RewardsService] ¡NIVEL SUBIDO! Nuevo nivel: " + (currentLevel + 1));
                        
                        awardCoinsForMission(userId, -1, 50);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando level up: " + e.getMessage());
        }
    }

    public boolean awardAchievement(int userId, int achievementId) {
        String checkSql = "SELECT id FROM public.user_achievements WHERE user_id = ? AND achievement_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, achievementId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                return false; // Ya tiene el logro
            }
            
            String insertSql = "INSERT INTO public.user_achievements (user_id, achievement_id, unlocked_at) " +
                               "VALUES (?, ?, CURRENT_TIMESTAMP)";
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, achievementId);
                insertStmt.executeUpdate();
                
                System.out.println("🏆 [RewardsService] Logro Desbloqueado: ID #" + achievementId);
                
                String achievementSql = "SELECT xp_reward, coin_reward FROM public.achievements WHERE id = ?";
                try (PreparedStatement achStmt = conn.prepareStatement(achievementSql)) {
                    achStmt.setInt(1, achievementId);
                    ResultSet achRs = achStmt.executeQuery();
                    
                    if (achRs.next()) {
                        int xpReward = achRs.getInt("xp_reward");
                        int coinReward = achRs.getInt("coin_reward");
                        
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

    public void checkAndAwardAchievements(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            String sql = "SELECT a.id, a.condition " +
                         "FROM public.achievements a " +
                         "WHERE a.id NOT IN (SELECT achievement_id FROM public.user_achievements WHERE user_id = ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    int achievementId = rs.getInt("id");
                    String conditionJson = rs.getString("condition");
                    
                    if (evaluateAchievementCondition(userId, conditionJson, conn)) {
                        awardAchievement(userId, achievementId);
                    }
                }
            }
            
        } catch (SQLException e) {
            // Ya no imprimimos error de 'name' aquí si es que estaba saltando por otra consulta
            System.err.println("⚠️  Error verificando logros: " + e.getMessage());
        }
    }

    private boolean evaluateAchievementCondition(int userId, String conditionJson, Connection conn) {
        if (conditionJson == null) return false;
        try {
            if (conditionJson.contains("missions_completed")) {
                int requiredCount = extractJsonIntValue(conditionJson, "count");
                // CORRECCIÓN: Buscar progreso completado en mission_progress
                String countSql = "SELECT COUNT(*) as cnt FROM public.mission_progress WHERE user_id = ? AND progress_percentage >= 100";
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
                int requiredDays = extractJsonIntValue(conditionJson, "days");
                return requiredDays <= 1; 
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Error evaluando condición: " + e.getMessage());
        }
        
        return false;
    }
    
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
            return 0;
        }
    }
}