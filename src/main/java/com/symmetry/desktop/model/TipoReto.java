package com.symmetry.desktop.model;

/**
 * Familia de un reto dinamico. Define como se mide el progreso del reto a partir de los
 * registros del usuario dentro de la ventana de tiempo del reto.
 */
public enum TipoReto {

    /** Completar N series de un grupo muscular concreto (el mas rezagado del usuario). */
    SERIES_GRUPO("Foco muscular"),
    /** Completar N series en total, sin importar el grupo. */
    SERIES_TOTALES("Volumen"),
    /** Beber N mililitros dentro de la ventana del reto. */
    HIDRATACION_VENTANA("Hidratación");

    private final String etiqueta;

    TipoReto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
