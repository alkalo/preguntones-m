package com.preguntones.mundos;

/**
 * El dragón de la pelea tiene más vida.
 * Con cristales en las torres casi no entra el golpe.
 * En el aire entra poco. Posado, el golpe tiene techo.
 * Las explosiones no le hacen nada.
 */
public final class Pelea {

    public static final double VIDA = 400;
    public static final double CON_CRISTALES = 1;
    public static final double EN_AIRE = 2;
    public static final double POSADO = 6;
    public static final long GOLPE_RECIENTE_MS = 30_000L;

    private Pelea() {
    }

    public static double dano(boolean explosion, boolean cristales, boolean posado, double recibido) {
        if (explosion || recibido <= 0) {
            return 0;
        }
        if (cristales) {
            return Math.min(CON_CRISTALES, recibido);
        }
        if (!posado) {
            return Math.min(EN_AIRE, recibido);
        }
        return Math.min(POSADO, recibido);
    }

    public static boolean posado(String fase) {
        return "LAND_ON_PORTAL".equals(fase)
            || "BREATH_ATTACK".equals(fase)
            || "SEARCH_FOR_BREATH_ATTACK_TARGET".equals(fase)
            || "ROAR_BEFORE_ATTACK".equals(fase)
            || "LEAVE_PORTAL".equals(fase);
    }
}
