package com.bey.desktop.service;

import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyTipoReto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class beyCatalogoRecuperacion {
    public record beySugerencia(String titulo, String instruccion) {
    }

    public record beyHabito(beyTipoReto tipo, String titulo, String instruccion) {
    }

    private static final List<beySugerencia> TORSO_SUPERIOR = List.of(
            new beySugerencia("Movilidad torácica",
                    "Realiza 3 minutos de apertura de pecho y estiramiento en marco de puerta."),
            new beySugerencia("Postura liberada",
                    "2 minutos de retracción escapular y respiración profunda para relajar pectorales."));

    private static final List<beySugerencia> TREN_INFERIOR = List.of(
            new beySugerencia("Estiramiento de cadenas",
                    "3 minutos de estiramiento suave de isquiotibiales y cuádriceps en casa."),
            new beySugerencia("Activación de circulación",
                    "Da una caminata ligera de 15 minutos al aire libre para relajar las piernas."));

    private static final List<beySugerencia> CUERPO_COMPLETO = List.of(
            new beySugerencia("Movilidad general",
                    "3 minutos de movilidad suave de cadera, tobillo y columna, sin carga."),
            new beySugerencia("Descompresión lumbar",
                    "2 minutos de postura del niño o estiramiento de columna en el suelo."));

    private static final Map<beyGrupoMuscular, List<beySugerencia>> ZONA = Map.ofEntries(
            Map.entry(beyGrupoMuscular.PECHO, TORSO_SUPERIOR),
            Map.entry(beyGrupoMuscular.ESPALDA, TORSO_SUPERIOR),
            Map.entry(beyGrupoMuscular.HOMBROS, TORSO_SUPERIOR),
            Map.entry(beyGrupoMuscular.BICEPS, TORSO_SUPERIOR),
            Map.entry(beyGrupoMuscular.TRICEPS, TORSO_SUPERIOR),
            Map.entry(beyGrupoMuscular.CUADRICEPS, TREN_INFERIOR),
            Map.entry(beyGrupoMuscular.ISQUIOTIBIALES, TREN_INFERIOR),
            Map.entry(beyGrupoMuscular.GLUTEOS, TREN_INFERIOR),
            Map.entry(beyGrupoMuscular.PANTORRILLAS, TREN_INFERIOR),
            Map.entry(beyGrupoMuscular.ABDOMEN, CUERPO_COMPLETO));

    private static final List<beyHabito> HABITOS = List.of(
            new beyHabito(beyTipoReto.RESPIRACION, "Respiración diafragmática",
                    "3 minutos de respiración profunda para bajar el cortisol y acelerar el descanso."),
            new beyHabito(beyTipoReto.MOVILIDAD, "Descompresión lumbar",
                    "2 minutos de postura del niño o estiramiento de columna en el suelo."),
            new beyHabito(beyTipoReto.PAUSA_ACTIVA, "Pausa activa",
                    "Cada 2 horas sentado, levántate 5 minutos: camina y abre el pecho sin cargar peso."));

    private static final beyHabito HIDRATACION_POST_ENTRENO = new beyHabito(
            beyTipoReto.HIDRATACION_VENTANA, "Hidratación post-entreno",
            "Bebe 500 ml de agua con electrolitos o agua pura en la siguiente hora.");

    public static final int ML_HIDRATACION_POST_ENTRENO = 500;

    private beyCatalogoRecuperacion() {
    }

    public static beySugerencia beyMovilidadDe(beyGrupoMuscular grupo, LocalDate fecha) {
        List<beySugerencia> zona = grupo == null ? CUERPO_COMPLETO : ZONA.getOrDefault(grupo, CUERPO_COMPLETO);
        return zona.get(Math.floorMod(fecha.getDayOfYear(), zona.size()));
    }

    public static beySugerencia beyMovilidadGeneral(LocalDate fecha) {
        return CUERPO_COMPLETO.get(Math.floorMod(fecha.getDayOfYear(), CUERPO_COMPLETO.size()));
    }

    public static beyHabito beyHabitoDelDia(LocalDate fecha) {
        return HABITOS.get(Math.floorMod(fecha.getDayOfYear(), HABITOS.size()));
    }

    public static beyHabito beyHidratacionPostEntreno() {
        return HIDRATACION_POST_ENTRENO;
    }
}
