package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Preferencias de la aplicacion guardadas junto a los datos del usuario.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfiguracionApp {

    /** Descanso por defecto entre series, en segundos (rango 60 a 180). */
    private int descansoPorDefectoSegundos = 90;

    /** Ultima carpeta de musica elegida por el usuario (Modulo 4). */
    private String ultimaCarpetaMusica;

    /** Ultima carpeta desde la que se adjunto multimedia de ejercicios (Modulo 2). */
    private String ultimaCarpetaMultimedia;

    private double volumenReproductor = 0.8;
    private boolean guardadoAutomatico = true;

    /** Tema visual preferido por el usuario (claro u oscuro). */
    private TemaApp tema = TemaApp.CLARO;

    public ConfiguracionApp() {
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

    /** Tema activo; nunca es null (los archivos antiguos sin el campo usan modo claro). */
    public TemaApp getTema() {
        return tema == null ? TemaApp.CLARO : tema;
    }

    public void setTema(TemaApp tema) {
        this.tema = tema == null ? TemaApp.CLARO : tema;
    }
}
