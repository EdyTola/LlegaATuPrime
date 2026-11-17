package com.symmetry.desktop.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato de fechas, horas y numeros usado por toda la interfaz.
 */
public final class Formato {

    public static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    public static final Locale LOCALE_TECNICO = Locale.ROOT;

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LOCALE_ES);
    private static final DateTimeFormatter FECHA_CORTA =
            DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_ES);
    private static final DateTimeFormatter FECHA_EJE =
            DateTimeFormatter.ofPattern("dd/MM", LOCALE_ES);
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm", LOCALE_ES);

    private Formato() {
    }

    /** Ejemplo: "Lunes 17 de noviembre de 2026". */
    public static String fechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return capitalizar(FECHA_LARGA.format(fecha));
    }

    /** Ejemplo: "17 nov 2026". */
    public static String fechaCorta(LocalDate fecha) {
        return fecha == null ? "" : FECHA_CORTA.format(fecha);
    }

    /** Etiqueta compacta para ejes de graficos: "17/11". */
    public static String etiquetaEje(LocalDate fecha) {
        return fecha == null ? "" : FECHA_EJE.format(fecha);
    }

    /** Ejemplo: "17 nov 2026 · 18:45". */
    public static String fechaHora(LocalDateTime fechaHora) {
        if (fechaHora == null) {
            return "";
        }
        return fechaCorta(fechaHora.toLocalDate()) + " · " + HORA.format(fechaHora);
    }

    public static String hora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : HORA.format(fechaHora);
    }

    /** Numero sin decimales innecesarios: 62 -> "62", 62.5 -> "62.5". */
    public static String numero(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.05) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(LOCALE_TECNICO, "%.1f", valor);
    }

    /** Cronometro con formato HH:MM:SS. */
    public static String cronometro(Duration duracion) {
        long segundos = duracion == null ? 0 : Math.max(0, duracion.getSeconds());
        return String.format(LOCALE_TECNICO, "%02d:%02d:%02d",
                segundos / 3600, (segundos % 3600) / 60, segundos % 60);
    }

    /** Convierte texto introducido por el usuario en numero (acepta coma o punto). */
    public static double aNumero(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(texto.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase(LOCALE_ES) + texto.substring(1);
    }
}
