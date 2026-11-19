package com.symmetry.desktop.service;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.Implemento;
import com.symmetry.desktop.model.ModalidadEntrenamiento;
import com.symmetry.desktop.model.PatronMovimiento;
import com.symmetry.desktop.model.VarianteMovimiento;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Decide que patron biomecanico animar para un ejercicio a partir de su nombre (las palabras
 * clave se evaluan en orden de especificidad) y, si no hay coincidencia, de su grupo muscular.
 * Funciona 100% offline: no depende de archivos ni enlaces del usuario.
 */
public final class GuiaTecnicaService {

    private GuiaTecnicaService() {
    }

    /** Todo lo necesario para animar un ejercicio: patron, equipamiento y variante de postura. */
    public record Perfil(PatronMovimiento patron, Implemento implemento, VarianteMovimiento variante) {
    }

    /**
     * Analiza el nombre del ejercicio (y su modalidad con/sin equipo) y devuelve el patron de
     * movimiento, el implemento a dibujar y la variante de postura. Nunca devuelve nulos:
     * si el nombre no coincide con nada conocido se infiere por palabras clave y, en ultima
     * instancia, por el grupo muscular.
     */
    public static Perfil perfil(String nombre, GrupoMuscular grupo, ModalidadEntrenamiento modalidad) {
        String n = nombre == null ? "" : normalizar(nombre);
        boolean casa = modalidad == ModalidadEntrenamiento.CASA;
        PatronMovimiento patron = detectar(nombre, grupo);
        // En calistenia no existen poleas ni jalones: el equivalente es la dominada.
        if (casa && patron == PatronMovimiento.JALON) {
            patron = PatronMovimiento.DOMINADA;
        }
        Implemento implemento = detectarImplemento(n, patron, casa);
        VarianteMovimiento variante = detectarVariante(n, patron, implemento);
        // Estas variantes son de peso corporal o de material domestico: nunca llevan maquina ni barra.
        switch (variante) {
            case REMO_INVERTIDO, FLEXION_PICA, PICA_ELEVADA, CURL_ISOMETRICO_TOALLA, FONDOS_SILLA,
                 FLEXION_DECLINADA, PISTOL, PLANCHA_ALTA, CURL_FEMORAL_NORDICO, RUEDA_ABDOMINAL ->
                    implemento = Implemento.NINGUNO;
            case JALON_NEUTRO -> implemento = Implemento.POLEA;
            case CRUNCH_POLEA -> implemento = Implemento.POLEA;
            case CURL_FEMORAL_SENTADO, HACK, PRENSA, PRESS_SENTADO, TALONES_SENTADO -> {
                if (implemento != Implemento.MAQUINA) {
                    implemento = Implemento.MAQUINA;
                }
            }
            default -> {
            }
        }
        if (patron == PatronMovimiento.APERTURA_PECHO || patron == PatronMovimiento.EXTENSION_CUADRICEPS) {
            implemento = Implemento.MAQUINA;
        }
        // Sin pose de maquina propia para el patron: se usa el implemento que si se dibuja.
        if (implemento == Implemento.MAQUINA && !tienePoseDeMaquina(patron, variante)) {
            implemento = switch (patron) {
                case CURL_BICEPS, ELEVACION_LATERAL, EXTENSION_TRICEPS, CRUNCH -> Implemento.POLEA;
                case ZANCADA -> Implemento.MANCUERNA;
                case HIP_THRUST, PESO_MUERTO -> Implemento.BARRA;
                default -> casa ? Implemento.NINGUNO : Implemento.BARRA;
            };
        }
        return new Perfil(patron, implemento, variante);
    }

    /** true si el patron/variante sabe dibujar una maquina (asiento, respaldo y estructura). */
    private static boolean tienePoseDeMaquina(PatronMovimiento patron, VarianteMovimiento variante) {
        return switch (patron) {
            case SENTADILLA, JALON, CURL_FEMORAL, ELEVACION_TALONES, APERTURA_PECHO, EXTENSION_CUADRICEPS -> true;
            case PRESS_PLANO, PRESS_INCLINADO -> variante == VarianteMovimiento.PRESS_SENTADO;
            case PRESS_HOMBRO -> variante == VarianteMovimiento.PRESS_HOMBRO_SENTADO;
            case REMO -> variante == VarianteMovimiento.REMO_SENTADO;
            default -> false;
        };
    }

