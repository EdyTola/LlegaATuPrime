package com.symmetry.desktop.model;

import java.time.LocalDate;

/**
 * Punto de datos para los graficos del modulo de progreso (no se persiste).
 */
public class PuntoSerie {

    private final LocalDate fecha;
    private final String etiqueta;
    private final double valor;

    public PuntoSerie(LocalDate fecha, String etiqueta, double valor) {
        this.fecha = fecha;
        this.etiqueta = etiqueta;
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getValor() {
        return valor;
    }
}
