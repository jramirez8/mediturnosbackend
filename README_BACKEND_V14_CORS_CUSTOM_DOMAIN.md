# Backend v14 - CORS para dominio propio mediturnos.net.ar

Este update agrega soporte explícito para el dominio propio del front:

- `https://mediturnos.net.ar`
- `https://www.mediturnos.net.ar`
- `http://mediturnos.net.ar`
- `http://www.mediturnos.net.ar`

## Archivos modificados

- `src/main/java/com/ramirez/mediturnosback/config/CorsConfig.java`
- `src/main/resources/application.properties`

## Qué cambia

Antes, si Railway tenía definida la variable `APP_CORS_ALLOWED_ORIGINS`, el backend usaba solo esa lista. Si ahí no estaba `mediturnos.net.ar`, el login y las llamadas desde el dominio propio fallaban aunque desde la URL genérica de Vercel funcionaran.

Ahora el backend siempre incluye una base segura de orígenes comunes:

- localhost / 127.0.0.1
- Vercel
- Railway
- `mediturnos.net.ar`

Y además suma lo que venga en `APP_CORS_ALLOWED_ORIGINS`.

## Variable recomendada en Railway

Aunque el código ya lo contempla, en Railway conviene dejar esta variable así:

```txt
APP_CORS_ALLOWED_ORIGINS=https://mediturnos.net.ar,https://www.mediturnos.net.ar,https://*.vercel.app,https://*.railway.app,http://localhost:8081,http://localhost:19006
```

Después de cambiar la variable o subir este backend, redeploy del servicio backend en Railway.

## Cómo probar rápido

Desde el dominio propio, abrí DevTools > Network y probá login. La request ya no debería morir con CORS/Network Error.

Si todavía falla, revisar:

1. que el backend de Railway haya redeployado;
2. que el front apunte al backend correcto o al proxy correcto;
3. que en Network la respuesta no sea 401/403 real de credenciales;
4. que `mediturnos.net.ar` esté sirviendo por HTTPS y no mezclando contenido HTTP.
