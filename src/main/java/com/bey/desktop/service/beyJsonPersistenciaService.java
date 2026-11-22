package com.bey.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bey.desktop.model.beyBaseDatos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class beyJsonPersistenciaService implements beyPersistenciaService {
    private static final String NOMBRE_ARCHIVO = "bey-datos.json";
    private static final String CARPETA_DATOS = ".bey-desktop";
    private static final String NOMBRE_ANTERIOR = "symmetry-datos.json";
    private static final String CARPETA_ANTERIOR = ".symmetry-desktop";

    private final Path beyArchivoDatos;
    private final ObjectMapper beyMapper;

    public beyJsonPersistenciaService() {
        this(beyRutaPorDefecto());
    }

    public beyJsonPersistenciaService(Path archivoDatos) {
        this.beyArchivoDatos = archivoDatos;
        this.beyMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static Path beyRutaPorDefecto() {
        Path inicio = Path.of(System.getProperty("user.home"));
        Path carpeta = inicio.resolve(CARPETA_DATOS);
        Path anterior = inicio.resolve(CARPETA_ANTERIOR);
        if (!Files.exists(carpeta) && Files.isDirectory(anterior)) {
            try {
                Files.move(anterior, carpeta);
                Path archivoAnterior = carpeta.resolve(NOMBRE_ANTERIOR);
                if (Files.exists(archivoAnterior)) {
                    Files.move(archivoAnterior, carpeta.resolve(NOMBRE_ARCHIVO));
                }
            } catch (IOException ignorada) {
            }
        }
        return carpeta.resolve(NOMBRE_ARCHIVO);
    }

    @Override
    public beyBaseDatos beyCargar() {
        if (!Files.exists(beyArchivoDatos)) {
            return null;
        }
        try (InputStream entrada = Files.newInputStream(beyArchivoDatos)) {
            return beyMapper.readValue(entrada, beyBaseDatos.class);
        } catch (IOException e) {
            beyRespaldarArchivoCorrupto();
            return null;
        }
    }

    @Override
    public void beyGuardar(beyBaseDatos datos) {
        if (datos == null) {
            return;
        }
        try {
            if (beyArchivoDatos.getParent() != null) {
                Files.createDirectories(beyArchivoDatos.getParent());
            }
            Path temporal = beyArchivoDatos.resolveSibling(beyArchivoDatos.getFileName() + ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                beyMapper.writeValue(salida, datos);
            }
            Files.move(temporal, beyArchivoDatos, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new beyPersistenciaException("No se pudieron guardar los datos en " + beyArchivoDatos, e);
        }
    }

    @Override
    public Path beyGetArchivoDatos() {
        return beyArchivoDatos;
    }

    private void beyRespaldarArchivoCorrupto() {
        try {
            String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path respaldo = beyArchivoDatos.resolveSibling(beyArchivoDatos.getFileName() + ".corrupto-" + marca);
            Files.move(beyArchivoDatos, respaldo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
