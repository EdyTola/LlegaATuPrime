package com.symmetry.desktop.service;

import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.model.PatronMovimiento;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Decide que patron biomecanico animar para un ejercicio a partir de su nombre (las palabras
 * clave se evaluan en orden de especificidad) y, si no hay coincidencia, de su grupo muscular.
 * Funciona 100% offline: no depende de archivos ni enlaces del usuario.
 */
public final class GuiaTecnicaService {

    private GuiaTecnicaService() {
    }

    public static PatronMovimiento detectar(String nombre, GrupoMuscular grupo) {
        String n = nombre == null ? "" : normalizar(nombre);

        if (n.contains("hip thrust")) {
            return PatronMovimiento.HIP_THRUST;
        }
        if (n.contains("puente de gluteos")) {
            return PatronMovimiento.PUENTE_GLUTEOS;
        }
        if (n.contains("peso muerto")) {
            return PatronMovimiento.PESO_MUERTO;
        }
        if (n.contains("zancada") || n.contains("bulgara")) {
            return PatronMovimiento.ZANCADA;
        }
        if (n.contains("sentadilla") || n.contains("hack") || n.contains("prensa") || n.contains("pistol")) {
            return PatronMovimiento.SENTADILLA;
        }
        if (n.contains("pica") || n.contains("pike")) {
            return PatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("flexion")) {
            return PatronMovimiento.FLEXION;
        }
        if (n.contains("plancha")) {
            return PatronMovimiento.PLANCHA;
        }
        if (n.contains("fondos")) {
            return PatronMovimiento.FONDOS;
        }
        if (n.contains("dominada") || n.contains("chin-up") || n.contains("chin up")) {
            return PatronMovimiento.DOMINADA;
        }
        if (n.contains("jalon")) {
            return PatronMovimiento.JALON;
        }
        if (n.contains("superman")) {
            return PatronMovimiento.SUPERMAN;
        }
        if (n.contains("remo")) {
            return PatronMovimiento.REMO;
        }
        if (n.contains("elevacion lateral")) {
            return PatronMovimiento.ELEVACION_LATERAL;
        }
        if (n.contains("press inclinado")) {
            return PatronMovimiento.PRESS_INCLINADO;
        }
        if (n.contains("press militar") || n.contains("press de hombro") || n.contains("hombros")) {
            return PatronMovimiento.PRESS_HOMBRO;
        }
        if (n.contains("press") || n.contains("pec deck")) {
            return PatronMovimiento.PRESS_PLANO;
        }
        if (n.contains("curl femoral")) {
            return PatronMovimiento.CURL_FEMORAL;
        }
        if (n.contains("curl")) {
            return PatronMovimiento.CURL_BICEPS;
        }
        if (n.contains("triceps")) {
            return PatronMovimiento.EXTENSION_TRICEPS;
        }
        if (n.contains("pantorrilla") || n.contains("talones")) {
            return PatronMovimiento.ELEVACION_TALONES;
        }
        if (n.contains("elevacion de piernas") || n.contains("dead bug")) {
            return PatronMovimiento.ELEVACION_PIERNAS;
        }
        if (n.contains("crunch") || n.contains("abdominal") || n.contains("rueda")) {
            return PatronMovimiento.CRUNCH;
        }
        return porGrupo(grupo);
    }

    /** Patron representativo de un grupo muscular, usado como respaldo. */
    public static PatronMovimiento porGrupo(GrupoMuscular grupo) {
        if (grupo == null) {
            return PatronMovimiento.SENTADILLA;
        }
        return switch (grupo) {
            case PECHO -> PatronMovimiento.PRESS_PLANO;
            case ESPALDA -> PatronMovimiento.REMO;
            case HOMBROS -> PatronMovimiento.PRESS_HOMBRO;
            case BICEPS -> PatronMovimiento.CURL_BICEPS;
            case TRICEPS -> PatronMovimiento.EXTENSION_TRICEPS;
            case CUADRICEPS -> PatronMovimiento.SENTADILLA;
            case ISQUIOTIBIALES -> PatronMovimiento.PESO_MUERTO;
            case GLUTEOS -> PatronMovimiento.HIP_THRUST;
            case PANTORRILLAS -> PatronMovimiento.ELEVACION_TALONES;
            case ABDOMEN -> PatronMovimiento.CRUNCH;
        };
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
