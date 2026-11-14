package com.preguntones.recuperar;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

public final class Pruebas {

    public static void main(String[] args) {
        codigos();
        huellas();
        pedidos();
        politica();
        avisos();
        System.out.println("Recuperar ok");
    }

    private static void codigos() {
        SecureRandom azar = new SecureRandom();
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String codigo = Codigo.generar(azar);
            if (codigo.length() != Codigo.LARGO) {
                throw new IllegalStateException(codigo);
            }
            if (!codigo.equals(Codigo.normalizar(Codigo.mostrar(codigo).toLowerCase()))) {
                throw new IllegalStateException(codigo);
            }
            vistos.add(codigo);
        }
        if (vistos.size() < 190) {
            throw new IllegalStateException("demasiados códigos repetidos");
        }
        if (Codigo.normalizar("ABCD-EFGH") == null) {
            throw new IllegalStateException("ABCD-EFGH");
        }
        if (Codigo.normalizar("OOOO-OOOO") != null || Codigo.normalizar("1234") != null) {
            throw new IllegalStateException("aceptó un código imposible");
        }
    }

    private static void huellas() {
        SecureRandom azar = new SecureRandom();
        String sal = Huella.sal(azar);
        String huella = Huella.calcular(sal, "ABCD2345");
        if (!Huella.coincide(sal, huella, "ABCD2345")) {
            throw new IllegalStateException("la huella no reconoce su código");
        }
        if (Huella.coincide(sal, huella, "ABCD2346") || Huella.coincide(Huella.sal(azar), huella, "ABCD2345")) {
            throw new IllegalStateException("la huella acepta otro código");
        }
    }

    private static void pedidos() {
        if (Pedido.leer(new String[0]).tipo != Pedido.Tipo.AYUDA) {
            throw new IllegalStateException("ayuda");
        }
        if (Pedido.leer(new String[]{"Nuevo"}).tipo != Pedido.Tipo.NUEVO) {
            throw new IllegalStateException("nuevo");
        }
        if (Pedido.leer(new String[]{"discord"}).tipo != Pedido.Tipo.NUEVO) {
            throw new IllegalStateException("discord");
        }
        Pedido cambio = Pedido.leer(new String[]{"abcd-2345", "clave", "clave"});
        if (cambio.tipo != Pedido.Tipo.CAMBIAR || !"clave".equals(cambio.clave)) {
            throw new IllegalStateException("cambiar");
        }
        if (Pedido.leer(new String[]{"solo"}).tipo != Pedido.Tipo.USO) {
            throw new IllegalStateException("uso");
        }
    }

    private static void politica() {
        if (Politica.rechazo("Ana", "clave", "ABCD2345") != null) {
            throw new IllegalStateException("clave válida rechazada");
        }
        if (Politica.rechazo("Ana", "12345", "ABCD2345") == null) {
            throw new IllegalStateException("aceptó una clave fácil");
        }
        if (Politica.rechazo("Ana", "ana", "ABCD2345") == null) {
            throw new IllegalStateException("aceptó el nombre");
        }
        if (Politica.rechazo("Ana", "ABCD2345", "ABCD2345") == null) {
            throw new IllegalStateException("aceptó el código como clave");
        }
    }

    private static void avisos() {
        String texto = Aviso.texto("Alkalo_", "ABCD-2345", true);
        if (!texto.contains("ABCD-2345") || !texto.contains("Recuperar Contraseña")) {
            throw new IllegalStateException("el aviso no lleva el código");
        }
        if (!texto.startsWith("El código anterior ya no vale.")) {
            throw new IllegalStateException("no avisa de que el anterior murió");
        }
        String primero = Aviso.texto("Ana\n", "WXYZ-6789", false);
        if (primero.contains("ya no vale") || primero.contains("\n") && primero.contains("Ana\n")) {
            throw new IllegalStateException("el primer aviso filtra el nombre o mata un código que no existe");
        }
        if (!primero.contains("Ana:") || !primero.contains("Código: WXYZ-6789")) {
            throw new IllegalStateException("el primer aviso no explica el código");
        }
    }
}
