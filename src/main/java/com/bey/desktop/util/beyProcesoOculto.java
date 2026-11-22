package com.bey.desktop.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class beyProcesoOculto {
    public record beyLanzamiento(Process proceso, Path registro) {
    }

    public record beyResultado(int codigo, String salida) {
        public boolean exito() {
            return codigo == 0;
        }
    }

    private beyProcesoOculto() {
    }

    public static boolean beyEsWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static beyLanzamiento beyLanzar(List<String> comando, Path registro) throws IOException {
        if (beyEsWindows()) {
            Path lanzador = beyCrearLanzador(comando, registro);
            try {
                Process proceso = new ProcessBuilder("wscript.exe", "//Nologo", lanzador.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                proceso.onExit().thenRun(() -> beyBorrar(lanzador));
                return new beyLanzamiento(proceso, registro);
            } catch (IOException e) {
                beyBorrar(lanzador);
                throw e;
            }
        }
        Process proceso = new ProcessBuilder(comando)
                .redirectErrorStream(true)
                .redirectOutput(registro.toFile())
                .start();
        return new beyLanzamiento(proceso, registro);
    }

    public static beyResultado beyEjecutar(List<String> comando, long segundos) throws IOException, InterruptedException {
        Path registro = Files.createTempFile("ltp-proc-", ".log");
        try {
            beyLanzamiento lanzamiento = beyLanzar(comando, registro);
            Process proceso = lanzamiento.proceso();
            if (!proceso.waitFor(segundos, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                return new beyResultado(-1, "");
            }
            String salida = Files.exists(registro) ? Files.readString(registro, StandardCharsets.UTF_8) : "";
            return new beyResultado(proceso.exitValue(), salida.trim());
        } finally {
            beyBorrar(registro);
        }
    }

    private static Path beyCrearLanzador(List<String> comando, Path registro) throws IOException {
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

    private static void beyBorrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorada) {
        }
    }
}
