# BoilerReport

Web app (BoilerReport) para generar informes de inspección IQS en Word (.docx) a partir de una plantilla con marcadores `{{CAMPO}}`.

- **Datos:** OT, TAG, equipo, cliente, fecha y campos extra detectados en la plantilla.
- **Editor:** introducción, esquemas, espesores de referencia, ficha técnica, RER, secciones de inspección con fotos y recomendaciones. Dictado por voz en Chrome.
- **Revisión:** lista de campos completos y pendientes.

Todo se procesa en el navegador: la plantilla, los logos, los datos técnicos y los borradores se guardan solo en el dispositivo y no se envían a ningún servidor. El sitio no incluye logos ni datos técnicos de clientes; se cargan desde la app.

Para instalarla en el teléfono: abrir el sitio en Chrome → menú → *Agregar a la pantalla principal*.

## App Android

Cada cambio compila automáticamente el APK (GitHub Actions). Descárgalo desde el teléfono en
**Releases → BoilerReport (Android) → BoilerReport.apk** e instálalo. Las versiones nuevas se instalan encima sin perder los informes.

## Sincronización celular ↔ PC

Los informes se sincronizan con un repositorio **privado** propio (por ejemplo `BoilerReport-datos`) usando un token de GitHub con permiso *Contents: Read and write* sobre ese repositorio. Se configura en la app: Datos → Informe → Configurar sincronización.

## Google Drive (opcional)

Para guardar los informes en Google Drive (carpeta **BoilerReport**), una sola vez en **console.cloud.google.com**:

1. Crear un proyecto (ej. *BoilerReport*) y activar **Google Drive API**.
2. **Pantalla de consentimiento OAuth**: tipo *Externo*, en modo *Prueba*, agregar tu correo en *Usuarios de prueba*.
3. **Credenciales → Crear ID de cliente OAuth**:
   - *Aplicación web*: origen autorizado `https://moliveras-ops.github.io`. Copia el ID y pégalo en la app web (Datos → Informe → Guardar en Google Drive).
   - *Android*: paquete `cl.iqs.boilerreport` y huella SHA-1 `52:3E:AF:CB:7C:6F:06:F6:8C:03:43:6D:7F:D8:F3:32:0D:B8:E4:0F` (requiere el secreto `ANDROID_KEYSTORE`). En la app Android no hay que pegar nada.
