package com.ctrlaltquest.ui.controllers;

import java.io.IOException;

import com.ctrlaltquest.dao.AuthDAO;
import com.ctrlaltquest.services.AuditService;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class VerifyController {

    @FXML private TextField codeField;
    
    private String userEmail;
    private final AuthDAO authDAO = new AuthDAO();

    // Este método lo llama el RegisterController al abrir la ventana
    public void setEmail(String email) {
        this.userEmail = email;
    }

    @FXML
    public void initialize() {
        // init toast container if available
        try {
            StackPane root = (StackPane) codeField.getScene().getRoot();
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            Toast.initialize(toastContainer);
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showToast("Campo Vacío", "Debes ingresar el código de verificación.", Toast.ToastType.WARNING);
            return;
        }

        try {
            // 1. Validar el código en la BD
            boolean isCorrect = authDAO.verifyUserCode(userEmail, code);

            if (isCorrect) {
                // 🛡️ AUDITORÍA: Éxito en la validación
                AuditService.log(null, "CUENTA_ACTIVADA", "Correo verificado: " + userEmail);
                
                showToast("¡Verificación Completada!", "Tu cuenta ha sido activada. Volviendo al inicio...", Toast.ToastType.SUCCESS);
                
                // 2. Cerrar el overlay actual
                closeWindow();

                // 3. Redirigir la ventana principal al Login
                regresarAlLoginGlobal();
            } else {
                // 🛡️ AUDITORÍA: Intento fallido
                AuditService.log(null, "VERIFICACION_FALLIDA", "Código incorrecto para: " + userEmail);
                showToast("Código Inválido", "El código no coincide con el enviado a tu correo.", Toast.ToastType.ERROR);
            }
        } catch (Exception e) {
            showToast("Error en la Validación", "Ocurrió un error al verificar el código: " + e.getMessage(), Toast.ToastType.ERROR);
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

    private void showToast(String title, String message, Toast.ToastType type) {
        switch (type) {
            case SUCCESS:
                Toast.success(title, message);
                break;
            case ERROR:
                Toast.error(title, message);
                break;
            case WARNING:
                Toast.warning(title, message);
                break;
            default:
                Toast.info(title, message);
                break;
        }
    }
}