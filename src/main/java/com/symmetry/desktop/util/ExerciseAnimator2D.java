package com.symmetry.desktop.util;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.Implemento;
import com.symmetry.desktop.model.PatronMovimiento;
import com.symmetry.desktop.model.VarianteMovimiento;
import com.symmetry.desktop.service.GuiaTecnicaService;
import com.symmetry.desktop.util.Biomecanica2D.Pose;
import com.symmetry.desktop.util.MunecoRenderer2D.Paleta;
import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Animacion 2D de la biomecanica de un ejercicio sobre un {@link Canvas}. Combina tres capas:
 * {@link Biomecanica2D} (cinematica: postura y equipo por patron, implemento y variante),
 * {@link MunecoRenderer2D} (maniqui con volumen y resaltado muscular) y este nodo, que lleva el
 * reloj (fase concentrica, pausa y excentrica con suavizado), el encuadre y los rotulos.
 *
 * <p>No usa recursos externos: funciona siempre sin conexion y respeta el tema claro/oscuro
 * (consulta la clase {@code tema-oscuro} de los ancestros en cada fotograma). El reloj corre
 * con {@link AnimationTimer} (sincronizado al refresco de pantalla, 60 FPS) y avanza por
 * tiempo real, no por fotograma, por lo que la velocidad no depende del hardware.</p>
 */
public final class ExerciseAnimator2D extends StackPane {

    /** Reparto del ciclo: subida 42 %, pausa 8 %, bajada 50 %. */
    private static final double FIN_SUBIDA = 0.42;
    private static final double FIN_PAUSA = 0.50;

    private final Canvas lienzo = new Canvas();
    private final AnimationTimer reloj;

    private PatronMovimiento patron;
    private Implemento implemento = Implemento.NINGUNO;
    private VarianteMovimiento variante = VarianteMovimiento.ESTANDAR;
    private GrupoMuscular grupo;
    private boolean pausado;
    private double velocidad = 1.0;
    private double tiempo;
    private long ultimoNanos;

    public ExerciseAnimator2D(PatronMovimiento patron, GrupoMuscular grupo) {
        this(new GuiaTecnicaService.Perfil(patron, Implemento.NINGUNO, VarianteMovimiento.ESTANDAR), grupo);
    }

    public ExerciseAnimator2D(GuiaTecnicaService.Perfil perfil, GrupoMuscular grupo) {
        this.patron = perfil.patron();
        this.implemento = perfil.implemento();
        this.variante = perfil.variante();
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
                    // Se limita el salto de tiempo para que un bloqueo de la UI no provoque un tiron brusco.
                    double dt = Math.min(0.05, (ahora - ultimoNanos) / 1_000_000_000.0);
                    tiempo += dt * velocidad;
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
        setPerfil(new GuiaTecnicaService.Perfil(patron, Implemento.NINGUNO, VarianteMovimiento.ESTANDAR), grupo);
    }

    public void setPerfil(GuiaTecnicaService.Perfil perfil, GrupoMuscular grupo) {
        this.patron = perfil.patron();
        this.implemento = perfil.implemento();
        this.variante = perfil.variante();
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
        if (variante.esIsometrica()) {
            // Contraccion mantenida: temblor rapido (5 Hz) en la pose, sin recorrido.
            return new Ciclo(0.5 + 0.5 * Math.sin(tiempo * 2 * Math.PI * 5.0), Fase.ISOMETRICA);
        }
        if (patron.isIsometrico()) {
            return new Ciclo(0.5 + 0.5 * Math.sin(u * 2 * Math.PI), Fase.ISOMETRICA);
        }
        double p;
        boolean subiendo;
        boolean pausa = false;
        if (u < FIN_SUBIDA) {
            p = Biomecanica2D.suave(u / FIN_SUBIDA);
            subiendo = true;
        } else if (u < FIN_PAUSA) {
            p = 1;
            subiendo = true;
            pausa = true;
        } else {
            p = 1 - Biomecanica2D.suave((u - FIN_PAUSA) / (1 - FIN_PAUSA));
            subiendo = false;
        }
        Fase fase;
        if (pausa) {
            fase = Fase.PAUSA;
        } else {
            fase = subiendo == variante.concentricaAlSubir(patron) ? Fase.CONCENTRICA : Fase.EXCENTRICA;
        }
        return new Ciclo(p, fase);
    }

