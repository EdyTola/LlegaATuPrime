package com.bey.desktop.service;

import com.bey.desktop.model.beyBaseDatos;
import com.bey.desktop.model.beyDiaSalud;
import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyRegistroEjercicio;
import com.bey.desktop.model.beyRegistroHidratacion;
import com.bey.desktop.model.beyRetoDiario;
import com.bey.desktop.model.beyRutina;
import com.bey.desktop.model.beySesionEntrenamiento;
import com.bey.desktop.model.beyTipoReto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class beyGeneradorRetosService {
    private beyGeneradorRetosService() {
    }

    public static String beyIdVentanaDiaria(LocalDateTime ahora) {
        return "D-" + ahora.toLocalDate();
    }

    public static String beyIdVentanaFlash(LocalDateTime ahora) {
        return "F-" + ahora.toLocalDate() + "-" + (ahora.getHour() < 12 ? 0 : 1);
    }

    public static List<beyRetoDiario> beyGenerarDiarios(beyBaseDatos datos, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime expira = inicio.plusHours(24);
        String ventana = beyIdVentanaDiaria(ahora);
        Optional<beyRutina> rutina = datos.buscarRutinaDe(hoy);

        List<beyRetoDiario> retos = new ArrayList<>();
        retos.add(beyRetoMovilidad(ventana, inicio, expira, rutina.orElse(null), hoy));
        retos.add(beyRetoHabito(ventana, inicio, expira, hoy));
        return retos;
    }

    public static beyRetoDiario beyGenerarFlash(beyBaseDatos datos, LocalDateTime ahora) {
        boolean manana = ahora.getHour() < 12;
        LocalDateTime inicio = ahora.toLocalDate().atTime(manana ? 0 : 12, 0);
        LocalDateTime expira = inicio.plusHours(12);
        String ventana = beyIdVentanaFlash(ahora);

        return manana
                ? beyFlashActivacion(datos, ventana, ahora.toLocalDate(), inicio, expira)
                : beyFlashHidratacion(ventana, inicio, expira);
    }

    private static beyRetoDiario beyRetoMovilidad(String ventana, LocalDateTime inicio, LocalDateTime expira,
                                            beyRutina rutina, LocalDate hoy) {
        beyRetoDiario reto = beyNuevo(ventana, false, beyTipoReto.MOVILIDAD, inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(45);
        if (rutina == null) {
            beyCatalogoRecuperacion.beySugerencia sugerencia = beyCatalogoRecuperacion.beyMovilidadGeneral(hoy);
            reto.setTitulo(sugerencia.titulo());
            reto.setDescripcion("Día sin entrenamiento. " + sugerencia.instruccion()
                    + " Movimiento sin carga para favorecer la recuperación.");
            return reto;
        }
        beyGrupoMuscular foco = beyGrupoPrincipal(rutina);
        beyCatalogoRecuperacion.beySugerencia sugerencia = beyCatalogoRecuperacion.beyMovilidadDe(foco, hoy);
        reto.setGrupoMuscular(foco);
        reto.setTitulo(sugerencia.titulo());
        reto.setDescripcion("Hoy entrenas " + rutina.getNombre() + ". " + sugerencia.instruccion()
                + " Recupera la zona trabajada sin añadir carga ni fatiga muscular.");
        return reto;
    }

    private static beyRetoDiario beyRetoHabito(String ventana, LocalDateTime inicio, LocalDateTime expira,
                                         LocalDate hoy) {
        beyCatalogoRecuperacion.beyHabito habito = beyCatalogoRecuperacion.beyHabitoDelDia(hoy);
        beyRetoDiario reto = beyNuevo(ventana, false, habito.tipo(), inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(30);
        reto.setTitulo(habito.titulo());
        reto.setDescripcion(habito.instruccion());
        return reto;
    }

    private static beyRetoDiario beyFlashActivacion(beyBaseDatos datos, String ventana, LocalDate hoy,
                                              LocalDateTime inicio, LocalDateTime expira) {
        beyRutina rutina = datos.buscarRutinaDe(hoy).orElse(null);
        beyGrupoMuscular grupo = rutina == null ? null : beyGrupoSecundario(rutina);
        beyRetoDiario reto = beyNuevo(ventana, true, beyTipoReto.MOVILIDAD, inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(25);
        reto.setGrupoMuscular(grupo);
        beyCatalogoRecuperacion.beySugerencia sugerencia = grupo == null
                ? beyCatalogoRecuperacion.beyMovilidadGeneral(hoy)
                : beyCatalogoRecuperacion.beyMovilidadDe(grupo, hoy);
        reto.setTitulo("Flash · " + sugerencia.titulo());
        reto.setDescripcion((rutina == null
                ? "Empieza el día con recuperación activa. "
                : "Antes de tu sesión de " + rutina.getNombre() + ". ")
                + sugerencia.instruccion());
        return reto;
    }

    private static beyRetoDiario beyFlashHidratacion(String ventana, LocalDateTime inicio, LocalDateTime expira) {
        beyCatalogoRecuperacion.beyHabito habito = beyCatalogoRecuperacion.beyHidratacionPostEntreno();
        beyRetoDiario reto = beyNuevo(ventana, true, beyTipoReto.HIDRATACION_VENTANA, inicio, expira);
        reto.setObjetivo(beyCatalogoRecuperacion.ML_HIDRATACION_POST_ENTRENO);
        reto.setXpRecompensa(25);
        reto.setTitulo("Flash · " + habito.titulo());
        reto.setDescripcion(habito.instruccion());
        return reto;
    }

    private static beyRetoDiario beyNuevo(String ventana, boolean flash, beyTipoReto tipo,
                                    LocalDateTime inicio, LocalDateTime expira) {
        beyRetoDiario reto = new beyRetoDiario();
        reto.setVentanaId(ventana);
        reto.setFlash(flash);
        reto.setTipo(tipo);
        reto.setInicio(inicio);
        reto.setExpira(expira);
        return reto;
    }

    private static beyGrupoMuscular beyGrupoPrincipal(beyRutina rutina) {
        for (beyEjercicio ejercicio : rutina.getEjercicios()) {
            if (ejercicio.getGrupoMuscular() != null) {
                return ejercicio.getGrupoMuscular();
            }
        }
        return null;
    }

    private static beyGrupoMuscular beyGrupoSecundario(beyRutina rutina) {
        LinkedHashSet<beyGrupoMuscular> grupos = new LinkedHashSet<>();
        for (beyEjercicio ejercicio : rutina.getEjercicios()) {
            if (ejercicio.getGrupoMuscular() != null) {
                grupos.add(ejercicio.getGrupoMuscular());
            }
        }
        if (grupos.size() < 2) {
            return null;
        }
        return new ArrayList<>(grupos).get(1);
    }

    public static int beyCalcularProgreso(beyRetoDiario reto, beyBaseDatos datos) {
        beyTipoReto tipo = reto.getTipo();
        LocalDateTime desde = reto.getInicio();
        LocalDateTime hasta = reto.getExpira();
        if (tipo == null || !tipo.esAutomatico() || desde == null || hasta == null) {
            return reto.getProgreso();
        }
        if (tipo == beyTipoReto.HIDRATACION_VENTANA) {
            int total = 0;
            for (beyDiaSalud dia : datos.getDias()) {
                for (beyRegistroHidratacion registro : dia.getHidratacion()) {
                    LocalDateTime momento = registro.getFechaHora();
                    if (momento != null && !momento.isBefore(desde) && momento.isBefore(hasta)) {
                        total += registro.getMililitros();
                    }
                }
            }
            return total;
        }

        long series = 0;
        for (beySesionEntrenamiento sesion : datos.getSesiones()) {
            LocalDateTime momento = sesion.getFechaFin() != null ? sesion.getFechaFin() : sesion.getFechaInicio();
            if (momento == null || momento.isBefore(desde) || !momento.isBefore(hasta)) {
                continue;
            }
            for (beyRegistroEjercicio registro : sesion.getRegistros()) {
                boolean cuenta = tipo == beyTipoReto.SERIES_TOTALES
                        || registro.getGrupoMuscular() == reto.getGrupoMuscular();
                if (cuenta) {
                    series += registro.getSeriesCompletadas();
                }
            }
        }
        return (int) series;
    }
}
