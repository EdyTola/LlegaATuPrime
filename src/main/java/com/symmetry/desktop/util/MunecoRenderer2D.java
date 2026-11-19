package com.symmetry.desktop.util;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.Implemento;
import com.symmetry.desktop.util.Biomecanica2D.Equipo;
import com.symmetry.desktop.util.Biomecanica2D.Pose;
import com.symmetry.desktop.util.Biomecanica2D.Pt;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

import static com.symmetry.desktop.util.Biomecanica2D.ANTEBRAZO;
import static com.symmetry.desktop.util.Biomecanica2D.BRAZO;

/**
 * Dibuja el maniquí humano 2D estilizado (volumen en torso y extremidades, articulaciones
 * redondeadas, degradados sutiles) y el equipamiento sobre un {@link GraphicsContext}. El
 * musculo principal se resalta con el color de acento y pulsa con el esfuerzo.
 */
final class MunecoRenderer2D {

    /** Colores del tema; el modelo se ve nitido tanto sobre fondo oscuro como claro. */
    record Paleta(Color fondo, Color suelo, Color luz, Color base, Color sombra, Color lejano, Color ropa,
                  Color acento, Color equipo, Color equipoOscuro, Color equipoClaro, Color texto,
                  Color excentrica, Color secundario) {

        static Paleta de(boolean oscuro) {
            return oscuro
                    ? new Paleta(Color.web("#161B22"), Color.web("#3B434F"), Color.web("#E9EEF6"),
                    Color.web("#B4BFD1"), Color.web("#7E8AA0"), Color.web("#566175"), Color.web("#3C485D"),
                    Color.web("#19C8FF"), Color.web("#8791A3"), Color.web("#4E5869"), Color.web("#A9B3C3"),
                    Color.web("#E6EAF0"), Color.web("#F2A93B"), Color.web("#7C8594"))
                    : new Paleta(Color.web("#F4F7FB"), Color.web("#C5CCD7"), Color.web("#A9B7CD"),
                    Color.web("#6C7C97"), Color.web("#3E4B63"), Color.web("#A3AEC1"), Color.web("#283245"),
                    Color.web("#0A84FF"), Color.web("#8791A3"), Color.web("#4B5567"), Color.web("#B7C0CE"),
                    Color.web("#1F2937"), Color.web("#C2710C"), Color.web("#98A2B3"));
        }
    }

    // Radios de los segmentos (unidades de mundo).
    private static final double R_CABEZA = 0.055;
    private static final double R_MUSLO_1 = 0.046;
    private static final double R_MUSLO_2 = 0.034;
    private static final double R_PANT_1 = 0.034;
    private static final double R_PANT_2 = 0.021;
    private static final double R_BRAZO_1 = 0.030;
    private static final double R_BRAZO_2 = 0.024;
    private static final double R_ANTE_1 = 0.024;
    private static final double R_ANTE_2 = 0.016;

    // Perfil del torso a lo largo de la columna (0 = cadera, 1 = hombro).
    private static final double[] F = {0.0, 0.18, 0.38, 0.58, 0.78, 1.0};
    private static final double[] PECHO = {0.050, 0.046, 0.042, 0.062, 0.078, 0.050};
    private static final double[] ESPALDA = {0.058, 0.048, 0.044, 0.052, 0.060, 0.042};

    private final GraphicsContext gc;
    private final Paleta c;
    private final double s;
    private final double ox;
    private final double oy;

    MunecoRenderer2D(GraphicsContext gc, Paleta c, double escala, double ox, double oy) {
        this.gc = gc;
        this.c = c;
        this.s = escala;
        this.ox = ox;
        this.oy = oy;
    }

    private double x(Pt p) {
        return ox + p.x() * s;
    }

    private double y(Pt p) {
        return oy + p.y() * s;
    }

    /** El equipo apoyado en el suelo se modela en y = 0 y se dibuja hasta el plano real del suelo. */
    private Pt alSuelo(Pt p) {
        return Math.abs(p.y()) < 1e-9 ? new Pt(p.x(), Biomecanica2D.SUELO) : p;
    }

