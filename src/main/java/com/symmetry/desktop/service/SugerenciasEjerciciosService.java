package com.symmetry.desktop.service;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.ModalidadEntrenamiento;
import com.symmetry.desktop.model.NivelEjercicio;

import java.util.List;
import java.util.Map;

/**
 * Catalogo de ejercicios sugeridos por nivel de experiencia. Alimenta el panel
 * lateral de sugerencias del modulo de entrenamiento; cada llamada devuelve
 * instancias nuevas para poder anadirlas a una rutina sin compartir estado.
 */
public final class SugerenciasEjerciciosService {

    /** Descripciones tecnicas breves (1-2 lineas) por ejercicio de los catalogos. */
    private static final Map<String, String> DESCRIPCIONES = Map.ofEntries(
            // --- Con equipo: principiante ---
            Map.entry("Prensa de piernas", "Empuja la plataforma sin bloquear las rodillas. Base segura para ganar fuerza de piernas."),
            Map.entry("Jalón al pecho en polea", "Lleva la barra a la clavícula con el pecho alto. Enseña el patrón de tracción vertical."),
            Map.entry("Press de pecho en máquina", "Trayectoria guiada que permite empujar con control y aprender el movimiento."),
            Map.entry("Remo sentado en máquina", "Junta las escápulas al tirar y evita balancear el torso. Trabaja el grosor de la espalda."),
            Map.entry("Curl femoral tumbado", "Flexiona la rodilla sin despegar la cadera del banco. Aísla los isquiotibiales."),
            Map.entry("Elevación lateral con mancuernas", "Sube hasta la altura del hombro con el codo ligeramente flexionado. Ensancha el hombro."),
            Map.entry("Curl de bíceps con mancuernas", "Codos pegados al torso y bajada lenta. Aísla el bíceps sin impulso."),
            Map.entry("Extensión de tríceps en polea", "Codos fijos junto al cuerpo; extiende por completo. Aislamiento seguro del tríceps."),
            Map.entry("Elevación de pantorrillas de pie", "Sube al máximo y baja con pausa abajo. Rango completo para desarrollar el gemelo."),
            Map.entry("Crunch abdominal", "Enrolla el torso exhalando, sin tirar del cuello. Activa el recto abdominal."),
            // --- Con equipo: intermedio ---
            Map.entry("Sentadilla con barra", "Baja con la espalda neutra hasta la paralela. Rey de los ejercicios de fuerza de pierna."),
            Map.entry("Peso muerto rumano", "Lleva la cadera atrás con las piernas casi rectas. Estira y fortalece isquios y glúteos."),
            Map.entry("Press de banca con barra", "Escápulas retraídas y barra al pecho medio. Principal constructor de fuerza en el pecho."),
            Map.entry("Remo con barra", "Torso inclinado y espalda recta; tira hacia el ombligo. Gran estímulo para toda la espalda."),
            Map.entry("Hip thrust con barra", "Empuja con los talones y aprieta el glúteo arriba. El mejor estímulo directo del glúteo."),
            Map.entry("Press militar con barra", "Empuja la barra sobre la cabeza con el core firme. Fuerza y volumen de hombros."),
            Map.entry("Dominadas asistidas", "Tracción vertical con ayuda parcial. Progresa hacia la dominada completa."),
            Map.entry("Zancadas con mancuernas", "Paso largo con la rodilla alineada al pie. Trabaja pierna y equilibrio de forma unilateral."),
            Map.entry("Curl de bíceps con barra", "Agarre supino y sin balanceo; permite cargar más peso en el bíceps."),
            Map.entry("Extensión de tríceps con cuerda", "Abre la cuerda al final del recorrido para contraer más el tríceps."),
            // --- Con equipo: avanzado ---
            Map.entry("Sentadilla libre", "Sentadilla profunda con técnica estricta. Fuerza de tren inferior, con o sin carga."),
            Map.entry("Peso muerto convencional", "Tracción desde el suelo con espalda neutra. Ejercicio de cadena posterior completa."),
            Map.entry("Press de banca pesado", "Series cortas y pesadas con pausa en el pecho. Máxima fuerza de empuje."),
            Map.entry("Dominadas lastradas", "Dominada estricta con peso añadido. Potencia la espalda ancha y los bíceps."),
            Map.entry("Remo Pendlay", "Remo explosivo desde el suelo con el torso paralelo. Fuerza y potencia de espalda."),
            Map.entry("Press militar de pie", "Empuje vertical sin impulso de piernas. Hombros fuertes y core estable."),
            Map.entry("Hip thrust pesado", "Cargas altas con pausa arriba. Máxima activación del glúteo mayor."),
            Map.entry("Fondos en paralelas con lastre", "Inclínate adelante para cargar el pecho. Empuje pesado con peso corporal añadido."),
            Map.entry("Curl femoral nórdico", "Baja el torso controlando con los isquios. Exigente y protector frente a lesiones."),
            Map.entry("Rueda abdominal", "Extiende sin hundir la zona lumbar. Core de alta exigencia."),
            // --- Sin equipo ---
            Map.entry("Flexiones con rodillas apoyadas", "Versión asequible de la flexión: aprende el patrón de empuje con el core activo."),
            Map.entry("Flexiones clásicas", "Cuerpo en plancha y pecho al suelo. Empuje completo sin material."),
            Map.entry("Flexiones declinadas (pies elevados)", "Pies sobre una silla para cargar más pecho alto y hombro."),
            Map.entry("Superman con pausa", "Eleva brazos y piernas boca abajo y mantén. Activa la espalda baja y los erectores."),
            Map.entry("Remo invertido bajo una mesa", "Tira del pecho hacia el borde de una mesa estable. Espalda sin necesitar máquinas."),
            Map.entry("Dominadas en barra o marco de puerta", "Tracción vertical con tu peso corporal. El gran ejercicio de espalda en casa."),
            Map.entry("Plancha con toque de hombro", "Toca el hombro contrario sin girar la cadera. Hombros y core estables."),
            Map.entry("Flexiones pica (pike push-up)", "Cadera alta en V y cabeza hacia el suelo. Empuje vertical para hombros."),
            Map.entry("Flexiones pica con pies elevados", "Pies sobre una silla para cargar más hombro. Alternativa casera al press militar."),
            Map.entry("Curl isométrico con toalla", "Tira de una toalla contra tu propia resistencia. Bíceps sin mancuernas."),
            Map.entry("Curl con mochila cargada", "Mochila con libros como lastre. Curl progresivo con material de casa."),
            Map.entry("Dominadas supinas (chin-up)", "Agarre supino que carga más el bíceps junto a la espalda."),
            Map.entry("Fondos en silla con rodillas flexionadas", "Baja controlado con los codos atrás. Tríceps con carga reducida."),
            Map.entry("Flexiones diamante", "Manos juntas bajo el pecho. Gran énfasis en el tríceps."),
            Map.entry("Fondos en silla con piernas estiradas", "Piernas rectas para aumentar la carga sobre el tríceps."),
            Map.entry("Sentadilla búlgara", "Pie trasero elevado en una silla. Alta exigencia unilateral para cuádriceps y glúteo."),
            Map.entry("Sentadilla pistol asistida", "Sentadilla a una pierna con apoyo. Fuerza, equilibrio y movilidad."),
            Map.entry("Puente de glúteos a una pierna", "Eleva la cadera con una sola pierna. Glúteo e isquios sin material."),
            Map.entry("Peso muerto a una pierna sin carga", "Bisagra de cadera sobre una pierna. Cadena posterior y equilibrio."),
            Map.entry("Curl femoral nórdico asistido", "Baja controlado apoyándote con las manos. Isquios muy exigidos."),
            Map.entry("Puente de glúteos", "Eleva la cadera y aprieta arriba. Activación base del glúteo."),
            Map.entry("Hip thrust con la espalda en el sofá", "Espalda alta apoyada en el sofá. Versión casera del hip thrust."),
            Map.entry("Hip thrust a una pierna en el sofá", "Misma técnica con una pierna: más carga y control."),
            Map.entry("Elevación de talones en el suelo", "Sube sobre las puntas con pausa arriba. Gemelo sin material."),
            Map.entry("Elevación de talones en escalón", "Mayor recorrido al bajar el talón del escalón."),
            Map.entry("Elevación de talones a una pierna", "Una pierna a la vez: duplica la carga sobre el gemelo."),
            Map.entry("Dead bug", "Extiende brazo y pierna contrarios con la zona lumbar pegada al suelo. Core profundo."),
            Map.entry("Plancha frontal", "Cuerpo recto, glúteos y abdomen apretados. Estabilidad del core."),
            Map.entry("Elevación de piernas colgado o en el suelo", "Sube las piernas sin arquear la espalda. Abdomen inferior."));

