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
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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

    // Thumbnails para las 10 clases
    @FXML private ImageView thumb1;
    @FXML private ImageView thumb2;
    @FXML private ImageView thumb3;
    @FXML private ImageView thumb4;
    @FXML private ImageView thumb5;
    @FXML private ImageView thumb6;
    @FXML private ImageView thumb7;
    @FXML private ImageView thumb8;
    @FXML private ImageView thumb9;
    @FXML private ImageView thumb10;

    private Character characterData;
    private int userId;

    @FXML
    public void initialize() {
        lblUsernameHeader.setText("SINTONIZANDO...");
        this.userId = SessionManager.getInstance().getUserId();
        
        cargarDatosUsuario();
        
        // Inicializar Toast y efectos cuando la escena esté lista
        lblUsernameHeader.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                initializeToast();
                agregarHoverEffects();
                agregarTooltips();
                cargarThumbnails(); // Cargar thumbnails cuando la escena esté lista
            }
        });
        
        prepararAnimacion(headerCard);
        prepararAnimacion(configCard);
        prepararAnimacion(securityCard);
        prepararAnimacion(footerContainer);
        
        animarEntrada();

        // Hacer el avatar circular
        if (imgAvatar != null) {
            Circle clip = new Circle(55, 55, 55); // Radio basado en fitWidth/2 = 110/2
            imgAvatar.setClip(clip);
            imgAvatar.setEffect(new Glow(0.3)); // Añadir un glow sutil para más visual
        }
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

    // Método para cargar thumbnails
    private void cargarThumbnails() {
        thumb1.setImage(new Image(getClass().getResource("/assets/images/sprites/class_1_idle.png").toExternalForm()));
        thumb2.setImage(new Image(getClass().getResource("/assets/images/sprites/class_2_idle.png").toExternalForm()));
        thumb3.setImage(new Image(getClass().getResource("/assets/images/sprites/class_3_idle.png").toExternalForm()));
        thumb4.setImage(new Image(getClass().getResource("/assets/images/sprites/class_4_idle.png").toExternalForm()));
        thumb5.setImage(new Image(getClass().getResource("/assets/images/sprites/class_5_idle.png").toExternalForm()));
        thumb6.setImage(new Image(getClass().getResource("/assets/images/sprites/class_6_idle.png").toExternalForm()));
        thumb7.setImage(new Image(getClass().getResource("/assets/images/sprites/class_7_idle.png").toExternalForm()));
        thumb8.setImage(new Image(getClass().getResource("/assets/images/sprites/class_8_idle.png").toExternalForm()));
        thumb9.setImage(new Image(getClass().getResource("/assets/images/sprites/class_9_idle.png").toExternalForm()));
        thumb10.setImage(new Image(getClass().getResource("/assets/images/sprites/class_10_idle.png").toExternalForm()));

        // Añadir hover y click a cada thumbnail
        agregarThumbHover(thumb1, 1);
        agregarThumbHover(thumb2, 2);
        agregarThumbHover(thumb3, 3);
        agregarThumbHover(thumb4, 4);
        agregarThumbHover(thumb5, 5);
        agregarThumbHover(thumb6, 6);
        agregarThumbHover(thumb7, 7);
        agregarThumbHover(thumb8, 8);
        agregarThumbHover(thumb9, 9);
        agregarThumbHover(thumb10, 10);
    }

    private void agregarThumbHover(ImageView thumb, int classId) {
        thumb.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), thumb);
            st.setToX(1.2); st.setToY(1.2);
            st.play();
            thumb.setEffect(new Glow(0.5));
        });
        thumb.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), thumb);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
            thumb.setEffect(null);
        });
        thumb.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> selectClass(classId));
    }

    private void selectClass(int classId) {
        if (characterData == null) return;
        SoundManager.playClickSound();
        // Animaciones como en handleChangeAvatar()
        RotateTransition rotate = new RotateTransition(Duration.millis(500), imgAvatar);
        rotate.setByAngle(360);
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), imgAvatar);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(0.5); scale.setToY(0.5);
        scale.setAutoReverse(true); scale.setCycleCount(2);
        rotate.setOnFinished(event -> {
            characterData.setClassId(classId);
            cargarAvatar(classId);
        });
        rotate.play(); scale.play();
        // Guardar en BD
        Task<Boolean> updateTask = new Task<>() {
            @Override protected Boolean call() { return CharacterDAO.updateCharacterClass(characterData.getId(), classId); }
        };
        updateTask.setOnSucceeded(e -> {
            if (updateTask.getValue()) Toast.success("IMAGEN ACTUALIZADA", "Seleccionaste la Variante " + classId);
            else Toast.error("ERROR", "No se guardó el cambio.");
        });
        new Thread(updateTask).start();
    }

    @FXML
    private void handleChangeAvatar() {
        // Este método ya no es necesario con thumbnails, pero lo mantenemos por compatibilidad o como ciclo fallback si quieres
        if (characterData == null) return;
        SoundManager.playClickSound();

        int currentClass = characterData.getClassId();
        int nextClass = (currentClass % 10) + 1; // Ciclo de 10 clases

        RotateTransition rotate = new RotateTransition(Duration.millis(500), imgAvatar);
        rotate.setByAngle(360);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), imgAvatar);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(0.5); scale.setToY(0.5);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        rotate.setOnFinished(event -> {
            characterData.setClassId(nextClass);
            cargarAvatar(nextClass);
        });
        
        rotate.play();
        scale.play();

        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() {
                return CharacterDAO.updateCharacterClass(characterData.getId(), nextClass);
            }
        };

        updateTask.setOnSucceeded(e -> {
            if (updateTask.getValue()) {
                Toast.success("IMAGEN ACTUALIZADA", "Tu apariencia base ha cambiado a la Variante " + nextClass);
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
                // Animación de confirmación en el header
                animarConfirmacion(lblUsernameHeader);
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
                // Animación de confirmación en la sección de seguridad
                animarConfirmacion(securityCard);
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

    // Nuevas mejoras: Hover effects para dinamismo
    private void agregarHoverEffects() {
        // Hover en botones
        agregarHover(btnChangeAvatar);
        agregarHover(findButtonByText("GUARDAR CAMBIOS"));
        agregarHover(findButtonByText("ACTUALIZAR"));
        agregarHover(findButtonByText("📄 CSV"));
        agregarHover(findButtonByText("📊 PDF"));
        agregarHover(findButtonByText("ELIMINAR CUENTA"));

        // Hover en campos de texto para resaltar
        agregarFieldHover(txtUsername);
        agregarFieldHover(txtNewPass);
        agregarFieldHover(txtConfirmPass);

        // Hover en avatar para efecto de escala
        imgAvatar.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), imgAvatar);
            st.setToX(1.1); st.setToY(1.1);
            st.play();
        });
        imgAvatar.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), imgAvatar);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }

    private void agregarHover(Button button) {
        if (button == null) return;
        Glow glow = new Glow(0.5);
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            button.setEffect(glow);
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.05); st.setToY(1.05);
            st.play();
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            button.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
    }

    private void agregarFieldHover(TextField field) {
        if (field == null) return;
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "; -fx-border-color: #f7d27a; -fx-background-color: rgba(247, 210, 122, 0.1);");
            } else {
                field.setStyle(field.getStyle() + "; -fx-border-color: transparent; -fx-background-color: transparent;");
            }
        });
    }

    private void agregarFieldHover(PasswordField field) {
        if (field == null) return;
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "; -fx-border-color: #f7d27a; -fx-background-color: rgba(247, 210, 122, 0.1);");
            } else {
                field.setStyle(field.getStyle() + "; -fx-border-color: transparent; -fx-background-color: transparent;");
            }
        });
    }

    // Helper para encontrar botones por texto (ya que no tienen IDs)
    private Button findButtonByText(String text) {
        // Buscar en la escena, esto es aproximado; ajusta si es necesario
        for (Node node : lblUsernameHeader.getScene().getRoot().getChildrenUnmodifiable()) {
            if (node instanceof ScrollPane) {
                Node contentNode = ((ScrollPane) node).getContent();
                if (contentNode instanceof Parent) {
                    for (Node child : ((Parent) contentNode).getChildrenUnmodifiable()) {
                        if (child instanceof Button && ((Button) child).getText().equals(text)) {
                            return (Button) child;
                        } else if (child instanceof Parent) {
                            Button btn = findButtonInContainer(child, text);
                            if (btn != null) return btn;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Button findButtonInContainer(Node container, String text) {
        if (container instanceof Parent) {
            for (Node child : ((Parent) container).getChildrenUnmodifiable()) {
                if (child instanceof Button && ((Button) child).getText().equals(text)) {
                    return (Button) child;
                } else if (child instanceof Parent) {
                    return findButtonInContainer(child, text);
                }
            }
        }
        return null;
    }

    // Agregar tooltips para interactividad
    private void agregarTooltips() {
        Tooltip.install(btnChangeAvatar, new Tooltip("Cambia la clase de tu avatar ciclando entre variantes."));
        Tooltip.install(txtUsername, new Tooltip("Ingresa un nuevo alias para tu agente."));
        Tooltip.install(txtEmail, new Tooltip("El correo no se puede editar."));
        Tooltip.install(txtNewPass, new Tooltip("Ingresa una nueva contraseña segura."));
        Tooltip.install(txtConfirmPass, new Tooltip("Confirma la nueva contraseña."));
        Tooltip.install(findButtonByText("📄 CSV"), new Tooltip("Exporta tu historial de misiones en formato CSV."));
        Tooltip.install(findButtonByText("📊 PDF"), new Tooltip("Exporta en PDF (en desarrollo)."));
        Tooltip.install(findButtonByText("ELIMINAR CUENTA"), new Tooltip("Elimina permanentemente tu cuenta. ¡Cuidado!"));

        // Tooltips para thumbnails
        Tooltip.install(thumb1, new Tooltip("Variante 1"));
        Tooltip.install(thumb2, new Tooltip("Variante 2"));
        Tooltip.install(thumb3, new Tooltip("Variante 3"));
        Tooltip.install(thumb4, new Tooltip("Variante 4"));
        Tooltip.install(thumb5, new Tooltip("Variante 5"));
        Tooltip.install(thumb6, new Tooltip("Variante 6"));
        Tooltip.install(thumb7, new Tooltip("Variante 7"));
        Tooltip.install(thumb8, new Tooltip("Variante 8"));
        Tooltip.install(thumb9, new Tooltip("Variante 9"));
        Tooltip.install(thumb10, new Tooltip("Variante 10"));
    }

    // Animación de confirmación para más dinamismo
    private void animarConfirmacion(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.5);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }
}