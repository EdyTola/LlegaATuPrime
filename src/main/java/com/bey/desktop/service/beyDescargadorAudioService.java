package com.bey.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bey.desktop.util.beyProcesoOculto;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class beyDescargadorAudioService {
    @FunctionalInterface
    public interface beyEscucha {
        void beyProgreso(double fraccion, String mensaje);
    }

    public record beyResultado(File archivo, String titulo, String artista, double duracionSegundos) {
    }

    private static final Pattern PORCENTAJE = Pattern.compile("\\[download]\\s+(\\d+(?:\\.\\d+)?)%");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long ESPERA_PROGRESO = 250;

    private record beyLanzamiento(Process proceso, Path registro) {
    }

    private static String beyRutaYtDlp;

    private beyDescargadorAudioService() {
    }

    public static synchronized String beyRutaYtDlp() {
        if (beyRutaYtDlp != null) {
            return beyRutaYtDlp;
        }
        Path local = beyGestorHerramientasService.beyRutaYtDlp();
        if (local != null) {
            beyRutaYtDlp = local.toString();
            return beyRutaYtDlp;
        }
        for (String candidato : beyCandidatos()) {
            if (candidato != null && !candidato.isBlank() && beyVerifica(candidato)) {
                beyRutaYtDlp = candidato;
                break;
            }
        }
        return beyRutaYtDlp;
    }

    public static Path beyRutaFfmpeg() {
        Path local = beyGestorHerramientasService.beyRutaFfmpeg();
        if (local != null) {
            return local;
        }
        String propiedad = System.getProperty("llegatuprime.ffmpeg");
        return propiedad != null && Files.isRegularFile(Path.of(propiedad))
                ? Path.of(propiedad) : null;
    }

    public static void beyDescargar(String url, Path carpetaDestino, beyEscucha escucha,
                                 Consumer<beyResultado> alTerminar, Consumer<String> alFallar) {
        String ejecutable = beyRutaYtDlp();
        if (ejecutable == null) {
            Platform.runLater(() -> alFallar.accept(
                    "Las herramientas de descarga todavía no están instaladas."));
            return;
        }
        Thread hilo = new Thread(() -> beyEjecutar(ejecutable, url, carpetaDestino, escucha, alTerminar, alFallar),
                "descarga-audio");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void beyEjecutar(String ejecutable, String url, Path carpetaDestino, beyEscucha escucha,
                                 Consumer<beyResultado> alTerminar, Consumer<String> alFallar) {
        try {
            Files.createDirectories(carpetaDestino);
            String registro = "";
            for (int intento = 1; intento <= INTENTOS_PROCESO; intento++) {
                beyNotificar(escucha, -1, intento == 1
                        ? "Conectando con el servidor…"
                        : "El servidor no respondió, reintentando (" + intento + "/" + INTENTOS_PROCESO + ")…");
                beyLanzamiento lanzamiento = beyIniciar(ejecutable, beyArgumentos(url, carpetaDestino));
                int codigo;
                try {
                    codigo = beySeguirProgreso(lanzamiento, escucha);
                    registro = beyLeerRegistro(lanzamiento.registro());
                } finally {
                    beyEliminar(lanzamiento.registro());
                }
                if (codigo == 0) {
                    beyResultado resultado = beyResolverResultado(carpetaDestino);
                    if (resultado == null) {
                        beyFallar(alFallar, "La conversión finalizó pero no se encontró el archivo MP3 generado."
                                + SUGERENCIA_ARCHIVOS);
                        return;
                    }
                    Platform.runLater(() -> alTerminar.accept(resultado));
                    return;
                }
                if (!beyEsTransitorio(registro) || intento == INTENTOS_PROCESO) {
                    break;
                }
                Thread.sleep(3000L * intento);
            }
            beyFallar(alFallar, beyDiagnosticar(registro));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            beyFallar(alFallar, "La descarga fue interrumpida.");
        } catch (Exception e) {
            beyFallar(alFallar, beyMensajeAmigable(e));
        }
    }

    private static final int INTENTOS_PROCESO = 3;
    private static final String SUGERENCIA_ARCHIVOS =
            " Mientras tanto puedes usar «Añadir archivos» o arrastrar tus MP3/WAV a la lista.";
    private static final Pattern ERROR_503 = Pattern.compile(
            "HTTP Error 5\\d\\d|\\b50[0234]\\b.*(unavailable|gateway|timeout)|Service Unavailable",
            Pattern.CASE_INSENSITIVE);

    private static String beyLeerRegistro(Path registro) {
        try {
            return Files.readString(registro, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static boolean beyEsTransitorio(String registro) {
        String min = registro.toLowerCase(Locale.ROOT);
        return ERROR_503.matcher(registro).find() || min.contains("timed out")
                || min.contains("connection reset") || min.contains("temporarily");
    }

    private static String beyDiagnosticar(String registro) {
        String min = registro.toLowerCase(Locale.ROOT);
        if (ERROR_503.matcher(registro).find()) {
            return "El servidor del enlace respondió con un error temporal (503) al obtener el audio, "
                    + "incluso tras varios reintentos. No es un problema de la aplicación. Espera unos "
                    + "segundos y vuelve a intentarlo, prueba con otro enlace o pulsa «Actualizar motor» "
                    + "por si el extractor quedó desactualizado." + SUGERENCIA_ARCHIVOS;
        }
        if (min.contains("http error 429") || min.contains("too many requests")) {
            return "La plataforma limitó temporalmente las peticiones (429). Espera unos minutos y "
                    + "vuelve a intentarlo." + SUGERENCIA_ARCHIVOS;
        }
        if (min.contains("sign in") || min.contains("confirm you") || min.contains("http error 403")) {
            return "La plataforma bloqueó la descarga (acceso denegado o verificación). Pulsa «Actualizar "
                    + "motor» y reintenta; si persiste, prueba con otro enlace." + SUGERENCIA_ARCHIVOS;
        }
        if (min.contains("video unavailable") || min.contains("private video") || min.contains("not available")) {
            return "El video no está disponible (privado, eliminado o restringido por región)."
                    + SUGERENCIA_ARCHIVOS;
        }
        if (min.contains("unsupported url")) {
            return "El enlace no corresponde a una plataforma de audio/video compatible.";
        }
        if (min.contains("unable to extract") || min.contains("unable to download")
                || min.contains("extractor")) {
            return "No se pudo extraer el audio del enlace; el motor de descarga probablemente está "
                    + "desactualizado. Pulsa «Actualizar motor» y reintenta." + SUGERENCIA_ARCHIVOS;
        }
        if (min.contains("urlopen error") || min.contains("getaddrinfo") || min.contains("network is unreachable")) {
            return "No se pudo conectar con el servidor. Comprueba tu conexión a internet."
                    + SUGERENCIA_ARCHIVOS;
        }
        return "No se pudo completar la descarga. Comprueba tu conexión y que el enlace sea válido, o "
                + "pulsa «Actualizar motor» y reintenta." + SUGERENCIA_ARCHIVOS;
    }

    private static List<String> beyArgumentos(String url, Path carpetaDestino) {
        List<String> argumentos = new ArrayList<>(List.of(
                "--no-playlist",
                "--no-mtime",
                "--newline",
                "--retries", "5",
                "--fragment-retries", "5",
                "--extractor-retries", "3",
                "--retry-sleep", "exp=1:20",
                "--socket-timeout", "30",
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--write-info-json"));
        Path ffmpeg = beyRutaFfmpeg();
        if (ffmpeg != null) {
            argumentos.add("--ffmpeg-location");
            argumentos.add(ffmpeg.toString());
        }
        argumentos.add("-o");
        argumentos.add(carpetaDestino.resolve("%(title)s.%(ext)s").toString());
        argumentos.add(url);
        return argumentos;
    }

    private static beyLanzamiento beyIniciar(String ejecutable, List<String> argumentos) throws IOException {
        Path registro = Files.createTempFile("ltp-descarga-", ".log");
        List<String> comando = new ArrayList<>();
        comando.add(ejecutable);
        comando.addAll(argumentos);
        try {
            return new beyLanzamiento(beyProcesoOculto.beyLanzar(comando, registro).proceso(), registro);
        } catch (IOException e) {
            beyEliminar(registro);
            throw e;
        }
    }

    private static int beySeguirProgreso(beyLanzamiento lanzamiento, beyEscucha escucha) throws InterruptedException {
        Process proceso = lanzamiento.proceso();
        int procesadas = 0;
        while (proceso.isAlive()) {
            procesadas = beyInterpretar(lanzamiento.registro(), procesadas, escucha);
            Thread.sleep(ESPERA_PROGRESO);
        }
        int codigo = proceso.waitFor();
        beyInterpretar(lanzamiento.registro(), procesadas, escucha);
        return codigo;
    }

    private static int beyInterpretar(Path registro, int desde, beyEscucha escucha) {
        if (!Files.isRegularFile(registro)) {
            return desde;
        }
        String contenido;
        try {
            contenido = Files.readString(registro, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return desde;
        }
        String[] lineas = contenido.split("\n", -1);
        int completas = Math.max(0, lineas.length - 1);
        for (int i = desde; i < completas; i++) {
            beyTraducir(lineas[i], escucha);
        }
        return completas;
    }

    private static void beyTraducir(String linea, beyEscucha escucha) {
        Matcher porcentaje = PORCENTAJE.matcher(linea);
        if (porcentaje.find()) {
            double fraccion = Math.min(1.0, Double.parseDouble(porcentaje.group(1)) / 100.0);
            beyNotificar(escucha, fraccion, "Descargando audio… " + Math.round(fraccion * 100) + "%");
        } else if (linea.contains("[ExtractAudio]") || linea.contains("[ffmpeg]")) {
            beyNotificar(escucha, -1, "Convirtiendo a MP3…");
        }
    }

    private static beyResultado beyResolverResultado(Path carpeta) {
        File mp3 = null;
        String titulo = null;
        String artista = null;
        double duracion = 0;

        File info = beyUltimoInfoJson(carpeta);
        if (info != null) {
            try {
                JsonNode raiz = MAPPER.readTree(info);
                titulo = beyTexto(raiz, "title");
                artista = beyPrimerTexto(raiz, "artist", "uploader", "channel", "creator");
                duracion = raiz.path("duration").asDouble(0);
            } catch (IOException ignored) {
            }
            String nombre = info.getName();
            String base = nombre.substring(0, nombre.length() - ".info.json".length());
            mp3 = new File(info.getParentFile(), base + ".mp3");
            info.delete();
        }
        if (mp3 == null || !mp3.isFile()) {
            mp3 = beyUltimoMp3(carpeta);
        }
        if (mp3 == null || !mp3.isFile()) {
            return null;
        }
        return new beyResultado(mp3, titulo, artista, duracion);
    }

    private static File beyUltimoInfoJson(Path carpeta) {
        File[] archivos = carpeta.toFile().listFiles(
                f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".info.json"));
        return beyMasReciente(archivos);
    }

    private static File beyUltimoMp3(Path carpeta) {
        File[] archivos = carpeta.toFile().listFiles(
                f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3"));
        return beyMasReciente(archivos);
    }

    private static File beyMasReciente(File[] archivos) {
        if (archivos == null || archivos.length == 0) {
            return null;
        }
        return Arrays.stream(archivos).max(Comparator.comparingLong(File::lastModified)).orElse(null);
    }

    private static void beyNotificar(beyEscucha escucha, double fraccion, String mensaje) {
        if (escucha != null) {
            Platform.runLater(() -> escucha.beyProgreso(fraccion, mensaje));
        }
    }

    private static void beyFallar(Consumer<String> alFallar, String mensaje) {
        if (alFallar != null) {
            Platform.runLater(() -> alFallar.accept(mensaje));
        }
    }

    private static void beyEliminar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
        }
    }

    private static boolean beyEsWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String beyTexto(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private static String beyPrimerTexto(JsonNode raiz, String... campos) {
        for (String campo : campos) {
            String valor = beyTexto(raiz, campo);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static String beyMensajeAmigable(Exception e) {
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return "No se pudo conectar con el servidor. Comprueba tu conexión a internet.";
        }
        if (e instanceof IOException) {
            return "No se pudo iniciar la descarga de audio. Comprueba tu conexión a internet.";
        }
        String mensaje = e.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? "No se pudo descargar ni convertir el audio." : mensaje;
    }

    private static List<String> beyCandidatos() {
        String ejecutable = beyGestorHerramientasService.beyNombreYtDlp();
        List<String> lista = new ArrayList<>();
        lista.add(System.getProperty("llegatuprime.ytdlp"));
        lista.add(System.getenv("YTDLP"));
        lista.add(System.getenv("YT_DLP"));
        lista.add(Path.of("tools", ejecutable).toAbsolutePath().toString());
        lista.add("yt-dlp");
        lista.add(ejecutable);
        return lista;
    }

    private static boolean beyVerifica(String comando) {
        try {
            return beyProcesoOculto.beyEjecutar(List.of(comando, "--version"), 15).exito();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
