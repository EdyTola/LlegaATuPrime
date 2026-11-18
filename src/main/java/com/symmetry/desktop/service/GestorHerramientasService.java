package com.symmetry.desktop.service;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gestion de las herramientas externas necesarias para convertir un enlace a MP3 (yt-dlp y
 * ffmpeg). Los ejecutables se guardan en una carpeta local de la aplicacion
 * ({@code userData/tools}) y, si todavia no existen, se descargan automaticamente desde sus
 * repositorios oficiales mostrando el avance dentro de la propia interfaz, sin requerir que
 * el usuario instale nada en su sistema.
 *
 * <p>La aplicacion detecta el sistema operativo ({@link System#getProperty(String)} con
 * {@code os.name}) y elige los binarios adecuados para Windows o macOS. En macOS, ademas,
 * marca los ejecutables descargados como ejecutables para que el sistema los permita usar.</p>
 */
public final class GestorHerramientasService {

    /** Escucha de progreso: fraccion 0..1, o -1 cuando el avance es indeterminado. */
    @FunctionalInterface
    public interface Escucha {
        void progreso(double fraccion, String mensaje);
    }

    /** Ejecutable de yt-dlp publicado en las "releases" oficiales del proyecto (Windows). */
    private static final String URL_YTDLP_WINDOWS =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    /** Binario universal de yt-dlp para macOS, sin dependencias externas. */
    private static final String URL_YTDLP_MACOS =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos";
    /** Compilacion estatica de ffmpeg para Windows (incluye ffmpeg.exe y ffprobe.exe). */
    private static final String URL_FFMPEG_WINDOWS =
            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
    /** Compilacion estatica de ffmpeg para macOS con Apple Silicon (M1/M2/M3). */
    private static final String URL_FFMPEG_MACOS_ARM =
            "https://github.com/eugeneware/ffmpeg-static/releases/latest/download/ffmpeg-darwin-arm64";
    /** Compilacion estatica de ffmpeg para macOS con procesador Intel. */
    private static final String URL_FFMPEG_MACOS_X64 =
            "https://github.com/eugeneware/ffmpeg-static/releases/latest/download/ffmpeg-darwin-x64";

    private static final String FFPROBE = "ffprobe.exe";

    private GestorHerramientasService() {
    }

    /** Carpeta local donde se guardan las herramientas ({@code userData/tools}). */
    public static Path carpetaHerramientas() {
        Path base = JsonPersistenciaService.rutaPorDefecto().getParent();
        return (base == null ? Path.of("tools") : base.resolve("tools"));
    }

    /** Nombre del ejecutable de yt-dlp segun el sistema operativo. */
    public static String nombreYtDlp() {
        return esWindows() ? "yt-dlp.exe" : "yt-dlp";
    }

    /** Nombre del ejecutable de ffmpeg segun el sistema operativo. */
    public static String nombreFfmpeg() {
        return esWindows() ? "ffmpeg.exe" : "ffmpeg";
    }

    /** Ruta del ejecutable de yt-dlp si ya esta instalado, o {@code null}. */
    public static Path rutaYtDlp() {
        return existente(carpetaHerramientas().resolve(nombreYtDlp()));
    }

    /** Ruta del ejecutable de ffmpeg si ya esta instalado, o {@code null}. */
    public static Path rutaFfmpeg() {
        return existente(carpetaHerramientas().resolve(nombreFfmpeg()));
    }

    /** true cuando yt-dlp y ffmpeg estan disponibles localmente. */
    public static boolean instaladas() {
        return rutaYtDlp() != null && rutaFfmpeg() != null;
    }

    /**
     * Garantiza que yt-dlp y ffmpeg esten presentes, descargando en segundo plano los que
     * falten. Las llamadas de retorno se entregan ya en el hilo de la interfaz de JavaFX.
     *
     * @param escucha notificaciones de progreso durante la descarga de las herramientas
     * @param exito   se invoca cuando ambas herramientas estan listas para usarse
     * @param error   recibe un mensaje claro y sin tecnicismos si algo falla
     */
    public static void asegurar(Escucha escucha, Runnable exito, Consumer<String> error) {
        Thread hilo = new Thread(() -> {
            try {
                Path carpeta = carpetaHerramientas();
                Files.createDirectories(carpeta);

                if (rutaYtDlp() == null) {
                    notificar(escucha, -1, "Preparando el motor de descargas (yt-dlp)…");
                    Path destino = carpeta.resolve(nombreYtDlp());
                    descargar(urlYtDlp(), destino,
                            avance -> notificar(escucha, avance * 0.5,
                                    "Descargando yt-dlp… " + porcentaje(avance)));
                    hacerEjecutable(destino);
                }
                if (rutaFfmpeg() == null) {
                    notificar(escucha, 0.5, "Preparando el conversor de audio (ffmpeg)…");
                    if (esWindows()) {
                        Path zip = Files.createTempFile("ltp-ffmpeg-", ".zip");
                        try {
                            descargar(URL_FFMPEG_WINDOWS, zip,
                                    avance -> notificar(escucha, 0.5 + avance * 0.5,
                                            "Descargando ffmpeg… " + porcentaje(avance)));
                            extraerBinario(zip, carpeta, nombreFfmpeg());
                            extraerBinario(zip, carpeta, FFPROBE);
                        } finally {
                            Files.deleteIfExists(zip);
                        }
                    } else {
                        Path destino = carpeta.resolve(nombreFfmpeg());
                        descargar(urlFfmpeg(), destino,
                                avance -> notificar(escucha, 0.5 + avance * 0.5,
                                        "Descargando ffmpeg… " + porcentaje(avance)));
                        hacerEjecutable(destino);
                    }
                }
                if (!instaladas()) {
                    throw new IOException("Las herramientas no se instalaron correctamente.");
                }
                Platform.runLater(exito);
            } catch (Exception e) {
                String mensaje = mensajeAmigable(e);
                Platform.runLater(() -> error.accept(mensaje));
            }
        }, "herramientas-descarga");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ------------------------------------------------------------------
    // Descarga HTTP con progreso
    // ------------------------------------------------------------------

    private static void descargar(String url, Path destino, DoubleConsumer avance) throws IOException {
        HttpClient cliente = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(30))
                .header("User-Agent", "LlegaATuPrime")
                .GET()
                .build();

        HttpResponse<InputStream> respuesta;
        try {
            respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La descarga fue interrumpida.", e);
        }
        if (respuesta.statusCode() / 100 != 2) {
            throw new IOException("El servidor respondió con el código " + respuesta.statusCode() + ".");
        }

        long total = respuesta.headers().firstValueAsLong("Content-Length").orElse(-1);
        Path parcial = destino.resolveSibling(destino.getFileName() + ".parcial");
        try (InputStream entrada = respuesta.body();
             OutputStream salida = Files.newOutputStream(parcial)) {
            byte[] buffer = new byte[64 * 1024];
            long leidos = 0;
            int n;
            while ((n = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, n);
                leidos += n;
                if (total > 0) {
                    avance.accept(Math.min(1.0, (double) leidos / total));
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(parcial);
            throw e;
        }
        Files.move(parcial, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Extrae el binario indicado del paquete comprimido; devuelve true si lo encontro. */
    private static boolean extraerBinario(Path zip, Path carpeta, String nombre) throws IOException {
        try (ZipInputStream lector = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entrada;
            while ((entrada = lector.getNextEntry()) != null) {
                String ruta = entrada.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                if (!entrada.isDirectory() && (ruta.endsWith("/bin/" + nombre) || ruta.equals(nombre))) {
                    Path salida = carpeta.resolve(nombre);
                    Path parcial = carpeta.resolve(nombre + ".parcial");
                    try (OutputStream destino = Files.newOutputStream(parcial)) {
                        lector.transferTo(destino);
                    }
                    Files.move(parcial, salida, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Path existente(Path ruta) {
        if (!Files.isRegularFile(ruta)) {
            return null;
        }
        // En macOS un binario sin permiso de ejecucion se considera no instalado.
        return esWindows() || Files.isExecutable(ruta) ? ruta : null;
    }

    /** Marca el binario como ejecutable para que macOS no bloquee su uso. */
    private static void hacerEjecutable(Path binario) {
        if (binario != null && !esWindows()) {
            binario.toFile().setExecutable(true, false);
        }
    }

    private static String urlYtDlp() {
        return esWindows() ? URL_YTDLP_WINDOWS : URL_YTDLP_MACOS;
    }

    private static String urlFfmpeg() {
        return arquitecturaArm() ? URL_FFMPEG_MACOS_ARM : URL_FFMPEG_MACOS_X64;
    }

    /** true cuando el equipo usa una arquitectura ARM (Apple Silicon). */
    private static boolean arquitecturaArm() {
        String arquitectura = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arquitectura.contains("aarch64") || arquitectura.contains("arm");
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void notificar(Escucha escucha, double fraccion, String mensaje) {
        if (escucha != null) {
            Platform.runLater(() -> escucha.progreso(fraccion, mensaje));
        }
    }

    private static String porcentaje(double fraccion) {
        return Math.round(Math.max(0, Math.min(1, fraccion)) * 100) + "%";
    }

    private static String mensajeAmigable(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException
                || e instanceof HttpConnectTimeoutException || e instanceof HttpTimeoutException) {
            return "No se pudo conectar con el servidor para descargar las herramientas. "
                    + "Comprueba tu conexión a internet e inténtalo de nuevo.";
        }
        String mensaje = e.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? "No se pudieron preparar las herramientas de descarga." : mensaje;
    }
}
