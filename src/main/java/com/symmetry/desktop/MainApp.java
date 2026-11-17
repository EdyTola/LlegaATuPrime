package com.symmetry.desktop;

import com.symmetry.desktop.service.ArchivoMultimediaService;
import com.symmetry.desktop.service.GestorDatos;
import com.symmetry.desktop.service.TemaService;
import com.symmetry.desktop.util.Dialogos;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

    private static HostServices hostServices;

    @Override
    public void start(Stage escenarioPrincipal) {
        hostServices = getHostServices();
        try {
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

            escenarioPrincipal.setTitle("Llega a tu Prime - Entrenamiento, nutricion y habitos saludables");
            escenarioPrincipal.setMinWidth(1120);
            escenarioPrincipal.setMinHeight(720);
            escenarioPrincipal.setScene(escena);
            escenarioPrincipal.show();
        } catch (Exception e) {
            Dialogos.error("Error al iniciar la aplicacion",
                    "No se pudo cargar la interfaz principal.\n" + e.getMessage());
            e.printStackTrace();
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
