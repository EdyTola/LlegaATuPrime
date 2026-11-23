# Llega a tu Prime en macOS

## Parte 1. Cómo generar el instalador de Mac (sin tener una Mac)

El `.dmg` solo se puede crear en macOS, así que se genera en los servidores de GitHub.

1. Sube el proyecto a GitHub (rama `main`).
2. En el repositorio entra en la pestaña **Actions** > **Instalador macOS** > **Run workflow**.
3. Espera unos 5–10 minutos. Al terminar, abre la ejecución y descarga los artefactos de la sección **Artifacts**:
   - `LlegaATuPrime-macOS-arm64`: para Macs con chip Apple Silicon (M1, M2, M3, M4...).
   - `LlegaATuPrime-macOS-x64`: para Macs con procesador Intel.
4. Cada artefacto contiene `LlegaATuPrime-1.0.0-macOS-<arquitectura>.dmg`, el mismo programa en `.zip` y un archivo `.sha256` de verificación.

Para publicar una versión descargable por cualquiera, crea una etiqueta y súbela:

```
git tag v1.0.0
git push origin v1.0.0
```

Los archivos quedan adjuntos automáticamente en **Releases**.

Si tienes una Mac, también puedes generarlo localmente con JDK 17 o superior:

```
bash empaquetado/build-mac.sh
```

El resultado queda en `dist-mac/`.

## Parte 2. Instrucciones para el usuario final de Mac

### Qué archivo descargar
Apple menú > **Acerca de este Mac**:
- «Chip: Apple M1/M2/M3/M4» → versión **arm64**.
- «Procesador: Intel» → versión **x64**.

### Instalar
1. Abre el `.dmg` con doble clic.
2. Arrastra **Llega a tu Prime** a la carpeta **Aplicaciones**.
3. Expulsa el disco y abre la aplicación desde Aplicaciones o Launchpad.

La aplicación incluye su propio Java: no hay que instalar nada más.

### Aviso de macOS la primera vez (Gatekeeper)

La aplicación **no está firmada ni notarizada por Apple** (para eso se necesita una cuenta de Apple Developer de pago). macOS la bloqueará la primera vez con un mensaje como «no se puede abrir porque Apple no puede comprobar si contiene software malicioso» o «está dañada y no se puede abrir». No es un virus; es la protección estándar para apps de desarrolladores no registrados. Se resuelve así:

**macOS 14 Sonoma o anterior**
1. En Aplicaciones, haz **clic derecho** (o Control + clic) sobre «Llega a tu Prime» y elige **Abrir**.
2. En el cuadro que aparece, pulsa **Abrir**. Solo se pide la primera vez.

**macOS 15 Sequoia o posterior** (el clic derecho ya no basta)
1. Intenta abrir la aplicación con doble clic y cierra el aviso.
2. Abre **Ajustes del Sistema** > **Privacidad y seguridad**.
3. Baja hasta el mensaje sobre «Llega a tu Prime» y pulsa **Abrir igualmente**.
4. Confirma con tu contraseña o Touch ID.

**Si dice «está dañada» o nada de lo anterior funciona**, quita la marca de cuarentena con la Terminal:

```
xattr -cr "/Applications/Llega a tu Prime.app"
```

y vuelve a abrirla. Esto solo debe hacerse con archivos descargados de una fuente en la que confíes.

### Notas útiles
- Tus datos (perfil, rutinas, progreso, música) se guardan en tu carpeta de usuario, en `~/.bey-desktop`, de forma local.
- Para desinstalar, arrastra la aplicación a la Papelera. Tus datos no se borran; para eliminarlos, borra la carpeta `.bey-desktop` (en Finder: Cmd + Mayús + punto muestra las carpetas ocultas).
- La descarga de música desde enlaces prepara herramientas adicionales la primera vez y necesita internet.

## Parte 3. Advertencias para quien distribuye

- **Sin firma de Apple**: los avisos de arriba son inevitables. Para evitarlos hace falta una cuenta Apple Developer (99 USD/año), firmar con un certificado «Developer ID Application» y notarizar con `notarytool`. jpackage admite la firma con `--mac-sign` y `--mac-signing-key-user-name`; habría que añadir el certificado como secreto del repositorio.
- **Firma ad hoc**: el script firma la aplicación con una firma «ad hoc» (`codesign --sign -`). No elimina el aviso de Gatekeeper, pero es obligatoria para que el código arm64 se ejecute en Apple Silicon.
- **Etiquetas de los ejecutores**: `macos-latest` es Apple Silicon y `macos-15-intel` es Intel. GitHub cambia estas etiquetas con el tiempo; si el workflow falla en la fase de asignación, revisa la lista vigente en la documentación de GitHub Actions («GitHub-hosted runners»).
- **No se ha probado en una Mac real**: el script y el workflow se validaron en sintaxis, pero la primera ejecución real ocurrirá en GitHub Actions. Prueba el `.dmg` en una Mac antes de repartirlo.
