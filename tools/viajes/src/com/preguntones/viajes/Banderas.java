package com.preguntones.viajes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class Banderas implements Listener {
    static final NamespacedKey CLAVE = new NamespacedKey("viajes", "bandera");
    private static final NamespacedKey ID = new NamespacedKey("viajes", "id");

    private final ViajesPlugin plugin;
    private final File archivo;
    private final Map<UUID, Punto> puntos = new LinkedHashMap<>();
    private final Map<UUID, List<String>> pendientes = new HashMap<>();

    Banderas(ViajesPlugin plugin) {
        this.plugin = plugin;
        archivo = new File(plugin.getDataFolder(), "banderas.yml");
        cargar();
        plugin.getLogger().info("Banderas listas. Hasta " + NombreBandera.MAXIMO + " por persona.");
    }

    List<Punto> vigentes(UUID dueno) {
        boolean cambio = false;
        List<Punto> lista = new ArrayList<>();
        for (Punto punto : new ArrayList<>(de(dueno))) {
            World mundo = plugin.getServer().getWorld(punto.mundo);
            if (mundo != null && !sigue(punto)) {
                puntos.remove(punto.id);
                entregar(punto.dueno, punto.nombre, "§eLa bandera §f" + punto.nombre + "§e ya no está puesta. Ha vuelto a ti.", false);
                cambio = true;
                continue;
            }
            lista.add(punto);
        }
        if (cambio) {
            guardar();
        }
        return lista;
    }

    Punto buscar(UUID dueno, UUID id) {
        Punto punto = puntos.get(id);
        if (punto == null || !dueno.equals(punto.dueno)) {
            return null;
        }
        return punto;
    }

    boolean sigue(Punto punto) {
        World mundo = plugin.getServer().getWorld(punto.mundo);
        if (mundo == null) {
            return true;
        }
        Block bloque = mundo.getBlockAt(punto.bx, punto.by, punto.bz);
        if (!(bloque.getState() instanceof Banner banner)) {
            return false;
        }
        String id = banner.getPersistentDataContainer().get(ID, PersistentDataType.STRING);
        return id == null || punto.id.toString().equals(id);
    }

    void quitar(Punto punto) {
        if (punto == null || puntos.remove(punto.id) == null) {
            return;
        }
        entregar(punto.dueno, punto.nombre, "§eEsa bandera ya no está. §f" + punto.nombre + "§e ha vuelto a ti.", false);
        guardar();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alLimitar(BlockPlaceEvent event) {
        if (!esItem(event.getItemInHand())) {
            return;
        }
        if (NombreBandera.cabe(vigentes(event.getPlayer().getUniqueId()).size())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYa tienes " + NombreBandera.MAXIMO + " banderas. Quita una para poner otra.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPoner(BlockPlaceEvent event) {
        ItemStack usado = event.getItemInHand() == null ? null : event.getItemInHand().clone();
        if (usado != null) {
            usado.setAmount(1);
        }
        Block bloque = event.getBlock();
        Punto viejo = enSitio(bloque);
        if (viejo != null) {
            puntos.remove(viejo.id);
            if (!viejo.dueno.equals(event.getPlayer().getUniqueId())) {
                entregar(viejo.dueno, viejo.nombre, "§eTe han quitado la bandera §f" + viejo.nombre + "§e. Ha vuelto a ti.", false);
            }
        }
        if (!esItem(usado)) {
            if (viejo != null) {
                guardar();
            }
            return;
        }
        Player jugador = event.getPlayer();
        if (!(bloque.getState() instanceof Banner)) {
            bloque.setType(Material.AIR, false);
            dar(jugador, usado);
            guardar();
            jugador.sendMessage("§cAhí no se queda la bandera. Prueba en otro bloque.");
            return;
        }
        if (!NombreBandera.cabe(cuantas(jugador.getUniqueId()))) {
            bloque.setType(Material.AIR, false);
            dar(jugador, usado);
            guardar();
            jugador.sendMessage("§cYa tienes " + NombreBandera.MAXIMO + " banderas. Quita una para poner otra.");
            return;
        }
        String nombre = NombreBandera.elegir(escrito(usado), nombres(jugador.getUniqueId()));
        UUID id = UUID.randomUUID();
        Location sitio = bloque.getLocation().add(0.5, 0, 0.5);
        sitio.setYaw(yawDe(bloque, jugador));
        sitio.setPitch(0f);
        Punto punto = new Punto(
            id,
            jugador.getUniqueId(),
            nombre,
            bloque.getWorld().getName(),
            sitio.getX(),
            sitio.getY(),
            sitio.getZ(),
            sitio.getYaw(),
            bloque.getX(),
            bloque.getY(),
            bloque.getZ()
        );
        puntos.put(id, punto);
        marcar(bloque, id);
        guardar();
        UUID marcarId = id;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (puntos.containsKey(marcarId) && bloque.getState() instanceof Banner) {
                marcar(bloque, marcarId);
            }
        });
        jugador.sendMessage("§aBandera puesta. §fEn el panel, Spawn, eliges §e" + nombre + "§f.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alProteger(BlockBreakEvent event) {
        Punto punto = deBloque(event.getBlock());
        if (punto == null || puedeQuitar(event.getPlayer(), punto)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cEsa bandera no es tuya.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alRomper(BlockBreakEvent event) {
        Punto punto = deBloque(event.getBlock());
        if (punto == null || !punto.dueno.equals(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setDropItems(false);
        puntos.remove(punto.id);
        Player jugador = event.getPlayer();
        if (!punto.dueno.equals(jugador.getUniqueId())) {
            entregar(punto.dueno, punto.nombre, "§e" + jugador.getName() + " ha quitado tu bandera §f" + punto.nombre + "§e. Ha vuelto a ti.", false);
            jugador.sendMessage("§eBandera de otra persona. §fLe ha vuelto.");
            guardar();
            return;
        }
        if (jugador.getGameMode() != GameMode.CREATIVE) {
            dar(jugador, item(punto.nombre));
        }
        guardar();
        jugador.sendMessage("§eHas quitado la bandera. §fEl punto ya no está.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alSuelo(BlockBreakEvent event) {
        if (apoyadas(event.getBlock()).isEmpty()) {
            return;
        }
        for (Punto punto : apoyadas(event.getBlock())) {
            if (!puedeQuitar(event.getPlayer(), punto)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cEsa bandera no es tuya.");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alSueloRoto(BlockBreakEvent event) {
        List<Punto> lista = apoyadas(event.getBlock());
        if (lista.isEmpty()) {
            return;
        }
        Player jugador = event.getPlayer();
        for (Punto punto : lista) {
            quitarBloque(punto);
            puntos.remove(punto.id);
            if (punto.dueno.equals(jugador.getUniqueId())) {
                if (jugador.getGameMode() != GameMode.CREATIVE) {
                    dar(jugador, item(punto.nombre));
                }
                jugador.sendMessage("§eHas quitado la bandera. §fEl punto ya no está.");
            } else {
                entregar(punto.dueno, punto.nombre, "§e" + jugador.getName() + " ha quitado tu bandera §f" + punto.nombre + "§e. Ha vuelto a ti.", false);
                jugador.sendMessage("§eBandera de otra persona. §fLe ha vuelto.");
            }
        }
        guardar();
    }

    @EventHandler(ignoreCancelled = true)
    public void alExplotar(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(ignoreCancelled = true)
    public void alExplotarBloque(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protege);
    }

    @EventHandler(ignoreCancelled = true)
    public void alCambiar(EntityChangeBlockEvent event) {
        if (protege(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        List<String> lista = pendientes.remove(event.getPlayer().getUniqueId());
        if (lista == null || lista.isEmpty()) {
            return;
        }
        Player jugador = event.getPlayer();
        for (String nombre : lista) {
            dar(jugador, item(nombre));
        }
        guardar();
        jugador.sendMessage(lista.size() == 1
            ? "§eHa vuelto tu bandera §f" + lista.get(0) + "§e."
            : "§eHan vuelto §f" + lista.size() + "§e banderas.");
    }

    @EventHandler(ignoreCancelled = true)
    public void alEmpujar(BlockPistonExtendEvent event) {
        if (mueve(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alTirar(BlockPistonRetractEvent event) {
        if (mueve(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Punto punto : puntos.values()) {
            String base = "puntos." + punto.dueno + "." + punto.id;
            yaml.set(base + ".nombre", punto.nombre);
            yaml.set(base + ".mundo", punto.mundo);
            yaml.set(base + ".x", punto.x);
            yaml.set(base + ".y", punto.y);
            yaml.set(base + ".z", punto.z);
            yaml.set(base + ".yaw", punto.yaw);
            yaml.set(base + ".bx", punto.bx);
            yaml.set(base + ".by", punto.by);
            yaml.set(base + ".bz", punto.bz);
        }
        for (Map.Entry<UUID, List<String>> entrada : pendientes.entrySet()) {
            if (!entrada.getValue().isEmpty()) {
                yaml.set("pendientes." + entrada.getKey(), entrada.getValue());
            }
        }
        try {
            File carpeta = archivo.getParentFile();
            if (carpeta != null && !carpeta.exists()) {
                carpeta.mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude guardar las banderas: " + e.getMessage());
        }
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection espera = yaml.getConfigurationSection("pendientes");
        if (espera != null) {
            for (String clave : espera.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(clave);
                    List<String> nombres = new ArrayList<>(yaml.getStringList("pendientes." + clave));
                    if (!nombres.isEmpty()) {
                        pendientes.put(id, nombres);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ignoro una bandera pendiente.");
                }
            }
        }
        ConfigurationSection raiz = yaml.getConfigurationSection("puntos");
        if (raiz == null) {
            return;
        }
        for (String duenoClave : raiz.getKeys(false)) {
            UUID dueno;
            try {
                dueno = UUID.fromString(duenoClave);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection persona = raiz.getConfigurationSection(duenoClave);
            if (persona == null) {
                continue;
            }
            for (String idClave : persona.getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(idClave);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection datos = persona.getConfigurationSection(idClave);
                if (datos == null) {
                    continue;
                }
                String mundo = datos.getString("mundo", "");
                if (mundo.isEmpty()) {
                    continue;
                }
                puntos.put(id, new Punto(
                    id,
                    dueno,
                    datos.getString("nombre", "Bandera"),
                    mundo,
                    datos.getDouble("x"),
                    datos.getDouble("y"),
                    datos.getDouble("z"),
                    (float) datos.getDouble("yaw"),
                    datos.getInt("bx"),
                    datos.getInt("by"),
                    datos.getInt("bz")
                ));
            }
        }
    }

    private List<Punto> de(UUID dueno) {
        List<Punto> lista = new ArrayList<>();
        for (Punto punto : puntos.values()) {
            if (dueno.equals(punto.dueno)) {
                lista.add(punto);
            }
        }
        return lista;
    }

    private int cuantas(UUID dueno) {
        return de(dueno).size();
    }

    private List<String> nombres(UUID dueno) {
        List<String> lista = new ArrayList<>();
        for (Punto punto : de(dueno)) {
            lista.add(punto.nombre);
        }
        return lista;
    }

    private Punto enSitio(Block bloque) {
        if (bloque == null || bloque.getWorld() == null) {
            return null;
        }
        String mundo = bloque.getWorld().getName();
        for (Punto punto : puntos.values()) {
            if (punto.mundo.equals(mundo) && punto.bx == bloque.getX() && punto.by == bloque.getY() && punto.bz == bloque.getZ()) {
                return punto;
            }
        }
        return null;
    }

    private Punto deBloque(Block bloque) {
        if (bloque == null || !(bloque.getState() instanceof Banner banner)) {
            return null;
        }
        String id = banner.getPersistentDataContainer().get(ID, PersistentDataType.STRING);
        if (id != null) {
            try {
                Punto punto = puntos.get(UUID.fromString(id));
                if (punto != null) {
                    return punto;
                }
            } catch (IllegalArgumentException ignored) {
                // El bloque no es una bandera de spawn.
            }
        }
        return enSitio(bloque);
    }

    void recogerMundos(List<String> mundos) {
        if (mundos == null || mundos.isEmpty()) {
            return;
        }
        List<Punto> dentro = new ArrayList<>();
        for (Punto punto : puntos.values()) {
            if (mundos.contains(punto.mundo)) {
                dentro.add(punto);
            }
        }
        recoger(dentro, "§eEse mundo se ha renovado. La bandera §f", true);
    }

    void recogerZona(String mundo, int minX, int minZ, int maxX, int maxZ) {
        if (mundo == null) {
            return;
        }
        List<Punto> dentro = new ArrayList<>();
        for (Punto punto : puntos.values()) {
            if (!mundo.equals(punto.mundo) || punto.bx < minX || punto.bx > maxX || punto.bz < minZ || punto.bz > maxZ) {
                continue;
            }
            dentro.add(punto);
        }
        recoger(dentro, "§eLa bandera §f", false);
    }

    private void recoger(List<Punto> dentro, String aviso, boolean alBarrio) {
        if (dentro.isEmpty()) {
            return;
        }
        for (Punto punto : dentro) {
            quitarBloque(punto);
            puntos.remove(punto.id);
            entregar(punto.dueno, punto.nombre, aviso + punto.nombre + "§e ha vuelto a ti.", alBarrio);
        }
        guardar();
    }

    private void entregar(UUID dueno, String nombre, String aviso, boolean alBarrio) {
        ItemStack bandera = item(nombre);
        Player online = plugin.getServer().getPlayer(dueno);
        if (online == null || !online.isOnline()) {
            pendientes.computeIfAbsent(dueno, id -> new ArrayList<>()).add(nombreVisible(nombre));
            return;
        }
        Map<Integer, ItemStack> sobra = online.getInventory().addItem(bandera);
        online.sendMessage(aviso);
        if (sobra.isEmpty()) {
            return;
        }
        if (alBarrio && soltarEnBarrio(sobra.values())) {
            online.sendMessage("§eNo te cabía. Ha caído en el barrio.");
            return;
        }
        for (ItemStack resto : sobra.values()) {
            online.getWorld().dropItemNaturally(online.getLocation(), resto);
        }
        online.sendMessage("§eNo te cabía. Ha caído al suelo.");
    }

    private boolean soltarEnBarrio(Collection<ItemStack> items) {
        World barrio = plugin.getServer().getWorld("world");
        if (barrio == null) {
            return false;
        }
        Location sitio = barrio.getSpawnLocation();
        for (ItemStack item : items) {
            barrio.dropItemNaturally(sitio, item);
        }
        return true;
    }

    private String nombreVisible(String nombre) {
        return nombre == null || nombre.isBlank() ? "Bandera" : nombre;
    }

    private boolean puedeQuitar(Player jugador, Punto punto) {
        return punto.dueno.equals(jugador.getUniqueId())
            || jugador.isOp()
            || jugador.hasPermission("viaje.colocar")
            || jugador.hasPermission("parcelas.admin");
    }

    private boolean protege(Block bloque) {
        return deBloque(bloque) != null || !apoyadas(bloque).isEmpty();
    }

    private List<Punto> apoyadas(Block suelo) {
        List<Punto> lista = new ArrayList<>();
        if (suelo == null) {
            return lista;
        }
        Block arriba = suelo.getRelative(BlockFace.UP);
        if (!(arriba.getBlockData() instanceof Directional)) {
            Punto dePie = deBloque(arriba);
            if (dePie != null) {
                lista.add(dePie);
            }
        }
        BlockFace[] caras = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace cara : caras) {
            Block vecino = suelo.getRelative(cara);
            if (!(vecino.getBlockData() instanceof Directional dir) || dir.getFacing() != cara) {
                continue;
            }
            Punto punto = deBloque(vecino);
            if (punto != null) {
                lista.add(punto);
            }
        }
        return lista;
    }

    private void quitarBloque(Punto punto) {
        World mundo = plugin.getServer().getWorld(punto.mundo);
        if (mundo == null) {
            return;
        }
        Block bloque = mundo.getBlockAt(punto.bx, punto.by, punto.bz);
        Punto ahi = deBloque(bloque);
        if (ahi != null && ahi.id.equals(punto.id)) {
            bloque.setType(Material.AIR, false);
        }
    }

    private boolean mueve(List<Block> bloques) {
        for (Block bloque : bloques) {
            if (protege(bloque)) {
                return true;
            }
        }
        return false;
    }

    private void marcar(Block bloque, UUID id) {
        if (!(bloque.getState() instanceof Banner banner)) {
            return;
        }
        banner.getPersistentDataContainer().set(ID, PersistentDataType.STRING, id.toString());
        banner.update(true, false);
    }

    private boolean esItem(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("BANNER") || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(CLAVE, PersistentDataType.BYTE);
        return marca != null && marca == (byte) 1;
    }

    private String escrito(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return "";
        }
        return item.getItemMeta().getDisplayName();
    }

    private ItemStack item(String nombre) {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = item.getItemMeta();
        String visible = nombre == null || nombre.isBlank() ? "Bandera" : nombre;
        meta.displayName(Component.text(visible, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Pónla donde quieras.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Crea un punto en el menú de Spawn.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(CLAVE, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void dar(Player jugador, ItemStack item) {
        for (ItemStack sobra : jugador.getInventory().addItem(item).values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobra);
        }
    }

    private float yawDe(Block bloque, Player jugador) {
        if (bloque.getBlockData() instanceof Rotatable rot) {
            return yawDe(rot.getRotation());
        }
        if (bloque.getBlockData() instanceof Directional dir) {
            return yawDe(dir.getFacing());
        }
        return jugador.getLocation().getYaw();
    }

    private float yawDe(BlockFace cara) {
        return switch (cara) {
            case SOUTH -> 0f;
            case SOUTH_WEST -> 45f;
            case WEST -> 90f;
            case NORTH_WEST -> 135f;
            case NORTH -> 180f;
            case NORTH_EAST -> -135f;
            case EAST -> -90f;
            case SOUTH_EAST -> -45f;
            default -> 0f;
        };
    }

    static final class Punto {
        final UUID id;
        final UUID dueno;
        final String nombre;
        final String mundo;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final int bx;
        final int by;
        final int bz;

        private Punto(UUID id, UUID dueno, String nombre, String mundo, double x, double y, double z, float yaw, int bx, int by, int bz) {
            this.id = id;
            this.dueno = dueno;
            this.nombre = nombre;
            this.mundo = mundo;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
        }
    }
}
