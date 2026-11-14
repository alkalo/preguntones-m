package com.preguntones.parcelas;

/** Cuentas enteras del barrio: el impuesto, el interés y el tope de una compra. */
final class Cuentas {
    static final int IMPUESTO = 10;
    static final int COMPRA_MAX = 96;
    static final int PRECIO_MIN = 1;
    static final int PRECIO_MAX = 48;
    /** De cada cinco duros que costó la parcela, el banco devuelve uno. Las seis quedan en la misma proporción. */
    static final int RECOMPRA = 5;

    private Cuentas() {
    }

    static int impuesto(int total) {
        return impuesto(total, IMPUESTO);
    }

    static int impuesto(int total, int porcentaje) {
        if (total <= 0 || porcentaje <= 0) {
            return 0;
        }
        return total * porcentaje / 100;
    }

    static int paraDueno(int total) {
        return total - impuesto(total);
    }

    /** Redondeo hacia arriba, y como mínimo un duro. */
    static int interes(int capital, int porcentaje) {
        if (capital <= 0 || porcentaje <= 0) {
            return 0;
        }
        int duros = (capital * porcentaje + 99) / 100;
        return Math.max(1, duros);
    }

    static int tope(int stock, int precio) {
        if (stock <= 0 || precio <= 0) {
            return 0;
        }
        int porTope = COMPRA_MAX / precio;
        if (porTope <= 0) {
            return 0;
        }
        return Math.min(stock, porTope);
    }

    /** Duros por unidad, o null si no es un entero entre el mínimo y el máximo. */
    static Integer leerPrecio(String escrito) {
        if (escrito == null) {
            return null;
        }
        String limpio = escrito.trim();
        if (limpio.isEmpty() || limpio.length() > 3) {
            return null;
        }
        for (int i = 0; i < limpio.length(); i++) {
            char cifra = limpio.charAt(i);
            if (cifra < '0' || cifra > '9') {
                return null;
            }
        }
        try {
            int precio = Integer.parseInt(limpio);
            if (precio < PRECIO_MIN || precio > PRECIO_MAX) {
                return null;
            }
            return precio;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** La quinta parte, hacia abajo. Si hubo pago, al menos un duro. */
    static int recompra(int pagado) {
        if (pagado <= 0) {
            return 0;
        }
        return Math.max(1, pagado / RECOMPRA);
    }
}
