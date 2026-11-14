package com.preguntones.viajes;

import java.util.Locale;

final class Casa {
    private Casa() {
    }

    static boolean es(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        String texto = mensaje.trim().toLowerCase(Locale.ROOT);
        if (!texto.startsWith("/")) {
            return false;
        }
        String base = texto.split(" ")[0];
        if (base.startsWith("/")) {
            base = base.substring(1);
        }
        int dos = base.lastIndexOf(':');
        if (dos >= 0 && dos < base.length() - 1) {
            base = base.substring(dos + 1);
        }
        return switch (base) {
            case "home", "homes", "sethome", "delhome", "createhome", "removehome", "renamehome" -> true;
            default -> false;
        };
    }
}
