package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyMedicionCorporal {
    private String id = UUID.randomUUID().toString();
    private LocalDate fecha;
    private Double pesoKg;
    private Double cinturaCm;

    private Double grasaPorcentaje;

    private Double masaMuscularKg;
    private String notas;

    public beyMedicionCorporal() {
    }

    public beyMedicionCorporal(LocalDate fecha, Double pesoKg, Double cinturaCm) {
        this.fecha = fecha;
        this.pesoKg = pesoKg;
        this.cinturaCm = cinturaCm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Double getCinturaCm() {
        return cinturaCm;
    }

    public void setCinturaCm(Double cinturaCm) {
        this.cinturaCm = cinturaCm;
    }

    public Double getGrasaPorcentaje() {
        return grasaPorcentaje;
    }

    public void setGrasaPorcentaje(Double grasaPorcentaje) {
        this.grasaPorcentaje = grasaPorcentaje;
    }

    public Double getMasaMuscularKg() {
        return masaMuscularKg;
    }

    public void setMasaMuscularKg(Double masaMuscularKg) {
        this.masaMuscularKg = masaMuscularKg;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return fecha + " - " + pesoKg + " kg / " + cinturaCm + " cm";
    }
}
