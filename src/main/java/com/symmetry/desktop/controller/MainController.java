package com.symmetry.desktop.controller;

import com.symmetry.desktop.model.TemaApp;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.TemaService;
import com.symmetry.desktop.util.Dialogos;
import com.symmetry.desktop.util.Formato;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador de la ventana principal: menu lateral de navegacion y contenedor de vistas.
 */
public class MainController implements Initializable {

    private static final String VISTA_DASHBOARD = "/com/symmetry/desktop/view/DashboardView.fxml";
    private static final String VISTA_ENTRENAMIENTO = "/com/symmetry/desktop/view/EntrenamientoView.fxml";
    private static final String VISTA_NUTRICION = "/com/symmetry/desktop/view/NutricionView.fxml";
    private static final String VISTA_MUSICA = "/com/symmetry/desktop/view/MusicaView.fxml";
    private static final String VISTA_PROGRESO = "/com/symmetry/desktop/view/ProgresoView.fxml";
    private static final String VISTA_PERFIL = "/com/symmetry/desktop/view/PerfilView.fxml";

    private static final String CLAVE_DASHBOARD = "dashboard";
    private static final String CLAVE_ENTRENAMIENTO = "entrenamiento";
    private static final String CLAVE_NUTRICION = "nutricion";
    private static final String CLAVE_MUSICA = "musica";
    private static final String CLAVE_PROGRESO = "progreso";
    private static final String CLAVE_PERFIL = "perfil";

    @FXML
    private StackPane contenedorVistas;
    @FXML
    private Label etiquetaTituloSeccion;
    @FXML
    private Label etiquetaFechaCabecera;
    @FXML
    private Label etiquetaRutaDatos;
    @FXML
    private ToggleButton botonInicio;
    @FXML
    private ToggleButton botonEntrenamiento;
    @FXML
    private ToggleButton botonNutricion;
    @FXML
    private ToggleButton botonMusica;
    @FXML
    private ToggleButton botonProgreso;
    @FXML
    private ToggleButton botonPerfil;
    @FXML
    private ToggleButton botonTema;
    @FXML
    private Label iconoTema;

    private final Map<String, VistaCargada> vistas = new HashMap<>();

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        ToggleGroup grupo = new ToggleGroup();
        botonInicio.setToggleGroup(grupo);
        botonEntrenamiento.setToggleGroup(grupo);
        botonNutricion.setToggleGroup(grupo);
        botonMusica.setToggleGroup(grupo);
        botonProgreso.setToggleGroup(grupo);
        botonPerfil.setToggleGroup(grupo);

        etiquetaFechaCabecera.setText(Formato.fechaLarga(LocalDate.now()));
        etiquetaRutaDatos.setText("Archivo: " + GestorDatos.getInstancia().getRutaDatos().getFileName());
        sincronizarBotonTema();

        mostrar(CLAVE_DASHBOARD);
    }

    /** Alterna el tema claro/oscuro de toda la aplicacion en caliente. */
    @FXML
    private void alternarTema() {
        TemaApp nuevo = GestorDatos.getInstancia().alternarTema();
        TemaService.aplicarGlobal(nuevo);
        sincronizarBotonTema();
    }

    /** Refleja en el interruptor el tema actualmente activo. */
    private void sincronizarBotonTema() {
        boolean oscuro = GestorDatos.getInstancia().getTema() == TemaApp.OSCURO;
        botonTema.setSelected(oscuro);
        botonTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
        if (iconoTema != null) {
            // Luna cuando se ofrece activar el modo oscuro; sol cuando ya esta activo.
            iconoTema.setText(oscuro ? "\u2600" : "\u263D");
        }
    }

    @FXML
    private void mostrarInicio() {
        botonInicio.setSelected(true);
        mostrar(CLAVE_DASHBOARD);
    }

    @FXML
    private void mostrarEntrenamiento() {
        botonEntrenamiento.setSelected(true);
        mostrar(CLAVE_ENTRENAMIENTO);
    }

    @FXML
    private void mostrarNutricion() {
        botonNutricion.setSelected(true);
        mostrar(CLAVE_NUTRICION);
    }

    @FXML
    private void mostrarMusica() {
        botonMusica.setSelected(true);
        mostrar(CLAVE_MUSICA);
    }

    @FXML
    private void mostrarProgreso() {
        botonProgreso.setSelected(true);
        mostrar(CLAVE_PROGRESO);
    }

    @FXML
    private void mostrarPerfil() {
        botonPerfil.setSelected(true);
        mostrar(CLAVE_PERFIL);
    }

    /**
     * Reinicia la interfaz tras un cambio de perfil: descarta las vistas en cache (que
     * apuntaban a los datos del usuario anterior), actualiza la ruta de datos y vuelve
     * al panel de inicio del nuevo perfil.
     */
    private void reiniciarTrasCambioDePerfil() {
        vistas.clear();
        contenedorVistas.getChildren().clear();
        etiquetaRutaDatos.setText("Archivo: " + GestorDatos.getInstancia().getRutaDatos().getFileName());
        botonInicio.setSelected(true);
        mostrar(CLAVE_DASHBOARD);
    }

    /** Carga (o reutiliza) la vista indicada y la coloca en el area de contenido. */
    public void mostrar(String clave) {
        VistaCargada vista = vistas.get(clave);
        if (vista == null) {
            vista = cargarVista(clave);
            if (vista == null) {
                return;
            }
            vistas.put(clave, vista);
        }
        contenedorVistas.getChildren().setAll(vista.nodo());
        etiquetaTituloSeccion.setText(vista.titulo());
        if (vista.controlador() instanceof VistaConRefresco refresco) {
            refresco.refrescar();
        }
    }

    private VistaCargada cargarVista(String clave) {
        String fxml;
        String titulo;
        switch (clave) {
            case CLAVE_ENTRENAMIENTO -> {
                fxml = VISTA_ENTRENAMIENTO;
                titulo = "Entrenamiento";
            }
            case CLAVE_NUTRICION -> {
                fxml = VISTA_NUTRICION;
                titulo = "Nutricion, hidratacion y descanso";
            }
            case CLAVE_MUSICA -> {
                fxml = VISTA_MUSICA;
                titulo = "Reproductor de musica local";
            }
            case CLAVE_PROGRESO -> {
                fxml = VISTA_PROGRESO;
                titulo = "Progreso y analiticas";
            }
            case CLAVE_PERFIL -> {
                fxml = VISTA_PERFIL;
                titulo = "Perfil de usuario";
            }
            default -> {
                fxml = VISTA_DASHBOARD;
                titulo = "Inicio";
            }
        }
        try {
            FXMLLoader cargador = new FXMLLoader(MainController.class.getResource(fxml));
            Node nodo = cargador.load();
            Object controlador = cargador.getController();
            if (controlador instanceof PerfilController perfil) {
                perfil.setAlCambiarPerfil(this::reiniciarTrasCambioDePerfil);
            }
            return new VistaCargada(nodo, controlador, titulo);
        } catch (IOException | RuntimeException e) {
            Dialogos.error("Error al abrir la vista",
                    "No se pudo cargar la pantalla solicitada.\n" + e.getMessage());
            return null;
        }
    }

    /** Vista cargada en memoria junto a su controlador. */
    private record VistaCargada(Node nodo, Object controlador, String titulo) {
    }
}
