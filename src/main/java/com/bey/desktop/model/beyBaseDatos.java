package com.bey.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class beyBaseDatos {
    private int version = 3;

    private beyPerfilUsuario perfil = new beyPerfilUsuario();
    private beyConfiguracionApp configuracion = new beyConfiguracionApp();
    private List<beyRutina> rutinas = new ArrayList<>();
    private List<beySesionEntrenamiento> sesiones = new ArrayList<>();
    private List<beyDiaSalud> dias = new ArrayList<>();
    private List<beyMedicionCorporal> mediciones = new ArrayList<>();
    private List<beyFotoProgreso> fotos = new ArrayList<>();
    private List<beyPistaAudio> bibliotecaMusical = new ArrayList<>();
    private beyProgresoRpg progresoRpg = new beyProgresoRpg();

    public beyBaseDatos() {
    }

    public beyDiaSalud obtenerDia(LocalDate fecha) {
        return buscarDia(fecha).orElseGet(() -> {
            beyDiaSalud nuevo = new beyDiaSalud(fecha);
            dias.add(nuevo);
            return nuevo;
        });
    }

    public Optional<beyDiaSalud> buscarDia(LocalDate fecha) {
        return dias.stream().filter(d -> fecha.equals(d.getFecha())).findFirst();
    }

    public Optional<beyRutina> buscarRutinaDe(LocalDate fecha) {
        return rutinaPorDia(beyDiaSemana.desdeFecha(fecha));
    }

    public Optional<beyRutina> rutinaPorDia(beyDiaSemana dia) {
        return rutinas.stream().filter(r -> r.getDiaSemana() == dia).findFirst();
    }

    public Optional<beyRutina> buscarRutinaPorId(String id) {
        return rutinas.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public List<beyMedicionCorporal> medicionesOrdenadas() {
        List<beyMedicionCorporal> copia = new ArrayList<>(mediciones);
        copia.sort(Comparator.comparing(beyMedicionCorporal::getFecha));
        return copia;
    }

    public List<beySesionEntrenamiento> sesionesOrdenadas() {
        List<beySesionEntrenamiento> copia = new ArrayList<>(sesiones);
        copia.sort(Comparator.comparing(beySesionEntrenamiento::getFechaInicio));
        return copia;
    }

    public List<beyFotoProgreso> fotosOrdenadas() {
        List<beyFotoProgreso> copia = new ArrayList<>(fotos);
        copia.sort(Comparator.comparing(beyFotoProgreso::getFecha).reversed());
        return copia;
    }

    public List<beyEjercicio> catalogoEjercicios() {
        List<beyEjercicio> catalogo = new ArrayList<>();
        for (beyRutina rutina : rutinas) {
            for (beyEjercicio ejercicio : rutina.getEjercicios()) {
                boolean repetido = catalogo.stream()
                        .anyMatch(e -> e.getNombre().equalsIgnoreCase(ejercicio.getNombre()));
                if (!repetido) {
                    catalogo.add(ejercicio);
                }
            }
        }
        return catalogo;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public beyPerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(beyPerfilUsuario perfil) {
        this.perfil = perfil == null ? new beyPerfilUsuario() : perfil;
    }

    public beyConfiguracionApp getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(beyConfiguracionApp configuracion) {
        this.configuracion = configuracion == null ? new beyConfiguracionApp() : configuracion;
    }

    public List<beyRutina> getRutinas() {
        return rutinas;
    }

    public void setRutinas(List<beyRutina> rutinas) {
        this.rutinas = rutinas == null ? new ArrayList<>() : rutinas;
    }

    public List<beySesionEntrenamiento> getSesiones() {
        return sesiones;
    }

    public void setSesiones(List<beySesionEntrenamiento> sesiones) {
        this.sesiones = sesiones == null ? new ArrayList<>() : sesiones;
    }

    public List<beyDiaSalud> getDias() {
        return dias;
    }

    public void setDias(List<beyDiaSalud> dias) {
        this.dias = dias == null ? new ArrayList<>() : dias;
    }

    public List<beyMedicionCorporal> getMediciones() {
        return mediciones;
    }

    public void setMediciones(List<beyMedicionCorporal> mediciones) {
        this.mediciones = mediciones == null ? new ArrayList<>() : mediciones;
    }

    public List<beyFotoProgreso> getFotos() {
        return fotos;
    }

    public void setFotos(List<beyFotoProgreso> fotos) {
        this.fotos = fotos == null ? new ArrayList<>() : fotos;
    }

    public List<beyPistaAudio> getBibliotecaMusical() {
        return bibliotecaMusical;
    }

    public void setBibliotecaMusical(List<beyPistaAudio> bibliotecaMusical) {
        this.bibliotecaMusical = bibliotecaMusical == null ? new ArrayList<>() : bibliotecaMusical;
    }

    public beyProgresoRpg getProgresoRpg() {
        return progresoRpg;
    }

    public void setProgresoRpg(beyProgresoRpg progresoRpg) {
        this.progresoRpg = progresoRpg == null ? new beyProgresoRpg() : progresoRpg;
    }
}
