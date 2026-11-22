package com.bey.desktop.model;

public enum beyModalidadEntrenamiento {
    GIMNASIO("Con equipo", "Gym Mode · máquinas, poleas, barras y mancuernas"),
    CASA("Sin equipo", "Home Mode · calistenia con peso corporal");

    private final String etiqueta;
    private final String descripcion;

    beyModalidadEntrenamiento(String etiqueta, String descripcion) {
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
