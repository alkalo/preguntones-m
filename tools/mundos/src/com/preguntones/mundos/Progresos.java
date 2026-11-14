package com.preguntones.mundos;

import java.util.Locale;

/** Duros de los progresos del menú de pausa. Las recetas no entran. */
final class Progresos {
    static final int TAREA = 1;
    static final int OBJETIVO = 3;
    static final int RETO = 8;

    private Progresos() {
    }

    static int duros(String marco) {
        if (marco == null) {
            return TAREA;
        }
        return switch (marco.toLowerCase(Locale.ROOT)) {
            case "challenge" -> RETO;
            case "goal" -> OBJETIVO;
            default -> TAREA;
        };
    }

    static String sufijo(int duros) {
        return duros == 1 ? " · 1 duro" : " · " + duros + " duros";
    }

    static boolean cuenta(String clave) {
        if (clave == null) {
            return false;
        }
        return clave.startsWith("story/")
            || clave.startsWith("nether/")
            || clave.startsWith("end/")
            || clave.startsWith("adventure/")
            || clave.startsWith("husbandry/");
    }
}
