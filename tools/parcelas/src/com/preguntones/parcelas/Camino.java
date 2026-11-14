package com.preguntones.parcelas;

import java.util.List;

/**
 * El camino estrecho entre parcelas es de quien tiene todos los lados.
 * Si una venta rompe esa unión, el tramo vuelve al barrio:
 * nadie lo toca, salvo quien administra.
 */
public final class Camino {
    public enum Estado {
        SUYO, CERRADO, BARRIO
    }

    public static final class Lado {
        public final String dueno;
        public final boolean entra;

        public Lado(String dueno, boolean entra) {
            this.dueno = dueno;
            this.entra = entra;
        }
    }

    private Camino() {
    }

    public static Estado estado(List<Lado> lados) {
        if (lados == null || lados.isEmpty()) {
            return Estado.BARRIO;
        }
        boolean entra = true;
        boolean cerrado = true;
        String dueno = null;
        for (Lado lado : lados) {
            if (lado == null || lado.dueno == null) {
                entra = false;
                cerrado = false;
                continue;
            }
            if (!lado.entra) {
                entra = false;
            }
            if (dueno == null) {
                dueno = lado.dueno;
            } else if (!dueno.equals(lado.dueno)) {
                cerrado = false;
            }
        }
        if (cerrado && entra) {
            return Estado.SUYO;
        }
        if (cerrado) {
            return Estado.CERRADO;
        }
        return Estado.BARRIO;
    }

    public static boolean puede(Estado estado, boolean admin) {
        if (estado == Estado.SUYO) {
            return true;
        }
        return estado == Estado.BARRIO && admin;
    }
}
