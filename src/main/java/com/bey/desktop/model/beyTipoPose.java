package com.bey.desktop.model;

public enum beyTipoPose {
    FRENTE("Frente"),
    ESPALDA("Espalda"),
    PERFIL("Perfil");

    private final String etiqueta;

    beyTipoPose(String etiqueta) {
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
