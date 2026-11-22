package com.bey.desktop.service;

import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyNivelEjercicio;
import com.bey.desktop.model.beyRutina;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class beyAdaptadorEquipoService {
    private static final List<String> PALABRAS_EQUIPO = List.of(
            "maquina", "polea", "cable", "barra", "mancuerna", "prensa", "jalon", "pec deck",
            "hack", "smith", "lastre", "rueda", "press", "remo", "peso muerto", "hip thrust",
            "dominada", "curl de", "curl femoral sentado", "curl femoral tumbado",
            "extension de triceps", "elevacion lateral", "sentadilla con", "zancadas con",
            "pull", "banca", "extension de cuadriceps", "curl femoral");

    private static final Map<beyGrupoMuscular, List<beyVariante>> VARIANTES = new EnumMap<>(beyGrupoMuscular.class);

    static {
        VARIANTES.put(beyGrupoMuscular.PECHO, List.of(
                new beyVariante("Flexiones con rodillas apoyadas", 3, 10, 15, 2, 60, false),
                new beyVariante("Flexiones clásicas", 4, 8, 15, 1, 75, false),
                new beyVariante("Flexiones declinadas (pies elevados)", 4, 8, 12, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.ESPALDA, List.of(
                new beyVariante("Superman con pausa", 3, 12, 15, 2, 60, false),
                new beyVariante("Remo invertido bajo una mesa", 4, 8, 12, 1, 75, false),
                new beyVariante("Dominadas en barra o marco de puerta", 4, 5, 10, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.HOMBROS, List.of(
                new beyVariante("Plancha con toque de hombro", 3, 12, 16, 2, 60, false),
                new beyVariante("Flexiones pica (pike push-up)", 3, 8, 12, 1, 75, false),
                new beyVariante("Flexiones pica con pies elevados", 4, 6, 10, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.BICEPS, List.of(
                new beyVariante("Curl isométrico con toalla", 3, 20, 30, 2, 45, true),
                new beyVariante("Curl con mochila cargada", 3, 10, 15, 1, 60, false),
                new beyVariante("Dominadas supinas (chin-up)", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.TRICEPS, List.of(
                new beyVariante("Fondos en silla con rodillas flexionadas", 3, 10, 15, 2, 60, false),
                new beyVariante("Flexiones diamante", 3, 8, 12, 1, 75, false),
                new beyVariante("Fondos en silla con piernas estiradas", 4, 8, 15, 1, 75, false)));
        VARIANTES.put(beyGrupoMuscular.CUADRICEPS, List.of(
                new beyVariante("Sentadilla libre", 3, 15, 20, 2, 60, false),
                new beyVariante("Sentadilla búlgara", 3, 8, 12, 1, 75, false),
                new beyVariante("Sentadilla pistol asistida", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.ISQUIOTIBIALES, List.of(
                new beyVariante("Puente de glúteos a una pierna", 3, 10, 15, 2, 60, false),
                new beyVariante("Peso muerto a una pierna sin carga", 3, 8, 12, 1, 75, false),
                new beyVariante("Curl femoral nórdico asistido", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(beyGrupoMuscular.GLUTEOS, List.of(
                new beyVariante("Puente de glúteos", 3, 15, 20, 2, 45, false),
                new beyVariante("Hip thrust con la espalda en el sofá", 4, 12, 15, 1, 60, false),
                new beyVariante("Hip thrust a una pierna en el sofá", 4, 8, 12, 1, 75, false)));
        VARIANTES.put(beyGrupoMuscular.PANTORRILLAS, List.of(
                new beyVariante("Elevación de talones en el suelo", 3, 15, 25, 2, 45, false),
                new beyVariante("Elevación de talones en escalón", 4, 15, 20, 1, 45, false),
                new beyVariante("Elevación de talones a una pierna", 4, 10, 15, 1, 60, false)));
        VARIANTES.put(beyGrupoMuscular.ABDOMEN, List.of(
                new beyVariante("Dead bug", 3, 10, 14, 2, 45, false),
                new beyVariante("Plancha frontal", 3, 30, 60, 1, 45, true),
                new beyVariante("Elevación de piernas colgado o en el suelo", 4, 10, 15, 1, 60, false)));
    }

    private beyAdaptadorEquipoService() {
    }

    public static boolean beyRequiereEquipo(beyEjercicio ejercicio) {
        if (ejercicio == null || ejercicio.getNombre() == null) {
            return false;
        }
        String nombre = beyNormalizar(ejercicio.getNombre());
        if (nombre.contains("nordico")) {
            return false;
        }
        return PALABRAS_EQUIPO.stream().anyMatch(nombre::contains);
    }

    public static beyRutina beyAdaptarRutina(beyRutina original) {
        beyRutina copia = new beyRutina(original.getNombre(), original.getDiaSemana());
        copia.setId(original.getId());
        copia.setNotas(original.getNotas());
        Set<String> usados = new HashSet<>();
        for (beyEjercicio ejercicio : original.getEjercicios()) {
            beyEjercicio adaptado = beyAdaptar(ejercicio, usados);
            usados.add(beyNormalizar(adaptado.getNombre()));
            copia.agregarEjercicio(adaptado);
        }
        return copia;
    }

    public static beyEjercicio beyAdaptar(beyEjercicio original, Set<String> nombresUsados) {
        beyEjercicio copia = beyCopiar(original);
        if (!beyRequiereEquipo(original) || original.getGrupoMuscular() == null) {
            return copia;
        }
        List<beyVariante> opciones = VARIANTES.get(original.getGrupoMuscular());
        if (opciones == null) {
            return copia;
        }
        int preferido = beyNivelPrescrito(original).ordinal();

        for (int desvio = 0; desvio < opciones.size(); desvio++) {
            beyVariante candidata = opciones.get((preferido + desvio) % opciones.size());
            if (!nombresUsados.contains(beyNormalizar(candidata.nombre()))) {
                return beyConstruir(original, candidata);
            }
        }
        return beyConstruir(original, opciones.get(preferido));
    }

    public static List<beyEjercicio> beyCatalogoCasa(beyNivelEjercicio nivel) {
        List<beyEjercicio> lista = new ArrayList<>();
        for (beyGrupoMuscular grupo : beyGrupoMuscular.values()) {
            List<beyVariante> opciones = VARIANTES.get(grupo);
            if (opciones != null) {
                lista.add(opciones.get(nivel.ordinal()).crear(grupo));
            }
        }
        return lista;
    }

    public static List<String> beyNombresCatalogoCasa(beyNivelEjercicio nivel) {
        return beyCatalogoCasa(nivel).stream().map(beyEjercicio::getNombre).toList();
    }

    private static beyNivelEjercicio beyNivelPrescrito(beyEjercicio ejercicio) {
        if (ejercicio.getRepeticionesMax() <= 8 || ejercicio.getSeries() >= 5) {
            return beyNivelEjercicio.AVANZADO;
        }
        if (ejercicio.getRepeticionesMax() <= 12) {
            return beyNivelEjercicio.INTERMEDIO;
        }
        return beyNivelEjercicio.PRINCIPIANTE;
    }

    private static beyEjercicio beyConstruir(beyEjercicio original, beyVariante variante) {
        beyEjercicio adaptado = variante.crear(original.getGrupoMuscular());
        adaptado.setId(original.getId());
        adaptado.setAdaptadoDe(original.getNombre());
        adaptado.setRutaMultimedia(null);
        adaptado.setNotas("Variante sin equipo de \"" + original.getNombre() + "\". Usa solo tu peso corporal.");

        adaptado.setDescansoSegundos(Math.min(adaptado.getDescansoSegundos(), original.getDescansoSegundos()));
        return adaptado;
    }

    private static beyEjercicio beyCopiar(beyEjercicio origen) {
        beyEjercicio copia = new beyEjercicio(origen.getNombre(), origen.getGrupoMuscular(), origen.getSeries(),
                origen.getRepeticionesMin(), origen.getRepeticionesMax(), origen.getRirObjetivo());
        copia.setId(origen.getId());
        copia.setPorTiempo(origen.isPorTiempo());
        copia.setDescansoSegundos(origen.getDescansoSegundos());
        copia.setRutaMultimedia(origen.getRutaMultimedia());
        copia.setEnlaceMultimedia(origen.getEnlaceMultimedia());
        copia.setNotas(origen.getNotas());
        copia.setAlternativasPersonalizadas(new ArrayList<>(origen.getAlternativasPersonalizadas()));
        return copia;
    }

    private static String beyNormalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record beyVariante(String nombre, int series, int repsMin, int repsMax, int rir,
                            int descanso, boolean porTiempo) {
        beyEjercicio crear(beyGrupoMuscular grupo) {
            beyEjercicio ejercicio = new beyEjercicio(nombre, grupo, series, repsMin, repsMax, rir);
            ejercicio.setDescansoSegundos(descanso);
            ejercicio.setPorTiempo(porTiempo);
            return ejercicio;
        }
    }
}
