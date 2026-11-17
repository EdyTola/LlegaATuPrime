package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.ObjetivosNutricionales;
import com.symmetry.desktop.model.PerfilUsuario;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.PerfilService;
import com.symmetry.desktop.service.QuoteService;
import com.symmetry.desktop.service.SonidoService;
import com.symmetry.desktop.model.ConfiguracionApp;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.Formato;
import com.symmetry.desktop.util.FormularioDialogo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Modulo 5 - Perfil de usuario y multiusuario local.
 *
 * <p>Permite visualizar y editar la informacion personal del perfil activo (nombre, edad,
 * objetivo general y foto), ajustar sus metas diarias y gestionar los distintos perfiles
 * locales, cambiando entre ellos sin reiniciar la aplicacion.</p>
 */
public class PerfilController implements Initializable, VistaConRefresco {

    private static final String RUTA_FORMULARIO_PERFIL = "/com/symmetry/desktop/view/FormularioPerfil.fxml";
    private static final double RADIO_AVATAR = 52;

    private final GestorDatos gestor = GestorDatos.getInstancia();
    private final PerfilService perfiles = PerfilService.getInstancia();
    private final ObservableList<PerfilUsuario> lista = FXCollections.observableArrayList();

    /** Accion a ejecutar cuando cambia el perfil activo (inyectada por la ventana principal). */
    private Runnable alCambiarPerfil;

    @FXML
    private StackPane marcoAvatar;
    @FXML
    private ImageView imagenAvatar;
    @FXML
    private Label etiquetaIniciales;
    @FXML
    private TextField campoNombre;
    @FXML
    private CheckBox checkSfx;
    @FXML
    private Slider sliderSfx;
    @FXML
    private Button botonProbarSfx;
    /** Evita guardar mientras se cargan los valores iniciales en los controles de sonido. */
    private boolean cargandoSfx;
    @FXML
    private TextField campoFrase;
    @FXML
    private Label vistaFraseTexto;
    @FXML
    private Label vistaFraseAutor;
    @FXML
    private Label contadorFrase;
    @FXML
    private Spinner<Integer> spinnerEdad;
    @FXML
    private TextArea campoObjetivo;
    @FXML
    private Label etiquetaFechaRegistro;
    @FXML
    private Label etiquetaErrorPerfil;
    @FXML
    private Button botonQuitarFoto;
    @FXML
    private Spinner<Double> spinnerProteina;
    @FXML
    private Spinner<Double> spinnerCalorias;
    @FXML
    private Spinner<Integer> spinnerAgua;
    @FXML
    private Spinner<Double> spinnerCreatina;
    @FXML
    private Spinner<Double> spinnerSuenoMin;
    @FXML
    private Spinner<Double> spinnerSuenoMax;
    @FXML
    private ListView<PerfilUsuario> listaPerfiles;
    @FXML
    private Label etiquetaEstadoPerfiles;
    @FXML
    private Button botonUsarPerfil;
    @FXML
    private Button botonEliminarPerfil;

    /** Registra la accion que se dispara al cambiar de perfil activo. */
    public void setAlCambiarPerfil(Runnable alCambiarPerfil) {
        this.alCambiarPerfil = alCambiarPerfil;
    }

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        imagenAvatar.setClip(new Circle(RADIO_AVATAR, RADIO_AVATAR, RADIO_AVATAR));

        // La frase personal se limita en longitud y la vista previa se actualiza al escribir.
        campoFrase.setTextFormatter(new TextFormatter<>(cambio -> {
            int exceso = cambio.getControlNewText().length() - QuoteService.LONGITUD_MAXIMA;
            if (exceso <= 0) {
                return cambio;
            }
            // Un texto pegado demasiado largo se recorta en lugar de rechazarse.
            String texto = cambio.getText();
            if (texto.length() <= exceso) {
                return null;
            }
            cambio.setText(texto.substring(0, texto.length() - exceso));
            return cambio;
        }));
        campoFrase.textProperty().addListener((obs, anterior, actual) -> actualizarVistaFrase());

        // Los ajustes de sonido se guardan al momento, sin esperar a "Guardar cambios".
        checkSfx.selectedProperty().addListener((obs, antes, ahora) -> {
            sliderSfx.setDisable(!ahora);
            botonProbarSfx.setDisable(!ahora);
            guardarSfx();
        });
        sliderSfx.valueProperty().addListener((obs, antes, ahora) -> guardarSfx());

