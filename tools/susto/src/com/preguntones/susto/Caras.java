package com.preguntones.susto;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Caras propias, distintas según la semilla. No hay foto de nadie. */
final class Caras {
    static final int LADO = 128;

    private static final int FONDO = rgb(2, 1, 3);
    private static final int SOMBRA = rgb(8, 6, 10);
    private static final int PUPILA = rgb(232, 220, 196);

    private static final int[] PIELES = {
        rgb(168, 176, 142),
        rgb(186, 174, 154),
        rgb(154, 168, 150),
        rgb(198, 186, 168)
    };

    private Caras() {
    }

    static BufferedImage pintar(long semilla) {
        Rasgos rasgos = rasgos(semilla);
        BufferedImage imagen = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                imagen.setRGB(x, y, color(x, y, rasgos));
            }
        }
        return imagen;
    }

    static BufferedImage ampliar(BufferedImage base, int lado) {
        BufferedImage grande = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                grande.setRGB(x, y, base.getRGB(x * base.getWidth() / lado, y * base.getHeight() / lado));
            }
        }
        return grande;
    }

    static int firma(BufferedImage imagen) {
        int suma = 1;
        for (int y = 0; y < imagen.getHeight(); y += 2) {
            for (int x = 0; x < imagen.getWidth(); x += 2) {
                suma = suma * 31 + (imagen.getRGB(x, y) & 0xFFFFFF);
            }
        }
        return suma;
    }

    static int claros(BufferedImage imagen) {
        int n = 0;
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                int rgb = imagen.getRGB(x, y) & 0xFFFFFF;
                int luma = ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
                if (luma > 380) {
                    n++;
                }
            }
        }
        return n;
    }

    private static Rasgos rasgos(long semilla) {
        Random azar = new Random(semilla);
        Rasgos rasgos = new Rasgos();
        rasgos.semilla = semilla;
        rasgos.piel = PIELES[azar.nextInt(PIELES.length)];
        rasgos.cx = 60 + azar.nextInt(9);
        rasgos.cy = 62 + azar.nextInt(7);
        rasgos.rx = 62;
        rasgos.ry = 62;
        rasgos.lado = azar.nextBoolean() ? 1 : -1;
        rasgos.ojoIx = rasgos.cx - 18 - azar.nextInt(4);
        rasgos.ojoIy = rasgos.cy - 14 + azar.nextInt(5);
        rasgos.ojoIrx = 14 + azar.nextInt(6);
        rasgos.ojoIry = 18 + azar.nextInt(6);
        rasgos.ojoDx = rasgos.cx + 16 + azar.nextInt(5);
        rasgos.ojoDy = rasgos.cy - 18 + azar.nextInt(6);
        rasgos.ojoDrx = 8 + azar.nextInt(5);
        rasgos.ojoDry = 12 + azar.nextInt(6);
        rasgos.pupIx = rasgos.ojoIx + azar.nextInt(7) - 3;
        rasgos.pupIy = rasgos.ojoIy + azar.nextInt(7) - 2;
        rasgos.pupDx = rasgos.ojoDx + azar.nextInt(5) - 1;
        rasgos.pupDy = rasgos.ojoDy + azar.nextInt(5) - 3;
        rasgos.bocaX = rasgos.cx + azar.nextInt(9) - 4;
        rasgos.bocaY = rasgos.cy + 10 + azar.nextInt(4);
        rasgos.bocaRx = 18 + azar.nextInt(8);
        rasgos.bocaAlto = 16 + azar.nextInt(8);
        rasgos.grieta = rasgos.cx - 20 + azar.nextInt(40);
        return rasgos;
    }

    private static int color(int x, int y, Rasgos rasgos) {
        int color = rasgos.piel;
        int dx = x - rasgos.cx;
        int dy = y - rasgos.cy;
        int lejos = dx * dx + dy * dy;
        if (lejos > 46 * 46) {
            color = restar(color, (lejos - 46 * 46) / 70);
        }
        if ((grano(x / 5, y / 5, rasgos.semilla) & 7) == 0) {
            color = restar(color, 36);
        }
        if (x * rasgos.lado > rasgos.cx * rasgos.lado) {
            color = restar(color, Math.abs(x - rasgos.cx) / 4);
        }
        if (y < rasgos.cy - 6) {
            color = restar(color, (rasgos.cy - y) / 6);
        }
        if (ojera(x, y, rasgos)) {
            color = restar(rasgos.piel, 48);
        }
        if (ceja(x, y, rasgos) || ojo(x, y, rasgos)) {
            color = SOMBRA;
        }
        if (pupila(x, y, rasgos)) {
            color = PUPILA;
        }
        if (boca(x, y, rasgos)) {
            color = SOMBRA;
            if (diente(x, y, rasgos)) {
                color = restar(rasgos.piel, 8);
            }
        }
        if (grieta(x, y, rasgos)) {
            color = SOMBRA;
        }
        return color;
    }

    private static boolean cabeza(int x, int y, Rasgos rasgos) {
        return elipse(x, y, rasgos.cx, rasgos.cy, rasgos.rx, rasgos.ry)
            || elipse(x, y, rasgos.cx - 12, rasgos.cy + 10, rasgos.rx - 10, rasgos.ry - 14)
            || elipse(x, y, rasgos.cx + 16, rasgos.cy - 6, rasgos.rx - 18, rasgos.ry - 12);
    }

    private static boolean borde(int x, int y, Rasgos rasgos) {
        return elipse(x, y, rasgos.cx, rasgos.cy, rasgos.rx + 3, rasgos.ry + 3)
            || elipse(x, y, rasgos.cx - 12, rasgos.cy + 10, rasgos.rx - 7, rasgos.ry - 11);
    }

    private static boolean ojera(int x, int y, Rasgos rasgos) {
        return elipse(x, y, rasgos.ojoIx, rasgos.ojoIy + rasgos.ojoIry - 2, rasgos.ojoIrx + 3, 6)
            || elipse(x, y, rasgos.ojoDx, rasgos.ojoDy + rasgos.ojoDry - 1, rasgos.ojoDrx + 3, 5);
    }

    private static boolean ceja(int x, int y, Rasgos rasgos) {
        int izq = rasgos.ojoIy - rasgos.ojoIry - 3 + (x - rasgos.ojoIx) / 3;
        int der = rasgos.ojoDy - rasgos.ojoDry - 2 - (x - rasgos.ojoDx) / 5;
        boolean una = Math.abs(x - rasgos.ojoIx) <= rasgos.ojoIrx + 2 && Math.abs(y - izq) <= 1;
        boolean otra = Math.abs(x - rasgos.ojoDx) <= rasgos.ojoDrx + 2 && Math.abs(y - der) <= 1;
        return una || otra;
    }

    private static boolean ojo(int x, int y, Rasgos rasgos) {
        return elipse(x, y, rasgos.ojoIx, rasgos.ojoIy, rasgos.ojoIrx, rasgos.ojoIry)
            || elipse(x, y, rasgos.ojoDx, rasgos.ojoDy, rasgos.ojoDrx, rasgos.ojoDry);
    }

    private static boolean pupila(int x, int y, Rasgos rasgos) {
        return elipse(x, y, rasgos.pupIx, rasgos.pupIy, 2, 3)
            || elipse(x, y, rasgos.pupDx, rasgos.pupDy, 2, 2);
    }

    private static boolean boca(int x, int y, Rasgos rasgos) {
        int dx = x - rasgos.bocaX;
        if (Math.abs(dx) > rasgos.bocaRx) {
            return false;
        }
        int onda = (int) Math.round(Math.sin((x + rasgos.semilla % 13) * 0.45) * 5);
        int techo = rasgos.bocaY - 6 - onda / 2;
        int suelo = rasgos.bocaY + rasgos.bocaAlto + onda + Math.abs(dx) / 8;
        return y >= techo && y <= suelo;
    }

    private static boolean diente(int x, int y, Rasgos rasgos) {
        int columna = Math.floorMod(grano(x, 1, rasgos.semilla), 9);
        int largo = 3 + Math.floorMod(grano(x, 2, rasgos.semilla), 6);
        return columna < 3 && y >= rasgos.bocaY - 6 && y < rasgos.bocaY - 6 + largo;
    }

    private static boolean grieta(int x, int y, Rasgos rasgos) {
        int desvio = (y % 8) - 3;
        return Math.abs(x - (rasgos.grieta + desvio)) <= 1 && y > rasgos.cy - 28 && y < rasgos.bocaY + 4;
    }

    private static boolean elipse(int x, int y, int cx, int cy, int rx, int ry) {
        if (rx <= 0 || ry <= 0) {
            return false;
        }
        double dx = (x - cx) / (double) rx;
        double dy = (y - cy) / (double) ry;
        return dx * dx + dy * dy <= 1.0;
    }

    private static int grano(int x, int y, long semilla) {
        long n = semilla ^ (x * 734287L) ^ (y * 912271L);
        n ^= n << 13;
        n ^= n >>> 17;
        return (int) n;
    }

    private static int restar(int color, int cuanto) {
        return rgb(canal(color, 16) - cuanto, canal(color, 8) - cuanto, canal(color, 0) - cuanto);
    }

    private static int canal(int color, int desplazamiento) {
        return (color >> desplazamiento) & 255;
    }

    private static int rgb(int r, int g, int b) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }

    private static final class Rasgos {
        private long semilla;
        private int piel;
        private int cx;
        private int cy;
        private int rx;
        private int ry;
        private int lado;
        private int ojoIx;
        private int ojoIy;
        private int ojoIrx;
        private int ojoIry;
        private int ojoDx;
        private int ojoDy;
        private int ojoDrx;
        private int ojoDry;
        private int pupIx;
        private int pupIy;
        private int pupDx;
        private int pupDy;
        private int bocaX;
        private int bocaY;
        private int bocaRx;
        private int bocaAlto;
        private int grieta;
    }
}
