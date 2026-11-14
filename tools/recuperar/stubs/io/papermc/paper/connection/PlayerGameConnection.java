package io.papermc.paper.connection;

import org.bukkit.entity.Player;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface PlayerGameConnection extends PlayerCommonConnection {
    Player getPlayer();
}
