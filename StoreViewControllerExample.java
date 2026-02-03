package com.ctrlaltquest.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import com.ctrlaltquest.dao.StoreDAO;
import com.ctrlaltquest.dao.PaymentProductDAO;
import com.ctrlaltquest.dao.InventoryDAO;
import com.ctrlaltquest.models.StoreOffer;
import com.ctrlaltquest.models.PaymentProduct;
import com.ctrlaltquest.models.InventoryItem;
import com.ctrlaltquest.services.BoostService;

import java.io.IOException;
import java.util.List;

/**
 * Controlador mejorado para la tienda.
 * Integra compras con coins y dinero real, así como gestión de boosts.
 * 
 * NOTA: Este es un ejemplo de cómo integrar el sistema de tienda.
 * Reemplazar en el StoreViewController existente.
 */
public class StoreViewControllerExample {
    
    // UI Components
    @FXML private VBox storeContainer;
    @FXML private Label userCoinsLabel;
    @FXML private Label boostIndicator;
    @FXML private ScrollPane offerScroll;
    @FXML private VBox offersList;
    
    // Sesión
    private int userId;
    private int currentCoins;
    
    /**
     * Inicializa la vista de tienda.
     */
    @FXML
    public void initialize() {
        // Obtener userId de sesión (este es un ejemplo)
        userId = 3; // En producción, obtener de sesión
        
        // Cargar datos iniciales
        cargarCoinsActuales();
        cargarOfertas();
        cargarBoostActivo();
    }
    
    /**
     * Carga el saldo actual de coins del usuario.
     */
    private void cargarCoinsActuales() {
        // En producción, obtener de BD o sesión
        currentCoins = 1250;
        actualizarUICoin();
    }
    
    /**
     * Actualiza la etiqueta de coins en UI.
     */
    private void actualizarUICoin() {
        userCoinsLabel.setText(currentCoins + " 💰 Coins");
    }
    
    /**
     * Carga y muestra las ofertas disponibles.
     */
    private void cargarOfertas() {
        try {
            // Obtener ofertas desde BD
            List<StoreOffer> ofertas = StoreDAO.obtenerOfertas();
            
            // Limpiar lista
            offersList.getChildren().clear();
            
            // Crear UI para cada oferta
            for (StoreOffer oferta : ofertas) {
                HBox ofertaCard = crearCardOferta(oferta);
                offersList.getChildren().add(ofertaCard);
            }
            
            // Si no hay ofertas
            if (ofertas.isEmpty()) {
                Label vacio = new Label("No hay ofertas disponibles");
                vacio.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
                offersList.getChildren().add(vacio);
            }
            
        } catch (Exception e) {
            System.err.println("Error al cargar ofertas: " + e.getMessage());
            mostrarError("Error al cargar las ofertas");
        }
    }
    
    /**
     * Crea una tarjeta visual para una oferta.
     */
    private HBox crearCardOferta(StoreOffer oferta) {
        HBox card = new HBox(15);
        card.setStyle("-fx-border-color: #00d4ff; -fx-border-width: 2; " +
                      "-fx-padding: 10; -fx-border-radius: 5;");
        
        // Información
        VBox info = new VBox(5);
        Label nombre = new Label(oferta.getName());
        nombre.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
        Label desc = new Label(oferta.getDescription());
        desc.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12;");
        Label precio = new Label(oferta.getPrice() + " 💰 Coins");
        precio.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 13; -fx-font-weight: bold;");
        
        info.getChildren().addAll(nombre, desc, precio);
        
        // Botones
        VBox botones = new VBox(5);
        Button comprarBtn = new Button("Comprar con Coins");
        comprarBtn.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        comprarBtn.setOnAction(e -> comprarConCoins(oferta));
        
        botones.getChildren().add(comprarBtn);
        
        card.getChildren().addAll(info, botones);
        return card;
    }
    
