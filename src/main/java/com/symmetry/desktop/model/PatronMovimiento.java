package com.symmetry.desktop.model;

import java.util.List;

/**
 * Patron biomecanico de un ejercicio: define como se anima el modelo 2D, si la animacion se
 * ve de lado o de frente y los puntos clave de ejecucion (postura, rango y respiracion).
 */
public enum PatronMovimiento {

    SENTADILLA("Sentadilla / prensa", false, false, 4.2, 0.0, List.of(
            "Postura: pies al ancho de hombros, espalda neutra y pecho alto durante todo el movimiento.",
            "Rango: baja hasta la paralela o más, con las rodillas siguiendo la línea de los pies.",
            "Respiración: inspira al bajar y sostén el aire al subir; exhala pasando el punto más difícil.")),

    PRESS_PLANO("Press de pecho", true, false, 3.6, 0.1, List.of(
            "Postura: escápulas juntas y hundidas en el banco, pies firmes en el suelo.",
            "Rango: baja controlado hasta rozar el pecho y empuja sin bloquear los codos de golpe.",
            "Respiración: inspira al bajar la carga y exhala mientras empujas.")),

    PRESS_INCLINADO("Press inclinado", true, false, 3.6, 0.1, List.of(
            "Postura: banco a 30-45°, escápulas retraídas y muñecas alineadas con los codos.",
            "Rango: baja hasta la parte alta del pecho y extiende sin perder la tensión.",
            "Respiración: inspira al bajar y exhala al empujar hacia arriba.")),

    FLEXION("Flexión de brazos", true, false, 3.4, 0.4, List.of(
            "Postura: cuerpo en línea recta de cabeza a talones, abdomen y glúteos apretados.",
            "Rango: baja hasta que el pecho casi toque el suelo, con los codos a unos 45° del torso.",
            "Respiración: inspira al bajar y exhala al empujar el suelo.")),

    PLANCHA("Plancha isométrica", true, false, 4.0, 0.4, List.of(
            "Postura: codos bajo los hombros, cuerpo recto y cadera sin hundirse ni subir.",
            "Rango: no hay movimiento; mantén la tensión del abdomen y los glúteos.",
            "Respiración: respira de forma lenta y constante, sin contener el aire.")),

    JALON("Jalón al pecho", true, false, 3.8, 0.0, List.of(
            "Postura: pecho alto, torso ligeramente inclinado atrás y hombros lejos de las orejas.",
            "Rango: lleva la barra a la clavícula guiando con los codos y estira del todo al subir.",
            "Respiración: exhala al tirar hacia ti e inspira al dejar subir la barra.")),

    REMO("Remo", true, false, 3.6, 0.0, List.of(
            "Postura: espalda recta y torso inclinado, sin balancear el cuerpo para mover la carga.",
            "Rango: tira hacia el ombligo juntando las escápulas y estira los brazos al volver.",
            "Respiración: exhala al tirar e inspira al extender los brazos.")),

    DOMINADA("Dominada", true, false, 4.0, 0.0, List.of(
            "Postura: hombros activos (lejos de las orejas), piernas juntas y sin balanceo.",
            "Rango: sube hasta pasar la barbilla por la barra y baja con los brazos casi extendidos.",
            "Respiración: exhala al subir e inspira durante la bajada controlada.")),

    CURL_BICEPS("Curl de bíceps", true, false, 3.4, 0.0, List.of(
            "Postura: codos pegados al torso y sin balancear la espalda para subir el peso.",
            "Rango: flexiona por completo arriba y baja hasta casi extender el codo.",
            "Respiración: exhala al subir e inspira mientras bajas lento (2-3 segundos).")),

    EXTENSION_TRICEPS("Extensión de tríceps", true, false, 3.4, 0.0, List.of(
            "Postura: codos fijos junto al cuerpo; solo se mueve el antebrazo.",
            "Rango: extiende hasta bloquear el codo y vuelve hasta unos 90° de flexión.",
            "Respiración: exhala al extender e inspira al subir con control.")),

