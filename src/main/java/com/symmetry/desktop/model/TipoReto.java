package com.symmetry.desktop.model;

/**
 * Familia de un reto dinamico. Define como se mide el progreso del reto: hay familias
 * que se calculan solas con los datos registrados y familias de recuperacion activa
 * que el usuario confirma manualmente al realizarlas.
 */
public enum TipoReto {

    /** Movilidad articular y estiramientos de la zona entrenada (se confirma manualmente). */
    MOVILIDAD("Movilidad", true),
    /** Respiracion diafragmatica para bajar el tono y favorecer la recuperacion (manual). */
    RESPIRACION("Respiración", true),
    /** Caminata ligera post-comida como recuperacion activa de bajo impacto (manual). */
    CAMINATA("Caminata ligera", true),
    /** Pausa activa: levantarse y movilizar articulaciones tras periodos sentado (manual). */
    PAUSA_ACTIVA("Pausa activa", true),
    /** Micro-habito complementario al entrenamiento del dia (se confirma manualmente). */
    HABITO("Micro-hábito", true),

    /** Beber N mililitros dentro de la ventana del reto; se mide con los registros de agua. */
    HIDRATACION_VENTANA("Hidratación", false),

    /**
     * Legado: retos de fuerza de versiones anteriores (completar series de un grupo o en total).
     * Ya no se generan; se conservan solo para poder leer los datos guardados de versiones previas.
     */
    SERIES_GRUPO("Foco muscular", false),
    SERIES_TOTALES("Volumen", false);

    private final String etiqueta;
    private final boolean requiereConfirmacion;

    TipoReto(String etiqueta, boolean requiereConfirmacion) {
        this.etiqueta = etiqueta;
        this.requiereConfirmacion = requiereConfirmacion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** true si el reto no se puede medir solo y el usuario debe confirmarlo al realizarlo. */
    public boolean requiereConfirmacion() {
        return requiereConfirmacion;
    }

    /** true si el progreso se calcula automaticamente a partir de los datos registrados. */
    public boolean esAutomatico() {
        return !requiereConfirmacion;
    }
}
