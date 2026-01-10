package com.ctrlaltquest.services;

import com.ctrlaltquest.db.DatabaseConnection;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditService {
    public static void log(Integer userId, String action, String description) {
        String sql = "INSERT INTO audit_logs (user_id, action, description, ip_address, pc_name, os_info) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            InetAddress localhost = InetAddress.getLocalHost();
            if (userId != null) ps.setInt(1, userId); else ps.setNull(1, java.sql.Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.setString(4, localhost.getHostAddress());
            ps.setString(5, localhost.getHostName());
            ps.setString(6, System.getProperty("os.name"));

            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("⚠️ Error Auditoría: " + e.getMessage());
        }
    }
}