package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyRegistroEjercicio {
    private LocalDate fecha;
    private String ejercicioId;
    private String nombreEjercicio;
    private beyGrupoMuscular grupoMuscular;
    private String prescripcion;
    private List<beySerieRegistrada> series = new ArrayList<>();

    public beyRegistroEjercicio() {
    }

    public beyRegistroEjercicio(beyEjercicio ejercicio) {
        this(ejercicio, null);
    }

    public beyRegistroEjercicio(beyEjercicio ejercicio, LocalDate fecha) {
        this.ejercicioId = ejercicio.getId();
        this.nombreEjercicio = ejercicio.getNombre();
        this.grupoMuscular = ejercicio.getGrupoMuscular();
        this.prescripcion = ejercicio.getPrescripcion();
        this.fecha = fecha;
    }

    @JsonIgnore
    public double getVolumenTotal() {
        return series.stream().filter(beySerieRegistrada::esRegistrada)
                .mapToDouble(beySerieRegistrada::getVolumen).sum();
    }

    @JsonIgnore
    public long getSeriesCompletadas() {
        return series.stream().filter(beySerieRegistrada::isCompletada).count();
    }

    @JsonIgnore
    public double getPesoMaximo() {
        return series.stream().filter(beySerieRegistrada::esRegistrada)
                .mapToDouble(beySerieRegistrada::getPesoKg).max().orElse(0);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public String getNombreEjercicio() {
        return nombreEjercicio;
    }

    public void setNombreEjercicio(String nombreEjercicio) {
        this.nombreEjercicio = nombreEjercicio;
    }

    public beyGrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(beyGrupoMuscular grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getPrescripcion() {
        return prescripcion;
    }

    public void setPrescripcion(String prescripcion) {
        this.prescripcion = prescripcion;
    }

    public List<beySerieRegistrada> getSeries() {
        return series;
    }

    public void setSeries(List<beySerieRegistrada> series) {
        this.series = series == null ? new ArrayList<>() : series;
    }
}
