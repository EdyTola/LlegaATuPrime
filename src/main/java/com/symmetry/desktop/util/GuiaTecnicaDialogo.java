package com.symmetry.desktop.util;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.PatronMovimiento;
import com.symmetry.desktop.service.GuiaTecnicaService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Ventana modal "Guia tecnica" de un ejercicio. Siempre ofrece una referencia visual offline:
 * <ul>
 *   <li><b>Pestana 1 - Modelo 2D animado</b> (por defecto): {@link ExerciseAnimator2D} con
 *       pausa, camara lenta y los puntos clave de ejecucion.</li>
 *   <li><b>Pestana 2 - Video / imagen personalizada</b>: el archivo o enlace que el usuario
 *       adjunto, con el boton para adjuntar uno nuevo.</li>
 * </ul>
 */
public final class GuiaTecnicaDialogo {

    private GuiaTecnicaDialogo() {
    }

    /**
     * @param ejercicio     ejercicio cuya tecnica se muestra
     * @param nombre        nombre a mostrar (puede ser una sustitucion del Workout Player)
     * @param alAdjuntar    se invoca con la ruta del archivo adjuntado; {@code null} oculta el boton
     */
    public static void mostrar(Ejercicio ejercicio, String nombre, Window propietario,
                               Consumer<String> alAdjuntar) {
        String titulo = nombre == null || nombre.isBlank() ? ejercicio.getNombre() : nombre;
        PatronMovimiento patron = GuiaTecnicaService.detectar(titulo, ejercicio.getGrupoMuscular());

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Guía técnica");
        dialogo.setResizable(true);
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        DialogPane panel = dialogo.getDialogPane();
        panel.getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        Dialogos.aplicarEstilo(panel);

        Label etiquetaTitulo = new Label(titulo);
        etiquetaTitulo.getStyleClass().add("titulo-seccion");
        Label etiquetaPatron = new Label(patron.getTitulo()
                + (ejercicio.getGrupoMuscular() == null ? "" : "  ·  " + ejercicio.getGrupoMuscular().getEtiqueta())
                + "  ·  " + ejercicio.getPrescripcion());
        etiquetaPatron.getStyleClass().add("texto-secundario");

        // ----- Pestana 1: modelo 2D animado -----
        ExerciseAnimator2D animador = new ExerciseAnimator2D(patron, ejercicio.getGrupoMuscular());
        VBox.setVgrow(animador, Priority.ALWAYS);
        HBox controles = crearControles(animador);
        VBox izquierda = new VBox(10, animador, controles);
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        HBox cuerpo2d = new HBox(18, izquierda, crearPuntosClave(patron));
        cuerpo2d.setPadding(new Insets(12, 4, 4, 4));
        Tab pestana2d = new Tab("Modelo 2D animado", cuerpo2d);
        pestana2d.setClosable(false);

        // ----- Pestana 2: multimedia personalizada (se crea al abrirla por primera vez) -----
        Tab pestanaMedia = new Tab("Video / imagen personalizada");
        pestanaMedia.setClosable(false);
        VisorMultimedia.PanelMultimedia[] panelMedia = new VisorMultimedia.PanelMultimedia[1];
        boolean[] pausadoPorUsuario = new boolean[1];

        TabPane pestanas = new TabPane(pestana2d, pestanaMedia);
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanas.getStyleClass().add("guia-pestanas");
        pestanas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual == pestanaMedia) {
                pausadoPorUsuario[0] = animador.isPausado();
                animador.setPausado(true);
                if (panelMedia[0] == null) {
                    panelMedia[0] = VisorMultimedia.crearPanel(ejercicio.getRutaMultimedia(),
                            ejercicio.getEnlaceMultimedia(), propietario, alAdjuntar);
                    pestanaMedia.setContent(panelMedia[0].getNodo());
                } else {
                    panelMedia[0].reanudar();
                }
            } else {
                if (panelMedia[0] != null) {
                    panelMedia[0].pausar();
                }
                animador.setPausado(pausadoPorUsuario[0]);
            }
        });

        VBox contenido = new VBox(10, new VBox(2, etiquetaTitulo, etiquetaPatron), pestanas);
        VBox.setVgrow(pestanas, Priority.ALWAYS);
        contenido.setPrefSize(960, 560);
        panel.setContent(contenido);

        dialogo.setOnHidden(e -> {
            animador.detener();
            if (panelMedia[0] != null) {
                panelMedia[0].liberar();
            }
        });
        dialogo.showAndWait();
    }

    /** Pausar/Reanudar y selector de velocidad (Normal / Camara lenta 0.5x). */
    private static HBox crearControles(ExerciseAnimator2D animador) {
        Button pausar = new Button("Pausar");
        pausar.getStyleClass().add("boton-secundario");
        pausar.setMinWidth(96);
        pausar.setOnAction(e -> {
            animador.setPausado(!animador.isPausado());
            pausar.setText(animador.isPausado() ? "Reanudar" : "Pausar");
        });

        ToggleButton normal = new ToggleButton("Normal");
        ToggleButton lenta = new ToggleButton("Cámara lenta 0.5x");
        normal.getStyleClass().add("segmento-boton");
        lenta.getStyleClass().add("segmento-boton");
        ToggleGroup grupo = new ToggleGroup();
        normal.setToggleGroup(grupo);
        lenta.setToggleGroup(grupo);
        normal.setSelected(true);
        grupo.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            animador.setVelocidad(actual == lenta ? 0.5 : 1.0);
        });
        HBox segmento = new HBox(normal, lenta);
        segmento.getStyleClass().add("segmento-contenedor");
        segmento.setAlignment(Pos.CENTER);

        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label ayuda = new Label("La cámara lenta ayuda a ver la postura en cada fase.");
        ayuda.getStyleClass().add("texto-secundario");

        HBox barra = new HBox(12, pausar, segmento, hueco, ayuda);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    /** Lista numerada con los 3 puntos clave de ejecucion del patron. */
    private static VBox crearPuntosClave(PatronMovimiento patron) {
        Label titulo = new Label("Puntos clave de ejecución");
        titulo.getStyleClass().add("titulo-seccion");
        VBox lista = new VBox(12);
        int n = 1;
        for (String punto : patron.getPuntosClave()) {
            Label numero = new Label(String.valueOf(n++));
            numero.getStyleClass().add("guia-numero");
            numero.setMinSize(24, 24);
            numero.setAlignment(Pos.CENTER);
            Label texto = new Label(punto);
            texto.getStyleClass().add("guia-punto");
            texto.setWrapText(true);
            HBox fila = new HBox(10, numero, texto);
            fila.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(texto, Priority.ALWAYS);
            lista.getChildren().add(fila);
        }
        VBox caja = new VBox(14, titulo, lista);
        caja.getStyleClass().add("guia-panel");
        caja.setPrefWidth(300);
        caja.setMinWidth(260);
        caja.setMaxWidth(320);
        return caja;
    }
}
