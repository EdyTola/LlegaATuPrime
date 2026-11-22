package com.bey.desktop.util;

import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyImplemento;
import com.bey.desktop.model.beyPatronMovimiento;
import com.bey.desktop.model.beyVarianteMovimiento;
import com.bey.desktop.service.beyGuiaTecnicaService;
import com.bey.desktop.util.beyBiomecanica2D.beyPose;
import com.bey.desktop.util.beyMunecoRenderer2D.beyPaleta;
import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class beyExerciseAnimator2D extends StackPane {
    private static final double FIN_SUBIDA = 0.42;
    private static final double FIN_PAUSA = 0.50;

    private final Canvas beyLienzo = new Canvas();
    private final AnimationTimer beyReloj;

    private beyPatronMovimiento beyPatron;
    private beyImplemento beyImplementoActual = beyImplemento.NINGUNO;
    private beyVarianteMovimiento beyVariante = beyVarianteMovimiento.ESTANDAR;
    private beyGrupoMuscular beyGrupo;
    private boolean beyPausado;
    private double beyVelocidad = 1.0;
    private double beyTiempo;
    private long beyUltimoNanos;

    public beyExerciseAnimator2D(beyPatronMovimiento patron, beyGrupoMuscular grupo) {
        this(new beyGuiaTecnicaService.beyPerfil(patron, beyImplemento.NINGUNO, beyVarianteMovimiento.ESTANDAR), grupo);
    }

    public beyExerciseAnimator2D(beyGuiaTecnicaService.beyPerfil perfil, beyGrupoMuscular grupo) {
        this.beyPatron = perfil.patron();
        this.beyImplementoActual = perfil.implemento();
        this.beyVariante = perfil.variante();
        this.beyGrupo = grupo;
        getStyleClass().add("animador-2d");
        setMinSize(360, 280);
        setPrefSize(560, 380);
        getChildren().add(beyLienzo);
        beyLienzo.widthProperty().bind(widthProperty());
        beyLienzo.heightProperty().bind(heightProperty());
        beyLienzo.widthProperty().addListener((obs, a, b) -> beyDibujar());
        beyLienzo.heightProperty().addListener((obs, a, b) -> beyDibujar());

        beyReloj = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (beyUltimoNanos != 0 && !beyPausado) {
                    double dt = Math.min(0.05, (ahora - beyUltimoNanos) / 1_000_000_000.0);
                    beyTiempo += dt * beyVelocidad;
                }
                beyUltimoNanos = ahora;
                beyDibujar();
            }
        };

        sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena == null) {
                beyDetener();
            } else {
                beyReproducir();
            }
        });
    }

    public void beyReproducir() {
        beyUltimoNanos = 0;
        beyReloj.start();
    }

    public void beyDetener() {
        beyReloj.stop();
        beyUltimoNanos = 0;
    }

    public void beySetPausado(boolean pausado) {
        this.beyPausado = pausado;
        beyDibujar();
    }

    public boolean beyIsPausado() {
        return beyPausado;
    }

    public void beySetVelocidad(double velocidad) {
        this.beyVelocidad = Math.max(0.1, velocidad);
    }

    public void beySetPatron(beyPatronMovimiento patron, beyGrupoMuscular grupo) {
        beySetPerfil(new beyGuiaTecnicaService.beyPerfil(patron, beyImplemento.NINGUNO, beyVarianteMovimiento.ESTANDAR), grupo);
    }

    public void beySetPerfil(beyGuiaTecnicaService.beyPerfil perfil, beyGrupoMuscular grupo) {
        this.beyPatron = perfil.patron();
        this.beyImplementoActual = perfil.implemento();
        this.beyVariante = perfil.variante();
        this.beyGrupo = grupo;
        beyTiempo = 0;
        beyDibujar();
    }

    private enum beyFase { CONCENTRICA, PAUSA, EXCENTRICA, ISOMETRICA }

    private record beyCiclo(double progreso, beyFase fase) {
    }

    private beyCiclo beyCiclo() {
        double u = (beyTiempo / beyPatron.getPeriodoSegundos()) % 1.0;
        if (beyVariante.esIsometrica()) {
            return new beyCiclo(0.5 + 0.5 * Math.sin(beyTiempo * 2 * Math.PI * 5.0), beyFase.ISOMETRICA);
        }
        if (beyPatron.isIsometrico()) {
            return new beyCiclo(0.5 + 0.5 * Math.sin(u * 2 * Math.PI), beyFase.ISOMETRICA);
        }
        double p;
        boolean subiendo;
        boolean pausa = false;
        if (u < FIN_SUBIDA) {
            p = beyBiomecanica2D.beySuave(u / FIN_SUBIDA);
            subiendo = true;
        } else if (u < FIN_PAUSA) {
            p = 1;
            subiendo = true;
            pausa = true;
        } else {
            p = 1 - beyBiomecanica2D.beySuave((u - FIN_PAUSA) / (1 - FIN_PAUSA));
            subiendo = false;
        }
        beyFase fase;
        if (pausa) {
            fase = beyFase.PAUSA;
        } else {
            fase = subiendo == beyVariante.concentricaAlSubir(beyPatron) ? beyFase.CONCENTRICA : beyFase.EXCENTRICA;
        }
        return new beyCiclo(p, fase);
    }

    private double beyEsfuerzo(beyCiclo c) {
        if (c.fase() == beyFase.ISOMETRICA) {
            return beyVariante.esIsometrica() ? 0.80 + 0.20 * Math.sin(beyTiempo * 2 * Math.PI * 1.2) : 0.85;
        }
        return beyVariante.concentricaAlSubir(beyPatron) ? c.progreso() : 1 - c.progreso();
    }

    private boolean beyTemaOscuro() {
        for (Parent padre = getParent(); padre != null; padre = padre.getParent()) {
            if (padre.getStyleClass().contains("tema-oscuro")) {
                return true;
            }
        }
        return getScene() != null && getScene().getRoot() != null
                && getScene().getRoot().getStyleClass().contains("tema-oscuro");
    }

    private double beyAltoVista() {
        return switch (beyPatron) {
            case APERTURA_PECHO -> 1.3;
            case DOMINADA -> 1.75;
            case FONDOS -> beyVariante == beyVarianteMovimiento.FONDOS_SILLA ? 1.15 : 1.65;
            case JALON -> 1.62;
            case EXTENSION_TRICEPS -> beyVariante == beyVarianteMovimiento.TRICEPS_SOBRE_CABEZA ? 1.2 : 1.4;
            case CRUNCH -> beyVariante == beyVarianteMovimiento.CRUNCH_POLEA ? 1.5 : 1.15;
            case PRESS_HOMBRO -> beyVariante == beyVarianteMovimiento.FLEXION_PICA || beyVariante == beyVarianteMovimiento.PICA_ELEVADA
                    ? 1.0 : (beyVariante == beyVarianteMovimiento.PRESS_HOMBRO_SENTADO ? 1.3 : 1.15);
            case PRESS_PLANO, PRESS_INCLINADO -> beyVariante == beyVarianteMovimiento.PRESS_SENTADO ? 1.15 : 1.0;
            default -> 1.15;
        };
    }

    private void beyDibujar() {
        double w = beyLienzo.getWidth();
        double h = beyLienzo.getHeight();
        if (w < 10 || h < 10 || beyPatron == null) {
            return;
        }
        GraphicsContext gc = beyLienzo.getGraphicsContext2D();
        beyPaleta c = beyPaleta.de(beyTemaOscuro());
        gc.setFill(c.fondo());
        gc.fillRect(0, 0, w, h);

        double escala = Math.min(w / 2.0, h / beyAltoVista());
        double ox = w / 2 + beyPatron.getDesplazamientoX() * escala;
        double oy = h - 0.12 * escala;

        beyCiclo ciclo = beyCiclo();
        double esfuerzo = beyEsfuerzo(ciclo);
        beyMunecoRenderer2D muneco = new beyMunecoRenderer2D(gc, c, escala, ox, oy);
        if (beyPatron == beyPatronMovimiento.APERTURA_PECHO) {
            muneco.beyDibujarPecDeck(beyGrupo, ciclo.progreso(), esfuerzo);
        } else if (beyPatron.isVistaFrontal()) {
            muneco.beyDibujarFrontal(beyImplementoActual, beyGrupo, ciclo.progreso(), esfuerzo);
        } else {
            beyPose pose = beyBiomecanica2D.beyPose(beyPatron, beyImplementoActual, beyVariante, ciclo.progreso());
            muneco.beyDibujarLateral(pose, beyGrupo, esfuerzo);
        }
        beyDibujarRotulos(gc, c, ciclo);
    }

    private void beyDibujarRotulos(GraphicsContext gc, beyPaleta c, beyCiclo ciclo) {
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

        if (beyGrupo != null) {
            gc.setFill(c.acento());
            gc.fillOval(16, 34, 10, 10);
            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            gc.setFill(c.texto());
            gc.fillText("Músculo principal: " + beyGrupo.getEtiqueta(), 32, 43);
        }
        gc.setFill(c.secundario());
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        gc.fillText(beyGuiaTecnicaService.beyTitulo(new beyGuiaTecnicaService.beyPerfil(beyPatron, beyImplementoActual, beyVariante))
                + "  ·  " + beyImplementoActual.getEtiqueta()
                + (beyPausado ? "  ·  en pausa" : "")
                + (beyVelocidad < 1 ? "  ·  cámara lenta 0.5x" : ""), 16, beyLienzo.getHeight() - 10);
    }
}
