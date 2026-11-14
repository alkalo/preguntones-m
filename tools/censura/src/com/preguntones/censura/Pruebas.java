package com.preguntones.censura;

public final class Pruebas {

    public static void main(String[] args) {
        igual("/login secreto", "/login ****");
        igual("/register una dos", "/register **** ****");
        igual("/recuperar ABCD-EFGH clave clave", "/recuperar **** **** ****");
        igual("Alkalo_ issued server command: /recuperar ABCD-EFGH una una", "Alkalo_ issued server command: /recuperar **** **** ****");
        igual("Alkalo_ issued server command: /login secreto", "Alkalo_ issued server command: /login ****");
        igual("hola barrio", "hola barrio");
        igual("/login ****", "/login ****");
        igual("/lp user Alkalo_ parent add vecino", "/lp user Alkalo_ parent add vecino");
        igual("voy al login del banco", "voy al login del banco");
        System.out.println("Censura ok");
    }

    private static void igual(String entrada, String esperado) {
        String salida = Clave.censurar(entrada);
        if (!esperado.equals(salida)) {
            throw new IllegalStateException(entrada + " -> " + salida + " y esperaba " + esperado);
        }
    }
}
