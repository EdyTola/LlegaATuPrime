package com.symmetry.desktop.service;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.TipoReto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Catalogo exclusivo de recuperacion activa para los retos flash y diarios.
 *
 * <p>Todo el contenido es recuperacion, bienestar y movilidad: estiramientos, pausas
 * activas, respiracion y micro-habitos. Nunca se piden series de gimnasio, repeticiones
 * de fuerza ni volumen extra, sino acciones que complementan (y no repiten) el grupo
 * muscular entrenado ese dia.</p>
 */
public final class CatalogoRecuperacion {

    /** Sugerencia de recuperacion: titulo corto y accion practica para la tarjeta. */
    public record Sugerencia(String titulo, String instruccion) {
    }

    /** Micro-habito complementario: familia de reto, titulo corto e instruccion practica. */
    public record Habito(TipoReto tipo, String titulo, String instruccion) {
    }

    // ------------------------------------------------------------------
    // Movilidad y recuperacion por zona entrenada
    // ------------------------------------------------------------------

    /** Torso superior: pecho, espalda, hombros y brazos. */
    private static final List<Sugerencia> TORSO_SUPERIOR = List.of(
            new Sugerencia("Movilidad torácica",
                    "Realiza 3 minutos de apertura de pecho y estiramiento en marco de puerta."),
            new Sugerencia("Postura liberada",
                    "2 minutos de retracción escapular y respiración profunda para relajar pectorales."));

    /** Tren inferior: piernas y glúteos. */
    private static final List<Sugerencia> TREN_INFERIOR = List.of(
            new Sugerencia("Estiramiento de cadenas",
                    "3 minutos de estiramiento suave de isquiotibiales y cuádriceps en casa."),
            new Sugerencia("Activación de circulación",
                    "Da una caminata ligera de 15 minutos al aire libre para relajar las piernas."));

    /** Cuerpo completo: dias sin entrenamiento o zonas sin recuperacion especifica. */
    private static final List<Sugerencia> CUERPO_COMPLETO = List.of(
            new Sugerencia("Movilidad general",
                    "3 minutos de movilidad suave de cadera, tobillo y columna, sin carga."),
            new Sugerencia("Descompresión lumbar",
                    "2 minutos de postura del niño o estiramiento de columna en el suelo."));

    /** Zona de recuperacion que corresponde a cada grupo muscular entrenado. */
    private static final Map<GrupoMuscular, List<Sugerencia>> ZONA = Map.ofEntries(
            Map.entry(GrupoMuscular.PECHO, TORSO_SUPERIOR),
            Map.entry(GrupoMuscular.ESPALDA, TORSO_SUPERIOR),
            Map.entry(GrupoMuscular.HOMBROS, TORSO_SUPERIOR),
            Map.entry(GrupoMuscular.BICEPS, TORSO_SUPERIOR),
            Map.entry(GrupoMuscular.TRICEPS, TORSO_SUPERIOR),
            Map.entry(GrupoMuscular.CUADRICEPS, TREN_INFERIOR),
            Map.entry(GrupoMuscular.ISQUIOTIBIALES, TREN_INFERIOR),
            Map.entry(GrupoMuscular.GLUTEOS, TREN_INFERIOR),
            Map.entry(GrupoMuscular.PANTORRILLAS, TREN_INFERIOR),
            Map.entry(GrupoMuscular.ABDOMEN, CUERPO_COMPLETO));

    // ------------------------------------------------------------------
    // Micro-habitos generales de recuperacion
    // ------------------------------------------------------------------

    /** Micro-habitos que se rotan dia a dia como reto diario complementario. */
    private static final List<Habito> HABITOS = List.of(
            new Habito(TipoReto.RESPIRACION, "Respiración diafragmática",
                    "3 minutos de respiración profunda para bajar el cortisol y acelerar el descanso."),
            new Habito(TipoReto.MOVILIDAD, "Descompresión lumbar",
                    "2 minutos de postura del niño o estiramiento de columna en el suelo."),
            new Habito(TipoReto.PAUSA_ACTIVA, "Pausa activa",
                    "Cada 2 horas sentado, levántate 5 minutos: camina y abre el pecho sin cargar peso."));

    /** Hidratacion post-entreno que cierra la ventana flash de la tarde. */
    private static final Habito HIDRATACION_POST_ENTRENO = new Habito(
            TipoReto.HIDRATACION_VENTANA, "Hidratación post-entreno",
            "Bebe 500 ml de agua con electrolitos o agua pura en la siguiente hora.");

    /** Cantidad de agua del micro-habito de hidratacion post-entreno (ml). */
    public static final int ML_HIDRATACION_POST_ENTRENO = 500;

    private CatalogoRecuperacion() {
    }

    /** Recuperacion de la zona entrenada, rotando dentro de la zona segun la fecha. */
    public static Sugerencia movilidadDe(GrupoMuscular grupo, LocalDate fecha) {
        List<Sugerencia> zona = grupo == null ? CUERPO_COMPLETO : ZONA.getOrDefault(grupo, CUERPO_COMPLETO);
        return zona.get(Math.floorMod(fecha.getDayOfYear(), zona.size()));
    }

    /** Recuperacion de cuerpo completo (dias sin entrenamiento programado). */
    public static Sugerencia movilidadGeneral(LocalDate fecha) {
        return CUERPO_COMPLETO.get(Math.floorMod(fecha.getDayOfYear(), CUERPO_COMPLETO.size()));
    }

    /** Micro-habito complementario del dia, rotando por la fecha para variar el contenido. */
    public static Habito habitoDelDia(LocalDate fecha) {
        return HABITOS.get(Math.floorMod(fecha.getDayOfYear(), HABITOS.size()));
    }

    /** Micro-habito de hidratacion post-entreno (cierra la ventana flash de la tarde). */
    public static Habito hidratacionPostEntreno() {
        return HIDRATACION_POST_ENTRENO;
    }
}
