package com.ctrlaltquest.dao;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.db.DatabaseConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CharacterDAO {

    /**
     * Recupera todos los personajes de un usuario específico.
     * Mapeado por slot_index para la pantalla de selección.
     */
    public static Map<Integer, Character> getCharactersByUser(int userId) {
        Map<Integer, Character> map = new HashMap<>();
        String query = "SELECT id, name, class_id, user_id, level, slot_index FROM characters WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return map;

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Creamos el objeto usando el constructor compatible
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
     * Este es el método que llama el CharacterEditorController.
     */
    public static boolean saveCharacter(Character c) {
        String query = "INSERT INTO characters (user_id, name, class_id, slot_index, level) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (conn == null) return false;

            ps.setInt(1, c.getUserId());
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
     * Método alternativo por parámetros individuales.
     */
    public static boolean createCharacter(int userId, String name, int classId, int slotIndex) {
        Character nuevo = new Character();
        nuevo.setUserId(userId);
        nuevo.setName(name);
        nuevo.setClassId(classId);
        nuevo.setSlotIndex(slotIndex);
        nuevo.setLevel(1);
        return saveCharacter(nuevo);
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