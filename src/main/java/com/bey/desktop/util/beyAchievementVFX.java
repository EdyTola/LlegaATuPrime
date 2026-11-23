package com.bey.desktop.util;

import com.bey.desktop.service.beyGamificacionService;
import com.bey.desktop.service.beyGamificacionService.beyInfoNivel;
import com.bey.desktop.service.beySonidoService;
import com.bey.desktop.service.beySonidoService.beyEfecto;
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

public final class beyAchievementVFX {
    private static final Color ORO = Color.web("#8A6D3B");
    private static final Color[] PALETA = {
            Color.web("#0F6CBD"), Color.web("#4A93D6"), Color.web("#475569"), Color.web("#94A3B8"),
            Color.web("#B08D57"), Color.web("#6E7691"), Color.web("#4F8A74"), Color.web("#3F7FA6")};

    private static final String[] MOTIVACION = {
            "La constancia de hoy es el rendimiento de mañana.",
            "Cada repetición te acerca a tu mejor versión.",
            "Disciplina por encima de motivación: ya lo estás demostrando.",
            "Los hábitos pequeños construyen resultados enormes.",
            "No pares ahora: el siguiente nivel ya te espera."};

    private static StackPane beyCapaGlobal;
    private static Pane beyCapaConfetiGlobal;

    private beyAchievementVFX() {
    }

    public static void beyRegistrarCapas(StackPane capa, Pane capaConfeti) {
        beyCapaGlobal = capa;
        beyCapaConfetiGlobal = capaConfeti;
    }

    public static void beyCelebrarMeta(Node tarjeta, String texto) {
        beyCelebrarMeta(tarjeta, beyCapaGlobal, texto);
    }

    public static void beyCelebrarMeta(Node tarjeta, Pane capaTextos, String texto) {
        beyPulsoGlow(tarjeta);
        beyTextoFlotante(tarjeta, capaTextos, texto);
        beySonidoService.beyReproducir(beyEfecto.EXITO);
    }

    public static void beyPulsoGlow(Node nodo) {
        if (nodo == null) {
            return;
        }
        Effect previo = nodo.getEffect();
        DropShadow brillo = new DropShadow(0, beyColorAcento(nodo));
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

    public static void beyTextoFlotante(Node origen, String texto) {
        beyTextoFlotante(origen, beyCapaGlobal, texto);
    }

    public static void beyTextoFlotante(Node origen, Pane capa, String texto) {
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
                new PauseTransition(Duration.millis(900)), beyFade(etiqueta, 600));
        ParallelTransition todo = new ParallelTransition(entra, aparece, sube, desvanece);
        todo.setOnFinished(e -> capa.getChildren().remove(etiqueta));
        todo.play();
    }

