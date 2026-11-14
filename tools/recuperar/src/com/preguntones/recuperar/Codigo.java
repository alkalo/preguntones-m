package com.preguntones.recuperar;

import java.security.SecureRandom;
import java.util.Locale;

/** Código de 8 signos, sin 0, O, 1 ni I, para poder copiarlo sin confundirse. */
public final class Codigo {

    static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LARGO = 8;

    private Codigo() {
    }

    public static String generar(SecureRandom azar) {
        StringBuilder texto = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            texto.append(LETRAS.charAt(azar.nextInt(LETRAS.length())));
        }
        return texto.toString();
    }

    public static String mostrar(String codigo) {
        return codigo.substring(0, 4) + "-" + codigo.substring(4);
    }

    /** Ocho signos válidos, o null si no tiene forma de código. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (limpio.length() != LARGO) {
            return null;
        }
        for (int i = 0; i < limpio.length(); i++) {
            if (LETRAS.indexOf(limpio.charAt(i)) < 0) {
                return null;
            }
        }
        return limpio;
    }
}