    private static Implemento detectarImplemento(String n, PatronMovimiento patron, boolean casa) {
        switch (patron) {
            case FLEXION, PLANCHA, DOMINADA, FONDOS, ELEVACION_PIERNAS, CRUNCH, SUPERMAN,
                 PUENTE_GLUTEOS, BURPEE -> {
                return Implemento.NINGUNO;
            }
            case JALON, EXTENSION_TRICEPS -> {
                if (n.contains("mancuerna")) {
                    return Implemento.MANCUERNA;
                }
                if (n.contains("barra") && patron == PatronMovimiento.EXTENSION_TRICEPS) {
                    return Implemento.BARRA;
                }
                return casa && !contiene(n, "polea", "cuerda", "cable") ? Implemento.NINGUNO : Implemento.POLEA;
            }
            default -> {
            }
        }
        if (contiene(n, "sin carga", "sin equipo", "peso corporal", "libre", "sin peso")) {
            return Implemento.NINGUNO;
        }
        // Remo con el pecho apoyado: maquina o banco con soporte, nunca barra libre.
        if (patron == PatronMovimiento.REMO && n.contains("pecho apoyado")) {
            return Implemento.MAQUINA;
        }
        // Press sobre la cabeza, extensiones y aperturas "en maquina": maquina real.
        if (patron == PatronMovimiento.PRESS_HOMBRO && contiene(n, "maquina") && !contiene(n, "mancuerna", "barra")) {
            return Implemento.MAQUINA;
        }
        if (contiene(n, "mancuerna", "mochila", "goblet", "kettlebell", "pesa rusa")) {
            return Implemento.MANCUERNA;
        }
        if (patron == PatronMovimiento.HIP_THRUST && n.contains("barra")) {
            return Implemento.BARRA;
        }
        if (contiene(n, "polea", "cuerda", "cable")) {
            return Implemento.POLEA;
        }
        if (contiene(n, "maquina", "prensa", "pec deck", "hack", "smith", "multipower")) {
            return Implemento.MAQUINA;
        }
        if (contiene(n, "barra", "pendlay", "banca")) {
            return Implemento.BARRA;
        }
        if (casa) {
            return Implemento.NINGUNO;
        }
        // Con equipo y sin pista en el nombre: el implemento mas habitual de cada patron.
        return switch (patron) {
            case SENTADILLA, PRESS_PLANO, PRESS_INCLINADO, PRESS_HOMBRO, PESO_MUERTO, HIP_THRUST, REMO ->
                    Implemento.BARRA;
            case CURL_BICEPS, ELEVACION_LATERAL, ZANCADA -> Implemento.MANCUERNA;
            case CURL_FEMORAL, ELEVACION_TALONES -> Implemento.MAQUINA;
            default -> Implemento.NINGUNO;
        };
    }