    PESO_MUERTO("Peso muerto / bisagra de cadera", true, false, 4.2, 0.0, List.of(
            "Postura: espalda neutra, barra pegada a las piernas y cadera llevada hacia atrás.",
            "Rango: baja hasta sentir el estiramiento de isquios y sube extendiendo la cadera.",
            "Respiración: toma aire y bloquea el abdomen antes de subir; exhala arriba.")),

    HIP_THRUST("Hip thrust", true, false, 3.6, 0.1, List.of(
            "Postura: espalda alta apoyada, mentón al pecho y pies a la anchura de la cadera.",
            "Rango: sube hasta alinear tronco y muslos y aprieta los glúteos en la cima.",
            "Respiración: exhala al subir y haz una pausa de un segundo arriba.")),

    PUENTE_GLUTEOS("Puente de glúteos", true, false, 3.4, 0.1, List.of(
            "Postura: espalda en el suelo, rodillas flexionadas y pies cerca de los glúteos.",
            "Rango: eleva la cadera hasta alinear hombros, cadera y rodillas sin arquear la zona lumbar.",
            "Respiración: exhala al subir y aprieta los glúteos un segundo.")),

    ELEVACION_LATERAL("Elevación lateral", true, true, 3.4, 0.0, List.of(
            "Postura: ligera flexión de codos y torso quieto, sin impulso con el cuerpo.",
            "Rango: sube hasta la altura de los hombros y baja lento, sin dejar caer los brazos.",
            "Respiración: exhala al elevar e inspira al bajar controlando el peso.")),

    PRESS_HOMBRO("Press de hombros", true, false, 3.6, 0.0, List.of(
            "Postura: abdomen y glúteos firmes, sin arquear la zona lumbar al empujar.",
            "Rango: baja hasta la altura de la barbilla y extiende por encima de la cabeza.",
            "Respiración: inspira antes de empujar y exhala al extender los brazos.")),

    ZANCADA("Zancada", false, false, 4.0, 0.0, List.of(
            "Postura: torso erguido y rodilla delantera alineada con el pie, sin dejarla caer hacia dentro.",
            "Rango: baja hasta que ambas rodillas formen unos 90° y empuja con el talón delantero.",
            "Respiración: inspira al bajar y exhala al subir.")),

    ELEVACION_TALONES("Elevación de talones", true, false, 3.0, 0.0, List.of(
            "Postura: apoya la parte delantera del pie y mantén las rodillas casi extendidas.",
            "Rango: sube lo máximo posible, haz una pausa arriba y baja estirando el gemelo.",
            "Respiración: exhala al subir e inspira durante la bajada lenta.")),

    CURL_FEMORAL("Curl femoral", true, false, 3.6, -0.2, List.of(
            "Postura: cadera pegada al banco y sin levantar la pelvis para ayudarte.",
            "Rango: flexiona hasta acercar el talón al glúteo y vuelve casi hasta extender.",
            "Respiración: exhala al flexionar e inspira al volver con control.")),

    CRUNCH("Crunch abdominal", true, false, 3.2, 0.1, List.of(
            "Postura: zona lumbar apoyada y mirada al techo, sin tirar del cuello con las manos.",
            "Rango: enrolla el torso hasta despegar las escápulas; no hace falta sentarse del todo.",
            "Respiración: exhala fuerte al subir y suelta el aire al volver.")),

    ELEVACION_PIERNAS("Elevación de piernas", true, false, 3.8, 0.0, List.of(
            "Postura: zona lumbar pegada al suelo y abdomen contraído durante todo el movimiento.",
            "Rango: sube las piernas casi verticales y baja sin tocar el suelo.",
            "Respiración: exhala al subir e inspira al bajar lento.")),

