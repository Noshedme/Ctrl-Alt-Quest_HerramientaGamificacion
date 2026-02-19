package com.ctrlaltquest.services;

/**
 * XPChangeListener - Interface para observadores de cambios de XP
 * 
 * Implementa el patrón Observer para notificar a la UI y otros servicios
 * cuando cambia el XP de un usuario
 */
public interface XPChangeListener {
    
    /**
     * Se llama cuando cambia el XP de un usuario
     * 
     * @param userId ID del usuario que cambió
     * @param event Detalles del cambio
     */
    void onXPChanged(int userId, XPSyncService.XPChangeEvent event);
    
    /**
     * Se llama cuando el usuario sube de nivel
     * 
     * @param userId ID del usuario
     * @param newLevel Nuevo nivel alcanzado
     */
    void onLevelUp(int userId, int newLevel);
}
