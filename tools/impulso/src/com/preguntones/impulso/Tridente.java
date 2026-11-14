package com.preguntones.impulso;

/**
 * Cuándo un aviso de movimiento es el lanzamiento del tridente y no un vuelo.
 * El arranque dura un instante: sin la confirmación del servidor no se alarga.
 */
public final class Tridente {

    public static final long VENTANA_MS = 1300L;
    public static final long ARRANQUE_MS = 150L;
    public static final long HUECO_MS = 350L;
    public static final double TOPE = 6.0;
    public static final double ARRANQUE_MIN = 0.1;

    private Tridente() {
    }

    public static final class Memoria {
        public long giroHasta;
        public long arranqueHasta;
        public long ultimoArranque;
    }

    public static boolean ignorar(boolean puede, int nivel, boolean girando, long ahora, Memoria memoria, String chequeo, double desfase) {
        Tipo tipo = tipo(chequeo);
        if (tipo == Tipo.OTRO) {
            return false;
        }
        if (girando || ahora <= memoria.giroHasta) {
            return tipo != Tipo.SIMULACION || desfase <= TOPE;
        }
        if (tipo == Tipo.LENTITUD) {
            return puede && nivel > 0;
        }
        if (!puede || nivel <= 0) {
            return false;
        }
        if (ahora <= memoria.arranqueHasta) {
            return tipo != Tipo.SIMULACION || desfase <= TOPE;
        }
        if (ahora - memoria.ultimoArranque < HUECO_MS) {
            return false;
        }
        if (tipo == Tipo.SIMULACION && !(desfase >= ARRANQUE_MIN && desfase <= TOPE)) {
            return false;
        }
        memoria.ultimoArranque = ahora;
        memoria.arranqueHasta = ahora + ARRANQUE_MS;
        return true;
    }

    private static Tipo tipo(String chequeo) {
        if (chequeo == null) {
            return Tipo.OTRO;
        }
        return switch (chequeo) {
            case "Simulation" -> Tipo.SIMULACION;
            case "GroundSpoof", "NoFall" -> Tipo.SUELO;
            case "NoSlow" -> Tipo.LENTITUD;
            default -> Tipo.OTRO;
        };
    }

    private enum Tipo {
        SIMULACION,
        SUELO,
        LENTITUD,
        OTRO
    }
}
