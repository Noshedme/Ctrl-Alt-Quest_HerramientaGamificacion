package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection;
import com.ctrlaltquest.services.SessionManager;
import org.mindrot.jbcrypt.BCrypt;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.sql.*;
import java.util.UUID;
import java.util.Random;

public class AuthDAO {

    private static class GeoData {
        String publicIp = "0.0.0.0";
        String city = "Desconocida";
        String country = "Desconocido";
        String isp = "Desconocido";
    }

    private static String limit(String text, int max) {
        if (text == null) return "Unknown";
        return text.length() > max ? text.substring(0, max) : text;
    }

    public static int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return -1;
            ps.setString(1, limit(username, 100));
            ps.setString(2, limit(username, 100));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en getUserIdByUsername: " + e.getMessage());
        }
        return -1;
    }

    private static GeoData fetchGeoData() {
        GeoData data = new GeoData();
        try {
            URL url = new URL("http://ip-api.com/csv/?fields=query,city,country,isp");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000); 

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        data.publicIp = parts[0];
                        data.city = parts[1];
                        data.country = parts[2];
                        data.isp = parts[3];
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ GeoLog Error: " + e.getMessage());
        }
        return data;
    }

    public static boolean loginCompleto(String identifier, String plainPassword) {
        String sql = "SELECT id, username, password_hash, is_active FROM users WHERE username = ? OR email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, limit(identifier, 100));
                ps.setString(2, limit(identifier, 100));
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String dbUsername = rs.getString("username");
                        String hash = rs.getString("password_hash");
                        boolean isActive = rs.getBoolean("is_active");

                        if (BCrypt.checkpw(plainPassword, hash)) {
                            if (isActive) {
                                GeoData geo = fetchGeoData();
                                String pcName = limit(InetAddress.getLocalHost().getHostName(), 50);
                                String osInfo = limit(System.getProperty("os.name") + " " + System.getProperty("os.version"), 50);

                                int deviceId = registrarDispositivo(conn, userId, pcName, osInfo);
                                int networkIpId = registrarIPCompleta(conn, deviceId, geo);
                                int sessionId = abrirSesionActividad(conn, userId, deviceId, networkIpId);
                                
                                if (sessionId > 0) {
                                    SessionManager.getInstance().startSession(userId, deviceId, sessionId, dbUsername);
                                    registrarLogLogin(conn, userId, deviceId, true, null, geo.publicIp);
                                    registrarAuditLog(conn, userId, "LOGIN_SUCCESS", 
                                        "Acceso desde " + geo.city + ", " + geo.country, geo.publicIp, pcName, osInfo);

                                    return true;
                                }
                            } else {
                                registrarLogLogin(conn, userId, 0, false, "Cuenta no activada", "0.0.0.0");
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error en loginCompleto: " + e.getMessage());
        }
        return false;
    }

    private static int registrarDispositivo(Connection conn, int userId, String pcName, String osInfo) throws Exception {
        String cpu = limit(System.getenv("PROCESSOR_IDENTIFIER"), 100);
        String deviceUuidStr = UUID.nameUUIDFromBytes(pcName.getBytes()).toString();

        String sql = "INSERT INTO devices (user_id, device_uuid, device_name, os, cpu_info, last_seen) " +
                     "VALUES (?, ?, ?, ?, ?, NOW()) ON CONFLICT (device_uuid) " +
                     "DO UPDATE SET last_seen = NOW(), device_name = EXCLUDED.device_name, os = EXCLUDED.os " +
                     "RETURNING id";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setObject(2, UUID.fromString(deviceUuidStr));
            ps.setString(3, pcName); 
            ps.setString(4, osInfo); 
            ps.setString(5, cpu);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static int registrarIPCompleta(Connection conn, int deviceId, GeoData geo) throws Exception {
        String localIp = limit(InetAddress.getLocalHost().getHostAddress(), 45);
        String sql = "INSERT INTO network_ips (device_id, local_ip, public_ip, isp, country, city, last_detected) " +
                     "VALUES (?, ?, ?, ?, ?, ?, NOW()) " +
                     "ON CONFLICT (device_id, local_ip) DO UPDATE SET " +
                     "public_ip = EXCLUDED.public_ip, isp = EXCLUDED.isp, " +
                     "country = EXCLUDED.country, city = EXCLUDED.city, last_detected = NOW() " +
                     "RETURNING id"; 
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            ps.setString(2, localIp);
            ps.setString(3, limit(geo.publicIp, 45));
            ps.setString(4, limit(geo.isp, 100));
            ps.setString(5, limit(geo.country, 50));
            ps.setString(6, limit(geo.city, 50));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static int abrirSesionActividad(Connection conn, int userId, int deviceId, int networkIpId) throws SQLException {
        String sql = "INSERT INTO activity_sessions (user_id, device_id, network_ip_id, session_start) VALUES (?, ?, ?, NOW()) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deviceId);
            if (networkIpId > 0) ps.setInt(3, networkIpId); else ps.setNull(3, Types.INTEGER);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static void registrarLogLogin(Connection conn, int userId, int deviceId, boolean success, String reason, String ip) {
        String sql = "INSERT INTO login_logs (user_id, device_id, success, failure_reason, ip_address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId > 0) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            if (deviceId > 0) ps.setInt(2, deviceId); else ps.setNull(2, Types.INTEGER);
            ps.setBoolean(3, success);
            ps.setString(4, limit(reason, 255));
            ps.setString(5, limit(ip, 45));
            ps.executeUpdate();
        } catch (Exception e) {}
    }

    private static void registrarAuditLog(Connection conn, int userId, String action, String desc, String ip, String pc, String os) {
        String sql = "INSERT INTO events (user_id, type, description, occurred_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId > 0) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, limit(action, 50));
            ps.setString(3, limit(desc + " | IP: " + ip + " | PC: " + pc, 500));
            ps.executeUpdate();
        } catch (SQLException e) {}
    }

    // --- REGISTRO DE USUARIO ---
    public String registerUser(String username, String email, String pass) throws SQLException {
        String passwordHash = BCrypt.hashpw(pass, BCrypt.gensalt(12));
        String token = String.format("%06d", new Random().nextInt(999999));
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) throw new SQLException("Error de conexión");
            conn.setAutoCommit(false);
            try {
                String sqlUser = "INSERT INTO users (username, email, password_hash, is_active) VALUES (?, ?, ?, false) RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                    ps.setString(1, limit(username, 50));
                    ps.setString(2, limit(email, 100));
                    ps.setString(3, passwordHash);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int uid = rs.getInt("id");
                            String sqlV = "INSERT INTO email_verifications (user_id, email, token, expires_at) VALUES (?, ?, ?, NOW() + INTERVAL '24 hours')";
                            try (PreparedStatement psV = conn.prepareStatement(sqlV)) {
                                psV.setInt(1, uid); psV.setString(2, limit(email, 100)); psV.setString(3, token);
                                psV.executeUpdate();
                            }
                        }
                    }
                }
                conn.commit(); 
                return token;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // --- LOGICA DE CLASES (NUEVO) ---
    public void updateUserClass(int userId, int classId) throws SQLException {
        String sql = "UPDATE users SET selected_class_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, classId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    // --- LÓGICA DE RECUPERACIÓN DE CONTRASEÑA ---

    public String generateResetCode(String email) throws SQLException {
        int userId = getUserIdByUsername(email);
        if (userId == -1) return null;

        String token = String.format("%06d", new Random().nextInt(999999));
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String deleteOld = "DELETE FROM email_verifications WHERE email = ?";
                try (PreparedStatement psD = conn.prepareStatement(deleteOld)) {
                    psD.setString(1, limit(email, 100));
                    psD.executeUpdate();
                }

                String sql = "INSERT INTO email_verifications (user_id, email, token, expires_at) " +
                             "VALUES (?, ?, ?, NOW() + INTERVAL '15 minutes')";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, limit(email, 100));
                    ps.setString(3, token);
                    ps.executeUpdate();
                }
                conn.commit();
                return token;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean verifyResetCode(String email, String code) throws SQLException {
        String sql = "SELECT id FROM email_verifications WHERE email = ? AND token = ? AND expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, limit(email, 100));
            ps.setString(2, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean resetPassword(String email, String newPassword) throws SQLException {
        String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, passwordHash);
                    ps.setString(2, limit(email, 100));
                    int affected = ps.executeUpdate();
                    
                    if (affected > 0) {
                        try (PreparedStatement psD = conn.prepareStatement("DELETE FROM email_verifications WHERE email = ?")) {
                            psD.setString(1, limit(email, 100));
                            psD.executeUpdate();
                        }
                        conn.commit();
                        return true;
                    }
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean verifyUserCode(String email, String inputCode) throws SQLException {
        String sql = "SELECT user_id FROM email_verifications WHERE email = ? AND token = ? AND expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, limit(email, 100)); ps.setString(2, inputCode.trim());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) userId = rs.getInt("user_id"); }
            }
            if (userId != -1) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement psA = conn.prepareStatement("UPDATE users SET is_active = true WHERE id = ?")) {
                        psA.setInt(1, userId); psA.executeUpdate();
                    }
                    try (PreparedStatement psD = conn.prepareStatement("DELETE FROM email_verifications WHERE email = ?")) {
                        psD.setString(1, limit(email, 100)); psD.executeUpdate();
                    }
                    conn.commit(); return true;
                } catch (SQLException ex) { conn.rollback(); throw ex; }
            }
            return false;
        }
    }

    public boolean userExists(String user, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, limit(user, 100)); ps.setString(2, limit(email, 100));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        }
    }
}