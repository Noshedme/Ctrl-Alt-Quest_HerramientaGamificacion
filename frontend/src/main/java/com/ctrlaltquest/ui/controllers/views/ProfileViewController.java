package com.ctrlaltquest.ui.controllers.views;

import java.io.File;
import java.net.URL;
import java.util.Optional;

import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.ui.utils.SoundManager;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class ProfileViewController {

    @FXML private ImageView imgAvatar;
    @FXML private Label lblUsernameHeader;
    @FXML private Label lblEmailHeader;
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    private Character characterData;

    @FXML
    public void initialize() {
        lblUsernameHeader.setText("Cargando datos...");
    }

    /**
     * Inyección de datos desde el Home
     */
    public void setPlayerData(Character c) {
        this.characterData = c;
        if (c != null) {
            // Actualizar UI con datos reales
            lblUsernameHeader.setText(c.getName().toUpperCase());
            txtUsername.setText(c.getName());
            
            // Simulamos el email (esto vendría de UserDAO en producción)
            String emailSimulado = "usuario_" + c.getId() + "@gremio.com";
            lblEmailHeader.setText(emailSimulado);
            txtEmail.setText(emailSimulado);

            cargarAvatar(c.getClassId());
        }
    }

    private void cargarAvatar(int classId) {
        try {
            String path = "/assets/images/sprites/base/class_" + classId + ".png";
            URL url = getClass().getResource(path);
            if (url != null) imgAvatar.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el avatar del perfil.");
        }
    }

    // --- ACCIONES DEL PERFIL ---

    @FXML
    private void handleSaveProfile() {
        SoundManager.playClickSound();
        String newName = txtUsername.getText().trim();
        
        if (newName.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos Incompletos", "El nombre de héroe no puede estar vacío.");
            return;
        }
        
        // Simulación de guardado
        // TODO: UserDAO.updateUsername(userId, newName);
        
        lblUsernameHeader.setText(newName.toUpperCase());
        
        // Feedback visual temporal en el botón (opcional, aquí usamos alerta simple)
        mostrarAlerta(Alert.AlertType.INFORMATION, "Perfil Actualizado", "Tu identidad ha sido reescrita en los registros.");
    }

    @FXML
    private void handleChangePassword() {
        SoundManager.playClickSound();
        String p1 = txtNewPass.getText();
        String p2 = txtConfirmPass.getText();

        if (p1.isEmpty() || p2.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Seguridad", "Los campos de la nueva llave están vacíos.");
            return;
        }
        if (!p1.equals(p2)) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Cifrado", "Las llaves no coinciden. Inténtalo de nuevo.");
            return;
        }

        // TODO: AuthDAO.updatePassword(...)
        mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Tu seguridad ha sido reforzada. No olvides tu nueva llave.");
        txtNewPass.clear();
        txtConfirmPass.clear();
    }

    @FXML
    private void handleExportCSV() {
        SoundManager.playClickSound();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Registro de Misiones");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("historial_aventura.csv");
        
        File file = fileChooser.showSaveDialog(txtUsername.getScene().getWindow());
        if (file != null) {
            // TODO: ServiceExport.toCSV(file, characterData.getId());
            System.out.println("Generando pergamino en: " + file.getAbsolutePath());
            
            // Simular proceso
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación", "El pergamino CSV ha sido sellado y entregado."));
            pause.play();
        }
    }

    @FXML
    private void handleExportPDF() {
        SoundManager.playErrorSound(); // Sonido diferente para indicar que no está listo
        mostrarAlerta(Alert.AlertType.INFORMATION, "En Construcción", "Los escribas aún están aprendiendo a generar PDFs. Próximamente en v1.0.");
    }

    @FXML
    private void handleDeleteAccount() {
        SoundManager.playErrorSound(); // Sonido de alerta grave
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("ZONA DE PELIGRO");
        alert.setHeaderText("¿INICIAR PROTOCOLO DE AUTODESTRUCCIÓN?");
        alert.setContentText("Esta acción es irreversible. Tu héroe, inventario y logros se perderán en el vacío para siempre.\n\n¿Estás absolutamente seguro?");

        // Estilizar alerta si tienes CSS global para diálogos
        // alert.getDialogPane().getStylesheets().add(...);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: UserDAO.deleteUser(userId);
            System.out.println("Cuenta eliminada... :(");
            // Aquí deberías redirigir al Login o cerrar la app
            // MainController.navigateToLogin();
        }
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}