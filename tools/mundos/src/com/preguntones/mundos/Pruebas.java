package com.preguntones.mundos;

public final class Pruebas {
    public static void main(String[] args) {
        if (!Ciclo.pideDias(1) || Ciclo.pideBastion(1) || Ciclo.pideCiudad(1)) {
            throw new IllegalStateException("el ciclo 1 es solo la espera y los blazes");
        }
        if (Ciclo.pideDias(2) || !Ciclo.pideBastion(2) || !Ciclo.pideCiudad(2)) {
            throw new IllegalStateException("el ciclo 2 pide bastión y ciudad, sin espera");
        }
        if (Ciclo.pideBastion(3) || !Ciclo.pideCiudad(3)) {
            throw new IllegalStateException("el ciclo 3 es la ciudad");
        }
        if (!Ciclo.pideBastion(4) || Ciclo.pideCiudad(4)) {
            throw new IllegalStateException("el ciclo 4 es el bastión");
        }
        if (Ciclo.pideBastion(5) || !Ciclo.pideCiudad(5) || !Ciclo.pideBastion(6)) {
            throw new IllegalStateException("a partir del 3 se turnan");
        }
        if (Ciclo.normalizar(0) != 1) {
            throw new IllegalStateException("un ciclo vacío cuenta como el primero");
        }
        if (Pelea.dano(true, false, true, 40) != 0) {
            throw new IllegalStateException("una explosión no daña al dragón");
        }
        if (Pelea.dano(false, true, true, 20) != 1) {
            throw new IllegalStateException("con cristales el golpe casi no entra");
        }
        if (Pelea.dano(false, false, false, 20) != 2) {
            throw new IllegalStateException("en el aire el golpe tiene techo bajo");
        }
        if (Pelea.dano(false, false, true, 20) != 6) {
            throw new IllegalStateException("posado, el golpe tiene techo");
        }
        if (Pelea.dano(false, false, true, 3) != 3) {
            throw new IllegalStateException("un golpe pequeño se queda como es");
        }
        if (!Pelea.posado("BREATH_ATTACK") || Pelea.posado("CIRCLING")) {
            throw new IllegalStateException("solo cuenta posado cuando está en la fuente");
        }
        if (Progresos.duros("task") != 1 || Progresos.duros("GOAL") != 3 || Progresos.duros("challenge") != 8) {
            throw new IllegalStateException("el marco del progreso no cobra lo dicho");
        }
        if (!Progresos.sufijo(1).equals(" · 1 duro") || !Progresos.sufijo(8).equals(" · 8 duros")) {
            throw new IllegalStateException("el progreso no enseña los duros");
        }
        if (!Progresos.cuenta("story/mine_stone") || Progresos.cuenta("recipes/misc/stick") || Progresos.cuenta(null)) {
            throw new IllegalStateException("el progreso cuenta recetas o se deja fuera la historia");
        }
        laEsquinaNoSigueLaCamara();
        if (Fuego.radio(true) != Fuego.RADIO_SALVAJE || Fuego.radio(false) != Fuego.RADIO_QUIETO) {
            throw new IllegalStateException("el fuego no queda encendido solo en el salvaje");
        }
        if (!Fuego.puedeEncender(true) || Fuego.puedeEncender(false)) {
            throw new IllegalStateException("el barrio puede encenderse");
        }
        System.out.println("Ciclo y pelea: bien");
    }

    private static void laEsquinaNoSigueLaCamara() {
        float[] arriba = Esquina.cara(0);
        float[] abajo = Esquina.cara(1);
        if (arriba[0] >= 0 || arriba[2] >= 0 || arriba[1] <= abajo[1]) {
            throw new IllegalStateException("la cara no queda arriba a la izquierda, delante del ojo");
        }
        float[] letra = Esquina.texto(0);
        if (letra[0] <= arriba[0] || letra[1] != arriba[1] || letra[2] != arriba[2]) {
            throw new IllegalStateException("el nombre no queda a la derecha de la cara");
        }
        if (Esquina.cara(3)[1] >= Esquina.cara(2)[1]) {
            throw new IllegalStateException("las filas no bajan");
        }
        if (Esquina.ESCALA_CARA <= 0 || Esquina.ESCALA_CARA >= 0.2f || Esquina.ESCALA_TEXTO <= Esquina.ESCALA_CARA) {
            throw new IllegalStateException("el marco sale demasiado grande para ir junto al ojo");
        }
        if (Math.abs(arriba[2]) > 1.2f) {
            throw new IllegalStateException("el marco queda tan lejos que un bloque se lo come");
        }
    }
}
