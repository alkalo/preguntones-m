package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

final class Tienda implements Listener {
    private static final String MENU = "Tienda";
    private static final String PANEL = "preguntones-estadisticas";
    private static final String ROTULO = "preguntones-tienda";

    private final ParcelasPlugin plugin;
    private final NamespacedKey precioKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey cantidadKey;
    private final NamespacedKey categoriaKey;
    private final NamespacedKey paginaKey;
    private final NamespacedKey banderaKey;
    private final NamespacedKey fichaKey;
    private final List<Articulo> catalogo = new ArrayList<>();

    Tienda(ParcelasPlugin plugin) {
        this.plugin = plugin;
        precioKey = new NamespacedKey(plugin, "precio");
        materialKey = new NamespacedKey(plugin, "material");
        cantidadKey = new NamespacedKey(plugin, "cantidad");
        categoriaKey = new NamespacedKey(plugin, "categoria");
        paginaKey = new NamespacedKey(plugin, "pagina");
        banderaKey = new NamespacedKey(plugin, "bandera");
        fichaKey = new NamespacedKey(plugin, "ficha");
        catalogo();
    }

    void arrancar() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::refrescar, 80L, 100L);
        Bukkit.getScheduler().runTaskLater(plugin, this::quitarDelSpawn, 40L);
        plugin.getLogger().info(resumen());
    }

    void quitar() {
    }

    void abrir(Player jugador) {
        Inventory menu = Bukkit.createInventory(null, 54, MENU);
        int[] huecos = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
        List<Catalogo.Seccion> secciones = secciones();
        for (int i = 0; i < secciones.size() && i < huecos.length; i++) {
            menu.setItem(huecos[i], seccion(secciones.get(i)));
        }
        menu.setItem(31, botonAldeano());
        menu.setItem(49, portada());
        menu.setItem(51, saldo(jugador));
        jugador.openInventory(menu);
    }

    boolean comprarBandera(Player jugador) {
        int precio = 40;
        if (!plugin.cobrar(jugador, precio)) {
            jugador.sendMessage("§cTe faltan duros. Cuesta " + precio + ". Tienes " + plugin.duros(jugador) + ".");
            return true;
        }
        if (!jugador.getInventory().addItem(itemBandera()).isEmpty()) {
            plugin.devolver(jugador, precio);
            jugador.sendMessage("§cNo cabe. Te devuelvo los duros.");
            return true;
        }
        jugador.sendMessage("§aBandera. §f−" + precio + " duros. Ponla donde quieras. Te quedan " + plugin.duros(jugador) + ".");
        return true;
    }

    @EventHandler
    public void alPulsar(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador) || !esTienda(event.getView().getTitle())) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getBottomInventory() || event.getRawSlot() == -999) {
            if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        ClickType clic = event.getClick();
        if (clic != ClickType.LEFT && clic != ClickType.RIGHT && clic != ClickType.SHIFT_LEFT && clic != ClickType.SHIFT_RIGHT) {
            return;
        }
        ItemStack actual = event.getCurrentItem();
        if (actual == null || !actual.hasItemMeta()) {
            return;
        }
        var datos = actual.getItemMeta().getPersistentDataContainer();
        String categoria = datos.get(categoriaKey, PersistentDataType.STRING);
        Integer pagina = datos.get(paginaKey, PersistentDataType.INTEGER);
        Byte ficha = datos.get(fichaKey, PersistentDataType.BYTE);
        if (ficha != null && ficha == (byte) 1) {
            comprarFicha(jugador);
            event.getInventory().setItem(51, saldo(jugador));
            return;
        }
        if ("volver".equals(categoria)) {
            abrir(jugador);
            return;
        }
        if (categoria != null) {
            abrirSeccion(jugador, categoria, pagina == null ? 0 : pagina);
            return;
        }
        Integer precio = datos.get(precioKey, PersistentDataType.INTEGER);
        String material = datos.get(materialKey, PersistentDataType.STRING);
        Integer cantidad = datos.get(cantidadKey, PersistentDataType.INTEGER);
        if (precio == null || material == null || cantidad == null) {
            return;
        }
        Material tipo;
        try {
            tipo = Material.valueOf(material);
        } catch (IllegalArgumentException e) {
            return;
        }
        Byte bandera = datos.get(banderaKey, PersistentDataType.BYTE);
        if (!plugin.cobrar(jugador, precio)) {
            jugador.sendMessage("§cTe faltan duros. Cuesta " + precio + ". Tienes " + plugin.duros(jugador) + ".");
            return;
        }
        ItemStack compra = bandera != null && bandera == (byte) 1 ? itemBandera() : new ItemStack(tipo, cantidad);
        if (!jugador.getInventory().addItem(compra).isEmpty()) {
            plugin.devolver(jugador, precio);
            jugador.sendMessage("§cNo cabe. Te devuelvo los duros.");
            return;
        }
        jugador.sendMessage("§aListo. §f−" + precio + " duros. Te quedan " + plugin.duros(jugador) + ".");
        event.getInventory().setItem(51, saldo(jugador));
    }

    @EventHandler
    public void alArrastrar(InventoryDragEvent event) {
        if (!esTienda(event.getView().getTitle())) {
            return;
        }
        int arriba = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < arriba) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void abrirSeccion(Player jugador, String categoria, int pagina) {
        List<Articulo> lista = new ArrayList<>();
        for (Articulo articulo : catalogo) {
            if (articulo.categoria.equals(categoria)) {
                lista.add(articulo);
            }
        }
        if (lista.isEmpty()) {
            abrir(jugador);
            return;
        }
        int porPagina = 45;
        int paginas = Math.max(1, (lista.size() + porPagina - 1) / porPagina);
        int actual = Math.max(0, Math.min(pagina, paginas - 1));
        Inventory menu = Bukkit.createInventory(null, 54, MENU + " · " + categoria);
        int desde = actual * porPagina;
        for (int i = 0; i < porPagina && desde + i < lista.size(); i++) {
            menu.setItem(i, articulo(lista.get(desde + i)));
        }
        if (actual > 0) {
            menu.setItem(45, flecha(categoria, actual - 1, "Anterior"));
            menu.setItem(47, volver());
        } else {
            menu.setItem(45, volver());
        }
        menu.setItem(49, marca(actual, paginas));
        menu.setItem(51, saldo(jugador));
        if (actual + 1 < paginas) {
            menu.setItem(53, flecha(categoria, actual + 1, "Siguiente"));
        }
        jugador.openInventory(menu);
    }

    private void quitarDelSpawn() {
        File archivo = new File(plugin.getDataFolder(), "tienda.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        if (yaml.getBoolean("fuera")) {
            return;
        }
        World mundo = Bukkit.getWorld("world");
        if (mundo == null) {
            Bukkit.getScheduler().runTaskLater(plugin, this::quitarDelSpawn, 40L);
            return;
        }
        for (int x = 13; x <= 19; x++) {
            for (int z = 5; z <= 11; z++) {
                for (int y = 68; y >= 65; y--) {
                    Block bloque = mundo.getBlockAt(x, y, z);
                    if (x == 17 && y == 66 && z == 8 && bloque.getType() == Material.STONE_BUTTON) {
                        continue;
                    }
                    if (!bloque.getType().isAir()) {
                        bloque.setType(Material.AIR, false);
                    }
                }
                Block suelo = mundo.getBlockAt(x, 64, z);
                if (suelo.getType() != Material.DIRT_PATH) {
                    suelo.setType(Material.DIRT_PATH, false);
                }
            }
        }
        for (TextDisplay display : mundo.getEntitiesByClass(TextDisplay.class)) {
            if (display.getScoreboardTags().contains(ROTULO) || esLetrero(display)) {
                display.remove();
            }
        }
        yaml.set("fuera", true);
        try {
            File carpeta = archivo.getParentFile();
            if (carpeta != null && !carpeta.exists()) {
                carpeta.mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude anotar que la tienda ya no está en el spawn: " + e.getMessage());
        }
        plugin.getLogger().info("He quitado la tienda del spawn.");
    }

    private boolean esLetrero(TextDisplay display) {
        if (display.getScoreboardTags().contains(PANEL)) {
            return false;
        }
        Location sitio = display.getLocation();
        int x = sitio.getBlockX();
        int y = sitio.getBlockY();
        int z = sitio.getBlockZ();
        return x >= 13 && x <= 19 && z >= 5 && z <= 11 && y >= 64 && y <= 72;
    }

    private void refrescar() {
        World mundo = Bukkit.getWorld("world");
        if (mundo == null) {
            return;
        }
        for (TextDisplay display : mundo.getEntitiesByClass(TextDisplay.class)) {
            if (display.getScoreboardTags().contains(PANEL)) {
                display.text(textoPanel());
            }
        }
    }

    private Component textoPanel() {
        int conectados = Bukkit.getOnlinePlayers().size();
        int parcelas = plugin.ocupadas();
        String dragon = leer("Mundos", "ultimoDragon", false);
        String xo = leer("Xo", "ultimoGanador", true);
        return Component.text("El barrio", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)
                .append(Component.newline())
                .append(linea(conectados + (conectados == 1 ? " conectado" : " conectados"), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(linea(parcelas + (parcelas == 1 ? " parcela con dueño" : " parcelas con dueño"), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(linea("Dragón: " + (dragon.isEmpty() ? "nadie" : dragon), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(linea("Última XO: " + (xo.isEmpty() ? "nadie" : xo), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(linea("XO 21:30–22:30", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(linea("Tienda en el panel", NamedTextColor.AQUA));
    }

    private Component linea(String texto, NamedTextColor color) {
        return Component.text(texto, color).decoration(TextDecoration.ITALIC, false);
    }

    private String leer(String pluginNombre, String metodo, boolean estatico) {
        Plugin otro = Bukkit.getPluginManager().getPlugin(pluginNombre);
        if (otro == null) {
            return "";
        }
        try {
            Object valor = estatico
                    ? otro.getClass().getMethod(metodo).invoke(null)
                    : otro.getClass().getMethod(metodo).invoke(otro);
            return valor == null ? "" : valor.toString();
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    private ItemStack flecha(String categoria, int pagina, String nombre) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(categoriaKey, PersistentDataType.STRING, categoria);
        meta.getPersistentDataContainer().set(paginaKey, PersistentDataType.INTEGER, pagina);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack marca(int actual, int paginas) {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Página " + (actual + 1) + " de " + paginas, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("El precio va en el nombre.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private boolean esTienda(String titulo) {
        return MENU.equals(titulo) || (titulo != null && titulo.startsWith(MENU + " · "));
    }

    private ItemStack seccion(Catalogo.Seccion seccion) {
        int cosas = contar(seccion.nombre);
        int paginas = Math.max(1, (cosas + 44) / 45);
        int minimo = Integer.MAX_VALUE;
        int maximo = 0;
        for (Articulo articulo : catalogo) {
            if (!articulo.categoria.equals(seccion.nombre)) {
                continue;
            }
            minimo = Math.min(minimo, articulo.precio);
            maximo = Math.max(maximo, articulo.precio);
        }
        ItemStack item = new ItemStack(seccion.icono);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(seccion.nombre, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        String precios = minimo == maximo ? minimo + " duros" : minimo + " a " + maximo + " duros";
        meta.lore(List.of(
                texto(cosas + " cosas · " + paginas + (paginas == 1 ? " página" : " páginas")),
                texto(precios),
                texto("Entra")));
        meta.getPersistentDataContainer().set(categoriaKey, PersistentDataType.STRING, seccion.nombre);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack portada() {
        int paginas = 0;
        for (Catalogo.Seccion seccion : secciones()) {
            paginas += Math.max(1, (contar(seccion.nombre) + 44) / 45);
        }
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("La tienda", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                texto(catalogo.size() + " cosas · " + paginas + " páginas"),
                texto("El precio sigue el crafteo."),
                texto("La bandera sigue a 40."),
                texto("El aldeano, el botón del centro.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack articulo(Articulo articulo) {
        int cantidad = Math.min(articulo.cantidad, articulo.material.getMaxStackSize());
        ItemStack item = new ItemStack(articulo.material, cantidad);
        ItemMeta meta = item.getItemMeta();
        String titulo = cantidad > 1
                ? articulo.nombre + " · " + cantidad + " por " + articulo.precio + " duros"
                : articulo.nombre + " · " + articulo.precio + " duros";
        meta.displayName(Component.text(titulo, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        if (articulo.bandera) {
            meta.lore(List.of(
                texto("Pónla donde quieras."),
                texto("Crea un punto en el menú de Spawn.")
            ));
            meta.getPersistentDataContainer().set(banderaKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            meta.lore(List.of(texto("Para la parcela.")));
        }
        meta.getPersistentDataContainer().set(precioKey, PersistentDataType.INTEGER, articulo.precio);
        meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, articulo.material.name());
        meta.getPersistentDataContainer().set(cantidadKey, PersistentDataType.INTEGER, item.getAmount());
        item.setItemMeta(meta);
        return item;
    }

    private Component texto(String linea) {
        return Component.text(linea, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack volver() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Volver", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(categoriaKey, PersistentDataType.STRING, "volver");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack botonAldeano() {
        ItemStack item = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Aldeano · " + Aldeanos.PRECIO + " duros", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                texto("Una ficha, para un aldeano."),
                texto("Luego, agáchate y púlsalo en el survival."),
                texto("Hasta 8. Se queda en tu parcela.")));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(fichaKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void comprarFicha(Player jugador) {
        Aldeanos aldeanos = plugin.aldeanos();
        if (aldeanos == null) {
            jugador.sendMessage("§eLa ficha no está lista.");
            return;
        }
        if (!aldeanos.puedeComprar(jugador)) {
            jugador.sendMessage("§eYa tienes 8 aldeanos, contando las fichas sin usar.");
            return;
        }
        if (!plugin.cobrar(jugador, Aldeanos.PRECIO)) {
            jugador.sendMessage("§cTe faltan duros. Cuesta " + Aldeanos.PRECIO + ". Tienes " + plugin.duros(jugador) + ".");
            return;
        }
        if (!jugador.getInventory().addItem(aldeanos.fichaVacia()).isEmpty()) {
            plugin.devolver(jugador, Aldeanos.PRECIO);
            jugador.sendMessage("§cNo cabe. Te devuelvo los duros.");
            return;
        }
        jugador.sendMessage("§aFicha de aldeano. §f−" + Aldeanos.PRECIO
                + " duros. Agáchate y pulsa un aldeano del survival. Te quedan " + plugin.duros(jugador) + ".");
    }

    private ItemStack saldo(Player jugador) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Tus duros", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(plugin.duros(jugador) + " duros", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack itemBandera() {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Bandera", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Pónla donde quieras.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Crea un punto en el menú de Spawn.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey("viajes", "bandera"), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void catalogo() {
        for (Catalogo.Fila fila : Catalogo.filas()) {
            add(fila.categoria, fila.material, fila.cantidad, fila.precio, fila.nombre);
        }
        for (Articulo articulo : catalogo) {
            if (!Catalogo.conocida(articulo.categoria)) {
                throw new IllegalStateException("Sección desconocida: " + articulo.categoria);
            }
        }
        for (Catalogo.Seccion seccion : secciones()) {
            if (contar(seccion.nombre) == 0) {
                throw new IllegalStateException("La tienda no tiene " + seccion.nombre);
            }
        }
        if (!Catalogo.saltados().isEmpty()) {
            throw new IllegalStateException("La tienda nombra bloques que no existen: " + Catalogo.saltados());
        }
    }

    private List<Catalogo.Seccion> secciones() {
        return Catalogo.secciones();
    }

    private int contar(String categoria) {
        int total = 0;
        for (Articulo articulo : catalogo) {
            if (articulo.categoria.equals(categoria)) {
                total++;
            }
        }
        return total;
    }

    private String resumen() {
        StringBuilder texto = new StringBuilder("Tienda: " + catalogo.size() + " cosas.");
        for (Catalogo.Seccion seccion : secciones()) {
            int cosas = contar(seccion.nombre);
            int paginas = Math.max(1, (cosas + 44) / 45);
            texto.append(' ').append(seccion.nombre).append(' ').append(cosas);
            texto.append(paginas == 1 ? " (1 página)." : " (" + paginas + " páginas).");
        }
        return texto.toString();
    }

    private void add(String categoria, Material material, int cantidad, int precio, String nombre) {
        if (material == null || !material.isItem() || cantidad < 1 || precio < 1) {
            return;
        }
        for (Articulo ya : catalogo) {
            if (ya.material == material) {
                return;
            }
        }
        int pila = Math.min(cantidad, material.getMaxStackSize());
        catalogo.add(new Articulo(categoria, material, pila, precio, nombre, false));
    }

    private static final class Articulo {
        final String categoria;
        final Material material;
        final int cantidad;
        final int precio;
        final String nombre;
        final boolean bandera;

        Articulo(String categoria, Material material, int cantidad, int precio, String nombre, boolean bandera) {
            this.categoria = categoria;
            this.material = material;
            this.cantidad = cantidad;
            this.precio = precio;
            this.nombre = nombre;
            this.bandera = bandera;
        }
    }

}