    private static VarianteMovimiento detectarVariante(String n, PatronMovimiento patron, Implemento imp) {
        boolean sentado = contiene(n, "sentado", "en maquina");
        switch (patron) {
            case APERTURA_PECHO -> {
                if (n.contains("inverso") || n.contains("posterior")) {
                    return VarianteMovimiento.APERTURA_INVERSA;
                }
            }
            case JALON -> {
                if (n.contains("neutro")) {
                    return VarianteMovimiento.JALON_NEUTRO;
                }
            }
            case CURL_FEMORAL -> {
                if (n.contains("nordico")) {
                    return VarianteMovimiento.CURL_FEMORAL_NORDICO;
                }
                if (sentado || imp == Implemento.MAQUINA && !n.contains("tumbado")) {
                    return n.contains("tumbado") ? VarianteMovimiento.ESTANDAR : VarianteMovimiento.CURL_FEMORAL_SENTADO;
                }
            }
            case CRUNCH -> {
                if (n.contains("rueda")) {
                    return VarianteMovimiento.RUEDA_ABDOMINAL;
                }
                if (contiene(n, "polea", "cable")) {
                    return VarianteMovimiento.CRUNCH_POLEA;
                }
            }
            case ELEVACION_TALONES -> {
                if (n.contains("sentado")) {
                    return VarianteMovimiento.TALONES_SENTADO;
                }
            }
            case PLANCHA -> {
                if (contiene(n, "toque", "manos", "alta")) {
                    return VarianteMovimiento.PLANCHA_ALTA;
                }
            }
            case SENTADILLA -> {
                if (n.contains("prensa")) {
                    return VarianteMovimiento.PRENSA;
                }
                if (n.contains("hack")) {
                    return VarianteMovimiento.HACK;
                }
                if (contiene(n, "pistol", "una pierna")) {
                    return VarianteMovimiento.PISTOL;
                }
            }
            case PRESS_PLANO, PRESS_INCLINADO -> {
                if (imp == Implemento.MAQUINA || n.contains("pec deck")) {
                    return VarianteMovimiento.PRESS_SENTADO;
                }
            }
            case REMO -> {
                if (contiene(n, "invertido", "mesa", "australiano")) {
                    return VarianteMovimiento.REMO_INVERTIDO;
                }
                if (imp == Implemento.MAQUINA || imp == Implemento.POLEA
                        || contiene(n, "sentado", "pecho apoyado", "polea baja")) {
                    return VarianteMovimiento.REMO_SENTADO;
                }
                if (imp == Implemento.NINGUNO && !contiene(n, "inclinado", "pendlay")) {
                    return VarianteMovimiento.REMO_INVERTIDO;
                }
            }
            case FONDOS -> {
                if (contiene(n, "silla", "banco", "sofa", "cama")) {
                    return VarianteMovimiento.FONDOS_SILLA;
                }
            }
            case FLEXION -> {
                if (n.contains("rodillas")) {
                    return VarianteMovimiento.FLEXION_RODILLAS;
                }
                if (contiene(n, "declinada", "pies elevados")) {
                    return VarianteMovimiento.FLEXION_DECLINADA;
                }
            }
            case PRESS_HOMBRO -> {
                if (contiene(n, "pica", "pike")) {
                    return contiene(n, "pies elevados", "elevados") ? VarianteMovimiento.PICA_ELEVADA
                            : VarianteMovimiento.FLEXION_PICA;
                }
                if (imp == Implemento.MAQUINA || n.contains("sentado")) {
                    return VarianteMovimiento.PRESS_HOMBRO_SENTADO;
                }
            }
            case CURL_BICEPS -> {
                if (n.contains("toalla") || n.contains("isometric")) {
                    return VarianteMovimiento.CURL_ISOMETRICO_TOALLA;
                }
                if (n.contains("inclinado")) {
                    return VarianteMovimiento.CURL_INCLINADO;
                }
            }
            case ZANCADA -> {
                if (contiene(n, "bulgara")) {
                    return VarianteMovimiento.ZANCADA_BULGARA;
                }
            }
            case EXTENSION_TRICEPS -> {
                if (contiene(n, "sobre", "cabeza", "frances") || imp != Implemento.POLEA) {
                    return VarianteMovimiento.TRICEPS_SOBRE_CABEZA;
                }
            }
            default -> {
            }
        }
        return VarianteMovimiento.ESTANDAR;
    }

