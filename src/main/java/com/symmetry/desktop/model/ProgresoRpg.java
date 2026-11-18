package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado de gamificacion del usuario: experiencia acumulada, metas diarias ya reclamadas
 * (para no otorgar XP dos veces) y retos dinamicos vigentes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgresoRpg {

    private int xpTotal;
    /** Metas reclamadas por fecha ISO (yyyy-MM-dd) -> nombres de {@link MetaXp}. */
    private Map<String, List<String>> metasReclamadas = new HashMap<>();
    private List<RetoDiario> retos = new ArrayList<>();
    private int retosCompletados;

    public ProgresoRpg() {
    }

    public boolean yaReclamada(LocalDate fecha, MetaXp meta) {
        return metasReclamadas.getOrDefault(fecha.toString(), List.of()).contains(meta.name());
    }

    public void marcarReclamada(LocalDate fecha, MetaXp meta) {
        metasReclamadas.computeIfAbsent(fecha.toString(), k -> new ArrayList<>()).add(meta.name());
    }

    public int getXpTotal() {
        return xpTotal;
    }

    public void setXpTotal(int xpTotal) {
        this.xpTotal = Math.max(0, xpTotal);
    }

    public Map<String, List<String>> getMetasReclamadas() {
        return metasReclamadas;
    }

    public void setMetasReclamadas(Map<String, List<String>> metasReclamadas) {
        this.metasReclamadas = metasReclamadas == null ? new HashMap<>() : metasReclamadas;
    }

    public List<RetoDiario> getRetos() {
        return retos;
    }

    public void setRetos(List<RetoDiario> retos) {
        this.retos = retos == null ? new ArrayList<>() : retos;
    }

    public int getRetosCompletados() {
        return retosCompletados;
    }

    public void setRetosCompletados(int retosCompletados) {
        this.retosCompletados = retosCompletados;
    }
}
