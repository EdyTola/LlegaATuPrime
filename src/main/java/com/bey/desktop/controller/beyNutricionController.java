package com.bey.desktop.controller;

import com.bey.desktop.model.beyComida;
import com.bey.desktop.model.beyDiaSalud;
import com.bey.desktop.model.beyObjetivosNutricionales;
import com.bey.desktop.model.beyRecipiente;
import com.bey.desktop.model.beyRegistroHidratacion;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.util.beyAchievementVFX;
import com.bey.desktop.util.beyConfeti;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormato;
import com.bey.desktop.util.beyFormularioDialogo;
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

public class beyNutricionController implements Initializable, beyVistaConRefresco {
    private static final String RUTA_FORMULARIO_COMIDA = "/com/bey/desktop/view/beyFormularioComida.fxml";

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

    private static final String COLOR_AGUA = "liquido-agua";
    private static final String COLOR_PROTEINA = "liquido-proteina";
    private static final String COLOR_CALORIAS = "liquido-calorias";
    private static final String COLOR_CREATINA = "liquido-creatina";
    private static final String COLOR_DESCANSO = "liquido-descanso";

    private static final double ALTO_CABECERA_TABLA = 37;
    private static final double ALTO_FILA_TABLA = 34;
    private static final double ALTO_TABLA_MIN = 130;
    private static final double ALTO_TABLA_MAX = 420;
    private static final double ALTO_FILA_LISTA = 36;
    private static final double ALTO_LISTA_MIN = 110;
    private static final double ALTO_LISTA_MAX = 280;

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();

    private final Set<String> beyMetasCelebradas = new HashSet<>();
    private LocalDate beyFechaEstado;

    @FXML
    private StackPane beyRaizNutricion;
    @FXML
    private Pane beyCapaConfeti;
    @FXML
    private DatePicker beySelectorFecha;
    @FXML
    private Label beyEtiquetaResumenDia;
    @FXML
    private HBox beyRejillaResumen;
    @FXML
    private TableView<beyComida> beyTablaComidas;
    @FXML
    private GridPane beyContenedorEnvases;
    @FXML
    private TextField beyCampoCantidadPersonalizada;
    @FXML
    private ListView<beyRegistroHidratacion> beyListaHidratacion;
    @FXML
    private CheckBox beyCheckCreatina;
    @FXML
    private Label beyDetalleCreatina;
    @FXML
    private Spinner<Double> beySpinnerSueno;
    @FXML
    private Label beyDetalleSueno;