    /**
     * Maneja la compra con coins.
     */
    @FXML
    private void comprarConCoins(StoreOffer oferta) {
        try {
            // Validar coins
            if (currentCoins < oferta.getPrice()) {
                mostrarError("No tienes suficientes coins.\n" +
                            "Necesitas: " + oferta.getPrice() + " 💰\n" +
                            "Tienes: " + currentCoins + " 💰");
                return;
            }
            
            // Procesar compra
            System.out.println("Comprando oferta " + oferta.getId() + " con coins...");
            boolean exito = StoreDAO.comprarConCoins(userId, oferta.getId());
            
            if (exito) {
                // Actualizar saldo local
                currentCoins -= oferta.getPrice();
                actualizarUICoin();
                
                // Mostrar confirmación
                mostrarExito("¡Compra Exitosa!", 
                            oferta.getName() + " ha sido agregado a tu inventario");
                
                // Recargar inventario si existe pantalla
                System.out.println("✅ Oferta comprada. Items agregados al inventario.");
                
            } else {
                mostrarError("Error al procesar la compra. Intenta nuevamente.");
            }
            
        } catch (Exception e) {
            System.err.println("Error en compra con coins: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al procesar la compra");
        }
    }
    
    /**
     * Abre el formulario de pago para compra con dinero real.
     */
    @FXML
    private void abrirTiendaPremium() {
        try {
            // Obtener productos disponibles
            List<PaymentProduct> productos = PaymentProductDAO.obtenerProductos();
            
            if (productos.isEmpty()) {
                mostrarInfo("No hay productos premium disponibles");
                return;
            }
            
            // Mostrar selector de producto
            Dialog<PaymentProduct> dialog = new Dialog<>();
            dialog.setTitle("Seleccionar Producto Premium");
            dialog.setHeaderText("Elige un producto para comprar");
            
            ComboBox<PaymentProduct> combo = new ComboBox<>();
            combo.getItems().addAll(productos);
            combo.setCellFactory(lv -> new ListCell<PaymentProduct>() {
                @Override
                protected void updateItem(PaymentProduct item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName() + " - " + item.getPriceFormatted());
                    }
                }
            });
            combo.setButtonCell(new ListCell<PaymentProduct>() {
                @Override
                protected void updateItem(PaymentProduct item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Seleccionar...");
                    } else {
                        setText(item.getName() + " - " + item.getPriceFormatted());
                    }
                }
            });
            
            dialog.getDialogPane().setContent(combo);
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);
            
            var resultado = dialog.showAndWait();
            if (resultado.isPresent()) {
                PaymentProduct seleccionado = resultado.get();
                abrirFormularioPago(seleccionado);
            }
            
        } catch (Exception e) {
            System.err.println("Error al abrir tienda premium: " + e.getMessage());
            mostrarError("Error al abrir la tienda premium");
        }
    }
    
    /**
     * Abre el formulario de pago.
     */
    private void abrirFormularioPago(PaymentProduct producto) {
        try {
            // Cargar FXML del formulario
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/payment_form.fxml"));
            
            Stage formStage = new Stage();
            formStage.setTitle("Formulario de Pago");
            formStage.setScene(new Scene(loader.load(), 600, 500));
            
            // Obtener controlador e inicializar
            PaymentFormController controller = loader.getController();
            controller.inicializar(userId, producto, null, () -> {
                // Callback: actualizar UI después de pago
                cargarCoinsActuales();
                cargarOfertas();
                mostrarExito("¡Pago exitoso!", 
                            "Has recibido " + producto.getCoinsReward() + " 💰 coins");
            });
            
            // Mostrar formulario
            formStage.show();
            
        } catch (IOException e) {
            System.err.println("Error al cargar formulario de pago: " + e.getMessage());
            mostrarError("Error al abrir el formulario de pago");
        }
    }
    
    /**
     * Carga y muestra información del boost activo.
     */
    private void cargarBoostActivo() {
        if (BoostService.getInstance().tieneBoostActivo(userId)) {
            var info = BoostService.getInstance().obtenerInfoBoost(userId);
            String itemName = (String) info.get("itemName");
            String tiempoRestante = (String) info.get("tiempoRestanteFormato");
            
            boostIndicator.setText("⚡ " + itemName + " activo - " + tiempoRestante);
            boostIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
        } else {
            boostIndicator.setText("⚫ Sin boost activo");
            boostIndicator.setStyle("-fx-text-fill: #cccccc;");
        }
    }
    
    /**
     * Obtiene el inventario del usuario.
     */
    @FXML
    private void abrirInventario() {
        try {
            List<InventoryItem> inventario = InventoryDAO.obtenerInventario(userId);
            
            // Crear diálogo con inventario
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Inventario");
            dialog.setHeaderText("Tus items (" + inventario.size() + ")");
            
            VBox content = new VBox(10);
            content.setStyle("-fx-padding: 10;");
            
            for (InventoryItem item : inventario) {
                HBox itemCard = crearCardInventario(item);
                content.getChildren().add(itemCard);
            }
            
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            dialog.showAndWait();
            
        } catch (Exception e) {
            System.err.println("Error al cargar inventario: " + e.getMessage());
            mostrarError("Error al cargar el inventario");
        }
    }
    
    /**
     * Crea una tarjeta para un item en el inventario.
     */
    private HBox crearCardInventario(InventoryItem item) {
        HBox card = new HBox(10);
        card.setStyle("-fx-border-color: #333; -fx-border-width: 1; " +
                      "-fx-padding: 8; -fx-background-color: #1a1a2e;");
        
        // Info del item
        VBox info = new VBox(3);
        Label nombre = new Label(item.getItem().getName());
        nombre.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;");
        Label rareza = new Label(item.getItem().getRarityEmoji());
        rareza.setStyle("-fx-font-size: 11; -fx-text-fill: #90EE90;");
        Label cantidad = new Label("x" + item.getQuantity());
        cantidad.setStyle("-fx-text-fill: #cccccc;");
        
        info.getChildren().addAll(nombre, rareza, cantidad);
        
        // Botones
        VBox botones = new VBox(5);
        
        if (item.isBoost()) {
            Button activarBtn = new Button("Activar");
            activarBtn.setStyle("-fx-font-size: 11; -fx-padding: 5;");
            activarBtn.setOnAction(e -> activarBoost(item));
            botones.getChildren().add(activarBtn);
        }
        
        if (item.getItem().getType().startsWith("CHEST") || 
            item.getItem().getType().startsWith("HELMET") ||
            item.getItem().getType().startsWith("LEGS")) {
            Button equiparBtn = new Button(item.isEquipped() ? "Desequipar" : "Equipar");
            equiparBtn.setStyle("-fx-font-size: 11; -fx-padding: 5;");
            equiparBtn.setOnAction(e -> toggleEquipar(item, equiparBtn));
            botones.getChildren().add(equiparBtn);
        }
        
        card.getChildren().addAll(info, botones);
        return card;
    }
    
    /**
     * Activa un boost.
     */
    private void activarBoost(InventoryItem boost) {
        try {
            int durationSeconds = boost.getItem().getBoostDurationSeconds();
            BoostService.getInstance().activarBoost(userId, durationSeconds, 
                                                     boost.getItem().getName());
            
            // Consumir item
            InventoryDAO.consumirItem(userId, boost.getItem().getId());
            
            cargarBoostActivo();
            mostrarExito("¡Boost Activado!", 
                        "x2 XP durante " + (durationSeconds / 60) + " minutos");
            
        } catch (Exception e) {
            System.err.println("Error al activar boost: " + e.getMessage());
            mostrarError("Error al activar el boost");
        }
    }
    
    /**
     * Equipa/desequipa un item.
     */
    private void toggleEquipar(InventoryItem item, Button btn) {
        try {
            if (item.isEquipped()) {
                InventoryDAO.desequiparItem(userId, item.getItem().getId());
                btn.setText("Equipar");
            } else {
                InventoryDAO.equiparItem(userId, item.getItem().getId());
                btn.setText("Desequipar");
            }
        } catch (Exception e) {
            System.err.println("Error al equipar/desequipar: " + e.getMessage());
            mostrarError("Error al equipar el item");
        }
    }
    
    /**
     * Muestra un diálogo de error.
     */
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    /**
     * Muestra un diálogo de éxito.
     */
    private void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Éxito");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    /**
     * Muestra un diálogo informativo.
     */
    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
