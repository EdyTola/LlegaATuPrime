package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.Ejercicio;
import com.symmetry.desktop.model.RegistroEjercicio;
import com.symmetry.desktop.model.Rutina;
import com.symmetry.desktop.model.SerieRegistrada;
import com.symmetry.desktop.model.SesionEntrenamiento;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.Formato;
import com.symmetry.desktop.service.SonidoService;
import com.symmetry.desktop.service.SonidoService.Efecto;
import com.symmetry.desktop.service.TemaService;
import com.symmetry.desktop.util.AchievementVFX;
import com.symmetry.desktop.util.GuiaTecnicaDialogo;
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

/**
 * Modulo 2 - Workout Player: cronometro global de la sesion, registro serie a serie
 * (peso, repeticiones, RIR real y marcado de serie completada), temporizador de descanso
 * configurable, sustitucion por "Maquina ocupada" y consulta de la tecnica del ejercicio.
 */
public class WorkoutPlayerController {

    private static final String RUTA_FXML = "/com/symmetry/desktop/view/WorkoutPlayerView.fxml";
    private static final String RUTA_CSS = "/com/symmetry/desktop/css/symmetry.css";

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final Map<String, List<FilaSerie>> seriesPorEjercicio = new LinkedHashMap<>();
    private final Map<String, String> sustituciones = new LinkedHashMap<>();

    private Rutina rutina;
    private SesionEntrenamiento sesion;
    private Stage escenario;
    private Runnable alFinalizar;
    private Ejercicio ejercicioActual;
    /** Capa transparente sobre el reproductor para los efectos de logro (texto flotante). */
    private Pane capaEfectos;

    private Timeline relojGlobal;
    private Timeline relojDescanso;
    private int segundosSesion;
    private int segundosDescanso;

    @FXML
    private Label etiquetaRutina;
    @FXML
    private Label cronometro;
    @FXML
    private Button botonCronometro;
    @FXML
    private ListView<Ejercicio> listaEjercicios;
    @FXML
    private Label etiquetaProgreso;
    @FXML
    private Label etiquetaEjercicio;
    @FXML
    private Label etiquetaPrescripcion;
    @FXML
    private Label etiquetaRir;
    @FXML
    private TableView<FilaSerie> tablaSeries;
    @FXML
    private Spinner<Integer> spinnerDescanso;
    @FXML
    private Button botonDescanso;
    @FXML
    private Button botonAgregar15;
    @FXML
    private Button botonSaltarDescanso;
    @FXML
    private Label etiquetaDescanso;
    @FXML
    private Label etiquetaVolumen;
    @FXML
    private Label etiquetaEstado;
    @FXML
    private Button botonMaquinaOcupada;
    @FXML
    private Button botonVerTecnica;
    @FXML
    private Button botonAdjuntar;