    /** Descripcion tecnica breve del ejercicio, o un texto generico segun su grupo muscular. */
    public static String descripcionTecnica(Ejercicio ejercicio) {
        if (ejercicio == null) {
            return "";
        }
        String texto = DESCRIPCIONES.get(ejercicio.getNombre());
        if (texto != null) {
            return texto;
        }
        GrupoMuscular grupo = ejercicio.getGrupoMuscular();
        return grupo == null ? "" : "Ejercicio enfocado en " + grupo.getEtiqueta().toLowerCase()
                + ". Controla la bajada y mantén la técnica en cada repetición.";
    }

    private SugerenciasEjerciciosService() {
    }

    /** Ejercicios sugeridos para el nivel indicado. */
    public static List<Ejercicio> paraNivel(NivelEjercicio nivel) {
        if (nivel == null) {
            return List.of();
        }
        return switch (nivel) {
            case PRINCIPIANTE -> principiante();
            case INTERMEDIO -> intermedio();
            case AVANZADO -> avanzado();
        };
    }

    /** Ejercicios sugeridos para el nivel y la modalidad (con equipo o sin equipo / casa). */
    public static List<Ejercicio> paraNivel(NivelEjercicio nivel, ModalidadEntrenamiento modalidad) {
        if (nivel != null && modalidad == ModalidadEntrenamiento.CASA) {
            return AdaptadorEquipoService.catalogoCasa(nivel);
        }
        return paraNivel(nivel);
    }

