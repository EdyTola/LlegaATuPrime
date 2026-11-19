package com.symmetry.desktop.util;

import com.symmetry.desktop.model.Implemento;
import com.symmetry.desktop.model.PatronMovimiento;
import com.symmetry.desktop.model.VarianteMovimiento;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinematica 2D del modelo humano: calcula la postura (posicion de cada articulacion) y el
 * equipamiento de cada ejercicio para un progreso {@code t} del movimiento (0..1).
 *
 * <p>Convenciones: unidades de mundo con el alto total del cuerpo = 1.0; el eje Y crece hacia
 * abajo y el suelo esta en {@code y = 0} (todo punto del cuerpo debe cumplir {@code y <= 0});
 * el modelo mira hacia +X. Rodillas y codos se resuelven con cinematica inversa de dos
 * segmentos, de modo que las extremidades nunca se desconectan ni cambian de longitud.</p>
 *
 * <p>Esta clase no depende de JavaFX: puede auditarse numericamente (longitudes, suelo,
 * continuidad) sin abrir ninguna ventana.</p>
 */
public final class Biomecanica2D {

    // --- Proporciones del cuerpo (alto total = 1.0 unidad de mundo) ---
    public static final double TORSO = 0.30;
    public static final double MUSLO = 0.24;
    public static final double PANTORRILLA = 0.24;
    public static final double BRAZO = 0.17;
    public static final double ANTEBRAZO = 0.15;
    public static final double PIE = 0.09;
    public static final double CABEZA_DISTANCIA = 0.095;
    /**
     * Plano del suelo en coordenadas de mundo: el tobillo se modela en y = 0 y la suela del
     * zapato mide ~2 cm, por lo que el suelo visible esta en y = SUELO. Todo punto del cuerpo
     * (mas su radio) debe quedar por encima de este valor.
     */
    public static final double SUELO = 0.022;

    /** Signo de flexion: hacia donde apuntan la rodilla / el codo respecto a la linea del segmento. */
    static final int ADELANTE = -1;
    static final int ATRAS = 1;

    private Biomecanica2D() {
    }

    // ------------------------------------------------------------------
    // Tipos
    // ------------------------------------------------------------------

    public record Pt(double x, double y) {

        public Pt mas(double dx, double dy) {
            return new Pt(x + dx, y + dy);
        }

        public static Pt lerp(Pt a, Pt b, double t) {
            return new Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
        }

        public double distancia(Pt o) {
            return Math.hypot(x - o.x, y - o.y);
        }
    }

    /** Elementos de equipamiento que sabe dibujar el renderizador. */
    public enum TipoEquipo {
        /** Disco de una barra olimpica visto de canto (radio). */
        DISCO,
        /** Cabeza hexagonal de mancuerna (radio). */
        MANCUERNA,
        /** Banco con patas hasta el suelo (a-b es la tapizada). */
        BANCO,
        /** Almohadilla de asiento o respaldo de maquina (a-b). */
        ASIENTO,
        /** Pila de placas de una maquina (a y b son esquinas opuestas). */
        PILA,
        /** Estructura rigida (poste, riel, base) de a hasta b. */
        RIEL,
        /** Brazo articulado de una maquina (a-b); se dibuja como estructura, detras del cuerpo. */
        PALANCA,
        /** Plataforma de pies de la prensa (a-b). */
        PLATAFORMA,
        /** Cable de a hasta b. */
        CABLE,
        /** Polea (radio). */
        POLEA,
        /** Mesa resistente vista de lado: a-b es el tablero; las patas bajan al suelo, retiradas de los extremos. */
        MESA,
        /** Toalla: tramo plano en el suelo o tira tensa entre la mano y el suelo (a-b). */
        TOALLA,
        /** Barra fija o punto de agarre (radio). */
        BARRA,
        /** Empunadura de maquina o cuerda (radio). */
        AGARRE
    }

    public record Equipo(TipoEquipo tipo, Pt a, Pt b, double radio) {
    }

    /** Postura completa del modelo en un instante. Los campos opcionales son {@code null} si no aplican. */
    public static final class Pose {
        public Pt cabeza;
        /** Hombro / base del cuello. */
        public Pt cuello;
        public Pt cadera;
        public Pt rodilla;
        public Pt tobillo;
        public Pt punta;
        public Pt codo;
        public Pt muneca;
        /** Pierna lejana (zancadas); si es null se dibuja un reflejo tenue de la cercana. */
        public Pt rodilla2;
        public Pt tobillo2;
        public Pt punta2;
        /** true cuando el pecho mira al lado contrario del calculado por la espalda (boca abajo con la cabeza a la izquierda). */
        public boolean pechoInvertido;
        public final List<Equipo> equipo = new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // Utilidades geometricas
    // ------------------------------------------------------------------

    /** Punto a {@code largo} de {@code o}, con el angulo medido desde la vertical hacia arriba (+ = adelante). */
    static Pt arriba(Pt o, double largo, double grados) {
        double r = Math.toRadians(grados);
        return new Pt(o.x + largo * Math.sin(r), o.y - largo * Math.cos(r));
    }

