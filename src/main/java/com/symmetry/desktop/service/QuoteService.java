package com.symmetry.desktop.service;

import com.symmetry.desktop.model.PerfilUsuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Banco de frases de poder y logica de seleccion. Combina frases cortas de estrategas,
 * filosofos, deportistas y figuras de la ficcion con la frase personal del usuario
 * ({@link PerfilUsuario#getCustomQuote()}).
 *
 * <p>La frase mostrada se conserva durante el mismo dia y perfil para que el panel no cambie
 * de texto cada vez que se navega; se renueva al abrir la aplicacion en un dia nuevo, al
 * cambiar de perfil, al editar la frase personal o al pedir otra con {@link #siguiente}.</p>
 */
public final class QuoteService {

    /** Autor mostrado para la frase escrita por el usuario. */
    public static final String AUTOR_PERSONAL = "Mi Objetivo Personal";

    /** Longitud maxima de la frase personal. */
    public static final int LONGITUD_MAXIMA = 140;

    /** Probabilidad de que salga la frase personal cuando existe (1 de cada 3 aproximadamente). */
    private static final double PROBABILIDAD_PERSONAL = 0.34;

    /** Frase lista para mostrar: texto, autor y si es la frase personal del usuario. */
    public record Frase(String texto, String autor, boolean personal) {
    }

    private static final List<Frase> LEGENDARIAS = List.of(
            // --- Estrategas y lideres historicos ---
            f("La victoria pertenece al más perseverante.", "Napoleón Bonaparte"),
            f("Un líder es un vendedor de esperanza.", "Napoleón Bonaparte"),
            f("Imposible es una palabra que solo se encuentra en el diccionario de los necios.", "Napoleón Bonaparte"),
            f("Temo más a un ejército de ovejas dirigido por un león que a uno de leones dirigido por una oveja.", "Alejandro Magno"),
            f("No hay nada imposible para quien lo intenta.", "Alejandro Magno"),
            f("Vine, vi, vencí.", "Julio César"),
            f("Quien se conoce a sí mismo y conoce a su enemigo no temerá el resultado de cien batallas.", "Sun Tzu"),
            f("En medio del caos también hay oportunidad.", "Sun Tzu"),
            f("La suprema excelencia consiste en vencer sin luchar.", "Sun Tzu"),
            f("Hoy es la victoria sobre tu yo de ayer.", "Miyamoto Musashi"),
            f("Haz lo que puedas, con lo que tengas, donde estés.", "Theodore Roosevelt"),
            f("Si estás atravesando el infierno, sigue adelante.", "Winston Churchill"),
            f("El éxito es ir de fracaso en fracaso sin perder el entusiasmo.", "Winston Churchill"),
            f("Todo parece imposible hasta que se hace.", "Nelson Mandela"),
            // --- Estoicos y filosofos ---
            f("El obstáculo del camino se convierte en el camino.", "Marco Aurelio"),
            f("Tienes poder sobre tu mente, no sobre los acontecimientos externos. Comprende esto y encontrarás la fuerza.", "Marco Aurelio"),
            f("Si no es correcto, no lo hagas; si no es verdad, no lo digas.", "Marco Aurelio"),
            f("No es que tengamos poco tiempo, sino que perdemos mucho.", "Séneca"),
            f("Las dificultades fortalecen la mente, como el trabajo fortalece el cuerpo.", "Séneca"),
            f("La suerte es lo que sucede cuando la preparación se encuentra con la oportunidad.", "Séneca"),
            f("No hay viento favorable para el que no sabe adónde va.", "Séneca"),
            f("Ningún hombre es libre si no es dueño de sí mismo.", "Epicteto"),
            f("Primero di qué quieres ser; después haz lo que tengas que hacer.", "Epicteto"),
            f("No son las cosas las que nos perturban, sino la opinión que tenemos de ellas.", "Epicteto"),
            f("Somos lo que hacemos repetidamente. La excelencia no es un acto, sino un hábito.", "Will Durant"),
            f("Lo que no me mata me hace más fuerte.", "Friedrich Nietzsche"),
            f("No importa lo lento que vayas, siempre que no te detengas.", "Confucio"),
            f("Un viaje de mil millas comienza con un solo paso.", "Lao Tse"),
            // --- Productividad y mentalidad ---
            f("Las personas exitosas son simplemente personas con hábitos exitosos.", "Brian Tracy"),
            f("No puedes controlar lo que te sucede, pero sí tu actitud ante ello.", "Brian Tracy"),
            f("Sal de tu zona de confort: solo creces cuando aceptas sentirte incómodo.", "Brian Tracy"),
            f("La disciplina es el puente entre las metas y los logros.", "Jim Rohn"),
            f("Tanto si crees que puedes como si crees que no puedes, tienes razón.", "Henry Ford"),
            // --- Deporte y fuerza ---
            f("No cuentes los días; haz que los días cuenten.", "Muhammad Ali"),
            f("Sufre ahora y vive el resto de tu vida como un campeón.", "Muhammad Ali"),
            f("He fallado una y otra vez en mi vida. Por eso tengo éxito.", "Michael Jordan"),
            f("Las últimas repeticiones son las que hacen crecer el músculo.", "Arnold Schwarzenegger"),
            f("La fuerza no viene de ganar: tus luchas desarrollan tus fuerzas.", "Arnold Schwarzenegger"),
            f("No temo al que practicó 10.000 patadas una vez, sino al que practicó una patada 10.000 veces.", "Bruce Lee"),
            f("Sé agua, amigo mío.", "Bruce Lee"),
            f("Todo lo negativo —presión, desafíos— es una oportunidad para elevarme.", "Kobe Bryant"),
            // --- Cine, anime y videojuegos ---
            f("No importa lo fuerte que golpees, sino lo fuerte que te golpean y sigues avanzando.", "Rocky Balboa"),
            f("Lo que hacemos en la vida tiene eco en la eternidad.", "Máximo Décimo Meridio · Gladiator"),
            f("¿Por qué nos caemos? Para aprender a levantarnos.", "Thomas Wayne · Batman Begins"),
            f("Hazlo o no lo hagas, pero no lo intentes.", "Yoda · Star Wars"),
            f("No te disculpes. Sé mejor.", "Kratos · God of War"),
            f("Es peligroso ir solo. ¡Toma esto!", "The Legend of Zelda"),
            f("Terminemos esta pelea.", "Jefe Maestro · Halo"),
            f("Jamás retrocedo en mi palabra: ese es mi camino ninja.", "Naruto Uzumaki"));

    private static Frase actual;
    private static String claveActual = "";
    private static Frase ultima;
    private static final Random AZAR = new Random();

    private QuoteService() {
    }

    private static Frase f(String texto, String autor) {
        return new Frase(texto, autor, false);
    }

    /** Todas las frases legendarias del banco inicial. */
    public static List<Frase> legendarias() {
        return LEGENDARIAS;
    }

    /**
     * Frase a mostrar hoy para el perfil dado. Se mantiene estable durante el mismo dia,
     * perfil y frase personal; en cuanto alguno cambia se elige otra al azar.
     */
    public static synchronized Frase fraseDelDia(PerfilUsuario perfil, LocalDate hoy) {
        String clave = perfil.getId() + "|" + hoy + "|" + Objects.toString(personal(perfil), "");
        if (actual == null || !clave.equals(claveActual)) {
            claveActual = clave;
            actual = elegir(perfil, AZAR);
        }
        return actual;
    }

    /** Elige una frase distinta de la actual (boton "otra frase"). */
    public static synchronized Frase siguiente(PerfilUsuario perfil, LocalDate hoy) {
        String clave = perfil.getId() + "|" + hoy + "|" + Objects.toString(personal(perfil), "");
        claveActual = clave;
        actual = elegir(perfil, AZAR);
        return actual;
    }

    /**
     * Seleccion aleatoria: si el usuario escribio una frase propia, sale aproximadamente una de
     * cada tres veces; en el resto se toma una legendaria. Nunca repite la frase anterior.
     */
    static Frase elegir(PerfilUsuario perfil, Random azar) {
        String propia = personal(perfil);
        Frase elegida;
        int intentos = 0;
        do {
            if (propia != null && azar.nextDouble() < PROBABILIDAD_PERSONAL) {
                elegida = new Frase(propia, AUTOR_PERSONAL, true);
            } else {
                elegida = LEGENDARIAS.get(azar.nextInt(LEGENDARIAS.size()));
            }
        } while (elegida.equals(ultima) && ++intentos < 8);
        ultima = elegida;
        return elegida;
    }

    /** Frase personal normalizada (sin espacios sobrantes), o null si esta vacia. */
    public static String personal(PerfilUsuario perfil) {
        if (perfil == null || perfil.getCustomQuote() == null) {
            return null;
        }
        String texto = perfil.getCustomQuote().trim();
        return texto.isEmpty() ? null : texto;
    }
}
