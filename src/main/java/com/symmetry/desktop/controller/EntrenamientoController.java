package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.DiaSemana;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.ModalidadEntrenamiento;
import com.symmetry.desktop.model.NivelEjercicio;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.service.AdaptadorEquipoService;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.SugerenciasEjerciciosService;
import com.symmetry.desktop.util.DiagramaAnatomico;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.FormularioDialogo;
import com.symmetry.desktop.util.GuiaTecnicaDialogo;
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

/**
 * Modulo 2 - Entrenamiento: gestion (CRUD) de rutinas y ejercicios precargados o personalizados,
 * adjuncion de multimedia de tecnica y acceso al Workout Player.
 *
 * <p>La tabla de ejercicios ocupa todo el ancho disponible y agrupa las acciones de cada
 * ejercicio (ver tecnica, adjuntar multimedia, editar y eliminar) en iconos dentro de la fila.
 * La biblioteca de ejercicios se abre en un modal centrado con una cuadricula de tarjetas de 2 columnas.</p>
 */
public class EntrenamientoController implements Initializable, VistaConRefresco {

    private static final String RUTA_FORMULARIO_RUTINA = "/com/symmetry/desktop/view/FormularioRutina.fxml";
    private static final String RUTA_FORMULARIO_EJERCICIO = "/com/symmetry/desktop/view/FormularioEjercicio.fxml";

    /** Tamano final (en px) de los iconos vectoriales de accion dentro de cada fila. */
    private static final double LADO_ICONO_ACCION = 17;
    private static final double ESCALA_ICONO_ACCION = LADO_ICONO_ACCION / 24.0;

    /* Siluetas vectoriales (viewBox 24x24, estilo Fluent) de las acciones por ejercicio. */
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

    /** Ayuda compartida que identifica la rutina programada para el dia en curso. */
    private static final Tooltip AYUDA_RUTINA_HOY = new Tooltip("Rutina programada para hoy");

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final ObservableList<Rutina> rutinas = FXCollections.observableArrayList();

    @FXML
    private ListView<Rutina> listaRutinas;
    @FXML
    private TableView<Ejercicio> tablaEjercicios;
    @FXML
    private TableColumn<Ejercicio, String> columnaNombre;
    @FXML
    private TableColumn<Ejercicio, String> columnaGrupo;
    @FXML
    private TableColumn<Ejercicio, String> columnaPrescripcion;
    @FXML
    private TableColumn<Ejercicio, String> columnaRir;
    @FXML
    private TableColumn<Ejercicio, Integer> columnaDescanso;
    @FXML
    private TableColumn<Ejercicio, String> columnaMultimedia;
    @FXML
    private TableColumn<Ejercicio, Void> columnaAcciones;
    @FXML
    private Label etiquetaNombreRutina;
    @FXML
    private Label etiquetaDetalleRutina;
    @FXML
    private Label etiquetaNotasRutina;
    @FXML
    private Label etiquetaEstado;
    @FXML
    private Label etiquetaRutinaHoy;
    @FXML
    private Button botonEditarRutina;
    @FXML
    private Button botonEliminarRutina;
    @FXML
    private Button botonAgregarEjercicio;
    @FXML
    private Button botonAbrirSugerencias;
    @FXML
    private Button botonIniciarEntrenamiento;
    @FXML
    private StackPane modalBiblioteca;
    @FXML
    private ComboBox<NivelEjercicio> comboNivelSugerencias;
    @FXML
    private Label etiquetaDescripcionNivel;
    @FXML
    private ScrollPane scrollSugerencias;
    @FXML
    private GridPane cuadriculaSugerencias;
    @FXML
    private Label etiquetaSeleccion;
    @FXML
    private Button botonAgregarSugerencia;
    @FXML
    private ToggleButton botonModalGimnasio;
    @FXML
    private ToggleButton botonModalCasa;
    @FXML
    private ToggleButton botonModoGimnasio;
    @FXML
    private ToggleButton botonModoCasa;
    @FXML
    private Label etiquetaModalidad;

