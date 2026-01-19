package com.ctrlaltquest.dao;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.services.SessionManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CharacterDAO {

    /**
     * Recupera todos los personajes de un usuario específico incluyendo XP, Monedas y Racha.
     */
    public static Map<Integer, Character> getCharactersByUser(int userId) {
        Map<Integer, Character> map = new HashMap<>();
        
        if (userId <= 0) {
            userId = SessionManager.getInstance().getUserId();
        }
        
        if (userId <= 0) return map;

        // SELECT actualizado con los campos de gamificación
        String query = "SELECT id, name, class_id, user_id, level, slot_index, current_xp, coins, health_streak " +
                       "FROM characters WHERE user_id = ? ORDER BY slot_index";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return map;

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Character c = new Character();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setClassId(rs.getInt("class_id"));
                    c.setUserId(rs.getInt("user_id"));
                    c.setLevel(rs.getInt("level"));
                    c.setSlotIndex(rs.getInt("slot_index"));
                    
                    // Mapeo de nuevos campos requeridos por el HomeController
                    c.setCurrentXp(rs.getInt("current_xp"));
                    c.setCoins(rs.getInt("coins"));
                    c.setHealthStreak(rs.getInt("health_streak"));
                    
                    map.put(c.getSlotIndex(), c);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener personajes: " + e.getMessage());
        }
        return map;
    }

    /**
     * Guarda o actualiza un objeto Character completo.
     */
    public static boolean saveCharacter(Character c) {
        int userId = c.getUserId();
        if (userId <= 0) userId = SessionManager.getInstance().getUserId();

        // SQL actualizado para incluir persistencia de progreso
        String query = "INSERT INTO characters (user_id, name, class_id, slot_index, level, current_xp, coins, health_streak) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                       "ON CONFLICT (user_id, slot_index) DO UPDATE SET " +
                       "name = EXCLUDED.name, " +
                       "class_id = EXCLUDED.class_id, " +
                       "level = EXCLUDED.level, " +
                       "current_xp = EXCLUDED.current_xp, " +
                       "coins = EXCLUDED.coins, " +
                       "health_streak = EXCLUDED.health_streak";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return false;

            ps.setInt(1, userId);
            ps.setString(2, c.getName());
            ps.setInt(3, c.getClassId());
            ps.setInt(4, c.getSlotIndex());
            ps.setInt(5, c.getLevel());
            ps.setInt(6, c.getCurrentXp());
            ps.setInt(7, c.getCoins());
            ps.setInt(8, c.getHealthStreak());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al salvar personaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea un personaje nuevo inicializando valores por defecto (XP 0, Monedas 0).
     */
    public static boolean createCharacter(int userId, String name, int classId, int slotIndex) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false); 

            // Insertamos con valores iniciales de nivel 1 y 0 progreso
            String sqlChar = "INSERT INTO characters (user_id, name, class_id, slot_index, level, current_xp, coins, health_streak) " +
                             "VALUES (?, ?, ?, ?, 1, 0, 0, 0) RETURNING id";
            
            int characterId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlChar)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setInt(3, classId);
                ps.setInt(4, slotIndex);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) characterId = rs.getInt(1);
            }

            if (characterId != -1) {
                // Sincronizamos la clase en la tabla de usuarios para preferencias globales
                String sqlUpdateUser = "UPDATE users SET selected_class_id = ? WHERE id = ?";
                try (PreparedStatement psUp = conn.prepareStatement(sqlUpdateUser)) {
                    psUp.setInt(1, classId);
                    psUp.setInt(2, userId);
                    psUp.executeUpdate();
                }

                conn.commit();
                System.out.println("✨ Personaje '" + name + "' creado exitosamente con stats base.");
                return true;
            }
            
            conn.rollback();
            return false;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { }
            System.err.println("❌ Error en createCharacter: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { }
        }
    }

    /**
     * Elimina un personaje por su ID único.
     */
    public static boolean deleteCharacter(int characterId) {
        String query = "DELETE FROM characters WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return false;
            
            ps.setInt(1, characterId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar personaje: " + e.getMessage());
            return false;
        }
    }
}