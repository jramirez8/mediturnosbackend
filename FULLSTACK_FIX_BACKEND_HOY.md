# Mediturnos backend - fixes de hoy

Cambios aplicados:

- CORS habilitado para Expo Web (`localhost:8081`, `8082`, `19006`) y deploys comunes.
- `POST /api/auth/login` ahora acepta `{ identificador, password }` y también `{ email, password }`.
- Login devuelve `token`, `accessToken` y `jwt` para que el mobile no falle esperando JWT.
- `POST /api/auth/forgot-password` ahora acepta `{ identificador }`, `{ emailOrDni }`, `{ email }` o `{ dni }`.
- Recuperación de contraseña usa Brevo por API HTTPS si está configurado. Si no, queda modo demo y loguea el token.
- `POST /api/auth/reset-password` acepta `{ token, password, confirmPassword }` y aliases `{ newPassword, confirmNewPassword }`.
- Errores REST ahora devuelven también `message`, además de `error`, para que Axios muestre bien el mensaje.

Variables útiles en Railway:

```env
APP_BASE_URL=https://mediturnosbackend-production.up.railway.app
APP_FRONTEND_RESET_URL=http://localhost:8081/reset-password
APP_AUTH_EXPOSE_RESET_TOKEN=true
APP_JWT_SECRET=poner-una-frase-larga-de-32-caracteres-minimo
BREVO_API_KEY=xkeysib-...
BREVO_SENDER_EMAIL=mail-verificado@dominio.com
BREVO_SENDER_NAME=Mediturnos
```

Para demo sin Brevo, dejá `APP_AUTH_EXPOSE_RESET_TOKEN=true`: el endpoint devuelve `resetToken` y la app puede navegar directo a crear nueva contraseña.
