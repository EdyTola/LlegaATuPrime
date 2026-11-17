package com.symmetry.desktop.service;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.NivelEjercicio;
import com.symmetry.desktop.model.Rutina;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Modo "Sin equipo / Home": convierte ejercicios que requieren maquinas, poleas, barras o
 * mancuernas en variantes de calistenia con peso corporal del mismo grupo muscular, y ofrece
 * un catalogo propio de ejercicios sin equipo por nivel.
 *
 * <p>Las variantes son copias en memoria (no se persisten): la rutina original del usuario
 * nunca se modifica, de modo que al volver al modo gimnasio todo queda intacto.</p>
 */
public final class AdaptadorEquipoService {

    /** Palabras (sin tildes, en minusculas) que delatan que un ejercicio necesita equipo. */
    private static final List<String> PALABRAS_EQUIPO = List.of(
            "maquina", "polea", "cable", "barra", "mancuerna", "prensa", "jalon", "pec deck",
            "hack", "smith", "lastre", "rueda", "press", "remo", "peso muerto", "hip thrust",
            "dominada", "curl de", "curl femoral sentado", "curl femoral tumbado",
            "extension de triceps", "elevacion lateral", "sentadilla con", "zancadas con",
            "pull", "banca", "extension de cuadriceps", "curl femoral");

    /** Variantes sin equipo por grupo muscular, ordenadas de PRINCIPIANTE a AVANZADO. */
    private static final Map<GrupoMuscular, List<Variante>> VARIANTES = new EnumMap<>(GrupoMuscular.class);

