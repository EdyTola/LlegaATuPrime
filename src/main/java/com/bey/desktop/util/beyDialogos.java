package com.bey.desktop.util;

import com.bey.desktop.model.beyTemaApp;
import com.bey.desktop.service.beyTemaService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public final class beyDialogos {
    private static final String RUTA_CSS = "/com/bey/desktop/css/bey.css";

    private beyDialogos() {
    }

    public static void beyAplicarEstilo(DialogPane panel) {
        URL css = beyDialogos.class.getResource(RUTA_CSS);
        if (css != null) {
            String ruta = css.toExternalForm();
            if (!panel.getStylesheets().contains(ruta)) {
                panel.getStylesheets().add(ruta);
            }
        }
        if (!panel.getStyleClass().contains("dialogo-aplicacion")) {
            panel.getStyleClass().add("dialogo-aplicacion");
        }

        panel.getStyleClass().remove(beyTemaApp.CLARO.getClaseCss());
        panel.getStyleClass().remove(beyTemaApp.OSCURO.getClaseCss());
        panel.getStyleClass().add(beyTemaService.beyClaseCssActiva());
    }

    public static void beyInfo(String titulo, String mensaje) {
        beyAlerta(Alert.AlertType.INFORMATION, titulo, mensaje, null);
    }

    public static void beyError(String titulo, String mensaje) {
        beyAlerta(Alert.AlertType.ERROR, titulo, mensaje, null);
    }

    public static void beyError(String titulo, String mensaje, Window propietario) {
        beyAlerta(Alert.AlertType.ERROR, titulo, mensaje, propietario);
    }

    public static boolean beyConfirmar(String titulo, String mensaje, Window propietario) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        if (propietario != null) {
            alerta.initOwner(propietario);
        }
        beyAplicarEstilo(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    public static Optional<String> beySeleccionar(String titulo, String cabecera, List<String> opciones,
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
        beyAplicarEstilo(dialogo.getDialogPane());
        return dialogo.showAndWait();
    }

    private static void beyAlerta(Alert.AlertType tipo, String titulo, String mensaje, Window propietario) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        if (propietario != null) {
            alerta.initOwner(propietario);
        }
        beyAplicarEstilo(alerta.getDialogPane());
        alerta.showAndWait();
    }
}
