package com.bey.desktop.controller;

import com.bey.desktop.model.beyDiaSemana;
import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyGrupoMuscular;
import com.bey.desktop.model.beyModalidadEntrenamiento;
import com.bey.desktop.model.beyNivelEjercicio;
import com.bey.desktop.model.beyRutina;
import com.bey.desktop.service.beyAdaptadorEquipoService;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beySugerenciasEjerciciosService;
import com.bey.desktop.util.beyDiagramaAnatomico;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormularioDialogo;
import com.bey.desktop.util.beyGuiaTecnicaDialogo;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.ResourceBundle;

public class beyEntrenamientoController implements Initializable, beyVistaConRefresco {
    private static final String RUTA_FORMULARIO_RUTINA = "/com/bey/desktop/view/beyFormularioRutina.fxml";
    private static final String RUTA_FORMULARIO_EJERCICIO = "/com/bey/desktop/view/beyFormularioEjercicio.fxml";

    private static final double LADO_ICONO_ACCION = 17;
    private static final double ESCALA_ICONO_ACCION = LADO_ICONO_ACCION / 24.0;

    private static final String SVG_VER_TECNICA = "M8,5 L19,12 L8,19 Z";
    private static final String SVG_ADJUNTAR =
            "M16.5,6v11.5c0,2.21 -1.79,4 -4,4s-4,-1.79 -4,-4V5c0,-1.38 1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5v10.5"
            + "c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V6H10v9.5c0,1.38 1.12,2.5 2.5,2.5s2.5,-1.12 2.5,-2.5V5"
            + "c0,-2.21 -1.79,-4 -4,-4S7,2.79 7,5v12.5c0,3.04 2.46,5.5 5.5,5.5s5.5,-2.46 5.5,-5.5V6h-1.5z";
    private static final String SVG_EDITAR =
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41"
            + "l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z";
    private static final String SVG_ELIMINAR =
            "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z";

    private static final String SVG_MEDIA_DISPONIBLE =
            "M17,10.5V7c0,-0.55 -0.45,-1 -1,-1H4c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h12"
            + "c0.55,0 1,-0.45 1,-1v-3.5l4,4v-11l-4,4z";
    private static final String SVG_MEDIA_ENLACE =
            "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7"
            + "c-1.71,0 -3.1,-1.39 -3.1,-3.1zM8,13h8v-2H8v2zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1"
            + "s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z";

    private static final double LADO_ICONO_MEDIA = 16;

    private static final Tooltip AYUDA_RUTINA_HOY = new Tooltip("Rutina programada para hoy");

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();
    private final ObservableList<beyRutina> beyRutinas = FXCollections.observableArrayList();

    @FXML
    private ListView<beyRutina> beyListaRutinas;
    @FXML
    private TableView<beyEjercicio> beyTablaEjercicios;
    @FXML
    private TableColumn<beyEjercicio, String> beyColumnaNombre;
    @FXML
    private TableColumn<beyEjercicio, String> beyColumnaGrupo;
    @FXML
    private TableColumn<beyEjercicio, String> beyColumnaPrescripcion;
    @FXML
    private TableColumn<beyEjercicio, String> beyColumnaRir;
    @FXML
    private TableColumn<beyEjercicio, Integer> beyColumnaDescanso;
    @FXML
    private TableColumn<beyEjercicio, String> beyColumnaMultimedia;
    @FXML
    private TableColumn<beyEjercicio, Void> beyColumnaAcciones;
    @FXML
    private Label beyEtiquetaNombreRutina;
    @FXML
    private Label beyEtiquetaDetalleRutina;
    @FXML
    private Label beyEtiquetaNotasRutina;
    @FXML
    private Label beyEtiquetaEstado;
    @FXML
    private Label beyEtiquetaRutinaHoy;
    @FXML
    private Button beyBotonEditarRutina;
    @FXML
    private Button beyBotonEliminarRutina;
    @FXML
    private Button beyBotonAgregarEjercicio;
    @FXML
    private Button beyBotonAbrirSugerencias;
    @FXML
    private Button beyBotonIniciarEntrenamiento;
    @FXML
    private StackPane beyModalBiblioteca;
    @FXML
    private ComboBox<beyNivelEjercicio> beyComboNivelSugerencias;
    @FXML
    private Label beyEtiquetaDescripcionNivel;
    @FXML
    private ScrollPane beyScrollSugerencias;
    @FXML
    private GridPane beyCuadriculaSugerencias;
    @FXML
    private Label beyEtiquetaSeleccion;
    @FXML
    private Button beyBotonAgregarSugerencia;
    @FXML
    private ToggleButton beyBotonModalGimnasio;
    @FXML
    private ToggleButton beyBotonModalCasa;
    @FXML
    private ToggleButton beyBotonModoGimnasio;
    @FXML
    private ToggleButton beyBotonModoCasa;
    @FXML
    private Label beyEtiquetaModalidad;

