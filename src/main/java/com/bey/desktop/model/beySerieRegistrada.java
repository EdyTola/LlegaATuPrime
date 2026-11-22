package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beySerieRegistrada {
    private int numeroSerie;
    private double pesoKg;
    private int repeticiones;
    private int rirReal = -1;
    private boolean completada;

    public beySerieRegistrada() {
    }

    public beySerieRegistrada(int numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    @JsonIgnore
    public double getVolumen() {
        return pesoKg * repeticiones;
    }

    @JsonIgnore
    public boolean esRegistrada() {
        return completada || (pesoKg > 0 && repeticiones > 0);
    }

    public int getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(int numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(int repeticiones) {
        this.repeticiones = repeticiones;
    }

    public int getRirReal() {
        return rirReal;
    }

    public void setRirReal(int rirReal) {
        this.rirReal = rirReal;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    @Override
    public String toString() {
        return "Serie " + numeroSerie + ": " + pesoKg + " kg x " + repeticiones
                + (rirReal >= 0 ? " (RIR " + rirReal + ")" : "");
    }
}