    /** Titulo del patron teniendo en cuenta la variante (por ejemplo, "Flexión pica"). */
    public static String titulo(Perfil perfil) {
        return switch (perfil.variante()) {
            case REMO_INVERTIDO -> "Remo invertido bajo una mesa";
            case REMO_SENTADO -> perfil.implemento() == Implemento.MAQUINA ? "Remo sentado en máquina" : "Remo sentado en polea";
            case FLEXION_PICA -> "Flexión pica (pike push-up)";
            case PICA_ELEVADA -> "Flexión pica con pies elevados";
            case CURL_ISOMETRICO_TOALLA -> "Curl isométrico con toalla";
            case JALON_NEUTRO -> "Jalón al pecho con agarre neutro";
            case PRESS_SENTADO -> perfil.patron() == PatronMovimiento.PRESS_INCLINADO
                    ? "Press inclinado en máquina" : "Press de pecho en máquina";
            case PRESS_HOMBRO_SENTADO -> "Press de hombros sentado";
            case APERTURA_INVERSA -> "Pec deck inverso (deltoides posterior)";
            case CURL_FEMORAL_SENTADO -> "Curl femoral sentado";
            case CURL_FEMORAL_NORDICO -> "Curl femoral nórdico";
            case CRUNCH_POLEA -> "Crunch en polea";
            case RUEDA_ABDOMINAL -> "Rueda abdominal";
            case HACK -> "Sentadilla hack";
            case CURL_INCLINADO -> "Curl de bíceps inclinado";
            case TALONES_SENTADO -> "Elevación de talones sentado";
            case FLEXION_DECLINADA -> "Flexión declinada (pies elevados)";
            case PISTOL -> "Sentadilla a una pierna";
            case PLANCHA_ALTA -> "Plancha sobre las manos";
            default -> perfil.patron().getTitulo();
        };
    }

