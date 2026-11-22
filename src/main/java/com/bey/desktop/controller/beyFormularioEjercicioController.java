package com.bey.desktop.controller;

import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.io.File;

public class beyFormularioEjercicioController {
    @FXML
    private TextField beyCampoNombre;
    @FXML
    private ComboBox<beyGrupoMuscular> beyComboGrupo;
    @FXML
    private Spinner<Integer> beySpinnerSeries;
    @FXML
    private Spinner<Integer> beySpinnerRepsMin;
    @FXML
    private Spinner<Integer> beySpinnerRepsMax;
    @FXML
    private Spinner<Integer> beySpinnerRir;
    @FXML
    private Spinner<Integer> beySpinnerDescanso;
    @FXML
    private CheckBox beyCheckPorTiempo;
    @FXML
    private Label beyEtiquetaMultimedia;
    @FXML
    private TextField beyCampoEnlace;
    @FXML
    private Label beyEtiquetaError;
    @FXML
    private Button beyBotonQuitarMultimedia;
    @FXML
    private TextArea beyCampoNotas;

    private beyEjercicio beyEjercicio;
    private String beyRutaMultimedia;

    @FXML
    private void initialize() {
        beyComboGrupo.setItems(FXCollections.observableArrayList(beyGrupoMuscular.values()));
        beySpinnerSeries.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 3));
        beySpinnerRepsMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 8));
        beySpinnerRepsMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 12));
        beySpinnerRir.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 2));
        beySpinnerDescanso.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 180, 90, 5));
    }

    public void beyCargar(beyEjercicio ejercicio) {
        this.beyEjercicio = ejercicio;
        this.beyRutaMultimedia = ejercicio.getRutaMultimedia();

        beyCampoNombre.setText(ejercicio.getNombre());
        beyComboGrupo.setValue(ejercicio.getGrupoMuscular());
        beySpinnerSeries.getValueFactory().setValue(ejercicio.getSeries());
        beySpinnerRepsMin.getValueFactory().setValue(ejercicio.getRepeticionesMin());
        beySpinnerRepsMax.getValueFactory().setValue(ejercicio.getRepeticionesMax());
        beySpinnerRir.getValueFactory().setValue(ejercicio.getRirObjetivo());
        beySpinnerDescanso.getValueFactory().setValue(ejercicio.getDescansoSegundos());
        beyCheckPorTiempo.setSelected(ejercicio.isPorTiempo());
        beyCampoNotas.setText(ejercicio.getNotas() == null ? "" : ejercicio.getNotas());
        beyCampoEnlace.setText(ejercicio.getEnlaceMultimedia() == null ? "" : ejercicio.getEnlaceMultimedia());
        beyActualizarEtiquetaMultimedia();
    }

    public boolean beyValidar() {
        if (beyCampoNombre.getText() == null || beyCampoNombre.getText().isBlank()) {
            beyEtiquetaError.setText("El nombre del ejercicio es obligatorio.");
            return false;
        }
        if (beyComboGrupo.getValue() == null) {
            beyEtiquetaError.setText("Selecciona el grupo muscular.");
            return false;
        }
        if (beySpinnerRepsMax.getValue() < beySpinnerRepsMin.getValue()) {
            beyEtiquetaError.setText("Las repeticiones máximas no pueden ser menores que las mínimas.");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public void beyAplicar() {
        beyEjercicio.setNombre(beyCampoNombre.getText().trim());
        beyEjercicio.setGrupoMuscular(beyComboGrupo.getValue());
        beyEjercicio.setSeries(beySpinnerSeries.getValue());
        beyEjercicio.setRepeticionesMin(beySpinnerRepsMin.getValue());
        beyEjercicio.setRepeticionesMax(beySpinnerRepsMax.getValue());
        beyEjercicio.setRirObjetivo(beySpinnerRir.getValue());
        beyEjercicio.setDescansoSegundos(beySpinnerDescanso.getValue());
        beyEjercicio.setPorTiempo(beyCheckPorTiempo.isSelected());
        beyEjercicio.setRutaMultimedia(beyRutaMultimedia);
        beyEjercicio.setEnlaceMultimedia(beyCampoEnlace.getText() == null || beyCampoEnlace.getText().isBlank()
                ? null : beyCampoEnlace.getText().trim());
        beyEjercicio.setNotas(beyCampoNotas.getText() == null || beyCampoNotas.getText().isBlank()
                ? null : beyCampoNotas.getText().trim());
    }

    @FXML
    private void beySeleccionarMultimedia() {
        File archivo = beyArchivoMultimediaService
                .beySelectorMultimedia(beyGestorDatos.beyGetInstancia().beyGetDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(beyVentana());
        if (archivo != null) {
            beyRutaMultimedia = archivo.getAbsolutePath();
            beyGestorDatos.beyGetInstancia().beyRecordarCarpetaMultimedia(archivo.getParent());
            beyActualizarEtiquetaMultimedia();
        }
    }

    @FXML
    private void beyQuitarMultimedia() {
        beyRutaMultimedia = null;
        beyActualizarEtiquetaMultimedia();
    }

    private void beyActualizarEtiquetaMultimedia() {
        boolean existe = beyArchivoMultimediaService.beyExiste(beyRutaMultimedia);
        beyEtiquetaMultimedia.setText(existe
                ? beyArchivoMultimediaService.beyNombreArchivo(beyRutaMultimedia)
                : "Sin archivo (.png, .jpg, .gif, .mp4)");
        beyBotonQuitarMultimedia.setDisable(!existe);
    }

    private Window beyVentana() {
        return beyEtiquetaMultimedia.getScene() == null ? null : beyEtiquetaMultimedia.getScene().getWindow();
    }
}
