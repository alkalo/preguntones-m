package io.papermc.paper.connection;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.audience.Audience;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface PlayerConfigurationConnection extends PlayerCommonConnection {
    Audience getAudience();

    PlayerProfile getProfile();
}
