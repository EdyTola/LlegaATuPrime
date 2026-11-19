package com.symmetry.desktop.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta programas externos (yt-dlp, ffmpeg) sin que Windows abra una ventana de consola.
 *
 * <p>Cuando la aplicacion corre con {@code javaw.exe} no tiene consola, y Windows crea una
 * ventana negra nueva por cada subproceso de consola que se inicia con {@link ProcessBuilder}
 * (Java no expone {@code CREATE_NO_WINDOW}). Por eso en Windows el comando se ejecuta mediante
 * un script de WScript con {@code WScript.Shell.Run ..., 0, True} (ventana oculta) y su salida
 * se redirige a un archivo temporal. En otros sistemas se usa {@link ProcessBuilder} directo.</p>
 */
public final class ProcesoOculto {

    /** Proceso lanzado junto con el archivo donde vuelca su salida (stdout + stderr). */
    public record Lanzamiento(Process proceso, Path registro) {
    }

    /** Resultado de una ejecucion completa. */
    public record Resultado(int codigo, String salida) {
        public boolean exito() {
            return codigo == 0;
        }
    }

    private ProcesoOculto() {
    }

    public static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * Inicia el comando sin ventana, volcando su salida a {@code registro}. El {@link Process}
     * devuelto termina cuando termina el comando y su {@code exitValue} es el codigo del comando.
     */
    public static Lanzamiento lanzar(List<String> comando, Path registro) throws IOException {
        if (esWindows()) {
            Path lanzador = crearLanzador(comando, registro);
            try {
                Process proceso = new ProcessBuilder("wscript.exe", "//Nologo", lanzador.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                proceso.onExit().thenRun(() -> borrar(lanzador));
                return new Lanzamiento(proceso, registro);
            } catch (IOException e) {
                borrar(lanzador);
                throw e;
            }
        }
        Process proceso = new ProcessBuilder(comando)
                .redirectErrorStream(true)
                .redirectOutput(registro.toFile())
                .start();
        return new Lanzamiento(proceso, registro);
    }

    /** Ejecuta el comando sin ventana y espera su fin (o {@code segundos} como maximo). */
    public static Resultado ejecutar(List<String> comando, long segundos) throws IOException, InterruptedException {
        Path registro = Files.createTempFile("ltp-proc-", ".log");
        try {
            Lanzamiento lanzamiento = lanzar(comando, registro);
            Process proceso = lanzamiento.proceso();
            if (!proceso.waitFor(segundos, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                return new Resultado(-1, "");
            }
            String salida = Files.exists(registro) ? Files.readString(registro, StandardCharsets.UTF_8) : "";
            return new Resultado(proceso.exitValue(), salida.trim());
        } finally {
            borrar(registro);
        }
    }

    private static Path crearLanzador(List<String> comando, Path registro) throws IOException {
        List<String> partes = new ArrayList<>();
        for (String parte : comando) {
            partes.add("\"" + parte + "\"");
        }
        String linea = "cmd.exe /c \"" + String.join(" ", partes) + " > \"" + registro + "\" 2>&1\"";
        String script = "Set sh = CreateObject(\"WScript.Shell\")\r\n"
                + "codigo = sh.Run(\"" + linea.replace("\"", "\"\"") + "\", 0, True)\r\n"
                + "WScript.Quit codigo\r\n";
        Path archivo = Files.createTempFile("ltp-lanzador-", ".vbs");
        Files.writeString(archivo, script, StandardCharsets.UTF_8);
        return archivo;
    }

    private static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorada) {
            // Lo limpiara el sistema con los temporales.
        }
    }
}
