package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyRegistroHidratacion {
    private String id = UUID.randomUUID().toString();
    private int mililitros;
    private beyRecipiente recipiente = beyRecipiente.PERSONALIZADO;
    private LocalDateTime fechaHora = LocalDateTime.now();

    public beyRegistroHidratacion() {
    }

    public beyRegistroHidratacion(int mililitros, beyRecipiente recipiente, LocalDateTime fechaHora) {
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

    public beyRecipiente getRecipiente() {
        return recipiente;
    }

    public void setRecipiente(beyRecipiente recipiente) {
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
