package com.bey.desktop.controller;

import com.bey.desktop.model.beyFotoProgreso;
import com.bey.desktop.model.beyTipoPose;
import com.bey.desktop.service.beyArchivoMultimediaService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;

public class beyFormularioFotoController {
    @FXML
    private DatePicker beyCampoFecha;
    @FXML
    private ComboBox<beyTipoPose> beyCampoPose;
    @FXML
    private Label beyEtiquetaArchivo;
    @FXML
    private Label beyEtiquetaError;

    private beyFotoProgreso beyFoto;
    private String beyRutaSeleccionada;

    public void beyCargar(beyFotoProgreso foto) {
        this.beyFoto = foto;
        this.beyRutaSeleccionada = foto.getRutaArchivo();
        beyCampoFecha.setValue(foto.getFecha() == null ? LocalDate.now() : foto.getFecha());
        beyCampoPose.setItems(FXCollections.observableArrayList(beyTipoPose.values()));
        beyCampoPose.setValue(foto.getPose());
        beyActualizarEtiquetaArchivo();
    }

    @FXML
    private void beySeleccionarImagen() {
        File archivo = beyArchivoMultimediaService.beySelectorImagen(null).showOpenDialog(beyVentana());
        if (archivo != null) {
            beyRutaSeleccionada = archivo.getAbsolutePath();
            beyActualizarEtiquetaArchivo();
            beyEtiquetaError.setText("");
        }
    }

    public boolean beyValidar() {
        if (beyCampoFecha.getValue() == null) {
            beyEtiquetaError.setText("Selecciona la fecha de la foto.");
            return false;
        }
        if (beyRutaSeleccionada == null || beyRutaSeleccionada.isBlank()) {
            beyEtiquetaError.setText("Selecciona una imagen para la galería.");
            return false;
        }
        if (!beyArchivoMultimediaService.beyEsImagen(beyRutaSeleccionada)) {
            beyEtiquetaError.setText("El archivo debe ser una imagen (.png, .jpg, .jpeg, .gif o .bmp).");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public void beyAplicar() {
        beyFoto.setFecha(beyCampoFecha.getValue());
        beyFoto.setPose(beyCampoPose.getValue());
        beyFoto.setRutaArchivo(beyRutaSeleccionada);
    }

    private void beyActualizarEtiquetaArchivo() {
        beyEtiquetaArchivo.setText(beyRutaSeleccionada == null || beyRutaSeleccionada.isBlank()
                ? "Ningún archivo seleccionado"
                : beyArchivoMultimediaService.beyNombreArchivo(beyRutaSeleccionada));
    }

    private Window beyVentana() {
        return beyCampoFecha.getScene() == null ? null : beyCampoFecha.getScene().getWindow();
    }
}
