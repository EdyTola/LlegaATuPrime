package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.Comida;
import com.symmetry.desktop.util.Formato;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Formulario modal para registrar una comida: nombre, aporte proteico y calorico.
 */
public class FormularioComidaController {

    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoProteina;
    @FXML
    private TextField campoCalorias;
    @FXML
    private TextArea campoNotas;
    @FXML
    private Label etiquetaError;

    private Comida comida;

    /** Rellena el formulario con los datos de la comida a editar. */
    public void cargar(Comida comida) {
        this.comida = comida;
        campoNombre.setText(comida.getNombre());
        campoProteina.setText(comida.getProteinaGramos() > 0 ? Formato.numero(comida.getProteinaGramos()) : "");
        campoCalorias.setText(comida.getCalorias() > 0 ? Formato.numero(comida.getCalorias()) : "");
        campoNotas.setText(comida.getNotas() == null ? "" : comida.getNotas());
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        if (campoNombre.getText() == null || campoNombre.getText().isBlank()) {
            etiquetaError.setText("El nombre de la comida es obligatorio.");
            return false;
        }
        if (Formato.aNumero(campoProteina.getText()) < 0 || Formato.aNumero(campoCalorias.getText()) < 0) {
            etiquetaError.setText("Los valores de proteína y calorías no pueden ser negativos.");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    /** Vuelca los valores del formulario sobre la comida. */
    public void aplicar() {
        comida.setNombre(campoNombre.getText().trim());
        comida.setProteinaGramos(Formato.aNumero(campoProteina.getText()));
        comida.setCalorias(Formato.aNumero(campoCalorias.getText()));
        comida.setNotas(campoNotas.getText() == null || campoNotas.getText().isBlank()
                ? null : campoNotas.getText().trim());
    }
}
