package com.ctrlaltquest.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.ctrlaltquest.db.DatabaseConnection;

public class UserDAO {

    public static boolean otorgarRecompensas(int userId, int xp, int coins) {
        boolean levelUp = false;
        String sqlSelect = "SELECT level, current_xp, coins FROM public.users WHERE id = ?";
        String sqlUpdate = "UPDATE public.users SET level = ?, current_xp = ?, total_xp = total_xp + ?, coins = ? WHERE id = ?";
        String sqlLog = "INSERT INTO public.xp_history (user_id, amount, reason) VALUES (?, ?, 'Misión Completada')";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Transacción para seguridad

            // 1. Obtener estado actual
            int currentLevel = 1;
            int currentXp = 0;
            int currentCoins = 0;
            
            try (PreparedStatement pstSelect = conn.prepareStatement(sqlSelect)) {
                pstSelect.setInt(1, userId);
                ResultSet rs = pstSelect.executeQuery();
                if (rs.next()) {
                    currentLevel = rs.getInt("level");
                    currentXp = rs.getInt("current_xp");
                    currentCoins = rs.getInt("coins");
                }
            }

            // 2. Calcular nuevos valores
            int newXp = currentXp + xp;
            int newCoins = currentCoins + coins;
            
            // Fórmula de Nivel RPG: XP necesaria = Nivel * 1000
            int xpRequired = currentLevel * 1000;
            
            while (newXp >= xpRequired) {
                newXp -= xpRequired;
                currentLevel++;
                levelUp = true;
                xpRequired = currentLevel * 1000; // Recalcular para el siguiente nivel
                System.out.println("🎉 ¡SUBIDA DE NIVEL! Nuevo nivel: " + currentLevel);
            }

            // 3. Actualizar Usuario
            try (PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdate)) {
                pstUpdate.setInt(1, currentLevel);
                pstUpdate.setInt(2, newXp);
                pstUpdate.setInt(3, xp); // Total XP histórico
                pstUpdate.setInt(4, newCoins);
                pstUpdate.setInt(5, userId);
                pstUpdate.executeUpdate();
            }

            // 4. Log Historial XP
            try (PreparedStatement pstLog = conn.prepareStatement(sqlLog)) {
                pstLog.setInt(1, userId);
                pstLog.setInt(2, xp);
                pstLog.executeUpdate();
            }

            // 5. Log Historial Monedas
            if (coins > 0) {
                registrarTransaccionCoins(userId, coins, "Misión Completada");
            }

            conn.commit(); // Guardar cambios
            return levelUp;

        } catch (SQLException e) {
            System.err.println("❌ Error otorgando recompensas: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void registrarTransaccionCoins(int userId, int amount, String reason) {
        registrarTransaccionCoins(userId, amount, reason, "system", null);
    }

    public static void registrarTransaccionCoins(int userId, int amount, String reason, String refType, Integer refId) {
        String sql = "INSERT INTO public.coin_transactions (user_id, amount, reason, ref_type, ref_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, amount);
            pstmt.setString(3, reason);
            pstmt.setString(4, refType);
            
            if (refId != null) {
                pstmt.setInt(5, refId);
            } else {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            }
            
            pstmt.executeUpdate();
            
            System.out.println("✅ Transacción de monedas registrada: usuario=" + userId + 
                              ", amount=" + amount + ", reason=" + reason);
            
        } catch (SQLException e) {
            System.err.println("❌ Error registrando transacción de monedas: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static boolean actualizarMonedas(int userId, int newBalance) {
        String sql = "UPDATE public.users SET coins = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newBalance);
            pstmt.setInt(2, userId);
            
            int affected = pstmt.executeUpdate();
            return affected > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando monedas: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static int obtenerBalanceMonedas(int userId) {
        String sql = "SELECT coins FROM public.users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("coins");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo balance: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }

    public static String getUserEmail(int userId) {
        String sql = "SELECT email FROM public.users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo email: " + e.getMessage());
            e.printStackTrace();
        }
        return "Desconocido";
    }

    public static boolean updatePassword(int userId, String newPassword) {
        // Hashear antes de guardar (BCrypt con factor de trabajo 12)
        String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String sql = "UPDATE public.users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, passwordHash); // Guardamos el hash, no texto plano
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Contraseña actualizada para usuario ID: " + userId);
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(int userId) {
        String sql = "DELETE FROM public.users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Usuario eliminado: ID " + userId);
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ Error eliminando usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
