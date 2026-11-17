package com.symmetry.desktop.model;

import java.time.LocalDate;

/**
 * Dias de la semana usados para programar las rutinas.
 */
public enum DiaSemana {

    LUNES("Lunes"),
    MARTES("Martes"),
    MIERCOLES("Miércoles"),
    JUEVES("Jueves"),
    VIERNES("Viernes"),
    SABADO("Sábado"),
    DOMINGO("Domingo");

    private final String etiqueta;

    DiaSemana(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Devuelve el dia de la semana correspondiente a una fecha concreta. */
    public static DiaSemana desdeFecha(LocalDate fecha) {
        return values()[fecha.getDayOfWeek().getValue() - 1];
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
