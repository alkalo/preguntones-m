package com.preguntones.viajes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ViajesPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final double AL_LADO = 36.0;
    private final Map<UUID, Vuelta> vueltas = new HashMap<>();
    private final Consentir consentir = new Consentir();
    private final Map<UUID, List<UUID>> companeros = new HashMap<>();
    private final Map<UUID, Long> golpes = new HashMap<>();
    private final Set<UUID> buscando = new HashSet<>();
    private final Set<UUID> permitidos = new HashSet<>();
    private final Random azar = new Random();
    private NamespacedKey claveBoton;
    private NamespacedKey claveMuerte;
    private MenuPantalla menu;
    private Banderas banderas;
    private Economy economia;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        claveBoton = new NamespacedKey(this, "volver");
        claveMuerte = new NamespacedKey(this, "muerte");
        prepararEconomia();
        menu = new MenuPantalla(this);
        banderas = new Banderas(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(banderas, this);
        if (getCommand("volver") != null) {
            getCommand("volver").setExecutor(this);
        }
        if (getCommand("viaje") != null) {
            getCommand("viaje").setExecutor(this);
        }
        if (getCommand("menu") != null) {
            getCommand("menu").setExecutor(this);
        }
        getServer().getScheduler().runTaskTimer(this, this::latido, 10L, 10L);
        getServer().getScheduler().runTaskTimer(this, this::quitarBrujulas, 40L, 200L);
        ArrayList<Path> raices = new ArrayList<>();
        raices.add(Path.of("world"));
        raices.add(getServer().getWorldContainer().toPath().resolve("world"));
        World barrio = getServer().getWorld("world");
        if (barrio != null) {
            raices.add(barrio.getWorldFolder().toPath());
        }
        PanelBootstrap.copiarEn(raices, getLogger());
        getServer().getScheduler().runTask(this, this::aplicarBorde);
        getServer().getScheduler().runTaskLater(this, this::aplicarBorde, 200L);
        getLogger().info("Viajes listo. El panel vuelve al spawn en "
            + getConfig().getInt("viaje.espera-segundos", 15) + " s si no hay peligro. La última muerte cuesta "
            + Huella.PRECIO + " duros.");
    }

    @Override
    public void onDisable() {
        if (banderas != null) {
            banderas.guardar();
        }
        for (Vuelta vuelta : vueltas.values()) {
            vuelta.barra.removeAll();
        }
        vueltas.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("menu") || command.getName().equalsIgnoreCase("viaje")) {
            if (!(sender instanceof Player jugador)) {
                sender.sendMessage("Entra al barrio para abrir el Panel Preguntones.");
                return true;
            }
            if (args.length == 0) {
                menu.abrir(jugador);
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "aceptar" -> aceptar(jugador);
                case "rechazar", "no" -> rechazar(jugador);
                case "spawn" -> menu.spawn(jugador);
                case "ajustes", "tienda" -> abrirTienda(jugador);
                case "bandera" -> menu.bandera(jugador);
                case "jugador", "jugadores" -> menu.jugadores(jugador);
                case "survival" -> menu.survival(jugador);
                case "muerte" -> menu.muerte(jugador);
                case "eventos" -> menu.eventos(jugador);
                case "xo" -> irXo(jugador);
                case "parcelas" -> abrirParcelas(jugador);
                case "banco" -> abrirBanco(jugador);
                default -> menu.abrir(jugador);
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("volver")) {
            if (!(sender instanceof Player jugador)) {
                sender.sendMessage("Entra al barrio para volver al spawn.");
                return true;
            }
            empezarVuelta(jugador);
            return true;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void alEscribir(PlayerCommandPreprocessEvent event) {
        String texto = event.getMessage().trim().toLowerCase(Locale.ROOT);
        String[] partes = texto.split(" ");
        String base = partes[0];
        if (Casa.es(event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§eLa casa se guarda con la bandera. §fEn el panel, Bandera. Al ponerla, sale en Spawn.");
            return;
        }
        if (partes.length == 1 && (base.equals("/back") || base.equals("/essentials:back") || base.equals("/e:back"))) {
            event.setCancelled(true);
            menu.muerte(event.getPlayer());
            return;
        }
        if (!base.equals("/spawn") && !base.equals("/volver") && !base.equals("/barrio")) {
            return;
        }
        event.setCancelled(true);
        empezarVuelta(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alTeleport(PlayerTeleportEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (permitidos.contains(id)) {
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
            && llegaAlSpawn(event.getTo())
            && !event.getPlayer().hasPermission("viaje.colocar")) {
            event.setCancelled(true);
            empezarVuelta(event.getPlayer());
            return;
        }
        if (vueltas.containsKey(id)) {
            cortarSilencioso(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alGolpe(EntityDamageByEntityEvent event) {
        Entity origen = event.getDamager();
        if (origen instanceof Projectile proyectil && proyectil.getShooter() instanceof Entity tirador) {
            origen = tirador;
        }
        if (event.getEntity() instanceof Player herido && origen instanceof LivingEntity && !(origen instanceof Player)) {
            marcarPelea(herido);
            if (vueltas.containsKey(herido.getUniqueId())) {
                cortar(herido, textoCorte(Peligro.Motivo.PELEA));
            }
            return;
        }
        if (origen instanceof Player atacante && event.getEntity() instanceof LivingEntity vivo
            && !(vivo instanceof Player) && !(vivo instanceof Animals)) {
            marcarPelea(atacante);
            if (vueltas.containsKey(atacante.getUniqueId())) {
                cortar(atacante, textoCorte(Peligro.Motivo.PELEA));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void alSufrir(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player jugador)) {
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent)) {
            String causa = event.getCause().name();
            if (causa.equals("LAVA") && vueltas.containsKey(jugador.getUniqueId())) {
                cortar(jugador, textoCorte(Peligro.Motivo.LAVA));
            } else if (causa.equals("DROWNING") && vueltas.containsKey(jugador.getUniqueId())) {
                cortar(jugador, textoCorte(Peligro.Motivo.AHOGO));
            }
        }
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            Player jugador = event.getPlayer();
            quitarBrujula(jugador);
            Cuaderno.entregar(this, jugador);
            NamespacedKey visto = new NamespacedKey(this, "aviso-escape");
            if (jugador.getPersistentDataContainer().has(visto, PersistentDataType.BYTE)) {
                return;
            }
            jugador.getPersistentDataContainer().set(visto, PersistentDataType.BYTE, (byte) 1);
            jugador.sendMessage("§6Pulsa Escape. §7Abajo, aparte, el botón dorado es el §6Panel Preguntones§7. O escribe §f/menu§7.");
            jugador.sendMessage("§7Ahí van el spawn, un jugador, el survival, la muerte, los eventos, el PVP, la tienda, la bandera y el banco.");
            jugador.sendMessage("§7Volver tarda 15 segundos. No vale en lava, ahogándote o peleando.");
        }, 20L);
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        buscando.remove(id);
        golpes.remove(id);
        permitidos.remove(id);
        soltarCompaneros(id, "§7" + event.getPlayer().getName() + " ya no está. Te quedas.");
        aplicar(consentir.salir(id));
        cortarSilencioso(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void alMorir(org.bukkit.event.entity.PlayerDeathEvent event) {
        event.getDrops().removeIf(this::esBoton);
        Player jugador = event.getEntity();
        Vuelta viaje = vueltas.get(jugador.getUniqueId());
        boolean iba = viaje != null && viaje.huella != null;
        Location sitio = jugador.getLocation();
        boolean cuenta = sitio.getWorld() != null && Huella.cuenta(sitio.getWorld().getName());
        if (cuenta) {
            anotarMuerte(jugador);
            if (iba) {
                jugador.sendMessage("§7El viaje anterior se ha cortado. No te he cobrado.");
            }
        } else if (iba) {
            jugador.sendMessage("§7El viaje se ha cortado. No te he cobrado.");
        }
        cortarSilencioso(jugador);
    }

    @EventHandler
    public void alRevivir(PlayerRespawnEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> quitarBrujula(event.getPlayer()), 1L);
    }

    @EventHandler
    public void alSoltar(PlayerDropItemEvent event) {
        if (esBoton(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) {
            return;
        }
        boolean mueveBoton = esBoton(event.getCurrentItem()) || esBoton(event.getCursor());
        if (event.getClick() == ClickType.NUMBER_KEY) {
            mueveBoton = mueveBoton || esBoton(jugador.getInventory().getItem(event.getHotbarButton()));
        }
        if (!mueveBoton) {
            return;
        }
        InventoryType tipo = event.getView().getTopInventory().getType();
        if (tipo == InventoryType.CRAFTING || tipo == InventoryType.CREATIVE) {
            int arriba = event.getView().getTopInventory().getSize();
            if (event.getRawSlot() < arriba || event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent event) {
        if (!esBoton(event.getOldCursor())) {
            return;
        }
        int arriba = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < arriba) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void empezarVuelta(Player jugador) {
        empezarVuelta(jugador, null);
    }

    private boolean empezarVuelta(Player jugador, UUID hacia) {
        if (!listoParaSalir(jugador, hacia == null ? "§cNo puedes volver al spawn así." : "§cNo puedes ir así.")) {
            return false;
        }
        if (hacia == null && llegaAlSpawn(jugador.getLocation()) && jugador.getWorld().getEnvironment() == World.Environment.NORMAL) {
            jugador.sendMessage("§eYa estás en el spawn. El Panel Preguntones te lleva al survival.");
            return false;
        }
        String destino = "el spawn";
        String rotulo = null;
        if (hacia != null) {
            Player otro = getServer().getPlayer(hacia);
            destino = otro == null ? "ese jugador" : otro.getName();
            rotulo = destino;
        }
        int espera = Math.max(1, getConfig().getInt("viaje.espera-segundos", 15));
        BossBar barra = Bukkit.createBossBar("Yendo a " + destino + " · " + espera + " s", BarColor.GREEN, BarStyle.SOLID);
        barra.addPlayer(jugador);
        barra.setProgress(1.0);
        vueltas.put(jugador.getUniqueId(), new Vuelta(barra, espera * 2, hacia, null, rotulo));
        jugador.sendActionBar("§bYendo a " + destino + " · " + espera + " s");
        jugador.sendMessage("§aLlegas en " + espera + " segundos. §7Si entras en lava, te ahogas o te ataca un mob, se cancela.");
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        return true;
    }

    private boolean listoParaSalir(Player jugador, String prohibido) {
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage(prohibido);
            return false;
        }
        if (!jugador.isOnline() || jugador.isDead()) {
            return false;
        }
        Vuelta ya = vueltas.get(jugador.getUniqueId());
        if (ya != null) {
            int quedan = Reloj.quedan(ya.totalMedios, ya.medios);
            jugador.sendMessage("§7Ya estás volviendo. Quedan §f" + Math.max(quedan, 1) + " §7segundos.");
            return false;
        }
        Peligro.Motivo motivo = motivoDe(jugador);
        if (motivo != null) {
            jugador.sendMessage("§c" + textoAhora(motivo));
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }
        return true;
    }

    private void latido() {
        aplicar(consentir.caducar(System.currentTimeMillis()));
        for (UUID id : new ArrayList<>(vueltas.keySet())) {
            Player jugador = getServer().getPlayer(id);
            Vuelta vuelta = vueltas.get(id);
            if (jugador == null || !jugador.isOnline() || vuelta == null) {
                if (vuelta != null) {
                    vuelta.barra.removeAll();
                }
                vueltas.remove(id);
                continue;
            }
            Peligro.Motivo motivo = motivoDe(jugador);
            if (motivo != null) {
                cortar(jugador, textoCorte(motivo));
                continue;
            }
            vuelta.medios++;
            if (vuelta.medios >= vuelta.totalMedios) {
                llegar(jugador);
                continue;
            }
            int quedan = Reloj.quedan(vuelta.totalMedios, vuelta.medios);
            String texto = (vuelta.rotulo != null ? "A " + vuelta.rotulo : vuelta.cama != null ? "A tu cama" : vuelta.hacia == null ? "Al spawn" : "Al jugador") + " · " + quedan + " s";
            vuelta.barra.setTitle(texto);
            vuelta.barra.setProgress(Math.max(0.0, (vuelta.totalMedios - vuelta.medios) / (double) vuelta.totalMedios));
            jugador.sendActionBar("§b" + texto);
        }
    }

    private void llegar(Player jugador) {
        Vuelta vuelta = vueltas.get(jugador.getUniqueId());
        UUID hacia = vuelta == null ? null : vuelta.hacia;
        Location cama = vuelta == null ? null : vuelta.cama;
        String rotulo = vuelta == null ? null : vuelta.rotulo;
        Huella huella = vuelta == null ? null : vuelta.huella;
        UUID bandera = vuelta == null ? null : vuelta.bandera;
        Player anfitrion = null;
        cortarSilencioso(jugador);
        Location destino;
        String llegada = "§aHas vuelto al spawn.";
        boolean cobrada = false;
        if (huella != null) {
            Location segura = prepararMuerte(jugador, huella);
            if (segura == null) {
                return;
            }
            if (!cobrar(jugador, Huella.PRECIO)) {
                jugador.sendMessage("§cNo he podido cobrarte. No te he movido.");
                return;
            }
            cobrada = true;
            destino = segura;
            llegada = "§aHas llegado a tu muerte. §f−" + Huella.PRECIO + " duros. Te quedan " + duros(jugador) + ".";
        } else if (bandera != null) {
            Banderas.Punto punto = banderas.buscar(jugador.getUniqueId(), bandera);
            World mundo = punto == null ? null : getServer().getWorld(punto.mundo);
            if (punto == null) {
                jugador.sendMessage("§eEsa bandera ya no está.");
                return;
            }
            if (mundo == null) {
                jugador.sendMessage("§eEse mundo no está listo.");
                return;
            }
            if (!banderas.sigue(punto)) {
                banderas.quitar(punto);
                return;
            }
            destino = enLaBandera(mundo, punto);
            if (destino == null) {
                jugador.sendMessage("§eJunto a esa bandera no cabe nadie.");
                return;
            }
            llegada = "§aHas llegado a " + (rotulo == null ? "tu bandera" : rotulo) + ".";
        } else if (cama != null) {
            if (rotulo == null && !camaSigue(cama)) {
                jugador.sendMessage("§eEsa cama ya no está. Duerme otra vez.");
                return;
            }
            destino = rotulo == null ? sueloCerca(cama) : cama.clone();
            if (destino == null) {
                jugador.sendMessage("§eJunto a esa cama no cabe nadie.");
                return;
            }
            llegada = rotulo == null ? "§aHas llegado a tu cama." : "§aHas llegado a " + rotulo + ".";
        } else if (hacia != null) {
            Player otro = getServer().getPlayer(hacia);
            if (otro == null || !otro.isOnline()) {
                jugador.sendMessage("§cEsa persona ya no está.");
                return;
            }
            destino = otro.getLocation().clone();
            llegada = "§aHas llegado junto a §f" + otro.getName() + "§a.";
            anfitrion = otro;
        } else {
            World mundo = overworld();
            if (mundo == null) {
                jugador.sendMessage("§eEl spawn todavía no está listo.");
                return;
            }
            destino = mundo.getSpawnLocation().clone();
            destino.setX(destino.getBlockX() + 0.5);
            destino.setZ(destino.getBlockZ() + 0.5);
            int tope = mundo.getMaxHeight() - 2;
            while (destino.getY() < tope && destino.getBlock().getType().isSolid()) {
                destino.add(0, 1, 0);
            }
        }
        if (destino.getWorld() == null) {
            if (cobrada) {
                devolver(jugador, Huella.PRECIO);
                jugador.sendMessage("§eEse sitio ya no está. Te devuelvo los duros.");
            } else {
                jugador.sendMessage("§eEse sitio ya no está.");
            }
            return;
        }
        jugador.leaveVehicle();
        jugador.setFallDistance(0f);
        permitidos.add(jugador.getUniqueId());
        boolean bien = jugador.teleport(destino);
        permitidos.remove(jugador.getUniqueId());
        if (!bien) {
            if (cobrada) {
                devolver(jugador, Huella.PRECIO);
                jugador.sendMessage("§eNo he podido dejarte allí. Te devuelvo los duros.");
            } else {
                jugador.sendMessage("§eNo he podido dejarte en el spawn. Prueba otra vez.");
            }
            return;
        }
        if (cobrada) {
            olvidarMuerte(jugador);
        }
        jugador.setFallDistance(0f);
        if (anfitrion != null && anfitrion.isOnline()) {
            anfitrion.sendMessage("§7Ha llegado §f" + jugador.getName() + "§7.");
        }
        jugador.sendMessage(llegada);
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        getServer().getScheduler().runTask(this, () -> quitarBrujula(jugador));
    }

    private void irAZona(Player jugador, List<UUID> vienen) {
        List<UUID> grupo = vienen == null ? List.of() : vienen;
        if (jugador == null || !jugador.isOnline()) {
            avisar(grupo, "§7Esa persona ya no está. Te quedas.");
            return;
        }
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage("§cNo puedes usar el teleportador.");
            avisar(grupo, "§7" + jugador.getName() + " no puede salir. Te quedas.");
            return;
        }
        if (!buscando.add(jugador.getUniqueId())) {
            jugador.sendMessage("§7Sigo buscando.");
            avisar(grupo, "§7" + jugador.getName() + " ya está buscando. Te quedas.");
            return;
        }
        cortarSilencioso(jugador);
        aplicarBorde();
        World mundo = overworld();
        if (mundo == null) {
            buscando.remove(jugador.getUniqueId());
            jugador.sendMessage("§eEl mundo todavía no está listo.");
            avisar(grupo, "§7" + jugador.getName() + " no ha podido salir. Te quedas.");
            return;
        }
        World destino = mundoDeJuego();
        if (destino == null) {
            buscando.remove(jugador.getUniqueId());
            jugador.sendMessage("§eLos mundos de juego todavía no están listos.");
            avisar(grupo, "§7" + jugador.getName() + " no ha podido salir. Te quedas.");
            return;
        }
        if (grupo.isEmpty()) {
            companeros.remove(jugador.getUniqueId());
            jugador.sendMessage("§7Busco un sitio en §f" + destino.getName() + "§7.");
        } else {
            companeros.put(jugador.getUniqueId(), new ArrayList<>(grupo));
            jugador.sendMessage("§7Busco un sitio en §f" + destino.getName() + "§7. Vienen " + lista(grupo) + "§7.");
        }
        buscar(jugador, destino, 0);
    }

    private World mundoDeJuego() {
        String[] nombres = {"survival"};
        for (int i = 0; i < nombres.length; i++) {
            int idx = azar.nextInt(nombres.length);
            World mundo = getServer().getWorld(nombres[idx]);
            if (mundo != null) {
                return mundo;
            }
        }
        return null;
    }

    private void buscar(Player jugador, World mundo, int intento) {
        if (!sigueBuscando(jugador)) {
            return;
        }
        int maximo = Math.max(1, getConfig().getInt("viaje.intentos", 40));
        if (intento >= maximo) {
            abortarBusqueda(jugador, "§eNo he encontrado suelo firme. Pulsa otra vez.");
            return;
        }
        double minimo = getConfig().getDouble("viaje.minimo", 1200);
        double lejos = radioMaximo(mundo);
        if (lejos <= minimo + 10) {
            abortarBusqueda(jugador, "§eEl mundo todavía es demasiado pequeño para salir del barrio.");
            return;
        }
        double[] punto = Radio.punto(azar, minimo, lejos);
        int x = (int) Math.floor(punto[0]);
        int z = (int) Math.floor(punto[1]);
        CompletableFuture<org.bukkit.Chunk> futuro = mundo.getChunkAtAsync(new Location(mundo, x, 0, z), true);
        futuro.thenAccept(chunk -> getServer().getScheduler().runTask(this, () -> {
            if (!isEnabled()) {
                buscando.remove(jugador.getUniqueId());
                companeros.remove(jugador.getUniqueId());
                return;
            }
            if (!sigueBuscando(jugador)) {
                return;
            }
            try {
            Location sitio = sueloFirme(mundo, x, z);
            if (sitio == null || hayReclamo(sitio)) {
                buscar(jugador, mundo, intento + 1);
                return;
            }
            if (!sigueBuscando(jugador)) {
                return;
            }
            sitio.setYaw(jugador.getLocation().getYaw());
            sitio.setPitch(0f);
            jugador.leaveVehicle();
            jugador.setFallDistance(0f);
            permitidos.add(jugador.getUniqueId());
            boolean bien = jugador.teleport(sitio);
            permitidos.remove(jugador.getUniqueId());
            buscando.remove(jugador.getUniqueId());
            if (!bien) {
                soltarCompaneros(jugador.getUniqueId(), "§7" + jugador.getName() + " no ha podido salir. Te quedas.");
                jugador.sendMessage("§eNo he podido dejarte allí. Pulsa otra vez.");
                return;
            }
            jugador.setFallDistance(0f);
            List<UUID> cerca = companeros.remove(jugador.getUniqueId());
            if (cerca != null) {
                int n = 1;
                for (UUID id : cerca) {
                    Player amigo = vivo(id);
                    if (amigo == null) {
                        continue;
                    }
                    Location alLado = sitio.clone().add(n, 0, 0);
                    amigo.leaveVehicle();
                    amigo.setFallDistance(0f);
                    permitidos.add(id);
                    amigo.teleport(alLado);
                    permitidos.remove(id);
                    amigo.setFallDistance(0f);
                    amigo.sendMessage("§7Has salido con §f" + jugador.getName() + "§7 a §f" + sitio.getWorld().getName() + "§7.");
                    n++;
                }
            }
            jugador.setFallDistance(0f);
            jugador.sendMessage("§7Te dejo en §f" + sitio.getWorld().getName() + " §7(" + sitio.getBlockX() + ", " + sitio.getBlockZ() + ").");
            jugador.sendMessage("§7Este mundo se renueva. Las parcelas no. Para guardar un sitio, pon una bandera. §f/volver §7te trae al barrio.");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.1f);
            getServer().getScheduler().runTask(this, () -> quitarBrujula(jugador));
            } catch (RuntimeException error) {
                getLogger().warning("No pude buscar una zona: " + error.getMessage());
                abortarBusqueda(jugador, "§eNo he podido dejarte allí. Pulsa otra vez.");
            }
        }));
        futuro.exceptionally(error -> {
            getServer().getScheduler().runTask(this, () -> {
                if (!isEnabled()) {
                    buscando.remove(jugador.getUniqueId());
                    companeros.remove(jugador.getUniqueId());
                    return;
                }
                abortarBusqueda(jugador, "§eNo he podido cargar ese sitio. Pulsa otra vez.");
            });
            return null;
        });
    }

    private Location sueloFirme(World mundo, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int sx = x + dx;
                int sz = z + dz;
                if ((sx >> 4) != chunkX || (sz >> 4) != chunkZ) {
                    continue;
                }
                Location sitio = columna(mundo, sx, sz);
                if (sitio != null) {
                    return sitio;
                }
            }
        }
        return null;
    }

    private Location columna(World mundo, int x, int z) {
        double minimo = getConfig().getDouble("viaje.minimo", 1200);
        double lejos = radioMaximo(mundo);
        if (Math.hypot(x, z) < minimo || Math.hypot(x, z) > lejos) {
            return null;
        }
        int tope = mundo.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minimoY = Math.max(mundo.getMinHeight() + 2, tope - 16);
        for (int y = tope; y >= minimoY; y--) {
            Block suelo = mundo.getBlockAt(x, y, z);
            if (!pisable(suelo.getType())) {
                continue;
            }
            if (!aireLibre(mundo.getBlockAt(x, y + 1, z)) || !aireLibre(mundo.getBlockAt(x, y + 2, z))) {
                continue;
            }
            String bioma = suelo.getBiome().getKey().getKey();
            if (bioma.contains("ocean") || bioma.contains("river")) {
                return null;
            }
            return new Location(mundo, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private boolean pisable(Material tipo) {
        if (tipo.isAir() || !tipo.isSolid()) {
            return false;
        }
        String nombre = tipo.name();
        if (nombre.contains("LEAVES") || nombre.contains("LOG") || nombre.contains("WOOD")
            || nombre.contains("FENCE") || nombre.contains("WALL") || nombre.contains("DOOR")
            || nombre.contains("SIGN") || nombre.contains("BUTTON") || nombre.contains("PRESSURE")
            || nombre.contains("CARPET") || nombre.contains("BANNER") || nombre.contains("BED")) {
            return false;
        }
        return tipo != Material.CACTUS
            && tipo != Material.MAGMA_BLOCK
            && tipo != Material.POINTED_DRIPSTONE
            && tipo != Material.SWEET_BERRY_BUSH
            && tipo != Material.CAMPFIRE
            && tipo != Material.SOUL_CAMPFIRE
            && tipo != Material.POWDER_SNOW
            && tipo != Material.LAVA
            && tipo != Material.WATER
            && tipo != Material.BARRIER
            && tipo != Material.BEDROCK
            && tipo != Material.END_PORTAL
            && tipo != Material.END_GATEWAY
            && tipo != Material.CRYING_OBSIDIAN;
    }

    private boolean aireLibre(Block bloque) {
        Material tipo = bloque.getType();
        if (tipo == Material.AIR || tipo == Material.CAVE_AIR || tipo == Material.VOID_AIR) {
            return true;
        }
        return tipo == Material.SHORT_GRASS
            || tipo == Material.FERN
            || tipo == Material.SNOW
            || tipo == Material.DEAD_BUSH;
    }

    private double radioMaximo(World mundo) {
        double borde = mundo.getWorldBorder().getSize() / 2.0;
        double margen = getConfig().getDouble("viaje.margen-borde", 400);
        return Math.max(0, borde - margen);
    }

    private boolean hayReclamo(Location sitio) {
        Plugin plugin = getServer().getPluginManager().getPlugin("GriefPrevention");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            Class<?> clase = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object instancia = clase.getField("instance").get(null);
            Object datos = clase.getField("dataStore").get(instancia);
            for (java.lang.reflect.Method metodo : datos.getClass().getMethods()) {
                if (!metodo.getName().equals("getClaimAt") || metodo.getParameterCount() != 3) {
                    continue;
                }
                return metodo.invoke(datos, sitio, false, null) != null;
            }
        } catch (Exception e) {
            getLogger().warning("No pude mirar los claims: " + e.getMessage());
        }
        return false;
    }

    private Peligro.Motivo motivoDe(Player jugador) {
        boolean lava = jugador.isInLava()
            || jugador.getLocation().getBlock().getType() == Material.LAVA
            || jugador.getEyeLocation().getBlock().getType() == Material.LAVA;
        long ahora = System.currentTimeMillis();
        long reciente = golpes.getOrDefault(jugador.getUniqueId(), 0L);
        int segundos = Math.max(1, getConfig().getInt("viaje.pelea-segundos", 8));
        boolean pelea = ahora - reciente < segundos * 1000L || mobApuntando(jugador);
        return Peligro.evaluar(lava, ojosEnAgua(jugador), jugador.isInWater(), jugador.getRemainingAir(), jugador.getMaximumAir(), pelea);
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
        double bloques = getConfig().getDouble("viaje.pelea-bloques", 24);
        double limite = bloques * bloques;
        for (Entity entidad : jugador.getNearbyEntities(bloques, bloques, bloques)) {
            if (!(entidad instanceof Mob mob)) {
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

    private void marcarPelea(Player jugador) {
        golpes.put(jugador.getUniqueId(), System.currentTimeMillis());
    }

    private void cortar(Player jugador, String texto) {
        Vuelta vuelta = vueltas.remove(jugador.getUniqueId());
        if (vuelta == null) {
            return;
        }
        vuelta.barra.removeAll();
        jugador.sendActionBar("§c" + texto);
        jugador.sendMessage("§c" + texto);
        if (vuelta.huella != null) {
            jugador.sendMessage("§7No te he cobrado.");
        }
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private void cortarSilencioso(Player jugador) {
        Vuelta vuelta = vueltas.remove(jugador.getUniqueId());
        if (vuelta != null) {
            vuelta.barra.removeAll();
        }
    }

    private String textoAhora(Peligro.Motivo motivo) {
        return switch (motivo) {
            case LAVA -> "Ahora no. Estás en lava.";
            case AHOGO -> "Ahora no. Te estás ahogando.";
            case PELEA -> "Ahora no. Estás peleando.";
        };
    }

    private String textoCorte(Peligro.Motivo motivo) {
        return switch (motivo) {
            case LAVA -> "Se ha cortado. Estás en lava.";
            case AHOGO -> "Se ha cortado. Te estás ahogando.";
            case PELEA -> "Se ha cortado. Estás peleando.";
        };
    }

    private boolean llegaAlSpawn(Location sitio) {
        if (sitio == null || sitio.getWorld() == null) {
            return false;
        }
        World mundo = overworld();
        if (mundo == null || !sitio.getWorld().equals(mundo)) {
            return false;
        }
        Location spawn = mundo.getSpawnLocation();
        double dx = sitio.getX() - spawn.getX();
        double dz = sitio.getZ() - spawn.getZ();
        return dx * dx + dz * dz < 40 * 40;
    }

    private void quitarBrujulas() {
        for (Player jugador : getServer().getOnlinePlayers()) {
            quitarBrujula(jugador);
        }
    }

    @EventHandler
    public void alCerrar(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player jugador) {
            getServer().getScheduler().runTask(this, () -> quitarBrujula(jugador));
        }
    }

    private void quitarBrujula(Player jugador) {
        if (!jugador.isOnline()) {
            return;
        }
        PlayerInventory inv = jugador.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (esBoton(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        if (esBoton(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
        if (esBoton(jugador.getItemOnCursor())) {
            jugador.setItemOnCursor(null);
        }
        org.bukkit.inventory.Inventory extra = jugador.getEnderChest();
        for (int i = 0; i < extra.getSize(); i++) {
            if (esBoton(extra.getItem(i))) {
                extra.setItem(i, null);
            }
        }
    }

    private boolean esBoton(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(claveBoton, PersistentDataType.BYTE);
        return marca != null && marca == (byte) 1;
    }

    private void abrirTienda(Player jugador) {
        getServer().getScheduler().runTask(this, () -> {
            if (!jugador.isOnline()) {
                return;
            }
            if (!jugador.performCommand("puesto")) {
                jugador.sendMessage("§eLa tienda no está lista.");
            }
        });
    }

    private void abrirBanco(Player jugador) {
        getServer().getScheduler().runTask(this, () -> {
            if (!jugador.isOnline()) {
                return;
            }
            if (!jugador.performCommand("banco")) {
                jugador.sendMessage("§eEl banco no está listo.");
            }
        });
    }

    private void abrirParcelas(Player jugador) {
        getServer().getScheduler().runTask(this, () -> {
            if (!jugador.isOnline()) {
                return;
            }
            if (!jugador.performCommand("parcela panel")) {
                jugador.sendMessage("§eLas parcelas no están listas.");
            }
        });
    }

    void volverSpawn(Player jugador) {
        empezarVuelta(jugador);
    }

    List<Banderas.Punto> puntos(Player jugador) {
        return banderas.vigentes(jugador.getUniqueId());
    }

    void irBandera(Player jugador, UUID id) {
        Banderas.Punto punto = banderas.buscar(jugador.getUniqueId(), id);
        if (punto == null) {
            jugador.sendMessage("§eEsa bandera ya no está.");
            return;
        }
        World mundo = getServer().getWorld(punto.mundo);
        if (mundo == null) {
            jugador.sendMessage("§eEse mundo no está listo.");
            return;
        }
        if (!banderas.sigue(punto)) {
            banderas.quitar(punto);
            return;
        }
        Location sitio = enLaBandera(mundo, punto);
        if (sitio == null) {
            jugador.sendMessage("§eJunto a esa bandera no cabe nadie.");
            return;
        }
        empezarSitio(jugador, sitio, punto.nombre, punto.id);
    }

    private Location enLaBandera(World mundo, Banderas.Punto punto) {
        mundo.getChunkAt(punto.bx >> 4, punto.bz >> 4);
        return sueloCerca(new Location(mundo, punto.bx + 0.5, punto.by, punto.bz + 0.5, punto.yaw, 0f));
    }

    private void empezarSitio(Player jugador, Location sitio, String nombre, UUID bandera) {
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage("§cNo puedes ir así.");
            return;
        }
        if (!jugador.isOnline() || jugador.isDead() || sitio == null || sitio.getWorld() == null) {
            return;
        }
        Vuelta ya = vueltas.get(jugador.getUniqueId());
        if (ya != null) {
            int quedan = Reloj.quedan(ya.totalMedios, ya.medios);
            jugador.sendMessage("§7Ya estás volviendo. Quedan §f" + Math.max(quedan, 1) + " §7segundos.");
            return;
        }
        Peligro.Motivo motivo = motivoDe(jugador);
        if (motivo != null) {
            jugador.sendMessage("§c" + textoAhora(motivo));
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int espera = Math.max(1, getConfig().getInt("viaje.espera-segundos", 15));
        BossBar barra = Bukkit.createBossBar("Yendo a " + nombre + " · " + espera + " s", BarColor.GREEN, BarStyle.SOLID);
        barra.addPlayer(jugador);
        barra.setProgress(1.0);
        vueltas.put(jugador.getUniqueId(), new Vuelta(barra, espera * 2, null, sitio.clone(), nombre, null, bandera));
        jugador.sendActionBar("§bYendo a " + nombre + " · " + espera + " s");
        jugador.sendMessage("§aLlegas en " + espera + " segundos. §7Si entras en lava, te ahogas o te ataca un mob, se cancela.");
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    void irCon(Player jugador, UUID hacia) {
        if (hacia == null || hacia.equals(jugador.getUniqueId())) {
            return;
        }
        if (!listoParaSalir(jugador, "§cNo puedes ir así.")) {
            return;
        }
        Player otro = vivo(hacia);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona ya no está.");
            return;
        }
        List<Consentir.Hecho> efectos = new ArrayList<>();
        Consentir.Peticion peticion = consentir.pedirVisita(jugador.getUniqueId(), hacia, System.currentTimeMillis(), efectos);
        aplicar(efectos);
        if (peticion == null || !otro.isOnline()) {
            return;
        }
        jugador.sendMessage("§7Le he pedido a §f" + otro.getName() + "§7 que acepte. Si dice que sí, tardas 15 segundos.");
        String cuerpo = jugador.getName() + " quiere ir a donde estás. Si aceptas, llega en 15 segundos.";
        menu.pedir(otro, "Ir a donde estás", cuerpo, "Aceptar", "No", "No dejas que venga.");
        decirClic(otro, jugador.getName() + " quiere ir a donde estás. ", "No");
    }

    void irSurvival(Player jugador) {
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage("§cNo puedes usar el teleportador.");
            return;
        }
        if (!jugador.isOnline() || jugador.isDead()) {
            return;
        }
        if (buscando.contains(jugador.getUniqueId())) {
            jugador.sendMessage("§7Sigo buscando.");
            return;
        }
        if (consentir.tieneSalida(jugador.getUniqueId())) {
            jugador.sendMessage("§7Sigo esperando a que acepten.");
            return;
        }
        List<UUID> cerca = new ArrayList<>();
        for (Player otro : jugador.getWorld().getPlayers()) {
            if (otro.equals(jugador) || otro.isDead()) {
                continue;
            }
            if (otro.getLocation().distanceSquared(jugador.getLocation()) <= AL_LADO) {
                cerca.add(otro.getUniqueId());
            }
        }
        if (cerca.isEmpty()) {
            irAZona(jugador, List.of());
            return;
        }
        List<Consentir.Hecho> efectos = consentir.pedirSalida(jugador.getUniqueId(), cerca, System.currentTimeMillis());
        aplicar(efectos);
        if (!jugador.isOnline() || buscando.contains(jugador.getUniqueId())) {
            return;
        }
        if (!consentir.tieneSalida(jugador.getUniqueId())) {
            irAZona(jugador, List.of());
            return;
        }
        List<UUID> esperando = consentir.esperandoDe(jugador.getUniqueId());
        for (UUID id : esperando) {
            Player otro = vivo(id);
            if (otro == null) {
                continue;
            }
            String cuerpo = jugador.getName() + " quiere llevarte al survival. Si aceptas, sales con esa persona.";
            menu.pedir(otro, "Salir al survival", cuerpo, "Aceptar", "Quedarme", "Te quedas donde estás.");
            decirClic(otro, jugador.getName() + " quiere llevarte al survival. ", "Quedarme");
        }
        jugador.sendMessage("§7Quien esté a tu lado tiene que aceptar: " + lista(esperando) + "§7.");
    }

    void aceptar(Player jugador) {
        List<Consentir.Hecho> hechos = consentir.aceptar(jugador.getUniqueId(), System.currentTimeMillis());
        if (hechos.isEmpty()) {
            jugador.sendMessage("§eNo tienes ninguna petición.");
            return;
        }
        aplicar(hechos);
    }

    void rechazar(Player jugador) {
        List<Consentir.Hecho> hechos = consentir.rechazar(jugador.getUniqueId(), System.currentTimeMillis());
        if (hechos.isEmpty()) {
            jugador.sendMessage("§eNo tienes ninguna petición.");
            return;
        }
        aplicar(hechos);
    }

    private void aplicar(List<Consentir.Hecho> hechos) {
        for (Consentir.Hecho hecho : hechos) {
            aplicar(hecho);
        }
    }

    private void aplicar(Consentir.Hecho hecho) {
        switch (hecho.que) {
            case VISITA_ACEPTADA -> visitaAceptada(hecho);
            case VISITA_RECHAZADA -> {
                Player pide = vivo(hecho.pide);
                Player responde = vivo(hecho.responde);
                if (pide != null) {
                    pide.sendMessage("§c" + nombre(hecho.responde) + " no quiere que vayas.");
                }
                if (responde != null) {
                    responde.sendMessage("§7Le has dicho que no a §f" + nombre(hecho.pide) + "§7.");
                }
            }
            case VISITA_CADUCADA -> {
                Player pide = vivo(hecho.pide);
                Player responde = vivo(hecho.responde);
                if (pide != null) {
                    pide.sendMessage("§e" + nombre(hecho.responde) + " no ha respondido.");
                }
                if (responde != null) {
                    responde.sendMessage("§7La petición de §f" + nombre(hecho.pide) + " §7ha caducado.");
                }
            }
            case VISITA_SUSTITUIDA -> {
                Player pide = vivo(hecho.pide);
                if (pide != null) {
                    pide.sendMessage("§e" + nombre(hecho.responde) + " está mirando otra petición.");
                }
            }
            case VISITA_AUSENTE -> {
                Player pide = vivo(hecho.pide);
                if (pide != null) {
                    pide.sendMessage("§c" + nombre(hecho.responde) + " ya no está.");
                }
            }
            case VISITA_RETIRADA -> {
                Player responde = vivo(hecho.responde);
                if (responde != null) {
                    responde.sendMessage("§7" + nombre(hecho.pide) + " ya no quiere ir.");
                }
            }
            case SALIDA_ACEPTADA -> {
                Player lider = vivo(hecho.pide);
                Player responde = vivo(hecho.responde);
                if (responde != null && lider != null) {
                    responde.sendMessage("§aHas aceptado. Sales con §f" + lider.getName() + " §acuando salga.");
                }
                if (lider != null) {
                    lider.sendMessage("§7" + nombre(hecho.responde) + " viene.");
                }
            }
            case SALIDA_RECHAZADA -> {
                Player lider = vivo(hecho.pide);
                Player responde = vivo(hecho.responde);
                if (responde != null) {
                    responde.sendMessage("§7Te quedas.");
                }
                if (lider != null) {
                    lider.sendMessage("§7" + nombre(hecho.responde) + " se queda.");
                }
            }
            case SALIDA_CANCELADA -> {
                Player responde = vivo(hecho.responde);
                if (responde != null) {
                    responde.sendMessage("§7" + nombre(hecho.pide) + " ya no sale. Te quedas.");
                }
            }
            case SALIDA_APARTADA -> {
                Player lider = vivo(hecho.pide);
                if (lider != null) {
                    lider.sendMessage("§7" + nombre(hecho.responde) + " se ha apartado.");
                }
            }
            case SALIDA_AUSENTE -> {
                Player lider = vivo(hecho.pide);
                if (lider != null) {
                    lider.sendMessage("§7" + nombre(hecho.responde) + " ya no está.");
                }
            }
            case GRUPO_LISTO -> salirEnGrupo(hecho);
        }
    }

    private void visitaAceptada(Consentir.Hecho hecho) {
        Player pide = vivo(hecho.pide);
        Player responde = vivo(hecho.responde);
        if (pide == null || responde == null) {
            if (pide != null) {
                pide.sendMessage("§cEsa persona ya no está.");
            }
            if (responde != null) {
                responde.sendMessage("§cEsa persona ya no está.");
            }
            return;
        }
        if (!listoParaSalir(pide, "§cNo puedes ir así.")) {
            responde.sendMessage("§e" + pide.getName() + " no puede salir ahora.");
            return;
        }
        aplicar(consentir.dejarSalida(pide.getUniqueId()));
        if (buscando.remove(pide.getUniqueId())) {
            soltarCompaneros(pide.getUniqueId(), "§7" + pide.getName() + " ya no sale. Te quedas.");
        }
        responde.sendMessage("§aHas aceptado. §f" + pide.getName() + " §7llega en 15 segundos.");
        if (!empezarVuelta(pide, responde.getUniqueId())) {
            responde.sendMessage("§e" + pide.getName() + " no puede salir ahora.");
        }
    }

    private void salirEnGrupo(Consentir.Hecho hecho) {
        Player lider = vivo(hecho.pide);
        if (hecho.responde != null && hecho.aceptados.contains(hecho.responde)) {
            Player responde = vivo(hecho.responde);
            if (responde != null && lider != null) {
                responde.sendMessage("§aSales con §f" + lider.getName() + "§a.");
            }
        } else if (hecho.responde != null && !hecho.callados.contains(hecho.responde)) {
            Player responde = vivo(hecho.responde);
            if (responde != null) {
                responde.sendMessage("§7Te quedas.");
            }
            if (lider != null) {
                lider.sendMessage("§7" + nombre(hecho.responde) + " se queda.");
            }
        }
        if (lider != null) {
            for (UUID id : hecho.callados) {
                lider.sendMessage("§e" + nombre(id) + " no ha respondido y se queda.");
            }
        }
        irAZona(lider, hecho.aceptados);
    }

    private void decirClic(Player quien, String frase, String no) {
        TextColor tinta = TextColor.color(0xC5CED8);
        Component mensaje = Component.text(frase, tinta)
            .append(Component.text("[Aceptar]", TextColor.color(0x8FE3A0))
                .clickEvent(ClickEvent.runCommand("/viaje aceptar"))
                .hoverEvent(HoverEvent.showText(Component.text("Aceptar"))))
            .append(Component.text(" ", tinta))
            .append(Component.text("[" + no + "]", TextColor.color(0xE07A7A))
                .clickEvent(ClickEvent.runCommand("/viaje rechazar"))
                .hoverEvent(HoverEvent.showText(Component.text(no))));
        quien.sendMessage(mensaje);
    }

    private void abortarBusqueda(Player jugador, String aviso) {
        boolean estaba = buscando.remove(jugador.getUniqueId());
        List<UUID> iban = companeros.remove(jugador.getUniqueId());
        if (!estaba && iban == null) {
            return;
        }
        if (aviso != null && jugador.isOnline()) {
            jugador.sendMessage(aviso);
        }
        avisar(iban, "§7" + jugador.getName() + " no ha podido salir. Te quedas.");
    }

    private boolean sigueBuscando(Player jugador) {
        if (!buscando.contains(jugador.getUniqueId())) {
            return false;
        }
        if (!jugador.isOnline()) {
            buscando.remove(jugador.getUniqueId());
            soltarCompaneros(jugador.getUniqueId(), "§7" + jugador.getName() + " ya no está. Te quedas.");
            return false;
        }
        return true;
    }

    private void soltarCompaneros(UUID id, String texto) {
        avisar(companeros.remove(id), texto);
    }

    private void avisar(List<UUID> ids, String texto) {
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Player otro = vivo(id);
            if (otro != null) {
                otro.sendMessage(texto);
            }
        }
    }

    private Player vivo(UUID id) {
        if (id == null) {
            return null;
        }
        Player jugador = getServer().getPlayer(id);
        if (jugador == null || !jugador.isOnline() || jugador.isDead()) {
            return null;
        }
        return jugador;
    }

    private String nombre(UUID id) {
        if (id == null) {
            return "Alguien";
        }
        Player jugador = getServer().getPlayer(id);
        if (jugador != null) {
            return jugador.getName();
        }
        String guardado = getServer().getOfflinePlayer(id).getName();
        return guardado == null ? "Alguien" : guardado;
    }

    private String lista(List<UUID> ids) {
        List<String> nombres = new ArrayList<>();
        for (UUID id : ids) {
            nombres.add(nombre(id));
        }
        if (nombres.isEmpty()) {
            return "nadie";
        }
        if (nombres.size() == 1) {
            return "§f" + nombres.get(0);
        }
        if (nombres.size() == 2) {
            return "§f" + nombres.get(0) + " §7y §f" + nombres.get(1);
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < nombres.size(); i++) {
            if (i > 0) {
                texto.append(i == nombres.size() - 1 ? " §7y " : "§7, ");
            }
            texto.append("§f").append(nombres.get(i));
        }
        return texto.toString();
    }

    Huella huella(Player jugador) {
        if (claveMuerte == null) {
            return null;
        }
        return Huella.leer(jugador.getPersistentDataContainer().get(claveMuerte, PersistentDataType.STRING));
    }

    boolean economiaLista() {
        return economia() != null;
    }

    int duros(Player jugador) {
        Economy bolsa = economia();
        if (bolsa == null || jugador == null) {
            return 0;
        }
        return (int) Math.floor(bolsa.getBalance(jugador));
    }

    void irMuerte(Player jugador) {
        if (!economiaLista()) {
            jugador.sendMessage("§cLa economía no está lista. La muerte sigue guardada.");
            return;
        }
        Huella huella = huella(jugador);
        Location aqui = jugador.getLocation();
        String mundo = aqui.getWorld() == null ? "" : aqui.getWorld().getName();
        if (Huella.junto(huella, mundo, aqui.getBlockX(), aqui.getBlockY(), aqui.getBlockZ())) {
            jugador.sendMessage("§eYa estás junto a esa muerte.");
            return;
        }
        Huella.Paso paso = Huella.pedir(huella, duros(jugador));
        if (paso == Huella.Paso.SIN_SITIO) {
            jugador.sendMessage("§eTodavía no has muerto.");
            return;
        }
        if (paso == Huella.Paso.SIN_DUROS) {
            jugador.sendMessage("§cTe faltan duros. Cuesta " + Huella.PRECIO + ". Tienes " + duros(jugador) + ".");
            return;
        }
        empezarMuerte(jugador, huella);
    }

    private void anotarMuerte(Player jugador) {
        Location sitio = jugador.getLocation();
        if (sitio.getWorld() == null) {
            return;
        }
        Huella nueva = Huella.alMorir(huella(jugador), sitio.getWorld().getName(), sitio.getBlockX(), sitio.getBlockY(), sitio.getBlockZ());
        if (nueva == null || !Huella.cuenta(sitio.getWorld().getName())) {
            return;
        }
        jugador.getPersistentDataContainer().set(claveMuerte, PersistentDataType.STRING, nueva.texto());
        jugador.sendMessage("§7Muerte guardada. §fPanel, Muerte§7: §e" + Huella.PRECIO + " duros§7, una vez.");
    }

    private void olvidarMuerte(Player jugador) {
        jugador.getPersistentDataContainer().remove(claveMuerte);
    }

    private void empezarMuerte(Player jugador, Huella huella) {
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage("§cNo puedes ir así.");
            return;
        }
        if (!jugador.isOnline() || jugador.isDead() || huella == null) {
            return;
        }
        World mundo = getServer().getWorld(huella.mundo);
        if (mundo == null) {
            jugador.sendMessage("§eEse mundo ya no está.");
            return;
        }
        Vuelta ya = vueltas.get(jugador.getUniqueId());
        if (ya != null) {
            int quedan = Reloj.quedan(ya.totalMedios, ya.medios);
            jugador.sendMessage("§7Ya estás volviendo. Quedan §f" + Math.max(quedan, 1) + " §7segundos.");
            return;
        }
        Peligro.Motivo motivo = motivoDe(jugador);
        if (motivo != null) {
            jugador.sendMessage("§c" + textoAhora(motivo));
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int espera = Math.max(1, getConfig().getInt("viaje.espera-segundos", 15));
        Location sitio = new Location(mundo, huella.x + 0.5, huella.y, huella.z + 0.5, jugador.getLocation().getYaw(), 0f);
        BossBar barra = Bukkit.createBossBar("Yendo a tu muerte · " + espera + " s", BarColor.GREEN, BarStyle.SOLID);
        barra.addPlayer(jugador);
        barra.setProgress(1.0);
        vueltas.put(jugador.getUniqueId(), new Vuelta(barra, espera * 2, null, sitio, "tu muerte", huella));
        jugador.sendActionBar("§bYendo a tu muerte · " + espera + " s");
        jugador.sendMessage("§aLlegas en " + espera + " segundos. §7Si entras en lava, te ahogas o te ataca un mob, se cancela y no se cobra.");
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    private Location prepararMuerte(Player jugador, Huella pedida) {
        if (!economiaLista()) {
            jugador.sendMessage("§cLa economía no está lista. No te he cobrado.");
            return null;
        }
        Huella ahora = huella(jugador);
        Huella.Paso paso = Huella.llegar(pedida, ahora, duros(jugador));
        if (paso == Huella.Paso.SIN_SITIO) {
            if (ahora == null) {
                jugador.sendMessage("§eEsa muerte ya no está. No te he cobrado.");
            } else {
                jugador.sendMessage("§eHas muerto otra vez. No te he cobrado.");
            }
            return null;
        }
        if (paso != Huella.Paso.COBRAR) {
            jugador.sendMessage("§cTe faltan duros. Cuesta " + Huella.PRECIO + ". Tienes " + duros(jugador) + ".");
            return null;
        }
        World mundo = getServer().getWorld(pedida.mundo);
        if (mundo == null) {
            jugador.sendMessage("§eEse mundo ya no está. No te he cobrado.");
            return null;
        }
        Location segura;
        try {
            segura = sueloCerca(new Location(mundo, pedida.x, pedida.y, pedida.z, jugador.getLocation().getYaw(), 0f));
        } catch (RuntimeException error) {
            getLogger().warning("No pude mirar la muerte de " + jugador.getName() + ": " + error.getMessage());
            segura = null;
        }
        if (segura == null) {
            jugador.sendMessage("§eNo hay suelo junto a esa muerte. No te he cobrado.");
            return null;
        }
        return segura;
    }

    private Location sueloCerca(Location muerte) {
        World mundo = muerte.getWorld();
        if (mundo == null) {
            return null;
        }
        int cx = muerte.getBlockX();
        int cy = muerte.getBlockY();
        int cz = muerte.getBlockZ();
        for (int radio = 0; radio <= 4; radio++) {
            Location mejor = null;
            int mejorDy = Integer.MAX_VALUE;
            for (int dx = -radio; dx <= radio; dx++) {
                for (int dz = -radio; dz <= radio; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radio) {
                        continue;
                    }
                    Location candidato = columnaMuerte(mundo, cx + dx, cy, cz + dz);
                    if (candidato == null) {
                        continue;
                    }
                    int dy = Math.abs(candidato.getBlockY() - cy);
                    if (dy < mejorDy) {
                        mejorDy = dy;
                        mejor = candidato;
                        mejor.setYaw(muerte.getYaw());
                        mejor.setPitch(0f);
                    }
                }
            }
            if (mejor != null) {
                return mejor;
            }
        }
        return null;
    }

    private Location columnaMuerte(World mundo, int x, int yMuerte, int z) {
        int alto = Math.min(mundo.getMaxHeight() - 2, yMuerte + 6);
        int bajo = Math.max(mundo.getMinHeight() + 1, yMuerte - 12);
        Location mejor = null;
        int mejorDy = Integer.MAX_VALUE;
        for (int y = bajo; y <= alto; y++) {
            if (!apto(mundo, x, y, z)) {
                continue;
            }
            int dy = Math.abs(y - yMuerte);
            if (dy < mejorDy) {
                mejorDy = dy;
                mejor = new Location(mundo, x + 0.5, y, z + 0.5);
            }
        }
        return mejor;
    }

    private boolean apto(World mundo, int x, int y, int z) {
        if (y <= mundo.getMinHeight() || y >= mundo.getMaxHeight() - 1) {
            return false;
        }
        return sueloFirme(mundo.getBlockAt(x, y - 1, z).getType())
            && cabe(mundo.getBlockAt(x, y, z).getType())
            && cabe(mundo.getBlockAt(x, y + 1, z).getType());
    }

    private boolean sueloFirme(Material tipo) {
        if (!tipo.isSolid()) {
            return false;
        }
        return switch (tipo) {
            case MAGMA_BLOCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE, POINTED_DRIPSTONE -> false;
            default -> true;
        };
    }

    private boolean cabe(Material tipo) {
        if (tipo.isSolid()) {
            return false;
        }
        return switch (tipo) {
            case LAVA, FIRE, SOUL_FIRE, CACTUS, SWEET_BERRY_BUSH, POWDER_SNOW, COBWEB,
                WITHER_ROSE, NETHER_PORTAL, END_PORTAL, END_GATEWAY -> false;
            default -> true;
        };
    }

    private boolean cobrar(Player jugador, int precio) {
        Economy bolsa = economia();
        if (bolsa == null || precio <= 0 || duros(jugador) < precio) {
            return false;
        }
        double antes = bolsa.getBalance(jugador);
        EconomyResponse respuesta = bolsa.withdrawPlayer(jugador, precio);
        if (!respuesta.transactionSuccess() || bolsa.getBalance(jugador) > antes - precio + 0.5) {
            if (respuesta.transactionSuccess()) {
                bolsa.depositPlayer(jugador, precio);
            }
            return false;
        }
        return true;
    }

    private void devolver(Player jugador, int precio) {
        Economy bolsa = economia();
        if (bolsa == null || precio <= 0) {
            getLogger().warning("No he podido devolver " + precio + " duros a " + jugador.getName());
            jugador.sendMessage("§cNo he podido devolverte los duros.");
            return;
        }
        EconomyResponse respuesta = bolsa.depositPlayer(jugador, precio);
        if (!respuesta.transactionSuccess()) {
            getLogger().warning("No he podido devolver " + precio + " duros a " + jugador.getName());
            jugador.sendMessage("§cNo he podido devolverte los duros.");
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

    void irCama(Player jugador) {
        Location cama = camaDe(jugador);
        if (cama == null) {
            jugador.sendMessage("§eDuerme en el survival para guardar la cama.");
            return;
        }
        if (!camaSigue(cama)) {
            jugador.sendMessage("§eEsa cama ya no está. Duerme otra vez.");
            return;
        }
        empezarCama(jugador, cama);
    }

    private Location camaDe(Player jugador) {
        Location cama = jugador.getRespawnLocation();
        if (cama == null || cama.getWorld() == null) {
            return null;
        }
        String nombre = cama.getWorld().getName();
        if (!nombre.equals("survival") && !nombre.equals("survival_nether") && !nombre.equals("fin")) {
            return null;
        }
        return cama.clone();
    }

    private boolean camaSigue(Location sitio) {
        World mundo = sitio.getWorld();
        if (mundo == null) {
            return false;
        }
        int x = sitio.getBlockX();
        int y = sitio.getBlockY();
        int z = sitio.getBlockZ();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int yy = y + dy;
                    if (yy < mundo.getMinHeight() || yy >= mundo.getMaxHeight()) {
                        continue;
                    }
                    Block bloque = mundo.getBlockAt(x + dx, yy, z + dz);
                    if (bloque.getBlockData() instanceof Bed || bloque.getBlockData() instanceof RespawnAnchor) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void empezarCama(Player jugador, Location cama) {
        if (!jugador.hasPermission("viaje.usar") && !jugador.hasPermission("viaje.colocar")) {
            jugador.sendMessage("§cNo puedes volver a la cama así.");
            return;
        }
        if (!jugador.isOnline() || jugador.isDead()) {
            return;
        }
        Vuelta ya = vueltas.get(jugador.getUniqueId());
        if (ya != null) {
            int quedan = Reloj.quedan(ya.totalMedios, ya.medios);
            jugador.sendMessage("§7Ya estás volviendo. Quedan §f" + Math.max(quedan, 1) + " §7segundos.");
            return;
        }
        Peligro.Motivo motivo = motivoDe(jugador);
        if (motivo != null) {
            jugador.sendMessage("§c" + textoAhora(motivo));
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int espera = Math.max(1, getConfig().getInt("viaje.espera-segundos", 15));
        BossBar barra = Bukkit.createBossBar("Yendo a tu cama · " + espera + " s", BarColor.GREEN, BarStyle.SOLID);
        barra.addPlayer(jugador);
        barra.setProgress(1.0);
        vueltas.put(jugador.getUniqueId(), new Vuelta(barra, espera * 2, null, cama.clone(), null));
        jugador.sendActionBar("§bYendo a tu cama · " + espera + " s");
        jugador.sendMessage("§aLlegas en " + espera + " segundos. §7Si entras en lava, te ahogas o te ataca un mob, se cancela.");
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    void irXo(Player jugador) {
        Plugin xo = getServer().getPluginManager().getPlugin("Xo");
        if (xo == null || !xo.isEnabled()) {
            jugador.sendMessage("§eLa XO no está lista.");
            return;
        }
        try {
            boolean abierta = (boolean) xo.getClass().getMethod("estaAbierta").invoke(null);
            if (!abierta) {
                jugador.sendMessage("§eLa XO no está abierta. Se abre sola el sábado a las 21:30.");
                return;
            }
            xo.getClass().getMethod("llevar", Player.class).invoke(null, jugador);
        } catch (ReflectiveOperationException error) {
            getLogger().warning("No pude abrir la XO: " + error.getMessage());
            jugador.sendMessage("§cLa XO no está lista.");
        }
    }

    private void aplicarBorde() {
        for (World mundo : getServer().getWorlds()) {
            String nombre = mundo.getName();
            if (nombre.equals("world")) {
                continue;
            }
            int diametro = switch (nombre) {
                case "survival" -> getConfig().getInt("borde.overworld", 100000);
                case "survival_nether" -> getConfig().getInt("borde.nether", 12500);
                case "fin" -> getConfig().getInt("borde.end", 30000);
                default -> 0;
            };
            if (diametro <= 0) {
                continue;
            }
            WorldBorder borde = mundo.getWorldBorder();
            borde.setCenter(0, 0);
            if (Math.abs(borde.getSize() - diametro) > 1) {
                borde.setSize(diametro);
                getLogger().info("Borde de " + mundo.getName() + ": " + diametro + ".");
            }
        }
    }

    public void recogerMundos(List<String> mundos) {
        if (banderas != null) {
            banderas.recogerMundos(mundos);
        }
    }

    public void recogerZona(String mundo, int minX, int minZ, int maxX, int maxZ) {
        if (banderas != null) {
            banderas.recogerZona(mundo, minX, minZ, maxX, maxZ);
        }
    }

    private World overworld() {
        World porNombre = getServer().getWorld("world");
        if (porNombre != null && porNombre.getEnvironment() == World.Environment.NORMAL) {
            return porNombre;
        }
        for (World mundo : getServer().getWorlds()) {
            if (mundo.getEnvironment() == World.Environment.NORMAL) {
                return mundo;
            }
        }
        return null;
    }

    private static final class Vuelta {
        private final BossBar barra;
        private final int totalMedios;
        private final UUID hacia;
        private final Location cama;
        private final String rotulo;
        private final Huella huella;
        private final UUID bandera;
        private int medios;

        private Vuelta(BossBar barra, int totalMedios, UUID hacia, Location cama, String rotulo) {
            this(barra, totalMedios, hacia, cama, rotulo, null, null);
        }

        private Vuelta(BossBar barra, int totalMedios, UUID hacia, Location cama, String rotulo, Huella huella) {
            this(barra, totalMedios, hacia, cama, rotulo, huella, null);
        }

        private Vuelta(BossBar barra, int totalMedios, UUID hacia, Location cama, String rotulo, Huella huella, UUID bandera) {
            this.barra = barra;
            this.totalMedios = totalMedios;
            this.hacia = hacia;
            this.cama = cama;
            this.rotulo = rotulo;
            this.huella = huella;
            this.bandera = bandera;
        }
    }
}
