package com.symmetry.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.symmetry.desktop.model.BaseDatos;
import com.symmetry.desktop.model.PerfilUsuario;
import com.symmetry.desktop.model.RegistroPerfiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestion de multiples perfiles locales dentro de la misma instalacion.
 *
 * <p>Cada usuario dispone de su propio archivo JSON de datos, de modo que sus rutinas,
 * progresos y metricas quedan totalmente aislados. Esta clase mantiene el indice de
 * perfiles ({@code perfiles.json}), resuelve la ruta de datos de cada uno, crea/elimina
 * perfiles y realiza el cambio de perfil activo en caliente sobre {@link GestorDatos}.</p>
 */
public class PerfilService {

    private static final String NOMBRE_INDICE = "perfiles.json";
    private static final String CARPETA_PERFILES = "perfiles";
    private static final String SUFIJO_ARCHIVO = ".json";

    private static PerfilService instancia;

    private final Path carpetaDatos;
    private final Path archivoIndice;
    private final Path carpetaPerfiles;
    private final Path archivoLegacy;
    private final ObjectMapper mapper;

    private RegistroPerfiles registro;

    private PerfilService() {
        this.carpetaDatos = JsonPersistenciaService.rutaPorDefecto().getParent();
        this.archivoIndice = carpetaDatos.resolve(NOMBRE_INDICE);
        this.carpetaPerfiles = carpetaDatos.resolve(CARPETA_PERFILES);
        this.archivoLegacy = JsonPersistenciaService.rutaPorDefecto();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static synchronized PerfilService getInstancia() {
        if (instancia == null) {
            instancia = new PerfilService();
        }
        return instancia;
    }

    /**
     * Prepara el indice de perfiles (creandolo o migrando la instalacion de un unico
     * usuario si fuera necesario) y devuelve la persistencia del perfil activo.
     */
    public PersistenciaService inicializar() {
        registro = cargarOCrearRegistro();
        return persistenciaDe(registro.getPerfilActivoId());
    }

    public RegistroPerfiles getRegistro() {
        if (registro == null) {
            registro = cargarOCrearRegistro();
        }
        return registro;
    }

    /** Copia ordenada de los perfiles disponibles. */
    public List<PerfilUsuario> listarPerfiles() {
        return getRegistro().getPerfiles();
    }

    /** Perfil actualmente activo, o {@code null} si todavia no hay ninguno. */
    public PerfilUsuario getPerfilActivo() {
        return getRegistro().getPerfilActivo();
    }

    public Optional<PerfilUsuario> buscarPerfil(String id) {
        return getRegistro().buscar(id);
    }

    /** Ruta del archivo JSON de datos de un perfil. */
    public Path rutaDatosDe(String id) {
        return carpetaPerfiles.resolve(id + SUFIJO_ARCHIVO);
    }

    // ------------------------------------------------------------------
    // Cambio de perfil
    // ------------------------------------------------------------------

    /** Activa otro perfil: persiste el actual, cambia el archivo de datos y recarga el estado. */
    public void activar(String id) {
        if (id == null || buscarPerfil(id).isEmpty()) {
            return;
        }
        if (id.equals(registro.getPerfilActivoId())) {
            return;
        }
        registro.setPerfilActivoId(id);
        guardarRegistro();
        GestorDatos.getInstancia().cambiarPersistencia(persistenciaDe(id));
    }

    // ------------------------------------------------------------------
    // Alta, edicion y baja de perfiles
    // ------------------------------------------------------------------

    /** Crea un perfil nuevo con su propio archivo de datos (rutinas iniciales y metas por defecto). */
    public PerfilUsuario crearPerfil(String nombre, Integer edad, String objetivoGeneral) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre del perfil es obligatorio.");
        }
        if (getRegistro().existeNombre(limpio)) {
            throw new IllegalArgumentException("Ya existe un perfil llamado \"" + limpio + "\".");
        }
        PerfilUsuario perfil = new PerfilUsuario(limpio);
        perfil.setEdad(edad);
        perfil.setObjetivoGeneral(objetivoGeneral == null || objetivoGeneral.isBlank()
                ? null : objetivoGeneral.trim());

        // Semilla de datos: rutinas precargadas y metas nutricionales por defecto.
        BaseDatos base = DatosInicialesService.crearBaseDatosInicial();
        perfil.setObjetivos(base.getPerfil().getObjetivos());
        perfil.setFechaRegistro(LocalDate.now());
        base.setPerfil(perfil);
        new JsonPersistenciaService(rutaDatosDe(perfil.getId())).guardar(base);

