package com.bey.desktop.util;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class beyEspectroAudio extends HBox {
    private static final int BARRAS = 44;
    private static final double ANCHO_BARRA = 5;
    private static final double ALTO_MAXIMO = 78;
    private static final double ALTO_MINIMO = 3;
    private static final double SUAVIZADO = 0.35;

    private static final long ESPERA_DATOS_MS = 300;

    private final List<Rectangle> beyBarras = new ArrayList<>();
    private final double[] beyObjetivo = new double[BARRAS];
    private final double[] beyNivel = new double[BARRAS];
    private final AnimationTimer beyAnimacion;

    private long beyUltimoDato;
    private long beyInicio;
    private boolean beyActivo;

    public beyEspectroAudio() {
        getStyleClass().add("espectro-audio");
        setSpacing(3);
        setAlignment(Pos.BOTTOM_CENTER);
        setMinHeight(ALTO_MAXIMO);
        setPrefHeight(ALTO_MAXIMO);

        for (int i = 0; i < BARRAS; i++) {
            Rectangle barra = new Rectangle(ANCHO_BARRA, ALTO_MINIMO);
            barra.setArcWidth(3);
            barra.setArcHeight(3);
            barra.getStyleClass().add("espectro-barra");
            beyBarras.add(barra);
            getChildren().add(barra);
        }

        beyAnimacion = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (beyInicio == 0) {
                    beyInicio = ahora;
                    beyUltimoDato = 0;
                }
                boolean datosRecientes = beyUltimoDato > 0
                        && (ahora - beyUltimoDato) / 1_000_000 < ESPERA_DATOS_MS;
                if (!datosRecientes) {
                    beyPatronSintetico((ahora - beyInicio) / 1_000_000_000.0);
                }
                beyPintar();
            }
        };
    }

    public void beyActivar() {
        if (beyActivo) {
            return;
        }
        beyActivo = true;
        beyInicio = 0;
        beyAnimacion.start();
    }

    public void beyDesactivar() {
        beyActivo = false;
        beyAnimacion.stop();
        Arrays.fill(beyObjetivo, 0);
        Arrays.fill(beyNivel, 0);
        for (Rectangle barra : beyBarras) {
            barra.setHeight(ALTO_MINIMO);
        }
    }

    public void beyActualizar(double[] magnitudes) {
        if (magnitudes == null || magnitudes.length == 0) {
            return;
        }
        beyUltimoDato = System.nanoTime();
        for (int i = 0; i < BARRAS; i++) {
            int indice = (int) ((long) i * magnitudes.length / BARRAS);
            double decibelios = Math.max(-60, Math.min(0, magnitudes[Math.min(indice, magnitudes.length - 1)]));
            beyObjetivo[i] = (decibelios + 60) / 60.0;
        }
    }

    private void beyPatronSintetico(double tiempo) {
        for (int i = 0; i < BARRAS; i++) {
            double centro = 1 - Math.abs(i - BARRAS / 2.0) / (BARRAS / 2.0);
            double onda = 0.34 + 0.30 * Math.sin(tiempo * 2.3 + i * 0.42);
            double detalle = 0.18 * Math.sin(tiempo * 5.1 + i * 0.9);
            double aleatorio = 0.10 * Math.random();
            beyObjetivo[i] = Math.max(0.04,
                    Math.min(1, (onda + detalle) * (0.55 + 0.45 * centro) + aleatorio));
        }
    }

    private void beyPintar() {
        for (int i = 0; i < BARRAS; i++) {
            beyNivel[i] += (beyObjetivo[i] - beyNivel[i]) * SUAVIZADO;
            beyBarras.get(i).setHeight(ALTO_MINIMO + (ALTO_MAXIMO - ALTO_MINIMO) * beyNivel[i]);
        }
    }
}
