package com.ctrlaltquest.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.ctrlaltquest.db.DatabaseConnection;

import javafx.scene.chart.XYChart;

public class DashboardDAO {

    // DTO para transferir datos al controlador
    public static class MisionResumen {
        public String titulo;
        public double progreso; // 0.0 a 1.0
        public String dificultad; // "FACIL", "MEDIA", "DIFICIL"

        public MisionResumen(String t, double p, String d) { 
            this.titulo = t; 
            this.progreso = p; 
            this.dificultad = d;
        }
    }

    /**
     * Obtiene las misiones activas.
     */
    public static List<MisionResumen> getMisionesActivas(int userId) {
        List<MisionResumen> lista = new ArrayList<>();
        
        // CORRECCIÓN: Eliminado 'mp.updated_at'. Ordenamos por las que tienen más progreso primero.
        String sql = "SELECT m.title, m.difficulty, m.xp_reward, mp.progress_percentage " +
                     "FROM public.missions m " +
                     "LEFT JOIN public.mission_progress mp ON m.id = mp.mission_id AND mp.user_id = ? " +
                     "WHERE (mp.progress_percentage < 100 OR mp.progress_percentage IS NULL) " +
                     "AND m.is_daily = true " + 
                     "ORDER BY COALESCE(mp.progress_percentage, 0) DESC LIMIT 3";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                String difficulty = rs.getString("difficulty");
                int xpReward = rs.getInt("xp_reward");
                
                // Lógica de negocio: Inferir dificultad si es nula
                if (difficulty == null) {
                    if (xpReward >= 500) difficulty = "DIFICIL";
                    else if (xpReward >= 200) difficulty = "MEDIA";
                    else difficulty = "FACIL";
                }

                // Obtener progreso, por defecto 0 si es nulo
                int progressRaw = rs.getInt("progress_percentage");
                if (rs.wasNull()) progressRaw = 0;
                
                lista.add(new MisionResumen(title, progressRaw / 100.0, difficulty));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error cargando misiones dashboard: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene la XP de los últimos 7 días.
     */
    public static XYChart.Series<String, Number> getRendimientoSemanal(int userId) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("XP");

        // 1. Pre-llenar los últimos 7 días con 0 (Estructura base)
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE"); // Lun, Mar...
        
        String[] diasLabels = new String[7];
        int[] xpValues = new int[7];

        for (int i = 0; i < 7; i++) {
            // Orden: Hace 6 días -> Hoy
            LocalDate d = today.minusDays(6 - i);
            diasLabels[i] = d.format(formatter).toUpperCase();
            xpValues[i] = 0;
        }

        // 2. Consultar datos reales
        String sql = "SELECT created_at::DATE as fecha, SUM(amount) as total_xp " +
                     "FROM public.xp_history " +
                     "WHERE user_id = ? AND created_at >= CURRENT_DATE - INTERVAL '6 days' " +
                     "GROUP BY fecha";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                LocalDate fechaDB = rs.getDate("fecha").toLocalDate();
                int xp = rs.getInt("total_xp");

                // Mapear fecha DB al índice del array
                for (int i = 0; i < 7; i++) {
                    LocalDate d = today.minusDays(6 - i);
                    if (d.equals(fechaDB)) {
                        xpValues[i] = xp;
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error cargando gráfica XP: " + e.getMessage());
        }

        // 3. Convertir a datos de JavaFX
        for (int i = 0; i < 7; i++) {
            series.getData().add(new XYChart.Data<>(diasLabels[i], xpValues[i]));
        }

        return series;
    }
}