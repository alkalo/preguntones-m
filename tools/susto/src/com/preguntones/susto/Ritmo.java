package com.preguntones.susto;

import java.util.Random;

/** Cuenta minutos con gente dentro y avisa una sola vez al llegar a la meta. */
final class Ritmo {
    private final Random azar;
    private final int minimo;
    private final int maximo;
    private int minutos;
    private int meta;

    Ritmo(Random azar, int minimo, int maximo) {
        this.azar = azar;
        this.minimo = Math.max(1, minimo);
        this.maximo = Math.max(this.minimo, maximo);
        this.meta = sortear();
    }

    boolean pulso(boolean cuenta) {
        if (!cuenta) {
            return false;
        }
        minutos++;
        if (minutos < meta) {
            return false;
        }
        minutos = 0;
        meta = sortear();
        return true;
    }

    /** Si no había nadie que llevara el tiempo, el siguiente minuto vuelve a probar. */
    void aplazar() {
        minutos = Math.max(0, meta - 1);
    }

    int meta() {
        return meta;
    }

    private int sortear() {
        return minimo + azar.nextInt(maximo - minimo + 1);
    }
}
