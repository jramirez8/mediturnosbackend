Cambios incluidos:
- ObraSocial como entidad propia y endpoint GET /api/obras-sociales
- Registro de paciente por DTO en POST /api/pacientes/registro
- Campos nuevos de paciente: obra social, numero de carnet, numero de historia clinica, fecha de nacimiento, telefono, email
- Flujo de verificacion de cuenta listo con token
- Envio de email/WhatsApp simulado por logs para no atar el proyecto ahora a SMTP o Meta/Twilio

Endpoints nuevos importantes:
- GET  /api/obras-sociales
- POST /api/pacientes/registro
- GET  /api/pacientes/verificar-email?token=...

Si despues querés envío real por email:
- agregar starter mail
- crear servicio con JavaMailSender
- configurar SMTP en application.properties

Si querés WhatsApp real:
- integrar Meta WhatsApp Cloud API o Twilio
- hoy quedó encapsulado en VerificationDispatchService
