package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.OrigenPista;
import com.symmetry.desktop.model.PistaAudio;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.DescargadorAudioService;
import com.symmetry.desktop.service.DuracionPistaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.GestorHerramientasService;
import com.symmetry.desktop.service.ReproductorService;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.EspectroAudio;
import com.symmetry.desktop.util.IconosMedia;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Modulo 4 - Reproductor de musica: biblioteca de pistas MP3/WAV y enlaces, controles de
 * reproduccion con modo aleatorio y repeticion, espectro de audio animado, miniaturas de los
 * videos, reordenacion por arrastrar y soltar, borrado individual por fila y descarga y
 * conversion de enlaces a MP3 dentro de la propia aplicacion.
 *
 * <p>La reproduccion se delega en el {@link ReproductorService} global, de modo que la musica
 * sigue sonando al cambiar de seccion a traves del mini-reproductor persistente.</p>
 */
public class MusicaController implements Initializable, VistaConRefresco {

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final ReproductorService servicio = ReproductorService.getInstancia();
    private final EspectroAudio espectro = new EspectroAudio();

    private PistaAudio pistaArrastrada;
    private boolean descargando;

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
    private TableColumn<PistaAudio, Void> columnaAcciones;
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
    private Button botonActualizarMotor;
    @FXML
    private HBox panelDescarga;
    @FXML
    private ProgressBar progresoDescarga;
    @FXML
    private Label etiquetaDescarga;
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
    @FXML
    private Label etiquetaModoReproduccion;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        columnaTitulo.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getTitulo()));
        columnaArtista.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getArtista() == null || datos.getValue().getArtista().isBlank()
                        ? "—" : datos.getValue().getArtista()));
        columnaDuracion.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getDuracionFormateada()));
        columnaOrigen.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getOrigen().getEtiqueta()));
        columnaPortada.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        columnaPortada.setCellFactory(columna -> celdaPortada());
        columnaAcciones.setCellValueFactory(datos -> null);
        columnaAcciones.setCellFactory(columna -> celdaAcciones());
        listaPistas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        listaPistas.setPlaceholder(new Label(
                "La biblioteca está vacía. Añade archivos MP3/WAV, suéltalos aquí o descarga un enlace."));
        listaPistas.setRowFactory(tabla -> filaReproducible());
        configurarArrastreExterno();
        // Enter reproduce la pista seleccionada (ademas del doble clic).
        listaPistas.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ENTER && !evento.isConsumed()) {
                PistaAudio seleccionada = listaPistas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    iniciar(seleccionada);
                    evento.consume();
                }
            }
        });
        listaPistas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> botonEliminarPista.setDisable(actual == null));

        configurarEspectro();
        configurarPortada();
        configurarControles();
        configurarEnlacesGlobales();

        sliderVolumen.valueProperty().bindBidirectional(servicio.volumenProperty());
        sliderVolumen.valueProperty().addListener((obs, anterior, actual) -> actualizarEtiquetaVolumen());
        actualizarEtiquetaVolumen();

        sliderProgreso.setOnMousePressed(evento -> servicio.setArrastrandoProgreso(true));
        sliderProgreso.setOnMouseReleased(evento -> {
            servicio.setArrastrandoProgreso(false);
            servicio.seek(sliderProgreso.getValue());
        });

        botonEliminarPista.setDisable(true);
        refrescar();
    }

    @Override
    public void refrescar() {
        listaPistas.setItems(FXCollections.observableArrayList(gestor.getDatos().getBibliotecaMusical()));
        etiquetaBiblioteca.setText(gestor.getDatos().getBibliotecaMusical().size()
                + " pistas · .mp3, .wav, .m4a, .aac o enlaces");
        // Duracion real (mm:ss) de las pistas locales que aun no la tienen, sin esperar a reproducirlas.
        DuracionPistaService.completar(gestor.getDatos().getBibliotecaMusical(), listaPistas::refresh);
    }

    // ------------------------------------------------------------------
    // Espectro, portada y controles multimedia
    // ------------------------------------------------------------------

    /** Coloca el visualizador de audio en su zona y lo alimenta con el espectro global. */
    private void configurarEspectro() {
        zonaEspectro.getChildren().setAll(espectro);
        servicio.setEscuchaEspectro(datos -> espectro.actualizar(datos));
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
        botonAnterior.setGraphic(IconosMedia.icono(IconosMedia.ANTERIOR, 22));
        botonAnterior.setTooltip(new Tooltip("Pista anterior"));
        botonSiguiente.setGraphic(IconosMedia.icono(IconosMedia.SIGUIENTE, 22));
        botonSiguiente.setTooltip(new Tooltip("Pista siguiente"));
        botonAleatorio.setGraphic(IconosMedia.icono(IconosMedia.ALEATORIO, 20));
        actualizarBotonAleatorio();
        actualizarBotonRepeticion();
        actualizarBotonReproducir(servicio.isReproduciendo());
        servicio.setEscuchaError(mensaje -> Dialogos.info("Reproducción",
                "JavaFX no pudo reproducir este archivo. Se recomienda MP3 o WAV estándar.\n" + mensaje));
    }

    /** Enlaza la tarjeta del reproductor con el estado del reproductor global. */
    private void configurarEnlacesGlobales() {
        servicio.pistaActualProperty().addListener((obs, antes, ahora) -> actualizarPistaMostrada(ahora));
        servicio.tituloProperty().addListener((obs, antes, ahora) -> etiquetaTituloPista.setText(
                ahora == null || ahora.isBlank() ? "Sin pista en reproducción" : ahora));
        servicio.artistaProperty().addListener((obs, antes, ahora) ->
                etiquetaArtista.setText(ahora == null ? "" : ahora));
        servicio.reproduciendoProperty().addListener((obs, antes, ahora) -> {
            actualizarBotonReproducir(ahora);
            if (ahora) {
                espectro.activar();
            } else {
                espectro.desactivar();
            }
        });
        servicio.progresoSegundosProperty().addListener((obs, antes, ahora) -> {
            sliderProgreso.setValue(ahora.doubleValue());
            etiquetaTiempoActual.setText(formatear(ahora.doubleValue()));
        });
        servicio.duracionSegundosProperty().addListener((obs, antes, ahora) -> {
            sliderProgreso.setMax(Math.max(1, ahora.doubleValue()));
            etiquetaTiempoTotal.setText(formatear(ahora.doubleValue()));
        });
        servicio.aleatorioProperty().addListener((obs, antes, ahora) -> actualizarBotonAleatorio());
        servicio.modoRepeticionProperty().addListener((obs, antes, ahora) -> actualizarBotonRepeticion());

        // Estado inicial: la pista pudo quedar activa desde otra seccion o desde el mini-reproductor.
        PistaAudio actual = servicio.getPistaActual();
        actualizarPistaMostrada(actual);
        sliderProgreso.setMax(Math.max(1, servicio.getDuracionSegundos()));
        sliderProgreso.setValue(servicio.getProgresoSegundos());
        etiquetaTiempoActual.setText(formatear(servicio.getProgresoSegundos()));
        etiquetaTiempoTotal.setText(formatear(servicio.getDuracionSegundos()));
        if (servicio.isReproduciendo()) {
            espectro.activar();
        }
    }

    private void actualizarBotonReproducir(boolean reproduciendo) {
        botonReproducir.setGraphic(IconosMedia.icono(
                reproduciendo ? IconosMedia.PAUSA : IconosMedia.REPRODUCIR, 26));
        botonReproducir.setTooltip(new Tooltip(reproduciendo ? "Pausar" : "Reproducir"));
    }

    private void actualizarBotonAleatorio() {
        boolean activo = servicio.isAleatorio();
        botonAleatorio.getStyleClass().remove("boton-media-activo");
        if (activo) {
            botonAleatorio.getStyleClass().add("boton-media-activo");
        }
        botonAleatorio.setTooltip(new Tooltip(activo
                ? "Aleatorio activado (pulsa para desactivar)" : "Activar modo aleatorio"));
        actualizarEtiquetaModo();
    }

    private void actualizarBotonRepeticion() {
        ReproductorService.ModoRepeticion modo = servicio.getModoRepeticion();
        botonRepeticion.setGraphic(IconosMedia.icono(
                modo == ReproductorService.ModoRepeticion.UNA
                        ? IconosMedia.REPETIR_UNA : IconosMedia.REPETIR, 20));
        botonRepeticion.getStyleClass().remove("boton-media-activo");
        if (modo != ReproductorService.ModoRepeticion.DESACTIVADO) {
            botonRepeticion.getStyleClass().add("boton-media-activo");
        }
        botonRepeticion.setTooltip(new Tooltip(switch (modo) {
            case DESACTIVADO -> "Repetición desactivada (pulsa para activar el bucle de lista)";
            case TODAS -> "Bucle de lista activado (pulsa para repetir solo esta pista)";
            case UNA -> "Repetir esta pista (pulsa para desactivar)";
        }));
        actualizarEtiquetaModo();
    }

    /** Rotulo bajo los controles: el modo activo, siempre visible y coherente con el mini-reproductor. */
    private void actualizarEtiquetaModo() {
        etiquetaModoReproduccion.setText("Modo: " + servicio.descripcionModo());
        boolean especial = servicio.isAleatorio()
                || servicio.getModoRepeticion() != ReproductorService.ModoRepeticion.DESACTIVADO;
        etiquetaModoReproduccion.getStyleClass().remove("etiqueta-modo-activo");
        if (especial) {
            etiquetaModoReproduccion.getStyleClass().add("etiqueta-modo-activo");
        }
    }

    @FXML
    private void alternarAleatorio() {
        servicio.alternarAleatorio();
    }

    @FXML
    private void cambiarRepeticion() {
        servicio.ciclarRepeticion();
    }

    // ------------------------------------------------------------------
    // Lista de reproduccion: filas, acciones por fila y arrastrar y soltar
    // ------------------------------------------------------------------

    /** Fila de la biblioteca: doble clic para reproducir y arrastre para reordenar. */
    private TableRow<PistaAudio> filaReproducible() {
        TableRow<PistaAudio> fila = new TableRow<>();

        fila.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2 && fila.getItem() != null) {
                iniciar(fila.getItem());
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
            if (evento.getDragboard().hasFiles()) {
                // Suelta de archivos externos: la gestiona la tabla completa.
                return;
            }
            if (puedeSoltarEn(fila)) {
                evento.acceptTransferModes(TransferMode.MOVE);
                evento.consume();
            }
        });

        fila.setOnDragEntered(evento -> {
            if (!evento.getDragboard().hasFiles() && puedeSoltarEn(fila)) {
                fila.getStyleClass().add("fila-destino");
            }
        });

        fila.setOnDragExited(evento -> fila.getStyleClass().remove("fila-destino"));

        fila.setOnDragDropped(evento -> {
            if (evento.getDragboard().hasFiles()) {
                return;
            }
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

    /** Admite archivos de audio (.mp3, .wav, .m4a, .aac) soltados desde el explorador. */
    private void configurarArrastreExterno() {
        listaPistas.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles() && contieneAudio(evento.getDragboard().getFiles())) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });

        listaPistas.setOnDragDropped(evento -> {
            List<File> archivos = evento.getDragboard().getFiles();
            List<File> audio = archivos == null ? List.of() : archivos.stream()
                    .filter(archivo -> archivo.isFile()
                            && ArchivoMultimediaService.esAudio(archivo.getName()))
                    .toList();
            if (audio.isEmpty()) {
                evento.setDropCompleted(false);
            } else {
                agregar(audio);
                refrescar();
                evento.setDropCompleted(true);
            }
            evento.consume();
        });
    }

    private static boolean contieneAudio(List<File> archivos) {
        if (archivos == null) {
            return false;
        }
        for (File archivo : archivos) {
            if (archivo.isFile() && ArchivoMultimediaService.esAudio(archivo.getName())) {
                return true;
            }
        }
        return false;
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

    /** Celda de la columna de portada: miniatura del video o icono de musica. */
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
                Image imagen = ArchivoMultimediaService.portada(pista);
                vista.setImage(imagen);
                vista.setVisible(imagen != null);
                simbolo.setVisible(imagen == null);
                simbolo.setText(pista.esEnlace() ? "\u25B6" : "\u266B");
                setGraphic(marco);
            }
        };
    }

    /** Celda de la columna de acciones: borra la pista de esa fila con un solo clic. */
    private TableCell<PistaAudio, Void> celdaAcciones() {
        return new TableCell<>() {
            private final Button boton = new Button("\u2715");

            {
                boton.getStyleClass().add("boton-accion-eliminar");
                boton.setTooltip(new Tooltip("Eliminar esta pista de la lista"));
                boton.setOnAction(evento -> {
                    int indice = getIndex();
                    if (indice >= 0 && indice < getTableView().getItems().size()) {
                        eliminarDeLista(getTableView().getItems().get(indice));
                    }
                });
            }

            @Override
            protected void updateItem(Void elemento, boolean vacio) {
                super.updateItem(elemento, vacio);
                setGraphic(vacio ? null : boton);
            }
        };
    }

    private void mostrarPortadaActual(PistaAudio pista) {
        Image imagen = ArchivoMultimediaService.portada(pista);
        portadaActual.setImage(imagen);
        portadaActual.setVisible(imagen != null);
        simboloPortada.setVisible(imagen == null);
    }

    /** Refleja en la tarjeta la pista que esta sonando y la selecciona en la lista. */
    private void actualizarPistaMostrada(PistaAudio pista) {
        if (pista == null) {
            etiquetaTituloPista.setText("Sin pista en reproducción");
            etiquetaArtista.setText("");
            mostrarPortadaActual(null);
            return;
        }
        etiquetaTituloPista.setText(pista.getTitulo());
        etiquetaArtista.setText(servicio.getArtista());
        mostrarPortadaActual(pista);
        if (listaPistas.getItems().contains(pista)) {
            listaPistas.getSelectionModel().select(pista);
        }
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

    /** Descarga el audio del enlace a la carpeta gestionada y lo agrega como pista local. */
    @FXML
    private void agregarEnlace() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Descargar enlace de audio");
        dialogo.initOwner(ventana());

        DialogPane panel = dialogo.getDialogPane();
        ButtonType aceptar = new ButtonType("Descargar", ButtonBar.ButtonData.OK_DONE);
        panel.getButtonTypes().addAll(aceptar, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));

        TextField campoEnlace = new TextField();
        campoEnlace.setPromptText("https://www.youtube.com/watch?v=...");
        campoEnlace.setPrefWidth(360);
        TextField campoTitulo = new TextField();
        campoTitulo.setPromptText("Título (opcional)");

        Label nota = new Label("El audio se descargará y convertirá a MP3 en la carpeta gestionada "
                + "por la aplicación y se añadirá a la lista, sin abrir el navegador. "
                + "La primera vez, las herramientas necesarias se preparan automáticamente.");
        nota.setWrapText(true);
        nota.setMaxWidth(360);
        nota.getStyleClass().add("texto-secundario");

        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(6, 4, 4, 4));
        formulario.add(new Label("Enlace"), 0, 0);
        formulario.add(campoEnlace, 1, 0);
        formulario.add(new Label("Título"), 0, 1);
        formulario.add(campoTitulo, 1, 1);
        formulario.add(nota, 1, 2);
        panel.setContent(formulario);
        panel.setPrefWidth(540);
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
        if (!ArchivoMultimediaService.esUrl(enlace)) {
            Dialogos.info("Enlace no válido", "Introduce una dirección http(s) válida.");
            return;
        }
        String titulo = campoTitulo.getText() == null || campoTitulo.getText().isBlank()
                ? null : campoTitulo.getText().trim();
        descargarEnlace(enlace, titulo);
    }

    /**
     * Prepara las herramientas necesarias (yt-dlp y ffmpeg, descargándolas la primera vez) y,
     * a continuación, descarga y convierte el enlace a MP3 en segundo plano. Todo el proceso
     * se refleja dentro de la propia aplicación, sin ventanas de consola ni instalaciones
     * manuales por parte del usuario.
     */
    private void descargarEnlace(String enlace, String titulo) {
        if (descargando) {
            Dialogos.info("Descarga en curso", "Espera a que finalice la descarga actual.");
            return;
        }
        descargando = true;
        mostrarPanelDescarga(true);
        progresoDescarga.setProgress(-1);
        etiquetaDescarga.setText("Preparando la descarga…");

        GestorHerramientasService.asegurar(
                (fraccion, mensaje) -> {
                    etiquetaDescarga.setText(mensaje);
                    progresoDescarga.setProgress(fraccion);
                },
                () -> convertirEnlace(enlace, titulo),
                error -> {
                    descargando = false;
                    mostrarPanelDescarga(false);
                    mostrarFalloDescarga("No se pudo preparar la descarga", error);
                });
    }

    /** Ejecuta la descarga y conversión del enlace una vez las herramientas están listas. */
    private void convertirEnlace(String enlace, String titulo) {
        progresoDescarga.setProgress(-1);
        etiquetaDescarga.setText("Conectando con el servidor…");
        DescargadorAudioService.descargar(enlace, gestor.getCarpetaMusica(),
                (fraccion, mensaje) -> {
                    etiquetaDescarga.setText(mensaje);
                    progresoDescarga.setProgress(fraccion);
                },
                resultado -> {
                    descargando = false;
                    mostrarPanelDescarga(false);
                    PistaAudio pista = new PistaAudio(
                            resultado.titulo() == null || resultado.titulo().isBlank()
                                    ? tituloDe(resultado.archivo().getName()) : resultado.titulo(),
                            resultado.archivo().getAbsolutePath());
                    pista.setOrigen(OrigenPista.DESCARGADO);
                    if (titulo != null && !titulo.isBlank()) {
                        pista.setTitulo(titulo);
                    }
                    pista.setArtista(resultado.artista() == null || resultado.artista().isBlank()
                            ? ArchivoMultimediaService.tipoEnlace(enlace) : resultado.artista());
                    pista.setDuracionSegundos(resultado.duracionSegundos());
                    gestor.agregarPista(pista);
                    refrescar();
                    iniciar(pista);
                },
                error -> {
                    descargando = false;
                    mostrarPanelDescarga(false);
                    mostrarFalloDescarga("No se pudo descargar el enlace", error);
                });
    }

    /** Actualiza yt-dlp a su ultima version oficial bajo demanda. */
    @FXML
    private void actualizarMotor() {
        if (descargando) {
            Dialogos.info("Operación en curso", "Espera a que finalice la descarga actual.");
            return;
        }
        descargando = true;
        botonActualizarMotor.setDisable(true);
        mostrarPanelDescarga(true);
        progresoDescarga.setProgress(-1);
        etiquetaDescarga.setText("Actualizando el motor de descarga…");
        GestorHerramientasService.actualizarYtDlp(
                (fraccion, mensaje) -> {
                    etiquetaDescarga.setText(mensaje);
                    progresoDescarga.setProgress(fraccion);
                },
                mensaje -> {
                    terminarOperacionMotor();
                    Dialogos.info("Motor de descarga", mensaje);
                },
                error -> {
                    terminarOperacionMotor();
                    mostrarFalloDescarga("No se pudo actualizar el motor", error);
                });
    }

    private void terminarOperacionMotor() {
        descargando = false;
        botonActualizarMotor.setDisable(false);
        mostrarPanelDescarga(false);
    }

    /**
     * Muestra el fallo con salidas claras: reintentar tras actualizar el motor o seguir con
     * archivos locales, de modo que el usuario nunca quede bloqueado por un servidor externo.
     */
    private void mostrarFalloDescarga(String titulo, String mensaje) {
        ButtonType actualizar = new ButtonType("Actualizar motor", ButtonBar.ButtonData.LEFT);
        ButtonType archivos = new ButtonType("Añadir archivos", ButtonBar.ButtonData.OTHER);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, actualizar, archivos, cerrar);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.initOwner(ventana());
        Dialogos.aplicarEstilo(alerta.getDialogPane());
        Optional<ButtonType> eleccion = alerta.showAndWait();
        if (eleccion.isPresent()) {
            if (eleccion.get() == actualizar) {
                actualizarMotor();
            } else if (eleccion.get() == archivos) {
                agregarArchivos();
            }
        }
    }

    private void mostrarPanelDescarga(boolean mostrar) {
        panelDescarga.setVisible(mostrar);
        panelDescarga.setManaged(mostrar);
    }

    @FXML
    private void eliminarPista() {
        eliminarDeLista(listaPistas.getSelectionModel().getSelectedItem());
    }

    /** Quita una pista concreta de la biblioteca; si es la que suena, detiene el reproductor. */
    private void eliminarDeLista(PistaAudio pista) {
        if (pista == null) {
            return;
        }
        if (pista == servicio.getPistaActual()) {
            servicio.detener();
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
            servicio.detener();
            gestor.limpiarBiblioteca();
            refrescar();
        }
    }

    // ------------------------------------------------------------------
    // Reproduccion
    // ------------------------------------------------------------------

    @FXML
    private void alternarReproduccion() {
        if (!servicio.isActivo()) {
            PistaAudio pista = listaPistas.getSelectionModel().getSelectedItem();
            if (pista == null && !listaPistas.getItems().isEmpty()) {
                pista = listaPistas.getItems().get(0);
            }
            if (pista == null) {
                Dialogos.info("Biblioteca vacía", "Añade archivos de audio antes de reproducir.");
                return;
            }
            iniciar(pista);
            return;
        }
        servicio.alternar();
    }

    @FXML
    private void siguiente() {
        servicio.siguiente();
    }

    @FXML
    private void anterior() {
        servicio.anterior();
    }

    /** Arranca una pista: los enlaces pendientes se descargan y los archivos suenan al momento. */
    private void iniciar(PistaAudio pista) {
        if (pista == null) {
            return;
        }
        if (pista.esEnlace()) {
            descargarEnlace(pista.getEnlace(), pista.getTitulo());
            return;
        }
        if (!ArchivoMultimediaService.existe(pista.getRutaArchivo())) {
            Dialogos.info("Archivo no disponible",
                    "No se encontró el archivo:\n" + pista.getRutaArchivo());
            return;
        }
        servicio.reproducir(pista);
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
