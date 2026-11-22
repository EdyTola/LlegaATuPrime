package com.bey.desktop.controller;

import com.bey.desktop.model.beyComida;
import com.bey.desktop.util.beyFormato;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class beyFormularioComidaController {
    @FXML
    private TextField beyCampoNombre;
    @FXML
    private TextField beyCampoProteina;
    @FXML
    private TextField beyCampoCalorias;
    @FXML
    private TextArea beyCampoNotas;
    @FXML
    private Label beyEtiquetaError;

    private beyComida beyComida;

    public void beyCargar(beyComida comida) {
        this.beyComida = comida;
        beyCampoNombre.setText(comida.getNombre());
        beyCampoProteina.setText(comida.getProteinaGramos() > 0 ? beyFormato.beyNumero(comida.getProteinaGramos()) : "");
        beyCampoCalorias.setText(comida.getCalorias() > 0 ? beyFormato.beyNumero(comida.getCalorias()) : "");
        beyCampoNotas.setText(comida.getNotas() == null ? "" : comida.getNotas());
    }

    public boolean beyValidar() {
        if (beyCampoNombre.getText() == null || beyCampoNombre.getText().isBlank()) {
            beyEtiquetaError.setText("El nombre de la comida es obligatorio.");
            return false;
        }
        if (beyFormato.beyANumero(beyCampoProteina.getText()) < 0 || beyFormato.beyANumero(beyCampoCalorias.getText()) < 0) {
            beyEtiquetaError.setText("Los valores de proteína y calorías no pueden ser negativos.");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public void beyAplicar() {
        beyComida.setNombre(beyCampoNombre.getText().trim());
        beyComida.setProteinaGramos(beyFormato.beyANumero(beyCampoProteina.getText()));
        beyComida.setCalorias(beyFormato.beyANumero(beyCampoCalorias.getText()));
        beyComida.setNotas(beyCampoNotas.getText() == null || beyCampoNotas.getText().isBlank()
                ? null : beyCampoNotas.getText().trim());
    }
}
