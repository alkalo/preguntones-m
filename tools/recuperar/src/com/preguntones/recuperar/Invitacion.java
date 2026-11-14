package com.preguntones.recuperar;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Invite;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.GuildMemberJoinEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** Invitación de un solo uso. Quien entra con ella queda vinculado a esa cuenta de Minecraft. */
final class Invitacion extends ListenerAdapter {

    static final String PUBLICA = "https://discord.gg/ZjcnvSkqsQ";

    private final RecuperarPlugin plugin;
    private final Map<String, UUID> pendientes = new ConcurrentHashMap<>();
    private final Map<String, Integer> usos = new ConcurrentHashMap<>();
    private final Map<UUID, String> codigoDe = new ConcurrentHashMap<>();
    private final Map<UUID, String> urlDe = new ConcurrentHashMap<>();
    private boolean escuchando;
    private org.bukkit.scheduler.BukkitTask esperaJda;

    Invitacion(RecuperarPlugin plugin) {
        this.plugin = plugin;
    }

    void escuchar() {
        esperaJda = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (escuchando) {
                cancelarEspera();
                return;
            }
            JDA jda = Correo.cliente();
            if (jda == null) {
                return;
            }
            jda.addEventListener(this);
            escuchando = true;
            cancelarEspera();
        }, 40L, 40L);
    }

    private void cancelarEspera() {
        if (esperaJda != null) {
            esperaJda.cancel();
            esperaJda = null;
        }
    }

    void cerrar() {
        JDA jda = Correo.cliente();
        if (jda != null && escuchando) {
            jda.removeEventListener(this);
        }
        escuchando = false;
    }

    String urlDe(UUID uuid) {
        return urlDe.get(uuid);
    }

    void preparar(UUID uuid, BiConsumer<String, Boolean> listo) {
        String ya = urlDe.get(uuid);
        if (ya != null) {
            listo.accept(ya, true);
            return;
        }
        TextChannel canal = canal();
        if (canal == null) {
            listo.accept(PUBLICA, false);
            return;
        }
        canal.createInvite()
                .setMaxAge(10L, TimeUnit.MINUTES)
                .setMaxUses(5)
                .setUnique(true)
                .queue(invite -> {
                    retirar(uuid);
                    pendientes.put(invite.getCode(), uuid);
                    usos.put(invite.getCode(), 0);
                    codigoDe.put(uuid, invite.getCode());
                    urlDe.put(uuid, invite.getUrl());
                    listo.accept(invite.getUrl(), true);
                }, error -> {
                    plugin.getLogger().warning("No pude crear la invitación de " + uuid + ".");
                    listo.accept(PUBLICA, false);
                });
    }

    void retirar(UUID uuid) {
        String codigo = codigoDe.remove(uuid);
        urlDe.remove(uuid);
        if (codigo == null) {
            return;
        }
        pendientes.remove(codigo);
        usos.remove(codigo);
        TextChannel canal = canal();
        if (canal == null) {
            return;
        }
        canal.retrieveInvites().queue(lista -> {
            for (Invite invite : lista) {
                if (codigo.equals(invite.getCode())) {
                    invite.delete().queue();
                }
            }
        }, error -> {
            // La invitación ya no está.
        });
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (pendientes.isEmpty() || event.getUser() == null || event.getUser().isBot()) {
            return;
        }
        TextChannel canal = canal();
        if (canal == null || event.getGuild() == null || canal.getGuild() == null
                || !event.getGuild().getId().equals(canal.getGuild().getId())) {
            return;
        }
        String discord = event.getUser().getId();
        canal.retrieveInvites().queue(lista -> {
            List<String> subieron = new ArrayList<>();
            for (Invite invite : lista) {
                UUID dueno = pendientes.get(invite.getCode());
                if (dueno == null) {
                    continue;
                }
                int antes = usos.getOrDefault(invite.getCode(), 0);
                if (invite.getUses() > antes) {
                    subieron.add(invite.getCode());
                }
                usos.put(invite.getCode(), invite.getUses());
            }
            if (subieron.size() != 1) {
                return;
            }
            String codigo = subieron.get(0);
            UUID uuid = pendientes.get(codigo);
            if (uuid == null) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> vincular(uuid, discord));
        }, error -> plugin.getLogger().warning("No pude leer las invitaciones."));
    }

    private void vincular(UUID uuid, String discord) {
        try {
            var manager = DiscordSRV.getPlugin().getAccountLinkManager();
            UUID otra = manager.getUuid(discord);
            if (otra != null && !otra.equals(uuid)) {
                plugin.getLogger().warning("Ese Discord ya está en otra cuenta.");
                plugin.puerta().ocupado(uuid);
                return;
            }
            String ya = manager.getDiscordIdBypassCache(uuid);
            if (ya == null || ya.isBlank() || "0".equals(ya)) {
                manager.link(discord, uuid);
            }
            pendientes.remove(codigoDe.get(uuid));
            plugin.getLogger().info("Discord vinculado por la invitación.");
            plugin.puerta().conectado(uuid);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("No pude vincular por la invitación.");
        }
    }

    private TextChannel canal() {
        JDA jda = Correo.cliente();
        if (jda == null) {
            return null;
        }
        TextChannel primero = null;
        TextChannel sistema = null;
        for (Guild guild : jda.getGuilds()) {
            if (sistema == null) {
                sistema = guild.getSystemChannel();
            }
            for (TextChannel texto : guild.getTextChannels()) {
                if (primero == null) {
                    primero = texto;
                }
                if ("chat".equals(texto.getName()) || "como-jugar".equals(texto.getName())) {
                    return texto;
                }
            }
        }
        return sistema != null ? sistema : primero;
    }
}
