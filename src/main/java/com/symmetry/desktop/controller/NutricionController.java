package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.Comida;
import com.symmetry.desktop.model.DiaSalud;
import com.symmetry.desktop.model.ObjetivosNutricionales;
import com.symmetry.desktop.model.Recipiente;
import com.symmetry.desktop.model.RegistroHidratacion;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.util.Confeti;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.Formato;
import com.symmetry.desktop.util.FormularioDialogo;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Modulo 3 - Nutricion, hidratacion y vida saludable: objetivos diarios con iconos vectoriales
 * que se rellenan de forma animada, registro de comidas, hidratacion rapida por envases
 * comerciales, creatina y descanso.
 *
 * <p>El "Resumen del dia" ya no usa barras horizontales: cada indicador es una tarjeta con un
 * icono (vaso, shaker, llama, suplemento y luna) que se rellena de abajo hacia arriba segun el
 * progreso real. La tabla de comidas y la lista de hidratacion ajustan su altura al numero de
 * registros y permiten eliminar cada fila con un icono de papelera.</p>
 */
public class NutricionController implements Initializable, VistaConRefresco {

    private static final String RUTA_FORMULARIO_COMIDA = "/com/symmetry/desktop/view/FormularioComida.fxml";

    /** Siluetas SVG (viewBox 64x64) de cada indicador. */
    private static final String SVG_AGUA =
            "M12,10 L52,10 L46,58 Q45,62 41,62 L23,62 Q19,62 18,58 Z";
    private static final String SVG_PROTEINA =
            "M22,12 L42,12 L42,24 L22,24 Z M20,26 L44,26 L44,58 Q44,62 40,62 L24,62 Q20,62 20,58 Z";
    private static final String SVG_CALORIAS =
            "M32,6 C36,16 46,20 47,32 C48,44 41,54 32,54 C23,54 16,44 17,32 C18,20 28,16 32,6 Z";
    private static final String SVG_CREATINA =
            "M16,12 L48,12 L48,24 L16,24 Z M18,26 L46,26 L46,58 Q46,62 42,62 L22,62 Q18,62 18,58 Z";
    private static final String SVG_DESCANSO =
            "M56,34.1 A24,24 0 1 1 29.9,8 A18.7,18.7 0 0 0 56,34.1 Z";

    /** Clases de color del liquido, definidas en la hoja de estilos. */
    private static final String COLOR_AGUA = "liquido-agua";
    private static final String COLOR_PROTEINA = "liquido-proteina";
    private static final String COLOR_CALORIAS = "liquido-calorias";
    private static final String COLOR_CREATINA = "liquido-creatina";
    private static final String COLOR_DESCANSO = "liquido-descanso";

    /** Metricas verticales usadas para que la tabla y la lista crezcan con su contenido. */
    private static final double ALTO_CABECERA_TABLA = 37;
    private static final double ALTO_FILA_TABLA = 34;
    private static final double ALTO_TABLA_MIN = 130;
    private static final double ALTO_TABLA_MAX = 420;
    private static final double ALTO_FILA_LISTA = 36;
    private static final double ALTO_LISTA_MIN = 110;
    private static final double ALTO_LISTA_MAX = 280;

    private final GestorDatos gestor = GestorDatos.getInstancia();

    /** Metas de la fecha en curso que ya se celebraron, para no repetir el efecto. */
    private final Set<String> metasCelebradas = new HashSet<>();
    private LocalDate fechaEstado;

    @FXML
    private StackPane raizNutricion;
    @FXML
    private Pane capaConfeti;
    @FXML
    private DatePicker selectorFecha;
    @FXML
    private Label etiquetaResumenDia;
    @FXML
    private HBox rejillaResumen;
    @FXML
    private TableView<Comida> tablaComidas;
    @FXML
    private GridPane contenedorEnvases;
    @FXML
    private TextField campoCantidadPersonalizada;
    @FXML
    private ListView<RegistroHidratacion> listaHidratacion;
    @FXML
    private CheckBox checkCreatina;
    @FXML
    private Label detalleCreatina;
    @FXML
    private Spinner<Double> spinnerSueno;
    @FXML
    private Label detalleSueno;

