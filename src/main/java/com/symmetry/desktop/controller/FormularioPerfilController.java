package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.PerfilUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Formulario modal para dar de alta un nuevo perfil local (nombre, edad y objetivo general).
 */
public class FormularioPerfilController {

    @FXML
    private TextField campoNombre;
    @FXML
    private Spinner<Integer> spinnerEdad;
    @FXML
    private TextArea campoObjetivo;
    @FXML
    private Label etiquetaError;

    private List<PerfilUsuario> perfilesExistentes = List.of();

    @FXML
    private void initialize() {
        spinnerEdad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, 25));
    }

    /** Perfiles ya registrados, usados para impedir nombres duplicados. */
    public void setPerfilesExistentes(List<PerfilUsuario> perfiles) {
        this.perfilesExistentes = perfiles == null ? List.of() : perfiles;
    }

    /** Estado inicial del formulario. */
    public void preparar() {
        campoNombre.clear();
        campoObjetivo.clear();
        spinnerEdad.getValueFactory().setValue(25);
        etiquetaError.setText("");
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        String nombre = getNombre();
        if (nombre.isEmpty()) {
            etiquetaError.setText("El nombre del perfil es obligatorio.");
            return false;
        }
        boolean duplicado = perfilesExistentes.stream()
                .anyMatch(p -> nombre.equalsIgnoreCase(p.getNombre()));
        if (duplicado) {
            etiquetaError.setText("Ya existe un perfil con ese nombre.");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    public String getNombre() {
        return campoNombre.getText() == null ? "" : campoNombre.getText().trim();
    }

    public Integer getEdad() {
        return spinnerEdad.getValue();
    }

    public String getObjetivo() {
        return campoObjetivo.getText() == null ? "" : campoObjetivo.getText().trim();
    }
}
