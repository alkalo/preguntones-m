package com.preguntones.mundos;

/**
 * La cara del grupo, en el espacio de la cámara.
 * +X es la derecha de la pantalla, +Y es arriba y -Z queda delante del ojo.
 * El cartel se gira con la cámara en el cliente, así que no persigue el ratón.
 */
final class Esquina {
    static final float CERCA = 0.9f;
    private static final float LEJOS = 2.4f;
    private static final float PARTE = CERCA / LEJOS;
    static final float ESCALA_CARA = 0.28f * PARTE;
    static final float ESCALA_TEXTO = 0.32f * PARTE;

    private Esquina() {}

    static float[] cara(int fila) {
        return new float[] { izquierda(), arriba(fila), -CERCA };
    }

    static float[] texto(int fila) {
        return new float[] { izquierda() + 0.34f * PARTE, arriba(fila), -CERCA };
    }

    private static float izquierda() {
        return -1.15f * PARTE;
    }

    private static float arriba(int fila) {
        return (0.70f - fila * 0.20f) * PARTE;
    }
}
