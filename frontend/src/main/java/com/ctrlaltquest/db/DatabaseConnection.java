package com.ctrlaltquest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    // CONFIGURACIÓN (Idealmente esto iría en un archivo .env, pero por ahora lo pondremos aquí)
    private static final String URL = "jdbc:postgresql://localhost:5432/CtrlAltQuestDB";
    private static final String USER = "postgres"; // Tu usuario
    private static final String PASSWORD = "crac10andy"; // ⚠️ ¡PON TU CLAVE REAL!

    private static Connection connection = null;

    // Patrón Singleton: Solo una conexión activa para no saturar
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Conexión establecida con la Base de Datos.");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Error crítico al conectar BD: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}