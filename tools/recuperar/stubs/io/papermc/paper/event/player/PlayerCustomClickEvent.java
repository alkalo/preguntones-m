package io.papermc.paper.event.player;

import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.key.Key;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public class PlayerCustomClickEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public Key getIdentifier() {
        throw new UnsupportedOperationException();
    }

    public DialogResponseView getDialogResponseView() {
        throw new UnsupportedOperationException();
    }

    public PlayerCommonConnection getCommonConnection() {
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
