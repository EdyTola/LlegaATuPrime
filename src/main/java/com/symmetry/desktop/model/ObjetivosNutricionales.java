package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Metas diarias del perfil: proteina, calorias, agua, creatina y descanso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjetivosNutricionales {

    private double proteinaGramos = 145;
    private double calorias = 2400;
    private int aguaMililitros = 2800;
    private double creatinaGramos = 5;
    private double suenoMinimoHoras = 7;
    private double suenoMaximoHoras = 9;

    public ObjetivosNutricionales() {
    }

    public double getProteinaGramos() {
        return proteinaGramos;
    }

    public void setProteinaGramos(double proteinaGramos) {
        this.proteinaGramos = proteinaGramos;
    }

    public double getCalorias() {
        return calorias;
    }

    public void setCalorias(double calorias) {
        this.calorias = calorias;
    }

    public int getAguaMililitros() {
        return aguaMililitros;
    }

    public void setAguaMililitros(int aguaMililitros) {
        this.aguaMililitros = aguaMililitros;
    }

    public double getCreatinaGramos() {
        return creatinaGramos;
    }

    public void setCreatinaGramos(double creatinaGramos) {
        this.creatinaGramos = creatinaGramos;
    }

    public double getSuenoMinimoHoras() {
        return suenoMinimoHoras;
    }

    public void setSuenoMinimoHoras(double suenoMinimoHoras) {
        this.suenoMinimoHoras = suenoMinimoHoras;
    }

    public double getSuenoMaximoHoras() {
        return suenoMaximoHoras;
    }

    public void setSuenoMaximoHoras(double suenoMaximoHoras) {
        this.suenoMaximoHoras = suenoMaximoHoras;
    }

    /** Texto del rango objetivo de descanso, por ejemplo "7 - 9 h". */
    @JsonIgnore
    public String getSuenoTexto() {
        return formatearHoras(suenoMinimoHoras) + " - " + formatearHoras(suenoMaximoHoras) + " h";
    }

    private static String formatearHoras(double valor) {
        return valor == Math.floor(valor) ? String.valueOf((int) valor) : String.valueOf(valor);
    }
}
