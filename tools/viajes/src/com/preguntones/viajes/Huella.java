package com.preguntones.viajes;

/** La última muerte y lo que cuesta volver. Los duros se destruyen al llegar. */
public final class Huella {
    /** Dos banderas. Por encima de la parcela 2 (55) y del préstamo sin aval (50). Por debajo de la parcela 3 (95). */
    public static final int PRECIO = 80;
    private static final int JUNTO = 16;

    public final String mundo;
    public final int x;
    public final int y;
    public final int z;

    public Huella(String mundo, int x, int y, int z) {
        this.mundo = mundo;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static boolean cuenta(String mundo) {
        return mundo != null && !mundo.equals("duelo") && !mundo.equals("xo");
    }

    public static Huella alMorir(Huella anterior, String mundo, int x, int y, int z) {
        if (!cuenta(mundo)) {
            return anterior;
        }
        return new Huella(mundo, x, y, z);
    }

    public static Paso pedir(Huella huella, int saldo) {
        if (huella == null) {
            return Paso.SIN_SITIO;
        }
        if (saldo < PRECIO) {
            return Paso.SIN_DUROS;
        }
        return Paso.ESPERAR;
    }

    public static Paso llegar(Huella pedida, Huella ahora, int saldo) {
        if (pedida == null || ahora == null || !pedida.misma(ahora)) {
            return Paso.SIN_SITIO;
        }
        if (saldo < PRECIO) {
            return Paso.SIN_DUROS;
        }
        return Paso.COBRAR;
    }

    public boolean misma(Huella otra) {
        return otra != null && mundo.equals(otra.mundo) && x == otra.x && y == otra.y && z == otra.z;
    }

    public static boolean junto(Huella huella, String mundo, int x, int y, int z) {
        if (huella == null || mundo == null || !huella.mundo.equals(mundo)) {
            return false;
        }
        int dx = huella.x - x;
        int dy = huella.y - y;
        int dz = huella.z - z;
        return dx * dx + dy * dy + dz * dz <= JUNTO;
    }

    public static Huella leer(String texto) {
        if (texto == null || texto.isEmpty()) {
            return null;
        }
        String[] partes = texto.split(",");
        if (partes.length != 4 || !cuenta(partes[0])) {
            return null;
        }
        try {
            return new Huella(partes[0], Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String texto() {
        return mundo + "," + x + "," + y + "," + z;
    }

    public static String sitio(String mundo) {
        if (mundo == null) {
            return "";
        }
        return switch (mundo) {
            case "world" -> "barrio";
            case "survival" -> "survival";
            case "survival_nether" -> "nether";
            case "fin" -> "end";
            default -> mundo;
        };
    }

    public enum Paso {
        SIN_SITIO, SIN_DUROS, ESPERAR, COBRAR
    }
}
