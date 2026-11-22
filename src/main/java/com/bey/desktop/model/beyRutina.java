package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyRutina {
    private String id = UUID.randomUUID().toString();
    private String nombre;
    private beyDiaSemana diaSemana;
    private String notas;
    private List<beyEjercicio> ejercicios = new ArrayList<>();

    public beyRutina() {
    }

    public beyRutina(String nombre, beyDiaSemana diaSemana) {
        this.nombre = nombre;
        this.diaSemana = diaSemana;
    }

    public void agregarEjercicio(beyEjercicio ejercicio) {
        if (ejercicio != null) {
            ejercicios.add(ejercicio);
        }
    }

    @JsonIgnore
    public int getSeriesTotales() {
        return ejercicios.stream().mapToInt(beyEjercicio::getSeries).sum();
    }

    @JsonIgnore
    public String getTituloCompleto() {
        return (diaSemana == null ? "" : diaSemana.getEtiqueta() + " - ") + nombre;
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

    public beyDiaSemana getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(beyDiaSemana diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<beyEjercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<beyEjercicio> ejercicios) {
        this.ejercicios = ejercicios == null ? new ArrayList<>() : ejercicios;
    }

    @Override
    public String toString() {
        return getTituloCompleto();
    }
}