    private static Color alfa(Color color, double a) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(1, a)));
    }

    // ------------------------------------------------------------------
    // Escena lateral
    // ------------------------------------------------------------------

    void dibujarLateral(Pose p, GrupoMuscular grupo, double esfuerzo) {
        dibujarSuelo(p);
        // 1) Estructura y equipo de fondo
        for (Equipo e : p.equipo) {
            if (!esDelantero(e)) {
                dibujarEquipo(e);
            }
        }
        // 2) Extremidades lejanas (mas oscuras, dan profundidad)
        Pt rodilla2 = p.rodilla2 != null ? p.rodilla2 : p.rodilla.mas(-0.026, 0);
        Pt tobillo2 = p.tobillo2 != null ? p.tobillo2 : p.tobillo.mas(-0.026, 0);
        Pt punta2 = p.punta2 != null ? p.punta2 : p.punta.mas(-0.026, 0);
        capsula(p.cadera, rodilla2, R_MUSLO_1, R_MUSLO_2, c.lejano(), c.lejano(), c.lejano());
        capsula(rodilla2, tobillo2, R_PANT_1, R_PANT_2, c.lejano(), c.lejano(), c.lejano());
        capsula(tobillo2, punta2, 0.022, 0.017, c.ropa(), c.ropa(), c.ropa());
        if (p.codo != null) {
            Pt codo2 = p.codo.mas(-0.022, 0);
            Pt muneca2 = p.muneca.mas(-0.022, 0);
            capsula(p.cuello, codo2, R_BRAZO_1, R_BRAZO_2, c.lejano(), c.lejano(), c.lejano());
            capsula(codo2, muneca2, R_ANTE_1, R_ANTE_2, c.lejano(), c.lejano(), c.lejano());
        }
        // 3) Torso con volumen
        torso(p);
        // 4) Pierna cercana
        capsula(p.cadera, p.rodilla, R_MUSLO_1, R_MUSLO_2, c.luz(), c.base(), c.sombra());
        capsula(p.rodilla, p.tobillo, R_PANT_1, R_PANT_2, c.luz(), c.base(), c.sombra());
        capsula(p.tobillo, p.punta, 0.024, 0.018, c.ropa(), c.ropa(), c.ropa());
        // Pantalon corto: banda oscura en el muslo y pelvis redondeada
        capsula(p.cadera, Pt.lerp(p.cadera, p.rodilla, 0.42), 0.050, 0.044, c.ropa(), c.ropa(), c.ropa());
        circulo(p.cadera, 0.054, c.ropa());
        circulo(p.rodilla, R_MUSLO_2 * 0.92, c.base());
        // 5) Brazo cercano
        if (p.codo != null) {
            capsula(p.cuello, p.codo, R_BRAZO_1, R_BRAZO_2, c.luz(), c.base(), c.sombra());
            capsula(p.codo, p.muneca, R_ANTE_1, R_ANTE_2, c.luz(), c.base(), c.sombra());
            circulo(p.codo, R_BRAZO_2 * 0.95, c.base());
            circulo(p.muneca, 0.020, c.base());
        }
        // 6) Resaltado del musculo principal
        resaltar(p, grupo, esfuerzo);
        // 7) Hombro, cuello y cabeza
        circulo(p.cuello, 0.034, c.base());
        capsula(p.cuello, p.cabeza, 0.024, 0.022, c.luz(), c.base(), c.sombra());
        cabeza(p.cabeza);
        // 8) Equipo en primer plano
        for (Equipo e : p.equipo) {
            if (esDelantero(e)) {
                dibujarEquipo(e);
            }
        }
    }

    private static boolean esDelantero(Equipo e) {
        return switch (e.tipo()) {
            case DISCO, MANCUERNA, AGARRE, BARRA -> true;
            default -> false;
        };
    }

    private void dibujarSuelo(Pose p) {
        double ancho = 1.0 * s;
        gc.setFill(new LinearGradient(0, oy + Biomecanica2D.SUELO * s, 0, oy + 0.1 * s, false, CycleMethod.NO_CYCLE,
                new Stop(0, alfa(c.suelo(), 0.55)), new Stop(1, alfa(c.suelo(), 0.0))));
        gc.fillRect(ox - ancho, oy + Biomecanica2D.SUELO * s, ancho * 2, 0.08 * s);
        gc.setStroke(c.suelo());
        gc.setLineWidth(Math.max(2, 0.012 * s));
        gc.setLineCap(StrokeLineCap.ROUND);
        double ySuelo = oy + Biomecanica2D.SUELO * s;
        gc.strokeLine(ox - ancho, ySuelo, ox + ancho, ySuelo);
    }

    // ------------------------------------------------------------------
    // Primitivas del cuerpo
    // ------------------------------------------------------------------

    /** Miembro conico con extremos redondeados y degradado de luz a sombra perpendicular al eje. */
    private void capsula(Pt a, Pt b, double r1, double r2, Color luz, Color base, Color sombra) {
        double ax = x(a);
        double ay = y(a);
        double bx = x(b);
        double by = y(b);
        double dx = bx - ax;
        double dy = by - ay;
        double l = Math.hypot(dx, dy);
        double R1 = Math.max(1.5, r1 * s);
        double R2 = Math.max(1.5, r2 * s);
        if (l < 0.5) {
            gc.setFill(base);
            gc.fillOval(ax - R1, ay - R1, R1 * 2, R1 * 2);
            return;
        }
        double nx = -dy / l;
        double ny = dx / l;
        // La luz llega desde arriba: el lado de menor Y es el mas claro.
        double signo = ny > 0 ? 1 : -1;
        gc.setFill(new LinearGradient(ax - nx * R1 * signo, ay - ny * R1 * signo,
                ax + nx * R1 * signo, ay + ny * R1 * signo, false, CycleMethod.NO_CYCLE,
                new Stop(0, luz), new Stop(0.42, base), new Stop(1, sombra)));
        gc.fillPolygon(new double[]{ax + nx * R1, bx + nx * R2, bx - nx * R2, ax - nx * R1},
                new double[]{ay + ny * R1, by + ny * R2, by - ny * R2, ay - ny * R1}, 4);
        gc.fillOval(ax - R1, ay - R1, R1 * 2, R1 * 2);
        gc.fillOval(bx - R2, by - R2, R2 * 2, R2 * 2);
    }

    private void circulo(Pt centro, double radio, Color color) {
        double r = Math.max(1.5, radio * s);
        gc.setFill(color);
        gc.fillOval(x(centro) - r, y(centro) - r, r * 2, r * 2);
    }

    private void cabeza(Pt centro) {
        double r = R_CABEZA * s;
        double cx = x(centro);
        double cy = y(centro);
        gc.setFill(new RadialGradient(0, 0, cx - r * 0.35, cy - r * 0.40, r * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, c.luz()), new Stop(0.55, c.base()), new Stop(1, c.sombra())));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.setStroke(alfa(c.sombra(), 0.55));
        gc.setLineWidth(Math.max(1, 0.006 * s));
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static double perfil(double[] v, double f) {
        f = Math.max(0, Math.min(1, f));
        for (int i = 0; i < F.length - 1; i++) {
            if (f <= F[i + 1]) {
                double u = (f - F[i]) / (F[i + 1] - F[i]);
                return v[i] + (v[i + 1] - v[i]) * u;
            }
        }
        return v[v.length - 1];
    }

    /** Normal hacia el pecho de un torso que va de la cadera al hombro. */
    private static Pt normalPecho(Pose p) {
        double vx = p.cuello.x() - p.cadera.x();
        double vy = p.cuello.y() - p.cadera.y();
        double n = Math.max(1e-6, Math.hypot(vx, vy));
        double nx = -vy / n;
        double ny = vx / n;
        return p.pechoInvertido ? new Pt(-nx, -ny) : new Pt(nx, ny);
    }

    private Pt columna(Pose p, double f) {
        return Pt.lerp(p.cadera, p.cuello, f);
    }

    /** Poligono del torso entre las fracciones f0 y f1; {@code lado}: 0 = completo, 1 = pecho, -1 = espalda. */
    private double[][] poligonoTorso(Pose p, double f0, double f1, int lado, double margen) {
        Pt nf = normalPecho(p);
        int muestras = 9;
        double[] xs = new double[muestras * 2];
        double[] ys = new double[muestras * 2];
        for (int i = 0; i < muestras; i++) {
            double f = f0 + (f1 - f0) * i / (muestras - 1);
            Pt cen = columna(p, f);
            double delante = lado >= 0 ? perfil(PECHO, f) - margen : 0.0;
            xs[i] = x(cen.mas(nf.x() * delante, nf.y() * delante));
            ys[i] = y(cen.mas(nf.x() * delante, nf.y() * delante));
            double detras = lado <= 0 ? perfil(ESPALDA, f) - margen : 0.0;
            xs[muestras * 2 - 1 - i] = x(cen.mas(-nf.x() * detras, -nf.y() * detras));
            ys[muestras * 2 - 1 - i] = y(cen.mas(-nf.x() * detras, -nf.y() * detras));
        }
        return new double[][]{xs, ys};
    }

    private void torso(Pose p) {
        double[][] poli = poligonoTorso(p, 0, 1, 0, 0);
        Pt nf = normalPecho(p);
        Pt mitad = columna(p, 0.55);
        Pt frente = mitad.mas(nf.x() * 0.07, nf.y() * 0.07);
        Pt dorso = mitad.mas(-nf.x() * 0.06, -nf.y() * 0.06);
        // El lado mas alto de la pantalla recibe la luz.
        boolean frenteArriba = y(frente) < y(dorso);
        Pt claro = frenteArriba ? frente : dorso;
        Pt oscuro = frenteArriba ? dorso : frente;
        gc.setFill(new LinearGradient(x(claro), y(claro), x(oscuro), y(oscuro), false, CycleMethod.NO_CYCLE,
                new Stop(0, c.luz()), new Stop(0.45, c.base()), new Stop(1, c.sombra())));
        gc.fillPolygon(poli[0], poli[1], poli[0].length);
        gc.setStroke(c.base());
        gc.setLineWidth(Math.max(1, 0.012 * s));
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.strokePolygon(poli[0], poli[1], poli[0].length);
    }

    // ------------------------------------------------------------------
    // Resaltado muscular
    // ------------------------------------------------------------------

    private void resaltar(Pose p, GrupoMuscular grupo, double esfuerzo) {
        if (grupo == null) {
            return;
        }
        double intensidad = 0.35 + 0.55 * esfuerzo;
        switch (grupo) {
            case PECHO -> zonaTorso(p, 0.56, 0.93, 1, intensidad);
            case ESPALDA -> zonaTorso(p, 0.28, 0.95, -1, intensidad);
            case ABDOMEN -> zonaTorso(p, 0.06, 0.50, 1, intensidad);
            case HOMBROS -> {
                zonaCirculo(p.cuello, 0.056, intensidad);
                if (p.codo != null) {
                    zonaMiembro(p.cuello, Pt.lerp(p.cuello, p.codo, 0.55), 0.034, 0.029, intensidad);
                }
            }
            case BICEPS, TRICEPS -> {
                if (p.codo != null) {
                    zonaMiembro(Pt.lerp(p.cuello, p.codo, 0.10), Pt.lerp(p.cuello, p.codo, 0.92), 0.032, 0.027,
                            intensidad);
                }
            }
            case CUADRICEPS, ISQUIOTIBIALES -> zonaMiembro(Pt.lerp(p.cadera, p.rodilla, 0.12),
                    Pt.lerp(p.cadera, p.rodilla, 0.95), 0.047, 0.036, intensidad);
            case GLUTEOS -> {
                Pt nf = normalPecho(p);
                zonaCirculo(p.cadera.mas(-nf.x() * 0.022, -nf.y() * 0.022), 0.072, intensidad);
            }
            case PANTORRILLAS -> zonaMiembro(Pt.lerp(p.rodilla, p.tobillo, 0.10),
                    Pt.lerp(p.rodilla, p.tobillo, 0.90), 0.035, 0.024, intensidad);
        }
    }

    private void zonaTorso(Pose p, double f0, double f1, int lado, double intensidad) {
        double[][] glow = poligonoTorso(p, f0, f1, lado, -0.016);
        gc.setFill(alfa(c.acento(), 0.20 * intensidad));
        gc.fillPolygon(glow[0], glow[1], glow[0].length);
        double[][] poli = poligonoTorso(p, f0, f1, lado, 0.0);
        gc.setFill(alfa(c.acento(), intensidad));
        gc.fillPolygon(poli[0], poli[1], poli[0].length);
        gc.setStroke(alfa(c.acento(), Math.min(1, intensidad + 0.2)));
        gc.setLineWidth(Math.max(1, 0.008 * s));
        gc.strokePolygon(poli[0], poli[1], poli[0].length);
    }

    private void zonaCirculo(Pt centro, double radio, double intensidad) {
        circulo(centro, radio * 1.45, alfa(c.acento(), 0.18 * intensidad));
        circulo(centro, radio, alfa(c.acento(), intensidad));
    }

    private void zonaMiembro(Pt a, Pt b, double r1, double r2, double intensidad) {
        Color halo = alfa(c.acento(), 0.18 * intensidad);
        capsula(a, b, r1 * 1.4, r2 * 1.4, halo, halo, halo);
        Color nucleo = alfa(c.acento(), intensidad);
        capsula(a, b, r1, r2, nucleo, nucleo, nucleo);
    }

    // ------------------------------------------------------------------
    // Equipamiento
    // ------------------------------------------------------------------

    private void linea(Pt a, Pt b, double ancho, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1.5, ancho * s));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(x(a), y(a), x(b), y(b));
    }

    /** Igual que {@link #linea} pero los extremos apoyados en y = 0 llegan hasta el suelo real. */
    private void lineaEstructura(Pt a, Pt b, double ancho, Color color) {
        linea(alSuelo(a), alSuelo(b), ancho, color);
    }

    private void dibujarEquipo(Equipo e) {
        switch (e.tipo()) {
            case BANCO -> {
                for (Pt extremo : new Pt[]{e.a(), e.b()}) {
                    if (extremo.y() < -0.03) {
                        lineaEstructura(extremo, new Pt(extremo.x(), 0), 0.016, c.equipoOscuro());
                    }
                }
                linea(e.a(), e.b(), 0.052, c.equipoOscuro());
                linea(e.a(), e.b(), 0.040, c.equipo());
            }
            case ASIENTO -> {
                linea(e.a(), e.b(), 0.046, c.equipoOscuro());
                linea(e.a(), e.b(), 0.034, c.equipo());
            }
            case PALANCA -> lineaEstructura(e.a(), e.b(), 0.020, c.equipoOscuro());
            case RIEL -> {
                if (e.a().distancia(e.b()) > 1e-6) {
                    lineaEstructura(e.a(), e.b(), 0.016, c.equipoOscuro());
                }
            }
            case PLATAFORMA -> {
                lineaEstructura(e.a(), e.b(), 0.034, c.equipoOscuro());
                lineaEstructura(e.a(), e.b(), 0.022, c.equipoClaro());
            }
            case CABLE -> linea(e.a(), e.b(), 0.007, c.equipoClaro());
            case POLEA -> {
                double r = e.radio() * s;
                gc.setStroke(c.equipoOscuro());
                gc.setFill(c.fondo());
                gc.setLineWidth(Math.max(2, 0.012 * s));
                gc.fillOval(x(e.a()) - r, y(e.a()) - r, r * 2, r * 2);
                gc.strokeOval(x(e.a()) - r, y(e.a()) - r, r * 2, r * 2);
                circulo(e.a(), e.radio() * 0.3, c.equipoOscuro());
            }
            case PILA -> pila(e);
            case MESA -> mesa(e);
            case TOALLA -> {
                linea(e.a(), e.b(), 0.016, c.excentrica());
                linea(e.a(), e.b(), 0.007, alfa(Color.WHITE, 0.35));
            }
            case DISCO -> disco(e);
            case MANCUERNA -> mancuerna(e);
            case BARRA -> {
                circulo(e.a(), e.radio() * 1.25, c.equipoOscuro());
                circulo(e.a(), e.radio() * 0.8, c.equipoClaro());
            }
            case AGARRE -> {
                circulo(e.a(), e.radio(), c.equipoOscuro());
                circulo(e.a(), e.radio() * 0.55, c.equipoClaro());
            }
        }
    }

    /** Mesa: tablero grueso con patas retiradas de los extremos, en un tono mas claro (plano de fondo). */
    private void mesa(Equipo e) {
        double largo = e.b().x() - e.a().x();
        for (double fraccion : new double[]{0.04, 0.96}) {
            Pt arriba = new Pt(e.a().x() + largo * fraccion, e.a().y());
            lineaEstructura(arriba, new Pt(arriba.x(), 0), 0.026, c.equipoOscuro());
        }
        linea(e.a(), e.b(), 0.052, c.equipoOscuro());
        linea(e.a(), e.b(), 0.040, c.equipo());
        linea(e.a().mas(0.02, -0.012), e.b().mas(-0.02, -0.012), 0.008, c.equipoClaro());
    }

    private void pila(Equipo e) {
        // Se aceptan las esquinas en cualquier orden; las apoyadas en y = 0 llegan al suelo real.
        Pt a = alSuelo(e.a());
        Pt b = alSuelo(e.b());
        double x0 = Math.min(x(a), x(b));
        double x1 = Math.max(x(a), x(b));
        double y0 = Math.min(y(a), y(b));
        double y1 = Math.max(y(a), y(b));
        int placas = 9;
        double alto = (y1 - y0) / placas;
        gc.setStroke(c.equipoOscuro());
        gc.setLineWidth(Math.max(1, 0.006 * s));
        for (int i = 0; i < placas; i++) {
            gc.setFill(i == 3 ? alfa(c.acento(), 0.85) : (i % 2 == 0 ? c.equipo() : c.equipoClaro()));
            gc.fillRoundRect(x0, y0 + i * alto + 1, x1 - x0, alto - 2, 3, 3);
            gc.strokeRoundRect(x0, y0 + i * alto + 1, x1 - x0, alto - 2, 3, 3);
        }
    }

    private void disco(Equipo e) {
        double r = e.radio() * s;
        double cx = x(e.a());
        double cy = y(e.a());
        gc.setFill(new RadialGradient(0, 0, cx - r * 0.3, cy - r * 0.3, r * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, c.equipo()), new Stop(1, c.equipoOscuro())));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.setStroke(c.equipoClaro());
        gc.setLineWidth(Math.max(1, 0.007 * s));
        gc.strokeOval(cx - r * 0.78, cy - r * 0.78, r * 1.56, r * 1.56);
        double h = r * 0.22;
        gc.setFill(c.fondo());
        gc.fillOval(cx - h, cy - h, h * 2, h * 2);
        double b = r * 0.12;
        gc.setFill(c.equipoClaro());
        gc.fillOval(cx - b, cy - b, b * 2, b * 2);
    }

    private void mancuerna(Equipo e) {
        double r = e.radio() * s;
        double cx = x(e.a());
        double cy = y(e.a());
        double[] px = new double[6];
        double[] py = new double[6];
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(60 * i + 30);
            px[i] = cx + r * Math.cos(ang);
            py[i] = cy + r * Math.sin(ang);
        }
        gc.setFill(new LinearGradient(cx, cy - r, cx, cy + r, false, CycleMethod.NO_CYCLE,
                new Stop(0, c.equipoClaro()), new Stop(1, c.equipoOscuro())));
        gc.fillPolygon(px, py, 6);
        gc.setStroke(c.equipoOscuro());
        gc.setLineWidth(Math.max(1, 0.007 * s));
        gc.strokePolygon(px, py, 6);
    }

    // ------------------------------------------------------------------
    // Vista frontal (elevacion lateral)
    // ------------------------------------------------------------------

    /**
     * Pec deck visto de frente: sentado con respaldo, columna de placas detras y dos brazos
     * articulados laterales que giran en un plano horizontal. {@code t} = 0 con los brazos
     * juntos al frente y 1 abiertos; los codos permanecen semiflexionados y fijos.
     */
    void dibujarPecDeck(GrupoMuscular grupo, double t, double esfuerzo) {
        dibujarSuelo(new Pose());
        double intensidad = 0.35 + 0.55 * esfuerzo;
        double dy = 0.09;
        // Maquina de fondo: columna de placas, respaldo, asiento, base y pivotes laterales.
        dibujarEquipo(new Equipo(Biomecanica2D.TipoEquipo.PILA, new Pt(-0.075, -1.14), new Pt(0.075, -0.36), 0));
        gc.setFill(c.equipo());
        gc.setStroke(c.equipoOscuro());
        gc.setLineWidth(Math.max(1, 0.008 * s));
        gc.fillRoundRect(x(new Pt(-0.125, 0)), y(new Pt(0, -0.80)), 0.25 * s, 0.50 * s, 14, 14);
        gc.strokeRoundRect(x(new Pt(-0.125, 0)), y(new Pt(0, -0.80)), 0.25 * s, 0.50 * s, 14, 14);
        gc.setFill(c.equipo());
        gc.fillRoundRect(x(new Pt(-0.15, 0)), y(new Pt(0, -0.375)), 0.30 * s, 0.05 * s, 8, 8);
        gc.strokeRoundRect(x(new Pt(-0.15, 0)), y(new Pt(0, -0.375)), 0.30 * s, 0.05 * s, 8, 8);
        lineaEstructura(new Pt(0, -0.33), new Pt(0, 0), 0.03, c.equipoOscuro());
        lineaEstructura(new Pt(-0.30, 0), new Pt(0.30, 0), 0.025, c.equipoOscuro());

        double codoX = Biomecanica2D.lerp(0.14, 0.285, t);
        Pt cuello = new Pt(0, -0.77 + dy);
        // Piernas sentado: muslos hacia el observador (acortados), pantorrillas verticales, pies.
        for (int lado : new int[]{-1, 1}) {
            Pt cad = new Pt(lado * 0.05, -0.37);
            Pt rod = new Pt(lado * 0.078, -0.31);
            Pt tob = new Pt(lado * 0.085, -0.03);
            capsula(cad, rod, R_MUSLO_1, R_MUSLO_2, c.luz(), c.base(), c.sombra());
            capsula(rod, tob, R_PANT_1, R_PANT_2, c.luz(), c.base(), c.sombra());
            capsula(tob, new Pt(lado * 0.125, -0.012), 0.022, 0.018, c.ropa(), c.ropa(), c.ropa());
        }
        // Torso
        Pt[] puntos = {new Pt(-0.118, -0.765 + dy), new Pt(0.118, -0.765 + dy), new Pt(0.064, -0.50 + dy),
                new Pt(0.074, -0.45 + dy), new Pt(-0.074, -0.45 + dy), new Pt(-0.064, -0.50 + dy)};
        double[] xs = new double[puntos.length];
        double[] ys = new double[puntos.length];
        for (int i = 0; i < puntos.length; i++) {
            xs[i] = x(puntos[i]);
            ys[i] = y(puntos[i]);
        }
        gc.setFill(new LinearGradient(x(new Pt(-0.12, 0)), 0, x(new Pt(0.12, 0)), 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, c.sombra()), new Stop(0.35, c.luz()), new Stop(0.65, c.base()), new Stop(1, c.sombra())));
        gc.fillPolygon(xs, ys, xs.length);
        gc.setStroke(c.base());
        gc.setLineWidth(Math.max(1, 0.012 * s));
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.strokePolygon(xs, ys, xs.length);
        capsula(new Pt(-0.05, -0.375), new Pt(0.05, -0.375), 0.05, 0.05, c.ropa(), c.ropa(), c.ropa());
        if (grupo == GrupoMuscular.PECHO) {
            gc.setFill(alfa(c.acento(), intensidad));
            gc.fillRoundRect(x(new Pt(-0.085, 0)), y(new Pt(0, -0.66)), 0.17 * s, 0.13 * s, 8, 8);
        }
        // Brazos articulados: palanca desde el pivote lateral, codo semiflexionado y almohadilla.
        for (int lado : new int[]{-1, 1}) {
            Pt pivote = new Pt(lado * 0.42, -0.60);
            Pt hombro = new Pt(lado * 0.118, -0.675);
            Pt codo = new Pt(lado * codoX, -0.60);
            Pt mano = new Pt(lado * (codoX - 0.025), -0.73);
            lineaEstructura(new Pt(pivote.x(), 0), pivote, 0.022, c.equipoOscuro());
            linea(pivote, codo, 0.020, c.equipoOscuro());
            circulo(pivote, 0.026, c.equipoOscuro());
            capsula(hombro, codo, R_BRAZO_1, R_BRAZO_2, c.luz(), c.base(), c.sombra());
            capsula(codo, mano, R_ANTE_1, R_ANTE_2, c.luz(), c.base(), c.sombra());
            circulo(codo, R_BRAZO_2 * 0.95, c.base());
            circulo(mano, 0.020, c.base());
            // Almohadilla del antebrazo
            linea(codo.mas(lado * 0.034, 0.012), mano.mas(lado * 0.034, -0.012), 0.038, c.equipoOscuro());
            linea(codo.mas(lado * 0.034, 0.012), mano.mas(lado * 0.034, -0.012), 0.026, c.equipo());
            if (grupo == GrupoMuscular.HOMBROS) {
                zonaCirculo(hombro, 0.058, intensidad);
            }
            circulo(hombro, 0.036, c.base());
        }
        capsula(cuello.mas(0, 0.0), cuello.mas(0, -0.06), 0.026, 0.024, c.luz(), c.base(), c.sombra());
        cabeza(new Pt(0, -0.865 + dy));
    }

    void dibujarFrontal(Implemento imp, GrupoMuscular grupo, double t, double esfuerzo) {
        dibujarSuelo(new Pose());
        double phi = Math.toRadians(Biomecanica2D.lerp(4, 88, t));
        double phiAnte = Math.toRadians(Biomecanica2D.lerp(4, 78, t));
        double intensidad = 0.35 + 0.55 * esfuerzo;

        // Piernas y pies
        for (int lado : new int[]{-1, 1}) {
            Pt cad = new Pt(lado * 0.045, -0.47);
            Pt rod = new Pt(lado * 0.058, -0.235);
            Pt tob = new Pt(lado * 0.066, -0.025);
            capsula(cad, rod, R_MUSLO_1, R_MUSLO_2, c.luz(), c.base(), c.sombra());
            capsula(rod, tob, R_PANT_1, R_PANT_2, c.luz(), c.base(), c.sombra());
            capsula(tob, new Pt(lado * 0.108, -0.012), 0.022, 0.018, c.ropa(), c.ropa(), c.ropa());
        }
        // Torso en V
        Pt[] puntos = {new Pt(-0.118, -0.765), new Pt(0.118, -0.765), new Pt(0.064, -0.50),
                new Pt(0.074, -0.45), new Pt(-0.074, -0.45), new Pt(-0.064, -0.50)};
        double[] xs = new double[puntos.length];
        double[] ys = new double[puntos.length];
        for (int i = 0; i < puntos.length; i++) {
            xs[i] = x(puntos[i]);
            ys[i] = y(puntos[i]);
        }
        gc.setFill(new LinearGradient(x(new Pt(-0.12, 0)), 0, x(new Pt(0.12, 0)), 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, c.sombra()), new Stop(0.35, c.luz()), new Stop(0.65, c.base()), new Stop(1, c.sombra())));
        gc.fillPolygon(xs, ys, xs.length);
        gc.setStroke(c.base());
        gc.setLineWidth(Math.max(1, 0.012 * s));
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.strokePolygon(xs, ys, xs.length);
        // Pantalon corto
        capsula(new Pt(-0.05, -0.455), new Pt(0.05, -0.455), 0.05, 0.05, c.ropa(), c.ropa(), c.ropa());

        // Resaltado de torso para grupos que no son el hombro
        if (grupo == GrupoMuscular.PECHO || grupo == GrupoMuscular.ABDOMEN || grupo == GrupoMuscular.ESPALDA) {
            double yTop = grupo == GrupoMuscular.ABDOMEN ? -0.60 : -0.74;
            double yBot = grupo == GrupoMuscular.ABDOMEN ? -0.47 : -0.58;
            Color relleno = alfa(c.acento(), intensidad);
            gc.setFill(relleno);
            gc.fillRoundRect(x(new Pt(-0.08, 0)), y(new Pt(0, yTop)), 0.16 * s, (yBot - yTop) * s, 8, 8);
        }

        // Brazos
        double hy = -0.745;
        for (int lado : new int[]{-1, 1}) {
            Pt hombro = new Pt(lado * 0.118, hy);
            Pt codo = hombro.mas(lado * Math.sin(phi) * BRAZO, Math.cos(phi) * BRAZO);
            Pt muneca = codo.mas(lado * Math.sin(phiAnte) * ANTEBRAZO, Math.cos(phiAnte) * ANTEBRAZO);
            capsula(hombro, codo, R_BRAZO_1, R_BRAZO_2, c.luz(), c.base(), c.sombra());
            capsula(codo, muneca, R_ANTE_1, R_ANTE_2, c.luz(), c.base(), c.sombra());
            circulo(codo, R_BRAZO_2 * 0.95, c.base());
            circulo(muneca, 0.020, c.base());
            if (grupo == GrupoMuscular.HOMBROS) {
                zonaCirculo(hombro.mas(lado * 0.004, 0.008), 0.058, intensidad);
            } else if (grupo == GrupoMuscular.BICEPS || grupo == GrupoMuscular.TRICEPS) {
                zonaMiembro(hombro, Pt.lerp(hombro, codo, 0.9), 0.030, 0.026, intensidad);
            }
            circulo(hombro, 0.036, c.base());
            if (imp == Implemento.MANCUERNA || imp == Implemento.BARRA) {
                Pt agarre = muneca;
                linea(agarre.mas(-0.045, 0), agarre.mas(0.045, 0), 0.012, c.equipoOscuro());
                for (int cabeza : new int[]{-1, 1}) {
                    mancuerna(new Equipo(Biomecanica2D.TipoEquipo.MANCUERNA, agarre.mas(cabeza * 0.05, 0), null, 0.036));
                }
            } else if (imp == Implemento.POLEA) {
                Pt polea = new Pt(lado * 0.42, -0.05);
                linea(muneca, polea, 0.007, c.equipoClaro());
                dibujarEquipo(new Equipo(Biomecanica2D.TipoEquipo.POLEA, polea, null, 0.03));
                dibujarEquipo(new Equipo(Biomecanica2D.TipoEquipo.AGARRE, muneca, null, 0.022));
            }
        }
        // Cuello y cabeza
        capsula(new Pt(0, -0.77), new Pt(0, -0.83), 0.026, 0.024, c.luz(), c.base(), c.sombra());
        cabeza(new Pt(0, -0.865));
    }
}
