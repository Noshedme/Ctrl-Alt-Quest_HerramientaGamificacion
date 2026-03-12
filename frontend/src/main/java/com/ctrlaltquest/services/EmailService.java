package com.ctrlaltquest.services;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private final String myEmail    = "ctrlaltquest.notify@gmail.com";
    private final String myPassword = "mubz qhos cvsf pgqm";

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
        return Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, myPassword);
            }
        });
    }

    // ── Registro ──────────────────────────────────────────────────────────────
    public void sendVerificationCode(String recipient, String code) throws Exception {
        Message msg = new MimeMessage(createSession());
        msg.setFrom(new InternetAddress(myEmail, "Ctrl+Alt+Quest"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        msg.setSubject("⚔️ ¡Bienvenido Aventurero! - Forja tu Identidad");
        msg.setContent(buildHtmlTemplate(
            "¡BIENVENIDO A LA GESTA!",
            "Has sido elegido para iniciar tu viaje. Para validar tu presencia en estos reinos, utiliza este código de forja:",
            code, "#f7d27a"), "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ── Recuperación de contraseña ────────────────────────────────────────────
    public void sendPasswordResetCode(String recipient, String code) throws Exception {
        Message msg = new MimeMessage(createSession());
        msg.setFrom(new InternetAddress(myEmail, "El Oráculo - Ctrl+Alt+Quest"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        msg.setSubject("🕯️ Recuperación de Acceso - El Oráculo");
        msg.setContent(buildHtmlTemplate(
            "RESTAURACIÓN ARCANO",
            "Un cuervo ha llegado solicitando una nueva llave para tu cuenta. Utiliza este código en el altar de restauración:",
            code, "#a292b1"), "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ── Alerta de seguridad por bloqueo ───────────────────────────────────────

    /**
     * Se envía automáticamente cuando la cuenta queda bloqueada
     * por alcanzar el máximo de intentos fallidos.
     *
     * @param recipient Email del dueño de la cuenta
     * @param ipAddress IP desde la que se realizaron los intentos
     */
    public void sendSecurityAlert(String recipient, String ipAddress) throws Exception {
        long lockMinutes = LoginAttemptService.LOCKOUT_DURATION_MS / 60_000;

        Message msg = new MimeMessage(createSession());
        msg.setFrom(new InternetAddress(myEmail, "Guardia Real - Ctrl+Alt+Quest"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        msg.setSubject("🚨 Alerta de Seguridad — Acceso Bloqueado");

        String body = "Se han detectado <strong>" + LoginAttemptService.MAX_ATTEMPTS +
                      " intentos fallidos</strong> de acceso a tu cuenta desde la dirección IP: " +
                      "<strong>" + ipAddress + "</strong>.<br><br>" +
                      "Como medida de protección, tu cuenta ha sido bloqueada temporalmente durante " +
                      "<strong>" + lockMinutes + " minuto(s)</strong>.<br><br>" +
                      "Si fuiste tú quien intentó ingresar, simplemente espera y vuelve a intentarlo.<br>" +
                      "Si <strong>no reconoces esta actividad</strong>, te recomendamos cambiar tu contraseña inmediatamente " +
                      "usando la opción '¿Olvidaste tu contraseña?' en la pantalla de inicio de sesión.";

        msg.setContent(buildAlertHtml(body), "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PLANTILLAS HTML
    // ════════════════════════════════════════════════════════════════════════

    private String buildHtmlTemplate(String title, String body, String code, String accentColor) {
        return "<div style='background-color:#0d0915;padding:40px;font-family:\"Segoe UI\",Tahoma,Geneva,Verdana,sans-serif;" +
               "text-align:center;border:3px solid " + accentColor + ";border-radius:15px;'>" +
               "<div style='font-size:40px;margin-bottom:10px;'>📜</div>" +
               "<h1 style='color:" + accentColor + ";letter-spacing:3px;font-size:26px;margin-top:0;'>" + title + "</h1>" +
               "<hr style='border:0;border-top:1px solid #4a3b5a;width:60%;margin:20px auto;'>" +
               "<p style='color:#e0d7e5;font-size:16px;line-height:1.6;max-width:400px;margin:0 auto;'>" + body + "</p>" +
               "<div style='margin:30px auto;padding:15px 30px;background-color:#1a0f26;border:2px dashed " + accentColor + ";" +
               "display:inline-block;border-radius:10px;'>" +
               "<span style='color:" + accentColor + ";font-size:36px;font-weight:bold;letter-spacing:8px;'>" + code + "</span>" +
               "</div>" +
               "<p style='color:#6a5a7a;font-size:12px;margin-top:25px;'>Si no has invocado este mensaje, puedes ignorarlo con seguridad.<br>" +
               "Tu seguridad es nuestra prioridad en la Matriz.</p></div>";
    }

    private String buildAlertHtml(String body) {
        return "<div style='background-color:#0d0915;padding:40px;font-family:\"Segoe UI\",Tahoma,Geneva,Verdana,sans-serif;" +
               "text-align:center;border:3px solid #ef4444;border-radius:15px;'>" +
               "<div style='font-size:48px;margin-bottom:10px;'>🚨</div>" +
               "<h1 style='color:#ef4444;letter-spacing:3px;font-size:26px;margin-top:0;'>ALERTA DE SEGURIDAD</h1>" +
               "<hr style='border:0;border-top:1px solid #7f1d1d;width:60%;margin:20px auto;'>" +
               "<p style='color:#e0d7e5;font-size:16px;line-height:1.8;max-width:460px;margin:0 auto;text-align:left;'>" + body + "</p>" +
               "<div style='margin-top:30px;padding:12px 24px;background-color:#7f1d1d;border-radius:8px;display:inline-block;'>" +
               "<span style='color:#fca5a5;font-size:14px;font-weight:bold;'>⚔️ Ctrl+Alt+Quest — Guardia Real</span>" +
               "</div>" +
               "<p style='color:#6a5a7a;font-size:12px;margin-top:20px;'>Este es un mensaje automático de seguridad. No respondas a este correo.</p>" +
               "</div>";
    }
}