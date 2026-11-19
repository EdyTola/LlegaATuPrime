package com.symmetry.desktop.model;

/**
 * Variante de postura de un {@link PatronMovimiento} cuando la biomecanica difiere de la
 * version base (por ejemplo, la prensa de piernas reclinada frente a la sentadilla de pie).
 */
public enum VarianteMovimiento {

    ESTANDAR,
    /** Prensa de piernas: espalda reclinada y empuje de la plataforma. */
    PRENSA,
    /** Press en maquina: sentado con respaldo y agarres. */
    PRESS_SENTADO,
    /** Remo sentado en maquina o polea baja, con el torso casi vertical. */
    REMO_SENTADO,
    /** Remo invertido bajo una barra o mesa (peso corporal). */
    REMO_INVERTIDO,
    /** Fondos con las manos apoyadas en una silla o banco, piernas hacia delante. */
    FONDOS_SILLA,
    /** Flexion con las rodillas apoyadas en el suelo. */
    FLEXION_RODILLAS,
    /** Flexion pica (cadera alta, forma de V invertida). */
    FLEXION_PICA,
    /** Zancada con el pie trasero elevado sobre un banco. */
    ZANCADA_BULGARA,
    /** Extension de triceps por encima de la cabeza. */
    TRICEPS_SOBRE_CABEZA,
    /** Curl isometrico: de pie sobre una toalla, tension sostenida al tirar de sus extremos. */
    CURL_ISOMETRICO_TOALLA,
    /** Press de hombros sentado con respaldo vertical (en maquina o con mancuernas). */
    PRESS_HOMBRO_SENTADO,
    /** Jalon con agarre neutro (mismo movimiento; cambia el agarre y el texto). */
    JALON_NEUTRO,
    /** Pec deck inverso: el brazo se abre hacia atras (deltoides posterior). */
    APERTURA_INVERSA,
    /** Curl femoral sentado en maquina. */
    CURL_FEMORAL_SENTADO,
    /** Curl femoral nordico: de rodillas con los tobillos anclados, el cuerpo cae hacia delante. */
    CURL_FEMORAL_NORDICO,
    /** Crunch de rodillas con cable (polea alta). */
    CRUNCH_POLEA,
    /** Rueda abdominal: de rodillas, la rueda se aleja y vuelve. */
    RUEDA_ABDOMINAL,
    /** Sentadilla hack: espalda sobre un respaldo inclinado con guias. */
    HACK,
    /** Curl de biceps sentado en un banco inclinado. */
    CURL_INCLINADO,
    /** Elevacion de talones sentado (rodillas a 90 grados). */
    TALONES_SENTADO,
    /** Flexion con los pies elevados sobre un banco. */
    FLEXION_DECLINADA,
    /** Flexion pica con los pies elevados sobre un banco. */
    PICA_ELEVADA,
    /** Sentadilla a una pierna con la otra extendida al frente. */
    PISTOL,
    /** Plancha sobre las manos (brazos extendidos). */
    PLANCHA_ALTA;

    /** true si la fase concentrica ocurre mientras el progreso aumenta (ajusta el patron base). */
    public boolean concentricaAlSubir(PatronMovimiento patron) {
        return switch (this) {
            case APERTURA_INVERSA -> true;
            case CURL_FEMORAL_NORDICO, RUEDA_ABDOMINAL -> false;
            default -> patron.isConcentricaAlSubir();
        };
    }

    /** true si el ejercicio es una contraccion mantenida (sin recorrido). */
    public boolean esIsometrica() {
        return this == CURL_ISOMETRICO_TOALLA;
    }
}
