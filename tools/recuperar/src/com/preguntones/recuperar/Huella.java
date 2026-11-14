package com.preguntones.recuperar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Guarda el código como huella. El archivo del servidor no contiene el código. */
public final class Huella {

    private Huella() {
    }

    public static String sal(SecureRandom azar) {
        byte[] bytes = new byte[16];
        azar.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String calcular(String sal, String codigo) {
        return hex(resumen(sal, codigo));
    }

    public static boolean coincide(String sal, String huella, String codigo) {
        if (sal == null || huella == null || codigo == null || (huella.length() & 1) != 0) {
            return false;
        }
        byte[] esperada;
        try {
            esperada = decodificar(huella);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(esperada, resumen(sal, codigo));
    }

    private static byte[] resumen(String sal, String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sal.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(codigo.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder texto = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            texto.append(Character.forDigit((b >> 4) & 0xF, 16));
            texto.append(Character.forDigit(b & 0xF, 16));
        }
        return texto.toString();
    }

    private static byte[] decodificar(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int alto = Character.digit(hex.charAt(i * 2), 16);
            int bajo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (alto < 0 || bajo < 0) {
                throw new IllegalArgumentException();
            }
            bytes[i] = (byte) ((alto << 4) + bajo);
        }
        return bytes;
    }
}
