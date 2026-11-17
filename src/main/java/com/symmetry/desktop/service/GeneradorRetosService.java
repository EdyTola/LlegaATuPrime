package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.ModalidadEntrenamiento;
import com.symmetry.desktop.model.NivelEjercicio;
import com.symmetry.desktop.model.RegistroEjercicio;
import com.symmetry.desktop.model.RegistroHidratacion;
import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.RetoDiario;
import com.symmetry.desktop.model.SesionEntrenamiento;
import com.symmetry.desktop.model.TipoReto;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genera los retos dinamicos a partir del historial real del usuario y mide su progreso.
 *
 * <p>Ventanas: los retos diarios duran 24 h (de 00:00 a 00:00) y los retos flash 12 h
 * (00:00-12:00 y 12:00-24:00). Cada ventana se genera una sola vez.</p>
 *
 * <p>Criterios de personalizacion:</p>
 * <ul>
 *   <li><b>Grupo muscular</b>: se prioriza el que lleva mas dias sin entrenarse (o nunca).</li>
 *   <li><b>Nivel</b>: se infiere por la frecuencia de las ultimas 4 semanas y, entre los
 *       niveles ya alcanzados, se elige aquel cuyo catalogo se practico hace mas tiempo.</li>
 * </ul>
 */
public final class GeneradorRetosService {

    private static final int SIN_HISTORIAL = 999;

    private GeneradorRetosService() {
    }

    /** Resultado del analisis del historial. */
    public record Analisis(Map<GrupoMuscular, Integer> diasSinEntrenar, List<GrupoMuscular> prioridad,
                           NivelEjercicio nivelInferido, NivelEjercicio nivelObjetivo) {
    }

    // ------------------------------------------------------------------
    // Generacion
    // ------------------------------------------------------------------

    public static String idVentanaDiaria(LocalDateTime ahora) {
        return "D-" + ahora.toLocalDate();
    }

    public static String idVentanaFlash(LocalDateTime ahora) {
        return "F-" + ahora.toLocalDate() + "-" + (ahora.getHour() < 12 ? 0 : 1);
    }

    /** Retos de la ventana diaria (24 h): foco en el musculo rezagado y volumen total. */
    public static List<RetoDiario> generarDiarios(BaseDatos datos, ModalidadEntrenamiento modalidad,
                                                  LocalDateTime ahora) {
        Analisis analisis = analizar(datos, ahora.toLocalDate());
        LocalDateTime inicio = ahora.toLocalDate().atStartOfDay();
        LocalDateTime expira = inicio.plusHours(24);
        String ventana = idVentanaDiaria(ahora);
        List<RetoDiario> retos = new ArrayList<>();

        GrupoMuscular foco = analisis.prioridad().get(0);
        int dias = analisis.diasSinEntrenar().get(foco);
        RetoDiario reto = nuevo(ventana, false, TipoReto.SERIES_GRUPO, inicio, expira);
        reto.setGrupoMuscular(foco);
        reto.setNivel(analisis.nivelObjetivo());
        reto.setObjetivo(porNivel(analisis.nivelObjetivo(), 6, 8, 10));
        reto.setXpRecompensa(dias >= SIN_HISTORIAL ? 60 : 45);
        reto.setTitulo("Rescata tu " + foco.getEtiqueta().toLowerCase(Locale.ROOT));
        reto.setDescripcion(descripcionFoco(foco, dias, analisis.nivelObjetivo(), modalidad, reto.getObjetivo()));
        retos.add(reto);

        RetoDiario volumen = nuevo(ventana, false, TipoReto.SERIES_TOTALES, inicio, expira);
        volumen.setNivel(analisis.nivelInferido());
        volumen.setObjetivo(porNivel(analisis.nivelInferido(), 14, 18, 22));
        volumen.setXpRecompensa(50);
        volumen.setTitulo("Volumen del día");
        volumen.setDescripcion("Completa " + volumen.getObjetivo()
                + " series efectivas hoy. Nivel detectado: " + analisis.nivelInferido().getEtiqueta() + ".");
        retos.add(volumen);
        return retos;
    }

