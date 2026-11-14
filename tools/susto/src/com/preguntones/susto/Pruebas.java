package com.preguntones.susto;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;

public final class Pruebas {
    public static void main(String[] args) throws Exception {
        ritmo();
        elegir();
        caras();
        gritos();
        if (args.length > 0) {
            File dir = new File(args[0]);
            if (!dir.mkdirs() && !dir.isDirectory()) {
                throw new IllegalStateException("No pude crear " + dir);
            }
            long[] semillas = {11L, 29L, 47L, 71L, 97L, 131L};
            for (int i = 0; i < semillas.length; i++) {
                ImageIO.write(Caras.ampliar(Caras.pintar(semillas[i]), 256), "png", new File(dir, "susto_" + i + ".png"));
            }
            ImageIO.write(Pantalla.miedo(1024), "png", new File(dir, "miedo.png"));
        }
        System.out.println("Susto ok");
    }

    private static void ritmo() {
        Ritmo fijo = new Ritmo(new Random(1), 3, 3);
        if (fijo.meta() != 3) {
            throw new IllegalStateException("La meta fija no es 3");
        }
        if (fijo.pulso(false) || fijo.pulso(false)) {
            throw new IllegalStateException("Sin gente no cuenta");
        }
        if (fijo.pulso(true) || fijo.pulso(true) || !fijo.pulso(true)) {
            throw new IllegalStateException("Tenia que saltar al tercer minuto");
        }
        if (fijo.pulso(true) || fijo.pulso(true) || !fijo.pulso(true)) {
            throw new IllegalStateException("El siguiente tampoco espera tres minutos");
        }
        Ritmo hora = new Ritmo(new Random(2), 60, 60);
        for (int i = 0; i < 59; i++) {
            if (hora.pulso(true)) {
                throw new IllegalStateException("Saltó antes de la hora");
            }
        }
        if (!hora.pulso(true)) {
            throw new IllegalStateException("No saltó a la hora");
        }
        hora.aplazar();
        if (!hora.pulso(true)) {
            throw new IllegalStateException("Sin gente que lleve una hora tenía que reintentar");
        }
        Ritmo ancho = new Ritmo(new Random(2), 60, 60);
        for (int i = 0; i < 40; i++) {
            int meta = ancho.meta();
            if (meta != 60) {
                throw new IllegalStateException("Meta fuera de rango: " + meta);
            }
            boolean toco = false;
            for (int m = 0; m < meta; m++) {
                toco = ancho.pulso(true);
            }
            if (!toco) {
                throw new IllegalStateException("No salto al llegar a la meta");
            }
        }
    }

    private static void elegir() {
        UUID a = new UUID(0, 1);
        UUID b = new UUID(0, 2);
        for (int i = 0; i < 30; i++) {
            if (!b.equals(Elegir.uno(List.of(a, b), a, new Random(i)))) {
                throw new IllegalStateException("Repitio al ultimo habiendo otro");
            }
        }
        if (!a.equals(Elegir.uno(List.of(a), a, new Random(0)))) {
            throw new IllegalStateException("Si solo queda el ultimo, le toca");
        }
        if (Elegir.uno(List.of(), null, new Random(0)) != null) {
            throw new IllegalStateException("Sin gente devolvio a alguien");
        }
    }

    private static void caras() {
        BufferedImage una = Caras.pintar(7);
        BufferedImage otraVez = Caras.pintar(7);
        BufferedImage distinta = Caras.pintar(8);
        if (una.getWidth() != Caras.LADO || una.getHeight() != Caras.LADO) {
            throw new IllegalStateException("La cara no es de 128");
        }
        if (Caras.firma(una) != Caras.firma(otraVez)) {
            throw new IllegalStateException("La misma semilla cambio la cara");
        }
        if (Caras.firma(una) == Caras.firma(distinta)) {
            throw new IllegalStateException("Dos semillas salieron iguales");
        }
        int vistos = 0;
        int anterior = 0;
        for (int i = 0; i < 8; i++) {
            BufferedImage cara = Caras.pintar(50L + i * 97);
            int claros = Caras.claros(cara);
            if (claros < 4000 || claros > 15000) {
                throw new IllegalStateException("La cara " + i + " no se lee: " + claros);
            }
            int firma = Caras.firma(cara);
            if (i > 0 && firma == anterior) {
                throw new IllegalStateException("Dos caras seguidas iguales");
            }
            anterior = firma;
            vistos++;
        }
        if (vistos != 8) {
            throw new IllegalStateException("Faltan caras");
        }
    }

    private static void gritos() {
        if (!"preguntones:susto.grito".equals(Gritos.GRITO)) {
            throw new IllegalStateException("El grito tiene que ser siempre el mismo");
        }
    }
}
