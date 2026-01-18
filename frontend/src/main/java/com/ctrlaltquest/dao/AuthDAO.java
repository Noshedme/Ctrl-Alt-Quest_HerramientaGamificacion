package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.services.SessionManager;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.net.InetAddress;
import java.util.UUID;

public class AuthDAO {

    /**
     * Obtiene el ID único de un usuario por su nombre o email.
     */
    public static int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return -1;
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en getUserIdByUsername: " + e.getMessage());
        }
        return -1;
    }

    /**
     * VALIDA USUARIO Y REGISTRA DISPOSITIVO/IP/SESIÓN/LOGS
     * Este es el corazón del rastreo de tu aplicación.
     */
    public static boolean loginCompleto(String identifier, String plainPassword) {
        String sql = "SELECT id, username, password_hash, is_active FROM users WHERE username = ? OR email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identifier);
                ps.setString(2, identifier);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String dbUsername = rs.getString("username");
                        String hash = rs.getString("password_hash");
                        boolean isActive = rs.getBoolean("is_active");

                        if (BCrypt.checkpw(plainPassword, hash)) {
                            if (isActive) {
                                // 1. Registrar/Obtener Dispositivo
                                int deviceId = registrarDispositivo(conn, userId);
                                
                                // 2. Registrar/Obtener IP de red
                                int networkIpId = registrarIP(conn, deviceId);
                                
                                // 3. Crear Sesión de Actividad
                                int sessionId = abrirSesionActividad(conn, userId, deviceId, networkIpId);
                                
                                // 4. Inicializar el Singleton Global (SessionManager)
                                if (sessionId > 0) {
                                    SessionManager.getInstance().startSession(userId, deviceId, sessionId, dbUsername);
                                    
                                    // 5. Log de Auditoría de Login Exitoso
                                    registrarLogLogin(conn, userId, deviceId, true, null);
                                    System.out.println("✅ Login exitoso: " + dbUsername + " [Sesión: " + sessionId + "]");
                                    return true;
                                } else {
                                    System.err.println("❌ Fallo al generar ID de sesión.");
                                }
                            } else {
                                registrarLogLogin(conn, userId, 0, false, "Cuenta no activada");
                                return false;
                            }
                        }
                    }
                }
            }
            // Fallo de credenciales o usuario no encontrado
            registrarLogLogin(conn, 0, 0, false, "Credenciales incorrectas para: " + identifier);
        } catch (Exception e) {
            System.err.println("❌ Error crítico en login: " + e.getMessage());
        }
        return false;
    }

    private static int registrarDispositivo(Connection conn, int userId) throws Exception {
        String deviceName = InetAddress.getLocalHost().getHostName();
        String os = System.getProperty("os.name");
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        // Generamos un UUID basado en el nombre del equipo para que sea consistente
        String deviceUuidStr = UUID.nameUUIDFromBytes(deviceName.getBytes()).toString();

        String sql = "INSERT INTO devices (user_id, device_uuid, device_name, os, cpu_info, last_seen) " +
                     "VALUES (?, ?, ?, ?, ?, NOW()) " +
                     "ON CONFLICT (device_uuid) DO UPDATE SET last_seen = NOW(), user_id = EXCLUDED.user_id " +
                     "RETURNING id";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setObject(2, UUID.fromString(deviceUuidStr)); // PostgreSQL requiere objeto UUID
            ps.setString(3, deviceName);
            ps.setString(4, os);
            ps.setString(5, cpu != null ? cpu : "Unknown");
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int registrarIP(Connection conn, int deviceId) throws Exception {
        String localIp = InetAddress.getLocalHost().getHostAddress();
        
        String sql = "INSERT INTO network_ips (device_id, local_ip, last_detected) " +
                     "VALUES (?, ?, NOW()) " +
                     "ON CONFLICT (device_id, local_ip) DO UPDATE SET last_detected = NOW() " +
                     "RETURNING id"; 
                     
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            ps.setString(2, localIp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int abrirSesionActividad(Connection conn, int userId, int deviceId, int networkIpId) throws SQLException {
        String sql = "INSERT INTO activity_sessions (user_id, device_id, network_ip_id, session_start) " +
                     "VALUES (?, ?, ?, NOW()) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deviceId);
            if (networkIpId > 0) ps.setInt(3, networkIpId); 
            else ps.setNull(3, Types.INTEGER);
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void registrarLogLogin(Connection conn, int userId, int deviceId, boolean success, String reason) {
        String sql = "INSERT INTO login_logs (user_id, device_id, success, failure_reason, ip_address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId > 0) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            if (deviceId > 0) ps.setInt(2, deviceId); else ps.setNull(2, Types.INTEGER);
            ps.setBoolean(3, success);
            ps.setString(4, reason);
            ps.setString(5, InetAddress.getLocalHost().getHostAddress());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo guardar el log de login: " + e.getMessage());
        }
    }

    public String registerUser(String username, String email, String plainPassword) throws SQLException {
        String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        String tokenString = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) throw new SQLException("No hay conexión con la base de datos.");
            conn.setAutoCommit(false);
            
            String sqlUser = "INSERT INTO users (username, email, password_hash, is_active) VALUES (?, ?, ?, false) RETURNING id";
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser)) {
                psUser.setString(1, username);
                psUser.setString(2, email);
                psUser.setString(3, passwordHash);
                
                try (ResultSet rs = psUser.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String sqlVerify = "INSERT INTO email_verifications (user_id, email, token, expires_at) " +
                                           "VALUES (?, ?, ?, NOW() + INTERVAL '24 hours')";
                        try (PreparedStatement psVerify = conn.prepareStatement(sqlVerify)) {
                            psVerify.setInt(1, userId);
                            psVerify.setString(2, email);
                            psVerify.setString(3, tokenString);
                            psVerify.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
            return tokenString;
        } catch (SQLException e) {
            System.err.println("❌ Error en registro: " + e.getMessage());
            throw e;
        }
    }

    public boolean verifyUserCode(String email, String inputCode) throws SQLException {
        String sqlCheck = "SELECT user_id FROM email_verifications WHERE email = ? AND token = ? AND expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            int userId = -1;
            
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setString(1, email);
                ps.setString(2, inputCode.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) userId = rs.getInt("user_id");
                }
            }
            
            if (userId != -1) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement psAct = conn.prepareStatement("UPDATE users SET is_active = true WHERE id = ?")) {
                        psAct.setInt(1, userId);
                        psAct.executeUpdate();
                    }
                    try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM email_verifications WHERE email = ?")) {
                        psDel.setString(1, email);
                        psDel.executeUpdate();
                    }
                    conn.commit();
                    return true;
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            }
            return false;
        }
    }

    public boolean userExists(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}