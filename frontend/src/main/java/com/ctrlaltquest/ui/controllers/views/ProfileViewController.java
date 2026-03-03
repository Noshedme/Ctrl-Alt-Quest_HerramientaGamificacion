package com.ctrlaltquest.ui.controllers.views;

import java.io.File;
import java.net.URL;

import com.ctrlaltquest.dao.CharacterDAO;
import com.ctrlaltquest.dao.MissionsDAO;
import com.ctrlaltquest.dao.UserDAO;
import com.ctrlaltquest.models.Character;
import com.ctrlaltquest.services.SessionManager;
import com.ctrlaltquest.ui.utils.SoundManager;
import com.ctrlaltquest.ui.utils.Toast;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class ProfileViewController {

    @FXML private ImageView imgAvatar;
    @FXML private Label lblUsernameHeader;
    @FXML private Label lblEmailHeader;
    @FXML private Label lblJoinDate;
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private Button btnChangeAvatar;

    @FXML private HBox headerCard;
    @FXML private VBox configCard;
    @FXML private VBox securityCard;
    @FXML private HBox footerContainer;

    private Character characterData;
    private int userId;

    @FXML
    public void initialize() {
        lblUsernameHeader.setText("SINTONIZANDO...");
        this.userId = SessionManager.getInstance().getUserId();
        
        cargarDatosUsuario();
        
        // Inicializar Toast cuando la escena esté lista
        lblUsernameHeader.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                initializeToast();
            }
        });
        
        prepararAnimacion(headerCard);
        prepararAnimacion(configCard);
        prepararAnimacion(securityCard);
        prepararAnimacion(footerContainer);
        
        animarEntrada();
    }

    private void initializeToast() {
        try {
            StackPane root = (StackPane) lblUsernameHeader.getScene().getRoot();
            
            // Crear contenedor de Toast
            VBox toastContainer = new VBox();
            toastContainer.setPrefSize(400, 600);
            toastContainer.setStyle("-fx-background-color: transparent;");
            toastContainer.setMouseTransparent(true);
            
            // Inicializar el sistema de Toast
            Toast.initialize(toastContainer);
            
            // Añadir al root
            if (root != null && !root.getChildren().contains(toastContainer)) {
                root.getChildren().add(toastContainer);
                StackPane.setAlignment(toastContainer, javafx.geometry.Pos.TOP_RIGHT);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Toast: " + e.getMessage());
        }
    }

    private void cargarDatosUsuario() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return UserDAO.getUserEmail(userId);
            }
        };
        
        task.setOnSucceeded(e -> {
            String email = task.getValue();
            lblEmailHeader.setText(email);
            txtEmail.setText(email);
            lblJoinDate.setText("ESTADO: OPERATIVO");
        });
        
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void setPlayerData(Character c) {
        this.characterData = c;
        if (c != null) {
            Platform.runLater(() -> {
                lblUsernameHeader.setText(c.getName().toUpperCase());
                txtUsername.setText(c.getName());
                cargarAvatar(c.getClassId());
            });
        }
    }

    private void cargarAvatar(int classId) {
        try {
            // Nota: Aquí se debería leer una ruta personalizada si existe
            // Para mantener compatibilidad con tu sistema de "clases", rotamos imágenes base:
            String path = "/assets/images/sprites/class_" + classId + "_idle.png";
            URL url = getClass().getResource(path);
            
            if (url == null) {
                path = "/assets/images/sprites/base/class_" + classId + ".png";
                url = getClass().getResource(path);
            }
            if (url == null) {
                url = getClass().getResource("/assets/images/sprites/class_1_idle.png");
            }
            
            if (url != null) {
                imgAvatar.setImage(new Image(url.toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error visualizando avatar: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangeAvatar() {
        if (characterData == null) return;
        SoundManager.playClickSound();

        int currentClass = characterData.getClassId();
        int nextClass = (currentClass % 3) + 1; // Ciclo de clases 1 -> 2 -> 3 -> 1

        // 1. Actualizar Visual Instantáneamente
        characterData.setClassId(nextClass);
        cargarAvatar(nextClass);

        // 2. Guardar en BD usando el nuevo método DAO
        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() {
                return CharacterDAO.updateCharacterClass(characterData.getId(), nextClass);
            }
        };

        updateTask.setOnSucceeded(e -> {
            if (updateTask.getValue()) {
                Toast.success("IMAGEN ACTUALIZADA", "Tu apariencia base ha cambiado a la Variante " + nextClass);
                // Aquí el HomeController debería detectar el cambio en la siguiente recarga, 
                // pero si quieres instantáneo, se recomienda usar un EventBus o similar.
            } else {
                Toast.error("ERROR", "No se guardó el cambio de imagen.");
            }
        });

        Thread t = new Thread(updateTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleSaveProfile() {
        SoundManager.playClickSound();
        String newName = txtUsername.getText().trim();
        
        if (newName.length() < 3) {
            Toast.warning("ERROR", "El nombre debe tener al menos 3 caracteres.");
            return;
        }
        
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return CharacterDAO.updateCharacterName(characterData.getId(), newName);
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue()) {
                lblUsernameHeader.setText(newName.toUpperCase());
                characterData.setName(newName);
                SoundManager.playSuccessSound();
                Toast.success("GUARDADO", "Perfil actualizado correctamente.");
            } else {
                Toast.error("ERROR", "Fallo al guardar en la base de datos.");
            }
        });
        
        Thread t = new Thread(saveTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleChangePassword() {
        String p1 = txtNewPass.getText();
        String p2 = txtConfirmPass.getText();
        
        if (p1.isEmpty() || !p1.equals(p2)) {
            Toast.error("ERROR", "Las contraseñas no coinciden o están vacías.");
            return;
        }
        
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() { return UserDAO.updatePassword(userId, p1); }
        };
        t.setOnSucceeded(e -> {
            if(t.getValue()) {
                Toast.success("ÉXITO", "Contraseña de seguridad actualizada.");
                txtNewPass.clear(); 
                txtConfirmPass.clear();
            } else {
                Toast.error("ERROR", "No se pudo actualizar la contraseña.");
            }
        });
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void handleExportCSV() {
        SoundManager.playClickSound();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Historial de Misiones");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));
        fileChooser.setInitialFileName("historial_misiones.csv");
        
        File file = fileChooser.showSaveDialog(lblUsernameHeader.getScene().getWindow());
        if (file != null) {
            Task<Boolean> t = new Task<>() {
                @Override protected Boolean call() { return MissionsDAO.exportMissionHistoryToCSV(userId, file); }
            };
            t.setOnSucceeded(e -> {
                if(t.getValue()) Toast.success("EXPORTADO", "Archivo CSV generado con éxito en tu equipo.");
                else Toast.error("ERROR", "Fallo al intentar exportar el archivo.");
            });
            Thread th = new Thread(t);
            th.setDaemon(true);
            th.start();
        }
    }

    @FXML
    private void handleDeleteAccount() {
        // Lógica de borrado (sin cambios respecto a lo que ya funcionaba)
        Toast.warning("PELIGRO", "Esta función borrará todos tus datos. Contáctate con el Admin.");

    }

    // --- Animaciones y Alertas ---
    
    private void prepararAnimacion(Node node) {
        if(node!=null) { node.setOpacity(0); node.setTranslateY(20); }
    }
    
    private void animarEntrada() {
        playAnimation(headerCard, 0);
        playAnimation(configCard, 100);
        playAnimation(securityCard, 200);
        playAnimation(footerContainer, 300);
    }
    
    private void playAnimation(Node node, int delay) {
        if(node==null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), node);
        tt.setToY(0); tt.setDelay(Duration.millis(delay));
        
        FadeTransition ft = new FadeTransition(Duration.millis(600), node);
        ft.setToValue(1); ft.setDelay(Duration.millis(delay));
        
        tt.play(); ft.play();
    }
    
    private void mostrarAlerta(javafx.scene.control.Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            switch (type) {
                case INFORMATION -> Toast.info(title, content);
                case ERROR -> Toast.error(title, content);
                case WARNING -> Toast.warning(title, content);
                case CONFIRMATION -> Toast.info(title, content);
                default -> Toast.info(title, content);
            }
        });
    }
    
    @FXML 
    private void handleExportPDF() { 
        SoundManager.playClickSound();
        Toast.info("INFO", "Generación de PDF en desarrollo. Usa CSV por ahora."); 
    }
}