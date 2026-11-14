package com.preguntones.xo;

/** Cuánto se lleva quien gana una ronda: 8 duros, da igual cuánta gente hubiera en la pista. */
final class Bote {
    static final int PREMIO = 8;

    private Bote() {
    }

    static int premio(int personas) {
        return PREMIO;
    }
}
