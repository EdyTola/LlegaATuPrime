package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ejercicio de una rutina: prescripcion (series x repeticiones, RIR) y recursos multimedia.
 * El campo {@code rutaMultimedia} apunta a un archivo local (.png, .jpg, .gif, .mp4).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ejercicio {

    private String id = UUID.randomUUID().toString();
    private String nombre;
    private GrupoMuscular grupoMuscular;

    private int series = 3;
    private int repeticionesMin = 8;
    private int repeticionesMax = 12;
    private int rirObjetivo = 2;

    /** true cuando la prescripcion se mide en segundos (por ejemplo, plancha abdominal). */
    private boolean porTiempo;

    /** Descanso recomendado entre series, en segundos (60 a 180). */
    private int descansoSegundos = 90;

    /** Ruta absoluta del archivo multimedia adjunto, o null si aun no se ha cargado. */
    private String rutaMultimedia;

    /**
     * Enlace o ruta de referencia externa (por ejemplo, un video de YouTube) que sirve
     * como guia tecnica visual cuando no se adjunta un archivo local.
     */
    private String enlaceMultimedia;

    private String notas;

    /** Alternativas adicionales definidas por el usuario para este ejercicio. */
    private List<String> alternativasPersonalizadas = new ArrayList<>();

    /**
     * Nombre del ejercicio original cuando esta instancia es una variante generada por el
     * modo sin equipo. Es solo de presentacion: nunca se persiste.
     */
    @JsonIgnore
    private String adaptadoDe;

    public Ejercicio() {
    }

    @JsonIgnore
    public String getAdaptadoDe() {
        return adaptadoDe;
    }

    @JsonIgnore
    public void setAdaptadoDe(String adaptadoDe) {
        this.adaptadoDe = adaptadoDe;
    }

    public Ejercicio(String nombre, GrupoMuscular grupoMuscular, int series, int repeticionesMin,
                     int repeticionesMax, int rirObjetivo) {
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
        this.series = series;
        this.repeticionesMin = repeticionesMin;
        this.repeticionesMax = repeticionesMax;
        this.rirObjetivo = rirObjetivo;
    }

    /** Texto compacto de la prescripcion, por ejemplo "3x6-10" o "2x30-60s". */
    @JsonIgnore
    public String getPrescripcion() {
        String rango = repeticionesMin == repeticionesMax
                ? String.valueOf(repeticionesMin)
                : repeticionesMin + "-" + repeticionesMax;
        return series + "x" + rango + (porTiempo ? "s" : "");
    }

    /** Texto compacto del RIR objetivo, por ejemplo "RIR 1-2". */
    @JsonIgnore
    public String getRirTexto() {
        return "RIR " + rirObjetivo + "-" + (rirObjetivo + 1);
    }

    /** true si el ejercicio ya cuenta con un archivo local o un enlace de referencia. */
    @JsonIgnore
    public boolean tieneMultimedia() {
        return (rutaMultimedia != null && !rutaMultimedia.isBlank())
                || (enlaceMultimedia != null && !enlaceMultimedia.isBlank());
    }

    /** Descripcion corta del recurso visual disponible, para mostrar en las tablas. */
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

    public GrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(GrupoMuscular grupoMuscular) {
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
