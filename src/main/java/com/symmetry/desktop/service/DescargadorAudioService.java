package com.symmetry.desktop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Descarga y conversion de audio a partir de enlaces (YouTube y otras plataformas soportadas
 * por yt-dlp). La herramienta externa y el conversor ffmpeg se localizan en la carpeta local
 * de la aplicacion ({@code tools}) y, si no existen, {@link GestorHerramientasService} los
 * descarga automaticamente segun el sistema operativo (Windows o macOS). Todo el proceso se
 * ejecuta en segundo plano sin abrir ninguna ventana de consola ni terminal.
 *
 * <p>Tras la conversion, la pista se guarda como MP3 en la carpeta gestionada por la
 * aplicacion y sus metadatos (titulo, artista y duracion) se extraen de forma automatica.</p>
 */
public final class DescargadorAudioService {

    /** Escucha de progreso: fraccion 0..1, o -1 cuando el avance es indeterminado. */
    @FunctionalInterface
    public interface Escucha {
        void progreso(double fraccion, String mensaje);
    }

    /** Pista descargada junto con los metadatos leidos del origen del enlace. */
    public record Resultado(File archivo, String titulo, String artista, double duracionSegundos) {
    }

    private static final Pattern PORCENTAJE = Pattern.compile("\\[download]\\s+(\\d+(?:\\.\\d+)?)%");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long ESPERA_PROGRESO = 250;

    /** Proceso en ejecucion junto con el archivo donde yt-dlp vuelca su salida. */
    private record Lanzamiento(Process proceso, Path registro) {
    }

    private static String rutaYtDlp;

    private DescargadorAudioService() {
    }

    /** Ruta del ejecutable de yt-dlp, o {@code null} si no se encuentra en el sistema. */
    public static synchronized String rutaYtDlp() {
        if (rutaYtDlp != null) {
            return rutaYtDlp;
        }
        Path local = GestorHerramientasService.rutaYtDlp();
        if (local != null) {
            rutaYtDlp = local.toString();
            return rutaYtDlp;
        }
        for (String candidato : candidatos()) {
            if (candidato != null && !candidato.isBlank() && verifica(candidato)) {
                rutaYtDlp = candidato;
                break;
            }
        }
        return rutaYtDlp;
    }

    /** Ruta de ffmpeg si esta disponible localmente, o {@code null} (se usara el del PATH). */
    public static Path rutaFfmpeg() {
        Path local = GestorHerramientasService.rutaFfmpeg();
        if (local != null) {
            return local;
        }
        String propiedad = System.getProperty("llegatuprime.ffmpeg");
        return propiedad != null && Files.isRegularFile(Path.of(propiedad))
                ? Path.of(propiedad) : null;
    }

