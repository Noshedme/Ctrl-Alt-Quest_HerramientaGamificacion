package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Event;
import com.ctrlaltquest.models.EventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EventsDAO - Data Access Object para gestionar eventos en la base de datos.
 */
public class EventsDAO {
    
    /**
     * Registra un nuevo evento en la BD.
     * 
     * @param userId ID del usuario
     * @param eventType Tipo de evento
     * @param description Descripción
     * @param target Objetivo
     * @param xpReward XP de recompensa
     * @param coinReward Monedas de recompensa
     * @return ID del evento creado o -1 si falló
     */
    public static int createEvent(int userId, EventType eventType, String description, 
                                   int target, int xpReward, int coinReward) {
        String sql = "INSERT INTO public.events (user_id, type, description, trigger, occurred_at, handled) " +
                    "VALUES (?, ?, ?, ?::jsonb, NOW(), false) RETURNING id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String trigger = String.format("{\"type\":\"%s\", \"target\":%d, \"current\":0, \"xp_reward\":%d, \"coin_reward\":%d}",
                                          eventType.code, target, xpReward, coinReward);
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, eventType.code);
            pstmt.setString(3, description);
            pstmt.setString(4, trigger);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int eventId = rs.getInt("id");
                System.out.println("✅ Evento creado: " + eventType.displayName + " (ID: " + eventId + ")");
                return eventId;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error creando evento: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Obtiene un evento activo por ID.
     * 
     * @param eventId ID del evento
     * @return Event o null si no existe
     */
    public static Event getEventById(int eventId) {
        String sql = "SELECT id, user_id, type, description, trigger, occurred_at, handled " +
                    "FROM public.events WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                EventType type = EventType.fromCode(rs.getString("type"));
                String trigger = rs.getString("trigger");
                int target = extractJsonIntValue(trigger, "target");
                int xpReward = extractJsonIntValue(trigger, "xp_reward");
                int coinReward = extractJsonIntValue(trigger, "coin_reward");
                
                Event event = new Event(rs.getInt("user_id"), type, rs.getString("description"), target, xpReward, coinReward);
                event.setId(rs.getInt("id"));
                
                return event;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo evento: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene eventos activos (no completados) del usuario.
     * 
     * @param userId ID del usuario
     * @return Lista de eventos activos
     */
    public static List<Event> getActiveEvents(int userId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT id, user_id, type, description, trigger, occurred_at, handled " +
                    "FROM public.events WHERE user_id = ? AND handled = false ORDER BY occurred_at DESC LIMIT 5";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                EventType type = EventType.fromCode(rs.getString("type"));
                String trigger = rs.getString("trigger");
                int target = extractJsonIntValue(trigger, "target");
                int xpReward = extractJsonIntValue(trigger, "xp_reward");
                int coinReward = extractJsonIntValue(trigger, "coin_reward");
                
                Event event = new Event(rs.getInt("user_id"), type, rs.getString("description"), target, xpReward, coinReward);
                event.setId(rs.getInt("id"));
                events.add(event);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo eventos activos: " + e.getMessage());
        }
        
        return events;
    }
    
    /**
     * Completa un evento (actualiza handled=true y outcome).
     * 
     * @param eventId ID del evento
     * @param completed ¿Se completó exitosamente?
     * @param finalProgress Progreso final (0-100)
     */
    public static void completeEvent(int eventId, boolean completed, int finalProgress) {
        String sql = "UPDATE public.events SET handled = true, outcome = ?::jsonb WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String outcome = String.format("{\"completed\":%b, \"progress\":%d}", completed, finalProgress);
            pstmt.setString(1, outcome);
            pstmt.setInt(2, eventId);
            
            pstmt.executeUpdate();
            System.out.println("✅ Evento completado: ID " + eventId + " (" + (completed ? "exitoso" : "fallido") + ")");
        } catch (SQLException e) {
            System.err.println("❌ Error completando evento: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el tiempo (segundos) desde el último evento del usuario.
     * 
     * @param userId ID del usuario
     * @return Segundos desde el último evento
     */
    public static long getSecondsSinceLastEvent(int userId) {
        String sql = "SELECT EXTRACT(EPOCH FROM (NOW() - MAX(occurred_at))) as seconds " +
                    "FROM public.events WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Object secondsObj = rs.getObject("seconds");
                if (secondsObj != null) {
                    return (long) ((Number) secondsObj).doubleValue();
                }
                return 999999; // No hay eventos previos
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error obteniendo tiempo desde último evento: " + e.getMessage());
        }
        
        return 999999;
    }
    
    /**
     * Obtiene estadísticas de eventos del usuario.
     * 
     * @param userId ID del usuario
     * @return String con estadísticas
     */
    public static String getEventStats(int userId) {
        String sql = "SELECT COUNT(*) as total, " +
                    "SUM(CASE WHEN handled = true THEN 1 ELSE 0 END) as completed, " +
                    "SUM(CASE WHEN outcome->>'completed' = 'true' THEN 1 ELSE 0 END) as successful " +
                    "FROM public.events WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total");
                int completed = rs.getInt("completed");
                int successful = rs.getInt("successful");
                return String.format("Total: %d, Completados: %d, Exitosos: %d", total, completed, successful);
            }
        } catch (SQLException e) {
            System.err.println("⚠️  Error obteniendo estadísticas: " + e.getMessage());
        }
        
        return "Sin estadísticas";
    }
    
    /**
     * Extrae un valor entero de un JSON string de forma segura.
     */
    private static int extractJsonIntValue(String jsonString, String key) {
        try {
            if (jsonString == null) return 0;
            
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
