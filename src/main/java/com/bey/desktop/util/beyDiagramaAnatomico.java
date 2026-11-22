package com.bey.desktop.util;

import com.bey.desktop.model.beyGrupoMuscular;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class beyDiagramaAnatomico extends HBox {
    private static final String CLASE_REGION = "musculo-region";
    private static final String CLASE_PRINCIPAL = "musculo-principal";
    private static final String CLASE_SECUNDARIO = "musculo-secundario";

    private static final double OPACIDAD_BASE = 0.22;
    private static final double OPACIDAD_PRINCIPAL = 1.0;
    private static final double OPACIDAD_SECUNDARIO = 0.72;

    private static final double ANCHO_FIGURA = 74;
    private static final double ALTO_FIGURA = 168;

    private static final double ANCHO_MINI = 22;
    private static final double ALTO_MINI = 40;
    private static final double ESCALA_MINI = 0.21;

    private static final double ANCHO_GRANDE = 78;
    private static final double ALTO_GRANDE = 112;
    private static final double ESCALA_GRANDE = 0.64;

    private final List<beyFigura> beyFiguras = new ArrayList<>();

    public beyDiagramaAnatomico() {
        getStyleClass().add("diagrama-anatomico");
        setSpacing(14);
        setAlignment(Pos.CENTER_LEFT);
        beyAgregarFigura(beyFiguraFrontal());
        beyAgregarFigura(beyFiguraDorsal());
        beyLimpiar();
    }

    public void beyResaltar(beyGrupoMuscular principal, List<beyGrupoMuscular> secundarios) {
        beyLimpiar();
        if (principal == null) {
            return;
        }
        beyResaltarGrupo(principal, CLASE_PRINCIPAL, OPACIDAD_PRINCIPAL);
        if (secundarios != null) {
            for (beyGrupoMuscular grupo : secundarios) {
                if (grupo != null && grupo != principal) {
                    beyResaltarGrupo(grupo, CLASE_SECUNDARIO, OPACIDAD_SECUNDARIO);
                }
            }
        }
    }

    public void beyLimpiar() {
        for (beyFigura figura : beyFiguras) {
            for (List<Shape> regiones : figura.beyRegiones.values()) {
                for (Shape region : regiones) {
                    region.getStyleClass().removeAll(CLASE_PRINCIPAL, CLASE_SECUNDARIO);
                    region.setOpacity(OPACIDAD_BASE);
                }
            }
        }
    }

    public static Node beyIconoMini(beyGrupoMuscular grupo) {
        beyFigura figura = beyPrefiereDorsal(grupo) ? beyFiguraDorsal() : beyFiguraFrontal();
        figura.beyNodo.setScaleX(ESCALA_MINI);
        figura.beyNodo.setScaleY(ESCALA_MINI);

        StackPane marco = new StackPane(figura.beyNodo);
        marco.getStyleClass().add("icono-musculo");
        marco.setMinSize(ANCHO_MINI, ALTO_MINI);
        marco.setPrefSize(ANCHO_MINI, ALTO_MINI);
        marco.setMaxSize(ANCHO_MINI, ALTO_MINI);

        if (grupo != null) {
            for (Shape region : figura.beyRegiones.getOrDefault(grupo, List.of())) {
                if (!region.getStyleClass().contains(CLASE_PRINCIPAL)) {
                    region.getStyleClass().add(CLASE_PRINCIPAL);
                }
                region.setOpacity(OPACIDAD_PRINCIPAL);
            }
        }
        return marco;
    }

    public static Node beyIconoGrande(beyGrupoMuscular grupo) {
        beyFigura figura = beyPrefiereDorsal(grupo) ? beyFiguraDorsal() : beyFiguraFrontal();
        figura.beyNodo.setScaleX(ESCALA_GRANDE);
        figura.beyNodo.setScaleY(ESCALA_GRANDE);

        StackPane marco = new StackPane(figura.beyNodo);
        marco.getStyleClass().add("icono-musculo");
        marco.setMinSize(ANCHO_GRANDE, ALTO_GRANDE);
        marco.setPrefSize(ANCHO_GRANDE, ALTO_GRANDE);
        marco.setMaxSize(ANCHO_GRANDE, ALTO_GRANDE);

        if (grupo != null) {
            for (beyGrupoMuscular secundario : grupo.getSecundarios()) {
                for (Shape region : figura.beyRegiones.getOrDefault(secundario, List.of())) {
                    region.getStyleClass().add(CLASE_SECUNDARIO);
                    region.setOpacity(OPACIDAD_SECUNDARIO);
                }
            }
            for (Shape region : figura.beyRegiones.getOrDefault(grupo, List.of())) {
                region.getStyleClass().add(CLASE_PRINCIPAL);
                region.setOpacity(OPACIDAD_PRINCIPAL);
            }
        }
        return marco;
    }

    private void beyAgregarFigura(beyFigura figura) {
        beyFiguras.add(figura);
        Label titulo = new Label(figura.beyEtiqueta);
        titulo.getStyleClass().add("diagrama-vista");
        VBox columna = new VBox(4, figura.beyNodo, titulo);
        columna.setAlignment(Pos.CENTER);
        getChildren().add(columna);
    }

    private void beyResaltarGrupo(beyGrupoMuscular grupo, String clase, double opacidad) {
        for (beyFigura figura : beyFiguras) {
            List<Shape> regiones = figura.beyRegiones.get(grupo);
            if (regiones == null) {
                continue;
            }
            for (Shape region : regiones) {
                if (!region.getStyleClass().contains(clase)) {
                    region.getStyleClass().add(clase);
                }
                region.setOpacity(opacidad);
            }
        }
    }

    private static boolean beyPrefiereDorsal(beyGrupoMuscular grupo) {
        return grupo == beyGrupoMuscular.ESPALDA || grupo == beyGrupoMuscular.GLUTEOS
                || grupo == beyGrupoMuscular.ISQUIOTIBIALES;
    }

    private static beyFigura beyFiguraFrontal() {
        Map<beyGrupoMuscular, List<Shape>> regiones = new EnumMap<>(beyGrupoMuscular.class);
        regiones.put(beyGrupoMuscular.PECHO, List.of(
                beyElipse(30, 42, 7, 6), beyElipse(44, 42, 7, 6)));
        regiones.put(beyGrupoMuscular.HOMBROS, List.of(
                beyCirculo(25, 32, 6), beyCirculo(49, 32, 6)));
        regiones.put(beyGrupoMuscular.BICEPS, List.of(
                beyElipse(18.5, 50, 4.5, 8), beyElipse(55.5, 50, 4.5, 8)));
        regiones.put(beyGrupoMuscular.ABDOMEN, List.of(
                beyElipse(37, 62, 9, 10)));
        regiones.put(beyGrupoMuscular.CUADRICEPS, List.of(
                beyElipse(32, 108, 5.5, 17), beyElipse(42, 108, 5.5, 17)));
        regiones.put(beyGrupoMuscular.PANTORRILLAS, List.of(
                beyElipse(30.5, 138, 4.5, 10), beyElipse(43.5, 138, 4.5, 10)));
        return beyConstruir("Frontal", regiones);
    }

    private static beyFigura beyFiguraDorsal() {
        Map<beyGrupoMuscular, List<Shape>> regiones = new EnumMap<>(beyGrupoMuscular.class);
        regiones.put(beyGrupoMuscular.ESPALDA, List.of(
                beyElipse(37, 45, 13, 15)));
        regiones.put(beyGrupoMuscular.TRICEPS, List.of(
                beyElipse(18.5, 50, 4.5, 8), beyElipse(55.5, 50, 4.5, 8)));
        regiones.put(beyGrupoMuscular.GLUTEOS, List.of(
                beyElipse(31, 82, 7, 7), beyElipse(43, 82, 7, 7)));
        regiones.put(beyGrupoMuscular.ISQUIOTIBIALES, List.of(
                beyElipse(32, 110, 5.5, 15), beyElipse(42, 110, 5.5, 15)));
        regiones.put(beyGrupoMuscular.PANTORRILLAS, List.of(
                beyElipse(30.5, 138, 4.5, 10), beyElipse(43.5, 138, 4.5, 10)));
        return beyConstruir("Dorsal", regiones);
    }

    private static beyFigura beyConstruir(String etiqueta, Map<beyGrupoMuscular, List<Shape>> regiones) {
        Pane lienzo = beySilueta();
        Pane capa = beyCapaRegiones();
        lienzo.getChildren().add(capa);
        for (List<Shape> grupo : regiones.values()) {
            for (Shape region : grupo) {
                region.getStyleClass().add(CLASE_REGION);
                region.setOpacity(OPACIDAD_BASE);
                capa.getChildren().add(region);
            }
        }
        return new beyFigura(etiqueta, lienzo, regiones);
    }

    private static Pane beySilueta() {
        Pane lienzo = new Pane();
        lienzo.setPrefSize(ANCHO_FIGURA, ALTO_FIGURA);
        lienzo.setMinSize(ANCHO_FIGURA, ALTO_FIGURA);
        lienzo.setMaxSize(ANCHO_FIGURA, ALTO_FIGURA);
        lienzo.setMouseTransparent(true);

        Rectangle cuello = new Rectangle(33, 22, 8, 8);
        Circle cabeza = new Circle(37, 14, 10);
        Rectangle tronco = new Rectangle(24, 28, 26, 50);
        tronco.setArcWidth(16);
        tronco.setArcHeight(16);
        beyMarcar(cuello, cabeza, tronco);

        lienzo.getChildren().addAll(
                cuello, cabeza, tronco,
                beyMiembro(24, 31, 14, 86, 11), beyMiembro(50, 31, 60, 86, 11),
                beyMiembro(31, 76, 30, 152, 13), beyMiembro(43, 76, 44, 152, 13),
                beyMiembro(30, 152, 26, 158, 10), beyMiembro(44, 152, 48, 158, 10));
        return lienzo;
    }

    private static void beyMarcar(Shape... formas) {
        for (Shape forma : formas) {
            forma.getStyleClass().add("cuerpo-silueta");
        }
    }

    private static Pane beyCapaRegiones() {
        Pane capa = new Pane();
        capa.setPrefSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMinSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMaxSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMouseTransparent(true);
        return capa;
    }

    private static Line beyMiembro(double x1, double y1, double x2, double y2, double grosor) {
        Line linea = new Line(x1, y1, x2, y2);
        linea.setStrokeWidth(grosor);
        linea.setStrokeLineCap(StrokeLineCap.ROUND);
        linea.getStyleClass().add("cuerpo-miembro");
        return linea;
    }

    private static Ellipse beyElipse(double centroX, double centroY, double radioX, double radioY) {
        return new Ellipse(centroX, centroY, radioX, radioY);
    }

    private static Circle beyCirculo(double centroX, double centroY, double radio) {
        return new Circle(centroX, centroY, radio);
    }

    private static final class beyFigura {
        private final String beyEtiqueta;
        private final Pane beyNodo;
        private final Map<beyGrupoMuscular, List<Shape>> beyRegiones;

        private beyFigura(String etiqueta, Pane nodo, Map<beyGrupoMuscular, List<Shape>> regiones) {
            this.beyEtiqueta = etiqueta;
            this.beyNodo = nodo;
            this.beyRegiones = regiones;
        }
    }
}
