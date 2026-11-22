package com.bey.desktop.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class beyFormularioDialogo {
    private final FXMLLoader beyCargador;
    private final Dialog<ButtonType> beyDialogo;
    private final ButtonType beyBotonAceptar;
    private BooleanSupplier beyValidador;

    public beyFormularioDialogo(String rutaFxml, String titulo, String textoAceptar, Window propietario) {
        this.beyCargador = new FXMLLoader(beyFormularioDialogo.class.getResource(rutaFxml));
        Node contenido;
        try {
            contenido = beyCargador.load();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el formulario " + rutaFxml, e);
        }

        this.beyDialogo = new Dialog<>();
        this.beyDialogo.setTitle(titulo);
        this.beyDialogo.setResizable(true);
        if (propietario != null) {
            this.beyDialogo.initOwner(propietario);
            this.beyDialogo.initModality(Modality.WINDOW_MODAL);
        }

        DialogPane panel = beyDialogo.getDialogPane();
        panel.setContent(contenido);
        this.beyBotonAceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().addAll(beyBotonAceptar, botonCancelar);
        beyDialogos.beyAplicarEstilo(panel);

        Node nodoAceptar = panel.lookupButton(beyBotonAceptar);
        nodoAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (beyValidador != null && !beyValidador.getAsBoolean()) {
                evento.consume();
            }
        });
    }

    public Object beyGetControlador() {
        return beyCargador.getController();
    }

    public void beySetValidador(BooleanSupplier validador) {
        this.beyValidador = validador;
    }

    public boolean beyMostrarYConfirmar() {
        Optional<ButtonType> resultado = beyDialogo.showAndWait();
        return resultado.isPresent() && resultado.get() == beyBotonAceptar;
    }
}
