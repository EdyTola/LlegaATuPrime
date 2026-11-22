package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyConfiguracionApp {
    private int descansoPorDefectoSegundos = 90;

    private String ultimaCarpetaMusica;

    private String ultimaCarpetaMultimedia;

    private double volumenReproductor = 0.8;
    private boolean guardadoAutomatico = true;

    private beyTemaApp tema = beyTemaApp.CLARO;

    private beyModalidadEntrenamiento modalidad = beyModalidadEntrenamiento.GIMNASIO;

    private boolean sfxActivados = true;
    private double volumenSfx = 0.6;

    public beyConfiguracionApp() {
    }

    public boolean isSfxActivados() {
        return sfxActivados;
    }

    public void setSfxActivados(boolean sfxActivados) {
        this.sfxActivados = sfxActivados;
    }

    public double getVolumenSfx() {
        return volumenSfx;
    }

    public void setVolumenSfx(double volumenSfx) {
        this.volumenSfx = Math.max(0, Math.min(1, volumenSfx));
    }

    public beyModalidadEntrenamiento getModalidad() {
        return modalidad == null ? beyModalidadEntrenamiento.GIMNASIO : modalidad;
    }

    public void setModalidad(beyModalidadEntrenamiento modalidad) {
        this.modalidad = modalidad == null ? beyModalidadEntrenamiento.GIMNASIO : modalidad;
    }

    public int getDescansoPorDefectoSegundos() {
        return descansoPorDefectoSegundos;
    }

    public void setDescansoPorDefectoSegundos(int descansoPorDefectoSegundos) {
        this.descansoPorDefectoSegundos = descansoPorDefectoSegundos;
    }

    public String getUltimaCarpetaMusica() {
        return ultimaCarpetaMusica;
    }

    public void setUltimaCarpetaMusica(String ultimaCarpetaMusica) {
        this.ultimaCarpetaMusica = ultimaCarpetaMusica;
    }

    public String getUltimaCarpetaMultimedia() {
        return ultimaCarpetaMultimedia;
    }

    public void setUltimaCarpetaMultimedia(String ultimaCarpetaMultimedia) {
        this.ultimaCarpetaMultimedia = ultimaCarpetaMultimedia;
    }

    public double getVolumenReproductor() {
        return volumenReproductor;
    }

    public void setVolumenReproductor(double volumenReproductor) {
        this.volumenReproductor = volumenReproductor;
    }

    public boolean isGuardadoAutomatico() {
        return guardadoAutomatico;
    }

    public void setGuardadoAutomatico(boolean guardadoAutomatico) {
        this.guardadoAutomatico = guardadoAutomatico;
    }

    public beyTemaApp getTema() {
        return tema == null ? beyTemaApp.CLARO : tema;
    }

    public void setTema(beyTemaApp tema) {
        this.tema = tema == null ? beyTemaApp.CLARO : tema;
    }
}
