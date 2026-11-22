package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyPistaAudio {
    private String id = UUID.randomUUID().toString();
    private String titulo;
    private String rutaArchivo;
    private String artista;
    private double duracionSegundos;

    private beyOrigenPista origen = beyOrigenPista.LOCAL;

    private String enlace;

    public beyPistaAudio() {
    }

    public beyPistaAudio(String titulo, String rutaArchivo) {
        this.titulo = titulo;
        this.rutaArchivo = rutaArchivo;
    }

    public static beyPistaAudio desdeEnlace(String titulo, String enlace) {
        beyPistaAudio pista = new beyPistaAudio();
        pista.setTitulo(titulo);
        pista.setEnlace(enlace);
        pista.setOrigen(beyOrigenPista.ENLACE);
        return pista;
    }

    @JsonIgnore
    public boolean esEnlace() {
        return getOrigen() == beyOrigenPista.ENLACE;
    }

    @JsonIgnore
    public String getDuracionFormateada() {
        if (esEnlace() && duracionSegundos <= 0) {
            return "—";
        }
        int total = (int) Math.round(duracionSegundos);
        return String.format("%02d:%02d", total / 60, total % 60);
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

    public beyOrigenPista getOrigen() {
        return origen == null ? beyOrigenPista.LOCAL : origen;
    }

    public void setOrigen(beyOrigenPista origen) {
        this.origen = origen == null ? beyOrigenPista.LOCAL : origen;
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
