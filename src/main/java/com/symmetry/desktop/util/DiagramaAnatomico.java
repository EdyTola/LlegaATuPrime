package com.symmetry.desktop.util;

import com.symmetry.desktop.model.GrupoMuscular;
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

/**
 * Diagrama anatomico del cuerpo humano (vista frontal y dorsal) que resalta el grupo
 * muscular principal y los secundarios que trabaja un ejercicio. Se construye por
 * completo en codigo para no ensuciar el FXML del panel de sugerencias.
 *
 * <p>Cada figura se dibuja con una silueta simplificada (cabeza, tronco y extremidades)
 * sobre la que se superponen las regiones musculares como formas independientes. El
 * resaltado se hace alternando clases de estilo, por lo que respeta el tema claro/oscuro.</p>
 */
public final class DiagramaAnatomico extends HBox {

    private static final String CLASE_REGION = "musculo-region";
    private static final String CLASE_PRINCIPAL = "musculo-principal";
    private static final String CLASE_SECUNDARIO = "musculo-secundario";

    private static final double OPACIDAD_BASE = 0.22;
    private static final double OPACIDAD_PRINCIPAL = 1.0;
    private static final double OPACIDAD_SECUNDARIO = 0.72;

    private static final double ANCHO_FIGURA = 74;
    private static final double ALTO_FIGURA = 168;

    /** Tamano reducido del icono de grupo muscular usado en las filas de la lista. */
    private static final double ANCHO_MINI = 22;
    private static final double ALTO_MINI = 40;
    private static final double ESCALA_MINI = 0.21;

    /** Tamano del icono grande usado en las tarjetas amplias de la biblioteca. */
    private static final double ANCHO_GRANDE = 78;
    private static final double ALTO_GRANDE = 112;
    private static final double ESCALA_GRANDE = 0.64;

    private final List<Figura> figuras = new ArrayList<>();

    public DiagramaAnatomico() {
        getStyleClass().add("diagrama-anatomico");
        setSpacing(14);
        setAlignment(Pos.CENTER_LEFT);
        agregarFigura(figuraFrontal());
        agregarFigura(figuraDorsal());
        limpiar();
    }

    /** Resalta el grupo principal y los secundarios; {@code principal} nulo restablece la base. */
    public void resaltar(GrupoMuscular principal, List<GrupoMuscular> secundarios) {
        limpiar();
        if (principal == null) {
            return;
        }
        resaltarGrupo(principal, CLASE_PRINCIPAL, OPACIDAD_PRINCIPAL);
        if (secundarios != null) {
            for (GrupoMuscular grupo : secundarios) {
                if (grupo != null && grupo != principal) {
                    resaltarGrupo(grupo, CLASE_SECUNDARIO, OPACIDAD_SECUNDARIO);
                }
            }
        }
    }

    /** Restablece todas las regiones a su estado atenuado. */
    public void limpiar() {
        for (Figura figura : figuras) {
            for (List<Shape> regiones : figura.regiones.values()) {
                for (Shape region : regiones) {
                    region.getStyleClass().removeAll(CLASE_PRINCIPAL, CLASE_SECUNDARIO);
                    region.setOpacity(OPACIDAD_BASE);
                }
            }
        }
    }

    /**
     * Icono reducido de un grupo muscular para las filas de la lista de sugerencias:
     * muestra la vista (frontal o dorsal) donde el grupo es mas visible.
     */
    public static Node iconoMini(GrupoMuscular grupo) {
        Figura figura = prefiereDorsal(grupo) ? figuraDorsal() : figuraFrontal();
        figura.nodo.setScaleX(ESCALA_MINI);
        figura.nodo.setScaleY(ESCALA_MINI);

        StackPane marco = new StackPane(figura.nodo);
        marco.getStyleClass().add("icono-musculo");
        marco.setMinSize(ANCHO_MINI, ALTO_MINI);
        marco.setPrefSize(ANCHO_MINI, ALTO_MINI);
        marco.setMaxSize(ANCHO_MINI, ALTO_MINI);

        if (grupo != null) {
            for (Shape region : figura.regiones.getOrDefault(grupo, List.of())) {
                if (!region.getStyleClass().contains(CLASE_PRINCIPAL)) {
                    region.getStyleClass().add(CLASE_PRINCIPAL);
                }
                region.setOpacity(OPACIDAD_PRINCIPAL);
            }
        }
        return marco;
    }

