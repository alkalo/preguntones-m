package com.preguntones.viajes;

import java.util.Random;

public final class Radio {
    private Radio() {
    }

    public static double radio(Random azar, double minimo, double maximo) {
        double cerca = minimo * minimo;
        double lejos = maximo * maximo;
        return Math.sqrt(cerca + azar.nextDouble() * (lejos - cerca));
    }

    public static double[] punto(Random azar, double minimo, double maximo) {
        double angulo = azar.nextDouble() * Math.PI * 2.0;
        double distancia = radio(azar, minimo, maximo);
        return new double[] {Math.cos(angulo) * distancia, Math.sin(angulo) * distancia};
    }
}