    /** Puntos clave de ejecucion; las variantes con tecnica propia sustituyen a los del patron. */
    public static List<String> puntosClave(Perfil perfil) {
        return switch (perfil.variante()) {
            case REMO_INVERTIDO -> List.of(
                    "Postura: túmbate bajo una mesa estable, agarra el borde y mantén el cuerpo recto de talones a hombros, con glúteos y abdomen firmes.",
                    "Rango: tira del pecho hacia el borde de la mesa juntando las escápulas y baja con control hasta extender los brazos, sin dejar caer la cadera.",
                    "Respiración: exhala al tirar hacia la mesa e inspira al bajar lento (2-3 segundos). Comprueba antes que la mesa aguanta tu peso.");
            case FLEXION_PICA -> List.of(
                    "Postura: manos en el suelo algo por delante de los hombros, pies firmes y cadera alta formando una V invertida; mantén las piernas casi rectas.",
                    "Rango: flexiona los codos hacia atrás y afuera para llevar la coronilla al suelo entre las manos; empuja hasta extender los codos sin bajar la cadera.",
                    "Respiración: inspira al bajar y exhala al empujar. Si la cadera cae, acerca los pies a las manos.");
            case PICA_ELEVADA -> List.of(
                    "Postura: pies sobre un banco estable y manos en el suelo; la cadera alta forma una V invertida más cerrada que en la pica normal.",
                    "Rango: baja la coronilla hacia el suelo entre las manos doblando los codos hacia atrás y empuja hasta extenderlos; el hombro asume más carga.",
                    "Respiración: inspira al bajar y exhala al empujar. Pasa a la versión con pies en el suelo si pierdes el control del cuello.");
            case CURL_ISOMETRICO_TOALLA -> List.of(
                    "Postura: de pie con los pies sobre el centro de la toalla y las manos en los extremos; codos pegados al torso y flexionados a unos 90°.",
                    "Tensión: tira de la toalla hacia arriba mientras los pies la presionan hacia abajo y mantén 20-30 s sin balancear la espalda; sentirás el bíceps contraído.",
                    "Respiración: continua y controlada, sin contener el aire. Aumenta la tensión poco a poco y suéltala despacio al terminar.");
            case JALON_NEUTRO -> List.of(
                    "Postura: siéntate de cara a la torre con los muslos bajo el rodillo, pecho alto y torso ligeramente inclinado atrás.",
                    "Rango: baja el agarre neutro en diagonal hacia la parte alta del pecho, guiando con los codos hacia abajo, y sube hasta estirar los brazos sin perder la escápula.",
                    "Respiración: exhala al tirar e inspira al dejar subir el agarre; no balancees el torso para ayudarte.");
            case PRESS_SENTADO -> perfil.patron() == PatronMovimiento.PRESS_INCLINADO ? List.of(
                    "Postura: espalda y cabeza apoyadas en el respaldo inclinado, escápulas atrás y agarres a la altura de la parte alta del pecho.",
                    "Rango: empuja los agarres hacia delante y arriba hasta casi extender los codos y vuelve controlado sin que la pila apoye.",
                    "Respiración: exhala al empujar e inspira al volver; no despegues la espalda del respaldo.") : List.of(
                    "Postura: espalda apoyada en el respaldo, asiento regulado para que los agarres queden a la altura del pecho y escápulas atrás.",
                    "Rango: empuja los agarres hacia delante hasta casi extender los codos y vuelve controlado hasta sentir el pecho estirado, sin que la pila apoye.",
                    "Respiración: exhala al empujar e inspira al volver; mantén los hombros abajo.");
            case PRESS_HOMBRO_SENTADO -> List.of(
                    "Postura: espalda pegada al respaldo vertical, asiento a la altura que deje los agarres a la altura de los hombros y abdomen firme.",
                    "Rango: empuja los agarres hacia arriba hasta casi extender los codos sin encogerte y baja hasta la altura de las orejas.",
                    "Respiración: exhala al empujar e inspira al bajar controlado; no arquees la zona lumbar.");
            case APERTURA_INVERSA -> List.of(
                    "Postura: pecho contra el respaldo, brazos al frente a la altura de los hombros y codos ligeramente semiflexionados y fijos.",
                    "Rango: abre los brazos hacia atrás hasta la línea de los hombros juntando las escápulas y vuelve controlado; trabaja el deltoides posterior.",
                    "Respiración: exhala al abrir e inspira al volver; no uses impulso del torso.");
            case CURL_FEMORAL_SENTADO -> List.of(
                    "Postura: espalda apoyada, rodillo sobre el muslo y almohadilla del tobillo justo encima del talón, con el eje de la máquina alineado con la rodilla.",
                    "Rango: flexiona las rodillas llevando los talones bajo el asiento, aprieta los isquios y vuelve lento hasta casi extender las piernas.",
                    "Respiración: exhala al flexionar e inspira al volver; no levantes la cadera del asiento.");
            case CURL_FEMORAL_NORDICO -> List.of(
                    "Postura: de rodillas sobre una colchoneta con los tobillos anclados y el cuerpo recto de rodillas a cabeza, glúteos y abdomen apretados.",
                    "Rango: deja caer el cuerpo hacia delante frenando con los isquios lo más lento posible y ayúdate con las manos al final para volver.",
                    "Respiración: inspira antes de bajar y exhala al volver a la posición; no dobles la cadera.");
            case CRUNCH_POLEA -> List.of(
                    "Postura: de rodillas frente a la polea alta, cuerda a los lados de la cabeza y cadera fija sobre las rodillas.",
                    "Rango: enrolla el torso llevando los codos hacia las rodillas hasta contraer el abdomen y vuelve controlado sin sentarte sobre los talones.",
                    "Respiración: exhala fuerte al flexionar e inspira al volver; mueve la columna, no la cadera.");
            case RUEDA_ABDOMINAL -> List.of(
                    "Postura: de rodillas con las manos en la rueda bajo los hombros, abdomen apretado y espalda ligeramente redondeada.",
                    "Rango: rueda hacia delante hasta donde mantengas la espalda neutra, sin hundir la zona lumbar, y vuelve tirando con el abdomen.",
                    "Respiración: inspira al rodar y exhala al volver; acorta el recorrido si la espalda se arquea.");
            case HACK -> List.of(
                    "Postura: espalda y cabeza apoyadas en el respaldo inclinado, hombros bajo las hombreras y pies adelantados sobre la plataforma.",
                    "Rango: baja hasta que los muslos queden paralelos o más abajo sin despegar la espalda y empuja con todo el pie sin bloquear las rodillas.",
                    "Respiración: inspira al bajar y exhala al empujar; las rodillas siguen la línea de los pies.");
            case CURL_INCLINADO -> List.of(
                    "Postura: sentado en el banco inclinado con la espalda apoyada y los brazos colgando detrás del cuerpo, codos fijos.",
                    "Rango: flexiona desde el estiramiento máximo sin adelantar los codos hasta contraer el bíceps y baja despacio hasta casi extender.",
                    "Respiración: exhala al subir e inspira al bajar lento; no despegues la espalda del banco.");
            case TALONES_SENTADO -> List.of(
                    "Postura: sentado con las rodillas a 90°, la almohadilla sobre los muslos y la parte delantera de los pies en la plataforma.",
                    "Rango: sube los talones lo máximo posible, pausa arriba y baja hasta estirar el sóleo sin rebotar.",
                    "Respiración: exhala al subir e inspira al bajar lento.");
            case FLEXION_DECLINADA -> List.of(
                    "Postura: pies sobre un banco y manos en el suelo bajo los hombros; cuerpo recto con abdomen y glúteos apretados.",
                    "Rango: baja el pecho hasta casi tocar el suelo con los codos a unos 45° y empuja hasta extenderlos; trabaja más el pecho alto y el hombro.",
                    "Respiración: inspira al bajar y exhala al empujar.");
            case PISTOL -> List.of(
                    "Postura: una pierna de apoyo con el pie entero en el suelo, la otra extendida al frente y los brazos adelante como contrapeso.",
                    "Rango: baja con control hasta donde mantengas el talón apoyado y la rodilla alineada con el pie; apóyate en un soporte si hace falta.",
                    "Respiración: inspira al bajar y exhala al subir; evita que la rodilla caiga hacia dentro.");
            case PLANCHA_ALTA -> List.of(
                    "Postura: manos bajo los hombros, brazos extendidos y cuerpo recto de cabeza a talones con abdomen y glúteos apretados.",
                    "Tensión: mantén la posición sin girar ni subir la cadera; si tocas el hombro contrario, hazlo sin balancearte.",
                    "Respiración: lenta y constante, sin contener el aire.");
            default -> perfil.patron().getPuntosClave();
        };
    }

