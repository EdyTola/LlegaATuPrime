package com.symmetry.desktop.model;

/**
 * Nivel de experiencia del usuario. Se usa para filtrar el panel de sugerencias
 * de ejercicios del modulo de entrenamiento.
 */
public enum NivelEjercicio {

    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado");

    private final String etiqueta;

    NivelEjercicio(String etiqueta) {
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
