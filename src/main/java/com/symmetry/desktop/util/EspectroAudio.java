package com.symmetry.desktop.util;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visualizador de audio tipo ecualizador: una fila de barras que se mueven al ritmo de la
 * pista. Cuando el reproductor entrega magnitudes reales (archivos locales) las barras siguen
 * el espectro real; si no hay datos (por ejemplo, un enlace de YouTube abierto en el
 * navegador) se dibuja un patron sintetico para mantener vivo el efecto durante el entreno.
 *
 * <p>Se construye por completo en codigo y respeta el tema claro/oscuro mediante clases de
 * estilo, igual que el resto de componentes graficos de la aplicacion.</p>
 */
public final class EspectroAudio extends HBox {

    private static final int BARRAS = 44;
    private static final double ANCHO_BARRA = 5;
    private static final double ALTO_MAXIMO = 78;
    private static final double ALTO_MINIMO = 3;
    private static final double SUAVIZADO = 0.35;

    /** Margen (ms) sin magnitudes reales tras el cual se usa el patron sintetico. */
    private static final long ESPERA_DATOS_MS = 300;

    private final List<Rectangle> barras = new ArrayList<>();
    private final double[] objetivo = new double[BARRAS];
    private final double[] nivel = new double[BARRAS];
    private final AnimationTimer animacion;

    private long ultimoDato;
    private long inicio;
    private boolean activo;

    public EspectroAudio() {
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
            barras.add(barra);
            getChildren().add(barra);
        }

        animacion = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (inicio == 0) {
                    inicio = ahora;
                    ultimoDato = 0;
                }
                boolean datosRecientes = ultimoDato > 0
                        && (ahora - ultimoDato) / 1_000_000 < ESPERA_DATOS_MS;
                if (!datosRecientes) {
                    patronSintetico((ahora - inicio) / 1_000_000_000.0);
                }
                pintar();
            }
        };
    }

    /** Arranca la animacion mientras hay una pista en curso. */
    public void activar() {
        if (activo) {
            return;
        }
        activo = true;
        inicio = 0;
        animacion.start();
    }

    /** Detiene la animacion y devuelve las barras a su altura de reposo. */
    public void desactivar() {
        activo = false;
        animacion.stop();
        Arrays.fill(objetivo, 0);
        Arrays.fill(nivel, 0);
        for (Rectangle barra : barras) {
            barra.setHeight(ALTO_MINIMO);
        }
    }

    /**
     * Alimenta el espectro con las magnitudes en decibelios que emite el reproductor. El
     * arreglo se remuestrea al numero de barras del visualizador.
     */
    public void actualizar(double[] magnitudes) {
        if (magnitudes == null || magnitudes.length == 0) {
            return;
        }
        ultimoDato = System.nanoTime();
        for (int i = 0; i < BARRAS; i++) {
            int indice = (int) ((long) i * magnitudes.length / BARRAS);
            double decibelios = Math.max(-60, Math.min(0, magnitudes[Math.min(indice, magnitudes.length - 1)]));
            objetivo[i] = (decibelios + 60) / 60.0;
        }
    }

    /** Patron animado con forma de onda central, usado cuando no hay magnitudes reales. */
    private void patronSintetico(double tiempo) {
        for (int i = 0; i < BARRAS; i++) {
            double centro = 1 - Math.abs(i - BARRAS / 2.0) / (BARRAS / 2.0);
            double onda = 0.34 + 0.30 * Math.sin(tiempo * 2.3 + i * 0.42);
            double detalle = 0.18 * Math.sin(tiempo * 5.1 + i * 0.9);
            double aleatorio = 0.10 * Math.random();
            objetivo[i] = Math.max(0.04,
                    Math.min(1, (onda + detalle) * (0.55 + 0.45 * centro) + aleatorio));
        }
    }

    /** Interpola la altura real de cada barra hacia su objetivo para un movimiento fluido. */
    private void pintar() {
        for (int i = 0; i < BARRAS; i++) {
            nivel[i] += (objetivo[i] - nivel[i]) * SUAVIZADO;
            barras.get(i).setHeight(ALTO_MINIMO + (ALTO_MAXIMO - ALTO_MINIMO) * nivel[i]);
        }
    }
}
