package com.preguntones.reinicio;

/** Si el apagado ya empezó y todavía no se avisó, hay que avisar antes de echar a nadie. */
final class Decidir {
    private Decidir() {
    }

    static boolean alApagar(boolean avisado, boolean parando, int gente) {
        return !avisado && parando && gente > 0;
    }
}
