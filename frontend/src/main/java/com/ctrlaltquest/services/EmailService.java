package com.ctrlaltquest.services;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private final String myEmail = "ctrlaltquest.notify@gmail.com";
    private final String myPassword = "plwu ecyg ucjq iapk"; // Google App Password

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, myPassword);
            }
        });
    }

    /**
     * MÉTODO 1: Para nuevos registros
     * Ahora utiliza un estilo de "Bienvenida Heroica"
     */
    public void sendVerificationCode(String recipient, String code) throws Exception {
        Message message = new MimeMessage(createSession());
        message.setFrom(new InternetAddress(myEmail, "Ctrl+Alt+Quest"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("⚔️ ¡Bienvenido Aventurero! - Forja tu Identidad");

        String htmlContent = buildHtmlTemplate(
            "¡BIENVENIDO A LA GESTA!",
            "Has sido elegido para iniciar tu viaje. Para validar tu presencia en estos reinos, utiliza este código de forja:",
            code,
            "#f7d27a" // Oro para bienvenida
        );

        message.setContent(htmlContent, "text/html; charset=utf-8");
        Transport.send(message);
    }

    /**
     * MÉTODO 2: Para recuperación de contraseña
     * Mantiene el estilo místico del "Oráculo"
     */
    public void sendPasswordResetCode(String recipient, String code) throws Exception {
        Message message = new MimeMessage(createSession());
        message.setFrom(new InternetAddress(myEmail, "El Oráculo - Ctrl+Alt+Quest"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("🕯️ Recuperación de Acceso - El Oráculo");

        String htmlContent = buildHtmlTemplate(
            "RESTAURACIÓN ARCANO",
            "Un cuervo ha llegado solicitando una nueva llave para tu cuenta. Utiliza este código en el altar de restauración:",
            code,
            "#a292b1" // Púrpura/Plata para lo místico
        );

        message.setContent(htmlContent, "text/html; charset=utf-8");
        Transport.send(message);
    }

    /**
     * Plantilla base optimizada con colores dinámicos
     */
    private String buildHtmlTemplate(String title, String body, String code, String accentColor) {
        return "<div style='background-color: #0d0915; padding: 40px; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; text-align: center; border: 3px solid " + accentColor + "; border-radius: 15px;'>"
                + "<div style='font-size: 40px; margin-bottom: 10px;'>📜</div>"
                + "<h1 style='color: " + accentColor + "; letter-spacing: 3px; font-size: 26px; margin-top: 0;'>" + title + "</h1>"
                + "<hr style='border: 0; border-top: 1px solid #4a3b5a; width: 60%; margin: 20px auto;'>"
                + "<p style='color: #e0d7e5; font-size: 16px; line-height: 1.6; max-width: 400px; margin: 0 auto;'>" + body + "</p>"
                + "<div style='margin: 30px auto; padding: 15px 30px; background-color: #1a0f26; border: 2px dashed " + accentColor + "; display: inline-block; border-radius: 10px;'>"
                + "  <span style='color: " + accentColor + "; font-size: 36px; font-weight: bold; letter-spacing: 8px;'>" + code + "</span>"
                + "</div>"
                + "<p style='color: #6a5a7a; font-size: 12px; margin-top: 25px;'>Si no has invocado este mensaje, puedes ignorarlo con seguridad.<br>Tu seguridad es nuestra prioridad en la Matriz.</p>"
                + "</div>";
    }
}