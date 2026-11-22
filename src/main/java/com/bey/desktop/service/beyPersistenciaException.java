package com.bey.desktop.service;

public class beyPersistenciaException extends RuntimeException {
    public beyPersistenciaException(String mensaje) {
        super(mensaje);
    }

    public beyPersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
