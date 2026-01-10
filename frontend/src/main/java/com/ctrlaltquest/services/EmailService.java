package com.ctrlaltquest.services;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    // ⚠️ CONFIGURA ESTO CON UNA CUENTA REAL PARA PRUEBAS
    private final String myEmail = "ctrlaltquest.notify@gmail.com";
    private final String myPassword = "plwu ecyg ucjq iapk"; // Google App Password

    public void sendVerificationCode(String recipient, String code) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, myPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(myEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("⚔️ Ctrl+Alt+Quest: Código de Verificación");
        
        String htmlCode = "<h1>¡Saludos Aventurero!</h1>"
                + "<p>Tu código para forjar tu cuenta es:</p>"
                + "<h2 style='color:#f7d27a; background:#1a0f26; padding:10px; display:inline-block;'>" + code + "</h2>"
                + "<p>Si no solicitaste esto, ignora el mensaje.</p>";
                
        message.setContent(htmlCode, "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("📧 Correo enviado a: " + recipient);
    }
}