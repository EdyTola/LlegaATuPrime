package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reto dinamico generado a partir del historial del usuario. Vive en una ventana de tiempo
 * (12 h para los retos flash, 24 h para los diarios) y otorga XP extra al completarse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetoDiario {

    private String id = UUID.randomUUID().toString();
    /** Identificador de la ventana que lo origino (evita regenerarlo dos veces). */
    private String ventanaId;
    private boolean flash;
    private TipoReto tipo;
    private String titulo;
    private String descripcion;
    private GrupoMuscular grupoMuscular;
    private NivelEjercicio nivel;
    /** Valor a alcanzar: series o mililitros segun el tipo. */
    private int objetivo;
    private int progreso;
    private int xpRecompensa;
    private boolean completado;
    private LocalDateTime inicio;
    private LocalDateTime expira;

    public RetoDiario() {
    }

    @JsonIgnore
    public double getFraccion() {
        return objetivo <= 0 ? 0 : Math.min(1.0, (double) progreso / objetivo);
    }

    /** Tiempo restante en texto corto, por ejemplo "5 h 12 min". */
    public String restante(LocalDateTime ahora) {
        if (expira == null || !ahora.isBefore(expira)) {
            return "Expirado";
        }
        Duration d = Duration.between(ahora, expira);
        long horas = d.toHours();
        long minutos = d.toMinutes() % 60;
        return horas > 0 ? horas + " h " + minutos + " min" : minutos + " min";
    }

    /** Texto del progreso, por ejemplo "4 / 8 series" o "900 / 1500 ml". */
    @JsonIgnore
    public String getProgresoTexto() {
        String unidad = tipo == TipoReto.HIDRATACION_VENTANA ? "ml" : "series";
        return Math.min(progreso, objetivo) + " / " + objetivo + " " + unidad;
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

    public TipoReto getTipo() {
        return tipo;
    }

    public void setTipo(TipoReto tipo) {
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

    public GrupoMuscular getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(GrupoMuscular grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public NivelEjercicio getNivel() {
        return nivel;
    }

    public void setNivel(NivelEjercicio nivel) {
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
