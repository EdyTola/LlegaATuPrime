package com.bey.desktop;

import com.bey.desktop.service.beyArchivoMultimediaService;
import com.bey.desktop.service.beyGestorDatos;
import com.bey.desktop.service.beyTemaService;
import com.bey.desktop.util.beyDialogos;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class beyMainApp extends Application {
    public static final String RUTA_CSS = "/com/bey/desktop/css/bey.css";
    private static final String RUTA_VISTA_PRINCIPAL = "/com/bey/desktop/view/beyMainView.fxml";
    private static final String RUTA_VISTA_SPLASH = "/com/bey/desktop/view/beySplashView.fxml";

    private static final String RUTA_ICONO = "/com/bey/desktop/assets/app-icon-";
    private static final int[] TAMANOS_ICONO = {16, 32, 64, 512};

    private static final Duration DURACION_SPLASH = Duration.millis(1100);

    private static HostServices beyHostServices;

    @Override
    public void start(Stage escenarioPrincipal) {
        beyHostServices = getHostServices();
        beyAplicarIcono(escenarioPrincipal);

        Stage splash = beyCrearSplash();
        if (splash != null) {
            splash.show();
        }

        PauseTransition espera = new PauseTransition(DURACION_SPLASH);
        espera.setOnFinished(evento -> {
            try {
                beyMostrarVentanaPrincipal(escenarioPrincipal);
            } catch (Exception e) {
                beyDialogos.beyError("Error al iniciar la aplicacion",
                        "No se pudo cargar la interfaz principal.\n" + e.getMessage());
                e.printStackTrace();
            } finally {
                beyCerrarSplash(splash);
            }
        });
        espera.play();
    }

    private void beyMostrarVentanaPrincipal(Stage escenario) throws Exception {
        beyGestorDatos gestor = beyGestorDatos.beyGetInstancia();
        gestor.beyCargar();

        FXMLLoader cargador = new FXMLLoader(beyMainApp.class.getResource(RUTA_VISTA_PRINCIPAL));
        Parent raiz = cargador.load();

        Scene escena = new Scene(raiz, 1320, 820);
        URL css = beyMainApp.class.getResource(RUTA_CSS);
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }

        beyTemaService.beyRegistrar(escena);
        beyTemaService.beyAplicarGlobal(gestor.beyGetTema());

        escenario.setTitle("Llega a tu Prime - Entrenamiento, nutricion y habitos saludables");
        escenario.setMinWidth(1120);
        escenario.setMinHeight(720);
        escenario.setScene(escena);
        escenario.show();
        escenario.toFront();
    }

    private Stage beyCrearSplash() {
        try {
            FXMLLoader cargador = new FXMLLoader(beyMainApp.class.getResource(RUTA_VISTA_SPLASH));
            Parent raiz = cargador.load();

            Scene escena = new Scene(raiz);
            escena.setFill(Color.TRANSPARENT);
            URL css = beyMainApp.class.getResource(RUTA_CSS);
            if (css != null) {
                escena.getStylesheets().add(css.toExternalForm());
            }

            Stage splash = new Stage(StageStyle.TRANSPARENT);
            beyAplicarIcono(splash);
            splash.setScene(escena);
            splash.centerOnScreen();
            return splash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void beyCerrarSplash(Stage splash) {
        if (splash == null || splash.getScene() == null) {
            return;
        }
        FadeTransition fundido = new FadeTransition(Duration.millis(320), splash.getScene().getRoot());
        fundido.setToValue(0);
        fundido.setOnFinished(evento -> splash.close());
        fundido.play();
    }

    private static void beyAplicarIcono(Stage escenario) {
        for (int lado : TAMANOS_ICONO) {
            URL recurso = beyMainApp.class.getResource(RUTA_ICONO + lado + ".png");
            if (recurso != null) {
                escenario.getIcons().add(new Image(recurso.toExternalForm()));
            }
        }
    }

    @Override
    public void stop() {
        beyGestorDatos.beyGetInstancia().beyGuardar();
    }

    public static void beyAbrirEnlace(String recurso) {
        if (recurso == null || recurso.isBlank()) {
            return;
        }
        String objetivo = recurso.trim();
        if (!beyArchivoMultimediaService.beyEsUrl(objetivo)) {
            try {
                objetivo = Path.of(objetivo).toUri().toString();
            } catch (RuntimeException ignored) {
            }
        }
        if (beyHostServices != null) {
            try {
                beyHostServices.showDocument(objetivo);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(objetivo));
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
