package com.ctrlaltquest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestión de conexión con PostgreSQL.
 * MODIFICADO: Genera una nueva conexión por solicitud para evitar conflictos de hilos.
 */
public class DatabaseConnection {
    
    // Configuración de la base de datos
    private static final String URL = "jdbc:postgresql://localhost:5432/CtrlAltQuestDB";
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "crac10andy"; 

    /**
     * Obtiene una NUEVA instancia de conexión a la base de datos.
     * Es vital que el método que llame a esto cierre la conexión (usando try-with-resources).
     * * @return Connection objeto de conexión activo.
     * @throws SQLException Si falla la conexión.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Registrar el driver de PostgreSQL (necesario en algunas versiones de Java/JDBC)
            Class.forName("org.postgresql.Driver");
            
            // Establecer y devolver una NUEVA conexión
            return DriverManager.getConnection(URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error crítico: No se encontró el Driver de PostgreSQL en el classpath.");
            throw new SQLException("Driver PostgreSQL no encontrado", e);
        }
        // Nota: No capturamos SQLException aquí para que el DAO que llama pueda manejar el error específico.
    }
}