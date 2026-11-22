package com.bey.desktop.service;

import com.bey.desktop.model.beyPistaAudio;
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

public final class beyDuracionPistaService {
    private static final Duration ESPERA_MAXIMA = Duration.seconds(8);

    private static final Deque<beyPistaAudio> COLA = new ArrayDeque<>();
    private static final Set<String> EN_COLA = new HashSet<>();
    private static boolean beyProcesando;

    private beyDuracionPistaService() {
    }

    public static void beyCompletar(Collection<beyPistaAudio> pistas, Runnable alActualizar) {
        if (pistas == null) {
            return;
        }
        for (beyPistaAudio pista : List.copyOf(pistas)) {
            if (pista == null || pista.esEnlace() || pista.getDuracionSegundos() > 0
                    || pista.getRutaArchivo() == null || !EN_COLA.add(pista.getId())) {
                continue;
            }
            COLA.add(pista);
        }
        if (!beyProcesando) {
            beySiguiente(alActualizar);
        }
    }

    private static void beySiguiente(Runnable alActualizar) {
        beyPistaAudio pista = COLA.poll();
        if (pista == null) {
            beyProcesando = false;
            return;
        }
        beyProcesando = true;
        beyLeer(pista, segundos -> {
            EN_COLA.remove(pista.getId());
            if (segundos > 0) {
                pista.setDuracionSegundos(segundos);
                beyGestorDatos.beyGetInstancia().beyGuardarAutomatico();
                if (alActualizar != null) {
                    alActualizar.run();
                }
            }

            Platform.runLater(() -> beySiguiente(alActualizar));
        });
    }

    private static void beyLeer(beyPistaAudio pista, java.util.function.DoubleConsumer resultado) {
        if (!beyArchivoMultimediaService.beyExiste(pista.getRutaArchivo())) {
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

            javafx.animation.PauseTransition limite = new javafx.animation.PauseTransition(ESPERA_MAXIMA);
            limite.setOnFinished(e -> entregar.accept(0));
            limite.play();
        } catch (RuntimeException e) {
            entregar.accept(0);
        }
    }
}
