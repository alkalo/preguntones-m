package com.preguntones.mundos;

/** Qué pide la puerta del End en cada ciclo. El 1 es la partida. El 2 pide las dos excursiones. Luego se turnan. */
public final class Ciclo {

    private Ciclo() {
    }

    public static int normalizar(int ciclo) {
        return Math.max(1, ciclo);
    }

    public static boolean pideDias(int ciclo) {
        return normalizar(ciclo) <= 1;
    }

    public static boolean pideBastion(int ciclo) {
        int n = normalizar(ciclo);
        return n == 2 || (n >= 4 && n % 2 == 0);
    }

    public static boolean pideCiudad(int ciclo) {
        int n = normalizar(ciclo);
        return n == 2 || (n >= 3 && n % 2 == 1);
    }
}
