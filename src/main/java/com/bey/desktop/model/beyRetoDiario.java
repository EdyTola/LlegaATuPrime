package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyRetoDiario {
    private String id = UUID.randomUUID().toString();

    private String ventanaId;
    private boolean flash;
    private beyTipoReto tipo;
    private String titulo;
    private String descripcion;
    private beyGrupoMuscular grupoMuscular;
    private beyNivelEjercicio nivel;

    private int objetivo;
    private int progreso;
    private int xpRecompensa;
    private boolean completado;
    private LocalDateTime inicio;
    private LocalDateTime expira;

    public beyRetoDiario() {
    }

    @JsonIgnore
    public double getFraccion() {
        return objetivo <= 0 ? 0 : Math.min(1.0, (double) progreso / objetivo);
    }

    public String restante(LocalDateTime ahora) {
        if (expira == null || !ahora.isBefore(expira)) {
            return "Expirado";
        }
        Duration d = Duration.between(ahora, expira);
        long horas = d.toHours();
        long minutos = d.toMinutes() % 60;
        return horas > 0 ? horas + " h " + minutos + " min" : minutos + " min";
    }

    @JsonIgnore
    public String getProgresoTexto() {
        if (tipo != null && tipo.requiereConfirmacion()) {
            return completado ? "Completado" : "Pendiente de confirmar";
        }
        String unidad = tipo == beyTipoReto.HIDRATACION_VENTANA ? "ml" : "series";
        return Math.min(progreso, objetivo) + " / " + objetivo + " " + unidad;
    }

    @JsonIgnore
    public boolean esManual() {
        return tipo != null && tipo.requiereConfirmacion();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVentanaId() {
        return ventanaId;
    }

    public void setVentanaId(String ventanaId) {
        this.ventanaId = ventanaId;
    }

    public boolean isFlash() {
        return flash;
    }

    public void setFlash(boolean flash) {
        this.flash = flash;
    }

    public beyTipoReto getTipo() {
        return tipo;
    }

    public void setTipo(beyTipoReto tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public beyGrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(beyGrupoMuscular grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public beyNivelEjercicio getNivel() {
        return nivel;
    }

    public void setNivel(beyNivelEjercicio nivel) {
        this.nivel = nivel;
    }

    public int getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(int objetivo) {
        this.objetivo = objetivo;
    }

    public int getProgreso() {
        return progreso;
    }

    public void setProgreso(int progreso) {
        this.progreso = progreso;
    }

    public int getXpRecompensa() {
        return xpRecompensa;
    }

    public void setXpRecompensa(int xpRecompensa) {
        this.xpRecompensa = xpRecompensa;
    }

    public boolean isCompletado() {
        return completado;
    }

    public void setCompletado(boolean completado) {
        this.completado = completado;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getExpira() {
        return expira;
    }

    public void setExpira(LocalDateTime expira) {
        this.expira = expira;
    }
}
