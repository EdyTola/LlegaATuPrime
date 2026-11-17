package com.symmetry.desktop.controller;

import com.symmetry.desktop.MainApp;
import com.symmetry.desktop.model.PistaAudio;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.EspectroAudio;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Modulo 4 - Reproductor de musica: biblioteca de pistas MP3/WAV y enlaces, controles de
 * reproduccion con modo aleatorio y repeticion, espectro de audio animado, miniaturas de los
 * videos de YouTube y reordenacion de la lista de reproduccion por arrastrar y soltar.
 */
public class MusicaController implements Initializable, VistaConRefresco {

    /* Iconos vectoriales (viewBox 24x24) de los controles multimedia. */
    private static final String SVG_REPRODUCIR = "M8,5 L19,12 L8,19 Z";
    private static final String SVG_PAUSA = "M6,19h4V5H6v14zM14,5v14h4V5h-4z";
    private static final String SVG_ANTERIOR = "M6,6h2.4v12H6V6z M18,6v12l-8.6,-6L18,6z";
    private static final String SVG_SIGUIENTE = "M15.6,6H18v12h-2.4V6z M6,6v12l8.6,-6L6,6z";
    private static final String SVG_ALEATORIO =
            "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 "
            + "17.96,7.46 20,9.5V4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z";
    private static final String SVG_REPETIR =
            "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4z";
    private static final String SVG_REPETIR_UNA =
            "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4zM13,15V9h-1l-2,1v1h1.5v4H13z";

    /** Cache de miniaturas descargadas, compartida entre la lista y la portada del reproductor. */
    private static final Map<String, Image> PORTADAS = new HashMap<>();

    /** Estados del modo de repeticion, recorridos al pulsar el boton correspondiente. */
    private enum ModoRepeticion { DESACTIVADO, TODAS, UNA }

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final EspectroAudio espectro = new EspectroAudio();

    private MediaPlayer reproductor;
    private PistaAudio pistaActual;
    private PistaAudio pistaArrastrada;
    private boolean arrastrando;
    private boolean aleatorio;
    private ModoRepeticion modoRepeticion = ModoRepeticion.DESACTIVADO;

