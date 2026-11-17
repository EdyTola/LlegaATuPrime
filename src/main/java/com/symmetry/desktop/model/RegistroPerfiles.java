package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Indice de los perfiles locales de la aplicacion. Se guarda en un unico archivo
 * ({@code perfiles.json}) y almacena la lista de usuarios disponibles junto al
 * identificador del perfil activo. Cada perfil apunta a su propio archivo de datos,
 * de modo que los progresos, rutinas y metricas de cada usuario quedan aislados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistroPerfiles {

    private int version = 1;

    /** Identificador del perfil actualmente en uso. */
    private String perfilActivoId;

    private List<PerfilUsuario> perfiles = new ArrayList<>();

    public RegistroPerfiles() {
    }

    /** Perfil activo, o {@code null} si el registro aun no tiene perfiles. */
    @JsonIgnore
    public PerfilUsuario getPerfilActivo() {
        return buscar(perfilActivoId).orElse(perfiles.isEmpty() ? null : perfiles.get(0));
    }

    public Optional<PerfilUsuario> buscar(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return perfiles.stream().filter(p -> id.equals(p.getId())).findFirst();
    }

    /** Anade o actualiza un perfil del indice (por identificador). */
    public void registrar(PerfilUsuario perfil) {
        if (perfil == null) {
            return;
        }
        perfiles.removeIf(p -> p.getId().equals(perfil.getId()));
        perfiles.add(perfil);
        if (perfilActivoId == null) {
            perfilActivoId = perfil.getId();
        }
    }

    /** Quita un perfil del indice; devuelve true si realmente existia. */
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

    /** true cuando ya existe otro perfil con el mismo nombre (sin distinguir mayusculas). */
    @JsonIgnore
    public boolean existeNombre(String nombre) {
        if (nombre == null) {
            return false;
        }
        return perfiles.stream().anyMatch(p -> nombre.equalsIgnoreCase(p.getNombre()));
    }

    /** Numero de perfiles registrados. */
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

    public List<PerfilUsuario> getPerfiles() {
        return perfiles;
    }

    public void setPerfiles(List<PerfilUsuario> perfiles) {
        this.perfiles = perfiles == null ? new ArrayList<>() : perfiles;
    }
}
