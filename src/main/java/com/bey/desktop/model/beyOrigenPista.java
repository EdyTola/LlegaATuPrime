package com.bey.desktop.model;

public enum beyOrigenPista {
    LOCAL("Archivo local"),
    DESCARGADO("Descargado"),
    ENLACE("Enlace / YouTube");

    private final String etiqueta;

    beyOrigenPista(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
