package com.bey.desktop.util;

import com.bey.desktop.model.beyImplemento;
import com.bey.desktop.model.beyPatronMovimiento;
import com.bey.desktop.model.beyVarianteMovimiento;

import java.util.ArrayList;
import java.util.List;

public final class beyBiomecanica2D {
    public static final double TORSO = 0.30;
    public static final double MUSLO = 0.24;
    public static final double PANTORRILLA = 0.24;
    public static final double BRAZO = 0.17;
    public static final double ANTEBRAZO = 0.15;
    public static final double PIE = 0.09;
    public static final double CABEZA_DISTANCIA = 0.095;

    public static final double SUELO = 0.022;

    static final int ADELANTE = -1;
    static final int ATRAS = 1;

    private beyBiomecanica2D() {
    }

    public record beyPt(double x, double y) {
        public beyPt mas(double dx, double dy) {
            return new beyPt(x + dx, y + dy);
        }

        public static beyPt lerp(beyPt a, beyPt b, double t) {
            return new beyPt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        }

        public double distancia(beyPt o) {
            return Math.hypot(x - o.x, y - o.y);
        }
    }

    public enum beyTipoEquipo {
        DISCO,

        MANCUERNA,

        BANCO,

        ASIENTO,

        PILA,

        RIEL,

        PALANCA,

        PLATAFORMA,

        CABLE,

        POLEA,

        MESA,

        TOALLA,

        BARRA,

        AGARRE
    }

    public record beyEquipo(beyTipoEquipo tipo, beyPt a, beyPt b, double radio) {
    }

    public static final class beyPose {
        public beyPt beyCabeza;

        public beyPt beyCuello;
        public beyPt beyCadera;
        public beyPt beyRodilla;
        public beyPt beyTobillo;
        public beyPt beyPunta;
        public beyPt beyCodo;
        public beyPt beyMuneca;

        public beyPt beyRodilla2;
        public beyPt beyTobillo2;
        public beyPt beyPunta2;

        public boolean beyPechoInvertido;
        public final List<beyEquipo> beyEquipo = new ArrayList<>();
    }

    static beyPt beyArriba(beyPt o, double largo, double grados) {
        double r = Math.toRadians(grados);
        return new beyPt(o.x + largo * Math.sin(r), o.y - largo * Math.cos(r));
    }

    static beyPt beyAlcance(beyPt origen, beyPt objetivo, double maximo) {
        double dx = objetivo.x - origen.x;
        double dy = objetivo.y - origen.y;
        double d = Math.hypot(dx, dy);
        double tope = maximo - 0.0005;
        if (d <= tope || d == 0) {
            return objetivo;
        }
        return new beyPt(origen.x + dx / d * tope, origen.y + dy / d * tope);
    }

