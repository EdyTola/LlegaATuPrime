package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.MedicionCorporal;
import com.symmetry.desktop.util.Formato;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;

/**
 * Formulario modal para registrar una medicion corporal: peso, cintura, grasa corporal y
 * masa muscular. Todos los campos son opcionales, pero al menos uno debe informarse.
 */
public class FormularioMedicionController {

    @FXML
    private DatePicker campoFecha;
    @FXML
    private TextField campoPeso;
    @FXML
    private TextField campoCintura;
    @FXML
    private TextField campoGrasa;
    @FXML
    private TextField campoMasaMuscular;
    @FXML
    private Label etiquetaError;

    private MedicionCorporal medicion;

    /** Rellena el formulario con los datos de la medicion a registrar o editar. */
    public void cargar(MedicionCorporal medicion) {
        this.medicion = medicion;
        campoFecha.setValue(medicion.getFecha() == null ? LocalDate.now() : medicion.getFecha());
        campoPeso.setText(textoOpcional(medicion.getPesoKg()));
        campoCintura.setText(textoOpcional(medicion.getCinturaCm()));
        campoGrasa.setText(textoOpcional(medicion.getGrasaPorcentaje()));
        campoMasaMuscular.setText(textoOpcional(medicion.getMasaMuscularKg()));
    }

    /** Valida los campos; devuelve false si el dialogo debe permanecer abierto. */
    public boolean validar() {
        if (campoFecha.getValue() == null) {
            etiquetaError.setText("Selecciona la fecha de la medición.");
            return false;
        }
        boolean sinPeso = vacio(campoPeso);
        boolean sinCintura = vacio(campoCintura);
        boolean sinGrasa = vacio(campoGrasa);
        boolean sinMasa = vacio(campoMasaMuscular);
        if (sinPeso && sinCintura && sinGrasa && sinMasa) {
            etiquetaError.setText("Introduce al menos una medición (peso, cintura, grasa o masa muscular).");
            return false;
        }
        if (!sinPeso && Formato.aNumero(campoPeso.getText()) <= 0) {
            etiquetaError.setText("El peso debe ser un número mayor que cero.");
            return false;
        }
        if (!sinCintura && Formato.aNumero(campoCintura.getText()) <= 0) {
            etiquetaError.setText("La cintura debe ser un número mayor que cero.");
            return false;
        }
        if (!sinGrasa && Formato.aNumero(campoGrasa.getText()) <= 0) {
            etiquetaError.setText("El porcentaje de grasa debe ser un número mayor que cero.");
            return false;
        }
        if (!sinGrasa && Formato.aNumero(campoGrasa.getText()) > 70) {
            etiquetaError.setText("El porcentaje de grasa debe estar entre 0 y 70 %.");
            return false;
        }
        if (!sinMasa && Formato.aNumero(campoMasaMuscular.getText()) <= 0) {
            etiquetaError.setText("La masa muscular debe ser un número mayor que cero.");
            return false;
        }
        etiquetaError.setText("");
        return true;
    }

    /** Vuelca los valores del formulario sobre la medicion. */
    public void aplicar() {
        medicion.setFecha(campoFecha.getValue());
        medicion.setPesoKg(valorOpcional(campoPeso.getText()));
        medicion.setCinturaCm(valorOpcional(campoCintura.getText()));
        medicion.setGrasaPorcentaje(valorOpcional(campoGrasa.getText()));
        medicion.setMasaMuscularKg(valorOpcional(campoMasaMuscular.getText()));
    }

    private static boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private static String textoOpcional(Double valor) {
        return valor == null ? "" : Formato.numero(valor);
    }

    private static Double valorOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : Formato.aNumero(texto);
    }
}
