package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.DiaSemana;
import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.NivelEjercicio;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.SugerenciasEjerciciosService;
import com.symmetry.desktop.util.DiagramaAnatomico;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.FormularioDialogo;
import com.symmetry.desktop.util.VisorMultimedia;
import javafx.animation.TranslateTransition;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Modulo 2 - Entrenamiento: gestion (CRUD) de rutinas y ejercicios precargados o personalizados,
 * adjuncion de multimedia de tecnica y acceso al Workout Player.
 *
 * <p>La tabla de ejercicios ocupa todo el ancho disponible y agrupa las acciones de cada
 * ejercicio (ver tecnica, adjuntar multimedia, editar y eliminar) en iconos dentro de la fila.
 * Las sugerencias por nivel se muestran en un panel lateral (drawer) que se abre bajo demanda.</p>
 */
public class EntrenamientoController implements Initializable, VistaConRefresco {

    private static final String RUTA_FORMULARIO_RUTINA = "/com/symmetry/desktop/view/FormularioRutina.fxml";
    private static final String RUTA_FORMULARIO_EJERCICIO = "/com/symmetry/desktop/view/FormularioEjercicio.fxml";

    /** Desplazamiento horizontal que deja el drawer fuera de la pantalla cuando esta cerrado. */
    private static final double DESPLAZAMIENTO_DRAWER = 380;

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
    private Region scrimSugerencias;
    @FXML
    private VBox panelSugerencias;
    @FXML
    private ComboBox<NivelEjercicio> comboNivelSugerencias;
    @FXML
    private Label etiquetaDescripcionNivel;
    @FXML
    private ListView<Ejercicio> listaSugerencias;
    @FXML
    private Label detalleSugerencia;
    @FXML
    private VBox contenedorAnatomia;
    @FXML
    private Button botonAgregarSugerencia;

    /** Diagrama corporal que resalta el grupo muscular del ejercicio sugerido seleccionado. */
    private final DiagramaAnatomico diagramaSugerencia = new DiagramaAnatomico();

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        columnaNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
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

        configurarSugerencias();
        refrescar();
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
    // Panel lateral (drawer) de sugerencias
    // ------------------------------------------------------------------

    /** Abre o cierra el panel de sugerencias con una transicion de deslizamiento. */
    @FXML
    private void alternarSugerencias() {
        if (panelSugerencias.isVisible()) {
            cerrarSugerencias();
            return;
        }
        panelSugerencias.setVisible(true);
        scrimSugerencias.setVisible(true);
        animarDrawer(DESPLAZAMIENTO_DRAWER, 0, null);
    }

    @FXML
    private void cerrarSugerencias() {
        if (!panelSugerencias.isVisible()) {
            return;
        }
        animarDrawer(panelSugerencias.getTranslateX(), DESPLAZAMIENTO_DRAWER, () -> {
            panelSugerencias.setVisible(false);
            scrimSugerencias.setVisible(false);
        });
    }

    private void animarDrawer(double desde, double hasta, Runnable alTerminar) {
        panelSugerencias.setTranslateX(desde);
        TranslateTransition transicion = new TranslateTransition(Duration.millis(180), panelSugerencias);
        transicion.setFromX(desde);
        transicion.setToX(hasta);
        if (alTerminar != null) {
            transicion.setOnFinished(evento -> alTerminar.run());
        }
        transicion.play();
    }

