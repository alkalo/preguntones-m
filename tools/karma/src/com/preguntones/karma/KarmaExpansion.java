package com.preguntones.karma;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class KarmaExpansion extends PlaceholderExpansion {
    private final KarmaPlugin plugin;

    public KarmaExpansion(KarmaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "karma";
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
        if (player == null || params == null || !params.equalsIgnoreCase("tag")) {
            return null;
        }
        return plugin.textoTab(player.getUniqueId());
    }
}
