package com.ctrlaltquest.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ctrlaltquest.db.DatabaseConnection;

/**
 * MissionProgressService - Servicio que actualiza misiones automáticamente basado en actividad.
 * Se integra con el monitoreo de actividad para progresar misiones en tiempo real.
 */
public class MissionProgressService {
    
    private static MissionProgressService instance;
    private final RewardsService rewardsService = RewardsService.getInstance();
    private final EventsService eventsService = EventsService.getInstance();
    
    private MissionProgressService() {}
    
    public static synchronized MissionProgressService getInstance() {
        if (instance == null) {
            instance = new MissionProgressService();
        }
        return instance;
    }

    public void captureActivityManual(int userId, String appName, boolean isProductive) {
        System.out.println("🎯 [MissionProgressService] Captura MANUAL de actividad");
        System.out.println("   └─ Usuario: " + userId);
        System.out.println("   └─ App: " + appName);
        System.out.println("   └─ Productiva: " + isProductive);
        
        processActivityEvent(userId, appName, isProductive);
    }

    public void processActivityEvent(int userId, String appName, boolean isProductive) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // 1. Determinar la categoría de la app
            String category = categorizeApp(appName, isProductive);
            
            // 2. Actualizar misiones de tiempo (segundos en aplicación)
            updateTimeBasedMissions(userId, category, conn);
            
            // 3. Actualizar misiones de contadores (apps usadas, etc.)
            updateCounterBasedMissions(userId, appName, category, conn);
            
            // 4. OTORGAR XP POR ACTIVIDAD PRODUCTIVA USANDO XPSYNCSERVICE
            if (isProductive) {
                XPSyncService.getInstance().awardXPFromActivity(userId, 1, "time_" + category.toLowerCase());
            }
            
            // 5. Verificar y completar logros
            RewardsService.getInstance().checkAndAwardAchievements(userId);
            
