package com.preguntones.voces;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Category;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class VocesPlugin extends JavaPlugin {
    private static final String HUB = "1570203493657088050";
    private static final String SALAS = "1570203491605942315";
    private static VocesPlugin instancia;
    private final Set<String> propias = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, String> clanDeCanal = new ConcurrentHashMap<>();
    private BukkitTask espera;
    private boolean enganchado;

    public static void salaDeClan(Player jugador, String nombre) {
        if (instancia == null) {
            jugador.sendMessage("§eLa voz de Discord no está lista.");
            return;
        }
        instancia.abrirClan(jugador, nombre);
    }

    public static void cerrarClan(String nombre) {
        if (instancia != null) {
            instancia.borrarClan(nombre);
        }
    }

    @Override
    public void onEnable() {
        instancia = this;
        espera = Bukkit.getScheduler().runTaskTimer(this, this::enganchar, 40L, 40L);
    }

    @Override
    public void onDisable() {
        JDA jda = jda();
        if (jda != null) {
            jda.removeEventListener(oyente);
        }
        if (espera != null) {
            espera.cancel();
        }
        instancia = null;
    }

    private final ListenerAdapter oyente = new ListenerAdapter() {
        @Override
        public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
            VoiceChannel entro = event.getChannelJoined();
            VoiceChannel salio = event.getChannelLeft();
            if (entro != null && HUB.equals(entro.getId())) {
                abrirPersonal(event.getGuild(), event.getMember());
            }
            if (salio != null) {
                vaciar(salio);
            }
        }
    };

    private void enganchar() {
        if (enganchado) {
            espera.cancel();
            return;
        }
        JDA jda = jda();
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) {
            return;
        }
        jda.addEventListener(oyente);
        enganchado = true;
        espera.cancel();
        getLogger().info("Voz lista. Entrar en crear abre una sala. /clan voz abre la del clan.");
    }

    private void abrirPersonal(Guild guild, Member miembro) {
        String ya = duenoDe(miembro.getId());
        if (ya != null) {
            VoiceChannel sala = guild.getVoiceChannelById(ya);
            if (sala != null) {
                guild.moveVoiceMember(miembro, sala).queue();
                return;
            }
            propias.remove(ya);
        }
        Category categoria = guild.getCategoryById(SALAS);
        if (categoria == null) {
            return;
        }
        String nombre = recortar("Sala de " + miembro.getEffectiveName());
        guild.createVoiceChannel(nombre, categoria)
            .syncPermissionOverrides()
            .addMemberPermissionOverride(miembro.getIdLong(), permisoDeDueno(), 0L)
            .queue(canal -> {
                propias.add(canal.getId());
                guild.moveVoiceMember(miembro, canal).queue(null, error -> canal.delete().queue());
            });
    }

    private void abrirClan(Player jugador, String nombreClan) {
        DiscordSRV srv = DiscordSRV.getPlugin();
        if (srv == null || srv.getJda() == null || srv.getMainGuild() == null) {
            jugador.sendMessage("§eDiscord todavía no está conectado.");
            return;
        }
        String discord = srv.getAccountLinkManager().getDiscordId(jugador.getUniqueId());
        if (discord == null || discord.isEmpty()) {
            jugador.sendMessage("§eVincula tu cuenta con §f/discord link §ey vuelve a escribir §f/clan voz§e.");
            return;
        }
        Guild guild = srv.getMainGuild();
        Category categoria = guild.getCategoryById(SALAS);
        if (categoria == null) {
            jugador.sendMessage("§cNo está la categoría Salas en Discord.");
            return;
        }
        String clave = nombreClan.toLowerCase();
        String existente = clanDeCanal.get(clave);
        VoiceChannel sala = existente == null ? null : guild.getVoiceChannelById(existente);
        if (sala != null) {
            moverSiEsta(guild, discord, sala);
            jugador.sendMessage("§aLa sala §fClan " + nombreClan + " §aya está en Discord. Entra en Salas.");
            return;
        }
        Member miembro = guild.getMemberById(discord);
        var accion = guild.createVoiceChannel(recortar("Clan " + nombreClan), categoria)
            .syncPermissionOverrides()
            .setUserlimit(8);
        if (miembro != null) {
            accion = accion.addMemberPermissionOverride(miembro.getIdLong(), permisoDeDueno(), 0L);
        }
        accion.queue(canal -> {
            propias.add(canal.getId());
            clanDeCanal.put(clave, canal.getId());
            if (miembro != null) {
                moverSiEsta(guild, discord, canal);
            }
            Bukkit.getScheduler().runTask(this, () ->
                jugador.sendMessage("§aSala §fClan " + nombreClan + " §aabierta en Discord, en Salas. Cabe 8 y se borra al vaciarse."));
        }, error -> Bukkit.getScheduler().runTask(this, () ->
            jugador.sendMessage("§cNo he podido crear la sala: Discord no ha dejado.")));
    }

    private void borrarClan(String nombre) {
        String id = clanDeCanal.remove(nombre.toLowerCase());
        if (id == null) {
            return;
        }
        propias.remove(id);
        JDA jda = jda();
        if (jda == null) {
            return;
        }
        VoiceChannel canal = jda.getVoiceChannelById(id);
        if (canal != null) {
            canal.delete().queue();
        }
    }

    private void moverSiEsta(Guild guild, String discord, VoiceChannel sala) {
        Member miembro = guild.getMemberById(discord);
        if (miembro != null && miembro.getVoiceState() != null && miembro.getVoiceState().inVoiceChannel()) {
            guild.moveVoiceMember(miembro, sala).queue();
        }
    }

    private void vaciar(VoiceChannel canal) {
        if (!propias.contains(canal.getId()) || !canal.getMembers().isEmpty()) {
            return;
        }
        propias.remove(canal.getId());
        clanDeCanal.entrySet().removeIf(entrada -> canal.getId().equals(entrada.getValue()));
        canal.delete().queue();
    }

    private String duenoDe(String usuario) {
        JDA jda = jda();
        if (jda == null) {
            return null;
        }
        for (String id : propias) {
            VoiceChannel canal = jda.getVoiceChannelById(id);
            if (canal == null) {
                continue;
            }
            if (canal.getName().startsWith("Sala de ") && canal.getMembers().stream().anyMatch(miembro -> miembro.getId().equals(usuario))) {
                return id;
            }
        }
        return null;
    }

    private long permisoDeDueno() {
        return Permission.getRaw(
            Permission.VIEW_CHANNEL,
            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.MANAGE_CHANNEL,
            Permission.VOICE_MOVE_OTHERS
        );
    }

    private String recortar(String nombre) {
        String limpio = nombre.replaceAll("[\\r\\n#@]", " ").trim();
        if (limpio.isEmpty()) {
            return "Sala";
        }
        return limpio.length() <= 90 ? limpio : limpio.substring(0, 90);
    }

    private JDA jda() {
        DiscordSRV srv = DiscordSRV.getPlugin();
        return srv == null ? null : srv.getJda();
    }
}
