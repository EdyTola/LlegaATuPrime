package com.symmetry.desktop.model;

/**
 * Modalidad de entrenamiento activa: define si los ejercicios usan el equipamiento
 * del gimnasio o unicamente el peso corporal (calistenia / entrenamiento en casa).
 */
public enum ModalidadEntrenamiento {

    GIMNASIO("Con equipo", "Gym Mode · máquinas, poleas, barras y mancuernas"),
    CASA("Sin equipo", "Home Mode · calistenia con peso corporal");

    private final String etiqueta;
    private final String descripcion;

    ModalidadEntrenamiento(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
