package com.preguntones.reinicio;

/** Texto del título: disculpa y segundos arriba, broma debajo. */
final class Cartel {
    static final int SEGUNDOS = 15;

    private Cartel() {
    }

    static String titulo(int segundos) {
        if (segundos <= 0) {
            return "Perdón, ya";
        }
        return "Perdón, " + segundos + " s";
    }

    static String frase(int segundos) {
        if (segundos <= 0) {
            return "El reinicio es ahora.";
        }
        if (segundos == 1) {
            return "Reinicio en 1 segundo.";
        }
        return "Reinicio en " + segundos + " segundos.";
    }
}