    static beyPt beyArticulacion(beyPt a, beyPt b, double l1, double l2, int signo) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double d = Math.max(Math.abs(l1 - l2) + 0.001, Math.min(Math.hypot(dx, dy), l1 + l2 - 0.0005));
        double base = Math.atan2(dy, dx);
        double coseno = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double ang = Math.acos(Math.max(-1, Math.min(1, coseno)));
        double th = base + signo * ang;
        return new beyPt(a.x + l1 * Math.cos(th), a.y + l1 * Math.sin(th));
    }

    static double beyLerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double beySuave(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static beyPt beyUnitario(beyPt a, beyPt b) {
        double d = Math.max(1e-9, a.distancia(b));
        return new beyPt((b.x - a.x) / d, (b.y - a.y) / d);
    }

    static beyPt beyNormalEspalda(beyPt cadera, beyPt cuello) {
        beyPt v = beyUnitario(cadera, cuello);
        return new beyPt(v.y, -v.x);
    }

    static beyPt beyInterseccion(beyPt a, double ra, beyPt b, double rb, boolean arriba) {
        double d = Math.max(1e-6, a.distancia(b));
        double x = (d * d + ra * ra - rb * rb) / (2 * d);
        double h = Math.sqrt(Math.max(0, ra * ra - x * x));
        double ux = (b.x - a.x) / d;
        double uy = (b.y - a.y) / d;
        beyPt p1 = new beyPt(a.x + ux * x - uy * h, a.y + uy * x + ux * h);
        beyPt p2 = new beyPt(a.x + ux * x + uy * h, a.y + uy * x - ux * h);
        return (p1.y < p2.y) == arriba ? p1 : p2;
    }

    private static beyPose beyArmar(beyPt cadera, double inclinacionTorso, beyPt tobillo, beyPt objetivoMuneca,
                              int signoRodilla, int signoCodo) {
        beyPose p = new beyPose();
        p.beyCadera = cadera;
        p.beyCuello = beyArriba(cadera, TORSO, inclinacionTorso);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, inclinacionTorso * 0.7);
        p.beyTobillo = beyAlcance(cadera, tobillo, MUSLO + PANTORRILLA);
        p.beyRodilla = beyArticulacion(cadera, p.beyTobillo, MUSLO, PANTORRILLA, signoRodilla);
        p.beyPunta = new beyPt(p.beyTobillo.x + PIE, Math.min(0, p.beyTobillo.y + 0.0));
        if (objetivoMuneca != null) {
            beyBrazo(p, objetivoMuneca, signoCodo);
        }
        return p;
    }

    private static void beyBrazo(beyPose p, beyPt objetivoMuneca, int signoCodo) {
        p.beyMuneca = beyAlcance(p.beyCuello, objetivoMuneca, BRAZO + ANTEBRAZO);
        p.beyCodo = beyArticulacion(p.beyCuello, p.beyMuneca, BRAZO, ANTEBRAZO, signoCodo);
    }

    private static beyPt beyRodillaMasAlta(beyPt cadera, beyPt tobillo) {
        beyPt a = beyArticulacion(cadera, tobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPt b = beyArticulacion(cadera, tobillo, MUSLO, PANTORRILLA, ATRAS);
        return a.y() <= b.y() ? a : b;
    }

    private static void beyPieEnSuelo(beyPose p) {
        p.beyPunta = new beyPt(p.beyTobillo.x + PIE, 0);
    }

    private static void beyCarga(beyPose p, beyImplemento imp, beyPt mano, double radioDisco) {
        switch (imp) {
            case BARRA -> p.beyEquipo.add(new beyEquipo(beyTipoEquipo.DISCO, mano, null, radioDisco));
            case MANCUERNA -> p.beyEquipo.add(new beyEquipo(beyTipoEquipo.MANCUERNA, mano, null,
                    Math.max(0.04, radioDisco * 0.42)));
            default -> {
            }
        }
    }

    public static beyPose beyPose(beyPatronMovimiento patron, beyImplemento imp, beyVarianteMovimiento variante, double t) {
        t = Math.max(0, Math.min(1, t));
        beyPose p = switch (patron) {
            case SENTADILLA -> beySentadilla(imp, variante, t);
            case EXTENSION_CUADRICEPS -> beySentadoPierna(imp, t, true);
            case APERTURA_PECHO -> new beyPose();
            case PRESS_PLANO -> beyPress(imp, variante, t, false);
            case PRESS_INCLINADO -> beyPress(imp, variante, t, true);
            case FLEXION -> switch (variante) {
                case FLEXION_RODILLAS -> beyFlexionRodillas(t);
                case FLEXION_DECLINADA -> beyTablonElevado(beyLerp(0.13, 0.318, t), -0.05);
                default -> beyTablon(beyLerp(0.13, 0.318, t), false, -0.05);
            };
            case PLANCHA -> variante == beyVarianteMovimiento.PLANCHA_ALTA
                    ? beyTablon(0.318 + 0.004 * (t - 0.5), false, -0.05)
                    : beyTablon(0.197 + 0.004 * (t - 0.5), true, -0.05);
            case JALON -> beyJalon(variante == beyVarianteMovimiento.JALON_NEUTRO, t);
            case REMO -> beyRemo(imp, variante, t);
            case DOMINADA -> beyDominada(t);
            case CURL_BICEPS -> switch (variante) {
                case CURL_ISOMETRICO_TOALLA -> beyCurlToalla(t);
                case CURL_INCLINADO -> beyCurlInclinado(imp, t);
                default -> beyCurl(imp, t);
            };
            case EXTENSION_TRICEPS -> beyTriceps(imp, variante, t);
            case PESO_MUERTO -> beyPesoMuerto(imp, t);
            case HIP_THRUST -> beyHipThrust(imp, t);
            case PUENTE_GLUTEOS -> beyPuente(t);
            case ELEVACION_LATERAL -> new beyPose();
            case PRESS_HOMBRO -> switch (variante) {
                case FLEXION_PICA -> beyPica(t, false);
                case PICA_ELEVADA -> beyPica(t, true);
                case PRESS_HOMBRO_SENTADO -> beyPressHombroSentado(imp, t);
                default -> beyPressHombro(imp, t);
            };
            case ZANCADA -> beyZancada(imp, variante, t);
            case ELEVACION_TALONES -> variante == beyVarianteMovimiento.TALONES_SENTADO
                    ? beyTalonesSentado(imp, t) : beyTalones(imp, t);
            case CURL_FEMORAL -> switch (variante) {
                case CURL_FEMORAL_SENTADO -> beySentadoPierna(imp, t, false);
                case CURL_FEMORAL_NORDICO -> beyCaidaDeRodillas(t, false);
                default -> beyCurlFemoral(imp, t);
            };
            case CRUNCH -> switch (variante) {
                case CRUNCH_POLEA -> beyCrunchPolea(t);
                case RUEDA_ABDOMINAL -> beyCaidaDeRodillas(t, true);
                default -> beyCrunch(t);
            };
            case ELEVACION_PIERNAS -> beyElevacionPiernas(t);
            case FONDOS -> variante == beyVarianteMovimiento.FONDOS_SILLA ? beyFondosSilla(t) : beyFondos(t);
            case SUPERMAN -> beySuperman(t);
            case BURPEE -> beyBurpee(t);
        };
        return p;
    }

    private static beyPose beySentadilla(beyImplemento imp, beyVarianteMovimiento var, double t) {
        if (var == beyVarianteMovimiento.PRENSA) {
            return beyPrensa(t);
        }
        if (var == beyVarianteMovimiento.HACK) {
            return beyHack(t);
        }
        boolean guiada = imp == beyImplemento.MAQUINA;
        double inc = beyLerp(3, 42, t);
        double pieX = guiada ? 0.07 : 0.0;
        beyPt cadera = new beyPt(guiada ? 0.02 - TORSO * Math.sin(Math.toRadians(inc)) : beyLerp(0, -0.14, t),
                -beyLerp(0.47, 0.24, t));
        beyPose p = beyArmar(cadera, inc, new beyPt(pieX, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        beyPt barra = p.beyCuello.mas(esp.x() * 0.045, esp.y() * 0.045);
        switch (imp) {
            case BARRA, MAQUINA -> {
                beyBrazo(p, p.beyCuello.mas(0.03, 0.03), ATRAS);
                p.beyEquipo.add(new beyEquipo(beyTipoEquipo.DISCO, barra, null, 0.10));
                if (guiada) {
                    p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(barra.x(), 0), new beyPt(barra.x(), -1.12), 0));
                    p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(barra.x() - 0.30, 0), new beyPt(barra.x() + 0.30, 0), 0));
                }
            }
            case MANCUERNA -> {
                beyBrazo(p, p.beyCuello.mas(0.10, 0.07), ATRAS);
                beyCarga(p, imp, p.beyMuneca, 0.09);
            }
            default -> beyBrazo(p, p.beyCuello.mas(0.27, 0.03), ATRAS);
        }
        if (var == beyVarianteMovimiento.PISTOL) {
            p.beyTobillo2 = new beyPt(p.beyCadera.x + MUSLO + PANTORRILLA - 0.004, p.beyCadera.y);
            p.beyRodilla2 = beyPt.lerp(p.beyCadera, p.beyTobillo2, MUSLO / (MUSLO + PANTORRILLA));
            p.beyPunta2 = p.beyTobillo2.mas(0.0, -0.07);
        }
        return p;
    }

    private static beyPose beyHack(double t) {
        double beta = Math.toRadians(18);
        beyPt subida = new beyPt(-Math.sin(beta), -Math.cos(beta));
        beyPt cadera0 = new beyPt(-0.08, -0.45);
        double descenso = 0.26 * t;
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(cadera0.x() - subida.x() * descenso, cadera0.y() - subida.y() * descenso);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -18);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -18);
        p.beyTobillo = beyAlcance(p.beyCadera, new beyPt(0.10, 0), MUSLO + PANTORRILLA);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(0.07, 0.10), ATRAS);
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        beyPt bajo = new beyPt(cadera0.x() + subida.x() * -0.45, cadera0.y() + subida.y() * -0.45);
        beyPt alto = new beyPt(cadera0.x() + subida.x() * 0.62, cadera0.y() + subida.y() * 0.62);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, bajo.mas(esp.x() * 0.085, esp.y() * 0.085),
                alto.mas(esp.x() * 0.085, esp.y() * 0.085), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, bajo.mas(esp.x() * 0.15, esp.y() * 0.15),
                alto.mas(esp.x() * 0.15, esp.y() * 0.15), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, bajo.mas(esp.x() * 0.15, esp.y() * 0.15),
                new beyPt(bajo.x() + esp.x() * 0.15, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.40, 0), new beyPt(0.42, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PLATAFORMA, new beyPt(0.0, 0), new beyPt(0.32, 0), 0));

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyCuello.mas(esp.x() * 0.06, esp.y() * 0.06), null, 0.03));
        return p;
    }

    private static beyPose beyPrensa(double t) {
        beyPose p = new beyPose();
        double angulo = 38;
        beyPt u = new beyPt(Math.cos(Math.toRadians(angulo)), -Math.sin(Math.toRadians(angulo)));
        beyPt n = new beyPt(Math.sin(Math.toRadians(angulo)), Math.cos(Math.toRadians(angulo)));
        p.beyCadera = new beyPt(-0.30, -0.20);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -55);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -55);
        double d = beyLerp(0.47, 0.28, t);
        p.beyTobillo = new beyPt(p.beyCadera.x + d * u.x(), p.beyCadera.y + d * u.y());
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        p.beyPunta = new beyPt(p.beyTobillo.x - n.x() * 0.075, p.beyTobillo.y - n.y() * 0.075);
        beyBrazo(p, p.beyCadera.mas(0.04, -0.13), ATRAS);

        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        beyPt centroPlataforma = new beyPt(p.beyTobillo.x + u.x() * 0.045, p.beyTobillo.y + u.y() * 0.045);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PLATAFORMA, centroPlataforma.mas(-n.x() * 0.12, -n.y() * 0.12),
                centroPlataforma.mas(n.x() * 0.12, n.y() * 0.12), 0));
        beyPt finRiel = new beyPt(p.beyCadera.x + 0.52 * u.x() + n.x() * 0.13, p.beyCadera.y + 0.52 * u.y() + n.y() * 0.13);
        beyPt inicioRiel = new beyPt(p.beyCadera.x - 0.10 + n.x() * 0.13, p.beyCadera.y + 0.0 + n.y() * 0.13);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, inicioRiel, finRiel, 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, finRiel, new beyPt(finRiel.x(), 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, inicioRiel, new beyPt(inicioRiel.x(), 0), 0));

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO,
                p.beyCadera.mas(esp.x() * 0.075 - 0.02, esp.y() * 0.075 + 0.0),
                p.beyCuello.mas(esp.x() * 0.075, esp.y() * 0.075), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(-0.10, 0.075), p.beyCadera.mas(0.11, 0.075), 0));
        return p;
    }

    private static beyPose beyPress(beyImplemento imp, beyVarianteMovimiento var, double t, boolean inclinado) {
        if (var == beyVarianteMovimiento.PRESS_SENTADO) {
            return beyPressSentado(t, inclinado ? 35 : 12);
        }
        beyPose p = new beyPose();
        double d = beyLerp(0.12, 0.31, t);
        if (!inclinado) {
            p.beyCadera = new beyPt(0, -0.34);
            p.beyCuello = new beyPt(-TORSO, -0.34);
            p.beyCabeza = new beyPt(-TORSO - CABEZA_DISTANCIA, -0.34);
            p.beyTobillo = new beyPt(0.24, 0);
            p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
            beyPieEnSuelo(p);
            beyBrazo(p, new beyPt(p.beyCuello.x + 0.02, p.beyCuello.y - d), ATRAS);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(-0.58, -0.265), new beyPt(0.05, -0.265), 0));
        } else {
            p.beyCadera = new beyPt(0, -0.34);
            p.beyCuello = beyArriba(p.beyCadera, TORSO, -50);
            p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -50);
            p.beyTobillo = new beyPt(0.24, 0);
            p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
            beyPieEnSuelo(p);
            beyBrazo(p, p.beyCuello.mas(0.643 * d, -0.766 * d), ATRAS);
            beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, p.beyCadera.mas(esp.x() * 0.085 + 0.05, esp.y() * 0.085),
                    p.beyCuello.mas(esp.x() * 0.085 - 0.02, esp.y() * 0.085 - 0.03), 0));
        }
        beyCarga(p, imp, p.beyMuneca, 0.09);
        return p;
    }

    private static beyPose beyPressSentado(double t, double inclinacion) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0, -0.30);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -inclinacion);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -inclinacion);
        p.beyTobillo = new beyPt(0.21, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        double d = beyLerp(0.12, 0.31, t);
        double r = Math.toRadians(inclinacion);
        beyBrazo(p, p.beyCuello.mas(Math.cos(r) * d, -Math.sin(r) * d), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyMuneca, null, 0.024));
        beyMarcoSentado(p, 0.085);
        return p;
    }

    private static void beyMarcoSentado(beyPose p, double separacion) {
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(-0.10, 0.075), p.beyCadera.mas(0.12, 0.075), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO,
                p.beyCadera.mas(esp.x() * separacion, esp.y() * separacion),
                p.beyCuello.mas(esp.x() * separacion, esp.y() * separacion), 0));
        beyPt tope = p.beyCuello.mas(esp.x() * (separacion + 0.04), esp.y() * (separacion + 0.04));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(p.beyCadera.x() - 0.16, 0), new beyPt(tope.x(), tope.y() - 0.05), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.95, 0), new beyPt(0.16, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(-0.92, -0.86), new beyPt(-0.77, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.845, -0.86), new beyPt(tope.x(), tope.y() - 0.05), 0));
    }

    private static beyPose beyTablon(double alturaHombro, boolean antebrazos, double manoX) {
        beyPose p = new beyPose();
                p.beyTobillo = new beyPt(manoX - 0.75, -0.03);
        p.beyCuello = new beyPt(manoX, -alturaHombro);
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.beyCadera = beyPt.lerp(p.beyTobillo, p.beyCuello, (MUSLO + PANTORRILLA) / largo);
        p.beyRodilla = beyPt.lerp(p.beyTobillo, p.beyCadera, 0.5);
        p.beyPunta = new beyPt(p.beyTobillo.x - 0.035, 0);
        beyPt dir = beyUnitario(p.beyCadera, p.beyCuello);
        p.beyCabeza = p.beyCuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        if (antebrazos) {
            p.beyCodo = new beyPt(manoX, -0.026);
            p.beyMuneca = new beyPt(manoX + ANTEBRAZO - 0.002, -0.026);
        } else {
            beyBrazo(p, new beyPt(manoX, -0.02), ATRAS);
        }
        return p;
    }

    private static beyPose beyTablonElevado(double alturaHombro, double manoX) {
        beyPose p = new beyPose();
        p.beyTobillo = new beyPt(manoX - 0.75, -0.43);
        p.beyCuello = new beyPt(manoX, -alturaHombro);
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.beyCadera = beyPt.lerp(p.beyTobillo, p.beyCuello, (MUSLO + PANTORRILLA) / largo);
        p.beyRodilla = beyPt.lerp(p.beyTobillo, p.beyCadera, 0.5);
        p.beyPunta = new beyPt(p.beyTobillo.x - 0.035, -0.40);
        beyPt dir = beyUnitario(p.beyCadera, p.beyCuello);
        p.beyCabeza = p.beyCuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        beyBrazo(p, new beyPt(manoX, -0.02), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(p.beyTobillo.x - 0.14, -0.375), new beyPt(p.beyTobillo.x + 0.10, -0.375), 0));
        return p;
    }

    private static beyPose beyFlexionRodillas(double t) {
        beyPose p = new beyPose();
                double h = beyLerp(0.13, 0.318, t);
        double manoX = -0.05;
        p.beyCuello = new beyPt(manoX, -h);
        double distancia = TORSO + MUSLO;
        double dx = Math.sqrt(Math.max(0.01, distancia * distancia - Math.pow(h - 0.03, 2)));
        p.beyRodilla = new beyPt(manoX - Math.max(0.46, dx), -0.03);
        p.beyCadera = beyPt.lerp(p.beyRodilla, p.beyCuello, MUSLO / distancia);
        p.beyTobillo = new beyPt(p.beyRodilla.x - PANTORRILLA * 0.9, -0.10);
        p.beyPunta = new beyPt(p.beyTobillo.x - 0.05, -0.04);
        beyPt dir = beyUnitario(p.beyCadera, p.beyCuello);
        p.beyCabeza = p.beyCuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        beyBrazo(p, new beyPt(manoX, -0.02), ATRAS);
        return p;
    }

    private static beyPose beyPica(double t, boolean piesElevados) {
        beyPose p = new beyPose();
        beyPt mano = new beyPt(0.05, -0.02);
        double alturaPie = piesElevados ? -0.43 : -0.03;
        p.beyTobillo = new beyPt(-0.42, alturaPie);
        p.beyPunta = new beyPt(p.beyTobillo.x + 0.07, piesElevados ? -0.40 : 0);

        p.beyCuello = new beyPt(mano.x() - beyLerp(0.09, 0.01, t), -beyLerp(0.31, 0.15, t));
        p.beyCadera = beyInterseccion(p.beyCuello, TORSO, p.beyTobillo, MUSLO + PANTORRILLA - 0.002, true);
        p.beyRodilla = beyPt.lerp(p.beyCadera, p.beyTobillo, MUSLO / (MUSLO + PANTORRILLA));
        beyPt dir = beyUnitario(p.beyCadera, p.beyCuello);
        p.beyCabeza = p.beyCuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        beyBrazo(p, mano, ATRAS);
        if (piesElevados) {
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(p.beyTobillo.x - 0.14, -0.375), new beyPt(p.beyTobillo.x + 0.10, -0.375), 0));
        }
        return p;
    }

    private static beyPose beyJalon(boolean neutro, double t) {
        beyPose p = beyArmar(new beyPt(0, -0.26), -8, new beyPt(0.22, 0), null, ADELANTE, ADELANTE);
        beyPieEnSuelo(p);
        beyPt mano = p.beyCuello.mas(beyLerp(0.14, 0.07, t), beyLerp(-0.31, 0.10, t));
        beyBrazo(p, mano, ADELANTE);
        beyPt polea = new beyPt(0.34, -1.30);
        double torreX = 0.52;

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, new beyPt(-0.14, -0.20), new beyPt(0.10, -0.20), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.12, -0.20), new beyPt(-0.12, 0), 0));
        beyPt rodillo = beyPt.lerp(p.beyCadera, p.beyRodilla, 0.80).mas(0.0, -0.075);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, rodillo.mas(-0.03, 0), rodillo.mas(0.03, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, rodillo.mas(0.03, 0), new beyPt(torreX, rodillo.y() - 0.02), 0));

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(torreX, 0), new beyPt(torreX, -1.40), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(torreX, -1.40), new beyPt(polea.x(), -1.40), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(polea.x(), -1.40), polea, 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.20, 0), new beyPt(0.74, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(torreX + 0.07, -1.18), new beyPt(torreX + 0.23, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.CABLE, polea, mano, 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.POLEA, polea, null, 0.035));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, mano, null, neutro ? 0.026 : 0.02));
        return p;
    }

    private static beyPose beyRemo(beyImplemento imp, beyVarianteMovimiento var, double t) {
        if (var == beyVarianteMovimiento.REMO_INVERTIDO) {
            return beyRemoInvertido(t);
        }
        if (var == beyVarianteMovimiento.REMO_SENTADO) {
            return beyRemoSentado(imp, t);
        }
        beyPose p = beyArmar(new beyPt(-0.12, -0.42), 62, new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(beyLerp(0.0, -0.12, t), beyLerp(0.31, 0.12, t)), ATRAS);
        beyCarga(p, imp, p.beyMuneca, 0.10);
        return p;
    }

    private static beyPose beyRemoSentado(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        boolean maquina = imp == beyImplemento.MAQUINA;
        p.beyCadera = new beyPt(0, -0.30);
        double inc = maquina ? 12 : -4;
        p.beyCuello = beyArriba(p.beyCadera, TORSO, inc);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, inc);
        p.beyTobillo = new beyPt(0.24, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(beyLerp(0.32, 0.10, t), 0.08), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyMuneca, null, 0.024));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(-0.10, 0.075), p.beyCadera.mas(0.12, 0.075), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.95, 0), new beyPt(0.60, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(0.28, 0), new beyPt(0.28, -0.14), 0));
        if (maquina) {
            beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
            beyPt frente = new beyPt(-esp.x(), -esp.y());
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCuello.mas(frente.x() * 0.092 - 0.0, frente.y() * 0.092 + 0.04),
                    beyPt.lerp(p.beyCuello, p.beyCadera, 0.30).mas(frente.x() * 0.092, frente.y() * 0.092), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(0.16, -0.30), new beyPt(0.16, -0.62), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(-0.92, -0.86), new beyPt(-0.77, 0), 0));
        } else {
            beyPt polea = new beyPt(0.74, p.beyCuello.y() + 0.08);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.CABLE, p.beyMuneca, polea, 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.POLEA, polea, null, 0.035));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, polea, new beyPt(polea.x(), 0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.14, -0.30), new beyPt(-0.14, 0), 0));
        }
        return p;
    }

    private static beyPose beyRemoInvertido(double t) {
        beyPose p = new beyPose();
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.beyTobillo = new beyPt(0.66, -0.03);
        double theta = Math.toRadians(beyLerp(28, 42, t));
        p.beyCuello = new beyPt(p.beyTobillo.x - largo * Math.cos(theta), p.beyTobillo.y - largo * Math.sin(theta));
        p.beyCadera = beyPt.lerp(p.beyTobillo, p.beyCuello, (MUSLO + PANTORRILLA) / largo);
        p.beyRodilla = beyPt.lerp(p.beyTobillo, p.beyCadera, 0.5);
        p.beyPunta = new beyPt(p.beyTobillo.x + 0.02, 0);
        beyPt dir = beyUnitario(p.beyCadera, p.beyCuello);
        p.beyCabeza = p.beyCuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        beyPt borde = new beyPt(0.03, -0.70);
        beyBrazo(p, borde, ADELANTE);

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.MESA, new beyPt(-0.34, -0.745), new beyPt(0.98, -0.745), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, borde, null, 0.022));
        return p;
    }

    private static beyPose beyDominada(double t) {
        beyPt barra = new beyPt(0.05, -1.30);
        beyPose p = new beyPose();
        p.beyCuello = new beyPt(barra.x() - 0.04, barra.y() + beyLerp(0.31, 0.06, t));
        p.beyCadera = p.beyCuello.mas(0.0, TORSO);
        p.beyCabeza = p.beyCuello.mas(0.02, -CABEZA_DISTANCIA);
        p.beyTobillo = beyAlcance(p.beyCadera, p.beyCadera.mas(-0.08, 0.47), MUSLO + PANTORRILLA);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ATRAS);
        p.beyPunta = p.beyTobillo.mas(0.0, 0.07);
        beyBrazo(p, barra, ADELANTE);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BARRA, barra, null, 0.02));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, barra.mas(0.10, 0), barra.mas(0.32, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, barra.mas(0.32, 0), new beyPt(barra.x() + 0.32, 0), 0));
        return p;
    }

    private static beyPose beyCurlToalla(double t) {
        beyPose p = beyArmar(new beyPt(0, -0.47), 0, new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        p.beyCodo = p.beyCuello.mas(0.012, BRAZO);
        double psi = Math.toRadians(90 + beyLerp(-1.6, 1.6, t));
        p.beyMuneca = p.beyCodo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.TOALLA, new beyPt(-0.12, -0.012), new beyPt(0.20, -0.012), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.TOALLA, p.beyMuneca, new beyPt(0.20, -0.012), 0));
        return p;
    }

    private static beyPose beyCurlInclinado(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0, -0.32);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -55);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -55);
        p.beyTobillo = new beyPt(0.22, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        p.beyCodo = p.beyCuello.mas(0.0, BRAZO);
        double psi = Math.toRadians(beyLerp(8, 150, t));
        p.beyMuneca = p.beyCodo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        beyCarga(p, imp, p.beyMuneca, 0.065);
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, p.beyCadera.mas(esp.x() * 0.085 - 0.04, esp.y() * 0.085 + 0.02),
                p.beyCuello.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.02), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, new beyPt(-0.10, -0.25), new beyPt(0.10, -0.25), 0));
        return p;
    }

    private static beyPose beyCurl(beyImplemento imp, double t) {
        beyPose p = beyArmar(new beyPt(0, -0.47), beyLerp(0, -4, t), new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        p.beyCodo = p.beyCuello.mas(0.02, BRAZO);
        double psi = Math.toRadians(beyLerp(8, 150, t));
        p.beyMuneca = p.beyCodo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        if (imp == beyImplemento.POLEA) {
            beyPt polea = new beyPt(0.46, -0.06);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.CABLE, p.beyMuneca, polea, 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.POLEA, polea, null, 0.035));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, polea, polea.mas(0.14, 0.0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(0.60, 0), new beyPt(0.60, -0.95), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(0.64, -0.95), new beyPt(0.79, -0.20), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyMuneca, null, 0.022));
        } else {
            beyCarga(p, imp, p.beyMuneca, 0.065);
        }
        return p;
    }

    private static beyPose beyTriceps(beyImplemento imp, beyVarianteMovimiento var, double t) {
        beyPose p = beyArmar(new beyPt(0, -0.47), 0, new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        if (var == beyVarianteMovimiento.TRICEPS_SOBRE_CABEZA) {
            p.beyCodo = p.beyCuello.mas(0.0, -BRAZO);
            double psi = Math.toRadians(beyLerp(-150, -8, t));
            p.beyMuneca = p.beyCodo.mas(Math.sin(psi) * ANTEBRAZO, -Math.cos(psi) * ANTEBRAZO);
            beyCarga(p, imp, p.beyMuneca, 0.07);
            return p;
        }
        p.beyCodo = p.beyCuello.mas(0.03, BRAZO);
        double psi = Math.toRadians(beyLerp(105, 4, t));
        p.beyMuneca = p.beyCodo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        beyPt polea = new beyPt(0.07, -1.22);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.CABLE, p.beyMuneca, polea, 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.POLEA, polea, null, 0.035));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, polea, polea.mas(-0.36, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, polea.mas(-0.36, 0), new beyPt(polea.x() - 0.36, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(polea.x() - 0.60, -1.0), new beyPt(polea.x() - 0.45, -0.30), 0));

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyMuneca, null, 0.022));
        return p;
    }

    private static beyPose beyPesoMuerto(beyImplemento imp, double t) {
        beyPt cadera = new beyPt(beyLerp(-0.20, -0.01, t), -beyLerp(0.40, 0.47, t));
        beyPose p = beyArmar(cadera, beyLerp(58, 3, t), new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(0.0, 0.31), ATRAS);
        beyCarga(p, imp, p.beyMuneca, 0.12);
        return p;
    }

    private static beyPose beyHipThrust(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        p.beyCuello = new beyPt(-0.30, -0.40);
        double phi = Math.toRadians(beyLerp(35, -8, t));
        p.beyCadera = new beyPt(p.beyCuello.x + TORSO * Math.cos(phi), p.beyCuello.y + TORSO * Math.sin(phi));
        p.beyCabeza = p.beyCuello.mas(-0.09, -0.03);
        p.beyTobillo = new beyPt(0.17, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCadera.mas(0.02, -0.04), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(-0.60, -0.335), new beyPt(-0.22, -0.335), 0));
        beyCarga(p, imp, p.beyCadera.mas(0.0, -0.075), 0.09);
        return p;
    }

    private static beyPose beyPuente(double t) {
        beyPose p = new beyPose();
        p.beyCuello = new beyPt(-0.32, -0.058);
        double phi = Math.toRadians(beyLerp(0, -32, t));
        p.beyCadera = new beyPt(p.beyCuello.x + TORSO * Math.cos(phi), p.beyCuello.y + TORSO * Math.sin(phi));
        p.beyCabeza = p.beyCuello.mas(-CABEZA_DISTANCIA, 0.0);
        p.beyTobillo = new beyPt(0.18, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, new beyPt(p.beyCuello.x + 0.32, -0.02), ATRAS);
        return p;
    }

    private static beyPose beyPressHombro(beyImplemento imp, double t) {
        beyPose p = beyArmar(new beyPt(0, -0.47), beyLerp(0, -3, t), new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(0.01, -beyLerp(0.08, 0.31, t)), ATRAS);
        beyCarga(p, imp, p.beyMuneca, 0.08);
        return p;
    }

    private static beyPose beyPressHombroSentado(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0, -0.30);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -2);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -2);
        p.beyTobillo = new beyPt(0.21, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(-0.10, 0.075), p.beyCadera.mas(0.12, 0.075), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(esp.x() * 0.085, esp.y() * 0.085),
                p.beyCuello.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.05), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.95, 0), new beyPt(0.16, 0), 0));
        if (imp == beyImplemento.MAQUINA) {
            beyPt pivote = new beyPt(-0.26, -0.46);
            double angulo = Math.toRadians(beyLerp(24, 56, t));
            beyPt agarre = new beyPt(pivote.x() + 0.42 * Math.cos(angulo), pivote.y() - 0.42 * Math.sin(angulo));
            beyBrazo(p, agarre, ATRAS);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.20, 0), new beyPt(-0.20, -1.0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(-0.92, -0.86), new beyPt(-0.77, 0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.845, -0.86), new beyPt(-0.20, -1.0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PALANCA, pivote, agarre, 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, pivote, null, 0.022));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, agarre, null, 0.026));
        } else {
            beyBrazo(p, p.beyCuello.mas(0.01, -beyLerp(0.08, 0.31, t)), ATRAS);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.20, 0), new beyPt(-0.20, -0.95), 0));
            beyCarga(p, imp, p.beyMuneca, 0.08);
        }
        return p;
    }

    private static beyPose beyZancada(beyImplemento imp, beyVarianteMovimiento var, double t) {
        beyPt cadera = new beyPt(-0.03, -beyLerp(0.45, 0.27, t));
        beyPose p = beyArmar(cadera, 2, new beyPt(0.24, 0), null, ADELANTE, ATRAS);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCuello.mas(0.05, 0.25), ATRAS);
        beyCarga(p, imp, p.beyMuneca, 0.08);
        if (var == beyVarianteMovimiento.ZANCADA_BULGARA) {
            p.beyTobillo2 = beyAlcance(cadera, new beyPt(-0.40, -0.42), MUSLO + PANTORRILLA);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(-0.58, -0.375), new beyPt(-0.34, -0.375), 0));
            p.beyPunta2 = p.beyTobillo2.mas(-0.08, 0.0);
        } else {
            p.beyTobillo2 = beyAlcance(cadera, new beyPt(-0.30, -0.07), MUSLO + PANTORRILLA);
            p.beyPunta2 = new beyPt(p.beyTobillo2.x, 0);
        }
        p.beyRodilla2 = beyArticulacion(cadera, p.beyTobillo2, MUSLO, PANTORRILLA, ADELANTE);
        return p;
    }

    private static beyPose beyTalones(beyImplemento imp, double t) {
        double alza = t * 0.07;
        beyPose p = beyArmar(new beyPt(0, -0.47 - alza), 0, new beyPt(0, -alza), null, ADELANTE, ATRAS);
        beyBrazo(p, p.beyCuello.mas(0.02, 0.30), ATRAS);
        p.beyPunta = new beyPt(p.beyTobillo.x + PIE, 0);
        if (imp == beyImplemento.MAQUINA) {
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCuello.mas(-0.07, -0.035), p.beyCuello.mas(0.06, -0.035), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, p.beyCuello.mas(-0.10, -0.035), p.beyCuello.mas(-0.10, -0.50), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.22, 0), new beyPt(0.22, 0), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PLATAFORMA, new beyPt(-0.07, -0.0), new beyPt(0.20, -0.0), 0));
        } else if (imp == beyImplemento.BARRA) {
            beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.DISCO, p.beyCuello.mas(esp.x() * 0.045, esp.y() * 0.045), null, 0.09));
        } else if (imp == beyImplemento.MANCUERNA) {
            beyCarga(p, imp, p.beyMuneca, 0.08);
        }
        return p;
    }

    private static beyPose beySentadoPierna(beyImplemento imp, double t, boolean extension) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0, -0.42);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, -4);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, -4);
        p.beyRodilla = new beyPt(MUSLO, -0.42);
        double fi = Math.toRadians(extension ? beyLerp(90, 6, t) : beyLerp(8, 100, t));
        p.beyTobillo = new beyPt(p.beyRodilla.x + PANTORRILLA * Math.cos(fi), p.beyRodilla.y + PANTORRILLA * Math.sin(fi));
        beyPt frente = new beyPt(Math.sin(fi), -Math.cos(fi));
        p.beyPunta = new beyPt(p.beyTobillo.x + frente.x() * 0.08, p.beyTobillo.y + frente.y() * 0.08);
        beyBrazo(p, p.beyCadera.mas(0.11, -0.10), ATRAS);
        beyPt esp = beyNormalEspalda(p.beyCadera, p.beyCuello);

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, new beyPt(-0.10, -0.35), new beyPt(0.20, -0.35), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyCadera.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.02),
                p.beyCuello.mas(esp.x() * 0.085, esp.y() * 0.085), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.95, 0), new beyPt(0.40, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.18, -0.35), new beyPt(-0.18, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(-0.92, -0.86), new beyPt(-0.77, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.845, -0.86), new beyPt(-0.28, -0.86), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.28, -0.86), p.beyCuello.mas(esp.x() * 0.13, esp.y() * 0.13 - 0.05), 0));

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyRodilla.mas(-0.09, -0.075), p.beyRodilla.mas(-0.03, -0.075), 0));
        double lado = extension ? 1 : 1;
        beyPt rodilloTobillo = p.beyTobillo.mas(frente.x() * 0.05 * lado, frente.y() * 0.05 * lado);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PALANCA, p.beyRodilla.mas(-0.03, 0.06),
                p.beyTobillo.mas(-frente.x() * 0.06, -frente.y() * 0.06), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.MANCUERNA, rodilloTobillo, null, 0.035));
        return p;
    }

    private static beyPose beyTalonesSentado(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        double alza = t * 0.04;
        p.beyCadera = new beyPt(0, -0.46 - alza);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, 0);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, 0);
        p.beyRodilla = new beyPt(MUSLO, p.beyCadera.y);
        p.beyTobillo = new beyPt(MUSLO, p.beyRodilla.y + PANTORRILLA);
        p.beyPunta = new beyPt(p.beyTobillo.x + PIE, -0.15);
        beyBrazo(p, p.beyRodilla.mas(-0.04, -0.05), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, new beyPt(-0.10, -0.39), new beyPt(0.14, -0.39), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(-0.10, -0.39), new beyPt(-0.10, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyRodilla.mas(-0.10, -0.085), p.beyRodilla.mas(0.03, -0.085), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PALANCA, p.beyRodilla.mas(0.03, -0.085), p.beyRodilla.mas(0.20, -0.085), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PLATAFORMA, new beyPt(0.10, -0.14), new beyPt(0.36, -0.14), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(0.36, -0.14), new beyPt(0.36, 0), 0));
        return p;
    }

    private static beyPose beyCurlFemoral(beyImplemento imp, double t) {
        beyPose p = new beyPose();
        p.beyPechoInvertido = true;
        p.beyCadera = new beyPt(0, -0.33);
        p.beyCuello = new beyPt(-TORSO, -0.33);
        p.beyCabeza = new beyPt(-TORSO - CABEZA_DISTANCIA, -0.33);
        p.beyRodilla = new beyPt(0.24, -0.33);
        double phi = Math.toRadians(beyLerp(4, 100, t));
        p.beyTobillo = new beyPt(p.beyRodilla.x + PANTORRILLA * Math.cos(phi), p.beyRodilla.y - PANTORRILLA * Math.sin(phi));
        p.beyPunta = new beyPt(p.beyTobillo.x - Math.sin(phi) * 0.07, p.beyTobillo.y - Math.cos(phi) * 0.07);
        beyBrazo(p, new beyPt(p.beyCuello.x - 0.05, -0.12), ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(-0.46, -0.25), new beyPt(0.34, -0.25), 0));
        if (imp == beyImplemento.MAQUINA) {
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.MANCUERNA, p.beyTobillo, null, 0.04));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(0.52, -0.80), new beyPt(0.67, -0.20), 0));
        }
        return p;
    }

    private static beyPose beyCrunch(double t) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0.05, -0.058);
        double phi = Math.toRadians(beyLerp(3, 40, t));
        p.beyCuello = new beyPt(p.beyCadera.x - TORSO * Math.cos(phi), p.beyCadera.y - TORSO * Math.sin(phi));
        double phiCabeza = Math.toRadians(beyLerp(3, 40, t) + 10);
        p.beyCabeza = new beyPt(p.beyCuello.x - CABEZA_DISTANCIA * Math.cos(phiCabeza),
                p.beyCuello.y - CABEZA_DISTANCIA * Math.sin(phiCabeza));
        p.beyTobillo = new beyPt(0.32, 0);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, p.beyCabeza.mas(0.03, -0.02), ATRAS);
        return p;
    }

    private static beyPose beyCrunchPolea(double t) {
        beyPose p = new beyPose();
        p.beyRodilla = new beyPt(0.10, -0.03);
        p.beyCadera = new beyPt(0.07, -0.265);
        p.beyTobillo = new beyPt(-0.14, -0.03);
        p.beyPunta = new beyPt(-0.21, -0.012);
        double inc = beyLerp(8, 62, t);
        p.beyCuello = beyArriba(p.beyCadera, TORSO, inc);
        p.beyCabeza = beyArriba(p.beyCuello, CABEZA_DISTANCIA, inc * 0.8);
        beyBrazo(p, p.beyCabeza.mas(0.09, 0.03), ATRAS);
        beyPt polea = new beyPt(0.55, -1.30);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.CABLE, p.beyMuneca, polea, 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.POLEA, polea, null, 0.035));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, polea, new beyPt(0.66, -1.30), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, new beyPt(0.66, -1.30), new beyPt(0.66, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.PILA, new beyPt(0.70, -1.10), new beyPt(0.85, -0.35), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.AGARRE, p.beyMuneca, null, 0.022));
        return p;
    }

    private static beyPose beyCaidaDeRodillas(double t, boolean rueda) {
        beyPose p = new beyPose();
        double grados = rueda ? beyLerp(50, 80, t) : beyLerp(8, 72, t);
        double r = Math.toRadians(grados);
        p.beyRodilla = new beyPt(0.0, -0.03);
        p.beyTobillo = new beyPt(-PANTORRILLA, -0.03);
        p.beyPunta = new beyPt(p.beyTobillo.x - 0.07, -0.012);
        p.beyCadera = new beyPt(p.beyRodilla.x + MUSLO * Math.sin(r), p.beyRodilla.y - MUSLO * Math.cos(r));
        p.beyCuello = new beyPt(p.beyCadera.x + TORSO * Math.sin(r), p.beyCadera.y - TORSO * Math.cos(r));
        p.beyCabeza = new beyPt(p.beyCuello.x + CABEZA_DISTANCIA * Math.sin(r * 0.8), p.beyCuello.y - CABEZA_DISTANCIA * Math.cos(r * 0.8));
        if (rueda) {
            beyPt manoRueda = new beyPt(p.beyCuello.x + 0.02, -0.07);
            beyBrazo(p, manoRueda, ADELANTE);
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.DISCO, p.beyMuneca.mas(0.0, 0.0), null, 0.05));
        } else {
            beyBrazo(p, new beyPt(p.beyCuello.x + 0.04, -0.02), ATRAS);

            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.ASIENTO, p.beyTobillo.mas(-0.10, -0.06), p.beyTobillo.mas(0.05, -0.06), 0));
            p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, p.beyTobillo.mas(-0.10, -0.06), new beyPt(p.beyTobillo.x - 0.10, 0), 0));
        }
        return p;
    }

    private static beyPose beyElevacionPiernas(double t) {
        beyPose p = new beyPose();
        p.beyCadera = new beyPt(0.05, -0.058);
        p.beyCuello = new beyPt(p.beyCadera.x - TORSO, -0.058);
        p.beyCabeza = new beyPt(p.beyCuello.x - CABEZA_DISTANCIA, -0.058);
        double phi = Math.toRadians(beyLerp(8, 85, t));
        double largo = MUSLO + PANTORRILLA - 0.002;
        p.beyTobillo = new beyPt(p.beyCadera.x + largo * Math.cos(phi), p.beyCadera.y - largo * Math.sin(phi));
        p.beyRodilla = beyPt.lerp(p.beyCadera, p.beyTobillo, MUSLO / (MUSLO + PANTORRILLA));
        p.beyPunta = new beyPt(p.beyTobillo.x - Math.sin(phi) * 0.08, p.beyTobillo.y - Math.cos(phi) * 0.08);
        beyBrazo(p, new beyPt(p.beyCuello.x + 0.31, -0.026), ATRAS);
        return p;
    }

    private static beyPose beyFondos(double t) {
        beyPt agarre = new beyPt(0.0, -0.95);
        beyPose p = new beyPose();
        p.beyCuello = new beyPt(agarre.x() - 0.03, agarre.y() - beyLerp(0.02, 0.29, t));
        p.beyCadera = p.beyCuello.mas(-0.03, TORSO);
        p.beyCabeza = p.beyCuello.mas(0.025, -CABEZA_DISTANCIA);
        p.beyTobillo = beyAlcance(p.beyCadera, p.beyCadera.mas(-0.13, 0.40), MUSLO + PANTORRILLA);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ATRAS);
        p.beyPunta = p.beyTobillo.mas(0.0, 0.07);
        beyBrazo(p, agarre, ATRAS);

        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, agarre.mas(0.03, 0), agarre.mas(0.44, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, agarre.mas(0.44, 0), new beyPt(agarre.x() + 0.44, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, agarre.mas(-0.42, 0), agarre.mas(-0.30, 0), 0));
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.RIEL, agarre.mas(-0.42, 0), new beyPt(agarre.x() - 0.42, 0), 0));
        return p;
    }

    private static beyPose beyFondosSilla(double t) {
        double alturaSilla = 0.38;
        beyPt agarre = new beyPt(-0.04, -alturaSilla);
        beyPose p = new beyPose();

        p.beyCuello = new beyPt(agarre.x() + 0.10, agarre.y() - beyLerp(0.10, 0.285, t));
        p.beyCadera = p.beyCuello.mas(0.0, TORSO);
        p.beyCabeza = p.beyCuello.mas(0.02, -CABEZA_DISTANCIA);
        p.beyTobillo = beyAlcance(p.beyCadera, new beyPt(0.34, 0), MUSLO + PANTORRILLA);
        p.beyRodilla = beyArticulacion(p.beyCadera, p.beyTobillo, MUSLO, PANTORRILLA, ADELANTE);
        beyPieEnSuelo(p);
        beyBrazo(p, agarre, ATRAS);
        p.beyEquipo.add(new beyEquipo(beyTipoEquipo.BANCO, new beyPt(-0.40, -alturaSilla + 0.025), new beyPt(-0.02, -alturaSilla + 0.025), 0));
        return p;
    }

    private static beyPose beySuperman(double t) {
        beyPose p = new beyPose();
        p.beyPechoInvertido = true;
        p.beyCadera = new beyPt(0.02, -0.06);
        double a = Math.toRadians(beyLerp(0, 14, t));
        p.beyCuello = new beyPt(p.beyCadera.x - TORSO * Math.cos(a), p.beyCadera.y - TORSO * Math.sin(a));
        p.beyCabeza = new beyPt(p.beyCuello.x - CABEZA_DISTANCIA * Math.cos(a + 0.1), p.beyCuello.y - CABEZA_DISTANCIA * Math.sin(a + 0.1));
        double b = Math.toRadians(beyLerp(2, 20, t));
        double c = Math.toRadians(beyLerp(0, 14, t));
        double largoPierna = MUSLO + PANTORRILLA - 0.002;
        p.beyTobillo = new beyPt(p.beyCadera.x + largoPierna * Math.cos(c), p.beyCadera.y - largoPierna * Math.sin(c));
        p.beyRodilla = beyPt.lerp(p.beyCadera, p.beyTobillo, MUSLO / (MUSLO + PANTORRILLA));
        p.beyPunta = new beyPt(p.beyTobillo.x + 0.05, p.beyTobillo.y - 0.01);
        p.beyMuneca = new beyPt(p.beyCuello.x - (BRAZO + ANTEBRAZO - 0.002) * Math.cos(b),
                p.beyCuello.y - (BRAZO + ANTEBRAZO - 0.002) * Math.sin(b));
        p.beyCodo = beyPt.lerp(p.beyCuello, p.beyMuneca, BRAZO / (BRAZO + ANTEBRAZO));
        return p;
    }

    private static beyPose beyBurpee(double t) {
        double manoX = 0.19;
        beyPt mano = new beyPt(manoX, -0.02);
        if (t < 0.30) {
            double u = beySuave(t / 0.30);
            beyPt cadera = new beyPt(beyLerp(0, -0.16, u), -beyLerp(0.47, 0.27, u));
            beyPose p = beyArmar(cadera, beyLerp(3, 85, u), new beyPt(0, 0), null, ADELANTE, ATRAS);
            beyPieEnSuelo(p);
            beyBrazo(p, beyPt.lerp(p.beyCuello.mas(0.02, -0.31), mano, u), ATRAS);
            return p;
        }
        beyPose plancha = beyTablon(0.318, false, manoX);
        beyPose sentadilla = beyBurpeeSentadilla();
        if (t < 0.62) {
            double v = beySuave((t - 0.30) / 0.32);
            beyPose p = new beyPose();
            p.beyCuello = beyPt.lerp(sentadilla.beyCuello, plancha.beyCuello, v);
            beyPt cadLerp = beyPt.lerp(sentadilla.beyCadera, plancha.beyCadera, v);
            beyPt dir = beyUnitario(p.beyCuello, cadLerp);
            p.beyCadera = new beyPt(p.beyCuello.x() + dir.x() * TORSO, p.beyCuello.y() + dir.y() * TORSO);
            beyPt tobObj = beyPt.lerp(new beyPt(0, 0), plancha.beyTobillo, v).mas(0, -0.12 * Math.sin(Math.PI * v));
            p.beyTobillo = beyAlcance(p.beyCadera, tobObj, MUSLO + PANTORRILLA);
            p.beyRodilla = beyRodillaMasAlta(p.beyCadera, p.beyTobillo);
            p.beyPunta = new beyPt(p.beyTobillo.x - 0.035 * v + PIE * (1 - v), Math.min(0, p.beyTobillo.y + 0.03));
            beyPt dc = beyUnitario(p.beyCadera, p.beyCuello);
            p.beyCabeza = p.beyCuello.mas(dc.x() * CABEZA_DISTANCIA, dc.y() * CABEZA_DISTANCIA);
            beyBrazo(p, mano, ATRAS);
            return p;
        }
        double w = beySuave((t - 0.62) / 0.38);
        beyPose p = beyTablon(beyLerp(0.318, 0.15, w), false, manoX);
        return p;
    }

    private static beyPose beyBurpeeSentadilla() {
        beyPt cadera = new beyPt(-0.16, -0.27);
        beyPose p = beyArmar(cadera, 85, new beyPt(0, 0), null, ADELANTE, ATRAS);
        beyBrazo(p, new beyPt(0.19, -0.02), ATRAS);
        return p;
    }
}
