package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fotografia de progreso de la galeria de transformacion (Modulo 5).
 * Guarda la fecha, el tipo de pose y la ruta del archivo de imagen local.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FotoProgreso {

    private String id = UUID.randomUUID().toString();
    private LocalDate fecha;
    private TipoPose pose = TipoPose.FRENTE;
    private String rutaArchivo;
    private String notas;

    public FotoProgreso() {
    }

    public FotoProgreso(LocalDate fecha, TipoPose pose, String rutaArchivo) {
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

    public TipoPose getPose() {
        return pose == null ? TipoPose.FRENTE : pose;
    }

    public void setPose(TipoPose pose) {
        this.pose = pose == null ? TipoPose.FRENTE : pose;
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
