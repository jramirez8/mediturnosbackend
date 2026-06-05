package com.ramirez.mediturnosback.service;

import com.ramirez.mediturnosback.dto.TurnoResponse;
import com.ramirez.mediturnosback.model.Paciente;
import com.ramirez.mediturnosback.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(VerificationDispatchService.class);

    private final MailService mailService;
    private final String appBaseUrl;
    private final String frontendResetUrl;

    public VerificationDispatchService(
            MailService mailService,
            @Value("${app.base-url:http://127.0.0.1:8080}") String appBaseUrl,
            @Value("${app.frontend-reset-url:}") String frontendResetUrl
    ) {
        this.mailService = mailService;
        this.appBaseUrl = appBaseUrl;
        this.frontendResetUrl = frontendResetUrl;
    }

    public boolean enviarValidacionEmail(Usuario usuario, Paciente paciente, String verificationUrl) {
        String html = """
                <html><body style="font-family: Arial, sans-serif;">
                <h2>Mediturnos - Verificá tu cuenta</h2>
                <p>Hola %s,</p>
                <p>Gracias por registrarte en Mediturnos.</p>
                <p>Para activar tu cuenta, abrí este enlace:</p>
                <p><a href="%s">Verificar cuenta</a></p>
                <p style="font-size:12px;color:#666">%s</p>
                <p>Si no solicitaste esta cuenta, podés ignorar este mensaje.</p>
                </body></html>
                """.formatted(paciente.getNombre(), verificationUrl, verificationUrl);
        boolean enviado = mailService.enviarEmail(usuario.getEmail(), paciente.getNombre(), "Mediturnos - Verificá tu cuenta", html);
        log.info("EMAIL VERIFICACION | Para: {} | Link: {} | enviado={}", usuario.getEmail(), verificationUrl, enviado);
        return enviado;
    }

    public void enviarValidacionWhatsapp(Paciente paciente, String verificationCode) {
        log.info("WHATSAPP no implementado | Para: {} | Paciente: {} {}",
                paciente.getTelefono(), paciente.getNombre(), paciente.getApellido());
    }

    public boolean enviarRecuperacionEmail(Usuario usuario, String resetToken) {
        String resetUrl = generarResetUrl(resetToken);
        String html = """
                <html><body style="font-family: Arial, sans-serif;">
                <h2>Mediturnos - Recuperación de contraseña</h2>
                <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                <p>Token:</p>
                <h2 style="letter-spacing:2px">%s</h2>
                <p>También podés abrir este enlace:</p>
                <p><a href="%s">Crear nueva contraseña</a></p>
                <p style="font-size:12px;color:#666">%s</p>
                <p>Si no solicitaste este cambio, ignorá este mensaje.</p>
                </body></html>
                """.formatted(resetToken, resetUrl, resetUrl);
        boolean enviado = mailService.enviarEmail(usuario.getEmail(), usuario.getEmail(), "Mediturnos - Recuperación de contraseña", html);
        log.info("RESET PASSWORD | Para: {} | Link: {} | enviado={}", usuario.getEmail(), resetUrl, enviado);
        return enviado;
    }

    public void enviarConfirmacionTurno(TurnoResponse turno, String emailDestino) {
        String detalle = "Profesional: " + turno.getProfesionalApellido() + ", " + turno.getProfesionalNombre()
                + "<br/>Especialidad: " + turno.getEspecialidad()
                + "<br/>Institución: " + turno.getInstitucionNombre()
                + "<br/>Dirección: " + turno.getDireccionAtencion()
                + "<br/>Fecha y hora: " + turno.getFechaHora();
        boolean enviado = mailService.enviarTurnoConfirmado(emailDestino, turno.getPacienteNombre(), detalle);
        log.info("EMAIL TURNO | Para: {} | Turno: {} | enviado={}", emailDestino, turno.getId(), enviado);
    }

    public boolean enviarRecordatorioTresHoras(TurnoResponse turno, String emailDestino) {
        String detalle = "Profesional: " + turno.getProfesionalApellido() + ", " + turno.getProfesionalNombre()
                + "<br/>Especialidad: " + turno.getEspecialidad()
                + "<br/>Institución: " + turno.getInstitucionNombre()
                + "<br/>Dirección: " + turno.getDireccionAtencion()
                + "<br/>Fecha y hora: " + turno.getFechaHora();
        String html = """
                <html><body style=\"font-family: Arial, sans-serif;\">
                <h2>Recordatorio de turno</h2>
                <p>Tu turno es en aproximadamente 3 horas.</p>
                <p><strong>%s</strong></p>
                <p>Si no podés asistir, cancelalo o reprogramalo desde la app.</p>
                </body></html>
                """.formatted(detalle);
        boolean enviado = mailService.enviarEmail(emailDestino, turno.getPacienteNombre(), "Mediturnos - Recordatorio de turno", html);
        log.info("EMAIL RECORDATORIO 3H | Para: {} | Turno: {} | enviado={}", emailDestino, turno.getId(), enviado);
        return enviado;
    }

    public boolean enviarAvisoListaEspera(String emailDestino, String nombre, TurnoResponse turnoLiberado) {
        String html = """
                <html><body style=\"font-family: Arial, sans-serif;\">
                <h2>Se liberó un turno</h2>
                <p>Hola %s,</p>
                <p>Se liberó un horario compatible con tu lista de espera.</p>
                <p><strong>%s - %s</strong></p>
                <p>Ingresá a Mediturnos para tomarlo antes de que lo reserve otra persona.</p>
                </body></html>
                """.formatted(nombre != null ? nombre : "", turnoLiberado.getEspecialidad(), turnoLiberado.getFechaHora());
        boolean enviado = mailService.enviarEmail(emailDestino, nombre, "Mediturnos - Turno disponible", html);
        log.info("EMAIL LISTA ESPERA | Para: {} | Turno: {} | enviado={}", emailDestino, turnoLiberado.getId(), enviado);
        return enviado;
    }

    public boolean enviarCodigoDosFactores(Usuario usuario, String codigo) {
        boolean enviado = mailService.enviarCodigoVerificacion(usuario.getEmail(), usuario.getEmail(), codigo);
        log.info("EMAIL 2FA | Para: {} | enviado={}", usuario.getEmail(), enviado);
        return enviado;
    }

    public String generarResetUrl(String resetToken) {
        String base = frontendResetUrl != null && !frontendResetUrl.isBlank()
                ? frontendResetUrl
                : appBaseUrl + "/reset-password";
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "token=" + resetToken;
    }
}
