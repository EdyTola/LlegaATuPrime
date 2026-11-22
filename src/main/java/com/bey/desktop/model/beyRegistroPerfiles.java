package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyRegistroPerfiles {
    private int version = 1;

    private String perfilActivoId;

    private List<beyPerfilUsuario> perfiles = new ArrayList<>();

    public beyRegistroPerfiles() {
    }

    @JsonIgnore
    public beyPerfilUsuario getPerfilActivo() {
        return buscar(perfilActivoId).orElse(perfiles.isEmpty() ? null : perfiles.get(0));
    }

    public Optional<beyPerfilUsuario> buscar(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return perfiles.stream().filter(p -> id.equals(p.getId())).findFirst();
    }

    public void registrar(beyPerfilUsuario perfil) {
        if (perfil == null) {
            return;
        }
        perfiles.removeIf(p -> p.getId().equals(perfil.getId()));
        perfiles.add(perfil);
        if (perfilActivoId == null) {
            perfilActivoId = perfil.getId();
        }
    }

    public boolean quitar(String id) {
        if (id == null) {
            return false;
        }
        boolean quitado = perfiles.removeIf(p -> id.equals(p.getId()));
        if (quitado && id.equals(perfilActivoId)) {
            perfilActivoId = perfiles.isEmpty() ? null : perfiles.get(0).getId();
        }
        return quitado;
    }

    @JsonIgnore
    public boolean existeNombre(String nombre) {
        if (nombre == null) {
            return false;
        }
        return perfiles.stream().anyMatch(p -> nombre.equalsIgnoreCase(p.getNombre()));
    }

    @JsonIgnore
    public int getTotal() {
        return perfiles.size();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPerfilActivoId() {
        return perfilActivoId;
    }

    public void setPerfilActivoId(String perfilActivoId) {
        this.perfilActivoId = perfilActivoId;
    }

    public List<beyPerfilUsuario> getPerfiles() {
        return perfiles;
    }

    public void setPerfiles(List<beyPerfilUsuario> perfiles) {
        this.perfiles = perfiles == null ? new ArrayList<>() : perfiles;
    }
}
