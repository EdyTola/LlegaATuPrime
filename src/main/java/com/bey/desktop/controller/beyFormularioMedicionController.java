package com.bey.desktop.controller;

import com.bey.desktop.model.beyMedicionCorporal;
import com.bey.desktop.util.beyFormato;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class beyFormularioMedicionController {
    @FXML
    private DatePicker beyCampoFecha;
    @FXML
    private TextField beyCampoPeso;
    @FXML
    private TextField beyCampoCintura;
    @FXML
    private TextField beyCampoGrasa;
    @FXML
    private TextField beyCampoMasaMuscular;
    @FXML
    private Label beyEtiquetaError;

    private beyMedicionCorporal beyMedicion;

    public void beyCargar(beyMedicionCorporal medicion) {
        this.beyMedicion = medicion;
        beyCampoFecha.setValue(medicion.getFecha() == null ? LocalDate.now() : medicion.getFecha());
        beyCampoPeso.setText(beyTextoOpcional(medicion.getPesoKg()));
        beyCampoCintura.setText(beyTextoOpcional(medicion.getCinturaCm()));
        beyCampoGrasa.setText(beyTextoOpcional(medicion.getGrasaPorcentaje()));
        beyCampoMasaMuscular.setText(beyTextoOpcional(medicion.getMasaMuscularKg()));
    }

    public boolean beyValidar() {
        if (beyCampoFecha.getValue() == null) {
            beyEtiquetaError.setText("Selecciona la fecha de la medición.");
            return false;
        }
        boolean sinPeso = beyVacio(beyCampoPeso);
        boolean sinCintura = beyVacio(beyCampoCintura);
        boolean sinGrasa = beyVacio(beyCampoGrasa);
        boolean sinMasa = beyVacio(beyCampoMasaMuscular);
        if (sinPeso && sinCintura && sinGrasa && sinMasa) {
            beyEtiquetaError.setText("Introduce al menos una medición (peso, cintura, grasa o masa muscular).");
            return false;
        }
        if (!sinPeso && beyFormato.beyANumero(beyCampoPeso.getText()) <= 0) {
            beyEtiquetaError.setText("El peso debe ser un número mayor que cero.");
            return false;
        }
        if (!sinCintura && beyFormato.beyANumero(beyCampoCintura.getText()) <= 0) {
            beyEtiquetaError.setText("La cintura debe ser un número mayor que cero.");
            return false;
        }
        if (!sinGrasa && beyFormato.beyANumero(beyCampoGrasa.getText()) <= 0) {
            beyEtiquetaError.setText("El porcentaje de grasa debe ser un número mayor que cero.");
            return false;
        }
        if (!sinGrasa && beyFormato.beyANumero(beyCampoGrasa.getText()) > 70) {
            beyEtiquetaError.setText("El porcentaje de grasa debe estar entre 0 y 70 %.");
            return false;
        }
        if (!sinMasa && beyFormato.beyANumero(beyCampoMasaMuscular.getText()) <= 0) {
            beyEtiquetaError.setText("La masa muscular debe ser un número mayor que cero.");
            return false;
        }
        beyEtiquetaError.setText("");
        return true;
    }

    public void beyAplicar() {
        beyMedicion.setFecha(beyCampoFecha.getValue());
        beyMedicion.setPesoKg(beyValorOpcional(beyCampoPeso.getText()));
        beyMedicion.setCinturaCm(beyValorOpcional(beyCampoCintura.getText()));
        beyMedicion.setGrasaPorcentaje(beyValorOpcional(beyCampoGrasa.getText()));
        beyMedicion.setMasaMuscularKg(beyValorOpcional(beyCampoMasaMuscular.getText()));
    }

    private static boolean beyVacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private static String beyTextoOpcional(Double valor) {
        return valor == null ? "" : beyFormato.beyNumero(valor);
    }

    private static Double beyValorOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : beyFormato.beyANumero(texto);
    }
}
