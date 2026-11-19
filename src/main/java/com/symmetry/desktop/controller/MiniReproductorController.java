package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.PistaAudio;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.ReproductorService;
import com.symmetry.desktop.util.IconosMedia;
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

/**
 * Barra de reproduccion persistente situada en la parte inferior de la ventana principal.
 * Se enlaza al {@link ReproductorService} global, por lo que permanece visible y operativa
 * en cualquier seccion mientras haya una pista cargada (sonando o en pausa).
 */
public class MiniReproductorController implements Initializable {

    private final ReproductorService servicio = ReproductorService.getInstancia();

    @FXML
    private HBox raizMini;
    @FXML
    private StackPane marcoMiniPortada;
    @FXML
    private ImageView miniPortada;
    @FXML
    private Label simboloMiniPortada;
    @FXML
    private Label miniTitulo;
    @FXML
    private Label miniArtista;
    @FXML
    private Button miniAnterior;
    @FXML
    private Button miniReproducir;
    @FXML
    private Button miniSiguiente;
    @FXML
    private Label miniTiempo;
    @FXML
    private Slider miniProgreso;
    @FXML
    private Slider miniVolumen;
    @FXML
    private Button miniCerrar;
    @FXML
    private Button miniAleatorio;
    @FXML
    private Button miniRepeticion;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        // La barra aparece sola en cuanto hay una pista cargada, en cualquier seccion.
        raizMini.visibleProperty().bind(servicio.pistaActualProperty().isNotNull());
        raizMini.managedProperty().bind(raizMini.visibleProperty());

        Rectangle recorte = new Rectangle(44, 44);
        recorte.setArcWidth(12);
        recorte.setArcHeight(12);
        marcoMiniPortada.setClip(recorte);
        miniPortada.setVisible(false);

        miniAnterior.setGraphic(IconosMedia.icono(IconosMedia.ANTERIOR, 18));
        miniAnterior.setTooltip(new Tooltip("Pista anterior"));
        miniSiguiente.setGraphic(IconosMedia.icono(IconosMedia.SIGUIENTE, 18));
        miniSiguiente.setTooltip(new Tooltip("Pista siguiente"));
        miniAleatorio.setGraphic(IconosMedia.icono(IconosMedia.ALEATORIO, 17));
        servicio.aleatorioProperty().addListener((obs, antes, ahora) -> actualizarModos());
        servicio.modoRepeticionProperty().addListener((obs, antes, ahora) -> actualizarModos());
        actualizarModos();
        miniCerrar.setGraphic(IconosMedia.icono(IconosMedia.CERRAR, 16));
        miniCerrar.setTooltip(new Tooltip("Cerrar el reproductor"));

        miniVolumen.valueProperty().bindBidirectional(servicio.volumenProperty());

        miniProgreso.setOnMousePressed(evento -> servicio.setArrastrandoProgreso(true));
        miniProgreso.setOnMouseReleased(evento -> {
            servicio.setArrastrandoProgreso(false);
            servicio.seek(miniProgreso.getValue());
        });

        servicio.pistaActualProperty().addListener((obs, antes, ahora) -> mostrarPortada(ahora));
        servicio.tituloProperty().addListener((obs, antes, ahora) ->
                miniTitulo.setText(ahora == null ? "" : ahora));
        servicio.artistaProperty().addListener((obs, antes, ahora) ->
                miniArtista.setText(ahora == null ? "" : ahora));
        servicio.reproduciendoProperty().addListener((obs, antes, ahora) -> actualizarBoton(ahora));
        servicio.progresoSegundosProperty().addListener((obs, antes, ahora) -> {
            miniProgreso.setValue(ahora.doubleValue());
            actualizarTiempo();
        });
        servicio.duracionSegundosProperty().addListener((obs, antes, ahora) -> {
            miniProgreso.setMax(Math.max(1, ahora.doubleValue()));
            actualizarTiempo();
        });

        // Estado inicial: el modulo de musica pudo dejar una pista en curso.
        actualizarBoton(servicio.isReproduciendo());
        miniTitulo.setText(servicio.getTitulo());
        miniArtista.setText(servicio.getArtista());
        miniProgreso.setMax(Math.max(1, servicio.getDuracionSegundos()));
        mostrarPortada(servicio.getPistaActual());
        actualizarTiempo();
    }

    @FXML
    private void anterior() {
        servicio.anterior();
    }

    @FXML
    private void siguiente() {
        servicio.siguiente();
    }

    @FXML
    private void alternar() {
        servicio.alternar();
    }

    @FXML
    private void alternarAleatorio() {
        servicio.alternarAleatorio();
    }

    @FXML
    private void cambiarRepeticion() {
        servicio.ciclarRepeticion();
    }

    /** Refleja el modo activo (mismas reglas y mismo resaltado que la vista de musica). */
    private void actualizarModos() {
        boolean aleatorio = servicio.isAleatorio();
        miniAleatorio.getStyleClass().remove("boton-media-mini-activo");
        if (aleatorio) {
            miniAleatorio.getStyleClass().add("boton-media-mini-activo");
        }
        miniAleatorio.setTooltip(new Tooltip(aleatorio
                ? "Aleatorio activado (pulsa para desactivar)" : "Activar modo aleatorio"));

        ReproductorService.ModoRepeticion modo = servicio.getModoRepeticion();
        miniRepeticion.setGraphic(IconosMedia.icono(
                modo == ReproductorService.ModoRepeticion.UNA
                        ? IconosMedia.REPETIR_UNA : IconosMedia.REPETIR, 17));
        miniRepeticion.getStyleClass().remove("boton-media-mini-activo");
        if (modo != ReproductorService.ModoRepeticion.DESACTIVADO) {
            miniRepeticion.getStyleClass().add("boton-media-mini-activo");
        }
        miniRepeticion.setTooltip(new Tooltip(switch (modo) {
            case DESACTIVADO -> "Repetición desactivada";
            case TODAS -> "Bucle de lista activado";
            case UNA -> "Repetir esta pista";
        }));
    }

    @FXML
    private void cerrar() {
        servicio.detener();
    }

    private void actualizarBoton(boolean reproduciendo) {
        miniReproducir.setGraphic(
                IconosMedia.icono(reproduciendo ? IconosMedia.PAUSA : IconosMedia.REPRODUCIR, 20));
        miniReproducir.setTooltip(new Tooltip(reproduciendo ? "Pausar" : "Reproducir"));
    }

    private void actualizarTiempo() {
        miniTiempo.setText(formatear(servicio.getProgresoSegundos())
                + " / " + formatear(servicio.getDuracionSegundos()));
    }

    private void mostrarPortada(PistaAudio pista) {
        Image imagen = ArchivoMultimediaService.portada(pista);
        miniPortada.setImage(imagen);
        miniPortada.setVisible(imagen != null);
        simboloMiniPortada.setVisible(imagen == null);
    }

    private static String formatear(double segundosTotales) {
        int total = (int) Math.max(0, Math.round(segundosTotales));
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
