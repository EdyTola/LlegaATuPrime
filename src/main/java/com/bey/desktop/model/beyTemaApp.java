package com.bey.desktop.model;

public enum beyTemaApp {
    CLARO("Modo claro"),
    OSCURO("Modo oscuro");

    private final String etiqueta;

    beyTemaApp(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return this == OSCURO ? "tema-oscuro" : "tema-claro";
    }

    public beyTemaApp alternado() {
        return this == OSCURO ? CLARO : OSCURO;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
