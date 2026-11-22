package com.bey.desktop.service;

import com.bey.desktop.model.beyBaseDatos;
import com.bey.desktop.model.beyDiaSemana;
import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyPerfilUsuario;
import com.bey.desktop.model.beyRutina;

import java.time.LocalDate;

public final class beyDatosInicialesService {
    private beyDatosInicialesService() {
    }

    public static beyBaseDatos beyCrearBaseDatosInicial() {
        beyBaseDatos baseDatos = new beyBaseDatos();
        baseDatos.setPerfil(new beyPerfilUsuario("Yoel Fernando"));
        baseDatos.getPerfil().getObjetivos().setProteinaGramos(145);
        baseDatos.getPerfil().getObjetivos().setCalorias(2400);
        baseDatos.getPerfil().getObjetivos().setAguaMililitros(2800);
        baseDatos.getPerfil().getObjetivos().setCreatinaGramos(5);
        baseDatos.getPerfil().getObjetivos().setSuenoMinimoHoras(7);
        baseDatos.getPerfil().getObjetivos().setSuenoMaximoHoras(9);
        baseDatos.getPerfil().setFechaRegistro(LocalDate.now());

        baseDatos.getRutinas().add(beyTorsoA());
        baseDatos.getRutinas().add(beyPiernaA());
        baseDatos.getRutinas().add(beyTorsoB());
        baseDatos.getRutinas().add(beyPiernaB());

        return baseDatos;
    }

    private static beyRutina beyTorsoA() {
        beyRutina rutina = new beyRutina("Torso A", beyDiaSemana.LUNES);
        rutina.setNotas("Énfasis en espalda y pecho. Mantener RIR 1-2 en todas las series.");
        rutina.agregarEjercicio(beyEjercicio("Jalón al pecho agarre medio", beyGrupoMuscular.ESPALDA, 3, 6, 10, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Press inclinado en máquina", beyGrupoMuscular.PECHO, 3, 6, 10, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Remo con pecho apoyado", beyGrupoMuscular.ESPALDA, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Elevación lateral en máquina o cable", beyGrupoMuscular.HOMBROS, 3, 12, 20, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Pec deck inverso", beyGrupoMuscular.HOMBROS, 2, 12, 20, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Curl de bíceps en máquina o cable", beyGrupoMuscular.BICEPS, 2, 8, 12, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Extensión de tríceps con cuerda", beyGrupoMuscular.TRICEPS, 2, 10, 15, 1, 90));
        return rutina;
    }

    private static beyRutina beyPiernaA() {
        beyRutina rutina = new beyRutina("Pierna A", beyDiaSemana.MARTES);
        rutina.setNotas("Énfasis en cuádriceps y glúteo. RIR 2 en los ejercicios principales.");
        rutina.agregarEjercicio(beyEjercicio("Hack squat o sentadilla en máquina", beyGrupoMuscular.CUADRICEPS, 3, 6, 10, 2, 150));
        rutina.agregarEjercicio(beyEjercicio("Hip thrust en máquina o barra", beyGrupoMuscular.GLUTEOS, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Prensa de piernas", beyGrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Curl femoral sentado", beyGrupoMuscular.ISQUIOTIBIALES, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Extensión de cuádriceps", beyGrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Elevación de pantorrillas", beyGrupoMuscular.PANTORRILLAS, 3, 10, 15, 1, 60));
        rutina.agregarEjercicio(beyEjercicio("Crunch en polea o máquina", beyGrupoMuscular.ABDOMEN, 2, 10, 15, 1, 60));
        return rutina;
    }

    private static beyRutina beyTorsoB() {
        beyRutina rutina = new beyRutina("Torso B", beyDiaSemana.JUEVES);
        rutina.setNotas("Variante de torso con agarre neutro y mayor volumen de deltoides medio.");
        rutina.agregarEjercicio(beyEjercicio("Jalón al pecho agarre neutro", beyGrupoMuscular.ESPALDA, 3, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Press de hombros en máquina", beyGrupoMuscular.HOMBROS, 2, 6, 10, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Remo sentado en cable", beyGrupoMuscular.ESPALDA, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Pec deck para pecho", beyGrupoMuscular.PECHO, 3, 10, 15, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Elevación lateral en máquina o cable", beyGrupoMuscular.HOMBROS, 3, 12, 20, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Curl de bíceps inclinado", beyGrupoMuscular.BICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Extensión de tríceps sobre cabeza", beyGrupoMuscular.TRICEPS, 2, 10, 15, 1, 90));
        return rutina;
    }

    private static beyRutina beyPiernaB() {
        beyRutina rutina = new beyRutina("Pierna B", beyDiaSemana.VIERNES);
        rutina.setNotas("Énfasis en cadena posterior. RIR 2 en peso muerto rumano.");
        rutina.agregarEjercicio(beyEjercicio("Peso muerto rumano", beyGrupoMuscular.ISQUIOTIBIALES, 3, 6, 10, 2, 150));
        rutina.agregarEjercicio(beyEjercicio("Sentadilla búlgara", beyGrupoMuscular.CUADRICEPS, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Curl femoral", beyGrupoMuscular.ISQUIOTIBIALES, 3, 8, 12, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Hip thrust", beyGrupoMuscular.GLUTEOS, 2, 8, 12, 1, 120));
        rutina.agregarEjercicio(beyEjercicio("Extensión de cuádriceps", beyGrupoMuscular.CUADRICEPS, 2, 10, 15, 1, 90));
        rutina.agregarEjercicio(beyEjercicio("Elevación de pantorrillas", beyGrupoMuscular.PANTORRILLAS, 3, 10, 15, 1, 60));

        beyEjercicio plancha = beyEjercicio("Plancha abdominal", beyGrupoMuscular.ABDOMEN, 2, 30, 60, 1, 60);
        plancha.setPorTiempo(true);
        plancha.setNotas("Mantener línea neutra de cadera y respiración controlada.");
        rutina.agregarEjercicio(plancha);
        return rutina;
    }

    private static beyEjercicio beyEjercicio(String nombre, beyGrupoMuscular grupo, int series, int repsMin,
                                       int repsMax, int rir, int descansoSegundos) {
        beyEjercicio ejercicio = new beyEjercicio(nombre, grupo, series, repsMin, repsMax, rir);
        ejercicio.setDescansoSegundos(descansoSegundos);
        return ejercicio;
    }
}
