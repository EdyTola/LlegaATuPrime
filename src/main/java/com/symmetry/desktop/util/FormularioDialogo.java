package com.symmetry.desktop.util;

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

/**
 * Envuelve un formulario FXML dentro de un dialogo modal con botones Aceptar/Cancelar.
 */
public class FormularioDialogo {

    private final FXMLLoader cargador;
    private final Dialog<ButtonType> dialogo;
    private final ButtonType botonAceptar;
    private BooleanSupplier validador;

    public FormularioDialogo(String rutaFxml, String titulo, String textoAceptar, Window propietario) {
        this.cargador = new FXMLLoader(FormularioDialogo.class.getResource(rutaFxml));
        Node contenido;
        try {
            contenido = cargador.load();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el formulario " + rutaFxml, e);
        }

        this.dialogo = new Dialog<>();
        this.dialogo.setTitle(titulo);
        this.dialogo.setResizable(true);
        if (propietario != null) {
            this.dialogo.initOwner(propietario);
            this.dialogo.initModality(Modality.WINDOW_MODAL);
        }

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        this.botonAceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().addAll(botonAceptar, botonCancelar);
        Dialogos.aplicarEstilo(panel);

        Node nodoAceptar = panel.lookupButton(botonAceptar);
        nodoAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (validador != null && !validador.getAsBoolean()) {
                evento.consume();
            }
        });
    }

    /** Controlador FXML del formulario contenido. */
    public Object getControlador() {
        return cargador.getController();
    }

    /** Funcion de validacion; si devuelve false el dialogo permanece abierto. */
    public void setValidador(BooleanSupplier validador) {
        this.validador = validador;
    }

    public boolean mostrarYConfirmar() {
        Optional<ButtonType> resultado = dialogo.showAndWait();
        return resultado.isPresent() && resultado.get() == botonAceptar;
    }
}
