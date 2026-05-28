package com.ramirez.mediturnosback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class MailService {

    private final RestClient restClient;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME:Mediturnos}")
    private String senderName;

    public MailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public void enviarEmail(String destinatario, String nombreDestinatario, String asunto, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(
                        Map.of(
                                "email", destinatario,
                                "name", nombreDestinatario != null ? nombreDestinatario : destinatario
                        )
                ),
                "subject", asunto,
                "htmlContent", html
        );

        restClient.post()
                .uri("/smtp/email")
                .header("api-key", brevoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void enviarCodigoVerificacion(String destinatario, String nombre, String codigo) {
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

        enviarEmail(
                destinatario,
                nombre,
                "Código de verificación - Mediturnos",
                html
        );
    }

    public void enviarTurnoConfirmado(String destinatario, String nombre, String detalleTurno) {
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

        enviarEmail(
                destinatario,
                nombre,
                "Turno confirmado - Mediturnos",
                html
        );
    }
}