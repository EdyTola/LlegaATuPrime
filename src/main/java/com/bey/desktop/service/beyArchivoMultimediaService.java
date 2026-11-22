package com.bey.desktop.service;

import com.bey.desktop.model.beyPistaAudio;
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

public final class beyArchivoMultimediaService {
    public static final List<String> EXTENSIONES_IMAGEN = List.of("png", "jpg", "jpeg", "gif", "bmp");
    public static final List<String> EXTENSIONES_VIDEO = List.of("mp4", "m4v", "mov");
    public static final List<String> EXTENSIONES_AUDIO = List.of("mp3", "wav", "m4a", "aac");

    private static final Map<String, Image> CACHE_PORTADAS = new HashMap<>();

    private beyArchivoMultimediaService() {
    }

    public static boolean beyExiste(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(ruta));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static String beyExtension(String ruta) {
        if (ruta == null) {
            return "";
        }
        int punto = ruta.lastIndexOf('.');
        return punto < 0 ? "" : ruta.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean beyEsImagen(String ruta) {
        return EXTENSIONES_IMAGEN.contains(beyExtension(ruta));
    }

    public static boolean beyEsVideo(String ruta) {
        return EXTENSIONES_VIDEO.contains(beyExtension(ruta));
    }

    public static boolean beyEsAudio(String ruta) {
        return EXTENSIONES_AUDIO.contains(beyExtension(ruta));
    }

    public static String beyNombreArchivo(String ruta) {
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

    public static FileChooser beySelectorMultimedia(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar imagen o video de la tecnica");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes y videos", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.mp4", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        beyAplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    public static FileChooser beySelectorImagen(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar foto de progreso");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        beyAplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    public static FileChooser beySelectorAudio(String carpetaInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar archivos de audio");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio (MP3 / WAV)", "*.mp3", "*.wav", "*.m4a", "*.aac"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        beyAplicarCarpetaInicial(selector, carpetaInicial);
        return selector;
    }

    public static DirectoryChooser beySelectorCarpeta(String carpetaInicial, String titulo) {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle(titulo);
        File carpeta = beyCarpetaValida(carpetaInicial);
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }
        return selector;
    }

    public static List<File> beyBuscarArchivosAudio(File carpeta) {
        if (carpeta == null || !carpeta.isDirectory()) {
            return List.of();
        }
        File[] archivos = carpeta.listFiles(f -> f.isFile() && beyEsAudio(f.getName()));
        if (archivos == null) {
            return List.of();
        }
        List<File> lista = new ArrayList<>(Arrays.asList(archivos));
        lista.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return lista;
    }

    private static void beyAplicarCarpetaInicial(FileChooser selector, String carpetaInicial) {
        File carpeta = beyCarpetaValida(carpetaInicial);
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }
    }

    private static File beyCarpetaValida(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        File carpeta = new File(ruta);
        return carpeta.isDirectory() ? carpeta : null;
    }

    public static String beyExtensionesAudioTexto() {
        return EXTENSIONES_AUDIO.stream().map(e -> "." + e).collect(Collectors.joining(", "));
    }

    public static boolean beyEsUrl(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String texto = valor.trim().toLowerCase(Locale.ROOT);
        return texto.startsWith("http://") || texto.startsWith("https://");
    }

    public static boolean beyEsYoutube(String url) {
        if (!beyEsUrl(url)) {
            return false;
        }
        String texto = url.trim().toLowerCase(Locale.ROOT);
        return texto.contains("youtube.com") || texto.contains("youtu.be");
    }

    public static String beyTipoEnlace(String url) {
        return beyEsYoutube(url) ? "YouTube" : (beyEsUrl(url) ? "Enlace web" : "Referencia");
    }

    public static String beyIdYoutube(String url) {
        if (!beyEsYoutube(url)) {
            return null;
        }
        try {
            String texto = url.trim();
            for (String marca : new String[]{"youtu.be/", "v=", "/embed/", "/shorts/", "/live/"}) {
                int indice = texto.indexOf(marca);
                if (indice >= 0) {
                    String id = beyRecortarId(texto.substring(indice + marca.length()));
                    if (id != null) {
                        return id;
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    public static String beyMiniaturaYoutube(String url) {
        String id = beyIdYoutube(url);
        return id == null ? null : "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
    }

    public static Image beyPortada(beyPistaAudio pista) {
        if (pista == null || !pista.esEnlace()) {
            return null;
        }
        String url = beyMiniaturaYoutube(pista.getEnlace());
        if (url == null) {
            return null;
        }
        return CACHE_PORTADAS.computeIfAbsent(url, clave -> new Image(clave, 0, 0, true, true, true));
    }

    private static String beyRecortarId(String valor) {
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
