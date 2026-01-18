package com.ctrlaltquest.services;

import java.time.LocalDateTime;

/**
 * SessionManager - Singleton para gestionar el estado global del aventurero.
 * Mantiene la referencia al usuario, su dispositivo y la sesión activa en la BBDD.
 */
public class SessionManager {

    private static SessionManager instance;

    // Datos de identidad de la sesión actual
    private int userId = -1;
    private int deviceId = -1;
    private int currentSessionId = -1;
    private String username;
    
    // Metadatos de la sesión
    private LocalDateTime sessionStartTime;

    // Constructor privado para patrón Singleton
    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Inicializa los datos después de un login exitoso.
     */
    public void startSession(int userId, int deviceId, int sessionId, String username) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.currentSessionId = sessionId;
        this.username = username;
        this.sessionStartTime = LocalDateTime.now();
        System.out.println("✅ Sesión Global Iniciada: [User: " + username + " | SessionID: " + sessionId + "]");
    }

    /**
     * Limpia los datos al cerrar sesión o salir de la app.
     */
    public void closeSession() {
        this.userId = -1;
        this.deviceId = -1;
        this.currentSessionId = -1;
        this.username = null;
        this.sessionStartTime = null;
        System.out.println("🔒 Sesión Global Cerrada.");
    }

    // --- GETTERS Y SETTERS ---

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }

    public int getCurrentSessionId() { return currentSessionId; }
    public void setCurrentSessionId(int sessionId) { this.currentSessionId = sessionId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getSessionStartTime() { return sessionStartTime; }

    public boolean isLoggedIn() {
        return userId != -1 && currentSessionId != -1;
    }
}