    /** Limita un objetivo al alcance maximo de una extremidad de dos segmentos. */
    static Pt alcance(Pt origen, Pt objetivo, double maximo) {
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
    static Pt articulacion(Pt a, Pt b, double l1, double l2, int signo) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double d = Math.max(Math.abs(l1 - l2) + 0.001, Math.min(Math.hypot(dx, dy), l1 + l2 - 0.0005));
        double base = Math.atan2(dy, dx);
        double coseno = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double ang = Math.acos(Math.max(-1, Math.min(1, coseno)));
        double th = base + signo * ang;
        return new Pt(a.x + l1 * Math.cos(th), a.y + l1 * Math.sin(th));
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double suave(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Vector unitario de a hacia b. */
    private static Pt unitario(Pt a, Pt b) {
        double d = Math.max(1e-9, a.distancia(b));
        return new Pt((b.x - a.x) / d, (b.y - a.y) / d);
    }

    /** Normal del lado de la espalda (opuesta al pecho) de un torso que va de la cadera al hombro. */
    static Pt normalEspalda(Pt cadera, Pt cuello) {
        Pt v = unitario(cadera, cuello);
        return new Pt(v.y, -v.x);
    }

    /** Punto de interseccion de dos circulos; {@code arriba} elige el de menor Y (mas alto en pantalla). */
    static Pt interseccion(Pt a, double ra, Pt b, double rb, boolean arriba) {
        double d = Math.max(1e-6, a.distancia(b));
        double x = (d * d + ra * ra - rb * rb) / (2 * d);
        double h = Math.sqrt(Math.max(0, ra * ra - x * x));
        double ux = (b.x - a.x) / d;
        double uy = (b.y - a.y) / d;
        Pt p1 = new Pt(a.x + ux * x - uy * h, a.y + uy * x + ux * h);
        Pt p2 = new Pt(a.x + ux * x + uy * h, a.y + uy * x - ux * h);
        return (p1.y < p2.y) == arriba ? p1 : p2;
    }

    /**
     * Completa una postura de pie o sentada: cuello por la inclinacion del torso, cabeza,
     * pierna (IK cadera-tobillo) y, si hay objetivo, brazo (IK hombro-muneca).
     */
    private static Pose armar(Pt cadera, double inclinacionTorso, Pt tobillo, Pt objetivoMuneca,
                              int signoRodilla, int signoCodo) {
        Pose p = new Pose();
        p.cadera = cadera;
        p.cuello = arriba(cadera, TORSO, inclinacionTorso);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, inclinacionTorso * 0.7);
        p.tobillo = alcance(cadera, tobillo, MUSLO + PANTORRILLA);
        p.rodilla = articulacion(cadera, p.tobillo, MUSLO, PANTORRILLA, signoRodilla);
        p.punta = new Pt(p.tobillo.x + PIE, Math.min(0, p.tobillo.y + 0.0));
        if (objetivoMuneca != null) {
            brazo(p, objetivoMuneca, signoCodo);
        }
        return p;
    }

    private static void brazo(Pose p, Pt objetivoMuneca, int signoCodo) {
        p.muneca = alcance(p.cuello, objetivoMuneca, BRAZO + ANTEBRAZO);
        p.codo = articulacion(p.cuello, p.muneca, BRAZO, ANTEBRAZO, signoCodo);
    }

    /** Rodilla de la pierna cadera-tobillo eligiendo la solucion mas alta (la rodilla nunca baja al suelo). */
    private static Pt rodillaMasAlta(Pt cadera, Pt tobillo) {
        Pt a = articulacion(cadera, tobillo, MUSLO, PANTORRILLA, ADELANTE);
        Pt b = articulacion(cadera, tobillo, MUSLO, PANTORRILLA, ATRAS);
        return a.y() <= b.y() ? a : b;
    }

    /** Pie plano apoyado en el suelo, apuntando hacia delante. */
    private static void pieEnSuelo(Pose p) {
        p.punta = new Pt(p.tobillo.x + PIE, 0);
    }

    /** Coloca la carga (disco de barra o mancuerna) en la mano segun el implemento. */
    private static void carga(Pose p, Implemento imp, Pt mano, double radioDisco) {
        switch (imp) {
            case BARRA -> p.equipo.add(new Equipo(TipoEquipo.DISCO, mano, null, radioDisco));
            case MANCUERNA -> p.equipo.add(new Equipo(TipoEquipo.MANCUERNA, mano, null,
                    Math.max(0.04, radioDisco * 0.42)));
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------
    // Punto de entrada
    // ------------------------------------------------------------------

    /**
     * Postura del ejercicio para el progreso {@code t} (0..1).
     *
     * @param patron   patron biomecanico base
     * @param imp      equipamiento a dibujar (nunca null)
     * @param variante variante de postura (nunca null)
     */
    public static Pose pose(PatronMovimiento patron, Implemento imp, VarianteMovimiento variante, double t) {
        t = Math.max(0, Math.min(1, t));
        Pose p = switch (patron) {
            case SENTADILLA -> sentadilla(imp, variante, t);
            case EXTENSION_CUADRICEPS -> sentadoPierna(imp, t, true);
            case APERTURA_PECHO -> new Pose();
            case PRESS_PLANO -> press(imp, variante, t, false);
            case PRESS_INCLINADO -> press(imp, variante, t, true);
            case FLEXION -> switch (variante) {
                case FLEXION_RODILLAS -> flexionRodillas(t);
                case FLEXION_DECLINADA -> tablonElevado(lerp(0.13, 0.318, t), -0.05);
                default -> tablon(lerp(0.13, 0.318, t), false, -0.05);
            };
            case PLANCHA -> variante == VarianteMovimiento.PLANCHA_ALTA
                    ? tablon(0.318 + 0.004 * (t - 0.5), false, -0.05)
                    : tablon(0.197 + 0.004 * (t - 0.5), true, -0.05);
            case JALON -> jalon(variante == VarianteMovimiento.JALON_NEUTRO, t);
            case REMO -> remo(imp, variante, t);
            case DOMINADA -> dominada(t);
            case CURL_BICEPS -> switch (variante) {
                case CURL_ISOMETRICO_TOALLA -> curlToalla(t);
                case CURL_INCLINADO -> curlInclinado(imp, t);
                default -> curl(imp, t);
            };
            case EXTENSION_TRICEPS -> triceps(imp, variante, t);
            case PESO_MUERTO -> pesoMuerto(imp, t);
            case HIP_THRUST -> hipThrust(imp, t);
            case PUENTE_GLUTEOS -> puente(t);
            case ELEVACION_LATERAL -> new Pose();
            case PRESS_HOMBRO -> switch (variante) {
                case FLEXION_PICA -> pica(t, false);
                case PICA_ELEVADA -> pica(t, true);
                case PRESS_HOMBRO_SENTADO -> pressHombroSentado(imp, t);
                default -> pressHombro(imp, t);
            };
            case ZANCADA -> zancada(imp, variante, t);
            case ELEVACION_TALONES -> variante == VarianteMovimiento.TALONES_SENTADO
                    ? talonesSentado(imp, t) : talones(imp, t);
            case CURL_FEMORAL -> switch (variante) {
                case CURL_FEMORAL_SENTADO -> sentadoPierna(imp, t, false);
                case CURL_FEMORAL_NORDICO -> caidaDeRodillas(t, false);
                default -> curlFemoral(imp, t);
            };
            case CRUNCH -> switch (variante) {
                case CRUNCH_POLEA -> crunchPolea(t);
                case RUEDA_ABDOMINAL -> caidaDeRodillas(t, true);
                default -> crunch(t);
            };
            case ELEVACION_PIERNAS -> elevacionPiernas(t);
            case FONDOS -> variante == VarianteMovimiento.FONDOS_SILLA ? fondosSilla(t) : fondos(t);
            case SUPERMAN -> superman(t);
            case BURPEE -> burpee(t);
        };
        return p;
    }

    // ------------------------------------------------------------------
    // Patrones
    // ------------------------------------------------------------------

    private static Pose sentadilla(Implemento imp, VarianteMovimiento var, double t) {
        if (var == VarianteMovimiento.PRENSA) {
            return prensa(t);
        }
        if (var == VarianteMovimiento.HACK) {
            return hack(t);
        }
        boolean guiada = imp == Implemento.MAQUINA;
        double inc = lerp(3, 42, t);
        double pieX = guiada ? 0.07 : 0.0;
        Pt cadera = new Pt(guiada ? 0.02 - TORSO * Math.sin(Math.toRadians(inc)) : lerp(0, -0.14, t),
                -lerp(0.47, 0.24, t));
        Pose p = armar(cadera, inc, new Pt(pieX, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        Pt esp = normalEspalda(p.cadera, p.cuello);
        Pt barra = p.cuello.mas(esp.x() * 0.045, esp.y() * 0.045);
        switch (imp) {
            case BARRA, MAQUINA -> {
                brazo(p, p.cuello.mas(0.03, 0.03), ATRAS);
                p.equipo.add(new Equipo(TipoEquipo.DISCO, barra, null, 0.10));
                if (guiada) {
                    p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(barra.x(), 0), new Pt(barra.x(), -1.12), 0));
                    p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(barra.x() - 0.30, 0), new Pt(barra.x() + 0.30, 0), 0));
                }
            }
            case MANCUERNA -> {
                // Sentadilla goblet: la carga se sujeta pegada al pecho.
                brazo(p, p.cuello.mas(0.10, 0.07), ATRAS);
                carga(p, imp, p.muneca, 0.09);
            }
            default -> brazo(p, p.cuello.mas(0.27, 0.03), ATRAS);
        }
        if (var == VarianteMovimiento.PISTOL) {
            // La pierna libre se extiende al frente, a la altura de la cadera.
            p.tobillo2 = new Pt(p.cadera.x + MUSLO + PANTORRILLA - 0.004, p.cadera.y);
            p.rodilla2 = Pt.lerp(p.cadera, p.tobillo2, MUSLO / (MUSLO + PANTORRILLA));
            p.punta2 = p.tobillo2.mas(0.0, -0.07);
        }
        return p;
    }

    /**
     * Sentadilla hack: la espalda descansa en un respaldo inclinado 18 grados y el cuerpo se
     * desliza por las guias; los pies quedan adelantados sobre la plataforma.
     */
    private static Pose hack(double t) {
        double beta = Math.toRadians(18);
        Pt subida = new Pt(-Math.sin(beta), -Math.cos(beta));
        Pt cadera0 = new Pt(-0.08, -0.45);
        double descenso = 0.26 * t;
        Pose p = new Pose();
        p.cadera = new Pt(cadera0.x() - subida.x() * descenso, cadera0.y() - subida.y() * descenso);
        p.cuello = arriba(p.cadera, TORSO, -18);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -18);
        p.tobillo = alcance(p.cadera, new Pt(0.10, 0), MUSLO + PANTORRILLA);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(0.07, 0.10), ATRAS);
        Pt esp = normalEspalda(p.cadera, p.cuello);
        Pt bajo = new Pt(cadera0.x() + subida.x() * -0.45, cadera0.y() + subida.y() * -0.45);
        Pt alto = new Pt(cadera0.x() + subida.x() * 0.62, cadera0.y() + subida.y() * 0.62);
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, bajo.mas(esp.x() * 0.085, esp.y() * 0.085),
                alto.mas(esp.x() * 0.085, esp.y() * 0.085), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, bajo.mas(esp.x() * 0.15, esp.y() * 0.15),
                alto.mas(esp.x() * 0.15, esp.y() * 0.15), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, bajo.mas(esp.x() * 0.15, esp.y() * 0.15),
                new Pt(bajo.x() + esp.x() * 0.15, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.40, 0), new Pt(0.42, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PLATAFORMA, new Pt(0.0, 0), new Pt(0.32, 0), 0));
        // Hombreras sobre los hombros, pegadas al respaldo.
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.cuello.mas(esp.x() * 0.06, esp.y() * 0.06), null, 0.03));
        return p;
    }

    /** Prensa de piernas: espalda reclinada en el asiento y empuje de la plataforma. */
    private static Pose prensa(double t) {
        Pose p = new Pose();
        double angulo = 38;
        Pt u = new Pt(Math.cos(Math.toRadians(angulo)), -Math.sin(Math.toRadians(angulo)));
        Pt n = new Pt(Math.sin(Math.toRadians(angulo)), Math.cos(Math.toRadians(angulo)));
        p.cadera = new Pt(-0.30, -0.20);
        p.cuello = arriba(p.cadera, TORSO, -55);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -55);
        double d = lerp(0.47, 0.28, t);
        p.tobillo = new Pt(p.cadera.x + d * u.x(), p.cadera.y + d * u.y());
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        p.punta = new Pt(p.tobillo.x - n.x() * 0.075, p.tobillo.y - n.y() * 0.075);
        brazo(p, p.cadera.mas(0.04, -0.13), ATRAS);

        Pt esp = normalEspalda(p.cadera, p.cuello);
        Pt centroPlataforma = new Pt(p.tobillo.x + u.x() * 0.045, p.tobillo.y + u.y() * 0.045);
        p.equipo.add(new Equipo(TipoEquipo.PLATAFORMA, centroPlataforma.mas(-n.x() * 0.12, -n.y() * 0.12),
                centroPlataforma.mas(n.x() * 0.12, n.y() * 0.12), 0));
        Pt finRiel = new Pt(p.cadera.x + 0.52 * u.x() + n.x() * 0.13, p.cadera.y + 0.52 * u.y() + n.y() * 0.13);
        Pt inicioRiel = new Pt(p.cadera.x - 0.10 + n.x() * 0.13, p.cadera.y + 0.0 + n.y() * 0.13);
        p.equipo.add(new Equipo(TipoEquipo.RIEL, inicioRiel, finRiel, 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, finRiel, new Pt(finRiel.x(), 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, inicioRiel, new Pt(inicioRiel.x(), 0), 0));
        // Asiento y respaldo siguen la espalda; el respaldo se desplaza hacia atras para no cruzar el torso.
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO,
                p.cadera.mas(esp.x() * 0.075 - 0.02, esp.y() * 0.075 + 0.0),
                p.cuello.mas(esp.x() * 0.075, esp.y() * 0.075), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(-0.10, 0.075), p.cadera.mas(0.11, 0.075), 0));
        return p;
    }

    private static Pose press(Implemento imp, VarianteMovimiento var, double t, boolean inclinado) {
        if (var == VarianteMovimiento.PRESS_SENTADO) {
            return pressSentado(t, inclinado ? 35 : 12);
        }
        Pose p = new Pose();
        double d = lerp(0.12, 0.31, t);
        if (!inclinado) {
            p.cadera = new Pt(0, -0.34);
            p.cuello = new Pt(-TORSO, -0.34);
            p.cabeza = new Pt(-TORSO - CABEZA_DISTANCIA, -0.34);
            p.tobillo = new Pt(0.24, 0);
            p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
            pieEnSuelo(p);
            brazo(p, new Pt(p.cuello.x + 0.02, p.cuello.y - d), ATRAS);
            p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.58, -0.265), new Pt(0.05, -0.265), 0));
        } else {
            p.cadera = new Pt(0, -0.34);
            p.cuello = arriba(p.cadera, TORSO, -50);
            p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -50);
            p.tobillo = new Pt(0.24, 0);
            p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
            pieEnSuelo(p);
            brazo(p, p.cuello.mas(0.643 * d, -0.766 * d), ATRAS);
            Pt esp = normalEspalda(p.cadera, p.cuello);
            p.equipo.add(new Equipo(TipoEquipo.BANCO, p.cadera.mas(esp.x() * 0.085 + 0.05, esp.y() * 0.085),
                    p.cuello.mas(esp.x() * 0.085 - 0.02, esp.y() * 0.085 - 0.03), 0));
        }
        carga(p, imp, p.muneca, 0.09);
        return p;
    }

    /** Press de pecho en maquina: sentado con respaldo y empuje perpendicular a la espalda. */
    private static Pose pressSentado(double t, double inclinacion) {
        Pose p = new Pose();
        p.cadera = new Pt(0, -0.30);
        p.cuello = arriba(p.cadera, TORSO, -inclinacion);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -inclinacion);
        p.tobillo = new Pt(0.21, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        double d = lerp(0.12, 0.31, t);
        double r = Math.toRadians(inclinacion);
        brazo(p, p.cuello.mas(Math.cos(r) * d, -Math.sin(r) * d), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.muneca, null, 0.024));
        marcoSentado(p, 0.085);
        return p;
    }

    /** Asiento, respaldo, pila de placas y estructura de una maquina con el usuario sentado. */
    private static void marcoSentado(Pose p, double separacion) {
        Pt esp = normalEspalda(p.cadera, p.cuello);
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(-0.10, 0.075), p.cadera.mas(0.12, 0.075), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO,
                p.cadera.mas(esp.x() * separacion, esp.y() * separacion),
                p.cuello.mas(esp.x() * separacion, esp.y() * separacion), 0));
        Pt tope = p.cuello.mas(esp.x() * (separacion + 0.04), esp.y() * (separacion + 0.04));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(p.cadera.x() - 0.16, 0), new Pt(tope.x(), tope.y() - 0.05), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.95, 0), new Pt(0.16, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(-0.92, -0.86), new Pt(-0.77, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.845, -0.86), new Pt(tope.x(), tope.y() - 0.05), 0));
    }

    /**
     * Postura de cuerpo recto apoyado en el suelo con manos y pies fijos (flexion y plancha).
     * El cuerpo se estira o encoge un par de por ciento al variar la altura, imperceptible.
     */
    private static Pose tablon(double alturaHombro, boolean antebrazos, double manoX) {
        Pose p = new Pose();
                p.tobillo = new Pt(manoX - 0.75, -0.03);
        p.cuello = new Pt(manoX, -alturaHombro);
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.cadera = Pt.lerp(p.tobillo, p.cuello, (MUSLO + PANTORRILLA) / largo);
        p.rodilla = Pt.lerp(p.tobillo, p.cadera, 0.5);
        p.punta = new Pt(p.tobillo.x - 0.035, 0);
        Pt dir = unitario(p.cadera, p.cuello);
        p.cabeza = p.cuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        if (antebrazos) {
            // Antebrazos planos en el suelo: el codo queda bajo el hombro y la muneca al frente.
            p.codo = new Pt(manoX, -0.026);
            p.muneca = new Pt(manoX + ANTEBRAZO - 0.002, -0.026);
        } else {
            brazo(p, new Pt(manoX, -0.02), ATRAS);
        }
        return p;
    }

    /** Flexion con los pies sobre un banco (declinada): igual que el tablon, con los tobillos elevados. */
    private static Pose tablonElevado(double alturaHombro, double manoX) {
        Pose p = new Pose();
        p.tobillo = new Pt(manoX - 0.75, -0.43);
        p.cuello = new Pt(manoX, -alturaHombro);
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.cadera = Pt.lerp(p.tobillo, p.cuello, (MUSLO + PANTORRILLA) / largo);
        p.rodilla = Pt.lerp(p.tobillo, p.cadera, 0.5);
        p.punta = new Pt(p.tobillo.x - 0.035, -0.40);
        Pt dir = unitario(p.cadera, p.cuello);
        p.cabeza = p.cuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        brazo(p, new Pt(manoX, -0.02), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(p.tobillo.x - 0.14, -0.375), new Pt(p.tobillo.x + 0.10, -0.375), 0));
        return p;
    }

    private static Pose flexionRodillas(double t) {
        Pose p = new Pose();
                double h = lerp(0.13, 0.318, t);
        double manoX = -0.05;
        p.cuello = new Pt(manoX, -h);
        double distancia = TORSO + MUSLO;
        double dx = Math.sqrt(Math.max(0.01, distancia * distancia - Math.pow(h - 0.03, 2)));
        p.rodilla = new Pt(manoX - Math.max(0.46, dx), -0.03);
        p.cadera = Pt.lerp(p.rodilla, p.cuello, MUSLO / distancia);
        p.tobillo = new Pt(p.rodilla.x - PANTORRILLA * 0.9, -0.10);
        p.punta = new Pt(p.tobillo.x - 0.05, -0.04);
        Pt dir = unitario(p.cadera, p.cuello);
        p.cabeza = p.cuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        brazo(p, new Pt(manoX, -0.02), ATRAS);
        return p;
    }

    /**
     * Flexion pica: V invertida con la cadera alta, pies firmes (en el suelo o sobre un banco) y
     * manos por delante de la linea de los hombros. Al bajar, los codos se flexionan hacia atras
     * y la coronilla desciende hacia el suelo entre las manos; al subir se extienden sin bajar la
     * cadera.
     */
    private static Pose pica(double t, boolean piesElevados) {
        Pose p = new Pose();
        Pt mano = new Pt(0.05, -0.02);
        double alturaPie = piesElevados ? -0.43 : -0.03;
        p.tobillo = new Pt(-0.42, alturaPie);
        p.punta = new Pt(p.tobillo.x + 0.07, piesElevados ? -0.40 : 0);
        // Arriba los hombros quedan 9 cm detras de las manos; abajo casi sobre ellas.
        p.cuello = new Pt(mano.x() - lerp(0.09, 0.01, t), -lerp(0.31, 0.15, t));
        p.cadera = interseccion(p.cuello, TORSO, p.tobillo, MUSLO + PANTORRILLA - 0.002, true);
        p.rodilla = Pt.lerp(p.cadera, p.tobillo, MUSLO / (MUSLO + PANTORRILLA));
        Pt dir = unitario(p.cadera, p.cuello);
        p.cabeza = p.cuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        brazo(p, mano, ATRAS);
        if (piesElevados) {
            p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(p.tobillo.x - 0.14, -0.375), new Pt(p.tobillo.x + 0.10, -0.375), 0));
        }
        return p;
    }

    /**
     * Jalon al pecho: la torre de poleas esta delante del usuario (a la derecha), el asiento
     * mira a la torre y el rodillo sujeta los muslos. La polea alta queda por delante de la
     * cabeza, de modo que el cable baja en diagonal hasta el agarre sin cruzar la cabeza.
     */
    private static Pose jalon(boolean neutro, double t) {
        Pose p = armar(new Pt(0, -0.26), -8, new Pt(0.22, 0), null, ADELANTE, ADELANTE);
        pieEnSuelo(p);
        Pt mano = p.cuello.mas(lerp(0.14, 0.07, t), lerp(-0.31, 0.10, t));
        brazo(p, mano, ADELANTE);
        Pt polea = new Pt(0.34, -1.30);
        double torreX = 0.52;
        // Asiento y rodillo sujetador sobre los muslos.
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, new Pt(-0.14, -0.20), new Pt(0.10, -0.20), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.12, -0.20), new Pt(-0.12, 0), 0));
        Pt rodillo = Pt.lerp(p.cadera, p.rodilla, 0.80).mas(0.0, -0.075);
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, rodillo.mas(-0.03, 0), rodillo.mas(0.03, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, rodillo.mas(0.03, 0), new Pt(torreX, rodillo.y() - 0.02), 0));
        // Torre de poleas delante: columna, brazo superior, polea y pila de placas.
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(torreX, 0), new Pt(torreX, -1.40), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(torreX, -1.40), new Pt(polea.x(), -1.40), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(polea.x(), -1.40), polea, 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.20, 0), new Pt(0.74, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(torreX + 0.07, -1.18), new Pt(torreX + 0.23, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.CABLE, polea, mano, 0));
        p.equipo.add(new Equipo(TipoEquipo.POLEA, polea, null, 0.035));
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, mano, null, neutro ? 0.026 : 0.02));
        return p;
    }

    private static Pose remo(Implemento imp, VarianteMovimiento var, double t) {
        if (var == VarianteMovimiento.REMO_INVERTIDO) {
            return remoInvertido(t);
        }
        if (var == VarianteMovimiento.REMO_SENTADO) {
            return remoSentado(imp, t);
        }
        Pose p = armar(new Pt(-0.12, -0.42), 62, new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(lerp(0.0, -0.12, t), lerp(0.31, 0.12, t)), ATRAS);
        carga(p, imp, p.muneca, 0.10);
        return p;
    }

    /** Remo sentado en maquina (pecho apoyado) o polea baja: tiron horizontal con el torso erguido. */
    private static Pose remoSentado(Implemento imp, double t) {
        Pose p = new Pose();
        boolean maquina = imp == Implemento.MAQUINA;
        p.cadera = new Pt(0, -0.30);
        double inc = maquina ? 12 : -4;
        p.cuello = arriba(p.cadera, TORSO, inc);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, inc);
        p.tobillo = new Pt(0.24, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(lerp(0.32, 0.10, t), 0.08), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.muneca, null, 0.024));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(-0.10, 0.075), p.cadera.mas(0.12, 0.075), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.95, 0), new Pt(0.60, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(0.28, 0), new Pt(0.28, -0.14), 0));
        if (maquina) {
            Pt esp = normalEspalda(p.cadera, p.cuello);
            Pt frente = new Pt(-esp.x(), -esp.y());
            p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cuello.mas(frente.x() * 0.092 - 0.0, frente.y() * 0.092 + 0.04),
                    Pt.lerp(p.cuello, p.cadera, 0.30).mas(frente.x() * 0.092, frente.y() * 0.092), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(0.16, -0.30), new Pt(0.16, -0.62), 0));
            p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(-0.92, -0.86), new Pt(-0.77, 0), 0));
        } else {
            Pt polea = new Pt(0.74, p.cuello.y() + 0.08);
            p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, polea, 0));
            p.equipo.add(new Equipo(TipoEquipo.POLEA, polea, null, 0.035));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, polea, new Pt(polea.x(), 0), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.14, -0.30), new Pt(-0.14, 0), 0));
        }
        return p;
    }

    /**
     * Remo invertido bajo una mesa: el cuerpo, recto de talones a hombros, queda debajo del
     * tablero y pivota sobre los pies. Las manos agarran el borde del tablero; al tirar, el
     * pecho sube hacia ese borde y al bajar los brazos se extienden. La cabeza queda fuera del
     * borde de la mesa para no atravesarla.
     */
    private static Pose remoInvertido(double t) {
        Pose p = new Pose();
        double largo = TORSO + MUSLO + PANTORRILLA;
        p.tobillo = new Pt(0.66, -0.03);
        double theta = Math.toRadians(lerp(28, 42, t));
        p.cuello = new Pt(p.tobillo.x - largo * Math.cos(theta), p.tobillo.y - largo * Math.sin(theta));
        p.cadera = Pt.lerp(p.tobillo, p.cuello, (MUSLO + PANTORRILLA) / largo);
        p.rodilla = Pt.lerp(p.tobillo, p.cadera, 0.5);
        p.punta = new Pt(p.tobillo.x + 0.02, 0);
        Pt dir = unitario(p.cadera, p.cuello);
        p.cabeza = p.cuello.mas(dir.x() * CABEZA_DISTANCIA, dir.y() * CABEZA_DISTANCIA);
        Pt borde = new Pt(0.03, -0.70);
        brazo(p, borde, ADELANTE);
        // Tablero a 0.745 de altura (cara inferior en -0.72) que cubre todo el cuerpo; las patas quedan fuera de el.
        p.equipo.add(new Equipo(TipoEquipo.MESA, new Pt(-0.34, -0.745), new Pt(0.98, -0.745), 0));
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, borde, null, 0.022));
        return p;
    }

    private static Pose dominada(double t) {
        Pt barra = new Pt(0.05, -1.30);
        Pose p = new Pose();
        p.cuello = new Pt(barra.x() - 0.04, barra.y() + lerp(0.31, 0.06, t));
        p.cadera = p.cuello.mas(0.0, TORSO);
        p.cabeza = p.cuello.mas(0.02, -CABEZA_DISTANCIA);
        p.tobillo = alcance(p.cadera, p.cadera.mas(-0.08, 0.47), MUSLO + PANTORRILLA);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ATRAS);
        p.punta = p.tobillo.mas(0.0, 0.07);
        brazo(p, barra, ADELANTE);
        p.equipo.add(new Equipo(TipoEquipo.BARRA, barra, null, 0.02));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, barra.mas(0.10, 0), barra.mas(0.32, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, barra.mas(0.32, 0), new Pt(barra.x() + 0.32, 0), 0));
        return p;
    }

    /**
     * Curl isometrico con toalla: de pie con los pies sobre el centro de la toalla; las manos
     * sujetan los extremos a la altura del antebrazo, con los codos pegados al torso a ~90 grados.
     * No hay recorrido: {@code t} solo aporta un temblor de +-1.6 grados que representa la
     * tension sostenida. La toalla va tensa entre las manos y los pies.
     */
    private static Pose curlToalla(double t) {
        Pose p = armar(new Pt(0, -0.47), 0, new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        p.codo = p.cuello.mas(0.012, BRAZO);
        double psi = Math.toRadians(90 + lerp(-1.6, 1.6, t));
        p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        p.equipo.add(new Equipo(TipoEquipo.TOALLA, new Pt(-0.12, -0.012), new Pt(0.20, -0.012), 0));
        p.equipo.add(new Equipo(TipoEquipo.TOALLA, p.muneca, new Pt(0.20, -0.012), 0));
        return p;
    }

    /** Curl de biceps sentado en un banco inclinado: el brazo cuelga detras del cuerpo y se flexiona. */
    private static Pose curlInclinado(Implemento imp, double t) {
        Pose p = new Pose();
        p.cadera = new Pt(0, -0.32);
        p.cuello = arriba(p.cadera, TORSO, -55);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -55);
        p.tobillo = new Pt(0.22, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        p.codo = p.cuello.mas(0.0, BRAZO);
        double psi = Math.toRadians(lerp(8, 150, t));
        p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        carga(p, imp, p.muneca, 0.065);
        Pt esp = normalEspalda(p.cadera, p.cuello);
        p.equipo.add(new Equipo(TipoEquipo.BANCO, p.cadera.mas(esp.x() * 0.085 - 0.04, esp.y() * 0.085 + 0.02),
                p.cuello.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.02), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, new Pt(-0.10, -0.25), new Pt(0.10, -0.25), 0));
        return p;
    }

    private static Pose curl(Implemento imp, double t) {
        Pose p = armar(new Pt(0, -0.47), lerp(0, -4, t), new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        p.codo = p.cuello.mas(0.02, BRAZO);
        double psi = Math.toRadians(lerp(8, 150, t));
        p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        if (imp == Implemento.POLEA) {
            Pt polea = new Pt(0.46, -0.06);
            p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, polea, 0));
            p.equipo.add(new Equipo(TipoEquipo.POLEA, polea, null, 0.035));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, polea, polea.mas(0.14, 0.0), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(0.60, 0), new Pt(0.60, -0.95), 0));
            p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(0.64, -0.95), new Pt(0.79, -0.20), 0));
            p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.muneca, null, 0.022));
        } else {
            carga(p, imp, p.muneca, 0.065);
        }
        return p;
    }

    private static Pose triceps(Implemento imp, VarianteMovimiento var, double t) {
        Pose p = armar(new Pt(0, -0.47), 0, new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        if (var == VarianteMovimiento.TRICEPS_SOBRE_CABEZA) {
            p.codo = p.cuello.mas(0.0, -BRAZO);
            double psi = Math.toRadians(lerp(-150, -8, t));
            p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, -Math.cos(psi) * ANTEBRAZO);
            carga(p, imp, p.muneca, 0.07);
            return p;
        }
        p.codo = p.cuello.mas(0.03, BRAZO);
        double psi = Math.toRadians(lerp(105, 4, t));
        p.muneca = p.codo.mas(Math.sin(psi) * ANTEBRAZO, Math.cos(psi) * ANTEBRAZO);
        Pt polea = new Pt(0.07, -1.22);
        p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, polea, 0));
        p.equipo.add(new Equipo(TipoEquipo.POLEA, polea, null, 0.035));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, polea, polea.mas(-0.36, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, polea.mas(-0.36, 0), new Pt(polea.x() - 0.36, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(polea.x() - 0.60, -1.0), new Pt(polea.x() - 0.45, -0.30), 0));
        // La cuerda termina en dos puntas de agarre.
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.muneca, null, 0.022));
        return p;
    }

    private static Pose pesoMuerto(Implemento imp, double t) {
        Pt cadera = new Pt(lerp(-0.20, -0.01, t), -lerp(0.40, 0.47, t));
        Pose p = armar(cadera, lerp(58, 3, t), new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(0.0, 0.31), ATRAS);
        carga(p, imp, p.muneca, 0.12);
        return p;
    }

    private static Pose hipThrust(Implemento imp, double t) {
        Pose p = new Pose();
        p.cuello = new Pt(-0.30, -0.40);
        double phi = Math.toRadians(lerp(35, -8, t));
        p.cadera = new Pt(p.cuello.x + TORSO * Math.cos(phi), p.cuello.y + TORSO * Math.sin(phi));
        p.cabeza = p.cuello.mas(-0.09, -0.03);
        p.tobillo = new Pt(0.17, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, p.cadera.mas(0.02, -0.04), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.60, -0.335), new Pt(-0.22, -0.335), 0));
        carga(p, imp, p.cadera.mas(0.0, -0.075), 0.09);
        return p;
    }

    private static Pose puente(double t) {
        Pose p = new Pose();
        p.cuello = new Pt(-0.32, -0.058);
        double phi = Math.toRadians(lerp(0, -32, t));
        p.cadera = new Pt(p.cuello.x + TORSO * Math.cos(phi), p.cuello.y + TORSO * Math.sin(phi));
        p.cabeza = p.cuello.mas(-CABEZA_DISTANCIA, 0.0);
        p.tobillo = new Pt(0.18, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, new Pt(p.cuello.x + 0.32, -0.02), ATRAS);
        return p;
    }

    private static Pose pressHombro(Implemento imp, double t) {
        Pose p = armar(new Pt(0, -0.47), lerp(0, -3, t), new Pt(0, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(0.01, -lerp(0.08, 0.31, t)), ATRAS);
        carga(p, imp, p.muneca, 0.08);
        return p;
    }

    /**
     * Press de hombros sentado con respaldo vertical. En maquina, los agarres van en brazos
     * articulados que giran sobre un pivote trasero y suben por encima de los hombros; con
     * mancuernas o barra, la carga se empuja sobre la cabeza desde un banco con respaldo.
     */
    private static Pose pressHombroSentado(Implemento imp, double t) {
        Pose p = new Pose();
        p.cadera = new Pt(0, -0.30);
        p.cuello = arriba(p.cadera, TORSO, -2);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -2);
        p.tobillo = new Pt(0.21, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        Pt esp = normalEspalda(p.cadera, p.cuello);
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(-0.10, 0.075), p.cadera.mas(0.12, 0.075), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(esp.x() * 0.085, esp.y() * 0.085),
                p.cuello.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.05), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.95, 0), new Pt(0.16, 0), 0));
        if (imp == Implemento.MAQUINA) {
            Pt pivote = new Pt(-0.26, -0.46);
            double angulo = Math.toRadians(lerp(24, 56, t));
            Pt agarre = new Pt(pivote.x() + 0.42 * Math.cos(angulo), pivote.y() - 0.42 * Math.sin(angulo));
            brazo(p, agarre, ATRAS);
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.20, 0), new Pt(-0.20, -1.0), 0));
            p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(-0.92, -0.86), new Pt(-0.77, 0), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.845, -0.86), new Pt(-0.20, -1.0), 0));
            p.equipo.add(new Equipo(TipoEquipo.PALANCA, pivote, agarre, 0));
            p.equipo.add(new Equipo(TipoEquipo.AGARRE, pivote, null, 0.022));
            p.equipo.add(new Equipo(TipoEquipo.AGARRE, agarre, null, 0.026));
        } else {
            brazo(p, p.cuello.mas(0.01, -lerp(0.08, 0.31, t)), ATRAS);
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.20, 0), new Pt(-0.20, -0.95), 0));
            carga(p, imp, p.muneca, 0.08);
        }
        return p;
    }

    private static Pose zancada(Implemento imp, VarianteMovimiento var, double t) {
        Pt cadera = new Pt(-0.03, -lerp(0.45, 0.27, t));
        Pose p = armar(cadera, 2, new Pt(0.24, 0), null, ADELANTE, ATRAS);
        pieEnSuelo(p);
        brazo(p, p.cuello.mas(0.05, 0.25), ATRAS);
        carga(p, imp, p.muneca, 0.08);
        if (var == VarianteMovimiento.ZANCADA_BULGARA) {
            p.tobillo2 = alcance(cadera, new Pt(-0.40, -0.42), MUSLO + PANTORRILLA);
            p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.58, -0.375), new Pt(-0.34, -0.375), 0));
            p.punta2 = p.tobillo2.mas(-0.08, 0.0);
        } else {
            p.tobillo2 = alcance(cadera, new Pt(-0.30, -0.07), MUSLO + PANTORRILLA);
            p.punta2 = new Pt(p.tobillo2.x, 0);
        }
        p.rodilla2 = articulacion(cadera, p.tobillo2, MUSLO, PANTORRILLA, ADELANTE);
        return p;
    }

    private static Pose talones(Implemento imp, double t) {
        double alza = t * 0.07;
        Pose p = armar(new Pt(0, -0.47 - alza), 0, new Pt(0, -alza), null, ADELANTE, ATRAS);
        brazo(p, p.cuello.mas(0.02, 0.30), ATRAS);
        p.punta = new Pt(p.tobillo.x + PIE, 0);
        if (imp == Implemento.MAQUINA) {
            p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cuello.mas(-0.07, -0.035), p.cuello.mas(0.06, -0.035), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, p.cuello.mas(-0.10, -0.035), p.cuello.mas(-0.10, -0.50), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.22, 0), new Pt(0.22, 0), 0));
            p.equipo.add(new Equipo(TipoEquipo.PLATAFORMA, new Pt(-0.07, -0.0), new Pt(0.20, -0.0), 0));
        } else if (imp == Implemento.BARRA) {
            Pt esp = normalEspalda(p.cadera, p.cuello);
            p.equipo.add(new Equipo(TipoEquipo.DISCO, p.cuello.mas(esp.x() * 0.045, esp.y() * 0.045), null, 0.09));
        } else if (imp == Implemento.MANCUERNA) {
            carga(p, imp, p.muneca, 0.08);
        }
        return p;
    }

    /**
     * Maquina de pierna sentado (respaldo y asiento): extension de cuadriceps ({@code extension})
     * o curl femoral sentado. La espinilla gira alrededor de la rodilla; en la extension va de
     * colgando (90 grados) a casi horizontal y en el curl, al reves.
     */
    private static Pose sentadoPierna(Implemento imp, double t, boolean extension) {
        Pose p = new Pose();
        p.cadera = new Pt(0, -0.42);
        p.cuello = arriba(p.cadera, TORSO, -4);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, -4);
        p.rodilla = new Pt(MUSLO, -0.42);
        double fi = Math.toRadians(extension ? lerp(90, 6, t) : lerp(8, 100, t));
        p.tobillo = new Pt(p.rodilla.x + PANTORRILLA * Math.cos(fi), p.rodilla.y + PANTORRILLA * Math.sin(fi));
        Pt frente = new Pt(Math.sin(fi), -Math.cos(fi));
        p.punta = new Pt(p.tobillo.x + frente.x() * 0.08, p.tobillo.y + frente.y() * 0.08);
        brazo(p, p.cadera.mas(0.11, -0.10), ATRAS);
        Pt esp = normalEspalda(p.cadera, p.cuello);
        // Asiento, respaldo y base.
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, new Pt(-0.10, -0.35), new Pt(0.20, -0.35), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.cadera.mas(esp.x() * 0.085, esp.y() * 0.085 - 0.02),
                p.cuello.mas(esp.x() * 0.085, esp.y() * 0.085), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.95, 0), new Pt(0.40, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.18, -0.35), new Pt(-0.18, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(-0.92, -0.86), new Pt(-0.77, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.845, -0.86), new Pt(-0.28, -0.86), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.28, -0.86), p.cuello.mas(esp.x() * 0.13, esp.y() * 0.13 - 0.05), 0));
        // Rodillo sobre el muslo (fija la cadera) y rodillo del tobillo con su palanca.
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.rodilla.mas(-0.09, -0.075), p.rodilla.mas(-0.03, -0.075), 0));
        double lado = extension ? 1 : 1;
        Pt rodilloTobillo = p.tobillo.mas(frente.x() * 0.05 * lado, frente.y() * 0.05 * lado);
        p.equipo.add(new Equipo(TipoEquipo.PALANCA, p.rodilla.mas(-0.03, 0.06),
                p.tobillo.mas(-frente.x() * 0.06, -frente.y() * 0.06), 0));
        p.equipo.add(new Equipo(TipoEquipo.MANCUERNA, rodilloTobillo, null, 0.035));
        return p;
    }

    /**
     * Elevacion de talones sentado: rodillas a 90 grados, almohadilla sobre los muslos y puntas
     * sobre una plataforma elevada. Al subir los talones, rodillas y cuerpo se elevan juntos.
     */
    private static Pose talonesSentado(Implemento imp, double t) {
        Pose p = new Pose();
        double alza = t * 0.04;
        p.cadera = new Pt(0, -0.46 - alza);
        p.cuello = arriba(p.cadera, TORSO, 0);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, 0);
        p.rodilla = new Pt(MUSLO, p.cadera.y);
        p.tobillo = new Pt(MUSLO, p.rodilla.y + PANTORRILLA);
        p.punta = new Pt(p.tobillo.x + PIE, -0.15);
        brazo(p, p.rodilla.mas(-0.04, -0.05), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, new Pt(-0.10, -0.39), new Pt(0.14, -0.39), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(-0.10, -0.39), new Pt(-0.10, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.rodilla.mas(-0.10, -0.085), p.rodilla.mas(0.03, -0.085), 0));
        p.equipo.add(new Equipo(TipoEquipo.PALANCA, p.rodilla.mas(0.03, -0.085), p.rodilla.mas(0.20, -0.085), 0));
        p.equipo.add(new Equipo(TipoEquipo.PLATAFORMA, new Pt(0.10, -0.14), new Pt(0.36, -0.14), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(0.36, -0.14), new Pt(0.36, 0), 0));
        return p;
    }

    private static Pose curlFemoral(Implemento imp, double t) {
        Pose p = new Pose();
        p.pechoInvertido = true;
        p.cadera = new Pt(0, -0.33);
        p.cuello = new Pt(-TORSO, -0.33);
        p.cabeza = new Pt(-TORSO - CABEZA_DISTANCIA, -0.33);
        p.rodilla = new Pt(0.24, -0.33);
        double phi = Math.toRadians(lerp(4, 100, t));
        p.tobillo = new Pt(p.rodilla.x + PANTORRILLA * Math.cos(phi), p.rodilla.y - PANTORRILLA * Math.sin(phi));
        p.punta = new Pt(p.tobillo.x - Math.sin(phi) * 0.07, p.tobillo.y - Math.cos(phi) * 0.07);
        brazo(p, new Pt(p.cuello.x - 0.05, -0.12), ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.46, -0.25), new Pt(0.34, -0.25), 0));
        if (imp == Implemento.MAQUINA) {
            p.equipo.add(new Equipo(TipoEquipo.MANCUERNA, p.tobillo, null, 0.04));
            p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(0.52, -0.80), new Pt(0.67, -0.20), 0));
        }
        return p;
    }

    private static Pose crunch(double t) {
        Pose p = new Pose();
        p.cadera = new Pt(0.05, -0.058);
        double phi = Math.toRadians(lerp(3, 40, t));
        p.cuello = new Pt(p.cadera.x - TORSO * Math.cos(phi), p.cadera.y - TORSO * Math.sin(phi));
        double phiCabeza = Math.toRadians(lerp(3, 40, t) + 10);
        p.cabeza = new Pt(p.cuello.x - CABEZA_DISTANCIA * Math.cos(phiCabeza),
                p.cuello.y - CABEZA_DISTANCIA * Math.sin(phiCabeza));
        p.tobillo = new Pt(0.32, 0);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, p.cabeza.mas(0.03, -0.02), ATRAS);
        return p;
    }

    /** Crunch de rodillas con cable: la polea alta queda delante y la cuerda baja hasta las manos junto a la cabeza. */
    private static Pose crunchPolea(double t) {
        Pose p = new Pose();
        p.rodilla = new Pt(0.10, -0.03);
        p.cadera = new Pt(0.07, -0.265);
        p.tobillo = new Pt(-0.14, -0.03);
        p.punta = new Pt(-0.21, -0.012);
        double inc = lerp(8, 62, t);
        p.cuello = arriba(p.cadera, TORSO, inc);
        p.cabeza = arriba(p.cuello, CABEZA_DISTANCIA, inc * 0.8);
        brazo(p, p.cabeza.mas(0.09, 0.03), ATRAS);
        Pt polea = new Pt(0.55, -1.30);
        p.equipo.add(new Equipo(TipoEquipo.CABLE, p.muneca, polea, 0));
        p.equipo.add(new Equipo(TipoEquipo.POLEA, polea, null, 0.035));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, polea, new Pt(0.66, -1.30), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, new Pt(0.66, -1.30), new Pt(0.66, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.PILA, new Pt(0.70, -1.10), new Pt(0.85, -0.35), 0));
        p.equipo.add(new Equipo(TipoEquipo.AGARRE, p.muneca, null, 0.022));
        return p;
    }

    /**
     * De rodillas con el cuerpo recto de rodillas a hombros que cae hacia delante: curl femoral
     * nordico (tobillos anclados) o rueda abdominal (manos sobre la rueda). El angulo crece con
     * {@code t}.
     */
    private static Pose caidaDeRodillas(double t, boolean rueda) {
        Pose p = new Pose();
        double grados = rueda ? lerp(50, 80, t) : lerp(8, 72, t);
        double r = Math.toRadians(grados);
        p.rodilla = new Pt(0.0, -0.03);
        p.tobillo = new Pt(-PANTORRILLA, -0.03);
        p.punta = new Pt(p.tobillo.x - 0.07, -0.012);
        p.cadera = new Pt(p.rodilla.x + MUSLO * Math.sin(r), p.rodilla.y - MUSLO * Math.cos(r));
        p.cuello = new Pt(p.cadera.x + TORSO * Math.sin(r), p.cadera.y - TORSO * Math.cos(r));
        p.cabeza = new Pt(p.cuello.x + CABEZA_DISTANCIA * Math.sin(r * 0.8), p.cuello.y - CABEZA_DISTANCIA * Math.cos(r * 0.8));
        if (rueda) {
            Pt manoRueda = new Pt(p.cuello.x + 0.02, -0.07);
            brazo(p, manoRueda, ADELANTE);
            p.equipo.add(new Equipo(TipoEquipo.DISCO, p.muneca.mas(0.0, 0.0), null, 0.05));
        } else {
            brazo(p, new Pt(p.cuello.x + 0.04, -0.02), ATRAS);
            // Ancla bajo los tobillos y colchoneta bajo las rodillas.
            p.equipo.add(new Equipo(TipoEquipo.ASIENTO, p.tobillo.mas(-0.10, -0.06), p.tobillo.mas(0.05, -0.06), 0));
            p.equipo.add(new Equipo(TipoEquipo.RIEL, p.tobillo.mas(-0.10, -0.06), new Pt(p.tobillo.x - 0.10, 0), 0));
        }
        return p;
    }

    private static Pose elevacionPiernas(double t) {
        Pose p = new Pose();
        p.cadera = new Pt(0.05, -0.058);
        p.cuello = new Pt(p.cadera.x - TORSO, -0.058);
        p.cabeza = new Pt(p.cuello.x - CABEZA_DISTANCIA, -0.058);
        double phi = Math.toRadians(lerp(8, 85, t));
        double largo = MUSLO + PANTORRILLA - 0.002;
        p.tobillo = new Pt(p.cadera.x + largo * Math.cos(phi), p.cadera.y - largo * Math.sin(phi));
        p.rodilla = Pt.lerp(p.cadera, p.tobillo, MUSLO / (MUSLO + PANTORRILLA));
        p.punta = new Pt(p.tobillo.x - Math.sin(phi) * 0.08, p.tobillo.y - Math.cos(phi) * 0.08);
        brazo(p, new Pt(p.cuello.x + 0.31, -0.026), ATRAS);
        return p;
    }

    private static Pose fondos(double t) {
        Pt agarre = new Pt(0.0, -0.95);
        Pose p = new Pose();
        p.cuello = new Pt(agarre.x() - 0.03, agarre.y() - lerp(0.02, 0.29, t));
        p.cadera = p.cuello.mas(-0.03, TORSO);
        p.cabeza = p.cuello.mas(0.025, -CABEZA_DISTANCIA);
        p.tobillo = alcance(p.cadera, p.cadera.mas(-0.13, 0.40), MUSLO + PANTORRILLA);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ATRAS);
        p.punta = p.tobillo.mas(0.0, 0.07);
        brazo(p, agarre, ATRAS);
        // Barras paralelas vistas de lado: una barra larga con un poste en cada extremo.
        // La barra sale de las manos hacia delante; el tramo trasero queda detras de las piernas.
        p.equipo.add(new Equipo(TipoEquipo.RIEL, agarre.mas(0.03, 0), agarre.mas(0.44, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, agarre.mas(0.44, 0), new Pt(agarre.x() + 0.44, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, agarre.mas(-0.42, 0), agarre.mas(-0.30, 0), 0));
        p.equipo.add(new Equipo(TipoEquipo.RIEL, agarre.mas(-0.42, 0), new Pt(agarre.x() - 0.42, 0), 0));
        return p;
    }

    /** Fondos con las manos en una silla o banco detras del cuerpo y los pies apoyados al frente. */
    private static Pose fondosSilla(double t) {
        double alturaSilla = 0.38;
        Pt agarre = new Pt(-0.04, -alturaSilla);
        Pose p = new Pose();
        // Las caderas quedan fuera del asiento; el hombro baja y sube casi en vertical.
        p.cuello = new Pt(agarre.x() + 0.10, agarre.y() - lerp(0.10, 0.285, t));
        p.cadera = p.cuello.mas(0.0, TORSO);
        p.cabeza = p.cuello.mas(0.02, -CABEZA_DISTANCIA);
        p.tobillo = alcance(p.cadera, new Pt(0.34, 0), MUSLO + PANTORRILLA);
        p.rodilla = articulacion(p.cadera, p.tobillo, MUSLO, PANTORRILLA, ADELANTE);
        pieEnSuelo(p);
        brazo(p, agarre, ATRAS);
        p.equipo.add(new Equipo(TipoEquipo.BANCO, new Pt(-0.40, -alturaSilla + 0.025), new Pt(-0.02, -alturaSilla + 0.025), 0));
        return p;
    }

    private static Pose superman(double t) {
        Pose p = new Pose();
        p.pechoInvertido = true;
        p.cadera = new Pt(0.02, -0.06);
        double a = Math.toRadians(lerp(0, 14, t));
        p.cuello = new Pt(p.cadera.x - TORSO * Math.cos(a), p.cadera.y - TORSO * Math.sin(a));
        p.cabeza = new Pt(p.cuello.x - CABEZA_DISTANCIA * Math.cos(a + 0.1), p.cuello.y - CABEZA_DISTANCIA * Math.sin(a + 0.1));
        double b = Math.toRadians(lerp(2, 20, t));
        double c = Math.toRadians(lerp(0, 14, t));
        double largoPierna = MUSLO + PANTORRILLA - 0.002;
        p.tobillo = new Pt(p.cadera.x + largoPierna * Math.cos(c), p.cadera.y - largoPierna * Math.sin(c));
        p.rodilla = Pt.lerp(p.cadera, p.tobillo, MUSLO / (MUSLO + PANTORRILLA));
        p.punta = new Pt(p.tobillo.x + 0.05, p.tobillo.y - 0.01);
        p.muneca = new Pt(p.cuello.x - (BRAZO + ANTEBRAZO - 0.002) * Math.cos(b),
                p.cuello.y - (BRAZO + ANTEBRAZO - 0.002) * Math.sin(b));
        p.codo = Pt.lerp(p.cuello, p.muneca, BRAZO / (BRAZO + ANTEBRAZO));
        return p;
    }

    /**
     * Burpee: de pie con los brazos arriba, baja a sentadilla con las manos en el suelo, lleva
     * los pies atras hasta la plancha y baja el pecho (flexion). El ciclo se recorre de ida y vuelta.
     */
    private static Pose burpee(double t) {
        double manoX = 0.19;
        Pt mano = new Pt(manoX, -0.02);
        if (t < 0.30) {
            double u = suave(t / 0.30);
            Pt cadera = new Pt(lerp(0, -0.16, u), -lerp(0.47, 0.27, u));
            Pose p = armar(cadera, lerp(3, 85, u), new Pt(0, 0), null, ADELANTE, ATRAS);
            pieEnSuelo(p);
            brazo(p, Pt.lerp(p.cuello.mas(0.02, -0.31), mano, u), ATRAS);
            return p;
        }
        Pose plancha = tablon(0.318, false, manoX);
        Pose sentadilla = burpeeSentadilla();
        if (t < 0.62) {
            double v = suave((t - 0.30) / 0.32);
            Pose p = new Pose();
            p.cuello = Pt.lerp(sentadilla.cuello, plancha.cuello, v);
            Pt cadLerp = Pt.lerp(sentadilla.cadera, plancha.cadera, v);
            Pt dir = unitario(p.cuello, cadLerp);
            p.cadera = new Pt(p.cuello.x() + dir.x() * TORSO, p.cuello.y() + dir.y() * TORSO);
            Pt tobObj = Pt.lerp(new Pt(0, 0), plancha.tobillo, v).mas(0, -0.12 * Math.sin(Math.PI * v));
            p.tobillo = alcance(p.cadera, tobObj, MUSLO + PANTORRILLA);
            p.rodilla = rodillaMasAlta(p.cadera, p.tobillo);
            p.punta = new Pt(p.tobillo.x - 0.035 * v + PIE * (1 - v), Math.min(0, p.tobillo.y + 0.03));
            Pt dc = unitario(p.cadera, p.cuello);
            p.cabeza = p.cuello.mas(dc.x() * CABEZA_DISTANCIA, dc.y() * CABEZA_DISTANCIA);
            brazo(p, mano, ATRAS);
            return p;
        }
        double w = suave((t - 0.62) / 0.38);
        Pose p = tablon(lerp(0.318, 0.15, w), false, manoX);
        return p;
    }

    private static Pose burpeeSentadilla() {
        Pt cadera = new Pt(-0.16, -0.27);
        Pose p = armar(cadera, 85, new Pt(0, 0), null, ADELANTE, ATRAS);
        brazo(p, new Pt(0.19, -0.02), ATRAS);
        return p;
    }
}