    /**
     * Icono grande (78x112 px) de un grupo muscular: muestra la vista (frontal o dorsal) donde
     * el grupo es mas visible, con el musculo principal en color de acento y los secundarios
     * en verde, para identificar de un vistazo que trabaja el ejercicio.
     */
    public static Node iconoGrande(GrupoMuscular grupo) {
        Figura figura = prefiereDorsal(grupo) ? figuraDorsal() : figuraFrontal();
        figura.nodo.setScaleX(ESCALA_GRANDE);
        figura.nodo.setScaleY(ESCALA_GRANDE);

        StackPane marco = new StackPane(figura.nodo);
        marco.getStyleClass().add("icono-musculo");
        marco.setMinSize(ANCHO_GRANDE, ALTO_GRANDE);
        marco.setPrefSize(ANCHO_GRANDE, ALTO_GRANDE);
        marco.setMaxSize(ANCHO_GRANDE, ALTO_GRANDE);

        if (grupo != null) {
            for (GrupoMuscular secundario : grupo.getSecundarios()) {
                for (Shape region : figura.regiones.getOrDefault(secundario, List.of())) {
                    region.getStyleClass().add(CLASE_SECUNDARIO);
                    region.setOpacity(OPACIDAD_SECUNDARIO);
                }
            }
            for (Shape region : figura.regiones.getOrDefault(grupo, List.of())) {
                region.getStyleClass().add(CLASE_PRINCIPAL);
                region.setOpacity(OPACIDAD_PRINCIPAL);
            }
        }
        return marco;
    }

    private void agregarFigura(Figura figura) {
        figuras.add(figura);
        Label titulo = new Label(figura.etiqueta);
        titulo.getStyleClass().add("diagrama-vista");
        VBox columna = new VBox(4, figura.nodo, titulo);
        columna.setAlignment(Pos.CENTER);
        getChildren().add(columna);
    }

