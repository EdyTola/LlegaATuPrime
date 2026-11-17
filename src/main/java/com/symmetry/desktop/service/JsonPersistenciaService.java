package com.symmetry.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.symmetry.desktop.model.BaseDatos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persistencia local basada en un unico archivo JSON (Jackson), sin dependencias de red.
 * La escritura es atomica: primero se genera un archivo temporal y despues se reemplaza.
 */
public class JsonPersistenciaService implements PersistenciaService {

    private static final String NOMBRE_ARCHIVO = "symmetry-datos.json";
    private static final String CARPETA_DATOS = ".symmetry-desktop";

    private final Path archivoDatos;
    private final ObjectMapper mapper;

    public JsonPersistenciaService() {
        this(rutaPorDefecto());
    }

    public JsonPersistenciaService(Path archivoDatos) {
        this.archivoDatos = archivoDatos;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Ubicacion por defecto: carpeta del usuario, para no depender del directorio de trabajo. */
    public static Path rutaPorDefecto() {
        return Path.of(System.getProperty("user.home"), CARPETA_DATOS, NOMBRE_ARCHIVO);
    }

    @Override
    public BaseDatos cargar() {
        if (!Files.exists(archivoDatos)) {
            return null;
        }
        try (InputStream entrada = Files.newInputStream(archivoDatos)) {
            return mapper.readValue(entrada, BaseDatos.class);
        } catch (IOException e) {
            respaldarArchivoCorrupto();
            return null;
        }
    }

    @Override
    public void guardar(BaseDatos datos) {
        if (datos == null) {
            return;
        }
        try {
            if (archivoDatos.getParent() != null) {
                Files.createDirectories(archivoDatos.getParent());
            }
            Path temporal = archivoDatos.resolveSibling(archivoDatos.getFileName() + ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                mapper.writeValue(salida, datos);
            }
            Files.move(temporal, archivoDatos, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudieron guardar los datos en " + archivoDatos, e);
        }
    }

    @Override
    public Path getArchivoDatos() {
        return archivoDatos;
    }

    /** Conserva una copia del archivo ilegible para no perder informacion del usuario. */
    private void respaldarArchivoCorrupto() {
        try {
            String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path respaldo = archivoDatos.resolveSibling(archivoDatos.getFileName() + ".corrupto-" + marca);
            Files.move(archivoDatos, respaldo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Si el respaldo falla, se continua con una base de datos nueva.
        }
    }
}
