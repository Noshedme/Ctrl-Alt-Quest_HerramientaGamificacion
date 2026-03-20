package com.ctrlaltquest.services;

import com.ctrlaltquest.dao.ActivityDAO;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class ActivityMonitorService {

    private static final int MAX_TITLE_LENGTH = 1024;

    private boolean isRunning    = false;
    private Thread  monitorThread;
    private int     currentUserId = -1;

    private static ActivityMonitorService instance;
    public static ActivityMonitorService getInstance() {
        if (instance == null) instance = new ActivityMonitorService();
        return instance;
    }

    public void startMonitoring(int userId) {
        if (isRunning) return;

        if (userId == -1) {
            try {
                this.currentUserId = SessionManager.getInstance().getUserId();
            } catch (Exception e) {
                System.err.println("⚠️ ActivityMonitor: No hay usuario activo.");
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
                    reportActivity();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error en ActivityMonitor: " + e.getMessage());
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopMonitoring() {
        isRunning = false;
        if (monitorThread != null) monitorThread.interrupt();
        System.out.println("🛑 ActivityMonitor: Detenido.");
    }


    private void reportActivity() {
        String currentApp = getActiveWindowTitle();
        String metricKey  = categorizeActivity(currentApp);
        boolean productive = isProductive(currentApp);

        // Actualizar contexto para el generador de eventos
        EventContextualService.getInstance().updateCurrentActivity(currentApp);

        if (metricKey != null && !metricKey.equals("unknown")) {
            ActivityDAO.registrarActividad(currentUserId, currentApp, metricKey);

            if (productive) {
                XPSyncService.getInstance().awardXPFromActivity(currentUserId, 1, metricKey);
            }

            GameService.getInstance().processActivityEvent(currentUserId, metricKey, 1);
            MissionProgressService.getInstance().processActivityEvent(currentUserId, metricKey, productive);
        }
    }


    private String categorizeActivity(String windowTitle) {
        if (windowTitle == null || windowTitle.isEmpty() || windowTitle.equals("Desconocido"))
            return "unknown";

        String lower = windowTitle.toLowerCase();

        if (lower.contains("intellij") || lower.contains("idea") || lower.contains("eclipse") ||
            lower.contains("visual studio") || lower.contains("code") || lower.contains("netbeans") ||
            lower.contains("stackoverflow") || lower.contains("github"))
            return "time_coding";

        if (lower.contains("word") || lower.contains("excel") || lower.contains("powerpoint") ||
            lower.contains("notion") || lower.contains("obsidian") || lower.contains("docs"))
            return "time_productivity";

        if (lower.contains("chrome") || lower.contains("edge") ||
            lower.contains("firefox") || lower.contains("opera") || lower.contains("brave"))
            return "time_browsing";

        return "app_usage_generic";
    }

    public String getActiveWindowTitle() {
        try {
            char[] buffer = new char[MAX_TITLE_LENGTH];
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) return "Desconocido";
            User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
            String title = Native.toString(buffer);
            return (title == null || title.isEmpty()) ? "Escritorio / Explorador" : title;
        } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
            return "Monitor Inactivo (Falta JNA)";
        } catch (Exception e) {
            return "Error al obtener ventana";
        }
    }

    public boolean isProductive(String windowTitle) {
        if (windowTitle == null) return false;
        String cat = categorizeActivity(windowTitle);
        return cat.equals("time_coding") || cat.equals("time_productivity");
    }
}