    private void resaltarGrupo(GrupoMuscular grupo, String clase, double opacidad) {
        for (Figura figura : figuras) {
            List<Shape> regiones = figura.regiones.get(grupo);
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

    /** Las regiones de espalda y tren inferior se leen mejor en la vista dorsal. */
    private static boolean prefiereDorsal(GrupoMuscular grupo) {
        return grupo == GrupoMuscular.ESPALDA || grupo == GrupoMuscular.GLUTEOS
                || grupo == GrupoMuscular.ISQUIOTIBIALES;
    }

    // ------------------------------------------------------------------
    // Construccion de las figuras
    // ------------------------------------------------------------------

    private static Figura figuraFrontal() {
        Map<GrupoMuscular, List<Shape>> regiones = new EnumMap<>(GrupoMuscular.class);
        regiones.put(GrupoMuscular.PECHO, List.of(
                elipse(30, 42, 7, 6), elipse(44, 42, 7, 6)));
        regiones.put(GrupoMuscular.HOMBROS, List.of(
                circulo(25, 32, 6), circulo(49, 32, 6)));
        regiones.put(GrupoMuscular.BICEPS, List.of(
                elipse(18.5, 50, 4.5, 8), elipse(55.5, 50, 4.5, 8)));
        regiones.put(GrupoMuscular.ABDOMEN, List.of(
                elipse(37, 62, 9, 10)));
        regiones.put(GrupoMuscular.CUADRICEPS, List.of(
                elipse(32, 108, 5.5, 17), elipse(42, 108, 5.5, 17)));
        regiones.put(GrupoMuscular.PANTORRILLAS, List.of(
                elipse(30.5, 138, 4.5, 10), elipse(43.5, 138, 4.5, 10)));
        return construir("Frontal", regiones);
    }

    private static Figura figuraDorsal() {
        Map<GrupoMuscular, List<Shape>> regiones = new EnumMap<>(GrupoMuscular.class);
        regiones.put(GrupoMuscular.ESPALDA, List.of(
                elipse(37, 45, 13, 15)));
        regiones.put(GrupoMuscular.TRICEPS, List.of(
                elipse(18.5, 50, 4.5, 8), elipse(55.5, 50, 4.5, 8)));
        regiones.put(GrupoMuscular.GLUTEOS, List.of(
                elipse(31, 82, 7, 7), elipse(43, 82, 7, 7)));
        regiones.put(GrupoMuscular.ISQUIOTIBIALES, List.of(
                elipse(32, 110, 5.5, 15), elipse(42, 110, 5.5, 15)));
        regiones.put(GrupoMuscular.PANTORRILLAS, List.of(
                elipse(30.5, 138, 4.5, 10), elipse(43.5, 138, 4.5, 10)));
        return construir("Dorsal", regiones);
    }

    private static Figura construir(String etiqueta, Map<GrupoMuscular, List<Shape>> regiones) {
        Pane lienzo = silueta();
        Pane capa = capaRegiones();
        lienzo.getChildren().add(capa);
        for (List<Shape> grupo : regiones.values()) {
            for (Shape region : grupo) {
                region.getStyleClass().add(CLASE_REGION);
                region.setOpacity(OPACIDAD_BASE);
                capa.getChildren().add(region);
            }
        }
        return new Figura(etiqueta, lienzo, regiones);
    }

    /** Silueta neutra del cuerpo: cabeza, cuello, tronco y extremidades. */
    private static Pane silueta() {
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
        marcar(cuello, cabeza, tronco);

        lienzo.getChildren().addAll(
                cuello, cabeza, tronco,
                miembro(24, 31, 14, 86, 11), miembro(50, 31, 60, 86, 11),
                miembro(31, 76, 30, 152, 13), miembro(43, 76, 44, 152, 13),
                miembro(30, 152, 26, 158, 10), miembro(44, 152, 48, 158, 10));
        return lienzo;
    }

    private static void marcar(Shape... formas) {
        for (Shape forma : formas) {
            forma.getStyleClass().add("cuerpo-silueta");
        }
    }

    private static Pane capaRegiones() {
        Pane capa = new Pane();
        capa.setPrefSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMinSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMaxSize(ANCHO_FIGURA, ALTO_FIGURA);
        capa.setMouseTransparent(true);
        return capa;
    }

    private static Line miembro(double x1, double y1, double x2, double y2, double grosor) {
        Line linea = new Line(x1, y1, x2, y2);
        linea.setStrokeWidth(grosor);
        linea.setStrokeLineCap(StrokeLineCap.ROUND);
        linea.getStyleClass().add("cuerpo-miembro");
        return linea;
    }

    private static Ellipse elipse(double centroX, double centroY, double radioX, double radioY) {
        return new Ellipse(centroX, centroY, radioX, radioY);
    }

    private static Circle circulo(double centroX, double centroY, double radio) {
        return new Circle(centroX, centroY, radio);
    }

    /** Figura corporal con sus regiones musculares asociadas. */
    private static final class Figura {

        private final String etiqueta;
        private final Pane nodo;
        private final Map<GrupoMuscular, List<Shape>> regiones;

        private Figura(String etiqueta, Pane nodo, Map<GrupoMuscular, List<Shape>> regiones) {
            this.etiqueta = etiqueta;
            this.nodo = nodo;
            this.regiones = regiones;
        }
    }
}
