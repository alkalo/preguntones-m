package com.preguntones.censura;

import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CensuraPlugin extends JavaPlugin implements Listener {

    private static final PlainTextComponentSerializer PLANO = PlainTextComponentSerializer.plainText();

    @Override
    public void onEnable() {
        ((Logger) LogManager.getRootLogger()).addFilter(new FiltroConsola());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alDecorar(AsyncChatCommandDecorateEvent event) {
        String texto = PLANO.serialize(event.originalMessage());
        String censurado = Clave.censurar(texto);
        if (!censurado.equals(texto)) {
            event.result(Component.text(censurado));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alChat(AsyncPlayerChatEvent event) {
        String censurado = Clave.censurar(event.getMessage());
        if (!censurado.equals(event.getMessage())) {
            event.setMessage(censurado);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alChatModerno(AsyncChatEvent event) {
        String texto = PLANO.serialize(event.message());
        String censurado = Clave.censurar(texto);
        if (!censurado.equals(texto)) {
            event.message(Component.text(censurado));
        }
    }
}
