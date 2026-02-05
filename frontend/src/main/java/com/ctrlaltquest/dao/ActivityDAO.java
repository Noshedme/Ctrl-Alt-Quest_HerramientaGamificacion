package com.ctrlaltquest.dao;

import java.sql.Connection; // Import corregido
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ctrlaltquest.db.DatabaseConnection;

public class ActivityDAO {

    public static int iniciarSesion(int userId) {
        // Usamos RETURNING id para obtener la clave primaria generada por serial
        String sql = "INSERT INTO public.activity_sessions (user_id, session_start) VALUES (?, CURRENT_TIMESTAMP) RETURNING id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1); 
            }
        } catch (SQLException e) {
            System.err.println("Error al iniciar sesión en BD: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static void cerrarSesion(int sessionId) {
        if (sessionId == -1) return;
        
        // Calculamos el tiempo total restando el inicio del tiempo actual
        String sql = "UPDATE public.activity_sessions SET session_end = CURRENT_TIMESTAMP, " +
                     "total_screen_time = (CURRENT_TIMESTAMP - session_start) WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, sessionId);
            pstmt.executeUpdate();
            // Actualizar el total de tiempo de juego acumulado en users y la racha de salud
            String updateUserSql = "UPDATE public.users u SET " +
                    "total_play_time = COALESCE(u.total_play_time, INTERVAL '0') + (a.session_end - a.session_start), " +
                    "health_streak = CASE " +
                    "WHEN (u.last_sync::date = CURRENT_DATE) THEN u.health_streak " +
                    "WHEN (u.last_sync::date = (CURRENT_DATE - INTERVAL '1 day')) THEN COALESCE(u.health_streak,0) + 1 " +
                    "ELSE 1 END, " +
                    "last_sync = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                    "FROM public.activity_sessions a WHERE a.id = ? AND u.id = a.user_id";

            try (PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql)) {
                updateUserStmt.setInt(1, sessionId);
                updateUserStmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error actualizando usuario al cerrar sesión: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar sesión en BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra un tick de actividad en app_usage_logs.
     * Se llama cada 1 segundo desde ActivityMonitorService.
     * 
     * @param userId ID del usuario (no usado directamente, se obtiene de session_id)
     * @param appName Nombre de la aplicación activa (almacenado como app_id)
     * @param metricKey Clave de métrica (no usado en schema actual)
     */
    public static void registrarActividad(int userId, String appName, String metricKey) {
        // Obtener session_id del usuario desde activity_sessions
        String sessionSql = "SELECT id FROM public.activity_sessions WHERE user_id = ? ORDER BY session_start DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement sessionStmt = conn.prepareStatement(sessionSql)) {
            
            sessionStmt.setInt(1, userId);
            ResultSet sessionRs = sessionStmt.executeQuery();
            
            if (sessionRs.next()) {
                int sessionId = sessionRs.getInt("id");
                
                // ✅ CORREGIDO: Obtener app_id válido o NULL en lugar de usar hash
                Integer appId = null;
                
                if (appName != null && !appName.isEmpty()) {
                    // Buscar si existe una app con este nombre
                    String findAppSql = "SELECT id FROM public.apps WHERE LOWER(name) = LOWER(?) LIMIT 1";
                    try (PreparedStatement findStmt = conn.prepareStatement(findAppSql)) {
                        findStmt.setString(1, appName);
                        ResultSet appRs = findStmt.executeQuery();
                        
                        if (appRs.next()) {
                            appId = appRs.getInt("id");
                        } else {
                            // Si no existe, crear una entrada genérica
                            String createAppSql = "INSERT INTO public.apps (name, category, is_productive) " +
                                                "VALUES (?, 'UNKNOWN', false) RETURNING id";
                            try (PreparedStatement createStmt = conn.prepareStatement(createAppSql)) {
                                createStmt.setString(1, appName);
                                ResultSet createdRs = createStmt.executeQuery();
                                if (createdRs.next()) {
                                    appId = createdRs.getInt("id");
                                }
                            }
                        }
                    }
                }
                
                // Insertar el log de actividad con app_id válido o NULL
                String logSql = "INSERT INTO public.app_usage_logs (session_id, app_id, start_time, duration) " +
                               "VALUES (?, ?, CURRENT_TIMESTAMP, INTERVAL '1 second')";
                
                try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                    logStmt.setInt(1, sessionId);
                    if (appId != null) {
                        logStmt.setInt(2, appId);
                    } else {
                        logStmt.setNull(2, java.sql.Types.INTEGER);
                    }
                    logStmt.executeUpdate();
                }
                // Acumular 1 segundo al total_play_time del usuario (actualiza last_sync y updated_at)
                String updateUserTimeSql = "UPDATE public.users SET " +
                        "total_play_time = COALESCE(total_play_time, INTERVAL '0') + INTERVAL '1 second', " +
                        "last_sync = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

                try (PreparedStatement updateUserTimeStmt = conn.prepareStatement(updateUserTimeSql)) {
                    updateUserTimeStmt.setInt(1, userId);
                    updateUserTimeStmt.executeUpdate();
                } catch (SQLException e) {
                    // No crítico; evitar spam en consola
                }
            }
            
        } catch (SQLException e) {
            // Log silencioso para no saturar console (ocurre cada segundo)
            System.err.println("⚠️  Error registrando actividad: " + e.getMessage());
        }
    }
}