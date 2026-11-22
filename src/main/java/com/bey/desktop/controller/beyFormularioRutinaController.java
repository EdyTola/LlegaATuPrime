package com.bey.desktop.controller;

import com.bey.desktop.model.beyDiaSemana;
import com.bey.desktop.model.beyRutina;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class beyFormularioRutinaController {
    @FXML
    private TextField beyCampoNombre;
    @FXML
    private ComboBox<beyDiaSemana> beyComboDia;
    @FXML
    private TextArea beyCampoNotas;
    @FXML
    private Label beyEtiquetaError;

    private beyRutina beyRutina;

    @FXML
    private void initialize() {
        beyComboDia.setItems(FXCollections.observableArrayList(beyDiaSemana.values()));
    }

    public void beyCargar(beyRutina rutina) {
        this.beyRutina = rutina;
        beyCampoNombre.setText(rutina.getNombre());
        beyComboDia.setValue(rutina.getDiaSemana());
        beyCampoNotas.setText(rutina.getNotas() == null ? "" : rutina.getNotas());
    }

    public boolean beyValidar() {
        if (beyCampoNombre.getText() == null || beyCampoNombre.getText().isBlank()) {
            beyEtiquetaError.setText("El nombre de la rutina es obligatorio.");
            return false;
        }
        if (beyComboDia.getValue() == null) {
            beyEtiquetaError.setText("Selecciona el día de la semana.");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public void beyAplicar() {
        beyRutina.setNombre(beyCampoNombre.getText().trim());
        beyRutina.setDiaSemana(beyComboDia.getValue());
        beyRutina.setNotas(beyCampoNotas.getText() == null || beyCampoNotas.getText().isBlank()
                ? null : beyCampoNotas.getText().trim());
    }
}