    /** Descripcion del nivel adaptada a la modalidad activa. */
    public static String descripcion(NivelEjercicio nivel, ModalidadEntrenamiento modalidad) {
        if (nivel != null && modalidad == ModalidadEntrenamiento.CASA) {
            return switch (nivel) {
                case PRINCIPIANTE -> "Peso corporal con palancas fáciles: base técnica y volumen moderado.";
                case INTERMEDIO -> "Progresiones de calistenia con mayor rango y control del movimiento.";
                case AVANZADO -> "Variantes a una pierna, pies elevados y tensión alta sin material.";
            };
        }
        return descripcion(nivel);
    }

    /** Descripcion breve del enfoque de cada nivel, mostrada en el panel. */
    public static String descripcion(NivelEjercicio nivel) {
        if (nivel == null) {
            return "";
        }
        return switch (nivel) {
            case PRINCIPIANTE -> "Maquinas y mancuernas, repeticiones altas y tecnica como prioridad.";
            case INTERMEDIO -> "Movimientos con barra y volumen moderado para progresar en fuerza.";
            case AVANZADO -> "Series principales pesadas con RIR bajo y mayor exigencia tecnica.";
        };
    }

    private static List<Ejercicio> principiante() {
        return List.of(
                ejercicio("Prensa de piernas", GrupoMuscular.CUADRICEPS, 3, 10, 15, 2, 90),
                ejercicio("Jalón al pecho en polea", GrupoMuscular.ESPALDA, 3, 10, 12, 2, 90),
                ejercicio("Press de pecho en máquina", GrupoMuscular.PECHO, 3, 10, 12, 2, 90),
                ejercicio("Remo sentado en máquina", GrupoMuscular.ESPALDA, 3, 10, 12, 2, 90),
                ejercicio("Curl femoral tumbado", GrupoMuscular.ISQUIOTIBIALES, 3, 10, 12, 2, 60),
                ejercicio("Elevación lateral con mancuernas", GrupoMuscular.HOMBROS, 3, 12, 15, 2, 60),
                ejercicio("Curl de bíceps con mancuernas", GrupoMuscular.BICEPS, 3, 10, 12, 1, 60),
                ejercicio("Extensión de tríceps en polea", GrupoMuscular.TRICEPS, 3, 10, 12, 1, 60),
                ejercicio("Elevación de pantorrillas de pie", GrupoMuscular.PANTORRILLAS, 3, 12, 15, 2, 45),
                ejercicio("Crunch abdominal", GrupoMuscular.ABDOMEN, 3, 15, 20, 2, 45)
        );
    }

