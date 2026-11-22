package com.bey.desktop.model;

import java.time.LocalDate;

public class beyPuntoSerie {
    private final LocalDate fecha;
    private final String etiqueta;
    private final double valor;

    public beyPuntoSerie(LocalDate fecha, String etiqueta, double valor) {
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
