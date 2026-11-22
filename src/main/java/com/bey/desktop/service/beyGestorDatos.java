package com.bey.desktop.service;

import com.bey.desktop.model.beyBaseDatos;
import com.bey.desktop.model.beyComida;
import com.bey.desktop.model.beyDiaSalud;
import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyFotoProgreso;
import com.bey.desktop.model.beyMedicionCorporal;
import com.bey.desktop.model.beyModalidadEntrenamiento;
import com.bey.desktop.model.beyPistaAudio;
import com.bey.desktop.model.beyRecipiente;
import com.bey.desktop.model.beyRegistroHidratacion;
import com.bey.desktop.model.beyRutina;
import com.bey.desktop.model.beySesionEntrenamiento;
import com.bey.desktop.model.beyTemaApp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class beyGestorDatos {
    private static beyGestorDatos beyInstancia;

    private beyPersistenciaService beyPersistencia;
    private beyBaseDatos beyDatos;
    private final beyGamificacionService beyGamificacion = new beyGamificacionService(this);

    private beyGestorDatos(beyPersistenciaService persistencia) {
        this.beyPersistencia = persistencia;
    }

    public static synchronized beyGestorDatos beyGetInstancia() {
        if (beyInstancia == null) {
            beyInstancia = new beyGestorDatos(beyPerfilService.beyGetInstancia().beyInicializar());
        }
        return beyInstancia;
    }

    public static synchronized beyGestorDatos beyConPersistencia(beyPersistenciaService persistencia) {
        beyInstancia = new beyGestorDatos(persistencia);
        beyInstancia.beyCargar();
        return beyInstancia;
    }

    public void beyCambiarPersistencia(beyPersistenciaService nuevaPersistencia) {
        if (nuevaPersistencia == null) {
            return;
        }
        if (beyDatos != null) {
            beyPersistencia.beyGuardar(beyDatos);
        }
        this.beyPersistencia = nuevaPersistencia;
        this.beyDatos = null;
        beyCargar();
    }

    public void beyCargar() {
        beyBaseDatos cargada = beyPersistencia.beyCargar();
        if (cargada == null) {
            beyDatos = beyDatosInicialesService.beyCrearBaseDatosInicial();
            beyPersistencia.beyGuardar(beyDatos);
        } else {
            beyDatos = cargada;
        }
    }

    public beyBaseDatos beyGetDatos() {
        if (beyDatos == null) {
            beyCargar();
        }
        return beyDatos;
    }

    public Path beyGetRutaDatos() {
        return beyPersistencia.beyGetArchivoDatos();
    }

    public void beyGuardar() {
        beyPersistencia.beyGuardar(beyGetDatos());
    }

    public void beyGuardarAutomatico() {
        if (beyGetDatos().getConfiguracion().isGuardadoAutomatico()) {
            beyGuardar();
        }
    }

    public Optional<beyRutina> beyRutinaDe(LocalDate fecha) {
        return beyGetDatos().buscarRutinaDe(fecha);
    }

    public List<String> beyAlternativasPara(beyEjercicio ejercicio) {
        if (ejercicio == null) {
            return List.of();
        }
        Set<String> alternativas = new LinkedHashSet<>(ejercicio.getAlternativasPersonalizadas());
        for (beyEjercicio candidato : beyGetDatos().catalogoEjercicios()) {
            boolean mismoGrupo = candidato.getGrupoMuscular() == ejercicio.getGrupoMuscular();
            boolean distinto = !candidato.getNombre().equalsIgnoreCase(ejercicio.getNombre());
            if (mismoGrupo && distinto) {
                alternativas.add(candidato.getNombre());
            }
        }
        return new ArrayList<>(alternativas);
    }

    public void beyGuardarSesion(beySesionEntrenamiento sesion) {
        if (sesion == null) {
            return;
        }
        beyGetDatos().getSesiones().removeIf(s -> s.getId().equals(sesion.getId()));
        beyGetDatos().getSesiones().add(sesion);

        beyGuardar();
        beyGamificacion.beyEvaluar();
    }

    public void beyAgregarRutina(beyRutina rutina) {
        if (rutina == null) {
            return;
        }
        beyGetDatos().getRutinas().add(rutina);
        beyGuardarAutomatico();
    }

    public void beyEliminarRutina(beyRutina rutina) {
        if (rutina == null) {
            return;
        }
        beyGetDatos().getRutinas().removeIf(r -> r.getId().equals(rutina.getId()));
        beyGuardarAutomatico();
    }

    public void beyGuardarCambiosEntrenamiento() {
        beyGuardarAutomatico();
    }

    public beyModalidadEntrenamiento beyGetModalidad() {
        return beyGetDatos().getConfiguracion().getModalidad();
    }

    public void beySetModalidad(beyModalidadEntrenamiento modalidad) {
        beyGetDatos().getConfiguracion().setModalidad(modalidad);
        beyGuardarAutomatico();
    }

    public beyGamificacionService beyGetGamificacion() {
        return beyGamificacion;
    }

    public beyDiaSalud beyDia(LocalDate fecha) {
        return beyGetDatos().obtenerDia(fecha);
    }

    public beyDiaSalud beyHoy() {
        return beyDia(LocalDate.now());
    }

    public void beyRegistrarComida(beyComida comida) {
        if (comida == null) {
            return;
        }
        LocalDate fecha = comida.getFechaHora() == null
                ? LocalDate.now() : comida.getFechaHora().toLocalDate();
        beyDia(fecha).getComidas().add(comida);
        beyGuardarAutomatico();
        beyGamificacion.beyEvaluar();
    }

    public void beyEliminarComida(LocalDate fecha, beyComida comida) {
        beyDia(fecha).getComidas().removeIf(c -> c.getId().equals(comida.getId()));
        beyGuardarAutomatico();
    }

    public void beyRegistrarHidratacion(LocalDate fecha, int mililitros, beyRecipiente recipiente) {
        if (mililitros <= 0) {
            return;
        }
        beyRegistroHidratacion registro = new beyRegistroHidratacion(
                mililitros,
                recipiente == null ? beyRecipiente.PERSONALIZADO : recipiente,
                LocalDateTime.now());
        beyDia(fecha).getHidratacion().add(registro);
        beyGuardarAutomatico();
        beyGamificacion.beyEvaluar();
    }

    public void beyEliminarHidratacion(LocalDate fecha, beyRegistroHidratacion registro) {
        beyDia(fecha).getHidratacion().removeIf(r -> r.getId().equals(registro.getId()));
        beyGuardarAutomatico();
    }

    public void beySetCreatinaTomada(LocalDate fecha, boolean tomada) {
        beyDia(fecha).setCreatinaTomada(tomada);
        beyGuardarAutomatico();
        beyGamificacion.beyEvaluar();
    }

    public void beyRegistrarSueno(LocalDate fecha, double horas) {
        beyDia(fecha).setHorasSueno(horas);
        beyGuardarAutomatico();
        beyGamificacion.beyEvaluar();
    }

    public void beyRegistrarMedicion(beyMedicionCorporal medicion) {
        if (medicion == null || medicion.getFecha() == null) {
            return;
        }
        List<beyMedicionCorporal> mediciones = beyGetDatos().getMediciones();
        beyMedicionCorporal existente = mediciones.stream()
                .filter(m -> medicion.getFecha().equals(m.getFecha()))
                .findFirst()
                .orElse(null);
        if (existente == null) {
            mediciones.add(medicion);
        } else {
            if (medicion.getPesoKg() != null) {
                existente.setPesoKg(medicion.getPesoKg());
            }
            if (medicion.getCinturaCm() != null) {
                existente.setCinturaCm(medicion.getCinturaCm());
            }
            if (medicion.getGrasaPorcentaje() != null) {
                existente.setGrasaPorcentaje(medicion.getGrasaPorcentaje());
            }
            if (medicion.getMasaMuscularKg() != null) {
                existente.setMasaMuscularKg(medicion.getMasaMuscularKg());
            }
        }
        beyGuardarAutomatico();
    }

    public void beyEliminarMedicion(beyMedicionCorporal medicion) {
        if (medicion == null) {
            return;
        }
        beyGetDatos().getMediciones().removeIf(m -> m.getId().equals(medicion.getId()));
        beyGuardarAutomatico();
    }

    public void beyAgregarFoto(beyFotoProgreso foto) {
        if (foto == null) {
            return;
        }
        beyGetDatos().getFotos().add(foto);
        beyGuardarAutomatico();
    }

    public void beyEliminarFoto(beyFotoProgreso foto) {
        if (foto == null) {
            return;
        }
        beyGetDatos().getFotos().removeIf(f -> f.getId().equals(foto.getId()));
        beyGuardarAutomatico();
    }

    public void beyAgregarPistas(List<beyPistaAudio> pistas) {
        if (pistas == null) {
            return;
        }
        for (beyPistaAudio pista : pistas) {
            beyAgregarPista(pista);
        }
    }

    public void beyAgregarPista(beyPistaAudio pista) {
        if (pista == null) {
            return;
        }
        boolean duplicada = beyGetDatos().getBibliotecaMusical().stream()
                .anyMatch(p -> beyMismaPista(p, pista));
        if (!duplicada) {
            beyGetDatos().getBibliotecaMusical().add(pista);
        }
        beyGuardarAutomatico();
    }

    private static boolean beyMismaPista(beyPistaAudio a, beyPistaAudio b) {
        if (a.esEnlace() || b.esEnlace()) {
            return a.getEnlace() != null && a.getEnlace().equalsIgnoreCase(b.getEnlace());
        }
        return a.getRutaArchivo() != null && a.getRutaArchivo().equalsIgnoreCase(b.getRutaArchivo());
    }

    public void beyEliminarPista(beyPistaAudio pista) {
        if (pista == null) {
            return;
        }
        beyGetDatos().getBibliotecaMusical().removeIf(p -> p.getId().equals(pista.getId()));
        beyGuardarAutomatico();
    }

    public void beyLimpiarBiblioteca() {
        beyGetDatos().getBibliotecaMusical().clear();
        beyGuardarAutomatico();
    }

    public void beyReordenarBiblioteca(List<beyPistaAudio> pistas) {
        if (pistas == null) {
            return;
        }
        List<beyPistaAudio> copia = new ArrayList<>(pistas);
        List<beyPistaAudio> biblioteca = beyGetDatos().getBibliotecaMusical();
        biblioteca.clear();
        biblioteca.addAll(copia);
        beyGuardarAutomatico();
    }

    public Path beyGetCarpetaMusica() {
        Path base = beyJsonPersistenciaService.beyRutaPorDefecto().getParent();
        Path carpeta = (base == null ? Path.of(".") : base).resolve("music");
        try {
            Files.createDirectories(carpeta);
        } catch (IOException ignored) {
        }
        return carpeta;
    }

    public void beyRecordarCarpetaMusica(String rutaCarpeta) {
        beyGetDatos().getConfiguracion().setUltimaCarpetaMusica(rutaCarpeta);
        beyGuardarAutomatico();
    }

    public void beyRecordarCarpetaMultimedia(String rutaCarpeta) {
        beyGetDatos().getConfiguracion().setUltimaCarpetaMultimedia(rutaCarpeta);
        beyGuardarAutomatico();
    }

    public beyTemaApp beyGetTema() {
        return beyGetDatos().getConfiguracion().getTema();
    }

    public void beySetTema(beyTemaApp tema) {
        beyGetDatos().getConfiguracion().setTema(tema);
        beyGuardarAutomatico();
    }

    public beyTemaApp beyAlternarTema() {
        beyTemaApp nuevo = beyGetTema().alternado();
        beySetTema(nuevo);
        return nuevo;
    }
}
