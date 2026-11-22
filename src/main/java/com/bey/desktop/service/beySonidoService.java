package com.bey.desktop.service;

import com.bey.desktop.model.beyConfiguracionApp;
import javafx.scene.media.AudioClip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class beySonidoService {
    public enum beyEfecto {
        EXITO,

        SERIE,

        LOGRO,

        NIVEL
    }

    private static final int FRECUENCIA_MUESTREO = 44_100;

    private static final long MIN_SEPARACION_MS = 600;

    private static final Map<beyEfecto, AudioClip> CLIPS = new EnumMap<>(beyEfecto.class);
    private static final Map<beyEfecto, Long> ULTIMA = new EnumMap<>(beyEfecto.class);

    private beySonidoService() {
    }

    public static void beyReproducir(beyEfecto efecto) {
        beyConfiguracionApp config = beyGestorDatos.beyGetInstancia().beyGetDatos().getConfiguracion();
        if (!config.isSfxActivados()) {
            return;
        }
        beyReproducir(efecto, config.getVolumenSfx());
    }

    public static void beyReproducir(beyEfecto efecto, double volumen) {
        long ahora = System.currentTimeMillis();
        Long previo = ULTIMA.get(efecto);
        if (previo != null && ahora - previo < MIN_SEPARACION_MS) {
            return;
        }
        ULTIMA.put(efecto, ahora);
        try {
            AudioClip clip = CLIPS.computeIfAbsent(efecto, beySonidoService::beyCrearClip);
            if (clip != null) {
                clip.play(Math.max(0, Math.min(1, volumen)));
            }
        } catch (RuntimeException ignorado) {
        }
    }

    private record beyNota(double hz, int inicioMs, int duracionMs, double amplitud) {
    }

    private static AudioClip beyCrearClip(beyEfecto efecto) {
        try {
            byte[] wav = beySintetizar(beyNotasDe(efecto));
            Path archivo = Files.createTempFile("prime-sfx-" + efecto.name().toLowerCase() + "-", ".wav");
            Files.write(archivo, wav);
            archivo.toFile().deleteOnExit();
            return new AudioClip(archivo.toUri().toString());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static beyNota[] beyNotasDe(beyEfecto efecto) {
        return switch (efecto) {
            case EXITO -> new beyNota[]{
                    new beyNota(1318.5, 0, 260, 0.9), new beyNota(1760.0, 110, 420, 1.0)};
            case SERIE -> new beyNota[]{
                    new beyNota(987.8, 0, 170, 0.8), new beyNota(1318.5, 70, 240, 0.9)};
            case LOGRO -> new beyNota[]{
                    new beyNota(1046.5, 0, 300, 0.8), new beyNota(1318.5, 110, 300, 0.85),
                    new beyNota(1568.0, 220, 320, 0.9), new beyNota(2093.0, 330, 650, 1.0)};
            case NIVEL -> new beyNota[]{
                    new beyNota(523.3, 0, 260, 0.8), new beyNota(659.3, 150, 260, 0.85),
                    new beyNota(784.0, 300, 280, 0.9), new beyNota(1046.5, 450, 800, 1.0),
                    new beyNota(1318.5, 700, 900, 0.55), new beyNota(1568.0, 700, 900, 0.55)};
        };
    }

    private static byte[] beySintetizar(beyNota[] notas) throws IOException {
        int totalMs = 0;
        for (beyNota n : notas) {
            totalMs = Math.max(totalMs, n.inicioMs() + n.duracionMs());
        }
        int muestras = FRECUENCIA_MUESTREO * (totalMs + 80) / 1000;
        double[] mezcla = new double[muestras];

        for (beyNota n : notas) {
            int inicio = FRECUENCIA_MUESTREO * n.inicioMs() / 1000;
            int largo = FRECUENCIA_MUESTREO * n.duracionMs() / 1000;
            for (int i = 0; i < largo && inicio + i < muestras; i++) {
                double t = (double) i / FRECUENCIA_MUESTREO;
                double progreso = (double) i / largo;
                double ataque = Math.min(1.0, i / (FRECUENCIA_MUESTREO * 0.005));
                double envolvente = ataque * Math.exp(-4.2 * progreso);
                double onda = Math.sin(2 * Math.PI * n.hz() * t)
                        + 0.30 * Math.sin(2 * Math.PI * 2 * n.hz() * t)
                        + 0.12 * Math.sin(2 * Math.PI * 3 * n.hz() * t);
                mezcla[inicio + i] += onda * envolvente * n.amplitud();
            }
        }

        double pico = 0;
        for (double m : mezcla) {
            pico = Math.max(pico, Math.abs(m));
        }
        double ganancia = pico == 0 ? 0 : 0.8 / pico;

        ByteBuffer datos = ByteBuffer.allocate(muestras * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (double m : mezcla) {
            datos.putShort((short) Math.round(m * ganancia * Short.MAX_VALUE));
        }
        return beyEnvolverWav(datos.array());
    }

    private static byte[] beyEnvolverWav(byte[] pcm) throws IOException {
        ByteBuffer cabecera = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        cabecera.put("RIFF".getBytes());
        cabecera.putInt(36 + pcm.length);
        cabecera.put("WAVE".getBytes());
        cabecera.put("fmt ".getBytes());
        cabecera.putInt(16);
        cabecera.putShort((short) 1);
        cabecera.putShort((short) 1);
        cabecera.putInt(FRECUENCIA_MUESTREO);
        cabecera.putInt(FRECUENCIA_MUESTREO * 2);
        cabecera.putShort((short) 2);
        cabecera.putShort((short) 16);
        cabecera.put("data".getBytes());
        cabecera.putInt(pcm.length);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(cabecera.array());
        salida.write(pcm);
        return salida.toByteArray();
    }
}
