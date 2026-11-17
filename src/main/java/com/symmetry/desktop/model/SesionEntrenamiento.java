package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sesion de entrenamiento completada o en curso (Workout Player, Modulo 2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SesionEntrenamiento {

    private String id = UUID.randomUUID().toString();
    private String rutinaId;
    private String nombreRutina;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String notas;
    private List<RegistroEjercicio> registros = new ArrayList<>();

    public SesionEntrenamiento() {
    }

    public SesionEntrenamiento(Rutina rutina, LocalDateTime fechaInicio) {
        this.rutinaId = rutina.getId();
        this.nombreRutina = rutina.getNombre();
        this.fechaInicio = fechaInicio;
    }

    /** Duracion efectiva de la sesion; si sigue abierta usa el momento actual. */
    @JsonIgnore
    public Duration getDuracion() {
        if (fechaInicio == null) {
            return Duration.ZERO;
        }
        LocalDateTime fin = fechaFin != null ? fechaFin : LocalDateTime.now();
        return Duration.between(fechaInicio, fin);
    }

    @JsonIgnore
    public long getDuracionMinutos() {
        return getDuracion().toMinutes();
    }

    @JsonIgnore
    public String getDuracionFormateada() {
        long totalSegundos = getDuracion().getSeconds();
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        return horas > 0
                ? String.format("%dh %02dm %02ds", horas, minutos, segundos)
                : String.format("%02dm %02ds", minutos, segundos);
    }

    @JsonIgnore
    public double getVolumenTotal() {
        return registros.stream().mapToDouble(RegistroEjercicio::getVolumenTotal).sum();
    }

    @JsonIgnore
    public long getSeriesCompletadas() {
        return registros.stream().mapToLong(RegistroEjercicio::getSeriesCompletadas).sum();
    }

    @JsonIgnore
    public LocalDate getFecha() {
        return fechaInicio == null ? null : fechaInicio.toLocalDate();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRutinaId() {
        return rutinaId;
    }

    public void setRutinaId(String rutinaId) {
        this.rutinaId = rutinaId;
    }

    public String getNombreRutina() {
        return nombreRutina;
    }

    public void setNombreRutina(String nombreRutina) {
        this.nombreRutina = nombreRutina;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<RegistroEjercicio> getRegistros() {
        return registros;
    }

    public void setRegistros(List<RegistroEjercicio> registros) {
        this.registros = registros == null ? new ArrayList<>() : registros;
    }

    @Override
    public String toString() {
        return nombreRutina + " - " + (fechaInicio == null ? "" : fechaInicio.toLocalDate());
    }
}
