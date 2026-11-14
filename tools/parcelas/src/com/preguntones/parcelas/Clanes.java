package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

final class Clanes {
    private final ParcelasPlugin plugin;
    private final Map<String, Clan> clanes = new HashMap<>();
    private final Map<UUID, String> deQuien = new HashMap<>();
    private final Map<UUID, String> invitaciones = new HashMap<>();
    private final int maxPersonas;
    private final File archivo;

    Clanes(ParcelasPlugin plugin) {
        this.plugin = plugin;
        this.maxPersonas = Math.max(2, plugin.getConfig().getInt("clan.max-personas", 8));
        this.archivo = new File(plugin.getDataFolder(), "clanes.yml");
        cargar();
    }

    int maxPersonas() {
        return maxPersonas;
    }

    Clan de(UUID id) {
        String clave = deQuien.get(id);
        return clave == null ? null : clanes.get(clave);
    }

    List<UUID> companeros(UUID id) {
        Clan clan = de(id);
        List<UUID> lista = new ArrayList<>();
        if (clan == null) {
            return lista;
        }
        for (UUID miembro : clan.miembros) {
            if (!miembro.equals(id)) {
                lista.add(miembro);
            }
        }
        return lista;
    }

    String resumen(UUID id) {
        Clan clan = de(id);
        if (clan == null) {
            return "";
        }
        return "§6Clan §f" + clan.nombre + "§6. §f" + clan.miembros.size() + "/" + maxPersonas + "§6 personas.";
    }

    boolean orden(Player jugador, String[] args) {
        if (args.length == 0) {
            info(jugador);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "crear" -> crear(jugador, args);
            case "invitar" -> invitar(jugador, args);
            case "aceptar" -> aceptar(jugador);
            case "salir" -> salir(jugador);
            case "echar" -> echar(jugador, args);
            case "voz" -> voz(jugador);
            case "info" -> info(jugador);
            default -> jugador.sendMessage("§e/clan crear <nombre>§f, §einvitar§f, §eaceptar§f, §esalir§f, §eechar§f, §evoz§f, §einfo");
        }
        return true;
    }

    private void crear(Player jugador, String[] args) {
        if (de(jugador.getUniqueId()) != null) {
            jugador.sendMessage("§cYa estás en un clan. §f/clan salir");
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/clan crear <nombre>");
            return;
        }
        String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (nombre.length() < 3 || nombre.length() > 16) {
            jugador.sendMessage("§cEl nombre va de 3 a 16 letras.");
            return;
        }
        String clave = nombre.toLowerCase(Locale.ROOT);
        if (clanes.containsKey(clave)) {
            jugador.sendMessage("§cEse clan ya existe.");
            return;
        }
        Clan clan = new Clan();
        clan.nombre = nombre;
        clan.lider = jugador.getUniqueId();
        clan.miembros.add(jugador.getUniqueId());
        clan.nombres.put(jugador.getUniqueId(), jugador.getName());
        clanes.put(clave, clan);
        deQuien.put(jugador.getUniqueId(), clave);
        guardar();
        jugador.sendMessage("§aClan §f" + nombre + "§a creado. Hasta " + maxPersonas + " personas. El tope sigue en " + plugin.parcelasBase() + " parcelas por persona.");
    }

