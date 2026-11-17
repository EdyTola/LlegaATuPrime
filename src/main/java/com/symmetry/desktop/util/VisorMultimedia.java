package com.symmetry.desktop.util;

import com.symmetry.desktop.MainApp;
import com.symmetry.desktop.service.ArchivoMultimediaService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Visor de la tecnica de un ejercicio. Muestra imagenes con ImageView, videos con
 * MediaView + MediaPlayer y, cuando el ejercicio solo tiene un enlace de referencia
 * (por ejemplo YouTube), ofrece abrirlo con el navegador del sistema.
 */
public final class VisorMultimedia {

    private VisorMultimedia() {
    }

    public static void mostrar(String rutaLocal, String enlace, Window propietario,
                               Consumer<String> alCambiarArchivo) {
        boolean hayArchivo = ArchivoMultimediaService.existe(rutaLocal);
        boolean hayEnlace = enlace != null && !enlace.isBlank();
        if (!hayArchivo && !hayEnlace) {
            Dialogos.info("Sin multimedia",
                    "Este ejercicio todavia no tiene una imagen, video o enlace asociado.\n"
                            + "Usa \"Adjuntar multimedia\" para un archivo local (.png, .jpg, .gif, .mp4) "
                            + "o define un enlace de referencia (por ejemplo, YouTube).");
            return;
        }

        Contexto contexto = new Contexto();
        contexto.ruta = hayArchivo ? rutaLocal : null;
        contexto.enlace = hayEnlace ? enlace.trim() : null;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Tecnica del ejercicio");
        dialogo.initOwner(propietario);

        DialogPane panel = dialogo.getDialogPane();
        panel.getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        Dialogos.aplicarEstilo(panel);

        Label etiquetaArchivo = new Label();
        etiquetaArchivo.getStyleClass().add("texto-secundario");

        StackPane contenedor = new StackPane();
        contenedor.getStyleClass().add("visor-media");
        contenedor.setMinSize(620, 360);

        HBox acciones = new HBox(10);
        acciones.setAlignment(Pos.CENTER_LEFT);
        if (alCambiarArchivo != null) {
            Button botonCambiar = new Button("Usar archivo local...");
            botonCambiar.getStyleClass().add("boton-secundario");
            botonCambiar.setOnAction(e -> {
                File archivo = ArchivoMultimediaService.selectorMultimedia(null).showOpenDialog(propietario);
                if (archivo != null) {
                    contexto.ruta = archivo.getAbsolutePath();
                    contexto.enlace = null;
                    construir(contexto, contenedor, etiquetaArchivo);
                    alCambiarArchivo.accept(contexto.ruta);
                }
            });
            acciones.getChildren().add(botonCambiar);
        }
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label etiquetaAyuda = new Label("Archivos .png .jpg .gif .mp4 o enlaces web");
        etiquetaAyuda.getStyleClass().add("texto-secundario");
        acciones.getChildren().addAll(hueco, etiquetaAyuda);

        VBox contenido = new VBox(12, etiquetaArchivo, contenedor, acciones);
        contenido.setPadding(new Insets(6, 4, 4, 4));
        panel.setContent(contenido);

        construir(contexto, contenedor, etiquetaArchivo);
        dialogo.setOnHidden(e -> contexto.liberar());
        dialogo.showAndWait();
    }

    /**
     * Panel de multimedia personalizada para incrustar en otras ventanas (por ejemplo, la pestana
     * del visor de tecnica): muestra el archivo o enlace del usuario y siempre ofrece el boton
     * para adjuntar un video o una imagen.
     */
    public static final class PanelMultimedia {

        private final Contexto contexto = new Contexto();
        private final VBox raiz;

        private PanelMultimedia(String rutaLocal, String enlace, Window propietario,
                                Consumer<String> alAdjuntar) {
            contexto.ruta = ArchivoMultimediaService.existe(rutaLocal) ? rutaLocal : null;
            contexto.enlace = enlace != null && !enlace.isBlank() ? enlace.trim() : null;

            Label etiquetaArchivo = new Label();
            etiquetaArchivo.getStyleClass().add("texto-secundario");
            StackPane contenedor = new StackPane();
            contenedor.getStyleClass().add("visor-media");
            contenedor.setMinSize(520, 320);
            VBox.setVgrow(contenedor, Priority.ALWAYS);

            Runnable redibujar = () -> {
                construir(contexto, contenedor, etiquetaArchivo);
                if (contenedor.getChildren().isEmpty()) {
                    etiquetaArchivo.setText("Sin video ni imagen personalizada");
                    contenedor.getChildren().add(vacio());
                }
            };

            HBox acciones = new HBox(10);
            acciones.setAlignment(Pos.CENTER_LEFT);
            if (alAdjuntar != null) {
                Button adjuntar = new Button("＋ Adjuntar video o imagen");
                adjuntar.getStyleClass().add("boton-acento");
                adjuntar.setOnAction(e -> {
                    File archivo = ArchivoMultimediaService.selectorMultimedia(null).showOpenDialog(propietario);
                    if (archivo != null) {
                        contexto.ruta = archivo.getAbsolutePath();
                        contexto.enlace = null;
                        redibujar.run();
                        alAdjuntar.accept(contexto.ruta);
                    }
                });
                acciones.getChildren().add(adjuntar);
            }
            Region hueco = new Region();
            HBox.setHgrow(hueco, Priority.ALWAYS);
            Label ayuda = new Label("Archivos .png .jpg .gif .mp4 o enlaces web");
            ayuda.getStyleClass().add("texto-secundario");
            acciones.getChildren().addAll(hueco, ayuda);

            raiz = new VBox(12, etiquetaArchivo, contenedor, acciones);
            raiz.setPadding(new Insets(10, 4, 4, 4));
            redibujar.run();
        }

