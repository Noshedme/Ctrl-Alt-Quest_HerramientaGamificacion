package com.ctrlaltquest.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para procesar pagos REALES con Stripe.
 * Integración completa con transferencias bancarias reales.
 */
public class StripePaymentService {
    
    // IMPORTANTE: Reemplaza con tu clave API de Stripe
    // Obtén en: https://dashboard.stripe.com/apikeys
    private static final String STRIPE_API_KEY = System.getenv("STRIPE_SECRET_KEY");
    
    static {
        if (STRIPE_API_KEY == null || STRIPE_API_KEY.isEmpty()) {
            System.err.println("⚠️  STRIPE_SECRET_KEY no está configurada. Set en variables de entorno.");
            System.err.println("   Para usar pagos REALES, agrega tu clave Stripe en: https://dashboard.stripe.com/apikeys");
        } else {
            Stripe.apiKey = STRIPE_API_KEY;
            System.out.println("✅ Stripe API inicializado correctamente.");
        }
    }

    /**
     * Crea un Payment Intent (intención de pago) en Stripe.
     * Este es el paso 1 para procesar pagos con tarjeta.
     * 
     * @param amountCents Monto en centavos (ej: 2999 = $29.99 USD)
     * @param currency Moneda (ej: "usd")
     * @param email Email del cliente
     * @param customerName Nombre del cliente
     * @param orderUUID UUID de la orden (para referencia)
     * @return clientSecret para frontend, o null si falla
     */
    public static String crearPaymentIntent(int amountCents, String currency, String email, 
                                           String customerName, String orderUUID) {
        try {
            // 1. Buscar o crear cliente en Stripe
            String customerId = obtenerOCrearCliente(email, customerName);
            
            // 2. Crear Payment Intent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) amountCents)
                .setCurrency(currency)
                .setCustomer(customerId)
                .setDescription("Compra de producto: " + orderUUID)
                .putMetadata("order_uuid", orderUUID)
                .putMetadata("tipo", "tienda_producto")
                .setConfirm(false)  // No confirmar automáticamente
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            System.out.println("✅ Payment Intent creado: " + paymentIntent.getId());
            System.out.println("   Client Secret: " + paymentIntent.getClientSecret());
            
            return paymentIntent.getClientSecret();
            
        } catch (StripeException e) {
            System.err.println("❌ Error creando Payment Intent: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Procesa un pago con datos de tarjeta.
     * IMPORTANTE: En producción, NUNCA manejes números de tarjeta en el backend.
     * Usa Stripe Elements en el frontend para capturar de forma segura.
     * 
     * @param paymentIntentClientSecret Client secret del Payment Intent
     * @param cardToken Token de la tarjeta de Stripe
     * @return ID de la transacción si es exitosa, null si falla
     */
    public static String procesarPagoConToken(String paymentIntentClientSecret, String cardToken) {
        try {
            // Confirmar Payment Intent con el token
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(cardToken)
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentClientSecret);
            paymentIntent = paymentIntent.confirm(params);
            
            if ("succeeded".equals(paymentIntent.getStatus())) {
                System.out.println("✅ Pago procesado exitosamente!");
                String chargeId = paymentIntent.getId();
                System.out.println("   Charge ID: " + chargeId);
                return chargeId;
            } else if ("requires_action".equals(paymentIntent.getStatus())) {
                System.out.println("⚠️  Pago requiere autenticación adicional (3D Secure)");
                return null;
            } else {
                System.out.println("❌ Pago fallido: " + paymentIntent.getStatus());
                return null;
            }
            
        } catch (StripeException e) {
            System.err.println("❌ Error procesando pago: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Crea una fuente de tarjeta en Stripe.
     * ADVERTENCIA: Solo para demostración. En producción, usa Stripe Elements.
     * 
     * @param cardNumber Número de tarjeta (16 dígitos)
     * @param expMonth Mes de vencimiento (1-12)
     * @param expYear Año de vencimiento (4 dígitos)
     * @param cvc Código de verificación
     * @return Token de la tarjeta, o null si falla
     */
    public static String crearTokenTarjeta(String cardNumber, int expMonth, int expYear, String cvc) {
        try {
            TokenCreateParams.Card.Builder cardBuilder = TokenCreateParams.Card.builder()
                .setNumber(cardNumber)
                .setExpMonth(String.valueOf(expMonth))
                .setExpYear(String.valueOf(expYear));
            
            if (cvc != null && !cvc.isEmpty()) {
                cardBuilder.setCvc(cvc);
            }
            
            TokenCreateParams params = TokenCreateParams.builder()
                .setCard(cardBuilder.build())
                .build();
            
            Token token = Token.create(params);
            System.out.println("✅ Token de tarjeta creado: " + token.getId());
            return token.getId();
            
        } catch (StripeException e) {
            System.err.println("❌ Error creando token de tarjeta: " + e.getMessage());
            // Detalles del error específico
            if (e.getStripeError() != null) {
                System.err.println("   Código: " + e.getStripeError().getCode());
                System.err.println("   Mensaje: " + e.getStripeError().getMessage());
            }
            return null;
        }
    }

    /**
     * Obtiene o crea un cliente en Stripe.
     */
    private static String obtenerOCrearCliente(String email, String customerName) throws StripeException {
        // Buscar cliente existente
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
            .setQuery("email:'" + email + "'")
            .build();
        
        CustomerSearchResult result = Customer.search(searchParams);
        if (!result.getData().isEmpty()) {
            return result.getData().get(0).getId();
        }
        
        // Crear nuevo cliente
        CustomerCreateParams createParams = CustomerCreateParams.builder()
            .setEmail(email)
            .setName(customerName)
            .putMetadata("app", "ctrl-alt-quest")
            .build();
        
        Customer customer = Customer.create(createParams);
        System.out.println("✅ Cliente Stripe creado: " + customer.getId());
        return customer.getId();
    }

    /**
     * Verifica el estado de un pago.
     * @param transactionId ID de la transacción/charge en Stripe
     * @return "succeeded", "failed", "pending", o null si hay error
     */
    public static String verificarEstadoPago(String transactionId) {
        try {
            Charge charge = Charge.retrieve(transactionId);
            System.out.println("📊 Estado del pago: " + charge.getStatus());
            
            if ("succeeded".equals(charge.getStatus())) {
                return "succeeded";
            } else if ("failed".equals(charge.getStatus())) {
                return "failed";
            } else {
                return "pending";
            }
            
        } catch (StripeException e) {
            System.err.println("❌ Error verificando pago: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reembolsa un pago (refund).
     * @param transactionId ID de la transacción/charge a reembolsar
     * @param reason Razón del reembolso (ej: "requested_by_customer")
     * @return true si el reembolso fue exitoso
     */
    public static boolean reembolsarPago(String transactionId, String reason) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setCharge(transactionId)
                .setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()))
                .build();
            
            Refund refund = Refund.create(params);
            System.out.println("✅ Reembolso procesado: " + refund.getId());
            System.out.println("   Monto: $" + (refund.getAmount() / 100.0));
            return true;
            
        } catch (StripeException e) {
            System.err.println("❌ Error procesando reembolso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el saldo de la cuenta Stripe.
     * Útil para monitorear fondos.
     */
    public static void mostrarSaldoCuenta() {
        try {
            Balance balance = Balance.retrieve();
            
            System.out.println("💰 Saldo Stripe Account:");
            balance.getAvailable().forEach(bal -> {
                System.out.println("   Disponible: " + (bal.getAmount() / 100.0) + " " + bal.getCurrency().toUpperCase());
            });
            
            balance.getPending().forEach(bal -> {
                System.out.println("   Pendiente: " + (bal.getAmount() / 100.0) + " " + bal.getCurrency().toUpperCase());
            });
            
        } catch (StripeException e) {
            System.err.println("❌ Error obteniendo balance: " + e.getMessage());
        }
    }

    /**
     * Crear una cuenta bancaria conectada para recibir pagos.
     * IMPORTANTE: Esto es para TESTING. En producción, usa Stripe Connect.
     */
    public static void configurarCuentaBancaria() {
        System.out.println("\n🏦 CONFIGURACIÓN DE CUENTA BANCARIA:");
        System.out.println("═".repeat(60));
        System.out.println("1. Ve a: https://dashboard.stripe.com/settings/payouts");
        System.out.println("2. Agrega tu información bancaria");
        System.out.println("3. Los pagos se transferirán automáticamente a tu cuenta");
        System.out.println("═".repeat(60) + "\n");
    }

    /**
     * Valida un número de tarjeta usando el algoritmo de Luhn.
     */
    public static boolean validarNumeroTarjeta(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
