package com.ctrlaltquest.ui.utils;

/**
 * ToastHelper - Utilidades especializadas para manejo de excepciones
 * Facilita el uso de Toast en diferentes contextos (BD, API, validación, etc.)
 */
public class ToastHelper {
    
    /**
     * Maneja errores de Base de Datos
     * Uso: ToastHelper.handleDatabaseError(e);
     */
    public static void handleDatabaseError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Error desconocido de BD";
        
        if (message.contains("connection") || message.contains("Connection")) {
            Toast.error("Error de Conexión a BD", 
                "No se pudo conectar a la base de datos");
        } else if (message.contains("duplicate") || message.contains("Duplicate")) {
            Toast.error("Error de Duplicado", 
                "Este registro ya existe");
        } else if (message.contains("constraint") || message.contains("foreign")) {
            Toast.error("Error de Integridad", 
                "Referencia a datos inexistentes");
        } else {
            Toast.exception("Error de Base de Datos", e);
        }
    }
    
    /**
     * Maneja errores de API/Red
     * Uso: ToastHelper.handleNetworkError(e);
     */
    public static void handleNetworkError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Error de red";
        
        if (message.contains("timeout") || message.contains("Timeout")) {
            Toast.warning("Conexión Lenta", 
                "La solicitud tardó demasiado. Intenta de nuevo");
        } else if (message.contains("404") || message.contains("not found")) {
            Toast.error("Recurso no Encontrado", 
                "El servidor no encontró lo que buscas");
        } else if (message.contains("500") || message.contains("Server")) {
            Toast.error("Error del Servidor", 
                "El servidor está teniendo problemas");
        } else if (message.contains("403") || message.contains("Forbidden")) {
            Toast.error("Acceso Denegado", 
                "No tienes permiso para acceder a esto");
        } else {
            Toast.exception("Error de Conexión", e);
        }
    }
    
    /**
     * Maneja errores de validación
     * Uso: ToastHelper.validationError("Email", "formato inválido");
     */
    public static void validationError(String field, String issue) {
        Toast.formError(field, "Error: " + issue);
    }
    
    /**
     * Valida campo no vacío
     * Retorna true si es válido, false y muestra toast si no
     */
    public static boolean validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            Toast.formError(fieldName, fieldName + " es requerido");
            return false;
        }
        return true;
    }
    
    /**
     * Valida rango numérico
     * Retorna true si es válido, false y muestra toast si no
     */
    public static boolean validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            Toast.formError(fieldName, 
                fieldName + " debe estar entre " + min + " y " + max);
            return false;
        }
        return true;
    }
    
    /**
     * Valida email
     * Retorna true si es válido, false y muestra toast si no
     */
    public static boolean validateEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            Toast.formError("Email Inválido", 
                "Ingresa un correo electrónico válido");
            return false;
        }
        return true;
    }
    
    /**
     * Valida contraseña (mínimo 6 caracteres)
     * Retorna true si es válido, false y muestra toast si no
     */
    public static boolean validatePassword(String password) {
        if (password.length() < 6) {
            Toast.formError("Contraseña Débil", 
                "La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        return true;
    }
    
    /**
     * Maneja transacciones de pago
     * Uso: ToastHelper.handlePaymentError(e);
     */
    public static void handlePaymentError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Error de pago";
        
        if (message.contains("insufficient") || message.contains("Insufficient")) {
            Toast.warning("Dinero Insuficiente", 
                "No tienes suficientes monedas/dinero");
        } else if (message.contains("declined") || message.contains("Declined")) {
            Toast.error("Pago Rechazado", 
                "Tu tarjeta/cuenta fue rechazada");
        } else if (message.contains("expired") || message.contains("Expired")) {
            Toast.error("Pago Expirado", 
                "Tu método de pago ha expirado");
        } else {
            Toast.exception("Error de Pago", e);
        }
    }
    
    /**
     * Operación exitosa con detalles
     * Uso: ToastHelper.operationSuccess("Compra", "articulo.nombre", "100 monedas");
     */
    public static void operationSuccess(String operation, String item, String details) {
        Toast.success(operation + " Realizada", 
            item + " - " + details);
    }
    
    /**
     * Carga iniciada (para operaciones largas)
     * Uso: ToastHelper.loadingStarted("Descargando datos...");
     */
    public static void loadingStarted(String activity) {
        Toast.info("Cargando... ⏳", activity);
    }
    
    /**
     * Carga completada
     * Uso: ToastHelper.loadingComplete("Datos", "150 registros");
     */
    public static void loadingComplete(String dataType, String count) {
        Toast.success("Cargado", "Se obtuvieron " + count + " " + dataType);
    }
    
    /**
     * Sincronización completada
     * Uso: ToastHelper.syncComplete("Progreso de Misión");
     */
    public static void syncComplete(String dataType) {
        Toast.success("Sincronizado", dataType + " actualizado en el servidor");
    }
    
    /**
     * Error de sincronización
     * Uso: ToastHelper.syncError(e);
     */
    public static void syncError(Exception e) {
        Toast.error("Error de Sincronización", 
            "Los cambios no se guardaron en el servidor");
    }
    
    /**
     * Evento épico (logro, misión especial, etc.)
     * Uso: ToastHelper.epicEvent("LOGRO DESBLOQUEADO", "Completaste 10 misiones");
     */
    public static void epicEvent(String title, String description) {
        Toast.epic("⭐ " + title, description);
    }
    
    /**
     * Eliminar exitoso
     * Uso: ToastHelper.deleteSuccess("Usuario", "Juan");
     */
    public static void deleteSuccess(String type, String name) {
        Toast.success("Eliminado", name + " fue eliminado correctamente");
    }
    
    /**
     * Error al eliminar
     * Uso: ToastHelper.deleteError(e, "Usuario");
     */
    public static void deleteError(Exception e, String type) {
        Toast.error("Error al Eliminar", 
            "No se pudo eliminar el " + type);
    }
    
    /**
     * Verificación fallida (acceso denegado de lógica, no de cliente)
     * Uso: ToastHelper.verificationFailed("Permisos insuficientes");
     */
    public static void verificationFailed(String reason) {
        Toast.warning("Verificación Fallida", reason);
    }
}