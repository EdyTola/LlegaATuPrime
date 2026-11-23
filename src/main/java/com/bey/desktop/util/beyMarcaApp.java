package com.bey.desktop.util;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.Locale;

public final class beyMarcaApp {
    private static final double LIENZO = 64.0;
    private static final double MARGEN = 0.15;
    private static final double RADIO_ESQUINA = 14.0;

    public static final Color AZUL_CLARO = Color.web("#4A93D6");
    public static final Color AZUL_PRIME = Color.web("#0F6CBD");
    public static final Color AZUL_PROFUNDO = Color.web("#0B4F8A");

    private static final String RUTA_CORREDOR =
            "M13.49,5.48c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2z"
            + "M9.89,19.38l1,-4.4 2.1,2v6h2v-7.5l-2.1,-2 0.6,-3c1.3,1.5 3.3,2.5 5.5,2.5v-2"
            + "c-1.9,0 -3.5,-1 -4.3,-2.4l-1,-1.6c-0.4,-0.6 -1,-1 -1.7,-1 -0.3,0 -0.5,0.1 -0.8,0.1"
            + "l-5.2,2.2v4.7h2v-3.4l1.8,-0.7 -1.6,8.1 -4.9,-1 -0.4,2 7,1.4z";
    private static final double CORREDOR_X = 6.0;
    private static final double CORREDOR_Y = 9.0;
    private static final double CORREDOR_ESCALA = 1.7;

    private static final String RUTA_PENDIENTE = "M12,49 L27,36 L34,42 L43,33 L50,49 Z";
    private static final double PENDIENTE_OPACIDAD = 0.22;

    private static final String RUTA_FLECHA = "M40,37 L54,19 M46,19 L54,19 L54,27";
    private static final double FLECHA_GROSOR = 3.4;

    private static double[] ajuste;

    private beyMarcaApp() {
    }

    private static Node beyGlifoCrudo(boolean detalle) {
        SVGPath pendiente = new SVGPath();
        pendiente.setContent(RUTA_PENDIENTE);
        pendiente.setFill(Color.web("#FFFFFF", detalle ? PENDIENTE_OPACIDAD : 0));

        SVGPath flecha = new SVGPath();
        flecha.setContent(RUTA_FLECHA);
        flecha.setFill(null);
        flecha.setStroke(Color.WHITE);
        flecha.setStrokeWidth(FLECHA_GROSOR);
        flecha.setStrokeLineCap(StrokeLineCap.ROUND);
        flecha.setStrokeLineJoin(StrokeLineJoin.ROUND);

        SVGPath corredor = new SVGPath();
        corredor.setContent(RUTA_CORREDOR);
        corredor.setFill(Color.WHITE);
        corredor.getTransforms().addAll(new Translate(CORREDOR_X, CORREDOR_Y),
                new Scale(CORREDOR_ESCALA, CORREDOR_ESCALA, 0, 0));

        return new Group(pendiente, flecha, corredor);
    }

    private static synchronized double[] beyAjuste() {
        if (ajuste == null) {
            Bounds b = beyGlifoCrudo(true).getBoundsInLocal();
            double area = LIENZO * (1 - 2 * MARGEN);
            double k = Math.min(area / b.getWidth(), area / b.getHeight());
            double cx = (b.getMinX() + b.getMaxX()) / 2;
            double cy = (b.getMinY() + b.getMaxY()) / 2;
            ajuste = new double[]{k, LIENZO / 2 - k * cx, LIENZO / 2 - k * cy};
        }
        return ajuste;
    }

    public static Node beyIsotipo(double lado) {
        return beyIsotipo(lado, 0);
    }

    public static Node beyIsotipo(double lado, double resplandor) {
        return beyIsotipo(lado, true);
    }

    public static Node beyIsotipo(double lado, boolean detalle) {
        double[] a = beyAjuste();

        Rectangle escudo = new Rectangle(LIENZO, LIENZO);
        escudo.setArcWidth(RADIO_ESQUINA * 2);
        escudo.setArcHeight(RADIO_ESQUINA * 2);
        escudo.setFill(AZUL_PRIME);

        Node glifo = beyGlifoCrudo(detalle);
        glifo.getTransforms().addAll(new Translate(a[1], a[2]), new Scale(a[0], a[0], 0, 0));

        Group lienzo = new Group(escudo, glifo);
        double escala = lado / LIENZO;
        lienzo.getTransforms().add(new Scale(escala, escala, 0, 0));

        Pane marca = new Pane(lienzo);
        marca.setMinSize(lado, lado);
        marca.setPrefSize(lado, lado);
        marca.setMaxSize(lado, lado);
        return marca;
    }

    public static String beySvg() {
        double[] a = beyAjuste();
        String color = String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(AZUL_PRIME.getRed() * 255), Math.round(AZUL_PRIME.getGreen() * 255),
                Math.round(AZUL_PRIME.getBlue() * 255));
        return String.format(Locale.ROOT,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n"
                        + "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\" width=\"512\" height=\"512\" role=\"img\" aria-label=\"Llega a tu Prime\">%n"
                        + "    <rect width=\"64\" height=\"64\" rx=\"%s\" ry=\"%s\" fill=\"%s\"/>%n"
                        + "    <g transform=\"translate(%.4f %.4f) scale(%.4f)\">%n"
                        + "        <path d=\"%s\" fill=\"#FFFFFF\" fill-opacity=\"%s\"/>%n"
                        + "        <path d=\"%s\" fill=\"none\" stroke=\"#FFFFFF\" stroke-width=\"%s\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>%n"
                        + "        <path transform=\"translate(%s %s) scale(%s)\" d=\"%s\" fill=\"#FFFFFF\"/>%n"
                        + "    </g>%n"
                        + "</svg>%n",
                trim(RADIO_ESQUINA), trim(RADIO_ESQUINA), color, a[1], a[2], a[0],
                RUTA_PENDIENTE, trim(PENDIENTE_OPACIDAD), RUTA_FLECHA, trim(FLECHA_GROSOR),
                trim(CORREDOR_X), trim(CORREDOR_Y), trim(CORREDOR_ESCALA), RUTA_CORREDOR);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.4f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
