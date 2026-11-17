package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rutina de entrenamiento asociada a un dia de la semana (por ejemplo, "Torso A" el lunes).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rutina {

    private String id = UUID.randomUUID().toString();
    private String nombre;
    private DiaSemana diaSemana;
    private String notas;
    private List<Ejercicio> ejercicios = new ArrayList<>();

    public Rutina() {
    }

    public Rutina(String nombre, DiaSemana diaSemana) {
        this.nombre = nombre;
        this.diaSemana = diaSemana;
    }

    public void agregarEjercicio(Ejercicio ejercicio) {
        if (ejercicio != null) {
            ejercicios.add(ejercicio);
        }
    }

    /** Numero total de series prescritas en la rutina. */
    @JsonIgnore
    public int getSeriesTotales() {
        return ejercicios.stream().mapToInt(Ejercicio::getSeries).sum();
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

    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DiaSemana diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios == null ? new ArrayList<>() : ejercicios;
    }

    @Override
    public String toString() {
        return getTituloCompleto();
    }
}
