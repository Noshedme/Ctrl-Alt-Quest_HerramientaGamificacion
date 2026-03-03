package com.ctrlaltquest.ui.utils;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

/**
 * EJEMPLO DE CONTROLADOR CON TOAST NOTIFICATIONS
 * 
 * Este archivo muestra cómo implementar el sistema de Toast en un controlador
 * real (Login, Registro, etc.)
 * 
 * Copia y adapta estos patrones a tus controladores existentes
 */
public class ExampleControllerWithToast {
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    
    // ========== EJEMPLO 1: LOGIN CON VALIDACIÓN ==========
    
    @FXML
    private void handleLogin() {
        // Validar campos vacíos
        if (!ToastHelper.validateNotEmpty(emailField.getText(), "Email")) {
            return;
        }
        
        if (!ToastHelper.validateNotEmpty(passwordField.getText(), "Contraseña")) {
            return;
        }
        
        // Validar formato de email
        if (!ToastHelper.validateEmail(emailField.getText())) {
            return;
        }
        
        try {
            // Simular autenticación
            boolean loginExitoso = autenticar(emailField.getText(), passwordField.getText());
            
            if (loginExitoso) {
                Toast.success("¡Bienvenido!", "Login realizado correctamente");
                // Cambiar a pantalla principal
            } else {
                Toast.error("Login Fallido", "Email o contraseña incorrectos");
            }
            
        } catch (IllegalArgumentException e) {
            ToastHelper.validationError("Credenciales", e.getMessage());
        } catch (RuntimeException e) {
            ToastHelper.handleNetworkError(e);
        }
    }
    
    // ========== EJEMPLO 2: REGISTRO CON MÚLTIPLES VALIDACIONES ==========
    
    @FXML
    private void handleRegister() {
        // Validación en cadena usando ToastHelper
        if (!ToastHelper.validateNotEmpty(emailField.getText(), "Email")) {
            return;
        }
        if (!ToastHelper.validateEmail(emailField.getText())) {
            return;
        }
        if (!ToastHelper.validateNotEmpty(passwordField.getText(), "Contraseña")) {
            return;
        }
        if (!ToastHelper.validatePassword(passwordField.getText())) {
            return;
        }
        
        try {
            crearCuenta(emailField.getText(), passwordField.getText());
            Toast.success("¡Registro Exitoso!", 
                "Tu cuenta está lista. Inicia sesión para comenzar");
            
        } catch (IllegalArgumentException e) {
            // Email duplicado o inválido
            if (e.getMessage().contains("duplicate")) {
                Toast.error("Email Duplicado", 
                    "Este email ya está registrado");
            } else {
                ToastHelper.validationError("Email", e.getMessage());
            }
        } catch (Exception e) {
            Toast.exception("Error de Registro", e);
        }
    }
    
    // ========== EJEMPLO 3: OPERACIÓN EN DB (CRUD) ==========
    
    @FXML
    private void handleActualizarPerfil() {
        try {
            // Validar datos
            if (!ToastHelper.validateNotEmpty(emailField.getText(), "Email")) {
                return;
            }
            
            // Operación BD
            guardarCambiosEnBD(emailField.getText());
            
            // Éxito
            Toast.success("Perfil Actualizado", 
                "Tus cambios se guardaron correctamente");
            
        } catch (Exception e) {
            // Manejo especializado de errores BD
            ToastHelper.handleDatabaseError(e);
        }
    }
    
    // ========== EJEMPLO 4: ELIMINAR CON CONFIRMACIÓN ==========
    
    @FXML
    private void handleEliminarCuenta() {
        try {
            // Aquí normalmente irría un Dialog de confirmación
            // Por ahora asumimos confirmación del usuario
            
            boolean eliminado = eliminarCuentaBD();
            
            if (eliminado) {
                ToastHelper.deleteSuccess("Cuenta", "Tu cuenta fue eliminada");
                // Regresar a login
            }
            
        } catch (Exception e) {
            ToastHelper.deleteError(e, "cuenta");
        }
    }
    
    // ========== EJEMPLO 5: OPERACIÓN LARGA ASINCRÓNICA ==========
    
