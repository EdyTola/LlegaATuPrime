package com.bey.desktop.service;

import com.bey.desktop.model.beyBaseDatos;

import java.nio.file.Path;

public interface beyPersistenciaService {
    beyBaseDatos beyCargar();

    void beyGuardar(beyBaseDatos datos);

    Path beyGetArchivoDatos();
}