    private static FadeTransition beyFade(Node nodo, int ms) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), nodo);
        f.setToValue(0);
        return f;
    }

    private static Color beyColorAcento(Node nodo) {
        for (Parent p = nodo instanceof Parent padre ? padre : nodo.getParent(); p != null; p = p.getParent()) {
            if (p.getStyleClass().contains("tema-oscuro")) {
                return Color.web("#3D8AD0");
            }
        }
        if (nodo.getScene() != null && nodo.getScene().getRoot().getStyleClass().contains("tema-oscuro")) {
            return Color.web("#3D8AD0");
        }
        return Color.web("#0F6CBD");
    }

    public static void beyConfeti() {
        beyConfeti(beyCapaConfetiGlobal);
    }

    public static void beyConfeti(Pane capa) {
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
        List<beyParticula> particulas = new ArrayList<>();
        for (int i = 0; i < 190; i++) {
            particulas.add(new beyParticula(azar, ancho));
        }

        final double duracion = 3.0;
        AnimationTimer reloj = new AnimationTimer() {
            private long beyInicio;
            private long beyAnterior;

            @Override
            public void handle(long ahora) {
                if (beyInicio == 0) {
                    beyInicio = ahora;
                    beyAnterior = ahora;
                    return;
                }
                double t = (ahora - beyInicio) / 1e9;
                double dt = (ahora - beyAnterior) / 1e9;
                beyAnterior = ahora;
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
                for (beyParticula p : particulas) {
                    p.beyAvanzar(t, dt);
                    p.beyDibujar(gc, t, desvanecer);
                }
            }
        };
        reloj.start();
    }

    private static final class beyParticula {
        private final double beyRetraso;
        private final double beyVelocidadY;
        private final double beyVelocidadX;
        private final double beyBalanceo;
        private final double beyFrecuencia;
        private final double beyFase;
        private final double beyGiro;
        private final double beyTamano;
        private final int beyForma;
        private final Color beyColor;
        private double beyX;
        private double beyY;
        private double beyAngulo;

        beyParticula(Random azar, double ancho) {
            beyRetraso = azar.nextDouble() * 0.9;
            beyX = azar.nextDouble() * ancho;
            beyY = -20 - azar.nextDouble() * 60;
            beyVelocidadY = 150 + azar.nextDouble() * 210;
            beyVelocidadX = -35 + azar.nextDouble() * 70;
            beyBalanceo = 12 + azar.nextDouble() * 34;
            beyFrecuencia = 2 + azar.nextDouble() * 3;
            beyFase = azar.nextDouble() * Math.PI * 2;
            beyGiro = -320 + azar.nextDouble() * 640;
            beyTamano = 5 + azar.nextDouble() * 6;
            beyForma = azar.nextInt(3);
            beyColor = PALETA[azar.nextInt(PALETA.length)];
        }

        void beyAvanzar(double t, double dt) {
            if (t < beyRetraso) {
                return;
            }
            beyY += beyVelocidadY * dt;
            beyX += (beyVelocidadX + Math.cos(t * beyFrecuencia + beyFase) * beyBalanceo) * dt;
            beyAngulo += beyGiro * dt;
        }

        void beyDibujar(GraphicsContext gc, double t, double desvanecer) {
            if (t < beyRetraso) {
                return;
            }
            gc.save();
            gc.setGlobalAlpha(desvanecer);
            gc.translate(beyX, beyY);
            gc.rotate(beyAngulo);
            gc.setFill(beyColor);

            double volteo = 0.35 + 0.65 * Math.abs(Math.cos(t * beyFrecuencia * 1.7 + beyFase));
            switch (beyForma) {
                case 0 -> gc.fillRect(-beyTamano / 2, -beyTamano * volteo, beyTamano, beyTamano * 2 * volteo);
                case 1 -> gc.fillOval(-beyTamano / 2, -beyTamano / 2 * volteo, beyTamano, beyTamano * volteo);
                default -> gc.fillRect(-beyTamano * 0.18, -beyTamano * 1.8, beyTamano * 0.36, beyTamano * 3.6 * volteo);
            }
            gc.restore();
        }
    }

    public static void beyBannerLogro(String titulo, String detalle) {
        beyToastLogro(beyCapaGlobal, titulo, detalle);
    }

    private static final double TOAST_ANCHO_MAX = 340;
    private static final double TOAST_ALTO_MAX = 68;
    private static final double TOAST_MARGEN = 24;

    public static void beyToastLogro(StackPane capa, String titulo, String detalle) {
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
        if (beyCapaConfetiGlobal != null && capa == beyCapaGlobal) {
            beyCapaConfetiGlobal.toFront();
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

    public static void beyAvisoXp(VBox contenedor, int xp, String motivo) {
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
                new PauseTransition(Duration.millis(2800)), beyFade(aviso, 500));
        secuencia.setOnFinished(e -> contenedor.getChildren().remove(aviso));
        secuencia.play();
    }

    public static void beyNivelSubido(StackPane capa, Pane capaConfeti, beyInfoNivel info,
                                   List<? extends Node> aDesenfocar, Runnable alCerrar) {
        StackPane destino = capa != null ? capa : beyCapaGlobal;
        if (destino == null) {
            return;
        }
        Region velo = new Region();
        velo.getStyleClass().add("levelup-velo");

        Label etiqueta = new Label("¡HAS SUBIDO DE NIVEL!");
        etiqueta.getStyleClass().add("levelup-etiqueta");

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

        Circle anillo = beyAnillo();
        Circle anilloDos = beyAnillo();
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

        DropShadow resplandor = new DropShadow(26, ORO);
        resplandor.setSpread(0.25);
        medalla.setEffect(resplandor);
        Timeline pulso = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(resplandor.radiusProperty(), 14, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900), new KeyValue(resplandor.radiusProperty(), 44, Interpolator.EASE_BOTH)));
        pulso.setAutoReverse(true);
        pulso.setCycleCount(Animation.INDEFINITE);
        pulso.play();

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

        beyExpandir(anillo, 0);
        beyExpandir(anilloDos, 650);
        beySonidoService.beyReproducir(beyEfecto.NIVEL);
        PauseTransition retardo = new PauseTransition(Duration.millis(250));
        retardo.setOnFinished(e -> beyConfeti(capaConfeti != null ? capaConfeti : beyCapaConfetiGlobal));
        retardo.play();

        seguir.setOnAction(e -> {
            pulso.stop();
            ParallelTransition cierre = new ParallelTransition(beyFade(tarjeta, 200), beyFade(velo, 200));
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

    private static Circle beyAnillo() {
        Circle c = new Circle(66);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(ORO);
        c.setStrokeWidth(3);
        c.setOpacity(0);
        c.setMouseTransparent(true);
        return c;
    }

    private static void beyExpandir(Circle anillo, int retrasoMs) {
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

    public static String beyRango(int nivel) {
        return "Nivel " + nivel + ": " + beyGamificacionService.beyTitulo(nivel);
    }
}
