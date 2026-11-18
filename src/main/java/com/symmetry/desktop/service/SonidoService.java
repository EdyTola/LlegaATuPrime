package com.symmetry.desktop.service;

import com.symmetry.desktop.model.ConfiguracionApp;
import javafx.scene.media.AudioClip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Efectos de sonido (SFX) cortos de logro. Los sonidos se sintetizan en codigo (tonos con
 * armonicos y decaimiento) y se guardan como WAV temporales, de modo que la aplicacion no
 * depende de archivos de audio externos y sigue siendo 100% offline.
 *
 * <p>Respeta la configuracion del perfil ({@link ConfiguracionApp#isSfxActivados()} y
 * {@link ConfiguracionApp#getVolumenSfx()}). Si el equipo no tiene salida de audio, los
 * errores se ignoran en silencio.</p>
 */
public final class SonidoService {

    /** Efectos disponibles. */
    public enum Efecto {
        /** Chime breve al cumplir una meta. */
        EXITO,
        /** Tintineo muy corto al completar una serie. */
        SERIE,
        /** Arpegio ascendente al desbloquear un logro (reto o dia perfecto). */
        LOGRO,
        /** Fanfarria al subir de nivel. */
        NIVEL
    }

    private static final int FRECUENCIA_MUESTREO = 44_100;
    /** Evita que dos eventos simultaneos reproduzcan el mismo efecto encimado. */
    private static final long MIN_SEPARACION_MS = 600;

    private static final Map<Efecto, AudioClip> CLIPS = new EnumMap<>(Efecto.class);
    private static final Map<Efecto, Long> ULTIMA = new EnumMap<>(Efecto.class);

    private SonidoService() {
    }

    /** Reproduce el efecto con el volumen configurado (no hace nada si esta silenciado). */
    public static void reproducir(Efecto efecto) {
        ConfiguracionApp config = GestorDatos.getInstancia().getDatos().getConfiguracion();
        if (!config.isSfxActivados()) {
            return;
        }
        reproducir(efecto, config.getVolumenSfx());
    }

    /** Reproduce el efecto con un volumen explicito (usado por el boton "Probar"). */
    public static void reproducir(Efecto efecto, double volumen) {
        long ahora = System.currentTimeMillis();
        Long previo = ULTIMA.get(efecto);
        if (previo != null && ahora - previo < MIN_SEPARACION_MS) {
            return;
        }
        ULTIMA.put(efecto, ahora);
        try {
            AudioClip clip = CLIPS.computeIfAbsent(efecto, SonidoService::crearClip);
            if (clip != null) {
                clip.play(Math.max(0, Math.min(1, volumen)));
            }
        } catch (RuntimeException ignorado) {
            // Sin dispositivo de audio o formato no soportado: la app sigue funcionando.
        }
    }

    // ------------------------------------------------------------------
    // Sintesis
    // ------------------------------------------------------------------

    /** Nota: frecuencia (Hz), inicio (ms), duracion (ms) y amplitud relativa. */
    private record Nota(double hz, int inicioMs, int duracionMs, double amplitud) {
    }

    private static AudioClip crearClip(Efecto efecto) {
        try {
            byte[] wav = sintetizar(notasDe(efecto));
            Path archivo = Files.createTempFile("prime-sfx-" + efecto.name().toLowerCase() + "-", ".wav");
            Files.write(archivo, wav);
            archivo.toFile().deleteOnExit();
            return new AudioClip(archivo.toUri().toString());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Nota[] notasDe(Efecto efecto) {
        return switch (efecto) {
            case EXITO -> new Nota[]{
                    new Nota(1318.5, 0, 260, 0.9), new Nota(1760.0, 110, 420, 1.0)};
            case SERIE -> new Nota[]{
                    new Nota(987.8, 0, 170, 0.8), new Nota(1318.5, 70, 240, 0.9)};
            case LOGRO -> new Nota[]{
                    new Nota(1046.5, 0, 300, 0.8), new Nota(1318.5, 110, 300, 0.85),
                    new Nota(1568.0, 220, 320, 0.9), new Nota(2093.0, 330, 650, 1.0)};
            case NIVEL -> new Nota[]{
                    new Nota(523.3, 0, 260, 0.8), new Nota(659.3, 150, 260, 0.85),
                    new Nota(784.0, 300, 280, 0.9), new Nota(1046.5, 450, 800, 1.0),
                    new Nota(1318.5, 700, 900, 0.55), new Nota(1568.0, 700, 900, 0.55)};
        };
    }

    /** Mezcla las notas (onda con 2 armonicos, ataque rapido y decaimiento exponencial) en un WAV mono de 16 bits. */
    private static byte[] sintetizar(Nota[] notas) throws IOException {
        int totalMs = 0;
        for (Nota n : notas) {
            totalMs = Math.max(totalMs, n.inicioMs() + n.duracionMs());
        }
        int muestras = FRECUENCIA_MUESTREO * (totalMs + 80) / 1000;
        double[] mezcla = new double[muestras];

        for (Nota n : notas) {
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
        return envolverWav(datos.array());
    }

    private static byte[] envolverWav(byte[] pcm) throws IOException {
        ByteBuffer cabecera = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        cabecera.put("RIFF".getBytes());
        cabecera.putInt(36 + pcm.length);
        cabecera.put("WAVE".getBytes());
        cabecera.put("fmt ".getBytes());
        cabecera.putInt(16);
        cabecera.putShort((short) 1);                       // PCM
        cabecera.putShort((short) 1);                       // mono
        cabecera.putInt(FRECUENCIA_MUESTREO);
        cabecera.putInt(FRECUENCIA_MUESTREO * 2);           // bytes por segundo
        cabecera.putShort((short) 2);                       // alineacion de bloque
        cabecera.putShort((short) 16);                      // bits por muestra
        cabecera.put("data".getBytes());
        cabecera.putInt(pcm.length);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(cabecera.array());
        salida.write(pcm);
        return salida.toByteArray();
    }
}