    /** Panel lateral de sugerencias de ejercicios filtradas por nivel. */
    private void configurarSugerencias() {
        comboNivelSugerencias.setItems(FXCollections.observableArrayList(NivelEjercicio.values()));
        comboNivelSugerencias.setValue(NivelEjercicio.PRINCIPIANTE);
        comboNivelSugerencias.valueProperty().addListener((obs, anterior, actual) -> cargarSugerencias());

        contenedorAnatomia.getChildren().add(diagramaSugerencia);
        contenedorAnatomia.setVisible(false);
        contenedorAnatomia.setManaged(false);

        listaSugerencias.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Ejercicio ejercicio, boolean vacio) {
                super.updateItem(ejercicio, vacio);
                if (vacio || ejercicio == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Cada sugerencia muestra el icono anatomico del grupo muscular que trabaja.
                Node icono = DiagramaAnatomico.iconoMini(ejercicio.getGrupoMuscular());
                Label nombre = new Label(ejercicio.getNombre());
                nombre.getStyleClass().add("sugerencia-nombre");
                Label detalle = new Label(ejercicio.getGrupoMuscular().getEtiqueta()
                        + "  ·  " + ejercicio.getPrescripcion());
                detalle.getStyleClass().add("sugerencia-detalle");
                VBox textos = new VBox(2, nombre, detalle);
                textos.setAlignment(Pos.CENTER_LEFT);
                HBox fila = new HBox(10, icono, textos);
                fila.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(fila);
            }
        });
        listaSugerencias.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> actualizarDetalleSugerencia(actual));
        listaSugerencias.setPlaceholder(new Label("Sin sugerencias para este nivel."));

        cargarSugerencias();
    }

    private void cargarSugerencias() {
        NivelEjercicio nivel = comboNivelSugerencias.getValue();
        etiquetaDescripcionNivel.setText(SugerenciasEjerciciosService.descripcion(nivel));
        List<Ejercicio> sugeridas = SugerenciasEjerciciosService.paraNivel(nivel);
        listaSugerencias.getItems().setAll(sugeridas);
        listaSugerencias.getSelectionModel().clearSelection();
        detalleSugerencia.setText("");
        actualizarBotones();
    }

    private void actualizarDetalleSugerencia(Ejercicio ejercicio) {
        if (ejercicio == null) {
            detalleSugerencia.setText("");
            diagramaSugerencia.limpiar();
            contenedorAnatomia.setVisible(false);
            contenedorAnatomia.setManaged(false);
        } else {
            detalleSugerencia.setText(ejercicio.getRirTexto() + " · descanso "
                    + ejercicio.getDescansoSegundos() + " s");
            diagramaSugerencia.resaltar(ejercicio.getGrupoMuscular(),
                    ejercicio.getGrupoMuscular().getSecundarios());
            contenedorAnatomia.setVisible(true);
            contenedorAnatomia.setManaged(true);
        }
        actualizarBotones();
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
        tablaEjercicios.getItems().setAll(rutina.getEjercicios());
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
        botonAgregarSugerencia.setDisable(rutina == null
                || listaSugerencias.getSelectionModel().getSelectedItem() == null);
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

    private void editarEjercicio(Ejercicio ejercicio) {
        if (ejercicio == null) {
            return;
        }
        if (abrirFormularioEjercicio(ejercicio, "Editar ejercicio", "Guardar")) {
            gestor.guardarCambiosEntrenamiento();
            mostrarRutina(listaRutinas.getSelectionModel().getSelectedItem());
            etiquetaEstado.setText("Ejercicio actualizado.");
        }
    }

    private void eliminarEjercicio(Ejercicio ejercicio) {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || ejercicio == null) {
            return;
        }
        boolean confirmado = Dialogos.confirmar("Eliminar ejercicio",
                "¿Quitar \"" + ejercicio.getNombre() + "\" de " + rutina.getNombre() + "?", ventana());
        if (confirmado) {
            rutina.getEjercicios().remove(ejercicio);
            gestor.guardarCambiosEntrenamiento();
            mostrarRutina(rutina);
            etiquetaEstado.setText("Ejercicio eliminado.");
        }
    }

    private void verTecnica(Ejercicio ejercicio) {
        if (ejercicio == null) {
            return;
        }
        VisorMultimedia.mostrar(ejercicio.getRutaMultimedia(), ejercicio.getEnlaceMultimedia(), ventana(), ruta -> {
            ejercicio.setRutaMultimedia(ruta);
            gestor.guardarCambiosEntrenamiento();
            tablaEjercicios.refresh();
        });
    }

    private void adjuntarMultimedia(Ejercicio ejercicio) {
        if (ejercicio == null) {
            return;
        }
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

    /** Anade el ejercicio sugerido seleccionado como copia editable en la rutina activa. */
    @FXML
    private void agregarSugerencia() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        Ejercicio sugerido = listaSugerencias.getSelectionModel().getSelectedItem();
        if (rutina == null || sugerido == null) {
            return;
        }
        Ejercicio nuevo = new Ejercicio(sugerido.getNombre(), sugerido.getGrupoMuscular(),
                sugerido.getSeries(), sugerido.getRepeticionesMin(), sugerido.getRepeticionesMax(),
                sugerido.getRirObjetivo());
        nuevo.setDescansoSegundos(sugerido.getDescansoSegundos());
        nuevo.setPorTiempo(sugerido.isPorTiempo());
        rutina.agregarEjercicio(nuevo);
        gestor.guardarCambiosEntrenamiento();
        mostrarRutina(rutina);
        etiquetaEstado.setText("Sugerencia \"" + nuevo.getNombre() + "\" añadida a "
                + rutina.getNombre() + ".");
    }

    @FXML
    private void iniciarEntrenamiento() {
        Rutina rutina = listaRutinas.getSelectionModel().getSelectedItem();
        if (rutina == null || rutina.getEjercicios().isEmpty()) {
            Dialogos.info("Rutina vacía", "Añade al menos un ejercicio antes de iniciar el entrenamiento.");
            return;
        }
        WorkoutPlayerController.abrir(rutina, ventana(), () -> recargarRutinas(false));
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
