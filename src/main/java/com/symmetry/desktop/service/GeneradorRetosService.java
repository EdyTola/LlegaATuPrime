package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.RegistroEjercicio;
import com.symmetry.desktop.model.RegistroHidratacion;
import com.symmetry.desktop.model.RetoDiario;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.model.SesionEntrenamiento;
import com.symmetry.desktop.model.TipoReto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Genera los retos dinamicos flash y diarios a partir de la rutina programada para el dia
 * y mide su progreso dentro de la ventana de tiempo del reto.
 *
 * <p>Ventanas: los retos diarios duran 24 h (de 00:00 a 00:00) y los retos flash 12 h
 * (00:00-12:00 y 12:00-24:00). Cada ventana se genera una sola vez.</p>
 *
 * <p>Criterio de contextualizacion: el sistema lee que parte del cuerpo se entrena hoy
 * segun la rutina activa y propone acciones de <b>recuperacion activa</b> para esa zona
 * (movilidad, estiramientos, respiracion, caminatas y micro-habitos) en lugar de mas
 * series de fuerza. Asi se evita repetir los ejercicios de la rutina y el sobreentrenamiento.
 * En los dias sin rutina se proponen retos de recuperacion de cuerpo completo.</p>
 */
public final class GeneradorRetosService {

    private GeneradorRetosService() {
    }

    // ------------------------------------------------------------------
    // Ventanas
    // ------------------------------------------------------------------

    public static String idVentanaDiaria(LocalDateTime ahora) {
        return "D-" + ahora.toLocalDate();
    }

