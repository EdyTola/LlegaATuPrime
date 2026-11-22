package com.bey.desktop.model;

import java.time.LocalDate;

public enum beyDiaSemana {
    LUNES("Lunes"),
    MARTES("Martes"),
    MIERCOLES("Miércoles"),
    JUEVES("Jueves"),
    VIERNES("Viernes"),
    SABADO("Sábado"),
    DOMINGO("Domingo");

    private final String etiqueta;

    beyDiaSemana(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static beyDiaSemana desdeFecha(LocalDate fecha) {
        return values()[fecha.getDayOfWeek().getValue() - 1];
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