        getRegistro().registrar(perfil);
        guardarRegistro();
        return perfil;
    }

    /** Actualiza la ficha del perfil dentro del indice (nombre y foto del avatar). */
    public void sincronizarIndice(PerfilUsuario perfil) {
        if (perfil == null) {
            return;
        }
        getRegistro().registrar(perfil);
        guardarRegistro();
    }

    /** Elimina un perfil y, opcionalmente, su archivo de datos. */
    public void eliminarPerfil(String id, boolean borrarDatos) {
        if (id == null || getRegistro().getTotal() <= 1) {
            return;
        }
        boolean quitado = getRegistro().quitar(id);
        if (!quitado) {
            return;
        }
        if (borrarDatos) {
            try {
                Files.deleteIfExists(rutaDatosDe(id));
            } catch (IOException ignored) {
                // Si el archivo no se puede borrar, el perfil ya no aparece en el indice.
            }
        }
        guardarRegistro();
    }

    // ------------------------------------------------------------------
    // Lectura/escritura del indice y migracion
    // ------------------------------------------------------------------

    private PersistenciaService persistenciaDe(String id) {
        return new JsonPersistenciaService(rutaDatosDe(id));
    }

    /** Carga el indice existente o lo reconstruye migrando la instalacion de un solo usuario. */
    private RegistroPerfiles cargarOCrearRegistro() {
        RegistroPerfiles leido = leerIndice();
        if (leido != null && !leido.getPerfiles().isEmpty()) {
            return leido;
        }
        // Migracion: la instalacion antigua guardaba todo en un unico archivo.
        if (Files.exists(archivoLegacy)) {
            RegistroPerfiles migrado = migrarDesdeArchivoUnico();
            if (migrado != null) {
                return migrado;
            }
        }
        // Primer arranque: se crea el perfil por defecto.
        PerfilUsuario perfil = crearPerfilEnDisco(new PerfilUsuario("Yoel Fernando"));
        RegistroPerfiles nuevo = new RegistroPerfiles();
        nuevo.registrar(perfil);
        guardarRegistroInterno(nuevo);
        return nuevo;
    }

    /**
     * Convierte el archivo unico antiguo ({@code symmetry-datos.json}) en el primer perfil
     * multiusuario, garantizando que su identificador quede persistido.
     */
    private RegistroPerfiles migrarDesdeArchivoUnico() {
        BaseDatos legacy = leerBaseDatos(archivoLegacy);
        if (legacy == null) {
            return null;
        }
        PerfilUsuario perfil = legacy.getPerfil();
        perfil.setId(java.util.UUID.randomUUID().toString());
        legacy.setPerfil(perfil);
        new JsonPersistenciaService(rutaDatosDe(perfil.getId())).guardar(legacy);
        try {
            Files.deleteIfExists(archivoLegacy);
        } catch (IOException ignored) {
            // El archivo migrado se conserva si no puede eliminarse.
        }
        RegistroPerfiles migrado = new RegistroPerfiles();
        migrado.registrar(perfil);
        guardarRegistroInterno(migrado);
        return migrado;
    }

    /** Crea en disco la base de datos inicial del perfil indicado, respetando su identificador. */
    private PerfilUsuario crearPerfilEnDisco(PerfilUsuario perfil) {
        BaseDatos base = DatosInicialesService.crearBaseDatosInicial();
        perfil.setObjetivos(base.getPerfil().getObjetivos());
        base.setPerfil(perfil);
        new JsonPersistenciaService(rutaDatosDe(perfil.getId())).guardar(base);
        return perfil;
    }

    private RegistroPerfiles leerIndice() {
        if (!Files.exists(archivoIndice)) {
            return null;
        }
        try (InputStream entrada = Files.newInputStream(archivoIndice)) {
            return mapper.readValue(entrada, RegistroPerfiles.class);
        } catch (IOException e) {
            respaldarArchivo(archivoIndice, ".corrupto-");
            return null;
        }
    }

    private BaseDatos leerBaseDatos(Path archivo) {
        try (InputStream entrada = Files.newInputStream(archivo)) {
            return mapper.readValue(entrada, BaseDatos.class);
        } catch (IOException e) {
            return null;
        }
    }

    /** Persiste el indice de perfiles de forma atomica. */
    public void guardarRegistro() {
        guardarRegistroInterno(getRegistro());
    }

    private void guardarRegistroInterno(RegistroPerfiles destino) {
        if (destino == null) {
            return;
        }
        try {
            Files.createDirectories(carpetaDatos);
            Path temporal = archivoIndice.resolveSibling(archivoIndice.getFileName() + ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                mapper.writeValue(salida, destino);
            }
            Files.move(temporal, archivoIndice, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo guardar el indice de perfiles en " + archivoIndice, e);
        }
    }

    private void respaldarArchivo(Path archivo, String marca) {
        try {
            Path respaldo = archivo.resolveSibling(archivo.getFileName() + marca
                    + LocalDate.now());
            Files.move(archivo, respaldo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Si el respaldo falla se continua regenerando el indice.
        }
    }
}
