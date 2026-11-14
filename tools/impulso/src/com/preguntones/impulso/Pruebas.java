package com.preguntones.impulso;

public final class Pruebas {

    public static void main(String[] args) {
        Tridente.Memoria memoria = new Tridente.Memoria();
        no(memoria, true, 3, false, 1_000, "Reach", 3);
        no(memoria, true, 3, false, 1_000, "Simulation", 0.02);
        si(memoria, true, 3, false, 1_000, "Simulation", 2.5);
        si(memoria, true, 3, false, 1_010, "GroundSpoof", Double.NaN);
        si(memoria, true, 3, false, 1_020, "NoFall", Double.NaN);
        no(memoria, true, 3, false, 1_200, "Simulation", 2.0);
        si(memoria, true, 3, false, 1_800, "Simulation", 1.2);
        no(memoria, true, 3, false, 1_900, "Simulation", 9);

        memoria = new Tridente.Memoria();
        si(memoria, true, 1, false, 5_000, "GroundSpoof", Double.NaN);
        si(memoria, true, 1, false, 5_020, "Simulation", 3);
        no(memoria, true, 1, false, 5_020, "Simulation", 9);
        no(memoria, false, 1, false, 6_000, "GroundSpoof", Double.NaN);
        no(memoria, true, 0, false, 6_000, "Simulation", 3);

        memoria = new Tridente.Memoria();
        memoria.giroHasta = 8_000;
        si(memoria, false, 0, false, 7_500, "Simulation", 0.2);
        si(memoria, false, 0, false, 7_500, "NoSlow", Double.NaN);
        no(memoria, false, 0, false, 7_500, "Simulation", 9);
        no(memoria, false, 0, false, 8_001, "Simulation", 0.2);

        memoria = new Tridente.Memoria();
        si(memoria, false, 0, true, 9_000, "Simulation", 1);
        si(memoria, true, 3, false, 10_000, "NoSlow", Double.NaN);
        no(memoria, false, 3, false, 10_100, "NoSlow", Double.NaN);
        System.out.println("Impulso ok");
    }

    private static void si(Tridente.Memoria memoria, boolean puede, int nivel, boolean girando, long ahora, String chequeo, double desfase) {
        if (!Tridente.ignorar(puede, nivel, girando, ahora, memoria, chequeo, desfase)) {
            throw new IllegalStateException(chequeo + " en " + ahora + " debía ignorarse");
        }
    }

    private static void no(Tridente.Memoria memoria, boolean puede, int nivel, boolean girando, long ahora, String chequeo, double desfase) {
        if (Tridente.ignorar(puede, nivel, girando, ahora, memoria, chequeo, desfase)) {
            throw new IllegalStateException(chequeo + " en " + ahora + " no debía ignorarse");
        }
    }
}
