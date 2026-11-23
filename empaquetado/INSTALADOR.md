# Setup de Llega a tu Prime (Windows)

Resultado: `dist\LlegaATuPrime_Setup.exe` (30 MB), un instalador clásico con asistente en español. También se genera `dist\LlegaATuPrime_Setup.zip` con el Setup y un `LEEME.txt` de dos líneas, listo para enviar.

## Tecnología y por qué

La aplicación es Java/JavaFX, no web, por lo que Tauri y Electron no aplican (obligarían a reescribir la interfaz). Se usa:

- **jpackage** (incluido en el JDK): empaqueta la aplicación con su propio Java, así que el usuario no instala nada más.
- **Inno Setup 6.7.4**: crea el `Setup.exe` con asistente, accesos directos y desinstalador estándar.

## Estructura

```
AppLlegaATuPrime/
├── pom.xml
├── src/                                  código de la aplicación
│   └── main/java/com/bey/desktop/beyLanzador.java    punto de entrada del paquete
├── empaquetado/
│   ├── construir-instalador.cmd          doble clic: genera todo
│   ├── construir-instalador.ps1          script de construcción
│   ├── LlegaATuPrime.iss                 configuración del Setup (Inno Setup)
│   ├── bey.ico                           icono 16, 24, 32, 48, 64, 128 y 256 px
│   ├── bey-512.png                       icono PNG de alta resolución
│   ├── asistente-*.bmp                   imágenes del asistente (bienvenida y cabecera)
│   ├── LEEME.txt                         guía de 2 líneas que viaja junto al Setup
│   └── herramientas/                     Inno Setup descargado automáticamente (no va a Git)
└── dist/                                 salida (no va a Git)
    ├── app-image/                        la aplicación con su Java
    ├── LlegaATuPrime_Setup.exe           el instalador
    └── LlegaATuPrime_Setup.zip           Setup + LEEME.txt
```

## Cómo se genera (en el equipo de desarrollo)

Requisitos: JDK 17 o superior (con `jpackage`) e internet la primera vez.

1. Abre la carpeta del proyecto.
2. Doble clic en `empaquetado\construir-instalador.cmd`.
3. El script, en orden:
   1. busca `jpackage`;
   2. si no hay Inno Setup, descarga la versión 6.7.4 desde el repositorio oficial, **comprueba su firma digital (Pyrsys B.V., editor de Inno Setup) y su huella SHA-256** y la instala solo para tu usuario en `empaquetado\herramientas\inno`;
   3. compila con Maven y copia las dependencias;
   4. calcula los módulos de Java necesarios y crea la aplicación con `jpackage` (icono incluido);
   5. compila el Setup con `LlegaATuPrime.iss`;
   6. crea el `.zip` de distribución.
4. Entrega `dist\LlegaATuPrime_Setup.exe` o `dist\LlegaATuPrime_Setup.zip`.

Comando equivalente en PowerShell: `.\empaquetado\construir-instalador.ps1`

## Qué hace el Setup

1. **Bienvenida** con el nombre «Llega a tu Prime» y el logotipo centrado.
2. **Carpeta de destino**: por defecto `C:\Users\<usuario>\AppData\Local\Programs\Llega a tu Prime` (no pide permisos de administrador). El usuario puede cambiarla.
3. **Accesos directos**: Menú Inicio siempre; Escritorio marcado por defecto (se puede desmarcar).
4. **Barra de progreso** de instalación.
5. **Finalizar** con la casilla «Ejecutar Llega a tu Prime».
6. **Desinstalador** en «Agregar o quitar programas» (con el icono de la app) y acceso «Desinstalar Llega a tu Prime» en el Menú Inicio.

## Datos del usuario

Todo se guarda de forma local, en `C:\Users\<usuario>\.bey-desktop`: perfil, rutinas, pesos, XP, hidratación, progreso, lista de música y respaldos JSON. No hay servidores. Sobrevive a cerrar la app, reiniciar el equipo, actualizar y desinstalar (la desinstalación **no borra** esa carpeta, a propósito).

## Checklist de prueba en un equipo limpio

1. Copia `LlegaATuPrime_Setup.zip` (o el `.exe`) al equipo y descomprime.
2. Ejecuta `LlegaATuPrime_Setup.exe`. Si Windows muestra «Windows protegió su PC», pulsa «Más información» > «Ejecutar de todas formas» (ver advertencias).
3. Comprueba: bienvenida con logotipo, carpeta de destino, casilla de Escritorio, progreso y pantalla final.
4. Marca «Ejecutar Llega a tu Prime» y finaliza: debe abrirse la pantalla de bienvenida y luego la ventana principal, sin ventana negra.
5. Cambia el nombre en «Mi perfil», pulsa «Guardar cambios» y registra un vaso de agua. Cierra la aplicación.
6. Reabre con el acceso directo del Escritorio: el nombre y el agua siguen ahí. Reinicia el equipo y comprueba de nuevo.
7. Verifica el icono en la barra de tareas, el acceso del Escritorio y el Menú Inicio.
8. Desinstala desde Configuración > Aplicaciones > Llega a tu Prime. Desaparecen la carpeta, los accesos y la entrada del Menú Inicio.
9. Reinstala: tus datos siguen disponibles.

## Advertencias reales de entrega

- **SmartScreen / antivirus**: el `.exe` no está firmado digitalmente y es nuevo, así que Windows puede mostrar «Windows protegió su PC» y algunos antivirus lo marcan como poco común. No es un virus; desaparece con reputación o con un certificado de firma de código (de pago). Mientras tanto, indica a los usuarios el paso «Más información > Ejecutar de todas formas».
- **Dentro del `.rar` o `.zip`** debe ir solo: `LlegaATuPrime_Setup.exe` y `LEEME.txt`. El script ya genera el `.zip`; si prefieres `.rar`, comprime esos dos archivos con WinRAR (el script no lo hace porque WinRAR es de pago).
- **Actualizar versión**: cambia `Version` en `LlegaATuPrime.iss` y en `construir-instalador.ps1`. Conserva el `AppId` para que Windows trate la nueva versión como actualización de la misma aplicación.
- **Instalación para todos los usuarios**: no se ofrece, para evitar permisos de administrador. Cada usuario del equipo instala la suya.
