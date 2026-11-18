package com.symmetry.desktop.service;

import com.symmetry.desktop.model.PistaAudio;
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

/**
 * Reproductor de audio global de la aplicacion. Mantiene un unico {@link MediaPlayer}
 * compartido por el modulo de musica y por el mini-reproductor persistente, de modo que la
 * musica sigue sonando (o en pausa) aunque el usuario cambie de seccion.
 *
 * <p>El estado se expone mediante propiedades observables para que cualquier vista pueda
 * enlazarse (binding) sin depender del ciclo de vida de las demas pantallas.</p>
 */
public final class ReproductorService {

    /** Modos de repeticion, recorridos en ciclo al pulsar el boton correspondiente. */
    public enum ModoRepeticion { DESACTIVADO, TODAS, UNA }

    private static final ReproductorService INSTANCIA = new ReproductorService();

    private final GestorDatos gestor = GestorDatos.getInstancia();

    private final ObjectProperty<PistaAudio> pistaActual = new SimpleObjectProperty<>();
    private final StringProperty titulo = new SimpleStringProperty("");
    private final StringProperty artista = new SimpleStringProperty("");
    private final BooleanProperty reproduciendo = new SimpleBooleanProperty(false);
    private final DoubleProperty progresoSegundos = new SimpleDoubleProperty(0);
    private final DoubleProperty duracionSegundos = new SimpleDoubleProperty(0);
    private final DoubleProperty volumen = new SimpleDoubleProperty(0.8);
    private final BooleanProperty aleatorio = new SimpleBooleanProperty(false);
    private final ObjectProperty<ModoRepeticion> modoRepeticion =
            new SimpleObjectProperty<>(ModoRepeticion.DESACTIVADO);

    private MediaPlayer reproductor;
    private Consumer<double[]> escuchaEspectro;
    private Consumer<String> escuchaError;
    private boolean arrastrandoProgreso;

    private ReproductorService() {
        double inicial = gestor.getDatos().getConfiguracion().getVolumenReproductor();
        volumen.set(Math.max(0, Math.min(1, inicial)));
        volumen.addListener((obs, antes, ahora) -> {
            if (reproductor != null) {
                reproductor.setVolume(ahora.doubleValue());
            }
            gestor.getDatos().getConfiguracion().setVolumenReproductor(ahora.doubleValue());
        });
    }

    public static ReproductorService getInstancia() {
        return INSTANCIA;
    }

    // ------------------------------------------------------------------
    // Estado observable
    // ------------------------------------------------------------------

    public ReadOnlyObjectProperty<PistaAudio> pistaActualProperty() {
        return pistaActual;
    }

    public ReadOnlyStringProperty tituloProperty() {
        return titulo;
    }

    public ReadOnlyStringProperty artistaProperty() {
        return artista;
    }

    public ReadOnlyBooleanProperty reproduciendoProperty() {
        return reproduciendo;
    }

    public ReadOnlyDoubleProperty progresoSegundosProperty() {
        return progresoSegundos;
    }

    public ReadOnlyDoubleProperty duracionSegundosProperty() {
        return duracionSegundos;
    }

    public DoubleProperty volumenProperty() {
        return volumen;
    }

    public ReadOnlyBooleanProperty aleatorioProperty() {
        return aleatorio;
    }

    public ReadOnlyObjectProperty<ModoRepeticion> modoRepeticionProperty() {
        return modoRepeticion;
    }

    public PistaAudio getPistaActual() {
        return pistaActual.get();
    }

    public String getTitulo() {
        return titulo.get();
    }

    public String getArtista() {
        return artista.get();
    }

    public double getProgresoSegundos() {
        return progresoSegundos.get();
    }

    public double getDuracionSegundos() {
        return duracionSegundos.get();
    }

    public boolean isActivo() {
        return pistaActual.get() != null;
    }

    public boolean isReproduciendo() {
        return reproduciendo.get();
    }

    public boolean isAleatorio() {
        return aleatorio.get();
    }

    public ModoRepeticion getModoRepeticion() {
        return modoRepeticion.get();
    }