    /** Reto flash de 12 h: el segundo musculo mas rezagado por la manana, agua por la tarde. */
    public static RetoDiario generarFlash(BaseDatos datos, ModalidadEntrenamiento modalidad,
                                          LocalDateTime ahora) {
        boolean manana = ahora.getHour() < 12;
        LocalDateTime inicio = ahora.toLocalDate().atTime(manana ? 0 : 12, 0);
        LocalDateTime expira = inicio.plusHours(12);
        String ventana = idVentanaFlash(ahora);

        if (manana) {
            Analisis analisis = analizar(datos, ahora.toLocalDate());
            GrupoMuscular grupo = analisis.prioridad().get(1);
            RetoDiario reto = nuevo(ventana, true, TipoReto.SERIES_GRUPO, inicio, expira);
            reto.setGrupoMuscular(grupo);
            reto.setNivel(analisis.nivelObjetivo());
            reto.setObjetivo(porNivel(analisis.nivelObjetivo(), 4, 5, 6));
            reto.setXpRecompensa(30);
            reto.setTitulo("Flash · " + grupo.getEtiqueta());
            reto.setDescripcion(descripcionFoco(grupo, analisis.diasSinEntrenar().get(grupo),
                    analisis.nivelObjetivo(), modalidad, reto.getObjetivo()));
            return reto;
        }

        int meta = datos.getPerfil().getObjetivos().getAguaMililitros();
        int objetivo = Math.max(500, (int) Math.round(meta * 0.55 / 100.0) * 100);
        RetoDiario reto = nuevo(ventana, true, TipoReto.HIDRATACION_VENTANA, inicio, expira);
        reto.setObjetivo(objetivo);
        reto.setXpRecompensa(25);
        reto.setTitulo("Flash · Hidratación");
        reto.setDescripcion("Bebe " + objetivo + " ml entre las 12:00 y las 24:00. Tu rendimiento y tu recuperación lo notarán.");
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

    private static String descripcionFoco(GrupoMuscular grupo, int dias, NivelEjercicio nivel,
                                          ModalidadEntrenamiento modalidad, int objetivo) {
        String antiguedad = dias >= SIN_HISTORIAL
                ? "Aún no has registrado series de " + grupo.getEtiqueta().toLowerCase(Locale.ROOT) + "."
                : "Llevas " + dias + (dias == 1 ? " día" : " días") + " sin trabajar "
                + grupo.getEtiqueta().toLowerCase(Locale.ROOT) + ".";
        String ejemplo = ejemploPara(grupo, nivel, modalidad);
        return antiguedad + " Completa " + objetivo + " series"
                + (ejemplo == null ? "." : ", por ejemplo con " + ejemplo + ".");
    }

    private static String ejemploPara(GrupoMuscular grupo, NivelEjercicio nivel, ModalidadEntrenamiento modalidad) {
        return SugerenciasEjerciciosService.paraNivel(nivel, modalidad).stream()
                .filter(e -> e.getGrupoMuscular() == grupo)
                .map(Ejercicio::getNombre)
                .findFirst()
                .orElse(null);
    }

    private static int porNivel(NivelEjercicio nivel, int principiante, int intermedio, int avanzado) {
        return switch (nivel) {
            case PRINCIPIANTE -> principiante;
            case INTERMEDIO -> intermedio;
            case AVANZADO -> avanzado;
        };
    }

    // ------------------------------------------------------------------
    // Analisis del historial
    // ------------------------------------------------------------------

    /** Examina las sesiones guardadas para decidir que musculo y que nivel necesitan atencion. */
    public static Analisis analizar(BaseDatos datos, LocalDate hoy) {
        Map<GrupoMuscular, LocalDate> ultimo = new EnumMap<>(GrupoMuscular.class);
        Map<NivelEjercicio, LocalDate> ultimoNivel = new EnumMap<>(NivelEjercicio.class);
        int sesionesRecientes = 0;

        for (SesionEntrenamiento sesion : datos.sesionesOrdenadas()) {
            LocalDate fecha = sesion.getFecha();
            if (fecha == null) {
                continue;
            }
            if (!fecha.isBefore(hoy.minusDays(28))) {
                sesionesRecientes++;
            }
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                if (registro.getSeriesCompletadas() <= 0) {
                    continue;
                }
                if (registro.getGrupoMuscular() != null) {
                    ultimo.merge(registro.getGrupoMuscular(), fecha, GeneradorRetosService::masReciente);
                }
                NivelEjercicio nivel = nivelDe(registro.getNombreEjercicio());
                if (nivel != null) {
                    ultimoNivel.merge(nivel, fecha, GeneradorRetosService::masReciente);
                }
            }
        }

        Map<GrupoMuscular, Integer> dias = new EnumMap<>(GrupoMuscular.class);
        for (GrupoMuscular grupo : GrupoMuscular.values()) {
            LocalDate fecha = ultimo.get(grupo);
            dias.put(grupo, fecha == null ? SIN_HISTORIAL : (int) java.time.temporal.ChronoUnit.DAYS.between(fecha, hoy));
        }
        List<GrupoMuscular> prioridad = new ArrayList<>(List.of(GrupoMuscular.values()));
        prioridad.sort(Comparator.comparing((GrupoMuscular g) -> dias.get(g)).reversed()
                .thenComparing(Enum::ordinal));

        NivelEjercicio inferido = sesionesRecientes < 6 ? NivelEjercicio.PRINCIPIANTE
                : sesionesRecientes < 12 ? NivelEjercicio.INTERMEDIO : NivelEjercicio.AVANZADO;

        // Entre los niveles ya alcanzados, el que lleva mas tiempo sin practicarse necesita atencion.
        NivelEjercicio objetivo = inferido;
        int mayorAntiguedad = -1;
        for (NivelEjercicio nivel : NivelEjercicio.values()) {
            if (nivel.ordinal() > inferido.ordinal()) {
                break;
            }
            LocalDate fecha = ultimoNivel.get(nivel);
            int antiguedad = fecha == null ? SIN_HISTORIAL
                    : (int) java.time.temporal.ChronoUnit.DAYS.between(fecha, hoy);
            if (antiguedad >= mayorAntiguedad) {
                mayorAntiguedad = antiguedad;
                objetivo = nivel;
            }
        }
        return new Analisis(dias, prioridad, inferido, objetivo);
    }

