package com.bey.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bey.desktop.model.beyBaseDatos;
import com.bey.desktop.model.beyPerfilUsuario;
import com.bey.desktop.model.beyRegistroPerfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class beyPerfilService {
    private static final String NOMBRE_INDICE = "perfiles.json";
    private static final String CARPETA_PERFILES = "perfiles";
    private static final String SUFIJO_ARCHIVO = ".json";

    private static beyPerfilService beyInstancia;

    private final Path beyCarpetaDatos;
    private final Path beyArchivoIndice;
    private final Path beyCarpetaPerfiles;
    private final Path beyArchivoLegacy;
    private final ObjectMapper beyMapper;

    private beyRegistroPerfiles beyRegistro;

    private beyPerfilService() {
        this.beyCarpetaDatos = beyJsonPersistenciaService.beyRutaPorDefecto().getParent();
        this.beyArchivoIndice = beyCarpetaDatos.resolve(NOMBRE_INDICE);
        this.beyCarpetaPerfiles = beyCarpetaDatos.resolve(CARPETA_PERFILES);
        this.beyArchivoLegacy = beyJsonPersistenciaService.beyRutaPorDefecto();
        this.beyMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static synchronized beyPerfilService beyGetInstancia() {
        if (beyInstancia == null) {
            beyInstancia = new beyPerfilService();
        }
        return beyInstancia;
    }

    public beyPersistenciaService beyInicializar() {
        beyRegistro = beyCargarOCrearRegistro();
        return beyPersistenciaDe(beyRegistro.getPerfilActivoId());
    }

    public beyRegistroPerfiles beyGetRegistro() {
        if (beyRegistro == null) {
            beyRegistro = beyCargarOCrearRegistro();
        }
        return beyRegistro;
    }

    public List<beyPerfilUsuario> beyListarPerfiles() {
        return beyGetRegistro().getPerfiles();
    }

    public beyPerfilUsuario beyGetPerfilActivo() {
        return beyGetRegistro().getPerfilActivo();
    }

    public Optional<beyPerfilUsuario> beyBuscarPerfil(String id) {
        return beyGetRegistro().buscar(id);
    }

    public Path beyRutaDatosDe(String id) {
        return beyCarpetaPerfiles.resolve(id + SUFIJO_ARCHIVO);
    }

    public void beyActivar(String id) {
        if (id == null || beyBuscarPerfil(id).isEmpty()) {
            return;
        }
        if (id.equals(beyRegistro.getPerfilActivoId())) {
            return;
        }
        beyRegistro.setPerfilActivoId(id);
        beyGuardarRegistro();
        beyGestorDatos.beyGetInstancia().beyCambiarPersistencia(beyPersistenciaDe(id));
    }

    public beyPerfilUsuario beyCrearPerfil(String nombre, Integer edad, String objetivoGeneral) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre del perfil es obligatorio.");
        }
        if (beyGetRegistro().existeNombre(limpio)) {
            throw new IllegalArgumentException("Ya existe un perfil llamado \"" + limpio + "\".");
        }
        beyPerfilUsuario perfil = new beyPerfilUsuario(limpio);
        perfil.setEdad(edad);
        perfil.setObjetivoGeneral(objetivoGeneral == null || objetivoGeneral.isBlank()
                ? null : objetivoGeneral.trim());

        beyBaseDatos base = beyDatosInicialesService.beyCrearBaseDatosInicial();
        perfil.setObjetivos(base.getPerfil().getObjetivos());
        perfil.setFechaRegistro(LocalDate.now());
        base.setPerfil(perfil);
        new beyJsonPersistenciaService(beyRutaDatosDe(perfil.getId())).beyGuardar(base);

        beyGetRegistro().registrar(perfil);
        beyGuardarRegistro();
        return perfil;
    }

    public void beySincronizarIndice(beyPerfilUsuario perfil) {
        if (perfil == null) {
            return;
        }
        beyGetRegistro().registrar(perfil);
        beyGuardarRegistro();
    }

    public void beyEliminarPerfil(String id, boolean borrarDatos) {
        if (id == null || beyGetRegistro().getTotal() <= 1) {
            return;
        }
        boolean quitado = beyGetRegistro().quitar(id);
        if (!quitado) {
            return;
        }
        if (borrarDatos) {
            try {
                Files.deleteIfExists(beyRutaDatosDe(id));
            } catch (IOException ignored) {
            }
        }
        beyGuardarRegistro();
    }

    private beyPersistenciaService beyPersistenciaDe(String id) {
        return new beyJsonPersistenciaService(beyRutaDatosDe(id));
    }

    private beyRegistroPerfiles beyCargarOCrearRegistro() {
        beyRegistroPerfiles leido = beyLeerIndice();
        if (leido != null && !leido.getPerfiles().isEmpty()) {
            return leido;
        }

        if (Files.exists(beyArchivoLegacy)) {
            beyRegistroPerfiles migrado = beyMigrarDesdeArchivoUnico();
            if (migrado != null) {
                return migrado;
            }
        }

        beyPerfilUsuario perfil = beyCrearPerfilEnDisco(new beyPerfilUsuario("Yoel Fernando"));
        beyRegistroPerfiles nuevo = new beyRegistroPerfiles();
        nuevo.registrar(perfil);
        beyGuardarRegistroInterno(nuevo);
        return nuevo;
    }

    private beyRegistroPerfiles beyMigrarDesdeArchivoUnico() {
        beyBaseDatos legacy = beyLeerBaseDatos(beyArchivoLegacy);
        if (legacy == null) {
            return null;
        }
        beyPerfilUsuario perfil = legacy.getPerfil();
        perfil.setId(java.util.UUID.randomUUID().toString());
        legacy.setPerfil(perfil);
        new beyJsonPersistenciaService(beyRutaDatosDe(perfil.getId())).beyGuardar(legacy);
        try {
            Files.deleteIfExists(beyArchivoLegacy);
        } catch (IOException ignored) {
        }
        beyRegistroPerfiles migrado = new beyRegistroPerfiles();
        migrado.registrar(perfil);
        beyGuardarRegistroInterno(migrado);
        return migrado;
    }

    private beyPerfilUsuario beyCrearPerfilEnDisco(beyPerfilUsuario perfil) {
        beyBaseDatos base = beyDatosInicialesService.beyCrearBaseDatosInicial();
        perfil.setObjetivos(base.getPerfil().getObjetivos());
        base.setPerfil(perfil);
        new beyJsonPersistenciaService(beyRutaDatosDe(perfil.getId())).beyGuardar(base);
        return perfil;
    }

    private beyRegistroPerfiles beyLeerIndice() {
        if (!Files.exists(beyArchivoIndice)) {
            return null;
        }
        try (InputStream entrada = Files.newInputStream(beyArchivoIndice)) {
            return beyMapper.readValue(entrada, beyRegistroPerfiles.class);
        } catch (IOException e) {
            beyRespaldarArchivo(beyArchivoIndice, ".corrupto-");
            return null;
        }
    }

    private beyBaseDatos beyLeerBaseDatos(Path archivo) {
        try (InputStream entrada = Files.newInputStream(archivo)) {
            return beyMapper.readValue(entrada, beyBaseDatos.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void beyGuardarRegistro() {
        beyGuardarRegistroInterno(beyGetRegistro());
    }

    private void beyGuardarRegistroInterno(beyRegistroPerfiles destino) {
        if (destino == null) {
            return;
        }
        try {
            Files.createDirectories(beyCarpetaDatos);
            Path temporal = beyArchivoIndice.resolveSibling(beyArchivoIndice.getFileName() + ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                beyMapper.writeValue(salida, destino);
            }
            Files.move(temporal, beyArchivoIndice, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new beyPersistenciaException("No se pudo guardar el indice de perfiles en " + beyArchivoIndice, e);
        }
    }

    private void beyRespaldarArchivo(Path archivo, String marca) {
        try {
            Path respaldo = archivo.resolveSibling(archivo.getFileName() + marca
                    + LocalDate.now());
            Files.move(archivo, respaldo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