    /** Abre el Workout Player en una ventana modal para la rutina indicada. */
    public static void abrir(Rutina rutina, Window propietario, Runnable alFinalizar) {
        try {
            FXMLLoader cargador = new FXMLLoader(WorkoutPlayerController.class.getResource(RUTA_FXML));
            Parent raiz = cargador.load();
            WorkoutPlayerController controlador = cargador.getController();

            Stage escenario = new Stage();
            escenario.setTitle("Workout Player - " + rutina.getTituloCompleto());
            if (propietario != null) {
                escenario.initOwner(propietario);
                escenario.initModality(Modality.WINDOW_MODAL);
            }
            Pane capaEfectos = new Pane();
            capaEfectos.setMouseTransparent(true);
            Scene escena = new Scene(new StackPane(raiz, capaEfectos), 1140, 740);
            controlador.capaEfectos = capaEfectos;
            TemaService.registrar(escena);
            escenario.setOnHidden(evento -> TemaService.desregistrar(escena));
            URL css = WorkoutPlayerController.class.getResource(RUTA_CSS);
            if (css != null) {
                escena.getStylesheets().add(css.toExternalForm());
            }
            escenario.setScene(escena);
            escenario.setMinWidth(980);
            escenario.setMinHeight(660);

            controlador.iniciar(rutina, escenario, alFinalizar);
            escenario.show();
        } catch (IOException | RuntimeException e) {
            Dialogos.error("Workout Player",
                    "No se pudo abrir el reproductor de entrenamiento.\n" + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        spinnerDescanso.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(60, 180, 90, 5));
        configurarTablaSeries();

        listaEjercicios.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Ejercicio ejercicio, boolean vacio) {
                super.updateItem(ejercicio, vacio);
                if (vacio || ejercicio == null) {
                    setText(null);
                    return;
                }
                long hechas = filas(ejercicio).stream().filter(FilaSerie::isCompletada).count();
                setText(ejercicio.getNombre() + "   (" + hechas + "/" + ejercicio.getSeries() + ")");
            }
        });
        listaEjercicios.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> mostrarEjercicio(actual));
        actualizarEstadoDescanso(false);
    }

    private void iniciar(Rutina rutina, Stage escenario, Runnable alFinalizar) {
        this.rutina = rutina;
        this.escenario = escenario;
        this.alFinalizar = alFinalizar;
        this.sesion = new SesionEntrenamiento(rutina, LocalDateTime.now());

        etiquetaRutina.setText(rutina.getTituloCompleto());
        listaEjercicios.setItems(FXCollections.observableArrayList(rutina.getEjercicios()));
        if (!rutina.getEjercicios().isEmpty()) {
            listaEjercicios.getSelectionModel().select(0);
        }

        spinnerDescanso.getValueFactory()
                .setValue(gestor.getDatos().getConfiguracion().getDescansoPorDefectoSegundos());
        segundosDescanso = descansoConfigurado();
        actualizarEtiquetaDescanso();
        etiquetaEstado.setText("Sesión iniciada " + Formato.hora(sesion.getFechaInicio()));

        escenario.setOnHidden(evento -> detener());
        iniciarCronometroGlobal();
        actualizarResumen();
    }

    // ------------------------------------------------------------------
    // Tabla de series
    // ------------------------------------------------------------------

    private void configurarTablaSeries() {
        tablaSeries.setEditable(true);
        tablaSeries.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaSeries.setPlaceholder(new Label("Este ejercicio no tiene series prescritas."));

        TableColumn<FilaSerie, Integer> colNumero = new TableColumn<>("#");
        colNumero.setCellValueFactory(datos -> datos.getValue().numeroProperty().asObject());
        colNumero.setPrefWidth(50);
        colNumero.setEditable(false);

        TableColumn<FilaSerie, Double> colPeso = new TableColumn<>("Peso (kg)");
        colPeso.setCellValueFactory(datos -> datos.getValue().pesoProperty().asObject());
        colPeso.setCellFactory(columna -> celdaEditable(convertidorPeso(), FilaSerie::setPeso));
        colPeso.setPrefWidth(120);

        TableColumn<FilaSerie, Integer> colReps = new TableColumn<>("Reps");
        colReps.setCellValueFactory(datos -> datos.getValue().repeticionesProperty().asObject());
        colReps.setCellFactory(columna -> celdaEditable(convertidorRepeticiones(), FilaSerie::setRepeticiones));
        colReps.setPrefWidth(90);

        TableColumn<FilaSerie, Integer> colRir = new TableColumn<>("RIR real");
        colRir.setCellValueFactory(datos -> datos.getValue().rirProperty().asObject());
        colRir.setCellFactory(columna -> celdaEditable(convertidorRir(), FilaSerie::setRir));
        colRir.setPrefWidth(90);

        TableColumn<FilaSerie, Boolean> colHecha = new TableColumn<>("Hecha");
        colHecha.setCellValueFactory(datos -> datos.getValue().completadaProperty());
        colHecha.setCellFactory(CheckBoxTableCell.forTableColumn(colHecha));
        colHecha.setPrefWidth(80);
        colHecha.setEditable(true);

        List<TableColumn<FilaSerie, ?>> columnas = new ArrayList<>();
        columnas.add(colNumero);
        columnas.add(colPeso);
        columnas.add(colReps);
        columnas.add(colRir);
        columnas.add(colHecha);
        tablaSeries.getColumns().setAll(columnas);
    }

    /**
     * Celda numerica con un campo de texto SIEMPRE visible y editable. Cada pulsacion vuelca el
     * valor directamente sobre el objeto de modelo de la serie (peso, repeticiones o RIR), sin
     * depender de entrar en modo edicion (doble clic) ni de confirmar con Intro. De este modo lo
     * introducido por el usuario nunca se pierde al pulsar "Finalizar y guardar".
     */
    private static <T> TableCell<FilaSerie, T> celdaEditable(StringConverter<T> convertidor,
                                                            BiConsumer<FilaSerie, T> aplicador) {
        return new TableCell<>() {
            private final TextField campo = new TextField();
            /** Evita reaplicar texto mientras se sincroniza de forma programatica. */
            private boolean sincronizando;

            {
                // La celda no usa el modo edicion de JavaFX: el campo de texto ya es interactivo.
                setEditable(false);
                campo.getStyleClass().add("editor-celda");
                campo.textProperty().addListener((obs, anterior, actual) -> {
                    if (sincronizando) {
                        return;
                    }
                    FilaSerie fila = filaActual();
                    if (fila != null) {
                        aplicador.accept(fila, convertidor.fromString(actual));
                    }
                });
                campo.focusedProperty().addListener((obs, antes, enfocado) -> {
                    if (enfocado) {
                        campo.selectAll();
                    } else {
                        sincronizar(convertidor.toString(getItem()));
                    }
                });
            }

            @Override
            protected void updateItem(T valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null || filaActual() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Nunca se reescribe el texto mientras el usuario esta escribiendo en el campo.
                if (!campo.isFocused()) {
                    sincronizar(convertidor.toString(valor));
                }
                setText(null);
                setGraphic(campo);
            }

            private FilaSerie filaActual() {
                TableRow<FilaSerie> fila = getTableRow();
                return fila == null ? null : fila.getItem();
            }

            private void sincronizar(String texto) {
                if (texto == null || texto.equals(campo.getText())) {
                    return;
                }
                sincronizando = true;
                campo.setText(texto);
                sincronizando = false;
            }
        };
    }

    /** Convertidor de la columna de peso: acepta coma o punto decimal y muestra vacio si es 0. */
    private static StringConverter<Double> convertidorPeso() {
        return new StringConverter<>() {
            @Override
            public String toString(Double valor) {
                return valor == null || valor == 0 ? "" : Formato.numero(valor);
            }

            @Override
            public Double fromString(String texto) {
                return Formato.aNumero(texto);
            }
        };
    }

    /** Convertidor de la columna de repeticiones (entero). */
    private static StringConverter<Integer> convertidorRepeticiones() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer valor) {
                return valor == null ? "" : String.valueOf(valor);
            }

            @Override
            public Integer fromString(String texto) {
                return (int) Math.round(Formato.aNumero(texto));
            }
        };
    }

    /** Convertidor de la columna de RIR real: vacio equivale a "no informado" (-1). */
    private static StringConverter<Integer> convertidorRir() {
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
                return (int) Math.round(Formato.aNumero(texto));
            }
        };
    }

    /**
     * Una serie entra en el historial si el usuario la marco como hecha o si registro carga real
     * (peso y repeticiones). De este modo el volumen introducido nunca se descarta al finalizar.
     */
    private static boolean registrable(FilaSerie fila) {
        return fila.isCompletada() || (fila.getPeso() > 0 && fila.getRepeticiones() > 0);
    }

    private List<FilaSerie> filas(Ejercicio ejercicio) {
        return seriesPorEjercicio.computeIfAbsent(ejercicio.getId(), clave -> {
            List<FilaSerie> lista = new ArrayList<>();
            for (int i = 1; i <= Math.max(1, ejercicio.getSeries()); i++) {
                FilaSerie fila = new FilaSerie(i, ejercicio.getRepeticionesMin(), ejercicio.getRirObjetivo());
                fila.completadaProperty().addListener((obs, antes, ahora) -> {
                    if (ahora) {
                        iniciarDescanso();
                        celebrarSerie(ejercicio, lista);
                    }
                    actualizarResumen();
                });
                fila.pesoProperty().addListener((obs, antes, ahora) -> actualizarResumen());
                fila.repeticionesProperty().addListener((obs, antes, ahora) -> actualizarResumen());
                lista.add(fila);
            }
            return lista;
        });
    }

    /**
     * Micro-interaccion al completar una serie: brillo sobre la tabla, texto flotante y sonido.
     * Si era la ultima serie del ejercicio, la celebracion es mayor (lista de ejercicios).
     */
    private void celebrarSerie(Ejercicio ejercicio, List<FilaSerie> filas) {
        boolean ejercicioCompleto = filas.stream().allMatch(FilaSerie::isCompletada);
        if (capaEfectos != null && tablaSeries.getScene() != null) {
            AchievementVFX.pulsoGlow(tablaSeries);
            AchievementVFX.textoFlotante(tablaSeries, capaEfectos,
                    ejercicioCompleto ? "¡Ejercicio completado!" : "¡Serie completada!");
            if (ejercicioCompleto) {
                AchievementVFX.pulsoGlow(listaEjercicios);
            }
        }
        SonidoService.reproducir(ejercicioCompleto ? Efecto.EXITO : Efecto.SERIE);
    }

    private void mostrarEjercicio(Ejercicio ejercicio) {
        this.ejercicioActual = ejercicio;
        boolean habilitado = ejercicio != null;
        botonMaquinaOcupada.setDisable(!habilitado);
        botonVerTecnica.setDisable(!habilitado);
        botonAdjuntar.setDisable(!habilitado);

        if (!habilitado) {
            etiquetaEjercicio.setText("Selecciona un ejercicio");
            etiquetaPrescripcion.setText("");
            etiquetaRir.setText("");
            tablaSeries.setItems(FXCollections.observableArrayList());
            return;
        }

        String unidad = ejercicio.isPorTiempo() ? "segundos por serie" : "repeticiones";
        etiquetaEjercicio.setText(tituloEjercicio(ejercicio));
        etiquetaPrescripcion.setText(ejercicio.getGrupoMuscular().getEtiqueta() + " · "
                + ejercicio.getPrescripcion() + " · " + unidad + " · descanso "
                + ejercicio.getDescansoSegundos() + " s");
        etiquetaRir.setText(ejercicio.getRirTexto());
        tablaSeries.setItems(FXCollections.observableArrayList(filas(ejercicio)));
        spinnerDescanso.getValueFactory().setValue(ejercicio.getDescansoSegundos());
    }

    private String tituloEjercicio(Ejercicio ejercicio) {
        String alternativa = sustituciones.get(ejercicio.getId());
        return alternativa == null
                ? ejercicio.getNombre()
                : ejercicio.getNombre() + " → " + alternativa;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    @FXML
    private void alternarCronometro() {
        if (relojGlobal == null) {
            return;
        }
        if (relojGlobal.getStatus() == Animation.Status.RUNNING) {
            relojGlobal.pause();
            botonCronometro.setText("Reanudar");
            etiquetaEstado.setText("Cronómetro en pausa.");
        } else {
            relojGlobal.play();
            botonCronometro.setText("Pausar");
            etiquetaEstado.setText("Sesión en curso.");
        }
    }

    @FXML
    private void iniciarDescanso() {
        if (relojDescanso != null) {
            relojDescanso.stop();
        }
        segundosDescanso = descansoConfigurado();
        actualizarEtiquetaDescanso();
        relojDescanso = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            segundosDescanso--;
            actualizarEtiquetaDescanso();
            if (segundosDescanso <= 0) {
                segundosDescanso = 0;
                relojDescanso.stop();
                etiquetaDescanso.setText("¡Listo!");
                actualizarEstadoDescanso(false);
            }
        }));
        relojDescanso.setCycleCount(Animation.INDEFINITE);
        relojDescanso.play();
        actualizarEstadoDescanso(true);
    }

    /**
     * Salta el descanso en curso: detiene la cuenta regresiva, reinicia el contador visual al
     * descanso configurado y devuelve los controles a su estado inicial. No afecta al
     * cronometro global de la sesion, que sigue corriendo.
     */
    @FXML
    private void saltarDescanso() {
        if (relojDescanso != null) {
            relojDescanso.stop();
        }
        segundosDescanso = descansoConfigurado();
        actualizarEtiquetaDescanso();
        actualizarEstadoDescanso(false);
        etiquetaEstado.setText("Descanso saltado. Listo para la siguiente serie.");
    }

    /** Sincroniza visibilidad y estado de los controles del descanso con la cuenta regresiva. */
    private void actualizarEstadoDescanso(boolean corriendo) {
        if (botonSaltarDescanso != null) {
            botonSaltarDescanso.setVisible(corriendo);
            botonSaltarDescanso.setManaged(corriendo);
            botonSaltarDescanso.setDisable(!corriendo);
        }
        if (botonAgregar15 != null) {
            botonAgregar15.setDisable(!corriendo);
        }
        if (botonDescanso != null) {
            botonDescanso.setText(corriendo ? "Reiniciar descanso" : "Iniciar descanso");
        }
    }

    @FXML
    private void agregarQuinceSegundos() {
        if (relojDescanso == null || relojDescanso.getStatus() != Animation.Status.RUNNING) {
            return;
        }
        segundosDescanso += 15;
        actualizarEtiquetaDescanso();
    }

    @FXML
    private void maquinaOcupada() {
        if (ejercicioActual == null) {
            return;
        }
        List<String> alternativas = gestor.alternativasPara(ejercicioActual);
        if (alternativas.isEmpty()) {
            Dialogos.info("Máquina ocupada",
                    "No hay ejercicios equivalentes registrados para \"" + ejercicioActual.getNombre() + "\".");
            return;
        }
        String mantener = "Mantener " + ejercicioActual.getNombre();
        List<String> opciones = new ArrayList<>();
        opciones.add(mantener);
        opciones.addAll(alternativas);

        Optional<String> elegida = Dialogos.seleccionar("Máquina ocupada",
                "Alternativas equivalentes para " + ejercicioActual.getNombre(), opciones,
                sustituciones.get(ejercicioActual.getId()), ventana());
        if (elegida.isEmpty()) {
            return;
        }
        if (elegida.get().equals(mantener)) {
            sustituciones.remove(ejercicioActual.getId());
            etiquetaEstado.setText("Se mantiene el ejercicio original.");
        } else {
            sustituciones.put(ejercicioActual.getId(), elegida.get());
            etiquetaEstado.setText("Serie sustituida por \"" + elegida.get() + "\".");
        }
        listaEjercicios.refresh();
        mostrarEjercicio(ejercicioActual);
    }

    @FXML
    private void verTecnica() {
        if (ejercicioActual == null) {
            return;
        }
        // Guia tecnica: modelo 2D animado siempre disponible + multimedia personalizada del usuario.
        GuiaTecnicaDialogo.mostrar(ejercicioActual, sustituciones.get(ejercicioActual.getId()),
                gestor.getModalidad(), ventana(), ruta -> {
            ejercicioActual.setRutaMultimedia(ruta);
            gestor.guardarCambiosEntrenamiento();
        });
    }

    @FXML
    private void adjuntarMultimedia() {
        if (ejercicioActual == null) {
            return;
        }
        File archivo = ArchivoMultimediaService
                .selectorMultimedia(gestor.getDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(ventana());
        if (archivo == null) {
            return;
        }
        ejercicioActual.setRutaMultimedia(archivo.getAbsolutePath());
        gestor.recordarCarpetaMultimedia(archivo.getParent());
        gestor.guardarCambiosEntrenamiento();
        etiquetaEstado.setText("Multimedia asociada a \"" + ejercicioActual.getNombre() + "\".");
    }

    @FXML
    private void finalizar() {
        long registradas = seriesPorEjercicio.values().stream()
                .flatMap(List::stream).filter(WorkoutPlayerController::registrable).count();
        if (registradas == 0 && !Dialogos.confirmar("Sesión sin series",
                "No has registrado ninguna serie.\n¿Guardar la sesión de todos modos?", ventana())) {
            return;
        }

        sesion.getRegistros().clear();
        for (Ejercicio ejercicio : rutina.getEjercicios()) {
            List<FilaSerie> filas = seriesPorEjercicio.getOrDefault(ejercicio.getId(), List.of());
            // El registro conserva fecha, nombre del ejercicio, grupo muscular y sus series
            // (peso en kg y repeticiones), de modo que el historial quede autocontenido en el JSON.
            RegistroEjercicio registro = new RegistroEjercicio(ejercicio, sesion.getFecha());
            String alternativa = sustituciones.get(ejercicio.getId());
            if (alternativa != null) {
                registro.setNombreEjercicio(alternativa);
            }
            for (FilaSerie fila : filas) {
                if (!registrable(fila)) {
                    continue;
                }
                // Se transcribe obligatoriamente el peso (kg) y las repeticiones de cada serie.
                SerieRegistrada serie = new SerieRegistrada(fila.getNumero());
                serie.setPesoKg(fila.getPeso());
                serie.setRepeticiones(fila.getRepeticiones());
                serie.setRirReal(fila.getRir());
                serie.setCompletada(true);
                // El volumen de carga real (peso x repeticiones) se deriva de estos valores.
                registro.getSeries().add(serie);
            }
            if (!registro.getSeries().isEmpty()) {
                sesion.getRegistros().add(registro);
            }
        }
        sesion.setFechaFin(LocalDateTime.now());
        gestor.guardarSesion(sesion);
        detener();
        if (alFinalizar != null) {
            alFinalizar.run();
        }
        escenario.close();
    }

    @FXML
    private void cancelar() {
        boolean descartar = Dialogos.confirmar("Descartar sesión",
                "¿Descartar el entrenamiento en curso? No se guardará ningún registro.", ventana());
        if (descartar) {
            detener();
            escenario.close();
        }
    }

    // ------------------------------------------------------------------
    // Cronometros y resumen
    // ------------------------------------------------------------------

    private void iniciarCronometroGlobal() {
        segundosSesion = 0;
        cronometro.setText(Formato.cronometro(java.time.Duration.ZERO));
        relojGlobal = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            segundosSesion++;
            cronometro.setText(Formato.cronometro(java.time.Duration.ofSeconds(segundosSesion)));
        }));
        relojGlobal.setCycleCount(Animation.INDEFINITE);
        relojGlobal.play();
    }

    private int descansoConfigurado() {
        try {
            String texto = spinnerDescanso.getEditor().getText();
            if (texto != null && !texto.isBlank()) {
                int valor = (int) Math.round(Formato.aNumero(texto));
                valor = Math.max(60, Math.min(180, valor));
                spinnerDescanso.getValueFactory().setValue(valor);
            }
        } catch (RuntimeException ignored) {
            // Si el editor no es valido se mantiene el valor actual del control.
        }
        Integer valor = spinnerDescanso.getValue();
        return valor == null ? 90 : valor;
    }

    private void actualizarEtiquetaDescanso() {
        int restante = Math.max(0, segundosDescanso);
        etiquetaDescanso.setText(String.format("%02d:%02d", restante / 60, restante % 60));
    }

    private void actualizarResumen() {
        double volumen = 0;
        int registradas = 0;
        for (List<FilaSerie> filas : seriesPorEjercicio.values()) {
            for (FilaSerie fila : filas) {
                if (registrable(fila)) {
                    volumen += fila.getPeso() * fila.getRepeticiones();
                    registradas++;
                }
            }
        }
        etiquetaVolumen.setText("Volumen: " + Formato.numero(volumen) + " kg · "
                + registradas + " series registradas");

        if (rutina != null) {
            long conSeries = rutina.getEjercicios().stream()
                    .filter(e -> seriesPorEjercicio.getOrDefault(e.getId(), List.of()).stream()
                            .anyMatch(WorkoutPlayerController::registrable))
                    .count();
            etiquetaProgreso.setText(conSeries + " de " + rutina.getEjercicios().size()
                    + " ejercicios con series registradas");
        }
        listaEjercicios.refresh();
    }

    private void detener() {
        if (relojGlobal != null) {
            relojGlobal.stop();
        }
        if (relojDescanso != null) {
            relojDescanso.stop();
        }
    }

    private Window ventana() {
        return escenario == null ? null : escenario;
    }

    /** Fila editable de la tabla de series del Workout Player. */
    public static final class FilaSerie {

        private final IntegerProperty numero;
        private final DoubleProperty peso = new SimpleDoubleProperty(0);
        private final IntegerProperty repeticiones = new SimpleIntegerProperty(0);
        private final IntegerProperty rir = new SimpleIntegerProperty(-1);
        private final BooleanProperty completada = new SimpleBooleanProperty(false);

        public FilaSerie(int numero, int repeticionesSugeridas, int rirSugerido) {
            this.numero = new SimpleIntegerProperty(numero);
            this.repeticiones.set(Math.max(0, repeticionesSugeridas));
            this.rir.set(rirSugerido);
        }

        public IntegerProperty numeroProperty() {
            return numero;
        }

        public DoubleProperty pesoProperty() {
            return peso;
        }

        public IntegerProperty repeticionesProperty() {
            return repeticiones;
        }

        public IntegerProperty rirProperty() {
            return rir;
        }

        public BooleanProperty completadaProperty() {
            return completada;
        }

        public int getNumero() {
            return numero.get();
        }

        public double getPeso() {
            return peso.get();
        }

        public void setPeso(double valor) {
            peso.set(valor);
        }

        public int getRepeticiones() {
            return repeticiones.get();
        }

        public void setRepeticiones(int valor) {
            repeticiones.set(valor);
        }

        public int getRir() {
            return rir.get();
        }

        public void setRir(int valor) {
            rir.set(valor);
        }

        public boolean isCompletada() {
            return completada.get();
        }
    }
}
