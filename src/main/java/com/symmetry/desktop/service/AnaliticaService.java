package com.symmetry.desktop.service;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.MedicionCorporal;
import com.symmetry.desktop.model.PuntoSerie;
import com.symmetry.desktop.model.RegistroEjercicio;
import com.symmetry.desktop.model.SesionEntrenamiento;
import com.symmetry.desktop.util.Formato;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculos del modulo de progreso y analiticas: series temporales de peso corporal,
 * cintura, progresion de carga por ejercicio y duracion de las sesiones.
 */
public class AnaliticaService {

    /** Grupos musculares mostrados en el grafico de distribucion de volumen, en orden. */
    private static final List<String> GRUPOS_VOLUMEN = List.of(
            "Pecho", "Espalda", "Cuádriceps", "Isquiotibiales", "Hombros", "Brazos", "Otros");

    private final GestorDatos gestor;

    public AnaliticaService(GestorDatos gestor) {
        this.gestor = gestor;
    }

    /** Fecha de inicio para cada rango de filtro; null significa historico completo. */
    public static LocalDate desdeRango(String rango) {
        if (rango == null) {
            return null;
        }
        return switch (rango) {
            case "Semanal" -> LocalDate.now().minusDays(7);
            case "Mensual" -> LocalDate.now().minusDays(30);
            default -> null;
        };
    }

    public List<PuntoSerie> pesoCorporal(LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        for (MedicionCorporal medicion : gestor.getDatos().medicionesOrdenadas()) {
            if (medicion.getPesoKg() != null && dentro(medicion.getFecha(), desde)) {
                puntos.add(new PuntoSerie(medicion.getFecha(),
                        Formato.etiquetaEje(medicion.getFecha()), medicion.getPesoKg()));
            }
        }
        return puntos;
    }

    public List<PuntoSerie> cintura(LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        for (MedicionCorporal medicion : gestor.getDatos().medicionesOrdenadas()) {
            if (medicion.getCinturaCm() != null && dentro(medicion.getFecha(), desde)) {
                puntos.add(new PuntoSerie(medicion.getFecha(),
                        Formato.etiquetaEje(medicion.getFecha()), medicion.getCinturaCm()));
            }
        }
        return puntos;
    }

    /** Evolucion del porcentaje de grasa corporal. */
    public List<PuntoSerie> grasaCorporal(LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        for (MedicionCorporal medicion : gestor.getDatos().medicionesOrdenadas()) {
            if (medicion.getGrasaPorcentaje() != null && dentro(medicion.getFecha(), desde)) {
                puntos.add(new PuntoSerie(medicion.getFecha(),
                        Formato.etiquetaEje(medicion.getFecha()), medicion.getGrasaPorcentaje()));
            }
        }
        return puntos;
    }

    /** Evolucion de la masa muscular estimada. */
    public List<PuntoSerie> masaMuscular(LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        for (MedicionCorporal medicion : gestor.getDatos().medicionesOrdenadas()) {
            if (medicion.getMasaMuscularKg() != null && dentro(medicion.getFecha(), desde)) {
                puntos.add(new PuntoSerie(medicion.getFecha(),
                        Formato.etiquetaEje(medicion.getFecha()), medicion.getMasaMuscularKg()));
            }
        }
        return puntos;
    }

