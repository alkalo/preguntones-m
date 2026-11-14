package com.preguntones.reinicio;

/**
 * Anchos de la fuente vanilla, en píxeles.
 * El título se dibuja a ×4 y el subtítulo a ×2, con la misma escala en los dos ejes.
 * La ventana mínima del juego mide 320 px: por debajo de estos topes el texto
 * cabe en el centro y no se recorta ni se aplasta.
 */
final class Ancho {
    static final int TITULO = 76;
    static final int SUBTITULO = 150;

    private static final int[] ASCII = {
        4, 2, 4, 6, 6, 6, 6, 2, 4, 4, 4, 6, 2, 6, 2, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
        7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
        3, 6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6,
        6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 4, 2, 4, 7
    };

    private Ancho() {
    }

    static int de(String texto) {
        int suma = 0;
        for (int i = 0; i < texto.length(); i++) {
            int paso = glifo(texto.charAt(i));
            if (paso < 0) {
                throw new IllegalArgumentException("Sin medida: " + texto.charAt(i));
            }
            suma += paso;
        }
        return suma;
    }

    static boolean cabe(String titulo, String subtitulo) {
        return de(titulo) <= TITULO && de(subtitulo) <= SUBTITULO;
    }

    private static int glifo(char c) {
        if (c >= 32 && c < 32 + ASCII.length) {
            return ASCII[c - 32];
        }
        return switch (c) {
            case 'á', 'é', 'ó', 'ú', 'ü', 'ñ', 'Á', 'É', 'Ó', 'Ú', 'Ü', 'Ñ' -> 6;
            case 'í' -> 3;
            case 'Í' -> 4;
            default -> -1;
        };
    }
}
