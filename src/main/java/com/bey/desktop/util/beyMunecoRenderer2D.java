package com.bey.desktop.util;

import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyImplemento;
import com.bey.desktop.util.beyBiomecanica2D.beyEquipo;
import com.bey.desktop.util.beyBiomecanica2D.beyPose;
import com.bey.desktop.util.beyBiomecanica2D.beyPt;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

import static com.bey.desktop.util.beyBiomecanica2D.ANTEBRAZO;
import static com.bey.desktop.util.beyBiomecanica2D.BRAZO;

final class beyMunecoRenderer2D {
    record beyPaleta(Color fondo, Color suelo, Color luz, Color base, Color sombra, Color lejano, Color ropa,
                  Color acento, Color equipo, Color equipoOscuro, Color equipoClaro, Color texto,
                  Color excentrica, Color secundario) {
        static beyPaleta de(boolean oscuro) {
            return oscuro
                    ? new beyPaleta(Color.web("#161B22"), Color.web("#3B434F"), Color.web("#E9EEF6"),
                    Color.web("#B4BFD1"), Color.web("#7E8AA0"), Color.web("#566175"), Color.web("#3C485D"),
                    Color.web("#19C8FF"), Color.web("#8791A3"), Color.web("#4E5869"), Color.web("#A9B3C3"),
                    Color.web("#E6EAF0"), Color.web("#F2A93B"), Color.web("#7C8594"))
                    : new beyPaleta(Color.web("#F4F7FB"), Color.web("#C5CCD7"), Color.web("#A9B7CD"),
                    Color.web("#6C7C97"), Color.web("#3E4B63"), Color.web("#A3AEC1"), Color.web("#283245"),
                    Color.web("#0A84FF"), Color.web("#8791A3"), Color.web("#4B5567"), Color.web("#B7C0CE"),
                    Color.web("#1F2937"), Color.web("#C2710C"), Color.web("#98A2B3"));
        }
    }

    private static final double R_CABEZA = 0.055;
    private static final double R_MUSLO_1 = 0.046;
    private static final double R_MUSLO_2 = 0.034;
    private static final double R_PANT_1 = 0.034;
    private static final double R_PANT_2 = 0.021;
    private static final double R_BRAZO_1 = 0.030;
    private static final double R_BRAZO_2 = 0.024;
    private static final double R_ANTE_1 = 0.024;
    private static final double R_ANTE_2 = 0.016;

    private static final double[] F = {0.0, 0.18, 0.38, 0.58, 0.78, 1.0};
    private static final double[] PECHO = {0.050, 0.046, 0.042, 0.062, 0.078, 0.050};
    private static final double[] ESPALDA = {0.058, 0.048, 0.044, 0.052, 0.060, 0.042};

    private final GraphicsContext beyGc;
    private final beyPaleta beyC;
    private final double beyS;
    private final double beyOx;
    private final double beyOy;

    beyMunecoRenderer2D(GraphicsContext gc, beyPaleta c, double escala, double ox, double oy) {
        this.beyGc = gc;
        this.beyC = c;
        this.beyS = escala;
        this.beyOx = ox;
        this.beyOy = oy;
    }

    private double beyX(beyPt p) {
        return beyOx + p.x() * beyS;
    }

    private double beyY(beyPt p) {
        return beyOy + p.y() * beyS;
    }

    private beyPt beyAlSuelo(beyPt p) {
        return Math.abs(p.y()) < 1e-9 ? new beyPt(p.x(), beyBiomecanica2D.SUELO) : p;
    }

