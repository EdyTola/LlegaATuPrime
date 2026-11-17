package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.FotoProgreso;
import com.symmetry.desktop.model.MedicionCorporal;
import com.symmetry.desktop.model.PuntoSerie;
import com.symmetry.desktop.service.AnaliticaService;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.Formato;
import com.symmetry.desktop.util.FormularioDialogo;
import com.symmetry.desktop.util.VisorMultimedia;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Modulo 5 - Progreso y analiticas: graficos de peso corporal, cintura, grasa, masa muscular,
 * progresion de carga y duracion de sesiones con filtros Semanal/Mensual/Historico, mas el
 * registro de mediciones, la racha de entrenamiento, el volumen por grupo muscular y la
 * galeria de fotos de transformacion.
 */
public class ProgresoController implements Initializable, VistaConRefresco {

    private static final String METRICA_PESO = "Peso corporal";
    private static final String METRICA_CINTURA = "Cintura";
    private static final String METRICA_GRASA = "Grasa corporal";
    private static final String METRICA_MASA = "Masa muscular";
    private static final String METRICA_CARGA = "Progresión de carga";
    private static final String METRICA_DURACION = "Duración de sesiones";

    private static final String RANGO_SEMANAL = "Semanal";
    private static final String RANGO_MENSUAL = "Mensual";
    private static final String RANGO_HISTORICO = "Histórico";

    private static final String RUTA_FORMULARIO_MEDICION = "/com/symmetry/desktop/view/FormularioMedicion.fxml";
    private static final String RUTA_FORMULARIO_FOTO = "/com/symmetry/desktop/view/FormularioFoto.fxml";

    /** Medidas de las miniaturas de la galeria de transformacion. */
    private static final double ANCHO_MINIATURA = 150;
    private static final double ALTO_MINIATURA = 190;

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final AnaliticaService analitica = new AnaliticaService(gestor);

    @FXML
    private ComboBox<String> selectorRango;
    @FXML
    private ComboBox<String> selectorMetrica;
    @FXML
    private ComboBox<String> selectorEjercicio;
    @FXML
    private Button botonRegistrarMedicion;
    @FXML
    private Button botonEliminarMedicion;
    @FXML
    private Label valorSesiones;
    @FXML
    private Label valorDuracionMedia;
    @FXML
    private Label valorVolumen;
    @FXML
    private Label valorRacha;
    @FXML
    private Label etiquetaGrafico;
    @FXML
    private Label etiquetaPuntos;
    @FXML
    private LineChart<String, Number> grafico;
    @FXML
    private CategoryAxis ejeX;
    @FXML
    private NumberAxis ejeY;
    @FXML
    private PieChart graficoGrupos;
    @FXML
    private Label etiquetaGrupos;
    @FXML
    private FlowPane galeriaFotos;
    @FXML
    private TableView<MedicionCorporal> tablaMediciones;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        selectorRango.setItems(FXCollections.observableArrayList(RANGO_SEMANAL, RANGO_MENSUAL, RANGO_HISTORICO));
        selectorRango.setValue(RANGO_MENSUAL);
        selectorMetrica.setItems(FXCollections.observableArrayList(
                METRICA_PESO, METRICA_CINTURA, METRICA_GRASA, METRICA_MASA, METRICA_CARGA, METRICA_DURACION));
        selectorMetrica.setValue(METRICA_PESO);

        ejeX.setLabel("Fecha");
        ejeY.setForceZeroInRange(false);
        ejeX.setAnimated(false);
        ejeY.setAnimated(false);
        grafico.setAnimated(false);
        graficoGrupos.setAnimated(false);

        configurarTablaMediciones();
        selectorEjercicio.setItems(FXCollections.observableArrayList(analitica.ejerciciosConHistorial()));
        marcarSeleccionEjercicio();

