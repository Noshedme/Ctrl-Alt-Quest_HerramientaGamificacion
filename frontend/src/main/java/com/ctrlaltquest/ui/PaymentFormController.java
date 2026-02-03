package com.ctrlaltquest.ui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.ctrlaltquest.dao.PaymentDAO;
import com.ctrlaltquest.models.PaymentProduct;
import com.ctrlaltquest.services.StripePaymentService;
import java.time.YearMonth;

/**
 * Controlador para el formulario de pago REAL con Stripe.
 * Procesa transacciones reales a cuentas bancarias.
 * 
 * INSTRUCCIONES PARA USAR PAGOS REALES:
 * 1. Ve a https://dashboard.stripe.com/apikeys
 * 2. Copia tu Secret Key
 * 3. Set la variable de entorno: STRIPE_SECRET_KEY=sk_...
 * 4. Reinicia la aplicación
 * 5. Los pagos ahora serán REALES y se transferirán a tu cuenta bancaria
 */
public class PaymentFormController {
    
    @FXML private Label titleLabel;
    @FXML private Label productNameLabel;
    @FXML private Label priceLabel;
    @FXML private TextArea productDescriptionText;
    
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private PasswordField cvvField;
    
    @FXML private Button payButton;
    @FXML private Button cancelButton;
    @FXML private Label warningLabel;
    @FXML private ProgressBar progressBar;
    
    private PaymentProduct product;
    private int userId;
    private Integer deviceId;
    private Runnable onPaymentSuccess;
    private boolean processingPayment = false;

    /**
     * Inicializa el controlador con los datos del producto.
     */
    public void inicializar(int userId, PaymentProduct product, Integer deviceId, Runnable onSuccess) {
        this.userId = userId;
        this.product = product;
        this.deviceId = deviceId;
        this.onPaymentSuccess = onSuccess;
        
        // Mostrar datos del producto
        if (productNameLabel != null) {
            productNameLabel.setText(product.getName());
        }
        if (priceLabel != null) {
            priceLabel.setText(String.format("$%.2f USD", product.getPriceCents() / 100.0));
        }
        if (productDescriptionText != null) {
            productDescriptionText.setText(product.getDescription());
            productDescriptionText.setWrapText(true);
            productDescriptionText.setEditable(false);
        }
        
        // Advertencia de Stripe
        if (warningLabel != null) {
            String stripeStatus = System.getenv("STRIPE_SECRET_KEY") != null ? 
                "💳 Pagos REALES con Stripe activados" : 
                "⚠️  Pagos simulados (sin STRIPE_SECRET_KEY configurada)";
            warningLabel.setText(stripeStatus);
            warningLabel.setStyle("-fx-text-fill: " + 
                (System.getenv("STRIPE_SECRET_KEY") != null ? "#2ecc71" : "#e74c3c") + 
                "; -fx-font-weight: bold;");
        }
        
        if (progressBar != null) {
            progressBar.setVisible(false);
        }
    }

    /**
     * Método alternativo más simple para establecer datos del producto desde StoreViewController.
     * Se usa cuando se abre el formulario desde la tienda sin datos de usuario complejos.
     */
    public void setProductoInfo(String nombre, double precio, String descripcion) {
        if (productNameLabel != null) {
            productNameLabel.setText(nombre);
        }
        if (priceLabel != null) {
            priceLabel.setText(String.format("$%.2f USD", precio));
        }
        if (productDescriptionText != null) {
            productDescriptionText.setText(descripcion);
            productDescriptionText.setWrapText(true);
            productDescriptionText.setEditable(false);
        }
        
        // Advertencia de Stripe
        if (warningLabel != null) {
            String stripeStatus = System.getenv("STRIPE_SECRET_KEY") != null ? 
                "💳 Pagos REALES con Stripe activados" : 
                "⚠️  Pagos simulados (sin STRIPE_SECRET_KEY configurada)";
            warningLabel.setText(stripeStatus);
            warningLabel.setStyle("-fx-text-fill: " + 
                (System.getenv("STRIPE_SECRET_KEY") != null ? "#2ecc71" : "#e74c3c") + 
                "; -fx-font-weight: bold;");
        }
        
        if (progressBar != null) {
            progressBar.setVisible(false);
        }
        
        // Por defecto, userId = 1 (usuario de demostración)
        this.userId = 1;
        this.deviceId = null;
    }
    
    
    @FXML
    private void handlePayClick(ActionEvent event) {
        if (processingPayment) {
            mostrarError("Ya hay un pago en proceso. Por favor espera...");
            return;
        }

        // Validar formulario
        if (!validarFormulario()) {
            return;
        }

        // Obtener datos
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String cardNumber = cardNumberField.getText().replaceAll("\\s+", "");
        String expiry = expiryField.getText().trim();
        String cvv = cvvField.getText().trim();

        // Procesar pago en background
        procesarPagoEnBackground(fullName, email, cardNumber, expiry, cvv);
    }
    
