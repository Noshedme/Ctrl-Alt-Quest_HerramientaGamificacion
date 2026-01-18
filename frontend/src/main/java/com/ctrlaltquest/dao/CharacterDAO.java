package com.ctrlaltquest.dao;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.services.SessionManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CharacterDAO {

    /**
     * Recupera todos los personajes de un usuario específico.
     * Mapeado por slot_index para la pantalla de selección.
     * @param userId El ID del usuario (si es <= 0, usa el del SessionManager)
     */
    public static Map<Integer, Character> getCharactersByUser(int userId) {
        Map<Integer, Character> map = new HashMap<>();
        
        // Si el controlador pasa 0 o -1, intentamos recuperar el ID de la sesión actual
        if (userId <= 0) {
            userId = SessionManager.getInstance().getUserId();
        }
        
        if (userId <= 0) return map; // Si sigue siendo inválido, devolvemos mapa vacío

        String query = "SELECT id, name, class_id, user_id, level, slot_index FROM characters WHERE user_id = ? ORDER BY slot_index";

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
                    
                    map.put(c.getSlotIndex(), c);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener personajes: " + e.getMessage());
        }
        return map;
    }

    /**
     * Guarda un objeto Character completo.
     * Implementa lógica de actualización si ya existe el slot para ese usuario.
     */
    public static boolean saveCharacter(Character c) {
        // Aseguramos que el ID de usuario esté presente
        int userId = c.getUserId();
        if (userId <= 0) userId = SessionManager.getInstance().getUserId();

        // SQL con ON CONFLICT para manejar actualizaciones de slots existentes
        String query = "INSERT INTO characters (user_id, name, class_id, slot_index, level) " +
                       "VALUES (?, ?, ?, ?, ?) " +
                       "ON CONFLICT (user_id, slot_index) DO UPDATE SET " +
                       "name = EXCLUDED.name, class_id = EXCLUDED.class_id, level = EXCLUDED.level";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return false;

            ps.setInt(1, userId);
            ps.setString(2, c.getName());
            ps.setInt(3, c.getClassId());
            ps.setInt(4, c.getSlotIndex());
            ps.setInt(5, c.getLevel());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al salvar personaje: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea un personaje nuevo inicializando su clase.
     * Este es el método que llama tu controlador por parámetros individuales.
     */
    public static boolean createCharacter(int userId, String name, int classId, int slotIndex) {
        // Usamos una transacción para asegurar integridad
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false); 

            // 1. Insertar el personaje
            String sqlChar = "INSERT INTO characters (user_id, name, class_id, slot_index, level) VALUES (?, ?, ?, ?, 1) RETURNING id";
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
                // 2. Actualizamos la clase seleccionada en el perfil del usuario principal
                String sqlUpdateUser = "UPDATE users SET selected_class_id = ? WHERE id = ?";
                try (PreparedStatement psUp = conn.prepareStatement(sqlUpdateUser)) {
                    psUp.setInt(1, classId);
                    psUp.setInt(2, userId);
                    psUp.executeUpdate();
                }

                conn.commit();
                System.out.println("✨ Personaje '" + name + "' creado exitosamente.");
                return true;
            }
            
            conn.rollback();
            return false;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { }
            System.err.println("❌ Error en createCharacter: " + e.getMessage());
            return false;
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