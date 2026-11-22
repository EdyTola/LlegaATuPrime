package com.bey.desktop.model;

public enum beyTipoReto {
    MOVILIDAD("Movilidad", true),

    RESPIRACION("Respiración", true),

    CAMINATA("Caminata ligera", true),

    PAUSA_ACTIVA("Pausa activa", true),

    HABITO("Micro-hábito", true),

    HIDRATACION_VENTANA("Hidratación", false),

    SERIES_GRUPO("Foco muscular", false),
    SERIES_TOTALES("Volumen", false);

    private final String etiqueta;
    private final boolean requiereConfirmacion;

    beyTipoReto(String etiqueta, boolean requiereConfirmacion) {
        this.etiqueta = etiqueta;
        this.requiereConfirmacion = requiereConfirmacion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean requiereConfirmacion() {
        return requiereConfirmacion;
    }

    public boolean esAutomatico() {
        return !requiereConfirmacion;
    }
}
