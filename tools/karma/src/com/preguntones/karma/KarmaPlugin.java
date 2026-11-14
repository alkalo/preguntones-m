package com.preguntones.karma;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import io.papermc.paper.event.player.PlayerTradeEvent;

public final class KarmaPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final long DIA = 86_400_000L;
    private static final NamespacedKey AVISO = new NamespacedKey("viajes", "aviso-panel");

    private final Map<UUID, Ficha> fichas = new HashMap<>();
    private final Map<UUID, Long> golpes = new HashMap<>();
    private final Map<UUID, Trato> tratos = new HashMap<>();
    private final java.util.Set<UUID> teleportes = new java.util.HashSet<>();
    private File archivo;
    private Ajustes ajustes;
    private Duelos duelos;
    private Arena arena;
    private Economy economia;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        archivo = new File(getDataFolder(), "karma.yml");
        cargar();
        ajustes = new Ajustes(this);
        duelos = new Duelos(this);
        arena = new Arena(this);
        prepararEconomia();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(duelos, this);
        getServer().getPluginManager().registerEvents(arena, this);
        if (getCommand("karma") != null) {
            getCommand("karma").setExecutor(this);
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KarmaExpansion(this).register();
        }
        for (World mundo : Bukkit.getWorlds()) {
            aplicarPvp(mundo);
        }
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            asegurarAlta(jugador);
            devolverSiToca(jugador);
        }
        getServer().getScheduler().runTaskTimer(this, duelos::latido, 10L, 10L);
        try {
            arena.preparar();
        } catch (RuntimeException e) {
            getLogger().warning("No he podido preparar la arena: " + e.getMessage());
        }
        getLogger().info("Karma listo. El modo agresivo pide " + diasCuenta() + " días en el servidor.");
    }

    @Override
    public void onDisable() {
        if (duelos != null) {
            duelos.cerrar();
        }
        for (UUID id : new ArrayList<>(tratos.keySet())) {
            cerrarTrato(id);
        }
        guardar();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Entra al servidor para abrir el PVP.");
            return true;
        }
        if (!jugador.hasPermission("karma.usar")) {
            jugador.sendMessage("§cNo puedes abrir el PVP.");
            return true;
        }
        if (args.length == 0) {
            ajustes.abrir(jugador);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "aceptar" -> duelos.aceptar(jugador);
            case "rechazar" -> duelos.rechazar(jugador);
            default -> ajustes.abrir(jugador);
        }
        return true;
    }

    Ajustes ajustes() {
        return ajustes;
    }

    Duelos duelos() {
        return duelos;
    }

    Arena arena() {
        return arena;
    }

    public boolean golpePermitido(Player atacante, Player victima) {
        if (atacante == null || victima == null || atacante.equals(victima)) {
            return false;
        }
        if (duelos != null && (duelos.ocupado(atacante) || duelos.ocupado(victima))) {
            return duelos.golpeEntre(atacante, victima);
        }
        if (esXo(atacante) || esXo(victima)) {
            return false;
        }
        if (karmaNegativo(victima.getUniqueId())) {
            return true;
        }
        if (!cuentaVieja(atacante.getUniqueId()) || !cuentaVieja(victima.getUniqueId())) {
            return false;
        }
        return ficha(victima.getUniqueId()).agresivo || ficha(atacante.getUniqueId()).agresivo;
    }

    boolean karmaNegativo(UUID id) {
        Ficha ficha = fichas.get(id);
        return ficha != null && ficha.karma < 0;
    }

    int puntos(UUID id) {
        Ficha ficha = fichas.get(id);
        return ficha == null ? 0 : ficha.karma;
    }

    boolean cuentaVieja(UUID id) {
        Ficha ficha = fichas.get(id);
        if (ficha == null || ficha.alta <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - ficha.alta >= diasCuenta() * DIA;
    }

    int diasQueFaltan(UUID id) {
        Ficha ficha = fichas.get(id);
        if (ficha == null || ficha.alta <= 0L) {
            return diasCuenta();
        }
        long falta = ficha.alta + diasCuenta() * DIA - System.currentTimeMillis();
        if (falta <= 0L) {
            return 0;
        }
        return (int) ((falta + DIA - 1L) / DIA);
    }

    boolean agresivo(UUID id) {
        Ficha ficha = fichas.get(id);
        return ficha != null && ficha.agresivo;
    }

    boolean cambiarAgresivo(Player jugador) {
        asegurarAlta(jugador);
        if (!cuentaVieja(jugador.getUniqueId())) {
            int dias = diasCuenta();
            int faltan = diasQueFaltan(jugador.getUniqueId());
            String queda = faltan == 1 ? "Te falta 1." : "Te faltan " + faltan + ".";
            jugador.sendMessage("§cEl modo agresivo se abre cuando lleves " + dias
                + (dias == 1 ? " día" : " días") + " en el servidor. " + queda);
            return false;
        }
        Ficha ficha = ficha(jugador.getUniqueId());
        ficha.agresivo = !ficha.agresivo;
        guardar();
        if (ficha.agresivo) {
            jugador.sendMessage("§aModo agresivo activado. §7Cualquiera puede matarte. Matar a quien no lo tenga te baja mucho el karma.");
        } else {
            jugador.sendMessage("§eModo agresivo quitado.");
        }
        return true;
    }

    Etiqueta etiqueta(UUID id) {
        Ficha ficha = fichas.get(id);
        return etiquetaDe(ficha == null ? 0 : ficha.karma);
    }

    private Etiqueta etiquetaDe(int karma) {
        Etiqueta etiqueta = Etiqueta.de(karma, buenoDesde(), maloDesde(), cruelDesde());
        if (karma < 0 && etiqueta == Etiqueta.NORMAL) {
            return Etiqueta.MALO;
        }
        return etiqueta;
    }

    String textoTab(UUID id) {
        Etiqueta etiqueta = etiqueta(id);
        return " &8· " + etiqueta.tab + etiqueta.visible;
    }

    String motivoDe(Player jugador) {
        if (jugador == null || !jugador.isOnline()) {
            return null;
        }
        boolean lava = jugador.isInLava()
            || jugador.getLocation().getBlock().getType() == Material.LAVA
            || jugador.getEyeLocation().getBlock().getType() == Material.LAVA;
        long ahora = System.currentTimeMillis();
        long reciente = golpes.getOrDefault(jugador.getUniqueId(), 0L);
        int segundos = Math.max(1, getConfig().getInt("pelea-segundos", 8));
        boolean pelea = ahora - reciente < segundos * 1000L || mobApuntando(jugador);
        if (lava) {
            return "lava";
        }
        if (ahogandose(jugador)) {
            return "ahogo";
        }
        if (pelea) {
            return "pelea";
        }
        return null;
    }

    String textoAhora(String motivo) {
        return switch (motivo) {
            case "lava" -> "Ahora no. Estás en lava.";
            case "ahogo" -> "Ahora no. Te estás ahogando.";
            case "pelea" -> "Ahora no. Estás peleando.";
            default -> "Ahora no.";
        };
    }

    String textoCorte(String motivo) {
        return switch (motivo) {
            case "lava" -> "Se ha cortado. Estás en lava.";
            case "ahogo" -> "Se ha cortado. Te estás ahogando.";
            case "pelea" -> "Se ha cortado. Estás peleando.";
            default -> "Se ha cortado.";
        };
    }

    String textoCorteAjeno(String nombre, String motivo) {
        return switch (motivo) {
            case "lava" -> "Se ha cortado. " + nombre + " está en lava.";
            case "ahogo" -> "Se ha cortado. " + nombre + " se está ahogando.";
            case "pelea" -> "Se ha cortado. " + nombre + " está peleando.";
            default -> "Se ha cortado.";
        };
    }

    boolean esXo(Player jugador) {
        return jugador.getWorld() != null && jugador.getWorld().getName().equals("xo");
    }

    void marcarTeleporte(UUID id) {
        teleportes.add(id);
    }

    void soltarTeleporte(UUID id) {
        teleportes.remove(id);
    }

    boolean esTeleportePropio(UUID id) {
        return teleportes.contains(id);
    }

    void guardarVuelta(UUID id, Location sitio) {
        Ficha ficha = ficha(id);
        if (sitio == null || sitio.getWorld() == null) {
            ficha.tieneVuelta = false;
            guardar();
            return;
        }
        ficha.tieneVuelta = true;
        ficha.vueltaMundo = sitio.getWorld().getName();
        ficha.vueltaX = sitio.getX();
        ficha.vueltaY = sitio.getY();
        ficha.vueltaZ = sitio.getZ();
        ficha.vueltaYaw = sitio.getYaw();
        ficha.vueltaPitch = sitio.getPitch();
        guardar();
    }

    Location tomarVuelta(UUID id) {
        Ficha ficha = fichas.get(id);
        if (ficha == null || !ficha.tieneVuelta) {
            return null;
        }
        Location sitio = vueltaDe(ficha);
        ficha.tieneVuelta = false;
        guardar();
        return sitio;
    }

    boolean tieneVuelta(UUID id) {
        Ficha ficha = fichas.get(id);
        return ficha != null && ficha.tieneVuelta;
    }

    int entero(String clave, int defecto) {
        return getConfig().getInt(clave, defecto);
    }

    @EventHandler
    public void alCargarMundo(WorldLoadEvent event) {
        aplicarPvp(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void alEntrar(PlayerJoinEvent event) {
        asegurarAlta(event.getPlayer());
        devolverSiToca(event.getPlayer());
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        cerrarTrato(event.getPlayer().getUniqueId());
        golpes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void alPelea(EntityDamageByEntityEvent event) {
        Entity origen = event.getDamager();
        if (origen instanceof Projectile proyectil && proyectil.getShooter() instanceof Entity tirador) {
            origen = tirador;
        }
        if (event.getEntity() instanceof Player herido && origen instanceof LivingEntity && !(origen instanceof Player)) {
            golpes.put(herido.getUniqueId(), System.currentTimeMillis());
            return;
        }
        if (origen instanceof Player atacante && event.getEntity() instanceof LivingEntity vivo
            && !(vivo instanceof Player) && !(vivo instanceof Animals)) {
            golpes.put(atacante.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alDano(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victima)) {
            return;
        }
        Player atacante = causante(event);
        if (atacante == null || atacante.equals(victima)) {
            return;
        }
        if (!golpePermitido(atacante, victima)) {
            event.setCancelled(true);
        } else if (karmaNegativo(victima.getUniqueId())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void alMorirJugador(PlayerDeathEvent event) {
        if (duelos.alMorir(event)) {
            return;
        }
        Player victima = event.getEntity();
        if (esXo(victima)) {
            return;
        }
        Player asesino = victima.getKiller();
        boolean negativo = karmaNegativo(victima.getUniqueId());
        if (negativo) {
            soltarObjetos(event, victima);
            victima.sendMessage("§cKarma negativo. Pierdes lo que llevas encima.");
            if (asesino != null && !asesino.equals(victima)) {
                asesino.sendMessage("§e" + victima.getName() + " suelta lo que llevaba.");
            }
            if (etiqueta(victima.getUniqueId()) == Etiqueta.CRUEL) {
                quitarDuros(victima);
            }
        }
        if (asesino == null || asesino.equals(victima)) {
            return;
        }
        if (!cuentaVieja(asesino.getUniqueId()) || !cuentaVieja(victima.getUniqueId())) {
            if (!negativo) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            }
            return;
        }
        boolean caza = ficha(victima.getUniqueId()).agresivo || negativo;
        boolean porLaCara = ficha(asesino.getUniqueId()).agresivo && !caza;
        if (!caza && !porLaCara) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            return;
        }
        if (caza) {
            sumarKarma(asesino, entero("matar-agresivo", 12), "§aHas cazado a " + victima.getName() + ".");
            if (!negativo) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            }
        } else {
            sumarKarma(asesino, entero("matar-inocente", -25), "§cHas matado a " + victima.getName() + " por la cara.");
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void alMorirMob(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        Player asesino = event.getEntity().getKiller();
        if (asesino == null) {
            return;
        }
        Ficha ficha = preparar(asesino);
        if (ficha.monstruos >= entero("tope-monstruos", 8)) {
            return;
        }
        ficha.monstruos++;
        sumarKarma(asesino, entero("matar-monstruo", 1), null);
    }

    @EventHandler
    public void alComerciar(PlayerTradeEvent event) {
        if (!(event.getVillager() instanceof Villager) || event.isCancelled()) {
            return;
        }
        Ficha ficha = preparar(event.getPlayer());
        if (ficha.tratos >= entero("tope-tratos", 3)) {
            return;
        }
        ficha.tratos++;
        sumarKarma(event.getPlayer(), entero("comerciar", 2), "§aHas comerciado con un aldeano.");
    }

    @EventHandler
    public void alDormir(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() != org.bukkit.Statistic.SLEEP_IN_BED) {
            return;
        }
        Ficha ficha = preparar(event.getPlayer());
        if (ficha.dormido) {
            return;
        }
        ficha.dormido = true;
        sumarKarma(event.getPlayer(), entero("dormir", 2), "§aHas dormido.");
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || event.isCancelled()) {
            return;
        }
        Ficha ficha = preparar(event.getPlayer());
        if (ficha.pescas >= entero("tope-pescas", 4)) {
            return;
        }
        ficha.pescas++;
        sumarKarma(event.getPlayer(), entero("pescar", 1), "§aHas pescado.");
    }

    @EventHandler
    public void alAbrirTrato(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player jugador)) {
            return;
        }
        if (!(event.getInventory() instanceof MerchantInventory inventario)) {
            return;
        }
        if (!(inventario.getMerchant() instanceof Merchant merchant)) {
            return;
        }
        if (!(merchant instanceof Villager)) {
            return;
        }
        if (etiqueta(jugador.getUniqueId()) != Etiqueta.BUENO) {
            return;
        }
        cerrarTrato(jugador.getUniqueId());
        int descuento = Math.max(0, entero("descuento-esmeraldas", 5));
        if (descuento == 0) {
            return;
        }
        List<MerchantRecipe> originales = new ArrayList<>();
        List<MerchantRecipe> nuevas = new ArrayList<>();
        for (MerchantRecipe receta : merchant.getRecipes()) {
            originales.add(copiar(receta));
            nuevas.add(abaratar(receta, descuento));
        }
        merchant.setRecipes(nuevas);
        tratos.put(jugador.getUniqueId(), new Trato(merchant, originales));
    }

    @EventHandler
    public void alCerrarTrato(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player jugador) {
            cerrarTrato(jugador.getUniqueId());
        }
    }

    private void soltarObjetos(PlayerDeathEvent event, Player victima) {
        event.setKeepInventory(false);
        List<ItemStack> encima = new ArrayList<>();
        for (ItemStack item : victima.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                encima.add(item.clone());
            }
        }
        ItemStack cursor = victima.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            encima.add(cursor.clone());
        }
        if (encima.isEmpty()) {
            return;
        }
        event.getDrops().clear();
        event.getDrops().addAll(encima);
        victima.getInventory().clear();
        victima.setItemOnCursor(null);
    }

    private void quitarDuros(Player victima) {
        Economy bolsa = economia();
        int pedido = Math.max(0, entero("cruel-duros", 8));
        if (bolsa == null || pedido == 0) {
            if (bolsa == null) {
                victima.sendMessage("§4Por ser Cruel no he podido quitarte duros.");
            }
            return;
        }
        int tiene = (int) Math.floor(bolsa.getBalance(victima));
        if (tiene <= 0) {
            victima.sendMessage("§4Por ser Cruel no tienes duros.");
            return;
        }
        int quita = Math.min(pedido, tiene);
        double antes = bolsa.getBalance(victima);
        EconomyResponse respuesta = bolsa.withdrawPlayer(victima, quita);
        if (!respuesta.transactionSuccess() || bolsa.getBalance(victima) > antes - quita + 0.5) {
            if (respuesta.transactionSuccess()) {
                bolsa.depositPlayer(victima, quita);
            }
            victima.sendMessage("§4Por ser Cruel no he podido quitarte duros.");
            return;
        }
        victima.sendMessage("§4Por ser Cruel pierdes " + quita + " duros.");
    }

    private void sumarKarma(Player jugador, int cantidad, String porque) {
        if (cantidad == 0) {
            guardar();
            return;
        }
        Ficha ficha = ficha(jugador.getUniqueId());
        int previo = ficha.karma;
        Etiqueta antes = etiquetaDe(previo);
        ficha.karma += cantidad;
        Etiqueta despues = etiquetaDe(ficha.karma);
        guardar();
        if (porque != null) {
            String signo = cantidad > 0 ? "+" + cantidad : Integer.toString(cantidad);
            jugador.sendMessage(porque + " §f" + signo + " de karma.");
        }
        if (antes != despues) {
            jugador.sendMessage("§7Tu karma ahora es " + despues.chat + despues.visible + "§7.");
        }
        if (previo >= 0 && ficha.karma < 0) {
            jugador.sendMessage("§cCon karma negativo te pueden matar. Si mueres, pierdes los objetos.");
        }
        if (despues == Etiqueta.CRUEL && antes != Etiqueta.CRUEL) {
            jugador.sendMessage("§4Si mueres, también pierdes " + Math.max(0, entero("cruel-duros", 8)) + " duros.");
        }
        if (previo < 0 && ficha.karma >= 0) {
            jugador.sendMessage("§aTu karma ya no es negativo.");
        }
    }

    private void prepararEconomia() {
        RegisteredServiceProvider<Economy> registro = getServer().getServicesManager().getRegistration(Economy.class);
        if (registro != null) {
            economia = registro.getProvider();
        }
    }

    private Economy economia() {
        if (economia == null) {
            prepararEconomia();
        }
        return economia;
    }

    private Ficha preparar(Player jugador) {
        asegurarAlta(jugador);
        Ficha ficha = ficha(jugador.getUniqueId());
        String hoy = LocalDate.now(MADRID).toString();
        if (!hoy.equals(ficha.dia)) {
            ficha.dia = hoy;
            ficha.monstruos = 0;
            ficha.tratos = 0;
            ficha.pescas = 0;
            ficha.dormido = false;
        }
        return ficha;
    }

    private void asegurarAlta(Player jugador) {
        Ficha ficha = ficha(jugador.getUniqueId());
        alDiaSiToca(ficha);
        if (ficha.alta > 0L) {
            return;
        }
        boolean viejo = jugador.getPersistentDataContainer().has(AVISO, PersistentDataType.BYTE)
            || logoutAntiguo(jugador.getUniqueId());
        long ahora = System.currentTimeMillis();
        ficha.alta = viejo ? ahora - (diasCuenta() + 1L) * DIA : ahora;
        guardar();
    }

    private void alDiaSiToca(Ficha ficha) {
        String hoy = LocalDate.now(MADRID).toString();
        if (ficha.dia == null || ficha.dia.isEmpty()) {
            ficha.dia = hoy;
            return;
        }
        if (!hoy.equals(ficha.dia)) {
            ficha.dia = hoy;
            ficha.monstruos = 0;
            ficha.tratos = 0;
            ficha.pescas = 0;
            ficha.dormido = false;
        }
    }

    private boolean logoutAntiguo(UUID id) {
        File datos = new File(getDataFolder().getParentFile(), "Essentials/userdata/" + id + ".yml");
        if (!datos.isFile()) {
            return false;
        }
        long logout = YamlConfiguration.loadConfiguration(datos).getLong("timestamps.logout", 0L);
        return logout > 0L && logout < System.currentTimeMillis() - 60_000L;
    }

    private void devolverSiToca(Player jugador) {
        if (!tieneVuelta(jugador.getUniqueId()) || duelos.ocupado(jugador)) {
            return;
        }
        Location sitio = tomarVuelta(jugador.getUniqueId());
        if (sitio == null) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (!jugador.isOnline()) {
                guardarVuelta(jugador.getUniqueId(), sitio);
                return;
            }
            if (duelos.llevar(jugador, sitio)) {
                jugador.sendMessage("§aHas vuelto a tu sitio.");
            } else {
                guardarVuelta(jugador.getUniqueId(), sitio);
            }
        });
    }

    private Location vueltaDe(Ficha ficha) {
        World mundo = Bukkit.getWorld(ficha.vueltaMundo == null ? "" : ficha.vueltaMundo);
        if (mundo == null) {
            mundo = Bukkit.getWorld("world");
            if (mundo == null) {
                return null;
            }
            return new Location(mundo, 0.5, 65, 12.5, 180f, 0f);
        }
        return new Location(mundo, ficha.vueltaX, ficha.vueltaY, ficha.vueltaZ, ficha.vueltaYaw, ficha.vueltaPitch);
    }

    private void aplicarPvp(World mundo) {
        mundo.setPVP(!mundo.getName().equals("xo"));
    }

    private Player causante(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent por) {
            Player directo = comoJugador(por.getDamager());
            if (directo != null) {
                return directo;
            }
        }
        Entity causa = event.getDamageSource().getCausingEntity();
        if (causa instanceof Player jugador) {
            return jugador;
        }
        return causa == null ? null : comoJugador(causa);
    }

    private Player comoJugador(Entity entidad) {
        if (entidad instanceof Player jugador) {
            return jugador;
        }
        if (entidad instanceof Projectile proyectil && proyectil.getShooter() instanceof Player jugador) {
            return jugador;
        }
        if (entidad instanceof AreaEffectCloud nube) {
            ProjectileSource fuente = nube.getSource();
            if (fuente instanceof Player jugador) {
                return jugador;
            }
        }
        return null;
    }

    private boolean ahogandose(Player jugador) {
        boolean cuerpo = jugador.isInWater();
        int aire = jugador.getRemainingAir();
        int aireMax = jugador.getMaximumAir();
        if (cuerpo && aire <= 0) {
            return true;
        }
        return ojosEnAgua(jugador) && aireMax > 0 && aire < aireMax;
    }

    private boolean ojosEnAgua(Player jugador) {
        Block bloque = jugador.getEyeLocation().getBlock();
        Material tipo = bloque.getType();
        if (tipo == Material.WATER || tipo == Material.BUBBLE_COLUMN || tipo == Material.KELP
            || tipo == Material.KELP_PLANT || tipo == Material.SEAGRASS || tipo == Material.TALL_SEAGRASS) {
            return true;
        }
        return bloque.getBlockData() instanceof Waterlogged agua && agua.isWaterlogged();
    }

    private boolean mobApuntando(Player jugador) {
        double bloques = getConfig().getDouble("pelea-bloques", 24);
        double limite = bloques * bloques;
        for (Entity entidad : jugador.getNearbyEntities(bloques, bloques, bloques)) {
            if (!(entidad instanceof org.bukkit.entity.Mob mob)) {
                continue;
            }
            if (!(mob.getTarget() instanceof Player objetivo) || !objetivo.getUniqueId().equals(jugador.getUniqueId())) {
                continue;
            }
            if (mob.getLocation().distanceSquared(jugador.getLocation()) <= limite) {
                return true;
            }
        }
        return false;
    }

    private MerchantRecipe copiar(MerchantRecipe receta) {
        MerchantRecipe copia = new MerchantRecipe(
            receta.getResult().clone(),
            receta.getUses(),
            receta.getMaxUses(),
            receta.hasExperienceReward(),
            receta.getVillagerExperience(),
            receta.getPriceMultiplier(),
            receta.getDemand(),
            receta.getSpecialPrice(),
            receta.shouldIgnoreDiscounts()
        );
        List<ItemStack> ingredientes = new ArrayList<>();
        for (ItemStack ingrediente : receta.getIngredients()) {
            ingredientes.add(ingrediente.clone());
        }
        copia.setIngredients(ingredientes);
        return copia;
    }

    private MerchantRecipe abaratar(MerchantRecipe receta, int descuento) {
        MerchantRecipe copia = copiar(receta);
        List<ItemStack> ingredientes = new ArrayList<>();
        boolean primero = true;
        for (ItemStack ingrediente : copia.getIngredients()) {
            ItemStack clon = ingrediente.clone();
            if (primero && clon.getType() == Material.EMERALD && clon.getAmount() > 1) {
                clon.setAmount(Math.max(1, clon.getAmount() - descuento));
            }
            primero = false;
            ingredientes.add(clon);
        }
        copia.setIngredients(ingredientes);
        return copia;
    }

    private void cerrarTrato(UUID id) {
        Trato trato = tratos.remove(id);
        if (trato == null) {
            return;
        }
        List<MerchantRecipe> actuales = trato.merchant.getRecipes();
        for (int i = 0; i < trato.originales.size() && i < actuales.size(); i++) {
            MerchantRecipe actual = actuales.get(i);
            MerchantRecipe original = trato.originales.get(i);
            original.setUses(actual.getUses());
            original.setDemand(actual.getDemand());
        }
        trato.merchant.setRecipes(trato.originales);
    }

    private int diasCuenta() {
        return Math.max(1, getConfig().getInt("dias-cuenta", 3));
    }

    private int buenoDesde() {
        return getConfig().getInt("bueno-desde", 20);
    }

    private int maloDesde() {
        return getConfig().getInt("malo-desde", -20);
    }

    private int cruelDesde() {
        return getConfig().getInt("cruel-desde", -60);
    }

    private Ficha ficha(UUID id) {
        return fichas.computeIfAbsent(id, clave -> new Ficha());
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection seccion = yaml.getConfigurationSection("jugadores");
        if (seccion == null) {
            return;
        }
        for (String clave : seccion.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(clave);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Karma con un id raro: " + clave);
                continue;
            }
            String base = "jugadores." + clave;
            Ficha ficha = new Ficha();
            ficha.karma = yaml.getInt(base + ".karma", 0);
            ficha.agresivo = yaml.getBoolean(base + ".agresivo", false);
            ficha.alta = yaml.getLong(base + ".alta", 0L);
            ficha.dia = yaml.getString(base + ".dia", "");
            ficha.monstruos = yaml.getInt(base + ".monstruos", 0);
            ficha.tratos = yaml.getInt(base + ".tratos", 0);
            ficha.pescas = yaml.getInt(base + ".pescas", 0);
            ficha.dormido = yaml.getBoolean(base + ".dormido", false);
            ficha.vueltaMundo = yaml.getString(base + ".vuelta.mundo", "");
            ficha.tieneVuelta = ficha.vueltaMundo != null && !ficha.vueltaMundo.isEmpty();
            ficha.vueltaX = yaml.getDouble(base + ".vuelta.x", 0);
            ficha.vueltaY = yaml.getDouble(base + ".vuelta.y", 0);
            ficha.vueltaZ = yaml.getDouble(base + ".vuelta.z", 0);
            ficha.vueltaYaw = (float) yaml.getDouble(base + ".vuelta.yaw", 0);
            ficha.vueltaPitch = (float) yaml.getDouble(base + ".vuelta.pitch", 0);
            fichas.put(id, ficha);
        }
    }

    private void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Ficha> entrada : fichas.entrySet()) {
            String base = "jugadores." + entrada.getKey();
            Ficha ficha = entrada.getValue();
            yaml.set(base + ".karma", ficha.karma);
            yaml.set(base + ".agresivo", ficha.agresivo);
            yaml.set(base + ".alta", ficha.alta);
            yaml.set(base + ".dia", ficha.dia);
            yaml.set(base + ".monstruos", ficha.monstruos);
            yaml.set(base + ".tratos", ficha.tratos);
            yaml.set(base + ".pescas", ficha.pescas);
            yaml.set(base + ".dormido", ficha.dormido);
            if (ficha.tieneVuelta) {
                yaml.set(base + ".vuelta.mundo", ficha.vueltaMundo);
                yaml.set(base + ".vuelta.x", ficha.vueltaX);
                yaml.set(base + ".vuelta.y", ficha.vueltaY);
                yaml.set(base + ".vuelta.z", ficha.vueltaZ);
                yaml.set(base + ".vuelta.yaw", ficha.vueltaYaw);
                yaml.set(base + ".vuelta.pitch", ficha.vueltaPitch);
            }
        }
        try {
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            getLogger().warning("No pude guardar el karma: " + e.getMessage());
        }
    }

    private static final class Ficha {
        private int karma;
        private boolean agresivo;
        private long alta;
        private String dia = "";
        private int monstruos;
        private int tratos;
        private int pescas;
        private boolean dormido;
        private boolean tieneVuelta;
        private String vueltaMundo = "";
        private double vueltaX;
        private double vueltaY;
        private double vueltaZ;
        private float vueltaYaw;
        private float vueltaPitch;
    }

    private static final class Trato {
        private final Merchant merchant;
        private final List<MerchantRecipe> originales;

        private Trato(Merchant merchant, List<MerchantRecipe> originales) {
            this.merchant = merchant;
            this.originales = originales;
        }
    }
}
