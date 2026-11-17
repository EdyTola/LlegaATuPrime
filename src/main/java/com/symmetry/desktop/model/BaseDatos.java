package com.symmetry.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Raiz de la persistencia local: agrupa todo el estado de Llega a tu Prime
 * y se serializa como un unico archivo JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseDatos {

    /** Version del formato de datos, para futuras migraciones. */
    private int version = 2;

    private PerfilUsuario perfil = new PerfilUsuario();
    private ConfiguracionApp configuracion = new ConfiguracionApp();
    private List<Rutina> rutinas = new ArrayList<>();
    private List<SesionEntrenamiento> sesiones = new ArrayList<>();
    private List<DiaSalud> dias = new ArrayList<>();
    private List<MedicionCorporal> mediciones = new ArrayList<>();
    private List<FotoProgreso> fotos = new ArrayList<>();
    private List<PistaAudio> bibliotecaMusical = new ArrayList<>();

    public BaseDatos() {
    }

    /** Devuelve el dia de salud de la fecha indicada, creandolo si no existe. */
    public DiaSalud obtenerDia(LocalDate fecha) {
        return buscarDia(fecha).orElseGet(() -> {
            DiaSalud nuevo = new DiaSalud(fecha);
            dias.add(nuevo);
            return nuevo;
        });
    }

    public Optional<DiaSalud> buscarDia(LocalDate fecha) {
        return dias.stream().filter(d -> fecha.equals(d.getFecha())).findFirst();
    }

    /** Rutina programada para la fecha indicada, si existe. */
    public Optional<Rutina> buscarRutinaDe(LocalDate fecha) {
        return rutinaPorDia(DiaSemana.desdeFecha(fecha));
    }

    public Optional<Rutina> rutinaPorDia(DiaSemana dia) {
        return rutinas.stream().filter(r -> r.getDiaSemana() == dia).findFirst();
    }

    public Optional<Rutina> buscarRutinaPorId(String id) {
        return rutinas.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    /** Todas las mediciones corporales ordenadas cronologicamente. */
    public List<MedicionCorporal> medicionesOrdenadas() {
        List<MedicionCorporal> copia = new ArrayList<>(mediciones);
        copia.sort(Comparator.comparing(MedicionCorporal::getFecha));
        return copia;
    }

    /** Sesiones de entrenamiento ordenadas cronologicamente. */
    public List<SesionEntrenamiento> sesionesOrdenadas() {
        List<SesionEntrenamiento> copia = new ArrayList<>(sesiones);
        copia.sort(Comparator.comparing(SesionEntrenamiento::getFechaInicio));
        return copia;
    }

    /** Fotos de progreso ordenadas de la mas reciente a la mas antigua. */
    public List<FotoProgreso> fotosOrdenadas() {
        List<FotoProgreso> copia = new ArrayList<>(fotos);
        copia.sort(Comparator.comparing(FotoProgreso::getFecha).reversed());
        return copia;
    }

    /** Catalogo de ejercicios sin repetir, tomado de todas las rutinas. */
    public List<Ejercicio> catalogoEjercicios() {
        List<Ejercicio> catalogo = new ArrayList<>();
        for (Rutina rutina : rutinas) {
            for (Ejercicio ejercicio : rutina.getEjercicios()) {
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

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil == null ? new PerfilUsuario() : perfil;
    }

    public ConfiguracionApp getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionApp configuracion) {
        this.configuracion = configuracion == null ? new ConfiguracionApp() : configuracion;
    }

    public List<Rutina> getRutinas() {
        return rutinas;
    }

    public void setRutinas(List<Rutina> rutinas) {
        this.rutinas = rutinas == null ? new ArrayList<>() : rutinas;
    }

    public List<SesionEntrenamiento> getSesiones() {
        return sesiones;
    }

    public void setSesiones(List<SesionEntrenamiento> sesiones) {
        this.sesiones = sesiones == null ? new ArrayList<>() : sesiones;
    }

    public List<DiaSalud> getDias() {
        return dias;
    }

    public void setDias(List<DiaSalud> dias) {
        this.dias = dias == null ? new ArrayList<>() : dias;
    }

    public List<MedicionCorporal> getMediciones() {
        return mediciones;
    }

    public void setMediciones(List<MedicionCorporal> mediciones) {
        this.mediciones = mediciones == null ? new ArrayList<>() : mediciones;
    }

    public List<FotoProgreso> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoProgreso> fotos) {
        this.fotos = fotos == null ? new ArrayList<>() : fotos;
    }

    public List<PistaAudio> getBibliotecaMusical() {
        return bibliotecaMusical;
    }

    public void setBibliotecaMusical(List<PistaAudio> bibliotecaMusical) {
        this.bibliotecaMusical = bibliotecaMusical == null ? new ArrayList<>() : bibliotecaMusical;
    }
}