    private IndicadorResumen indicadorAgua;
    private IndicadorResumen indicadorProteina;
    private IndicadorResumen indicadorCalorias;
    private IndicadorResumen indicadorCreatina;
    private IndicadorResumen indicadorDescanso;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        selectorFecha.setValue(LocalDate.now());
        spinnerSueno.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 14, 8, 0.5));

        construirResumenDelDia();
        configurarTablaComidas();
        construirBotonesEnvases();
        configurarListaHidratacion();

        refrescar();
    }

    @Override
    public void refrescar() {
        LocalDate fecha = fechaSeleccionada();
        DiaSalud dia = gestor.getDatos().buscarDia(fecha).orElseGet(() -> new DiaSalud(fecha));
        ObjetivosNutricionales objetivos = gestor.getDatos().getPerfil().getObjetivos();

        etiquetaResumenDia.setText(Formato.fechaLarga(fecha));

        List<MetaDiaria> metas = new ArrayList<>();
        metas.add(new MetaDiaria("proteina", "Proteína", indicadorProteina, actualizarProteina(dia, objetivos)));
        metas.add(new MetaDiaria("calorias", "Calorías", indicadorCalorias, actualizarCalorias(dia, objetivos)));
        metas.add(new MetaDiaria("agua", "Hidratación", indicadorAgua, actualizarHidratacion(dia, objetivos)));
        metas.add(new MetaDiaria("creatina", "Creatina", indicadorCreatina, actualizarCreatina(dia, objetivos)));
        metas.add(new MetaDiaria("descanso", "Descanso", indicadorDescanso, actualizarSueno(dia, objetivos)));

        tablaComidas.setItems(FXCollections.observableArrayList(dia.getComidas()));
        listaHidratacion.setItems(FXCollections.observableArrayList(dia.getHidratacion()));
        ajustarAlturaComidas();
        ajustarAlturaHidratacion();

        celebrarMetasNuevas(fecha, metas);
    }

    // ------------------------------------------------------------------
    // Resumen del dia (tarjetas con iconos de llenado animado)
    // ------------------------------------------------------------------

    /** Reune los cinco indicadores como tarjetas dentro de la rejilla del resumen. */
    private void construirResumenDelDia() {
        indicadorAgua = new IndicadorResumen("Agua", COLOR_AGUA, SVG_AGUA);
        indicadorProteina = new IndicadorResumen("Proteína", COLOR_PROTEINA, SVG_PROTEINA);
        indicadorCalorias = new IndicadorResumen("Calorías", COLOR_CALORIAS, SVG_CALORIAS);
        indicadorCreatina = new IndicadorResumen("Creatina", COLOR_CREATINA, SVG_CREATINA);
        indicadorDescanso = new IndicadorResumen("Descanso", COLOR_DESCANSO, SVG_DESCANSO);
        rejillaResumen.getChildren().setAll(
                indicadorAgua, indicadorProteina, indicadorCalorias, indicadorCreatina, indicadorDescanso);
    }

    private boolean actualizarProteina(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double total = dia.getTotalProteina();
        double meta = objetivos.getProteinaGramos();
        boolean exceso = total > meta;
        return indicadorProteina.mostrar(Formato.numero(total) + " g",
                exceso ? "+" + Formato.numero(total - meta) + " g" : "Meta " + Formato.numero(meta) + " g",
                fraccion(total, meta), exceso);
    }

    private boolean actualizarCalorias(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double total = dia.getTotalCalorias();
        double meta = objetivos.getCalorias();
        boolean exceso = total > meta;
        return indicadorCalorias.mostrar(Formato.numero(total) + " kcal",
                exceso ? "+" + Formato.numero(total - meta) + " kcal" : "Meta " + Formato.numero(meta) + " kcal",
                fraccion(total, meta), exceso);
    }

    private boolean actualizarHidratacion(DiaSalud dia, ObjetivosNutricionales objetivos) {
        int total = dia.getTotalHidratacionMl();
        int meta = objetivos.getAguaMililitros();
        boolean exceso = total > meta;
        return indicadorAgua.mostrar(total + " ml",
                exceso ? "+" + (total - meta) + " ml" : "Meta " + meta + " ml",
                fraccion(total, meta), exceso);
    }

    private boolean actualizarCreatina(DiaSalud dia, ObjetivosNutricionales objetivos) {
        checkCreatina.setSelected(dia.isCreatinaTomada());
        boolean tomada = dia.isCreatinaTomada();
        detalleCreatina.setText(tomada
                ? "Dosis registrada. Mantén una toma diaria constante."
                : "Aún no registras la dosis de hoy.");
        return indicadorCreatina.mostrar(
                tomada ? Formato.numero(objetivos.getCreatinaGramos()) + " g" : "0 g",
                tomada ? "Dosis registrada" : "Dosis pendiente",
                tomada ? 1 : 0, false);
    }

    private boolean actualizarSueno(DiaSalud dia, ObjetivosNutricionales objetivos) {
        double horas = dia.getHorasSueno();
        double min = objetivos.getSuenoMinimoHoras();
        double max = objetivos.getSuenoMaximoHoras();
        boolean exceso = horas > max;
        detalleSueno.setText("Objetivo " + objetivos.getSuenoTexto()
                + (horas <= 0 ? " · sin registrar" : ""));
        if (horas > 0) {
            spinnerSueno.getValueFactory().setValue(Math.min(14, horas));
        }
        return indicadorDescanso.mostrar(Formato.numero(horas) + " h",
                exceso ? "+" + Formato.numero(horas - max) + " h" : "Objetivo " + objetivos.getSuenoTexto(),
                fraccion(horas, min), exceso);
    }

    private static double fraccion(double valor, double meta) {
        return meta <= 0 ? 0 : Math.max(0, valor / meta);
    }

    // ------------------------------------------------------------------
    // Celebracion de metas diarias (confeti, insignias y destellos)
    // ------------------------------------------------------------------

    /**
     * Compara las metas cumplidas ahora con las ya celebradas en la fecha mostrada y lanza el
     * efecto de celebracion solo cuando alguna se alcanza por primera vez (al llegar al 100 %
     * o superarla). Al cambiar de fecha, el estado se reinicia sin celebrar lo ya conseguido.
     */
    private void celebrarMetasNuevas(LocalDate fecha, List<MetaDiaria> metas) {
        boolean cambioDeFecha = fechaEstado == null || !fechaEstado.equals(fecha);
        if (cambioDeFecha) {
            fechaEstado = fecha;
            metasCelebradas.clear();
        }

        List<MetaDiaria> nuevas = new ArrayList<>();
        for (MetaDiaria meta : metas) {
            if (meta.cumplida()) {
                if (metasCelebradas.add(meta.clave()) && !cambioDeFecha) {
                    nuevas.add(meta);
                }
            } else {
                metasCelebradas.remove(meta.clave());
            }
        }
        if (nuevas.isEmpty()) {
            return;
        }
        for (MetaDiaria meta : nuevas) {
            meta.tarjeta().celebrar();
        }
        Confeti.estallar(capaConfeti);
        mostrarInsignia(nuevas);
    }

    /** Muestra un distintivo flotante de "meta alcanzada" que aparece y se desvanece. */
    private void mostrarInsignia(List<MetaDiaria> metas) {
        Label simbolo = new Label("\u2B50");
        simbolo.getStyleClass().add("insignia-simbolo");
        Label titulo = new Label("¡Meta diaria alcanzada!");
        titulo.getStyleClass().add("insignia-titulo");
        Label detalle = new Label(String.join(" · ", metas.stream().map(MetaDiaria::nombre).toList()));
        detalle.getStyleClass().add("insignia-detalle");
        VBox textos = new VBox(2, titulo, detalle);
        textos.setAlignment(Pos.CENTER_LEFT);

        HBox insignia = new HBox(10, simbolo, textos);
        insignia.getStyleClass().add("insignia-victoria");
        insignia.setAlignment(Pos.CENTER_LEFT);
        insignia.setMouseTransparent(true);
        StackPane.setAlignment(insignia, Pos.TOP_CENTER);

        insignia.setTranslateY(70);
        insignia.setOpacity(0);
        raizNutricion.getChildren().add(insignia);

        Timeline aparicion = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(insignia.opacityProperty(), 0),
                        new KeyValue(insignia.translateYProperty(), 70)),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(insignia.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(insignia.translateYProperty(), 20, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(insignia.opacityProperty(), 1),
                        new KeyValue(insignia.translateYProperty(), 20)),
                new KeyFrame(Duration.millis(2500),
                        new KeyValue(insignia.opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(insignia.translateYProperty(), 0, Interpolator.EASE_IN)));
        aparicion.setOnFinished(evento -> raizNutricion.getChildren().remove(insignia));
        aparicion.play();
    }

    /** Meta diaria evaluada en el resumen, con la tarjeta asociada para el destello. */
    private record MetaDiaria(String clave, String nombre, IndicadorResumen tarjeta, boolean cumplida) {
    }

    // ------------------------------------------------------------------
    // Comidas
    // ------------------------------------------------------------------

    private void configurarTablaComidas() {
        TableColumn<Comida, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(datos -> new SimpleStringProperty(Formato.hora(datos.getValue().getFechaHora())));
        colHora.setPrefWidth(90);

        TableColumn<Comida, String> colNombre = new TableColumn<>("Comida");
        colNombre.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getNombre()));
        colNombre.setPrefWidth(280);

        TableColumn<Comida, String> colProteina = new TableColumn<>("Proteína");
        colProteina.setCellValueFactory(datos -> new SimpleStringProperty(
                Formato.numero(datos.getValue().getProteinaGramos()) + " g"));
        colProteina.setPrefWidth(110);

        TableColumn<Comida, String> colCalorias = new TableColumn<>("Calorías");
        colCalorias.setCellValueFactory(datos -> new SimpleStringProperty(
                Formato.numero(datos.getValue().getCalorias()) + " kcal"));
        colCalorias.setPrefWidth(120);

        TableColumn<Comida, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(90);
        colAcciones.setSortable(false);
        colAcciones.setResizable(false);
        colAcciones.setCellValueFactory(datos -> null);
        colAcciones.setCellFactory(columna -> celdaPapelera());

        List<TableColumn<Comida, ?>> columnas = new ArrayList<>();
        columnas.add(colHora);
        columnas.add(colNombre);
        columnas.add(colProteina);
        columnas.add(colCalorias);
        columnas.add(colAcciones);
        tablaComidas.getColumns().setAll(columnas);
        tablaComidas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaComidas.setPlaceholder(new Label("Sin comidas registradas para este día."));
    }

    /** Celda con el icono de papelera para eliminar la comida de esa fila. */
    private TableCell<Comida, Void> celdaPapelera() {
        return new TableCell<>() {
            private final Button boton = botonPapelera("Eliminar esta comida");

            {
                boton.setOnAction(evento -> eliminarComida(comidaDeLaFila()));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : boton);
            }

            private Comida comidaDeLaFila() {
                int indice = getIndex();
                List<Comida> items = getTableView().getItems();
                return indice >= 0 && indice < items.size() ? items.get(indice) : null;
            }
        };
    }

    @FXML
    private void agregarComida() {
        Comida comida = new Comida("", 0, 0, LocalDateTime.now());
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_COMIDA,
                "Registrar comida", "Añadir", ventana());
        FormularioComidaController controlador = (FormularioComidaController) dialogo.getControlador();
        controlador.cargar(comida);
        dialogo.setValidador(controlador::validar);
        if (dialogo.mostrarYConfirmar()) {
            controlador.aplicar();
            LocalDate fecha = fechaSeleccionada();
            comida.setFechaHora(LocalDateTime.of(fecha, LocalTime.now()));
            gestor.registrarComida(comida);
            refrescar();
        }
    }

    private void eliminarComida(Comida comida) {
        if (comida == null) {
            return;
        }
        if (Dialogos.confirmar("Eliminar comida", "¿Eliminar \"" + comida.getNombre() + "\"?", ventana())) {
            gestor.eliminarComida(fechaSeleccionada(), comida);
            refrescar();
        }
    }

    // ------------------------------------------------------------------
    // Hidratacion
    // ------------------------------------------------------------------

    private void construirBotonesEnvases() {
        contenedorEnvases.getChildren().clear();
        int columna = 0;
        int fila = 0;
        for (Recipiente recipiente : Recipiente.values()) {
            if (recipiente == Recipiente.PERSONALIZADO) {
                continue;
            }
            Button boton = new Button(nombreCorto(recipiente) + "\n" + recipiente.getMililitros() + " ml");
            boton.getStyleClass().add("boton-envase");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setWrapText(true);
            boton.setOnAction(evento -> registrarHidratacion(recipiente));
            GridPane.setHgrow(boton, Priority.ALWAYS);
            contenedorEnvases.add(boton, columna, fila);
            if (++columna == 2) {
                columna = 0;
                fila++;
            }
        }
    }

    /** Nombre corto del envase para que el boton de un clic quede legible. */
    private static String nombreCorto(Recipiente recipiente) {
        return switch (recipiente) {
            case VASO -> "Vaso";
            case BOTELLA_MEDIANA -> "Botella mediana";
            case BOTELLA_PERSONAL -> "Botella personal";
            case TOMATODO -> "Tomatodo";
            case PERSONALIZADO -> "Otros";
        };
    }

    /**
     * Cada registro de hidratacion se muestra con su detalle y una papelera para eliminarlo
     * sin salir de la fila.
     */
    private void configurarListaHidratacion() {
        listaHidratacion.setCellFactory(lista -> new ListCell<>() {
            private final Label texto = new Label();
            private final Region espaciador = new Region();
            private final Button boton = botonPapelera("Eliminar este registro");
            private final HBox caja = new HBox(8, texto, espaciador, boton);

            {
                HBox.setHgrow(espaciador, Priority.ALWAYS);
                caja.setAlignment(Pos.CENTER_LEFT);
                boton.setOnAction(evento -> eliminarHidratacion(getItem()));
            }

            @Override
            protected void updateItem(RegistroHidratacion registro, boolean vacio) {
                super.updateItem(registro, vacio);
                if (vacio || registro == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                texto.setText(Formato.hora(registro.getFechaHora()) + "  ·  " + registro.getMililitros()
                        + " ml  ·  " + registro.getRecipiente().getEtiqueta());
                setText(null);
                setGraphic(caja);
            }
        });
        listaHidratacion.setPlaceholder(new Label("Sin registros de hidratación."));
    }

    private void registrarHidratacion(Recipiente recipiente) {
        gestor.registrarHidratacion(fechaSeleccionada(), recipiente.getMililitros(), recipiente);
        refrescar();
    }

    @FXML
    private void registrarCantidadPersonalizada() {
        int mililitros = (int) Math.round(Formato.aNumero(campoCantidadPersonalizada.getText()));
        if (mililitros <= 0) {
            Dialogos.info("Cantidad no válida", "Introduce una cantidad de agua en mililitros mayor que cero.");
            return;
        }
        gestor.registrarHidratacion(fechaSeleccionada(), mililitros, Recipiente.PERSONALIZADO);
        campoCantidadPersonalizada.clear();
        refrescar();
    }

    private void eliminarHidratacion(RegistroHidratacion registro) {
        if (registro == null) {
            return;
        }
        gestor.eliminarHidratacion(fechaSeleccionada(), registro);
        refrescar();
    }

    // ------------------------------------------------------------------
    // Creatina, descanso y navegacion de fecha
    // ------------------------------------------------------------------

    @FXML
    private void alternarCreatina() {
        gestor.setCreatinaTomada(fechaSeleccionada(), checkCreatina.isSelected());
        refrescar();
    }

    @FXML
    private void registrarSueno() {
        Double horas = spinnerSueno.getValue();
        gestor.registrarSueno(fechaSeleccionada(), horas == null ? 0 : horas);
        refrescar();
    }

    /** Registra de inmediato las horas indicadas en un boton rapido (6, 7, 8 o 9 h). */
    @FXML
    private void registrarSuenoRapido(ActionEvent evento) {
        Object dato = ((Button) evento.getSource()).getUserData();
        double horas = Formato.aNumero(String.valueOf(dato));
        spinnerSueno.getValueFactory().setValue(horas);
        gestor.registrarSueno(fechaSeleccionada(), horas);
        refrescar();
    }

    @FXML
    private void cambiarFecha() {
        refrescar();
    }

    @FXML
    private void irAHoy() {
        selectorFecha.setValue(LocalDate.now());
        refrescar();
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    /** Ajusta la altura de la tabla de comidas al numero real de filas (sin espacio vacio). */
    private void ajustarAlturaComidas() {
        double altura = ALTO_CABECERA_TABLA + tablaComidas.getItems().size() * ALTO_FILA_TABLA + 2;
        tablaComidas.setPrefHeight(Math.max(ALTO_TABLA_MIN, Math.min(ALTO_TABLA_MAX, altura)));
    }

    /** Ajusta la altura de la lista de hidratacion al numero real de registros. */
    private void ajustarAlturaHidratacion() {
        double altura = listaHidratacion.getItems().size() * ALTO_FILA_LISTA + 2;
        listaHidratacion.setPrefHeight(Math.max(ALTO_LISTA_MIN, Math.min(ALTO_LISTA_MAX, altura)));
    }

    private static Button botonPapelera(String ayuda) {
        Button boton = new Button("\uD83D\uDDD1");
        boton.getStyleClass().addAll("boton-icono", "boton-icono-peligro");
        boton.setFocusTraversable(false);
        boton.setTooltip(new Tooltip(ayuda));
        return boton;
    }

    private LocalDate fechaSeleccionada() {
        return selectorFecha.getValue() == null ? LocalDate.now() : selectorFecha.getValue();
    }

    private Window ventana() {
        return selectorFecha.getScene() == null ? null : selectorFecha.getScene().getWindow();
    }

    // ------------------------------------------------------------------
    // Componentes del resumen: tarjeta + icono de llenado animado
    // ------------------------------------------------------------------

    /**
     * Tarjeta de un indicador: icono vectorial rellenable, nombre, valor y meta.
     * Se construye por completo en codigo para mantener el FXML limpio.
     */
    private static final class IndicadorResumen extends VBox {

        private final IndicadorIcono icono;
        private final Label valor = new Label();
        private final Label meta = new Label();
        private final Label insignia = new Label("\u2714");
        private ScaleTransition destello;

        IndicadorResumen(String titulo, String claseColor, String rutaSvg) {
            this.icono = new IndicadorIcono(rutaSvg, claseColor);

            Label nombre = new Label(titulo);
            nombre.getStyleClass().add("indicador-nombre");
            valor.getStyleClass().add("indicador-valor");
            meta.getStyleClass().add("indicador-meta");

            // Insignia de victoria superpuesta al icono cuando la meta esta cumplida.
            insignia.getStyleClass().add("indicador-insignia");
            insignia.setVisible(false);
            StackPane marcoIcono = new StackPane(icono.nodo(), insignia);
            StackPane.setAlignment(insignia, Pos.TOP_RIGHT);
            insignia.setTranslateX(4);
            insignia.setTranslateY(-2);

            setSpacing(6);
            setAlignment(Pos.CENTER);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("tarjeta-indicador");
            getChildren().addAll(marcoIcono, nombre, valor, meta);
            HBox.setHgrow(this, Priority.ALWAYS);
        }

        /**
         * Actualiza textos, estado de exito/exceso y dispara la animacion de llenado.
         *
         * @return {@code true} si la meta se alcanzo al 100 % o se supero.
         */
        boolean mostrar(String valorTexto, String metaTexto, double fraccion, boolean exceso) {
            valor.setText(valorTexto);
            meta.setText(metaTexto);

            boolean cumplida = fraccion >= 0.999;
            getStyleClass().remove("indicador-cumplido");
            if (cumplida) {
                getStyleClass().add("indicador-cumplido");
            }
            insignia.setVisible(cumplida);
            meta.getStyleClass().remove("indicador-meta-exceso");
            if (exceso) {
                meta.getStyleClass().add("indicador-meta-exceso");
            }
            icono.animar(fraccion, exceso);
            return cumplida;
        }

        /** Destello de celebracion: pulso de escala y aparicion de la insignia. */
        void celebrar() {
            if (destello != null) {
                destello.stop();
            }
            destello = new ScaleTransition(Duration.millis(320), this);
            destello.setFromX(1);
            destello.setFromY(1);
            destello.setToX(1.06);
            destello.setToY(1.06);
            destello.setAutoReverse(true);
            destello.setCycleCount(2);
            destello.setInterpolator(Interpolator.EASE_BOTH);
            destello.play();

            insignia.setScaleX(0.3);
            insignia.setScaleY(0.3);
            ScaleTransition pop = new ScaleTransition(Duration.millis(300), insignia);
            pop.setToX(1);
            pop.setToY(1);
            pop.setInterpolator(Interpolator.EASE_OUT);
            pop.play();
        }
    }

    /**
     * Icono SVG que se rellena de abajo hacia arriba. El relleno es un {@link Rectangle}
     * recortado con la propia silueta del icono (mascara de recorte) y su altura se anima
     * con una {@link Timeline}.
     */
    private static final class IndicadorIcono {

        private static final double LADO = 60;

        private final Rectangle liquido = new Rectangle();
        private final DoubleProperty progreso = new SimpleDoubleProperty(0);
        private final Pane raiz = new Pane();
        private final String claseColor;
        private Timeline animacion;

        IndicadorIcono(String rutaSvg, String claseColor) {
            this.claseColor = claseColor;

            raiz.setPrefSize(LADO, LADO);
            raiz.setMinSize(LADO, LADO);
            raiz.setMaxSize(LADO, LADO);

            SVGPath silueta = new SVGPath();
            silueta.setContent(rutaSvg);
            silueta.getStyleClass().add("icono-silueta");

            liquido.setX(0);
            liquido.setWidth(LADO);
            liquido.setHeight(0);
            liquido.getStyleClass().addAll("icono-liquido", claseColor);

            Pane capa = new Pane(liquido);
            capa.setPrefSize(LADO, LADO);
            capa.setMinSize(LADO, LADO);
            capa.setMaxSize(LADO, LADO);
            SVGPath mascara = new SVGPath();
            mascara.setContent(rutaSvg);
            capa.setClip(mascara);

            raiz.getChildren().addAll(silueta, capa);

            progreso.addListener((observable, viejo, nuevo) -> pintar(nuevo.doubleValue()));
            pintar(0);
        }

        Pane nodo() {
            return raiz;
        }

        /** Anima el nivel de llenado hasta la fraccion indicada (0 a 1). */
        void animar(double fraccion, boolean exceso) {
            liquido.getStyleClass().removeAll(claseColor, "liquido-exceso");
            liquido.getStyleClass().add(exceso ? "liquido-exceso" : claseColor);

            double destino = Math.max(0, Math.min(1, fraccion));
            if (animacion != null) {
                animacion.stop();
            }
            animacion = new Timeline(new KeyFrame(Duration.millis(600),
                    new KeyValue(progreso, destino, Interpolator.EASE_BOTH)));
            animacion.play();
        }

        /** Coloca el liquido anclado al borde inferior del icono. */
        private void pintar(double fraccion) {
            double alto = LADO * Math.max(0, Math.min(1, fraccion));
            liquido.setY(LADO - alto);
            liquido.setHeight(alto);
        }
    }
}
