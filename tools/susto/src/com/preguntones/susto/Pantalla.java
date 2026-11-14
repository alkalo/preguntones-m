package com.preguntones.susto;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Una cara a sangre, para estirarla sobre toda la pantalla. */
final class Pantalla {
    private Pantalla() {
    }

    static BufferedImage miedo(int lado) {
        BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(168, 154, 140), lado, lado, new Color(214, 198, 182)));
        g.fillRect(0, 0, lado, lado);
        g.setColor(new Color(96, 74, 68));
        g.fillOval(px(lado, -0.05), px(lado, 0.28), px(lado, 0.55), px(lado, 0.22));
        g.fillOval(px(lado, 0.52), px(lado, 0.34), px(lado, 0.5), px(lado, 0.16));
        ojo(g, lado, -0.02, 0.06, 0.52, 0.46, 0.08, 0.06);
        ojo(g, lado, 0.52, 0.0, 0.46, 0.36, -0.06, 0.08);
        g.setColor(new Color(18, 6, 8));
        g.fillPolygon(
            new int[] { px(lado, 0.08), px(lado, 0.22), px(lado, 0.78), px(lado, 0.94), px(lado, 0.72), px(lado, 0.28) },
            new int[] { px(lado, 0.58), px(lado, 0.52), px(lado, 0.50), px(lado, 0.62), px(lado, 0.98), px(lado, 0.96) },
            6
        );
        g.setColor(new Color(186, 170, 154));
        diente(g, lado, 0.24, 0.54, 0.07, 0.16);
        diente(g, lado, 0.40, 0.52, 0.05, 0.08);
        diente(g, lado, 0.62, 0.51, 0.06, 0.18);
        g.setColor(new Color(12, 4, 5));
        g.fillPolygon(
            new int[] { px(lado, 0.46), px(lado, 0.50), px(lado, 0.54) },
            new int[] { px(lado, 0.02), px(lado, 0.62), px(lado, 0.04) },
            3
        );
        g.dispose();
        grano(imagen, 4L);
        vineta(imagen);
        return imagen;
    }

    private static void ojo(Graphics2D g, int lado, double x, double y, double rx, double ry, double px, double py) {
        g.setColor(new Color(6, 4, 5));
        g.fillOval(px(lado, x), px(lado, y), px(lado, rx), px(lado, ry));
        g.setColor(new Color(236, 224, 200));
        g.fillOval(px(lado, x + rx * 0.38 + px), px(lado, y + ry * 0.36 + py), px(lado, rx * 0.16), px(lado, ry * 0.14));
    }

    private static void diente(Graphics2D g, int lado, double x, double y, double w, double h) {
        g.fillRect(px(lado, x), px(lado, y), Math.max(2, px(lado, w)), Math.max(2, px(lado, h)));
    }

    private static void grano(BufferedImage imagen, long semilla) {
        Random azar = new Random(semilla);
        int lado = imagen.getWidth();
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                int rgb = imagen.getRGB(x, y);
                int delta = azar.nextInt(19) - 9;
                int r = canal(rgb, 16) + delta;
                int g = canal(rgb, 8) + delta;
                int b = canal(rgb, 0) + delta;
                imagen.setRGB(x, y, rgb(r, g, b));
            }
        }
    }

    private static void vineta(BufferedImage imagen) {
        int lado = imagen.getWidth();
        double centro = (lado - 1) / 2.0;
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                double dx = (x - centro) / centro;
                double dy = (y - centro) / centro;
                double lejos = dx * dx + dy * dy;
                if (lejos < 0.55) {
                    continue;
                }
                int cuanto = (int) ((lejos - 0.55) * 90);
                int rgb = imagen.getRGB(x, y);
                imagen.setRGB(x, y, rgb(canal(rgb, 16) - cuanto, canal(rgb, 8) - cuanto, canal(rgb, 0) - cuanto));
            }
        }
    }

    private static int px(int lado, double parte) {
        return (int) Math.round(lado * parte);
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
}
