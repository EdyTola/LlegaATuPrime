package com.symmetry.desktop.service;

import com.symmetry.desktop.model.BaseDatos;

import java.nio.file.Path;

/**
 * Contrato de la capa de persistencia local.
 * La implementacion por defecto guarda todo el estado en un archivo JSON.
 */
public interface PersistenciaService {

    /**
     * Carga el estado completo de la aplicacion.
     *
     * @return los datos guardados, o {@code null} si aun no existe un archivo de datos.
     */
    BaseDatos cargar();

    /** Guarda el estado completo de forma segura (escritura atomica). */
    void guardar(BaseDatos datos);

    /** Ruta del archivo de datos utilizado. */
    Path getArchivoDatos();
}
