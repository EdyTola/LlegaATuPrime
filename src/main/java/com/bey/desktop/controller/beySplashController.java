package com.bey.desktop.controller;

import com.bey.desktop.util.beyMarcaApp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class beySplashController implements Initializable {
    private static final double LADO_ISOTIPO = 150;

    @FXML
    private StackPane beyMarcaSplash;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyMarcaSplash.getChildren().setAll(beyMarcaApp.beyIsotipo(LADO_ISOTIPO, 34));
    }
}
