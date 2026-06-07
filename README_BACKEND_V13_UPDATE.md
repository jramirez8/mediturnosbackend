# Mediturnos Backend v13 - seguridad, agenda y auditoría real

## Cambios principales

- Seguridad real con JWT en Spring Security.
- `Authorization: Bearer <token>` ahora crea un usuario autenticado con rol, usuarioId, pacienteId y profesionalId.
- Endpoints protegidos por rol:
  - `/api/admin/**`: solo ADMIN.
  - `/api/secretaria/**`: SECRETARY o ADMIN.
  - `/api/agenda/**`: PROFESSIONAL o ADMIN.
  - `/api/turnos/**`: autenticado, salvo disponibilidad pública.
  - `/api/pacientes/**`: autenticado, con validaciones de dueño.
- Se bloquea en backend que un médico atienda turnos de otro médico.
- Se bloquea que un paciente consulte/modifique turnos o perfiles de otro paciente.
- Nuevo endpoint: `GET /api/profesionales/me`.
- Nuevo endpoint: `GET /api/profesionales/me/sedes`.
- Nuevo endpoint: `GET /api/profesionales/agenda/me`.
- Nuevo endpoint: `GET /api/profesionales/proximo-turno/me`.
- Agenda médica mejorada:
  - no genera horarios falsos si el médico no configuró disponibilidad;
  - valida día, rango horario, duración, especialidad, bloqueos y turnos ocupados;
  - soporta editar horarios con `PUT /api/agenda/horarios/{id}`;
  - soporta editar bloqueos con `PUT /api/agenda/bloqueos/{id}`.
- Documentos de turno:
  - acepta PDF/JPG/PNG hasta 1 MB;
  - ya no comprime PDF como imagen;
  - valida MIME y extensión.
- Auditoría:
  - el actor sale del JWT cuando hay usuario autenticado;
  - se agregan columnas `actorUsuarioId`, `actorRol`, `actorEmail`;
  - las acciones de admin/agenda/turnos quedan mejor registradas.
- Fechas internas sensibles migradas a zona `America/Argentina/Buenos_Aires` mediante `AppClock`.

## Notas de base de datos

Con `spring.jpa.hibernate.ddl-auto=update`, Hibernate debería agregar automáticamente las nuevas columnas:

- `auditoria.actor_usuario_id`
- `auditoria.actor_rol`
- `auditoria.actor_email`
- `turno_adjuntos.tipo_documento`

Si Railway/MySQL no toma el update, ejecutar manualmente:

```sql
ALTER TABLE auditoria ADD COLUMN actor_usuario_id BIGINT NULL;
ALTER TABLE auditoria ADD COLUMN actor_rol VARCHAR(30) NULL;
ALTER TABLE auditoria ADD COLUMN actor_email VARCHAR(160) NULL;
ALTER TABLE turno_adjuntos ADD COLUMN tipo_documento VARCHAR(60) NULL;
```

## Variables recomendadas en Railway

```env
APP_JWT_SECRET=poneme-una-clave-larga-de-mas-de-32-caracteres
APP_CORS_ALLOWED_ORIGINS=https://tu-front.vercel.app,https://*.vercel.app,http://localhost:8081,http://localhost:19006
SPRING_JPA_HIBERNATE_DDL_AUTO=update
APP_UPLOAD_DIR=/data/uploads
```

## Advertencia honesta

No pude ejecutar Gradle en este entorno porque el wrapper intenta descargar Gradle desde `services.gradle.org` y la sesión no tiene Internet. Revisé sintaxis y consistencia de archivos, pero conviene correr localmente:

```bash
./gradlew clean test
./gradlew bootRun
```
