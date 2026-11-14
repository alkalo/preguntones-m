package com.preguntones.parcelas;

import java.util.List;
import java.util.Set;

public final class Pruebas {
    public static void main(String[] args) {
        if (!Cupo.cabe(6, 2) || Cupo.cabe(6, 3) || !Cupo.puedeSumarUno(7) || Cupo.puedeSumarUno(8)) {
            throw new IllegalStateException("el cupo de aldeanos es 8 por persona");
        }
        Cuadricula cuadricula = new Cuadricula(25, 20, 16, -18, 17, 20);
        zona(cuadricula, 0, 12, Cuadricula.Zona.SPAWN);
        zona(cuadricula, 16, 8, Cuadricula.Zona.SPAWN);
        if (cuadricula.parcela(16, 8) != null || cuadricula.unir(16, 8) != null) {
            throw new IllegalStateException("la tienda del spawn se puede comprar");
        }
        zona(cuadricula, 0, 30, Cuadricula.Zona.PASILLO);
        zona(cuadricula, 0, -30, Cuadricula.Zona.PASILLO);
        zona(cuadricula, 22, 30, Cuadricula.Zona.PASILLO);
        zona(cuadricula, -22, 30, Cuadricula.Zona.PASILLO);
        if (cuadricula.unir(0, 30) != null || cuadricula.unir(0, -30) != null) {
            throw new IllegalStateException("un pasillo grande se puede unir");
        }
        if (cuadricula.juntas(-1, 0)) {
            throw new IllegalStateException("las parcelas separadas por el spawn cuentan como juntas");
        }
        if (!cuadricula.juntas(0, 1) || !cuadricula.juntas(-2, -1)) {
            throw new IllegalStateException("dos parcelas seguidas no cuentan como juntas");
        }
        Cuadricula.Casilla parcela = cuadricula.parcela(30, 30);
        if (parcela == null || parcela.i != 0 || parcela.j != 0) {
            throw new IllegalStateException("30,30 no es la parcela 0,0");
        }
        zona(cuadricula, 30, 30, Cuadricula.Zona.PARCELA);
        zona(cuadricula, 42, 30, Cuadricula.Zona.CAMINO);
        if (!claves(cuadricula.unir(42, 30)).equals("0,0 1,0")) {
            throw new IllegalStateException("el camino corto no une 0,0 con 1,0: " + claves(cuadricula.unir(42, 30)));
        }
        zona(cuadricula, 42, 42, Cuadricula.Zona.CRUCE);
        if (cuadricula.unir(42, 42) == null || cuadricula.unir(42, 42).size() != 4) {
            throw new IllegalStateException("el cruce no mira a las cuatro parcelas");
        }
        zona(cuadricula, -42, -30, Cuadricula.Zona.CAMINO);
        if (!claves(cuadricula.unir(-42, -30)).equals("-2,-1 -1,-1")) {
            throw new IllegalStateException("el camino negativo no cuadra: " + claves(cuadricula.unir(-42, -30)));
        }
        zona(cuadricula, 1000, 1000, Cuadricula.Zona.FUERA);
        hueco(cuadricula);
        camino();
        cuentas();
        reparto();
        entrada();
        System.out.println("Cuadrícula del barrio: bien");
    }

    private static void hueco(Cuadricula cuadricula) {
        if (Enlace.par(cuadricula, -1, 0, 0, 0) != null || Enlace.par(cuadricula, 0, 0, 0, 0) != null) {
            throw new IllegalStateException("el spawn cuenta como vecino");
        }
        if (Enlace.par(cuadricula, 0, 0, 1, 1) != null || Enlace.par(cuadricula, 0, 0, 2, 0) != null) {
            throw new IllegalStateException("una diagonal o un salto de dos cuentan como vecinas");
        }
        String par = Enlace.par(cuadricula, 0, 0, 1, 0);
        if (!"0,0|1,0".equals(par) || !par.equals(Enlace.par(cuadricula, 1, 0, 0, 0))) {
            throw new IllegalStateException("0,0 y 1,0 no tienen clave estable: " + par);
        }
        for (int x = 40; x <= 45; x++) {
            for (int z = 25; z < 41; z++) {
                Cuadricula.Zona zona = cuadricula.zona(x, z);
                if (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO) {
                    throw new IllegalStateException("el hueco " + x + "," + z + " cae en " + zona);
                }
            }
        }
        Set<String> sueltas = Set.of(par);
        if (Enlace.grupo(List.of("0,0", "1,0"), sueltas) == null) {
            throw new IllegalStateException("dos parcelas unidas no forman grupo");
        }
        if (Enlace.grupo(List.of("0,0", "1,0"), Set.of()) != null) {
            throw new IllegalStateException("sin acuerdo el camino queda unido");
        }
        if (Enlace.grupo(List.of("0,0", "1,0", "0,1", "1,1"), sueltas) != null) {
            throw new IllegalStateException("un solo lado abre el cruce");
        }
        Set<String> cruz = Set.of(
            Enlace.par(cuadricula, 0, 0, 1, 0),
            Enlace.par(cuadricula, 0, 0, 0, 1),
            Enlace.par(cuadricula, 1, 0, 1, 1),
            Enlace.par(cuadricula, 0, 1, 1, 1)
        );
        if (Enlace.grupo(List.of("1,1", "0,0", "0,1", "1,0"), cruz) == null) {
            throw new IllegalStateException("las cuatro unidas no forman grupo");
        }
        System.out.println("Enlaces sin tocar el spawn: bien");
    }

