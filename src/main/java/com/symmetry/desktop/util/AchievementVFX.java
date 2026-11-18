package com.symmetry.desktop.util;

import com.symmetry.desktop.service.GamificacionService;
import com.symmetry.desktop.service.GamificacionService.InfoNivel;
import com.symmetry.desktop.service.SonidoService;
import com.symmetry.desktop.service.SonidoService.Efecto;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sistema de efectos visuales (VFX) de logros y celebraciones. Agrupa, de forma modular:
 * <ul>
 *   <li>{@link #pulsoGlow}: pulso de brillo en el borde de una tarjeta con el color de acento;</li>
 *   <li>{@link #textoFlotante}: texto temporal que asciende y se desvanece ("+25 XP");</li>
 *   <li>{@link #confeti}: lluvia de confeti y particulas vectoriales en un {@link Canvas}
 *       transparente superpuesto (2-3 s);</li>
 *   <li>{@link #bannerLogro}: insignia superior para logros grandes (dia perfecto);</li>
 *   <li>{@link #nivelSubido}: modal epico de "Level Up" con escudo, fondo desenfocado y SFX.</li>
 * </ul>
 * Todo se dibuja con formas vectoriales y clases CSS del tema (claro/oscuro); el audio se
 * delega en {@link SonidoService}.
 */
public final class AchievementVFX {

    private static final Color ORO = Color.web("#F5B301");
    private static final Color[] PALETA = {
            Color.web("#0078D4"), Color.web("#22C55E"), Color.web("#F5B301"), Color.web("#EF4444"),
            Color.web("#A855F7"), Color.web("#06B6D4"), Color.web("#FB923C"), Color.web("#F472B6")};

    private static final String[] MOTIVACION = {
            "La constancia de hoy es el rendimiento de mañana.",
            "Cada repetición te acerca a tu mejor versión.",
            "Disciplina por encima de motivación: ya lo estás demostrando.",
            "Los hábitos pequeños construyen resultados enormes.",
            "No pares ahora: el siguiente nivel ya te espera."};

    /** Capas globales (ventana principal) usadas por los efectos que no reciben una explicita. */
    private static StackPane capaGlobal;
    private static Pane capaConfetiGlobal;

    private AchievementVFX() {
    }

    /** Registra las capas superpuestas de la ventana principal. */
    public static void registrarCapas(StackPane capa, Pane capaConfeti) {
        capaGlobal = capa;
        capaConfetiGlobal = capaConfeti;
    }

    // ------------------------------------------------------------------
    // 1. Micro-interaccion de meta cumplida
    // ------------------------------------------------------------------

    /** Combina glow, texto flotante y chime de exito sobre la tarjeta indicada. */
    public static void celebrarMeta(Node tarjeta, String texto) {
        celebrarMeta(tarjeta, capaGlobal, texto);
    }

    public static void celebrarMeta(Node tarjeta, Pane capaTextos, String texto) {
        pulsoGlow(tarjeta);
        textoFlotante(tarjeta, capaTextos, texto);
        SonidoService.reproducir(Efecto.EXITO);
    }

    /** Pulso de brillo del color de acento alrededor del nodo (dos latidos y desvanecimiento). */
    public static void pulsoGlow(Node nodo) {
        if (nodo == null) {
            return;
        }
        Effect previo = nodo.getEffect();
        DropShadow brillo = new DropShadow(0, colorAcento(nodo));
        brillo.setSpread(0.30);
        nodo.setEffect(brillo);

        Timeline pulso = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(brillo.radiusProperty(), 0),
                        new KeyValue(brillo.spreadProperty(), 0.0)),
                new KeyFrame(Duration.millis(220), new KeyValue(brillo.radiusProperty(), 30, Interpolator.EASE_OUT),
                        new KeyValue(brillo.spreadProperty(), 0.38, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(520), new KeyValue(brillo.radiusProperty(), 10, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(760), new KeyValue(brillo.radiusProperty(), 34, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1400), new KeyValue(brillo.radiusProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(brillo.spreadProperty(), 0.0, Interpolator.EASE_IN)));
        pulso.setOnFinished(e -> nodo.setEffect(previo));

        ScaleTransition latido = new ScaleTransition(Duration.millis(260), nodo);
        latido.setToX(1.035);
        latido.setToY(1.035);
        latido.setAutoReverse(true);
        latido.setCycleCount(2);
        latido.setOnFinished(e -> {
            nodo.setScaleX(1);
            nodo.setScaleY(1);
        });
        new ParallelTransition(pulso, latido).play();
    }

    /** Texto que nace sobre el nodo, asciende suavemente y se desvanece. */
    public static void textoFlotante(Node origen, String texto) {
        textoFlotante(origen, capaGlobal, texto);
    }

    public static void textoFlotante(Node origen, Pane capa, String texto) {
        if (origen == null || capa == null || origen.getScene() == null) {
            return;
        }
        Bounds caja = origen.localToScene(origen.getBoundsInLocal());
        Point2D punto = capa.sceneToLocal(caja.getCenterX(), caja.getMinY() + caja.getHeight() * 0.35);
        if (punto == null) {
            return;
        }
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("vfx-texto-flotante");
        etiqueta.setManaged(false);
        etiqueta.setMouseTransparent(true);
        capa.getChildren().add(etiqueta);
        etiqueta.applyCss();
        etiqueta.autosize();
        etiqueta.setLayoutX(punto.getX() - etiqueta.getWidth() / 2);
        etiqueta.setLayoutY(punto.getY() - etiqueta.getHeight() / 2);
        etiqueta.setOpacity(0);
        etiqueta.setScaleX(0.6);
        etiqueta.setScaleY(0.6);

        ScaleTransition aparece = new ScaleTransition(Duration.millis(220), etiqueta);
        aparece.setToX(1);
        aparece.setToY(1);
        aparece.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition entra = new FadeTransition(Duration.millis(180), etiqueta);
        entra.setToValue(1);
        TranslateTransition sube = new TranslateTransition(Duration.millis(1500), etiqueta);
        sube.setByY(-70);
        sube.setInterpolator(Interpolator.EASE_OUT);
        SequentialTransition desvanece = new SequentialTransition(
                new PauseTransition(Duration.millis(900)), fade(etiqueta, 600));
        ParallelTransition todo = new ParallelTransition(entra, aparece, sube, desvanece);
        todo.setOnFinished(e -> capa.getChildren().remove(etiqueta));
        todo.play();
    }

    private static FadeTransition fade(Node nodo, int ms) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), nodo);
        f.setToValue(0);
        return f;
    }

    private static Color colorAcento(Node nodo) {
        for (Parent p = nodo instanceof Parent padre ? padre : nodo.getParent(); p != null; p = p.getParent()) {
            if (p.getStyleClass().contains("tema-oscuro")) {
                return Color.web("#3AA0FF");
            }
        }
        if (nodo.getScene() != null && nodo.getScene().getRoot().getStyleClass().contains("tema-oscuro")) {
            return Color.web("#3AA0FF");
        }
        return Color.web("#0078D4");
    }

    // ------------------------------------------------------------------
    // 2. Confeti y particulas vectoriales
    // ------------------------------------------------------------------

    /** Lluvia de confeti sobre la capa global de la ventana principal. */
    public static void confeti() {
        confeti(capaConfetiGlobal);
    }

    /**
     * Lluvia de confeti digital multicolor (rectangulos, discos y serpentinas) dibujada en un
     * {@link Canvas} transparente superpuesto a {@code capa}. Cae durante ~3 s y se desvanece;
     * el lienzo se retira solo al terminar.
     */
    public static void confeti(Pane capa) {
        if (capa == null) {
            return;
        }
        double ancho = capa.getWidth() > 0 ? capa.getWidth() : 900;
        double alto = capa.getHeight() > 0 ? capa.getHeight() : 600;
        Canvas lienzo = new Canvas(ancho, alto);
        lienzo.setMouseTransparent(true);
        lienzo.widthProperty().bind(capa.widthProperty());
        lienzo.heightProperty().bind(capa.heightProperty());
        capa.getChildren().add(lienzo);

        Random azar = new Random();
        List<Particula> particulas = new ArrayList<>();
        for (int i = 0; i < 190; i++) {
            particulas.add(new Particula(azar, ancho));
        }

        final double duracion = 3.0;
        AnimationTimer reloj = new AnimationTimer() {
            private long inicio;
            private long anterior;

            @Override
            public void handle(long ahora) {
                if (inicio == 0) {
                    inicio = ahora;
                    anterior = ahora;
                    return;
                }
                double t = (ahora - inicio) / 1e9;
                double dt = (ahora - anterior) / 1e9;
                anterior = ahora;
                if (t >= duracion) {
                    stop();
                    capa.getChildren().remove(lienzo);
                    lienzo.widthProperty().unbind();
                    lienzo.heightProperty().unbind();
                    return;
                }
                GraphicsContext gc = lienzo.getGraphicsContext2D();
                gc.clearRect(0, 0, lienzo.getWidth(), lienzo.getHeight());
                double desvanecer = Math.min(1.0, (duracion - t) / 0.8);
                for (Particula p : particulas) {
                    p.avanzar(t, dt);
                    p.dibujar(gc, t, desvanecer);
                }
            }
        };
        reloj.start();
    }

    /** Particula de confeti: posicion, velocidad, balanceo y giro propios. */
    private static final class Particula {

        private final double retraso;
        private final double velocidadY;
        private final double velocidadX;
        private final double balanceo;
        private final double frecuencia;
        private final double fase;
        private final double giro;
        private final double tamano;
        private final int forma;
        private final Color color;
        private double x;
        private double y;
        private double angulo;

        Particula(Random azar, double ancho) {
            retraso = azar.nextDouble() * 0.9;
            x = azar.nextDouble() * ancho;
            y = -20 - azar.nextDouble() * 60;
            velocidadY = 150 + azar.nextDouble() * 210;
            velocidadX = -35 + azar.nextDouble() * 70;
            balanceo = 12 + azar.nextDouble() * 34;
            frecuencia = 2 + azar.nextDouble() * 3;
            fase = azar.nextDouble() * Math.PI * 2;
            giro = -320 + azar.nextDouble() * 640;
            tamano = 5 + azar.nextDouble() * 6;
            forma = azar.nextInt(3);
            color = PALETA[azar.nextInt(PALETA.length)];
        }

        void avanzar(double t, double dt) {
            if (t < retraso) {
                return;
            }
            y += velocidadY * dt;
            x += (velocidadX + Math.cos(t * frecuencia + fase) * balanceo) * dt;
            angulo += giro * dt;
        }

        void dibujar(GraphicsContext gc, double t, double desvanecer) {
            if (t < retraso) {
                return;
            }
            gc.save();
            gc.setGlobalAlpha(desvanecer);
            gc.translate(x, y);
            gc.rotate(angulo);
            gc.setFill(color);
            // "Volteo" del confeti: la altura aparente oscila como al girar en 3D.
            double volteo = 0.35 + 0.65 * Math.abs(Math.cos(t * frecuencia * 1.7 + fase));
            switch (forma) {
                case 0 -> gc.fillRect(-tamano / 2, -tamano * volteo, tamano, tamano * 2 * volteo);
                case 1 -> gc.fillOval(-tamano / 2, -tamano / 2 * volteo, tamano, tamano * volteo);
                default -> gc.fillRect(-tamano * 0.18, -tamano * 1.8, tamano * 0.36, tamano * 3.6 * volteo);
            }
            gc.restore();
        }
    }

    // ------------------------------------------------------------------
    // 3. Banner de logro y modal de Level Up
    // ------------------------------------------------------------------

    /** Toast de logro sobre la capa global de la ventana principal. */
    public static void bannerLogro(String titulo, String detalle) {
        toastLogro(capaGlobal, titulo, detalle);
    }

    /** Ancho y alto maximos del toast compacto (estilo notificacion de Windows 11). */
    private static final double TOAST_ANCHO_MAX = 340;
    private static final double TOAST_ALTO_MAX = 68;
    private static final double TOAST_MARGEN = 24;

    /**
     * Toast compacto de logro en la esquina superior derecha de {@code capa}: nunca supera
     * 340x68 px, no captura clics, entra deslizandose desde la derecha, permanece 2,5 s y se
     * elimina por completo de la capa. Si ya hay otros toasts visibles, se apila debajo.
     */
    public static void toastLogro(StackPane capa, String titulo, String detalle) {
        if (capa == null) {
            return;
        }
        Label simbolo = new Label("★");
        simbolo.getStyleClass().add("toast-logro-simbolo");
        Label t = new Label(titulo);
        t.getStyleClass().add("toast-logro-titulo");
        Label d = new Label(detalle == null ? "" : detalle);
        d.getStyleClass().add("toast-logro-detalle");
        t.setMaxWidth(TOAST_ANCHO_MAX - 70);
        d.setMaxWidth(TOAST_ANCHO_MAX - 70);
        VBox textos = new VBox(2, t, d);
        textos.setAlignment(Pos.CENTER_LEFT);
        textos.setMinHeight(Region.USE_PREF_SIZE);

        HBox toast = new HBox(12, simbolo, textos);
        toast.getStyleClass().add("toast-logro");
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMouseTransparent(true);
        // Tamano acotado: el StackPane padre nunca puede estirarlo.
        toast.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        toast.setPrefHeight(Region.USE_COMPUTED_SIZE);
        toast.setMaxSize(TOAST_ANCHO_MAX, TOAST_ALTO_MAX);

        long activos = capa.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("toast-logro")).count();
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new javafx.geometry.Insets(
                TOAST_MARGEN + activos * (TOAST_ALTO_MAX + 8), TOAST_MARGEN, TOAST_MARGEN, TOAST_MARGEN));
        toast.setOpacity(0);
        toast.setTranslateX(60);
        capa.getChildren().add(toast);
        if (capaConfetiGlobal != null && capa == capaGlobal) {
            capaConfetiGlobal.toFront();
        }

        FadeTransition entra = new FadeTransition(Duration.millis(260), toast);
        entra.setToValue(1);
        TranslateTransition desliza = new TranslateTransition(Duration.millis(260), toast);
        desliza.setToX(0);
        desliza.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition sale = new FadeTransition(Duration.millis(260), toast);
        sale.setToValue(0);
        TranslateTransition retira = new TranslateTransition(Duration.millis(260), toast);
        retira.setToX(60);
        retira.setInterpolator(Interpolator.EASE_IN);
        SequentialTransition ciclo = new SequentialTransition(
                new ParallelTransition(entra, desliza),
                new PauseTransition(Duration.millis(2500)),
                new ParallelTransition(sale, retira));
        ciclo.setOnFinished(e -> capa.getChildren().remove(toast));
        ciclo.play();
    }

    /** Aviso breve "+N XP" en la esquina inferior derecha. */
    public static void avisoXp(VBox contenedor, int xp, String motivo) {
        if (contenedor == null) {
            return;
        }
        Label aviso = new Label("+" + xp + " XP  ·  " + motivo);
        aviso.getStyleClass().add("toast-xp");
        aviso.setOpacity(0);
        aviso.setTranslateY(14);
        contenedor.getChildren().add(aviso);

        FadeTransition entra = new FadeTransition(Duration.millis(240), aviso);
        entra.setToValue(1);
        Timeline sube = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(aviso.translateYProperty(), 0, Interpolator.EASE_OUT)));
        SequentialTransition secuencia = new SequentialTransition(new ParallelTransition(entra, sube),
                new PauseTransition(Duration.millis(2800)), fade(aviso, 500));
        secuencia.setOnFinished(e -> contenedor.getChildren().remove(aviso));
        secuencia.play();
    }

    /**
     * Modal epico de subida de nivel: velo oscuro, fondo desenfocado, escudo brillante con el
     * nuevo nivel, titulo "¡HAS SUBIDO DE NIVEL!", rango alcanzado, frase de motivacion y el
     * boton "¡A por más!". Acompana con confeti y fanfarria.
     *
     * @param aDesenfocar nodos de la interfaz que se desenfocan mientras el modal esta abierto
     */
    public static void nivelSubido(StackPane capa, Pane capaConfeti, InfoNivel info,
                                   List<? extends Node> aDesenfocar, Runnable alCerrar) {
        StackPane destino = capa != null ? capa : capaGlobal;
        if (destino == null) {
            return;
        }
        Region velo = new Region();
        velo.getStyleClass().add("levelup-velo");

        Label etiqueta = new Label("¡HAS SUBIDO DE NIVEL!");
        etiqueta.getStyleClass().add("levelup-etiqueta");

        // Escudo brillante con el numero de nivel
        SVGPath escudo = new SVGPath();
        escudo.setContent("M50,2 L94,16 L94,52 C94,78 75,95 50,106 C25,95 6,78 6,52 L6,16 Z");
        escudo.getStyleClass().add("levelup-escudo");
        Label numero = new Label(String.valueOf(info.nivel()));
        numero.getStyleClass().add("levelup-numero");
        StackPane medalla = new StackPane(escudo, numero);
        medalla.setMinSize(120, 130);
        medalla.setMaxSize(120, 130);
        escudo.setScaleX(1.2);
        escudo.setScaleY(1.2);

        Circle anillo = anillo();
        Circle anilloDos = anillo();
        StackPane escenario = new StackPane(anillo, anilloDos, medalla);
        escenario.setMinHeight(210);
        escenario.setPrefHeight(210);

        Label rango = new Label("Nivel " + info.nivel() + ": " + info.titulo());
        rango.getStyleClass().add("levelup-titulo");
        Label motivacion = new Label(MOTIVACION[info.nivel() % MOTIVACION.length]);
        motivacion.getStyleClass().add("levelup-detalle");
        motivacion.setWrapText(true);
        Label xp = new Label(info.xpTotal() + " XP acumulados");
        xp.getStyleClass().add("levelup-xp");

        Button seguir = new Button("¡A por más!");
        seguir.getStyleClass().addAll("boton-acento", "levelup-boton");
        seguir.setDefaultButton(true);

        VBox tarjeta = new VBox(10, etiqueta, escenario, rango, motivacion, xp, seguir);
        tarjeta.getStyleClass().add("levelup-tarjeta");
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setMaxSize(440, Region.USE_PREF_SIZE);

        destino.getChildren().addAll(velo, tarjeta);
        StackPane.setAlignment(tarjeta, Pos.CENTER);
        if (capaConfeti != null) {
            capaConfeti.toFront();
        }

        // Fondo desenfocado (Fluent "acrilico")
        List<BoxBlur> desenfoques = new ArrayList<>();
        List<Effect> previos = new ArrayList<>();
        if (aDesenfocar != null) {
            for (Node nodo : aDesenfocar) {
                BoxBlur blur = new BoxBlur(0, 0, 3);
                previos.add(nodo.getEffect());
                nodo.setEffect(blur);
                desenfoques.add(blur);
            }
        }
        Timeline enfoque = new Timeline();
        for (BoxBlur blur : desenfoques) {
            enfoque.getKeyFrames().add(new KeyFrame(Duration.millis(350),
                    new KeyValue(blur.widthProperty(), 12, Interpolator.EASE_OUT),
                    new KeyValue(blur.heightProperty(), 12, Interpolator.EASE_OUT)));
        }
        enfoque.play();

        // Brillo dorado que respira en el escudo
        DropShadow resplandor = new DropShadow(26, ORO);
        resplandor.setSpread(0.25);
        medalla.setEffect(resplandor);
        Timeline pulso = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(resplandor.radiusProperty(), 14, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900), new KeyValue(resplandor.radiusProperty(), 44, Interpolator.EASE_BOTH)));
        pulso.setAutoReverse(true);
        pulso.setCycleCount(Animation.INDEFINITE);
        pulso.play();

        // Entrada con rebote
        velo.setOpacity(0);
        tarjeta.setOpacity(0);
        tarjeta.setScaleX(0.55);
        tarjeta.setScaleY(0.55);
        FadeTransition apareceVelo = new FadeTransition(Duration.millis(220), velo);
        apareceVelo.setToValue(1);
        FadeTransition apareceTarjeta = new FadeTransition(Duration.millis(220), tarjeta);
        apareceTarjeta.setToValue(1);
        ScaleTransition salto = new ScaleTransition(Duration.millis(520), tarjeta);
        salto.setToX(1);
        salto.setToY(1);
        salto.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                double c = 1.9;
                double x = t - 1;
                return 1 + (c + 1) * x * x * x + c * x * x;
            }
        });
        new ParallelTransition(apareceVelo, apareceTarjeta, salto).play();

        expandir(anillo, 0);
        expandir(anilloDos, 650);
        SonidoService.reproducir(Efecto.NIVEL);
        PauseTransition retardo = new PauseTransition(Duration.millis(250));
        retardo.setOnFinished(e -> confeti(capaConfeti != null ? capaConfeti : capaConfetiGlobal));
        retardo.play();

        seguir.setOnAction(e -> {
            pulso.stop();
            ParallelTransition cierre = new ParallelTransition(fade(tarjeta, 200), fade(velo, 200));
            cierre.setOnFinished(ev -> {
                destino.getChildren().removeAll(velo, tarjeta);
                if (aDesenfocar != null) {
                    for (int i = 0; i < aDesenfocar.size(); i++) {
                        aDesenfocar.get(i).setEffect(previos.get(i));
                    }
                }
                if (alCerrar != null) {
                    alCerrar.run();
                }
            });
            cierre.play();
        });
        seguir.requestFocus();
    }

    private static Circle anillo() {
        Circle c = new Circle(66);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(ORO);
        c.setStrokeWidth(3);
        c.setOpacity(0);
        c.setMouseTransparent(true);
        return c;
    }

    /** Onda expansiva que nace en el escudo y se desvanece al crecer. */
    private static void expandir(Circle anillo, int retrasoMs) {
        ScaleTransition crece = new ScaleTransition(Duration.millis(1500), anillo);
        crece.setFromX(1);
        crece.setFromY(1);
        crece.setToX(2.4);
        crece.setToY(2.4);
        FadeTransition desvanece = new FadeTransition(Duration.millis(1500), anillo);
        desvanece.setFromValue(0.9);
        desvanece.setToValue(0);
        ParallelTransition onda = new ParallelTransition(crece, desvanece);
        onda.setDelay(Duration.millis(retrasoMs));
        onda.setCycleCount(3);
        onda.play();
    }

    /** Titulo del rango (conveniencia para otras vistas). */
    public static String rango(int nivel) {
        return "Nivel " + nivel + ": " + GamificacionService.titulo(nivel);
    }
}
