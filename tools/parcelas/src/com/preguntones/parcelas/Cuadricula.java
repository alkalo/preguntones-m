package com.preguntones.parcelas;

import java.util.List;

/**
 * El spawn y los pasillos anchos no se venden.
 * Solo la hierba es parcela. El camino de cuatro bloques entre dos parcelas
 * se puede quitar si los dos lados son de la misma persona,
 * o si los dos dueños han aceptado unirlos. El hueco que separa el spawn no es vecino.
 */
public final class Cuadricula {
    public enum Zona {
        SPAWN, PASILLO, PARCELA, CAMINO, CRUCE, FUERA
    }

    private final int primero;
    private final int salto;
    private final int lado;
    private final int desde;
    private final int hasta;
    private final int plaza;

    public Cuadricula(int primero, int salto, int lado, int desde, int hasta, int plaza) {
        this.primero = primero;
        this.salto = salto;
        this.lado = lado;
        this.desde = desde;
        this.hasta = hasta;
        this.plaza = plaza;
    }

    public int origen(int indice) {
        if (indice >= 0) {
            return primero + indice * salto;
        }
        return -primero - lado + 1 - (-indice - 1) * salto;
    }

    public boolean juntas(int a, int b) {
        return Math.abs(origen(a) - origen(b)) == salto;
    }

    public Casilla parcela(int x, int z) {
        if (Math.abs(x) <= plaza && Math.abs(z) <= plaza) {
            return null;
        }
        Integer i = indiceEn(x);
        Integer j = indiceEn(z);
        if (i == null || j == null) {
            return null;
        }
        return new Casilla(i, j);
    }

    public List<Casilla> unir(int x, int z) {
        Eje ex = eje(x);
        Eje ez = eje(z);
        if (ex.tipo == Tipo.PLOT && ez.tipo == Tipo.CAMINO) {
            return List.of(new Casilla(ex.indice, ez.a), new Casilla(ex.indice, ez.b));
        }
        if (ez.tipo == Tipo.PLOT && ex.tipo == Tipo.CAMINO) {
            return List.of(new Casilla(ex.a, ez.indice), new Casilla(ex.b, ez.indice));
        }
        if (ex.tipo == Tipo.CAMINO && ez.tipo == Tipo.CAMINO) {
            return List.of(
                    new Casilla(ex.a, ez.a),
                    new Casilla(ex.b, ez.a),
                    new Casilla(ex.a, ez.b),
                    new Casilla(ex.b, ez.b));
        }
        return null;
    }

    public Zona zona(int x, int z) {
        if (parcela(x, z) != null) {
            return Zona.PARCELA;
        }
        List<Casilla> lados = unir(x, z);
        if (lados != null) {
            return lados.size() == 4 ? Zona.CRUCE : Zona.CAMINO;
        }
        if (Math.abs(x) <= plaza && Math.abs(z) <= plaza) {
            return Zona.SPAWN;
        }
        Eje ex = eje(x);
        Eje ez = eje(z);
        if (ex.tipo == Tipo.PLAZA || ex.tipo == Tipo.ANILLO || ez.tipo == Tipo.PLAZA || ez.tipo == Tipo.ANILLO) {
            return Zona.PASILLO;
        }
        return Zona.FUERA;
    }

    private Integer indiceEn(int n) {
        for (int i = desde; i <= hasta; i++) {
            int origen = origen(i);
            if (n >= origen && n < origen + lado) {
                return i;
            }
        }
        return null;
    }

    private Eje eje(int n) {
        if (Math.abs(n) <= plaza) {
            return Eje.plaza();
        }
        Integer idx = indiceEn(n);
        if (idx != null) {
            return Eje.plot(idx);
        }
        int ancho = salto - lado;
        for (int i = desde; i < hasta; i++) {
            int fin = origen(i) + lado;
            int sig = origen(i + 1);
            if (sig - fin == ancho && n >= fin && n < sig) {
                return Eje.camino(i, i + 1);
            }
        }
        if ((n > plaza && n < primero) || (n < -plaza && n >= origen(-1) + lado)) {
            return Eje.anillo();
        }
        return Eje.fuera();
    }

    public static final class Casilla {
        public final int i;
        public final int j;

        public Casilla(int i, int j) {
            this.i = i;
            this.j = j;
        }

        public String clave() {
            return i + "," + j;
        }
    }

    private enum Tipo {
        PLOT, CAMINO, ANILLO, PLAZA, FUERA
    }

    private static final class Eje {
        final Tipo tipo;
        final int a;
        final int b;
        final int indice;

        private Eje(Tipo tipo, int a, int b, int indice) {
            this.tipo = tipo;
            this.a = a;
            this.b = b;
            this.indice = indice;
        }

        static Eje plot(int indice) {
            return new Eje(Tipo.PLOT, 0, 0, indice);
        }

        static Eje camino(int a, int b) {
            return new Eje(Tipo.CAMINO, a, b, 0);
        }

        static Eje anillo() {
            return new Eje(Tipo.ANILLO, 0, 0, 0);
        }

        static Eje plaza() {
            return new Eje(Tipo.PLAZA, 0, 0, 0);
        }

        static Eje fuera() {
            return new Eje(Tipo.FUERA, 0, 0, 0);
        }
    }
}
