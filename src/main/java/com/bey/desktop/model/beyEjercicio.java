package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyEjercicio {
    private String id = UUID.randomUUID().toString();
    private String nombre;
    private beyGrupoMuscular grupoMuscular;

    private int series = 3;
    private int repeticionesMin = 8;
    private int repeticionesMax = 12;
    private int rirObjetivo = 2;

    private boolean porTiempo;

    private int descansoSegundos = 90;

    private String rutaMultimedia;

    private String enlaceMultimedia;

    private String notas;

    private List<String> alternativasPersonalizadas = new ArrayList<>();

    @JsonIgnore
    private String adaptadoDe;

    public beyEjercicio() {
    }

    @JsonIgnore
    public String getAdaptadoDe() {
        return adaptadoDe;
    }

    @JsonIgnore
    public void setAdaptadoDe(String adaptadoDe) {
        this.adaptadoDe = adaptadoDe;
    }

    public beyEjercicio(String nombre, beyGrupoMuscular grupoMuscular, int series, int repeticionesMin,
                     int repeticionesMax, int rirObjetivo) {
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
        this.series = series;
        this.repeticionesMin = repeticionesMin;
        this.repeticionesMax = repeticionesMax;
        this.rirObjetivo = rirObjetivo;
    }

    @JsonIgnore
    public String getPrescripcion() {
        String rango = repeticionesMin == repeticionesMax
                ? String.valueOf(repeticionesMin)
                : repeticionesMin + "-" + repeticionesMax;
        return series + "x" + rango + (porTiempo ? "s" : "");
    }

    @JsonIgnore
    public String getRirTexto() {
        return "RIR " + rirObjetivo + "-" + (rirObjetivo + 1);
    }

    @JsonIgnore
    public boolean tieneMultimedia() {
        return (rutaMultimedia != null && !rutaMultimedia.isBlank())
                || (enlaceMultimedia != null && !enlaceMultimedia.isBlank());
    }

    @JsonIgnore
    public String getResumenMultimedia() {
        if (rutaMultimedia != null && !rutaMultimedia.isBlank()) {
            int separador = Math.max(rutaMultimedia.lastIndexOf('/'), rutaMultimedia.lastIndexOf('\\'));
            String archivo = separador >= 0 ? rutaMultimedia.substring(separador + 1) : rutaMultimedia;
            return "Archivo: " + archivo;
        }
        if (enlaceMultimedia != null && !enlaceMultimedia.isBlank()) {
            return "Enlace: " + enlaceMultimedia;
        }
        return "Sin recurso visual";
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

    public beyGrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(beyGrupoMuscular grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public int getSeries() {
        return series;
    }

    public void setSeries(int series) {
        this.series = series;
    }

    public int getRepeticionesMin() {
        return repeticionesMin;
    }

    public void setRepeticionesMin(int repeticionesMin) {
        this.repeticionesMin = repeticionesMin;
    }

    public int getRepeticionesMax() {
        return repeticionesMax;
    }

    public void setRepeticionesMax(int repeticionesMax) {
        this.repeticionesMax = repeticionesMax;
    }

    public int getRirObjetivo() {
        return rirObjetivo;
    }

    public void setRirObjetivo(int rirObjetivo) {
        this.rirObjetivo = rirObjetivo;
    }

    public boolean isPorTiempo() {
        return porTiempo;
    }

    public void setPorTiempo(boolean porTiempo) {
        this.porTiempo = porTiempo;
    }

    public int getDescansoSegundos() {
        return descansoSegundos;
    }

    public void setDescansoSegundos(int descansoSegundos) {
        this.descansoSegundos = descansoSegundos;
    }

    public String getRutaMultimedia() {
        return rutaMultimedia;
    }

    public void setRutaMultimedia(String rutaMultimedia) {
        this.rutaMultimedia = rutaMultimedia;
    }

    public String getEnlaceMultimedia() {
        return enlaceMultimedia;
    }

    public void setEnlaceMultimedia(String enlaceMultimedia) {
        this.enlaceMultimedia = enlaceMultimedia;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public List<String> getAlternativasPersonalizadas() {
        return alternativasPersonalizadas;
    }

    public void setAlternativasPersonalizadas(List<String> alternativasPersonalizadas) {
        this.alternativasPersonalizadas = alternativasPersonalizadas == null
                ? new ArrayList<>() : alternativasPersonalizadas;
    }

    @Override
    public String toString() {
        return nombre + " (" + getPrescripcion() + ")";
    }
}
