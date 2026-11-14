package com.preguntones.mundos;

/** El fuego del salvaje. En el barrio, la XO y el duelo no se enciende ni se extiende. */
public final class Fuego {
    /** Lo que trae el juego: arde cerca de quien está. */
    public static final int RADIO_SALVAJE = 128;
    /** No salta a otro bloque y no se apaga solo. */
    public static final int RADIO_QUIETO = 0;

    private Fuego() {
    }

    public static int radio(boolean salvaje) {
        return salvaje ? RADIO_SALVAJE : RADIO_QUIETO;
    }

    public static boolean puedeEncender(boolean salvaje) {
        return salvaje;
    }
}
