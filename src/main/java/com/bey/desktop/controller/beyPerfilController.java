package com.bey.desktop.controller;

import com.bey.desktop.model.beyObjetivosNutricionales;
import com.bey.desktop.model.beyPerfilUsuario;
import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beyPerfilService;
import com.bey.desktop.service.beyQuoteService;
import com.bey.desktop.service.beySonidoService;
import com.bey.desktop.model.beyConfiguracionApp;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormato;
import com.bey.desktop.util.beyFormularioDialogo;
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

public class beyPerfilController implements Initializable, beyVistaConRefresco {
    private static final String RUTA_FORMULARIO_PERFIL = "/com/bey/desktop/view/beyFormularioPerfil.fxml";
    private static final double RADIO_AVATAR = 52;

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();
    private final beyPerfilService beyPerfiles = beyPerfilService.beyGetInstancia();
    private final ObservableList<beyPerfilUsuario> beyLista = FXCollections.observableArrayList();

    private Runnable beyAlCambiarPerfil;

    @FXML
    private StackPane beyMarcoAvatar;
    @FXML
    private ImageView beyImagenAvatar;
    @FXML
    private Label beyEtiquetaIniciales;
    @FXML
    private TextField beyCampoNombre;
    @FXML
    private CheckBox beyCheckSfx;
    @FXML
    private Slider beySliderSfx;
    @FXML
    private Button beyBotonProbarSfx;

    private boolean beyCargandoSfx;
    @FXML
    private TextField beyCampoFrase;
    @FXML
    private Label beyVistaFraseTexto;
    @FXML
    private Label beyVistaFraseAutor;
    @FXML
    private Label beyContadorFrase;
    @FXML
    private Spinner<Integer> beySpinnerEdad;
    @FXML
    private TextArea beyCampoObjetivo;
    @FXML
    private Label beyEtiquetaFechaRegistro;
    @FXML
    private Label beyEtiquetaErrorPerfil;
    @FXML
    private Button beyBotonQuitarFoto;
    @FXML
    private Spinner<Double> beySpinnerProteina;
    @FXML
    private Spinner<Double> beySpinnerCalorias;
    @FXML
    private Spinner<Integer> beySpinnerAgua;
    @FXML
    private Spinner<Double> beySpinnerCreatina;
    @FXML
    private Spinner<Double> beySpinnerSuenoMin;
    @FXML
    private Spinner<Double> beySpinnerSuenoMax;
    @FXML
    private ListView<beyPerfilUsuario> beyListaPerfiles;
    @FXML
    private Label beyEtiquetaEstadoPerfiles;
    @FXML
    private Button beyBotonUsarPerfil;
    @FXML
    private Button beyBotonEliminarPerfil;

    public void beySetAlCambiarPerfil(Runnable alCambiarPerfil) {
        this.beyAlCambiarPerfil = alCambiarPerfil;
    }

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyImagenAvatar.setClip(new Circle(RADIO_AVATAR, RADIO_AVATAR, RADIO_AVATAR));

        beyCampoFrase.setTextFormatter(new TextFormatter<>(cambio -> {
            int exceso = cambio.getControlNewText().length() - beyQuoteService.LONGITUD_MAXIMA;
            if (exceso <= 0) {
                return cambio;
            }

            String texto = cambio.getText();
            if (texto.length() <= exceso) {
                return null;
            }
            cambio.setText(texto.substring(0, texto.length() - exceso));
            return cambio;
        }));
        beyCampoFrase.textProperty().addListener((obs, anterior, actual) -> beyActualizarVistaFrase());

        beyCheckSfx.selectedProperty().addListener((obs, antes, ahora) -> {
            beySliderSfx.setDisable(!ahora);
            beyBotonProbarSfx.setDisable(!ahora);
            beyGuardarSfx();
        });
        beySliderSfx.valueProperty().addListener((obs, antes, ahora) -> beyGuardarSfx());

