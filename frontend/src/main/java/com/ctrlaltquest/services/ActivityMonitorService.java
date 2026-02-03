package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.ActivityDAO;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class ActivityMonitorService {

    private static final int MAX_TITLE_LENGTH = 1024;
    private boolean isRunning = false;
    private Thread monitorThread;
    private int currentUserId = -1;

    // Singleton pattern
    private static ActivityMonitorService instance;

    public static ActivityMonitorService getInstance() {
        if (instance == null) {
            instance = new ActivityMonitorService();
        }
        return instance;
    }

    // --- CONTROL DEL SERVICIO ---

    /**
     * Inicia el monitoreo en segundo plano.
     * @param userId El ID del usuario actual. Si es -1, intenta obtenerlo del SessionManager.
     */
    public void startMonitoring(int userId) {
        if (isRunning) return; // Ya está corriendo

        // Prioriza argumento, si no, busca en SessionManager
        if (userId == -1) {
            try {
                this.currentUserId = SessionManager.getInstance().getUserId();
            } catch (Exception e) {
                System.err.println("⚠️ ActivityMonitor: No hay usuario activo en SessionManager. Esperando login...");
                return;
            }
        } else {
            this.currentUserId = userId;
        }

        this.isRunning = true;

        monitorThread = new Thread(() -> {
            System.out.println("⚡ ActivityMonitor: Iniciado para usuario " + currentUserId);
            
            while (isRunning) {
                try {
                    // Ejecuta la lógica de reporte (Integración con Game Loop)
                    reportActivity();

                    // Esperar 1 segundo antes del siguiente tick
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    System.out.println("ActivityMonitor interrumpido.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error en ActivityMonitor loop: " + e.getMessage());
                }
            }
        });

        monitorThread.setDaemon(true); // El hilo muere si cierras la app principal
        monitorThread.start();
    }

    /**
     * Detiene el monitoreo.
     */
    public void stopMonitoring() {
        isRunning = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        System.out.println("🛑 ActivityMonitor: Detenido.");
    }

    // --- LÓGICA PRINCIPAL ---

    /**
     * Método central que analiza la actividad actual y la reporta al motor de juego.
     */
    private void reportActivity() {
        // 1. Obtener ventana activa real (Usando JNA)
        String currentApp = getActiveWindowTitle();
        
        // 2. Determinar la métrica basada en el título
        String metricKey = categorizeActivity(currentApp);
        
        // 3. Determinar si es productiva
        boolean isProductive = isProductive(currentApp);

        // 4. Enviar evento al motor de juego (1 "tick" de actividad = 1 segundo)
        if (metricKey != null && !metricKey.equals("unknown")) {
            // Registrar en BD
            ActivityDAO.registrarActividad(currentUserId, currentApp, metricKey);
            
            // GameService está en el mismo paquete, no requiere import
            GameService.getInstance().processActivityEvent(currentUserId, metricKey, 1);
        }
    }

    /**
     * Clasifica el título de la ventana en una "metric_key" para la base de datos.
     */
    private String categorizeActivity(String windowTitle) {
        if (windowTitle == null || windowTitle.isEmpty() || windowTitle.equals("Desconocido")) {
            return "unknown";
        }

        String lower = windowTitle.toLowerCase();

        // 1. PROGRAMACIÓN (Coding)
        if (lower.contains("intellij") || 
            lower.contains("idea") ||      
            lower.contains("eclipse") || 
            lower.contains("visual studio") || 
            lower.contains("code") ||      // VS Code
            lower.contains("netbeans") ||
            lower.contains("stackoverflow") || 
            lower.contains("github")) {
            return "time_coding"; 
        } 
        // 2. PRODUCTIVIDAD GENERAL (Oficina/Notas)
        else if (lower.contains("word") || 
                 lower.contains("excel") || 
                 lower.contains("powerpoint") ||
                 lower.contains("notion") ||
                 lower.contains("obsidian") ||
                 lower.contains("docs")) { 
            return "time_productivity"; 
        }
        // 3. NAVEGACIÓN (Browsing)
        else if (lower.contains("chrome") || 
                 lower.contains("edge") || 
                 lower.contains("firefox") || 
                 lower.contains("opera")) {
            return "time_browsing"; 
        }

        return "app_usage_generic"; // Actividad en PC general
    }

    /**
     * Obtiene el título de la ventana que está actualmente en primer plano usando JNA.
     * Requiere la dependencia JNA en pom.xml.
     */
    public String getActiveWindowTitle() {
        try {
            char[] buffer = new char[MAX_TITLE_LENGTH];
            // Usamos la API nativa de Windows (User32)
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            
            if (hwnd == null) return "Desconocido";

            User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
            String title = Native.toString(buffer);
            
            if (title == null || title.isEmpty()) {
                return "Escritorio / Explorador";
            }
            return title;
        } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
            // Fallback silencioso si no está en Windows o falta la librería
            return "Monitor Inactivo (Falta JNA)";
        } catch (Exception e) {
            return "Error al obtener ventana";
        }
    }

    /**
     * Helper para verificar productividad simple (usado por otros servicios si es necesario)
     */
    public boolean isProductive(String windowTitle) {
        if (windowTitle == null) return false;
        String category = categorizeActivity(windowTitle);
        return category.equals("time_coding") || category.equals("time_productivity");
    }
}