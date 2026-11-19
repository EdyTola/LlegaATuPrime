package com.symmetry.desktop.model;

/** Equipamiento con el que se ejecuta un ejercicio; define que se dibuja en la animacion 2D. */
public enum Implemento {

    NINGUNO("Peso corporal"),
    BARRA("Barra olímpica"),
    MANCUERNA("Mancuernas"),
    POLEA("Polea / cable"),
    MAQUINA("Máquina");

    private final String etiqueta;

    Implemento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