    /** Lista de reproduccion viva: la biblioteca musical del usuario en memoria. */
    public List<PistaAudio> lista() {
        return gestor.getDatos().getBibliotecaMusical();
    }

    /** Registra el consumidor que recibe las magnitudes del espectro de la pista en curso. */
    public void setEscuchaEspectro(Consumer<double[]> escucha) {
        this.escuchaEspectro = escucha;
    }

    /** Registra el consumidor avisado cuando JavaFX no puede reproducir un archivo. */
    public void setEscuchaError(Consumer<String> escucha) {
        this.escuchaError = escucha;
    }

    /** Marca que el usuario esta arrastrando el deslizador: no se sobrescribe su posicion. */
    public void setArrastrandoProgreso(boolean arrastrando) {
        this.arrastrandoProgreso = arrastrando;
    }

    // ------------------------------------------------------------------
    // Control de reproduccion
    // ------------------------------------------------------------------

    /**
     * Carga y reproduce una pista local. Devuelve {@code false} si la pista es un enlace o
     * si el archivo ya no esta disponible en el disco.
     */
    public boolean reproducir(PistaAudio pista) {
        if (pista == null || pista.esEnlace()) {
            return false;
        }
        if (!ArchivoMultimediaService.existe(pista.getRutaArchivo())) {
            return false;
        }
        detenerMedia();
        try {
            Media media = new Media(Path.of(pista.getRutaArchivo()).toUri().toString());
            MediaPlayer reproductorNuevo = new MediaPlayer(media);
            reproductorNuevo.setVolume(volumen.get());

            // El espectro reacciona al audio real del archivo local.
            reproductorNuevo.setAudioSpectrumNumBands(64);
            reproductorNuevo.setAudioSpectrumInterval(0.05);
            reproductorNuevo.setAudioSpectrumThreshold(-60);
            reproductorNuevo.setAudioSpectrumListener((marca, duracion, magnitudes, fases) -> {
                Consumer<double[]> escucha = escuchaEspectro;
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
                duracionSegundos.set(Math.max(0, segundos));
                pista.setDuracionSegundos(segundos);
                gestor.guardarAutomatico();
            });
            reproductorNuevo.currentTimeProperty().addListener((obs, antes, ahora) -> {
                if (!arrastrandoProgreso) {
                    progresoSegundos.set(ahora.toSeconds());
                }
            });
            reproductorNuevo.statusProperty().addListener((obs, antes, ahora) ->
                    reproduciendo.set(ahora == MediaPlayer.Status.PLAYING));
            reproductorNuevo.setOnEndOfMedia(this::avanzarAutomaticamente);
            reproductorNuevo.setOnError(() -> {
                reproduciendo.set(false);
                MediaException fallo = reproductorNuevo.getError();
                notificarError(fallo == null ? null : fallo.getMessage());
            });

            reproductor = reproductorNuevo;
            pistaActual.set(pista);
            titulo.set(pista.getTitulo());
            artista.set(artistaDe(pista));
            progresoSegundos.set(0);
            duracionSegundos.set(pista.getDuracionSegundos());
            reproductorNuevo.play();
            return true;
        } catch (RuntimeException e) {
            detenerMedia();
            notificarError(e.getMessage());
            return false;
        }
    }

    /** Alterna entre reproducir y pausar; si no hay pista cargada, inicia la primera valida. */
    public void alternar() {
        if (reproductor == null) {
            PistaAudio pista = primeraPistaReproducible();
            if (pista != null) {
                reproducir(pista);
            }
            return;
        }
        if (reproductor.getStatus() == MediaPlayer.Status.PLAYING) {
            reproductor.pause();
        } else {
            reproductor.play();
        }
    }

    public void siguiente() {
        List<PistaAudio> pistas = lista();
        if (pistas.isEmpty()) {
            return;
        }
        reproducir(pistas.get(indiceSiguiente()));
    }

    public void anterior() {
        List<PistaAudio> pistas = lista();
        if (pistas.isEmpty()) {
            return;
        }
        int indice = indiceDe(pistaActual.get());
        int anterior = indice <= 0 ? pistas.size() - 1 : indice - 1;
        reproducir(pistas.get(anterior));
    }

