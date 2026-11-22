package com.bey.desktop.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class beyMarcaApp {
    private static final double LIENZO = 48.0;

    public static final Color AZUL_CLARO = Color.web("#2AA9F0");
    public static final Color AZUL_PRIME = Color.web("#0078D4");
    public static final Color AZUL_PROFUNDO = Color.web("#0A3D7A");

    private static final String RUTA_CORREDOR =
            "M13.49,5.48c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2z"
            + "M9.89,19.38l1,-4.4 2.1,2v6h2v-7.5l-2.1,-2 0.6,-3c1.3,1.5 3.3,2.5 5.5,2.5v-2"
            + "c-1.9,0 -3.5,-1 -4.3,-2.4l-1,-1.6c-0.4,-0.6 -1,-1 -1.7,-1 -0.3,0 -0.5,0.1 -0.8,0.1"
            + "l-5.2,2.2v4.7h2v-3.4l1.8,-0.7 -1.6,8.1 -4.9,-1 -0.4,2 7,1.4z";

    private static final String RUTA_MONTANAS = "M3,37 L13,24 L20,31 L30,19 L45,37 Z";

    private static final String RUTA_FLECHA = "M31,31 L44,15 M37,15 L44,15 L44,22";

    private beyMarcaApp() {
    }

    public static Node beyIsotipo(double lado) {
        return beyIsotipo(lado, 0);
    }

    public static Node beyIsotipo(double lado, double resplandor) {
        double escala = lado / LIENZO;

        Rectangle escudo = new Rectangle(lado, lado);
        escudo.setArcWidth(lado * 0.30);
        escudo.setArcHeight(lado * 0.30);
        escudo.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, AZUL_CLARO),
                new Stop(0.55, AZUL_PRIME),
                new Stop(1.0, AZUL_PROFUNDO)));
        if (resplandor > 0) {
            DropShadow halo = new DropShadow();
            halo.setColor(Color.web("#0A6EBD", 0.55));
            halo.setRadius(resplandor);
            halo.setSpread(0.22);
            escudo.setEffect(halo);
        }

        SVGPath montanas = new SVGPath();
        montanas.setContent(RUTA_MONTANAS);
        montanas.setFill(Color.web("#FFFFFF", 0.22));

        SVGPath flecha = new SVGPath();
        flecha.setContent(RUTA_FLECHA);
        flecha.setFill(null);
        flecha.setStroke(Color.web("#FFFFFF", 0.92));
        flecha.setStrokeWidth(3.4);
        flecha.setStrokeLineCap(StrokeLineCap.ROUND);
        flecha.setStrokeLineJoin(StrokeLineJoin.ROUND);

        SVGPath corredor = new SVGPath();
        corredor.setContent(RUTA_CORREDOR);
        corredor.getTransforms().addAll(new Scale(1.3, 1.3, 0, 0), new Translate(2, 6));
        corredor.setFill(Color.WHITE);

        Group glifo = new Group(montanas, flecha, corredor);
        glifo.getTransforms().add(new Scale(escala, escala, 0, 0));

        StackPane marca = new StackPane(escudo, glifo);
        marca.setMinSize(lado, lado);
        marca.setPrefSize(lado, lado);
        marca.setMaxSize(lado, lado);
        return marca;
    }
}
