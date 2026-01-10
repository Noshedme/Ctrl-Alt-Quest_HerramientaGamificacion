package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.UUID;

public class AuthDAO {

    /**
     * Registra al usuario, hashea la clave y genera el token de verificación.
     * Retorna el token generado para enviarlo por email.
     */
    public String registerUser(String username, String email, String plainPassword) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) throw new SQLException("No se pudo establecer conexión con la base de datos.");

        // 1. Seguridad: Hashear contraseña con BCrypt
        String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        
        // 2. Generar código corto de 6 caracteres (Runa de Verificación)
        String tokenString = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        PreparedStatement psUser = null;
        PreparedStatement psVerify = null;

        try {
            conn.setAutoCommit(false); // INICIO TRANSACCIÓN

            // A. Insertar Usuario (Estado inicial: inactivo o pendiente)
            String sqlUser = "INSERT INTO users (username, email, password_hash, is_active) VALUES (?, ?, ?, false) RETURNING id";
            psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, username);
            psUser.setString(2, email);
            psUser.setString(3, passwordHash);

            ResultSet rs = psUser.executeQuery();
            int userId = 0;
            if (rs.next()) {
                userId = rs.getInt("id");
            }

            // B. Insertar Verificación en la tabla email_verifications
            // Usamos el tokenString directamente para que la validación sea exacta al código enviado
            String sqlVerify = "INSERT INTO email_verifications (user_id, email, token, expires_at) VALUES (?, ?, ?, NOW() + INTERVAL '24 hours')";
            psVerify = conn.prepareStatement(sqlVerify);
            psVerify.setInt(1, userId);
            psVerify.setString(2, email);
            psVerify.setString(3, tokenString); 

            psVerify.executeUpdate();

            conn.commit(); // CONFIRMAR CAMBIOS
            System.out.println("✅ Usuario " + username + " forjado. Token generado: " + tokenString);
            return tokenString; 

        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // DESHACER si hay error
            throw e;
        } finally {
            if (conn != null) conn.setAutoCommit(true);
            if (psUser != null) psUser.close();
            if (psVerify != null) psVerify.close();
        }
    }
    
    /**
     * Verifica si el código ingresado coincide con el enviado al email y no ha expirado.
     */
    public boolean verifyUserCode(String email, String inputCode) throws SQLException {
        String sqlCheck = "SELECT user_id FROM email_verifications WHERE email = ? AND token = ? AND expires_at > NOW()";
        String sqlUpdateUser = "UPDATE users SET is_active = true WHERE id = ?";
        String sqlDeleteToken = "DELETE FROM email_verifications WHERE email = ?";

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false); // Transacción para asegurar que si se activa, se borre el token

            // 1. Verificar si el código existe y es válido
            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setString(1, email);
                ps.setString(2, inputCode.toUpperCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                }
            }

            // 2. Si es válido, activar usuario y limpiar tokens viejos
            if (userId != -1) {
                // Activar usuario
                try (PreparedStatement psAct = conn.prepareStatement(sqlUpdateUser)) {
                    psAct.setInt(1, userId);
                    psAct.executeUpdate();
                }
                // Borrar token usado
                try (PreparedStatement psDel = conn.prepareStatement(sqlDeleteToken)) {
                    psDel.setString(1, email);
                    psDel.executeUpdate();
                }

                conn.commit();
                return true;
            }

            return false;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Comprueba si el nombre o el correo ya están en uso.
     */
    public boolean userExists(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (conn == null) return false;
            
            ps.setString(1, username);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }
}