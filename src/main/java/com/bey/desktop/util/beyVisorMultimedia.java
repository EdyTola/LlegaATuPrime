package com.bey.desktop.util;

import com.bey.desktop.beyMainApp;
import com.bey.desktop.service.beyArchivoMultimediaService;
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

public final class beyVisorMultimedia {
    private beyVisorMultimedia() {
    }

    public static void beyMostrar(String rutaLocal, String enlace, Window propietario,
                               Consumer<String> alCambiarArchivo) {
        boolean hayArchivo = beyArchivoMultimediaService.beyExiste(rutaLocal);
        boolean hayEnlace = enlace != null && !enlace.isBlank();
        if (!hayArchivo && !hayEnlace) {
            beyDialogos.beyInfo("Sin multimedia",
                    "Este ejercicio todavia no tiene una imagen, video o enlace asociado.\n"
                            + "Usa \"Adjuntar multimedia\" para un archivo local (.png, .jpg, .gif, .mp4) "
                            + "o define un enlace de referencia (por ejemplo, YouTube).");
            return;
        }

        beyContexto contexto = new beyContexto();
        contexto.beyRuta = hayArchivo ? rutaLocal : null;
        contexto.beyEnlace = hayEnlace ? enlace.trim() : null;

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Tecnica del ejercicio");
        dialogo.initOwner(propietario);

        DialogPane panel = dialogo.getDialogPane();
        panel.getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        beyDialogos.beyAplicarEstilo(panel);

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
                File archivo = beyArchivoMultimediaService.beySelectorMultimedia(null).showOpenDialog(propietario);
                if (archivo != null) {
                    contexto.beyRuta = archivo.getAbsolutePath();
                    contexto.beyEnlace = null;
                    beyConstruir(contexto, contenedor, etiquetaArchivo);
                    alCambiarArchivo.accept(contexto.beyRuta);
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

        beyConstruir(contexto, contenedor, etiquetaArchivo);
        dialogo.setOnHidden(e -> contexto.beyLiberar());
        dialogo.showAndWait();
    }

    public static final class beyPanelMultimedia {
        private final beyContexto beyContexto = new beyContexto();
        private final VBox beyRaiz;

        private beyPanelMultimedia(String rutaLocal, String enlace, Window propietario,
                                Consumer<String> alAdjuntar) {
            beyContexto.beyRuta = beyArchivoMultimediaService.beyExiste(rutaLocal) ? rutaLocal : null;
            beyContexto.beyEnlace = enlace != null && !enlace.isBlank() ? enlace.trim() : null;

            Label etiquetaArchivo = new Label();
            etiquetaArchivo.getStyleClass().add("texto-secundario");
            StackPane contenedor = new StackPane();
            contenedor.getStyleClass().add("visor-media");
            contenedor.setMinSize(520, 320);
            VBox.setVgrow(contenedor, Priority.ALWAYS);

            Runnable redibujar = () -> {
                beyConstruir(beyContexto, contenedor, etiquetaArchivo);
                if (contenedor.getChildren().isEmpty()) {
                    etiquetaArchivo.setText("Sin video ni imagen personalizada");
                    contenedor.getChildren().add(beyVacio());
                }
            };

            HBox acciones = new HBox(10);
            acciones.setAlignment(Pos.CENTER_LEFT);
            if (alAdjuntar != null) {
                Button adjuntar = new Button("＋ Adjuntar video o imagen");
                adjuntar.getStyleClass().add("boton-acento");
                adjuntar.setOnAction(e -> {
                    File archivo = beyArchivoMultimediaService.beySelectorMultimedia(null).showOpenDialog(propietario);
                    if (archivo != null) {
                        beyContexto.beyRuta = archivo.getAbsolutePath();
                        beyContexto.beyEnlace = null;
                        redibujar.run();
                        alAdjuntar.accept(beyContexto.beyRuta);
                    }
                });
                acciones.getChildren().add(adjuntar);
            }
            Region hueco = new Region();
            HBox.setHgrow(hueco, Priority.ALWAYS);
            Label ayuda = new Label("Archivos .png .jpg .gif .mp4 o enlaces web");
            ayuda.getStyleClass().add("texto-secundario");
            acciones.getChildren().addAll(hueco, ayuda);

            beyRaiz = new VBox(12, etiquetaArchivo, contenedor, acciones);
            beyRaiz.setPadding(new Insets(10, 4, 4, 4));
            redibujar.run();
        }

        private static Node beyVacio() {
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

        public Node beyGetNodo() {
            return beyRaiz;
        }

        public void beyPausar() {
            if (beyContexto.beyReproductor != null) {
                beyContexto.beyReproductor.pause();
            }
        }

        public void beyReanudar() {
            if (beyContexto.beyReproductor != null) {
                beyContexto.beyReproductor.play();
            }
        }

        public void beyLiberar() {
            beyContexto.beyLiberar();
        }
    }

    public static beyPanelMultimedia beyCrearPanel(String rutaLocal, String enlace, Window propietario,
                                             Consumer<String> alAdjuntar) {
        return new beyPanelMultimedia(rutaLocal, enlace, propietario, alAdjuntar);
    }

    private static void beyConstruir(beyContexto contexto, StackPane contenedor, Label etiquetaArchivo) {
        contexto.beyLiberar();
        contenedor.getChildren().clear();

        if (beyArchivoMultimediaService.beyExiste(contexto.beyRuta)) {
            etiquetaArchivo.setText(beyArchivoMultimediaService.beyNombreArchivo(contexto.beyRuta));
            String uri = Path.of(contexto.beyRuta).toUri().toString();
            if (beyArchivoMultimediaService.beyEsVideo(contexto.beyRuta)) {
                contenedor.getChildren().add(beyConstruirVideo(contexto, uri));
            } else {
                contenedor.getChildren().add(beyConstruirImagen(uri));
            }
            return;
        }

        if (contexto.beyEnlace != null && !contexto.beyEnlace.isBlank()) {
            etiquetaArchivo.setText(beyArchivoMultimediaService.beyTipoEnlace(contexto.beyEnlace));
            contenedor.getChildren().add(beyConstruirEnlace(contexto.beyEnlace));
            return;
        }

        etiquetaArchivo.setText("Sin recurso visual");
    }

    private static Node beyConstruirEnlace(String enlace) {
        Label titulo = new Label(beyArchivoMultimediaService.beyTipoEnlace(enlace));
        titulo.getStyleClass().add("titulo-seccion");

        Label url = new Label(enlace);
        url.getStyleClass().add("texto-secundario");
        url.setWrapText(true);
        url.setMaxWidth(520);

        Button abrir = new Button("Abrir enlace");
        abrir.getStyleClass().add("boton-acento");
        abrir.setOnAction(e -> beyMainApp.beyAbrirEnlace(enlace));

        Label nota = new Label("El enlace se abrira en tu navegador predeterminado.");
        nota.getStyleClass().add("texto-secundario");

        VBox caja = new VBox(10, titulo, url, abrir, nota);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    private static Node beyConstruirVideo(beyContexto contexto, String uri) {
        try {
            Media media = new Media(uri);
            MediaPlayer reproductor = new MediaPlayer(media);
            contexto.beyReproductor = reproductor;

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

            reproductor.setOnError(() -> beyDialogos.beyError("Reproduccion de video",
                    "JavaFX no pudo reproducir este archivo. Se recomienda formato MP4 con video H.264 y audio AAC."));

            HBox controles = new HBox(10, botonAlternar, new Label("Volumen"), volumen);
            controles.setAlignment(Pos.CENTER_LEFT);

            VBox caja = new VBox(10, vista, controles);
            caja.setAlignment(Pos.CENTER);
            reproductor.play();
            return caja;
        } catch (MediaException ex) {
            return beyMensaje("Formato de video no soportado. Usa MP4 (H.264 / AAC).");
        } catch (RuntimeException ex) {
            return beyMensaje("No se pudo abrir el archivo de video seleccionado.");
        }
    }

    private static Node beyConstruirImagen(String uri) {
        try {
            Image imagen = new Image(uri, 760, 0, true, true, false);
            ImageView vista = new ImageView(imagen);
            vista.setPreserveRatio(true);
            vista.setFitWidth(720);
            return vista;
        } catch (RuntimeException ex) {
            return beyMensaje("No se pudo cargar la imagen seleccionada.");
        }
    }

    private static Label beyMensaje(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("texto-secundario");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    private static final class beyContexto {
        private String beyRuta;
        private String beyEnlace;
        private MediaPlayer beyReproductor;

        private void beyLiberar() {
            if (beyReproductor != null) {
                try {
                    beyReproductor.stop();
                    beyReproductor.dispose();
                } catch (RuntimeException ignored) {
                }
                beyReproductor = null;
            }
        }
    }
}
