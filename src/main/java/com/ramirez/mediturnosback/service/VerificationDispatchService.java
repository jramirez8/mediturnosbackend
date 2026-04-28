package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(VerificationDispatchService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String appBaseUrl;

    public VerificationDispatchService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String fromAddress,
            @Value("${app.base-url:http://127.0.0.1:8080}") String appBaseUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.appBaseUrl = appBaseUrl;
    }

    public void enviarValidacionEmail(Usuario usuario, Paciente paciente, String verificationUrl) {
        String subject = "Mediturnos - Verificá tu cuenta";
        String body = "Hola " + paciente.getNombre() + ",\n\n"
                + "Gracias por registrarte en Mediturnos.\n"
                + "Para activar tu cuenta, abrí este enlace:\n\n"
                + verificationUrl + "\n\n"
                + "Si no solicitaste esta cuenta, podés ignorar este mensaje.\n\n"
                + "Mediturnos";
        enviarEmail(usuario.getEmail(), subject, body);
    }

    public void enviarValidacionWhatsapp(Paciente paciente, String verificationCode) {
        log.info("SIMULACION ENVIO WHATSAPP | Para: {} | Paciente: {} {} | Codigo sugerido: {}",
                paciente.getTelefono(), paciente.getNombre(), paciente.getApellido(), verificationCode);
    }

    public void enviarRecuperacionEmail(Usuario usuario, String resetToken) {
        String resetUrl = appBaseUrl + "/reset-password?token=" + resetToken;
        String subject = "Mediturnos - Recuperación de contraseña";
        String body = "Hola,\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña.\n"
                + "Usá este token en la app o abrí este enlace:\n\n"
                + "Token: " + resetToken + "\n"
                + "Link: " + resetUrl + "\n\n"
                + "Si no solicitaste este cambio, ignorá este mensaje.\n\n"
                + "Mediturnos";
        enviarEmail(usuario.getEmail(), subject, body);
    }

    public void enviarConfirmacionTurno(TurnoResponse turno, String emailDestino) {
        String subject = "Mediturnos - Confirmación de turno";
        String body = "Hola " + turno.getPacienteNombre() + ",\n\n"
                + "Tu turno fue confirmado.\n\n"
                + "Profesional: " + turno.getProfesionalApellido() + ", " + turno.getProfesionalNombre() + "\n"
                + "Especialidad: " + turno.getEspecialidad() + "\n"
                + "Institución: " + turno.getInstitucionNombre() + "\n"
                + "Dirección: " + turno.getDireccionAtencion() + "\n"
                + "Fecha y hora: " + turno.getFechaHora() + "\n\n"
                + "Mediturnos";
        enviarEmail(emailDestino, subject, body);
    }

    private void enviarEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("No hay destinatario configurado para el email");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("No hay remitente configurado. Definí spring.mail.username o app.mail.from");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email enviado correctamente a {} con asunto '{}'", to, subject);
        } catch (MailException ex) {
            log.error("No se pudo enviar email a {}: {}", to, ex.getMessage(), ex);
            throw new IllegalStateException("No se pudo enviar el email a " + to + ". Revisá SMTP/remitente verificado.", ex);
        }
    }
}
