package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Pista de la biblioteca musical (Modulo 4). Puede ser un archivo de audio local
 * (.mp3 / .wav) o un enlace externo (por ejemplo, YouTube).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PistaAudio {

    private String id = UUID.randomUUID().toString();
    private String titulo;
    private String rutaArchivo;
    private String artista;
    private double duracionSegundos;

    /** Indica si la pista es un archivo local o un enlace externo. */
    private OrigenPista origen = OrigenPista.LOCAL;

    /** URL del enlace externo (YouTube u otro) cuando {@code origen} es ENLACE. */
    private String enlace;

    public PistaAudio() {
    }

    public PistaAudio(String titulo, String rutaArchivo) {
        this.titulo = titulo;
        this.rutaArchivo = rutaArchivo;
    }

    /** Crea una pista a partir de un enlace externo (por ejemplo, YouTube). */
    public static PistaAudio desdeEnlace(String titulo, String enlace) {
        PistaAudio pista = new PistaAudio();
        pista.setTitulo(titulo);
        pista.setEnlace(enlace);
        pista.setOrigen(OrigenPista.ENLACE);
        return pista;
    }

    /** true cuando la pista apunta a un enlace externo en lugar de a un archivo local. */
    @JsonIgnore
    public boolean esEnlace() {
        return getOrigen() == OrigenPista.ENLACE;
    }

    @JsonIgnore
    public String getDuracionFormateada() {
        if (esEnlace() && duracionSegundos <= 0) {
            return "—";
        }
        int total = (int) Math.round(duracionSegundos);
        return String.format("%d:%02d", total / 60, total % 60);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public double getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(double duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    /** Origen de la pista; nunca es null (los archivos antiguos sin el campo son locales). */
    public OrigenPista getOrigen() {
        return origen == null ? OrigenPista.LOCAL : origen;
    }

    public void setOrigen(OrigenPista origen) {
        this.origen = origen == null ? OrigenPista.LOCAL : origen;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
