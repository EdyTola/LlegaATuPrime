package com.symmetry.desktop.service;

import com.symmetry.desktop.model.TemaApp;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene el tema visual activo (claro u oscuro) y lo propaga a las escenas
 * registradas. El cambio se hace en caliente reemplazando la clase CSS sobre el
 * nodo raiz, cuyas variables (looked-up colors) se heredan a toda la escena.
 * Los dialogos auxiliares consultan {@link #getTema()} para alinearse al tema.
 */
public final class TemaService {

    private static final List<Scene> ESCENAS = new ArrayList<>();
    private static TemaApp temaActual = TemaApp.CLARO;

    private TemaService() {
    }

    /** Tema visual activo en este momento. */
    public static TemaApp getTema() {
        return temaActual;
    }

    /** Registra una escena y le aplica de inmediato el tema activo. */
    public static void registrar(Scene escena) {
        if (escena == null || ESCENAS.contains(escena)) {
            return;
        }
        ESCENAS.add(escena);
        aplicar(escena, temaActual);
    }

    /** Fija el tema activo y lo aplica a todas las escenas registradas. */
    public static void aplicarGlobal(TemaApp tema) {
        temaActual = tema == null ? TemaApp.CLARO : tema;
        for (Scene escena : ESCENAS) {
            aplicar(escena, temaActual);
        }
    }

    /** Aplica el tema indicado a una escena concreta. */
    public static void aplicar(Scene escena, TemaApp tema) {
        if (escena == null || escena.getRoot() == null) {
            return;
        }
        TemaApp efectivo = tema == null ? TemaApp.CLARO : tema;
        List<String> clases = escena.getRoot().getStyleClass();
        clases.remove(TemaApp.CLARO.getClaseCss());
        clases.remove(TemaApp.OSCURO.getClaseCss());
        clases.add(efectivo.getClaseCss());
    }

    /** Clase CSS que deben usar los nodos raiz de dialogos para respetar el tema activo. */
    public static String claseCssActiva() {
        return temaActual.getClaseCss();
    }
}
