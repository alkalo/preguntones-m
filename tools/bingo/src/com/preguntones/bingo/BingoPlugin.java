package com.preguntones.bingo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;

public final class BingoPlugin extends JavaPlugin implements Listener, TabCompleter {
    private static final long SESION_MS = 60L * 60L * 1000L;
    private static final Set<Material> PIEDRA = Set.of(
        Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE
    );
    private static final Set<Material> TIERRA = Set.of(
        Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.ROOTED_DIRT,
        Material.MUD, Material.PODZOL, Material.DIRT_PATH, Material.FARMLAND
    );
    private static final Set<Material> ANTORCHAS = Set.of(
        Material.TORCH, Material.WALL_TORCH, Material.SOUL_TORCH, Material.SOUL_WALL_TORCH
    );

    private final Map<UUID, Hecho> hechos = new HashMap<>();
    private final Set<UUID> enCaida = new HashSet<>();
    private Pantalla pantalla;
    private Economy economia;
    private List<Casilla> carta = List.of();
    private String semilla = "";
    private long abiertaHasta;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pantalla = new Pantalla(this);
        if (getCommand("bingo") != null) {
            getCommand("bingo").setExecutor(this);
            getCommand("bingo").setTabCompleter(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        cargar();
        prepararEconomia();
        getServer().getScheduler().runTaskTimer(this, this::latido, 10L, 10L);
        getLogger().info("Bingo listo. /bingo empezar abre una sesión de una hora.");
    }

    @Override
    public void onDisable() {
        guardar();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("empezar")) {
            if (!sender.hasPermission("bingo.admin")) {
                sender.sendMessage("§eEl bingo lo abre un admin.");
                return true;
            }
            if (abierta()) {
                sender.sendMessage("§eEl bingo ya está abierto.");
                return true;
            }
            empezar();
            sender.sendMessage("§eHas abierto una sesión del bingo. Dura una hora.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("parar")) {
            if (!sender.hasPermission("bingo.admin")) {
                sender.sendMessage("§eEl bingo lo abre un admin.");
                return true;
            }
            if (!abierta()) {
                sender.sendMessage("§eEl bingo ya está cerrado.");
                return true;
            }
            cerrar("La sesión del bingo se ha cerrado.");
            sender.sendMessage("§eSesión cerrada.");
            return true;
        }
        if (args.length > 0) {
            if (sender.hasPermission("bingo.admin")) {
                sender.sendMessage("§e/bingo empezar abre una sesión de una hora. /bingo parar la cierra.");
            } else {
                sender.sendMessage("§eEl bingo lo abre un admin.");
            }
            return true;
        }
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§e/bingo empezar abre una sesión de una hora. /bingo parar la cierra.");
            return true;
        }
        if (!abierta()) {
            jugador.sendMessage("§eEl bingo no está abierto.");
            return true;
        }
        pantalla.abrir(jugador);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("bingo.admin") || args.length != 1) {
            return List.of();
        }
        String escrito = args[0].toLowerCase(Locale.ROOT);
        List<String> opciones = new ArrayList<>();
        if ("empezar".startsWith(escrito)) {
            opciones.add("empezar");
        }
        if ("parar".startsWith(escrito)) {
            opciones.add("parar");
        }
        return opciones;
    }

    List<Casilla> carta() {
        return carta;
    }

    boolean abierta() {
        return abiertaHasta > System.currentTimeMillis() && !carta.isEmpty();
    }

    String queda() {
        long minutos = Math.max(1L, (abiertaHasta - System.currentTimeMillis() + 59_999L) / 60_000L);
        if (minutos == 1L) {
            return "Queda 1 minuto. La carta es la misma para todos.";
        }
        return "Quedan " + minutos + " minutos. La carta es la misma para todos.";
    }

    int duros() {
        int valor = getConfig().getInt("duros", 2);
        return valor < 1 ? 2 : valor;
    }

    int hechas(UUID id) {
        Hecho hecho = hechos.get(id);
        return hecho == null ? 0 : hecho.claves.size();
    }

    boolean hecha(UUID id, Casilla casilla) {
        Hecho hecho = hechos.get(id);
        return hecho != null && hecho.claves.contains(casilla.id);
    }

    boolean cobrado(UUID id) {
        Hecho hecho = hechos.get(id);
        return hecho != null && hecho.cobrado;
    }

    void intentarCobro(Player jugador) {
        if (!abierta()) {
            return;
        }
        Hecho hecho = hechos.get(jugador.getUniqueId());
        if (hecho == null) {
            return;
        }
        cobrar(jugador, hecho);
    }

    private void latido() {
        if (abiertaHasta > 0 && System.currentTimeMillis() >= abiertaHasta) {
            cerrar("La sesión del bingo se ha cerrado.");
        }
        if (!abierta()) {
            return;
        }
        mirarAgua();
        mirarSuelos();
    }

    private void empezar() {
        long ahora = System.currentTimeMillis();
        abiertaHasta = ahora + SESION_MS;
        semilla = Long.toString(ahora);
        carta = Carta.elegir(semilla);
        hechos.clear();
        enCaida.clear();
        guardar();
        getServer().broadcastMessage("§6El bingo está abierto durante una hora. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones. Al llenar la carta, "
            + duros() + " duros.");
    }

    private void cerrar(String aviso) {
        abiertaHasta = 0;
        semilla = "";
        carta = List.of();
        hechos.clear();
        enCaida.clear();
        guardar();
        getServer().broadcastMessage("§6" + aviso);
    }

    private boolean pendiente(Player jugador, Casilla casilla) {
        return abierta() && carta.contains(casilla) && !hecha(jugador.getUniqueId(), casilla);
    }

    private void marcar(Player jugador, Casilla casilla) {
        if (!pendiente(jugador, casilla)) {
            return;
        }
        Hecho hecho = hechos.computeIfAbsent(jugador.getUniqueId(), id -> new Hecho());
        if (!hecho.claves.add(casilla.id)) {
            return;
        }
        int van = hecho.claves.size();
        jugador.sendMessage("§a" + casilla.texto + ". §7" + van + " de " + Carta.TAMANO + ".");
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 1.4f);
        cobrar(jugador, hecho);
        guardar();
    }

    private void cobrar(Player jugador, Hecho hecho) {
        if (!Premio.toca(hecho.claves.size(), Carta.TAMANO, hecho.cobrado)) {
            return;
        }
        if (economia == null) {
            prepararEconomia();
        }
        int premio = duros();
        if (economia == null) {
            jugador.sendMessage("§cBingo. La economía no está lista, y los " + premio + " duros siguen pendientes.");
            return;
        }
        EconomyResponse respuesta = economia.depositPlayer(jugador, premio);
        if (!respuesta.transactionSuccess()) {
            jugador.sendMessage("§cBingo. No he podido darte los duros. Siguen pendientes.");
            return;
        }
        hecho.cobrado = true;
        guardar();
        jugador.sendMessage("§aBingo. §f+" + premio + " duros.");
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
    }

    private void mirarAgua() {
        if (!carta.contains(Casilla.AGUA)) {
            return;
        }
        for (Player jugador : getServer().getOnlinePlayers()) {
            if (jugador.isInWater()) {
                marcar(jugador, Casilla.AGUA);
            }
        }
    }

    private void mirarSuelos() {
        if (enCaida.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(enCaida)) {
            Player jugador = getServer().getPlayer(id);
            if (jugador == null || !jugador.isOnline() || jugador.isDead() || !pendiente(jugador, Casilla.CAIDA)) {
                enCaida.remove(id);
                continue;
            }
            if (jugador.getFallDistance() >= 10f) {
                continue;
            }
            if (jugador.isOnGround() || jugador.isInWater()) {
                enCaida.remove(id);
                marcar(jugador, Casilla.CAIDA);
            }
        }
    }

    private void mirarHorno(Player jugador, FurnaceInventory horno) {
        if (!pendiente(jugador, Casilla.HORNO) || (sirve(horno.getFuel()) && sirve(horno.getSmelting()))) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (jugador.isOnline() && sirve(horno.getFuel()) && sirve(horno.getSmelting())) {
                marcar(jugador, Casilla.HORNO);
            }
        });
    }

    private boolean sirve(ItemStack item) {
        return item != null && !item.getType().isAir();
    }

    private boolean mando(String mensaje, String nombre) {
        String[] partes = mensaje.trim().toLowerCase(Locale.ROOT).split(" ");
        if (partes.length == 0) {
            return false;
        }
        String base = partes[0].startsWith("/") ? partes[0].substring(1) : partes[0];
        if (base.equals(nombre)) {
            return true;
        }
        return base.equals("menu") && partes.length > 1 && partes[1].equals(nombre);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alRomper(BlockBreakEvent event) {
        Material tipo = event.getBlock().getType();
        Player jugador = event.getPlayer();
        if (Tag.LOGS.isTagged(tipo)) {
            marcar(jugador, Casilla.TALA);
        }
        if (PIEDRA.contains(tipo)) {
            marcar(jugador, Casilla.PIEDRA);
        }
        if (TIERRA.contains(tipo)) {
            marcar(jugador, Casilla.TIERRA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPoner(BlockPlaceEvent event) {
        if (ANTORCHAS.contains(event.getBlock().getType())) {
            marcar(event.getPlayer(), Casilla.ANTORCHA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alCraftear(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) {
            return;
        }
        ItemStack resultado = event.getCurrentItem();
        if (resultado == null || resultado.getType().isAir()) {
            return;
        }
        marcar(jugador, Casilla.CRAFTEA);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alClicHorno(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador) || !(event.getInventory() instanceof FurnaceInventory horno)) {
            return;
        }
        if (!(event.getClickedInventory() instanceof FurnaceInventory) && !event.isShiftClick()) {
            return;
        }
        mirarHorno(jugador, horno);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alArrastrarHorno(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador) || !(event.getInventory() instanceof FurnaceInventory horno)) {
            return;
        }
        int arriba = horno.getSize();
        boolean enHorno = false;
        for (int slot : event.getRawSlots()) {
            if (slot < arriba) {
                enHorno = true;
                break;
            }
        }
        if (enHorno) {
            mirarHorno(jugador, horno);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPulsar(PlayerInteractEvent event) {
        Block bloque = event.getClickedBlock();
        if (bloque == null) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK -> {
                if (Tag.BEDS.isTagged(bloque.getType())) {
                    marcar(event.getPlayer(), Casilla.CAMA);
                }
                if (bloque.getType() == Material.NOTE_BLOCK) {
                    marcar(event.getPlayer(), Casilla.NOTA);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alCaer(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player jugador)) {
            return;
        }
        boolean larga = jugador.getFallDistance() >= 10f || enCaida.contains(jugador.getUniqueId());
        if (!larga) {
            return;
        }
        if (event.getFinalDamage() >= jugador.getHealth()) {
            enCaida.remove(jugador.getUniqueId());
            return;
        }
        enCaida.remove(jugador.getUniqueId());
        marcar(jugador, Casilla.CAIDA);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alPescar(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            marcar(event.getPlayer(), Casilla.PESCA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alHablar(AsyncChatEvent event) {
        if (plano(event.message()).isBlank()) {
            return;
        }
        Player jugador = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            if (jugador.isOnline()) {
                marcar(jugador, Casilla.CHAT);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alMandar(PlayerCommandPreprocessEvent event) {
        String mensaje = event.getMessage();
        Player jugador = event.getPlayer();
        if (mando(mensaje, "banco")) {
            marcar(jugador, Casilla.BANCO);
        }
        if (mando(mensaje, "puesto") || mando(mensaje, "tienda") || mando(mensaje, "ajustes")) {
            marcar(jugador, Casilla.TIENDA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alCazar(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player jugador && event.getEntity() instanceof Monster) {
            marcar(jugador, Casilla.CAZA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alMontar(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player jugador)) {
            return;
        }
        if (event.getMount() instanceof Boat || event.getMount() instanceof Animals) {
            marcar(jugador, Casilla.MONTA);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alComer(PlayerItemConsumeEvent event) {
        if (event.getItem().getType().isEdible()) {
            marcar(event.getPlayer(), Casilla.COME);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alMover(PlayerMoveEvent event) {
        Player jugador = event.getPlayer();
        if (jugador.getFallDistance() >= 10f && pendiente(jugador, Casilla.CAIDA)) {
            enCaida.add(jugador.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void alCartel(SignChangeEvent event) {
        for (Component linea : event.lines()) {
            if (!plano(linea).isBlank()) {
                marcar(event.getPlayer(), Casilla.CARTEL);
                return;
            }
        }
    }

    @EventHandler
    public void alMorir(PlayerDeathEvent event) {
        enCaida.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        enCaida.remove(event.getPlayer().getUniqueId());
    }

    private static String plano(Component componente) {
        StringBuilder texto = new StringBuilder();
        if (componente instanceof TextComponent escrito) {
            texto.append(escrito.content());
        }
        for (Component hijo : componente.children()) {
            texto.append(plano(hijo));
        }
        return texto.toString().trim();
    }

    private void prepararEconomia() {
        RegisteredServiceProvider<Economy> registro = getServer().getServicesManager().getRegistration(Economy.class);
        if (registro != null) {
            economia = registro.getProvider();
        }
    }

    private File progreso() {
        return new File(getDataFolder(), "progreso.yml");
    }

    private void cargar() {
        File destino = progreso();
        if (!destino.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(destino);
        long hasta = yaml.getLong("hasta");
        String guardada = yaml.getString("semilla", "");
        if (hasta <= System.currentTimeMillis() || guardada.isBlank()) {
            return;
        }
        abiertaHasta = hasta;
        semilla = guardada;
        carta = Carta.elegir(semilla);
        ConfigurationSection jugadores = yaml.getConfigurationSection("jugadores");
        if (jugadores == null) {
            return;
        }
        for (String clave : jugadores.getKeys(false)) {
            try {
                UUID id = UUID.fromString(clave);
                Hecho hecho = new Hecho();
                hecho.claves.addAll(jugadores.getStringList(clave + ".hechas"));
                hecho.cobrado = jugadores.getBoolean(clave + ".cobrado");
                hechos.put(id, hecho);
            } catch (IllegalArgumentException ignorada) {
                getLogger().warning("Una ficha del bingo no es un jugador: " + clave);
            }
        }
    }

    private void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("hasta", abiertaHasta);
        yaml.set("semilla", semilla);
        for (Map.Entry<UUID, Hecho> entrada : hechos.entrySet()) {
            String ruta = "jugadores." + entrada.getKey();
            yaml.set(ruta + ".hechas", new ArrayList<>(entrada.getValue().claves));
            yaml.set(ruta + ".cobrado", entrada.getValue().cobrado);
        }
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("No pude crear la carpeta del bingo.");
                return;
            }
            yaml.save(progreso());
        } catch (IOException e) {
            getLogger().warning("No pude guardar el bingo: " + e.getMessage());
        }
    }

    private static final class Hecho {
        private final Set<String> claves = new HashSet<>();
        private boolean cobrado;
    }
}
