package com.bey.desktop.controller;

import com.bey.desktop.model.beyPistaAudio;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyReproductorService;
import com.bey.desktop.util.beyIconosMedia;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class beyMiniReproductorController implements Initializable {
    private final beyReproductorService beyServicio = beyReproductorService.beyGetInstancia();

    @FXML
    private HBox beyRaizMini;
    @FXML
    private StackPane beyMarcoMiniPortada;
    @FXML
    private ImageView beyMiniPortada;
    @FXML
    private Label beySimboloMiniPortada;
    @FXML
    private Label beyMiniTitulo;
    @FXML
    private Label beyMiniArtista;
    @FXML
    private Button beyMiniAnterior;
    @FXML
    private Button beyMiniReproducir;
    @FXML
    private Button beyMiniSiguiente;
    @FXML
    private Label beyMiniTiempo;
    @FXML
    private Slider beyMiniProgreso;
    @FXML
    private Slider beyMiniVolumen;
    @FXML
    private Button beyMiniCerrar;
    @FXML
    private Button beyMiniAleatorio;
    @FXML
    private Button beyMiniRepeticion;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyRaizMini.visibleProperty().bind(beyServicio.beyPistaActualProperty().isNotNull());
        beyRaizMini.managedProperty().bind(beyRaizMini.visibleProperty());

        Rectangle recorte = new Rectangle(44, 44);
        recorte.setArcWidth(12);
        recorte.setArcHeight(12);
        beyMarcoMiniPortada.setClip(recorte);
        beyMiniPortada.setVisible(false);

        beyMiniAnterior.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.ANTERIOR, 18));
        beyMiniAnterior.setTooltip(new Tooltip("Pista anterior"));
        beyMiniSiguiente.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.SIGUIENTE, 18));
        beyMiniSiguiente.setTooltip(new Tooltip("Pista siguiente"));
        beyMiniAleatorio.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.ALEATORIO, 17));
        beyServicio.beyAleatorioProperty().addListener((obs, antes, ahora) -> beyActualizarModos());
        beyServicio.beyModoRepeticionProperty().addListener((obs, antes, ahora) -> beyActualizarModos());
        beyActualizarModos();
        beyMiniCerrar.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.CERRAR, 16));
        beyMiniCerrar.setTooltip(new Tooltip("Cerrar el reproductor"));

        beyMiniVolumen.valueProperty().bindBidirectional(beyServicio.beyVolumenProperty());

        beyMiniProgreso.setOnMousePressed(evento -> beyServicio.beySetArrastrandoProgreso(true));
        beyMiniProgreso.setOnMouseReleased(evento -> {
            beyServicio.beySetArrastrandoProgreso(false);
            beyServicio.beySeek(beyMiniProgreso.getValue());
        });

        beyServicio.beyPistaActualProperty().addListener((obs, antes, ahora) -> beyMostrarPortada(ahora));
        beyServicio.beyTituloProperty().addListener((obs, antes, ahora) ->
                beyMiniTitulo.setText(ahora == null ? "" : ahora));
        beyServicio.beyArtistaProperty().addListener((obs, antes, ahora) ->
                beyMiniArtista.setText(ahora == null ? "" : ahora));
        beyServicio.beyReproduciendoProperty().addListener((obs, antes, ahora) -> beyActualizarBoton(ahora));
        beyServicio.beyProgresoSegundosProperty().addListener((obs, antes, ahora) -> {
            beyMiniProgreso.setValue(ahora.doubleValue());
            beyActualizarTiempo();
        });
        beyServicio.beyDuracionSegundosProperty().addListener((obs, antes, ahora) -> {
            beyMiniProgreso.setMax(Math.max(1, ahora.doubleValue()));
            beyActualizarTiempo();
        });

        beyActualizarBoton(beyServicio.beyIsReproduciendo());
        beyMiniTitulo.setText(beyServicio.beyGetTitulo());
        beyMiniArtista.setText(beyServicio.beyGetArtista());
        beyMiniProgreso.setMax(Math.max(1, beyServicio.beyGetDuracionSegundos()));
        beyMostrarPortada(beyServicio.beyGetPistaActual());
        beyActualizarTiempo();
    }

    @FXML
    private void beyAnterior() {
        beyServicio.beyAnterior();
    }

    @FXML
    private void beySiguiente() {
        beyServicio.beySiguiente();
    }

    @FXML
    private void beyAlternar() {
        beyServicio.beyAlternar();
    }

    @FXML
    private void beyAlternarAleatorio() {
        beyServicio.beyAlternarAleatorio();
    }

    @FXML
    private void beyCambiarRepeticion() {
        beyServicio.beyCiclarRepeticion();
    }

    private void beyActualizarModos() {
        boolean aleatorio = beyServicio.beyIsAleatorio();
        beyMiniAleatorio.getStyleClass().remove("boton-media-mini-activo");
        if (aleatorio) {
            beyMiniAleatorio.getStyleClass().add("boton-media-mini-activo");
        }
        beyMiniAleatorio.setTooltip(new Tooltip(aleatorio
                ? "Aleatorio activado (pulsa para desactivar)" : "Activar modo aleatorio"));

        beyReproductorService.beyModoRepeticion modo = beyServicio.beyGetModoRepeticion();
        beyMiniRepeticion.setGraphic(beyIconosMedia.beyIcono(
                modo == beyReproductorService.beyModoRepeticion.UNA
                        ? beyIconosMedia.REPETIR_UNA : beyIconosMedia.REPETIR, 17));
        beyMiniRepeticion.getStyleClass().remove("boton-media-mini-activo");
        if (modo != beyReproductorService.beyModoRepeticion.DESACTIVADO) {
            beyMiniRepeticion.getStyleClass().add("boton-media-mini-activo");
        }
        beyMiniRepeticion.setTooltip(new Tooltip(switch (modo) {
            case DESACTIVADO -> "Repetición desactivada";
            case TODAS -> "Bucle de lista activado";
            case UNA -> "Repetir esta pista";
        }));
    }

    @FXML
    private void beyCerrar() {
        beyServicio.beyDetener();
    }

    private void beyActualizarBoton(boolean reproduciendo) {
        beyMiniReproducir.setGraphic(
                beyIconosMedia.beyIcono(reproduciendo ? beyIconosMedia.PAUSA : beyIconosMedia.REPRODUCIR, 20));
        beyMiniReproducir.setTooltip(new Tooltip(reproduciendo ? "Pausar" : "Reproducir"));
    }

    private void beyActualizarTiempo() {
        beyMiniTiempo.setText(beyFormatear(beyServicio.beyGetProgresoSegundos())
                + " / " + beyFormatear(beyServicio.beyGetDuracionSegundos()));
    }

    private void beyMostrarPortada(beyPistaAudio pista) {
        Image imagen = beyArchivoMultimediaService.beyPortada(pista);
        beyMiniPortada.setImage(imagen);
        beyMiniPortada.setVisible(imagen != null);
        beySimboloMiniPortada.setVisible(imagen == null);
    }

    private static String beyFormatear(double segundosTotales) {
        int total = (int) Math.max(0, Math.round(segundosTotales));
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
