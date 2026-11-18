package com.symmetry.desktop.util;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.PatronMovimiento;
import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderizador 2D animado de la biomecanica de un ejercicio. Dibuja un modelo humano
 * estilizado (esqueleto articulado) sobre un {@link Canvas}: las articulaciones se calculan
 * con cinematica (IK de dos segmentos para rodillas y codos) a partir del progreso del
 * movimiento, que oscila en bucle con fase concentrica, pausa y fase excentrica. El musculo
 * principal se resalta con el color de acento y pulsa con la intensidad del esfuerzo.
 *
 * <p>No usa recursos externos: funciona siempre sin conexion y respeta el tema claro/oscuro
 * (consulta la clase {@code tema-oscuro} de los ancestros en cada fotograma).</p>
 */
public final class ExerciseAnimator2D extends StackPane {

    // --- Proporciones del cuerpo (alto total = 1.0 unidad de mundo) ---
    private static final double TORSO = 0.30;
    private static final double MUSLO = 0.24;
    private static final double PANTORRILLA = 0.24;
    private static final double BRAZO = 0.17;
    private static final double ANTEBRAZO = 0.15;
    private static final double PIE = 0.09;
    private static final double RADIO_CABEZA = 0.055;

    /** Reparto del ciclo: subida 42 %, pausa 8 %, bajada 50 %. */
    private static final double FIN_SUBIDA = 0.42;
    private static final double FIN_PAUSA = 0.50;

    private final Canvas lienzo = new Canvas();
    private final AnimationTimer reloj;

    private PatronMovimiento patron;
    private GrupoMuscular grupo;
    private boolean pausado;
    private double velocidad = 1.0;
    private double tiempo;
    private long ultimoNanos;

