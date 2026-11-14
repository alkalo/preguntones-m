package com.preguntones.parcelas;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class DurosExpansion extends PlaceholderExpansion {
    private final ParcelasPlugin plugin;

    public DurosExpansion(ParcelasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "preguntones";
    }

    @Override
    public String getAuthor() {
        return "Preguntones";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null) {
            return "";
        }
        if (params.equalsIgnoreCase("duros")) {
            return Integer.toString(plugin.durosDe(player));
        }
        if (params.equalsIgnoreCase("parcela")) {
            return plugin.textoParcela(player);
        }
        if (params.equalsIgnoreCase("ciclo")) {
            return textoAjeno("com.preguntones.mundos.MundosPlugin", "textoCiclo");
        }
        if (params.equalsIgnoreCase("xo")) {
            return textoAjeno("com.preguntones.xo.XoPlugin", "textoCita");
        }
        return null;
    }

    private String textoAjeno(String clase, String metodo) {
        try {
            Object valor = Class.forName(clase).getMethod(metodo).invoke(null);
            return valor == null ? "" : valor.toString();
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }
}
