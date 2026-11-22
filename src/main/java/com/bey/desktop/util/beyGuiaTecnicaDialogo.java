package com.bey.desktop.util;

import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyModalidadEntrenamiento;
import com.bey.desktop.model.beyPatronMovimiento;
import com.bey.desktop.service.beyGuiaTecnicaService;
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

public final class beyGuiaTecnicaDialogo {
    private beyGuiaTecnicaDialogo() {
    }

    public static void beyMostrar(beyEjercicio ejercicio, String nombre, Window propietario,
                               Consumer<String> alAdjuntar) {
        beyMostrar(ejercicio, nombre, null, propietario, alAdjuntar);
    }

    public static void beyMostrar(beyEjercicio ejercicio, String nombre, beyModalidadEntrenamiento modalidad,
                               Window propietario, Consumer<String> alAdjuntar) {
        String titulo = nombre == null || nombre.isBlank() ? ejercicio.getNombre() : nombre;
        beyModalidadEntrenamiento modo = modalidad != null ? modalidad
                : (ejercicio.getAdaptadoDe() != null ? beyModalidadEntrenamiento.CASA : beyModalidadEntrenamiento.GIMNASIO);
        beyGuiaTecnicaService.beyPerfil perfil = beyGuiaTecnicaService.beyPerfil(titulo, ejercicio.getGrupoMuscular(), modo);
        beyPatronMovimiento patron = perfil.patron();

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Guía técnica");
        dialogo.setResizable(true);
        if (propietario != null) {
            dialogo.initOwner(propietario);
        }
        DialogPane panel = dialogo.getDialogPane();
        panel.getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        beyDialogos.beyAplicarEstilo(panel);

        Label etiquetaTitulo = new Label(titulo);
        etiquetaTitulo.getStyleClass().add("titulo-seccion");
        Label etiquetaPatron = new Label(beyGuiaTecnicaService.beyTitulo(perfil) + "  ·  " + perfil.implemento().getEtiqueta()
                + (ejercicio.getGrupoMuscular() == null ? "" : "  ·  " + ejercicio.getGrupoMuscular().getEtiqueta())
                + "  ·  " + ejercicio.getPrescripcion());
        etiquetaPatron.getStyleClass().add("texto-secundario");

        beyExerciseAnimator2D animador = new beyExerciseAnimator2D(perfil, ejercicio.getGrupoMuscular());
        VBox.setVgrow(animador, Priority.ALWAYS);
        HBox controles = beyCrearControles(animador);
        VBox izquierda = new VBox(10, animador, controles);
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        HBox cuerpo2d = new HBox(18, izquierda, beyCrearPuntosClave(beyGuiaTecnicaService.beyPuntosClave(perfil)));
        cuerpo2d.setPadding(new Insets(12, 4, 4, 4));
        Tab pestana2d = new Tab("Modelo 2D animado", cuerpo2d);
        pestana2d.setClosable(false);

        Tab pestanaMedia = new Tab("Video / imagen personalizada");
        pestanaMedia.setClosable(false);
        beyVisorMultimedia.beyPanelMultimedia[] panelMedia = new beyVisorMultimedia.beyPanelMultimedia[1];
        boolean[] pausadoPorUsuario = new boolean[1];

        TabPane pestanas = new TabPane(pestana2d, pestanaMedia);
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanas.getStyleClass().add("guia-pestanas");
        pestanas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual == pestanaMedia) {
                pausadoPorUsuario[0] = animador.beyIsPausado();
                animador.beySetPausado(true);
                if (panelMedia[0] == null) {
                    panelMedia[0] = beyVisorMultimedia.beyCrearPanel(ejercicio.getRutaMultimedia(),
                            ejercicio.getEnlaceMultimedia(), propietario, alAdjuntar);
                    pestanaMedia.setContent(panelMedia[0].beyGetNodo());
                } else {
                    panelMedia[0].beyReanudar();
                }
            } else {
                if (panelMedia[0] != null) {
                    panelMedia[0].beyPausar();
                }
                animador.beySetPausado(pausadoPorUsuario[0]);
            }
        });

        VBox contenido = new VBox(10, new VBox(2, etiquetaTitulo, etiquetaPatron), pestanas);
        VBox.setVgrow(pestanas, Priority.ALWAYS);
        contenido.setPrefSize(960, 560);
        panel.setContent(contenido);

        dialogo.setOnHidden(e -> {
            animador.beyDetener();
            if (panelMedia[0] != null) {
                panelMedia[0].beyLiberar();
            }
        });
        dialogo.showAndWait();
    }

    private static HBox beyCrearControles(beyExerciseAnimator2D animador) {
        Button pausar = new Button("Pausar");
        pausar.getStyleClass().add("boton-secundario");
        pausar.setMinWidth(96);
        pausar.setOnAction(e -> {
            animador.beySetPausado(!animador.beyIsPausado());
            pausar.setText(animador.beyIsPausado() ? "Reanudar" : "Pausar");
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
            animador.beySetVelocidad(actual == lenta ? 0.5 : 1.0);
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

    private static VBox beyCrearPuntosClave(java.util.List<String> puntos) {
        Label titulo = new Label("Puntos clave de ejecución");
        titulo.getStyleClass().add("titulo-seccion");
        VBox lista = new VBox(12);
        int n = 1;
        for (String punto : puntos) {
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