    static {
        VARIANTES.put(GrupoMuscular.PECHO, List.of(
                new Variante("Flexiones con rodillas apoyadas", 3, 10, 15, 2, 60, false),
                new Variante("Flexiones clásicas", 4, 8, 15, 1, 75, false),
                new Variante("Flexiones declinadas (pies elevados)", 4, 8, 12, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.ESPALDA, List.of(
                new Variante("Superman con pausa", 3, 12, 15, 2, 60, false),
                new Variante("Remo invertido bajo una mesa", 4, 8, 12, 1, 75, false),
                new Variante("Dominadas en barra o marco de puerta", 4, 5, 10, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.HOMBROS, List.of(
                new Variante("Plancha con toque de hombro", 3, 12, 16, 2, 60, false),
                new Variante("Flexiones pica (pike push-up)", 3, 8, 12, 1, 75, false),
                new Variante("Flexiones pica con pies elevados", 4, 6, 10, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.BICEPS, List.of(
                new Variante("Curl isométrico con toalla", 3, 20, 30, 2, 45, true),
                new Variante("Curl con mochila cargada", 3, 10, 15, 1, 60, false),
                new Variante("Dominadas supinas (chin-up)", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.TRICEPS, List.of(
                new Variante("Fondos en silla con rodillas flexionadas", 3, 10, 15, 2, 60, false),
                new Variante("Flexiones diamante", 3, 8, 12, 1, 75, false),
                new Variante("Fondos en silla con piernas estiradas", 4, 8, 15, 1, 75, false)));
        VARIANTES.put(GrupoMuscular.CUADRICEPS, List.of(
                new Variante("Sentadilla libre", 3, 15, 20, 2, 60, false),
                new Variante("Sentadilla búlgara", 3, 8, 12, 1, 75, false),
                new Variante("Sentadilla pistol asistida", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.ISQUIOTIBIALES, List.of(
                new Variante("Puente de glúteos a una pierna", 3, 10, 15, 2, 60, false),
                new Variante("Peso muerto a una pierna sin carga", 3, 8, 12, 1, 75, false),
                new Variante("Curl femoral nórdico asistido", 4, 4, 8, 1, 90, false)));
        VARIANTES.put(GrupoMuscular.GLUTEOS, List.of(
                new Variante("Puente de glúteos", 3, 15, 20, 2, 45, false),
                new Variante("Hip thrust con la espalda en el sofá", 4, 12, 15, 1, 60, false),
                new Variante("Hip thrust a una pierna en el sofá", 4, 8, 12, 1, 75, false)));
        VARIANTES.put(GrupoMuscular.PANTORRILLAS, List.of(
                new Variante("Elevación de talones en el suelo", 3, 15, 25, 2, 45, false),
                new Variante("Elevación de talones en escalón", 4, 15, 20, 1, 45, false),
                new Variante("Elevación de talones a una pierna", 4, 10, 15, 1, 60, false)));
        VARIANTES.put(GrupoMuscular.ABDOMEN, List.of(
                new Variante("Dead bug", 3, 10, 14, 2, 45, false),
                new Variante("Plancha frontal", 3, 30, 60, 1, 45, true),
                new Variante("Elevación de piernas colgado o en el suelo", 4, 10, 15, 1, 60, false)));
    }

    private AdaptadorEquipoService() {
    }

    /** true si el ejercicio parece necesitar maquinas, poleas, barras o mancuernas. */
    public static boolean requiereEquipo(Ejercicio ejercicio) {
        if (ejercicio == null || ejercicio.getNombre() == null) {
            return false;
        }
        String nombre = normalizar(ejercicio.getNombre());
        if (nombre.contains("nordico")) {
            return false; // El curl femoral nordico ya es de peso corporal.
        }
        return PALABRAS_EQUIPO.stream().anyMatch(nombre::contains);
    }

    /**
     * Copia de la rutina con todos los ejercicios que requieren equipo sustituidos por su
     * variante de calistenia. Conserva el identificador de cada ejercicio para poder
     * relacionar la variante con el original.
     */
    public static Rutina adaptarRutina(Rutina original) {
        Rutina copia = new Rutina(original.getNombre(), original.getDiaSemana());
        copia.setId(original.getId());
        copia.setNotas(original.getNotas());
        Set<String> usados = new HashSet<>();
        for (Ejercicio ejercicio : original.getEjercicios()) {
            Ejercicio adaptado = adaptar(ejercicio, usados);
            usados.add(normalizar(adaptado.getNombre()));
            copia.agregarEjercicio(adaptado);
        }
        return copia;
    }

    /** Variante sin equipo de un ejercicio, o una copia identica si ya no necesita equipo. */
    public static Ejercicio adaptar(Ejercicio original, Set<String> nombresUsados) {
        Ejercicio copia = copiar(original);
        if (!requiereEquipo(original) || original.getGrupoMuscular() == null) {
            return copia;
        }
        List<Variante> opciones = VARIANTES.get(original.getGrupoMuscular());
        if (opciones == null) {
            return copia;
        }
        int preferido = nivelPrescrito(original).ordinal();
        // Se busca primero el nivel equivalente y despues los demas, evitando repetir ejercicios.
        for (int desvio = 0; desvio < opciones.size(); desvio++) {
            Variante candidata = opciones.get((preferido + desvio) % opciones.size());
            if (!nombresUsados.contains(normalizar(candidata.nombre()))) {
                return construir(original, candidata);
            }
        }
        return construir(original, opciones.get(preferido));
    }

    /** Catalogo de ejercicios sin equipo para un nivel: una variante por grupo muscular. */
    public static List<Ejercicio> catalogoCasa(NivelEjercicio nivel) {
        List<Ejercicio> lista = new ArrayList<>();
        for (GrupoMuscular grupo : GrupoMuscular.values()) {
            List<Variante> opciones = VARIANTES.get(grupo);
            if (opciones != null) {
                lista.add(opciones.get(nivel.ordinal()).crear(grupo));
            }
        }
        return lista;
    }

    /** Todos los nombres de ejercicios del catalogo sin equipo (para clasificar el historial). */
    public static List<String> nombresCatalogoCasa(NivelEjercicio nivel) {
        return catalogoCasa(nivel).stream().map(Ejercicio::getNombre).toList();
    }

    // ------------------------------------------------------------------

    /** Nivel estimado de un ejercicio segun su prescripcion (menos reps y mas series = mas duro). */
    private static NivelEjercicio nivelPrescrito(Ejercicio ejercicio) {
        if (ejercicio.getRepeticionesMax() <= 8 || ejercicio.getSeries() >= 5) {
            return NivelEjercicio.AVANZADO;
        }
        if (ejercicio.getRepeticionesMax() <= 12) {
            return NivelEjercicio.INTERMEDIO;
        }
        return NivelEjercicio.PRINCIPIANTE;
    }

    private static Ejercicio construir(Ejercicio original, Variante variante) {
        Ejercicio adaptado = variante.crear(original.getGrupoMuscular());
        adaptado.setId(original.getId());
        adaptado.setAdaptadoDe(original.getNombre());
        adaptado.setRutaMultimedia(null);
        adaptado.setNotas("Variante sin equipo de \"" + original.getNombre() + "\". Usa solo tu peso corporal.");
        // Se respeta el descanso configurado por el usuario si es menor al de la variante.
        adaptado.setDescansoSegundos(Math.min(adaptado.getDescansoSegundos(), original.getDescansoSegundos()));
        return adaptado;
    }

    private static Ejercicio copiar(Ejercicio origen) {
        Ejercicio copia = new Ejercicio(origen.getNombre(), origen.getGrupoMuscular(), origen.getSeries(),
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

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Plantilla inmutable de una variante de calistenia. */
    private record Variante(String nombre, int series, int repsMin, int repsMax, int rir,
                            int descanso, boolean porTiempo) {

        Ejercicio crear(GrupoMuscular grupo) {
            Ejercicio ejercicio = new Ejercicio(nombre, grupo, series, repsMin, repsMax, rir);
            ejercicio.setDescansoSegundos(descanso);
            ejercicio.setPorTiempo(porTiempo);
            return ejercicio;
        }
    }
}