    private static void cuentas() {
        if (Cuentas.impuesto(4) != 0 || Cuentas.impuesto(40) != 4 || Cuentas.paraDueno(40) != 36) {
            throw new IllegalStateException("el 10 % no cuadra");
        }
        if (Cuentas.interes(25, 10) != 3 || Cuentas.interes(100, 30) != 30 || Cuentas.interes(1, 10) != 1) {
            throw new IllegalStateException("el interés no redondea hacia arriba");
        }
        if (Cuentas.tope(64, 4) != 24 || Cuentas.tope(64, 100) != 0 || Cuentas.tope(10, 48) != 2) {
            throw new IllegalStateException("el tope de una compra no cuadra");
        }
        if (!Integer.valueOf(12).equals(Cuentas.leerPrecio("12"))
            || !Integer.valueOf(1).equals(Cuentas.leerPrecio(" 1 "))
            || !Integer.valueOf(48).equals(Cuentas.leerPrecio("48"))
            || Cuentas.leerPrecio("49") != null
            || Cuentas.leerPrecio("500") != null
            || Cuentas.leerPrecio("0") != null
            || Cuentas.leerPrecio("501") != null
            || Cuentas.leerPrecio("12a") != null
            || Cuentas.leerPrecio("12.5") != null
            || Cuentas.leerPrecio("") != null
            || Cuentas.leerPrecio(null) != null) {
            throw new IllegalStateException("el precio de la venta no se lee");
        }
        int precio = 25;
        int[] banco = {5, 11, 19, 29, 41, 55};
        int anterior = 0;
        for (int i = 0; i < banco.length; i++) {
            int vuelta = Cuentas.recompra(precio);
            if (vuelta != banco[i] || vuelta * Cuentas.RECOMPRA > precio || vuelta >= precio / 2 || vuelta < anterior) {
                throw new IllegalStateException("el banco no paga la quinta parte de " + precio + ": " + vuelta);
            }
            anterior = vuelta;
            if (i + 1 < banco.length) {
                precio += 30 + 10 * i;
            }
        }
        if (Cuentas.recompra(0) != 0 || Cuentas.recompra(4) != 1 || Cuentas.recompra(2000) != 400) {
            throw new IllegalStateException("la recompra no cuadra en los bordes");
        }
        System.out.println("Cuentas del barrio: bien");
    }

    private static void reparto() {
        if (!java.util.Arrays.equals(Reparto.partes(64, new int[] {64, 64, 64}, false), new int[] {22, 21, 21})) {
            throw new IllegalStateException("el arrastre no reparte 64 en tres");
        }
        if (!java.util.Arrays.equals(Reparto.partes(4, new int[] {64, 64, 64, 64, 64}, false), new int[] {0, 0, 0, 0, 0})) {
            throw new IllegalStateException("el arrastre izquierdo mete género de menos");
        }
        if (!java.util.Arrays.equals(Reparto.partes(3, new int[] {64, 0, 64, 64}, true), new int[] {1, 0, 1, 1})) {
            throw new IllegalStateException("el arrastre derecho no deja uno en cada hueco");
        }
        if (!java.util.Arrays.equals(Reparto.partes(10, new int[] {4, 64}, false), new int[] {4, 6})) {
            throw new IllegalStateException("el arrastre no respeta el hueco casi lleno");
        }
        System.out.println("Reparto del cofre: bien");
    }

