package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Un aldeano del survival se guarda en una ficha y se suelta en la parcela.
 * La ficha apunta a un guardado del servidor: no se puede copiar.
 */
final class Aldeanos implements Listener {

    static final int PRECIO = 40;

    private final ParcelasPlugin plugin;
    private final NamespacedKey claveFicha;
    private final NamespacedKey vaciaKey;
    private final File archivo;
    private final Map<UUID, Ficha> fichas = new HashMap<>();

    Aldeanos(ParcelasPlugin plugin) {
        this.plugin = plugin;
        claveFicha = new NamespacedKey(plugin, "aldeano");
        vaciaKey = new NamespacedKey(plugin, "ficha_vacia");
        archivo = new File(plugin.getDataFolder(), "aldeanos.yml");
        cargar();
    }

    void arrancar() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::encerrar, 10L, 10L);
    }

    boolean puedeComprar(Player jugador) {
        return Cupo.puedeSumarUno(cuantos(jugador.getUniqueId()) + vacias(jugador));
    }

    ItemStack fichaVacia() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dFicha de aldeano");
        meta.setLore(java.util.List.of("§7Agáchate y pulsa un aldeano del survival."));
        meta.getPersistentDataContainer().set(vaciaKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    boolean cabenEnCasa(String clave, UUID comprador) {
        return Cupo.cabe(cuantos(comprador), enCasa(clave));
    }

    void pasarCasa(String clave, UUID comprador) {
        cargarParcela(clave);
        boolean cambio = false;
        for (Ficha ficha : fichas.values()) {
            if (!ficha.bolsillo && clave.equals(ficha.parcela)) {
                ficha.dueno = comprador;
                cambio = true;
            }
        }
        if (!cambio) {
            return;
        }
        retocarCargados(clave, comprador);
        guardar();
    }

    void devolverCasa(Player dueno, String clave) {
        cargarParcela(clave);
        int devueltos = 0;
        int caidos = 0;
        for (Ficha ficha : fichas.values()) {
            if (ficha.bolsillo || !clave.equals(ficha.parcela)) {
                continue;
            }
            Villager vivo = buscar(ficha.id);
            if (vivo != null) {
                ficha.datos = vivo.createSnapshot().getAsString();
                vivo.remove();
            }
            if (ficha.datos == null || ficha.datos.isEmpty()) {
                dueno.sendMessage("§eNo he encontrado a un aldeano de esa parcela. Sigue siendo tuyo.");
                continue;
            }
            ficha.bolsillo = true;
            ficha.parcela = "";
            ItemStack fichaItem = fichaDe(ficha);
            if (dueno.getInventory().firstEmpty() >= 0) {
                dueno.getInventory().addItem(fichaItem);
            } else {
                dueno.getWorld().dropItemNaturally(dueno.getLocation(), fichaItem);
                caidos++;
            }
            devueltos++;
        }
        if (devueltos > 0) {
            dueno.sendMessage("§eTus aldeanos vuelven a ti en fichas.");
        }
        if (caidos > 0) {
            dueno.sendMessage("§eNo te cabían encima. Han caído al suelo.");
        }
        if (devueltos > 0) {
            guardar();
        }
    }

    @EventHandler
    public void alPulsar(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Villager aldeano)) {
            return;
        }
        Player jugador = event.getPlayer();
        if (!jugador.isSneaking()) {
            return;
        }
        ItemStack mano = jugador.getInventory().getItemInMainHand();
        if (!esVacia(mano)) {
            if (vacio(mano)) {
                event.setCancelled(true);
                jugador.sendMessage("§eLa ficha se compra en la tienda. §f/puesto, el botón Aldeano. " + PRECIO + " duros.");
            }
            return;
        }
        event.setCancelled(true);
        guardarEnFicha(jugador, aldeano);
    }

    @EventHandler
    public void alSoltar(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        ItemStack mano = event.getItem();
        UUID id = idDe(mano);
        if (id == null) {
            return;
        }
        event.setCancelled(true);
        soltar(event.getPlayer(), mano, event.getClickedBlock());
    }

    @EventHandler
    public void alCriar(EntityBreedEvent event) {
        if (!(event.getMother() instanceof Villager madre)) {
            return;
        }
        UUID dueno = duenoDe(madre);
        if (dueno == null) {
            return;
        }
        if (Cupo.puedeSumarUno(cuantos(dueno))) {
            return;
        }
        event.setCancelled(true);
        if (event.getBreeder() instanceof Player jugador) {
            jugador.sendMessage("§eYa tienes 8 aldeanos.");
        }
    }

    @EventHandler
    public void alNacer(CreatureSpawnEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.IRON_GOLEM
                && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                && esBarrio(event.getLocation().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Villager bebe)
                || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BREEDING
                || !esBarrio(event.getLocation().getWorld())) {
            return;
        }
        Villager padre = padreCercano(event.getLocation());
        UUID dueno = padre == null ? null : duenoDe(padre);
        if (dueno == null) {
            return;
        }
        if (!Cupo.puedeSumarUno(cuantos(dueno))) {
            event.setCancelled(true);
            return;
        }
        String parcela = padre == null ? "" : parcelaDe(padre);
        if (parcela.isEmpty()) {
            parcela = plugin.claveEn(event.getLocation().getBlockX(), event.getLocation().getBlockZ());
        }
        Ficha ficha = new Ficha(UUID.randomUUID(), dueno, parcela, false, null);
        marcar(bebe, ficha);
        fichas.put(ficha.id, ficha);
        guardar();
    }

    @EventHandler
    public void alMorir(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager aldeano)) {
            return;
        }
        UUID id = idDe(aldeano);
        if (id == null) {
            return;
        }
        fichas.remove(id);
        guardar();
    }

    @EventHandler
    public void alPerderFicha(ItemDespawnEvent event) {
        perder(event.getEntity().getItemStack(), null);
    }

    @EventHandler
    public void alRomperFicha(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item suelto) || event.isCancelled()) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID
                && event.getCause() != EntityDamageEvent.DamageCause.LAVA
                && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                && event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK) {
            return;
        }
        perder(suelto.getItemStack(), null);
    }

    @EventHandler
    public void alCargar(ChunkLoadEvent event) {
        if (!esBarrio(event.getWorld())) {
            return;
        }
        for (Entity entidad : event.getChunk().getEntities()) {
            if (!(entidad instanceof Villager aldeano)) {
                continue;
            }
            UUID id = idDe(aldeano);
            Ficha ficha = id == null ? null : fichas.get(id);
            if (ficha != null && !ficha.bolsillo) {
                marcar(aldeano, ficha);
            }
        }
    }

    private void guardarEnFicha(Player jugador, Villager aldeano) {
        World mundo = aldeano.getWorld();
        boolean salvaje = mundo != null && "survival".equals(mundo.getName());
        boolean barrio = esBarrio(mundo);
        if (!salvaje && !barrio) {
            jugador.sendMessage("§eEste aldeano se queda donde está.");
            return;
        }
        if (llevaFicha(jugador)) {
            jugador.sendMessage("§eYa llevas un aldeano. Suéltalo antes.");
            return;
        }
        ItemStack mano = jugador.getInventory().getItemInMainHand();
        if (!esVacia(mano)) {
            jugador.sendMessage("§eLa ficha se compra en la tienda. §f/puesto, el botón Aldeano. " + PRECIO + " duros.");
            return;
        }
        UUID id = idDe(aldeano);
        Ficha propia = id == null ? null : fichas.get(id);
        if (barrio) {
            String clave = plugin.claveEn(aldeano.getLocation().getBlockX(), aldeano.getLocation().getBlockZ());
            if (clave.isEmpty() || !plugin.esDueno(jugador.getUniqueId(), clave)) {
                jugador.sendMessage("§cEn una parcela, solo el dueño recoge al aldeano.");
                return;
            }
            if (propia != null && !propia.dueno.equals(jugador.getUniqueId())) {
                jugador.sendMessage("§cEste aldeano no es tuyo.");
                return;
            }
        }
        if (propia == null && !Cupo.puedeSumarUno(cuantos(jugador.getUniqueId()))) {
            jugador.sendMessage("§eYa tienes 8 aldeanos.");
            return;
        }
        Ficha ficha = propia == null
            ? new Ficha(UUID.randomUUID(), jugador.getUniqueId(), "", true, aldeano.createSnapshot().getAsString())
            : propia;
        ficha.dueno = jugador.getUniqueId();
        ficha.bolsillo = true;
        ficha.parcela = "";
        ficha.datos = aldeano.createSnapshot().getAsString();
        fichas.put(ficha.id, ficha);
        mano.setAmount(mano.getAmount() - 1);
        aldeano.remove();
        Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(fichaDe(ficha));
        if (!sobra.isEmpty()) {
            jugador.getWorld().dropItem(jugador.getLocation(), sobra.values().iterator().next());
            jugador.sendMessage("§eNo te cabía. La ficha ha caído al suelo.");
        }
        guardar();
        jugador.sendMessage("§aAldeano guardado. §fPulsa el suelo de tu parcela para soltarlo.");
    }

    private void soltar(Player jugador, ItemStack mano, Block suelo) {
        UUID id = idDe(mano);
        Ficha ficha = id == null ? null : fichas.get(id);
        if (ficha == null || !ficha.bolsillo || ficha.datos == null || !ficha.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsta ficha ya no vale.");
            mano.setAmount(0);
            if (ficha != null && !ficha.bolsillo) {
                guardar();
            }
            return;
        }
        if (!esBarrio(suelo.getWorld())) {
            jugador.sendMessage("§eSuéltalo en tu parcela del barrio.");
            return;
        }
        String clave = plugin.claveEn(suelo.getX(), suelo.getZ());
        if (clave.isEmpty() || !plugin.esDueno(jugador.getUniqueId(), clave)) {
            jugador.sendMessage("§cSolo en una parcela tuya.");
            return;
        }
        Block hueco = suelo.getRelative(0, 1, 0);
        if (!hueco.isPassable()) {
            jugador.sendMessage("§eAhí no cabe.");
            return;
        }
        Location sitio = hueco.getLocation().add(0.5, 0, 0.5);
        Villager aldeano;
        try {
            Entity creado = Bukkit.getEntityFactory().createEntitySnapshot(ficha.datos).createEntity(sitio.getWorld());
            if (!(creado instanceof Villager villager) || !villager.spawnAt(sitio, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                if (creado != null) {
                    creado.remove();
                }
                jugador.sendMessage("§cNo he podido soltar a ese aldeano.");
                return;
            }
            aldeano = villager;
        } catch (IllegalArgumentException e) {
            jugador.sendMessage("§cEsta ficha está rota.");
            return;
        }
        ficha.bolsillo = false;
        ficha.parcela = clave;
        ficha.datos = null;
        marcar(aldeano, ficha);
        aldeano.setRemoveWhenFarAway(false);
        aldeano.setPersistent(true);
        mano.setAmount(mano.getAmount() - 1);
        guardar();
        jugador.sendMessage("§aAquí se queda.");
    }

    private void encerrar() {
        World barrio = Bukkit.getWorld("world");
        if (barrio == null) {
            return;
        }
        for (Villager aldeano : barrio.getEntitiesByClass(Villager.class)) {
            UUID id = idDe(aldeano);
            Ficha ficha = id == null ? null : fichas.get(id);
            if (ficha == null || ficha.bolsillo || ficha.parcela.isEmpty()) {
                continue;
            }
            String actual = plugin.claveEn(aldeano.getLocation().getBlockX(), aldeano.getLocation().getBlockZ());
            if (ficha.parcela.equals(actual)) {
                continue;
            }
            aldeano.leaveVehicle();
            aldeano.teleport(centro(barrio, ficha.parcela));
        }
    }

    private void cargarParcela(String clave) {
        World barrio = Bukkit.getWorld("world");
        if (barrio == null || clave.isEmpty() || !clave.contains(",")) {
            return;
        }
        String[] partes = clave.split(",");
        int i = Integer.parseInt(partes[0]);
        int j = Integer.parseInt(partes[1]);
        int minX = plugin.rejilla().origen(i);
        int minZ = plugin.rejilla().origen(j);
        int lado = plugin.lado();
        for (int x = minX; x < minX + lado; x += 16) {
            for (int z = minZ; z < minZ + lado; z += 16) {
                barrio.getChunkAt(x >> 4, z >> 4).load();
            }
        }
    }

    private Location centro(World mundo, String clave) {
        String[] partes = clave.split(",");
        int i = Integer.parseInt(partes[0]);
        int j = Integer.parseInt(partes[1]);
        int x = plugin.rejilla().origen(i) + plugin.lado() / 2;
        int z = plugin.rejilla().origen(j) + plugin.lado() / 2;
        int y = mundo.getHighestBlockYAt(x, z) + 1;
        return new Location(mundo, x + 0.5, y, z + 0.5);
    }

    private void perder(ItemStack item, Player avisado) {
        UUID id = idDe(item);
        Ficha ficha = id == null ? null : fichas.get(id);
        if (ficha == null || !ficha.bolsillo) {
            return;
        }
        fichas.remove(id);
        guardar();
        Player dueno = avisado != null ? avisado : Bukkit.getPlayer(ficha.dueno);
        if (dueno != null) {
            dueno.sendMessage("§cSe ha perdido un aldeano. La ficha desapareció.");
        }
    }

    private void retocarCargados(String clave, UUID dueno) {
        World barrio = Bukkit.getWorld("world");
        if (barrio == null) {
            return;
        }
        for (Villager aldeano : barrio.getEntitiesByClass(Villager.class)) {
            UUID id = idDe(aldeano);
            Ficha ficha = id == null ? null : fichas.get(id);
            if (ficha != null && clave.equals(ficha.parcela)) {
                ficha.dueno = dueno;
                marcar(aldeano, ficha);
            }
        }
    }

    private Villager padreCercano(Location sitio) {
        Villager mejor = null;
        double distancia = 16;
        for (Entity entidad : sitio.getWorld().getNearbyEntities(sitio, 8, 4, 8)) {
            if (!(entidad instanceof Villager aldeano) || idDe(aldeano) == null) {
                continue;
            }
            double actual = entidad.getLocation().distanceSquared(sitio);
            if (actual < distancia) {
                distancia = actual;
                mejor = aldeano;
            }
        }
        return mejor;
    }

    private Villager buscar(UUID id) {
        World barrio = Bukkit.getWorld("world");
        if (barrio == null) {
            return null;
        }
        for (Villager aldeano : barrio.getEntitiesByClass(Villager.class)) {
            if (id.equals(idDe(aldeano))) {
                return aldeano;
            }
        }
        return null;
    }

    private int cuantos(UUID dueno) {
        int total = 0;
        for (Ficha ficha : fichas.values()) {
            if (dueno.equals(ficha.dueno)) {
                total++;
            }
        }
        return total;
    }

    private int enCasa(String clave) {
        int total = 0;
        for (Ficha ficha : fichas.values()) {
            if (!ficha.bolsillo && clave.equals(ficha.parcela)) {
                total++;
            }
        }
        return total;
    }

    private int vacias(Player jugador) {
        int total = 0;
        for (ItemStack item : jugador.getInventory().getContents()) {
            if (esVacia(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private boolean esVacia(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(vaciaKey, PersistentDataType.BYTE);
        return marca != null && marca == (byte) 1;
    }

    private boolean llevaFicha(Player jugador) {
        PlayerInventory inventario = jugador.getInventory();
        for (ItemStack item : inventario.getContents()) {
            if (idDe(item) != null) {
                return true;
            }
        }
        return false;
    }

    private ItemStack fichaDe(Ficha ficha) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dAldeano");
        meta.setLore(java.util.List.of("§7Suéltalo en tu parcela."));
        meta.getPersistentDataContainer().set(claveFicha, PersistentDataType.STRING, ficha.id.toString());
        item.setItemMeta(meta);
        return item;
    }

    private void marcar(Villager aldeano, Ficha ficha) {
        aldeano.getPersistentDataContainer().set(claveFicha, PersistentDataType.STRING, ficha.id.toString());
        aldeano.getPersistentDataContainer().set(new NamespacedKey(plugin, "dueno"), PersistentDataType.STRING, ficha.dueno.toString());
    }

    private UUID idDe(Villager aldeano) {
        String texto = aldeano.getPersistentDataContainer().get(claveFicha, PersistentDataType.STRING);
        return leerId(texto);
    }

    private UUID idDe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return leerId(item.getItemMeta().getPersistentDataContainer().get(claveFicha, PersistentDataType.STRING));
    }

    private UUID duenoDe(Villager aldeano) {
        UUID id = idDe(aldeano);
        Ficha ficha = id == null ? null : fichas.get(id);
        return ficha == null ? null : ficha.dueno;
    }

    private String parcelaDe(Villager aldeano) {
        UUID id = idDe(aldeano);
        Ficha ficha = id == null ? null : fichas.get(id);
        return ficha == null ? "" : ficha.parcela;
    }

    private static UUID leerId(String texto) {
        if (texto == null || texto.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean vacio(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static boolean esBarrio(World mundo) {
        return mundo != null && "world".equals(mundo.getName());
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection raiz = yaml.getConfigurationSection("fichas");
        if (raiz == null) {
            return;
        }
        for (String texto : raiz.getKeys(false)) {
            try {
                UUID id = UUID.fromString(texto);
                UUID dueno = UUID.fromString(raiz.getString(texto + ".dueno", ""));
                fichas.put(id, new Ficha(
                    id,
                    dueno,
                    raiz.getString(texto + ".parcela", ""),
                    raiz.getBoolean(texto + ".bolsillo"),
                    raiz.getString(texto + ".datos", null)
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoro un aldeano con id " + texto);
            }
        }
    }

    void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Ficha ficha : fichas.values()) {
            String camino = "fichas." + ficha.id;
            yaml.set(camino + ".dueno", ficha.dueno.toString());
            yaml.set(camino + ".parcela", ficha.parcela);
            yaml.set(camino + ".bolsillo", ficha.bolsillo);
            if (ficha.datos != null && !ficha.datos.isEmpty()) {
                yaml.set(camino + ".datos", ficha.datos);
            }
        }
        try {
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude guardar los aldeanos: " + e.getMessage());
        }
    }

    private static final class Ficha {
        final UUID id;
        UUID dueno;
        String parcela;
        boolean bolsillo;
        String datos;

        Ficha(UUID id, UUID dueno, String parcela, boolean bolsillo, String datos) {
            this.id = id;
            this.dueno = dueno;
            this.parcela = parcela == null ? "" : parcela;
            this.bolsillo = bolsillo;
            this.datos = datos;
        }
    }
}
