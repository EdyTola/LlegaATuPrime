package com.symmetry.desktop.service;

import com.symmetry.desktop.model.PistaAudio;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion de archivos multimedia locales: validacion de formatos y selectores
 * de archivos/carpetas usados por los modulos de entrenamiento y musica.
 */
public final class ArchivoMultimediaService {

    public static final List<String> EXTENSIONES_IMAGEN = List.of("png", "jpg", "jpeg", "gif", "bmp");
    public static final List<String> EXTENSIONES_VIDEO = List.of("mp4", "m4v", "mov");
    public static final List<String> EXTENSIONES_AUDIO = List.of("mp3", "wav", "m4a", "aac");

    /** Cache de miniaturas descargadas, compartida por el reproductor y el mini-reproductor. */
    private static final Map<String, Image> CACHE_PORTADAS = new HashMap<>();

    private ArchivoMultimediaService() {
    }

    public static boolean existe(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(ruta));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String extension(String ruta) {
        if (ruta == null) {
            return "";
        }
        int punto = ruta.lastIndexOf('.');
        return punto < 0 ? "" : ruta.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean esImagen(String ruta) {
        return EXTENSIONES_IMAGEN.contains(extension(ruta));
    }

    public static boolean esVideo(String ruta) {
        return EXTENSIONES_VIDEO.contains(extension(ruta));
    }

    public static boolean esAudio(String ruta) {
        return EXTENSIONES_AUDIO.contains(extension(ruta));
    }

    public static String nombreArchivo(String ruta) {
        if (ruta == null) {
            return "";
        }
        try {
            Path nombre = Path.of(ruta).getFileName();
            return nombre == null ? ruta : nombre.toString();
        } catch (RuntimeException e) {
            return ruta;
        }
    }

    /** Selector de archivos de imagen y video para la tecnica de los ejercicios. */
    public static FileChooser selectorMultimedia(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar imagen o video de la tecnica");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes y videos", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.mp4", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        aplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    /** Selector de archivos de imagen para las fotos de progreso. */
    public static FileChooser selectorImagen(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar foto de progreso");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        aplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    /** Selector de archivos de audio locales. */
    public static FileChooser selectorAudio(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar archivos de audio");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio (MP3 / WAV)", "*.mp3", "*.wav", "*.m4a", "*.aac"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        aplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    public static DirectoryChooser selectorCarpeta(String carpetaInicial, String titulo) {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle(titulo);
        File carpeta = carpetaValida(carpetaInicial);
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }
        return selector;
    }

    /** Lista ordenada de archivos de audio contenidos en una carpeta. */
    public static List<File> buscarArchivosAudio(File carpeta) {
        if (carpeta == null || !carpeta.isDirectory()) {
            return List.of();
        }
        File[] archivos = carpeta.listFiles(f -> f.isFile() && esAudio(f.getName()));
        if (archivos == null) {
            return List.of();
        }
        List<File> lista = new ArrayList<>(Arrays.asList(archivos));
        lista.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return lista;
    }

    private static void aplicarCarpetaInicial(FileChooser selector, String carpetaInicial) {
        File carpeta = carpetaValida(carpetaInicial);
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }
    }

    private static File carpetaValida(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        File carpeta = new File(ruta);
        return carpeta.isDirectory() ? carpeta : null;
    }

    /** Extensiones aceptadas mostradas al usuario. */
    public static String extensionesAudioTexto() {
        return EXTENSIONES_AUDIO.stream().map(e -> "." + e).collect(Collectors.joining(", "));
    }

    /** true cuando el texto es una URL http(s) valida. */
    public static boolean esUrl(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String texto = valor.trim().toLowerCase(Locale.ROOT);
        return texto.startsWith("http://") || texto.startsWith("https://");
    }

    /** true cuando la URL pertenece a YouTube (youtube.com o youtu.be). */
    public static boolean esYoutube(String url) {
        if (!esUrl(url)) {
            return false;
        }
        String texto = url.trim().toLowerCase(Locale.ROOT);
        return texto.contains("youtube.com") || texto.contains("youtu.be");
    }

    /** Descripcion del recurso externo, indicando si es un enlace de YouTube. */
    public static String tipoEnlace(String url) {
        return esYoutube(url) ? "YouTube" : (esUrl(url) ? "Enlace web" : "Referencia");
    }

    /** Identificador del video cuando la URL es de YouTube (watch, youtu.be, embed o shorts). */
    public static String idYoutube(String url) {
        if (!esYoutube(url)) {
            return null;
        }
        try {
            String texto = url.trim();
            for (String marca : new String[]{"youtu.be/", "v=", "/embed/", "/shorts/", "/live/"}) {
                int indice = texto.indexOf(marca);
                if (indice >= 0) {
                    String id = recortarId(texto.substring(indice + marca.length()));
                    if (id != null) {
                        return id;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // URL con formato inesperado: se trata como no reconocida.
        }
        return null;
    }

    /**
     * URL de la miniatura de un video de YouTube, o {@code null} si el enlace no permite
     * obtener el identificador. La imagen puede no estar disponible sin conexion.
     */
    public static String miniaturaYoutube(String url) {
        String id = idYoutube(url);
        return id == null ? null : "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
    }

    /**
     * Miniatura de la pista cuando es un enlace de YouTube, o {@code null} si no aplica o el
     * enlace no tiene identificador. La descarga se hace en segundo plano y se cachea; sin
     * conexion simplemente no hay imagen y la vista muestra su icono de respaldo.
     */
    public static Image portada(PistaAudio pista) {
        if (pista == null || !pista.esEnlace()) {
            return null;
        }
        String url = miniaturaYoutube(pista.getEnlace());
        if (url == null) {
            return null;
        }
        return CACHE_PORTADAS.computeIfAbsent(url, clave -> new Image(clave, 0, 0, true, true, true));
    }

    private static String recortarId(String valor) {
        int corte = valor.length();
        for (char c : new char[]{'&', '?', '/', '#', ' '}) {
            int posicion = valor.indexOf(c);
            if (posicion >= 0 && posicion < corte) {
                corte = posicion;
            }
        }
        String id = valor.substring(0, corte).trim();
        return id.isEmpty() ? null : id;
    }
}
