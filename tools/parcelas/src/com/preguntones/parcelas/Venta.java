package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

final class Venta implements Listener {
    private static final String MARCA = "preguntones-venta";
    private static final String CAMPO_PRECIO = "precio";
    private static final int HUECOS = 18;
    private static final int MAX_POR_PARCELA = 4;
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor ORO = TextColor.color(0xE6B84D);
    private static final Color FONDO = Color.fromARGB(180, 16, 24, 20);

    private final ParcelasPlugin plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey idKey;
    private final NamespacedKey recetaKey;
    private final File archivo;
    private final Map<String, Puesto> puestos = new HashMap<>();
    private final Map<UUID, List<ItemStack>> recuperar = new HashMap<>();
    private final Map<String, UUID> mirando = new HashMap<>();
    private final Map<UUID, String> abierto = new HashMap<>();
    private final Map<UUID, PrecioPedido> precioPedido = new HashMap<>();
    private final Map<UUID, int[]> cantidad = new HashMap<>();
    private final java.util.Set<UUID> saltar = new java.util.HashSet<>();

    Venta(ParcelasPlugin plugin) {
        this.plugin = plugin;
        itemKey = new NamespacedKey(plugin, "cofre_venta");
        idKey = new NamespacedKey(plugin, "venta_id");
        recetaKey = new NamespacedKey(plugin, "cofre_venta");
        archivo = new File(plugin.getDataFolder(), "ventas.yml");
        cargar();
    }

    @EventHandler
    public void alCargarServidor(ServerLoadEvent event) {
        registrarReceta();
    }

