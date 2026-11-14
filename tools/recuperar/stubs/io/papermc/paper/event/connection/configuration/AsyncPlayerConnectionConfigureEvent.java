package io.papermc.paper.event.connection.configuration;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public class AsyncPlayerConnectionConfigureEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerConfigurationConnection getConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
