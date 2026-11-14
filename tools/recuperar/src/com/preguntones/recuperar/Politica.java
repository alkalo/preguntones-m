package com.preguntones.recuperar;

import java.util.Locale;
import java.util.Set;

/** Reglas de la contraseña nueva, alineadas con AuthMe. */
public final class Politica {

    private static final Set<String> FACILES = Set.of(
            "123456", "password", "qwerty", "12345", "54321", "123456789", "help");

    private Politica() {
    }

    /** Null si la clave se puede usar. */
    public static String rechazo(String nombre, String clave, String codigo) {
        if (clave == null || clave.length() < 5 || clave.length() > 30) {
            return "La contraseña nueva tiene que tener entre 5 y 30 caracteres.";
        }
        String baja = clave.toLowerCase(Locale.ROOT);
        if (FACILES.contains(baja)) {
            return "Esa contraseña es demasiado fácil. Elige otra.";
        }
        if (nombre != null && baja.equals(nombre.toLowerCase(Locale.ROOT))) {
            return "La contraseña no puede ser tu nombre.";
        }
        if (codigo != null && clave.toUpperCase(Locale.ROOT).replace("-", "").equals(codigo)) {
            return "No uses el código de recuperación como contraseña.";
        }
        return null;
    }
}
