package com.bey.desktop.model;

import java.util.List;

public enum beyGrupoMuscular {
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

    beyGrupoMuscular(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public List<beyGrupoMuscular> getSecundarios() {
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
