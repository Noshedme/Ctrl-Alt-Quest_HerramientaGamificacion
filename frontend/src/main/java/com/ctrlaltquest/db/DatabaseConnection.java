package com.ctrlaltquest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    // Configuración de la base de datos
    private static final String URL = "jdbc:postgresql://localhost:5432/CtrlAltQuestDB";
    private static final String USER = "postgres"; 
    private static final String PASSWORD = "crac10andy"; 


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
    }
}
