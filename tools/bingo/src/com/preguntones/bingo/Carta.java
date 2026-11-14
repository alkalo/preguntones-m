package com.preguntones.bingo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class Carta {
    static final int TAMANO = 9;
    private static final int FACILES = 4;

    private Carta() {
    }

    static List<Casilla> elegir(String dia) {
        Random azar = new Random(dia.hashCode());
        List<Casilla> faciles = new ArrayList<>();
        List<Casilla> resto = new ArrayList<>();
        for (Casilla casilla : Casilla.values()) {
            if (casilla.facil) {
                faciles.add(casilla);
            } else {
                resto.add(casilla);
            }
        }
        Collections.shuffle(faciles, azar);
        Collections.shuffle(resto, azar);
        List<Casilla> carta = new ArrayList<>();
        int seguras = Math.min(FACILES, faciles.size());
        carta.addAll(faciles.subList(0, seguras));
        List<Casilla> bolsa = new ArrayList<>();
        if (faciles.size() > seguras) {
            bolsa.addAll(faciles.subList(seguras, faciles.size()));
        }
        bolsa.addAll(resto);
        Collections.shuffle(bolsa, azar);
        int faltan = Math.min(TAMANO - carta.size(), bolsa.size());
        carta.addAll(bolsa.subList(0, faltan));
        Collections.shuffle(carta, azar);
        return List.copyOf(carta);
    }
}