    private static void entrada() {
        if (!Entrada.es("OAK_DOOR") || !Entrada.es("IRON_DOOR") || !Entrada.es("WAXED_OXIDIZED_COPPER_DOOR")) {
            throw new IllegalStateException("una puerta no se abre");
        }
        if (!Entrada.es("OAK_FENCE_GATE") || !Entrada.es("IRON_TRAPDOOR") || !Entrada.es("BAMBOO_TRAPDOOR")) {
            throw new IllegalStateException("una trampilla o una puerta de valla no se abre");
        }
        if (Entrada.es("CHEST") || Entrada.es("TRAPPED_CHEST") || Entrada.es("BARREL")
            || Entrada.es("SHULKER_BOX") || Entrada.es("STONE_BUTTON") || Entrada.es("LEVER")
            || Entrada.es("FURNACE") || Entrada.es("ENDER_CHEST")) {
            throw new IllegalStateException("un cofre cuenta como puerta");
        }
        if (!Entrada.caja("CHEST") || !Entrada.caja("TRAPPED_CHEST") || !Entrada.caja("ENDER_CHEST")
            || !Entrada.caja("COPPER_CHEST") || !Entrada.caja("BARREL") || !Entrada.caja("SHULKER_BOX")
            || !Entrada.caja("FURNACE") || !Entrada.caja("HOPPER") || !Entrada.caja("LECTERN")) {
            throw new IllegalStateException("una caja no cuenta como caja");
        }
        if (Entrada.caja("OAK_DOOR") || Entrada.caja("IRON_TRAPDOOR") || Entrada.caja("OAK_FENCE_GATE")
            || Entrada.caja("STONE_BUTTON")) {
            throw new IllegalStateException("una puerta cuenta como caja");
        }
        if (!Entrada.hierro("IRON_DOOR") || !Entrada.hierro("IRON_TRAPDOOR") || Entrada.hierro("OAK_DOOR")
            || Entrada.hierro("COPPER_DOOR")) {
            throw new IllegalStateException("el hierro no se distingue");
        }
        System.out.println("Entradas de la parcela: bien");
    }

    private static void camino() {
        Camino.Lado ana = new Camino.Lado("ana", true);
        Camino.Lado ajena = new Camino.Lado("ana", false);
        Camino.Lado leo = new Camino.Lado("leo", true);
        Camino.Lado libre = new Camino.Lado(null, false);
        if (Camino.estado(List.of(ana, ana)) != Camino.Estado.SUYO) {
            throw new IllegalStateException("dos lados de la misma persona no son suyos");
        }
        if (Camino.estado(List.of(ajena, ajena)) != Camino.Estado.CERRADO) {
            throw new IllegalStateException("el camino cerrado se abre a un extraño");
        }
        if (Camino.puede(Camino.Estado.CERRADO, true)) {
            throw new IllegalStateException("el admin entra en un camino que sigue unido");
        }
        if (Camino.estado(List.of(ana, new Camino.Lado("leo", false))) != Camino.Estado.BARRIO) {
            throw new IllegalStateException("al vender, el camino no vuelve al barrio");
        }
        if (!Camino.puede(Camino.Estado.BARRIO, true) || Camino.puede(Camino.Estado.BARRIO, false)) {
            throw new IllegalStateException("el admin no recupera el camino vendido, o lo toca cualquiera");
        }
        if (Camino.estado(List.of(leo, leo)) != Camino.Estado.SUYO) {
            throw new IllegalStateException("el comprador no se queda el camino si ya tenía el otro lado");
        }
        if (Camino.estado(List.of(ana, ana, ana, new Camino.Lado("leo", false))) != Camino.Estado.BARRIO) {
            throw new IllegalStateException("el cruce no vuelve al barrio al vender una de las cuatro");
        }
        if (Camino.estado(List.of(ana, ajena)) != Camino.Estado.CERRADO) {
            throw new IllegalStateException("un amigo de un solo lado entra al camino");
        }
        if (Camino.estado(List.of(ana, libre)) != Camino.Estado.BARRIO) {
            throw new IllegalStateException("un lado libre no deja el camino al barrio");
        }
        System.out.println("Caminos de compra y venta: bien");
    }

    private static void zona(Cuadricula cuadricula, int x, int z, Cuadricula.Zona esperada) {
        Cuadricula.Zona zona = cuadricula.zona(x, z);
        if (zona != esperada) {
            throw new IllegalStateException(x + "," + z + " es " + zona + " y tenía que ser " + esperada);
        }
    }

    private static String claves(List<Cuadricula.Casilla> lados) {
        if (lados == null) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        for (Cuadricula.Casilla lado : lados) {
            if (texto.length() > 0) {
                texto.append(' ');
            }
            texto.append(lado.clave());
        }
        return texto.toString();
    }
}
