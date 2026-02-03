package com.ctrlaltquest.dao;

import com.ctrlaltquest.db.DatabaseConnection; // Import corregido
import javafx.scene.chart.XYChart;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    // Clase pública estática para transferir datos al controlador
    public static class MisionResumen {
        public String titulo;
        public double progreso;
        
        public MisionResumen(String t, double p) { 
            this.titulo = t; 
            this.progreso = p; 
        }
    }

    /**
     * Obtiene las misiones activas (no completadas) del usuario.
     */
    public static List<MisionResumen> getMisionesActivas(int userId) {
        List<MisionResumen> lista = new ArrayList<>();
        
        // Según tu esquema: table 'missions', columnas 'title', 'progress' (integer), 'completed' (boolean)
        String sql = "SELECT title, progress FROM public.missions " +
                     "WHERE user_id = ? AND completed = false " +
                     "ORDER BY created_at DESC LIMIT 3";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                int progressRaw = rs.getInt("progress"); // Tu tabla define progress como integer
                
                // Normalizamos el progreso a 0.0 - 1.0 para la ProgressBar de JavaFX.
                // Asumimos que el progreso máximo es 100 por defecto.
                lista.add(new MisionResumen(title, progressRaw / 100.0));
            }
        } catch (SQLException e) {
            System.err.println("Error cargando misiones: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Obtiene la XP ganada en los últimos 7 días.
     */
    public static XYChart.Series<String, Number> getRendimientoSemanal(int userId) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("XP Semanal");

        // Según tu esquema: table 'xp_history', columna 'amount'
        String sql = "SELECT TO_CHAR(created_at, 'Dy') as dia, SUM(amount) as total_xp " +
                     "FROM public.xp_history " +
                     "WHERE user_id = ? AND created_at > CURRENT_DATE - INTERVAL '7 days' " +
                     "GROUP BY dia, DATE(created_at) " +
                     "ORDER BY DATE(created_at) ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String dia = rs.getString("dia"); 
                int xp = rs.getInt("total_xp");
                series.getData().add(new XYChart.Data<>(dia, xp));
            }
        } catch (SQLException e) {
            System.err.println("Error cargando gráfica XP: " + e.getMessage());
            e.printStackTrace();
        }
        return series;
    }
}