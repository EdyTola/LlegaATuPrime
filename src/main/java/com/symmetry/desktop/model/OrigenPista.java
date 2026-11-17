package com.symmetry.desktop.model;

/**
 * Origen de una pista de la biblioteca musical (Modulo 4): puede ser un archivo
 * de audio local (.mp3 / .wav) o un enlace externo (por ejemplo, YouTube).
 */
public enum OrigenPista {

    LOCAL("Archivo local"),
    ENLACE("Enlace / YouTube");

    private final String etiqueta;

    OrigenPista(String etiqueta) {
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