    /** Desplaza la reproduccion a un segundo concreto de la pista actual. */
    public void seek(double segundos) {
        if (reproductor != null) {
            reproductor.seek(Duration.seconds(Math.max(0, segundos)));
            progresoSegundos.set(Math.max(0, segundos));
        }
    }

    /** Detiene la reproduccion y libera el reproductor, dejando la biblioteca intacta. */
    public void detener() {
        detenerMedia();
        pistaActual.set(null);
        titulo.set("");
        artista.set("");
        progresoSegundos.set(0);
        duracionSegundos.set(0);
        reproduciendo.set(false);
    }

    /** Activa o desactiva el modo aleatorio. */
    public void alternarAleatorio() {
        aleatorio.set(!aleatorio.get());
    }

    /** Recorre los modos de repeticion: desactivado, toda la lista y esta pista. */
    public void ciclarRepeticion() {
        modoRepeticion.set(switch (modoRepeticion.get()) {
            case DESACTIVADO -> ModoRepeticion.TODAS;
            case TODAS -> ModoRepeticion.UNA;
            case UNA -> ModoRepeticion.DESACTIVADO;
        });
    }

    // ------------------------------------------------------------------
    // Logica interna
    // ------------------------------------------------------------------

    private void detenerMedia() {
        if (reproductor != null) {
            try {
                reproductor.stop();
                reproductor.dispose();
            } catch (RuntimeException ignored) {
                // Liberacion silenciosa de recursos de audio.
            }
            reproductor = null;
        }
    }

    /** Avisa al interesado (por ejemplo, el modulo de musica) del fallo de reproduccion. */
    private void notificarError(String mensaje) {
        Consumer<String> escucha = escuchaError;
        if (escucha != null) {
            escucha.accept(mensaje == null ? "" : mensaje);
        }
    }

    private PistaAudio primeraPistaReproducible() {
        for (PistaAudio pista : lista()) {
            if (!pista.esEnlace() && ArchivoMultimediaService.existe(pista.getRutaArchivo())) {
                return pista;
            }
        }
        return null;
    }

    /** Avance automatico al terminar una pista, segun el modo de repeticion activo. */
    private void avanzarAutomaticamente() {
        if (modoRepeticion.get() == ModoRepeticion.UNA && reproductor != null) {
            reproductor.seek(Duration.ZERO);
            reproductor.play();
            return;
        }
        List<PistaAudio> pistas = lista();
        if (pistas.isEmpty()) {
            return;
        }
        int indice = indiceDe(pistaActual.get());
        if (aleatorio.get() && pistas.size() > 1) {
            reproducir(pistas.get(indiceSiguiente()));
            return;
        }
        if (indice >= 0 && indice < pistas.size() - 1) {
            reproducir(pistas.get(indice + 1));
        } else if (modoRepeticion.get() == ModoRepeticion.TODAS) {
            reproducir(pistas.get(0));
        } else {
            reproduciendo.set(false);
            progresoSegundos.set(duracionSegundos.get());
        }
    }

    /** Indice de la siguiente pista respetando el modo aleatorio. */
    private int indiceSiguiente() {
        List<PistaAudio> pistas = lista();
        int indice = indiceDe(pistaActual.get());
        if (aleatorio.get() && pistas.size() > 1) {
            int azar;
            do {
                azar = (int) (Math.random() * pistas.size());
            } while (azar == indice);
            return azar;
        }
        return indice < 0 ? 0 : (indice + 1) % pistas.size();
    }

    private int indiceDe(PistaAudio pista) {
        if (pista == null) {
            return -1;
        }
        List<PistaAudio> pistas = lista();
        for (int i = 0; i < pistas.size(); i++) {
            if (pistas.get(i).getId().equals(pista.getId())) {
                return i;
            }
        }
        return -1;
    }

    private static String artistaDe(PistaAudio pista) {
        if (pista.getArtista() != null && !pista.getArtista().isBlank()) {
            return pista.getArtista();
        }
        return ArchivoMultimediaService.nombreArchivo(pista.getRutaArchivo());
    }
}