    FONDOS("Fondos", true, false, 3.6, 0.0, List.of(
            "Postura: hombros hacia atrás y abajo, torso ligeramente inclinado y codos cerca del cuerpo.",
            "Rango: baja hasta unos 90° en el codo y extiende sin encogerte de hombros.",
            "Respiración: inspira al bajar y exhala al empujar.")),

    SUPERMAN("Superman", true, false, 3.6, 0.05, List.of(
            "Postura: boca abajo con el cuello neutro, mirando al suelo.",
            "Rango: eleva a la vez brazos y piernas unos centímetros y mantén un segundo.",
            "Respiración: exhala al elevar e inspira al bajar sin dejar caer el cuerpo.")),

    APERTURA_PECHO("Aperturas en máquina (pec deck)", false, true, 3.6, 0.0, List.of(
            "Postura: espalda y cabeza apoyadas en el respaldo, codos ligeramente semiflexionados y fijos a la altura del pecho, antebrazos sobre las almohadillas.",
            "Rango: abre los brazos de forma controlada hasta notar el estiramiento del pecho, sin pasar la línea de los hombros, y júntalos al frente apretando el pecho.",
            "Respiración: inspira al abrir y exhala al juntar los brazos; no cambies el ángulo de los codos ni encojas los hombros.")),

    EXTENSION_CUADRICEPS("Extensión de cuádriceps", true, false, 3.6, 0.0, List.of(
            "Postura: sentado con la espalda apoyada, rodillas alineadas con el eje de la máquina y el rodillo sobre la parte baja de las espinillas.",
            "Rango: extiende las rodillas hasta casi bloquearlas, aprieta el cuádriceps un segundo y baja lento sin dejar caer la pila.",
            "Respiración: exhala al extender e inspira al bajar controlando el peso (2-3 segundos).")),

    BURPEE("Burpee", true, false, 5.0, 0.1, List.of(
            "Postura: al bajar apoya las manos junto a los pies y lleva los pies atrás con el cuerpo en línea.",
            "Rango: pecho cerca del suelo en la flexión y vuelta explosiva a la posición de pie.",
            "Respiración: exhala al empujar el suelo y al incorporarte; no contengas el aire."));

    private final String titulo;
    private final boolean concentricaAlSubir;
    private final boolean vistaFrontal;
    private final double periodoSegundos;
    private final double desplazamientoX;
    private final List<String> puntosClave;

    /**
     * @param concentricaAlSubir true si la fase concentrica (esfuerzo) ocurre mientras el progreso
     *                           del movimiento aumenta (por ejemplo, subir el peso en un curl);
     *                           false si ocurre al disminuir (por ejemplo, subir en una sentadilla)
     * @param desplazamientoX    desplazamiento horizontal para centrar la figura en el lienzo
     */
    PatronMovimiento(String titulo, boolean concentricaAlSubir, boolean vistaFrontal,
                     double periodoSegundos, double desplazamientoX, List<String> puntosClave) {
        this.titulo = titulo;
        this.concentricaAlSubir = concentricaAlSubir;
        this.vistaFrontal = vistaFrontal;
        this.periodoSegundos = periodoSegundos;
        this.desplazamientoX = desplazamientoX;
        this.puntosClave = puntosClave;
    }

    public String getTitulo() {
        return titulo;
    }

    /** true si el esfuerzo concentrico ocurre mientras el progreso del movimiento aumenta. */
    public boolean isConcentricaAlSubir() {
        return concentricaAlSubir;
    }

    /** Solo la plancha es isometrica (sin recorrido, se mantiene la posicion). */
    public boolean isIsometrico() {
        return this == PLANCHA;
    }

    public boolean isVistaFrontal() {
        return vistaFrontal;
    }

    public double getPeriodoSegundos() {
        return periodoSegundos;
    }

    public double getDesplazamientoX() {
        return desplazamientoX;
    }

    public List<String> getPuntosClave() {
        return puntosClave;
    }
}
