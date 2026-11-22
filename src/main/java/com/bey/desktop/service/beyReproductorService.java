package com.bey.desktop.service;

import com.bey.desktop.model.beyPistaAudio;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class beyReproductorService {
    public enum beyModoRepeticion { DESACTIVADO, TODAS, UNA }

    private static final beyReproductorService INSTANCIA = new beyReproductorService();

    private final beyGestorDatos beyGestor = beyGestorDatos.beyGetInstancia();

    private final ObjectProperty<beyPistaAudio> beyPistaActual = new SimpleObjectProperty<>();
    private final StringProperty beyTitulo = new SimpleStringProperty("");
    private final StringProperty beyArtista = new SimpleStringProperty("");
    private final BooleanProperty beyReproduciendo = new SimpleBooleanProperty(false);
    private final DoubleProperty beyProgresoSegundos = new SimpleDoubleProperty(0);
    private final DoubleProperty beyDuracionSegundos = new SimpleDoubleProperty(0);
    private final DoubleProperty beyVolumen = new SimpleDoubleProperty(0.8);
    private final BooleanProperty beyAleatorio = new SimpleBooleanProperty(false);
    private final ObjectProperty<beyModoRepeticion> beyModoRepeticionActual =
            new SimpleObjectProperty<>(beyModoRepeticion.DESACTIVADO);

    private MediaPlayer beyReproductor;
    private Consumer<double[]> beyEscuchaEspectro;
    private Consumer<String> beyEscuchaError;
    private boolean beyArrastrandoProgreso;

    private beyReproductorService() {
        double inicial = beyGestor.beyGetDatos().getConfiguracion().getVolumenReproductor();
        beyVolumen.set(Math.max(0, Math.min(1, inicial)));
        beyVolumen.addListener((obs, antes, ahora) -> {
            if (beyReproductor != null) {
                beyReproductor.setVolume(ahora.doubleValue());
            }
            beyGestor.beyGetDatos().getConfiguracion().setVolumenReproductor(ahora.doubleValue());
        });
    }

    public static beyReproductorService beyGetInstancia() {
        return INSTANCIA;
    }

    public ReadOnlyObjectProperty<beyPistaAudio> beyPistaActualProperty() {
        return beyPistaActual;
    }

    public ReadOnlyStringProperty beyTituloProperty() {
        return beyTitulo;
    }

    public ReadOnlyStringProperty beyArtistaProperty() {
        return beyArtista;
    }

    public ReadOnlyBooleanProperty beyReproduciendoProperty() {
        return beyReproduciendo;
    }

    public ReadOnlyDoubleProperty beyProgresoSegundosProperty() {
        return beyProgresoSegundos;
    }

    public ReadOnlyDoubleProperty beyDuracionSegundosProperty() {
        return beyDuracionSegundos;
    }

    public DoubleProperty beyVolumenProperty() {
        return beyVolumen;
    }

    public ReadOnlyBooleanProperty beyAleatorioProperty() {
        return beyAleatorio;
    }

    public ReadOnlyObjectProperty<beyModoRepeticion> beyModoRepeticionProperty() {
        return beyModoRepeticionActual;
    }

    public beyPistaAudio beyGetPistaActual() {
        return beyPistaActual.get();
    }

    public String beyGetTitulo() {
        return beyTitulo.get();
    }

    public String beyGetArtista() {
        return beyArtista.get();
    }

    public double beyGetProgresoSegundos() {
        return beyProgresoSegundos.get();
    }

    public double beyGetDuracionSegundos() {
        return beyDuracionSegundos.get();
    }

    public boolean beyIsActivo() {
        return beyPistaActual.get() != null;
    }

    public boolean beyIsReproduciendo() {
        return beyReproduciendo.get();
    }

    public boolean beyIsAleatorio() {
        return beyAleatorio.get();
    }

    public beyModoRepeticion beyGetModoRepeticion() {
        return beyModoRepeticionActual.get();
    }

    public List<beyPistaAudio> beyLista() {
        return beyGestor.beyGetDatos().getBibliotecaMusical();
    }

    public void beySetEscuchaEspectro(Consumer<double[]> escucha) {
        this.beyEscuchaEspectro = escucha;
    }

    public void beySetEscuchaError(Consumer<String> escucha) {
        this.beyEscuchaError = escucha;
    }

    public void beySetArrastrandoProgreso(boolean arrastrando) {
        this.beyArrastrandoProgreso = arrastrando;
    }

    public boolean beyReproducir(beyPistaAudio pista) {
        if (pista == null || pista.esEnlace()) {
            return false;
        }
        if (!beyArchivoMultimediaService.beyExiste(pista.getRutaArchivo())) {
            return false;
        }
        beyDetenerMedia();
        try {
            Media media = new Media(Path.of(pista.getRutaArchivo()).toUri().toString());
            MediaPlayer reproductorNuevo = new MediaPlayer(media);
            reproductorNuevo.setVolume(beyVolumen.get());

            reproductorNuevo.setAudioSpectrumNumBands(64);
            reproductorNuevo.setAudioSpectrumInterval(0.05);
            reproductorNuevo.setAudioSpectrumThreshold(-60);
            reproductorNuevo.setAudioSpectrumListener((marca, duracion, magnitudes, fases) -> {
                Consumer<double[]> escucha = beyEscuchaEspectro;
                if (escucha == null || magnitudes == null) {
                    return;
                }
                double[] datos = new double[magnitudes.length];
                for (int i = 0; i < magnitudes.length; i++) {
                    datos[i] = magnitudes[i];
                }
                escucha.accept(datos);
            });

            reproductorNuevo.setOnReady(() -> {
                double segundos = media.getDuration().toSeconds();
                beyDuracionSegundos.set(Math.max(0, segundos));
                pista.setDuracionSegundos(segundos);
                beyGestor.beyGuardarAutomatico();
            });
            reproductorNuevo.currentTimeProperty().addListener((obs, antes, ahora) -> {
                if (!beyArrastrandoProgreso) {
                    beyProgresoSegundos.set(ahora.toSeconds());
                }
            });
            reproductorNuevo.statusProperty().addListener((obs, antes, ahora) ->
                    beyReproduciendo.set(ahora == MediaPlayer.Status.PLAYING));
            reproductorNuevo.setOnEndOfMedia(this::beyAvanzarAutomaticamente);
            reproductorNuevo.setOnError(() -> {
                beyReproduciendo.set(false);
                MediaException fallo = reproductorNuevo.getError();
                beyNotificarError(fallo == null ? null : fallo.getMessage());
            });

            beyReproductor = reproductorNuevo;
            beyPistaActual.set(pista);
            beyTitulo.set(pista.getTitulo());
            beyArtista.set(beyArtistaDe(pista));
            beyProgresoSegundos.set(0);
            beyDuracionSegundos.set(pista.getDuracionSegundos());
            reproductorNuevo.play();
            return true;
        } catch (RuntimeException e) {
            beyDetenerMedia();
            beyNotificarError(e.getMessage());
            return false;
        }
    }

    public void beyAlternar() {
        if (beyReproductor == null) {
            beyPistaAudio pista = beyPrimeraPistaReproducible();
            if (pista != null) {
                beyReproducir(pista);
            }
            return;
        }
        if (beyReproductor.getStatus() == MediaPlayer.Status.PLAYING) {
            beyReproductor.pause();
        } else {
            beyReproductor.play();
        }
    }

    public void beySiguiente() {
        List<beyPistaAudio> pistas = beyLista();
        if (pistas.isEmpty()) {
            return;
        }
        beyReproducirDesde(beyIndiceSiguiente(), pistas);
    }

    public void beyAnterior() {
        List<beyPistaAudio> pistas = beyLista();
        if (pistas.isEmpty()) {
            return;
        }
        int indice = beyIndiceDe(beyPistaActual.get());
        int anterior = indice <= 0 ? pistas.size() - 1 : indice - 1;
        beyReproducirDesde(anterior, pistas);
    }

    public void beySeek(double segundos) {
        if (beyReproductor != null) {
            beyReproductor.seek(Duration.seconds(Math.max(0, segundos)));
            beyProgresoSegundos.set(Math.max(0, segundos));
        }
    }

    public void beyDetener() {
        beyDetenerMedia();
        beyPistaActual.set(null);
        beyTitulo.set("");
        beyArtista.set("");
        beyProgresoSegundos.set(0);
        beyDuracionSegundos.set(0);
        beyReproduciendo.set(false);
    }

    public void beyAlternarAleatorio() {
        boolean activar = !beyAleatorio.get();
        if (activar) {
            beyModoRepeticionActual.set(beyModoRepeticion.DESACTIVADO);
        }
        beyAleatorio.set(activar);
    }

    public void beyCiclarRepeticion() {
        beyModoRepeticion actual = beyAleatorio.get() ? beyModoRepeticion.DESACTIVADO : beyModoRepeticionActual.get();
        beyAleatorio.set(false);
        beyModoRepeticionActual.set(switch (actual) {
            case DESACTIVADO -> beyModoRepeticion.TODAS;
            case TODAS -> beyModoRepeticion.UNA;
            case UNA -> beyModoRepeticion.DESACTIVADO;
        });
    }

    public String beyDescripcionModo() {
        if (beyAleatorio.get()) {
            return "Aleatorio continuo";
        }
        return switch (beyModoRepeticionActual.get()) {
            case TODAS -> "Bucle de lista";
            case UNA -> "Repetir pista";
            case DESACTIVADO -> "Normal (se detiene al final)";
        };
    }

    private void beyDetenerMedia() {
        if (beyReproductor != null) {
            try {
                beyReproductor.stop();
                beyReproductor.dispose();
            } catch (RuntimeException ignored) {
            }
            beyReproductor = null;
        }
    }

    private void beyNotificarError(String mensaje) {
        Consumer<String> escucha = beyEscuchaError;
        if (escucha != null) {
            escucha.accept(mensaje == null ? "" : mensaje);
        }
    }

    private beyPistaAudio beyPrimeraPistaReproducible() {
        for (beyPistaAudio pista : beyLista()) {
            if (!pista.esEnlace() && beyArchivoMultimediaService.beyExiste(pista.getRutaArchivo())) {
                return pista;
            }
        }
        return null;
    }

    private void beyAvanzarAutomaticamente() {
        List<beyPistaAudio> pistas = beyLista();
        if (pistas.isEmpty() || beyReproductor == null) {
            return;
        }
        if (beyModoRepeticionActual.get() == beyModoRepeticion.UNA) {
            beyReproductor.seek(Duration.ZERO);
            beyReproductor.play();
            return;
        }
        int indice = beyIndiceDe(beyPistaActual.get());
        if (beyAleatorio.get()) {
            if (pistas.size() == 1) {
                beyReproductor.seek(Duration.ZERO);
                beyReproductor.play();
            } else {
                beyReproducirDesde(beyIndiceSiguiente(), pistas);
            }
            return;
        }
        boolean hayMas = indice >= 0 && indice < pistas.size() - 1;
        if (hayMas) {
            beyReproducirDesde(indice + 1, pistas);
        } else if (beyModoRepeticionActual.get() == beyModoRepeticion.TODAS) {
            beyReproducirDesde(0, pistas);
        } else {
            beyReproduciendo.set(false);
            beyProgresoSegundos.set(beyDuracionSegundos.get());
        }
    }

    private void beyReproducirDesde(int inicio, List<beyPistaAudio> pistas) {
        for (int i = 0; i < pistas.size(); i++) {
            if (beyReproducir(pistas.get((inicio + i) % pistas.size()))) {
                return;
            }
        }
        beyReproduciendo.set(false);
    }

    private int beyIndiceSiguiente() {
        List<beyPistaAudio> pistas = beyLista();
        int indice = beyIndiceDe(beyPistaActual.get());
        if (beyAleatorio.get() && pistas.size() > 1) {
            int azar;
            do {
                azar = (int) (Math.random() * pistas.size());
            } while (azar == indice);
            return azar;
        }
        return indice < 0 ? 0 : (indice + 1) % pistas.size();
    }

    private int beyIndiceDe(beyPistaAudio pista) {
        if (pista == null) {
            return -1;
        }
        List<beyPistaAudio> pistas = beyLista();
        for (int i = 0; i < pistas.size(); i++) {
            if (pistas.get(i).getId().equals(pista.getId())) {
                return i;
            }
        }
        return -1;
    }

    private static String beyArtistaDe(beyPistaAudio pista) {
        if (pista.getArtista() != null && !pista.getArtista().isBlank()) {
            return pista.getArtista();
        }
        return beyArchivoMultimediaService.beyNombreArchivo(pista.getRutaArchivo());
    }
}
