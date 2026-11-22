package com.bey.desktop.controller;

import com.bey.desktop.model.beyPerfilUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

public class beyFormularioPerfilController {
    @FXML
    private TextField beyCampoNombre;
    @FXML
    private Spinner<Integer> beySpinnerEdad;
    @FXML
    private TextArea beyCampoObjetivo;
    @FXML
    private Label beyEtiquetaError;

    private List<beyPerfilUsuario> beyPerfilesExistentes = List.of();

    @FXML
    private void initialize() {
        beySpinnerEdad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, 25));
    }

    public void beySetPerfilesExistentes(List<beyPerfilUsuario> perfiles) {
        this.beyPerfilesExistentes = perfiles == null ? List.of() : perfiles;
    }

    public void beyPreparar() {
        beyCampoNombre.clear();
        beyCampoObjetivo.clear();
        beySpinnerEdad.getValueFactory().setValue(25);
        beyEtiquetaError.setText("");
    }

    public boolean beyValidar() {
        String nombre = getNombre();
        if (nombre.isEmpty()) {
            beyEtiquetaError.setText("El nombre del perfil es obligatorio.");
            return false;
        }
        boolean duplicado = beyPerfilesExistentes.stream()
                .anyMatch(p -> nombre.equalsIgnoreCase(p.getNombre()));
        if (duplicado) {
            beyEtiquetaError.setText("Ya existe un perfil con ese nombre.");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public String getNombre() {
        return beyCampoNombre.getText() == null ? "" : beyCampoNombre.getText().trim();
    }

    public Integer beyGetEdad() {
        return beySpinnerEdad.getValue();
    }

    public String beyGetObjetivo() {
        return beyCampoObjetivo.getText() == null ? "" : beyCampoObjetivo.getText().trim();
    }
}