        private static Node vacio() {
            Label titulo = new Label("Aún no has adjuntado un video o una imagen");
            titulo.getStyleClass().add("titulo-seccion");
            Label detalle = new Label("Sube tu propia referencia (.mp4, .gif, .png, .jpg) con el botón "
                    + "\"Adjuntar video o imagen\". Mientras tanto, el modelo 2D animado te sirve de guía.");
            detalle.getStyleClass().add("texto-secundario");
            detalle.setWrapText(true);
            detalle.setMaxWidth(440);
            VBox caja = new VBox(8, titulo, detalle);
            caja.setAlignment(Pos.CENTER);
            return caja;
        }

        public Node getNodo() {
            return raiz;
        }

        /** Pausa el video si hay uno en reproduccion (al cambiar de pestana). */
        public void pausar() {
            if (contexto.reproductor != null) {
                contexto.reproductor.pause();
            }
        }

        public void reanudar() {
            if (contexto.reproductor != null) {
                contexto.reproductor.play();
            }
        }

        public void liberar() {
            contexto.liberar();
        }
    }

    public static PanelMultimedia crearPanel(String rutaLocal, String enlace, Window propietario,
                                             Consumer<String> alAdjuntar) {
        return new PanelMultimedia(rutaLocal, enlace, propietario, alAdjuntar);
    }

    private static void construir(Contexto contexto, StackPane contenedor, Label etiquetaArchivo) {
        contexto.liberar();
        contenedor.getChildren().clear();

        if (ArchivoMultimediaService.existe(contexto.ruta)) {
            etiquetaArchivo.setText(ArchivoMultimediaService.nombreArchivo(contexto.ruta));
            String uri = Path.of(contexto.ruta).toUri().toString();
            if (ArchivoMultimediaService.esVideo(contexto.ruta)) {
                contenedor.getChildren().add(construirVideo(contexto, uri));
            } else {
                contenedor.getChildren().add(construirImagen(uri));
            }
            return;
        }

        if (contexto.enlace != null && !contexto.enlace.isBlank()) {
            etiquetaArchivo.setText(ArchivoMultimediaService.tipoEnlace(contexto.enlace));
            contenedor.getChildren().add(construirEnlace(contexto.enlace));
            return;
        }

        etiquetaArchivo.setText("Sin recurso visual");
    }

    private static Node construirEnlace(String enlace) {
        Label titulo = new Label(ArchivoMultimediaService.tipoEnlace(enlace));
        titulo.getStyleClass().add("titulo-seccion");

        Label url = new Label(enlace);
        url.getStyleClass().add("texto-secundario");
        url.setWrapText(true);
        url.setMaxWidth(520);

        Button abrir = new Button("Abrir enlace");
        abrir.getStyleClass().add("boton-acento");
        abrir.setOnAction(e -> MainApp.abrirEnlace(enlace));

        Label nota = new Label("El enlace se abrira en tu navegador predeterminado.");
        nota.getStyleClass().add("texto-secundario");

        VBox caja = new VBox(10, titulo, url, abrir, nota);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    private static Node construirVideo(Contexto contexto, String uri) {
        try {
            Media media = new Media(uri);
            MediaPlayer reproductor = new MediaPlayer(media);
            contexto.reproductor = reproductor;

            MediaView vista = new MediaView(reproductor);
            vista.setFitWidth(720);
            vista.setPreserveRatio(true);

            Button botonAlternar = new Button("Pausar");
            botonAlternar.getStyleClass().add("boton-secundario");
            botonAlternar.setOnAction(e -> {
                if (reproductor.getStatus() == MediaPlayer.Status.PLAYING) {
                    reproductor.pause();
                    botonAlternar.setText("Reproducir");
                } else {
                    reproductor.play();
                    botonAlternar.setText("Pausar");
                }
            });

            Slider volumen = new Slider(0, 1, 0.8);
            volumen.setPrefWidth(160);
            reproductor.volumeProperty().bind(volumen.valueProperty());

            reproductor.setOnError(() -> Dialogos.error("Reproduccion de video",
                    "JavaFX no pudo reproducir este archivo. Se recomienda formato MP4 con video H.264 y audio AAC."));

            HBox controles = new HBox(10, botonAlternar, new Label("Volumen"), volumen);
            controles.setAlignment(Pos.CENTER_LEFT);

            VBox caja = new VBox(10, vista, controles);
            caja.setAlignment(Pos.CENTER);
            reproductor.play();
            return caja;
        } catch (MediaException ex) {
            return mensaje("Formato de video no soportado. Usa MP4 (H.264 / AAC).");
        } catch (RuntimeException ex) {
            return mensaje("No se pudo abrir el archivo de video seleccionado.");
        }
    }

    private static Node construirImagen(String uri) {
        try {
            Image imagen = new Image(uri, 760, 0, true, true, false);
            ImageView vista = new ImageView(imagen);
            vista.setPreserveRatio(true);
            vista.setFitWidth(720);
            return vista;
        } catch (RuntimeException ex) {
            return mensaje("No se pudo cargar la imagen seleccionada.");
        }
    }

    private static Label mensaje(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("texto-secundario");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    /** Mantiene la referencia al reproductor activo para liberarlo correctamente. */
    private static final class Contexto {
        private String ruta;
        private String enlace;
        private MediaPlayer reproductor;

        private void liberar() {
            if (reproductor != null) {
                try {
                    reproductor.stop();
                    reproductor.dispose();
                } catch (RuntimeException ignored) {
                    // La liberacion de recursos falla de forma silenciosa.
                }
                reproductor = null;
            }
        }
    }
}
