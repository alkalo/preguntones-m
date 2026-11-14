package com.preguntones.mundos;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.advancement.Advancement;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class MundosPlugin extends JavaPlugin implements Listener {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern(
        "EEEE d 'de' MMMM 'a las' HH:mm", Locale.forLanguageTag("es-ES"));
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern(
        "EEE d HH:mm", Locale.forLanguageTag("es-ES"));
    private static MundosPlugin instancia;

    private final Map<String, Long> semillas = new HashMap<>();
    private final Random azar = new Random();
    private File estadoArchivo;
    private long ultimoReset;
    private long pregenHasta;
    private long avisoHoras;
    private long diasDragon;
    private int metaBlaze;
    private int ciclo = 1;
    private long renuevaEn;
    private long semillaFin = 240165L;
    private boolean dragonAvisado;
    private Pregen pregen;
    private int minutosAmigos;
    private UUID cazador;
    private String cazadorNombre = "";
    private String ultimoDragon = "";
    private String ultimoMundo = "";
    private int ultimoX;
    private int ultimoZ;
    private long ultimoViaje;
    private Economy economia;
    private final Map<UUID, int[]> bases = new HashMap<>();
    private final Map<String, Map<UUID, Integer>> hechos = new HashMap<>();
    private final Map<UUID, Set<String>> progresosPagados = new HashMap<>();
    private Grupo grupo;
    private final Map<UUID, Integer> baseBlaze = new HashMap<>();
    private final Map<UUID, Long> avisoPortal = new HashMap<>();
    private final Set<UUID> blazesHechos = new HashSet<>();
    private final Set<UUID> bastionHecho = new HashSet<>();
    private final Set<UUID> ciudadHecha = new HashSet<>();
    private final Set<UUID> rangoPendiente = new HashSet<>();
    private UUID ultimoGolpeador;
    private long ultimoGolpeEn;

    @Override
    public void onEnable() {
        instancia = this;
        saveDefaultConfig();
        avisoHoras = Math.max(1, getConfig().getLong("aviso-horas", 48));
        diasDragon = Math.max(0, getConfig().getLong("dias-reset", 14));
        metaBlaze = Math.max(0, getConfig().getInt("dragon-blazes", 12));
        minutosAmigos = getConfig().getInt("minutos-amigos", 3);
        if (getConfig().isConfigurationSection("mundos")) {
            for (String nombre : getConfig().getConfigurationSection("mundos").getKeys(false)) {
                semillas.put(nombre, getConfig().getLong("mundos." + nombre));
            }
        }
        estadoArchivo = new File(getDataFolder(), "estado.yml");
        grupo = new Grupo(this);
        cargarEstado();
        Combate combate = new Combate(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(combate, this);
        getServer().getScheduler().runTaskTimer(this, combate::pulso, 20L, 20L);
        asegurarMundos();
        aplicarTodos();
        getServer().getScheduler().runTask(this, this::aplicarTodos);
        prepararEconomia();
        getServer().getScheduler().runTaskTimer(this, this::revisarMisiones, 100L, 100L);
        if (getCommand("ciclo") != null) {
            getCommand("ciclo").setExecutor((sender, command, label, args) -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("mundos.admin")) {
                    sender.sendMessage("§eReinicio el ciclo de los mundos de juego.");
                    reiniciarCiclo();
                    return true;
                }
                sender.sendMessage("§6Ciclo " + ciclo + ". §fSe borra " + avisoHoras + " horas después de que un grupo mate al dragón. Las parcelas no.");
                sender.sendMessage("§f" + textoPuerta());
                sender.sendMessage("§fEn la pelea no valen las explosiones ni los élitros. Tiene más vida, y con cristales en las torres casi no entra el golpe.");
                sender.sendMessage("§fLos duros salen de jugar allí: madera, piedra, caza, explorar, hierro, diamante, Nether y el dragón.");
                sender.sendMessage("§fLa ficha del aldeano está en la tienda, el botón del centro. 40 duros. Hasta 8.");
                if (renuevaEn > System.currentTimeMillis()) {
                    sender.sendMessage("§d" + cazadorNombre + " §fya cazó al dragón. Se borra el " + fechaLarga(renuevaEn) + ".");
                } else if (cazador != null) {
                    sender.sendMessage("§d" + cazadorNombre + " §fya cazó al dragón. El mundo vuelve a empezar.");
                } else if (Ciclo.pideDias(ciclo) && System.currentTimeMillis() < aperturaDragon()) {
                    sender.sendMessage("§fEl dragón se abre el " + fechaLarga(aperturaDragon()) + ".");
                } else if (sender instanceof Player jugador) {
                    anclarCaza(jugador);
                    anotarBlazes(jugador);
                    String falta = faltaDe(jugador);
                    sender.sendMessage(falta.isEmpty() ? "§aYa puedes entrar al End." : falta);
                } else {
                    sender.sendMessage("§fEl dragón ya se puede cazar.");
                }
                return true;
            });
        }
        if (getCommand("grupo") != null) {
            getCommand("grupo").setExecutor((sender, command, label, args) -> grupo.orden(sender, args));
            getCommand("grupo").setTabCompleter((sender, command, label, args) -> grupo.completar(args));
        }
        getServer().getScheduler().runTask(this, this::engancharChunky);
        getServer().getScheduler().runTask(this, this::revisarRenovacion);
        getServer().getScheduler().runTaskTimer(this, this::revisarRenovacion, 1200L, 1200L);
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            anclarCaza(jugador);
            anotarBlazes(jugador);
            concederSiPendiente(jugador);
        }
        grupo.arrancar();
        getLogger().info("Survival listo. Ciclo " + ciclo + ". El dragón del primero abre a los " + diasDragon
            + " días y el mundo se renueva " + avisoHoras + " horas después.");
    }

    @Override
    public void onDisable() {
        if (grupo != null) {
            grupo.cerrar();
        }
        guardarEstado();
        instancia = null;
    }

    public static String textoCiclo() {
        if (instancia == null) {
            return "";
        }
        if (instancia.renuevaEn > System.currentTimeMillis()) {
            return fechaCorta(instancia.renuevaEn);
        }
        if (instancia.cazador == null && System.currentTimeMillis() < instancia.aperturaDragon()) {
            return "abre " + fechaCorta(instancia.aperturaDragon());
        }
        return "dragón vivo";
    }

    private void engancharChunky() {
        Plugin chunky = getServer().getPluginManager().getPlugin("Chunky");
        if (chunky == null || !chunky.isEnabled()) {
            getLogger().info("Chunky no está. El círculo de aterrizaje no se pregenera.");
            return;
        }
        pregen = Pregen.enganchar(this);
    }

    List<String> ordenPregen() {
        List<String> bases = new ArrayList<>(semillas.keySet());
        bases.sort(String::compareTo);
        List<String> orden = new ArrayList<>(bases);
        for (String base : bases) {
            orden.add(base + "_nether");
        }
        orden.add("fin");
        return orden;
    }

    double radioPregen(String nombre) {
        if (nombre.endsWith("_nether")) {
            return getConfig().getDouble("pregen.nether", 400);
        }
        if (nombre.equals("fin")) {
            return getConfig().getDouble("pregen.fin", 256);
        }
        return getConfig().getDouble("pregen.overworld", 1664);
    }

    boolean pregenAlDia() {
        return ultimoReset != 0 && pregenHasta == ultimoReset;
    }

    void marcarPregen() {
        pregenHasta = ultimoReset;
        guardarEstado();
    }

    private void asegurarMundos() {
        for (Map.Entry<String, Long> entrada : semillas.entrySet()) {
            crear(entrada.getKey(), entrada.getValue(), Environment.NORMAL);
            crear(entrada.getKey() + "_nether", entrada.getValue(), Environment.NETHER);
        }
        crear("fin", semillaFin, Environment.THE_END);
    }

    private void crear(String nombre, long semilla, Environment entorno) {
        if (Bukkit.getWorld(nombre) == null) {
            WorldCreator creador = new WorldCreator(nombre);
            creador.seed(semilla);
            creador.environment(entorno);
            creador.generateStructures(true);
            Bukkit.createWorld(creador);
            getLogger().info("Mundo " + nombre + " con semilla " + semilla);
        }
        World mundo = Bukkit.getWorld(nombre);
        if (mundo != null) {
            aplicarReglas(mundo);
        }
    }

    @EventHandler
    public void alCargarMundo(WorldLoadEvent event) {
        aplicarReglas(event.getWorld());
    }

    private void aplicarTodos() {
        for (World mundo : Bukkit.getWorlds()) {
            aplicarReglas(mundo);
        }
    }

    private boolean esSalvaje(String nombre) {
        if (nombre.equals("fin") || semillas.containsKey(nombre)) {
            return true;
        }
        if (nombre.endsWith("_nether")) {
            return semillas.containsKey(nombre.substring(0, nombre.length() - 7));
        }
        return false;
    }

    private void aplicarReglas(World mundo) {
        boolean salvaje = esSalvaje(mundo.getName());
        mundo.setDifficulty(salvaje ? Difficulty.HARD : Difficulty.PEACEFUL);
        regla(mundo, "doMobSpawning", true);
        regla(mundo, "spawn_mobs", true);
        regla(mundo, "pvp", !mundo.getName().equals("xo"));
        regla(mundo, "command_blocks_work", false);
        regla(mundo, "commandBlocksWork", false);
        regla(mundo, "doInsomnia", salvaje);
        regla(mundo, "spawn_phantoms", salvaje);
        if (salvaje) {
            regla(mundo, "mobGriefing", true);
        }
        mundo.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, Fuego.radio(salvaje));
        getLogger().info(mundo.getName() + (salvaje
                ? ": salvaje, dificultad difícil, con mobs y fuego."
                : ": sin mobs hostiles ni fuego."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alEncender(BlockIgniteEvent event) {
        if (event.getBlock().getWorld() != null && !Fuego.puedeEncender(esSalvaje(event.getBlock().getWorld().getName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alQuemar(BlockBurnEvent event) {
        if (event.getBlock().getWorld() != null && !Fuego.puedeEncender(esSalvaje(event.getBlock().getWorld().getName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alExtender(BlockSpreadEvent event) {
        Material tipo = event.getNewState().getType();
        if ((tipo == Material.FIRE || tipo == Material.SOUL_FIRE)
                && event.getBlock().getWorld() != null
                && !Fuego.puedeEncender(esSalvaje(event.getBlock().getWorld().getName()))) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void regla(World mundo, String nombre, boolean valor) {
        GameRule<?> encontrada = GameRule.getByName(nombre);
        if (encontrada == null) {
            return;
        }
        mundo.setGameRule((GameRule<Boolean>) encontrada, valor);
    }

    private void reiniciarCiclo() {
        if (pregen != null) {
            pregen.prepararReset();
        }
        World barrio = Bukkit.getWorld("world");
        Location spawn = barrio == null ? null : new Location(barrio, 0.5, 65, 12.5, 180, 0);
        List<String> nombres = new ArrayList<>();
        for (String nombre : semillas.keySet()) {
            nombres.add(nombre);
            nombres.add(nombre + "_nether");
        }
        nombres.add("fin");
        recogerBanderas(nombres);
        for (String nombre : nombres) {
            World mundo = Bukkit.getWorld(nombre);
            if (mundo == null) {
                continue;
            }
            if (spawn != null) {
                for (Player jugador : new ArrayList<>(mundo.getPlayers())) {
                    jugador.teleport(spawn);
                    jugador.sendMessage("§eEste mundo se ha renovado. Tus parcelas siguen en el barrio.");
                }
            }
            File carpeta = mundo.getWorldFolder();
            Bukkit.unloadWorld(mundo, false);
            borrar(carpeta);
        }
        cazador = null;
        cazadorNombre = "";
        renuevaEn = 0;
        bases.clear();
        hechos.clear();
        baseBlaze.clear();
        avisoPortal.clear();
        dragonAvisado = false;
        blazesHechos.clear();
        bastionHecho.clear();
        ciudadHecha.clear();
        ciclo = Ciclo.normalizar(ciclo + 1);
        dragonAvisado = !Ciclo.pideDias(ciclo);
        nuevasSemillas();
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            baseBlaze.put(jugador.getUniqueId(), muertes(jugador, EntityType.BLAZE));
        }
        ultimoReset = System.currentTimeMillis();
        guardarEstado();
        asegurarMundos();
        if (pregen != null) {
            getServer().getScheduler().runTaskLater(this, pregen::iniciarSiFalta, 40L);
        }
        Bukkit.broadcastMessage("§eCiclo " + ciclo + ". §f" + textoPuerta());
    }

    private void recogerBanderas(List<String> mundos) {
        Plugin viajes = Bukkit.getPluginManager().getPlugin("Viajes");
        if (viajes == null || !viajes.isEnabled()) {
            getLogger().warning("Viajes no está. Las banderas de ese mundo siguen apuntadas hasta que alguien abra Spawn.");
            return;
        }
        try {
            viajes.getClass().getMethod("recogerMundos", List.class).invoke(viajes, mundos);
        } catch (ReflectiveOperationException e) {
            getLogger().warning("No pude devolver las banderas: " + e.getMessage());
        }
    }

    private void borrar(File carpeta) {
        File[] hijos = carpeta.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                if (hijo.isDirectory()) {
                    borrar(hijo);
                } else {
                    hijo.delete();
                }
            }
        }
        carpeta.delete();
    }

    private Location sitioDeViaje() {
        long ahora = System.currentTimeMillis();
        if (!ultimoMundo.isEmpty() && ahora - ultimoViaje < minutosAmigos * 60_000L) {
            World mundo = Bukkit.getWorld(ultimoMundo);
            if (mundo != null) {
                int x = ultimoX + azar.nextInt(41) - 20;
                int z = ultimoZ + azar.nextInt(41) - 20;
                return suelo(mundo, x, z);
            }
        }
        if (semillas.isEmpty()) {
            return null;
        }
        List<String> nombres = new ArrayList<>(semillas.keySet());
        String nombre = nombres.get(azar.nextInt(nombres.size()));
        World mundo = Bukkit.getWorld(nombre);
        if (mundo == null) {
            return null;
        }
        int x = 0;
        int z = 0;
        for (int intento = 0; intento < 12; intento++) {
            x = azar.nextInt(3001) - 1500;
            z = azar.nextInt(3001) - 1500;
            Biome bioma = mundo.getBiome(x, z);
            if (bioma != Biome.OCEAN && bioma != Biome.DEEP_OCEAN && bioma != Biome.COLD_OCEAN && bioma != Biome.FROZEN_OCEAN) {
                break;
            }
        }
        ultimoMundo = nombre;
        ultimoX = x;
        ultimoZ = z;
        ultimoViaje = ahora;
        return suelo(mundo, x, z);
    }

    private Location suelo(World mundo, int x, int z) {
        int y = mundo.getHighestBlockYAt(x, z) + 1;
        return new Location(mundo, x + 0.5, y, z + 0.5);
    }

    @EventHandler
    public void alPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }
        World origen = event.getFrom().getWorld();
        if (origen == null) {
            return;
        }
        String nombre = origen.getName();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (semillas.containsKey(nombre)) {
                String freno = frenoDragon(event.getPlayer());
                if (!freno.isEmpty()) {
                    event.setCancelled(true);
                    avisarPortal(event.getPlayer(), freno);
                    return;
                }
                World fin = Bukkit.getWorld("fin");
                if (fin != null) {
                    event.setTo(new Location(fin, 0.5, 65, 0.5));
                }
            } else if (nombre.equals("fin")) {
                World barrio = Bukkit.getWorld("world");
                if (barrio != null) {
                    event.setTo(new Location(barrio, 0.5, 65, 12.5, 180, 0));
                }
            }
            return;
        }
        String base = nombre.endsWith("_nether") ? nombre.substring(0, nombre.length() - 7) : nombre;
        if (!semillas.containsKey(base)) {
            return;
        }
        if (nombre.endsWith("_nether")) {
            World overworld = Bukkit.getWorld(base);
            if (overworld == null) {
                return;
            }
            int x = event.getFrom().getBlockX() * 8;
            int z = event.getFrom().getBlockZ() * 8;
            event.setTo(suelo(overworld, x, z));
        } else {
            World nether = Bukkit.getWorld(base + "_nether");
            if (nether == null) {
                return;
            }
            int x = event.getFrom().getBlockX() / 8;
            int z = event.getFrom().getBlockZ() / 8;
            event.setTo(suelo(nether, x, z));
        }
    }

    @EventHandler
    public void alMorirDragon(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) {
            return;
        }
        if (cazador != null) {
            return;
        }
        EnderDragon dragon = (EnderDragon) event.getEntity();
        Player asesino = dragon.getKiller();
        if (asesino == null || !puertaLista(asesino)) {
            asesino = acreditable();
        }
        if (asesino == null || !puertaLista(asesino)) {
            event.setCancelled(true);
            Combate.llenar(dragon);
            return;
        }
        Set<UUID> equipo = miembros(asesino.getUniqueId());
        StringBuilder nombres = new StringBuilder();
        for (UUID id : equipo) {
            Player persona = Bukkit.getPlayer(id);
            if (persona != null) {
                if (!puertaLista(persona)) {
                    continue;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + persona.getName() + " parent add cazador");
                if (economia != null && economia.depositPlayer(persona, 20).transactionSuccess()) {
                    persona.sendMessage("§aHabéis cazado al dragón. +20 duros.");
                }
                if (nombres.length() > 0) {
                    nombres.append(", ");
                }
                nombres.append(persona.getName());
            } else if (puertaLista(id)) {
                rangoPendiente.add(id);
            }
        }
        cazador = asesino.getUniqueId();
        cazadorNombre = nombres.toString();
        ultimoDragon = cazadorNombre;
        renuevaEn = System.currentTimeMillis() + avisoHoras * 3_600_000L;
        guardarEstado();
        Bukkit.broadcastMessage("§d" + cazadorNombre + " §fhan cazado al dragón. El survival se borra el "
            + fechaLarga(renuevaEn) + ". El rango Cazador se queda.");
    }

    private void revisarRenovacion() {
        if (renuevaEn > 0 && System.currentTimeMillis() >= renuevaEn) {
            getLogger().info("Han pasado " + avisoHoras + " horas desde el dragón. Renuevo el survival.");
            reiniciarCiclo();
            return;
        }
        if (dragonAvisado || cazador != null) {
            return;
        }
        if (Ciclo.pideDias(ciclo) && System.currentTimeMillis() < aperturaDragon()) {
            return;
        }
        dragonAvisado = true;
        guardarEstado();
        Bukkit.broadcastMessage("§dEl dragón ya se puede cazar. §f" + textoPuerta());
    }

    private long aperturaDragon() {
        return ultimoReset + diasDragon * 86_400_000L;
    }

    private void nuevasSemillas() {
        for (String nombre : new ArrayList<>(semillas.keySet())) {
            semillas.put(nombre, azar.nextLong());
        }
        semillaFin = azar.nextLong();
        Long survival = semillas.get("survival");
        if (survival != null) {
            getLogger().info("Nuevo ciclo de survival, semilla " + survival + ".");
        }
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        anclarCaza(event.getPlayer());
        anotarBlazes(event.getPlayer());
        concederSiPendiente(event.getPlayer());
        Player jugador = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> liquidarProgresos(jugador), 40L);
    }

    @EventHandler
    public void alProgreso(PlayerAdvancementDoneEvent event) {
        int duros = cobrarProgreso(event.getPlayer(), event.getAdvancement());
        if (duros <= 0) {
            return;
        }
        guardarEstado();
        event.getPlayer().sendMessage(lineaProgreso(event.getAdvancement(), duros));
    }

    private void liquidarProgresos(Player jugador) {
        if (!jugador.isOnline()) {
            return;
        }
        int suma = 0;
        Iterator<Advancement> avances = Bukkit.advancementIterator();
        while (avances.hasNext()) {
            Advancement avance = avances.next();
            if (jugador.getAdvancementProgress(avance).isDone()) {
                suma += cobrarProgreso(jugador, avance);
            }
        }
        if (suma <= 0) {
            return;
        }
        guardarEstado();
        jugador.sendMessage("§aProgresos que ya tenías. §f+" + suma + " duros.");
    }

    private int cobrarProgreso(Player jugador, Advancement avance) {
        if (avance == null || avance.getKey() == null || economia == null) {
            return 0;
        }
        if (!"minecraft".equals(avance.getKey().getNamespace()) || !Progresos.cuenta(avance.getKey().getKey())) {
            return 0;
        }
        if (avance.getDisplay() == null) {
            return 0;
        }
        String clave = avance.getKey().getKey();
        Set<String> ya = progresosPagados.computeIfAbsent(jugador.getUniqueId(), id -> new HashSet<>());
        if (ya.contains(clave)) {
            return 0;
        }
        int duros = Progresos.duros(avance.getDisplay().frame().name());
        if (!economia.depositPlayer(jugador, duros).transactionSuccess()) {
            return 0;
        }
        ya.add(clave);
        return duros;
    }

    private Component lineaProgreso(Advancement avance, int duros) {
        String unidad = duros == 1 ? " duro. " : " duros. ";
        return Component.text("+" + duros + unidad, NamedTextColor.GOLD).append(avance.displayName());
    }

    private void anclarCaza(Player jugador) {
        if (baseBlaze.putIfAbsent(jugador.getUniqueId(), muertes(jugador, EntityType.BLAZE)) == null) {
            guardarEstado();
        }
    }

    private int blazesDe(Player jugador) {
        Integer base = baseBlaze.get(jugador.getUniqueId());
        if (base == null) {
            return 0;
        }
        return Math.max(0, muertes(jugador, EntityType.BLAZE) - base);
    }

    private String frenoDragon(Player jugador) {
        if (cazador != null) {
            return "";
        }
        if (Ciclo.pideDias(ciclo) && System.currentTimeMillis() < aperturaDragon()) {
            return "§eEl dragón se abre el " + fechaLarga(aperturaDragon()) + ". " + textoPuerta();
        }
        anclarCaza(jugador);
        anotarBlazes(jugador);
        return faltaDe(jugador);
    }

    private void avisarPortal(Player jugador, String texto) {
        long ahora = System.currentTimeMillis();
        Long antes = avisoPortal.get(jugador.getUniqueId());
        if (antes != null && ahora - antes < 4000L) {
            return;
        }
        avisoPortal.put(jugador.getUniqueId(), ahora);
        jugador.sendMessage(texto);
    }

    private static String fechaLarga(long instante) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(instante), MADRID).format(FECHA_LARGA);
    }

    private static String fechaCorta(long instante) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(instante), MADRID).format(FECHA_CORTA);
    }

    private void cargarEstado() {
        if (!estadoArchivo.exists()) {
            ultimoReset = System.currentTimeMillis();
            guardarEstado();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(estadoArchivo);
        ultimoReset = yaml.getLong("ultimo-reset", System.currentTimeMillis());
        pregenHasta = yaml.getLong("pregen", 0);
        String id = yaml.getString("cazador", "");
        if (!id.isEmpty()) {
            cazador = UUID.fromString(id);
            cazadorNombre = yaml.getString("cazador-nombre", "");
        }
        ultimoDragon = yaml.getString("ultimo-dragon", cazadorNombre);
        renuevaEn = yaml.getLong("renueva-en", 0);
        dragonAvisado = yaml.getBoolean("dragon-avisado", false);
        ciclo = Ciclo.normalizar(yaml.getInt("ciclo", 1));
        blazesHechos.addAll(leerIds(yaml, "blazes-hechos"));
        bastionHecho.addAll(leerIds(yaml, "bastion"));
        ciudadHecha.addAll(leerIds(yaml, "ciudad"));
        rangoPendiente.addAll(leerIds(yaml, "rango-pendiente"));
        if (grupo != null) {
            grupo.cargar(yaml);
        }
        if (yaml.contains("semilla-fin")) {
            semillaFin = yaml.getLong("semilla-fin");
        }
        ConfigurationSection semillasGuardadas = yaml.getConfigurationSection("semillas");
        if (semillasGuardadas != null) {
            for (String nombre : new ArrayList<>(semillas.keySet())) {
                if (semillasGuardadas.contains(nombre)) {
                    semillas.put(nombre, semillasGuardadas.getLong(nombre));
                }
            }
        }
        ConfigurationSection caza = yaml.getConfigurationSection("caza-blaze");
        if (caza != null) {
            for (String clave : caza.getKeys(false)) {
                try {
                    baseBlaze.put(UUID.fromString(clave), caza.getInt(clave));
                } catch (IllegalArgumentException ignorado) {
                    getLogger().warning("Ignoro una caza con id " + clave);
                }
            }
        }
        cargarHechos(yaml);
        cargarProgresos(yaml);
        if (cazador != null && renuevaEn <= 0) {
            renuevaEn = System.currentTimeMillis() + avisoHoras * 3_600_000L;
            guardarEstado();
        }
    }

    void guardarEstado() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ultimo-reset", ultimoReset);
        yaml.set("pregen", pregenHasta);
        yaml.set("cazador", cazador == null ? "" : cazador.toString());
        yaml.set("cazador-nombre", cazadorNombre);
        yaml.set("ultimo-dragon", ultimoDragon);
        yaml.set("renueva-en", renuevaEn);
        yaml.set("dragon-avisado", dragonAvisado);
        yaml.set("ciclo", ciclo);
        yaml.set("blazes-hechos", escribirIds(blazesHechos));
        yaml.set("bastion", escribirIds(bastionHecho));
        yaml.set("ciudad", escribirIds(ciudadHecha));
        yaml.set("rango-pendiente", escribirIds(rangoPendiente));
        if (grupo != null) {
            grupo.escribir(yaml);
        }
        yaml.set("semilla-fin", semillaFin);
        for (Map.Entry<String, Long> entrada : semillas.entrySet()) {
            yaml.set("semillas." + entrada.getKey(), entrada.getValue());
        }
        for (Map.Entry<UUID, Integer> entrada : baseBlaze.entrySet()) {
            yaml.set("caza-blaze." + entrada.getKey(), entrada.getValue());
        }
        escribirHechos(yaml);
        escribirProgresos(yaml);
        try {
            if (!estadoArchivo.getParentFile().exists()) {
                estadoArchivo.getParentFile().mkdirs();
            }
            yaml.save(estadoArchivo);
        } catch (IOException e) {
            getLogger().warning("No pude guardar el ciclo: " + e.getMessage());
        }
    }

    private void cargarHechos(YamlConfiguration yaml) {
        hechos.clear();
        ConfigurationSection raiz = yaml.getConfigurationSection("hechos");
        if (raiz == null) {
            return;
        }
        for (String clave : raiz.getKeys(false)) {
            ConfigurationSection gente = raiz.getConfigurationSection(clave);
            if (gente == null) {
                continue;
            }
            Map<UUID, Integer> mapa = new HashMap<>();
            for (String id : gente.getKeys(false)) {
                try {
                    mapa.put(UUID.fromString(id), gente.getInt(id));
                } catch (IllegalArgumentException ignorado) {
                    getLogger().warning("Ignoro un hecho con id " + id);
                }
            }
            if (!mapa.isEmpty()) {
                hechos.put(clave, mapa);
            }
        }
    }

    private void cargarProgresos(YamlConfiguration yaml) {
        progresosPagados.clear();
        ConfigurationSection raiz = yaml.getConfigurationSection("progresos");
        if (raiz == null) {
            return;
        }
        for (String id : raiz.getKeys(false)) {
            try {
                Set<String> claves = new HashSet<>(raiz.getStringList(id));
                claves.remove("");
                if (!claves.isEmpty()) {
                    progresosPagados.put(UUID.fromString(id), claves);
                }
            } catch (IllegalArgumentException ignorado) {
                getLogger().warning("Ignoro progresos de " + id);
            }
        }
    }

    private void escribirProgresos(YamlConfiguration yaml) {
        for (Map.Entry<UUID, Set<String>> entrada : progresosPagados.entrySet()) {
            yaml.set("progresos." + entrada.getKey(), new ArrayList<>(entrada.getValue()));
        }
    }

    private void escribirHechos(YamlConfiguration yaml) {
        for (Map.Entry<String, Map<UUID, Integer>> entrada : hechos.entrySet()) {
            for (Map.Entry<UUID, Integer> persona : entrada.getValue().entrySet()) {
                yaml.set("hechos." + entrada.getKey() + "." + persona.getKey(), persona.getValue());
            }
        }
    }

    private void prepararEconomia() {
        RegisteredServiceProvider<Economy> registro = getServer().getServicesManager().getRegistration(Economy.class);
        if (registro != null) {
            economia = registro.getProvider();
        }
    }

    private void revisarMisiones() {
        if (economia == null) {
            return;
        }
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            anotarBlazes(jugador);
            String mundo = jugador.getWorld() == null ? "" : jugador.getWorld().getName();
            if (!mundo.equals("survival") && !mundo.equals("survival_nether") && !mundo.equals("fin")) {
                continue;
            }
            int[] base = bases.computeIfAbsent(jugador.getUniqueId(), id -> foto(jugador));
            int[] ahora = foto(jugador);
            Mision[] misiones = misiones();
            for (int i = 0; i < misiones.length; i++) {
                pagar(jugador, misiones[i], ahora[i] - base[i]);
            }
            if (mundo.equals("survival_nether")) {
                pagarUnaVez(jugador, "nether", 16, "Has entrado al Nether.");
            }
            if (mundo.equals("fin")) {
                pagarUnaVez(jugador, "end", 18, "Has entrado al End.");
            }
        }
    }

    private Mision[] misiones() {
        return new Mision[] {
            new Mision("madera", 16, 3, 6, "Has talado madera."),
            new Mision("piedra", 32, 4, 8, "Has picado piedra."),
            new Mision("caza", 4, 3, 10, "Has cazado monstruos."),
            new Mision("comida", 3, 2, 6, "Has cazado para comer."),
            new Mision("pesca", 6, 2, 8, "Has pescado."),
            new Mision("camino", 40000, 3, 8, "Has explorado."),
            new Mision("cama", 1, 1, 8, "Has dormido en una cama."),
            new Mision("aldeano", 1, 1, 10, "Has comerciado con un aldeano."),
            new Mision("hierro", 8, 2, 12, "Has picado hierro."),
            new Mision("diamante", 2, 2, 16, "Has picado diamante."),
            new Mision("encantar", 1, 1, 12, "Has encantado."),
            new Mision("blaze", 2, 2, 14, "Has cazado blazes."),
            new Mision("escombro", 1, 1, 20, "Has picado escombros del Nether."),
            new Mision("enderman", 3, 2, 10, "Has cazado endermen.")
        };
    }

    private int[] foto(Player jugador) {
        Mision[] misiones = misiones();
        int[] valores = new int[misiones.length];
        int[] actual = new int[] {
            troncos(jugador),
            piedra(jugador),
            monstruos(jugador),
            comida(jugador),
            jugador.getStatistic(Statistic.FISH_CAUGHT),
            jugador.getStatistic(Statistic.WALK_ONE_CM) + jugador.getStatistic(Statistic.SPRINT_ONE_CM),
            jugador.getStatistic(Statistic.SLEEP_IN_BED),
            jugador.getStatistic(Statistic.TRADED_WITH_VILLAGER),
            mena(jugador, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
            mena(jugador, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
            jugador.getStatistic(Statistic.ITEM_ENCHANTED),
            muertes(jugador, EntityType.BLAZE),
            mena(jugador, Material.ANCIENT_DEBRIS),
            muertes(jugador, EntityType.ENDERMAN)
        };
        System.arraycopy(actual, 0, valores, 0, Math.min(valores.length, actual.length));
        return valores;
    }

    private int troncos(Player jugador) {
        return mena(jugador,
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM);
    }

    private int piedra(Player jugador) {
        return mena(jugador, Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE);
    }

    private int monstruos(Player jugador) {
        return muertes(jugador,
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.CREEPER, EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY,
            EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.PHANTOM);
    }

    private int comida(Player jugador) {
        return muertes(jugador, EntityType.COW, EntityType.PIG, EntityType.CHICKEN, EntityType.SHEEP, EntityType.RABBIT);
    }

    private int mena(Player jugador, Material... materiales) {
        int total = 0;
        for (Material material : materiales) {
            total += jugador.getStatistic(Statistic.MINE_BLOCK, material);
        }
        return total;
    }

    private int muertes(Player jugador, EntityType... tipos) {
        int total = 0;
        for (EntityType tipo : tipos) {
            total += jugador.getStatistic(Statistic.KILL_ENTITY, tipo);
        }
        return total;
    }

    private void pagar(Player jugador, Mision mision, int progreso) {
        if (progreso < mision.meta) {
            return;
        }
        Map<UUID, Integer> veces = hechos.computeIfAbsent(mision.clave, clave -> new HashMap<>());
        int hechas = veces.getOrDefault(jugador.getUniqueId(), 0);
        int merecidas = Math.min(mision.veces, progreso / mision.meta);
        if (merecidas <= hechas) {
            return;
        }
        int nuevas = merecidas - hechas;
        EconomyResponse respuesta = economia.depositPlayer(jugador, mision.duros * (double) nuevas);
        if (!respuesta.transactionSuccess()) {
            return;
        }
        veces.put(jugador.getUniqueId(), merecidas);
        guardarEstado();
        jugador.sendMessage("§a" + mision.texto + " +" + (mision.duros * nuevas) + " duros.");
    }

    private void pagarUnaVez(Player jugador, String clave, int duros, String texto) {
        Map<UUID, Integer> veces = hechos.computeIfAbsent(clave, id -> new HashMap<>());
        if (veces.getOrDefault(jugador.getUniqueId(), 0) > 0) {
            return;
        }
        EconomyResponse respuesta = economia.depositPlayer(jugador, duros);
        if (!respuesta.transactionSuccess()) {
            return;
        }
        veces.put(jugador.getUniqueId(), 1);
        guardarEstado();
        jugador.sendMessage("§a" + texto + " +" + duros + " duros.");
    }

    private static final class Mision {
        final String clave;
        final int meta;
        final int veces;
        final int duros;
        final String texto;

        Mision(String clave, int meta, int veces, int duros, String texto) {
            this.clave = clave;
            this.meta = meta;
            this.veces = veces;
            this.duros = duros;
            this.texto = texto;
        }
    }

    public String ultimoDragon() {
        return ultimoDragon == null ? "" : ultimoDragon;
    }

    private Set<UUID> leerIds(YamlConfiguration yaml, String clave) {
        Set<UUID> ids = new HashSet<>();
        for (String texto : yaml.getStringList(clave)) {
            try {
                ids.add(UUID.fromString(texto));
            } catch (IllegalArgumentException ignorado) {
                getLogger().warning("Ignoro un id en " + clave);
            }
        }
        return ids;
    }

    private static List<String> escribirIds(Set<UUID> ids) {
        List<String> lista = new ArrayList<>();
        for (UUID id : ids) {
            lista.add(id.toString());
        }
        return lista;
    }

    private String textoPuerta() {
        if (Ciclo.pideBastion(ciclo) && Ciclo.pideCiudad(ciclo)) {
            return "Hacen falta " + metaBlaze + " blazes, un bastión y una ciudad ancestral.";
        }
        if (Ciclo.pideCiudad(ciclo)) {
            return "Hacen falta " + metaBlaze + " blazes y una ciudad ancestral.";
        }
        if (Ciclo.pideBastion(ciclo)) {
            return "Hacen falta " + metaBlaze + " blazes y un bastión.";
        }
        return "Hacen falta " + metaBlaze + " blazes de este ciclo. Los de antes no cuentan.";
    }

    private String faltaDe(Player jugador) {
        List<String> falta = new ArrayList<>();
        int blazes = blazesDe(jugador);
        if (blazes < metaBlaze) {
            falta.add(blazes + "/" + metaBlaze + " blazes");
        }
        if (Ciclo.pideBastion(ciclo) && !bastionHecho.contains(jugador.getUniqueId())) {
            falta.add("un bastión");
        }
        if (Ciclo.pideCiudad(ciclo) && !ciudadHecha.contains(jugador.getUniqueId())) {
            falta.add("la ciudad ancestral");
        }
        if (falta.isEmpty()) {
            return "";
        }
        return "§eEste ciclo: §f" + String.join(", ", falta) + ".";
    }

    private void anotarBlazes(Player jugador) {
        if (blazesDe(jugador) >= metaBlaze) {
            blazesHechos.add(jugador.getUniqueId());
        }
    }

    boolean puertaLista(Player jugador) {
        anclarCaza(jugador);
        anotarBlazes(jugador);
        return puertaLista(jugador.getUniqueId());
    }

    boolean puertaLista(UUID id) {
        if (Ciclo.pideDias(ciclo) && System.currentTimeMillis() < aperturaDragon()) {
            return false;
        }
        if (metaBlaze > 0 && !blazesHechos.contains(id)) {
            return false;
        }
        if (Ciclo.pideBastion(ciclo) && !bastionHecho.contains(id)) {
            return false;
        }
        return !Ciclo.pideCiudad(ciclo) || ciudadHecha.contains(id);
    }

    boolean peleaEn(World mundo) {
        return mundo != null && "fin".equals(mundo.getName()) && cazador == null;
    }

    void recordarGolpe(Player jugador) {
        ultimoGolpeador = jugador.getUniqueId();
        ultimoGolpeEn = System.currentTimeMillis();
    }

    Player acreditable() {
        if (ultimoGolpeador == null || System.currentTimeMillis() - ultimoGolpeEn > Pelea.GOLPE_RECIENTE_MS) {
            return null;
        }
        Player jugador = Bukkit.getPlayer(ultimoGolpeador);
        if (jugador == null || !puertaLista(jugador)) {
            return null;
        }
        return jugador;
    }

    void marcarBastion(Player jugador) {
        if (bastionHecho.add(jugador.getUniqueId())) {
            guardarEstado();
            jugador.sendMessage("§aBastión de este ciclo anotado.");
        }
    }

    boolean faltaCiudad(Player jugador) {
        return Ciclo.pideCiudad(ciclo) && !ciudadHecha.contains(jugador.getUniqueId());
    }

    void marcarCiudad(Player jugador) {
        if (ciudadHecha.add(jugador.getUniqueId())) {
            guardarEstado();
            jugador.sendMessage("§aCiudad ancestral de este ciclo anotada.");
        }
    }

    void avisarPelea(Player jugador, String texto) {
        avisarPortal(jugador, texto);
    }

    private void concederSiPendiente(Player jugador) {
        if (!rangoPendiente.remove(jugador.getUniqueId())) {
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + jugador.getName() + " parent add cazador");
        guardarEstado();
        jugador.sendMessage("§dEres Cazador. §fEl rango es del dragón en el que estabas.");
    }

    private Set<UUID> miembros(UUID id) {
        if (grupo == null) {
            return Set.of(id);
        }
        return grupo.miembros(id);
    }
}