    private final Set<beyEjercicio> beySeleccionBiblioteca = new LinkedHashSet<>();

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyColumnaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        beyColumnaNombre.setCellFactory(column -> beyCeldaNombre());
        beyColumnaGrupo.setCellValueFactory(datos -> {
            beyGrupoMuscular grupo = datos.getValue().getGrupoMuscular();
            return new SimpleStringProperty(grupo == null ? "" : grupo.getEtiqueta());
        });
        beyColumnaGrupo.setCellFactory(column -> beyCeldaChip("chip-grupo"));
        beyColumnaPrescripcion.setCellValueFactory(new PropertyValueFactory<>("prescripcion"));
        beyColumnaRir.setCellValueFactory(new PropertyValueFactory<>("rirTexto"));
        beyColumnaRir.setCellFactory(column -> beyCeldaRir());
        beyColumnaDescanso.setCellValueFactory(new PropertyValueFactory<>("descansoSegundos"));
        beyColumnaDescanso.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : valor + " s");
            }
        });
        beyColumnaMultimedia.setCellValueFactory(datos -> null);
        beyColumnaMultimedia.setCellFactory(column -> beyCeldaMultimedia());
        beyColumnaAcciones.setCellValueFactory(datos -> null);
        beyColumnaAcciones.setCellFactory(column -> beyCeldaAcciones());

        beyTablaEjercicios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        beyTablaEjercicios.setPlaceholder(new Label(
                "Esta rutina no tiene ejercicios. Añade uno o usa \"Añadir desde biblioteca\"."));

        beyListaRutinas.setItems(beyRutinas);
        beyListaRutinas.setCellFactory(lista -> beyCeldaRutina());
        beyListaRutinas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> beyMostrarRutina(actual));

        beyConfigurarModalidad();
        beyConfigurarSugerencias();
        beyRefrescar();
    }

    private void beyConfigurarModalidad() {
        beyAgruparSegmento(beyBotonModoGimnasio, beyBotonModoCasa);
        beyAgruparSegmento(beyBotonModalGimnasio, beyBotonModalCasa);
        beySincronizarModalidad();
    }

    private static void beyAgruparSegmento(ToggleButton primero, ToggleButton segundo) {
        ToggleGroup grupo = new ToggleGroup();
        primero.setToggleGroup(grupo);
        segundo.setToggleGroup(grupo);
        grupo.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null && anterior != null) {
                anterior.setSelected(true);
            }
        });
    }

    @FXML
    private void beyUsarModoGimnasio() {
        beyCambiarModalidad(beyModalidadEntrenamiento.GIMNASIO);
    }

    @FXML
    private void beyUsarModoCasa() {
        beyCambiarModalidad(beyModalidadEntrenamiento.CASA);
    }

    private void beyCambiarModalidad(beyModalidadEntrenamiento modalidad) {
        if (beyGestor.beyGetModalidad() == modalidad) {
            beySincronizarModalidad();
            return;
        }
        beyGestor.beySetModalidad(modalidad);
        beySincronizarModalidad();
        beyMostrarRutina(beyListaRutinas.getSelectionModel().getSelectedItem());
        beyCargarSugerencias();
        beyEtiquetaEstado.setText(modalidad == beyModalidadEntrenamiento.CASA
                ? "Modo sin equipo: ejercicios adaptados a peso corporal."
                : "Modo con equipo: rutina original del gimnasio.");
    }

    private void beySincronizarModalidad() {
        beyModalidadEntrenamiento modalidad = beyGestor.beyGetModalidad();
        beyBotonModoGimnasio.setSelected(modalidad == beyModalidadEntrenamiento.GIMNASIO);
        beyBotonModoCasa.setSelected(modalidad == beyModalidadEntrenamiento.CASA);
        beyBotonModalGimnasio.setSelected(modalidad == beyModalidadEntrenamiento.GIMNASIO);
        beyBotonModalCasa.setSelected(modalidad == beyModalidadEntrenamiento.CASA);
        beyEtiquetaModalidad.setText(modalidad.getDescripcion());
    }

    private List<beyEjercicio> beyEjerciciosVisibles(beyRutina rutina) {
        if (beyGestor.beyGetModalidad() == beyModalidadEntrenamiento.CASA) {
            return beyAdaptadorEquipoService.beyAdaptarRutina(rutina).getEjercicios();
        }
        return rutina.getEjercicios();
    }

    private beyEjercicio beyOriginal(beyEjercicio visible) {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || visible == null) {
            return visible;
        }
        return rutina.getEjercicios().stream()
                .filter(e -> e.getId().equals(visible.getId()))
                .findFirst()
                .orElse(visible);
    }

    private boolean beyBloqueadoPorVariante(beyEjercicio visible) {
        if (visible != null && visible.getAdaptadoDe() != null) {
            beyDialogos.beyInfo("Variante sin equipo",
                    "\"" + visible.getNombre() + "\" es una variante automática de \""
                            + visible.getAdaptadoDe() + "\".\nCambia a \"Con equipo\" para editar el ejercicio original.");
            return true;
        }
        return false;
    }

    private TableCell<beyEjercicio, String> beyCeldaNombre() {
        return new TableCell<>() {
            private final Label beyNombre = new Label();
            private final Label beyOrigen = new Label();
            private final VBox beyCaja = new VBox(1, beyNombre, beyOrigen);

            {
                beyOrigen.getStyleClass().add("texto-variante");
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                int indice = getIndex();
                List<beyEjercicio> items = getTableView().getItems();
                if (vacio || indice < 0 || indice >= items.size()) {
                    setGraphic(null);
                    return;
                }
                beyEjercicio ejercicio = items.get(indice);
                beyNombre.setText(valor);
                boolean variante = ejercicio.getAdaptadoDe() != null;
                beyOrigen.setText(variante ? "\u21BA Adaptado de: " + ejercicio.getAdaptadoDe() : "");
                beyOrigen.setVisible(variante);
                beyOrigen.setManaged(variante);
                setGraphic(beyCaja);
            }
        };
    }

    private ListCell<beyRutina> beyCeldaRutina() {
        return new ListCell<>() {
            private final Label beyTitulo = new Label();
            private final Label beySubtitulo = new Label();
            private final Label beyChipHoy = new Label("Hoy");
            private final HBox beyFilaTitulo = new HBox(6, beyTitulo, beyChipHoy);
            private final VBox beyTextos = new VBox(2, beyFilaTitulo, beySubtitulo);
            private final Region beyIndicador = new Region();
            private final HBox beyContenido = new HBox(10, beyIndicador, beyTextos);

            {
                beyTitulo.getStyleClass().add("rutina-item-titulo");
                beySubtitulo.getStyleClass().add("rutina-item-subtitulo");
                beyChipHoy.getStyleClass().add("rutina-item-hoy");
                beyIndicador.getStyleClass().add("rutina-item-indicador");
                beyFilaTitulo.setAlignment(Pos.CENTER_LEFT);
                beyTextos.setAlignment(Pos.CENTER_LEFT);
                beyContenido.setAlignment(Pos.CENTER_LEFT);
                beyTextos.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(beyTextos, Priority.ALWAYS);

                beyIndicador.setMaxHeight(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(beyRutina rutina, boolean vacio) {
                super.updateItem(rutina, vacio);
                if (vacio || rutina == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    return;
                }
                boolean esHoy = rutina.getDiaSemana() == beyDiaSemana.desdeFecha(LocalDate.now());
                beyTitulo.setText(rutina.getNombre());
                beyChipHoy.setVisible(esHoy);
                beyChipHoy.setManaged(esHoy);
                String dia = rutina.getDiaSemana() == null
                        ? "Sin día asignado" : rutina.getDiaSemana().getEtiqueta();
                int total = rutina.getEjercicios().size();
                beySubtitulo.setText(dia + " · " + total + (total == 1 ? " ejercicio" : " ejercicios")
                        + " · " + rutina.getSeriesTotales() + " series");
                setText(null);
                setTooltip(esHoy ? AYUDA_RUTINA_HOY : null);
                setGraphic(beyContenido);
            }
        };
    }

    private TableCell<beyEjercicio, String> beyCeldaChip(String estilo) {
        return new TableCell<>() {
            private final Label beyChip = new Label();
            private final HBox beyCaja = new HBox(beyChip);

            {
                beyChip.getStyleClass().add(estilo);
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null || valor.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                beyChip.setText(valor);
                setGraphic(beyCaja);
            }
        };
    }

    private TableCell<beyEjercicio, String> beyCeldaRir() {
        return new TableCell<>() {
            private final Label beyChip = new Label();
            private final HBox beyCaja = new HBox(beyChip);

            {
                beyChip.getStyleClass().add("chip-rir");
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                beyChip.getStyleClass().removeAll("chip-rir-duro", "chip-rir-medio", "chip-rir-suave");
                beyEjercicio ejercicio = beyEjercicioDeLaFila(this);
                if (vacio || valor == null || ejercicio == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                beyChip.setText(valor);
                beyChip.getStyleClass().add(beyClaseRir(ejercicio.getRirObjetivo()));
                setGraphic(beyCaja);
            }
        };
    }

    private static String beyClaseRir(int rirObjetivo) {
        if (rirObjetivo <= 1) {
            return "chip-rir-duro";
        }
        if (rirObjetivo <= 3) {
            return "chip-rir-medio";
        }
        return "chip-rir-suave";
    }

    private TableCell<beyEjercicio, String> beyCeldaMultimedia() {
        return new TableCell<>() {
            private final Label beyEtiqueta = new Label();
            private final HBox beyCaja = new HBox(6);

            {
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                beyEjercicio ejercicio = beyEjercicioDeLaFila(this);
                if (vacio || ejercicio == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                beyEstadoMedia estado = beyEstadoMedia(ejercicio);
                Node icono = beyNodoIcono(estado.svg(), estado.claseIcono(), LADO_ICONO_MEDIA);
                beyEtiqueta.setText(estado.texto());
                beyEtiqueta.getStyleClass().setAll(estado.claseTexto());
                beyCaja.getChildren().setAll(icono, beyEtiqueta);
                setText(null);
                setGraphic(beyCaja);
            }
        };
    }

    private record beyEstadoMedia(String texto, String svg, String claseIcono, String claseTexto) {
    }

    private static beyEstadoMedia beyEstadoMedia(beyEjercicio ejercicio) {
        String ruta = ejercicio.getRutaMultimedia();
        if (beyArchivoMultimediaService.beyExiste(ruta)) {
            String tipo = beyArchivoMultimediaService.beyEsVideo(ruta) ? "Video"
                    : (beyArchivoMultimediaService.beyEsImagen(ruta) ? "Imagen" : "Archivo");
            return new beyEstadoMedia(tipo, SVG_MEDIA_DISPONIBLE, "icono-media-ok", "media-etiqueta-ok");
        }
        String enlace = ejercicio.getEnlaceMultimedia();
        if (enlace != null && !enlace.isBlank()) {
            String tipo = beyArchivoMultimediaService.beyEsYoutube(enlace) ? "YouTube" : "Enlace";
            return new beyEstadoMedia(tipo, SVG_MEDIA_ENLACE, "icono-media-ok", "media-etiqueta-ok");
        }
        return new beyEstadoMedia("Sin multimedia", SVG_MEDIA_DISPONIBLE, "icono-media-vacio",
                "media-etiqueta-vacio");
    }

    private beyEjercicio beyEjercicioDeLaFila(TableCell<beyEjercicio, ?> celda) {
        int indice = celda.getIndex();
        List<beyEjercicio> items = celda.getTableView().getItems();
        return indice >= 0 && indice < items.size() ? items.get(indice) : null;
    }

    private static Node beyNodoIcono(String contenidoSvg, String claseIcono, double lado) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(contenidoSvg);
        ruta.getStyleClass().add(claseIcono);
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

    private TableCell<beyEjercicio, Void> beyCeldaAcciones() {
        return new TableCell<>() {
            private final Button beyBotonVer = beyBotonIcono(SVG_VER_TECNICA, "Ver técnica del ejercicio",
                    "boton-icono");
            private final Button beyBotonAdjuntar = beyBotonIcono(SVG_ADJUNTAR, "Adjuntar o cambiar multimedia",
                    "boton-icono");
            private final Button beyBotonEditar = beyBotonIcono(SVG_EDITAR, "Editar ejercicio", "boton-icono");
            private final Button beyBotonEliminar = beyBotonIcono(SVG_ELIMINAR, "Eliminar de la rutina",
                    "boton-icono", "boton-icono-peligro");
            private final HBox beyCaja = new HBox(2, beyBotonVer, beyBotonAdjuntar, beyBotonEditar, beyBotonEliminar);

            {
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                beyBotonVer.setOnAction(evento -> beyVerTecnica(beyEjercicioDeLaFila()));
                beyBotonAdjuntar.setOnAction(evento -> beyAdjuntarMultimedia(beyEjercicioDeLaFila()));
                beyBotonEditar.setOnAction(evento -> beyEditarEjercicio(beyEjercicioDeLaFila()));
                beyBotonEliminar.setOnAction(evento -> beyEliminarEjercicio(beyEjercicioDeLaFila()));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : beyCaja);
            }

            private beyEjercicio beyEjercicioDeLaFila() {
                int indice = getIndex();
                List<beyEjercicio> items = getTableView().getItems();
                return indice >= 0 && indice < items.size() ? items.get(indice) : null;
            }
        };
    }

    private static Button beyBotonIcono(String contenidoSvg, String ayuda, String... estilos) {
        SVGPath ruta = new SVGPath();
        ruta.setContent(contenidoSvg);
        ruta.getStyleClass().add("icono-accion");
        ruta.setScaleX(ESCALA_ICONO_ACCION);
        ruta.setScaleY(ESCALA_ICONO_ACCION);

        StackPane marco = new StackPane(ruta);
        marco.setMinSize(LADO_ICONO_ACCION, LADO_ICONO_ACCION);
        marco.setPrefSize(LADO_ICONO_ACCION, LADO_ICONO_ACCION);
        marco.setMaxSize(LADO_ICONO_ACCION, LADO_ICONO_ACCION);
        marco.setMouseTransparent(true);

        Button boton = new Button();
        boton.setGraphic(marco);
        boton.getStyleClass().addAll(estilos);
        boton.setFocusTraversable(false);
        boton.setTooltip(new Tooltip(ayuda));
        return boton;
    }

    @FXML
    private void beyAlternarSugerencias() {
        if (beyModalBiblioteca.isVisible()) {
            beyCerrarSugerencias();
            return;
        }
        beySeleccionBiblioteca.clear();
        beyCargarSugerencias();
        beyScrollSugerencias.setVvalue(0);
        beyModalBiblioteca.setManaged(true);
        beyModalBiblioteca.setVisible(true);
        beyModalBiblioteca.setOpacity(0);
        FadeTransition aparicion = new FadeTransition(Duration.millis(160), beyModalBiblioteca);
        aparicion.setToValue(1);
        aparicion.play();
        beyModalBiblioteca.requestFocus();
    }

    @FXML
    private void beyCerrarSugerencias() {
        if (!beyModalBiblioteca.isVisible()) {
            return;
        }
        FadeTransition salida = new FadeTransition(Duration.millis(130), beyModalBiblioteca);
        salida.setToValue(0);
        salida.setOnFinished(evento -> {
            beyModalBiblioteca.setVisible(false);
            beyModalBiblioteca.setManaged(false);
        });
        salida.play();
    }

    private void beyConfigurarSugerencias() {
        beyComboNivelSugerencias.setItems(FXCollections.observableArrayList(beyNivelEjercicio.values()));
        beyComboNivelSugerencias.setValue(beyNivelEjercicio.PRINCIPIANTE);
        beyComboNivelSugerencias.valueProperty().addListener((obs, anterior, actual) -> {
            beySeleccionBiblioteca.clear();
            beyCargarSugerencias();
        });

        beyModalBiblioteca.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ESCAPE) {
                beyCerrarSugerencias();
            }
        });
        beyCargarSugerencias();
    }

    private void beyCargarSugerencias() {
        beyNivelEjercicio nivel = beyComboNivelSugerencias.getValue();
        beyEtiquetaDescripcionNivel.setText(beySugerenciasEjerciciosService.beyDescripcion(nivel, beyGestor.beyGetModalidad()));
        beySeleccionBiblioteca.clear();
        beyCuadriculaSugerencias.getChildren().clear();
        List<beyEjercicio> sugeridas = beySugerenciasEjerciciosService.beyParaNivel(nivel, beyGestor.beyGetModalidad());
        for (int i = 0; i < sugeridas.size(); i++) {
            beyCuadriculaSugerencias.add(beyCrearTarjeta(sugeridas.get(i)), i % 2, i / 2);
        }
        beyActualizarSeleccion();
    }

    private VBox beyCrearTarjeta(beyEjercicio ejercicio) {
        Node icono = beyDiagramaAnatomico.beyIconoGrande(ejercicio.getGrupoMuscular());

        Label nombre = new Label(ejercicio.getNombre());
        nombre.getStyleClass().add("tarjeta-ejercicio-nombre");
        nombre.setWrapText(true);

        Label grupo = new Label(ejercicio.getGrupoMuscular().getEtiqueta());
        grupo.getStyleClass().add("chip-grupo");
        Label prescripcion = new Label(ejercicio.getPrescripcion() + " · " + ejercicio.getRirTexto());
        prescripcion.getStyleClass().add("chip-prescripcion");
        HBox etiquetas = new HBox(6, grupo, prescripcion);
        etiquetas.setAlignment(Pos.CENTER_LEFT);

        Label descripcion = new Label(beySugerenciasEjerciciosService.beyDescripcionTecnica(ejercicio));
        descripcion.getStyleClass().add("tarjeta-ejercicio-descripcion");
        descripcion.setWrapText(true);
        descripcion.setMaxHeight(40);

        Label marca = new Label("\u2713");
        marca.getStyleClass().add("tarjeta-ejercicio-marca");
        marca.setVisible(false);

        VBox textos = new VBox(5, nombre, etiquetas, descripcion);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button verTecnica = new Button("▶");
        verTecnica.getStyleClass().add("boton-icono");
        verTecnica.setTooltip(new Tooltip("Ver técnica animada"));
        verTecnica.setFocusTraversable(false);
        verTecnica.setOnMouseClicked(evento -> evento.consume());
        verTecnica.setOnAction(evento -> {
            evento.consume();
            beyGuiaTecnicaDialogo.beyMostrar(ejercicio, ejercicio.getNombre(), beyGestor.beyGetModalidad(), beyVentana(), null);
        });

        StackPane lateral = new StackPane(marca, verTecnica);
        StackPane.setAlignment(marca, Pos.TOP_RIGHT);
        StackPane.setAlignment(verTecnica, Pos.BOTTOM_RIGHT);
        lateral.setAlignment(Pos.TOP_RIGHT);
        lateral.setMinWidth(34);
        lateral.setMaxWidth(34);

        HBox contenido = new HBox(14, icono, textos, lateral);
        contenido.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(contenido);
        tarjeta.getStyleClass().add("tarjeta-ejercicio");
        tarjeta.setMinHeight(132);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setOnMouseClicked(evento -> {
            boolean marcada = beySeleccionBiblioteca.add(ejercicio);
            if (!marcada) {
                beySeleccionBiblioteca.remove(ejercicio);
            }
            tarjeta.getStyleClass().remove("tarjeta-ejercicio-seleccionada");
            if (marcada) {
                tarjeta.getStyleClass().add("tarjeta-ejercicio-seleccionada");
            }
            marca.setVisible(marcada);
            beyActualizarSeleccion();
        });
        return tarjeta;
    }

    private void beyActualizarSeleccion() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        int total = beySeleccionBiblioteca.size();
        String destino = rutina == null ? "" : " a " + rutina.getTituloCompleto();
        beyEtiquetaSeleccion.setText(total == 0
                ? "Toca las tarjetas para seleccionar uno o varios ejercicios."
                : total + (total == 1 ? " ejercicio seleccionado" : " ejercicios seleccionados")
                + " · se añadirán" + destino);
        beyBotonAgregarSugerencia.setDisable(rutina == null || total == 0);
    }

    @Override
    public void beyRefrescar() {
        beyRecargarRutinas(true);
    }

    private void beyRecargarRutinas(boolean seleccionarHoy) {
        beyRutina seleccionada = beyListaRutinas.getSelectionModel().getSelectedItem();
        beyRutinas.setAll(beyGestor.beyGetDatos().getRutinas());
        beyRutinas.sort(Comparator.comparingInt(r -> r.getDiaSemana() == null ? 99 : r.getDiaSemana().ordinal()));

        beyRutina aSeleccionar = seleccionarHoy ? beyRutinaDelDia() : null;
        if (aSeleccionar == null) {
            aSeleccionar = beyRutinas.stream()
                    .filter(r -> seleccionada != null && r.getId().equals(seleccionada.getId()))
                    .findFirst()
                    .orElse(beyRutinas.isEmpty() ? null : beyRutinas.get(0));
        }
        beyListaRutinas.getSelectionModel().select(aSeleccionar);
        beyMostrarRutina(aSeleccionar);
        beyEtiquetaEstado.setText(beyRutinas.size() + " rutinas · " + beyGestor.beyGetDatos().catalogoEjercicios().size()
                + " ejercicios en catálogo");
    }

    private beyRutina beyRutinaDelDia() {
        beyDiaSemana hoy = beyDiaSemana.desdeFecha(LocalDate.now());
        return beyRutinas.stream().filter(r -> r.getDiaSemana() == hoy).findFirst().orElse(null);
    }

    private void beyMostrarRutina(beyRutina rutina) {
        if (rutina == null) {
            beyEtiquetaNombreRutina.setText("Sin rutina seleccionada");
            beyEtiquetaDetalleRutina.setText("");
            beyEtiquetaNotasRutina.setText("");
            beyTablaEjercicios.getItems().clear();
            beyMarcarRutinaHoy(false);
            beyActualizarBotones();
            return;
        }
        beyEtiquetaNombreRutina.setText(rutina.getTituloCompleto());
        beyEtiquetaDetalleRutina.setText(rutina.getEjercicios().size() + " ejercicios · "
                + rutina.getSeriesTotales() + " series prescritas");
        beyEtiquetaNotasRutina.setText(rutina.getNotas() == null ? "" : rutina.getNotas());
        beyTablaEjercicios.getItems().setAll(beyEjerciciosVisibles(rutina));
        beyMarcarRutinaHoy(rutina.getDiaSemana() == beyDiaSemana.desdeFecha(LocalDate.now()));
        beyActualizarBotones();
    }

    private void beyMarcarRutinaHoy(boolean esHoy) {
        beyEtiquetaRutinaHoy.setVisible(esHoy);
        beyEtiquetaRutinaHoy.setManaged(esHoy);
    }

    private void beyActualizarBotones() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();

        beyBotonEditarRutina.setDisable(rutina == null);
        beyBotonEliminarRutina.setDisable(rutina == null);
        beyBotonAgregarEjercicio.setDisable(rutina == null);
        beyBotonAbrirSugerencias.setDisable(rutina == null);
        beyBotonIniciarEntrenamiento.setDisable(rutina == null || rutina.getEjercicios().isEmpty());
        beyActualizarSeleccion();
    }

    @FXML
    private void beyNuevaRutina() {
        beyRutina rutina = new beyRutina();
        rutina.setDiaSemana(beyDiaLibre());
        if (beyAbrirFormularioRutina(rutina, "Nueva rutina", "Crear")) {
            beyGestor.beyAgregarRutina(rutina);
            beyRecargarRutinas(false);
            beyEtiquetaEstado.setText("Rutina \"" + rutina.getNombre() + "\" creada.");
        }
    }

    @FXML
    private void beyEditarRutina() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        if (beyAbrirFormularioRutina(rutina, "Editar rutina", "Guardar")) {
            beyGestor.beyGuardarCambiosEntrenamiento();
            beyRecargarRutinas(false);
            beyEtiquetaEstado.setText("Rutina actualizada.");
        }
    }

    @FXML
    private void beyEliminarRutina() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        boolean confirmado = beyDialogos.beyConfirmar("Eliminar rutina",
                "¿Eliminar \"" + rutina.getTituloCompleto() + "\" y todos sus ejercicios?",
                beyVentana());
        if (confirmado) {
            beyGestor.beyEliminarRutina(rutina);
            beyRecargarRutinas(false);
            beyEtiquetaEstado.setText("Rutina eliminada.");
        }
    }

    @FXML
    private void beyAgregarEjercicio() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        beyEjercicio ejercicio = new beyEjercicio();
        ejercicio.setDescansoSegundos(beyGestor.beyGetDatos().getConfiguracion().getDescansoPorDefectoSegundos());
        if (beyAbrirFormularioEjercicio(ejercicio, "Nuevo ejercicio", "Añadir")) {
            rutina.agregarEjercicio(ejercicio);
            beyGestor.beyGuardarCambiosEntrenamiento();
            beyMostrarRutina(rutina);
            beyEtiquetaEstado.setText("Ejercicio \"" + ejercicio.getNombre() + "\" añadido.");
        }
    }

    private void beyEditarEjercicio(beyEjercicio visible) {
        if (visible == null || beyBloqueadoPorVariante(visible)) {
            return;
        }
        beyEjercicio ejercicio = beyOriginal(visible);
        if (beyAbrirFormularioEjercicio(ejercicio, "Editar ejercicio", "Guardar")) {
            beyGestor.beyGuardarCambiosEntrenamiento();
            beyMostrarRutina(beyListaRutinas.getSelectionModel().getSelectedItem());
            beyEtiquetaEstado.setText("Ejercicio actualizado.");
        }
    }

    private void beyEliminarEjercicio(beyEjercicio visible) {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || visible == null) {
            return;
        }
        beyEjercicio ejercicio = beyOriginal(visible);
        boolean confirmado = beyDialogos.beyConfirmar("Eliminar ejercicio",
                "¿Quitar \"" + ejercicio.getNombre() + "\" de " + rutina.getNombre() + "?", beyVentana());
        if (confirmado) {
            rutina.getEjercicios().remove(ejercicio);
            beyGestor.beyGuardarCambiosEntrenamiento();
            beyMostrarRutina(rutina);
            beyEtiquetaEstado.setText("Ejercicio eliminado.");
        }
    }

    private void beyVerTecnica(beyEjercicio visible) {
        if (visible == null) {
            return;
        }
        beyEjercicio ejercicio = visible.getAdaptadoDe() != null ? visible : beyOriginal(visible);

        boolean variante = ejercicio.getAdaptadoDe() != null;
        beyGuiaTecnicaDialogo.beyMostrar(ejercicio, ejercicio.getNombre(), beyGestor.beyGetModalidad(), beyVentana(), variante ? null : ruta -> {
            ejercicio.setRutaMultimedia(ruta);
            beyGestor.beyGuardarCambiosEntrenamiento();
            beyTablaEjercicios.refresh();
        });
    }

    private void beyAdjuntarMultimedia(beyEjercicio visible) {
        if (visible == null || beyBloqueadoPorVariante(visible)) {
            return;
        }
        beyEjercicio ejercicio = beyOriginal(visible);
        File archivo = beyArchivoMultimediaService
                .beySelectorMultimedia(beyGestor.beyGetDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(beyVentana());
        if (archivo == null) {
            return;
        }
        ejercicio.setRutaMultimedia(archivo.getAbsolutePath());
        beyGestor.beyRecordarCarpetaMultimedia(archivo.getParent());
        beyGestor.beyGuardarCambiosEntrenamiento();
        beyTablaEjercicios.refresh();
        beyEtiquetaEstado.setText("Multimedia asociada a \"" + ejercicio.getNombre() + "\".");
    }

    @FXML
    private void beyAgregarSugerencia() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || beySeleccionBiblioteca.isEmpty()) {
            return;
        }
        int anadidos = 0;
        for (beyEjercicio sugerido : beySeleccionBiblioteca) {
            beyEjercicio nuevo = new beyEjercicio(sugerido.getNombre(), sugerido.getGrupoMuscular(),
                    sugerido.getSeries(), sugerido.getRepeticionesMin(), sugerido.getRepeticionesMax(),
                    sugerido.getRirObjetivo());
            nuevo.setDescansoSegundos(sugerido.getDescansoSegundos());
            nuevo.setPorTiempo(sugerido.isPorTiempo());
            nuevo.setNotas(beySugerenciasEjerciciosService.beyDescripcionTecnica(sugerido));
            rutina.agregarEjercicio(nuevo);
            anadidos++;
        }
        beyGestor.beyGuardarCambiosEntrenamiento();
        beyMostrarRutina(rutina);
        beyEtiquetaEstado.setText(anadidos + (anadidos == 1 ? " ejercicio añadido a " : " ejercicios añadidos a ")
                + rutina.getNombre() + ".");
        beySeleccionBiblioteca.clear();
        beyCerrarSugerencias();
    }

    @FXML
    private void beyIniciarEntrenamiento() {
        beyRutina rutina = beyListaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || rutina.getEjercicios().isEmpty()) {
            beyDialogos.beyInfo("Rutina vacía", "Añade al menos un ejercicio antes de iniciar el entrenamiento.");
            return;
        }
        beyRutina aEntrenar = beyGestor.beyGetModalidad() == beyModalidadEntrenamiento.CASA
                ? beyAdaptadorEquipoService.beyAdaptarRutina(rutina) : rutina;
        beyWorkoutPlayerController.beyAbrir(aEntrenar, beyVentana(), () -> beyRecargarRutinas(false));
    }

    private beyDiaSemana beyDiaLibre() {
        for (beyDiaSemana dia : beyDiaSemana.values()) {
            boolean ocupado = beyGestor.beyGetDatos().rutinaPorDia(dia).isPresent();
            if (!ocupado) {
                return dia;
            }
        }
        return beyDiaSemana.LUNES;
    }

    private boolean beyAbrirFormularioRutina(beyRutina rutina, String titulo, String textoAceptar) {
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_RUTINA, titulo, textoAceptar, beyVentana());
        beyFormularioRutinaController controlador = (beyFormularioRutinaController) dialogo.beyGetControlador();
        controlador.beyCargar(rutina);
        dialogo.beySetValidador(controlador::beyValidar);
        if (dialogo.beyMostrarYConfirmar()) {
            controlador.beyAplicar();
            return true;
        }
        return false;
    }

    private boolean beyAbrirFormularioEjercicio(beyEjercicio ejercicio, String titulo, String textoAceptar) {
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_EJERCICIO, titulo, textoAceptar, beyVentana());
        beyFormularioEjercicioController controlador = (beyFormularioEjercicioController) dialogo.beyGetControlador();
        controlador.beyCargar(ejercicio);
        dialogo.beySetValidador(controlador::beyValidar);
        if (dialogo.beyMostrarYConfirmar()) {
            controlador.beyAplicar();
            return true;
        }
        return false;
    }

    private Window beyVentana() {
        return beyListaRutinas.getScene() == null ? null : beyListaRutinas.getScene().getWindow();
    }
}