    private static List<Ejercicio> intermedio() {
        return List.of(
                ejercicio("Sentadilla con barra", GrupoMuscular.CUADRICEPS, 4, 6, 10, 2, 150),
                ejercicio("Peso muerto rumano", GrupoMuscular.ISQUIOTIBIALES, 3, 8, 10, 2, 120),
                ejercicio("Press de banca con barra", GrupoMuscular.PECHO, 4, 6, 10, 2, 120),
                ejercicio("Remo con barra", GrupoMuscular.ESPALDA, 4, 8, 10, 2, 120),
                ejercicio("Hip thrust con barra", GrupoMuscular.GLUTEOS, 3, 8, 12, 2, 120),
                ejercicio("Press militar con barra", GrupoMuscular.HOMBROS, 3, 8, 10, 2, 120),
                ejercicio("Dominadas asistidas", GrupoMuscular.ESPALDA, 3, 6, 10, 2, 120),
                ejercicio("Zancadas con mancuernas", GrupoMuscular.CUADRICEPS, 3, 10, 12, 2, 90),
                ejercicio("Curl de bíceps con barra", GrupoMuscular.BICEPS, 3, 8, 12, 1, 90),
                ejercicio("Extensión de tríceps con cuerda", GrupoMuscular.TRICEPS, 3, 10, 15, 1, 90)
        );
    }

    private static List<Ejercicio> avanzado() {
        return List.of(
                ejercicio("Sentadilla libre", GrupoMuscular.CUADRICEPS, 5, 5, 8, 1, 180),
                ejercicio("Peso muerto convencional", GrupoMuscular.ISQUIOTIBIALES, 4, 4, 6, 1, 180),
                ejercicio("Press de banca pesado", GrupoMuscular.PECHO, 5, 4, 6, 1, 180),
                ejercicio("Dominadas lastradas", GrupoMuscular.ESPALDA, 4, 5, 8, 1, 150),
                ejercicio("Remo Pendlay", GrupoMuscular.ESPALDA, 4, 5, 8, 1, 150),
                ejercicio("Press militar de pie", GrupoMuscular.HOMBROS, 4, 6, 8, 1, 150),
                ejercicio("Hip thrust pesado", GrupoMuscular.GLUTEOS, 4, 6, 10, 1, 150),
                ejercicio("Fondos en paralelas con lastre", GrupoMuscular.PECHO, 4, 6, 10, 1, 120),
                ejercicio("Curl femoral nórdico", GrupoMuscular.ISQUIOTIBIALES, 4, 6, 8, 1, 120),
                ejercicio("Rueda abdominal", GrupoMuscular.ABDOMEN, 4, 8, 12, 0, 90)
        );
    }

    private static Ejercicio ejercicio(String nombre, GrupoMuscular grupo, int series, int repsMin,
                                       int repsMax, int rir, int descansoSegundos) {
        Ejercicio ejercicio = new Ejercicio(nombre, grupo, series, repsMin, repsMax, rir);
        ejercicio.setDescansoSegundos(descansoSegundos);
        return ejercicio;
    }
}
