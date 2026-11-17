package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.GrupoMuscular;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
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

/**
 * Formulario modal para crear o editar un ejercicio: prescripcion (series, repeticiones, RIR,
 * descanso) y archivo multimedia de tecnica.
 */
public class FormularioEjercicioController {

    @FXML
    private TextField campoNombre;
    @FXML
    private ComboBox<GrupoMuscular> comboGrupo;
    @FXML
    private Spinner<Integer> spinnerSeries;
    @FXML
    private Spinner<Integer> spinnerRepsMin;
    @FXML
    private Spinner<Integer> spinnerRepsMax;
    @FXML
    private Spinner<Integer> spinnerRir;
    @FXML
    private Spinner<Integer> spinnerDescanso;
    @FXML
    private CheckBox checkPorTiempo;
    @FXML
    private Label etiquetaMultimedia;
    @FXML
    private TextField campoEnlace;
    @FXML
    private Label etiquetaError;
    @FXML
    private Button botonQuitarMultimedia;
    @FXML
    private TextArea campoNotas;

    private Ejercicio ejercicio;
    private String rutaMultimedia;

    @FXML
    private void initialize() {
        comboGrupo.setItems(FXCollections.observableArrayList(GrupoMuscular.values()));
        spinnerSeries.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 3));
        spinnerRepsMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 8));
        spinnerRepsMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 12));
        spinnerRir.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 2));
        spinnerDescanso.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 180, 90, 5));
    }

    /** Rellena el formulario con los datos del ejercicio a editar. */
    public void cargar(Ejercicio ejercicio) {
        this.ejercicio = ejercicio;
        this.rutaMultimedia = ejercicio.getRutaMultimedia();

        campoNombre.setText(ejercicio.getNombre());
        comboGrupo.setValue(ejercicio.getGrupoMuscular());
        spinnerSeries.getValueFactory().setValue(ejercicio.getSeries());
        spinnerRepsMin.getValueFactory().setValue(ejercicio.getRepeticionesMin());
        spinnerRepsMax.getValueFactory().setValue(ejercicio.getRepeticionesMax());
        spinnerRir.getValueFactory().setValue(ejercicio.getRirObjetivo());
        spinnerDescanso.getValueFactory().setValue(ejercicio.getDescansoSegundos());
        checkPorTiempo.setSelected(ejercicio.isPorTiempo());
        campoNotas.setText(ejercicio.getNotas() == null ? "" : ejercicio.getNotas());
        campoEnlace.setText(ejercicio.getEnlaceMultimedia() == null ? "" : ejercicio.getEnlaceMultimedia());
        actualizarEtiquetaMultimedia();
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
            etiquetaError.setText("El nombre del ejercicio es obligatorio.");
            return false;
        }
        if (comboGrupo.getValue() == null) {
            etiquetaError.setText("Selecciona el grupo muscular.");
            return false;
        }
        if (spinnerRepsMax.getValue() < spinnerRepsMin.getValue()) {
            etiquetaError.setText("Las repeticiones máximas no pueden ser menores que las mínimas.");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    /** Vuelca los valores del formulario sobre el ejercicio. */
    public void aplicar() {
        ejercicio.setNombre(campoNombre.getText().trim());
        ejercicio.setGrupoMuscular(comboGrupo.getValue());
        ejercicio.setSeries(spinnerSeries.getValue());
        ejercicio.setRepeticionesMin(spinnerRepsMin.getValue());
        ejercicio.setRepeticionesMax(spinnerRepsMax.getValue());
        ejercicio.setRirObjetivo(spinnerRir.getValue());
        ejercicio.setDescansoSegundos(spinnerDescanso.getValue());
        ejercicio.setPorTiempo(checkPorTiempo.isSelected());
        ejercicio.setRutaMultimedia(rutaMultimedia);
        ejercicio.setEnlaceMultimedia(campoEnlace.getText() == null || campoEnlace.getText().isBlank()
                ? null : campoEnlace.getText().trim());
        ejercicio.setNotas(campoNotas.getText() == null || campoNotas.getText().isBlank()
                ? null : campoNotas.getText().trim());
    }

    @FXML
    private void seleccionarMultimedia() {
        File archivo = ArchivoMultimediaService
                .selectorMultimedia(GestorDatos.getInstancia().getDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(ventana());
        if (archivo != null) {
            rutaMultimedia = archivo.getAbsolutePath();
            GestorDatos.getInstancia().recordarCarpetaMultimedia(archivo.getParent());
            actualizarEtiquetaMultimedia();
        }
    }

    @FXML
    private void quitarMultimedia() {
        rutaMultimedia = null;
        actualizarEtiquetaMultimedia();
    }

    private void actualizarEtiquetaMultimedia() {
        boolean existe = ArchivoMultimediaService.existe(rutaMultimedia);
        etiquetaMultimedia.setText(existe
                ? ArchivoMultimediaService.nombreArchivo(rutaMultimedia)
                : "Sin archivo (.png, .jpg, .gif, .mp4)");
        botonQuitarMultimedia.setDisable(!existe);
    }

    private Window ventana() {
        return etiquetaMultimedia.getScene() == null ? null : etiquetaMultimedia.getScene().getWindow();
    }
}