    /** Peso maximo alcanzado en el ejercicio indicado en cada sesion (sobrecarga progresiva). */
    public List<PuntoSerie> progresionEjercicio(String nombreEjercicio, LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        if (nombreEjercicio == null || nombreEjercicio.isBlank()) {
            return puntos;
        }
        for (SesionEntrenamiento sesion : gestor.getDatos().sesionesOrdenadas()) {
            if (sesion.getFechaInicio() == null || !dentro(sesion.getFecha(), desde)) {
                continue;
            }
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                if (registro.getNombreEjercicio() != null
                        && registro.getNombreEjercicio().equalsIgnoreCase(nombreEjercicio)) {
                    double peso = registro.getPesoMaximo();
                    if (peso > 0) {
                        puntos.add(new PuntoSerie(sesion.getFecha(),
                                Formato.etiquetaEje(sesion.getFecha()), peso));
                    }
                }
            }
        }
        return puntos;
    }

    /** Duracion en minutos de cada sesion registrada. */
    public List<PuntoSerie> duracionSesiones(LocalDate desde) {
        List<PuntoSerie> puntos = new ArrayList<>();
        for (SesionEntrenamiento sesion : gestor.getDatos().sesionesOrdenadas()) {
            if (sesion.getFechaInicio() != null && dentro(sesion.getFecha(), desde)) {
                puntos.add(new PuntoSerie(sesion.getFecha(),
                        Formato.etiquetaEje(sesion.getFecha()), sesion.getDuracionMinutos()));
            }
        }
        return puntos;
    }

    /** Duracion media de las sesiones de un periodo, en minutos. */
    public double duracionMedia(LocalDate desde) {
        List<PuntoSerie> puntos = duracionSesiones(desde);
        return puntos.stream().mapToDouble(PuntoSerie::getValor).average().orElse(0);
    }

    /** Volumen total levantado en un periodo. */
    public double volumenTotal(LocalDate desde) {
        double total = 0;
        for (SesionEntrenamiento sesion : gestor.getDatos().getSesiones()) {
            if (sesion.getFechaInicio() != null && dentro(sesion.getFecha(), desde)) {
                total += sesion.getVolumenTotal();
            }
        }
        return total;
    }

    public long sesionesRegistradas(LocalDate desde) {
        return gestor.getDatos().getSesiones().stream()
                .filter(s -> s.getFechaInicio() != null && dentro(s.getFecha(), desde))
                .count();
    }

    /**
     * Racha de entrenamiento: numero de dias consecutivos con una sesion registrada,
     * contando desde hoy (o desde ayer si hoy todavia no se ha entrenado).
     */
    public int rachaEntrenamiento() {
        Set<LocalDate> dias = new HashSet<>();
        for (SesionEntrenamiento sesion : gestor.getDatos().getSesiones()) {
            if (sesion.getFecha() != null) {
                dias.add(sesion.getFecha());
            }
        }
        LocalDate cursor = LocalDate.now();
        if (!dias.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int racha = 0;
        while (dias.contains(cursor)) {
            racha++;
            cursor = cursor.minusDays(1);
        }
        return racha;
    }

    /**
     * Volumen de carga levantado por grupo muscular en un periodo. Agrupa los grupos
     * solicitados por el usuario (Pecho, Espalda, Cuadriceps, Isquiotibiales, Hombros y
     * Brazos) y acumula el resto en "Otros". Solo se devuelven los grupos con volumen.
     */
    public Map<String, Double> volumenPorGrupo(LocalDate desde) {
        Map<String, Double> acumulado = new LinkedHashMap<>();
        for (String grupo : GRUPOS_VOLUMEN) {
            acumulado.put(grupo, 0.0);
        }
        for (SesionEntrenamiento sesion : gestor.getDatos().getSesiones()) {
            if (sesion.getFechaInicio() == null || !dentro(sesion.getFecha(), desde)) {
                continue;
            }
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                String grupo = grupoVisible(registro.getGrupoMuscular());
                if (grupo != null) {
                    acumulado.merge(grupo, registro.getVolumenTotal(), Double::sum);
                }
            }
        }
        Map<String, Double> volumen = new LinkedHashMap<>();
        acumulado.forEach((grupo, valor) -> {
            if (valor > 0) {
                volumen.put(grupo, valor);
            }
        });
        return volumen;
    }

    /** Nombre visible del grupo muscular para el grafico de volumen. */
    private static String grupoVisible(GrupoMuscular grupo) {
        if (grupo == null) {
            return null;
        }
        return switch (grupo) {
            case PECHO -> "Pecho";
            case ESPALDA -> "Espalda";
            case CUADRICEPS -> "Cuádriceps";
            case ISQUIOTIBIALES -> "Isquiotibiales";
            case HOMBROS -> "Hombros";
            case BICEPS, TRICEPS -> "Brazos";
            default -> "Otros";
        };
    }

    /** Ejercicios con historial de series registradas, para el selector de progresion. */
    public List<String> ejerciciosConHistorial() {
        Set<String> nombres = new LinkedHashSet<>();
        for (SesionEntrenamiento sesion : gestor.getDatos().sesionesOrdenadas()) {
            for (RegistroEjercicio registro : sesion.getRegistros()) {
                if (registro.getNombreEjercicio() != null && registro.getPesoMaximo() > 0) {
                    nombres.add(registro.getNombreEjercicio());
                }
            }
        }
        if (nombres.isEmpty()) {
            gestor.getDatos().catalogoEjercicios().forEach(e -> nombres.add(e.getNombre()));
        }
        return new ArrayList<>(nombres);
    }

    private boolean dentro(LocalDate fecha, LocalDate desde) {
        return fecha != null && (desde == null || !fecha.isBefore(desde));
    }
}