    private void invitar(Player jugador, String[] args) {
        Clan clan = propio(jugador);
        if (clan == null) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/clan invitar <nombre>");
            return;
        }
        if (clan.miembros.size() >= maxPersonas) {
            jugador.sendMessage("§cEl clan ya tiene " + maxPersonas + " personas.");
            return;
        }
        Player otro = Bukkit.getPlayerExact(args[1]);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar conectada.");
            return;
        }
        if (de(otro.getUniqueId()) != null) {
            jugador.sendMessage("§c" + otro.getName() + " ya está en un clan.");
            return;
        }
        invitaciones.put(otro.getUniqueId(), deQuien.get(jugador.getUniqueId()));
        otro.sendMessage("§6" + jugador.getName() + " §fte invita a §f" + clan.nombre + "§f. §e/clan aceptar");
        jugador.sendMessage("§aInvitación enviada a " + otro.getName() + ".");
    }

    private void aceptar(Player jugador) {
        if (de(jugador.getUniqueId()) != null) {
            jugador.sendMessage("§cYa estás en un clan.");
            return;
        }
        String clave = invitaciones.remove(jugador.getUniqueId());
        if (clave == null) {
            jugador.sendMessage("§cNadie te ha invitado a un clan.");
            return;
        }
        Clan clan = clanes.get(clave);
        if (clan == null) {
            jugador.sendMessage("§cEse clan ya no existe.");
            return;
        }
        if (clan.miembros.size() >= maxPersonas) {
            jugador.sendMessage("§cEl clan ya está lleno.");
            return;
        }
        clan.miembros.add(jugador.getUniqueId());
        clan.nombres.put(jugador.getUniqueId(), jugador.getName());
        deQuien.put(jugador.getUniqueId(), clave);
        guardar();
        jugador.sendMessage("§aEstás en §f" + clan.nombre + "§a. El tope sigue en §f" + plugin.parcelasBase() + "§a parcelas por persona.");
        Player lider = Bukkit.getPlayer(clan.lider);
        if (lider != null) {
            lider.sendMessage("§a" + jugador.getName() + " entra en el clan.");
        }
    }

    private void salir(Player jugador) {
        Clan clan = de(jugador.getUniqueId());
        if (clan == null) {
            jugador.sendMessage("§eNo estás en un clan. El tope sigue en " + plugin.parcelasBase() + " parcelas.");
            return;
        }
        String clave = deQuien.get(jugador.getUniqueId());
        clan.miembros.remove(jugador.getUniqueId());
        clan.nombres.remove(jugador.getUniqueId());
        deQuien.remove(jugador.getUniqueId());
        if (clan.miembros.isEmpty()) {
            clanes.remove(clave);
            cerrarSala(clan.nombre);
            jugador.sendMessage("§eHas disuelto el clan. El tope sigue en " + plugin.parcelasBase() + " parcelas.");
        } else {
            if (jugador.getUniqueId().equals(clan.lider)) {
                clan.lider = clan.miembros.get(0);
            }
            jugador.sendMessage("§eHas salido. El tope sigue en " + plugin.parcelasBase() + " parcelas. Las que ya tienes se quedan.");
        }
        guardar();
    }

    private void echar(Player jugador, String[] args) {
        Clan clan = propio(jugador);
        if (clan == null) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/clan echar <nombre>");
            return;
        }
        UUID id = null;
        for (UUID miembro : clan.miembros) {
            if (args[1].equalsIgnoreCase(clan.nombres.get(miembro))) {
                id = miembro;
                break;
            }
        }
        if (id == null) {
            jugador.sendMessage("§cEsa persona no está en tu clan.");
            return;
        }
        if (id.equals(clan.lider)) {
            jugador.sendMessage("§cEl líder no se echa. Puede salir con §e/clan salir§c.");
            return;
        }
        clan.miembros.remove(id);
        String nombre = clan.nombres.remove(id);
        deQuien.remove(id);
        guardar();
        jugador.sendMessage("§e" + nombre + " ya no está en el clan.");
        Player otro = Bukkit.getPlayer(id);
        if (otro != null) {
            otro.sendMessage("§eTe han echado de §f" + clan.nombre + "§e. El tope sigue en " + plugin.parcelasBase() + " parcelas.");
        }
    }

    private void info(Player jugador) {
        Clan clan = de(jugador.getUniqueId());
        if (clan == null) {
            jugador.sendMessage("§eNo estás en un clan. Tope: §f" + plugin.parcelasBase() + " parcelas§e. §f/clan crear <nombre>");
            jugador.sendMessage("§7El clan junta a la gente. §e/parcela cerca §7te deja al lado, y §e/clan voz §7abre la sala.");
            return;
        }
        StringBuilder gente = new StringBuilder();
        for (UUID id : clan.miembros) {
            if (gente.length() > 0) {
                gente.append(", ");
            }
            gente.append(clan.nombres.getOrDefault(id, "?"));
            if (id.equals(clan.lider)) {
                gente.append(" (líder)");
            }
        }
        jugador.sendMessage("§6" + clan.nombre + "§f. " + clan.miembros.size() + "/" + maxPersonas + ". " + gente);
        jugador.sendMessage("§fTope de parcelas de cada uno: §e" + plugin.parcelasBase() + "§f.");
        if (jugador.getUniqueId().equals(clan.lider)) {
            jugador.sendMessage("§7La sala de voz del clan: §e/clan voz");
        }
    }

    private void voz(Player jugador) {
        Clan clan = de(jugador.getUniqueId());
        if (clan == null) {
            jugador.sendMessage("§cNo estás en un clan.");
            return;
        }
        if (!jugador.getUniqueId().equals(clan.lider)) {
            jugador.sendMessage("§cLa sala del clan la abre el jefe.");
            return;
        }
        org.bukkit.plugin.Plugin voces = org.bukkit.Bukkit.getPluginManager().getPlugin("Voces");
        if (voces == null || !voces.isEnabled()) {
            jugador.sendMessage("§eLa voz de Discord no está lista.");
            return;
        }
        try {
            voces.getClass().getMethod("salaDeClan", org.bukkit.entity.Player.class, String.class).invoke(null, jugador, clan.nombre);
        } catch (ReflectiveOperationException e) {
            jugador.sendMessage("§cNo he podido abrir la sala.");
        }
    }

    private void cerrarSala(String nombre) {
        org.bukkit.plugin.Plugin voces = org.bukkit.Bukkit.getPluginManager().getPlugin("Voces");
        if (voces == null || !voces.isEnabled()) {
            return;
        }
        try {
            voces.getClass().getMethod("cerrarClan", String.class).invoke(null, nombre);
        } catch (ReflectiveOperationException ignored) {
            // La sala se borra sola al vaciarse.
        }
    }

    private Clan propio(Player jugador) {
        Clan clan = de(jugador.getUniqueId());
        if (clan == null) {
            jugador.sendMessage("§cNo estás en un clan.");
            return null;
        }
        if (!jugador.getUniqueId().equals(clan.lider)) {
            jugador.sendMessage("§cSolo el líder invita o echa.");
            return null;
        }
        return clan;
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        for (String clave : yaml.getKeys(false)) {
            Clan clan = new Clan();
            clan.nombre = yaml.getString(clave + ".nombre", clave);
            String lider = yaml.getString(clave + ".lider", "");
            if (!lider.isEmpty()) {
                clan.lider = UUID.fromString(lider);
            }
            for (String miembro : yaml.getStringList(clave + ".miembros")) {
                UUID id = UUID.fromString(miembro);
                clan.miembros.add(id);
                clan.nombres.put(id, yaml.getString(clave + ".nombres." + miembro, "?"));
                deQuien.put(id, clave);
            }
            if (clan.lider != null) {
                clanes.put(clave, clan);
            }
        }
    }

    private void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Clan> entrada : clanes.entrySet()) {
            Clan clan = entrada.getValue();
            String clave = entrada.getKey();
            yaml.set(clave + ".nombre", clan.nombre);
            yaml.set(clave + ".lider", clan.lider.toString());
            List<String> miembros = new ArrayList<>();
            for (UUID id : clan.miembros) {
                miembros.add(id.toString());
                yaml.set(clave + ".nombres." + id, clan.nombres.get(id));
            }
            yaml.set(clave + ".miembros", miembros);
        }
        try {
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude guardar los clanes: " + e.getMessage());
        }
    }

    static final class Clan {
        String nombre;
        UUID lider;
        final List<UUID> miembros = new ArrayList<>();
        final Map<UUID, String> nombres = new HashMap<>();
    }
}
