package com.symmetry.desktop.model;

/**
 * Tipo de pose de una foto de progreso, para comparar la evolucion fisica
 * desde el mismo angulo a lo largo del tiempo.
 */
public enum TipoPose {

    FRENTE("Frente"),
    ESPALDA("Espalda"),
    PERFIL("Perfil");

    private final String etiqueta;

    TipoPose(String etiqueta) {
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