        spinnerEdad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, 25));
        spinnerProteina.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 500, 145, 5));
        spinnerCalorias.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1000, 6000, 2400, 50));
        spinnerAgua.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 8000, 2800, 100));
        spinnerCreatina.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 20, 5, 1));
        spinnerSuenoMin.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 12, 7, 0.5));
        spinnerSuenoMax.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 14, 9, 0.5));

        listaPerfiles.setItems(lista);
        listaPerfiles.setCellFactory(vista -> celdaPerfil());
        listaPerfiles.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> actualizarBotones(actual));

        refrescar();
    }

    /** Recarga el perfil activo y la lista de perfiles disponibles cada vez que se muestra. */
    @Override
    public void refrescar() {
        cargarPerfilActivo();
        recargarPerfiles();
    }

    // ------------------------------------------------------------------
    // Carga de datos
    // ------------------------------------------------------------------

    private void cargarPerfilActivo() {
        PerfilUsuario perfil = gestor.getDatos().getPerfil();
        campoNombre.setText(perfil.getNombre());
        spinnerEdad.getValueFactory().setValue(perfil.getEdad() == null ? 25 : perfil.getEdad());
        campoObjetivo.setText(perfil.getObjetivoGeneral() == null ? "" : perfil.getObjetivoGeneral());
        campoFrase.setText(perfil.getCustomQuote() == null ? "" : perfil.getCustomQuote());
        actualizarVistaFrase();

        cargandoSfx = true;
        ConfiguracionApp config = gestor.getDatos().getConfiguracion();
        checkSfx.setSelected(config.isSfxActivados());
        sliderSfx.setValue(config.getVolumenSfx());
        sliderSfx.setDisable(!config.isSfxActivados());
        botonProbarSfx.setDisable(!config.isSfxActivados());
        cargandoSfx = false;
        etiquetaFechaRegistro.setText(perfil.getFechaRegistro() == null ? ""
                : "Perfil creado el " + Formato.fechaCorta(perfil.getFechaRegistro()));

        ObjetivosNutricionales metas = perfil.getObjetivos();
        spinnerProteina.getValueFactory().setValue(metas.getProteinaGramos());
        spinnerCalorias.getValueFactory().setValue(metas.getCalorias());
        spinnerAgua.getValueFactory().setValue(metas.getAguaMililitros());
        spinnerCreatina.getValueFactory().setValue(metas.getCreatinaGramos());
        spinnerSuenoMin.getValueFactory().setValue(metas.getSuenoMinimoHoras());
        spinnerSuenoMax.getValueFactory().setValue(metas.getSuenoMaximoHoras());

        etiquetaErrorPerfil.setText("");
        actualizarAvatar(perfil);
    }

    private void guardarSfx() {
        if (cargandoSfx) {
            return;
        }
        ConfiguracionApp config = gestor.getDatos().getConfiguracion();
        config.setSfxActivados(checkSfx.isSelected());
        config.setVolumenSfx(sliderSfx.getValue());
        gestor.guardarAutomatico();
    }

    /** Reproduce el sonido de logro con el volumen elegido para que el usuario lo compruebe. */
    @FXML
    private void probarSfx() {
        SonidoService.reproducir(SonidoService.Efecto.LOGRO, sliderSfx.getValue());
    }

    /** Vista previa de la frase personal tal como aparecera en el Inicio. */
    private void actualizarVistaFrase() {
        String texto = campoFrase.getText() == null ? "" : campoFrase.getText().trim();
        boolean hayFrase = !texto.isEmpty();
        vistaFraseTexto.setText(hayFrase ? "\u201C" + texto + "\u201D"
                : "Escribe tu frase de poder: aparecerá en tu Inicio junto a las frases legendarias.");
        vistaFraseAutor.setText(hayFrase ? QuoteService.AUTOR_PERSONAL : "");
        contadorFrase.setText(texto.length() + " / " + QuoteService.LONGITUD_MAXIMA);
    }

    private void recargarPerfiles() {
        String activoId = perfiles.getRegistro().getPerfilActivoId();
        lista.setAll(perfiles.listarPerfiles());
        listaPerfiles.getSelectionModel().select(
                lista.stream().filter(p -> p.getId().equals(activoId)).findFirst().orElse(null));
        etiquetaEstadoPerfiles.setText(lista.size() + (lista.size() == 1
                ? " perfil registrado" : " perfiles registrados"));
    }

    /** Refresca el avatar: foto local si existe, iniciales en caso contrario. */
    private void actualizarAvatar(PerfilUsuario perfil) {
        String ruta = perfil.getRutaFotoPerfil();
        boolean hayFoto = ArchivoMultimediaService.existe(ruta);
        if (hayFoto) {
            try {
                imagenAvatar.setImage(new Image(new File(ruta).toURI().toString(), 104, 104, false, true, true));
                imagenAvatar.setVisible(true);
                etiquetaIniciales.setVisible(false);
                botonQuitarFoto.setDisable(false);
                return;
            } catch (RuntimeException ignored) {
                // Si la imagen no puede cargarse se muestra el avatar con iniciales.
            }
        }
        imagenAvatar.setImage(null);
        imagenAvatar.setVisible(false);
        etiquetaIniciales.setText(perfil.getIniciales());
        etiquetaIniciales.setVisible(true);
        botonQuitarFoto.setDisable(true);
    }

    // ------------------------------------------------------------------
    // Edicion del perfil activo
    // ------------------------------------------------------------------

    /** Muestra un aviso bajo el formulario, con color de exito o de error. */
    private void mostrarMensaje(String mensaje, boolean exito) {
        etiquetaErrorPerfil.setText(mensaje);
        etiquetaErrorPerfil.getStyleClass().removeAll("texto-error", "texto-exito");
        etiquetaErrorPerfil.getStyleClass().add(exito ? "texto-exito" : "texto-error");
    }

    @FXML
    private void guardarCambios() {
        String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarMensaje("El nombre no puede estar vacío.", false);
            return;
        }
        if (spinnerSuenoMax.getValue() < spinnerSuenoMin.getValue()) {
            mostrarMensaje("El sueño máximo no puede ser menor que el mínimo.", false);
            return;
        }

        PerfilUsuario perfil = gestor.getDatos().getPerfil();
        perfil.setNombre(nombre);
        perfil.setEdad(spinnerEdad.getValue());
        String objetivo = campoObjetivo.getText() == null ? "" : campoObjetivo.getText().trim();
        perfil.setObjetivoGeneral(objetivo.isEmpty() ? null : objetivo);
        String frase = campoFrase.getText() == null ? "" : campoFrase.getText().trim();
        perfil.setCustomQuote(frase.isEmpty() ? null : frase);

        ObjetivosNutricionales metas = perfil.getObjetivos();
        metas.setProteinaGramos(spinnerProteina.getValue());
        metas.setCalorias(spinnerCalorias.getValue());
        metas.setAguaMililitros(spinnerAgua.getValue());
        metas.setCreatinaGramos(spinnerCreatina.getValue());
        metas.setSuenoMinimoHoras(spinnerSuenoMin.getValue());
        metas.setSuenoMaximoHoras(spinnerSuenoMax.getValue());

        gestor.guardar();
        perfiles.sincronizarIndice(perfil);
        actualizarAvatar(perfil);
        recargarPerfiles();
        mostrarMensaje("Cambios guardados.", true);
    }

    @FXML
    private void seleccionarFoto() {
        PerfilUsuario perfil = gestor.getDatos().getPerfil();
        File archivo = ArchivoMultimediaService
                .selectorImagen(gestor.getDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(ventana());
        if (archivo == null) {
            return;
        }
        perfil.setRutaFotoPerfil(archivo.getAbsolutePath());
        gestor.recordarCarpetaMultimedia(archivo.getParent());
        gestor.guardar();
        perfiles.sincronizarIndice(perfil);
        actualizarAvatar(perfil);
        etiquetaErrorPerfil.setText("");
    }

    @FXML
    private void quitarFoto() {
        PerfilUsuario perfil = gestor.getDatos().getPerfil();
        perfil.setRutaFotoPerfil(null);
        gestor.guardar();
        perfiles.sincronizarIndice(perfil);
        actualizarAvatar(perfil);
    }

    // ------------------------------------------------------------------
    // Gestion de perfiles locales
    // ------------------------------------------------------------------

    @FXML
    private void nuevoPerfil() {
        FormularioDialogo dialogo = new FormularioDialogo(RUTA_FORMULARIO_PERFIL,
                "Nuevo perfil", "Crear", ventana());
        FormularioPerfilController controlador = (FormularioPerfilController) dialogo.getControlador();
        controlador.setPerfilesExistentes(perfiles.listarPerfiles());
        controlador.preparar();
        dialogo.setValidador(controlador::validar);
        if (!dialogo.mostrarYConfirmar()) {
            return;
        }
        try {
            PerfilUsuario creado = perfiles.crearPerfil(controlador.getNombre(),
                    controlador.getEdad(), controlador.getObjetivo());
            recargarPerfiles();
            listaPerfiles.getSelectionModel().select(creado);
            boolean cambiar = Dialogos.confirmar("Perfil creado",
                    "¿Quieres empezar a usar el perfil \"" + creado.getNombre() + "\" ahora mismo?",
                    ventana());
            if (cambiar) {
                usarPerfil();
            }
        } catch (IllegalArgumentException e) {
            Dialogos.error("No se pudo crear el perfil", e.getMessage(), ventana());
        }
    }

    @FXML
    private void usarPerfil() {
        PerfilUsuario seleccionado = listaPerfiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null || esActivo(seleccionado)) {
            return;
        }
        boolean confirmado = Dialogos.confirmar("Cambiar de perfil",
                "Se abrirán los datos del perfil \"" + seleccionado.getNombre()
                        + "\". El progreso de cada usuario se mantiene guardado por separado.",
                ventana());
        if (!confirmado) {
            return;
        }
        perfiles.activar(seleccionado.getId());
        if (alCambiarPerfil != null) {
            alCambiarPerfil.run();
        }
    }

    @FXML
    private void eliminarPerfil() {
        PerfilUsuario seleccionado = listaPerfiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        if (esActivo(seleccionado)) {
            Dialogos.info("Perfil en uso",
                    "No puedes eliminar el perfil activo. Cambia a otro perfil antes de borrarlo.");
            return;
        }
        if (perfiles.getRegistro().getTotal() <= 1) {
            Dialogos.info("Único perfil", "Debe existir al menos un perfil en la aplicación.");
            return;
        }
        boolean confirmado = Dialogos.confirmar("Eliminar perfil",
                "¿Eliminar el perfil \"" + seleccionado.getNombre()
                        + "\" y todos sus datos (rutinas, progreso y métricas)?",
                ventana());
        if (confirmado) {
            perfiles.eliminarPerfil(seleccionado.getId(), true);
            recargarPerfiles();
        }
    }

    private boolean esActivo(PerfilUsuario perfil) {
        return perfil != null && perfil.getId().equals(perfiles.getRegistro().getPerfilActivoId());
    }

    private void actualizarBotones(PerfilUsuario seleccionado) {
        boolean activo = esActivo(seleccionado);
        botonUsarPerfil.setDisable(seleccionado == null || activo);
        botonEliminarPerfil.setDisable(seleccionado == null || activo
                || perfiles.getRegistro().getTotal() <= 1);
    }

    /** Celda de la lista de perfiles: avatar reducido, nombre y estado activo. */
    private ListCell<PerfilUsuario> celdaPerfil() {
        return new ListCell<>() {
            private final Label iniciales = new Label();
            private final StackPane avatar = new StackPane(iniciales);
            private final Label nombre = new Label();
            private final Label detalle = new Label();
            private final Label chipActivo = new Label("En uso");
            private final VBox textos = new VBox(1, nombre, detalle);
            private final Region separador = new Region();
            private final HBox caja = new HBox(10, avatar, textos, separador, chipActivo);

            {
                avatar.getStyleClass().add("avatar-mini");
                iniciales.getStyleClass().add("avatar-iniciales-mini");
                nombre.getStyleClass().add("perfil-nombre");
                detalle.getStyleClass().add("texto-secundario");
                chipActivo.getStyleClass().add("chip-activo");
                HBox.setHgrow(separador, javafx.scene.layout.Priority.ALWAYS);
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(PerfilUsuario perfil, boolean vacio) {
                super.updateItem(perfil, vacio);
                if (vacio || perfil == null) {
                    setGraphic(null);
                    return;
                }
                iniciales.setText(perfil.getIniciales());
                nombre.setText(perfil.getNombre());
                String objetivo = perfil.getObjetivoGeneral();
                detalle.setText(perfil.getEdadTexto()
                        + (objetivo == null || objetivo.isBlank() ? "" : " · " + objetivo));
                boolean activo = esActivo(perfil);
                chipActivo.setVisible(activo);
                chipActivo.setManaged(activo);
                setGraphic(caja);
            }
        };
    }

    private Window ventana() {
        return campoNombre.getScene() == null ? null : campoNombre.getScene().getWindow();
    }
}
