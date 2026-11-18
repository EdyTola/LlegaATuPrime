package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Serie realmente ejecutada durante una sesion de entrenamiento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SerieRegistrada {

    private int numeroSerie;
    private double pesoKg;
    private int repeticiones;
    private int rirReal = -1;
    private boolean completada;

    public SerieRegistrada() {
    }

    public SerieRegistrada(int numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    /** Volumen de la serie en kilogramos (peso x repeticiones). */
    @JsonIgnore
    public double getVolumen() {
        return pesoKg * repeticiones;
    }

    /**
     * true si la serie aporta al historico: se marco como completada o registra una carga real
     * (peso y repeticiones introducidos). Permite que el volumen no se pierda aunque la serie
     * se haya guardado sin marcar la casilla "Hecha".
     */
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
