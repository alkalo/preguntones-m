package com.preguntones.parcelas;

/** Cómo se reparte un montón al arrastrarlo por varios huecos. */
final class Reparto {
    private Reparto() {
    }

    /**
     * @param deUnoEnUno clic derecho: uno por hueco, en orden. Si no, el montón se parte
     *                    y el resto se vuelve a repartir. Con menos unidades que huecos, el clic
     *                    izquierdo no deja ninguna: hace falta el derecho.
     */
    static int[] partes(int cantidad, int[] espacio, boolean deUnoEnUno) {
        int[] poner = new int[espacio.length];
        if (cantidad <= 0 || espacio.length == 0) {
            return poner;
        }
        if (deUnoEnUno) {
            for (int i = 0; i < espacio.length && cantidad > 0; i++) {
                if (espacio[i] <= 0) {
                    continue;
                }
                poner[i] = 1;
                cantidad--;
            }
            return poner;
        }
        int utiles = 0;
        for (int hueco : espacio) {
            if (hueco > 0) {
                utiles++;
            }
        }
        if (utiles == 0 || cantidad < utiles) {
            return poner;
        }
        int cada = cantidad / utiles;
        for (int i = 0; i < espacio.length; i++) {
            if (espacio[i] <= 0) {
                continue;
            }
            int toma = Math.min(espacio[i], cada);
            poner[i] = toma;
            cantidad -= toma;
        }
        int guardia = cantidad + espacio.length;
        while (cantidad > 0 && guardia-- > 0) {
            boolean avanzo = false;
            for (int i = 0; i < espacio.length && cantidad > 0; i++) {
                if (poner[i] >= espacio[i]) {
                    continue;
                }
                poner[i]++;
                cantidad--;
                avanzo = true;
            }
            if (!avanzo) {
                break;
            }
        }
        return poner;
    }
}
