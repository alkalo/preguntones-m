package com.preguntones.parcelas;

/** Ocho aldeanos por persona, en todas las parcelas juntas. */
public final class Cupo {

    public static final int TOPE = 8;

    private Cupo() {
    }

    public static boolean cabe(int tienes, int llegan) {
        return llegan >= 0 && tienes >= 0 && tienes + llegan <= TOPE;
    }

    public static boolean puedeSumarUno(int tienes) {
        return tienes < TOPE;
    }
}