            // 6. Verificar y generar eventos contextuales
            eventsService.checkAndGenerateEvent(userId, category);
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error procesando evento de actividad: " + e.getMessage());
        }
    }

    private String categorizeApp(String appName, boolean isProductive) {
        if (appName == null) return "UNKNOWN";
        
        String lower = appName.toLowerCase();
        
        if (lower.contains("vscode") || lower.contains("visual studio") || 
            lower.contains("intellij") || lower.contains("eclipse") || 
            lower.contains("netbeans") || lower.contains("sublime")) {
            return "CODING";
        }
        if (lower.contains("chrome") || lower.contains("firefox") || 
            lower.contains("edge") || lower.contains("safari")) {
            return "BROWSING";
        }
        if (lower.contains("word") || lower.contains("excel") || 
            lower.contains("powerpoint") || lower.contains("docs") ||
            lower.contains("sheets")) {
            return "OFFICE";
        }
        if (lower.contains("slack") || lower.contains("teams") || 
            lower.contains("discord") || lower.contains("telegram")) {
            return "COMMUNICATION";
        }
        
        return isProductive ? "PRODUCTIVITY" : "ENTERTAINMENT";
    }

    private void updateTimeBasedMissions(int userId, String category, Connection conn) throws SQLException {
        // CORRECCIÓN: No existe m.completed. Buscamos donde progress_percentage < 100
        String sql = "SELECT mp.id, mp.mission_id, mp.current_value, mp.target_value, mp.progress_percentage " +
                     "FROM public.mission_progress mp " +
                     "WHERE mp.user_id = ? " +
                     "  AND mp.progress_percentage < 100 " +
                     "  AND mp.metric_key LIKE ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, "time_" + category.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int progressId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long currentValue = rs.getLong("current_value");
                long targetValue = rs.getLong("target_value");
                
                long newValue = Math.min(currentValue + 1, targetValue);
                double newProgress = (double) newValue / targetValue * 100;
                
                String updateSql = "UPDATE public.mission_progress " +
                                  "SET current_value = ?, progress_percentage = ? " +
                                  "WHERE id = ?";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, newValue);
                    updateStmt.setDouble(2, newProgress);
                    updateStmt.setInt(3, progressId);
                    updateStmt.executeUpdate();
                }
                
                // Si se completó la misión ahora mismo
                if (newValue >= targetValue && currentValue < targetValue) {
                    completeMission(userId, missionId, conn);
                }
            }
        }
    }

    private void updateCounterBasedMissions(int userId, String appName, String category, Connection conn) throws SQLException {
        // 1. Contar apps únicas usadas en esta sesión
        String countAppsSql = "SELECT COUNT(DISTINCT app_id) as count " +
                             "FROM public.app_usage_logs aul " +
                             "JOIN public.activity_sessions a ON aul.session_id = a.id " +
                             "WHERE a.user_id = ? AND DATE(aul.start_time) = CURRENT_DATE";
        
        try (PreparedStatement pstmt = conn.prepareStatement(countAppsSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int uniqueApps = rs.getInt("count");
                updateMissionCounter(userId, "apps_used", uniqueApps, conn);
            }
        }
        
        // 2. Contar categorías únicas usadas
        String countCatSql = "SELECT COUNT(DISTINCT LOWER(a.category)) as count " +
                            "FROM public.app_usage_logs aul " +
                            "JOIN public.apps a ON aul.app_id = a.id " +
                            "JOIN public.activity_sessions act ON aul.session_id = act.id " +
                            "WHERE act.user_id = ? AND DATE(aul.start_time) = CURRENT_DATE";
        
        try (PreparedStatement pstmt = conn.prepareStatement(countCatSql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int uniqueCategories = rs.getInt("count");
                updateMissionCounter(userId, "categories_used", uniqueCategories, conn);
            }
        }
    }

    private void updateMissionCounter(int userId, String metricKey, int value, Connection conn) throws SQLException {
        // CORRECCIÓN: Filtrar por progress_percentage < 100
        String sql = "SELECT mp.id, mp.mission_id, mp.target_value " +
                     "FROM public.mission_progress mp " +
                     "WHERE mp.user_id = ? " +
                     "  AND mp.progress_percentage < 100 " +
                     "  AND mp.metric_key = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, metricKey);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int progressId = rs.getInt("id");
                int missionId = rs.getInt("mission_id");
                long targetValue = rs.getLong("target_value");
                
                double newProgress = Math.min(((double) value / targetValue) * 100, 100.0);
                
                String updateSql = "UPDATE public.mission_progress " +
                                  "SET current_value = ?, progress_percentage = ? " +
                                  "WHERE id = ?";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, value);
                    updateStmt.setDouble(2, newProgress);
                    updateStmt.setInt(3, progressId);
                    updateStmt.executeUpdate();
                }
                
                if (value >= targetValue) {
                    completeMission(userId, missionId, conn);
                }
            }
        }
    }

    private void completeMission(int userId, int missionId, Connection conn) throws SQLException {
        // Asegurarse de que en mission_progress esté al 100%
        String completeSql = "UPDATE public.mission_progress " +
                            "SET progress_percentage = 100, current_value = target_value " +
                            "WHERE mission_id = ? AND user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(completeSql)) {
            pstmt.setInt(1, missionId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
        
        // Obtener recompensas
        String rewardSql = "SELECT xp_reward, coin_reward, title FROM public.missions WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(rewardSql)) {
            pstmt.setInt(1, missionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int xpReward = rs.getInt("xp_reward");
                int coinReward = rs.getInt("coin_reward");
                String title = rs.getString("title");
                
                if (xpReward > 0) {
                    String xpSql = "UPDATE public.users SET current_xp = current_xp + ?, total_xp = total_xp + ? WHERE id = ?";
                    try (PreparedStatement xpStmt = conn.prepareStatement(xpSql)) {
                        xpStmt.setInt(1, xpReward);
                        xpStmt.setInt(2, xpReward);
                        xpStmt.setInt(3, userId);
                        xpStmt.executeUpdate();
                    }
                }
                
                if (coinReward > 0) {
                    rewardsService.awardCoinsForMission(userId, missionId, coinReward);
                }
                
                System.out.println("✅ [MissionProgressService] Misión Completada: " + title + 
                                 " | +XP: " + xpReward + " | +Monedas: " + coinReward);
            }
        }
        
        RewardsService.getInstance().checkAndAwardAchievements(userId);
    }
}