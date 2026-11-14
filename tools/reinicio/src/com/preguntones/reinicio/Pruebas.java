package com.preguntones.reinicio;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Pruebas {
    public static void main(String[] args) {
        anchos();
        cartel();
        bromas();
        ordenes();
        apagado();
        System.out.println("Reinicio ok");
    }

    private static void anchos() {
        if (Ancho.de(" ") != 4 || Ancho.de("i") != 2 || Ancho.de("l") != 3) {
            throw new IllegalStateException("La fuente no mide como la vanilla");
        }
        if (Ancho.de("Perdón, 15 s") != 64 || Ancho.de("Perdón, ya") != 54) {
            throw new IllegalStateException("La disculpa cambió de ancho");
        }
    }

    private static void cartel() {
        for (int segundos = 0; segundos <= 30; segundos++) {
            String titulo = Cartel.titulo(segundos);
            if (Ancho.de(titulo) > Ancho.TITULO) {
                throw new IllegalStateException(titulo + " no cabe en el título");
            }
        }
        if (!"Perdón, 15 s".equals(Cartel.titulo(15)) || !"Perdón, ya".equals(Cartel.titulo(0))) {
            throw new IllegalStateException("El título no dice la disculpa y el tiempo");
        }
        if (!"Reinicio en 15 segundos.".equals(Cartel.frase(15))
            || !"Reinicio en 1 segundo.".equals(Cartel.frase(1))) {
            throw new IllegalStateException("La frase del chat no cuenta el reinicio");
        }
    }

    private static void bromas() {
        if (Bromas.TODAS.length < 8) {
            throw new IllegalStateException("Hacen falta varias bromas");
        }
        Set<String> vistas = new HashSet<>();
        for (String broma : Bromas.TODAS) {
            if (broma.isBlank() || broma.indexOf('\n') >= 0) {
                throw new IllegalStateException("Broma vacía o con salto");
            }
            if (!vistas.add(broma)) {
                throw new IllegalStateException("Broma repetida: " + broma);
            }
            int ancho = Ancho.de(broma);
            if (ancho > Ancho.SUBTITULO) {
                throw new IllegalStateException(broma + " mide " + ancho + " y el tope es " + Ancho.SUBTITULO);
            }
            if (!Ancho.cabe(Cartel.titulo(Cartel.SEGUNDOS), broma)) {
                throw new IllegalStateException("No cabe junto al contador: " + broma);
            }
        }
        String fija = Bromas.elegir(new Random(1), "");
        if (!vistas.contains(fija)) {
            throw new IllegalStateException("Eligió una broma que no está");
        }
        for (int i = 0; i < 40; i++) {
            if (fija.equals(Bromas.elegir(new Random(i), fija))) {
                throw new IllegalStateException("Repitió la última broma");
            }
        }
    }

    private static void ordenes() {
        if (Orden.tipo("stop") != Orden.Tipo.PARAR
            || Orden.tipo("/stop") != Orden.Tipo.PARAR
            || Orden.tipo("  minecraft:stop ") != Orden.Tipo.PARAR
            || Orden.tipo("STOP ahora") != Orden.Tipo.PARAR) {
            throw new IllegalStateException("No reconoció stop");
        }
        if (Orden.tipo("restart") != Orden.Tipo.REINICIAR
            || Orden.tipo("/bukkit:restart") != Orden.Tipo.REINICIAR) {
            throw new IllegalStateException("No reconoció restart");
        }
        if (Orden.tipo("end") != Orden.Tipo.PARAR || Orden.tipo("/minecraft:end") != Orden.Tipo.PARAR) {
            throw new IllegalStateException("No reconoció end");
        }
        if (Orden.tipo("stoplag") != null
            || Orden.tipo("save-all") != null
            || Orden.tipo("restart-on-crash") != null
            || Orden.tipo("") != null
            || Orden.tipo(null) != null) {
            throw new IllegalStateException("Paró con un comando que no es parar");
        }
    }

    private static void apagado() {
        if (!Decidir.alApagar(false, true, 2)) {
            throw new IllegalStateException("Tenía que avisar al apagar con gente");
        }
        if (Decidir.alApagar(true, true, 2) || Decidir.alApagar(false, false, 2) || Decidir.alApagar(false, true, 0)) {
            throw new IllegalStateException("Avisó cuando no tocaba");
        }
    }
}