    /** Ejercicios de la biblioteca marcados por el usuario, en el orden en que los eligio. */
    private final Set<Ejercicio> seleccionBiblioteca = new LinkedHashSet<>();

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        columnaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        columnaNombre.setCellFactory(column -> celdaNombre());
        columnaGrupo.setCellValueFactory(new PropertyValueFactory<>("grupoMuscular"));
        columnaPrescripcion.setCellValueFactory(new PropertyValueFactory<>("prescripcion"));
        columnaRir.setCellValueFactory(new PropertyValueFactory<>("rirTexto"));
        columnaDescanso.setCellValueFactory(new PropertyValueFactory<>("descansoSegundos"));
        columnaDescanso.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : valor + " s");
            }
        });
        columnaMultimedia.setCellValueFactory(datos -> new SimpleStringProperty(
                resumenMultimedia(datos.getValue())));
        columnaAcciones.setCellValueFactory(datos -> null);
        columnaAcciones.setCellFactory(column -> celdaAcciones());

        // Todas las columnas se ajustan al ancho disponible: sin scroll horizontal.
        tablaEjercicios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaEjercicios.setPlaceholder(new Label(
                "Esta rutina no tiene ejercicios. Añade uno o usa \"Añadir desde biblioteca\"."));

        listaRutinas.setItems(rutinas);
        listaRutinas.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Rutina rutina, boolean vacio) {
                super.updateItem(rutina, vacio);
                if (vacio || rutina == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                // La rutina programada para hoy se marca con un punto y un tooltip informativo.
                boolean esHoy = rutina.getDiaSemana() == DiaSemana.desdeFecha(LocalDate.now());
                setText((esHoy ? "\u25CF  " : "") + rutina.getTituloCompleto());
                setTooltip(esHoy ? AYUDA_RUTINA_HOY : null);
            }
        });
        listaRutinas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> mostrarRutina(actual));

        configurarModalidad();
        configurarSugerencias();
        refrescar();
    }

    // ------------------------------------------------------------------
    // Modalidad: con equipo (Gym Mode) / sin equipo (Home Mode)
    // ------------------------------------------------------------------

    private void configurarModalidad() {
        agruparSegmento(botonModoGimnasio, botonModoCasa);
        agruparSegmento(botonModalGimnasio, botonModalCasa);
        sincronizarModalidad();
    }

    /** Agrupa dos botones como segmento exclusivo: siempre queda uno activo. */
    private static void agruparSegmento(ToggleButton primero, ToggleButton segundo) {
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
    private void usarModoGimnasio() {
        cambiarModalidad(ModalidadEntrenamiento.GIMNASIO);
    }

    @FXML
    private void usarModoCasa() {
        cambiarModalidad(ModalidadEntrenamiento.CASA);
    }

    private void cambiarModalidad(ModalidadEntrenamiento modalidad) {
        if (gestor.getModalidad() == modalidad) {
            sincronizarModalidad();
            return;
        }
        gestor.setModalidad(modalidad);
        sincronizarModalidad();
        mostrarRutina(listaRutinas.getSelectionModel().getSelectedItem());
        cargarSugerencias();
        etiquetaEstado.setText(modalidad == ModalidadEntrenamiento.CASA
                ? "Modo sin equipo: ejercicios adaptados a peso corporal."
                : "Modo con equipo: rutina original del gimnasio.");
    }

    /** Refleja la modalidad guardada en el selector y en el mensaje explicativo. */
    private void sincronizarModalidad() {
        ModalidadEntrenamiento modalidad = gestor.getModalidad();
        botonModoGimnasio.setSelected(modalidad == ModalidadEntrenamiento.GIMNASIO);
        botonModoCasa.setSelected(modalidad == ModalidadEntrenamiento.CASA);
        botonModalGimnasio.setSelected(modalidad == ModalidadEntrenamiento.GIMNASIO);
        botonModalCasa.setSelected(modalidad == ModalidadEntrenamiento.CASA);
        etiquetaModalidad.setText(modalidad.getDescripcion());
    }

    /** Ejercicios que se muestran y se entrenan: los originales o sus variantes sin equipo. */
    private List<Ejercicio> ejerciciosVisibles(Rutina rutina) {
        if (gestor.getModalidad() == ModalidadEntrenamiento.CASA) {
            return AdaptadorEquipoService.adaptarRutina(rutina).getEjercicios();
        }
        return rutina.getEjercicios();
    }

    /** Ejercicio real de la rutina que corresponde a la fila visible (que puede ser una variante). */
    private Ejercicio original(Ejercicio visible) {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || visible == null) {
            return visible;
        }
        return rutina.getEjercicios().stream()
                .filter(e -> e.getId().equals(visible.getId()))
                .findFirst()
                .orElse(visible);
    }

    /** Las variantes son temporales: para modificarlas hay que volver al modo con equipo. */
    private boolean bloqueadoPorVariante(Ejercicio visible) {
        if (visible != null && visible.getAdaptadoDe() != null) {
            Dialogos.info("Variante sin equipo",
                    "\"" + visible.getNombre() + "\" es una variante automática de \""
                            + visible.getAdaptadoDe() + "\".\nCambia a \"Con equipo\" para editar el ejercicio original.");
            return true;
        }
        return false;
    }

    /** Celda del nombre: si es una variante sin equipo, muestra de que ejercicio proviene. */
    private TableCell<Ejercicio, String> celdaNombre() {
        return new TableCell<>() {
            private final Label nombre = new Label();
            private final Label origen = new Label();
            private final VBox caja = new VBox(1, nombre, origen);

            {
                origen.getStyleClass().add("texto-variante");
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                int indice = getIndex();
                List<Ejercicio> items = getTableView().getItems();
                if (vacio || indice < 0 || indice >= items.size()) {
                    setGraphic(null);
                    return;
                }
                Ejercicio ejercicio = items.get(indice);
                nombre.setText(valor);
                boolean variante = ejercicio.getAdaptadoDe() != null;
                origen.setText(variante ? "\u21BA Adaptado de: " + ejercicio.getAdaptadoDe() : "");
                origen.setVisible(variante);
                origen.setManaged(variante);
                setGraphic(caja);
            }
        };
    }

    // ------------------------------------------------------------------
    // Acciones por fila de la tabla de ejercicios
    // ------------------------------------------------------------------

    /** Celda con los iconos vectoriales de accion de cada ejercicio: ver, adjuntar, editar y eliminar. */
    private TableCell<Ejercicio, Void> celdaAcciones() {
        return new TableCell<>() {
            private final Button botonVer = botonIcono(SVG_VER_TECNICA, "Ver técnica del ejercicio",
                    "boton-icono");
            private final Button botonAdjuntar = botonIcono(SVG_ADJUNTAR, "Adjuntar o cambiar multimedia",
                    "boton-icono");
            private final Button botonEditar = botonIcono(SVG_EDITAR, "Editar ejercicio", "boton-icono");
            private final Button botonEliminar = botonIcono(SVG_ELIMINAR, "Eliminar de la rutina",
                    "boton-icono", "boton-icono-peligro");
            private final HBox caja = new HBox(2, botonVer, botonAdjuntar, botonEditar, botonEliminar);

            {
                caja.setAlignment(Pos.CENTER_LEFT);
                botonVer.setOnAction(evento -> verTecnica(ejercicioDeLaFila()));
                botonAdjuntar.setOnAction(evento -> adjuntarMultimedia(ejercicioDeLaFila()));
                botonEditar.setOnAction(evento -> editarEjercicio(ejercicioDeLaFila()));
                botonEliminar.setOnAction(evento -> eliminarEjercicio(ejercicioDeLaFila()));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : caja);
            }

            private Ejercicio ejercicioDeLaFila() {
                int indice = getIndex();
                List<Ejercicio> items = getTableView().getItems();
                return indice >= 0 && indice < items.size() ? items.get(indice) : null;
            }
        };
    }

    /**
     * Boton de accion reducido a un icono vectorial limpio (estilo Fluent). El glifo SVG se
     * escala a un tamano fijo para que todas las acciones de la fila queden alineadas, y la
     * ayuda se muestra como tooltip informativo al pasar el cursor.
     */
    private static Button botonIcono(String contenidoSvg, String ayuda, String... estilos) {
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

    // ------------------------------------------------------------------
    // Modal centrado: biblioteca de ejercicios (cuadricula de 2 columnas)
    // ------------------------------------------------------------------

    /** Abre la biblioteca como modal centrado sobre el contenido. */
    @FXML
    private void alternarSugerencias() {
        if (modalBiblioteca.isVisible()) {
            cerrarSugerencias();
            return;
        }
        seleccionBiblioteca.clear();
        cargarSugerencias();
        scrollSugerencias.setVvalue(0);
        modalBiblioteca.setManaged(true);
        modalBiblioteca.setVisible(true);
        modalBiblioteca.setOpacity(0);
        FadeTransition aparicion = new FadeTransition(Duration.millis(160), modalBiblioteca);
        aparicion.setToValue(1);
        aparicion.play();
        modalBiblioteca.requestFocus();
    }

    @FXML
    private void cerrarSugerencias() {
        if (!modalBiblioteca.isVisible()) {
            return;
        }
        FadeTransition salida = new FadeTransition(Duration.millis(130), modalBiblioteca);
        salida.setToValue(0);
        salida.setOnFinished(evento -> {
            modalBiblioteca.setVisible(false);
            modalBiblioteca.setManaged(false);
        });
        salida.play();
    }

    private void configurarSugerencias() {
        comboNivelSugerencias.setItems(FXCollections.observableArrayList(NivelEjercicio.values()));
        comboNivelSugerencias.setValue(NivelEjercicio.PRINCIPIANTE);
        comboNivelSugerencias.valueProperty().addListener((obs, anterior, actual) -> {
            seleccionBiblioteca.clear();
            cargarSugerencias();
        });
        // Escape cierra el modal.
        modalBiblioteca.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ESCAPE) {
                cerrarSugerencias();
            }
        });
        cargarSugerencias();
    }

    /** Reconstruye la cuadricula con los ejercicios del nivel y la modalidad activos. */
    private void cargarSugerencias() {
        NivelEjercicio nivel = comboNivelSugerencias.getValue();
        etiquetaDescripcionNivel.setText(SugerenciasEjerciciosService.descripcion(nivel, gestor.getModalidad()));
        seleccionBiblioteca.clear();
        cuadriculaSugerencias.getChildren().clear();
        List<Ejercicio> sugeridas = SugerenciasEjerciciosService.paraNivel(nivel, gestor.getModalidad());
        for (int i = 0; i < sugeridas.size(); i++) {
            cuadriculaSugerencias.add(crearTarjeta(sugeridas.get(i)), i % 2, i / 2);
        }
        actualizarSeleccion();
    }

    /** Tarjeta amplia: icono anatomico grande a la izquierda y datos del ejercicio a la derecha. */
    private VBox crearTarjeta(Ejercicio ejercicio) {
        Node icono = DiagramaAnatomico.iconoGrande(ejercicio.getGrupoMuscular());

        Label nombre = new Label(ejercicio.getNombre());
        nombre.getStyleClass().add("tarjeta-ejercicio-nombre");
        nombre.setWrapText(true);

        Label grupo = new Label(ejercicio.getGrupoMuscular().getEtiqueta());
        grupo.getStyleClass().add("chip-grupo");
        Label prescripcion = new Label(ejercicio.getPrescripcion() + " · " + ejercicio.getRirTexto());
        prescripcion.getStyleClass().add("chip-prescripcion");
        HBox etiquetas = new HBox(6, grupo, prescripcion);
        etiquetas.setAlignment(Pos.CENTER_LEFT);

        Label descripcion = new Label(SugerenciasEjerciciosService.descripcionTecnica(ejercicio));
        descripcion.getStyleClass().add("tarjeta-ejercicio-descripcion");
        descripcion.setWrapText(true);
        descripcion.setMaxHeight(40);

        Label marca = new Label("\u2713");
        marca.getStyleClass().add("tarjeta-ejercicio-marca");
        marca.setVisible(false);

        VBox textos = new VBox(5, nombre, etiquetas, descripcion);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);

        StackPane lateral = new StackPane(marca);
        lateral.setAlignment(Pos.TOP_RIGHT);
        lateral.setMinWidth(22);
        lateral.setMaxWidth(22);

        HBox contenido = new HBox(14, icono, textos, lateral);
        contenido.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(contenido);
        tarjeta.getStyleClass().add("tarjeta-ejercicio");
        tarjeta.setMinHeight(132);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setOnMouseClicked(evento -> {
            boolean marcada = seleccionBiblioteca.add(ejercicio);
            if (!marcada) {
                seleccionBiblioteca.remove(ejercicio);
            }
            tarjeta.getStyleClass().remove("tarjeta-ejercicio-seleccionada");
            if (marcada) {
                tarjeta.getStyleClass().add("tarjeta-ejercicio-seleccionada");
            }
            marca.setVisible(marcada);
            actualizarSeleccion();
        });
        return tarjeta;
    }

    /** Actualiza el contador del pie del modal y el estado del boton de confirmar. */
    private void actualizarSeleccion() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        int total = seleccionBiblioteca.size();
        String destino = rutina == null ? "" : " a " + rutina.getTituloCompleto();
        etiquetaSeleccion.setText(total == 0
                ? "Toca las tarjetas para seleccionar uno o varios ejercicios."
                : total + (total == 1 ? " ejercicio seleccionado" : " ejercicios seleccionados")
                + " · se añadirán" + destino);
        botonAgregarSugerencia.setDisable(rutina == null || total == 0);
    }

    /** Indicador corto del recurso visual: archivo local, YouTube u otro enlace. */
    private static String resumenMultimedia(Ejercicio ejercicio) {
        if (ArchivoMultimediaService.existe(ejercicio.getRutaMultimedia())) {
            return "Archivo local";
        }
        String enlace = ejercicio.getEnlaceMultimedia();
        if (enlace != null && !enlace.isBlank()) {
            return ArchivoMultimediaService.esYoutube(enlace) ? "YouTube" : "Enlace";
        }
        return "—";
    }

    /**
     * Se invoca cada vez que el modulo pasa a primer plano: selecciona automaticamente la
     * rutina programada para el dia actual para que el entrenamiento de hoy quede listo sin
     * clics manuales.
     */
    @Override
    public void refrescar() {
        recargarRutinas(true);
    }

    /**
     * Recarga la lista de rutinas desde el gestor. Cuando {@code seleccionarHoy} es verdadero
     * se prioriza la rutina del dia en curso; en caso contrario se conserva la seleccion previa
     * (por ejemplo, tras editar o eliminar una rutina).
     */
    private void recargarRutinas(boolean seleccionarHoy) {
        Rutina seleccionada = listaRutinas.getSelectionModel().getSelectedItem();
        rutinas.setAll(gestor.getDatos().getRutinas());
        rutinas.sort(Comparator.comparingInt(r -> r.getDiaSemana() == null ? 99 : r.getDiaSemana().ordinal()));

        Rutina aSeleccionar = seleccionarHoy ? rutinaDelDia() : null;
        if (aSeleccionar == null) {
            aSeleccionar = rutinas.stream()
                    .filter(r -> seleccionada != null && r.getId().equals(seleccionada.getId()))
                    .findFirst()
                    .orElse(rutinas.isEmpty() ? null : rutinas.get(0));
        }
        listaRutinas.getSelectionModel().select(aSeleccionar);
        mostrarRutina(aSeleccionar);
        etiquetaEstado.setText(rutinas.size() + " rutinas · " + gestor.getDatos().catalogoEjercicios().size()
                + " ejercicios en catálogo");
    }

    /** Rutina programada para el dia de la semana en curso, o {@code null} si es dia de descanso. */
    private Rutina rutinaDelDia() {
        DiaSemana hoy = DiaSemana.desdeFecha(LocalDate.now());
        return rutinas.stream().filter(r -> r.getDiaSemana() == hoy).findFirst().orElse(null);
    }

    private void mostrarRutina(Rutina rutina) {
        if (rutina == null) {
            etiquetaNombreRutina.setText("Sin rutina seleccionada");
            etiquetaDetalleRutina.setText("");
            etiquetaNotasRutina.setText("");
            tablaEjercicios.getItems().clear();
            marcarRutinaHoy(false);
            actualizarBotones();
            return;
        }
        etiquetaNombreRutina.setText(rutina.getTituloCompleto());
        etiquetaDetalleRutina.setText(rutina.getEjercicios().size() + " ejercicios · "
                + rutina.getSeriesTotales() + " series prescritas");
        etiquetaNotasRutina.setText(rutina.getNotas() == null ? "" : rutina.getNotas());
        tablaEjercicios.getItems().setAll(ejerciciosVisibles(rutina));
        marcarRutinaHoy(rutina.getDiaSemana() == DiaSemana.desdeFecha(LocalDate.now()));
        actualizarBotones();
    }

    /** Muestra u oculta el distintivo "Programada para hoy" de la rutina activa. */
    private void marcarRutinaHoy(boolean esHoy) {
        etiquetaRutinaHoy.setVisible(esHoy);
        etiquetaRutinaHoy.setManaged(esHoy);
    }

    private void actualizarBotones() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();

        botonEditarRutina.setDisable(rutina == null);
        botonEliminarRutina.setDisable(rutina == null);
        botonAgregarEjercicio.setDisable(rutina == null);
        botonAbrirSugerencias.setDisable(rutina == null);
        botonIniciarEntrenamiento.setDisable(rutina == null || rutina.getEjercicios().isEmpty());
        actualizarSeleccion();
    }

    // ------------------------------------------------------------------
    // Acciones sobre rutinas
    // ------------------------------------------------------------------

    @FXML
    private void nuevaRutina() {
        Rutina rutina = new Rutina();
        rutina.setDiaSemana(diaLibre());
        if (abrirFormularioRutina(rutina, "Nueva rutina", "Crear")) {
            gestor.agregarRutina(rutina);
            recargarRutinas(false);
            etiquetaEstado.setText("Rutina \"" + rutina.getNombre() + "\" creada.");
        }
    }

    @FXML
    private void editarRutina() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        if (abrirFormularioRutina(rutina, "Editar rutina", "Guardar")) {
            gestor.guardarCambiosEntrenamiento();
            recargarRutinas(false);
            etiquetaEstado.setText("Rutina actualizada.");
        }
    }

    @FXML
    private void eliminarRutina() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        boolean confirmado = Dialogos.confirmar("Eliminar rutina",
                "¿Eliminar \"" + rutina.getTituloCompleto() + "\" y todos sus ejercicios?",
                ventana());
        if (confirmado) {
            gestor.eliminarRutina(rutina);
            recargarRutinas(false);
            etiquetaEstado.setText("Rutina eliminada.");
        }
    }

    // ------------------------------------------------------------------
    // Acciones sobre ejercicios
    // ------------------------------------------------------------------

    @FXML
    private void agregarEjercicio() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null) {
            return;
        }
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setDescansoSegundos(gestor.getDatos().getConfiguracion().getDescansoPorDefectoSegundos());
        if (abrirFormularioEjercicio(ejercicio, "Nuevo ejercicio", "Añadir")) {
            rutina.agregarEjercicio(ejercicio);
            gestor.guardarCambiosEntrenamiento();
            mostrarRutina(rutina);
            etiquetaEstado.setText("Ejercicio \"" + ejercicio.getNombre() + "\" añadido.");
        }
    }

    private void editarEjercicio(Ejercicio visible) {
        if (visible == null || bloqueadoPorVariante(visible)) {
            return;
        }
        Ejercicio ejercicio = original(visible);
        if (abrirFormularioEjercicio(ejercicio, "Editar ejercicio", "Guardar")) {
            gestor.guardarCambiosEntrenamiento();
            mostrarRutina(listaRutinas.getSelectionModel().getSelectedItem());
            etiquetaEstado.setText("Ejercicio actualizado.");
        }
    }

    private void eliminarEjercicio(Ejercicio visible) {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || visible == null) {
            return;
        }
        Ejercicio ejercicio = original(visible);
        boolean confirmado = Dialogos.confirmar("Eliminar ejercicio",
                "¿Quitar \"" + ejercicio.getNombre() + "\" de " + rutina.getNombre() + "?", ventana());
        if (confirmado) {
            rutina.getEjercicios().remove(ejercicio);
            gestor.guardarCambiosEntrenamiento();
            mostrarRutina(rutina);
            etiquetaEstado.setText("Ejercicio eliminado.");
        }
    }

    private void verTecnica(Ejercicio visible) {
        if (visible == null) {
            return;
        }
        Ejercicio ejercicio = visible.getAdaptadoDe() != null ? visible : original(visible);
        // Las variantes sin equipo son temporales: no se les puede adjuntar multimedia.
        boolean variante = ejercicio.getAdaptadoDe() != null;
        GuiaTecnicaDialogo.mostrar(ejercicio, ejercicio.getNombre(), ventana(), variante ? null : ruta -> {
            ejercicio.setRutaMultimedia(ruta);
            gestor.guardarCambiosEntrenamiento();
            tablaEjercicios.refresh();
        });
    }

    private void adjuntarMultimedia(Ejercicio visible) {
        if (visible == null || bloqueadoPorVariante(visible)) {
            return;
        }
        Ejercicio ejercicio = original(visible);
        File archivo = ArchivoMultimediaService
                .selectorMultimedia(gestor.getDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(ventana());
        if (archivo == null) {
            return;
        }
        ejercicio.setRutaMultimedia(archivo.getAbsolutePath());
        gestor.recordarCarpetaMultimedia(archivo.getParent());
        gestor.guardarCambiosEntrenamiento();
        tablaEjercicios.refresh();
        etiquetaEstado.setText("Multimedia asociada a \"" + ejercicio.getNombre() + "\".");
    }

    /** Anade los ejercicios marcados en la biblioteca como copias editables de la rutina activa. */
    @FXML
    private void agregarSugerencia() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || seleccionBiblioteca.isEmpty()) {
            return;
        }
        int anadidos = 0;
        for (Ejercicio sugerido : seleccionBiblioteca) {
            Ejercicio nuevo = new Ejercicio(sugerido.getNombre(), sugerido.getGrupoMuscular(),
                    sugerido.getSeries(), sugerido.getRepeticionesMin(), sugerido.getRepeticionesMax(),
                    sugerido.getRirObjetivo());
            nuevo.setDescansoSegundos(sugerido.getDescansoSegundos());
            nuevo.setPorTiempo(sugerido.isPorTiempo());
            nuevo.setNotas(SugerenciasEjerciciosService.descripcionTecnica(sugerido));
            rutina.agregarEjercicio(nuevo);
            anadidos++;
        }
        gestor.guardarCambiosEntrenamiento();
        mostrarRutina(rutina);
        etiquetaEstado.setText(anadidos + (anadidos == 1 ? " ejercicio añadido a " : " ejercicios añadidos a ")
                + rutina.getNombre() + ".");
        seleccionBiblioteca.clear();
        cerrarSugerencias();
    }

    @FXML
    private void iniciarEntrenamiento() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || rutina.getEjercicios().isEmpty()) {
            Dialogos.info("Rutina vacía", "Añade al menos un ejercicio antes de iniciar el entrenamiento.");
            return;
        }
        Rutina aEntrenar = gestor.getModalidad() == ModalidadEntrenamiento.CASA
                ? AdaptadorEquipoService.adaptarRutina(rutina) : rutina;
        WorkoutPlayerController.abrir(aEntrenar, ventana(), () -> recargarRutinas(false));
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    private DiaSemana diaLibre() {
        for (DiaSemana dia : DiaSemana.values()) {
            boolean ocupado = gestor.getDatos().rutinaPorDia(dia).isPresent();
            if (!ocupado) {
                return dia;
            }
        }
        return DiaSemana.LUNES;
    }

    private boolean abrirFormularioRutina(Rutina rutina, String titulo, String textoAceptar) {
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_RUTINA, titulo, textoAceptar, ventana());
        FormularioRutinaController controlador = (FormularioRutinaController) dialogo.getControlador();
        controlador.cargar(rutina);
        dialogo.setValidador(controlador::validar);
        if (dialogo.mostrarYConfirmar()) {
            controlador.aplicar();
            return true;
        }
        return false;
    }

    private boolean abrirFormularioEjercicio(Ejercicio ejercicio, String titulo, String textoAceptar) {
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_EJERCICIO, titulo, textoAceptar, ventana());
        FormularioEjercicioController controlador = (FormularioEjercicioController) dialogo.getControlador();
        controlador.cargar(ejercicio);
        dialogo.setValidador(controlador::validar);
        if (dialogo.mostrarYConfirmar()) {
            controlador.aplicar();
            return true;
        }
        return false;
    }

    private Window ventana() {
        return listaRutinas.getScene() == null ? null : listaRutinas.getScene().getWindow();
    }
}
