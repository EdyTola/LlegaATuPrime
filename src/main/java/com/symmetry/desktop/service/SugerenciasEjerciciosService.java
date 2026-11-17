package com.symmetry.desktop.service;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.NivelEjercicio;

import java.util.List;

/**
 * Catalogo de ejercicios sugeridos por nivel de experiencia. Alimenta el panel
 * lateral de sugerencias del modulo de entrenamiento; cada llamada devuelve
 * instancias nuevas para poder anadirlas a una rutina sin compartir estado.
 */
public final class SugerenciasEjerciciosService {

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
