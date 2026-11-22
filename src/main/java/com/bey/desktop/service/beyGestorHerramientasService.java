package com.bey.desktop.service;

import com.bey.desktop.util.beyProcesoOculto;
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

public final class beyGestorHerramientasService {
    @FunctionalInterface
    public interface beyEscucha {
        void beyProgreso(double fraccion, String mensaje);
    }

    private static final String URL_YTDLP_WINDOWS =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final String URL_YTDLP_MACOS =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos";

    private static final String URL_FFMPEG_WINDOWS =
            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    private static final String URL_FFMPEG_MACOS_ARM =
            "https://github.com/eugeneware/ffmpeg-static/releases/latest/download/ffmpeg-darwin-arm64";

    private static final String URL_FFMPEG_MACOS_X64 =
            "https://github.com/eugeneware/ffmpeg-static/releases/latest/download/ffmpeg-darwin-x64";

    private static final String FFPROBE = "ffprobe.exe";

    private beyGestorHerramientasService() {
    }

    public static Path beyCarpetaHerramientas() {
        Path base = beyJsonPersistenciaService.beyRutaPorDefecto().getParent();
        return (base == null ? Path.of("tools") : base.resolve("tools"));
    }

    public static String beyNombreYtDlp() {
        return beyEsWindows() ? "yt-dlp.exe" : "yt-dlp";
    }

    public static String beyNombreFfmpeg() {
        return beyEsWindows() ? "ffmpeg.exe" : "ffmpeg";
    }

    public static Path beyRutaYtDlp() {
        return beyExistente(beyCarpetaHerramientas().resolve(beyNombreYtDlp()));
    }

    public static Path beyRutaFfmpeg() {
        return beyExistente(beyCarpetaHerramientas().resolve(beyNombreFfmpeg()));
    }

    public static boolean beyInstaladas() {
        return beyRutaYtDlp() != null && beyRutaFfmpeg() != null;
    }

    public static void beyAsegurar(beyEscucha escucha, Runnable exito, Consumer<String> error) {
        Thread hilo = new Thread(() -> {
            try {
                Path carpeta = beyCarpetaHerramientas();
                Files.createDirectories(carpeta);

                if (beyRutaYtDlp() == null) {
                    beyNotificar(escucha, -1, "Preparando el motor de descargas (yt-dlp)…");
                    Path destino = carpeta.resolve(beyNombreYtDlp());
                    beyDescargar("yt-dlp", beyUrlYtDlp(), destino,
                            avance -> beyNotificar(escucha, avance * 0.5,
                                    "Descargando yt-dlp… " + beyPorcentaje(avance)));
                    beyHacerEjecutable(destino);
                }
                if (beyRutaFfmpeg() == null) {
                    beyNotificar(escucha, 0.5, "Preparando el conversor de audio (ffmpeg)…");
                    if (beyEsWindows()) {
                        Path zip = Files.createTempFile("ltp-ffmpeg-", ".zip");
                        try {
                            beyDescargar("ffmpeg", URL_FFMPEG_WINDOWS, zip,
                                    avance -> beyNotificar(escucha, 0.5 + avance * 0.5,
                                            "Descargando ffmpeg… " + beyPorcentaje(avance)));
                            beyExtraerBinario(zip, carpeta, beyNombreFfmpeg());
                            beyExtraerBinario(zip, carpeta, FFPROBE);
                        } finally {
                            Files.deleteIfExists(zip);
                        }
                    } else {
                        Path destino = carpeta.resolve(beyNombreFfmpeg());
                        beyDescargar("ffmpeg", beyUrlFfmpeg(), destino,
                                avance -> beyNotificar(escucha, 0.5 + avance * 0.5,
                                        "Descargando ffmpeg… " + beyPorcentaje(avance)));
                        beyHacerEjecutable(destino);
                    }
                }
                if (!beyInstaladas()) {
                    throw new IOException("Las herramientas no se instalaron correctamente.");
                }
                Platform.runLater(exito);
            } catch (Exception e) {
                String mensaje = beyMensajeAmigable(e);
                Platform.runLater(() -> error.accept(mensaje));
            }
        }, "herramientas-descarga");
        hilo.setDaemon(true);
        hilo.start();
    }

