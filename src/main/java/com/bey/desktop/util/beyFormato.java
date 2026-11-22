package com.bey.desktop.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class beyFormato {
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

    private beyFormato() {
    }

    public static String beyFechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return beyCapitalizar(FECHA_LARGA.format(fecha));
    }

    public static String beyFechaCorta(LocalDate fecha) {
        return fecha == null ? "" : FECHA_CORTA.format(fecha);
    }

    public static String beyEtiquetaEje(LocalDate fecha) {
        return fecha == null ? "" : FECHA_EJE.format(fecha);
    }

    public static String beyFechaHora(LocalDateTime fechaHora) {
        if (fechaHora == null) {
            return "";
        }
        return beyFechaCorta(fechaHora.toLocalDate()) + " · " + HORA.format(fechaHora);
    }

    public static String beyHora(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : HORA.format(fechaHora);
    }

    public static String beyNumero(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.05) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(LOCALE_TECNICO, "%.1f", valor);
    }

    public static String beyCronometro(Duration duracion) {
        long segundos = duracion == null ? 0 : Math.max(0, duracion.getSeconds());
        return String.format(LOCALE_TECNICO, "%02d:%02d:%02d",
                segundos / 3600, (segundos % 3600) / 60, segundos % 60);
    }

    public static double beyANumero(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(texto.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String beyCapitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase(LOCALE_ES) + texto.substring(1);
    }
}