    @FXML
    private void handleCancelClick(ActionEvent event) {
        cerrarVentana();
    }
    
    /**
     * Valida los datos del formulario.
     */
    private boolean validarFormulario() {
        String nombre = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String cardNumber = cardNumberField.getText().replaceAll("\\s+", "");
        String expiry = expiryField.getText().trim();
        String cvv = cvvField.getText().trim();

        // Validar nombre
        if (nombre.isEmpty() || nombre.length() < 3) {
            mostrarError("Por favor ingresa un nombre válido (mínimo 3 caracteres)");
            return false;
        }

        // Validar email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            mostrarError("Por favor ingresa un email válido");
            return false;
        }

        // Validar número de tarjeta
        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            mostrarError("El número de tarjeta debe tener entre 13 y 19 dígitos");
            return false;
        }
        if (!cardNumber.matches("\\d+")) {
            mostrarError("El número de tarjeta debe contener solo dígitos");
            return false;
        }

        // Validar vencimiento (MM/YY)
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            mostrarError("El vencimiento debe estar en formato MM/YY (ej: 12/25)");
            return false;
        }
        
        try {
            int mes = Integer.parseInt(expiry.split("/")[0]);
            int año = Integer.parseInt(expiry.split("/")[1]);
            if (mes < 1 || mes > 12) {
                mostrarError("Mes de vencimiento inválido (01-12)");
                return false;
            }
            // Verificar que no esté vencida
            YearMonth expiryDate = YearMonth.of(2000 + año, mes);
            if (expiryDate.isBefore(YearMonth.now())) {
                mostrarError("La tarjeta está vencida");
                return false;
            }
        } catch (Exception e) {
            mostrarError("Formato de vencimiento inválido");
            return false;
        }

        // Validar CVV
        if (!cvv.matches("\\d{3,4}")) {
            mostrarError("El CVV debe tener 3 o 4 dígitos");
            return false;
        }

        return true;
    }
    
    /**
     * Procesa el pago en un thread separado para no bloquear la UI.
     * Conecta REALMENTE con Stripe si está configurado.
     */
    private void procesarPagoEnBackground(String fullName, String email, String cardNumber, 
                                         String expiry, String cvv) {
        processingPayment = true;
        payButton.setDisable(true);
        if (progressBar != null) {
            progressBar.setVisible(true);
        }
        
        Task<Boolean> pagoTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    updateProgress(0, 100);
                    updateMessage("Creando orden de pago...");
                    
                    // 1. Crear orden en BD
                    String orderUUID = PaymentDAO.crearOrdenPago(userId, product.getId(), deviceId);
                    if (orderUUID == null) {
                        updateMessage("Error al crear la orden");
                        return false;
                    }
                    
                    updateProgress(20, 100);
                    updateMessage("Verificando datos de tarjeta...");
                    
                    // 2. Validar número de tarjeta con algoritmo de Luhn
                    if (!StripePaymentService.validarNumeroTarjeta(cardNumber)) {
                        updateMessage("Número de tarjeta inválido");
                        return false;
                    }
                    
                    updateProgress(40, 100);
                    updateMessage("Tokenizando datos de tarjeta...");
                    
                    // 3. Crear token de tarjeta en Stripe
                    int expMonth = Integer.parseInt(expiry.split("/")[0]);
                    int expYear = Integer.parseInt("20" + expiry.split("/")[1]);
                    
                    String cardToken = StripePaymentService.crearTokenTarjeta(
                        cardNumber, expMonth, expYear, cvv
                    );
                    
                    if (cardToken == null) {
                        updateMessage("Error en los datos de la tarjeta");
                        return false;
                    }
                    
                    updateProgress(60, 100);
                    updateMessage("Creando intención de pago en Stripe...");
                    
                    // 4. Crear Payment Intent en Stripe
                    String clientSecret = StripePaymentService.crearPaymentIntent(
                        product.getPriceCents(),
                        "usd",
                        email,
                        fullName,
                        orderUUID
                    );
                    
                    if (clientSecret == null) {
                        updateMessage("Error conectando con Stripe");
                        return false;
                    }
                    
                    updateProgress(80, 100);
                    updateMessage("Procesando transacción...");
                    
                    // 5. Procesar el pago con Stripe
                    String transactionId = StripePaymentService.procesarPagoConToken(
                        clientSecret, cardToken
                    );
                    
                    if (transactionId == null) {
                        updateMessage("Pago rechazado. Verifica tus datos.");
                        // Registrar como fallido
                        PaymentDAO.registrarTransaccion(
                            orderUUID, "stripe", "failed", 
                            product.getPriceCents(), "usd", 
                            "failed", "{\"error\":\"pago_rechazado\"}"
                        );
                        return false;
                    }
                    
                    updateProgress(90, 100);
                    updateMessage("Finalizando compra...");
                    
                    // 6. Registrar transacción en BD
                    boolean registrado = PaymentDAO.registrarTransaccion(
                        orderUUID,
                        "stripe",
                        transactionId,
                        product.getPriceCents(),
                        "usd",
                        "success",
                        "{\"stripe_charge_id\":\"" + transactionId + "\",\"email\":\"" + email + "\"}"
                    );
                    
                    if (!registrado) {
                        updateMessage("Error guardando la transacción");
                        return false;
                    }
                    
                    // 7. Procesar compra (otorgar coins/items)
                    boolean compraProcessada = PaymentDAO.procesarCompraExitosa(orderUUID);
                    
                    if (!compraProcessada) {
                        updateMessage("Error entregando el producto");
                        return false;
                    }
                    
                    updateProgress(100, 100);
                    updateMessage("¡Pago completado exitosamente!");
                    
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("❌ Error en pago: " + e.getMessage());
                    e.printStackTrace();
                    updateMessage("Error: " + e.getMessage());
                    return false;
                }
            }
        };

        // Actualizar UI según el resultado
        pagoTask.setOnSucceeded(event -> {
            processingPayment = false;
            if (pagoTask.getValue()) {
                // Pago exitoso
                mostrarExito(
                    "¡Pago Exitoso!",
                    "Tu compra se ha completado.\n\n" +
                    "• Producto: " + product.getName() + "\n" +
                    "• Monto: $" + String.format("%.2f", product.getPriceCents() / 100.0) + " USD\n" +
                    "• El dinero se transfirió a tu cuenta bancaria\n" +
                    "• El producto fue agregado a tu inventario\n" +
                    "\nPuedes cerrar esta ventana."
                );
                if (onPaymentSuccess != null) {
                    onPaymentSuccess.run();
                }
                // Cerrar después de 3 segundos
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(this::cerrarVentana);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                mostrarError("Pago Fallido:\n" + pagoTask.getMessage());
                payButton.setDisable(false);
            }
            if (progressBar != null) {
                progressBar.setVisible(false);
            }
        });

        pagoTask.setOnFailed(event -> {
            processingPayment = false;
            mostrarError("Error procesando pago:\n" + pagoTask.getException().getMessage());
            payButton.setDisable(false);
            if (progressBar != null) {
                progressBar.setVisible(false);
            }
        });

        // Vincular progreso a la barra
        if (progressBar != null) {
            progressBar.progressProperty().bind(pagoTask.progressProperty());
        }
        
        new Thread(pagoTask).start();
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
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Cierra la ventana.
     */
    private void cerrarVentana() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
