package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyFotoProgreso {
    private String id = UUID.randomUUID().toString();
    private LocalDate fecha;
    private beyTipoPose pose = beyTipoPose.FRENTE;
    private String rutaArchivo;
    private String notas;

    public beyFotoProgreso() {
    }

    public beyFotoProgreso(LocalDate fecha, beyTipoPose pose, String rutaArchivo) {
        this.fecha = fecha;
        this.pose = pose;
        this.rutaArchivo = rutaArchivo;
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

    public beyTipoPose getPose() {
        return pose == null ? beyTipoPose.FRENTE : pose;
    }

    public void setPose(beyTipoPose pose) {
        this.pose = pose == null ? beyTipoPose.FRENTE : pose;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return fecha + " - " + getPose().getEtiqueta();
    }
}
