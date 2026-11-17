package com.symmetry.desktop.util;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Efecto de celebracion tipo videojuego: una lluvia de confeti que cae sobre la capa
 * indicada. Se implementa con formas vectoriales y un unico {@link AnimationTimer}, de
 * modo que no depende de recursos externos y funciona igual en modo claro y oscuro.
 */
public final class Confeti {

    /** Paleta de colores vivos, alineada con los acentos de la aplicacion. */
    private static final List<Color> PALETA = List.of(
            Color.web("#0078D4"), Color.web("#22C55E"), Color.web("#F59E0B"),
            Color.web("#EF4444"), Color.web("#A855F7"), Color.web("#06B6D4"));

    private static final int CANTIDAD = 90;
    private static final double DURACION = 2.2;

    private Confeti() {
    }

    /**
     * Lanza la lluvia de confeti sobre {@code capa} y retira los fragmentos al terminar.
     * La capa debe ser un contenedor transparente a los clics ({@code setMouseTransparent(true)}).
     */
    public static void estallar(Pane capa) {
        if (capa == null) {
            return;
        }
        double ancho = capa.getWidth() > 0 ? capa.getWidth() : 760;
        double alto = capa.getHeight() > 0 ? capa.getHeight() : 560;
        Random azar = new Random();

        List<Fragmento> fragmentos = new ArrayList<>();
        List<Node> nodos = new ArrayList<>();
        for (int i = 0; i < CANTIDAD; i++) {
            Rectangle trozo = new Rectangle(5 + azar.nextDouble() * 5, 8 + azar.nextDouble() * 8);
            trozo.setArcWidth(3);
            trozo.setArcHeight(3);
            trozo.setFill(PALETA.get(azar.nextInt(PALETA.size())));
            trozo.setMouseTransparent(true);
            trozo.setX(azar.nextDouble() * ancho);
            trozo.setY(-20 - azar.nextDouble() * alto * 0.4);

            capa.getChildren().add(trozo);
            nodos.add(trozo);
            fragmentos.add(new Fragmento(trozo,
                    -40 + azar.nextDouble() * 80,
                    90 + azar.nextDouble() * 190,
                    -200 + azar.nextDouble() * 400));
        }

        AnimationTimer animacion = new AnimationTimer() {

            private long inicio;

            @Override
            public void handle(long ahora) {
                if (inicio == 0) {
                    inicio = ahora;
                    return;
                }
                double transcurrido = (ahora - inicio) / 1_000_000_000.0;
                if (transcurrido >= DURACION) {
                    capa.getChildren().removeAll(nodos);
                    stop();
                    return;
                }
                double avance = transcurrido / DURACION;
                double opacidad = Math.max(0, 1 - Math.pow(avance, 3));
                for (Fragmento fragmento : fragmentos) {
                    fragmento.cae(transcurrido, opacidad);
                }
            }
        };
        animacion.start();
    }

    /** Fragmento de confeti con su propia velocidad y giro. */
    private record Fragmento(Node nodo, double velocidadX, double velocidadY, double giro) {

        private void cae(double segundos, double opacidad) {
            nodo.setTranslateX(velocidadX * segundos);
            nodo.setTranslateY(velocidadY * segundos);
            nodo.setRotate(giro * segundos);
            nodo.setOpacity(opacidad);
        }
    }
}
