package com.ctrlaltquest.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.SessionManager;

public class CharacterDAO {

    /**
     * Recupera todos los personajes de un usuario específico incluyendo XP, Monedas, Racha y SKIN.
     */
    public static Map<Integer, Character> getCharactersByUser(int userId) {
        Map<Integer, Character> map = new HashMap<>();
        
        if (userId <= 0) {
            userId = SessionManager.getInstance().getUserId();
        }
        
        if (userId <= 0) return map;

        // SELECT actualizado para incluir 'skin'
        String query = "SELECT id, name, class_id, user_id, level, slot_index, current_xp, coins, health_streak, skin " +
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
                    
                    // Datos de gamificación
                    c.setCurrentXp(rs.getInt("current_xp"));
                    c.setCoins(rs.getInt("coins"));
                    c.setHealthStreak(rs.getInt("health_streak"));
                    
                    // DATOS DE SKIN (APARIENCIA)
                    String skin = rs.getString("skin");
                    c.setSkin(skin != null ? skin : "body_female"); // Default seguro
                    
                    map.put(c.getSlotIndex(), c);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener personajes: " + e.getMessage());
        }
        return map;
    }

    /**
     * Guarda o actualiza un objeto Character completo, incluyendo la SKIN.
     */
    public static boolean saveCharacter(Character c) {
        int userId = c.getUserId();
        if (userId <= 0) userId = SessionManager.getInstance().getUserId();

        // SQL actualizado: Incluye 'skin' en INSERT y en ON CONFLICT UPDATE
        String query = "INSERT INTO characters (user_id, name, class_id, slot_index, level, current_xp, coins, health_streak, skin) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                       "ON CONFLICT (user_id, slot_index) DO UPDATE SET " +
                       "name = EXCLUDED.name, " +
                       "class_id = EXCLUDED.class_id, " +
                       "level = EXCLUDED.level, " +
                       "current_xp = EXCLUDED.current_xp, " +
                       "coins = EXCLUDED.coins, " +
                       "health_streak = EXCLUDED.health_streak, " +
                       "skin = EXCLUDED.skin"; // Actualizar skin si cambia
        
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
            
            // Guardar skin (con fallback)
            String skinToSave = (c.getSkin() != null && !c.getSkin().isEmpty()) ? c.getSkin() : "body_female";
            ps.setString(9, skinToSave);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al salvar personaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * SOBRECARGA: Crea un personaje nuevo con skin por defecto.
     * (Mantenemos este método para compatibilidad con código antiguo que no envía skin)
     */
    public static boolean createCharacter(int userId, String name, int classId, int slotIndex) {
        return createCharacter(userId, name, classId, slotIndex, "body_female");
    }

    /**
     * Crea un personaje nuevo inicializando valores por defecto.
     * Ahora acepta 'skin' como parámetro opcional.
     */
    public static boolean createCharacter(int userId, String name, int classId, int slotIndex, String skin) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false); 

            // Insertamos con valores iniciales
            String sqlChar = "INSERT INTO characters (user_id, name, class_id, slot_index, level, current_xp, coins, health_streak, skin) " +
                             "VALUES (?, ?, ?, ?, 1, 0, 0, 0, ?) RETURNING id";
            
            int characterId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlChar)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setInt(3, classId);
                ps.setInt(4, slotIndex);
                // Si no se pasa skin, usamos un default genérico
                ps.setString(5, (skin != null) ? skin : "body_female");
                
                ResultSet rs = ps.executeQuery();
                if (rs.next()) characterId = rs.getInt(1);
            }

            if (characterId != -1) {
                // Sincronizamos la clase en la tabla de usuarios
                String sqlUpdateUser = "UPDATE users SET selected_class_id = ? WHERE id = ?";
                try (PreparedStatement psUp = conn.prepareStatement(sqlUpdateUser)) {
                    psUp.setInt(1, classId);
                    psUp.setInt(2, userId);
                    psUp.executeUpdate();
                }

                conn.commit();
                System.out.println("✨ Personaje '" + name + "' creado exitosamente con skin: " + skin);
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
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                System.out.println("🗑️ Personaje eliminado: ID " + characterId);
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar personaje: " + e.getMessage());
            return false;
        }
    }

    // --- MÉTODOS DE ACTUALIZACIÓN PARCIAL ---

    /**
     * Actualiza solo el nombre.
     */
    public static boolean updateCharacterName(int characterId, String newName) {
        String sql = "UPDATE public.characters SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newName);
            pstmt.setInt(2, characterId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando nombre personaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza la clase (Avatar) del personaje.
     */
    public static boolean updateCharacterClass(int characterId, int newClassId) {
        String sql = "UPDATE public.characters SET class_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newClassId);
            pstmt.setInt(2, characterId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando clase personaje: " + e.getMessage());
            return false;
        }
    }
}