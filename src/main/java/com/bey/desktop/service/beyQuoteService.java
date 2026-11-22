package com.bey.desktop.service;

import com.bey.desktop.model.beyPerfilUsuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class beyQuoteService {
    public static final String AUTOR_PERSONAL = "Mi Objetivo Personal";

    public static final int LONGITUD_MAXIMA = 140;

    private static final double PROBABILIDAD_PERSONAL = 0.34;

    public record beyFrase(String texto, String autor, boolean personal) {
    }

    private static final List<beyFrase> LEGENDARIAS = List.of(

            beyF("La victoria pertenece al más perseverante.", "Napoleón Bonaparte"),
            beyF("Un líder es un vendedor de esperanza.", "Napoleón Bonaparte"),
            beyF("Imposible es una palabra que solo se encuentra en el diccionario de los necios.", "Napoleón Bonaparte"),
            beyF("Temo más a un ejército de ovejas dirigido por un león que a uno de leones dirigido por una oveja.", "Alejandro Magno"),
            beyF("No hay nada imposible para quien lo intenta.", "Alejandro Magno"),
            beyF("Vine, vi, vencí.", "Julio César"),
            beyF("Quien se conoce a sí mismo y conoce a su enemigo no temerá el resultado de cien batallas.", "Sun Tzu"),
            beyF("En medio del caos también hay oportunidad.", "Sun Tzu"),
            beyF("La suprema excelencia consiste en vencer sin luchar.", "Sun Tzu"),
            beyF("Hoy es la victoria sobre tu yo de ayer.", "Miyamoto Musashi"),
            beyF("Haz lo que puedas, con lo que tengas, donde estés.", "Theodore Roosevelt"),
            beyF("Si estás atravesando el infierno, sigue adelante.", "Winston Churchill"),
            beyF("El éxito es ir de fracaso en fracaso sin perder el entusiasmo.", "Winston Churchill"),
            beyF("Todo parece imposible hasta que se hace.", "Nelson Mandela"),

            beyF("El obstáculo del camino se convierte en el camino.", "Marco Aurelio"),
            beyF("Tienes poder sobre tu mente, no sobre los acontecimientos externos. Comprende esto y encontrarás la fuerza.", "Marco Aurelio"),
            beyF("Si no es correcto, no lo hagas; si no es verdad, no lo digas.", "Marco Aurelio"),
            beyF("No es que tengamos poco tiempo, sino que perdemos mucho.", "Séneca"),
            beyF("Las dificultades fortalecen la mente, como el trabajo fortalece el cuerpo.", "Séneca"),
            beyF("La suerte es lo que sucede cuando la preparación se encuentra con la oportunidad.", "Séneca"),
            beyF("No hay viento favorable para el que no sabe adónde va.", "Séneca"),
            beyF("Ningún hombre es libre si no es dueño de sí mismo.", "Epicteto"),
            beyF("Primero di qué quieres ser; después haz lo que tengas que hacer.", "Epicteto"),
            beyF("No son las cosas las que nos perturban, sino la opinión que tenemos de ellas.", "Epicteto"),
            beyF("Somos lo que hacemos repetidamente. La excelencia no es un acto, sino un hábito.", "Will Durant"),
            beyF("Lo que no me mata me hace más fuerte.", "Friedrich Nietzsche"),
            beyF("No importa lo lento que vayas, siempre que no te detengas.", "Confucio"),
            beyF("Un viaje de mil millas comienza con un solo paso.", "Lao Tse"),

            beyF("Las personas exitosas son simplemente personas con hábitos exitosos.", "Brian Tracy"),
            beyF("No puedes controlar lo que te sucede, pero sí tu actitud ante ello.", "Brian Tracy"),
            beyF("Sal de tu zona de confort: solo creces cuando aceptas sentirte incómodo.", "Brian Tracy"),
            beyF("La disciplina es el puente entre las metas y los logros.", "Jim Rohn"),
            beyF("Tanto si crees que puedes como si crees que no puedes, tienes razón.", "Henry Ford"),

            beyF("No cuentes los días; haz que los días cuenten.", "Muhammad Ali"),
            beyF("Sufre ahora y vive el resto de tu vida como un campeón.", "Muhammad Ali"),
            beyF("He fallado una y otra vez en mi vida. Por eso tengo éxito.", "Michael Jordan"),
            beyF("Las últimas repeticiones son las que hacen crecer el músculo.", "Arnold Schwarzenegger"),
            beyF("La fuerza no viene de ganar: tus luchas desarrollan tus fuerzas.", "Arnold Schwarzenegger"),
            beyF("No temo al que practicó 10.000 patadas una vez, sino al que practicó una patada 10.000 veces.", "Bruce Lee"),
            beyF("Sé agua, amigo mío.", "Bruce Lee"),
            beyF("Todo lo negativo —presión, desafíos— es una oportunidad para elevarme.", "Kobe Bryant"),

            beyF("No importa lo fuerte que golpees, sino lo fuerte que te golpean y sigues avanzando.", "Rocky Balboa"),
            beyF("Lo que hacemos en la vida tiene eco en la eternidad.", "Máximo Décimo Meridio · Gladiator"),
            beyF("¿Por qué nos caemos? Para aprender a levantarnos.", "Thomas Wayne · Batman Begins"),
            beyF("Hazlo o no lo hagas, pero no lo intentes.", "Yoda · Star Wars"),
            beyF("No te disculpes. Sé mejor.", "Kratos · God of War"),
            beyF("Es peligroso ir solo. ¡Toma esto!", "The Legend of Zelda"),
            beyF("Terminemos esta pelea.", "Jefe Maestro · Halo"),
            beyF("Jamás retrocedo en mi palabra: ese es mi camino ninja.", "Naruto Uzumaki"));

    private static beyFrase beyActual;
    private static String beyClaveActual = "";
    private static beyFrase beyUltima;
    private static final Random AZAR = new Random();

    private beyQuoteService() {
    }

    private static beyFrase beyF(String texto, String autor) {
        return new beyFrase(texto, autor, false);
    }

    public static List<beyFrase> beyLegendarias() {
        return LEGENDARIAS;
    }

    public static synchronized beyFrase beyFraseDelDia(beyPerfilUsuario perfil, LocalDate hoy) {
        String clave = perfil.getId() + "|" + hoy + "|" + Objects.toString(beyPersonal(perfil), "");
        if (beyActual == null || !clave.equals(beyClaveActual)) {
            beyClaveActual = clave;
            beyActual = beyElegir(perfil, AZAR);
        }
        return beyActual;
    }

    public static synchronized beyFrase beySiguiente(beyPerfilUsuario perfil, LocalDate hoy) {
        String clave = perfil.getId() + "|" + hoy + "|" + Objects.toString(beyPersonal(perfil), "");
        beyClaveActual = clave;
        beyActual = beyElegir(perfil, AZAR);
        return beyActual;
    }

    static beyFrase beyElegir(beyPerfilUsuario perfil, Random azar) {
        String propia = beyPersonal(perfil);
        beyFrase elegida;
        int intentos = 0;
        do {
            if (propia != null && azar.nextDouble() < PROBABILIDAD_PERSONAL) {
                elegida = new beyFrase(propia, AUTOR_PERSONAL, true);
            } else {
                elegida = LEGENDARIAS.get(azar.nextInt(LEGENDARIAS.size()));
            }
        } while (elegida.equals(beyUltima) && ++intentos < 8);
        beyUltima = elegida;
        return elegida;
    }

    public static String beyPersonal(beyPerfilUsuario perfil) {
        if (perfil == null || perfil.getCustomQuote() == null) {
            return null;
        }
        String texto = perfil.getCustomQuote().trim();
        return texto.isEmpty() ? null : texto;
    }
}
