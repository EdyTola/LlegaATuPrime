package com.symmetry.desktop.controller;

import com.symmetry.desktop.util.MarcaApp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de bienvenida: dibuja el isotipo oficial a gran tamano con un
 * resplandor azul suave para el primer contacto con la aplicacion.
 */
public class SplashController implements Initializable {

    /** Tamano (px) del isotipo en la pantalla de bienvenida. */
    private static final double LADO_ISOTIPO = 150;

    @FXML
    private StackPane marcaSplash;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        marcaSplash.getChildren().setAll(MarcaApp.isotipo(LADO_ISOTIPO, 34));
    }
}
