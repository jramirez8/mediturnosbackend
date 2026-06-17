# Calidad y coverage — Mediturnos Backend

Proyecto SonarQube Cloud:

- Organización: `jramirez8`
- Project key: `jramirez8_mediturnosbackend`
- Repositorio: `github.com/jramirez8/mediturnosbackend`

## Ejecución local en Windows

```powershell
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification
```

Reportes:

- Tests: `build/reports/tests/test/index.html`
- Coverage: `build/reports/jacoco/test/html/index.html`
- XML para Sonar: `build/reports/jacoco/test/jacocoTestReport.xml`

## Configuración única en GitHub

1. Entrar al repositorio `mediturnosbackend`.
2. Ir a **Settings → Secrets and variables → Actions**.
3. Crear un secreto llamado `SONAR_TOKEN` con el token generado en SonarQube Cloud.
4. En SonarQube Cloud, desactivar **Automatic Analysis** para evitar análisis duplicados.
5. Hacer push a `main` o `master`.

El workflow `.github/workflows/quality.yml`:

1. Compila con Java 17.
2. Ejecuta los tests.
3. Genera JaCoCo HTML/XML.
4. Verifica un piso de coverage del 70 % sobre la lógica incluida.
5. Ejecuta SonarQube Cloud.
6. Falla si quedan alertas abiertas de impacto **High** o **Medium**.
7. Publica los reportes como artifact de GitHub Actions.

## Alcance del coverage

Sonar analiza todo el backend para bugs, vulnerabilidades y code smells. El porcentaje de coverage se concentra en reglas de autorización, agenda, documentos, JWT y auditoría. DTO, entidades JPA, repositorios, controllers e integraciones externas se excluyen solamente del cálculo de coverage, no del análisis estático.


## Variables obligatorias nuevas

Para evitar secretos hardcodeados, producción debe tener:

```env
APP_JWT_SECRET=<secreto aleatorio de al menos 32 caracteres>
APP_ADMIN_PASSWORD=<contraseña inicial segura>
```

Generar un secreto JWT en PowerShell:

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copiar el resultado a Railway como `APP_JWT_SECRET`. No guardarlo en GitHub.
