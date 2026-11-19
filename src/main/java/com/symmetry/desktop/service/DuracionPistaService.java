package com.symmetry.desktop.service;

import com.symmetry.desktop.model.PistaAudio;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lee la duracion real de las pistas locales (mp3, wav, m4a, aac) para que la columna
 * "Duracion" muestre {@code mm:ss} desde que la pista se agrega, sin esperar a reproducirla.
 *
 * <p>Si {@link Media#getDuration()} ya es conocida se usa directamente; si es {@code UNKNOWN}
 * se abre un {@link MediaPlayer} temporal y se lee en {@code setOnReady}. Las pistas se
 * procesan de una en una (un solo reproductor temporal vivo a la vez) para no saturar los
 * recursos de audio al cargar carpetas grandes. Todo ocurre en el hilo de JavaFX.</p>
 */
public final class DuracionPistaService {

    /** Tiempo maximo de espera por pista antes de pasar a la siguiente. */
    private static final Duration ESPERA_MAXIMA = Duration.seconds(8);

    private static final Deque<PistaAudio> COLA = new ArrayDeque<>();
    private static final Set<String> EN_COLA = new HashSet<>();
    private static boolean procesando;

    private DuracionPistaService() {
    }

    /**
     * Encola las pistas locales cuya duracion aun es desconocida (0) y las resuelve en segundo
     * plano. {@code alActualizar} se invoca (en el hilo de JavaFX) cada vez que se completa una.
     * Debe llamarse desde el hilo de JavaFX.
     */
    public static void completar(Collection<PistaAudio> pistas, Runnable alActualizar) {
        if (pistas == null) {
            return;
        }
        for (PistaAudio pista : List.copyOf(pistas)) {
            if (pista == null || pista.esEnlace() || pista.getDuracionSegundos() > 0
                    || pista.getRutaArchivo() == null || !EN_COLA.add(pista.getId())) {
                continue;
            }
            COLA.add(pista);
        }
        if (!procesando) {
            siguiente(alActualizar);
        }
    }

    private static void siguiente(Runnable alActualizar) {
        PistaAudio pista = COLA.poll();
        if (pista == null) {
            procesando = false;
            return;
        }
        procesando = true;
        leer(pista, segundos -> {
            EN_COLA.remove(pista.getId());
            if (segundos > 0) {
                pista.setDuracionSegundos(segundos);
                GestorDatos.getInstancia().guardarAutomatico();
                if (alActualizar != null) {
                    alActualizar.run();
                }
            }
            // Se cede el turno al hilo de JavaFX antes de abrir la siguiente.
            Platform.runLater(() -> siguiente(alActualizar));
        });
    }

    /** Lee la duracion de una pista; entrega 0 si no se pudo determinar. */
    private static void leer(PistaAudio pista, java.util.function.DoubleConsumer resultado) {
        if (!ArchivoMultimediaService.existe(pista.getRutaArchivo())) {
            resultado.accept(0);
            return;
        }
        boolean[] terminado = new boolean[1];
        MediaPlayer[] temporal = new MediaPlayer[1];
        Runnable cerrar = () -> {
            if (temporal[0] != null) {
                try {
                    temporal[0].dispose();
                } catch (RuntimeException ignorada) {
                    // El reproductor temporal ya estaba liberado.
                }
                temporal[0] = null;
            }
        };
        java.util.function.DoubleConsumer entregar = segundos -> {
            if (terminado[0]) {
                return;
            }
            terminado[0] = true;
            cerrar.run();
            resultado.accept(Double.isFinite(segundos) && segundos > 0 ? segundos : 0);
        };
        try {
            Media media = new Media(Path.of(pista.getRutaArchivo()).toUri().toString());
            Duration directa = media.getDuration();
            if (directa != null && !directa.isUnknown() && !directa.isIndefinite() && directa.toSeconds() > 0) {
                entregar.accept(directa.toSeconds());
                return;
            }
            MediaPlayer jugador = new MediaPlayer(media);
            temporal[0] = jugador;
            jugador.setMute(true);
            jugador.setOnReady(() -> Platform.runLater(() -> entregar.accept(media.getDuration().toSeconds())));
            jugador.setOnError(() -> Platform.runLater(() -> entregar.accept(0)));
            media.setOnError(() -> Platform.runLater(() -> entregar.accept(0)));
            // Si el formato nunca responde, no se bloquea la cola.
            javafx.animation.PauseTransition limite = new javafx.animation.PauseTransition(ESPERA_MAXIMA);
            limite.setOnFinished(e -> entregar.accept(0));
            limite.play();
        } catch (RuntimeException e) {
            entregar.accept(0);
        }
    }
}
