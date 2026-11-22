package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyProgresoRpg {
    private int xpTotal;

    private Map<String, List<String>> metasReclamadas = new HashMap<>();
    private List<beyRetoDiario> retos = new ArrayList<>();
    private int retosCompletados;

    public beyProgresoRpg() {
    }

    public boolean yaReclamada(LocalDate fecha, beyMetaXp meta) {
        return metasReclamadas.getOrDefault(fecha.toString(), List.of()).contains(meta.name());
    }

    public void marcarReclamada(LocalDate fecha, beyMetaXp meta) {
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

    public List<beyRetoDiario> getRetos() {
        return retos;
    }

    public void setRetos(List<beyRetoDiario> retos) {
        this.retos = retos == null ? new ArrayList<>() : retos;
    }

    public int getRetosCompletados() {
        return retosCompletados;
    }

    public void setRetosCompletados(int retosCompletados) {
        this.retosCompletados = retosCompletados;
    }
}