    @FXML
    private TableView<PistaAudio> listaPistas;
    @FXML
    private TableColumn<PistaAudio, PistaAudio> columnaPortada;
    @FXML
    private TableColumn<PistaAudio, String> columnaTitulo;
    @FXML
    private TableColumn<PistaAudio, String> columnaArtista;
    @FXML
    private TableColumn<PistaAudio, String> columnaOrigen;
    @FXML
    private TableColumn<PistaAudio, String> columnaDuracion;
    @FXML
    private Button botonEliminarPista;
    @FXML
    private Button botonAleatorio;
    @FXML
    private Button botonAnterior;
    @FXML
    private Button botonReproducir;
    @FXML
    private Button botonSiguiente;
    @FXML
    private Button botonRepeticion;
    @FXML
    private HBox zonaEspectro;
    @FXML
    private StackPane marcoPortada;
    @FXML
    private ImageView portadaActual;
    @FXML
    private Label simboloPortada;
    @FXML
    private Label etiquetaBiblioteca;
    @FXML
    private Label etiquetaTituloPista;
    @FXML
    private Label etiquetaArtista;
    @FXML
    private Label etiquetaTiempoActual;
    @FXML
    private Label etiquetaTiempoTotal;
    @FXML
    private Slider sliderProgreso;
    @FXML
    private Slider sliderVolumen;
    @FXML
    private Label etiquetaVolumen;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        columnaTitulo.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getTitulo()));
        columnaArtista.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getArtista() == null || datos.getValue().getArtista().isBlank()
                        ? "—" : datos.getValue().getArtista()));
        columnaDuracion.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getDuracionFormateada()));
        columnaOrigen.setCellValueFactory(datos -> new SimpleStringProperty(origenDe(datos.getValue())));
        columnaPortada.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columnaPortada.setCellFactory(columna -> celdaPortada());
        listaPistas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        listaPistas.setPlaceholder(new Label(
                "La biblioteca está vacía. Añade archivos MP3/WAV o un enlace de YouTube."));
        listaPistas.setRowFactory(tabla -> filaReproducible());
        listaPistas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> botonEliminarPista.setDisable(actual == null));

        configurarEspectro();
        configurarPortada();
        configurarControles();

        sliderVolumen.setValue(gestor.getDatos().getConfiguracion().getVolumenReproductor());
        actualizarEtiquetaVolumen();
        sliderVolumen.valueProperty().addListener((obs, anterior, actual) -> {
            if (reproductor != null) {
                reproductor.setVolume(actual.doubleValue());
            }
            gestor.getDatos().getConfiguracion().setVolumenReproductor(actual.doubleValue());
            actualizarEtiquetaVolumen();
        });

        sliderProgreso.setOnMousePressed(evento -> arrastrando = true);
        sliderProgreso.setOnMouseReleased(evento -> {
            arrastrando = false;
            if (reproductor != null) {
                reproductor.seek(Duration.seconds(sliderProgreso.getValue()));
            }
        });

        botonEliminarPista.setDisable(true);
        refrescar();
    }

    @Override
    public void refrescar() {
        listaPistas.setItems(FXCollections.observableArrayList(gestor.getDatos().getBibliotecaMusical()));
        etiquetaBiblioteca.setText(gestor.getDatos().getBibliotecaMusical().size()
                + " pistas · .mp3, .wav, .m4a, .aac o enlaces");
    }

    // ------------------------------------------------------------------
    // Espectro, portada y controles multimedia
    // ------------------------------------------------------------------

    /** Coloca el visualizador de audio en su zona de la tarjeta del reproductor. */
    private void configurarEspectro() {
        zonaEspectro.getChildren().setAll(espectro);
    }

    /** Recorta la portada del reproductor en un marco redondeado, estilo Fluent. */
    private void configurarPortada() {
        Rectangle recorte = new Rectangle(76, 76);
        recorte.setArcWidth(16);
        recorte.setArcHeight(16);
        marcoPortada.setClip(recorte);
        portadaActual.setVisible(false);
    }

    /** Sustituye los textos de los controles por iconos vectoriales y aplica las ayudas. */
    private void configurarControles() {
        botonAnterior.setGraphic(iconoMedia(SVG_ANTERIOR, 22));
        botonAnterior.setTooltip(new Tooltip("Pista anterior"));
        botonSiguiente.setGraphic(iconoMedia(SVG_SIGUIENTE, 22));
        botonSiguiente.setTooltip(new Tooltip("Pista siguiente"));
        botonAleatorio.setGraphic(iconoMedia(SVG_ALEATORIO, 20));
        botonAleatorio.setTooltip(new Tooltip("Modo aleatorio desactivado"));
        actualizarBotonReproducir(false);
        actualizarBotonRepeticion();
    }

    /** Boton circular con un icono vectorial centrado de tamano fijo. */
    private static Node iconoMedia(String contenidoSvg, double lado) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(contenidoSvg);
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

    private void actualizarBotonReproducir(boolean reproduciendo) {
        botonReproducir.setGraphic(iconoMedia(reproduciendo ? SVG_PAUSA : SVG_REPRODUCIR, 26));
        botonReproducir.setTooltip(new Tooltip(reproduciendo ? "Pausar" : "Reproducir"));
    }

    @FXML
    private void alternarAleatorio() {
        aleatorio = !aleatorio;
        botonAleatorio.getStyleClass().remove("boton-media-activo");
        if (aleatorio) {
            botonAleatorio.getStyleClass().add("boton-media-activo");
        }
        botonAleatorio.setTooltip(new Tooltip(
                aleatorio ? "Modo aleatorio activado" : "Modo aleatorio desactivado"));
    }

    /** Recorre los modos de repeticion: desactivado, toda la lista y esta pista. */
    @FXML
    private void cambiarRepeticion() {
        modoRepeticion = switch (modoRepeticion) {
            case DESACTIVADO -> ModoRepeticion.TODAS;
            case TODAS -> ModoRepeticion.UNA;
            case UNA -> ModoRepeticion.DESACTIVADO;
        };
        actualizarBotonRepeticion();
    }

    private void actualizarBotonRepeticion() {
        botonRepeticion.setGraphic(iconoMedia(
                modoRepeticion == ModoRepeticion.UNA ? SVG_REPETIR_UNA : SVG_REPETIR, 20));
        botonRepeticion.getStyleClass().remove("boton-media-activo");
        if (modoRepeticion != ModoRepeticion.DESACTIVADO) {
            botonRepeticion.getStyleClass().add("boton-media-activo");
        }
        botonRepeticion.setTooltip(new Tooltip(switch (modoRepeticion) {
            case DESACTIVADO -> "Repetición desactivada";
            case TODAS -> "Repetir toda la lista";
            case UNA -> "Repetir esta pista";
        }));
    }

    // ------------------------------------------------------------------
    // Lista de reproduccion: filas con arrastrar y soltar y miniaturas
    // ------------------------------------------------------------------

    /** Fila de la biblioteca: doble clic para reproducir y arrastre para reordenar. */
    private TableRow<PistaAudio> filaReproducible() {
        TableRow<PistaAudio> fila = new TableRow<>();

        fila.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2 && fila.getItem() != null) {
                reproducir(fila.getItem());
            }
        });

        fila.setOnDragDetected(evento -> {
            if (fila.isEmpty() || fila.getItem() == null) {
                return;
            }
            pistaArrastrada = fila.getItem();
            Dragboard tablero = fila.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(fila.getItem().getId());
            tablero.setContent(contenido);
            fila.getStyleClass().add("fila-arrastrando");
            evento.consume();
        });

        fila.setOnDragOver(evento -> {
            if (puedeSoltarEn(fila)) {
                evento.acceptTransferModes(TransferMode.MOVE);
                evento.consume();
            }
        });

        fila.setOnDragEntered(evento -> {
            if (puedeSoltarEn(fila)) {
                fila.getStyleClass().add("fila-destino");
            }
        });

        fila.setOnDragExited(evento -> fila.getStyleClass().remove("fila-destino"));

        fila.setOnDragDropped(evento -> {
            if (puedeSoltarEn(fila)) {
                reordenar(pistaArrastrada, listaPistas.getItems().indexOf(fila.getItem()));
                evento.setDropCompleted(true);
            } else {
                evento.setDropCompleted(false);
            }
            evento.consume();
        });

        fila.setOnDragDone(evento -> {
            fila.getStyleClass().removeAll("fila-arrastrando", "fila-destino");
            pistaArrastrada = null;
        });

        return fila;
    }

    private boolean puedeSoltarEn(TableRow<PistaAudio> fila) {
        return pistaArrastrada != null && !fila.isEmpty() && fila.getItem() != null
                && fila.getItem() != pistaArrastrada;
    }

    /** Mueve la pista arrastrada a la posicion de destino y persiste el nuevo orden. */
    private void reordenar(PistaAudio pista, int destino) {
        List<PistaAudio> pistas = new ArrayList<>(listaPistas.getItems());
        int origen = pistas.indexOf(pista);
        if (origen < 0 || destino < 0 || origen == destino) {
            return;
        }
        pistas.remove(origen);
        pistas.add(Math.min(destino, pistas.size()), pista);
        gestor.reordenarBiblioteca(pistas);
        refrescar();
        listaPistas.getSelectionModel().select(pista);
    }

    /** Celda de la columna de portada: miniatura del video de YouTube o icono de musica. */
    private TableCell<PistaAudio, PistaAudio> celdaPortada() {
        return new TableCell<>() {
            private final ImageView vista = new ImageView();
            private final Label simbolo = new Label();
            private final StackPane marco = new StackPane(simbolo, vista);

            {
                vista.setFitWidth(72);
                vista.setFitHeight(40);
                vista.setPreserveRatio(true);
                vista.setSmooth(true);
                simbolo.getStyleClass().add("portada-simbolo");
                marco.getStyleClass().add("portada-miniatura");
                marco.setMinSize(76, 42);
                marco.setPrefSize(76, 42);
                marco.setMaxSize(76, 42);
                Rectangle recorte = new Rectangle(76, 42);
                recorte.setArcWidth(8);
                recorte.setArcHeight(8);
                marco.setClip(recorte);
            }

            @Override
            protected void updateItem(PistaAudio pista, boolean vacio) {
                super.updateItem(pista, vacio);
                if (vacio || pista == null) {
                    setGraphic(null);
                    return;
                }
                Image imagen = cargarPortada(pista);
                vista.setImage(imagen);
                vista.setVisible(imagen != null);
                simbolo.setVisible(imagen == null);
                simbolo.setText(pista.esEnlace() ? "\u25B6" : "\u266B");
                setGraphic(marco);
            }
        };
    }

    /**
     * Devuelve la miniatura del video cuando la pista es un enlace de YouTube. La descarga se
     * hace en segundo plano y se cachea; sin conexion simplemente no hay imagen y la celda
     * muestra su icono de respaldo.
     */
    private static Image cargarPortada(PistaAudio pista) {
        if (pista == null || !pista.esEnlace()) {
            return null;
        }
        String url = ArchivoMultimediaService.miniaturaYoutube(pista.getEnlace());
        if (url == null) {
            return null;
        }
        return PORTADAS.computeIfAbsent(url, clave -> new Image(clave, 0, 0, true, true, true));
    }

    private void mostrarPortadaActual(PistaAudio pista) {
        Image imagen = cargarPortada(pista);
        portadaActual.setImage(imagen);
        portadaActual.setVisible(imagen != null);
        simboloPortada.setVisible(imagen == null);
    }

    // ------------------------------------------------------------------
    // Biblioteca
    // ------------------------------------------------------------------

    @FXML
    private void agregarArchivos() {
        List<File> archivos = ArchivoMultimediaService
                .selectorAudio(gestor.getDatos().getConfiguracion().getUltimaCarpetaMusica())
                .showOpenMultipleDialog(ventana());
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        agregar(archivos);
        gestor.recordarCarpetaMusica(archivos.get(0).getParent());
        refrescar();
    }

    @FXML
    private void agregarCarpeta() {
        DirectoryChooser selector = ArchivoMultimediaService.selectorCarpeta(
                gestor.getDatos().getConfiguracion().getUltimaCarpetaMusica(),
                "Seleccionar carpeta con música");
        File carpeta = selector.showDialog(ventana());
        if (carpeta == null) {
            return;
        }
        List<File> archivos = ArchivoMultimediaService.buscarArchivosAudio(carpeta);
        if (archivos.isEmpty()) {
            Dialogos.info("Sin archivos de audio",
                    "La carpeta seleccionada no contiene archivos MP3, WAV, M4A o AAC.");
            return;
        }
        agregar(archivos);
        gestor.recordarCarpetaMusica(carpeta.getAbsolutePath());
        refrescar();
    }

    private void agregar(List<File> archivos) {
        List<PistaAudio> nuevas = new ArrayList<>();
        for (File archivo : archivos) {
            nuevas.add(new PistaAudio(tituloDe(archivo.getName()), archivo.getAbsolutePath()));
        }
        gestor.agregarPistas(nuevas);
    }

    /** Agrega a la biblioteca un enlace externo (YouTube u otro) para abrirlo en el navegador. */
    @FXML
    private void agregarEnlace() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Añadir enlace");
        dialogo.initOwner(ventana());

        DialogPane panel = dialogo.getDialogPane();
        ButtonType aceptar = new ButtonType("Añadir", ButtonBar.ButtonData.OK_DONE);
        panel.getButtonTypes().addAll(aceptar, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField campoEnlace = new TextField();
        campoEnlace.setPromptText("https://www.youtube.com/watch?v=...");
        campoEnlace.setPrefWidth(360);
        TextField campoTitulo = new TextField();
        campoTitulo.setPromptText("Título (opcional)");

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(6, 4, 4, 4));
        formulario.add(new Label("Enlace"), 0, 0);
        formulario.add(campoEnlace, 1, 0);
        formulario.add(new Label("Título"), 0, 1);
        formulario.add(campoTitulo, 1, 1);
        panel.setContent(formulario);
        panel.setPrefWidth(520);
        Dialogos.aplicarEstilo(panel);

        Node nodoAceptar = panel.lookupButton(aceptar);
        nodoAceptar.setDisable(true);
        campoEnlace.textProperty().addListener((obs, anterior, actual) ->
                nodoAceptar.setDisable(actual == null || actual.isBlank()));

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isEmpty() || resultado.get() != aceptar) {
            return;
        }
        String enlace = campoEnlace.getText().trim();
        String titulo = campoTitulo.getText() == null || campoTitulo.getText().isBlank()
                ? ArchivoMultimediaService.tipoEnlace(enlace) : campoTitulo.getText().trim();
        gestor.agregarPista(PistaAudio.desdeEnlace(titulo, enlace));
        refrescar();
    }

    /** Descripcion breve del origen de la pista, mostrada en la lista. */
    private static String origenDe(PistaAudio pista) {
        return pista.esEnlace() ? ArchivoMultimediaService.tipoEnlace(pista.getEnlace()) : "Archivo local";
    }

    @FXML
    private void eliminarPista() {
        PistaAudio pista = listaPistas.getSelectionModel().getSelectedItem();
        if (pista == null) {
            return;
        }
        if (pista == pistaActual) {
            detener();
            limpiarReproductor();
        }
        gestor.eliminarPista(pista);
        refrescar();
    }

    @FXML
    private void vaciarBiblioteca() {
        if (gestor.getDatos().getBibliotecaMusical().isEmpty()) {
            return;
        }
        if (Dialogos.confirmar("Vaciar biblioteca",
                "¿Quitar todas las pistas de la biblioteca? Los archivos no se eliminan del disco.",
                ventana())) {
            detener();
            limpiarReproductor();
            gestor.limpiarBiblioteca();
            refrescar();
        }
    }

    // ------------------------------------------------------------------
    // Reproduccion
    // ------------------------------------------------------------------

    @FXML
    private void alternarReproduccion() {
        if (reproductor == null) {
            PistaAudio pista = listaPistas.getSelectionModel().getSelectedItem();
            if (pista == null && !listaPistas.getItems().isEmpty()) {
                pista = listaPistas.getItems().get(0);
            }
            if (pista == null) {
                Dialogos.info("Biblioteca vacía", "Añade archivos de audio antes de reproducir.");
                return;
            }
            reproducir(pista);
            return;
        }
        if (reproductor.getStatus() == MediaPlayer.Status.PLAYING) {
            reproductor.pause();
            espectro.desactivar();
            actualizarBotonReproducir(false);
        } else {
            reproductor.play();
            espectro.activar();
            actualizarBotonReproducir(true);
        }
    }

    @FXML
    private void siguiente() {
        List<PistaAudio> pistas = listaPistas.getItems();
        if (pistas.isEmpty()) {
            return;
        }
        reproducir(pistas.get(indiceSiguiente()));
    }

    @FXML
    private void anterior() {
        List<PistaAudio> pistas = listaPistas.getItems();
        if (pistas.isEmpty()) {
            return;
        }
        int indice = indiceDe(pistaActual);
        int anterior = indice <= 0 ? pistas.size() - 1 : indice - 1;
        reproducir(pistas.get(anterior));
    }

    /** Indice de la siguiente pista respetando el modo aleatorio. */
    private int indiceSiguiente() {
        List<PistaAudio> pistas = listaPistas.getItems();
        int indice = indiceDe(pistaActual);
        if (aleatorio && pistas.size() > 1) {
            int azar;
            do {
                azar = (int) (Math.random() * pistas.size());
            } while (azar == indice);
            return azar;
        }
        return indice < 0 ? 0 : (indice + 1) % pistas.size();
    }

    /** Avance automatico al terminar una pista, segun el modo de repeticion activo. */
    private void avanzarAutomaticamente() {
        if (modoRepeticion == ModoRepeticion.UNA && reproductor != null) {
            reproductor.seek(Duration.ZERO);
            reproductor.play();
            return;
        }
        List<PistaAudio> pistas = listaPistas.getItems();
        if (pistas.isEmpty()) {
            return;
        }
        int indice = indiceDe(pistaActual);
        if (aleatorio && pistas.size() > 1) {
            reproducir(pistas.get(indiceSiguiente()));
            return;
        }
        if (indice >= 0 && indice < pistas.size() - 1) {
            reproducir(pistas.get(indice + 1));
        } else if (modoRepeticion == ModoRepeticion.TODAS) {
            reproducir(pistas.get(0));
        } else {
            botonReproducir.setText("");
            actualizarBotonReproducir(false);
            espectro.desactivar();
            sliderProgreso.setValue(sliderProgreso.getMax());
        }
    }

    private void reproducir(PistaAudio pista) {
        if (pista == null) {
            return;
        }
        if (pista.esEnlace()) {
            abrirEnlace(pista);
            return;
        }
        if (!ArchivoMultimediaService.existe(pista.getRutaArchivo())) {
            Dialogos.info("Archivo no disponible",
                    "No se encontró el archivo:\n" + pista.getRutaArchivo());
            return;
        }
        detener();
        try {
            Media media = new Media(Path.of(pista.getRutaArchivo()).toUri().toString());
            reproductor = new MediaPlayer(media);
            reproductor.setVolume(sliderVolumen.getValue());

            // El espectro reacciona al audio real del archivo local.
            reproductor.setAudioSpectrumNumBands(64);
            reproductor.setAudioSpectrumInterval(0.05);
            reproductor.setAudioSpectrumThreshold(-60);
            reproductor.setAudioSpectrumListener((marca, duracion, magnitudes, fases) -> {
                double[] datos = new double[magnitudes.length];
                for (int i = 0; i < magnitudes.length; i++) {
                    datos[i] = magnitudes[i];
                }
                espectro.actualizar(datos);
            });

            reproductor.setOnReady(() -> {
                double segundos = media.getDuration().toSeconds();
                sliderProgreso.setMax(Math.max(1, segundos));
                etiquetaTiempoTotal.setText(formatear(segundos));
                pista.setDuracionSegundos(segundos);
                gestor.guardarAutomatico();
                listaPistas.refresh();
            });
            reproductor.currentTimeProperty().addListener((obs, antes, ahora) -> {
                if (!arrastrando) {
                    sliderProgreso.setValue(ahora.toSeconds());
                }
                etiquetaTiempoActual.setText(formatear(ahora.toSeconds()));
            });
            reproductor.setOnEndOfMedia(this::avanzarAutomaticamente);
            reproductor.setOnError(() -> Dialogos.info("Reproducción",
                    "JavaFX no pudo reproducir este archivo. Se recomienda MP3 o WAV estándar."));
            reproductor.play();
            espectro.activar();

            pistaActual = pista;
            sliderProgreso.setValue(0);
            etiquetaTiempoActual.setText("00:00");
            etiquetaTiempoTotal.setText(pista.getDuracionFormateada());
            etiquetaTituloPista.setText(pista.getTitulo());
            etiquetaArtista.setText(pista.getArtista() == null || pista.getArtista().isBlank()
                    ? ArchivoMultimediaService.nombreArchivo(pista.getRutaArchivo())
                    : pista.getArtista());
            mostrarPortadaActual(pista);
            actualizarBotonReproducir(true);
            listaPistas.getSelectionModel().select(pista);
        } catch (RuntimeException e) {
            Dialogos.error("Reproducción", "No se pudo abrir el archivo seleccionado.\n" + e.getMessage());
        }
    }

    /** Reproduce un enlace externo abriendolo en el navegador del sistema. */
    private void abrirEnlace(PistaAudio pista) {
        detener();
        MainApp.abrirEnlace(pista.getEnlace());
        pistaActual = pista;
        sliderProgreso.setMax(100);
        sliderProgreso.setValue(0);
        etiquetaTiempoActual.setText("00:00");
        etiquetaTiempoTotal.setText("—");
        etiquetaTituloPista.setText(pista.getTitulo());
        etiquetaArtista.setText(pista.getEnlace());
        mostrarPortadaActual(pista);
        // El audio suena en el navegador: el espectro usa su patron animado.
        espectro.activar();
        actualizarBotonReproducir(false);
        listaPistas.getSelectionModel().select(pista);
    }

    private void detener() {
        if (reproductor != null) {
            try {
                reproductor.stop();
                reproductor.dispose();
            } catch (RuntimeException ignored) {
                // Liberacion silenciosa de recursos de audio.
            }
            reproductor = null;
        }
    }

    /** Restablece la tarjeta del reproductor cuando no hay ninguna pista activa. */
    private void limpiarReproductor() {
        pistaActual = null;
        espectro.desactivar();
        etiquetaTituloPista.setText("Sin pista en reproducción");
        etiquetaArtista.setText("");
        mostrarPortadaActual(null);
        actualizarBotonReproducir(false);
    }

    private int indiceDe(PistaAudio pista) {
        if (pista == null) {
            return -1;
        }
        List<PistaAudio> pistas = listaPistas.getItems();
        for (int i = 0; i < pistas.size(); i++) {
            if (pistas.get(i).getId().equals(pista.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void actualizarEtiquetaVolumen() {
        etiquetaVolumen.setText(Math.round(sliderVolumen.getValue() * 100) + "%");
    }

    private static String tituloDe(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto > 0 ? nombreArchivo.substring(0, punto) : nombreArchivo;
    }

    private static String formatear(double segundosTotales) {
        int total = (int) Math.max(0, Math.round(segundosTotales));
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private Window ventana() {
        return listaPistas.getScene() == null ? null : listaPistas.getScene().getWindow();
    }
}