    public static void beyActualizarYtDlp(beyEscucha escucha, Consumer<String> exito, Consumer<String> error) {
        Thread hilo = new Thread(() -> {
            try {
                Path carpeta = beyCarpetaHerramientas();
                Files.createDirectories(carpeta);
                Path destino = carpeta.resolve(beyNombreYtDlp());
                Path nuevo = carpeta.resolve(beyNombreYtDlp() + ".nuevo");
                beyNotificar(escucha, -1, "Buscando la última versión de yt-dlp…");
                try {
                    beyDescargar("yt-dlp", beyUrlYtDlp(), nuevo,
                            avance -> beyNotificar(escucha, avance, "Descargando yt-dlp… " + beyPorcentaje(avance)));
                    beyHacerEjecutable(nuevo);
                    Files.move(nuevo, destino, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(nuevo);
                }
                String version = beyVersionYtDlp(destino);
                String mensaje = version == null
                        ? "yt-dlp se actualizó correctamente."
                        : "yt-dlp se actualizó a la versión " + version + ".";
                Platform.runLater(() -> exito.accept(mensaje));
            } catch (Exception e) {
                String mensaje = beyMensajeAmigable(e);
                Platform.runLater(() -> error.accept(mensaje));
            }
        }, "herramientas-actualizar");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static String beyVersionYtDlp(Path ejecutable) {
        try {
            var resultado = beyProcesoOculto.beyEjecutar(java.util.List.of(ejecutable.toString(), "--version"), 15);
            return resultado.exito() && !resultado.salida().isBlank() ? resultado.salida() : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final int INTENTOS_DESCARGA = 3;

    public static final class beyErrorServidorHerramienta extends IOException {
        private final String beyHerramienta;
        private final int beyCodigo;

        beyErrorServidorHerramienta(String herramienta, int codigo) {
            super("El servidor respondió con el código " + codigo + " al descargar " + herramienta + ".");
            this.beyHerramienta = herramienta;
            this.beyCodigo = codigo;
        }

        public String beyHerramienta() {
            return beyHerramienta;
        }

        public int beyCodigo() {
            return beyCodigo;
        }
    }

    private static boolean beyTransitorio(int codigo) {
        return codigo == 429 || codigo >= 500;
    }

    private static void beyDescargar(String herramienta, String url, Path destino, DoubleConsumer avance)
            throws IOException {
        IOException ultimo = null;
        for (int intento = 1; intento <= INTENTOS_DESCARGA; intento++) {
            try {
                beyDescargarUnaVez(herramienta, url, destino, avance);
                return;
            } catch (beyErrorServidorHerramienta e) {
                if (!beyTransitorio(e.beyCodigo())) {
                    throw e;
                }
                ultimo = e;
            } catch (HttpTimeoutException | ConnectException e) {
                ultimo = e;
            }
            if (intento < INTENTOS_DESCARGA) {
                try {
                    Thread.sleep(2000L * intento);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("La descarga fue interrumpida.", e);
                }
            }
        }
        throw ultimo;
    }

    private static void beyDescargarUnaVez(String herramienta, String url, Path destino, DoubleConsumer avance)
            throws IOException {
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
            try (InputStream cuerpo = respuesta.body()) {
            }
            throw new beyErrorServidorHerramienta(herramienta, respuesta.statusCode());
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

    private static boolean beyExtraerBinario(Path zip, Path carpeta, String nombre) throws IOException {
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

    private static Path beyExistente(Path ruta) {
        if (!Files.isRegularFile(ruta)) {
            return null;
        }

        return beyEsWindows() || Files.isExecutable(ruta) ? ruta : null;
    }

    private static void beyHacerEjecutable(Path binario) {
        if (binario != null && !beyEsWindows()) {
            binario.toFile().setExecutable(true, false);
        }
    }

    private static String beyUrlYtDlp() {
        return beyEsWindows() ? URL_YTDLP_WINDOWS : URL_YTDLP_MACOS;
    }

    private static String beyUrlFfmpeg() {
        return beyArquitecturaArm() ? URL_FFMPEG_MACOS_ARM : URL_FFMPEG_MACOS_X64;
    }

    private static boolean beyArquitecturaArm() {
        String arquitectura = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arquitectura.contains("aarch64") || arquitectura.contains("arm");
    }

    private static boolean beyEsWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void beyNotificar(beyEscucha escucha, double fraccion, String mensaje) {
        if (escucha != null) {
            Platform.runLater(() -> escucha.beyProgreso(fraccion, mensaje));
        }
    }

    private static String beyPorcentaje(double fraccion) {
        return Math.round(Math.max(0, Math.min(1, fraccion)) * 100) + "%";
    }

    private static final String SUGERENCIA_ARCHIVOS =
            " Mientras tanto puedes usar «Añadir archivos» o arrastrar tus MP3/WAV a la lista.";

    private static String beyMensajeAmigable(Exception e) {
        if (e instanceof beyErrorServidorHerramienta fallo) {
            String que = "yt-dlp".equals(fallo.beyHerramienta())
                    ? "el motor de descarga (yt-dlp)" : "el conversor de audio (ffmpeg)";
            if (beyTransitorio(fallo.beyCodigo())) {
                return "El servidor que aloja " + que + " no está disponible ahora mismo (error "
                        + fallo.beyCodigo() + "), incluso tras varios reintentos. Esto no depende del enlace "
                        + "de música. Espera unos segundos e inténtalo de nuevo." + SUGERENCIA_ARCHIVOS;
            }
            return "No se pudo descargar " + que + " (el servidor respondió con el código "
                    + fallo.beyCodigo() + "). Inténtalo de nuevo más tarde." + SUGERENCIA_ARCHIVOS;
        }
        if (e instanceof UnknownHostException || e instanceof ConnectException
                || e instanceof HttpConnectTimeoutException || e instanceof HttpTimeoutException) {
            return "No se pudo conectar con el servidor para descargar las herramientas. "
                    + "Comprueba tu conexión a internet e inténtalo de nuevo." + SUGERENCIA_ARCHIVOS;
        }
        String mensaje = e.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? "No se pudieron preparar las herramientas de descarga." : mensaje;
    }
}
