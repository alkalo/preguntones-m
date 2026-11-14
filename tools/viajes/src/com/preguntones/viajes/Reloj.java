package com.preguntones.viajes;

public final class Reloj {
    private Reloj() {
    }

    public static int quedan(int totalMedios, int medios) {
        if (medios >= totalMedios) {
            return 0;
        }
        return (totalMedios - medios + 1) / 2;
    }
}
