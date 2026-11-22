package com.bey.desktop.model;

public enum beyVarianteMovimiento {
    ESTANDAR,

    PRENSA,

    PRESS_SENTADO,

    REMO_SENTADO,

    REMO_INVERTIDO,

    FONDOS_SILLA,

    FLEXION_RODILLAS,

    FLEXION_PICA,

    ZANCADA_BULGARA,

    TRICEPS_SOBRE_CABEZA,

    CURL_ISOMETRICO_TOALLA,

    PRESS_HOMBRO_SENTADO,

    JALON_NEUTRO,

    APERTURA_INVERSA,

    CURL_FEMORAL_SENTADO,

    CURL_FEMORAL_NORDICO,

    CRUNCH_POLEA,

    RUEDA_ABDOMINAL,

    HACK,

    CURL_INCLINADO,

    TALONES_SENTADO,

    FLEXION_DECLINADA,

    PICA_ELEVADA,

    PISTOL,

    PLANCHA_ALTA;

    public boolean concentricaAlSubir(beyPatronMovimiento patron) {
        return switch (this) {
            case APERTURA_INVERSA -> true;
            case CURL_FEMORAL_NORDICO, RUEDA_ABDOMINAL -> false;
            default -> patron.isConcentricaAlSubir();
        };
    }

    public boolean esIsometrica() {
        return this == CURL_ISOMETRICO_TOALLA;
    }
}
