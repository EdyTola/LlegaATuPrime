package com.bey.desktop.controller;

import com.bey.desktop.model.beyFotoProgreso;
import com.bey.desktop.model.beyMedicionCorporal;
import com.bey.desktop.model.beyPuntoSerie;
import com.bey.desktop.service.beyAnaliticaService;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormato;
import com.bey.desktop.util.beyFormularioDialogo;
import com.bey.desktop.util.beyVisorMultimedia;
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

public class beyProgresoController implements Initializable, beyVistaConRefresco {
    private static final String METRICA_PESO = "Peso corporal";
    private static final String METRICA_CINTURA = "Cintura";
    private static final String METRICA_GRASA = "Grasa corporal";
    private static final String METRICA_MASA = "Masa muscular";
    private static final String METRICA_CARGA = "Progresión de carga";
    private static final String METRICA_DURACION = "Duración de sesiones";

    private static final String RANGO_SEMANAL = "Semanal";
    private static final String RANGO_MENSUAL = "Mensual";
    private static final String RANGO_HISTORICO = "Histórico";

    private static final String RUTA_FORMULARIO_MEDICION = "/com/bey/desktop/view/beyFormularioMedicion.fxml";
    private static final String RUTA_FORMULARIO_FOTO = "/com/bey/desktop/view/beyFormularioFoto.fxml";

    private static final double ANCHO_MINIATURA = 150;
    private static final double ALTO_MINIATURA = 190;

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();
    private final beyAnaliticaService beyAnalitica = new beyAnaliticaService(beyGestor);

    @FXML
    private ComboBox<String> beySelectorRango;
    @FXML
    private ComboBox<String> beySelectorMetrica;
    @FXML
    private ComboBox<String> beySelectorEjercicio;
    @FXML
    private Button beyBotonRegistrarMedicion;
    @FXML
    private Button beyBotonEliminarMedicion;
    @FXML
    private Label beyValorSesiones;
    @FXML
    private Label beyValorDuracionMedia;
    @FXML
    private Label beyValorVolumen;
    @FXML
    private Label beyValorRacha;
    @FXML
    private Label beyEtiquetaGrafico;
    @FXML
    private Label beyEtiquetaPuntos;
    @FXML
    private LineChart<String, Number> beyGrafico;
    @FXML
    private CategoryAxis beyEjeX;
    @FXML
    private NumberAxis beyEjeY;
    @FXML
    private PieChart beyGraficoGrupos;
    @FXML
    private Label beyEtiquetaGrupos;
    @FXML
    private FlowPane beyGaleriaFotos;
    @FXML
    private TableView<beyMedicionCorporal> beyTablaMediciones;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beySelectorRango.setItems(FXCollections.observableArrayList(RANGO_SEMANAL, RANGO_MENSUAL, RANGO_HISTORICO));
        beySelectorRango.setValue(RANGO_MENSUAL);
        beySelectorMetrica.setItems(FXCollections.observableArrayList(
                METRICA_PESO, METRICA_CINTURA, METRICA_GRASA, METRICA_MASA, METRICA_CARGA, METRICA_DURACION));
        beySelectorMetrica.setValue(METRICA_PESO);

        beyEjeX.setLabel("Fecha");
        beyEjeY.setForceZeroInRange(false);
        beyEjeX.setAnimated(false);
        beyEjeY.setAnimated(false);
        beyGrafico.setAnimated(false);
        beyGraficoGrupos.setAnimated(false);

        beyConfigurarTablaMediciones();
        beySelectorEjercicio.setItems(FXCollections.observableArrayList(beyAnalitica.beyEjerciciosConHistorial()));
        beyMarcarSeleccionEjercicio();

