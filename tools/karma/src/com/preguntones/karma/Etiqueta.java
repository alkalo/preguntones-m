package com.preguntones.karma;

import net.kyori.adventure.text.format.TextColor;

enum Etiqueta {
    CRUEL("Cruel", TextColor.color(0xAA2222), "§4", "&4"),
    MALO("Malo", TextColor.color(0xE05050), "§c", "&c"),
    NORMAL("Normal", TextColor.color(0xC5CED8), "§7", "&7"),
    BUENO("Bueno", TextColor.color(0x6FCF97), "§a", "&a");

    final String visible;
    final TextColor color;
    final String chat;
    final String tab;

    Etiqueta(String visible, TextColor color, String chat, String tab) {
        this.visible = visible;
        this.color = color;
        this.chat = chat;
        this.tab = tab;
    }

    static Etiqueta de(int karma, int buenoDesde, int maloDesde, int cruelDesde) {
        if (karma <= cruelDesde) {
            return CRUEL;
        }
        if (karma <= maloDesde) {
            return MALO;
        }
        if (karma >= buenoDesde) {
            return BUENO;
        }
        return NORMAL;
    }
}
