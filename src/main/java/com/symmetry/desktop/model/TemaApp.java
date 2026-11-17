package com.symmetry.desktop.model;

/**
 * Tema visual de la aplicacion. Se guarda como preferencia del usuario y permite
 * alternar dinamicamente entre modo claro y modo oscuro (Modulo de interfaz).
 */
public enum TemaApp {

    CLARO("Modo claro"),
    OSCURO("Modo oscuro");

    private final String etiqueta;

    TemaApp(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Nombre de la clase CSS que se aplica a la escena para este tema. */
    public String getClaseCss() {
        return this == OSCURO ? "tema-oscuro" : "tema-claro";
    }

    /** Devuelve el tema opuesto, usado por el interruptor de la interfaz. */
    public TemaApp alternado() {
        return this == OSCURO ? CLARO : OSCURO;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