        beyTablaMediciones.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> beyBotonEliminarMedicion.setDisable(actual == null));

        beyRefrescar();
    }

    @Override
    public void beyRefrescar() {
        LocalDate desde = beyAnaliticaService.beyDesdeRango(beySelectorRango.getValue());
        beyActualizarResumen(desde);
        beyDibujarGrafico(desde);
        beyDibujarGrupos(desde);
        beyActualizarGaleria();
        beyTablaMediciones.setItems(FXCollections.observableArrayList(beyGestor.beyGetDatos().medicionesOrdenadas()));
        beyBotonEliminarMedicion.setDisable(true);
    }

    @FXML
    private void beyCambiarFiltro() {
        beyRefrescar();
    }

    @FXML
    private void beyCambiarMetrica() {
        beyMarcarSeleccionEjercicio();
        beyRefrescar();
    }

    private void beyActualizarResumen(LocalDate desde) {
        beyValorSesiones.setText(String.valueOf(beyAnalitica.beySesionesRegistradas(desde)));
        beyValorDuracionMedia.setText(beyFormato.beyNumero(beyAnalitica.beyDuracionMedia(desde)) + " min");
        beyValorVolumen.setText(beyFormato.beyNumero(beyAnalitica.beyVolumenTotal(desde)) + " kg");
        int racha = beyAnalitica.beyRachaEntrenamiento();
        beyValorRacha.setText(racha + (racha == 1 ? " día" : " días"));
    }

    private void beyDibujarGrafico(LocalDate desde) {
        String metrica = beyMetricaSeleccionada();
        List<beyPuntoSerie> puntos;
        String etiquetaEje;

        switch (metrica) {
            case METRICA_CINTURA -> {
                puntos = beyAnalitica.beyCintura(desde);
                etiquetaEje = "Cintura (cm)";
            }
            case METRICA_GRASA -> {
                puntos = beyAnalitica.beyGrasaCorporal(desde);
                etiquetaEje = "Grasa (%)";
            }
            case METRICA_MASA -> {
                puntos = beyAnalitica.beyMasaMuscular(desde);
                etiquetaEje = "Masa muscular (kg)";
            }
            case METRICA_CARGA -> {
                puntos = beyAnalitica.beyProgresionEjercicio(beySelectorEjercicio.getValue(), desde);
                etiquetaEje = "Carga (kg)";
            }
            case METRICA_DURACION -> {
                puntos = beyAnalitica.beyDuracionSesiones(desde);
                etiquetaEje = "Duración (min)";
            }
            default -> {
                puntos = beyAnalitica.beyPesoCorporal(desde);
                etiquetaEje = "Peso (kg)";
            }
        }

        beyEjeY.setLabel(etiquetaEje);
        beyGrafico.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(metrica);
        for (beyPuntoSerie punto : puntos) {
            serie.getData().add(new XYChart.Data<>(punto.getEtiqueta(), punto.getValor()));
        }
        beyGrafico.getData().add(serie);

        beyEtiquetaGrafico.setText("Evolución · " + beyTituloGrafico(metrica));
        beyEtiquetaPuntos.setText(puntos.isEmpty()
                ? "Sin datos registrados en este periodo"
                : puntos.size() + (puntos.size() == 1 ? " registro" : " registros"));
    }

    private void beyDibujarGrupos(LocalDate desde) {
        Map<String, Double> volumen = beyAnalitica.beyVolumenPorGrupo(desde);
        beyGraficoGrupos.getData().clear();

        double total = 0;
        for (Map.Entry<String, Double> entrada : volumen.entrySet()) {
            total += entrada.getValue();
            beyGraficoGrupos.getData().add(new PieChart.Data(
                    entrada.getKey() + " · " + beyFormato.beyNumero(entrada.getValue()) + " kg", entrada.getValue()));
        }
        beyEtiquetaGrupos.setText(total <= 0
                ? "Sin volumen registrado en este periodo"
                : "Volumen total · " + beyFormato.beyNumero(total) + " kg");
    }

    private String beyMetricaSeleccionada() {
        return beySelectorMetrica.getValue() == null ? METRICA_PESO : beySelectorMetrica.getValue();
    }

    private String beyTituloGrafico(String metrica) {
        if (METRICA_CARGA.equals(metrica)) {
            String ejercicio = beySelectorEjercicio.getValue();
            return ejercicio == null ? metrica : metrica + " (" + ejercicio + ")";
        }
        return metrica;
    }

    private void beyMarcarSeleccionEjercicio() {
        boolean progresion = METRICA_CARGA.equals(beyMetricaSeleccionada());
        beySelectorEjercicio.setDisable(!progresion);
        if (progresion && beySelectorEjercicio.getValue() == null && !beySelectorEjercicio.getItems().isEmpty()) {
            beySelectorEjercicio.setValue(beySelectorEjercicio.getItems().get(0));
        }
    }

    private void beyActualizarGaleria() {
        beyGaleriaFotos.getChildren().clear();
        List<beyFotoProgreso> fotos = beyGestor.beyGetDatos().fotosOrdenadas();
        if (fotos.isEmpty()) {
            Label vacio = new Label("Aún no has añadido fotos. Pulsa \"Añadir foto\" para empezar "
                    + "tu galería de transformación y comparar tu evolución mes a mes.");
            vacio.getStyleClass().add("texto-secundario");
            vacio.setWrapText(true);
            beyGaleriaFotos.getChildren().add(vacio);
            return;
        }
        for (beyFotoProgreso foto : fotos) {
            beyGaleriaFotos.getChildren().add(beyConstruirTarjetaFoto(foto));
        }
    }

    private Node beyConstruirTarjetaFoto(beyFotoProgreso foto) {
        StackPane marco = new StackPane(beyConstruirMiniatura(foto.getRutaArchivo()));
        marco.getStyleClass().add("marco-foto");
        marco.setPrefSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setMinSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setMaxSize(ANCHO_MINIATURA, ALTO_MINIATURA);
        marco.setCursor(javafx.scene.Cursor.HAND);
        marco.setOnMouseClicked(e -> beyVisorMultimedia.beyMostrar(foto.getRutaArchivo(), null, beyVentana(), null));

        Label fecha = new Label(beyFormato.beyFechaCorta(foto.getFecha()));
        fecha.getStyleClass().add("foto-fecha");

        Label pose = new Label(foto.getPose().getEtiqueta());
        pose.getStyleClass().add("chip-pose");

        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);

        Button botonEliminar = beyBotonIcono("\u2715", "Eliminar foto",
                "boton-icono", "boton-icono-peligro");
        botonEliminar.setOnAction(e -> beyEliminarFotoProgreso(foto));

        HBox cabecera = new HBox(6, fecha, hueco, botonEliminar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(6, marco, cabecera, pose);
        tarjeta.getStyleClass().add("tarjeta-foto");
        tarjeta.setPrefWidth(ANCHO_MINIATURA + 16);
        return tarjeta;
    }

    private Node beyConstruirMiniatura(String ruta) {
        if (!beyArchivoMultimediaService.beyExiste(ruta)) {
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
    private void beyAgregarFotoProgreso() {
        beyFotoProgreso foto = new beyFotoProgreso();
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_FOTO,
                "Añadir foto de progreso", "Guardar", beyVentana());
        beyFormularioFotoController controlador = (beyFormularioFotoController) dialogo.beyGetControlador();
        controlador.beyCargar(foto);
        dialogo.beySetValidador(controlador::beyValidar);
        if (dialogo.beyMostrarYConfirmar()) {
            controlador.beyAplicar();
            beyGestor.beyAgregarFoto(foto);
            beyActualizarGaleria();
        }
    }

    private void beyEliminarFotoProgreso(beyFotoProgreso foto) {
        if (beyDialogos.beyConfirmar("Eliminar foto",
                "¿Eliminar la foto del " + beyFormato.beyFechaCorta(foto.getFecha())
                        + " (" + foto.getPose().getEtiqueta() + ")?", beyVentana())) {
            beyGestor.beyEliminarFoto(foto);
            beyActualizarGaleria();
        }
    }

    private void beyConfigurarTablaMediciones() {
        TableColumn<beyMedicionCorporal, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(datos -> new SimpleStringProperty(beyFormato.beyFechaCorta(datos.getValue().getFecha())));
        colFecha.setPrefWidth(130);

        TableColumn<beyMedicionCorporal, String> colPeso = new TableColumn<>("Peso");
        colPeso.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getPesoKg() == null ? "—" : beyFormato.beyNumero(datos.getValue().getPesoKg()) + " kg"));
        colPeso.setPrefWidth(110);

        TableColumn<beyMedicionCorporal, String> colCintura = new TableColumn<>("Cintura");
        colCintura.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getCinturaCm() == null ? "—" : beyFormato.beyNumero(datos.getValue().getCinturaCm()) + " cm"));
        colCintura.setPrefWidth(110);

        TableColumn<beyMedicionCorporal, String> colGrasa = new TableColumn<>("Grasa");
        colGrasa.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getGrasaPorcentaje() == null ? "—" : beyFormato.beyNumero(datos.getValue().getGrasaPorcentaje()) + " %"));
        colGrasa.setPrefWidth(100);

        TableColumn<beyMedicionCorporal, String> colMasa = new TableColumn<>("Masa muscular");
        colMasa.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getMasaMuscularKg() == null ? "—" : beyFormato.beyNumero(datos.getValue().getMasaMuscularKg()) + " kg"));
        colMasa.setPrefWidth(130);

        List<TableColumn<beyMedicionCorporal, ?>> columnas = new ArrayList<>();
        columnas.add(colFecha);
        columnas.add(colPeso);
        columnas.add(colCintura);
        columnas.add(colGrasa);
        columnas.add(colMasa);
        beyTablaMediciones.getColumns().setAll(columnas);
        beyTablaMediciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        beyTablaMediciones.setPlaceholder(new Label("Sin mediciones registradas."));
    }

    @FXML
    private void beyRegistrarMedicion() {
        beyMedicionCorporal medicion = new beyMedicionCorporal();
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_MEDICION,
                "Registrar medición corporal", "Guardar", beyVentana());
        beyFormularioMedicionController controlador = (beyFormularioMedicionController) dialogo.beyGetControlador();
        controlador.beyCargar(medicion);
        dialogo.beySetValidador(controlador::beyValidar);
        if (dialogo.beyMostrarYConfirmar()) {
            controlador.beyAplicar();
            beyGestor.beyRegistrarMedicion(medicion);
            beyRefrescar();
        }
    }

    @FXML
    private void beyEliminarMedicion() {
        beyMedicionCorporal medicion = beyTablaMediciones.getSelectionModel().getSelectedItem();
        if (medicion == null) {
            return;
        }
        if (beyDialogos.beyConfirmar("Eliminar medición",
                "¿Eliminar la medición del " + beyFormato.beyFechaCorta(medicion.getFecha()) + "?", beyVentana())) {
            beyGestor.beyEliminarMedicion(medicion);
            beyRefrescar();
        }
    }

    private static Button beyBotonIcono(String glifo, String ayuda, String... estilos) {
        Button boton = new Button(glifo);
        boton.getStyleClass().addAll(estilos);
        if (ayuda != null) {
            boton.setTooltip(new Tooltip(ayuda));
        }
        return boton;
    }

    private Window beyVentana() {
        return beySelectorRango.getScene() == null ? null : beySelectorRango.getScene().getWindow();
    }
}