        beySpinnerEdad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, 25));
        beySpinnerProteina.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 500, 145, 5));
        beySpinnerCalorias.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1000, 6000, 2400, 50));
        beySpinnerAgua.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 8000, 2800, 100));
        beySpinnerCreatina.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 20, 5, 1));
        beySpinnerSuenoMin.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 12, 7, 0.5));
        beySpinnerSuenoMax.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 14, 9, 0.5));

        beyListaPerfiles.setItems(beyLista);
        beyListaPerfiles.setCellFactory(vista -> beyCeldaPerfil());
        beyListaPerfiles.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> beyActualizarBotones(actual));

        beyRefrescar();
    }

    @Override
    public void beyRefrescar() {
        beyCargarPerfilActivo();
        beyRecargarPerfiles();
    }

    private void beyCargarPerfilActivo() {
        beyPerfilUsuario perfil = beyGestor.beyGetDatos().getPerfil();
        beyCampoNombre.setText(perfil.getNombre());
        beySpinnerEdad.getValueFactory().setValue(perfil.getEdad() == null ? 25 : perfil.getEdad());
        beyCampoObjetivo.setText(perfil.getObjetivoGeneral() == null ? "" : perfil.getObjetivoGeneral());
        beyCampoFrase.setText(perfil.getCustomQuote() == null ? "" : perfil.getCustomQuote());
        beyActualizarVistaFrase();

        beyCargandoSfx = true;
        beyConfiguracionApp config = beyGestor.beyGetDatos().getConfiguracion();
        beyCheckSfx.setSelected(config.isSfxActivados());
        beySliderSfx.setValue(config.getVolumenSfx());
        beySliderSfx.setDisable(!config.isSfxActivados());
        beyBotonProbarSfx.setDisable(!config.isSfxActivados());
        beyCargandoSfx = false;
        beyEtiquetaFechaRegistro.setText(perfil.getFechaRegistro() == null ? ""
                : "Perfil creado el " + beyFormato.beyFechaCorta(perfil.getFechaRegistro()));

        beyObjetivosNutricionales metas = perfil.getObjetivos();
        beySpinnerProteina.getValueFactory().setValue(metas.getProteinaGramos());
        beySpinnerCalorias.getValueFactory().setValue(metas.getCalorias());
        beySpinnerAgua.getValueFactory().setValue(metas.getAguaMililitros());
        beySpinnerCreatina.getValueFactory().setValue(metas.getCreatinaGramos());
        beySpinnerSuenoMin.getValueFactory().setValue(metas.getSuenoMinimoHoras());
        beySpinnerSuenoMax.getValueFactory().setValue(metas.getSuenoMaximoHoras());

        beyEtiquetaErrorPerfil.setText("");
        beyActualizarAvatar(perfil);
    }

    private void beyGuardarSfx() {
        if (beyCargandoSfx) {
            return;
        }
        beyConfiguracionApp config = beyGestor.beyGetDatos().getConfiguracion();
        config.setSfxActivados(beyCheckSfx.isSelected());
        config.setVolumenSfx(beySliderSfx.getValue());
        beyGestor.beyGuardarAutomatico();
    }

    @FXML
    private void beyProbarSfx() {
        beySonidoService.beyReproducir(beySonidoService.beyEfecto.LOGRO, beySliderSfx.getValue());
    }

    private void beyActualizarVistaFrase() {
        String texto = beyCampoFrase.getText() == null ? "" : beyCampoFrase.getText().trim();
        boolean hayFrase = !texto.isEmpty();
        beyVistaFraseTexto.setText(hayFrase ? "\u201C" + texto + "\u201D"
                : "Escribe tu frase de poder: aparecerá en tu Inicio junto a las frases legendarias.");
        beyVistaFraseAutor.setText(hayFrase ? beyQuoteService.AUTOR_PERSONAL : "");
        beyContadorFrase.setText(texto.length() + " / " + beyQuoteService.LONGITUD_MAXIMA);
    }

    private void beyRecargarPerfiles() {
        String activoId = beyPerfiles.beyGetRegistro().getPerfilActivoId();
        beyLista.setAll(beyPerfiles.beyListarPerfiles());
        beyListaPerfiles.getSelectionModel().select(
                beyLista.stream().filter(p -> p.getId().equals(activoId)).findFirst().orElse(null));
        beyEtiquetaEstadoPerfiles.setText(beyLista.size() + (beyLista.size() == 1
                ? " perfil registrado" : " perfiles registrados"));
    }

    private void beyActualizarAvatar(beyPerfilUsuario perfil) {
        String ruta = perfil.getRutaFotoPerfil();
        boolean hayFoto = beyArchivoMultimediaService.beyExiste(ruta);
        if (hayFoto) {
            try {
                beyImagenAvatar.setImage(new Image(new File(ruta).toURI().toString(), 104, 104, false, true, true));
                beyImagenAvatar.setVisible(true);
                beyEtiquetaIniciales.setVisible(false);
                beyBotonQuitarFoto.setDisable(false);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        beyImagenAvatar.setImage(null);
        beyImagenAvatar.setVisible(false);
        beyEtiquetaIniciales.setText(perfil.getIniciales());
        beyEtiquetaIniciales.setVisible(true);
        beyBotonQuitarFoto.setDisable(true);
    }

    private void beyMostrarMensaje(String mensaje, boolean exito) {
        beyEtiquetaErrorPerfil.setText(mensaje);
        beyEtiquetaErrorPerfil.getStyleClass().removeAll("texto-error", "texto-exito");
        beyEtiquetaErrorPerfil.getStyleClass().add(exito ? "texto-exito" : "texto-error");
    }

    @FXML
    private void beyGuardarCambios() {
        String nombre = beyCampoNombre.getText() == null ? "" : beyCampoNombre.getText().trim();
        if (nombre.isEmpty()) {
            beyMostrarMensaje("El nombre no puede estar vacío.", false);
            return;
        }
        if (beySpinnerSuenoMax.getValue() < beySpinnerSuenoMin.getValue()) {
            beyMostrarMensaje("El sueño máximo no puede ser menor que el mínimo.", false);
            return;
        }

        beyPerfilUsuario perfil = beyGestor.beyGetDatos().getPerfil();
        perfil.setNombre(nombre);
        perfil.setEdad(beySpinnerEdad.getValue());
        String objetivo = beyCampoObjetivo.getText() == null ? "" : beyCampoObjetivo.getText().trim();
        perfil.setObjetivoGeneral(objetivo.isEmpty() ? null : objetivo);
        String frase = beyCampoFrase.getText() == null ? "" : beyCampoFrase.getText().trim();
        perfil.setCustomQuote(frase.isEmpty() ? null : frase);

        beyObjetivosNutricionales metas = perfil.getObjetivos();
        metas.setProteinaGramos(beySpinnerProteina.getValue());
        metas.setCalorias(beySpinnerCalorias.getValue());
        metas.setAguaMililitros(beySpinnerAgua.getValue());
        metas.setCreatinaGramos(beySpinnerCreatina.getValue());
        metas.setSuenoMinimoHoras(beySpinnerSuenoMin.getValue());
        metas.setSuenoMaximoHoras(beySpinnerSuenoMax.getValue());

        beyGestor.beyGuardar();
        beyPerfiles.beySincronizarIndice(perfil);
        beyActualizarAvatar(perfil);
        beyRecargarPerfiles();
        beyMostrarMensaje("Cambios guardados.", true);
    }

    @FXML
    private void beySeleccionarFoto() {
        beyPerfilUsuario perfil = beyGestor.beyGetDatos().getPerfil();
        File archivo = beyArchivoMultimediaService
                .beySelectorImagen(beyGestor.beyGetDatos().getConfiguracion().getUltimaCarpetaMultimedia())
                .showOpenDialog(beyVentana());
        if (archivo == null) {
            return;
        }
        perfil.setRutaFotoPerfil(archivo.getAbsolutePath());
        beyGestor.beyRecordarCarpetaMultimedia(archivo.getParent());
        beyGestor.beyGuardar();
        beyPerfiles.beySincronizarIndice(perfil);
        beyActualizarAvatar(perfil);
        beyEtiquetaErrorPerfil.setText("");
    }

    @FXML
    private void beyQuitarFoto() {
        beyPerfilUsuario perfil = beyGestor.beyGetDatos().getPerfil();
        perfil.setRutaFotoPerfil(null);
        beyGestor.beyGuardar();
        beyPerfiles.beySincronizarIndice(perfil);
        beyActualizarAvatar(perfil);
    }

    @FXML
    private void beyNuevoPerfil() {
        beyFormularioDialogo dialogo = new beyFormularioDialogo(RUTA_FORMULARIO_PERFIL,
                "Nuevo perfil", "Crear", beyVentana());
        beyFormularioPerfilController controlador = (beyFormularioPerfilController) dialogo.beyGetControlador();
        controlador.beySetPerfilesExistentes(beyPerfiles.beyListarPerfiles());
        controlador.beyPreparar();
        dialogo.beySetValidador(controlador::beyValidar);
        if (!dialogo.beyMostrarYConfirmar()) {
            return;
        }
        try {
            beyPerfilUsuario creado = beyPerfiles.beyCrearPerfil(controlador.getNombre(),
                    controlador.beyGetEdad(), controlador.beyGetObjetivo());
            beyRecargarPerfiles();
            beyListaPerfiles.getSelectionModel().select(creado);
            boolean cambiar = beyDialogos.beyConfirmar("Perfil creado",
                    "¿Quieres empezar a usar el perfil \"" + creado.getNombre() + "\" ahora mismo?",
                    beyVentana());
            if (cambiar) {
                beyUsarPerfil();
            }
        } catch (IllegalArgumentException e) {
            beyDialogos.beyError("No se pudo crear el perfil", e.getMessage(), beyVentana());
        }
    }

    @FXML
    private void beyUsarPerfil() {
        beyPerfilUsuario seleccionado = beyListaPerfiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null || beyEsActivo(seleccionado)) {
            return;
        }
        boolean confirmado = beyDialogos.beyConfirmar("Cambiar de perfil",
                "Se abrirán los datos del perfil \"" + seleccionado.getNombre()
                        + "\". El progreso de cada usuario se mantiene guardado por separado.",
                beyVentana());
        if (!confirmado) {
            return;
        }
        beyPerfiles.beyActivar(seleccionado.getId());
        if (beyAlCambiarPerfil != null) {
            beyAlCambiarPerfil.run();
        }
    }

    @FXML
    private void beyEliminarPerfil() {
        beyPerfilUsuario seleccionado = beyListaPerfiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        if (beyEsActivo(seleccionado)) {
            beyDialogos.beyInfo("Perfil en uso",
                    "No puedes eliminar el perfil activo. Cambia a otro perfil antes de borrarlo.");
            return;
        }
        if (beyPerfiles.beyGetRegistro().getTotal() <= 1) {
            beyDialogos.beyInfo("Único perfil", "Debe existir al menos un perfil en la aplicación.");
            return;
        }
        boolean confirmado = beyDialogos.beyConfirmar("Eliminar perfil",
                "¿Eliminar el perfil \"" + seleccionado.getNombre()
                        + "\" y todos sus datos (rutinas, progreso y métricas)?",
                beyVentana());
        if (confirmado) {
            beyPerfiles.beyEliminarPerfil(seleccionado.getId(), true);
            beyRecargarPerfiles();
        }
    }

    private boolean beyEsActivo(beyPerfilUsuario perfil) {
        return perfil != null && perfil.getId().equals(beyPerfiles.beyGetRegistro().getPerfilActivoId());
    }

    private void beyActualizarBotones(beyPerfilUsuario seleccionado) {
        boolean activo = beyEsActivo(seleccionado);
        beyBotonUsarPerfil.setDisable(seleccionado == null || activo);
        beyBotonEliminarPerfil.setDisable(seleccionado == null || activo
                || beyPerfiles.beyGetRegistro().getTotal() <= 1);
    }

    private ListCell<beyPerfilUsuario> beyCeldaPerfil() {
        return new ListCell<>() {
            private final Label beyIniciales = new Label();
            private final StackPane beyAvatar = new StackPane(beyIniciales);
            private final Label beyNombre = new Label();
            private final Label beyDetalle = new Label();
            private final Label beyChipActivo = new Label("En uso");
            private final VBox beyTextos = new VBox(1, beyNombre, beyDetalle);
            private final Region beySeparador = new Region();
            private final HBox beyCaja = new HBox(10, beyAvatar, beyTextos, beySeparador, beyChipActivo);

            {
                beyAvatar.getStyleClass().add("avatar-mini");
                beyIniciales.getStyleClass().add("avatar-iniciales-mini");
                beyNombre.getStyleClass().add("perfil-nombre");
                beyDetalle.getStyleClass().add("texto-secundario");
                beyChipActivo.getStyleClass().add("chip-activo");
                HBox.setHgrow(beySeparador, javafx.scene.layout.Priority.ALWAYS);
                beyCaja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(beyPerfilUsuario perfil, boolean vacio) {
                super.updateItem(perfil, vacio);
                if (vacio || perfil == null) {
                    setGraphic(null);
                    return;
                }
                beyIniciales.setText(perfil.getIniciales());
                beyNombre.setText(perfil.getNombre());
                String objetivo = perfil.getObjetivoGeneral();
                beyDetalle.setText(perfil.getEdadTexto()
                        + (objetivo == null || objetivo.isBlank() ? "" : " · " + objetivo));
                boolean activo = beyEsActivo(perfil);
                beyChipActivo.setVisible(activo);
                beyChipActivo.setManaged(activo);
                setGraphic(beyCaja);
            }
        };
    }

    private Window beyVentana() {
        return beyCampoNombre.getScene() == null ? null : beyCampoNombre.getScene().getWindow();
    }
}