    private static LocalDate masReciente(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    /** Nivel al que pertenece un ejercicio segun los catalogos de sugerencias (gimnasio y casa). */
    private static NivelEjercicio nivelDe(String nombre) {
        if (nombre == null) {
            return null;
        }
        String buscado = normalizar(nombre);
        for (NivelEjercicio nivel : NivelEjercicio.values()) {
            boolean coincide = SugerenciasEjerciciosService.paraNivel(nivel).stream()
                    .anyMatch(e -> normalizar(e.getNombre()).equals(buscado))
                    || AdaptadorEquipoService.nombresCatalogoCasa(nivel).stream()
                    .anyMatch(n -> normalizar(n).equals(buscado));
            if (coincide) {
                return nivel;
            }
        }
        return null;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    // ------------------------------------------------------------------
    // Progreso
    // ------------------------------------------------------------------

    /** Progreso actual del reto, medido solo con los registros dentro de su ventana. */
    public static int calcularProgreso(RetoDiario reto, BaseDatos datos) {
        LocalDateTime desde = reto.getInicio();
        LocalDateTime hasta = reto.getExpira();
        if (desde == null || hasta == null || reto.getTipo() == null) {
            return reto.getProgreso();
        }
        if (reto.getTipo() == TipoReto.HIDRATACION_VENTANA) {
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
        long series = 0;
        for (SesionEntrenamiento sesion : datos.getSesiones()) {
            LocalDateTime momento = sesion.getFechaFin() != null ? sesion.getFechaFin() : sesion.getFechaInicio();
            if (momento == null || momento.isBefore(desde) || !momento.isBefore(hasta)) {
                continue;
            }
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                boolean cuenta = reto.getTipo() == TipoReto.SERIES_TOTALES
                        || registro.getGrupoMuscular() == reto.getGrupoMuscular();
                if (cuenta) {
                    series += registro.getSeriesCompletadas();
                }
            }
        }
        return (int) series;
    }
}