    /**
     * Descarga el audio del enlace, lo convierte a MP3 dentro de {@code carpetaDestino} y
     * extrae sus metadatos. Todo el trabajo ocurre en un hilo de fondo; las llamadas de
     * retorno se entregan ya en el hilo de la interfaz de JavaFX.
     *
     * @param url             enlace de origen (YouTube u otra plataforma soportada)
     * @param carpetaDestino  carpeta gestionada por la aplicacion donde se guardara el MP3
     * @param escucha         notificaciones de progreso (fraccion y mensaje de estado)
     * @param alTerminar      recibe el MP3 resultante y sus metadatos cuando finaliza
     * @param alFallar        recibe un mensaje claro y sin tecnicismos si algo falla
     */
    public static void descargar(String url, Path carpetaDestino, Escucha escucha,
                                 Consumer<Resultado> alTerminar, Consumer<String> alFallar) {
        String ejecutable = rutaYtDlp();
        if (ejecutable == null) {
            Platform.runLater(() -> alFallar.accept(
                    "Las herramientas de descarga todavía no están instaladas."));
            return;
        }
        Thread hilo = new Thread(() -> ejecutar(ejecutable, url, carpetaDestino, escucha, alTerminar, alFallar),
                "descarga-audio");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void ejecutar(String ejecutable, String url, Path carpetaDestino, Escucha escucha,
                                 Consumer<Resultado> alTerminar, Consumer<String> alFallar) {
        Lanzamiento lanzamiento = null;
        try {
            Files.createDirectories(carpetaDestino);
            notificar(escucha, -1, "Conectando con el servidor…");

            lanzamiento = iniciar(ejecutable, argumentos(url, carpetaDestino));
            int codigo = seguirProgreso(lanzamiento, escucha);
            if (codigo != 0) {
                fallar(alFallar, "No se pudo completar la descarga. Comprueba tu conexión a internet "
                        + "y que el enlace sea válido y accesible.");
                return;
            }
            Resultado resultado = resolverResultado(carpetaDestino);
            if (resultado == null) {
                fallar(alFallar, "La conversión finalizó pero no se encontró el archivo MP3 generado.");
                return;
            }
            Platform.runLater(() -> alTerminar.accept(resultado));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fallar(alFallar, "La descarga fue interrumpida.");
        } catch (Exception e) {
            fallar(alFallar, mensajeAmigable(e));
        } finally {
            if (lanzamiento != null) {
                eliminar(lanzamiento.registro());
            }
        }
    }

    /** Argumentos de yt-dlp: audio MP3 de maxima calidad, metadatos y carpeta de ffmpeg. */
    private static List<String> argumentos(String url, Path carpetaDestino) {
        List<String> argumentos = new ArrayList<>(List.of(
                "--no-playlist",
                "--no-mtime",
                "--newline",
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "--write-info-json"));
        Path ffmpeg = rutaFfmpeg();
        if (ffmpeg != null) {
            argumentos.add("--ffmpeg-location");
            argumentos.add(ffmpeg.toString());
        }
        argumentos.add("-o");
        argumentos.add(carpetaDestino.resolve("%(title)s.%(ext)s").toString());
        argumentos.add(url);
        return argumentos;
    }

    /**
     * Lanza yt-dlp redirigiendo su salida a un archivo temporal que la aplicacion lee para
     * mostrar el progreso. En Windows se ejecuta a traves de un lanzador oculto (WScript y
     * cmd) para que no aparezca ninguna ventana de consola; si el lanzador no estuviera
     * disponible se recurre a la ejecucion directa.
     */
    private static Lanzamiento iniciar(String ejecutable, List<String> argumentos) throws IOException {
        Path registro = Files.createTempFile("ltp-descarga-", ".log");
        if (esWindows() && System.console() == null) {
            Path lanzador = crearLanzadorOculto(ejecutable, argumentos, registro);
            if (lanzador != null) {
                try {
                    ProcessBuilder constructor = new ProcessBuilder(
                            "wscript.exe", "//Nologo", lanzador.toString());
                    constructor.redirectErrorStream(true);
                    constructor.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    Process proceso = constructor.start();
                    proceso.onExit().thenRun(() -> eliminar(lanzador));
                    return new Lanzamiento(proceso, registro);
                } catch (IOException e) {
                    eliminar(lanzador);
                }
            }
        }
        List<String> comando = new ArrayList<>();
        comando.add(ejecutable);
        comando.addAll(argumentos);
        ProcessBuilder constructor = new ProcessBuilder(comando);
        constructor.redirectErrorStream(true);
        constructor.redirectOutput(registro.toFile());
        return new Lanzamiento(constructor.start(), registro);
    }

    /**
     * Crea un script WScript que ejecuta el comando oculto (ventana 0) y vuelca su salida al
     * archivo de registro. Devuelve la ruta del script, o {@code null} si no pudo crearse.
     */
    private static Path crearLanzadorOculto(String ejecutable, List<String> argumentos, Path registro) {
        try {
            StringBuilder linea = new StringBuilder(citar(ejecutable));
            for (String argumento : argumentos) {
                linea.append(' ').append(citar(argumento));
            }
            String comando = "cmd.exe /c \"" + linea + " > " + citar(registro.toString()) + " 2>&1\"";
            String script = "Set sh = CreateObject(\"WScript.Shell\")\r\n"
                    + "codigo = sh.Run(" + literalVbs(comando) + ", 0, True)\r\n"
                    + "WScript.Quit codigo\r\n";
            Path archivo = Files.createTempFile("ltp-descarga-", ".vbs");
            Files.writeString(archivo, script, StandardCharsets.UTF_8);
            return archivo;
        } catch (IOException e) {
            return null;
        }
    }

    /** Espera a que yt-dlp termine leyendo su salida para mantener informada a la interfaz. */
    private static int seguirProgreso(Lanzamiento lanzamiento, Escucha escucha) throws InterruptedException {
        Process proceso = lanzamiento.proceso();
        int procesadas = 0;
        while (proceso.isAlive()) {
            procesadas = interpretar(lanzamiento.registro(), procesadas, escucha);
            Thread.sleep(ESPERA_PROGRESO);
        }
        int codigo = proceso.waitFor();
        interpretar(lanzamiento.registro(), procesadas, escucha);
        return codigo;
    }

    /** Lee del registro las lineas nuevas y traduce el progreso de yt-dlp a la interfaz. */
    private static int interpretar(Path registro, int desde, Escucha escucha) {
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
            traducir(lineas[i], escucha);
        }
        return completas;
    }

