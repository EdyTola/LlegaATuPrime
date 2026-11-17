package com.symmetry.desktop.model;

import java.util.List;

/**
 * Grupos musculares soportados por el catalogo de ejercicios.
 * Se usan tambien como criterio para sugerir sustituciones equivalentes y para
 * alimentar el diagrama anatomico (grupo principal y secundarios).
 */
public enum GrupoMuscular {

    PECHO("Pecho"),
    ESPALDA("Espalda"),
    HOMBROS("Hombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    CUADRICEPS("Cuádriceps"),
    ISQUIOTIBIALES("Isquiotibiales"),
    GLUTEOS("Glúteos"),
    PANTORRILLAS("Pantorrillas"),
    ABDOMEN("Abdomen");

    private final String etiqueta;

    GrupoMuscular(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Grupos musculares que tambien intervienen (de forma secundaria) en los ejercicios
     * cuyo grupo principal es este. Se usa para resaltarlos en el diagrama anatomico.
     */
    public List<GrupoMuscular> getSecundarios() {
        return switch (this) {
            case PECHO -> List.of(HOMBROS, TRICEPS);
            case ESPALDA -> List.of(BICEPS, ISQUIOTIBIALES);
            case HOMBROS -> List.of(TRICEPS, PECHO);
            case BICEPS -> List.of(ESPALDA);
            case TRICEPS -> List.of(HOMBROS, PECHO);
            case CUADRICEPS -> List.of(GLUTEOS, PANTORRILLAS);
            case ISQUIOTIBIALES -> List.of(GLUTEOS, ESPALDA);
            case GLUTEOS -> List.of(ISQUIOTIBIALES, CUADRICEPS);
            case PANTORRILLAS -> List.of(ISQUIOTIBIALES);
            case ABDOMEN -> List.of();
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
