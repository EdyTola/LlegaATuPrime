package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.FotoProgreso;
import com.symmetry.desktop.model.TipoPose;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;

/**
 * Formulario modal para adjuntar una foto de progreso a la galeria de transformacion.
 * Requiere una fecha, un tipo de pose y un archivo de imagen local.
 */
public class FormularioFotoController {

    @FXML
    private DatePicker campoFecha;
    @FXML
    private ComboBox<TipoPose> campoPose;
    @FXML
    private Label etiquetaArchivo;
    @FXML
    private Label etiquetaError;

    private FotoProgreso foto;
    private String rutaSeleccionada;

    /** Rellena el formulario con los datos de la foto a registrar. */
    public void cargar(FotoProgreso foto) {
        this.foto = foto;
        this.rutaSeleccionada = foto.getRutaArchivo();
        campoFecha.setValue(foto.getFecha() == null ? LocalDate.now() : foto.getFecha());
        campoPose.setItems(FXCollections.observableArrayList(TipoPose.values()));
        campoPose.setValue(foto.getPose());
        actualizarEtiquetaArchivo();
    }

    @FXML
    private void seleccionarImagen() {
        File archivo = ArchivoMultimediaService.selectorImagen(null).showOpenDialog(ventana());
        if (archivo != null) {
            rutaSeleccionada = archivo.getAbsolutePath();
            actualizarEtiquetaArchivo();
            etiquetaError.setText("");
        }
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        if (campoFecha.getValue() == null) {
            etiquetaError.setText("Selecciona la fecha de la foto.");
            return false;
        }
        if (rutaSeleccionada == null || rutaSeleccionada.isBlank()) {
            etiquetaError.setText("Selecciona una imagen para la galería.");
            return false;
        }
        if (!ArchivoMultimediaService.esImagen(rutaSeleccionada)) {
            etiquetaError.setText("El archivo debe ser una imagen (.png, .jpg, .jpeg, .gif o .bmp).");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    /** Vuelca los valores del formulario sobre la foto. */
    public void aplicar() {
        foto.setFecha(campoFecha.getValue());
        foto.setPose(campoPose.getValue());
        foto.setRutaArchivo(rutaSeleccionada);
    }

    private void actualizarEtiquetaArchivo() {
        etiquetaArchivo.setText(rutaSeleccionada == null || rutaSeleccionada.isBlank()
                ? "Ningún archivo seleccionado"
                : ArchivoMultimediaService.nombreArchivo(rutaSeleccionada));
    }

    private Window ventana() {
        return campoFecha.getScene() == null ? null : campoFecha.getScene().getWindow();
    }
}
