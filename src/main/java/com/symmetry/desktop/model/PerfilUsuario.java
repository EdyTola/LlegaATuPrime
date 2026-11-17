package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Perfil de un usuario local de la aplicacion: identidad, datos personales y metas diarias.
 * Cada perfil se persiste en su propio archivo JSON, de modo que los progresos, rutinas y
 * metricas de cada usuario quedan aislados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerfilUsuario {

    /** Identificador unico del perfil; coincide con el nombre de su archivo de datos. */
    private String id = UUID.randomUUID().toString();
    private String nombre = "Yoel Fernando";
    private Integer edad;
    /** Objetivo principal declarado por el usuario (por ejemplo, ganar masa muscular). */
    private String objetivoGeneral;
    /** Ruta de la foto de perfil local elegida por el usuario. */
    private String rutaFotoPerfil;
    private LocalDate fechaRegistro = LocalDate.now();
    private ObjetivosNutricionales objetivos = new ObjetivosNutricionales();

    public PerfilUsuario() {
    }

    public PerfilUsuario(String nombre) {
        this.nombre = nombre;
    }

    /** Primer nombre del usuario, usado en el saludo del panel principal. */
    @JsonIgnore
    public String getPrimerNombre() {
        if (nombre == null || nombre.isBlank()) {
            return "Atleta";
        }
        return nombre.trim().split("\\s+")[0];
    }

    /** Iniciales del nombre (hasta dos letras) para el avatar cuando no hay foto. */
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

    /** Texto de la edad para mostrar en la interfaz. */
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

    public ObjetivosNutricionales getObjetivos() {
        return objetivos;
    }

    public void setObjetivos(ObjetivosNutricionales objetivos) {
        this.objetivos = objetivos == null ? new ObjetivosNutricionales() : objetivos;
    }
}
