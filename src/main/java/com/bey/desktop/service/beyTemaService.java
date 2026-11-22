package com.bey.desktop.service;

import com.bey.desktop.model.beyTemaApp;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public final class beyTemaService {
    private static final List<Scene> ESCENAS = new ArrayList<>();
    private static beyTemaApp beyTemaActual = beyTemaApp.CLARO;

    private beyTemaService() {
    }

    public static beyTemaApp beyGetTema() {
        return beyTemaActual;
    }

    public static void beyRegistrar(Scene escena) {
        if (escena == null || ESCENAS.contains(escena)) {
            return;
        }
        ESCENAS.add(escena);
        beyAplicar(escena, beyTemaActual);
    }

    public static void beyDesregistrar(Scene escena) {
        ESCENAS.remove(escena);
    }

    public static void beyAplicarGlobal(beyTemaApp tema) {
        beyTemaActual = tema == null ? beyTemaApp.CLARO : tema;
        for (Scene escena : ESCENAS) {
            beyAplicar(escena, beyTemaActual);
        }
    }

    public static void beyAplicar(Scene escena, beyTemaApp tema) {
        if (escena == null || escena.getRoot() == null) {
            return;
        }
        beyTemaApp efectivo = tema == null ? beyTemaApp.CLARO : tema;
        List<String> clases = escena.getRoot().getStyleClass();
        clases.remove(beyTemaApp.CLARO.getClaseCss());
        clases.remove(beyTemaApp.OSCURO.getClaseCss());
        clases.add(efectivo.getClaseCss());
    }

    public static String beyClaseCssActiva() {
        return beyTemaActual.getClaseCss();
    }
}
