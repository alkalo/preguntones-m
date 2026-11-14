package com.preguntones.reinicio;

import java.util.Locale;

/** Reconoce stop y restart, también con barra o con espacio de nombres. */
final class Orden {
    enum Tipo {
        PARAR,
        REINICIAR
    }

    private Orden() {
    }

    static Tipo tipo(String linea) {
        if (linea == null) {
            return null;
        }
        String limpia = linea.trim();
        if (limpia.startsWith("/")) {
            limpia = limpia.substring(1).trim();
        }
        if (limpia.isEmpty()) {
            return null;
        }
        String base = limpia.split("\\s+")[0].toLowerCase(Locale.ROOT);
        int dos = base.lastIndexOf(':');
        if (dos >= 0) {
            base = base.substring(dos + 1);
        }
        return switch (base) {
            case "stop", "end" -> Tipo.PARAR;
            case "restart" -> Tipo.REINICIAR;
            default -> null;
        };
    }
}
