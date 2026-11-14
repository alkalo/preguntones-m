package com.preguntones.recuperar;

/** Qué pide /recuperar, sin mirar todavía si el código o la clave valen. */
public final class Pedido {

    public enum Tipo {
        AYUDA, NUEVO, CAMBIAR, USO
    }

    public final Tipo tipo;
    public final String codigo;
    public final String clave;
    public final String repetida;

    private Pedido(Tipo tipo, String codigo, String clave, String repetida) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.clave = clave;
        this.repetida = repetida;
    }

    public static Pedido leer(String[] args) {
        if (args == null || args.length == 0) {
            return new Pedido(Tipo.AYUDA, null, null, null);
        }
        if (args.length == 1 && ("nuevo".equalsIgnoreCase(args[0]) || "discord".equalsIgnoreCase(args[0]))) {
            return new Pedido(Tipo.NUEVO, null, null, null);
        }
        if (args.length == 3) {
            return new Pedido(Tipo.CAMBIAR, args[0], args[1], args[2]);
        }
        return new Pedido(Tipo.USO, null, null, null);
    }
}
