package com.bey.desktop.controller;

import com.bey.desktop.model.beyTemaApp;
import com.bey.desktop.service.beyGamificacionService;
import com.bey.desktop.service.beyGamificacionService.beyInfoNivel;
import com.bey.desktop.service.beySonidoService;
import com.bey.desktop.service.beySonidoService.beyEfecto;
import com.bey.desktop.model.beyMetaXp;
import com.bey.desktop.model.beyRetoDiario;
import java.util.List;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beyTemaService;
import com.bey.desktop.util.beyAchievementVFX;
import com.bey.desktop.util.beyDialogos;
import com.bey.desktop.util.beyFormato;
import com.bey.desktop.util.beyMarcaApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class beyMainController implements Initializable {
    private static final String VISTA_DASHBOARD = "/com/bey/desktop/view/beyDashboardView.fxml";
    private static final String VISTA_ENTRENAMIENTO = "/com/bey/desktop/view/beyEntrenamientoView.fxml";
    private static final String VISTA_NUTRICION = "/com/bey/desktop/view/beyNutricionView.fxml";
    private static final String VISTA_MUSICA = "/com/bey/desktop/view/beyMusicaView.fxml";
    private static final String VISTA_PROGRESO = "/com/bey/desktop/view/beyProgresoView.fxml";
    private static final String VISTA_PERFIL = "/com/bey/desktop/view/beyPerfilView.fxml";

    private static final String CLAVE_DASHBOARD = "dashboard";
    private static final String CLAVE_ENTRENAMIENTO = "entrenamiento";
    private static final String CLAVE_NUTRICION = "nutricion";
    private static final String CLAVE_MUSICA = "musica";
    private static final String CLAVE_PROGRESO = "progreso";
    private static final String CLAVE_PERFIL = "perfil";

    @FXML
    private StackPane beyContenedorVistas;
    @FXML
    private StackPane beyLogoMarca;
    @FXML
    private Label beyEtiquetaTituloSeccion;
    @FXML
    private Label beyEtiquetaFechaCabecera;
    @FXML
    private Label beyEtiquetaRutaDatos;
    @FXML
    private ToggleButton beyBotonInicio;
    @FXML
    private ToggleButton beyBotonEntrenamiento;
    @FXML
    private ToggleButton beyBotonNutricion;
    @FXML
    private ToggleButton beyBotonMusica;
    @FXML
    private ToggleButton beyBotonProgreso;
    @FXML
    private ToggleButton beyBotonPerfil;
    @FXML
    private ToggleButton beyBotonTema;
    @FXML
    private Label beyIconoTema;
    @FXML
    private StackPane beyCapaCelebracion;
    @FXML
    private Pane beyCapaConfeti;
    @FXML
    private VBox beyContenedorToasts;
    @FXML
    private VBox beyMenuLateral;
    @FXML
    private VBox beyTarjetaNivelNav;
    @FXML
    private Label beyEtiquetaNivelNav;
    @FXML
    private Label beyEtiquetaTituloNav;
    @FXML
    private Label beyEtiquetaXpNav;
    @FXML
    private ProgressBar beyBarraXpNav;

    private final Map<String, beyVistaCargada> beyVistas = new HashMap<>();

    @Override
    public void initialize(URL ubicacion, ResourceBundle recursos) {
        beyLogoMarca.getChildren().setAll(beyMarcaApp.beyIsotipo(44));

        ToggleGroup grupo = new ToggleGroup();
        beyBotonInicio.setToggleGroup(grupo);
        beyBotonEntrenamiento.setToggleGroup(grupo);
        beyBotonNutricion.setToggleGroup(grupo);
        beyBotonMusica.setToggleGroup(grupo);
        beyBotonProgreso.setToggleGroup(grupo);
        beyBotonPerfil.setToggleGroup(grupo);

        beyEtiquetaFechaCabecera.setText(beyFormato.beyFechaLarga(LocalDate.now()));
        beyEtiquetaRutaDatos.setText("Archivo: " + beyGestorDatos.beyGetInstancia().beyGetRutaDatos().getFileName());
        beySincronizarBotonTema();
        beyConfigurarGamificacion();

        beyMostrar(CLAVE_DASHBOARD);
    }

    private void beyConfigurarGamificacion() {
        beyGamificacionService rpg = beyGestorDatos.beyGetInstancia().beyGetGamificacion();
        beyAchievementVFX.beyRegistrarCapas(beyCapaCelebracion, beyCapaConfeti);
        rpg.beyAgregarEscucha(new beyGamificacionService.beyEscucha() {
            @Override
            public void beyXpGanada(int xp, String motivo) {
                beyAchievementVFX.beyAvisoXp(beyContenedorToasts, xp, motivo);
                beyActualizarNivelNav();

                beyAchievementVFX.beyPulsoGlow(beyTarjetaNivelNav);
                beyAchievementVFX.beyTextoFlotante(beyTarjetaNivelNav, "+" + xp + " XP");
                beySonidoService.beyReproducir(beyEfecto.EXITO);
            }

            @Override
            public void beyMetaCumplida(beyMetaXp meta, int xp) {
                if (meta == beyMetaXp.DIA_PERFECTO) {
                    beyAchievementVFX.beyConfeti();
                    beyAchievementVFX.beyBannerLogro("¡Día perfecto!", "Completaste todas las metas de hoy");
                    beySonidoService.beyReproducir(beyEfecto.LOGRO);
                }
            }

            @Override
            public void beyRetoCompletado(beyRetoDiario reto) {
                beyAchievementVFX.beyBannerLogro("¡Reto completado!", reto.getTitulo());
                beySonidoService.beyReproducir(beyEfecto.LOGRO);
            }

            @Override
            public void beyNivelSubido(beyInfoNivel nivel) {
                beyActualizarNivelNav();
                beyAchievementVFX.beyNivelSubido(beyCapaCelebracion, beyCapaConfeti, nivel,
                        List.of(beyContenedorVistas, beyMenuLateral), null);
            }
        });

        rpg.beyEvaluar();
        beyActualizarNivelNav();
    }

    private void beyActualizarNivelNav() {
        beyInfoNivel info = beyGestorDatos.beyGetInstancia().beyGetGamificacion().beyInfoNivel();
        beyEtiquetaNivelNav.setText("Nv " + info.nivel());
        beyEtiquetaTituloNav.setText(info.titulo());
        beyBarraXpNav.setProgress(info.fraccion());
        beyEtiquetaXpNav.setText(info.xpEnNivel() + " / " + info.xpParaSiguiente() + " XP");
    }

    @FXML
    private void beyAlternarTema() {
        beyTemaApp nuevo = beyGestorDatos.beyGetInstancia().beyAlternarTema();
        beyTemaService.beyAplicarGlobal(nuevo);
        beySincronizarBotonTema();
    }

    private void beySincronizarBotonTema() {
        boolean oscuro = beyGestorDatos.beyGetInstancia().beyGetTema() == beyTemaApp.OSCURO;
        beyBotonTema.setSelected(oscuro);
        beyBotonTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
        if (beyIconoTema != null) {
            beyIconoTema.setText(oscuro ? "\u2600" : "\u263D");
        }
    }

    @FXML
    private void beyMostrarInicio() {
        beyBotonInicio.setSelected(true);
        beyMostrar(CLAVE_DASHBOARD);
    }

    @FXML
    private void beyMostrarEntrenamiento() {
        beyBotonEntrenamiento.setSelected(true);
        beyMostrar(CLAVE_ENTRENAMIENTO);
    }

    @FXML
    private void beyMostrarNutricion() {
        beyBotonNutricion.setSelected(true);
        beyMostrar(CLAVE_NUTRICION);
    }

    @FXML
    private void beyMostrarMusica() {
        beyBotonMusica.setSelected(true);
        beyMostrar(CLAVE_MUSICA);
    }

    @FXML
    private void beyMostrarProgreso() {
        beyBotonProgreso.setSelected(true);
        beyMostrar(CLAVE_PROGRESO);
    }

    @FXML
    private void beyMostrarPerfil() {
        beyBotonPerfil.setSelected(true);
        beyMostrar(CLAVE_PERFIL);
    }

    private void beyReiniciarTrasCambioDePerfil() {
        beyVistas.clear();
        beyContenedorVistas.getChildren().clear();
        beyEtiquetaRutaDatos.setText("Archivo: " + beyGestorDatos.beyGetInstancia().beyGetRutaDatos().getFileName());
        beyGestorDatos.beyGetInstancia().beyGetGamificacion().beyEvaluar();
        beyActualizarNivelNav();
        beyBotonInicio.setSelected(true);
        beyMostrar(CLAVE_DASHBOARD);
    }

    public void beyMostrar(String clave) {
        beyVistaCargada vista = beyVistas.get(clave);
        if (vista == null) {
            vista = beyCargarVista(clave);
            if (vista == null) {
                return;
            }
            beyVistas.put(clave, vista);
        }
        beyContenedorVistas.getChildren().setAll(vista.nodo());
        beyEtiquetaTituloSeccion.setText(vista.titulo());
        if (vista.controlador() instanceof beyVistaConRefresco refresco) {
            refresco.beyRefrescar();
        }
    }

    private beyVistaCargada beyCargarVista(String clave) {
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
            FXMLLoader cargador = new FXMLLoader(beyMainController.class.getResource(fxml));
            Node nodo = cargador.load();
            Object controlador = cargador.getController();
            if (controlador instanceof beyPerfilController perfil) {
                perfil.beySetAlCambiarPerfil(this::beyReiniciarTrasCambioDePerfil);
            }
            return new beyVistaCargada(nodo, controlador, titulo);
        } catch (IOException | RuntimeException e) {
            beyDialogos.beyError("Error al abrir la vista",
                    "No se pudo cargar la pantalla solicitada.\n" + e.getMessage());
            return null;
        }
    }

    private record beyVistaCargada(Node nodo, Object controlador, String titulo) {
    }
}
