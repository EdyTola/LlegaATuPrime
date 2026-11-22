package com.bey.desktop.service;

import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyMedicionCorporal;
import com.bey.desktop.model.beyPuntoSerie;
import com.bey.desktop.model.beyRegistroEjercicio;
import com.bey.desktop.model.beySesionEntrenamiento;
import com.bey.desktop.util.beyFormato;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class beyAnaliticaService {
    private static final List<String> GRUPOS_VOLUMEN = List.of(
            "Pecho", "Espalda", "Cuádriceps", "Isquiotibiales", "Hombros", "Brazos", "Otros");

    private final beyGestorDatos beyGestor;

    public beyAnaliticaService(beyGestorDatos gestor) {
        this.beyGestor = gestor;
    }

    public static LocalDate beyDesdeRango(String rango) {
        if (rango == null) {
            return null;
        }
        return switch (rango) {
            case "Semanal" -> LocalDate.now().minusDays(7);
            case "Mensual" -> LocalDate.now().minusDays(30);
            default -> null;
        };
    }

    public List<beyPuntoSerie> beyPesoCorporal(LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        for (beyMedicionCorporal medicion : beyGestor.beyGetDatos().medicionesOrdenadas()) {
            if (medicion.getPesoKg() != null && beyDentro(medicion.getFecha(), desde)) {
                puntos.add(new beyPuntoSerie(medicion.getFecha(),
                        beyFormato.beyEtiquetaEje(medicion.getFecha()), medicion.getPesoKg()));
            }
        }
        return puntos;
    }

    public List<beyPuntoSerie> beyCintura(LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        for (beyMedicionCorporal medicion : beyGestor.beyGetDatos().medicionesOrdenadas()) {
            if (medicion.getCinturaCm() != null && beyDentro(medicion.getFecha(), desde)) {
                puntos.add(new beyPuntoSerie(medicion.getFecha(),
                        beyFormato.beyEtiquetaEje(medicion.getFecha()), medicion.getCinturaCm()));
            }
        }
        return puntos;
    }

    public List<beyPuntoSerie> beyGrasaCorporal(LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        for (beyMedicionCorporal medicion : beyGestor.beyGetDatos().medicionesOrdenadas()) {
            if (medicion.getGrasaPorcentaje() != null && beyDentro(medicion.getFecha(), desde)) {
                puntos.add(new beyPuntoSerie(medicion.getFecha(),
                        beyFormato.beyEtiquetaEje(medicion.getFecha()), medicion.getGrasaPorcentaje()));
            }
        }
        return puntos;
    }

    public List<beyPuntoSerie> beyMasaMuscular(LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        for (beyMedicionCorporal medicion : beyGestor.beyGetDatos().medicionesOrdenadas()) {
            if (medicion.getMasaMuscularKg() != null && beyDentro(medicion.getFecha(), desde)) {
                puntos.add(new beyPuntoSerie(medicion.getFecha(),
                        beyFormato.beyEtiquetaEje(medicion.getFecha()), medicion.getMasaMuscularKg()));
            }
        }
        return puntos;
    }

    public List<beyPuntoSerie> beyProgresionEjercicio(String nombreEjercicio, LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        if (nombreEjercicio == null || nombreEjercicio.isBlank()) {
            return puntos;
        }
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().sesionesOrdenadas()) {
            if (sesion.getFechaInicio() == null || !beyDentro(sesion.getFecha(), desde)) {
                continue;
            }
            for (beyRegistroEjercicio registro : sesion.getRegistros()) {
                if (registro.getNombreEjercicio() != null
                        && registro.getNombreEjercicio().equalsIgnoreCase(nombreEjercicio)) {
                    double peso = registro.getPesoMaximo();
                    if (peso > 0) {
                        puntos.add(new beyPuntoSerie(sesion.getFecha(),
                                beyFormato.beyEtiquetaEje(sesion.getFecha()), peso));
                    }
                }
            }
        }
        return puntos;
    }

    public List<beyPuntoSerie> beyDuracionSesiones(LocalDate desde) {
        List<beyPuntoSerie> puntos = new ArrayList<>();
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().sesionesOrdenadas()) {
            if (sesion.getFechaInicio() != null && beyDentro(sesion.getFecha(), desde)) {
                puntos.add(new beyPuntoSerie(sesion.getFecha(),
                        beyFormato.beyEtiquetaEje(sesion.getFecha()), sesion.getDuracionMinutos()));
            }
        }
        return puntos;
    }

    public double beyDuracionMedia(LocalDate desde) {
        List<beyPuntoSerie> puntos = beyDuracionSesiones(desde);
        return puntos.stream().mapToDouble(beyPuntoSerie::getValor).average().orElse(0);
    }

    public double beyVolumenTotal(LocalDate desde) {
        double total = 0;
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().getSesiones()) {
            if (sesion.getFechaInicio() != null && beyDentro(sesion.getFecha(), desde)) {
                total += sesion.getVolumenTotal();
            }
        }
        return total;
    }

    public long beySesionesRegistradas(LocalDate desde) {
        return beyGestor.beyGetDatos().getSesiones().stream()
                .filter(s -> s.getFechaInicio() != null && beyDentro(s.getFecha(), desde))
                .count();
    }

    public int beyRachaEntrenamiento() {
        Set<LocalDate> dias = new HashSet<>();
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().getSesiones()) {
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

    public Map<String, Double> beyVolumenPorGrupo(LocalDate desde) {
        Map<String, Double> acumulado = new LinkedHashMap<>();
        for (String grupo : GRUPOS_VOLUMEN) {
            acumulado.put(grupo, 0.0);
        }
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().getSesiones()) {
            for (beyRegistroEjercicio registro : sesion.getRegistros()) {
                LocalDate fecha = registro.getFecha() != null ? registro.getFecha() : sesion.getFecha();
                if (!beyDentro(fecha, desde)) {
                    continue;
                }
                String grupo = beyGrupoVisible(registro.getGrupoMuscular());
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

    private static String beyGrupoVisible(beyGrupoMuscular grupo) {
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

    public List<String> beyEjerciciosConHistorial() {
        Set<String> nombres = new LinkedHashSet<>();
        for (beySesionEntrenamiento sesion : beyGestor.beyGetDatos().sesionesOrdenadas()) {
            for (beyRegistroEjercicio registro : sesion.getRegistros()) {
                if (registro.getNombreEjercicio() != null && registro.getPesoMaximo() > 0) {
                    nombres.add(registro.getNombreEjercicio());
                }
            }
        }
        if (nombres.isEmpty()) {
            beyGestor.beyGetDatos().catalogoEjercicios().forEach(e -> nombres.add(e.getNombre()));
        }
        return new ArrayList<>(nombres);
    }

    private boolean beyDentro(LocalDate fecha, LocalDate desde) {
        return fecha != null && (desde == null || !fecha.isBefore(desde));
    }
}
