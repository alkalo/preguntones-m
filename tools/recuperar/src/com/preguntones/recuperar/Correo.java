package com.preguntones.recuperar;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Mensaje privado a la cuenta de Discord ya vinculada. */
final class Correo {

    private Correo() {
    }

    static boolean listo() {
        return jda() != null;
    }

    /** Se puede saber si la cuenta está vinculada. El bot puede seguir desconectado. */
    static boolean disponible() {
        try {
            DiscordSRV srv = DiscordSRV.getPlugin();
            return srv != null && srv.getAccountLinkManager() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static String discordDe(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        DiscordSRV srv = DiscordSRV.getPlugin();
        if (srv == null || srv.getAccountLinkManager() == null) {
            return null;
        }
        String discord;
        try {
            discord = srv.getAccountLinkManager().getDiscordIdBypassCache(uuid);
        } catch (RuntimeException e) {
            discord = srv.getAccountLinkManager().getDiscordId(uuid);
        }
        if (discord == null || discord.isBlank() || "0".equals(discord)) {
            return null;
        }
        return discord;
    }

    static void enviar(String discordId, String texto, Runnable exito, Consumer<Throwable> fallo) {
        JDA jda = jda();
        if (jda == null || discordId == null || discordId.isBlank()) {
            fallo.accept(new IllegalStateException("desconectado"));
            return;
        }
        jda.retrieveUserById(discordId).queue(
                usuario -> usuario.openPrivateChannel().queue(
                        canal -> canal.sendMessage(texto).queue(mensaje -> exito.run(), fallo),
                        fallo),
                fallo);
    }

    static String explicar(Throwable error) {
        String crudo = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        if (crudo.contains("50007") || crudo.contains("cannot send")) {
            return "Discord no me deja escribirte. En Ajustes, Privacidad, activa los mensajes directos de miembros del servidor y vuelve a pedirlo.";
        }
        return "No he podido enviarte el mensaje privado. Prueba otra vez dentro de un momento.";
    }

    static JDA cliente() {
        return jda();
    }

    private static JDA jda() {
        DiscordSRV srv = DiscordSRV.getPlugin();
        if (srv == null) {
            return null;
        }
        JDA jda = srv.getJda();
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) {
            return null;
        }
        return jda;
    }
}
