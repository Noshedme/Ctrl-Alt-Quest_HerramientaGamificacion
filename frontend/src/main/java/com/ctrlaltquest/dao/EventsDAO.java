package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Event;
import com.ctrlaltquest.models.EventType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * EventsDAO - Data Access Object para gestionar los eventos inmersivos en la base de datos.
 * Maneja el almacenamiento JSON y el historial de combates/retos.
 */
public class EventsDAO {
    
    /**
     * Registra un nuevo evento en la base de datos.
     * Escapa caracteres especiales para evitar errores al guardar el JSON.
     */
    public static int createEvent(int userId, EventType eventType, String description, 
                                   int target, int xpReward, int coinReward, String imagePath) {
        
        String sql = "INSERT INTO public.events (user_id, type, description, trigger, occurred_at, handled) " +
                    "VALUES (?, ?, ?, ?::jsonb, NOW(), false) RETURNING id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Sanitizar la ruta de la imagen por si acaso (escapar barras y comillas)
            String safeImagePath = imagePath != null ? imagePath.replace("\"", "\\\"") : "";
            
            // Construcción segura del JSON
            String trigger = String.format("{\"type\":\"%s\", \"target\":%d, \"current\":0, \"xp_reward\":%d, \"coin_reward\":%d, \"image_url\":\"%s\"}",
                                          eventType.code, target, xpReward, coinReward, safeImagePath);
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, eventType.code);
            pstmt.setString(3, description);
            pstmt.setString(4, trigger);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int eventId = rs.getInt("id");
                // Log silencioso (dejamos que EventsService haga el ruido épico)
                System.out.println("💾 [DB] Evento guardado en registro: ID " + eventId);
                return eventId;
            }
        } catch (SQLException e) {
            System.err.println("❌ [DB ERROR] Falló la creación del evento: " + e.getMessage());
        }
        return -1;
    }
    
    /**
     * Recupera un evento específico desde la base de datos.
     */
    public static Event getEventById(int eventId) {
        String sql = "SELECT id, user_id, type, description, trigger, occurred_at, handled " +
                    "FROM public.events WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return buildEventFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ [DB ERROR] Obteniendo evento: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Recupera todos los eventos activos (no manejados/resueltos) de un usuario.
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
                Event event = buildEventFromResultSet(rs);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DB ERROR] Obteniendo eventos activos: " + e.getMessage());
        }
        return events;
    }
    
    /**
     * Método auxiliar para evitar repetir código al construir el objeto Event.
     */
    private static Event buildEventFromResultSet(ResultSet rs) throws SQLException {
        EventType type = EventType.fromCode(rs.getString("type"));
        if (type == null) return null; // Prevenir crasheos por tipos antiguos
        
        String trigger = rs.getString("trigger");
        
        int target = extractJsonIntValue(trigger, "target");
        int xpReward = extractJsonIntValue(trigger, "xp_reward");
        int coinReward = extractJsonIntValue(trigger, "coin_reward");
        String img = extractJsonStringValue(trigger, "image_url");
        
        Event event = new Event(rs.getInt("user_id"), type, rs.getString("description"), target, xpReward, coinReward, img);
        event.setId(rs.getInt("id"));
        
        return event;
    }
    
    /**
     * Marca un evento como completado y actualiza su progreso final.
     */
    public static void completeEvent(int eventId, boolean completed, int finalProgress) {
        String sql = "UPDATE public.events SET handled = true, outcome = ?::jsonb WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String outcome = String.format("{\"completed\":%b, \"progress\":%d, \"finished_at\":\"%s\"}", 
                                           completed, finalProgress, java.time.Instant.now().toString());
            pstmt.setString(1, outcome);
            pstmt.setInt(2, eventId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ [DB ERROR] Completando evento: " + e.getMessage());
        }
    }
    
    /**
     * Verifica cuánto tiempo ha pasado desde la última vez que el usuario fue atacado o tuvo un evento.
     * Útil para el sistema de "Cooldown".
     */
    public static long getSecondsSinceLastEvent(int userId) {
        String sql = "SELECT EXTRACT(EPOCH FROM (NOW() - MAX(occurred_at))) as seconds FROM public.events WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Object secondsObj = rs.getObject("seconds");
                if (secondsObj != null) {
                    return (long) ((Number) secondsObj).doubleValue();
                }
            }
        } catch (SQLException e) { 
            // Ignoramos silenciosamente, simplemente devolvemos un número alto
        }
        return 999999; // Si nunca ha tenido eventos, está listo para uno.
    }
    
    /**
     * Obtiene un resumen estadístico de las batallas y eventos del usuario.
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
                return String.format("Aventuras Totales: %d | Superadas: %d | Victorias: %d", 
                                     rs.getInt("total"), rs.getInt("completed"), rs.getInt("successful"));
            }
        } catch (SQLException e) { }
        return "El historial está vacío.";
    }
    
    // ==========================================
    // PARSERS MANUALES SEGUROS
    // ==========================================
    
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
            // Limpiar posibles comillas si se colaron
            valueStr = valueStr.replace("\"", ""); 
            return Integer.parseInt(valueStr);
        } catch (Exception e) { 
            return 0; 
        }
    }

    private static String extractJsonStringValue(String json, String key) {
        try {
            if (json == null) return null;
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;
            
            int valueStart = json.indexOf("\"", keyIndex + searchKey.length());
            if (valueStart == -1) return null;
            
            int valueEnd = json.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return null;
            
            return json.substring(valueStart + 1, valueEnd);
        } catch (Exception e) { 
            return null; 
        }
    }
}