package com.bey.desktop.controller;

import com.bey.desktop.model.beyOrigenPista;
import com.bey.desktop.model.beyPistaAudio;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyDescargadorAudioService;
import com.bey.desktop.service.beyDuracionPistaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beyGestorHerramientasService;
import com.bey.desktop.service.beyReproductorService;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyEspectroAudio;
import com.bey.desktop.util.beyIconosMedia;
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

public class beyMusicaController implements Initializable, beyVistaConRefresco {
    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();
    private final beyReproductorService beyServicio = beyReproductorService.beyGetInstancia();
    private final beyEspectroAudio beyEspectro = new beyEspectroAudio();

    private beyPistaAudio beyPistaArrastrada;
    private boolean beyDescargando;

    @FXML
    private TableView<beyPistaAudio> beyListaPistas;
    @FXML
    private TableColumn<beyPistaAudio, beyPistaAudio> beyColumnaPortada;
    @FXML
    private TableColumn<beyPistaAudio, String> beyColumnaTitulo;
    @FXML
    private TableColumn<beyPistaAudio, String> beyColumnaArtista;
    @FXML
    private TableColumn<beyPistaAudio, String> beyColumnaOrigen;
    @FXML
    private TableColumn<beyPistaAudio, String> beyColumnaDuracion;
    @FXML
    private TableColumn<beyPistaAudio, Void> beyColumnaAcciones;
    @FXML
    private Button beyBotonEliminarPista;
    @FXML
    private Button beyBotonAleatorio;
    @FXML
    private Button beyBotonAnterior;
    @FXML
    private Button beyBotonReproducir;
    @FXML
    private Button beyBotonSiguiente;
    @FXML
    private Button beyBotonRepeticion;
    @FXML
    private Button beyBotonActualizarMotor;
    @FXML
    private HBox beyPanelDescarga;
    @FXML
    private ProgressBar beyProgresoDescarga;
    @FXML
    private Label beyEtiquetaDescarga;
    @FXML
    private HBox beyZonaEspectro;
    @FXML
    private StackPane beyMarcoPortada;
    @FXML
    private ImageView beyPortadaActual;
    @FXML
    private Label beySimboloPortada;
    @FXML
    private Label beyEtiquetaBiblioteca;
    @FXML
    private Label beyEtiquetaTituloPista;
    @FXML
    private Label beyEtiquetaArtista;
    @FXML
    private Label beyEtiquetaTiempoActual;
    @FXML
    private Label beyEtiquetaTiempoTotal;
    @FXML
    private Slider beySliderProgreso;
    @FXML
    private Slider beySliderVolumen;
    @FXML
    private Label beyEtiquetaVolumen;
    @FXML
    private Label beyEtiquetaModoReproduccion;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyColumnaTitulo.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getTitulo()));
        beyColumnaArtista.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getArtista() == null || datos.getValue().getArtista().isBlank()
                        ? "—" : datos.getValue().getArtista()));
        beyColumnaDuracion.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getDuracionFormateada()));
        beyColumnaOrigen.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getOrigen().getEtiqueta()));
        beyColumnaPortada.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue()));
        beyColumnaPortada.setCellFactory(columna -> beyCeldaPortada());
        beyColumnaAcciones.setCellValueFactory(datos -> null);
        beyColumnaAcciones.setCellFactory(columna -> beyCeldaAcciones());
        beyListaPistas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        beyListaPistas.setPlaceholder(new Label(
                "La biblioteca está vacía. Añade archivos MP3/WAV, suéltalos aquí o descarga un enlace."));
        beyListaPistas.setRowFactory(tabla -> beyFilaReproducible());
        beyConfigurarArrastreExterno();

        beyListaPistas.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() == KeyCode.ENTER && !evento.isConsumed()) {
                beyPistaAudio seleccionada = beyListaPistas.getSelectionModel().getSelectedItem();
                if (seleccionada != null) {
                    beyIniciar(seleccionada);
                    evento.consume();
                }
            }
        });
        beyListaPistas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> beyBotonEliminarPista.setDisable(actual == null));

        beyConfigurarEspectro();
        beyConfigurarPortada();
        beyConfigurarControles();
        beyConfigurarEnlacesGlobales();

        beySliderVolumen.valueProperty().bindBidirectional(beyServicio.beyVolumenProperty());
        beySliderVolumen.valueProperty().addListener((obs, anterior, actual) -> beyActualizarEtiquetaVolumen());
        beyActualizarEtiquetaVolumen();

        beySliderProgreso.setOnMousePressed(evento -> beyServicio.beySetArrastrandoProgreso(true));
        beySliderProgreso.setOnMouseReleased(evento -> {
            beyServicio.beySetArrastrandoProgreso(false);
            beyServicio.beySeek(beySliderProgreso.getValue());
        });

        beyBotonEliminarPista.setDisable(true);
        beyRefrescar();
    }

    @Override
    public void beyRefrescar() {
        beyListaPistas.setItems(FXCollections.observableArrayList(beyGestor.beyGetDatos().getBibliotecaMusical()));
        beyEtiquetaBiblioteca.setText(beyGestor.beyGetDatos().getBibliotecaMusical().size()
                + " pistas · .mp3, .wav, .m4a, .aac o enlaces");

        beyDuracionPistaService.beyCompletar(beyGestor.beyGetDatos().getBibliotecaMusical(), beyListaPistas::refresh);
    }

    private void beyConfigurarEspectro() {
        beyZonaEspectro.getChildren().setAll(beyEspectro);
        beyServicio.beySetEscuchaEspectro(datos -> beyEspectro.beyActualizar(datos));
    }

    private void beyConfigurarPortada() {
        Rectangle recorte = new Rectangle(76, 76);
        recorte.setArcWidth(16);
        recorte.setArcHeight(16);
        beyMarcoPortada.setClip(recorte);
        beyPortadaActual.setVisible(false);
    }

    private void beyConfigurarControles() {
        beyBotonAnterior.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.ANTERIOR, 22));
        beyBotonAnterior.setTooltip(new Tooltip("Pista anterior"));
        beyBotonSiguiente.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.SIGUIENTE, 22));
        beyBotonSiguiente.setTooltip(new Tooltip("Pista siguiente"));
        beyBotonAleatorio.setGraphic(beyIconosMedia.beyIcono(beyIconosMedia.ALEATORIO, 20));
        beyActualizarBotonAleatorio();
        beyActualizarBotonRepeticion();
        beyActualizarBotonReproducir(beyServicio.beyIsReproduciendo());
        beyServicio.beySetEscuchaError(mensaje -> beyDialogos.beyInfo("Reproducción",
                "JavaFX no pudo reproducir este archivo. Se recomienda MP3 o WAV estándar.\n" + mensaje));
    }

    private void beyConfigurarEnlacesGlobales() {
        beyServicio.beyPistaActualProperty().addListener((obs, antes, ahora) -> beyActualizarPistaMostrada(ahora));
        beyServicio.beyTituloProperty().addListener((obs, antes, ahora) -> beyEtiquetaTituloPista.setText(
                ahora == null || ahora.isBlank() ? "Sin pista en reproducción" : ahora));
        beyServicio.beyArtistaProperty().addListener((obs, antes, ahora) ->
                beyEtiquetaArtista.setText(ahora == null ? "" : ahora));
        beyServicio.beyReproduciendoProperty().addListener((obs, antes, ahora) -> {
            beyActualizarBotonReproducir(ahora);
            if (ahora) {
                beyEspectro.beyActivar();
            } else {
                beyEspectro.beyDesactivar();
            }
        });
        beyServicio.beyProgresoSegundosProperty().addListener((obs, antes, ahora) -> {
            beySliderProgreso.setValue(ahora.doubleValue());
            beyEtiquetaTiempoActual.setText(beyFormatear(ahora.doubleValue()));
        });
        beyServicio.beyDuracionSegundosProperty().addListener((obs, antes, ahora) -> {
            beySliderProgreso.setMax(Math.max(1, ahora.doubleValue()));
            beyEtiquetaTiempoTotal.setText(beyFormatear(ahora.doubleValue()));
        });
        beyServicio.beyAleatorioProperty().addListener((obs, antes, ahora) -> beyActualizarBotonAleatorio());
        beyServicio.beyModoRepeticionProperty().addListener((obs, antes, ahora) -> beyActualizarBotonRepeticion());

        beyPistaAudio actual = beyServicio.beyGetPistaActual();
        beyActualizarPistaMostrada(actual);
        beySliderProgreso.setMax(Math.max(1, beyServicio.beyGetDuracionSegundos()));
        beySliderProgreso.setValue(beyServicio.beyGetProgresoSegundos());
        beyEtiquetaTiempoActual.setText(beyFormatear(beyServicio.beyGetProgresoSegundos()));
        beyEtiquetaTiempoTotal.setText(beyFormatear(beyServicio.beyGetDuracionSegundos()));
        if (beyServicio.beyIsReproduciendo()) {
            beyEspectro.beyActivar();
        }
    }

    private void beyActualizarBotonReproducir(boolean reproduciendo) {
        beyBotonReproducir.setGraphic(beyIconosMedia.beyIcono(
                reproduciendo ? beyIconosMedia.PAUSA : beyIconosMedia.REPRODUCIR, 26));
        beyBotonReproducir.setTooltip(new Tooltip(reproduciendo ? "Pausar" : "Reproducir"));
    }

    private void beyActualizarBotonAleatorio() {
        boolean activo = beyServicio.beyIsAleatorio();
        beyBotonAleatorio.getStyleClass().remove("boton-media-activo");
        if (activo) {
            beyBotonAleatorio.getStyleClass().add("boton-media-activo");
        }
        beyBotonAleatorio.setTooltip(new Tooltip(activo
                ? "Aleatorio activado (pulsa para desactivar)" : "Activar modo aleatorio"));
        beyActualizarEtiquetaModo();
    }

    private void beyActualizarBotonRepeticion() {
        beyReproductorService.beyModoRepeticion modo = beyServicio.beyGetModoRepeticion();
        beyBotonRepeticion.setGraphic(beyIconosMedia.beyIcono(
                modo == beyReproductorService.beyModoRepeticion.UNA
                        ? beyIconosMedia.REPETIR_UNA : beyIconosMedia.REPETIR, 20));
        beyBotonRepeticion.getStyleClass().remove("boton-media-activo");
        if (modo != beyReproductorService.beyModoRepeticion.DESACTIVADO) {
            beyBotonRepeticion.getStyleClass().add("boton-media-activo");
        }
        beyBotonRepeticion.setTooltip(new Tooltip(switch (modo) {
            case DESACTIVADO -> "Repetición desactivada (pulsa para activar el bucle de lista)";
            case TODAS -> "Bucle de lista activado (pulsa para repetir solo esta pista)";
            case UNA -> "Repetir esta pista (pulsa para desactivar)";
        }));
        beyActualizarEtiquetaModo();
    }

    private void beyActualizarEtiquetaModo() {
        beyEtiquetaModoReproduccion.setText("Modo: " + beyServicio.beyDescripcionModo());
        boolean especial = beyServicio.beyIsAleatorio()
                || beyServicio.beyGetModoRepeticion() != beyReproductorService.beyModoRepeticion.DESACTIVADO;
        beyEtiquetaModoReproduccion.getStyleClass().remove("etiqueta-modo-activo");
        if (especial) {
            beyEtiquetaModoReproduccion.getStyleClass().add("etiqueta-modo-activo");
        }
    }

    @FXML
    private void beyAlternarAleatorio() {
        beyServicio.beyAlternarAleatorio();
    }

    @FXML
    private void beyCambiarRepeticion() {
        beyServicio.beyCiclarRepeticion();
    }

    private TableRow<beyPistaAudio> beyFilaReproducible() {
        TableRow<beyPistaAudio> fila = new TableRow<>();

        fila.setOnMouseClicked(evento -> {
            if (evento.getClickCount() == 2 && fila.getItem() != null) {
                beyIniciar(fila.getItem());
            }
        });

        fila.setOnDragDetected(evento -> {
            if (fila.isEmpty() || fila.getItem() == null) {
                return;
            }
            beyPistaArrastrada = fila.getItem();
            Dragboard tablero = fila.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(fila.getItem().getId());
            tablero.setContent(contenido);
            fila.getStyleClass().add("fila-arrastrando");
            evento.consume();
        });

        fila.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles()) {
                return;
            }
            if (beyPuedeSoltarEn(fila)) {
                evento.acceptTransferModes(TransferMode.MOVE);
                evento.consume();
            }
        });

        fila.setOnDragEntered(evento -> {
            if (!evento.getDragboard().hasFiles() && beyPuedeSoltarEn(fila)) {
                fila.getStyleClass().add("fila-destino");
            }
        });

        fila.setOnDragExited(evento -> fila.getStyleClass().remove("fila-destino"));

        fila.setOnDragDropped(evento -> {
            if (evento.getDragboard().hasFiles()) {
                return;
            }
            if (beyPuedeSoltarEn(fila)) {
                beyReordenar(beyPistaArrastrada, beyListaPistas.getItems().indexOf(fila.getItem()));
                evento.setDropCompleted(true);
            } else {
                evento.setDropCompleted(false);
            }
            evento.consume();
        });

        fila.setOnDragDone(evento -> {
            fila.getStyleClass().removeAll("fila-arrastrando", "fila-destino");
            beyPistaArrastrada = null;
        });

        return fila;
    }

    private void beyConfigurarArrastreExterno() {
        beyListaPistas.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles() && beyContieneAudio(evento.getDragboard().getFiles())) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });

        beyListaPistas.setOnDragDropped(evento -> {
            List<File> archivos = evento.getDragboard().getFiles();
            List<File> audio = archivos == null ? List.of() : archivos.stream()
                    .filter(archivo -> archivo.isFile()
                            && beyArchivoMultimediaService.beyEsAudio(archivo.getName()))
                    .toList();
            if (audio.isEmpty()) {
                evento.setDropCompleted(false);
            } else {
                beyAgregar(audio);
                beyRefrescar();
                evento.setDropCompleted(true);
            }
            evento.consume();
        });
    }

    private static boolean beyContieneAudio(List<File> archivos) {
        if (archivos == null) {
            return false;
        }
        for (File archivo : archivos) {
            if (archivo.isFile() && beyArchivoMultimediaService.beyEsAudio(archivo.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean beyPuedeSoltarEn(TableRow<beyPistaAudio> fila) {
        return beyPistaArrastrada != null && !fila.isEmpty() && fila.getItem() != null
                && fila.getItem() != beyPistaArrastrada;
    }

    private void beyReordenar(beyPistaAudio pista, int destino) {
        List<beyPistaAudio> pistas = new ArrayList<>(beyListaPistas.getItems());
        int origen = pistas.indexOf(pista);
        if (origen < 0 || destino < 0 || origen == destino) {
            return;
        }
        pistas.remove(origen);
        pistas.add(Math.min(destino, pistas.size()), pista);
        beyGestor.beyReordenarBiblioteca(pistas);
        beyRefrescar();
        beyListaPistas.getSelectionModel().select(pista);
    }

    private TableCell<beyPistaAudio, beyPistaAudio> beyCeldaPortada() {
        return new TableCell<>() {
            private final ImageView beyVista = new ImageView();
            private final Label beySimbolo = new Label();
            private final StackPane beyMarco = new StackPane(beySimbolo, beyVista);

            {
                beyVista.setFitWidth(72);
                beyVista.setFitHeight(40);
                beyVista.setPreserveRatio(true);
                beyVista.setSmooth(true);
                beySimbolo.getStyleClass().add("portada-simbolo");
                beyMarco.getStyleClass().add("portada-miniatura");
                beyMarco.setMinSize(76, 42);
                beyMarco.setPrefSize(76, 42);
                beyMarco.setMaxSize(76, 42);
                Rectangle recorte = new Rectangle(76, 42);
                recorte.setArcWidth(8);
                recorte.setArcHeight(8);
                beyMarco.setClip(recorte);
            }

            @Override
            protected void updateItem(beyPistaAudio pista, boolean vacio) {
                super.updateItem(pista, vacio);
                if (vacio || pista == null) {
                    setGraphic(null);
                    return;
                }
                Image imagen = beyArchivoMultimediaService.beyPortada(pista);
                beyVista.setImage(imagen);
                beyVista.setVisible(imagen != null);
                beySimbolo.setVisible(imagen == null);
                beySimbolo.setText(pista.esEnlace() ? "\u25B6" : "\u266B");
                setGraphic(beyMarco);
            }
        };
    }

    private TableCell<beyPistaAudio, Void> beyCeldaAcciones() {
        return new TableCell<>() {
            private final Button beyBoton = new Button("\u2715");

            {
                beyBoton.getStyleClass().add("boton-accion-eliminar");
                beyBoton.setTooltip(new Tooltip("Eliminar esta pista de la lista"));
                beyBoton.setOnAction(evento -> {
                    int indice = getIndex();
                    if (indice >= 0 && indice < getTableView().getItems().size()) {
                        beyEliminarDeLista(getTableView().getItems().get(indice));
                    }
                });
            }

            @Override
            protected void updateItem(Void elemento, boolean vacio) {
                super.updateItem(elemento, vacio);
                setGraphic(vacio ? null : beyBoton);
            }
        };
    }

    private void beyMostrarPortadaActual(beyPistaAudio pista) {
        Image imagen = beyArchivoMultimediaService.beyPortada(pista);
        beyPortadaActual.setImage(imagen);
        beyPortadaActual.setVisible(imagen != null);
        beySimboloPortada.setVisible(imagen == null);
    }

    private void beyActualizarPistaMostrada(beyPistaAudio pista) {
        if (pista == null) {
            beyEtiquetaTituloPista.setText("Sin pista en reproducción");
            beyEtiquetaArtista.setText("");
            beyMostrarPortadaActual(null);
            return;
        }
        beyEtiquetaTituloPista.setText(pista.getTitulo());
        beyEtiquetaArtista.setText(beyServicio.beyGetArtista());
        beyMostrarPortadaActual(pista);
        if (beyListaPistas.getItems().contains(pista)) {
            beyListaPistas.getSelectionModel().select(pista);
        }
    }

    @FXML
    private void beyAgregarArchivos() {
        List<File> archivos = beyArchivoMultimediaService
                .beySelectorAudio(beyGestor.beyGetDatos().getConfiguracion().getUltimaCarpetaMusica())
                .showOpenMultipleDialog(beyVentana());
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        beyAgregar(archivos);
        beyGestor.beyRecordarCarpetaMusica(archivos.get(0).getParent());
        beyRefrescar();
    }

    @FXML
    private void beyAgregarCarpeta() {
        DirectoryChooser selector = beyArchivoMultimediaService.beySelectorCarpeta(
                beyGestor.beyGetDatos().getConfiguracion().getUltimaCarpetaMusica(),
                "Seleccionar carpeta con música");
        File carpeta = selector.showDialog(beyVentana());
        if (carpeta == null) {
            return;
        }
        List<File> archivos = beyArchivoMultimediaService.beyBuscarArchivosAudio(carpeta);
        if (archivos.isEmpty()) {
            beyDialogos.beyInfo("Sin archivos de audio",
                    "La carpeta seleccionada no contiene archivos MP3, WAV, M4A o AAC.");
            return;
        }
        beyAgregar(archivos);
        beyGestor.beyRecordarCarpetaMusica(carpeta.getAbsolutePath());
        beyRefrescar();
    }

    private void beyAgregar(List<File> archivos) {
        List<beyPistaAudio> nuevas = new ArrayList<>();
        for (File archivo : archivos) {
            nuevas.add(new beyPistaAudio(beyTituloDe(archivo.getName()), archivo.getAbsolutePath()));
        }
        beyGestor.beyAgregarPistas(nuevas);
    }

    @FXML
    private void beyAgregarEnlace() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Descargar enlace de audio");
        dialogo.initOwner(beyVentana());

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
        beyDialogos.beyAplicarEstilo(panel);

        Node nodoAceptar = panel.lookupButton(aceptar);
        nodoAceptar.setDisable(true);
        campoEnlace.textProperty().addListener((obs, anterior, actual) ->
                nodoAceptar.setDisable(actual == null || actual.isBlank()));

        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isEmpty() || resultado.get() != aceptar) {
            return;
        }
        String enlace = campoEnlace.getText().trim();
        if (!beyArchivoMultimediaService.beyEsUrl(enlace)) {
            beyDialogos.beyInfo("Enlace no válido", "Introduce una dirección http(s) válida.");
            return;
        }
        String titulo = campoTitulo.getText() == null || campoTitulo.getText().isBlank()
                ? null : campoTitulo.getText().trim();
        beyDescargarEnlace(enlace, titulo);
    }

    private void beyDescargarEnlace(String enlace, String titulo) {
        if (beyDescargando) {
            beyDialogos.beyInfo("Descarga en curso", "Espera a que finalice la descarga actual.");
            return;
        }
        beyDescargando = true;
        beyMostrarPanelDescarga(true);
        beyProgresoDescarga.setProgress(-1);
        beyEtiquetaDescarga.setText("Preparando la descarga…");

        beyGestorHerramientasService.beyAsegurar(
                (fraccion, mensaje) -> {
                    beyEtiquetaDescarga.setText(mensaje);
                    beyProgresoDescarga.setProgress(fraccion);
                },
                () -> beyConvertirEnlace(enlace, titulo),
                error -> {
                    beyDescargando = false;
                    beyMostrarPanelDescarga(false);
                    beyMostrarFalloDescarga("No se pudo preparar la descarga", error);
                });
    }

    private void beyConvertirEnlace(String enlace, String titulo) {
        beyProgresoDescarga.setProgress(-1);
        beyEtiquetaDescarga.setText("Conectando con el servidor…");
        beyDescargadorAudioService.beyDescargar(enlace, beyGestor.beyGetCarpetaMusica(),
                (fraccion, mensaje) -> {
                    beyEtiquetaDescarga.setText(mensaje);
                    beyProgresoDescarga.setProgress(fraccion);
                },
                resultado -> {
                    beyDescargando = false;
                    beyMostrarPanelDescarga(false);
                    beyPistaAudio pista = new beyPistaAudio(
                            resultado.titulo() == null || resultado.titulo().isBlank()
                                    ? beyTituloDe(resultado.archivo().getName()) : resultado.titulo(),
                            resultado.archivo().getAbsolutePath());
                    pista.setOrigen(beyOrigenPista.DESCARGADO);
                    if (titulo != null && !titulo.isBlank()) {
                        pista.setTitulo(titulo);
                    }
                    pista.setArtista(resultado.artista() == null || resultado.artista().isBlank()
                            ? beyArchivoMultimediaService.beyTipoEnlace(enlace) : resultado.artista());
                    pista.setDuracionSegundos(resultado.duracionSegundos());
                    beyGestor.beyAgregarPista(pista);
                    beyRefrescar();
                    beyIniciar(pista);
                },
                error -> {
                    beyDescargando = false;
                    beyMostrarPanelDescarga(false);
                    beyMostrarFalloDescarga("No se pudo descargar el enlace", error);
                });
    }

    @FXML
    private void beyActualizarMotor() {
        if (beyDescargando) {
            beyDialogos.beyInfo("Operación en curso", "Espera a que finalice la descarga actual.");
            return;
        }
        beyDescargando = true;
        beyBotonActualizarMotor.setDisable(true);
        beyMostrarPanelDescarga(true);
        beyProgresoDescarga.setProgress(-1);
        beyEtiquetaDescarga.setText("Actualizando el motor de descarga…");
        beyGestorHerramientasService.beyActualizarYtDlp(
                (fraccion, mensaje) -> {
                    beyEtiquetaDescarga.setText(mensaje);
                    beyProgresoDescarga.setProgress(fraccion);
                },
                mensaje -> {
                    beyTerminarOperacionMotor();
                    beyDialogos.beyInfo("Motor de descarga", mensaje);
                },
                error -> {
                    beyTerminarOperacionMotor();
                    beyMostrarFalloDescarga("No se pudo actualizar el motor", error);
                });
    }

    private void beyTerminarOperacionMotor() {
        beyDescargando = false;
        beyBotonActualizarMotor.setDisable(false);
        beyMostrarPanelDescarga(false);
    }

    private void beyMostrarFalloDescarga(String titulo, String mensaje) {
        ButtonType actualizar = new ButtonType("Actualizar motor", ButtonBar.ButtonData.LEFT);
        ButtonType archivos = new ButtonType("Añadir archivos", ButtonBar.ButtonData.OTHER);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, actualizar, archivos, cerrar);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.initOwner(beyVentana());
        beyDialogos.beyAplicarEstilo(alerta.getDialogPane());
        Optional<ButtonType> eleccion = alerta.showAndWait();
        if (eleccion.isPresent()) {
            if (eleccion.get() == actualizar) {
                beyActualizarMotor();
            } else if (eleccion.get() == archivos) {
                beyAgregarArchivos();
            }
        }
    }

    private void beyMostrarPanelDescarga(boolean mostrar) {
        beyPanelDescarga.setVisible(mostrar);
        beyPanelDescarga.setManaged(mostrar);
    }

    @FXML
    private void beyEliminarPista() {
        beyEliminarDeLista(beyListaPistas.getSelectionModel().getSelectedItem());
    }

    private void beyEliminarDeLista(beyPistaAudio pista) {
        if (pista == null) {
            return;
        }
        if (pista == beyServicio.beyGetPistaActual()) {
            beyServicio.beyDetener();
        }
        beyGestor.beyEliminarPista(pista);
        beyRefrescar();
    }

    @FXML
    private void beyVaciarBiblioteca() {
        if (beyGestor.beyGetDatos().getBibliotecaMusical().isEmpty()) {
            return;
        }
        if (beyDialogos.beyConfirmar("Vaciar biblioteca",
                "¿Quitar todas las pistas de la biblioteca? Los archivos no se eliminan del disco.",
                beyVentana())) {
            beyServicio.beyDetener();
            beyGestor.beyLimpiarBiblioteca();
            beyRefrescar();
        }
    }

    @FXML
    private void beyAlternarReproduccion() {
        if (!beyServicio.beyIsActivo()) {
            beyPistaAudio pista = beyListaPistas.getSelectionModel().getSelectedItem();
            if (pista == null && !beyListaPistas.getItems().isEmpty()) {
                pista = beyListaPistas.getItems().get(0);
            }
            if (pista == null) {
                beyDialogos.beyInfo("Biblioteca vacía", "Añade archivos de audio antes de reproducir.");
                return;
            }
            beyIniciar(pista);
            return;
        }
        beyServicio.beyAlternar();
    }

    @FXML
    private void beySiguiente() {
        beyServicio.beySiguiente();
    }

    @FXML
    private void beyAnterior() {
        beyServicio.beyAnterior();
    }

    private void beyIniciar(beyPistaAudio pista) {
        if (pista == null) {
            return;
        }
        if (pista.esEnlace()) {
            beyDescargarEnlace(pista.getEnlace(), pista.getTitulo());
            return;
        }
        if (!beyArchivoMultimediaService.beyExiste(pista.getRutaArchivo())) {
            beyDialogos.beyInfo("Archivo no disponible",
                    "No se encontró el archivo:\n" + pista.getRutaArchivo());
            return;
        }
        beyServicio.beyReproducir(pista);
    }

    private void beyActualizarEtiquetaVolumen() {
        beyEtiquetaVolumen.setText(Math.round(beySliderVolumen.getValue() * 100) + "%");
    }

    private static String beyTituloDe(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        return punto > 0 ? nombreArchivo.substring(0, punto) : nombreArchivo;
    }

    private static String beyFormatear(double segundosTotales) {
        int total = (int) Math.max(0, Math.round(segundosTotales));
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private Window beyVentana() {
        return beyListaPistas.getScene() == null ? null : beyListaPistas.getScene().getWindow();
    }
}