    /** Interpreta una linea de salida de yt-dlp y notifica el progreso correspondiente. */
    private static void traducir(String linea, Escucha escucha) {
        Matcher porcentaje = PORCENTAJE.matcher(linea);
        if (porcentaje.find()) {
            double fraccion = Math.min(1.0, Double.parseDouble(porcentaje.group(1)) / 100.0);
            notificar(escucha, fraccion, "Descargando audio… " + Math.round(fraccion * 100) + "%");
        } else if (linea.contains("[ExtractAudio]") || linea.contains("[ffmpeg]")) {
            notificar(escucha, -1, "Convirtiendo a MP3…");
        }
    }

    /** Localiza el MP3 generado y extrae sus metadatos del archivo de informacion de yt-dlp. */
    private static Resultado resolverResultado(Path carpeta) {
        File mp3 = null;
        String titulo = null;
        String artista = null;
        double duracion = 0;

        File info = ultimoInfoJson(carpeta);
        if (info != null) {
            try {
                JsonNode raiz = MAPPER.readTree(info);
                titulo = texto(raiz, "title");
                artista = primerTexto(raiz, "artist", "uploader", "channel", "creator");
                duracion = raiz.path("duration").asDouble(0);
            } catch (IOException ignored) {
                // Sin metadatos legibles la pista se agrega igualmente con los datos del archivo.
            }
            String nombre = info.getName();
            String base = nombre.substring(0, nombre.length() - ".info.json".length());
            mp3 = new File(info.getParentFile(), base + ".mp3");
            info.delete();
        }
        if (mp3 == null || !mp3.isFile()) {
            mp3 = ultimoMp3(carpeta);
        }
        if (mp3 == null || !mp3.isFile()) {
            return null;
        }
        return new Resultado(mp3, titulo, artista, duracion);
    }

    private static File ultimoInfoJson(Path carpeta) {
        File[] archivos = carpeta.toFile().listFiles(
                f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".info.json"));
        return masReciente(archivos);
    }

    /** MP3 mas reciente de la carpeta, usado como respaldo si no habia archivo de informacion. */
    private static File ultimoMp3(Path carpeta) {
        File[] archivos = carpeta.toFile().listFiles(
                f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3"));
        return masReciente(archivos);
    }

    private static File masReciente(File[] archivos) {
        if (archivos == null || archivos.length == 0) {
            return null;
        }
        return Arrays.stream(archivos).max(Comparator.comparingLong(File::lastModified)).orElse(null);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static void notificar(Escucha escucha, double fraccion, String mensaje) {
        if (escucha != null) {
            Platform.runLater(() -> escucha.progreso(fraccion, mensaje));
        }
    }

    private static void fallar(Consumer<String> alFallar, String mensaje) {
        if (alFallar != null) {
            Platform.runLater(() -> alFallar.accept(mensaje));
        }
    }

    private static String citar(String valor) {
        return "\"" + valor + "\"";
    }

    /** Convierte un texto en un literal de cadena valido dentro del script VBScript. */
    private static String literalVbs(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private static void eliminar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // El archivo temporal se limpiara por el sistema si no se puede borrar ahora.
        }
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String texto(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private static String primerTexto(JsonNode raiz, String... campos) {
        for (String campo : campos) {
            String valor = texto(raiz, campo);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static String mensajeAmigable(Exception e) {
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

    private static List<String> candidatos() {
        String ejecutable = GestorHerramientasService.nombreYtDlp();
        List<String> lista = new ArrayList<>();
        lista.add(System.getProperty("llegatuprime.ytdlp"));
        lista.add(System.getenv("YTDLP"));
        lista.add(System.getenv("YT_DLP"));
        lista.add(Path.of("tools", ejecutable).toAbsolutePath().toString());
        lista.add("yt-dlp");
        lista.add(ejecutable);
        return lista;
    }

    /** Comprueba que el comando responde ejecutando {@code --version}. */
    private static boolean verifica(String comando) {
        try {
            Process proceso = new ProcessBuilder(comando, "--version")
                    .redirectErrorStream(true)
                    .start();
            proceso.getInputStream().readAllBytes();
            return proceso.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