    public static String idVentanaFlash(LocalDateTime ahora) {
        return "F-" + ahora.toLocalDate() + "-" + (ahora.getHour() < 12 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // Generacion
    // ------------------------------------------------------------------

    /** Retos de la ventana diaria (24 h): movilidad de la zona entrenada y un micro-habito. */
    public static List<RetoDiario> generarDiarios(BaseDatos datos, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime expira = inicio.plusHours(24);
        String ventana = idVentanaDiaria(ahora);
        Optional<Rutina> rutina = datos.buscarRutinaDe(hoy);

        List<RetoDiario> retos = new ArrayList<>();
        retos.add(retoMovilidad(ventana, inicio, expira, rutina.orElse(null), hoy));
        retos.add(retoHabito(ventana, inicio, expira, hoy));
        return retos;
    }

    /** Reto flash de 12 h: movilidad matinal centrada en el dia; hidratacion post-entreno por la tarde. */
    public static RetoDiario generarFlash(BaseDatos datos, LocalDateTime ahora) {
        boolean manana = ahora.getHour() < 12;
        LocalDateTime inicio = ahora.toLocalDate().atTime(manana ? 0 : 12, 0);
        LocalDateTime expira = inicio.plusHours(12);
        String ventana = idVentanaFlash(ahora);

        return manana
                ? flashActivacion(datos, ventana, ahora.toLocalDate(), inicio, expira)
                : flashHidratacion(ventana, inicio, expira);
    }

    /** Movilidad y estiramiento de recuperacion de la zona que se entrena hoy (o de cuerpo completo). */
    private static RetoDiario retoMovilidad(String ventana, LocalDateTime inicio, LocalDateTime expira,
                                            Rutina rutina, LocalDate hoy) {
        RetoDiario reto = nuevo(ventana, false, TipoReto.MOVILIDAD, inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(45);
        if (rutina == null) {
            CatalogoRecuperacion.Sugerencia sugerencia = CatalogoRecuperacion.movilidadGeneral(hoy);
            reto.setTitulo(sugerencia.titulo());
            reto.setDescripcion("Día sin entrenamiento. " + sugerencia.instruccion()
                    + " Movimiento sin carga para favorecer la recuperación.");
            return reto;
        }
        GrupoMuscular foco = grupoPrincipal(rutina);
        CatalogoRecuperacion.Sugerencia sugerencia = CatalogoRecuperacion.movilidadDe(foco, hoy);
        reto.setGrupoMuscular(foco);
        reto.setTitulo(sugerencia.titulo());
        reto.setDescripcion("Hoy entrenas " + rutina.getNombre() + ". " + sugerencia.instruccion()
                + " Recupera la zona trabajada sin añadir carga ni fatiga muscular.");
        return reto;
    }

    /** Micro-habito de recuperacion del dia (rota entre respiracion, descompresion y pausa activa). */
    private static RetoDiario retoHabito(String ventana, LocalDateTime inicio, LocalDateTime expira,
                                         LocalDate hoy) {
        CatalogoRecuperacion.Habito habito = CatalogoRecuperacion.habitoDelDia(hoy);
        RetoDiario reto = nuevo(ventana, false, habito.tipo(), inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(30);
        reto.setTitulo(habito.titulo());
        reto.setDescripcion(habito.instruccion());
        return reto;
    }

    /** Movilidad matinal de la zona del dia para preparar la sesion sin generar fatiga. */
    private static RetoDiario flashActivacion(BaseDatos datos, String ventana, LocalDate hoy,
                                              LocalDateTime inicio, LocalDateTime expira) {
        Rutina rutina = datos.buscarRutinaDe(hoy).orElse(null);
        GrupoMuscular grupo = rutina == null ? null : grupoSecundario(rutina);
        RetoDiario reto = nuevo(ventana, true, TipoReto.MOVILIDAD, inicio, expira);
        reto.setObjetivo(1);
        reto.setXpRecompensa(25);
        reto.setGrupoMuscular(grupo);
        CatalogoRecuperacion.Sugerencia sugerencia = grupo == null
                ? CatalogoRecuperacion.movilidadGeneral(hoy)
                : CatalogoRecuperacion.movilidadDe(grupo, hoy);
        reto.setTitulo("Flash · " + sugerencia.titulo());
        reto.setDescripcion((rutina == null
                ? "Empieza el día con recuperación activa. "
                : "Antes de tu sesión de " + rutina.getNombre() + ". ")
                + sugerencia.instruccion());
        return reto;
    }

    /** Hidratacion post-entreno que cierra la recuperacion del dia. */
    private static RetoDiario flashHidratacion(String ventana, LocalDateTime inicio, LocalDateTime expira) {
        CatalogoRecuperacion.Habito habito = CatalogoRecuperacion.hidratacionPostEntreno();
        RetoDiario reto = nuevo(ventana, true, TipoReto.HIDRATACION_VENTANA, inicio, expira);
        reto.setObjetivo(CatalogoRecuperacion.ML_HIDRATACION_POST_ENTRENO);
        reto.setXpRecompensa(25);
        reto.setTitulo("Flash · " + habito.titulo());
        reto.setDescripcion(habito.instruccion());
        return reto;
    }

    private static RetoDiario nuevo(String ventana, boolean flash, TipoReto tipo,
                                    LocalDateTime inicio, LocalDateTime expira) {
        RetoDiario reto = new RetoDiario();
        reto.setVentanaId(ventana);
        reto.setFlash(flash);
        reto.setTipo(tipo);
        reto.setInicio(inicio);
        reto.setExpira(expira);
        return reto;
    }

    // ------------------------------------------------------------------
    // Lectura de la rutina del dia
    // ------------------------------------------------------------------

    /** Primer grupo muscular principal de la rutina (el de mayor presencia en la sesion). */
    private static GrupoMuscular grupoPrincipal(Rutina rutina) {
        for (Ejercicio ejercicio : rutina.getEjercicios()) {
            if (ejercicio.getGrupoMuscular() != null) {
                return ejercicio.getGrupoMuscular();
            }
        }
        return null;
    }

    /** Segundo grupo muscular distinto de la rutina, o null si la sesion trabaja uno solo. */
    private static GrupoMuscular grupoSecundario(Rutina rutina) {
        LinkedHashSet<GrupoMuscular> grupos = new LinkedHashSet<>();
        for (Ejercicio ejercicio : rutina.getEjercicios()) {
            if (ejercicio.getGrupoMuscular() != null) {
                grupos.add(ejercicio.getGrupoMuscular());
            }
        }
        if (grupos.size() < 2) {
            return null;
        }
        return new ArrayList<>(grupos).get(1);
    }

    // ------------------------------------------------------------------
    // Progreso
    // ------------------------------------------------------------------

    /**
     * Progreso actual del reto, medido solo con los registros dentro de su ventana.
     * Los retos de recuperacion se confirman manualmente y no se recalculan aqui.
     */
    public static int calcularProgreso(RetoDiario reto, BaseDatos datos) {
        TipoReto tipo = reto.getTipo();
        LocalDateTime desde = reto.getInicio();
        LocalDateTime hasta = reto.getExpira();
        if (tipo == null || !tipo.esAutomatico() || desde == null || hasta == null) {
            return reto.getProgreso();
        }
        if (tipo == TipoReto.HIDRATACION_VENTANA) {
            int total = 0;
            for (DiaSalud dia : datos.getDias()) {
                for (RegistroHidratacion registro : dia.getHidratacion()) {
                    LocalDateTime momento = registro.getFechaHora();
                    if (momento != null && !momento.isBefore(desde) && momento.isBefore(hasta)) {
                        total += registro.getMililitros();
                    }
                }
            }
            return total;
        }
        // Legado: retos de fuerza de versiones anteriores (series de un grupo o totales).
        long series = 0;
        for (SesionEntrenamiento sesion : datos.getSesiones()) {
            LocalDateTime momento = sesion.getFechaFin() != null ? sesion.getFechaFin() : sesion.getFechaInicio();
            if (momento == null || momento.isBefore(desde) || !momento.isBefore(hasta)) {
                continue;
            }
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                boolean cuenta = tipo == TipoReto.SERIES_TOTALES
                        || registro.getGrupoMuscular() == reto.getGrupoMuscular();
                if (cuenta) {
                    series += registro.getSeriesCompletadas();
                }
            }
        }
        return (int) series;
    }
}
