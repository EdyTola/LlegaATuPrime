package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.DiaSemana;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.PerfilUsuario;
import com.symmetry.desktop.model.Rutina;

import java.time.LocalDate;

/**
 * Construye la base de datos inicial de Symmetry Desktop: perfil del usuario y
 * las cuatro rutinas precargadas (Torso A, Pierna A, Torso B, Pierna B).
 * Todos los ejercicios son editables despues mediante el CRUD del modulo de entrenamiento.
 */
public final class DatosInicialesService {

    private DatosInicialesService() {
    }

    public static BaseDatos crearBaseDatosInicial() {
        BaseDatos baseDatos = new BaseDatos();
        baseDatos.setPerfil(new PerfilUsuario("Yoel Fernando"));
        baseDatos.getPerfil().getObjetivos().setProteinaGramos(145);
        baseDatos.getPerfil().getObjetivos().setCalorias(2400);
        baseDatos.getPerfil().getObjetivos().setAguaMililitros(2800);
        baseDatos.getPerfil().getObjetivos().setCreatinaGramos(5);
        baseDatos.getPerfil().getObjetivos().setSuenoMinimoHoras(7);
        baseDatos.getPerfil().getObjetivos().setSuenoMaximoHoras(9);
        baseDatos.getPerfil().setFechaRegistro(LocalDate.now());

        baseDatos.getRutinas().add(torsoA());
        baseDatos.getRutinas().add(piernaA());
        baseDatos.getRutinas().add(torsoB());
        baseDatos.getRutinas().add(piernaB());

        return baseDatos;
    }

    private static Rutina torsoA() {
        Rutina rutina = new Rutina("Torso A", DiaSemana.LUNES);
        rutina.setNotas("Énfasis en espalda y pecho. Mantener RIR 1-2 en todas las series.");
        rutina.agregarEjercicio(ejercicio("Jalón al pecho agarre medio", GrupoMuscular.ESPALDA, 3, 6, 10, 1, 120));
        rutina.agregarEjercicio(ejercicio("Press inclinado en máquina", GrupoMuscular.PECHO, 3, 6, 10, 1, 120));
        rutina.agregarEjercicio(ejercicio("Remo con pecho apoyado", GrupoMuscular.ESPALDA, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Elevación lateral en máquina o cable", GrupoMuscular.HOMBROS, 3, 12, 20, 1, 90));
        rutina.agregarEjercicio(ejercicio("Pec deck inverso", GrupoMuscular.HOMBROS, 2, 12, 20, 1, 90));
        rutina.agregarEjercicio(ejercicio("Curl de bíceps en máquina o cable", GrupoMuscular.BICEPS, 2, 8, 12, 1, 90));
        rutina.agregarEjercicio(ejercicio("Extensión de tríceps con cuerda", GrupoMuscular.TRICEPS, 2, 10, 15, 1, 90));
        return rutina;
    }

    private static Rutina piernaA() {
        Rutina rutina = new Rutina("Pierna A", DiaSemana.MARTES);
        rutina.setNotas("Énfasis en cuádriceps y glúteo. RIR 2 en los ejercicios principales.");
        rutina.agregarEjercicio(ejercicio("Hack squat o sentadilla en máquina", GrupoMuscular.CUADRICEPS, 3, 6, 10, 2, 150));
        rutina.agregarEjercicio(ejercicio("Hip thrust en máquina o barra", GrupoMuscular.GLUTEOS, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Prensa de piernas", GrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 120));
        rutina.agregarEjercicio(ejercicio("Curl femoral sentado", GrupoMuscular.ISQUIOTIBIALES, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(ejercicio("Extensión de cuádriceps", GrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(ejercicio("Elevación de pantorrillas", GrupoMuscular.PANTORRILLAS, 3, 10, 15, 1, 60));
        rutina.agregarEjercicio(ejercicio("Crunch en polea o máquina", GrupoMuscular.ABDOMEN, 2, 10, 15, 1, 60));
        return rutina;
    }

    private static Rutina torsoB() {
        Rutina rutina = new Rutina("Torso B", DiaSemana.JUEVES);
        rutina.setNotas("Variante de torso con agarre neutro y mayor volumen de deltoides medio.");
        rutina.agregarEjercicio(ejercicio("Jalón al pecho agarre neutro", GrupoMuscular.ESPALDA, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Press de hombros en máquina", GrupoMuscular.HOMBROS, 2, 6, 10, 1, 120));
        rutina.agregarEjercicio(ejercicio("Remo sentado en cable", GrupoMuscular.ESPALDA, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Pec deck para pecho", GrupoMuscular.PECHO, 3, 10, 15, 1, 90));
        rutina.agregarEjercicio(ejercicio("Elevación lateral en máquina o cable", GrupoMuscular.HOMBROS, 3, 12, 20, 1, 90));
        rutina.agregarEjercicio(ejercicio("Curl de bíceps inclinado", GrupoMuscular.BICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(ejercicio("Extensión de tríceps sobre cabeza", GrupoMuscular.TRICEPS, 2, 10, 15, 1, 90));
        return rutina;
    }

    private static Rutina piernaB() {
        Rutina rutina = new Rutina("Pierna B", DiaSemana.VIERNES);
        rutina.setNotas("Énfasis en cadena posterior. RIR 2 en peso muerto rumano.");
        rutina.agregarEjercicio(ejercicio("Peso muerto rumano", GrupoMuscular.ISQUIOTIBIALES, 3, 6, 10, 2, 150));
        rutina.agregarEjercicio(ejercicio("Sentadilla búlgara", GrupoMuscular.CUADRICEPS, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Curl femoral", GrupoMuscular.ISQUIOTIBIALES, 3, 8, 12, 1, 90));
        rutina.agregarEjercicio(ejercicio("Hip thrust", GrupoMuscular.GLUTEOS, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(ejercicio("Extensión de cuádriceps", GrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(ejercicio("Elevación de pantorrillas", GrupoMuscular.PANTORRILLAS, 3, 10, 15, 1, 60));

        Ejercicio plancha = ejercicio("Plancha abdominal", GrupoMuscular.ABDOMEN, 2, 30, 60, 1, 60);
        plancha.setPorTiempo(true);
        plancha.setNotas("Mantener línea neutra de cadera y respiración controlada.");
        rutina.agregarEjercicio(plancha);
        return rutina;
    }

    private static Ejercicio ejercicio(String nombre, GrupoMuscular grupo, int series, int repsMin,
                                       int repsMax, int rir, int descansoSegundos) {
        Ejercicio ejercicio = new Ejercicio(nombre, grupo, series, repsMin, repsMax, rir);
        ejercicio.setDescansoSegundos(descansoSegundos);
        return ejercicio;
    }
}
