package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyDiaSalud {
    private LocalDate fecha;
    private List<beyComida> comidas = new ArrayList<>();
    private List<beyRegistroHidratacion> hidratacion = new ArrayList<>();
    private boolean creatinaTomada;
    private double horasSueno;
    private String notas;

    public beyDiaSalud() {
    }

    public beyDiaSalud(LocalDate fecha) {
        this.fecha = fecha;
    }

    @JsonIgnore
    public double getTotalProteina() {
        return comidas.stream().mapToDouble(beyComida::getProteinaGramos).sum();
    }

    @JsonIgnore
    public double getTotalCalorias() {
        return comidas.stream().mapToDouble(beyComida::getCalorias).sum();
    }

    @JsonIgnore
    public int getTotalHidratacionMl() {
        return hidratacion.stream().mapToInt(beyRegistroHidratacion::getMililitros).sum();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public List<beyComida> getComidas() {
        return comidas;
    }

    public void setComidas(List<beyComida> comidas) {
        this.comidas = comidas == null ? new ArrayList<>() : comidas;
    }

    public List<beyRegistroHidratacion> getHidratacion() {
        return hidratacion;
    }

    public void setHidratacion(List<beyRegistroHidratacion> hidratacion) {
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
