package com.bey.desktop.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public final class beyIconosMedia {
    public static final String REPRODUCIR = "M8,5 L19,12 L8,19 Z";
    public static final String PAUSA = "M6,19h4V5H6v14zM14,5v14h4V5h-4z";
    public static final String ANTERIOR = "M6,6h2.4v12H6V6z M18,6v12l-8.6,-6L18,6z";
    public static final String SIGUIENTE = "M15.6,6H18v12h-2.4V6z M6,6v12l8.6,-6L6,6z";
    public static final String ALEATORIO =
            "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 "
            + "17.96,7.46 20,9.5V4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z";
    public static final String REPETIR =
            "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4z";
    public static final String REPETIR_UNA =
            "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4zM13,15V9h-1l-2,1v1h1.5v4H13z";
    public static final String CERRAR =
            "M6.4,5 L12,10.6 L17.6,5 L19,6.4 L13.4,12 L19,17.6 L17.6,19 L12,13.4 L6.4,19 L5,17.6 L10.6,12 L5,6.4 Z";

    private beyIconosMedia() {
    }

    public static Node beyIcono(String trazadoSvg, double lado) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(trazadoSvg);
        ruta.getStyleClass().add("icono-media");
        double escala = lado / 24.0;
        ruta.setScaleX(escala);
        ruta.setScaleY(escala);

        StackPane marco = new StackPane(ruta);
        marco.setMinSize(lado, lado);
        marco.setPrefSize(lado, lado);
        marco.setMaxSize(lado, lado);
        marco.setMouseTransparent(true);
        return marco;
    }
}