    @FXML
    private void handleCargarDatos() {
        ToastHelper.loadingStarted("Descargando datos del servidor...");
        
        // Ejecutar en thread separado
        new Thread(() -> {
            try {
                // Simular operación larga
                java.util.List<?> datos = cargarDatosDelServidor();
                
                // Volver al thread de JavaFX
                javafx.application.Platform.runLater(() -> {
                    ToastHelper.loadingComplete("registros", datos.size() + " elementos");
                    // Actualizar UI
                });
                
            } catch (java.io.IOException e) {
                javafx.application.Platform.runLater(() -> 
                    ToastHelper.handleNetworkError(e)
                );
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    Toast.exception("Error al Cargar", e)
                );
            }
        }).start();
    }
    
    // ========== EJEMPLO 6: TRANSACCIÓN/PAGO ==========
    
    @FXML
    private void handleComprarItem() {
        try {
            // Validar dinero disponible
            int dinero = obtenerDineroDisponible();
            int costo = 50;
            
            if (dinero < costo) {
                int falta = costo - dinero;
                Toast.warning("Dinero Insuficiente", 
                    "Te faltan " + falta + " monedas para esta compra");
                return;
            }
            
            // Procesar compra
            procesarCompra(costo);
            
            // Éxito épico
            Toast.gold("🛍️ ¡Compra Realizada!", 
                "Obtuviste un artículo especial");
            
            // Actualizar inventario
            actualizarInventario();
            
        } catch (IllegalArgumentException e) {
            ToastHelper.handlePaymentError(e);
        } catch (Exception e) {
            Toast.exception("Error de Transacción", e);
        }
    }
    
    // ========== EJEMPLO 7: SINCRONIZACIÓN CON SERVIDOR ==========
    
    @FXML
    private void handleSincronizar() {
        ToastHelper.loadingStarted("Sincronizando datos...");
        
        new Thread(() -> {
            try {
                sincronizarConServidor();
                
                javafx.application.Platform.runLater(() -> 
                    ToastHelper.syncComplete("Progreso")
                );
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    ToastHelper.syncError(e)
                );
            }
        }).start();
    }
    
    // ========== EJEMPLO 8: EVENTO ÉPICO/LOGRO ==========
    
    @FXML
    private void handleCompletarMisión() {
        try {
            completarMisionEnBD();
            
            // Evento épico
            ToastHelper.epicEvent("MISIÓN COMPLETADA", 
                "Has ganado 500 puntos de experiencia y un logro");
            
        } catch (Exception e) {
            Toast.exception("Error al Completar Misión", e);
        }
    }
    
    // ========== EJEMPLO 9: VALIDACIÓN DE MÚLTIPLES CAMPOS ==========
    
    @FXML
    private void handleGuardarConfiguracion() {
        // Validar rango de valores
        try {
            int volumen = Integer.parseInt(obteneryolumenField());
            if (!ToastHelper.validateRange(volumen, 0, 100, "Volumen")) {
                return;
            }
            
            int brillo = Integer.parseInt(obtenerBrilloField());
            if (!ToastHelper.validateRange(brillo, 0, 100, "Brillo")) {
                return;
            }
            
            guardarConfiguracion(volumen, brillo);
            Toast.success("Configuración Guardada", 
                "Tus preferencias fueron actualizadas");
            
        } catch (NumberFormatException e) {
            Toast.formError("Valores Inválidos", 
                "Los valores deben ser números entre 0 y 100");
        } catch (Exception e) {
            Toast.exception("Error al Guardar", e);
        }
    }
    
    // ========== EJEMPLO 10: MANEJO DE EXCEPCIONES ESPECÍFICAS ==========
    
    @FXML
    private void handleOperacionCompleja() {
        try {
            validarPermiso();
            conectarAServidor();
            enviarDatos();
            recibirConfirmacion();
            
            Toast.success("Operación Completa", 
                "Todos los datos fueron procesados correctamente");
            
        } catch (SecurityException e) {
            ToastHelper.verificationFailed(
                "No tienes permiso para realizar esta acción");
            
        } catch (java.net.ConnectException e) {
            ToastHelper.handleNetworkError(e);
            
        } catch (java.sql.SQLException e) {
            ToastHelper.handleDatabaseError(e);
            
        } catch (Exception e) {
            // Fallback para excepciones inesperadas
            Toast.exception("Error Inesperado", e);
        }
    }
    
    // ========== MÉTODOS DUMMY (son simulaciones) ==========
    
    private boolean autenticar(String email, String password) {
        return !email.isEmpty() && password.length() > 0;
    }
    
    private void crearCuenta(String email, String password) throws Exception {
        if (email.contains("duplicado")) {
            throw new IllegalArgumentException("Email duplicado");
        }
    }
    
    private void guardarCambiosEnBD(String email) throws Exception {}
    private boolean eliminarCuentaBD() { return true; }
    private java.util.List<?> cargarDatosDelServidor() throws Exception { 
        return java.util.Arrays.asList("a", "b", "c"); 
    }
    
    private int obtenerDineroDisponible() { return 100; }
    private void procesarCompra(int costo) {}
    private void actualizarInventario() {}
    
    private void sincronizarConServidor() throws Exception {}
    private void completarMisionEnBD() throws Exception {}
    
    private String obteneryolumenField() { return "50"; }
    private String obtenerBrilloField() { return "75"; }
    private void guardarConfiguracion(int volumen, int brillo) {}
    
    private void validarPermiso() throws SecurityException {}
    private void conectarAServidor() throws Exception {}
    private void enviarDatos() throws Exception {}
    private void recibirConfirmacion() throws Exception {}
}

/*
PATRÓN DE USO GENERAL:

1. ENTRADA:
   try {
       // Código que puede fallar
   } catch (SQLException e) {
       ToastHelper.handleDatabaseError(e);
   }

2. VALIDACIÓN:
   if (!ToastHelper.validateEmail(email)) {
       return; // El Toast se muestra automáticamente
   }

3. CARGA:
   ToastHelper.loadingStarted("Datos...");
   // ... operación ...
   ToastHelper.loadingComplete("registros", "100");

4. ÉXITO:
   Toast.success("Título", "Mensaje");

5. ESPECIAL:
   ToastHelper.epicEvent("LOGRO", "Descripción");
*/
