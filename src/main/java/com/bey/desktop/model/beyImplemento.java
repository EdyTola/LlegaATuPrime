package com.bey.desktop.model;

public enum beyImplemento {
    NINGUNO("Peso corporal"),
    BARRA("Barra olímpica"),
    MANCUERNA("Mancuernas"),
    POLEA("Polea / cable"),
    MAQUINA("Máquina");

    private final String etiqueta;

    beyImplemento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
