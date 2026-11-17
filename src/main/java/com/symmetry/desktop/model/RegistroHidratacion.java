package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro individual de hidratacion (Modulo 3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistroHidratacion {

    private String id = UUID.randomUUID().toString();
    private int mililitros;
    private Recipiente recipiente = Recipiente.PERSONALIZADO;
    private LocalDateTime fechaHora = LocalDateTime.now();

    public RegistroHidratacion() {
    }

    public RegistroHidratacion(int mililitros, Recipiente recipiente, LocalDateTime fechaHora) {
        this.mililitros = mililitros;
        this.recipiente = recipiente;
        this.fechaHora = fechaHora;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMililitros() {
        return mililitros;
    }

    public void setMililitros(int mililitros) {
        this.mililitros = mililitros;
    }

    public Recipiente getRecipiente() {
        return recipiente;
    }

    public void setRecipiente(Recipiente recipiente) {
        this.recipiente = recipiente;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public String toString() {
        return mililitros + " ml - " + recipiente.getEtiqueta();
    }
}
