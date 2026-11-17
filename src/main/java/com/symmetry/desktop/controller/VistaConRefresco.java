package com.symmetry.desktop.controller;

/**
 * Contrato de las vistas que necesitan recargar sus datos cada vez que se muestran.
 */
public interface VistaConRefresco {

    /** Se invoca cada vez que la vista pasa a primer plano. */
    void refrescar();
}
