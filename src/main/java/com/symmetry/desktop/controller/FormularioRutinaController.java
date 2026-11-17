package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.DiaSemana;
import com.symmetry.desktop.model.Rutina;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Formulario modal para crear o editar una rutina (nombre, dia y notas).
 */
public class FormularioRutinaController {

    @FXML
    private TextField campoNombre;
    @FXML
    private ComboBox<DiaSemana> comboDia;
    @FXML
    private TextArea campoNotas;
    @FXML
    private Label etiquetaError;

    private Rutina rutina;

    @FXML
    private void initialize() {
        comboDia.setItems(FXCollections.observableArrayList(DiaSemana.values()));
    }

    /** Rellena el formulario con los datos de la rutina a editar. */
    public void cargar(Rutina rutina) {
        this.rutina = rutina;
        campoNombre.setText(rutina.getNombre());
        comboDia.setValue(rutina.getDiaSemana());
        campoNotas.setText(rutina.getNotas() == null ? "" : rutina.getNotas());
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
            etiquetaError.setText("El nombre de la rutina es obligatorio.");
            return false;
        }
        if (comboDia.getValue() == null) {
            etiquetaError.setText("Selecciona el día de la semana.");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    /** Vuelca los valores del formulario sobre la rutina. */
    public void aplicar() {
        rutina.setNombre(campoNombre.getText().trim());
        rutina.setDiaSemana(comboDia.getValue());
        rutina.setNotas(campoNotas.getText() == null || campoNotas.getText().isBlank()
                ? null : campoNotas.getText().trim());
    }
}
