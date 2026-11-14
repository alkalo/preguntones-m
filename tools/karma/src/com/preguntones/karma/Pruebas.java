package com.preguntones.karma;

public final class Pruebas {

    public static void main(String[] args) {
        etiqueta(-100, Etiqueta.CRUEL);
        etiqueta(-60, Etiqueta.CRUEL);
        etiqueta(-59, Etiqueta.MALO);
        etiqueta(-20, Etiqueta.MALO);
        etiqueta(-19, Etiqueta.NORMAL);
        etiqueta(0, Etiqueta.NORMAL);
        etiqueta(19, Etiqueta.NORMAL);
        etiqueta(20, Etiqueta.BUENO);
        etiqueta(40, Etiqueta.BUENO);
        System.out.println("Karma ok");
    }

    private static void etiqueta(int karma, Etiqueta esperada) {
        Etiqueta salida = Etiqueta.de(karma, 20, -20, -60);
        if (salida != esperada) {
            throw new IllegalStateException(karma + " -> " + salida + " y esperaba " + esperada);
        }
    }
}