    public ExerciseAnimator2D(PatronMovimiento patron, GrupoMuscular grupo) {
        this.patron = patron;
        this.grupo = grupo;
        getStyleClass().add("animador-2d");
        setMinSize(360, 280);
        setPrefSize(560, 380);
        getChildren().add(lienzo);
        lienzo.widthProperty().bind(widthProperty());
        lienzo.heightProperty().bind(heightProperty());
        lienzo.widthProperty().addListener((obs, a, b) -> dibujar());
        lienzo.heightProperty().addListener((obs, a, b) -> dibujar());

        reloj = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (ultimoNanos != 0 && !pausado) {
                    tiempo += (ahora - ultimoNanos) / 1_000_000_000.0 * velocidad;
                }
                ultimoNanos = ahora;
                dibujar();
            }
        };
        // El reloj solo corre mientras el nodo esta en una escena: no deja procesos huerfanos.
        sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena == null) {
                detener();
            } else {
                reproducir();
            }
        });
    }

    // ------------------------------------------------------------------
    // Control publico
    // ------------------------------------------------------------------

    public void reproducir() {
        ultimoNanos = 0;
        reloj.start();
    }

    public void detener() {
        reloj.stop();
        ultimoNanos = 0;
    }

    public void setPausado(boolean pausado) {
        this.pausado = pausado;
        dibujar();
    }

    public boolean isPausado() {
        return pausado;
    }

    /** Velocidad de reproduccion: 1.0 normal, 0.5 camara lenta. */
    public void setVelocidad(double velocidad) {
        this.velocidad = Math.max(0.1, velocidad);
    }

    public void setPatron(PatronMovimiento patron, GrupoMuscular grupo) {
        this.patron = patron;
        this.grupo = grupo;
        tiempo = 0;
        dibujar();
    }

    // ------------------------------------------------------------------
    // Ciclo del movimiento
    // ------------------------------------------------------------------

    private enum Fase { CONCENTRICA, PAUSA, EXCENTRICA, ISOMETRICA }

    /** Estado del ciclo en un instante: progreso 0..1 del movimiento y fase actual. */
    private record Ciclo(double progreso, Fase fase) {
    }

    private Ciclo ciclo() {
        double u = (tiempo / patron.getPeriodoSegundos()) % 1.0;
        if (patron.isIsometrico()) {
            return new Ciclo(0.5 + 0.5 * Math.sin(u * 2 * Math.PI), Fase.ISOMETRICA);
        }
        double p;
        boolean subiendo;
        boolean pausa = false;
        if (u < FIN_SUBIDA) {
            p = suavizar(u / FIN_SUBIDA);
            subiendo = true;
        } else if (u < FIN_PAUSA) {
            p = 1;
            subiendo = true;
            pausa = true;
        } else {
            p = 1 - suavizar((u - FIN_PAUSA) / (1 - FIN_PAUSA));
            subiendo = false;
        }
        Fase fase;
        if (pausa) {
            fase = Fase.PAUSA;
        } else {
            fase = subiendo == patron.isConcentricaAlSubir() ? Fase.CONCENTRICA : Fase.EXCENTRICA;
        }
        return new Ciclo(p, fase);
    }

    private static double suavizar(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Intensidad de contraccion 0..1 usada para pulsar el resaltado muscular. */
    private double esfuerzo(Ciclo c) {
        if (c.fase() == Fase.ISOMETRICA) {
            return 0.85;
        }
        return patron.isConcentricaAlSubir() ? c.progreso() : 1 - c.progreso();
    }

    // ------------------------------------------------------------------
    // Geometria
    // ------------------------------------------------------------------

    private record Pt(double x, double y) {

        Pt mas(double dx, double dy) {
            return new Pt(x + dx, y + dy);
        }

        static Pt lerp(Pt a, Pt b, double t) {
            return new Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        }
    }

    private enum TipoEquipo { DISCO, BANCO, CABLE, POLEA, BARRA }

    private record Equipo(TipoEquipo tipo, Pt a, Pt b, double radio) {
    }

    /** Postura completa del modelo en un instante (el eje Y crece hacia abajo; el suelo es y = 0). */
    private static final class Pose {
        Pt cabeza;
        Pt cuello;
        Pt cadera;
        Pt rodilla;
        Pt tobillo;
        Pt punta;
        Pt codo;
        Pt muneca;
        Pt rodilla2;
        Pt tobillo2;
        Pt punta2;
        final List<Equipo> equipo = new ArrayList<>();
    }

    /** Punto a {@code largo} de {@code o}, con el angulo medido desde la vertical hacia arriba (+ = adelante). */
    private static Pt arriba(Pt o, double largo, double grados) {
        double r = Math.toRadians(grados);
        return new Pt(o.x + largo * Math.sin(r), o.y - largo * Math.cos(r));
    }

    /** Punto a {@code largo} de {@code o}, con el angulo medido desde la horizontal (+ = hacia arriba). */
    private static Pt horizontal(Pt o, double largo, double grados) {
        double r = Math.toRadians(grados);
        return new Pt(o.x + largo * Math.cos(r), o.y - largo * Math.sin(r));
    }

    /** Limita un objetivo al alcance maximo de una extremidad de dos segmentos. */
    private static Pt alcance(Pt origen, Pt objetivo, double maximo) {
        double dx = objetivo.x - origen.x;
        double dy = objetivo.y - origen.y;
        double d = Math.hypot(dx, dy);
        double tope = maximo - 0.0005;
        if (d <= tope || d == 0) {
            return objetivo;
        }
        return new Pt(origen.x + dx / d * tope, origen.y + dy / d * tope);
    }

    /** Articulacion intermedia (rodilla o codo) de una cadena de dos segmentos hacia un objetivo. */
    private static Pt articulacion(Pt a, Pt b, double l1, double l2, int signo) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double d = Math.max(Math.abs(l1 - l2) + 0.001, Math.min(Math.hypot(dx, dy), l1 + l2 - 0.0005));
        double base = Math.atan2(dy, dx);
        double coseno = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double ang = Math.acos(Math.max(-1, Math.min(1, coseno)));
        double th = base + signo * ang;
        return new Pt(a.x + l1 * Math.cos(th), a.y + l1 * Math.sin(th));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Signo de flexion: hacia donde apuntan la rodilla / el codo respecto a la linea del segmento. */
    private static final int ADELANTE = -1;
    private static final int ATRAS = 1;

    /**
     * Completa una postura de pie o sentada: cuello por la inclinacion del torso, cabeza, pierna
     * (IK cadera-tobillo) y brazo (IK hombro-muneca).
     */
    private static Pose armar(Pt cadera, double inclinacionTorso, Pt tobillo, Pt objetivoMuneca,
                              int signoRodilla, int signoCodo) {
        Pose p = new Pose();
        p.cadera = cadera;
        p.cuello = arriba(cadera, TORSO, inclinacionTorso);
        p.cabeza = arriba(p.cuello, 0.095, inclinacionTorso * 0.7);
        p.tobillo = alcance(cadera, tobillo, MUSLO + PANTORRILLA);
        p.rodilla = articulacion(cadera, p.tobillo, MUSLO, PANTORRILLA, signoRodilla);
        p.punta = new Pt(p.tobillo.x + PIE, 0);
        if (objetivoMuneca != null) {
            brazo(p, objetivoMuneca, signoCodo);
        }
        return p;
    }

    private static void brazo(Pose p, Pt objetivoMuneca, int signoCodo) {
        p.muneca = alcance(p.cuello, objetivoMuneca, BRAZO + ANTEBRAZO);
        p.codo = articulacion(p.cuello, p.muneca, BRAZO, ANTEBRAZO, signoCodo);
    }

    /** Postura de cuerpo recto apoyado en el suelo (flexion y plancha): pies atras, manos adelante. */
    private static Pose tablon(double alturaHombro, boolean antebrazos) {
        Pose p = new Pose();
        double largo = TORSO + MUSLO + PANTORRILLA;
        Pt tobillo = new Pt(-0.80, -0.03);
        double dx = Math.sqrt(largo * largo - Math.pow(alturaHombro - 0.03, 2));
        p.tobillo = tobillo;
        p.cuello = new Pt(tobillo.x + dx, -alturaHombro);
        p.cadera = Pt.lerp(tobillo, p.cuello, (MUSLO + PANTORRILLA) / largo);
        p.rodilla = Pt.lerp(tobillo, p.cadera, 0.5);
        p.punta = new Pt(tobillo.x - 0.04, 0);
        double vx = p.cuello.x - p.cadera.x;
        double vy = p.cuello.y - p.cadera.y;
        double n = Math.hypot(vx, vy);
        p.cabeza = new Pt(p.cuello.x + vx / n * 0.10, p.cuello.y + vy / n * 0.10);
        Pt mano = antebrazos ? new Pt(p.cuello.x + 0.14, 0) : new Pt(p.cuello.x + 0.01, 0);
        brazo(p, mano, ATRAS);
        return p;
    }

    private Pose pose(double t) {
        Pose p;
        switch (patron) {
            case SENTADILLA -> {
                Pt cadera = new Pt(lerp(0, -0.14, t), -lerp(0.47, 0.24, t));
                p = armar(cadera, lerp(3, 42, t), new Pt(0, 0), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(0.03, 0.03), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.cuello.mas(-0.01, -0.01), null, 0.10));
            }
            case PRESS_PLANO -> {
                p = new Pose();
                p.cadera = new Pt(0, -0.34);
                p.cuello = new Pt(-TORSO, -0.34);
                p.cabeza = new Pt(-TORSO - 0.095, -0.34);
                p.tobillo = new Pt(0.24, 0);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
                p.punta = new Pt(0.33, 0);
                brazo(p, new Pt(p.cuello.x + 0.02, p.cuello.y - lerp(0.12, 0.31, t)), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.55, -0.30), new Pt(0.05, -0.30), 0));
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.09));
            }
            case PRESS_INCLINADO -> {
                p = new Pose();
                p.cadera = new Pt(0, -0.34);
                p.cuello = arriba(p.cadera, TORSO, -50);
                p.cabeza = arriba(p.cuello, 0.095, -50);
                p.tobillo = new Pt(0.24, 0);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
                p.punta = new Pt(0.33, 0);
                double d = lerp(0.12, 0.31, t);
                brazo(p, p.cuello.mas(0.643 * d, -0.766 * d), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(0.10, -0.30), p.cuello.mas(-0.08, -0.05), 0));
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.09));
            }
            case FLEXION -> p = tablon(lerp(0.13, 0.30, t), false);
            case PLANCHA -> p = tablon(0.165 + 0.006 * (t - 0.5), true);
            case JALON -> {
                p = armar(new Pt(0, -0.26), -8, new Pt(0.22, 0), null, ADELANTE, ADELANTE);
                brazo(p, p.cuello.mas(lerp(0.14, 0.07, t), lerp(-0.31, 0.10, t)), ADELANTE);
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.12, -0.22), new Pt(0.10, -0.22), 0));
                p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, new Pt(p.muneca.x - 0.01, -1.20), 0));
                p.equipo.add(new Equipo(TipoEquipo.POLEA, new Pt(p.muneca.x - 0.01, -1.20), null, 0.035));
                p.equipo.add(new Equipo(TipoEquipo.BARRA, p.muneca.mas(0, 0), null, 0.016));
            }
            case REMO -> {
                p = armar(new Pt(-0.12, -0.42), 62, new Pt(0, 0), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(lerp(0.0, -0.12, t), lerp(0.31, 0.12, t)), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.10));
            }
            case DOMINADA -> {
                Pt barra = new Pt(0.05, -1.30);
                p = new Pose();
                p.cuello = new Pt(barra.x - 0.04, barra.y + lerp(0.31, 0.12, t));
                p.cadera = p.cuello.mas(0.0, TORSO);
                p.cabeza = p.cuello.mas(0.02, -0.095);
                p.tobillo = alcance(p.cadera, p.cadera.mas(-0.08, 0.47), MUSLO + PANTORRILLA);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ATRAS);
                p.punta = p.tobillo.mas(0.0, 0.07);
                brazo(p, barra, ADELANTE);
                p.equipo.add(new Equipo(TipoEquipo.BARRA, barra, null, 0.02));
            }
            case CURL_BICEPS -> {
                p = armar(new Pt(0, -0.47), lerp(0, -4, t), new Pt(0, 0), null, ADELANTE, ATRAS);
                p.codo = p.cuello.mas(0.02, BRAZO);
                double psi = Math.toRadians(lerp(8, 150, t));
                p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.05));
            }
            case EXTENSION_TRICEPS -> {
                p = armar(new Pt(0, -0.47), 0, new Pt(0, 0), null, ADELANTE, ATRAS);
                p.codo = p.cuello.mas(0.03, BRAZO);
                double psi = Math.toRadians(lerp(105, 4, t));
                p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
                p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, new Pt(0.07, -1.20), 0));
                p.equipo.add(new Equipo(TipoEquipo.POLEA, new Pt(0.07, -1.20), null, 0.035));
            }
            case PESO_MUERTO -> {
                Pt cadera = new Pt(lerp(-0.20, -0.01, t), -lerp(0.40, 0.47, t));
                p = armar(cadera, lerp(58, 3, t), new Pt(0, 0), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(0.0, 0.31), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.12));
            }
            case HIP_THRUST -> {
                p = new Pose();
                p.cuello = new Pt(-0.30, -0.40);
                double phi = Math.toRadians(lerp(35, -8, t));
                p.cadera = new Pt(p.cuello.x + TORSO * Math.cos(phi), p.cuello.y + TORSO * Math.sin(phi));
                p.cabeza = p.cuello.mas(-0.09, -0.03);
                p.tobillo = new Pt(0.22, 0);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
                p.punta = new Pt(0.31, 0);
                brazo(p, p.cadera.mas(0.02, -0.04), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.55, -0.37), new Pt(-0.18, -0.37), 0));
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.40, -0.37), new Pt(-0.40, 0), 0));
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.cadera.mas(0.0, -0.07), null, 0.09));
            }
            case PUENTE_GLUTEOS -> {
                p = new Pose();
                p.cuello = new Pt(-0.32, -0.05);
                double phi = Math.toRadians(lerp(12, -30, t));
                p.cadera = new Pt(p.cuello.x + TORSO * Math.cos(phi), p.cuello.y + TORSO * Math.sin(phi));
                p.cabeza = p.cuello.mas(-0.095, 0.0);
                p.tobillo = new Pt(0.18, 0);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
                p.punta = new Pt(0.27, 0);
                brazo(p, new Pt(p.cuello.x + 0.32, -0.015), ATRAS);
            }
            case ELEVACION_LATERAL -> p = pose(0.0, true);
            case PRESS_HOMBRO -> {
                p = armar(new Pt(0, -0.47), lerp(0, -3, t), new Pt(0, 0), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(0.01, -lerp(0.08, 0.31, t)), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca, null, 0.08));
            }
            case ZANCADA -> {
                Pt cadera = new Pt(-0.03, -lerp(0.45, 0.27, t));
                p = armar(cadera, 2, new Pt(0.24, 0), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(0.05, 0.25), ATRAS);
                p.tobillo2 = alcance(cadera, new Pt(-0.30, -0.07), MUSLO + PANTORRILLA);
                p.rodilla2 = articulacion(cadera, p.tobillo2, MUSLO, PANTORRILLA, ADELANTE);
                p.punta2 = new Pt(p.tobillo2.x + 0.0, 0);
            }
            case ELEVACION_TALONES -> {
                double alza = t * 0.07;
                p = armar(new Pt(0, -0.47 - alza), 0, new Pt(0, -alza), null, ADELANTE, ATRAS);
                brazo(p, p.cuello.mas(0.02, 0.30), ATRAS);
                p.punta = new Pt(p.tobillo.x + PIE, 0);
            }
            case CURL_FEMORAL -> {
                p = new Pose();
                p.cadera = new Pt(0, -0.33);
                p.cuello = new Pt(-TORSO, -0.33);
                p.cabeza = new Pt(-TORSO - 0.095, -0.33);
                p.rodilla = new Pt(0.24, -0.33);
                double phi = Math.toRadians(lerp(4, 100, t));
                p.tobillo = new Pt(p.rodilla.x + PANTORRILLA * Math.cos(phi), p.rodilla.y - PANTORRILLA * Math.sin(phi));
                p.punta = new Pt(p.tobillo.x - Math.sin(phi) * 0.07, p.tobillo.y - Math.cos(phi) * 0.07);
                brazo(p, new Pt(p.cuello.x - 0.05, -0.12), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.45, -0.28), new Pt(0.35, -0.28), 0));
                p.equipo.add(new Equipo(TipoEquipo.DISCO, p.tobillo, null, 0.045));
            }
            case CRUNCH -> {
                p = new Pose();
                p.cadera = new Pt(0.05, -0.04);
                double phi = Math.toRadians(lerp(3, 40, t));
                p.cuello = new Pt(p.cadera.x - TORSO * Math.cos(phi), p.cadera.y - TORSO * Math.sin(phi));
                double phiCabeza = Math.toRadians(lerp(3, 40, t) + 10);
                p.cabeza = new Pt(p.cuello.x - 0.095 * Math.cos(phiCabeza), p.cuello.y - 0.095 * Math.sin(phiCabeza));
                p.tobillo = new Pt(0.32, 0);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
                p.punta = new Pt(0.41, 0);
                brazo(p, p.cabeza.mas(0.03, -0.02), ATRAS);
            }
            case ELEVACION_PIERNAS -> {
                p = new Pose();
                p.cadera = new Pt(0.05, -0.04);
                p.cuello = new Pt(p.cadera.x - TORSO, -0.04);
                p.cabeza = new Pt(p.cuello.x - 0.095, -0.04);
                double phi = Math.toRadians(lerp(8, 85, t));
                double largo = MUSLO + PANTORRILLA - 0.002;
                p.tobillo = new Pt(p.cadera.x + largo * Math.cos(phi), p.cadera.y - largo * Math.sin(phi));
                p.rodilla = Pt.lerp(p.cadera, p.tobillo, MUSLO / (MUSLO + PANTORRILLA));
                p.punta = new Pt(p.tobillo.x - Math.sin(phi) * 0.08, p.tobillo.y - Math.cos(phi) * 0.08);
                brazo(p, new Pt(p.cadera.x - 0.12, -0.015), ATRAS);
            }
            case FONDOS -> {
                Pt agarre = new Pt(0.0, -0.95);
                p = new Pose();
                p.cuello = new Pt(agarre.x - 0.03, agarre.y - lerp(0.02, 0.29, t));
                p.cadera = p.cuello.mas(-0.03, TORSO);
                p.cabeza = p.cuello.mas(0.025, -0.095);
                p.tobillo = alcance(p.cadera, p.cadera.mas(-0.13, 0.40), MUSLO + PANTORRILLA);
                p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ATRAS);
                p.punta = p.tobillo.mas(0.0, 0.07);
                brazo(p, agarre, ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.BARRA, agarre, null, 0.02));
                p.equipo.add(new Equipo(TipoEquipo.BANCO, agarre.mas(-0.10, 0), agarre.mas(0.12, 0), 0));
            }
            case SUPERMAN -> {
                p = new Pose();
                p.cadera = new Pt(0.02, -0.06);
                double a = Math.toRadians(lerp(0, 14, t));
                p.cuello = new Pt(p.cadera.x - TORSO * Math.cos(a), p.cadera.y - TORSO * Math.sin(a));
                p.cabeza = new Pt(p.cuello.x - 0.095 * Math.cos(a + 0.1), p.cuello.y - 0.095 * Math.sin(a + 0.1));
                double b = Math.toRadians(lerp(2, 20, t));
                double c = Math.toRadians(lerp(0, 14, t));
                double largoPierna = MUSLO + PANTORRILLA - 0.002;
                p.tobillo = new Pt(p.cadera.x + largoPierna * Math.cos(c), p.cadera.y - largoPierna * Math.sin(c));
                p.rodilla = Pt.lerp(p.cadera, p.tobillo, MUSLO / (MUSLO + PANTORRILLA));
                p.punta = new Pt(p.tobillo.x + 0.05, p.tobillo.y - 0.01);
                p.muneca = new Pt(p.cuello.x - (BRAZO + ANTEBRAZO - 0.002) * Math.cos(b),
                        p.cuello.y - (BRAZO + ANTEBRAZO - 0.002) * Math.sin(b));
                p.codo = Pt.lerp(p.cuello, p.muneca, BRAZO / (BRAZO + ANTEBRAZO));
            }
            default -> p = armar(new Pt(0, -0.47), 0, new Pt(0, 0), new Pt(0.02, -0.2), ADELANTE, ATRAS);
        }
        return p;
    }

    /** Marcador para la vista frontal (la elevacion lateral usa otro dibujo). */
    private Pose pose(double t, boolean frontal) {
        return new Pose();
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    private record Paleta(Color fondo, Color suelo, Color hueso, Color hueso2, Color articulacion,
                          Color acento, Color equipo, Color texto, Color excentrica) {
    }

    private Paleta paleta() {
        boolean oscuro = false;
        for (Parent padre = getParent(); padre != null; padre = padre.getParent()) {
            if (padre.getStyleClass().contains("tema-oscuro")) {
                oscuro = true;
                break;
            }
        }
        if (getScene() != null && getScene().getRoot() != null
                && getScene().getRoot().getStyleClass().contains("tema-oscuro")) {
            oscuro = true;
        }
        return oscuro
                ? new Paleta(Color.web("#161B22"), Color.web("#3B434F"), Color.web("#DDE3EC"),
                Color.web("#7C8594"), Color.web("#161B22"), Color.web("#3AA0FF"), Color.web("#8A93A3"),
                Color.web("#E6EAF0"), Color.web("#F2A93B"))
                : new Paleta(Color.web("#F4F7FB"), Color.web("#C5CCD7"), Color.web("#2F3A4A"),
                Color.web("#98A2B3"), Color.WHITE, Color.web("#0078D4"), Color.web("#7B8494"),
                Color.web("#1F2937"), Color.web("#C2710C"));
    }

    private void dibujar() {
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        if (w < 10 || h < 10 || patron == null) {
            return;
        }
        GraphicsContext gc = lienzo.getGraphicsContext2D();
        Paleta c = paleta();
        gc.setFill(c.fondo());
        gc.fillRect(0, 0, w, h);

        double altoVista = switch (patron) {
            case DOMINADA -> 1.75;
            case FONDOS -> 1.5;
            case JALON, EXTENSION_TRICEPS, PRESS_HOMBRO -> 1.4;
            default -> 1.15;
        };
        double escala = Math.min(w / 2.0, h / altoVista);
        double ox = w / 2 + patron.getDesplazamientoX() * escala;
        double oy = h - 0.12 * escala;

        // Suelo
        gc.setStroke(c.suelo());
        gc.setLineWidth(Math.max(2, 0.012 * escala));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(ox - 0.95 * escala, oy, ox + 0.95 * escala, oy);

        Ciclo ciclo = ciclo();
        double esfuerzo = esfuerzo(ciclo);
        if (patron.isVistaFrontal()) {
            dibujarFrontal(gc, c, escala, ox, oy, ciclo.progreso(), esfuerzo);
        } else {
            dibujarLateral(gc, c, escala, ox, oy, pose(ciclo.progreso()), esfuerzo);
        }
        dibujarRotulos(gc, c, w, ciclo);
    }

    private double px(double escala, double ox, Pt p) {
        return ox + p.x() * escala;
    }

    private double py(double escala, double oy, Pt p) {
        return oy + p.y() * escala;
    }

    private void dibujarLateral(GraphicsContext gc, Paleta c, double s, double ox, double oy,
                                Pose p, double esfuerzo) {
        // Equipo de fondo (banco, cables, poleas)
        for (Equipo e : p.equipo) {
            if (e.tipo() == TipoEquipo.BANCO || e.tipo() == TipoEquipo.CABLE || e.tipo() == TipoEquipo.POLEA) {
                dibujarEquipo(gc, c, s, ox, oy, e);
            }
        }
        // Pierna lejana, mas tenue
        if (p.rodilla2 != null) {
            gc.setStroke(c.hueso2());
            gc.setLineWidth(Math.max(4, 0.034 * s));
            gc.setLineCap(StrokeLineCap.ROUND);
            linea(gc, s, ox, oy, p.cadera, p.rodilla2);
            linea(gc, s, ox, oy, p.rodilla2, p.tobillo2);
            linea(gc, s, ox, oy, p.tobillo2, p.punta2);
        }

        resaltarMusculo(gc, c, s, ox, oy, p, esfuerzo);

        gc.setStroke(c.hueso());
        gc.setLineWidth(Math.max(4, 0.036 * s));
        gc.setLineCap(StrokeLineCap.ROUND);
        linea(gc, s, ox, oy, p.cuello, p.cadera);
        linea(gc, s, ox, oy, p.cadera, p.rodilla);
        linea(gc, s, ox, oy, p.rodilla, p.tobillo);
        linea(gc, s, ox, oy, p.tobillo, p.punta);
        if (p.codo != null) {
            linea(gc, s, ox, oy, p.cuello, p.codo);
            linea(gc, s, ox, oy, p.codo, p.muneca);
        }

        // Articulaciones
        double r = Math.max(3.5, 0.022 * s);
        for (Pt j : new Pt[]{p.cuello, p.cadera, p.rodilla, p.tobillo, p.codo, p.muneca}) {
            if (j != null) {
                gc.setFill(c.articulacion());
                gc.setStroke(c.hueso());
                gc.setLineWidth(2);
                gc.fillOval(px(s, ox, j) - r, py(s, oy, j) - r, r * 2, r * 2);
                gc.strokeOval(px(s, ox, j) - r, py(s, oy, j) - r, r * 2, r * 2);
            }
        }
        // Cabeza
        double rc = RADIO_CABEZA * s;
        gc.setFill(c.articulacion());
        gc.setStroke(c.hueso());
        gc.setLineWidth(Math.max(3, 0.026 * s));
        gc.fillOval(px(s, ox, p.cabeza) - rc, py(s, oy, p.cabeza) - rc, rc * 2, rc * 2);
        gc.strokeOval(px(s, ox, p.cabeza) - rc, py(s, oy, p.cabeza) - rc, rc * 2, rc * 2);

        // Equipo en primer plano (discos, barras)
        for (Equipo e : p.equipo) {
            if (e.tipo() == TipoEquipo.DISCO || e.tipo() == TipoEquipo.BARRA) {
                dibujarEquipo(gc, c, s, ox, oy, e);
            }
        }
    }

    private void linea(GraphicsContext gc, double s, double ox, double oy, Pt a, Pt b) {
        gc.strokeLine(px(s, ox, a), py(s, oy, a), px(s, ox, b), py(s, oy, b));
    }

    private void dibujarEquipo(GraphicsContext gc, Paleta c, double s, double ox, double oy, Equipo e) {
        gc.setStroke(c.equipo());
        gc.setFill(c.equipo());
        switch (e.tipo()) {
            case BANCO -> {
                gc.setLineWidth(Math.max(5, 0.05 * s));
                gc.setLineCap(StrokeLineCap.ROUND);
                linea(gc, s, ox, oy, e.a(), e.b());
            }
            case CABLE -> {
                gc.setLineWidth(Math.max(1.5, 0.008 * s));
                linea(gc, s, ox, oy, e.a(), e.b());
            }
            case POLEA -> {
                double r = e.radio() * s;
                gc.setLineWidth(2.5);
                gc.strokeOval(px(s, ox, e.a()) - r, py(s, oy, e.a()) - r, r * 2, r * 2);
            }
            case DISCO -> {
                double r = e.radio() * s;
                gc.setGlobalAlpha(0.88);
                gc.fillOval(px(s, ox, e.a()) - r, py(s, oy, e.a()) - r, r * 2, r * 2);
                gc.setGlobalAlpha(1);
                gc.setFill(c.fondo());
                double r2 = r * 0.22;
                gc.fillOval(px(s, ox, e.a()) - r2, py(s, oy, e.a()) - r2, r2 * 2, r2 * 2);
            }
            case BARRA -> {
                double r = e.radio() * s;
                gc.fillOval(px(s, ox, e.a()) - r, py(s, oy, e.a()) - r, r * 2, r * 2);
            }
        }
    }

    /** Resalta el musculo principal con acento y un halo que pulsa segun el esfuerzo. */
    private void resaltarMusculo(GraphicsContext gc, Paleta c, double s, double ox, double oy,
                                 Pose p, double esfuerzo) {
        if (grupo == null) {
            return;
        }
        double vx = p.cuello.x() - p.cadera.x();
        double vy = p.cuello.y() - p.cadera.y();
        double n = Math.max(1e-6, Math.hypot(vx, vy));
        double nx = -vy / n;
        double ny = vx / n;
        double alfa = 0.40 + 0.60 * esfuerzo;

        Pt a;
        Pt b;
        double ancho;
        switch (grupo) {
            case PECHO -> {
                a = Pt.lerp(p.cuello, p.cadera, 0.10).mas(nx * 0.035, ny * 0.035);
                b = Pt.lerp(p.cuello, p.cadera, 0.38).mas(nx * 0.035, ny * 0.035);
                ancho = 0.09;
            }
            case ESPALDA -> {
                a = Pt.lerp(p.cuello, p.cadera, 0.08).mas(-nx * 0.03, -ny * 0.03);
                b = Pt.lerp(p.cuello, p.cadera, 0.72).mas(-nx * 0.03, -ny * 0.03);
                ancho = 0.085;
            }
            case HOMBROS -> {
                a = p.cuello;
                b = p.cuello;
                ancho = 0.12;
            }
            case BICEPS, TRICEPS -> {
                a = p.cuello;
                b = p.codo == null ? p.cuello : p.codo;
                ancho = 0.07;
            }
            case CUADRICEPS, ISQUIOTIBIALES -> {
                a = p.cadera;
                b = p.rodilla;
                ancho = 0.085;
            }
            case GLUTEOS -> {
                a = p.cadera;
                b = p.cadera;
                ancho = 0.14;
            }
            case PANTORRILLAS -> {
                a = p.rodilla;
                b = p.tobillo;
                ancho = 0.07;
            }
            default -> {
                a = Pt.lerp(p.cuello, p.cadera, 0.50).mas(nx * 0.03, ny * 0.03);
                b = Pt.lerp(p.cuello, p.cadera, 0.94).mas(nx * 0.03, ny * 0.03);
                ancho = 0.085;
            }
        }
        Color acento = c.acento();
        // Halo exterior y nucleo
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.color(acento.getRed(), acento.getGreen(), acento.getBlue(), 0.22 * alfa));
        gc.setLineWidth(ancho * 1.7 * s);
        linea(gc, s, ox, oy, a, b);
        gc.setStroke(Color.color(acento.getRed(), acento.getGreen(), acento.getBlue(), alfa));
        gc.setLineWidth(ancho * s);
        linea(gc, s, ox, oy, a, b);
    }

    /** Vista frontal para la elevacion lateral: ambos brazos se abren hasta la altura del hombro. */
    private void dibujarFrontal(GraphicsContext gc, Paleta c, double s, double ox, double oy,
                                double t, double esfuerzo) {
        double phi = Math.toRadians(lerp(4, 88, t));
        double phiAnte = Math.toRadians(lerp(4, 78, t));
        Pt cadera = new Pt(0, -0.47);
        Pt cuello = new Pt(0, -0.77);
        Pt cabeza = new Pt(0, -0.865);
        double alfa = 0.40 + 0.60 * esfuerzo;

        gc.setLineCap(StrokeLineCap.ROUND);
        for (int lado : new int[]{-1, 1}) {
            Pt hombro = new Pt(lado * 0.095, -0.75);
            Pt codo = hombro.mas(lado * Math.sin(phi) * BRAZO, Math.cos(phi) * BRAZO);
            Pt muneca = codo.mas(lado * Math.sin(phiAnte) * ANTEBRAZO, Math.cos(phiAnte) * ANTEBRAZO);
            // Deltoides resaltado
            Color acento = c.acento();
            gc.setStroke(Color.color(acento.getRed(), acento.getGreen(), acento.getBlue(), 0.22 * alfa));
            gc.setLineWidth(0.20 * s);
            linea(gc, s, ox, oy, hombro, hombro);
            gc.setStroke(Color.color(acento.getRed(), acento.getGreen(), acento.getBlue(), alfa));
            gc.setLineWidth(0.115 * s);
            linea(gc, s, ox, oy, hombro, hombro);

            gc.setStroke(c.hueso());
            gc.setLineWidth(Math.max(4, 0.036 * s));
            linea(gc, s, ox, oy, hombro, codo);
            linea(gc, s, ox, oy, codo, muneca);
            Pt cad = new Pt(lado * 0.05, -0.47);
            Pt tob = new Pt(lado * 0.09, 0);
            linea(gc, s, ox, oy, cad, tob);
            linea(gc, s, ox, oy, tob, new Pt(tob.x + lado * 0.05, 0));

            gc.setFill(c.equipo());
            double r = 0.04 * s;
            gc.fillOval(px(s, ox, muneca) - r, py(s, oy, muneca) - r, r * 2, r * 2);
        }
        gc.setStroke(c.hueso());
        gc.setLineWidth(Math.max(4, 0.036 * s));
        linea(gc, s, ox, oy, new Pt(-0.095, -0.75), new Pt(0.095, -0.75));
        linea(gc, s, ox, oy, cuello, cadera);
        linea(gc, s, ox, oy, new Pt(-0.05, -0.47), new Pt(0.05, -0.47));
        double rc = RADIO_CABEZA * s;
        gc.setFill(c.articulacion());
        gc.setLineWidth(Math.max(3, 0.026 * s));
        gc.fillOval(px(s, ox, cabeza) - rc, py(s, oy, cabeza) - rc, rc * 2, rc * 2);
        gc.strokeOval(px(s, ox, cabeza) - rc, py(s, oy, cabeza) - rc, rc * 2, rc * 2);
    }

    /** Rotulos del lienzo: fase actual y musculo principal. */
    private void dibujarRotulos(GraphicsContext gc, Paleta c, double w, Ciclo ciclo) {
        String fase;
        Color color;
        switch (ciclo.fase()) {
            case CONCENTRICA -> {
                fase = "Fase concéntrica · esfuerzo";
                color = c.acento();
            }
            case EXCENTRICA -> {
                fase = "Fase excéntrica · controla";
                color = c.excentrica();
            }
            case ISOMETRICA -> {
                fase = "Isométrico · mantén la posición";
                color = c.acento();
            }
            default -> {
                fase = "Pausa · contrae un segundo";
                color = c.texto();
            }
        }
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        gc.setFill(color);
        gc.fillText(fase, 16, 24);

        if (grupo != null) {
            gc.setFill(c.acento());
            gc.fillOval(16, 34, 10, 10);
            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            gc.setFill(c.texto());
            gc.fillText("Músculo principal: " + grupo.getEtiqueta(), 32, 43);
        }
        gc.setFill(c.hueso2());
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        gc.fillText(patron.getTitulo() + (pausado ? "  ·  en pausa" : "")
                + (velocidad < 1 ? "  ·  cámara lenta 0.5x" : ""), 16, lienzo.getHeight() - 10);
    }
}
