package com.bey.desktop.service;

import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyImplemento;
import com.bey.desktop.model.beyModalidadEntrenamiento;
import com.bey.desktop.model.beyPatronMovimiento;
import com.bey.desktop.model.beyVarianteMovimiento;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class beyGuiaTecnicaService {
    private beyGuiaTecnicaService() {
    }

    public record beyPerfil(beyPatronMovimiento patron, beyImplemento implemento, beyVarianteMovimiento variante) {
    }

    public static beyPerfil beyPerfil(String nombre, beyGrupoMuscular grupo, beyModalidadEntrenamiento modalidad) {
        String n = nombre == null ? "" : beyNormalizar(nombre);
        boolean casa = modalidad == beyModalidadEntrenamiento.CASA;
        beyPatronMovimiento patron = beyDetectar(nombre, grupo);

        if (casa && patron == beyPatronMovimiento.JALON) {
            patron = beyPatronMovimiento.DOMINADA;
        }
        beyImplemento implemento = beyDetectarImplemento(n, patron, casa);
        beyVarianteMovimiento variante = beyDetectarVariante(n, patron, implemento);

        switch (variante) {
            case REMO_INVERTIDO, FLEXION_PICA, PICA_ELEVADA, CURL_ISOMETRICO_TOALLA, FONDOS_SILLA,
                 FLEXION_DECLINADA, PISTOL, PLANCHA_ALTA, CURL_FEMORAL_NORDICO, RUEDA_ABDOMINAL ->
                    implemento = beyImplemento.NINGUNO;
            case JALON_NEUTRO -> implemento = beyImplemento.POLEA;
            case CRUNCH_POLEA -> implemento = beyImplemento.POLEA;
            case CURL_FEMORAL_SENTADO, HACK, PRENSA, PRESS_SENTADO, TALONES_SENTADO -> {
                if (implemento != beyImplemento.MAQUINA) {
                    implemento = beyImplemento.MAQUINA;
                }
            }
            default -> {
            }
        }
        if (patron == beyPatronMovimiento.APERTURA_PECHO || patron == beyPatronMovimiento.EXTENSION_CUADRICEPS) {
            implemento = beyImplemento.MAQUINA;
        }

        if (implemento == beyImplemento.MAQUINA && !beyTienePoseDeMaquina(patron, variante)) {
            implemento = switch (patron) {
                case CURL_BICEPS, ELEVACION_LATERAL, EXTENSION_TRICEPS, CRUNCH -> beyImplemento.POLEA;
                case ZANCADA -> beyImplemento.MANCUERNA;
                case HIP_THRUST, PESO_MUERTO -> beyImplemento.BARRA;
                default -> casa ? beyImplemento.NINGUNO : beyImplemento.BARRA;
            };
        }
        return new beyPerfil(patron, implemento, variante);
    }

    private static boolean beyTienePoseDeMaquina(beyPatronMovimiento patron, beyVarianteMovimiento variante) {
        return switch (patron) {
            case SENTADILLA, JALON, CURL_FEMORAL, ELEVACION_TALONES, APERTURA_PECHO, EXTENSION_CUADRICEPS -> true;
            case PRESS_PLANO, PRESS_INCLINADO -> variante == beyVarianteMovimiento.PRESS_SENTADO;
            case PRESS_HOMBRO -> variante == beyVarianteMovimiento.PRESS_HOMBRO_SENTADO;
            case REMO -> variante == beyVarianteMovimiento.REMO_SENTADO;
            default -> false;
        };
    }

    private static beyImplemento beyDetectarImplemento(String n, beyPatronMovimiento patron, boolean casa) {
        switch (patron) {
            case FLEXION, PLANCHA, DOMINADA, FONDOS, ELEVACION_PIERNAS, CRUNCH, SUPERMAN,
                 PUENTE_GLUTEOS, BURPEE -> {
                return beyImplemento.NINGUNO;
            }
            case JALON, EXTENSION_TRICEPS -> {
                if (n.contains("mancuerna")) {
                    return beyImplemento.MANCUERNA;
                }
                if (n.contains("barra") && patron == beyPatronMovimiento.EXTENSION_TRICEPS) {
                    return beyImplemento.BARRA;
                }
                return casa && !beyContiene(n, "polea", "cuerda", "cable") ? beyImplemento.NINGUNO : beyImplemento.POLEA;
            }
            default -> {
            }
        }
        if (beyContiene(n, "sin carga", "sin equipo", "peso corporal", "libre", "sin peso")) {
            return beyImplemento.NINGUNO;
        }

        if (patron == beyPatronMovimiento.REMO && n.contains("pecho apoyado")) {
            return beyImplemento.MAQUINA;
        }

        if (patron == beyPatronMovimiento.PRESS_HOMBRO && beyContiene(n, "maquina") && !beyContiene(n, "mancuerna", "barra")) {
            return beyImplemento.MAQUINA;
        }
        if (beyContiene(n, "mancuerna", "mochila", "goblet", "kettlebell", "pesa rusa")) {
            return beyImplemento.MANCUERNA;
        }
        if (patron == beyPatronMovimiento.HIP_THRUST && n.contains("barra")) {
            return beyImplemento.BARRA;
        }
        if (beyContiene(n, "polea", "cuerda", "cable")) {
            return beyImplemento.POLEA;
        }
        if (beyContiene(n, "maquina", "prensa", "pec deck", "hack", "smith", "multipower")) {
            return beyImplemento.MAQUINA;
        }
        if (beyContiene(n, "barra", "pendlay", "banca")) {
            return beyImplemento.BARRA;
        }
        if (casa) {
            return beyImplemento.NINGUNO;
        }

        return switch (patron) {
            case SENTADILLA, PRESS_PLANO, PRESS_INCLINADO, PRESS_HOMBRO, PESO_MUERTO, HIP_THRUST, REMO ->
                    beyImplemento.BARRA;
            case CURL_BICEPS, ELEVACION_LATERAL, ZANCADA -> beyImplemento.MANCUERNA;
            case CURL_FEMORAL, ELEVACION_TALONES -> beyImplemento.MAQUINA;
            default -> beyImplemento.NINGUNO;
        };
    }

    private static beyVarianteMovimiento beyDetectarVariante(String n, beyPatronMovimiento patron, beyImplemento imp) {
        boolean sentado = beyContiene(n, "sentado", "en maquina");
        switch (patron) {
            case APERTURA_PECHO -> {
                if (n.contains("inverso") || n.contains("posterior")) {
                    return beyVarianteMovimiento.APERTURA_INVERSA;
                }
            }
            case JALON -> {
                if (n.contains("neutro")) {
                    return beyVarianteMovimiento.JALON_NEUTRO;
                }
            }
            case CURL_FEMORAL -> {
                if (n.contains("nordico")) {
                    return beyVarianteMovimiento.CURL_FEMORAL_NORDICO;
                }
                if (sentado || imp == beyImplemento.MAQUINA && !n.contains("tumbado")) {
                    return n.contains("tumbado") ? beyVarianteMovimiento.ESTANDAR : beyVarianteMovimiento.CURL_FEMORAL_SENTADO;
                }
            }
            case CRUNCH -> {
                if (n.contains("rueda")) {
                    return beyVarianteMovimiento.RUEDA_ABDOMINAL;
                }
                if (beyContiene(n, "polea", "cable")) {
                    return beyVarianteMovimiento.CRUNCH_POLEA;
                }
            }
            case ELEVACION_TALONES -> {
                if (n.contains("sentado")) {
                    return beyVarianteMovimiento.TALONES_SENTADO;
                }
            }
            case PLANCHA -> {
                if (beyContiene(n, "toque", "manos", "alta")) {
                    return beyVarianteMovimiento.PLANCHA_ALTA;
                }
            }
            case SENTADILLA -> {
                if (n.contains("prensa")) {
                    return beyVarianteMovimiento.PRENSA;
                }
                if (n.contains("hack")) {
                    return beyVarianteMovimiento.HACK;
                }
                if (beyContiene(n, "pistol", "una pierna")) {
                    return beyVarianteMovimiento.PISTOL;
                }
            }
            case PRESS_PLANO, PRESS_INCLINADO -> {
                if (imp == beyImplemento.MAQUINA || n.contains("pec deck")) {
                    return beyVarianteMovimiento.PRESS_SENTADO;
                }
            }
            case REMO -> {
                if (beyContiene(n, "invertido", "mesa", "australiano")) {
                    return beyVarianteMovimiento.REMO_INVERTIDO;
                }
                if (imp == beyImplemento.MAQUINA || imp == beyImplemento.POLEA
                        || beyContiene(n, "sentado", "pecho apoyado", "polea baja")) {
                    return beyVarianteMovimiento.REMO_SENTADO;
                }
                if (imp == beyImplemento.NINGUNO && !beyContiene(n, "inclinado", "pendlay")) {
                    return beyVarianteMovimiento.REMO_INVERTIDO;
                }
            }
            case FONDOS -> {
                if (beyContiene(n, "silla", "banco", "sofa", "cama")) {
                    return beyVarianteMovimiento.FONDOS_SILLA;
                }
            }
            case FLEXION -> {
                if (n.contains("rodillas")) {
                    return beyVarianteMovimiento.FLEXION_RODILLAS;
                }
                if (beyContiene(n, "declinada", "pies elevados")) {
                    return beyVarianteMovimiento.FLEXION_DECLINADA;
                }
            }
            case PRESS_HOMBRO -> {
                if (beyContiene(n, "pica", "pike")) {
                    return beyContiene(n, "pies elevados", "elevados") ? beyVarianteMovimiento.PICA_ELEVADA
                            : beyVarianteMovimiento.FLEXION_PICA;
                }
                if (imp == beyImplemento.MAQUINA || n.contains("sentado")) {
                    return beyVarianteMovimiento.PRESS_HOMBRO_SENTADO;
                }
            }
            case CURL_BICEPS -> {
                if (n.contains("toalla") || n.contains("isometric")) {
                    return beyVarianteMovimiento.CURL_ISOMETRICO_TOALLA;
                }
                if (n.contains("inclinado")) {
                    return beyVarianteMovimiento.CURL_INCLINADO;
                }
            }
            case ZANCADA -> {
                if (beyContiene(n, "bulgara")) {
                    return beyVarianteMovimiento.ZANCADA_BULGARA;
                }
            }
            case EXTENSION_TRICEPS -> {
                if (beyContiene(n, "sobre", "cabeza", "frances") || imp != beyImplemento.POLEA) {
                    return beyVarianteMovimiento.TRICEPS_SOBRE_CABEZA;
                }
            }
            default -> {
            }
        }
        return beyVarianteMovimiento.ESTANDAR;
    }

    public static String beyTitulo(beyPerfil perfil) {
        return switch (perfil.variante()) {
            case REMO_INVERTIDO -> "Remo invertido bajo una mesa";
            case REMO_SENTADO -> perfil.implemento() == beyImplemento.MAQUINA ? "Remo sentado en máquina" : "Remo sentado en polea";
            case FLEXION_PICA -> "Flexión pica (pike push-up)";
            case PICA_ELEVADA -> "Flexión pica con pies elevados";
            case CURL_ISOMETRICO_TOALLA -> "Curl isométrico con toalla";
            case JALON_NEUTRO -> "Jalón al pecho con agarre neutro";
            case PRESS_SENTADO -> perfil.patron() == beyPatronMovimiento.PRESS_INCLINADO
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

    public static List<String> beyPuntosClave(beyPerfil perfil) {
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
            case PRESS_SENTADO -> perfil.patron() == beyPatronMovimiento.PRESS_INCLINADO ? List.of(
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

    private static boolean beyContiene(String texto, String... claves) {
        for (String clave : claves) {
            if (texto.contains(clave)) {
                return true;
            }
        }
        return false;
    }

    public static beyPatronMovimiento beyDetectar(String nombre, beyGrupoMuscular grupo) {
        String n = nombre == null ? "" : beyNormalizar(nombre);

        if (n.contains("burpee")) {
            return beyPatronMovimiento.BURPEE;
        }
        if (n.contains("extension de cuadriceps") || n.contains("extension de pierna")) {
            return beyPatronMovimiento.EXTENSION_CUADRICEPS;
        }

        if (n.contains("pec deck") || n.contains("mariposa") || (n.contains("apertura") && !n.contains("mancuerna"))) {
            return beyPatronMovimiento.APERTURA_PECHO;
        }

        if (n.contains("rueda")) {
            return beyPatronMovimiento.CRUNCH;
        }
        if (n.contains("hip thrust")) {
            return beyPatronMovimiento.HIP_THRUST;
        }
        if (n.contains("puente de gluteos")) {
            return beyPatronMovimiento.PUENTE_GLUTEOS;
        }
        if (n.contains("peso muerto")) {
            return beyPatronMovimiento.PESO_MUERTO;
        }
        if (n.contains("zancada") || n.contains("bulgara")) {
            return beyPatronMovimiento.ZANCADA;
        }
        if (n.contains("sentadilla") || n.contains("hack") || n.contains("prensa") || n.contains("pistol")) {
            return beyPatronMovimiento.SENTADILLA;
        }
        if (n.contains("pica") || n.contains("pike")) {
            return beyPatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("flexion")) {
            return beyPatronMovimiento.FLEXION;
        }
        if (n.contains("plancha")) {
            return beyPatronMovimiento.PLANCHA;
        }
        if (n.contains("fondos")) {
            return beyPatronMovimiento.FONDOS;
        }
        if (n.contains("dominada") || n.contains("chin-up") || n.contains("chin up")) {
            return beyPatronMovimiento.DOMINADA;
        }
        if (n.contains("jalon")) {
            return beyPatronMovimiento.JALON;
        }
        if (n.contains("superman")) {
            return beyPatronMovimiento.SUPERMAN;
        }
        if (n.contains("remo")) {
            return beyPatronMovimiento.REMO;
        }
        if (n.contains("elevacion lateral")) {
            return beyPatronMovimiento.ELEVACION_LATERAL;
        }
        if (n.contains("press inclinado")) {
            return beyPatronMovimiento.PRESS_INCLINADO;
        }
        if (n.contains("press militar") || n.contains("press de hombro") || n.contains("hombros")) {
            return beyPatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("apertura")) {
            return beyPatronMovimiento.PRESS_PLANO;
        }
        if (n.contains("press")) {
            return beyPatronMovimiento.PRESS_PLANO;
        }
        if (n.contains("curl femoral")) {
            return beyPatronMovimiento.CURL_FEMORAL;
        }
        if (n.contains("curl")) {
            return beyPatronMovimiento.CURL_BICEPS;
        }
        if (n.contains("triceps")) {
            return beyPatronMovimiento.EXTENSION_TRICEPS;
        }
        if (n.contains("pantorrilla") || n.contains("talones")) {
            return beyPatronMovimiento.ELEVACION_TALONES;
        }
        if (n.contains("elevacion de piernas") || n.contains("dead bug") || n.contains("rodillas al pecho")) {
            return beyPatronMovimiento.ELEVACION_PIERNAS;
        }
        if (n.contains("crunch") || n.contains("abdominal") || n.contains("rueda")) {
            return beyPatronMovimiento.CRUNCH;
        }

        if (n.contains("extension")) {
            return grupo == beyGrupoMuscular.CUADRICEPS ? beyPatronMovimiento.SENTADILLA
                    : beyPatronMovimiento.EXTENSION_TRICEPS;
        }
        if (n.contains("elevacion") || n.contains("vuelo") || n.contains("apertura")) {
            return grupo == beyGrupoMuscular.PANTORRILLAS ? beyPatronMovimiento.ELEVACION_TALONES
                    : beyPatronMovimiento.ELEVACION_LATERAL;
        }
        if (n.contains("tiron") || n.contains("pull") || n.contains("row")) {
            return beyPatronMovimiento.REMO;
        }
        if (n.contains("squat") || n.contains("cuclilla")) {
            return beyPatronMovimiento.SENTADILLA;
        }
        if (n.contains("lunge") || n.contains("avance")) {
            return beyPatronMovimiento.ZANCADA;
        }
        if (n.contains("core") || n.contains("sit-up") || n.contains("situp") || n.contains("encogimiento")) {
            return beyPatronMovimiento.CRUNCH;
        }
        if (n.contains("bisagra") || n.contains("buenos dias") || n.contains("good morning")) {
            return beyPatronMovimiento.PESO_MUERTO;
        }
        return beyPorGrupo(grupo);
    }

    public static beyPatronMovimiento beyPorGrupo(beyGrupoMuscular grupo) {
        if (grupo == null) {
            return beyPatronMovimiento.SENTADILLA;
        }
        return switch (grupo) {
            case PECHO -> beyPatronMovimiento.PRESS_PLANO;
            case ESPALDA -> beyPatronMovimiento.REMO;
            case HOMBROS -> beyPatronMovimiento.PRESS_HOMBRO;
            case BICEPS -> beyPatronMovimiento.CURL_BICEPS;
            case TRICEPS -> beyPatronMovimiento.EXTENSION_TRICEPS;
            case CUADRICEPS -> beyPatronMovimiento.SENTADILLA;
            case ISQUIOTIBIALES -> beyPatronMovimiento.PESO_MUERTO;
            case GLUTEOS -> beyPatronMovimiento.HIP_THRUST;
            case PANTORRILLAS -> beyPatronMovimiento.ELEVACION_TALONES;
            case ABDOMEN -> beyPatronMovimiento.CRUNCH;
        };
    }

    private static String beyNormalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