    private beyIndicadorResumen beyIndicadorAgua;
    private beyIndicadorResumen beyIndicadorProteina;
    private beyIndicadorResumen beyIndicadorCalorias;
    private beyIndicadorResumen beyIndicadorCreatina;
    private beyIndicadorResumen beyIndicadorDescanso;

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beySelectorFecha.setValue(LocalDate.now());
        beySpinnerSueno.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 14, 8, 0.5));

        beyConstruirResumenDelDia();
        beyConfigurarTablaComidas();
        beyConstruirBotonesEnvases();
        beyConfigurarListaHidratacion();

        beyRefrescar();
    }

    @Override
    public void beyRefrescar() {
        LocalDate fecha = beyFechaSeleccionada();
        beyDiaSalud dia = beyGestor.beyGetDatos().buscarDia(fecha).orElseGet(() -> new beyDiaSalud(fecha));
        beyObjetivosNutricionales objetivos = beyGestor.beyGetDatos().getPerfil().getObjetivos();

        beyEtiquetaResumenDia.setText(beyFormato.beyFechaLarga(fecha));

        List<beyMetaDiaria> metas = new ArrayList<>();
        metas.add(new beyMetaDiaria("proteina", "Proteína", beyIndicadorProteina, beyActualizarProteina(dia, objetivos)));
        metas.add(new beyMetaDiaria("calorias", "Calorías", beyIndicadorCalorias, beyActualizarCalorias(dia, objetivos)));
        metas.add(new beyMetaDiaria("agua", "Hidratación", beyIndicadorAgua, beyActualizarHidratacion(dia, objetivos)));
        metas.add(new beyMetaDiaria("creatina", "Creatina", beyIndicadorCreatina, beyActualizarCreatina(dia, objetivos)));
        metas.add(new beyMetaDiaria("descanso", "Descanso", beyIndicadorDescanso, beyActualizarSueno(dia, objetivos)));

        beyTablaComidas.setItems(FXCollections.observableArrayList(dia.getComidas()));
        beyListaHidratacion.setItems(FXCollections.observableArrayList(dia.getHidratacion()));
        beyAjustarAlturaComidas();
        beyAjustarAlturaHidratacion();

        beyCelebrarMetasNuevas(fecha, metas);
    }

    private void beyConstruirResumenDelDia() {
        beyIndicadorAgua = new beyIndicadorResumen("Agua", COLOR_AGUA, SVG_AGUA);
        beyIndicadorProteina = new beyIndicadorResumen("Proteína", COLOR_PROTEINA, SVG_PROTEINA);
        beyIndicadorCalorias = new beyIndicadorResumen("Calorías", COLOR_CALORIAS, SVG_CALORIAS);
        beyIndicadorCreatina = new beyIndicadorResumen("Creatina", COLOR_CREATINA, SVG_CREATINA);
        beyIndicadorDescanso = new beyIndicadorResumen("Descanso", COLOR_DESCANSO, SVG_DESCANSO);
        beyRejillaResumen.getChildren().setAll(
                beyIndicadorAgua, beyIndicadorProteina, beyIndicadorCalorias, beyIndicadorCreatina, beyIndicadorDescanso);
    }

    private boolean beyActualizarProteina(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double total = dia.getTotalProteina();
        double meta = objetivos.getProteinaGramos();
        boolean exceso = total > meta;
        return beyIndicadorProteina.beyMostrar(beyFormato.beyNumero(total) + " g",
                exceso ? "+" + beyFormato.beyNumero(total - meta) + " g" : "Meta " + beyFormato.beyNumero(meta) + " g",
                beyFraccion(total, meta), exceso);
    }

    private boolean beyActualizarCalorias(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double total = dia.getTotalCalorias();
        double meta = objetivos.getCalorias();
        boolean exceso = total > meta;
        return beyIndicadorCalorias.beyMostrar(beyFormato.beyNumero(total) + " kcal",
                exceso ? "+" + beyFormato.beyNumero(total - meta) + " kcal" : "Meta " + beyFormato.beyNumero(meta) + " kcal",
                beyFraccion(total, meta), exceso);
    }

    private boolean beyActualizarHidratacion(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        int total = dia.getTotalHidratacionMl();
        int meta = objetivos.getAguaMililitros();
        boolean exceso = total > meta;
        return beyIndicadorAgua.beyMostrar(total + " ml",
                exceso ? "+" + (total - meta) + " ml" : "Meta " + meta + " ml",
                beyFraccion(total, meta), exceso);
    }

    private boolean beyActualizarCreatina(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        beyCheckCreatina.setSelected(dia.isCreatinaTomada());
        boolean tomada = dia.isCreatinaTomada();
        beyDetalleCreatina.setText(tomada
                ? "Dosis registrada. Mantén una toma diaria constante."
                : "Aún no registras la dosis de hoy.");
        return beyIndicadorCreatina.beyMostrar(
                tomada ? beyFormato.beyNumero(objetivos.getCreatinaGramos()) + " g" : "0 g",
                tomada ? "Dosis registrada" : "Dosis pendiente",
                tomada ? 1 : 0, false);
    }

    private boolean beyActualizarSueno(beyDiaSalud dia, beyObjetivosNutricionales objetivos) {
        double horas = dia.getHorasSueno();
        double min = objetivos.getSuenoMinimoHoras();
        double max = objetivos.getSuenoMaximoHoras();
        boolean exceso = horas > max;
        beyDetalleSueno.setText("Objetivo " + objetivos.getSuenoTexto()
                + (horas <= 0 ? " · sin registrar" : ""));
        if (horas > 0) {
            beySpinnerSueno.getValueFactory().setValue(Math.min(14, horas));
        }
        return beyIndicadorDescanso.beyMostrar(beyFormato.beyNumero(horas) + " h",
                exceso ? "+" + beyFormato.beyNumero(horas - max) + " h" : "Objetivo " + objetivos.getSuenoTexto(),
                beyFraccion(horas, min), exceso);
    }

    private static double beyFraccion(double valor, double meta) {
        return meta <= 0 ? 0 : Math.max(0, valor / meta);
    }

    private void beyCelebrarMetasNuevas(LocalDate fecha, List<beyMetaDiaria> metas) {
        boolean cambioDeFecha = beyFechaEstado == null || !beyFechaEstado.equals(fecha);
        if (cambioDeFecha) {
            beyFechaEstado = fecha;
            beyMetasCelebradas.clear();
        }

        List<beyMetaDiaria> nuevas = new ArrayList<>();
        for (beyMetaDiaria meta : metas) {
            if (meta.cumplida()) {
                if (beyMetasCelebradas.add(meta.clave()) && !cambioDeFecha) {
                    nuevas.add(meta);
                }
            } else {
                beyMetasCelebradas.remove(meta.clave());
            }
        }
        if (nuevas.isEmpty()) {
            return;
        }
        for (beyMetaDiaria meta : nuevas) {
            meta.tarjeta().beyCelebrar();

            beyAchievementVFX.beyCelebrarMeta(meta.tarjeta(), beyCapaConfeti, "¡Completado!");
        }
        beyConfeti.beyEstallar(beyCapaConfeti);
        beyMostrarInsignia(nuevas);
    }

    private void beyMostrarInsignia(List<beyMetaDiaria> metas) {
        beyAchievementVFX.beyToastLogro(beyRaizNutricion, "¡Meta diaria alcanzada!",
                String.join(" · ", metas.stream().map(beyMetaDiaria::nombre).toList()));
    }

    private record beyMetaDiaria(String clave, String nombre, beyIndicadorResumen tarjeta, boolean cumplida) {
    }

    private void beyConfigurarTablaComidas() {
        TableColumn<beyComida, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(datos -> new SimpleStringProperty(beyFormato.beyHora(datos.getValue().getFechaHora())));
        colHora.setPrefWidth(90);

        TableColumn<beyComida, String> colNombre = new TableColumn<>("Comida");
        colNombre.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getNombre()));
        colNombre.setPrefWidth(280);

        TableColumn<beyComida, String> colProteina = new TableColumn<>("Proteína");
        colProteina.setCellValueFactory(datos -> new SimpleStringProperty(
                beyFormato.beyNumero(datos.getValue().getProteinaGramos()) + " g"));
        colProteina.setPrefWidth(110);

        TableColumn<beyComida, String> colCalorias = new TableColumn<>("Calorías");
        colCalorias.setCellValueFactory(datos -> new SimpleStringProperty(
                beyFormato.beyNumero(datos.getValue().getCalorias()) + " kcal"));
        colCalorias.setPrefWidth(120);

        TableColumn<beyComida, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(90);
        colAcciones.setSortable(false);
        colAcciones.setResizable(false);
        colAcciones.setCellValueFactory(datos -> null);
        colAcciones.setCellFactory(columna -> beyCeldaPapelera());

        List<TableColumn<beyComida, ?>> columnas = new ArrayList<>();
        columnas.add(colHora);
        columnas.add(colNombre);
        columnas.add(colProteina);
        columnas.add(colCalorias);
        columnas.add(colAcciones);
        beyTablaComidas.getColumns().setAll(columnas);
        beyTablaComidas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        beyTablaComidas.setPlaceholder(new Label("Sin comidas registradas para este día."));
    }

    private TableCell<beyComida, Void> beyCeldaPapelera() {
        return new TableCell<>() {
            private final Button beyBoton = beyBotonPapelera("Eliminar esta comida");

            {
                beyBoton.setOnAction(evento -> beyEliminarComida(beyComidaDeLaFila()));
            }

            @Override
            protected void updateItem(Void valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setGraphic(vacio ? null : beyBoton);
            }

            private beyComida beyComidaDeLaFila() {
                int indice = getIndex();
                List<beyComida> items = getTableView().getItems();
                return indice >= 0 && indice < items.size() ? items.get(indice) : null;
            }
        };
    }

    @FXML
    private void beyAgregarComida() {
        beyComida comida = new beyComida("", 0, 0, LocalDateTime.now());
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_COMIDA,
                "Registrar comida", "Añadir", beyVentana());
        beyFormularioComidaController controlador = (beyFormularioComidaController) dialogo.beyGetControlador();
        controlador.beyCargar(comida);
        dialogo.beySetValidador(controlador::beyValidar);
        if (dialogo.beyMostrarYConfirmar()) {
            controlador.beyAplicar();
            LocalDate fecha = beyFechaSeleccionada();
            comida.setFechaHora(LocalDateTime.of(fecha, LocalTime.now()));
            beyGestor.beyRegistrarComida(comida);
            beyRefrescar();
        }
    }

    private void beyEliminarComida(beyComida comida) {
        if (comida == null) {
            return;
        }
        if (beyDialogos.beyConfirmar("Eliminar comida", "¿Eliminar \"" + comida.getNombre() + "\"?", beyVentana())) {
            beyGestor.beyEliminarComida(beyFechaSeleccionada(), comida);
            beyRefrescar();
        }
    }

    private void beyConstruirBotonesEnvases() {
        beyContenedorEnvases.getChildren().clear();
        int columna = 0;
        int fila = 0;
        for (beyRecipiente recipiente : beyRecipiente.values()) {
            if (recipiente == beyRecipiente.PERSONALIZADO) {
                continue;
            }
            Button boton = new Button(beyNombreCorto(recipiente) + "\n" + recipiente.getMililitros() + " ml");
            boton.getStyleClass().add("boton-envase");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setWrapText(true);
            boton.setOnAction(evento -> beyRegistrarHidratacion(recipiente));
            GridPane.setHgrow(boton, Priority.ALWAYS);
            beyContenedorEnvases.add(boton, columna, fila);
            if (++columna == 2) {
                columna = 0;
                fila++;
            }
        }
    }

    private static String beyNombreCorto(beyRecipiente recipiente) {
        return switch (recipiente) {
            case VASO -> "Vaso";
            case BOTELLA_MEDIANA -> "Botella mediana";
            case BOTELLA_PERSONAL -> "Botella personal";
            case TOMATODO -> "Tomatodo";
            case PERSONALIZADO -> "Otros";
        };
    }

    private void beyConfigurarListaHidratacion() {
        beyListaHidratacion.setCellFactory(lista -> new ListCell<>() {
            private final Label beyTexto = new Label();
            private final Region beyEspaciador = new Region();
            private final Button beyBoton = beyBotonPapelera("Eliminar este registro");
            private final HBox beyCaja = new HBox(8, beyTexto, beyEspaciador, beyBoton);

            {
                HBox.setHgrow(beyEspaciador, Priority.ALWAYS);
                beyCaja.setAlignment(Pos.CENTER_LEFT);
                beyBoton.setOnAction(evento -> beyEliminarHidratacion(getItem()));
            }

            @Override
            protected void updateItem(beyRegistroHidratacion registro, boolean vacio) {
                super.updateItem(registro, vacio);
                if (vacio || registro == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                beyTexto.setText(beyFormato.beyHora(registro.getFechaHora()) + "  ·  " + registro.getMililitros()
                        + " ml  ·  " + registro.getRecipiente().getEtiqueta());
                setText(null);
                setGraphic(beyCaja);
            }
        });
        beyListaHidratacion.setPlaceholder(new Label("Sin registros de hidratación."));
    }

    private void beyRegistrarHidratacion(beyRecipiente recipiente) {
        beyGestor.beyRegistrarHidratacion(beyFechaSeleccionada(), recipiente.getMililitros(), recipiente);
        beyRefrescar();
    }

    @FXML
    private void beyRegistrarCantidadPersonalizada() {
        int mililitros = (int) Math.round(beyFormato.beyANumero(beyCampoCantidadPersonalizada.getText()));
        if (mililitros <= 0) {
            beyDialogos.beyInfo("Cantidad no válida", "Introduce una cantidad de agua en mililitros mayor que cero.");
            return;
        }
        beyGestor.beyRegistrarHidratacion(beyFechaSeleccionada(), mililitros, beyRecipiente.PERSONALIZADO);
        beyCampoCantidadPersonalizada.clear();
        beyRefrescar();
    }

    private void beyEliminarHidratacion(beyRegistroHidratacion registro) {
        if (registro == null) {
            return;
        }
        beyGestor.beyEliminarHidratacion(beyFechaSeleccionada(), registro);
        beyRefrescar();
    }

    @FXML
    private void beyAlternarCreatina() {
        beyGestor.beySetCreatinaTomada(beyFechaSeleccionada(), beyCheckCreatina.isSelected());
        beyRefrescar();
    }

    @FXML
    private void beyRegistrarSueno() {
        Double horas = beySpinnerSueno.getValue();
        beyGestor.beyRegistrarSueno(beyFechaSeleccionada(), horas == null ? 0 : horas);
        beyRefrescar();
    }

    @FXML
    private void beyRegistrarSuenoRapido(ActionEvent evento) {
        Object dato = ((Button) evento.getSource()).getUserData();
        double horas = beyFormato.beyANumero(String.valueOf(dato));
        beySpinnerSueno.getValueFactory().setValue(horas);
        beyGestor.beyRegistrarSueno(beyFechaSeleccionada(), horas);
        beyRefrescar();
    }

    @FXML
    private void beyCambiarFecha() {
        beyRefrescar();
    }

    @FXML
    private void beyIrAHoy() {
        beySelectorFecha.setValue(LocalDate.now());
        beyRefrescar();
    }

    private void beyAjustarAlturaComidas() {
        double altura = ALTO_CABECERA_TABLA + beyTablaComidas.getItems().size() * ALTO_FILA_TABLA + 2;
        beyTablaComidas.setPrefHeight(Math.max(ALTO_TABLA_MIN, Math.min(ALTO_TABLA_MAX, altura)));
    }

    private void beyAjustarAlturaHidratacion() {
        double altura = beyListaHidratacion.getItems().size() * ALTO_FILA_LISTA + 2;
        beyListaHidratacion.setPrefHeight(Math.max(ALTO_LISTA_MIN, Math.min(ALTO_LISTA_MAX, altura)));
    }

    private static Button beyBotonPapelera(String ayuda) {
        Button boton = new Button("\uD83D\uDDD1");
        boton.getStyleClass().addAll("boton-icono", "boton-icono-peligro");
        boton.setFocusTraversable(false);
        boton.setTooltip(new Tooltip(ayuda));
        return boton;
    }

    private LocalDate beyFechaSeleccionada() {
        return beySelectorFecha.getValue() == null ? LocalDate.now() : beySelectorFecha.getValue();
    }

    private Window beyVentana() {
        return beySelectorFecha.getScene() == null ? null : beySelectorFecha.getScene().getWindow();
    }

    private static final class beyIndicadorResumen extends VBox {
        private final beyIndicadorIcono beyIcono;
        private final Label beyValor = new Label();
        private final Label beyMeta = new Label();
        private final Label beyInsignia = new Label("\u2714");
        private ScaleTransition beyDestello;

        beyIndicadorResumen(String titulo, String claseColor, String rutaSvg) {
            this.beyIcono = new beyIndicadorIcono(rutaSvg, claseColor);

            Label nombre = new Label(titulo);
            nombre.getStyleClass().add("indicador-nombre");
            beyValor.getStyleClass().add("indicador-valor");
            beyMeta.getStyleClass().add("indicador-meta");

            beyInsignia.getStyleClass().add("indicador-insignia");
            beyInsignia.setVisible(false);
            StackPane marcoIcono = new StackPane(beyIcono.beyNodo(), beyInsignia);
            StackPane.setAlignment(beyInsignia, Pos.TOP_RIGHT);
            beyInsignia.setTranslateX(4);
            beyInsignia.setTranslateY(-2);

            setSpacing(6);
            setAlignment(Pos.CENTER);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("tarjeta-indicador");
            getChildren().addAll(marcoIcono, nombre, beyValor, beyMeta);
            HBox.setHgrow(this, Priority.ALWAYS);
        }

        boolean beyMostrar(String valorTexto, String metaTexto, double fraccion, boolean exceso) {
            beyValor.setText(valorTexto);
            beyMeta.setText(metaTexto);

            boolean cumplida = fraccion >= 0.999;
            getStyleClass().remove("indicador-cumplido");
            if (cumplida) {
                getStyleClass().add("indicador-cumplido");
            }
            beyInsignia.setVisible(cumplida);
            beyMeta.getStyleClass().remove("indicador-meta-exceso");
            if (exceso) {
                beyMeta.getStyleClass().add("indicador-meta-exceso");
            }
            beyIcono.beyAnimar(fraccion, exceso);
            return cumplida;
        }

        void beyCelebrar() {
            if (beyDestello != null) {
                beyDestello.stop();
            }
            beyDestello = new ScaleTransition(Duration.millis(320), this);
            beyDestello.setFromX(1);
            beyDestello.setFromY(1);
            beyDestello.setToX(1.06);
            beyDestello.setToY(1.06);
            beyDestello.setAutoReverse(true);
            beyDestello.setCycleCount(2);
            beyDestello.setInterpolator(Interpolator.EASE_BOTH);
            beyDestello.play();

            beyInsignia.setScaleX(0.3);
            beyInsignia.setScaleY(0.3);
            ScaleTransition pop = new ScaleTransition(Duration.millis(300), beyInsignia);
            pop.setToX(1);
            pop.setToY(1);
            pop.setInterpolator(Interpolator.EASE_OUT);
            pop.play();
        }
    }

    private static final class beyIndicadorIcono {
        private static final double LADO = 60;

        private final Rectangle beyLiquido = new Rectangle();
        private final DoubleProperty beyProgreso = new SimpleDoubleProperty(0);
        private final Pane beyRaiz = new Pane();
        private final String beyClaseColor;
        private Timeline beyAnimacion;

        beyIndicadorIcono(String rutaSvg, String claseColor) {
            this.beyClaseColor = claseColor;

            beyRaiz.setPrefSize(LADO, LADO);
            beyRaiz.setMinSize(LADO, LADO);
            beyRaiz.setMaxSize(LADO, LADO);

            SVGPath silueta = new SVGPath();
            silueta.setContent(rutaSvg);
            silueta.getStyleClass().add("icono-silueta");

            beyLiquido.setX(0);
            beyLiquido.setWidth(LADO);
            beyLiquido.setHeight(0);
            beyLiquido.getStyleClass().addAll("icono-liquido", claseColor);

            Pane capa = new Pane(beyLiquido);
            capa.setPrefSize(LADO, LADO);
            capa.setMinSize(LADO, LADO);
            capa.setMaxSize(LADO, LADO);
            SVGPath mascara = new SVGPath();
            mascara.setContent(rutaSvg);
            capa.setClip(mascara);

            beyRaiz.getChildren().addAll(silueta, capa);

            beyProgreso.addListener((observable, viejo, nuevo) -> beyPintar(nuevo.doubleValue()));
            beyPintar(0);
        }

        Pane beyNodo() {
            return beyRaiz;
        }

        void beyAnimar(double fraccion, boolean exceso) {
            beyLiquido.getStyleClass().removeAll(beyClaseColor, "liquido-exceso");
            beyLiquido.getStyleClass().add(exceso ? "liquido-exceso" : beyClaseColor);

            double destino = Math.max(0, Math.min(1, fraccion));
            if (beyAnimacion != null) {
                beyAnimacion.stop();
            }
            beyAnimacion = new Timeline(new KeyFrame(Duration.millis(600),
                    new KeyValue(beyProgreso, destino, Interpolator.EASE_BOTH)));
            beyAnimacion.play();
        }

        private void beyPintar(double fraccion) {
            double alto = LADO * Math.max(0, Math.min(1, fraccion));
            beyLiquido.setY(LADO - alto);
            beyLiquido.setHeight(alto);
        }
    }
}
