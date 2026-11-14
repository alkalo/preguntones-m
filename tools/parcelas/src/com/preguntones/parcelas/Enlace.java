package com.preguntones.parcelas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Une dos parcelas solo si comparten un lado. El hueco del spawn no cuenta:
 * esas casillas no son vecinas y no hay clave que las junte.
 */
public final class Enlace {
    private Enlace() {
    }

    public static String par(Cuadricula cuadricula, int ai, int aj, int bi, int bj) {
        if (cuadricula == null) {
            return null;
        }
        boolean fila = aj == bj && cuadricula.juntas(ai, bi);
        boolean columna = ai == bi && cuadricula.juntas(aj, bj);
        if (fila == columna) {
            return null;
        }
        return canon(ai + "," + aj, bi + "," + bj);
    }

    public static String canon(String a, String b) {
        if (a == null || b == null || a.equals(b)) {
            return null;
        }
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /** Marca común si todas las claves quedan ligadas por los pares. Si falta un lado, null. */
    public static String grupo(List<String> claves, Set<String> pares) {
        if (claves == null || claves.isEmpty()) {
            return null;
        }
        List<String> unicas = new ArrayList<>();
        for (String clave : claves) {
            if (clave == null) {
                return null;
            }
            if (!unicas.contains(clave)) {
                unicas.add(clave);
            }
        }
        if (unicas.size() < 2) {
            return null;
        }
        Set<String> pendientes = new HashSet<>(unicas);
        ArrayDeque<String> cola = new ArrayDeque<>();
        cola.add(unicas.get(0));
        pendientes.remove(unicas.get(0));
        while (!cola.isEmpty()) {
            String actual = cola.removeFirst();
            for (String otro : unicas) {
                if (!pendientes.contains(otro)) {
                    continue;
                }
                String par = canon(actual, otro);
                if (par != null && pares != null && pares.contains(par)) {
                    pendientes.remove(otro);
                    cola.add(otro);
                }
            }
        }
        if (!pendientes.isEmpty()) {
            return null;
        }
        List<String> orden = new ArrayList<>(unicas);
        orden.sort(String::compareTo);
        return String.join("+", orden);
    }
}
