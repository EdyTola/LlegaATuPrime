package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyPerfilUsuario {
    private String id = UUID.randomUUID().toString();
    private String nombre = "Yoel Fernando";
    private Integer edad;

    private String objetivoGeneral;

    private String customQuote;

    private String rutaFotoPerfil;
    private LocalDate fechaRegistro = LocalDate.now();
    private beyObjetivosNutricionales objetivos = new beyObjetivosNutricionales();

    public beyPerfilUsuario() {
    }

    public beyPerfilUsuario(String nombre) {
        this.nombre = nombre;
    }

    @JsonIgnore
    public String getPrimerNombre() {
        if (nombre == null || nombre.isBlank()) {
            return "Atleta";
        }
        return nombre.trim().split("\\s+")[0];
    }

    @JsonIgnore
    public String getIniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder iniciales = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isEmpty() && iniciales.length() < 2) {
                iniciales.append(parte.substring(0, 1).toUpperCase());
            }
        }
        return iniciales.toString();
    }

    @JsonIgnore
    public String getEdadTexto() {
        return edad == null ? "Edad sin definir" : edad + " años";
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

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getObjetivoGeneral() {
        return objetivoGeneral;
    }

    public void setObjetivoGeneral(String objetivoGeneral) {
        this.objetivoGeneral = objetivoGeneral;
    }

    public String getCustomQuote() {
        return customQuote;
    }

    public void setCustomQuote(String customQuote) {
        this.customQuote = customQuote;
    }

    public String getRutaFotoPerfil() {
        return rutaFotoPerfil;
    }

    public void setRutaFotoPerfil(String rutaFotoPerfil) {
        this.rutaFotoPerfil = rutaFotoPerfil;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public beyObjetivosNutricionales getObjetivos() {
        return objetivos;
    }

    public void setObjetivos(beyObjetivosNutricionales objetivos) {
        this.objetivos = objetivos == null ? new beyObjetivosNutricionales() : objetivos;
    }
}
