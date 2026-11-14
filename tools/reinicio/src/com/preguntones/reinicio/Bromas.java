package com.preguntones.reinicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bromas cortas. Cada una cabe en el subtítulo sin deformarse. */
final class Bromas {
    static final String[] TODAS = {
        "Ya vuelvo: no se borra nada",
        "El creeper quería mimos",
        "Guardad el pico y respirad",
        "El dragón también se estira",
        "Fui a por agua, de verdad",
        "No borro nada, palabra",
        "Contad ovejas un momento",
        "La puerta sigue abierta",
        "Es un bostezo, no un adiós",
        "El horno os guarda el sitio",
        "Ni os asustéis, ya vuelvo",
        "Prometo que todo sigue",
        "Un café y estoy de vuelta",
        "Dura menos que un horno",
        "Las parcelas siguen ahí",
        "Los cofres no se mueven"
    };

    private Bromas() {
    }

    static String elegir(Random azar, String ultima) {
        List<String> bolsa = new ArrayList<>();
        for (String broma : TODAS) {
            if (!broma.equals(ultima)) {
                bolsa.add(broma);
            }
        }
        if (bolsa.isEmpty()) {
            bolsa.add(TODAS[0]);
        }
        return bolsa.get(azar.nextInt(bolsa.size()));
    }
}
