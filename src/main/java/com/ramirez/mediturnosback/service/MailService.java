package com.ramirez.mediturnosback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final RestClient restClient;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL:${app.mail.from:}}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME:Mediturnos}")
    private String senderName;

    public MailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public boolean enviarEmail(String destinatario, String nombreDestinatario, String asunto, String html) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Email no enviado: destinatario vacío. Asunto: {}", asunto);
            return false;
        }

        if (brevoApiKey == null || brevoApiKey.isBlank() || senderEmail == null || senderEmail.isBlank()) {
            log.error("Email no enviado: falta BREVO_API_KEY o BREVO_SENDER_EMAIL. No se expone token al frontend.");
            return false;
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of(
                                "email", destinatario,
                                "name", nombreDestinatario != null && !nombreDestinatario.isBlank() ? nombreDestinatario : destinatario
                        )
                ),
                "subject", asunto,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email enviado por Brevo a {} con asunto '{}'", destinatario, asunto);
            return true;
        } catch (Exception ex) {
            log.error("No se pudo enviar email por Brevo a {}: {}", destinatario, ex.getMessage(), ex);
            return false;
        }
    }

    public boolean enviarCodigoVerificacion(String destinatario, String nombre, String codigo) {
        String html = """
                <html>
                  <body style="font-family: Arial, sans-serif;">
                    <h2>Verificación de cuenta - Mediturnos</h2>
                    <p>Hola %s,</p>
                    <p>Tu código de verificación es:</p>
                    <h1 style="letter-spacing: 4px;">%s</h1>
                    <p>Si no solicitaste este código, ignorá este mensaje.</p>
                  </body>
                </html>
                """.formatted(nombre != null ? nombre : "", codigo);

        return enviarEmail(destinatario, nombre, "Código de verificación - Mediturnos", html);
    }

    public boolean enviarTurnoConfirmado(String destinatario, String nombre, String detalleTurno) {
        String html = """
                <html>
                  <body style="font-family: Arial, sans-serif;">
                    <h2>Turno confirmado</h2>
                    <p>Hola %s,</p>
                    <p>Tu turno fue confirmado correctamente.</p>
                    <p><strong>%s</strong></p>
                    <p>Gracias por usar Mediturnos.</p>
                  </body>
                </html>
                """.formatted(nombre != null ? nombre : "", detalleTurno);

        return enviarEmail(destinatario, nombre, "Turno confirmado - Mediturnos", html);
    }
}
