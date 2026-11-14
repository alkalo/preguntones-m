package com.preguntones.viajes;

import java.util.List;

final class NombreBandera {
    static final int MAXIMO = 5;
    static final int LARGO = 16;

    private NombreBandera() {
    }

    static boolean cabe(int puestas) {
        return puestas < MAXIMO;
    }

    static String elegir(String escrito, List<String> ya) {
        String limpio = limpiar(escrito);
        if (!limpio.isEmpty() && !limpio.equalsIgnoreCase("bandera")) {
            return limpio;
        }
        int n = 1;
        while (ocupado(ya, n == 1 ? "Bandera" : "Bandera " + n)) {
            n++;
        }
        return n == 1 ? "Bandera" : "Bandera " + n;
    }

    private static boolean ocupado(List<String> ya, String nombre) {
        if (ya == null) {
            return false;
        }
        for (String otro : ya) {
            if (nombre.equalsIgnoreCase(otro)) {
                return true;
            }
        }
        return false;
    }

    private static String limpiar(String escrito) {
        if (escrito == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < escrito.length(); i++) {
            char c = escrito.charAt(i);
            if (c == '§' && i + 1 < escrito.length()) {
                i++;
                continue;
            }
            if (c < ' ') {
                continue;
            }
            out.append(c);
        }
        String texto = out.toString().trim().replaceFirst(" · \\d+ duros$", "").trim();
        if (texto.length() > LARGO) {
            texto = texto.substring(0, LARGO).trim();
        }
        return texto;
    }
}
