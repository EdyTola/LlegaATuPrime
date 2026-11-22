package com.bey.desktop.controller;

import com.bey.desktop.model.beyEjercicio;
import com.bey.desktop.model.beyRegistroEjercicio;
import com.bey.desktop.model.beyRutina;
import com.bey.desktop.model.beySerieRegistrada;
import com.bey.desktop.model.beySesionEntrenamiento;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormato;
import com.bey.desktop.service.beySonidoService;
import com.bey.desktop.service.beySonidoService.beyEfecto;
import com.bey.desktop.service.beyTemaService;
import com.bey.desktop.util.beyAchievementVFX;
import com.bey.desktop.util.beyGuiaTecnicaDialogo;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class beyWorkoutPlayerController {
    private static final String RUTA_FXML = "/com/bey/desktop/view/beyWorkoutPlayerView.fxml";
    private static final String RUTA_CSS = "/com/bey/desktop/css/bey.css";

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();
    private final Map<String, List<beyFilaSerie>> beySeriesPorEjercicio = new LinkedHashMap<>();
    private final Map<String, String> beySustituciones = new LinkedHashMap<>();

    private beyRutina beyRutina;
    private beySesionEntrenamiento beySesion;
    private Stage beyEscenario;
    private Runnable beyAlFinalizar;
    private beyEjercicio beyEjercicioActual;

    private Pane beyCapaEfectos;

    private Timeline beyRelojGlobal;
    private Timeline beyRelojDescanso;
    private int beySegundosSesion;
    private int beySegundosDescanso;

    @FXML
    private Label beyEtiquetaRutina;
    @FXML
    private Label beyCronometro;
    @FXML
    private Button beyBotonCronometro;
    @FXML
    private ListView<beyEjercicio> beyListaEjercicios;
    @FXML
    private Label beyEtiquetaProgreso;
    @FXML
    private Label beyEtiquetaEjercicio;
    @FXML
    private Label beyEtiquetaPrescripcion;
    @FXML
    private Label beyEtiquetaRir;
    @FXML
    private TableView<beyFilaSerie> beyTablaSeries;
    @FXML
    private Spinner<Integer> beySpinnerDescanso;
    @FXML
    private Button beyBotonDescanso;
    @FXML
    private Button beyBotonAgregar15;
    @FXML
    private Button beyBotonSaltarDescanso;
    @FXML
    private Label beyEtiquetaDescanso;
    @FXML
    private Label beyEtiquetaVolumen;
    @FXML
    private Label beyEtiquetaEstado;
    @FXML
    private Button beyBotonMaquinaOcupada;
    @FXML
    private Button beyBotonVerTecnica;
    @FXML
    private Button beyBotonAdjuntar;

    public static void beyAbrir(beyRutina rutina, Window propietario, Runnable alFinalizar) {
        try {
            FXMLLoader cargador = new FXMLLoader(beyWorkoutPlayerController.class.getResource(RUTA_FXML));
            Parent raiz = cargador.load();
            beyWorkoutPlayerController controlador = cargador.getController();

            Stage escenario = new Stage();
            escenario.setTitle("Workout Player - " + rutina.getTituloCompleto());
            if (propietario != null) {
                escenario.initOwner(propietario);
                escenario.initModality(Modality.WINDOW_MODAL);
            }
            Pane capaEfectos = new Pane();
            capaEfectos.setMouseTransparent(true);
            Scene escena = new Scene(new StackPane(raiz, capaEfectos), 1140, 740);
            controlador.beyCapaEfectos = capaEfectos;
            beyTemaService.beyRegistrar(escena);
            escenario.setOnHidden(evento -> beyTemaService.beyDesregistrar(escena));
            URL css = beyWorkoutPlayerController.class.getResource(RUTA_CSS);
            if (css != null) {
                escena.getStylesheets().add(css.toExternalForm());
            }
            escenario.setScene(escena);
            escenario.setMinWidth(980);
            escenario.setMinHeight(660);

            controlador.beyIniciar(rutina, escenario, alFinalizar);
            escenario.show();
        } catch (IOException | RuntimeException e) {
            beyDialogos.beyError("Workout Player",
                    "No se pudo abrir el reproductor de entrenamiento.\n" + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        beySpinnerDescanso.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 180, 90, 5));
        beyConfigurarTablaSeries();

        beyListaEjercicios.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(beyEjercicio ejercicio, boolean vacio) {
                super.updateItem(ejercicio, vacio);
                if (vacio || ejercicio == null) {
                    setText(null);
                    return;
                }
                long hechas = beyFilas(ejercicio).stream().filter(beyFilaSerie::beyIsCompletada).count();
                setText(ejercicio.getNombre() + "   (" + hechas + "/" + ejercicio.getSeries() + ")");
            }
        });
        beyListaEjercicios.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> beyMostrarEjercicio(actual));
        beyActualizarEstadoDescanso(false);
    }

    private void beyIniciar(beyRutina rutina, Stage escenario, Runnable alFinalizar) {
        this.beyRutina = rutina;
        this.beyEscenario = escenario;
        this.beyAlFinalizar = alFinalizar;
        this.beySesion = new beySesionEntrenamiento(rutina, LocalDateTime.now());

        beyEtiquetaRutina.setText(rutina.getTituloCompleto());
        beyListaEjercicios.setItems(FXCollections.observableArrayList(rutina.getEjercicios()));
        if (!rutina.getEjercicios().isEmpty()) {
            beyListaEjercicios.getSelectionModel().select(0);
        }

        beySpinnerDescanso.getValueFactory()
                .setValue(beyGestor.beyGetDatos().getConfiguracion().getDescansoPorDefectoSegundos());
        beySegundosDescanso = beyDescansoConfigurado();
        beyActualizarEtiquetaDescanso();
        beyEtiquetaEstado.setText("Sesión iniciada " + beyFormato.beyHora(beySesion.getFechaInicio()));

        escenario.setOnHidden(evento -> beyDetener());
        beyIniciarCronometroGlobal();
        beyActualizarResumen();
    }

    private void beyConfigurarTablaSeries() {
        beyTablaSeries.setEditable(true);
        beyTablaSeries.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        beyTablaSeries.setPlaceholder(new Label("Este ejercicio no tiene series prescritas."));

        TableColumn<beyFilaSerie, Integer> colNumero = new TableColumn<>("#");
        colNumero.setCellValueFactory(datos -> datos.getValue().beyNumeroProperty().asObject());
        colNumero.setPrefWidth(50);
        colNumero.setEditable(false);

        TableColumn<beyFilaSerie, Double> colPeso = new TableColumn<>("Peso (kg)");
        colPeso.setCellValueFactory(datos -> datos.getValue().beyPesoProperty().asObject());
        colPeso.setCellFactory(columna -> beyCeldaEditable(beyConvertidorPeso(), beyFilaSerie::beySetPeso));
        colPeso.setPrefWidth(120);

        TableColumn<beyFilaSerie, Integer> colReps = new TableColumn<>("Reps");
        colReps.setCellValueFactory(datos -> datos.getValue().beyRepeticionesProperty().asObject());
        colReps.setCellFactory(columna -> beyCeldaEditable(beyConvertidorRepeticiones(), beyFilaSerie::beySetRepeticiones));
        colReps.setPrefWidth(90);

        TableColumn<beyFilaSerie, Integer> colRir = new TableColumn<>("RIR real");
        colRir.setCellValueFactory(datos -> datos.getValue().beyRirProperty().asObject());
        colRir.setCellFactory(columna -> beyCeldaEditable(beyConvertidorRir(), beyFilaSerie::beySetRir));
        colRir.setPrefWidth(90);

        TableColumn<beyFilaSerie, Boolean> colHecha = new TableColumn<>("Hecha");
        colHecha.setCellValueFactory(datos -> datos.getValue().beyCompletadaProperty());
        colHecha.setCellFactory(CheckBoxTableCell.forTableColumn(colHecha));
        colHecha.setPrefWidth(80);
        colHecha.setEditable(true);

        List<TableColumn<beyFilaSerie, ?>> columnas = new ArrayList<>();
        columnas.add(colNumero);
        columnas.add(colPeso);
        columnas.add(colReps);
        columnas.add(colRir);
        columnas.add(colHecha);
        beyTablaSeries.getColumns().setAll(columnas);
    }

    private static <T> TableCell<beyFilaSerie, T> beyCeldaEditable(StringConverter<T> convertidor,
                                                            BiConsumer<beyFilaSerie, T> aplicador) {
        return new TableCell<>() {
            private final TextField beyCampo = new TextField();

            private boolean beySincronizando;

            {
                setEditable(false);
                beyCampo.getStyleClass().add("editor-celda");
                beyCampo.textProperty().addListener((obs, anterior, actual) -> {
                    if (beySincronizando) {
                        return;
                    }
                    beyFilaSerie fila = beyFilaActual();
                    if (fila != null) {
                        aplicador.accept(fila, convertidor.fromString(actual));
                    }
                });
                beyCampo.focusedProperty().addListener((obs, antes, enfocado) -> {
                    if (enfocado) {
                        beyCampo.selectAll();
                    } else {
                        beySincronizar(convertidor.toString(getItem()));
                    }
                });
            }

            @Override
            protected void updateItem(T valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null || beyFilaActual() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (!beyCampo.isFocused()) {
                    beySincronizar(convertidor.toString(valor));
                }
                setText(null);
                setGraphic(beyCampo);
            }

            private beyFilaSerie beyFilaActual() {
                TableRow<beyFilaSerie> fila = getTableRow();
                return fila == null ? null : fila.getItem();
            }

            private void beySincronizar(String texto) {
                if (texto == null || texto.equals(beyCampo.getText())) {
                    return;
                }
                beySincronizando = true;
                beyCampo.setText(texto);
                beySincronizando = false;
            }
        };
    }

    private static StringConverter<Double> beyConvertidorPeso() {
        return new StringConverter<>() {
            @Override
            public String toString(Double valor) {
                return valor == null || valor == 0 ? "" : beyFormato.beyNumero(valor);
            }

            @Override
            public Double fromString(String texto) {
                return beyFormato.beyANumero(texto);
            }
        };
    }

    private static StringConverter<Integer> beyConvertidorRepeticiones() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer valor) {
                return valor == null ? "" : String.valueOf(valor);
            }

            @Override
            public Integer fromString(String texto) {
                return (int) Math.round(beyFormato.beyANumero(texto));
            }
        };
    }

    private static StringConverter<Integer> beyConvertidorRir() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer valor) {
                return valor == null || valor < 0 ? "" : String.valueOf(valor);
            }

            @Override
            public Integer fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return -1;
                }
                return (int) Math.round(beyFormato.beyANumero(texto));
            }
        };
    }

    private static boolean beyRegistrable(beyFilaSerie fila) {
        return fila.beyIsCompletada() || (fila.beyGetPeso() > 0 && fila.beyGetRepeticiones() > 0);
    }

    private List<beyFilaSerie> beyFilas(beyEjercicio ejercicio) {
        return beySeriesPorEjercicio.computeIfAbsent(ejercicio.getId(), clave -> {
            List<beyFilaSerie> lista = new ArrayList<>();
            for (int i = 1; i <= Math.max(1, ejercicio.getSeries()); i++) {
                beyFilaSerie fila = new beyFilaSerie(i, ejercicio.getRepeticionesMin(), ejercicio.getRirObjetivo());
                fila.beyCompletadaProperty().addListener((obs, antes, ahora) -> {
                    if (ahora) {
                        beyIniciarDescanso();
                        beyCelebrarSerie(ejercicio, lista);
                    }
                    beyActualizarResumen();
                });
                fila.beyPesoProperty().addListener((obs, antes, ahora) -> beyActualizarResumen());
                fila.beyRepeticionesProperty().addListener((obs, antes, ahora) -> beyActualizarResumen());
                lista.add(fila);
            }
            return lista;
        });
    }

    private void beyCelebrarSerie(beyEjercicio ejercicio, List<beyFilaSerie> filas) {
        boolean ejercicioCompleto = filas.stream().allMatch(beyFilaSerie::beyIsCompletada);
        if (beyCapaEfectos != null && beyTablaSeries.getScene() != null) {
            beyAchievementVFX.beyPulsoGlow(beyTablaSeries);
            beyAchievementVFX.beyTextoFlotante(beyTablaSeries, beyCapaEfectos,
                    ejercicioCompleto ? "¡Ejercicio completado!" : "¡Serie completada!");
            if (ejercicioCompleto) {
                beyAchievementVFX.beyPulsoGlow(beyListaEjercicios);
            }
        }
        beySonidoService.beyReproducir(ejercicioCompleto ? beyEfecto.EXITO : beyEfecto.SERIE);
    }

    private void beyMostrarEjercicio(beyEjercicio ejercicio) {
        this.beyEjercicioActual = ejercicio;
        boolean habilitado = ejercicio != null;
        beyBotonMaquinaOcupada.setDisable(!habilitado);
        beyBotonVerTecnica.setDisable(!habilitado);
        beyBotonAdjuntar.setDisable(!habilitado);

        if (!habilitado) {
            beyEtiquetaEjercicio.setText("Selecciona un ejercicio");
            beyEtiquetaPrescripcion.setText("");
            beyEtiquetaRir.setText("");
            beyTablaSeries.setItems(FXCollections.observableArrayList());
            return;
        }

        String unidad = ejercicio.isPorTiempo() ? "segundos por serie" : "repeticiones";
        beyEtiquetaEjercicio.setText(beyTituloEjercicio(ejercicio));
        beyEtiquetaPrescripcion.setText(ejercicio.getGrupoMuscular().getEtiqueta() + " · "
                + ejercicio.getPrescripcion() + " · " + unidad + " · descanso "
                + ejercicio.getDescansoSegundos() + " s");
        beyEtiquetaRir.setText(ejercicio.getRirTexto());
        beyTablaSeries.setItems(FXCollections.observableArrayList(beyFilas(ejercicio)));
        beySpinnerDescanso.getValueFactory().setValue(ejercicio.getDescansoSegundos());
    }

    private String beyTituloEjercicio(beyEjercicio ejercicio) {
        String alternativa = beySustituciones.get(ejercicio.getId());
        return alternativa == null
                ? ejercicio.getNombre()
                : ejercicio.getNombre() + " → " + alternativa;
    }

    @FXML
    private void beyAlternarCronometro() {
        if (beyRelojGlobal == null) {
            return;
        }
        if (beyRelojGlobal.getStatus() == Animation.Status.RUNNING) {
            beyRelojGlobal.pause();
            beyBotonCronometro.setText("Reanudar");
            beyEtiquetaEstado.setText("Cronómetro en pausa.");
        } else {
            beyRelojGlobal.play();
            beyBotonCronometro.setText("Pausar");
            beyEtiquetaEstado.setText("Sesión en curso.");
        }
    }

    @FXML
    private void beyIniciarDescanso() {
        if (beyRelojDescanso != null) {
            beyRelojDescanso.stop();
        }
        beySegundosDescanso = beyDescansoConfigurado();
        beyActualizarEtiquetaDescanso();
        beyRelojDescanso = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            beySegundosDescanso--;
            beyActualizarEtiquetaDescanso();
            if (beySegundosDescanso <= 0) {
                beySegundosDescanso = 0;
                beyRelojDescanso.stop();
                beyEtiquetaDescanso.setText("¡Listo!");
                beyActualizarEstadoDescanso(false);
            }
        }));
        beyRelojDescanso.setCycleCount(Animation.INDEFINITE);
        beyRelojDescanso.play();
        beyActualizarEstadoDescanso(true);
    }

    @FXML
    private void beySaltarDescanso() {
        if (beyRelojDescanso != null) {
            beyRelojDescanso.stop();
        }
        beySegundosDescanso = beyDescansoConfigurado();
        beyActualizarEtiquetaDescanso();
        beyActualizarEstadoDescanso(false);
        beyEtiquetaEstado.setText("Descanso saltado. Listo para la siguiente serie.");
    }

    private void beyActualizarEstadoDescanso(boolean corriendo) {
        if (beyBotonSaltarDescanso != null) {
            beyBotonSaltarDescanso.setVisible(corriendo);
            beyBotonSaltarDescanso.setManaged(corriendo);
            beyBotonSaltarDescanso.setDisable(!corriendo);
        }
        if (beyBotonAgregar15 != null) {
            beyBotonAgregar15.setDisable(!corriendo);
        }
        if (beyBotonDescanso != null) {
            beyBotonDescanso.setText(corriendo ? "Reiniciar descanso" : "Iniciar descanso");
        }
    }

    @FXML
    private void beyAgregarQuinceSegundos() {
        if (beyRelojDescanso == null || beyRelojDescanso.getStatus() != Animation.Status.RUNNING) {
            return;
        }
        beySegundosDescanso += 15;
        beyActualizarEtiquetaDescanso();
    }

    @FXML
    private void beyMaquinaOcupada() {
        if (beyEjercicioActual == null) {
            return;
        }
        List<String> alternativas = beyGestor.beyAlternativasPara(beyEjercicioActual);
        if (alternativas.isEmpty()) {
            beyDialogos.beyInfo("Máquina ocupada",
                    "No hay ejercicios equivalentes registrados para \"" + beyEjercicioActual.getNombre() + "\".");
            return;
        }
        String mantener = "Mantener " + beyEjercicioActual.getNombre();
        List<String> opciones = new ArrayList<>();
        opciones.add(mantener);
        opciones.addAll(alternativas);

        Optional<String> elegida = beyDialogos.beySeleccionar("Máquina ocupada",
                "Alternativas equivalentes para " + beyEjercicioActual.getNombre(), opciones,
                beySustituciones.get(beyEjercicioActual.getId()), beyVentana());
        if (elegida.isEmpty()) {
            return;
        }
        if (elegida.get().equals(mantener)) {
            beySustituciones.remove(beyEjercicioActual.getId());
            beyEtiquetaEstado.setText("Se mantiene el ejercicio original.");
        } else {
            beySustituciones.put(beyEjercicioActual.getId(), elegida.get());
            beyEtiquetaEstado.setText("Serie sustituida por \"" + elegida.get() + "\".");
        }
        beyListaEjercicios.refresh();
        beyMostrarEjercicio(beyEjercicioActual);
    }

    @FXML
    private void beyVerTecnica() {
        if (beyEjercicioActual == null) {
            return;
        }

        beyGuiaTecnicaDialogo.beyMostrar(beyEjercicioActual, beySustituciones.get(beyEjercicioActual.getId()),
                beyGestor.beyGetModalidad(), beyVentana(), ruta -> {
            beyEjercicioActual.setRutaMultimedia(ruta);
            beyGestor.beyGuardarCambiosEntrenamiento();
        });
    }

    @FXML
    private void beyAdjuntarMultimedia() {
        if (beyEjercicioActual == null) {
            return;
        }
        File archivo = beyArchivoMultimediaService
                .beySelectorMultimedia(beyGestor.beyGetDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(beyVentana());
        if (archivo == null) {
            return;
        }
        beyEjercicioActual.setRutaMultimedia(archivo.getAbsolutePath());
        beyGestor.beyRecordarCarpetaMultimedia(archivo.getParent());
        beyGestor.beyGuardarCambiosEntrenamiento();
        beyEtiquetaEstado.setText("Multimedia asociada a \"" + beyEjercicioActual.getNombre() + "\".");
    }

    @FXML
    private void beyFinalizar() {
        long registradas = beySeriesPorEjercicio.values().stream()
                .flatMap(List::stream).filter(beyWorkoutPlayerController::beyRegistrable).count();
        if (registradas == 0 && !beyDialogos.beyConfirmar("Sesión sin series",
                "No has registrado ninguna serie.\n¿Guardar la sesión de todos modos?", beyVentana())) {
            return;
        }

        beySesion.getRegistros().clear();
        for (beyEjercicio ejercicio : beyRutina.getEjercicios()) {
            List<beyFilaSerie> filas = beySeriesPorEjercicio.getOrDefault(ejercicio.getId(), List.of());

            beyRegistroEjercicio registro = new beyRegistroEjercicio(ejercicio, beySesion.getFecha());
            String alternativa = beySustituciones.get(ejercicio.getId());
            if (alternativa != null) {
                registro.setNombreEjercicio(alternativa);
            }
            for (beyFilaSerie fila : filas) {
                if (!beyRegistrable(fila)) {
                    continue;
                }

                beySerieRegistrada serie = new beySerieRegistrada(fila.beyGetNumero());
                serie.setPesoKg(fila.beyGetPeso());
                serie.setRepeticiones(fila.beyGetRepeticiones());
                serie.setRirReal(fila.beyGetRir());
                serie.setCompletada(true);

                registro.getSeries().add(serie);
            }
            if (!registro.getSeries().isEmpty()) {
                beySesion.getRegistros().add(registro);
            }
        }
        beySesion.setFechaFin(LocalDateTime.now());
        beyGestor.beyGuardarSesion(beySesion);
        beyDetener();
        if (beyAlFinalizar != null) {
            beyAlFinalizar.run();
        }
        beyEscenario.close();
    }

    @FXML
    private void beyCancelar() {
        boolean descartar = beyDialogos.beyConfirmar("Descartar sesión",
                "¿Descartar el entrenamiento en curso? No se guardará ningún registro.", beyVentana());
        if (descartar) {
            beyDetener();
            beyEscenario.close();
        }
    }

    private void beyIniciarCronometroGlobal() {
        beySegundosSesion = 0;
        beyCronometro.setText(beyFormato.beyCronometro(java.time.Duration.ZERO));
        beyRelojGlobal = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            beySegundosSesion++;
            beyCronometro.setText(beyFormato.beyCronometro(java.time.Duration.ofSeconds(beySegundosSesion)));
        }));
        beyRelojGlobal.setCycleCount(Animation.INDEFINITE);
        beyRelojGlobal.play();
    }

    private int beyDescansoConfigurado() {
        try {
            String texto = beySpinnerDescanso.getEditor().getText();
            if (texto != null && !texto.isBlank()) {
                int valor = (int) Math.round(beyFormato.beyANumero(texto));
                valor = Math.max(60, Math.min(180, valor));
                beySpinnerDescanso.getValueFactory().setValue(valor);
            }
        } catch (RuntimeException ignored) {
        }
        Integer valor = beySpinnerDescanso.getValue();
        return valor == null ? 90 : valor;
    }

    private void beyActualizarEtiquetaDescanso() {
        int restante = Math.max(0, beySegundosDescanso);
        beyEtiquetaDescanso.setText(String.format("%02d:%02d", restante / 60, restante % 60));
    }

    private void beyActualizarResumen() {
        double volumen = 0;
        int registradas = 0;
        for (List<beyFilaSerie> filas : beySeriesPorEjercicio.values()) {
            for (beyFilaSerie fila : filas) {
                if (beyRegistrable(fila)) {
                    volumen += fila.beyGetPeso() * fila.beyGetRepeticiones();
                    registradas++;
                }
            }
        }
        beyEtiquetaVolumen.setText("Volumen: " + beyFormato.beyNumero(volumen) + " kg · "
                + registradas + " series registradas");

        if (beyRutina != null) {
            long conSeries = beyRutina.getEjercicios().stream()
                    .filter(e -> beySeriesPorEjercicio.getOrDefault(e.getId(), List.of()).stream()
                            .anyMatch(beyWorkoutPlayerController::beyRegistrable))
                    .count();
            beyEtiquetaProgreso.setText(conSeries + " de " + beyRutina.getEjercicios().size()
                    + " ejercicios con series registradas");
        }
        beyListaEjercicios.refresh();
    }

    private void beyDetener() {
        if (beyRelojGlobal != null) {
            beyRelojGlobal.stop();
        }
        if (beyRelojDescanso != null) {
            beyRelojDescanso.stop();
        }
    }

    private Window beyVentana() {
        return beyEscenario == null ? null : beyEscenario;
    }

    public static final class beyFilaSerie {
        private final IntegerProperty beyNumero;
        private final DoubleProperty beyPeso = new SimpleDoubleProperty(0);
        private final IntegerProperty beyRepeticiones = new SimpleIntegerProperty(0);
        private final IntegerProperty beyRir = new SimpleIntegerProperty(-1);
        private final BooleanProperty beyCompletada = new SimpleBooleanProperty(false);

        public beyFilaSerie(int numero, int repeticionesSugeridas, int rirSugerido) {
            this.beyNumero = new SimpleIntegerProperty(numero);
            this.beyRepeticiones.set(Math.max(0, repeticionesSugeridas));
            this.beyRir.set(rirSugerido);
        }

        public IntegerProperty beyNumeroProperty() {
            return beyNumero;
        }

        public DoubleProperty beyPesoProperty() {
            return beyPeso;
        }

        public IntegerProperty beyRepeticionesProperty() {
            return beyRepeticiones;
        }

        public IntegerProperty beyRirProperty() {
            return beyRir;
        }

        public BooleanProperty beyCompletadaProperty() {
            return beyCompletada;
        }

        public int beyGetNumero() {
            return beyNumero.get();
        }

        public double beyGetPeso() {
            return beyPeso.get();
        }

        public void beySetPeso(double valor) {
            beyPeso.set(valor);
        }

        public int beyGetRepeticiones() {
            return beyRepeticiones.get();
        }

        public void beySetRepeticiones(int valor) {
            beyRepeticiones.set(valor);
        }

        public int beyGetRir() {
            return beyRir.get();
        }

        public void beySetRir(int valor) {
            beyRir.set(valor);
        }

        public boolean beyIsCompletada() {
            return beyCompletada.get();
        }
    }
}