    private static boolean contiene(String texto, String... claves) {
        for (String clave : claves) {
            if (texto.contains(clave)) {
                return true;
            }
        }
        return false;
    }

    public static PatronMovimiento detectar(String nombre, GrupoMuscular grupo) {
        String n = nombre == null ? "" : normalizar(nombre);

        if (n.contains("burpee")) {
            return PatronMovimiento.BURPEE;
        }
        if (n.contains("extension de cuadriceps") || n.contains("extension de pierna")) {
            return PatronMovimiento.EXTENSION_CUADRICEPS;
        }
        // Pec deck y aperturas son un cierre de brazos en abanico, no un press.
        if (n.contains("pec deck") || n.contains("mariposa") || (n.contains("apertura") && !n.contains("mancuerna"))) {
            return PatronMovimiento.APERTURA_PECHO;
        }

        if (n.contains("rueda")) {
            return PatronMovimiento.CRUNCH;
        }
        if (n.contains("hip thrust")) {
            return PatronMovimiento.HIP_THRUST;
        }
        if (n.contains("puente de gluteos")) {
            return PatronMovimiento.PUENTE_GLUTEOS;
        }
        if (n.contains("peso muerto")) {
            return PatronMovimiento.PESO_MUERTO;
        }
        if (n.contains("zancada") || n.contains("bulgara")) {
            return PatronMovimiento.ZANCADA;
        }
        if (n.contains("sentadilla") || n.contains("hack") || n.contains("prensa") || n.contains("pistol")) {
            return PatronMovimiento.SENTADILLA;
        }
        if (n.contains("pica") || n.contains("pike")) {
            return PatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("flexion")) {
            return PatronMovimiento.FLEXION;
        }
        if (n.contains("plancha")) {
            return PatronMovimiento.PLANCHA;
        }
        if (n.contains("fondos")) {
            return PatronMovimiento.FONDOS;
        }
        if (n.contains("dominada") || n.contains("chin-up") || n.contains("chin up")) {
            return PatronMovimiento.DOMINADA;
        }
        if (n.contains("jalon")) {
            return PatronMovimiento.JALON;
        }
        if (n.contains("superman")) {
            return PatronMovimiento.SUPERMAN;
        }
        if (n.contains("remo")) {
            return PatronMovimiento.REMO;
        }
        if (n.contains("elevacion lateral")) {
            return PatronMovimiento.ELEVACION_LATERAL;
        }
        if (n.contains("press inclinado")) {
            return PatronMovimiento.PRESS_INCLINADO;
        }
        if (n.contains("press militar") || n.contains("press de hombro") || n.contains("hombros")) {
            return PatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("apertura")) {
            return PatronMovimiento.PRESS_PLANO;
        }
        if (n.contains("press")) {
            return PatronMovimiento.PRESS_PLANO;
        }
        if (n.contains("curl femoral")) {
            return PatronMovimiento.CURL_FEMORAL;
        }
        if (n.contains("curl")) {
            return PatronMovimiento.CURL_BICEPS;
        }
        if (n.contains("triceps")) {
            return PatronMovimiento.EXTENSION_TRICEPS;
        }
        if (n.contains("pantorrilla") || n.contains("talones")) {
            return PatronMovimiento.ELEVACION_TALONES;
        }
        if (n.contains("elevacion de piernas") || n.contains("dead bug") || n.contains("rodillas al pecho")) {
            return PatronMovimiento.ELEVACION_PIERNAS;
        }
        if (n.contains("crunch") || n.contains("abdominal") || n.contains("rueda")) {
            return PatronMovimiento.CRUNCH;
        }
        // Respaldo por palabras clave de ejercicios personalizados.
        if (n.contains("extension")) {
            return grupo == GrupoMuscular.CUADRICEPS ? PatronMovimiento.SENTADILLA
                    : PatronMovimiento.EXTENSION_TRICEPS;
        }
        if (n.contains("elevacion") || n.contains("vuelo") || n.contains("apertura")) {
            return grupo == GrupoMuscular.PANTORRILLAS ? PatronMovimiento.ELEVACION_TALONES
                    : PatronMovimiento.ELEVACION_LATERAL;
        }
        if (n.contains("tiron") || n.contains("pull") || n.contains("row")) {
            return PatronMovimiento.REMO;
        }
        if (n.contains("squat") || n.contains("cuclilla")) {
            return PatronMovimiento.SENTADILLA;
        }
        if (n.contains("lunge") || n.contains("avance")) {
            return PatronMovimiento.ZANCADA;
        }
        if (n.contains("core") || n.contains("sit-up") || n.contains("situp") || n.contains("encogimiento")) {
            return PatronMovimiento.CRUNCH;
        }
        if (n.contains("bisagra") || n.contains("buenos dias") || n.contains("good morning")) {
            return PatronMovimiento.PESO_MUERTO;
        }
        return porGrupo(grupo);
    }

    /** Patron representativo de un grupo muscular, usado como respaldo. */
    public static PatronMovimiento porGrupo(GrupoMuscular grupo) {
        if (grupo == null) {
            return PatronMovimiento.SENTADILLA;
        }
        return switch (grupo) {
            case PECHO -> PatronMovimiento.PRESS_PLANO;
            case ESPALDA -> PatronMovimiento.REMO;
            case HOMBROS -> PatronMovimiento.PRESS_HOMBRO;
            case BICEPS -> PatronMovimiento.CURL_BICEPS;
            case TRICEPS -> PatronMovimiento.EXTENSION_TRICEPS;
            case CUADRICEPS -> PatronMovimiento.SENTADILLA;
            case ISQUIOTIBIALES -> PatronMovimiento.PESO_MUERTO;
            case GLUTEOS -> PatronMovimiento.HIP_THRUST;
            case PANTORRILLAS -> PatronMovimiento.ELEVACION_TALONES;
            case ABDOMEN -> PatronMovimiento.CRUNCH;
        };
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