    void arrancar() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.discoverRecipe(recetaKey);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::repasar, 40L);
    }

    boolean orden(Player jugador, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("recuperar")) {
            entregarPendiente(jugador, true);
            return true;
        }
        jugador.sendMessage("§eEl cofre de venta se craftea. §fCuatro lingotes de oro, un cofre y un cartel.");
        jugador.sendMessage("§fPonlo en tu parcela. §e/tienda recuperar §fdevuelve el género si el cofre ya no está.");
        return true;
    }

    boolean alColocar(BlockPlaceEvent event) {
        ItemStack mano = event.getItemInHand();
        if (!esItem(mano)) {
            return false;
        }
        event.setCancelled(true);
        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();
        String motivo = plugin.motivoVenta(jugador, bloque.getX(), bloque.getZ());
        if (motivo != null) {
            jugador.sendMessage("§c" + motivo);
            return true;
        }
        EquipmentSlot manoSlot = event.getHand();
        BlockFace cara = jugador.getFacing().getOppositeFace();
        Bukkit.getScheduler().runTask(plugin, () -> colocar(jugador, bloque, manoSlot, cara));
        return true;
    }

    boolean esPuesto(Block bloque) {
        return idDe(bloque) != null;
    }

    boolean hayAlguno(String parcela) {
        for (Puesto puesto : puestos.values()) {
            if (parcela.equals(puesto.parcela)) {
                return true;
            }
        }
        return false;
    }

    int cuantos(String parcela) {
        int n = 0;
        for (Puesto puesto : puestos.values()) {
            if (parcela.equals(puesto.parcela)) {
                n++;
            }
        }
        return n;
    }

    void recogerParcela(String parcela) {
        List<String> ids = new ArrayList<>();
        for (Puesto puesto : puestos.values()) {
            if (parcela.equals(puesto.parcela)) {
                ids.add(puesto.id);
            }
        }
        for (String id : ids) {
            recoger(puestos.get(id), false);
        }
    }

    boolean romper(Player jugador, Block bloque) {
        String id = idDe(bloque);
        if (id == null) {
            return false;
        }
        Puesto puesto = puestos.get(id);
        if (puesto == null || !puesto.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEste cofre no es tuyo.");
            return true;
        }
        if (tieneGenero(puesto)) {
            jugador.sendMessage("§cVacía el cofre antes de recogerlo.");
            return true;
        }
        quitarBloque(puesto);
        puestos.remove(id);
        guardar();
        Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(item());
        for (ItemStack resto : sobra.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), resto);
        }
        jugador.sendMessage("§aCofre de venta recogido.");
        return true;
    }

    void abrir(Player jugador, Block bloque, boolean dueno) {
        String id = idDe(bloque);
        Puesto puesto = id == null ? null : puestos.get(id);
        if (puesto == null) {
            return;
        }
        UUID quien = mirando.get(id);
        if (quien != null && !quien.equals(jugador.getUniqueId()) && Bukkit.getPlayer(quien) != null) {
            jugador.sendMessage("§cAlguien está mirando esta venta.");
            return;
        }
        soltarOtro(jugador, id);
        mirando.put(id, jugador.getUniqueId());
        abierto.put(jugador.getUniqueId(), id);
        jugador.playSound(bloque.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.1f);
        if (dueno && puesto.dueno.equals(jugador.getUniqueId())) {
            abrirDueno(jugador, puesto);
            return;
        }
        abrirCompra(jugador, puesto);
    }

    private void colocar(Player jugador, Block bloque, EquipmentSlot manoSlot, BlockFace cara) {
        if (!jugador.isOnline()) {
            return;
        }
        String motivo = plugin.motivoVenta(jugador, bloque.getX(), bloque.getZ());
        if (motivo != null) {
            jugador.sendMessage("§c" + motivo);
            return;
        }
        if (!bloque.getType().isAir() && !bloque.isReplaceable()) {
            jugador.sendMessage("§cAhí no cabe.");
            return;
        }
        ItemStack enMano = manoSlot == EquipmentSlot.OFF_HAND
            ? jugador.getInventory().getItemInOffHand()
            : jugador.getInventory().getItemInMainHand();
        if (!esItem(enMano)) {
            jugador.sendMessage("§cYa no tienes el cofre.");
            return;
        }
        if (enMano.getAmount() <= 1) {
            if (manoSlot == EquipmentSlot.OFF_HAND) {
                jugador.getInventory().setItemInOffHand(null);
            } else {
                jugador.getInventory().setItemInMainHand(null);
            }
        } else {
            enMano.setAmount(enMano.getAmount() - 1);
        }
        bloque.setType(Material.BARREL, false);
        if (bloque.getBlockData() instanceof Directional directional) {
            BlockFace frente = cara == null || cara == BlockFace.UP || cara == BlockFace.DOWN ? BlockFace.NORTH : cara;
            directional.setFacing(frente);
            bloque.setBlockData(directional);
        }
        String id = UUID.randomUUID().toString();
        if (bloque.getState() instanceof TileState tile) {
            tile.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
            tile.update(true, false);
        }
        Puesto puesto = new Puesto(id);
        puesto.dueno = jugador.getUniqueId();
        puesto.nombre = jugador.getName();
        puesto.parcela = plugin.claveEn(bloque.getX(), bloque.getZ());
        puesto.mundo = bloque.getWorld().getName();
        puesto.x = bloque.getX();
        puesto.y = bloque.getY();
        puesto.z = bloque.getZ();
        puestos.put(id, puesto);
        guardar();
        asegurarLetrero(puesto);
        jugador.sendMessage("§aCofre de venta listo. §fArrastra el género. Clic en él para escribir el precio.");
    }

    private void registrarReceta() {
        Bukkit.removeRecipe(recetaKey);
        ShapedRecipe receta = new ShapedRecipe(recetaKey, item());
        receta.shape("GCG", "GSG");
        receta.setIngredient('G', Material.GOLD_INGOT);
        receta.setIngredient('C', Material.CHEST);
        receta.setIngredient('S', new RecipeChoice.MaterialChoice(carteles()));
        Bukkit.addRecipe(receta);
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.discoverRecipe(recetaKey);
        }
        plugin.getLogger().info("Receta del cofre de venta lista.");
    }

    private static List<Material> carteles() {
        List<Material> lista = new ArrayList<>();
        for (Material material : Material.values()) {
            String nombre = material.name();
            if (material.isItem() && nombre.endsWith("_SIGN") && !nombre.contains("HANGING")) {
                lista.add(material);
            }
        }
        if (lista.isEmpty()) {
            lista.add(Material.OAK_SIGN);
        }
        return lista;
    }

    private ItemStack item() {
        ItemStack creado = new ItemStack(Material.CHEST);
        ItemMeta meta = creado.getItemMeta();
        meta.displayName(Component.text("Cofre de venta", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Ponlo en tu parcela.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Lo que metas, se vende.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setItemModel(new NamespacedKey("preguntones", "cofre_venta"));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        creado.setItemMeta(meta);
        return creado;
    }

    private boolean esItem(ItemStack item) {
        if (item == null || item.getType() != Material.CHEST || !item.hasItemMeta()) {
            return false;
        }
        Byte marca = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.BYTE);
        return marca != null && marca == (byte) 1;
    }

    private boolean prohibido(ItemStack item) {
        if (item == null || esItem(item)) {
            return true;
        }
        Material tipo = item.getType();
        return tipo == Material.COMMAND_BLOCK
            || tipo == Material.CHAIN_COMMAND_BLOCK
            || tipo == Material.REPEATING_COMMAND_BLOCK
            || tipo == Material.COMMAND_BLOCK_MINECART
            || tipo == Material.BARRIER
            || tipo == Material.DEBUG_STICK
            || tipo == Material.STRUCTURE_BLOCK
            || tipo == Material.STRUCTURE_VOID
            || tipo == Material.JIGSAW
            || tipo == Material.LIGHT;
    }

    private void abrirDueno(Player jugador, Puesto puesto) {
        Menu menu = new Menu(Menu.Tipo.DUENO, puesto.id);
        Inventory inventario = Bukkit.createInventory(menu, 27, "Tu venta");
        menu.inventario = inventario;
        pintarDueno(inventario, puesto, jugador);
        saltar.add(jugador.getUniqueId());
        jugador.openInventory(inventario);
        saltar.remove(jugador.getUniqueId());
    }

    private void abrirCompra(Player jugador, Puesto puesto) {
        Menu menu = new Menu(Menu.Tipo.COMPRA, puesto.id);
        Inventory inventario = Bukkit.createInventory(menu, 27, "Venta");
        menu.inventario = inventario;
        pintarCompra(inventario, puesto, jugador);
        saltar.add(jugador.getUniqueId());
        jugador.openInventory(inventario);
        saltar.remove(jugador.getUniqueId());
    }

    private void abrirCantidad(Player jugador, Puesto puesto, int hueco) {
        ItemStack genero = puesto.huecos[hueco];
        int precio = puesto.precios[hueco];
        if (genero == null || !precioValido(precio)) {
            abrirCompra(jugador, puesto);
            return;
        }
        int maximo = Cuentas.tope(genero.getAmount(), precio);
        if (maximo <= 0) {
            jugador.sendMessage("§cEsa compra pasa de " + Cuentas.COMPRA_MAX + " duros.");
            return;
        }
        cantidad.put(jugador.getUniqueId(), new int[] {hueco, 1});
        Menu menu = new Menu(Menu.Tipo.CANTIDAD, puesto.id);
        Inventory inventario = Bukkit.createInventory(menu, 27, "Cuántos");
        menu.inventario = inventario;
        pintarCantidad(inventario, puesto, jugador, hueco, 1);
        saltar.add(jugador.getUniqueId());
        jugador.openInventory(inventario);
        saltar.remove(jugador.getUniqueId());
    }

    private void pintarDueno(Inventory inventario, Puesto puesto, Player jugador) {
        inventario.clear();
        for (int i = 0; i < HUECOS; i++) {
            inventario.setItem(i, vistaDueno(puesto.huecos[i], puesto.precios[i]));
        }
        inventario.setItem(18, guia(puesto));
        inventario.setItem(22, saldo(jugador));
        inventario.setItem(26, cerrar());
        for (int i = HUECOS; i < 27; i++) {
            if (inventario.getItem(i) == null) {
                inventario.setItem(i, relleno());
            }
        }
    }

    private void pintarCompra(Inventory inventario, Puesto puesto, Player jugador) {
        inventario.clear();
        boolean alguno = false;
        for (int i = 0; i < HUECOS; i++) {
            if (puesto.huecos[i] != null && precioValido(puesto.precios[i])) {
                inventario.setItem(i, vistaCompra(puesto.huecos[i], puesto.precios[i]));
                alguno = true;
            } else {
                inventario.setItem(i, relleno());
            }
        }
        if (!alguno) {
            inventario.setItem(13, boton(Material.BARRIER, "Nada a la venta", NamedTextColor.RED, List.of("Aquí salen las cosas con precio.")));
        }
        inventario.setItem(18, boton(Material.OAK_HANGING_SIGN, "Venta de " + puesto.nombre, NamedTextColor.GOLD, List.of("El precio es por cada uno.")));
        inventario.setItem(22, saldo(jugador));
        inventario.setItem(26, cerrar());
        for (int i = HUECOS; i < 27; i++) {
            if (inventario.getItem(i) == null) {
                inventario.setItem(i, relleno());
            }
        }
    }

    private void pintarCantidad(Inventory inventario, Puesto puesto, Player jugador, int hueco, int unidades) {
        inventario.clear();
        inventario.setItem(1, paso(Material.RED_CONCRETE, "−64", -64));
        inventario.setItem(2, paso(Material.RED_TERRACOTTA, "−10", -10));
        inventario.setItem(3, paso(Material.PINK_DYE, "−1", -1));
        inventario.setItem(4, vistaUnidades(puesto.huecos[hueco], puesto.precios[hueco], unidades));
        inventario.setItem(5, paso(Material.LIME_DYE, "+1", 1));
        inventario.setItem(6, paso(Material.LIME_TERRACOTTA, "+10", 10));
        inventario.setItem(7, paso(Material.LIME_CONCRETE, "+64", 64));
        inventario.setItem(13, resumen(puesto.precios[hueco], unidades));
        inventario.setItem(15, accion(Material.EMERALD, "Comprar", "comprar"));
        inventario.setItem(16, accion(Material.BARRIER, "Volver", "volver"));
        inventario.setItem(22, saldo(jugador));
        for (int i = 0; i < 27; i++) {
            if (inventario.getItem(i) == null) {
                inventario.setItem(i, relleno());
            }
        }
    }

    private ItemStack vistaDueno(ItemStack genero, int precio) {
        if (genero == null || genero.getType().isAir()) {
            return null;
        }
        ItemStack vista = genero.clone();
        ItemMeta meta = vista.getItemMeta();
        meta.displayName(nombreConPrecio(genero, meta, precio, true));
        if (precio < Cuentas.PRECIO_MIN) {
            meta.setEnchantmentGlintOverride(true);
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (precio >= Cuentas.PRECIO_MIN) {
            lore.add(texto(precio + " duros cada uno", NamedTextColor.GOLD));
            lore.add(texto("Clic: cambiar el precio", NamedTextColor.GRAY));
        } else {
            lore.add(texto("Sin precio", NamedTextColor.RED));
            lore.add(texto("Clic: escribir el precio", NamedTextColor.GOLD));
        }
        lore.add(texto("Shift: recoger", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        vista.setItemMeta(meta);
        return vista;
    }

    private ItemStack vistaCompra(ItemStack genero, int precio) {
        ItemStack vista = genero.clone();
        ItemMeta meta = vista.getItemMeta();
        meta.displayName(nombreConPrecio(genero, meta, precio, false));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(texto(precio + " duros cada uno", NamedTextColor.GOLD));
        lore.add(texto("Clic para elegir cuántos", NamedTextColor.GRAY));
        meta.lore(lore);
        vista.setItemMeta(meta);
        return vista;
    }

    private ItemStack vistaUnidades(ItemStack genero, int precio, int unidades) {
        ItemStack vista = genero.clone();
        vista.setAmount(Math.min(unidades, genero.getMaxStackSize()));
        ItemMeta meta = vista.getItemMeta();
        meta.displayName(nombreConPrecio(genero, meta, precio, false));
        meta.lore(List.of(
            texto(unidades + " × " + precio + " duros", NamedTextColor.GOLD),
            texto("Pagas " + (unidades * precio) + " duros", NamedTextColor.WHITE)
        ));
        vista.setItemMeta(meta);
        return vista;
    }

    private Component nombreConPrecio(ItemStack genero, ItemMeta meta, int precio, boolean dueno) {
        Component base = meta.hasDisplayName() ? meta.displayName() : null;
        if (base == null) {
            base = Component.translatable(genero.getType().getItemTranslationKey());
        }
        base = base.decoration(TextDecoration.ITALIC, false);
        if (precio >= Cuentas.PRECIO_MIN) {
            return base.append(texto(" · " + precio + " duros", NamedTextColor.GOLD));
        }
        if (dueno) {
            return base.append(texto(" · sin precio", NamedTextColor.RED));
        }
        return base;
    }

    private ItemStack guia(Puesto puesto) {
        int venta = 0;
        int falta = 0;
        for (int i = 0; i < HUECOS; i++) {
            ItemStack genero = puesto.huecos[i];
            if (genero == null || genero.getType().isAir()) {
                continue;
            }
            if (precioValido(puesto.precios[i])) {
                venta++;
            } else {
                falta++;
            }
        }
        if (venta == 0 && falta == 0) {
            return boton(Material.HOPPER, "Vacío", NamedTextColor.WHITE, List.of(
                "Arrastra el género aquí.",
                "Luego clic para el precio."
            ));
        }
        if (falta > 0) {
            return boton(Material.ORANGE_DYE, "Falta el precio", NamedTextColor.GOLD, List.of(
                falta + (falta == 1 ? " cosa sin precio." : " cosas sin precio."),
                "Clic en ella y escribe los duros."
            ));
        }
        return boton(Material.EMERALD, "En venta", NamedTextColor.GREEN, List.of(
            venta + (venta == 1 ? " cosa con precio." : " cosas con precio."),
            "Clic en una para cambiarlo."
        ));
    }

    private ItemStack resumen(int precio, int unidades) {
        int total = unidades * precio;
        int tasa = Cuentas.impuesto(total, plugin.porcentajeImpuesto());
        return boton(Material.PAPER, "Pagas " + total + " duros", NamedTextColor.GOLD, List.of(
            unidades + (unidades == 1 ? " unidad." : " unidades."),
            "El barrio se queda " + tasa + ".",
            "Recibe " + (total - tasa) + "."
        ));
    }

    private ItemStack saldo(Player jugador) {
        return boton(Material.GOLD_NUGGET, "Tus duros", NamedTextColor.GOLD, List.of(plugin.duros(jugador) + " duros"));
    }

    private ItemStack cerrar() {
        return boton(Material.BARRIER, "Cerrar", NamedTextColor.RED, null);
    }

    private ItemStack accion(Material material, String nombre, String orden) {
        NamedTextColor color = "comprar".equals(orden) ? NamedTextColor.GREEN : NamedTextColor.RED;
        ItemStack creado = boton(material, nombre, color, null);
        ItemMeta meta = creado.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "orden"), PersistentDataType.STRING, orden);
        creado.setItemMeta(meta);
        return creado;
    }

    private ItemStack paso(Material material, String nombre, int delta) {
        NamedTextColor color = delta < 0 ? NamedTextColor.RED : NamedTextColor.GREEN;
        ItemStack creado = boton(material, nombre, color, null);
        ItemMeta meta = creado.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "paso"), PersistentDataType.INTEGER, delta);
        creado.setItemMeta(meta);
        return creado;
    }

    private ItemStack boton(Material material, String nombre, List<String> lineas) {
        return boton(material, nombre, NamedTextColor.WHITE, lineas);
    }

    private ItemStack boton(Material material, String nombre, NamedTextColor color, List<String> lineas) {
        ItemStack creado = new ItemStack(material);
        ItemMeta meta = creado.getItemMeta();
        meta.displayName(texto(nombre, color));
        if (lineas != null) {
            List<Component> lore = new ArrayList<>();
            for (String linea : lineas) {
                lore.add(texto(linea, NamedTextColor.GRAY));
            }
            meta.lore(lore);
        }
        creado.setItemMeta(meta);
        return creado;
    }

    private ItemStack relleno() {
        ItemStack creado = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = creado.getItemMeta();
        meta.displayName(texto(" ", NamedTextColor.DARK_GRAY));
        meta.setHideTooltip(true);
        creado.setItemMeta(meta);
        return creado;
    }

    private static boolean precioValido(int precio) {
        return precio >= Cuentas.PRECIO_MIN && precio <= Cuentas.PRECIO_MAX;
    }

    private Component texto(String valor, NamedTextColor color) {
        return Component.text(valor, color).decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler
    public void alCraftear(PrepareItemCraftEvent event) {
        if (event.getRecipe() instanceof ShapedRecipe receta && recetaKey.equals(receta.getKey())) {
            event.getInventory().setResult(item());
        }
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(recetaKey);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                entregarPendiente(event.getPlayer(), false);
            }
        }, 40L);
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        soltar(event.getPlayer().getUniqueId());
        precioPedido.remove(event.getPlayer().getUniqueId());
        cantidad.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void alChunk(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals("world")) {
            return;
        }
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        for (Puesto puesto : puestos.values()) {
            if (puesto.x >> 4 == cx && puesto.z >> 4 == cz) {
                asegurarLetrero(puesto);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void alAbrirCaja(org.bukkit.event.inventory.InventoryOpenEvent event) {
        Location sitio = event.getInventory().getLocation();
        if (sitio != null && esPuesto(sitio.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alMover(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (esInventario(event.getSource()) || esInventario(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean esInventario(Inventory inventario) {
        Location sitio = inventario.getLocation();
        return sitio != null && esPuesto(sitio.getBlock());
    }

    @EventHandler
    public void alPiston(BlockPistonExtendEvent event) {
        if (toca(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alPiston(BlockPistonRetractEvent event) {
        if (toca(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alCambiar(EntityChangeBlockEvent event) {
        if (esPuesto(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alCorrer(BlockFromToEvent event) {
        if (esPuesto(event.getToBlock()) || esPuesto(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alPulsar(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            Puesto puesto = puestos.get(menu.id);
            if (puesto == null) {
                event.setCancelled(true);
                jugador.closeInventory();
                return;
            }
            if (menu.tipo == Menu.Tipo.DUENO && clicEnSuInventario(event)) {
                return;
            }
            event.setCancelled(true);
            if (menu.tipo == Menu.Tipo.DUENO) {
                pulsarDueno(jugador, puesto, event);
            } else if (menu.tipo == Menu.Tipo.COMPRA) {
                pulsarCompra(jugador, puesto, event);
            } else {
                pulsarCantidad(jugador, puesto, event);
            }
            return;
        }
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        if (menu.tipo != Menu.Tipo.DUENO) {
            event.setCancelled(true);
            return;
        }
        int arriba = event.getView().getTopInventory().getSize();
        boolean tienda = false;
        boolean relleno = false;
        for (int raw : event.getRawSlots()) {
            if (raw < HUECOS) {
                tienda = true;
            } else if (raw < arriba) {
                relleno = true;
            }
        }
        if (!tienda && !relleno) {
            return;
        }
        event.setCancelled(true);
        if (!tienda) {
            return;
        }
        Puesto puesto = puestos.get(menu.id);
        if (puesto == null || !puesto.dueno.equals(jugador.getUniqueId())) {
            return;
        }
        ItemStack cursor = event.getOldCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }
        if (prohibido(cursor)) {
            jugador.sendMessage("§cEso no se vende.");
            return;
        }
        List<Integer> orden = new ArrayList<>();
        for (int raw : event.getRawSlots()) {
            if (raw >= 0 && raw < HUECOS) {
                orden.add(raw);
            }
        }
        Collections.sort(orden);
        boolean deUno = event.getType() == DragType.SINGLE;
        ItemStack muestra = cursor.clone();
        Bukkit.getScheduler().runTask(plugin, () -> aplicarArrastre(jugador, puesto, orden, muestra, deUno));
    }

    private boolean clicEnSuInventario(InventoryClickEvent event) {
        boolean abajo = event.getClickedInventory() == event.getView().getBottomInventory() || event.getRawSlot() == -999;
        if (!abajo) {
            return false;
        }
        ClickType tipo = event.getClick();
        return tipo != ClickType.SHIFT_LEFT
            && tipo != ClickType.SHIFT_RIGHT
            && tipo != ClickType.DOUBLE_CLICK
            && tipo != ClickType.CREATIVE
            && tipo != ClickType.UNKNOWN;
    }

    private boolean clicDeElegir(InventoryClickEvent event) {
        ClickType tipo = event.getClick();
        return tipo == ClickType.LEFT
            || tipo == ClickType.RIGHT
            || tipo == ClickType.SHIFT_LEFT
            || tipo == ClickType.SHIFT_RIGHT;
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player jugador)) {
            return;
        }
        if (saltar.remove(jugador.getUniqueId())) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Menu) {
            soltar(jugador.getUniqueId());
            cantidad.remove(jugador.getUniqueId());
        }
    }

    private void pulsarDueno(Player jugador, Puesto puesto, InventoryClickEvent event) {
        int raw = event.getRawSlot();
        if (raw == 26) {
            jugador.closeInventory();
            return;
        }
        if (raw >= 0 && raw < HUECOS && event.getClickedInventory() == event.getView().getTopInventory()) {
            ClickType tipo = event.getClick();
            if (tipo == ClickType.NUMBER_KEY) {
                cambiarConBarra(jugador, puesto, raw, event.getHotbarButton());
                return;
            }
            if (tipo == ClickType.DOUBLE_CLICK || tipo == ClickType.DROP || tipo == ClickType.CONTROL_DROP || tipo == ClickType.CREATIVE) {
                return;
            }
            if (event.isShiftClick()) {
                sacar(jugador, puesto, raw);
                return;
            }
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (prohibido(cursor)) {
                    jugador.sendMessage("§cEso no se vende.");
                    return;
                }
                ItemStack alli = puesto.huecos[raw];
                if (alli != null && !alli.getType().isAir() && !alli.isSimilar(cursor)) {
                    intercambiarCursor(jugador, puesto, raw, cursor);
                    return;
                }
                int maximo = tipo == ClickType.RIGHT ? 1 : cursor.getAmount();
                meter(jugador, puesto, raw, cursor.clone(), true, -1, maximo, true);
                return;
            }
            if (puesto.huecos[raw] != null && (tipo == ClickType.LEFT || tipo == ClickType.RIGHT)) {
                pedirPrecio(jugador, puesto, raw);
            }
            return;
        }
        if (event.isShiftClick() && event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack actual = event.getCurrentItem();
            if (actual != null && !actual.getType().isAir()) {
                meter(jugador, puesto, -1, actual.clone(), false, event.getSlot());
            }
        }
    }

    private void pulsarCompra(Player jugador, Puesto puesto, InventoryClickEvent event) {
        if (!clicDeElegir(event)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw == 26) {
            jugador.closeInventory();
            return;
        }
        if (raw < 0 || raw >= HUECOS || puesto.huecos[raw] == null || !precioValido(puesto.precios[raw])) {
            return;
        }
        if (puesto.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEs tu venta.");
            return;
        }
        abrirCantidad(jugador, puesto, raw);
    }

    private void pulsarCantidad(Player jugador, Puesto puesto, InventoryClickEvent event) {
        if (!clicDeElegir(event)) {
            return;
        }
        ItemStack actual = event.getCurrentItem();
        if (actual == null || !actual.hasItemMeta()) {
            return;
        }
        String orden = actual.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "orden"), PersistentDataType.STRING);
        if ("volver".equals(orden)) {
            abrirCompra(jugador, puesto);
            return;
        }
        if ("comprar".equals(orden)) {
            comprar(jugador, puesto);
            return;
        }
        Integer paso = actual.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "paso"), PersistentDataType.INTEGER);
        if (paso == null) {
            return;
        }
        int[] seleccion = cantidad.get(jugador.getUniqueId());
        if (seleccion == null) {
            return;
        }
        ItemStack genero = puesto.huecos[seleccion[0]];
        if (genero == null) {
            abrirCompra(jugador, puesto);
            return;
        }
        int maximo = Cuentas.tope(genero.getAmount(), puesto.precios[seleccion[0]]);
        int siguiente = seleccion[1] + paso;
        if (siguiente < 1) {
            siguiente = 1;
        }
        if (siguiente > maximo) {
            if (paso > 0) {
                jugador.sendMessage("§cEl tope de una compra son " + Cuentas.COMPRA_MAX + " duros.");
            }
            siguiente = maximo;
        }
        seleccion[1] = Math.max(1, siguiente);
        pintarCantidad(event.getView().getTopInventory(), puesto, jugador, seleccion[0], seleccion[1]);
    }

    private void comprar(Player jugador, Puesto puesto) {
        int[] seleccion = cantidad.get(jugador.getUniqueId());
        if (seleccion == null) {
            return;
        }
        if (plugin.enMora(jugador.getUniqueId())) {
            jugador.sendMessage("§cDebes al banco. No puedes comprar hasta saldar.");
            return;
        }
        if (puesto.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEs tu venta.");
            return;
        }
        int hueco = seleccion[0];
        int unidades = seleccion[1];
        ItemStack genero = puesto.huecos[hueco];
        int precio = puesto.precios[hueco];
        if (genero == null || !precioValido(precio) || unidades < 1 || unidades > genero.getAmount()) {
            jugador.sendMessage("§cYa no queda tanto.");
            abrirCompra(jugador, puesto);
            return;
        }
        int maximo = Cuentas.tope(genero.getAmount(), precio);
        if (unidades > maximo) {
            jugador.sendMessage("§cEl tope de una compra son " + Cuentas.COMPRA_MAX + " duros.");
            return;
        }
        int total = unidades * precio;
        if (!plugin.cobrar(jugador, total)) {
            jugador.sendMessage("§cTe faltan duros. Son " + total + ". Tienes " + plugin.duros(jugador) + ".");
            return;
        }
        ItemStack compra = genero.clone();
        compra.setAmount(unidades);
        Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(compra);
        if (!sobra.isEmpty()) {
            plugin.devolver(jugador, total);
            jugador.sendMessage("§cNo cabe. No te he cobrado.");
            return;
        }
        ItemStack anterior = genero.clone();
        int precioAnterior = precio;
        if (genero.getAmount() == unidades) {
            puesto.huecos[hueco] = null;
            puesto.precios[hueco] = 0;
        } else {
            genero.setAmount(genero.getAmount() - unidades);
        }
        int tasa = Cuentas.impuesto(total, plugin.porcentajeImpuesto());
        int paraDueno = total - tasa;
        if (!plugin.banco.ingresar(Bukkit.getOfflinePlayer(puesto.dueno), paraDueno)) {
            puesto.huecos[hueco] = anterior;
            puesto.precios[hueco] = precioAnterior;
            plugin.devolver(jugador, total);
            jugador.getInventory().removeItem(compra);
            guardar();
            jugador.sendMessage("§cNo se ha podido pagar. Te he devuelto los duros y el género.");
            return;
        }
        plugin.banco.sumarCaja(tasa);
        guardar();
        Player dueno = Bukkit.getPlayer(puesto.dueno);
        if (dueno != null) {
            dueno.sendMessage("§a" + jugador.getName() + " te compra " + unidades + ". §f+" + paraDueno + " duros.");
        }
        jugador.sendMessage("§aListo. §f−" + total + " duros. El barrio se queda " + tasa + ". Te quedan " + plugin.duros(jugador) + ".");
        jugador.closeInventory();
    }

    private void meter(Player jugador, Puesto puesto, int hueco, ItemStack origen, boolean cursor, int slotJugador) {
        meter(jugador, puesto, hueco, origen, cursor, slotJugador, origen.getAmount(), false);
    }

    private void meter(Player jugador, Puesto puesto, int hueco, ItemStack origen, boolean cursor, int slotJugador, int maximo, boolean soloEse) {
        if (prohibido(origen)) {
            jugador.sendMessage("§cEso no se vende.");
            return;
        }
        if (maximo <= 0) {
            return;
        }
        int destino = huecoLibre(puesto, hueco, origen, soloEse);
        if (destino < 0) {
            jugador.sendMessage("§cNo cabe.");
            return;
        }
        ItemStack alli = puesto.huecos[destino];
        int cabe = origen.getMaxStackSize() - (alli == null || alli.getType().isAir() ? 0 : alli.getAmount());
        int mover = Math.min(cabe, Math.min(maximo, origen.getAmount()));
        if (mover <= 0) {
            jugador.sendMessage("§cNo cabe.");
            return;
        }
        if (alli == null || alli.getType().isAir()) {
            ItemStack nuevo = origen.clone();
            nuevo.setAmount(mover);
            puesto.huecos[destino] = nuevo;
            puesto.precios[destino] = 0;
        } else {
            alli.setAmount(alli.getAmount() + mover);
        }
        guardar();
        int queda = origen.getAmount() - mover;
        final ItemStack resto;
        if (queda > 0) {
            ItemStack quedaEnMano = origen.clone();
            quedaEnMano.setAmount(queda);
            resto = quedaEnMano;
        } else {
            resto = null;
        }
        Bukkit.getScheduler().runTask(plugin, () -> aplicarMano(jugador, puesto, cursor, slotJugador, resto));
    }

    private void aplicarMano(Player jugador, Puesto puesto, boolean cursor, int slotJugador, ItemStack resto) {
        if (!jugador.isOnline()) {
            return;
        }
        if (cursor) {
            jugador.setItemOnCursor(resto);
        } else if (slotJugador >= 0) {
            jugador.getInventory().setItem(slotJugador, resto);
        }
        repintar(jugador, puesto);
    }

    private void repintar(Player jugador, Puesto puesto) {
        if (jugador.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu
            && puesto.id.equals(menu.id)
            && menu.tipo == Menu.Tipo.DUENO) {
            pintarDueno(jugador.getOpenInventory().getTopInventory(), puesto, jugador);
        }
    }

    private int huecoLibre(Puesto puesto, int preferido, ItemStack copia, boolean soloEse) {
        if (preferido >= 0 && preferido < HUECOS) {
            if (cabeEn(puesto, preferido, copia)) {
                return preferido;
            }
            if (soloEse) {
                return -1;
            }
        } else if (soloEse) {
            return -1;
        }
        for (int i = 0; i < HUECOS; i++) {
            ItemStack alli = puesto.huecos[i];
            if (alli != null && !alli.getType().isAir() && alli.isSimilar(copia) && alli.getAmount() < alli.getMaxStackSize()) {
                return i;
            }
        }
        for (int i = 0; i < HUECOS; i++) {
            if (cabeEn(puesto, i, copia)) {
                return i;
            }
        }
        return -1;
    }

    private void intercambiarCursor(Player jugador, Puesto puesto, int hueco, ItemStack entrante) {
        ItemStack saliente = puesto.huecos[hueco].clone();
        int precio = puesto.precios[hueco];
        puesto.huecos[hueco] = entrante.clone();
        puesto.precios[hueco] = 0;
        guardar();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!jugador.isOnline()) {
                puesto.huecos[hueco] = saliente;
                puesto.precios[hueco] = precio;
                guardar();
                return;
            }
            jugador.setItemOnCursor(saliente);
            repintar(jugador, puesto);
        });
    }

    private void cambiarConBarra(Player jugador, Puesto puesto, int hueco, int boton) {
        if (boton < 0 || boton > 8) {
            return;
        }
        ItemStack barra = jugador.getInventory().getItem(boton);
        if (barra != null && prohibido(barra)) {
            jugador.sendMessage("§cEso no se vende.");
            return;
        }
        ItemStack venta = puesto.huecos[hueco];
        boolean barraVacia = barra == null || barra.getType().isAir();
        boolean ventaVacia = venta == null || venta.getType().isAir();
        if (barraVacia && ventaVacia) {
            return;
        }
        if (barraVacia) {
            ItemStack copia = venta.clone();
            puesto.huecos[hueco] = null;
            puesto.precios[hueco] = 0;
            guardar();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!jugador.isOnline()) {
                    puesto.huecos[hueco] = copia;
                    guardar();
                    return;
                }
                jugador.getInventory().setItem(boton, copia);
                repintar(jugador, puesto);
            });
            return;
        }
        if (ventaVacia || venta.isSimilar(barra)) {
            meter(jugador, puesto, hueco, barra.clone(), false, boton, barra.getAmount(), true);
            return;
        }
        ItemStack saliente = venta.clone();
        int precio = puesto.precios[hueco];
        puesto.huecos[hueco] = barra.clone();
        puesto.precios[hueco] = 0;
        guardar();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!jugador.isOnline()) {
                puesto.huecos[hueco] = saliente;
                puesto.precios[hueco] = precio;
                guardar();
                return;
            }
            jugador.getInventory().setItem(boton, saliente);
            repintar(jugador, puesto);
        });
    }

    private void aplicarArrastre(Player jugador, Puesto puesto, List<Integer> orden, ItemStack muestra, boolean deUno) {
        if (!jugador.isOnline()) {
            return;
        }
        if (!(jugador.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu)
            || !puesto.id.equals(menu.id)
            || menu.tipo != Menu.Tipo.DUENO) {
            return;
        }
        ItemStack mano = jugador.getItemOnCursor();
        if (mano == null || mano.getType().isAir() || !mano.isSimilar(muestra)) {
            return;
        }
        List<Integer> validos = new ArrayList<>();
        int[] espacio = new int[orden.size()];
        int n = 0;
        for (int raw : orden) {
            int cabe = espacioEn(puesto, raw, mano);
            if (cabe <= 0) {
                continue;
            }
            validos.add(raw);
            espacio[n++] = cabe;
        }
        if (n == 0) {
            return;
        }
        int[] recorte = new int[n];
        System.arraycopy(espacio, 0, recorte, 0, n);
        int[] partes = Reparto.partes(mano.getAmount(), recorte, deUno);
        int usado = 0;
        for (int i = 0; i < n; i++) {
            int poner = Math.min(partes[i], espacioEn(puesto, validos.get(i), mano));
            if (poner <= 0) {
                continue;
            }
            sumar(puesto, validos.get(i), mano, poner);
            usado += poner;
        }
        if (usado <= 0) {
            return;
        }
        int queda = mano.getAmount() - usado;
        if (queda <= 0) {
            jugador.setItemOnCursor(null);
        } else {
            mano.setAmount(queda);
            jugador.setItemOnCursor(mano);
        }
        guardar();
        repintar(jugador, puesto);
    }

    private int espacioEn(Puesto puesto, int hueco, ItemStack muestra) {
        ItemStack alli = puesto.huecos[hueco];
        if (alli == null || alli.getType().isAir()) {
            return muestra.getMaxStackSize();
        }
        if (!alli.isSimilar(muestra)) {
            return 0;
        }
        return Math.max(0, alli.getMaxStackSize() - alli.getAmount());
    }

    private void sumar(Puesto puesto, int hueco, ItemStack muestra, int cantidad) {
        ItemStack alli = puesto.huecos[hueco];
        if (alli == null || alli.getType().isAir()) {
            ItemStack nuevo = muestra.clone();
            nuevo.setAmount(cantidad);
            puesto.huecos[hueco] = nuevo;
            puesto.precios[hueco] = 0;
            return;
        }
        alli.setAmount(alli.getAmount() + cantidad);
    }

    private boolean cabeEn(Puesto puesto, int hueco, ItemStack copia) {
        ItemStack alli = puesto.huecos[hueco];
        if (alli == null || alli.getType().isAir()) {
            return true;
        }
        return alli.isSimilar(copia) && alli.getAmount() < alli.getMaxStackSize();
    }

    private void sacar(Player jugador, Puesto puesto, int hueco) {
        ItemStack genero = puesto.huecos[hueco];
        if (genero == null) {
            return;
        }
        ItemStack copia = genero.clone();
        if (!cabeEnInventario(jugador, copia)) {
            jugador.sendMessage("§cNo te cabe.");
            return;
        }
        puesto.huecos[hueco] = null;
        puesto.precios[hueco] = 0;
        guardar();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!jugador.isOnline()) {
                puesto.huecos[hueco] = copia;
                guardar();
                return;
            }
            Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(copia);
            if (!sobra.isEmpty()) {
                puesto.huecos[hueco] = copia;
                guardar();
                jugador.sendMessage("§cNo te cabe.");
            }
            if (jugador.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu
                && puesto.id.equals(menu.id)
                && menu.tipo == Menu.Tipo.DUENO) {
                pintarDueno(jugador.getOpenInventory().getTopInventory(), puesto, jugador);
            }
        });
    }

    private boolean cabeEnInventario(Player jugador, ItemStack copia) {
        int queda = copia.getAmount();
        for (ItemStack hueco : jugador.getInventory().getStorageContents()) {
            if (hueco == null || hueco.getType().isAir()) {
                return true;
            }
            if (hueco.isSimilar(copia)) {
                queda -= hueco.getMaxStackSize() - hueco.getAmount();
                if (queda <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void pedirPrecio(Player jugador, Puesto puesto, int hueco) {
        if (puesto.huecos[hueco] == null) {
            return;
        }
        precioPedido.put(jugador.getUniqueId(), new PrecioPedido(puesto.id, hueco));
        saltar.add(jugador.getUniqueId());
        jugador.closeInventory();
        saltar.remove(jugador.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline()) {
                mostrarPrecio(jugador, null, null);
            }
        });
    }

    private void mostrarPrecio(Player jugador, String aviso, String previo) {
        PrecioPedido pedido = precioPedido.get(jugador.getUniqueId());
        Puesto puesto = pedido == null ? null : puestos.get(pedido.id);
        if (pedido == null || puesto == null || pedido.hueco < 0 || pedido.hueco >= HUECOS || puesto.huecos[pedido.hueco] == null) {
            precioPedido.remove(jugador.getUniqueId());
            jugador.sendMessage("§cEse género ya no está.");
            if (puesto != null) {
                abrirDueno(jugador, puesto);
            } else {
                soltar(jugador.getUniqueId());
            }
            return;
        }
        int actual = puesto.precios[pedido.hueco];
        String inicial = previo != null ? previo.trim() : (actual >= Cuentas.PRECIO_MIN ? Integer.toString(actual) : "");
        if (inicial.length() > 3) {
            inicial = inicial.substring(0, 3);
        }
        String cuerpo = aviso == null
            ? "Escribe los duros de cada uno, de " + Cuentas.PRECIO_MIN + " a " + Cuentas.PRECIO_MAX + "."
            : aviso;
        var campo = DialogInput.text(CAMPO_PRECIO, Component.text("Duros")).maxLength(3).width(220);
        if (!inicial.isEmpty()) {
            campo.initial(inicial);
        }
        DialogInput entrada = campo.build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Precio", TEXTO))
                .canCloseWithEscape(false)
                .pause(false)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, SUAVE), 280)))
                .inputs(List.of(entrada))
                .build())
            .type(DialogType.confirmation(botonPoner(), botonVolverPrecio())));
        ((Audience) jugador).showDialog(dialog);
    }

    private ActionButton botonPoner() {
        return ActionButton.builder(Component.text("Poner", ORO))
            .tooltip(Component.text("Ese precio se cobra por cada uno."))
            .width(150)
            .action(DialogAction.customClick(this::tomarPrecio, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    private ActionButton botonVolverPrecio() {
        return ActionButton.builder(Component.text("Volver", SUAVE))
            .tooltip(Component.text("Vuelves al cofre sin cambiar el precio."))
            .width(120)
            .action(DialogAction.customClick((vista, audiencia) -> ejecutar(audiencia, () -> volverDelPrecio((Player) audiencia)),
                ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    private void tomarPrecio(DialogResponseView vista, Audience audiencia) {
        String escrito = textoPrecio(vista);
        ejecutar(audiencia, () -> aplicarPrecio((Player) audiencia, escrito));
    }

    private void aplicarPrecio(Player jugador, String escrito) {
        PrecioPedido pedido = precioPedido.get(jugador.getUniqueId());
        Puesto puesto = pedido == null ? null : puestos.get(pedido.id);
        if (pedido == null || puesto == null || !puesto.dueno.equals(jugador.getUniqueId())
            || pedido.hueco < 0 || pedido.hueco >= HUECOS || puesto.huecos[pedido.hueco] == null) {
            precioPedido.remove(jugador.getUniqueId());
            jugador.sendMessage("§cEse género ya no está.");
            if (puesto != null && puesto.dueno.equals(jugador.getUniqueId())) {
                abrirDueno(jugador, puesto);
            } else {
                soltar(jugador.getUniqueId());
            }
            return;
        }
        Integer precio = Cuentas.leerPrecio(escrito);
        if (precio == null) {
            mostrarPrecio(jugador, "Tiene que ser un número entero, de " + Cuentas.PRECIO_MIN + " a " + Cuentas.PRECIO_MAX + ".", escrito);
            return;
        }
        puesto.precios[pedido.hueco] = precio;
        guardar();
        precioPedido.remove(jugador.getUniqueId());
        jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.4f);
        jugador.sendMessage("§a" + precio + " duros cada uno.");
        abrirDueno(jugador, puesto);
    }

    private void volverDelPrecio(Player jugador) {
        PrecioPedido pedido = precioPedido.remove(jugador.getUniqueId());
        if (pedido == null) {
            soltar(jugador.getUniqueId());
            return;
        }
        Puesto puesto = puestos.get(pedido.id);
        if (puesto == null || !puesto.dueno.equals(jugador.getUniqueId())) {
            soltar(jugador.getUniqueId());
            return;
        }
        abrirDueno(jugador, puesto);
    }

    private void ejecutar(Audience audiencia, Runnable accion) {
        if (!(audiencia instanceof Player jugador)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline()) {
                accion.run();
            }
        });
    }

    private static String textoPrecio(DialogResponseView vista) {
        if (vista == null) {
            return "";
        }
        try {
            String valor = vista.getText(CAMPO_PRECIO);
            return valor == null ? "" : valor;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void soltar(UUID jugador) {
        String id = abierto.remove(jugador);
        if (id != null) {
            mirando.remove(id, jugador);
        }
    }

    private void soltarOtro(Player jugador, String id) {
        String previo = abierto.get(jugador.getUniqueId());
        if (previo != null && !previo.equals(id)) {
            mirando.remove(previo, jugador.getUniqueId());
        }
    }

    private boolean toca(List<Block> bloques) {
        for (Block bloque : bloques) {
            if (esPuesto(bloque)) {
                return true;
            }
        }
        return false;
    }

    private boolean tieneGenero(Puesto puesto) {
        for (ItemStack genero : puesto.huecos) {
            if (genero != null && !genero.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    private void recoger(Puesto puesto, boolean avisar) {
        if (puesto == null) {
            return;
        }
        List<ItemStack> bolsa = recuperar.computeIfAbsent(puesto.dueno, id -> new ArrayList<>());
        for (ItemStack genero : puesto.huecos) {
            if (genero != null && !genero.getType().isAir()) {
                bolsa.add(genero.clone());
            }
        }
        quitarBloque(puesto);
        puestos.remove(puesto.id);
        guardar();
        Player dueno = Bukkit.getPlayer(puesto.dueno);
        if (dueno != null) {
            entregarPendiente(dueno, avisar);
        }
    }

    private void entregarPendiente(Player jugador, boolean siempre) {
        List<ItemStack> bolsa = recuperar.get(jugador.getUniqueId());
        if (bolsa == null || bolsa.isEmpty()) {
            if (siempre) {
                jugador.sendMessage("§eNo tienes género pendiente.");
            }
            return;
        }
        Iterator<ItemStack> iter = bolsa.iterator();
        boolean dado = false;
        while (iter.hasNext()) {
            ItemStack genero = iter.next();
            Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(genero);
            if (sobra.isEmpty()) {
                iter.remove();
                dado = true;
            } else {
                break;
            }
        }
        if (bolsa.isEmpty()) {
            recuperar.remove(jugador.getUniqueId());
        }
        guardar();
        if (dado && bolsa.isEmpty()) {
            jugador.sendMessage("§aTe devuelvo el género de la venta.");
        } else if (!bolsa.isEmpty()) {
            jugador.sendMessage("§eTe queda género de una venta. Haz sitio y escribe §f/tienda recuperar");
        }
    }

    private String idDe(Block bloque) {
        if (bloque == null || bloque.getType() != Material.BARREL) {
            return null;
        }
        if (!(bloque.getState() instanceof TileState tile)) {
            return null;
        }
        return tile.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    private void quitarBloque(Puesto puesto) {
        World mundo = Bukkit.getWorld(puesto.mundo);
        if (mundo == null) {
            return;
        }
        Block bloque = mundo.getBlockAt(puesto.x, puesto.y, puesto.z);
        Location centro = bloque.getLocation().add(0.5, 0.5, 0.5);
        if (idDe(bloque) != null) {
            bloque.setType(Material.AIR, false);
            mundo.playEffect(centro, org.bukkit.Effect.STEP_SOUND, Material.OAK_PLANKS);
        }
        for (Entity entidad : mundo.getNearbyEntities(centro, 1.4, 2.4, 1.4)) {
            if (entidad.getScoreboardTags().contains(MARCA)) {
                entidad.remove();
            }
        }
    }

    private void asegurarLetrero(Puesto puesto) {
        World mundo = Bukkit.getWorld(puesto.mundo);
        if (mundo == null || !mundo.isChunkLoaded(puesto.x >> 4, puesto.z >> 4)) {
            return;
        }
        Block bloque = mundo.getBlockAt(puesto.x, puesto.y, puesto.z);
        if (bloque.getType() != Material.BARREL) {
            recoger(puesto, true);
            return;
        }
        if (bloque.getState() instanceof TileState tile
            && !puesto.id.equals(tile.getPersistentDataContainer().get(idKey, PersistentDataType.STRING))) {
            tile.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, puesto.id);
            tile.update(true, false);
        }
        Location centro = bloque.getLocation().add(0.5, 0.5, 0.5);
        ItemDisplay figura = null;
        TextDisplay rotulo = null;
        for (Entity entidad : mundo.getNearbyEntities(centro, 1.2, 2.2, 1.2)) {
            if (!entidad.getScoreboardTags().contains(MARCA)) {
                continue;
            }
            if (entidad instanceof ItemDisplay display) {
                if (figura == null) {
                    figura = display;
                } else {
                    display.remove();
                }
            } else if (entidad instanceof TextDisplay display) {
                if (rotulo == null) {
                    rotulo = display;
                } else {
                    display.remove();
                }
            }
        }
        if (figura == null) {
            float angulo = angulo(bloque);
            mundo.spawn(centro, ItemDisplay.class, display -> {
                display.setItemStack(item());
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                display.setBillboard(Display.Billboard.FIXED);
                display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(angulo, 0f, 1f, 0f),
                    new Vector3f(1.14f, 1.14f, 1.14f),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
                ));
                display.addScoreboardTag(MARCA);
                display.setPersistent(true);
            });
        }
        if (rotulo == null) {
            mundo.spawn(centro.clone().add(0, 0.92, 0), TextDisplay.class, display -> {
                display.addScoreboardTag(MARCA);
                display.setPersistent(true);
                pintarRotulo(display, puesto);
            });
        } else {
            pintarRotulo(rotulo, puesto);
        }
    }

    private void pintarRotulo(TextDisplay display, Puesto puesto) {
        display.text(rotulo(puesto));
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setSeeThrough(false);
        display.setShadowed(false);
        display.setLineWidth(140);
        display.setBackgroundColor(FONDO);
    }

    private Component rotulo(Puesto puesto) {
        int venta = 0;
        int falta = 0;
        for (int i = 0; i < HUECOS; i++) {
            ItemStack genero = puesto.huecos[i];
            if (genero == null || genero.getType().isAir()) {
                continue;
            }
            if (precioValido(puesto.precios[i])) {
                venta++;
            } else {
                falta++;
            }
        }
        Component texto = Component.text(puesto.nombre, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            .append(Component.newline())
            .append(Component.text("Venta", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
        if (venta > 0) {
            texto = texto.append(Component.newline())
                .append(Component.text(venta + " a la venta", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        } else if (falta > 0) {
            texto = texto.append(Component.newline())
                .append(Component.text("Falta el precio", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        return texto;
    }

    private void refrescarRotulos() {
        for (Puesto puesto : puestos.values()) {
            World mundo = Bukkit.getWorld(puesto.mundo);
            if (mundo == null || !mundo.isChunkLoaded(puesto.x >> 4, puesto.z >> 4)) {
                continue;
            }
            Location centro = new Location(mundo, puesto.x + 0.5, puesto.y + 0.5, puesto.z + 0.5);
            for (Entity entidad : mundo.getNearbyEntities(centro, 1.2, 2.2, 1.2)) {
                if (entidad instanceof TextDisplay display && display.getScoreboardTags().contains(MARCA)) {
                    pintarRotulo(display, puesto);
                    break;
                }
            }
        }
    }

    private float angulo(Block bloque) {
        if (!(bloque.getBlockData() instanceof Directional directional)) {
            return 0f;
        }
        return switch (directional.getFacing()) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (Math.PI / 2);
            case EAST -> (float) (-Math.PI / 2);
            default -> 0f;
        };
    }

    private void repasar() {
        List<Puesto> copia = new ArrayList<>(puestos.values());
        for (Puesto puesto : copia) {
            asegurarLetrero(puesto);
        }
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection seccion = yaml.getConfigurationSection("puestos");
        if (seccion != null) {
            for (String id : seccion.getKeys(false)) {
                Puesto puesto = new Puesto(id);
                puesto.dueno = UUID.fromString(seccion.getString(id + ".dueno"));
                puesto.nombre = seccion.getString(id + ".nombre", "?");
                puesto.parcela = seccion.getString(id + ".parcela", "");
                puesto.mundo = seccion.getString(id + ".mundo", "world");
                puesto.x = seccion.getInt(id + ".x");
                puesto.y = seccion.getInt(id + ".y");
                puesto.z = seccion.getInt(id + ".z");
                ConfigurationSection huecos = seccion.getConfigurationSection(id + ".huecos");
                if (huecos != null) {
                    for (String indice : huecos.getKeys(false)) {
                        int i = Integer.parseInt(indice);
                        if (i < 0 || i >= HUECOS) {
                            continue;
                        }
                        puesto.huecos[i] = huecos.getItemStack(indice + ".item");
                        puesto.precios[i] = huecos.getInt(indice + ".precio");
                    }
                }
                puestos.put(id, puesto);
            }
        }
        ConfigurationSection pendientes = yaml.getConfigurationSection("recuperar");
        if (pendientes != null) {
            for (String id : pendientes.getKeys(false)) {
                List<ItemStack> bolsa = new ArrayList<>();
                List<?> lista = pendientes.getList(id);
                if (lista != null) {
                    for (Object objeto : lista) {
                        if (objeto instanceof ItemStack item) {
                            bolsa.add(item);
                        }
                    }
                }
                if (!bolsa.isEmpty()) {
                    recuperar.put(UUID.fromString(id), bolsa);
                }
            }
        }
    }

    void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Puesto puesto : puestos.values()) {
            String id = puesto.id;
            yaml.set("puestos." + id + ".dueno", puesto.dueno.toString());
            yaml.set("puestos." + id + ".nombre", puesto.nombre);
            yaml.set("puestos." + id + ".parcela", puesto.parcela);
            yaml.set("puestos." + id + ".mundo", puesto.mundo);
            yaml.set("puestos." + id + ".x", puesto.x);
            yaml.set("puestos." + id + ".y", puesto.y);
            yaml.set("puestos." + id + ".z", puesto.z);
            for (int i = 0; i < HUECOS; i++) {
                if (puesto.huecos[i] == null || puesto.huecos[i].getType().isAir()) {
                    continue;
                }
                yaml.set("puestos." + id + ".huecos." + i + ".item", puesto.huecos[i]);
                if (puesto.precios[i] >= Cuentas.PRECIO_MIN) {
                    yaml.set("puestos." + id + ".huecos." + i + ".precio", puesto.precios[i]);
                }
            }
        }
        for (Map.Entry<UUID, List<ItemStack>> entrada : recuperar.entrySet()) {
            yaml.set("recuperar." + entrada.getKey(), entrada.getValue());
        }
        try {
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude guardar las ventas: " + e.getMessage());
        }
        refrescarRotulos();
    }

    private static final class PrecioPedido {
        final String id;
        final int hueco;

        PrecioPedido(String id, int hueco) {
            this.id = id;
            this.hueco = hueco;
        }
    }

    private static final class Puesto {
        final String id;
        UUID dueno;
        String nombre;
        String parcela;
        String mundo;
        int x;
        int y;
        int z;
        final ItemStack[] huecos = new ItemStack[HUECOS];
        final int[] precios = new int[HUECOS];

        Puesto(String id) {
            this.id = id;
        }
    }

    private static final class Menu implements InventoryHolder {
        enum Tipo { DUENO, COMPRA, CANTIDAD }

        final Tipo tipo;
        final String id;
        Inventory inventario;

        Menu(Tipo tipo, String id) {
            this.tipo = tipo;
            this.id = id;
        }

        @Override
        public Inventory getInventory() {
            return inventario;
        }
    }
}
