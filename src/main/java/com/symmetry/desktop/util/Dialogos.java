package com.symmetry.desktop.util;

import com.symmetry.desktop.model.TemaApp;
import com.symmetry.desktop.service.TemaService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Dialogos reutilizables con el estilo Fluent de la aplicacion.
 */
public final class Dialogos {

    private static final String RUTA_CSS = "/com/symmetry/desktop/css/symmetry.css";

    private Dialogos() {
    }

    /** Aplica la hoja de estilos de la aplicacion a un dialogo. */
    public static void aplicarEstilo(DialogPane panel) {
        URL css = Dialogos.class.getResource(RUTA_CSS);
        if (css != null) {
            String ruta = css.toExternalForm();
            if (!panel.getStylesheets().contains(ruta)) {
                panel.getStylesheets().add(ruta);
            }
        }
        if (!panel.getStyleClass().contains("dialogo-aplicacion")) {
            panel.getStyleClass().add("dialogo-aplicacion");
        }
        // Los dialogos heredan el tema activo (claro/oscuro) de la aplicacion.
        panel.getStyleClass().remove(TemaApp.CLARO.getClaseCss());
        panel.getStyleClass().remove(TemaApp.OSCURO.getClaseCss());
        panel.getStyleClass().add(TemaService.claseCssActiva());
    }

    public static void info(String titulo, String mensaje) {
        alerta(Alert.AlertType.INFORMATION, titulo, mensaje, null);
    }

    public static void error(String titulo, String mensaje) {
        alerta(Alert.AlertType.ERROR, titulo, mensaje, null);
    }

    public static void error(String titulo, String mensaje, Window propietario) {
        alerta(Alert.AlertType.ERROR, titulo, mensaje, propietario);
    }

    public static boolean confirmar(String titulo, String mensaje, Window propietario) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        if (propietario != null) {
            alerta.initOwner(propietario);
        }
        aplicarEstilo(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    /** Dialogo de seleccion simple a partir de una lista de opciones. */
    public static Optional<String> seleccionar(String titulo, String cabecera, List<String> opciones,
                                               String valorInicial, Window propietario) {
        if (opciones == null || opciones.isEmpty()) {
            return Optional.empty();
        }
        String inicial = valorInicial != null && opciones.contains(valorInicial)
                ? valorInicial : opciones.get(0);
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(inicial, opciones);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(cabecera);
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        aplicarEstilo(dialogo.getDialogPane());
        return dialogo.showAndWait();
    }

    private static void alerta(Alert.AlertType tipo, String titulo, String mensaje, Window propietario) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        if (propietario != null) {
            alerta.initOwner(propietario);
        }
        aplicarEstilo(alerta.getDialogPane());
        alerta.showAndWait();
    }
}