        tablaMediciones.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> botonEliminarMedicion.setDisable(actual == null));

        refrescar();
    }

    @Override
    public void refrescar() {
        LocalDate desde = AnaliticaService.desdeRango(selectorRango.getValue());
        actualizarResumen(desde);
        dibujarGrafico(desde);
        dibujarGrupos(desde);
        actualizarGaleria();
        tablaMediciones.setItems(FXCollections.observableArrayList(gestor.getDatos().medicionesOrdenadas()));
        botonEliminarMedicion.setDisable(true);
    }

    @FXML
    private void cambiarFiltro() {
        refrescar();
    }

    @FXML
    private void cambiarMetrica() {
        marcarSeleccionEjercicio();
        refrescar();
    }

    // ------------------------------------------------------------------
    // Resumen y graficos
    // ------------------------------------------------------------------

    private void actualizarResumen(LocalDate desde) {
        valorSesiones.setText(String.valueOf(analitica.sesionesRegistradas(desde)));
        valorDuracionMedia.setText(Formato.numero(analitica.duracionMedia(desde)) + " min");
        valorVolumen.setText(Formato.numero(analitica.volumenTotal(desde)) + " kg");
        int racha = analitica.rachaEntrenamiento();
        valorRacha.setText(racha + (racha == 1 ? " día" : " días"));
    }

    private void dibujarGrafico(LocalDate desde) {
        String metrica = metricaSeleccionada();
        List<PuntoSerie> puntos;
        String etiquetaEje;

        switch (metrica) {
            case METRICA_CINTURA -> {
                puntos = analitica.cintura(desde);
                etiquetaEje = "Cintura (cm)";
            }
            case METRICA_GRASA -> {
                puntos = analitica.grasaCorporal(desde);
                etiquetaEje = "Grasa (%)";
            }
            case METRICA_MASA -> {
                puntos = analitica.masaMuscular(desde);
                etiquetaEje = "Masa muscular (kg)";
            }
            case METRICA_CARGA -> {
                puntos = analitica.progresionEjercicio(selectorEjercicio.getValue(), desde);
                etiquetaEje = "Carga (kg)";
            }
            case METRICA_DURACION -> {
                puntos = analitica.duracionSesiones(desde);
                etiquetaEje = "Duración (min)";
            }
            default -> {
                puntos = analitica.pesoCorporal(desde);
                etiquetaEje = "Peso (kg)";
            }
        }

        ejeY.setLabel(etiquetaEje);
        grafico.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(metrica);
        for (PuntoSerie punto : puntos) {
            serie.getData().add(new XYChart.Data<>(punto.getEtiqueta(), punto.getValor()));
        }
        grafico.getData().add(serie);

        etiquetaGrafico.setText("Evolución · " + tituloGrafico(metrica));
        etiquetaPuntos.setText(puntos.isEmpty()
                ? "Sin datos registrados en este periodo"
                : puntos.size() + (puntos.size() == 1 ? " registro" : " registros"));
    }

    /** Distribucion circular del volumen de carga por grupo muscular en el periodo. */
    private void dibujarGrupos(LocalDate desde) {
        Map<String, Double> volumen = analitica.volumenPorGrupo(desde);
        graficoGrupos.getData().clear();

        double total = 0;
        for (Map.Entry<String, Double> entrada : volumen.entrySet()) {
            total += entrada.getValue();
            graficoGrupos.getData().add(new PieChart.Data(
                    entrada.getKey() + " · " + Formato.numero(entrada.getValue()) + " kg", entrada.getValue()));
        }
        etiquetaGrupos.setText(total <= 0
                ? "Sin volumen registrado en este periodo"
                : "Volumen total · " + Formato.numero(total) + " kg");
    }

    private String metricaSeleccionada() {
        return selectorMetrica.getValue() == null ? METRICA_PESO : selectorMetrica.getValue();
    }

    private String tituloGrafico(String metrica) {
        if (METRICA_CARGA.equals(metrica)) {
            String ejercicio = selectorEjercicio.getValue();
            return ejercicio == null ? metrica : metrica + " (" + ejercicio + ")";
        }
        return metrica;
    }

    private void marcarSeleccionEjercicio() {
        boolean progresion = METRICA_CARGA.equals(metricaSeleccionada());
        selectorEjercicio.setDisable(!progresion);
        if (progresion && selectorEjercicio.getValue() == null && !selectorEjercicio.getItems().isEmpty()) {
            selectorEjercicio.setValue(selectorEjercicio.getItems().get(0));
        }
    }

    // ------------------------------------------------------------------
    // Galeria de transformacion
    // ------------------------------------------------------------------

    private void actualizarGaleria() {
        galeriaFotos.getChildren().clear();
        List<FotoProgreso> fotos = gestor.getDatos().fotosOrdenadas();
        if (fotos.isEmpty()) {
            Label vacio = new Label("Aún no has añadido fotos. Pulsa \"Añadir foto\" para empezar "
                    + "tu galería de transformación y comparar tu evolución mes a mes.");
            vacio.getStyleClass().add("texto-secundario");
            vacio.setWrapText(true);
            galeriaFotos.getChildren().add(vacio);
            return;
        }
        for (FotoProgreso foto : fotos) {
            galeriaFotos.getChildren().add(construirTarjetaFoto(foto));
        }
    }

    private Node construirTarjetaFoto(FotoProgreso foto) {
        StackPane marco = new StackPane(construirMiniatura(foto.getRutaArchivo()));
        marco.getStyleClass().add("marco-foto");
        marco.setPrefSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setMinSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setMaxSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setCursor(javafx.scene.Cursor.HAND);
        marco.setOnMouseClicked(e -> VisorMultimedia.mostrar(foto.getRutaArchivo(), null, ventana(), null));

        Label fecha = new Label(Formato.fechaCorta(foto.getFecha()));
        fecha.getStyleClass().add("foto-fecha");

        Label pose = new Label(foto.getPose().getEtiqueta());
        pose.getStyleClass().add("chip-pose");

        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);

        Button botonEliminar = botonIcono("\u2715", "Eliminar foto",
                "boton-icono", "boton-icono-peligro");
        botonEliminar.setOnAction(e -> eliminarFotoProgreso(foto));

        HBox cabecera = new HBox(6, fecha, hueco, botonEliminar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(6, marco, cabecera, pose);
        tarjeta.getStyleClass().add("tarjeta-foto");
        tarjeta.setPrefWidth(ANCHO_MINIATURA + 16);
        return tarjeta;
    }

    private Node construirMiniatura(String ruta) {
        if (!ArchivoMultimediaService.existe(ruta)) {
            Label faltante = new Label("Imagen no encontrada");
            faltante.getStyleClass().add("texto-secundario");
            faltante.setWrapText(true);
            return faltante;
        }
        ImageView vista = new ImageView(new Image(
                Path.of(ruta).toUri().toString(), ANCHO_MINIATURA, ALTO_MINIATURA, true, true, true));
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        return vista;
    }

    @FXML
    private void agregarFotoProgreso() {
        FotoProgreso foto = new FotoProgreso();
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_FOTO,
                "Añadir foto de progreso", "Guardar", ventana());
        FormularioFotoController controlador = (FormularioFotoController) dialogo.getControlador();
        controlador.cargar(foto);
        dialogo.setValidador(controlador::validar);
        if (dialogo.mostrarYConfirmar()) {
            controlador.aplicar();
            gestor.agregarFoto(foto);
            actualizarGaleria();
        }
    }

    private void eliminarFotoProgreso(FotoProgreso foto) {
        if (Dialogos.confirmar("Eliminar foto",
                "¿Eliminar la foto del " + Formato.fechaCorta(foto.getFecha())
                        + " (" + foto.getPose().getEtiqueta() + ")?", ventana())) {
            gestor.eliminarFoto(foto);
            actualizarGaleria();
        }
    }

    // ------------------------------------------------------------------
    // Mediciones corporales
    // ------------------------------------------------------------------

    private void configurarTablaMediciones() {
        TableColumn<MedicionCorporal, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(datos -> new SimpleStringProperty(Formato.fechaCorta(datos.getValue().getFecha())));
        colFecha.setPrefWidth(130);

        TableColumn<MedicionCorporal, String> colPeso = new TableColumn<>("Peso");
        colPeso.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getPesoKg() == null ? "—" : Formato.numero(datos.getValue().getPesoKg()) + " kg"));
        colPeso.setPrefWidth(110);

        TableColumn<MedicionCorporal, String> colCintura = new TableColumn<>("Cintura");
        colCintura.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getCinturaCm() == null ? "—" : Formato.numero(datos.getValue().getCinturaCm()) + " cm"));
        colCintura.setPrefWidth(110);

        TableColumn<MedicionCorporal, String> colGrasa = new TableColumn<>("Grasa");
        colGrasa.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getGrasaPorcentaje() == null ? "—" : Formato.numero(datos.getValue().getGrasaPorcentaje()) + " %"));
        colGrasa.setPrefWidth(100);

        TableColumn<MedicionCorporal, String> colMasa = new TableColumn<>("Masa muscular");
        colMasa.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getMasaMuscularKg() == null ? "—" : Formato.numero(datos.getValue().getMasaMuscularKg()) + " kg"));
        colMasa.setPrefWidth(130);

        List<TableColumn<MedicionCorporal, ?>> columnas = new ArrayList<>();
        columnas.add(colFecha);
        columnas.add(colPeso);
        columnas.add(colCintura);
        columnas.add(colGrasa);
        columnas.add(colMasa);
        tablaMediciones.getColumns().setAll(columnas);
        tablaMediciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaMediciones.setPlaceholder(new Label("Sin mediciones registradas."));
    }

    @FXML
    private void registrarMedicion() {
        MedicionCorporal medicion = new MedicionCorporal();
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_MEDICION,
                "Registrar medición corporal", "Guardar", ventana());
        FormularioMedicionController controlador = (FormularioMedicionController) dialogo.getControlador();
        controlador.cargar(medicion);
        dialogo.setValidador(controlador::validar);
        if (dialogo.mostrarYConfirmar()) {
            controlador.aplicar();
            gestor.registrarMedicion(medicion);
            refrescar();
        }
    }

    @FXML
    private void eliminarMedicion() {
        MedicionCorporal medicion = tablaMediciones.getSelectionModel().getSelectedItem();
        if (medicion == null) {
            return;
        }
        if (Dialogos.confirmar("Eliminar medición",
                "¿Eliminar la medición del " + Formato.fechaCorta(medicion.getFecha()) + "?", ventana())) {
            gestor.eliminarMedicion(medicion);
            refrescar();
        }
    }

    private static Button botonIcono(String glifo, String ayuda, String... estilos) {
        Button boton = new Button(glifo);
        boton.getStyleClass().addAll(estilos);
        if (ayuda != null) {
            boton.setTooltip(new Tooltip(ayuda));
        }
        return boton;
    }

    private Window ventana() {
        return selectorRango.getScene() == null ? null : selectorRango.getScene().getWindow();
    }
}
