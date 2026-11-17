package com.symmetry.desktop.model;

/**
 * Envases comerciales usados para el registro rapido de hidratacion (Modulo 3).
 */
public enum Recipiente {

    VASO("Vaso estándar", 250),
    BOTELLA_MEDIANA("Botella mediana Cielo / San Luis", 500),
    BOTELLA_PERSONAL("Botella personal Cielo / San Luis", 625),
    TOMATODO("Tomatodo / Botella grande", 1000),
    PERSONALIZADO("Cantidad personalizada", 0);

    private final String etiqueta;
    private final int mililitros;

    Recipiente(String etiqueta, int mililitros) {
        this.etiqueta = etiqueta;
        this.mililitros = mililitros;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getMililitros() {
        return mililitros;
    }

    @Override
    public String toString() {
        return etiqueta + (mililitros > 0 ? " (" + mililitros + " ml)" : "");
    }
}
