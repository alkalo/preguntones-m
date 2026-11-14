package com.preguntones.viajes;

public final class Peligro {
    public enum Motivo {
        LAVA,
        AHOGO,
        PELEA
    }

    private Peligro() {
    }

    public static Motivo evaluar(boolean lava, boolean ojosEnAgua, boolean cuerpoEnAgua, int aire, int aireMax, boolean pelea) {
        if (lava) {
            return Motivo.LAVA;
        }
        if (ahogandose(ojosEnAgua, cuerpoEnAgua, aire, aireMax)) {
            return Motivo.AHOGO;
        }
        if (pelea) {
            return Motivo.PELEA;
        }
        return null;
    }

    public static boolean ahogandose(boolean ojosEnAgua, boolean cuerpoEnAgua, int aire, int aireMax) {
        if (cuerpoEnAgua && aire <= 0) {
            return true;
        }
        return ojosEnAgua && aireMax > 0 && aire < aireMax;
    }
}
