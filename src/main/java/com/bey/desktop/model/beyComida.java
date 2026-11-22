package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyComida {
    private String id = UUID.randomUUID().toString();
    private String nombre;
    private double proteinaGramos;
    private double calorias;
    private LocalDateTime fechaHora = LocalDateTime.now();
    private String notas;

    public beyComida() {
    }

    public beyComida(String nombre, double proteinaGramos, double calorias, LocalDateTime fechaHora) {
        this.nombre = nombre;
        this.proteinaGramos = proteinaGramos;
        this.calorias = calorias;
        this.fechaHora = fechaHora;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
