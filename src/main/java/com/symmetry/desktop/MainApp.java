package com.symmetry.desktop;

import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.TemaService;
import com.symmetry.desktop.util.Dialogos;
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

/**
 * Punto de entrada de Llega a tu Prime.
 * Aplicacion de escritorio 100% offline con persistencia local en JSON.
 */
public class MainApp extends Application {

    public static final String RUTA_CSS = "/com/symmetry/desktop/css/symmetry.css";
    private static final String RUTA_VISTA_PRINCIPAL = "/com/symmetry/desktop/view/MainView.fxml";
    private static final String RUTA_VISTA_SPLASH = "/com/symmetry/desktop/view/SplashView.fxml";

    /** Recursos raster del isotipo oficial (icono de ventana y de barra de tareas). */
    private static final String RUTA_ICONO = "/com/symmetry/desktop/assets/app-icon-";
    private static final int[] TAMANOS_ICONO = {16, 32, 64, 512};

    /** Tiempo minimo de exhibicion de la pantalla de bienvenida. */
    private static final Duration DURACION_SPLASH = Duration.millis(1100);

    private static HostServices hostServices;

    @Override
    public void start(Stage escenarioPrincipal) {
        hostServices = getHostServices();
        aplicarIcono(escenarioPrincipal);

        Stage splash = crearSplash();
        if (splash != null) {
            splash.show();
        }

        // La ventana principal aparece tras un instante de marca, para que el primer contacto
        // con la aplicacion tenga un arranque claro y motivador.
        PauseTransition espera = new PauseTransition(DURACION_SPLASH);
        espera.setOnFinished(evento -> {
            try {
                mostrarVentanaPrincipal(escenarioPrincipal);
            } catch (Exception e) {
                Dialogos.error("Error al iniciar la aplicacion",
                        "No se pudo cargar la interfaz principal.\n" + e.getMessage());
                e.printStackTrace();
            } finally {
                cerrarSplash(splash);
            }
        });
        espera.play();
    }

    /** Carga los datos locales, monta la ventana principal y la muestra. */
    private void mostrarVentanaPrincipal(Stage escenario) throws Exception {
        GestorDatos gestor = GestorDatos.getInstancia();
        gestor.cargar();

        FXMLLoader cargador = new FXMLLoader(MainApp.class.getResource(RUTA_VISTA_PRINCIPAL));
        Parent raiz = cargador.load();

        Scene escena = new Scene(raiz, 1320, 820);
        URL css = MainApp.class.getResource(RUTA_CSS);
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }

        // Aplica el tema guardado (claro/oscuro) antes de mostrar la ventana.
        TemaService.registrar(escena);
        TemaService.aplicarGlobal(gestor.getTema());

        escenario.setTitle("Llega a tu Prime - Entrenamiento, nutricion y habitos saludables");
        escenario.setMinWidth(1120);
        escenario.setMinHeight(720);
        escenario.setScene(escena);
        escenario.show();
        escenario.toFront();
    }

    /** Crea la pantalla de bienvenida con el isotipo oficial; devuelve {@code null} si falla. */
    private Stage crearSplash() {
        try {
            FXMLLoader cargador = new FXMLLoader(MainApp.class.getResource(RUTA_VISTA_SPLASH));
            Parent raiz = cargador.load();

            Scene escena = new Scene(raiz);
            escena.setFill(Color.TRANSPARENT);
            URL css = MainApp.class.getResource(RUTA_CSS);
            if (css != null) {
                escena.getStylesheets().add(css.toExternalForm());
            }

            Stage splash = new Stage(StageStyle.TRANSPARENT);
            aplicarIcono(splash);
            splash.setScene(escena);
            splash.centerOnScreen();
            return splash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Cierra la pantalla de bienvenida con un fundido suave hacia la ventana principal. */
    private static void cerrarSplash(Stage splash) {
        if (splash == null || splash.getScene() == null) {
            return;
        }
        FadeTransition fundido = new FadeTransition(Duration.millis(320), splash.getScene().getRoot());
        fundido.setToValue(0);
        fundido.setOnFinished(evento -> splash.close());
        fundido.play();
    }

    /** Establece el logotipo oficial como icono de ventana y de barra de tareas de Windows. */
    private static void aplicarIcono(Stage escenario) {
        for (int lado : TAMANOS_ICONO) {
            URL recurso = MainApp.class.getResource(RUTA_ICONO + lado + ".png");
            if (recurso != null) {
                escenario.getIcons().add(new Image(recurso.toExternalForm()));
            }
        }
    }

    @Override
    public void stop() {
        GestorDatos.getInstancia().guardar();
    }

    /**
     * Abre un recurso externo (enlace web o archivo/ruta de referencia) con la aplicacion
     * predeterminada del sistema. Se usa para los enlaces de YouTube y las referencias
     * multimedia de los ejercicios.
     */
    public static void abrirEnlace(String recurso) {
        if (recurso == null || recurso.isBlank()) {
            return;
        }
        String objetivo = recurso.trim();
        if (!ArchivoMultimediaService.esUrl(objetivo)) {
            try {
                objetivo = Path.of(objetivo).toUri().toString();
            } catch (RuntimeException ignored) {
                // Si no es una ruta valida se intenta abrir el texto tal cual.
            }
        }
        if (hostServices != null) {
            try {
                hostServices.showDocument(objetivo);
                return;
            } catch (RuntimeException ignored) {
                // Se intenta el respaldo con java.awt.Desktop.
            }
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(objetivo));
            }
        } catch (Exception ignored) {
            // Sin navegador disponible: no hay mas acciones posibles.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
