package com.preguntones.bingo;

enum Casilla {
    TALA("tala", "Tala un tronco", false),
    PIEDRA("piedra", "Pica piedra", false),
    CRAFTEA("craftea", "Craftea algo", true),
    HORNO("horno", "Cocina en un horno", false),
    CAMA("cama", "Pulsa una cama", false),
    CAIDA("caida", "Cae diez y vive", true),
    PESCA("pesca", "Pesca un pez", false),
    CHAT("chat", "Escribe en el chat", true),
    BANCO("banco", "Abre el banco", true),
    TIENDA("tienda", "Abre la tienda", true),
    CAZA("caza", "Caza un monstruo", false),
    MONTA("monta", "Súbete a una barca o a un animal", true),
    ANTORCHA("antorcha", "Coloca una antorcha", true),
    COME("come", "Come algo", false),
    AGUA("agua", "Métete en el agua", true),
    NOTA("nota", "Toca un bloque de notas", false),
    CARTEL("cartel", "Escribe un cartel", false),
    TIERRA("tierra", "Cava tierra", true);

    final String id;
    final String texto;
    final boolean facil;

    Casilla(String id, String texto, boolean facil) {
        this.id = id;
        this.texto = texto;
        this.facil = facil;
    }

    static Casilla porId(String id) {
        for (Casilla casilla : values()) {
            if (casilla.id.equals(id)) {
                return casilla;
            }
        }
        return null;
    }
}
