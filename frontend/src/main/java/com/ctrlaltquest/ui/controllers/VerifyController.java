package com.ctrlaltquest.ui.controllers;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.AuditService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class VerifyController {

    @FXML private TextField codeField;
    
    private String userEmail;
    private final AuthDAO authDAO = new AuthDAO();

    // Este método lo llama el RegisterController al abrir la ventana
    public void setEmail(String email) {
        this.userEmail = email;
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showSimpleAlert("Campo Vacío", "Debes ingresar el código de verificación.");
            return;
        }

        try {
            // 1. Validar el código en la BD
            boolean isCorrect = authDAO.verifyUserCode(userEmail, code);

            if (isCorrect) {
                // 🛡️ AUDITORÍA: Éxito en la validación
                AuditService.log(null, "CUENTA_ACTIVADA", "Correo verificado: " + userEmail);
                
                showSimpleAlert("¡Verificación Completada!", "Tu cuenta ha sido activada. Volviendo al inicio...");
                
                // 2. Cerrar el overlay actual
                closeWindow();

                // 3. Redirigir la ventana principal al Login
                regresarAlLoginGlobal();
            } else {
                // 🛡️ AUDITORÍA: Intento fallido
                AuditService.log(null, "VERIFICACION_FALLIDA", "Código incorrecto para: " + userEmail);
                showSimpleAlert("Código Inválido", "El código no coincide con el enviado a tu correo.");
            }
        } catch (Exception e) {
            showSimpleAlert("Error en la Validación", "Ocurrió un error al verificar el código: " + e.getMessage());
        }
    }

    /**
     * Busca la ventana principal y cambia su contenido al Login con un Fade.
     */
    private void regresarAlLoginGlobal() {
        try {
            // Buscamos la ventana principal (la que no es transparente/modal)
            Stage mainStage = (Stage) Stage.getWindows().stream()
                    .filter(w -> w instanceof Stage && w.isShowing() && !((Stage) w).getStyle().equals(StageStyle.TRANSPARENT))
                    .findFirst()
                    .orElse(null);

            if (mainStage != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent loginRoot = loader.load();
                
                // Efecto de transición para suavizar el cambio
                loginRoot.setOpacity(0);
                mainStage.getScene().setRoot(loginRoot);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), loginRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        } catch (IOException e) {
            System.err.println("Error al redirigir al Login: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }

    private void showSimpleAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ctrl + Alt + Quest");
        alert.setHeaderText(title);
        alert.setContentText(content);
        
        // Intentar centrar la alerta en el modal
        if (codeField.getScene() != null) {
            alert.initOwner(codeField.getScene().getWindow());
        }
        
        alert.showAndWait();
    }
}