    private static Color beyAlfa(Color color, double a) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(1, a)));
    }

    void beyDibujarLateral(beyPose p, beyGrupoMuscular grupo, double esfuerzo) {
        beyDibujarSuelo(p);

        for (beyEquipo e : p.beyEquipo) {
            if (!beyEsDelantero(e)) {
                beyDibujarEquipo(e);
            }
        }

        beyPt rodilla2 = p.beyRodilla2 != null ? p.beyRodilla2 : p.beyRodilla.mas(-0.026, 0);
        beyPt tobillo2 = p.beyTobillo2 != null ? p.beyTobillo2 : p.beyTobillo.mas(-0.026, 0);
        beyPt punta2 = p.beyPunta2 != null ? p.beyPunta2 : p.beyPunta.mas(-0.026, 0);
        beyCapsula(p.beyCadera, rodilla2, R_MUSLO_1, R_MUSLO_2, beyC.lejano(), beyC.lejano(), beyC.lejano());
        beyCapsula(rodilla2, tobillo2, R_PANT_1, R_PANT_2, beyC.lejano(), beyC.lejano(), beyC.lejano());
        beyCapsula(tobillo2, punta2, 0.022, 0.017, beyC.ropa(), beyC.ropa(), beyC.ropa());
        if (p.beyCodo != null) {
            beyPt codo2 = p.beyCodo.mas(-0.022, 0);
            beyPt muneca2 = p.beyMuneca.mas(-0.022, 0);
            beyCapsula(p.beyCuello, codo2, R_BRAZO_1, R_BRAZO_2, beyC.lejano(), beyC.lejano(), beyC.lejano());
            beyCapsula(codo2, muneca2, R_ANTE_1, R_ANTE_2, beyC.lejano(), beyC.lejano(), beyC.lejano());
        }

        beyTorso(p);

        beyCapsula(p.beyCadera, p.beyRodilla, R_MUSLO_1, R_MUSLO_2, beyC.luz(), beyC.base(), beyC.sombra());
        beyCapsula(p.beyRodilla, p.beyTobillo, R_PANT_1, R_PANT_2, beyC.luz(), beyC.base(), beyC.sombra());
        beyCapsula(p.beyTobillo, p.beyPunta, 0.024, 0.018, beyC.ropa(), beyC.ropa(), beyC.ropa());

        beyCapsula(p.beyCadera, beyPt.lerp(p.beyCadera, p.beyRodilla, 0.42), 0.050, 0.044, beyC.ropa(), beyC.ropa(), beyC.ropa());
        beyCirculo(p.beyCadera, 0.054, beyC.ropa());
        beyCirculo(p.beyRodilla, R_MUSLO_2 * 0.92, beyC.base());

        if (p.beyCodo != null) {
            beyCapsula(p.beyCuello, p.beyCodo, R_BRAZO_1, R_BRAZO_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(p.beyCodo, p.beyMuneca, R_ANTE_1, R_ANTE_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCirculo(p.beyCodo, R_BRAZO_2 * 0.95, beyC.base());
            beyCirculo(p.beyMuneca, 0.020, beyC.base());
        }

        beyResaltar(p, grupo, esfuerzo);

        beyCirculo(p.beyCuello, 0.034, beyC.base());
        beyCapsula(p.beyCuello, p.beyCabeza, 0.024, 0.022, beyC.luz(), beyC.base(), beyC.sombra());
        beyCabeza(p.beyCabeza);

        for (beyEquipo e : p.beyEquipo) {
            if (beyEsDelantero(e)) {
                beyDibujarEquipo(e);
            }
        }
    }

    private static boolean beyEsDelantero(beyEquipo e) {
        return switch (e.tipo()) {
            case DISCO, MANCUERNA, AGARRE, BARRA -> true;
            default -> false;
        };
    }

    private void beyDibujarSuelo(beyPose p) {
        double ancho = 1.0 * beyS;
        beyGc.setFill(new LinearGradient(0, beyOy + beyBiomecanica2D.SUELO * beyS, 0, beyOy + 0.1 * beyS, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyAlfa(beyC.suelo(), 0.55)), new Stop(1, beyAlfa(beyC.suelo(), 0.0))));
        beyGc.fillRect(beyOx - ancho, beyOy + beyBiomecanica2D.SUELO * beyS, ancho * 2, 0.08 * beyS);
        beyGc.setStroke(beyC.suelo());
        beyGc.setLineWidth(Math.max(2, 0.012 * beyS));
        beyGc.setLineCap(StrokeLineCap.ROUND);
        double ySuelo = beyOy + beyBiomecanica2D.SUELO * beyS;
        beyGc.strokeLine(beyOx - ancho, ySuelo, beyOx + ancho, ySuelo);
    }

    private void beyCapsula(beyPt a, beyPt b, double r1, double r2, Color luz, Color base, Color sombra) {
        double ax = beyX(a);
        double ay = beyY(a);
        double bx = beyX(b);
        double by = beyY(b);
        double dx = bx - ax;
        double dy = by - ay;
        double l = Math.hypot(dx, dy);
        double R1 = Math.max(1.5, r1 * beyS);
        double R2 = Math.max(1.5, r2 * beyS);
        if (l < 0.5) {
            beyGc.setFill(base);
            beyGc.fillOval(ax - R1, ay - R1, R1 * 2, R1 * 2);
            return;
        }
        double nx = -dy / l;
        double ny = dx / l;

        double signo = ny > 0 ? 1 : -1;
        beyGc.setFill(new LinearGradient(ax - nx * R1 * signo, ay - ny * R1 * signo,
                ax + nx * R1 * signo, ay + ny * R1 * signo, false, CycleMethod.NO_CYCLE,
                new Stop(0, luz), new Stop(0.42, base), new Stop(1, sombra)));
        beyGc.fillPolygon(new double[]{ax + nx * R1, bx + nx * R2, bx - nx * R2, ax - nx * R1},
                new double[]{ay + ny * R1, by + ny * R2, by - ny * R2, ay - ny * R1}, 4);
        beyGc.fillOval(ax - R1, ay - R1, R1 * 2, R1 * 2);
        beyGc.fillOval(bx - R2, by - R2, R2 * 2, R2 * 2);
    }

    private void beyCirculo(beyPt centro, double radio, Color color) {
        double r = Math.max(1.5, radio * beyS);
        beyGc.setFill(color);
        beyGc.fillOval(beyX(centro) - r, beyY(centro) - r, r * 2, r * 2);
    }

    private void beyCabeza(beyPt centro) {
        double r = R_CABEZA * beyS;
        double cx = beyX(centro);
        double cy = beyY(centro);
        beyGc.setFill(new RadialGradient(0, 0, cx - r * 0.35, cy - r * 0.40, r * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.luz()), new Stop(0.55, beyC.base()), new Stop(1, beyC.sombra())));
        beyGc.fillOval(cx - r, cy - r, r * 2, r * 2);
        beyGc.setStroke(beyAlfa(beyC.sombra(), 0.55));
        beyGc.setLineWidth(Math.max(1, 0.006 * beyS));
        beyGc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static double beyPerfil(double[] v, double f) {
        f = Math.max(0, Math.min(1, f));
        for (int i = 0; i < F.length - 1; i++) {
            if (f <= F[i + 1]) {
                double u = (f - F[i]) / (F[i + 1] - F[i]);
                return v[i] + (v[i + 1] - v[i]) * u;
            }
        }
        return v[v.length - 1];
    }

    private static beyPt beyNormalPecho(beyPose p) {
        double vx = p.beyCuello.x() - p.beyCadera.x();
        double vy = p.beyCuello.y() - p.beyCadera.y();
        double n = Math.max(1e-6, Math.hypot(vx, vy));
        double nx = -vy / n;
        double ny = vx / n;
        return p.beyPechoInvertido ? new beyPt(-nx, -ny) : new beyPt(nx, ny);
    }

    private beyPt beyColumna(beyPose p, double f) {
        return beyPt.lerp(p.beyCadera, p.beyCuello, f);
    }

    private double[][] beyPoligonoTorso(beyPose p, double f0, double f1, int lado, double margen) {
        beyPt nf = beyNormalPecho(p);
        int muestras = 9;
        double[] xs = new double[muestras * 2];
        double[] ys = new double[muestras * 2];
        for (int i = 0; i < muestras; i++) {
            double f = f0 + (f1 - f0) * i / (muestras - 1);
            beyPt cen = beyColumna(p, f);
            double delante = lado >= 0 ? beyPerfil(PECHO, f) - margen : 0.0;
            xs[i] = beyX(cen.mas(nf.x() * delante, nf.y() * delante));
            ys[i] = beyY(cen.mas(nf.x() * delante, nf.y() * delante));
            double detras = lado <= 0 ? beyPerfil(ESPALDA, f) - margen : 0.0;
            xs[muestras * 2 - 1 - i] = beyX(cen.mas(-nf.x() * detras, -nf.y() * detras));
            ys[muestras * 2 - 1 - i] = beyY(cen.mas(-nf.x() * detras, -nf.y() * detras));
        }
        return new double[][]{xs, ys};
    }

    private void beyTorso(beyPose p) {
        double[][] poli = beyPoligonoTorso(p, 0, 1, 0, 0);
        beyPt nf = beyNormalPecho(p);
        beyPt mitad = beyColumna(p, 0.55);
        beyPt frente = mitad.mas(nf.x() * 0.07, nf.y() * 0.07);
        beyPt dorso = mitad.mas(-nf.x() * 0.06, -nf.y() * 0.06);

        boolean frenteArriba = beyY(frente) < beyY(dorso);
        beyPt claro = frenteArriba ? frente : dorso;
        beyPt oscuro = frenteArriba ? dorso : frente;
        beyGc.setFill(new LinearGradient(beyX(claro), beyY(claro), beyX(oscuro), beyY(oscuro), false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.luz()), new Stop(0.45, beyC.base()), new Stop(1, beyC.sombra())));
        beyGc.fillPolygon(poli[0], poli[1], poli[0].length);
        beyGc.setStroke(beyC.base());
        beyGc.setLineWidth(Math.max(1, 0.012 * beyS));
        beyGc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        beyGc.strokePolygon(poli[0], poli[1], poli[0].length);
    }

    private void beyResaltar(beyPose p, beyGrupoMuscular grupo, double esfuerzo) {
        if (grupo == null) {
            return;
        }
        double intensidad = 0.35 + 0.55 * esfuerzo;
        switch (grupo) {
            case PECHO -> beyZonaTorso(p, 0.56, 0.93, 1, intensidad);
            case ESPALDA -> beyZonaTorso(p, 0.28, 0.95, -1, intensidad);
            case ABDOMEN -> beyZonaTorso(p, 0.06, 0.50, 1, intensidad);
            case HOMBROS -> {
                beyZonaCirculo(p.beyCuello, 0.056, intensidad);
                if (p.beyCodo != null) {
                    beyZonaMiembro(p.beyCuello, beyPt.lerp(p.beyCuello, p.beyCodo, 0.55), 0.034, 0.029, intensidad);
                }
            }
            case BICEPS, TRICEPS -> {
                if (p.beyCodo != null) {
                    beyZonaMiembro(beyPt.lerp(p.beyCuello, p.beyCodo, 0.10), beyPt.lerp(p.beyCuello, p.beyCodo, 0.92), 0.032, 0.027,
                            intensidad);
                }
            }
            case CUADRICEPS, ISQUIOTIBIALES -> beyZonaMiembro(beyPt.lerp(p.beyCadera, p.beyRodilla, 0.12),
                    beyPt.lerp(p.beyCadera, p.beyRodilla, 0.95), 0.047, 0.036, intensidad);
            case GLUTEOS -> {
                beyPt nf = beyNormalPecho(p);
                beyZonaCirculo(p.beyCadera.mas(-nf.x() * 0.022, -nf.y() * 0.022), 0.072, intensidad);
            }
            case PANTORRILLAS -> beyZonaMiembro(beyPt.lerp(p.beyRodilla, p.beyTobillo, 0.10),
                    beyPt.lerp(p.beyRodilla, p.beyTobillo, 0.90), 0.035, 0.024, intensidad);
        }
    }

    private void beyZonaTorso(beyPose p, double f0, double f1, int lado, double intensidad) {
        double[][] glow = beyPoligonoTorso(p, f0, f1, lado, -0.016);
        beyGc.setFill(beyAlfa(beyC.acento(), 0.20 * intensidad));
        beyGc.fillPolygon(glow[0], glow[1], glow[0].length);
        double[][] poli = beyPoligonoTorso(p, f0, f1, lado, 0.0);
        beyGc.setFill(beyAlfa(beyC.acento(), intensidad));
        beyGc.fillPolygon(poli[0], poli[1], poli[0].length);
        beyGc.setStroke(beyAlfa(beyC.acento(), Math.min(1, intensidad + 0.2)));
        beyGc.setLineWidth(Math.max(1, 0.008 * beyS));
        beyGc.strokePolygon(poli[0], poli[1], poli[0].length);
    }

    private void beyZonaCirculo(beyPt centro, double radio, double intensidad) {
        beyCirculo(centro, radio * 1.45, beyAlfa(beyC.acento(), 0.18 * intensidad));
        beyCirculo(centro, radio, beyAlfa(beyC.acento(), intensidad));
    }

    private void beyZonaMiembro(beyPt a, beyPt b, double r1, double r2, double intensidad) {
        Color halo = beyAlfa(beyC.acento(), 0.18 * intensidad);
        beyCapsula(a, b, r1 * 1.4, r2 * 1.4, halo, halo, halo);
        Color nucleo = beyAlfa(beyC.acento(), intensidad);
        beyCapsula(a, b, r1, r2, nucleo, nucleo, nucleo);
    }

    private void beyLinea(beyPt a, beyPt b, double ancho, Color color) {
        beyGc.setStroke(color);
        beyGc.setLineWidth(Math.max(1.5, ancho * beyS));
        beyGc.setLineCap(StrokeLineCap.ROUND);
        beyGc.strokeLine(beyX(a), beyY(a), beyX(b), beyY(b));
    }

    private void beyLineaEstructura(beyPt a, beyPt b, double ancho, Color color) {
        beyLinea(beyAlSuelo(a), beyAlSuelo(b), ancho, color);
    }

    private void beyDibujarEquipo(beyEquipo e) {
        switch (e.tipo()) {
            case BANCO -> {
                for (beyPt extremo : new beyPt[]{e.a(), e.b()}) {
                    if (extremo.y() < -0.03) {
                        beyLineaEstructura(extremo, new beyPt(extremo.x(), 0), 0.016, beyC.equipoOscuro());
                    }
                }
                beyLinea(e.a(), e.b(), 0.052, beyC.equipoOscuro());
                beyLinea(e.a(), e.b(), 0.040, beyC.equipo());
            }
            case ASIENTO -> {
                beyLinea(e.a(), e.b(), 0.046, beyC.equipoOscuro());
                beyLinea(e.a(), e.b(), 0.034, beyC.equipo());
            }
            case PALANCA -> beyLineaEstructura(e.a(), e.b(), 0.020, beyC.equipoOscuro());
            case RIEL -> {
                if (e.a().distancia(e.b()) > 1e-6) {
                    beyLineaEstructura(e.a(), e.b(), 0.016, beyC.equipoOscuro());
                }
            }
            case PLATAFORMA -> {
                beyLineaEstructura(e.a(), e.b(), 0.034, beyC.equipoOscuro());
                beyLineaEstructura(e.a(), e.b(), 0.022, beyC.equipoClaro());
            }
            case CABLE -> beyLinea(e.a(), e.b(), 0.007, beyC.equipoClaro());
            case POLEA -> {
                double r = e.radio() * beyS;
                beyGc.setStroke(beyC.equipoOscuro());
                beyGc.setFill(beyC.fondo());
                beyGc.setLineWidth(Math.max(2, 0.012 * beyS));
                beyGc.fillOval(beyX(e.a()) - r, beyY(e.a()) - r, r * 2, r * 2);
                beyGc.strokeOval(beyX(e.a()) - r, beyY(e.a()) - r, r * 2, r * 2);
                beyCirculo(e.a(), e.radio() * 0.3, beyC.equipoOscuro());
            }
            case PILA -> beyPila(e);
            case MESA -> beyMesa(e);
            case TOALLA -> {
                beyLinea(e.a(), e.b(), 0.016, beyC.excentrica());
                beyLinea(e.a(), e.b(), 0.007, beyAlfa(Color.WHITE, 0.35));
            }
            case DISCO -> beyDisco(e);
            case MANCUERNA -> beyMancuerna(e);
            case BARRA -> {
                beyCirculo(e.a(), e.radio() * 1.25, beyC.equipoOscuro());
                beyCirculo(e.a(), e.radio() * 0.8, beyC.equipoClaro());
            }
            case AGARRE -> {
                beyCirculo(e.a(), e.radio(), beyC.equipoOscuro());
                beyCirculo(e.a(), e.radio() * 0.55, beyC.equipoClaro());
            }
        }
    }

    private void beyMesa(beyEquipo e) {
        double largo = e.b().x() - e.a().x();
        for (double fraccion : new double[]{0.04, 0.96}) {
            beyPt arriba = new beyPt(e.a().x() + largo * fraccion, e.a().y());
            beyLineaEstructura(arriba, new beyPt(arriba.x(), 0), 0.026, beyC.equipoOscuro());
        }
        beyLinea(e.a(), e.b(), 0.052, beyC.equipoOscuro());
        beyLinea(e.a(), e.b(), 0.040, beyC.equipo());
        beyLinea(e.a().mas(0.02, -0.012), e.b().mas(-0.02, -0.012), 0.008, beyC.equipoClaro());
    }

    private void beyPila(beyEquipo e) {
        beyPt a = beyAlSuelo(e.a());
        beyPt b = beyAlSuelo(e.b());
        double x0 = Math.min(beyX(a), beyX(b));
        double x1 = Math.max(beyX(a), beyX(b));
        double y0 = Math.min(beyY(a), beyY(b));
        double y1 = Math.max(beyY(a), beyY(b));
        int placas = 9;
        double alto = (y1 - y0) / placas;
        beyGc.setStroke(beyC.equipoOscuro());
        beyGc.setLineWidth(Math.max(1, 0.006 * beyS));
        for (int i = 0; i < placas; i++) {
            beyGc.setFill(i == 3 ? beyAlfa(beyC.acento(), 0.85) : (i % 2 == 0 ? beyC.equipo() : beyC.equipoClaro()));
            beyGc.fillRoundRect(x0, y0 + i * alto + 1, x1 - x0, alto - 2, 3, 3);
            beyGc.strokeRoundRect(x0, y0 + i * alto + 1, x1 - x0, alto - 2, 3, 3);
        }
    }

    private void beyDisco(beyEquipo e) {
        double r = e.radio() * beyS;
        double cx = beyX(e.a());
        double cy = beyY(e.a());
        beyGc.setFill(new RadialGradient(0, 0, cx - r * 0.3, cy - r * 0.3, r * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.equipo()), new Stop(1, beyC.equipoOscuro())));
        beyGc.fillOval(cx - r, cy - r, r * 2, r * 2);
        beyGc.setStroke(beyC.equipoClaro());
        beyGc.setLineWidth(Math.max(1, 0.007 * beyS));
        beyGc.strokeOval(cx - r * 0.78, cy - r * 0.78, r * 1.56, r * 1.56);
        double h = r * 0.22;
        beyGc.setFill(beyC.fondo());
        beyGc.fillOval(cx - h, cy - h, h * 2, h * 2);
        double b = r * 0.12;
        beyGc.setFill(beyC.equipoClaro());
        beyGc.fillOval(cx - b, cy - b, b * 2, b * 2);
    }

    private void beyMancuerna(beyEquipo e) {
        double r = e.radio() * beyS;
        double cx = beyX(e.a());
        double cy = beyY(e.a());
        double[] px = new double[6];
        double[] py = new double[6];
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(60 * i + 30);
            px[i] = cx + r * Math.cos(ang);
            py[i] = cy + r * Math.sin(ang);
        }
        beyGc.setFill(new LinearGradient(cx, cy - r, cx, cy + r, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.equipoClaro()), new Stop(1, beyC.equipoOscuro())));
        beyGc.fillPolygon(px, py, 6);
        beyGc.setStroke(beyC.equipoOscuro());
        beyGc.setLineWidth(Math.max(1, 0.007 * beyS));
        beyGc.strokePolygon(px, py, 6);
    }

    void beyDibujarPecDeck(beyGrupoMuscular grupo, double t, double esfuerzo) {
        beyDibujarSuelo(new beyPose());
        double intensidad = 0.35 + 0.55 * esfuerzo;
        double dy = 0.09;

        beyDibujarEquipo(new beyEquipo(beyBiomecanica2D.beyTipoEquipo.PILA, new beyPt(-0.075, -1.14), new beyPt(0.075, -0.36), 0));
        beyGc.setFill(beyC.equipo());
        beyGc.setStroke(beyC.equipoOscuro());
        beyGc.setLineWidth(Math.max(1, 0.008 * beyS));
        beyGc.fillRoundRect(beyX(new beyPt(-0.125, 0)), beyY(new beyPt(0, -0.80)), 0.25 * beyS, 0.50 * beyS, 14, 14);
        beyGc.strokeRoundRect(beyX(new beyPt(-0.125, 0)), beyY(new beyPt(0, -0.80)), 0.25 * beyS, 0.50 * beyS, 14, 14);
        beyGc.setFill(beyC.equipo());
        beyGc.fillRoundRect(beyX(new beyPt(-0.15, 0)), beyY(new beyPt(0, -0.375)), 0.30 * beyS, 0.05 * beyS, 8, 8);
        beyGc.strokeRoundRect(beyX(new beyPt(-0.15, 0)), beyY(new beyPt(0, -0.375)), 0.30 * beyS, 0.05 * beyS, 8, 8);
        beyLineaEstructura(new beyPt(0, -0.33), new beyPt(0, 0), 0.03, beyC.equipoOscuro());
        beyLineaEstructura(new beyPt(-0.30, 0), new beyPt(0.30, 0), 0.025, beyC.equipoOscuro());

        double codoX = beyBiomecanica2D.beyLerp(0.14, 0.285, t);
        beyPt cuello = new beyPt(0, -0.77 + dy);

        for (int lado : new int[]{-1, 1}) {
            beyPt cad = new beyPt(lado * 0.05, -0.37);
            beyPt rod = new beyPt(lado * 0.078, -0.31);
            beyPt tob = new beyPt(lado * 0.085, -0.03);
            beyCapsula(cad, rod, R_MUSLO_1, R_MUSLO_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(rod, tob, R_PANT_1, R_PANT_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(tob, new beyPt(lado * 0.125, -0.012), 0.022, 0.018, beyC.ropa(), beyC.ropa(), beyC.ropa());
        }

        beyPt[] puntos = {new beyPt(-0.118, -0.765 + dy), new beyPt(0.118, -0.765 + dy), new beyPt(0.064, -0.50 + dy),
                new beyPt(0.074, -0.45 + dy), new beyPt(-0.074, -0.45 + dy), new beyPt(-0.064, -0.50 + dy)};
        double[] xs = new double[puntos.length];
        double[] ys = new double[puntos.length];
        for (int i = 0; i < puntos.length; i++) {
            xs[i] = beyX(puntos[i]);
            ys[i] = beyY(puntos[i]);
        }
        beyGc.setFill(new LinearGradient(beyX(new beyPt(-0.12, 0)), 0, beyX(new beyPt(0.12, 0)), 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.sombra()), new Stop(0.35, beyC.luz()), new Stop(0.65, beyC.base()), new Stop(1, beyC.sombra())));
        beyGc.fillPolygon(xs, ys, xs.length);
        beyGc.setStroke(beyC.base());
        beyGc.setLineWidth(Math.max(1, 0.012 * beyS));
        beyGc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        beyGc.strokePolygon(xs, ys, xs.length);
        beyCapsula(new beyPt(-0.05, -0.375), new beyPt(0.05, -0.375), 0.05, 0.05, beyC.ropa(), beyC.ropa(), beyC.ropa());
        if (grupo == beyGrupoMuscular.PECHO) {
            beyGc.setFill(beyAlfa(beyC.acento(), intensidad));
            beyGc.fillRoundRect(beyX(new beyPt(-0.085, 0)), beyY(new beyPt(0, -0.66)), 0.17 * beyS, 0.13 * beyS, 8, 8);
        }

        for (int lado : new int[]{-1, 1}) {
            beyPt pivote = new beyPt(lado * 0.42, -0.60);
            beyPt hombro = new beyPt(lado * 0.118, -0.675);
            beyPt codo = new beyPt(lado * codoX, -0.60);
            beyPt mano = new beyPt(lado * (codoX - 0.025), -0.73);
            beyLineaEstructura(new beyPt(pivote.x(), 0), pivote, 0.022, beyC.equipoOscuro());
            beyLinea(pivote, codo, 0.020, beyC.equipoOscuro());
            beyCirculo(pivote, 0.026, beyC.equipoOscuro());
            beyCapsula(hombro, codo, R_BRAZO_1, R_BRAZO_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(codo, mano, R_ANTE_1, R_ANTE_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCirculo(codo, R_BRAZO_2 * 0.95, beyC.base());
            beyCirculo(mano, 0.020, beyC.base());

            beyLinea(codo.mas(lado * 0.034, 0.012), mano.mas(lado * 0.034, -0.012), 0.038, beyC.equipoOscuro());
            beyLinea(codo.mas(lado * 0.034, 0.012), mano.mas(lado * 0.034, -0.012), 0.026, beyC.equipo());
            if (grupo == beyGrupoMuscular.HOMBROS) {
                beyZonaCirculo(hombro, 0.058, intensidad);
            }
            beyCirculo(hombro, 0.036, beyC.base());
        }
        beyCapsula(cuello.mas(0, 0.0), cuello.mas(0, -0.06), 0.026, 0.024, beyC.luz(), beyC.base(), beyC.sombra());
        beyCabeza(new beyPt(0, -0.865 + dy));
    }

    void beyDibujarFrontal(beyImplemento imp, beyGrupoMuscular grupo, double t, double esfuerzo) {
        beyDibujarSuelo(new beyPose());
        double phi = Math.toRadians(beyBiomecanica2D.beyLerp(4, 88, t));
        double phiAnte = Math.toRadians(beyBiomecanica2D.beyLerp(4, 78, t));
        double intensidad = 0.35 + 0.55 * esfuerzo;

        for (int lado : new int[]{-1, 1}) {
            beyPt cad = new beyPt(lado * 0.045, -0.47);
            beyPt rod = new beyPt(lado * 0.058, -0.235);
            beyPt tob = new beyPt(lado * 0.066, -0.025);
            beyCapsula(cad, rod, R_MUSLO_1, R_MUSLO_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(rod, tob, R_PANT_1, R_PANT_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(tob, new beyPt(lado * 0.108, -0.012), 0.022, 0.018, beyC.ropa(), beyC.ropa(), beyC.ropa());
        }

        beyPt[] puntos = {new beyPt(-0.118, -0.765), new beyPt(0.118, -0.765), new beyPt(0.064, -0.50),
                new beyPt(0.074, -0.45), new beyPt(-0.074, -0.45), new beyPt(-0.064, -0.50)};
        double[] xs = new double[puntos.length];
        double[] ys = new double[puntos.length];
        for (int i = 0; i < puntos.length; i++) {
            xs[i] = beyX(puntos[i]);
            ys[i] = beyY(puntos[i]);
        }
        beyGc.setFill(new LinearGradient(beyX(new beyPt(-0.12, 0)), 0, beyX(new beyPt(0.12, 0)), 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, beyC.sombra()), new Stop(0.35, beyC.luz()), new Stop(0.65, beyC.base()), new Stop(1, beyC.sombra())));
        beyGc.fillPolygon(xs, ys, xs.length);
        beyGc.setStroke(beyC.base());
        beyGc.setLineWidth(Math.max(1, 0.012 * beyS));
        beyGc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        beyGc.strokePolygon(xs, ys, xs.length);

        beyCapsula(new beyPt(-0.05, -0.455), new beyPt(0.05, -0.455), 0.05, 0.05, beyC.ropa(), beyC.ropa(), beyC.ropa());

        if (grupo == beyGrupoMuscular.PECHO || grupo == beyGrupoMuscular.ABDOMEN || grupo == beyGrupoMuscular.ESPALDA) {
            double yTop = grupo == beyGrupoMuscular.ABDOMEN ? -0.60 : -0.74;
            double yBot = grupo == beyGrupoMuscular.ABDOMEN ? -0.47 : -0.58;
            Color relleno = beyAlfa(beyC.acento(), intensidad);
            beyGc.setFill(relleno);
            beyGc.fillRoundRect(beyX(new beyPt(-0.08, 0)), beyY(new beyPt(0, yTop)), 0.16 * beyS, (yBot - yTop) * beyS, 8, 8);
        }

        double hy = -0.745;
        for (int lado : new int[]{-1, 1}) {
            beyPt hombro = new beyPt(lado * 0.118, hy);
            beyPt codo = hombro.mas(lado * Math.sin(phi) * BRAZO, Math.cos(phi) * BRAZO);
            beyPt muneca = codo.mas(lado * Math.sin(phiAnte) * ANTEBRAZO, Math.cos(phiAnte) * ANTEBRAZO);
            beyCapsula(hombro, codo, R_BRAZO_1, R_BRAZO_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCapsula(codo, muneca, R_ANTE_1, R_ANTE_2, beyC.luz(), beyC.base(), beyC.sombra());
            beyCirculo(codo, R_BRAZO_2 * 0.95, beyC.base());
            beyCirculo(muneca, 0.020, beyC.base());
            if (grupo == beyGrupoMuscular.HOMBROS) {
                beyZonaCirculo(hombro.mas(lado * 0.004, 0.008), 0.058, intensidad);
            } else if (grupo == beyGrupoMuscular.BICEPS || grupo == beyGrupoMuscular.TRICEPS) {
                beyZonaMiembro(hombro, beyPt.lerp(hombro, codo, 0.9), 0.030, 0.026, intensidad);
            }
            beyCirculo(hombro, 0.036, beyC.base());
            if (imp == beyImplemento.MANCUERNA || imp == beyImplemento.BARRA) {
                beyPt agarre = muneca;
                beyLinea(agarre.mas(-0.045, 0), agarre.mas(0.045, 0), 0.012, beyC.equipoOscuro());
                for (int cabeza : new int[]{-1, 1}) {
                    beyMancuerna(new beyEquipo(beyBiomecanica2D.beyTipoEquipo.MANCUERNA, agarre.mas(cabeza * 0.05, 0), null, 0.036));
                }
            } else if (imp == beyImplemento.POLEA) {
                beyPt polea = new beyPt(lado * 0.42, -0.05);
                beyLinea(muneca, polea, 0.007, beyC.equipoClaro());
                beyDibujarEquipo(new beyEquipo(beyBiomecanica2D.beyTipoEquipo.POLEA, polea, null, 0.03));
                beyDibujarEquipo(new beyEquipo(beyBiomecanica2D.beyTipoEquipo.AGARRE, muneca, null, 0.022));
            }
        }

        beyCapsula(new beyPt(0, -0.77), new beyPt(0, -0.83), 0.026, 0.024, beyC.luz(), beyC.base(), beyC.sombra());
        beyCabeza(new beyPt(0, -0.865));
    }
}
