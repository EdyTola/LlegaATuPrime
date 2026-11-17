package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de salud y nutricion de una fecha concreta: comidas, hidratacion,
 * suplementacion y descanso (Modulos 1 y 3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiaSalud {

    private LocalDate fecha;
    private List<Comida> comidas = new ArrayList<>();
    private List<RegistroHidratacion> hidratacion = new ArrayList<>();
    private boolean creatinaTomada;
    private double horasSueno;
    private String notas;

    public DiaSalud() {
    }

    public DiaSalud(LocalDate fecha) {
        this.fecha = fecha;
    }

    @JsonIgnore
    public double getTotalProteina() {
        return comidas.stream().mapToDouble(Comida::getProteinaGramos).sum();
    }

    @JsonIgnore
    public double getTotalCalorias() {
        return comidas.stream().mapToDouble(Comida::getCalorias).sum();
    }

    @JsonIgnore
    public int getTotalHidratacionMl() {
        return hidratacion.stream().mapToInt(RegistroHidratacion::getMililitros).sum();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public List<Comida> getComidas() {
        return comidas;
    }

    public void setComidas(List<Comida> comidas) {
        this.comidas = comidas == null ? new ArrayList<>() : comidas;
    }

    public List<RegistroHidratacion> getHidratacion() {
        return hidratacion;
    }

    public void setHidratacion(List<RegistroHidratacion> hidratacion) {
        this.hidratacion = hidratacion == null ? new ArrayList<>() : hidratacion;
    }

    public boolean isCreatinaTomada() {
        return creatinaTomada;
    }

    public void setCreatinaTomada(boolean creatinaTomada) {
        this.creatinaTomada = creatinaTomada;
    }

    public double getHorasSueno() {
        return horasSueno;
    }

    public void setHorasSueno(double horasSueno) {
        this.horasSueno = horasSueno;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return String.valueOf(fecha);
    }
}
