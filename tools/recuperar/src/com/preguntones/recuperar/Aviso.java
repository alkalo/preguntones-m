package com.preguntones.recuperar;

/** Texto del mensaje privado. El juego no vuelve a enseñar el código. */
public final class Aviso {

    private Aviso() {
    }

    public static String texto(String nombre, String visible, boolean sustituye) {
        String limpio = nombre == null ? "" : nombre.replaceAll("[\\r\\n`]", "");
        StringBuilder texto = new StringBuilder();
        if (sustituye) {
            texto.append("El código anterior ya no vale.\n\n");
        }
        texto.append("Código de recuperación de ").append(limpio).append(": ").append(visible).append("\n\n");
        texto.append("Solo esta cuenta de Discord puede usarlo. En el juego no se enseña.\n\n");
        texto.append("Si olvidas la contraseña:\n");
        texto.append("1. Entra al servidor.\n");
        texto.append("2. En la ventana pulsa Recuperar Contraseña.\n");
        texto.append("3. Escribe este código y la contraseña nueva, dos veces.\n");
        texto.append("Código: ").append(visible);
        return texto.toString();
    }
}
