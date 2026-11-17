package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloque de series ejecutadas para un ejercicio concreto dentro de una sesion.
 * Guarda una copia del nombre y del grupo muscular para conservar el historico
 * aunque el ejercicio se edite o se elimine del catalogo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistroEjercicio {

    private String ejercicioId;
    private String nombreEjercicio;
    private GrupoMuscular grupoMuscular;
    private String prescripcion;
    private List<SerieRegistrada> series = new ArrayList<>();

    public RegistroEjercicio() {
    }

    public RegistroEjercicio(Ejercicio ejercicio) {
        this.ejercicioId = ejercicio.getId();
        this.nombreEjercicio = ejercicio.getNombre();
        this.grupoMuscular = ejercicio.getGrupoMuscular();
        this.prescripcion = ejercicio.getPrescripcion();
    }

    @JsonIgnore
    public double getVolumenTotal() {
        return series.stream().filter(SerieRegistrada::isCompletada)
                .mapToDouble(SerieRegistrada::getVolumen).sum();
    }

    @JsonIgnore
    public long getSeriesCompletadas() {
        return series.stream().filter(SerieRegistrada::isCompletada).count();
    }

    /** Peso maximo levantado en el ejercicio (indicador de sobrecarga progresiva). */
    @JsonIgnore
    public double getPesoMaximo() {
        return series.stream().filter(SerieRegistrada::isCompletada)
                .mapToDouble(SerieRegistrada::getPesoKg).max().orElse(0);
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

    public GrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(GrupoMuscular grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getPrescripcion() {
        return prescripcion;
    }

    public void setPrescripcion(String prescripcion) {
        this.prescripcion = prescripcion;
    }

    public List<SerieRegistrada> getSeries() {
        return series;
    }

    public void setSeries(List<SerieRegistrada> series) {
        this.series = series == null ? new ArrayList<>() : series;
    }
}
