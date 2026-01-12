package com.ctrlaltquest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestión de conexión centralizada con PostgreSQL.
 * Implementa el patrón Singleton para reutilizar la conexión.
 */
public class DatabaseConnection {
    
    // Configuración de la base de datos
    private static final String URL = "jdbc:postgresql://localhost:5432/CtrlAltQuestDB";
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "crac10andy"; 

    private static Connection connection = null;

    /**
     * Obtiene la instancia activa de la conexión.
     * Si no existe o está cerrada, intenta abrir una nueva.
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Registrar el driver de PostgreSQL
                Class.forName("org.postgresql.Driver");
                
                // Establecer conexión
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Portal de datos abierto: Conexión establecida.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: No se encontró el Driver de PostgreSQL en el classpath.");
        } catch (SQLException e) {
            System.err.println("❌ Error de SQL: No se pudo conectar a la base de datos.");
            System.err.println("   Mensaje: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Cierra la conexión de forma segura cuando la app termina.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔌 Conexión con la base de datos cerrada.");
            } catch (SQLException e) {
                System.err.println("⚠️ Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }
}