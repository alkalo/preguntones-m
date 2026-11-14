package com.preguntones.susto;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SustoPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final int CADA_MINUTOS = 60;
    private static final long LLEVA_JUGANDO = 60L * 60L * 1000L;
    private static final long PELEA = 20L * 1000L;
    private static final long PANTALLA = 30L;

    private final Map<UUID, Long> llegada = new HashMap<>();
    private final Map<UUID, Long> golpe = new HashMap<>();
    private final Map<UUID, BufferedImage> caras = new HashMap<>();
    private final Map<UUID, Integer> marcas = new HashMap<>();
    private final Map<UUID, Sesion> sesiones = new HashMap<>();
    private final Set<UUID> conPack = ConcurrentHashMap.newKeySet();
    private final Random azar = new Random();
    private NamespacedKey clave;
    private Ritmo ritmo;
    private MapView mapa;
    private UUID ultimo;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        clave = new NamespacedKey(this, "susto");
        getConfig().set("minutos-min", CADA_MINUTOS);
        getConfig().set("minutos-max", CADA_MINUTOS);
        saveConfig();
        ritmo = new Ritmo(azar, CADA_MINUTOS, CADA_MINUTOS);
        limpiarCaras();
        long ahora = System.currentTimeMillis();
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            llegada.put(jugador.getUniqueId(), ahora);
            quitarMapa(jugador);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("susto") != null) {
            getCommand("susto").setExecutor(this);
            getCommand("susto").setTabCompleter(this);
        }
        getLogger().info("Susto activo. Solo sale con /susto.");
    }

    @Override
    public void onDisable() {
        for (Player jugador : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            cortar(jugador);
        }
        limpiarCaras();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String etiqueta, String[] args) {
        Player objetivo;
        if (args.length == 0) {
            if (!(sender instanceof Player jugador)) {
                sender.sendMessage("§eEntra al juego para probarlo, o pon un nombre.");
                return true;
            }
            objetivo = jugador;
        } else {
            objetivo = Bukkit.getPlayerExact(args[0]);
            if (objetivo == null) {
                sender.sendMessage("§eEse jugador no está.");
                return true;
            }
        }
        if (!asustar(objetivo)) {
            sender.sendMessage("§eTodavía está el susto.");
        } else if (sender != objetivo) {
            sender.sendMessage("§eHecho.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefijo = args[0].toLowerCase(java.util.Locale.ROOT);
        List<String> nombres = new ArrayList<>();
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            if (jugador.getName().toLowerCase(java.util.Locale.ROOT).startsWith(prefijo)) {
                nombres.add(jugador.getName());
            }
        }
        return nombres;
    }

    private void pulso() {
        boolean hayJuego = false;
        List<Player> candidatos = new ArrayList<>();
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            if (enJuego(jugador)) {
                hayJuego = true;
            }
            if (sirve(jugador)) {
                candidatos.add(jugador);
            }
        }
        if (!ritmo.pulso(hayJuego)) {
            return;
        }
        if (candidatos.isEmpty()) {
            ritmo.aplazar();
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (Player jugador : candidatos) {
            ids.add(jugador.getUniqueId());
        }
        UUID id = Elegir.uno(ids, ultimo, azar);
        if (id == null) {
            return;
        }
        Player elegido = Bukkit.getPlayer(id);
        if (elegido != null) {
            asustar(elegido);
        }
    }

    private boolean sirve(Player jugador) {
        if (!enJuego(jugador)) {
            return false;
        }
        if (!jugador.hasPermission("susto.recibir") || sesiones.containsKey(jugador.getUniqueId())) {
            return false;
        }
        Long entro = llegada.get(jugador.getUniqueId());
        if (entro == null || System.currentTimeMillis() - entro < LLEVA_JUGANDO) {
            return false;
        }
        Long cuando = golpe.get(jugador.getUniqueId());
        if (cuando != null && System.currentTimeMillis() - cuando < PELEA) {
            return false;
        }
        InventoryType tipo = jugador.getOpenInventory().getType();
        return tipo == InventoryType.CRAFTING || tipo == InventoryType.CREATIVE;
    }

    private boolean asustar(Player jugador) {
        UUID id = jugador.getUniqueId();
        if (sesiones.containsKey(id)) {
            return false;
        }
        int marca = marcas.merge(id, 1, Integer::sum);
        Sesion sesion = new Sesion(marca);
        sesiones.put(id, sesion);
        boolean pack = conPack.contains(id) || jugador.getResourcePackStatus() == Status.SUCCESSFULLY_LOADED;
        tapar(jugador, sesion);
        if (pack) {
            jugador.playSound(jugador, Gritos.GRITO, SoundCategory.MASTER, 1f, 1f);
        } else {
            jugador.playSound(jugador, Sound.ENTITY_GOAT_SCREAMING_DEATH, SoundCategory.MASTER, 1f, 1f);
            ofrecerPack(jugador);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!sigue(id, marca) || !jugador.isOnline()) {
                return;
            }
            if (pack) {
                jugador.stopSound(Gritos.GRITO, SoundCategory.MASTER);
            } else {
                jugador.stopSound(Sound.ENTITY_GOAT_SCREAMING_DEATH, SoundCategory.MASTER);
            }
            cortar(jugador);
        }, PANTALLA);
        ultimo = id;
        getLogger().info("Susto a " + jugador.getName());
        return true;
    }

    private void tapar(Player jugador, Sesion sesion) {
        ItemStack casco = jugador.getInventory().getHelmet();
        sesion.casco = casco == null || casco.getType().isAir() || esCalabaza(casco)
            ? new ItemStack(Material.AIR)
            : casco.clone();
        jugador.getInventory().setHelmet(calabaza());
    }

    private ItemStack calabaza() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(clave, PersistentDataType.BYTE, (byte) 1);
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    private void forzar(Player jugador, String sonido) {
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 1f);
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 0.97f);
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 1.03f);
    }

    private void forzar(Player jugador, Sound sonido) {
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 1f);
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 0.97f);
        jugador.playSound(jugador, sonido, SoundCategory.MASTER, 1f, 1.03f);
    }

    private boolean sigue(UUID id, int marca) {
        Sesion sesion = sesiones.get(id);
        return sesion != null && sesion.marca == marca;
    }

    private void cortar(Player jugador) {
        Sesion sesion = sesiones.remove(jugador.getUniqueId());
        if (sesion == null) {
            return;
        }
        quitarMapa(jugador);
        if (esCalabaza(jugador.getInventory().getHelmet())) {
            jugador.getInventory().setHelmet(sesion.casco);
        }
    }

    private void limpiarCaras() {
        for (World mundo : Bukkit.getWorlds()) {
            for (ItemDisplay entidad : mundo.getEntitiesByClass(ItemDisplay.class)) {
                if (entidad.getScoreboardTags().contains("susto")) {
                    entidad.remove();
                }
            }
        }
    }

    private ItemStack modelo(int indice) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("preguntones", "susto_" + indice));
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    private void quitarMapa(Player jugador) {
        PlayerInventory inventario = jugador.getInventory();
        for (int i = 0; i < inventario.getSize(); i++) {
            if (esMapa(inventario.getItem(i))) {
                inventario.setItem(i, null);
            }
        }
        if (esMapa(inventario.getItemInOffHand())) {
            inventario.setItemInOffHand(null);
        }
    }

    private void prepararMapa(World mundo) {
        if (mapa != null || mundo == null) {
            return;
        }
        int id = getConfig().getInt("mapa", -1);
        if (id >= 0) {
            mapa = Bukkit.getMap(id);
        }
        if (mapa == null) {
            mapa = Bukkit.createMap(mundo);
            getConfig().set("mapa", mapa.getId());
            saveConfig();
        }
        mapa.setTrackingPosition(false);
        mapa.setUnlimitedTracking(false);
        mapa.setScale(MapView.Scale.CLOSEST);
        for (MapRenderer renderer : new ArrayList<>(mapa.getRenderers())) {
            mapa.removeRenderer(renderer);
        }
        mapa.addRenderer(new MapRenderer(true) {
            @Override
            public void render(MapView view, MapCanvas canvas, Player jugador) {
                BufferedImage cara = caras.get(jugador.getUniqueId());
                if (cara != null) {
                    canvas.drawImage(0, 0, cara);
                }
            }
        });
    }

    private ItemStack papel() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(mapa);
        meta.setHideTooltip(true);
        meta.getPersistentDataContainer().set(clave, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean esMapa(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(clave, PersistentDataType.BYTE);
        return marca != null && marca == 1;
    }

    private boolean enJuego(Player jugador) {
        return jugador.isOnline() && !jugador.isDead() && jugador.getGameMode() != GameMode.SPECTATOR;
    }

    private void marcarGolpe(Entity entidad) {
        if (entidad instanceof Player jugador) {
            golpe.put(jugador.getUniqueId(), System.currentTimeMillis());
        } else if (entidad instanceof Projectile proyectil && proyectil.getShooter() instanceof Player jugador) {
            golpe.put(jugador.getUniqueId(), System.currentTimeMillis());
        }
    }

    private boolean enSusto(Player jugador) {
        return sesiones.containsKey(jugador.getUniqueId());
    }

    private boolean esCalabaza(ItemStack item) {
        if (item == null || item.getType() != Material.CARVED_PUMPKIN || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(clave, PersistentDataType.BYTE);
        return marca != null && marca == 1;
    }

    private void ofrecerPack(Player jugador) {
        String url = getConfig().getString("pack-url", "https://alkalo.github.io/preguntones-m/pack/cofre-venta.zip?v=7");
        String hash = getConfig().getString("pack-sha1", "205cac961bfa4acf1782249d210891137609ed52");
        if (url.isBlank() || hash.isBlank()) {
            return;
        }
        jugador.setResourcePack(
            UUID.fromString(getConfig().getString("pack-id", "7c1e4a2b-6d58-4f0e-9b33-a1d6e5c84f27")),
            url,
            hash,
            net.kyori.adventure.text.Component.text("Este pack muestra el cofre de venta."),
            true
        );
    }

    @EventHandler
    public void alPack(PlayerResourcePackStatusEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Status estado = event.getStatus();
        if (estado == Status.SUCCESSFULLY_LOADED) {
            conPack.add(id);
        } else if (estado == Status.DECLINED || estado == Status.FAILED_DOWNLOAD
            || estado == Status.INVALID_URL || estado == Status.FAILED_RELOAD
            || estado == Status.DISCARDED) {
            conPack.remove(id);
        }
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        llegada.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        quitarMapa(event.getPlayer());
        if (esCalabaza(event.getPlayer().getInventory().getHelmet())) {
            event.getPlayer().getInventory().setHelmet(null);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player jugador = event.getPlayer();
            if (jugador.isOnline() && jugador.getResourcePackStatus() != Status.SUCCESSFULLY_LOADED) {
                ofrecerPack(jugador);
            }
        }, 40L);
    }

    @EventHandler
    public void alCasco(PlayerArmorChangeEvent event) {
        if (event.getSlot() != org.bukkit.inventory.EquipmentSlot.HEAD || !enSusto(event.getPlayer())) {
            return;
        }
        if (esCalabaza(event.getNewItem())) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (enSusto(event.getPlayer())) {
                event.getPlayer().getInventory().setHelmet(calabaza());
            }
        });
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        cortar(event.getPlayer());
        llegada.remove(event.getPlayer().getUniqueId());
        golpe.remove(event.getPlayer().getUniqueId());
        conPack.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void alMundo(PlayerChangedWorldEvent event) {
        cortar(event.getPlayer());
    }

    @EventHandler
    public void alMorir(PlayerDeathEvent event) {
        Player jugador = event.getEntity();
        Sesion sesion = sesiones.get(jugador.getUniqueId());
        if (sesion == null) {
            return;
        }
        if (sesion.casco != null && !event.getKeepInventory()) {
            List<ItemStack> drops = event.getDrops();
            for (int i = 0; i < drops.size(); i++) {
                if (esCalabaza(drops.get(i))) {
                    drops.set(i, sesion.casco.getType().isAir() ? null : sesion.casco.clone());
                }
            }
            drops.removeIf(item -> item == null || item.getType().isAir());
            sesion.casco = null;
            if (esCalabaza(jugador.getInventory().getHelmet())) {
                jugador.getInventory().setHelmet(null);
            }
        }
        cortar(jugador);
    }

    @EventHandler(ignoreCancelled = true)
    public void alGolpe(EntityDamageByEntityEvent event) {
        marcarGolpe(event.getEntity());
        marcarGolpe(event.getDamager());
    }

    @EventHandler(ignoreCancelled = true)
    public void alSoltar(PlayerDropItemEvent event) {
        if (esMapa(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alCambiar(PlayerSwapHandItemsEvent event) {
        if (enSusto(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alElegir(PlayerItemHeldEvent event) {
        if (enSusto(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador) || !enSusto(jugador)) {
            return;
        }
        ItemStack hotbar = event.getHotbarButton() >= 0 ? jugador.getInventory().getItem(event.getHotbarButton()) : null;
        if (esMapa(event.getCurrentItem()) || esMapa(event.getCursor()) || esMapa(hotbar)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alArrastrar(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player jugador
            && enSusto(jugador)
            && esMapa(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    private static final class Sesion {
        private final int marca;
        private ItemStack casco;

        private Sesion(int marca) {
            this.marca = marca;
        }
    }
}