    /** Intensidad de contraccion 0..1 usada para pulsar el resaltado muscular. */
    private double esfuerzo(Ciclo c) {
        if (c.fase() == Fase.ISOMETRICA) {
            // Pulso lento del resaltado (~1.2 Hz): la tension se mantiene y respira.
            return variante.esIsometrica() ? 0.80 + 0.20 * Math.sin(tiempo * 2 * Math.PI * 1.2) : 0.85;
        }
        return variante.concentricaAlSubir(patron) ? c.progreso() : 1 - c.progreso();
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    private boolean temaOscuro() {
        for (Parent padre = getParent(); padre != null; padre = padre.getParent()) {
            if (padre.getStyleClass().contains("tema-oscuro")) {
                return true;
            }
        }
        return getScene() != null && getScene().getRoot() != null
                && getScene().getRoot().getStyleClass().contains("tema-oscuro");
    }

    /** Alto del mundo visible segun el equipo: los cables y barras altas necesitan mas encuadre. */
    private double altoVista() {
        return switch (patron) {
            case APERTURA_PECHO -> 1.3;
            case DOMINADA -> 1.75;
            case FONDOS -> variante == VarianteMovimiento.FONDOS_SILLA ? 1.15 : 1.65;
            case JALON -> 1.62;
            case EXTENSION_TRICEPS -> variante == VarianteMovimiento.TRICEPS_SOBRE_CABEZA ? 1.2 : 1.4;
            case CRUNCH -> variante == VarianteMovimiento.CRUNCH_POLEA ? 1.5 : 1.15;
            case PRESS_HOMBRO -> variante == VarianteMovimiento.FLEXION_PICA || variante == VarianteMovimiento.PICA_ELEVADA
                    ? 1.0 : (variante == VarianteMovimiento.PRESS_HOMBRO_SENTADO ? 1.3 : 1.15);
            case PRESS_PLANO, PRESS_INCLINADO -> variante == VarianteMovimiento.PRESS_SENTADO ? 1.15 : 1.0;
            default -> 1.15;
        };
    }

    private void dibujar() {
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        if (w < 10 || h < 10 || patron == null) {
            return;
        }
        GraphicsContext gc = lienzo.getGraphicsContext2D();
        Paleta c = Paleta.de(temaOscuro());
        gc.setFill(c.fondo());
        gc.fillRect(0, 0, w, h);

        double escala = Math.min(w / 2.0, h / altoVista());
        double ox = w / 2 + patron.getDesplazamientoX() * escala;
        double oy = h - 0.12 * escala;

        Ciclo ciclo = ciclo();
        double esfuerzo = esfuerzo(ciclo);
        MunecoRenderer2D muneco = new MunecoRenderer2D(gc, c, escala, ox, oy);
        if (patron == PatronMovimiento.APERTURA_PECHO) {
            muneco.dibujarPecDeck(grupo, ciclo.progreso(), esfuerzo);
        } else if (patron.isVistaFrontal()) {
            muneco.dibujarFrontal(implemento, grupo, ciclo.progreso(), esfuerzo);
        } else {
            Pose pose = Biomecanica2D.pose(patron, implemento, variante, ciclo.progreso());
            muneco.dibujarLateral(pose, grupo, esfuerzo);
        }
        dibujarRotulos(gc, c, ciclo);
    }

    /** Rotulos del lienzo: fase actual, musculo principal y equipamiento. */
    private void dibujarRotulos(GraphicsContext gc, Paleta c, Ciclo ciclo) {
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
        gc.setFill(c.secundario());
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        gc.fillText(GuiaTecnicaService.titulo(new GuiaTecnicaService.Perfil(patron, implemento, variante))
                + "  ·  " + implemento.getEtiqueta()
                + (pausado ? "  ·  en pausa" : "")
                + (velocidad < 1 ? "  ·  cámara lenta 0.5x" : ""), 16, lienzo.getHeight() - 10);
    }
}
