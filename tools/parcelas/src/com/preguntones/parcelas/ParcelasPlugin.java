package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParcelasPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static ParcelasPlugin instancia;
    private Economy economia;
    private Clanes clanes;
    private final Map<String, Parcela> parcelas = new HashMap<>();
    private final Map<UUID, Oferta> ofertas = new HashMap<>();
    private final Map<UUID, Long> avisos = new HashMap<>();
    private int primero;
    private int salto;
    private int lado;
    private int desde;
    private int hasta;
    private int plaza;
    private int precioLibre;
    private int precioMin;
    private int precioMax;
    private int maxParcelas;
    private int impuesto;
    private File archivo;
    private Cuadricula cuadricula;
    private Tienda tienda;
    Venta venta;
    Banco banco;
    private Aldeanos aldeanos;
    private PanelParcelas panel;
    private final Map<UUID, Propuesta> propuestas = new HashMap<>();
    private boolean avisoReserva;

    @Override
    public void onEnable() {
        instancia = this;
        saveDefaultConfig();
        primero = getConfig().getInt("cuadricula.primero", 25);
        salto = getConfig().getInt("cuadricula.salto", 20);
        lado = getConfig().getInt("cuadricula.lado", 16);
        desde = getConfig().getInt("cuadricula.desde", -18);
        hasta = getConfig().getInt("cuadricula.hasta", 17);
        plaza = getConfig().getInt("cuadricula.plaza", 20);
        precioLibre = getConfig().getInt("precios.libre", 25);
        precioMin = getConfig().getInt("precios.minimo", 20);
        precioMax = getConfig().getInt("precios.maximo", 275);
        maxParcelas = getConfig().getInt("precios.max-por-jugador", 6);
        impuesto = getConfig().getInt("precios.impuesto-porcentaje", 10);
        archivo = new File(getDataFolder(), "parcelas.yml");
        cargar();
        RegisteredServiceProvider<Economy> registro = getServer().getServicesManager().getRegistration(Economy.class);
        if (registro == null) {
            getLogger().severe("Vault no tiene economía. Las parcelas no pueden cobrarse.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economia = registro.getProvider();
        clanes = new Clanes(this);
        cuadricula = new Cuadricula(primero, salto, lado, desde, hasta, plaza);
        podarUniones();
        tienda = new Tienda(this);
        banco = new Banco(this);
        venta = new Venta(this);
        panel = new PanelParcelas(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(tienda, this);
        getServer().getPluginManager().registerEvents(banco, this);
        getServer().getPluginManager().registerEvents(venta, this);
        aldeanos = new Aldeanos(this);
        getServer().getPluginManager().registerEvents(aldeanos, this);
        aldeanos.arrancar();
        if (getCommand("parcela") != null) {
            getCommand("parcela").setExecutor(this);
        }
        if (getCommand("clan") != null) {
            getCommand("clan").setExecutor(this);
        }
        if (getCommand("puesto") != null) {
            getCommand("puesto").setExecutor(this);
        }
        if (getCommand("banco") != null) {
            getCommand("banco").setExecutor(this);
        }
        if (getCommand("bandera") != null) {
            getCommand("bandera").setExecutor(this);
        }
        if (getCommand("tienda") != null) {
            getCommand("tienda").setExecutor(this);
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DurosExpansion(this).register();
        }
        tienda.arrancar();
        banco.arrancar();
        venta.arrancar();
        Bukkit.getScheduler().runTaskLater(this, this::pintarOcupadas, 40L);
        getLogger().info("Parcelas listas. La primera cuesta " + precioLibre + " duros. Máximo " + maxParcelas + " por persona.");
    }

    @Override
    public void onDisable() {
        if (tienda != null) {
            tienda.quitar();
        }
        if (venta != null) {
            venta.guardar();
        }
        if (banco != null) {
            banco.guardar();
        }
        if (aldeanos != null) {
            aldeanos.guardar();
        }
        guardar();
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Propuesta entrante = propuestas.remove(id);
        if (entrante != null) {
            Player de = Bukkit.getPlayer(entrante.de);
            if (de != null) {
                de.sendMessage("§e" + event.getPlayer().getName() + " se ha ido. La unión no sigue.");
            }
        }
        List<UUID> quitar = new ArrayList<>();
        for (Map.Entry<UUID, Propuesta> entrada : propuestas.entrySet()) {
            if (id.equals(entrada.getValue().de)) {
                quitar.add(entrada.getKey());
            }
        }
        for (UUID para : quitar) {
            propuestas.remove(para);
            Player otro = Bukkit.getPlayer(para);
            if (otro != null) {
                otro.sendMessage("§e" + event.getPlayer().getName() + " se ha ido. La unión no sigue.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Entra al barrio para usar las parcelas.");
            return true;
        }
        if (!jugador.hasPermission("parcelas.usar") && !jugador.hasPermission("parcelas.admin")) {
            jugador.sendMessage("§cNo puedes usar las parcelas.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("clan")) {
            return clanes.orden(jugador, args);
        }
        if (command.getName().equalsIgnoreCase("puesto")) {
            tienda.abrir(jugador);
            return true;
        }
        if (command.getName().equalsIgnoreCase("banco")) {
            return banco.orden(jugador, args);
        }
        if (command.getName().equalsIgnoreCase("bandera")) {
            return tienda.comprarBandera(jugador);
        }
        if (command.getName().equalsIgnoreCase("tienda")) {
            return venta.orden(jugador, args);
        }
        Indice indice = parcelaEn(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        if (args.length == 0) {
            informar(jugador, indice);
            return true;
        }
        String accion = args[0].toLowerCase();
        if (accion.equals("panel") || accion.equals("menu")) {
            panel.abrir(jugador);
            return true;
        }
        if (accion.equals("cerca")) {
            cerca(jugador);
            return true;
        }
        if (accion.equals("aceptar")) {
            aceptar(jugador);
            return true;
        }
        if (accion.equals("unir") && args.length >= 2 && args[1].equalsIgnoreCase("aceptar")) {
            aceptarUnion(jugador);
            return true;
        }
        if (accion.equals("unir") && args.length >= 2 && (args[1].equalsIgnoreCase("no") || args[1].equalsIgnoreCase("rechazar"))) {
            rechazarUnion(jugador);
            return true;
        }
        if (indice == null) {
            if (accion.equals("banco") || accion.equals("unir") || accion.equals("separar")) {
                jugador.sendMessage("§eEntra en tu parcela, o abre §f/parcela panel§e.");
                return true;
            }
            informar(jugador, null);
            return true;
        }
        switch (accion) {
            case "reclamar" -> reclamar(jugador, indice);
            case "vender" -> vender(jugador, indice, args);
            case "cancelar" -> cancelar(jugador, indice);
            case "comprar" -> comprar(jugador, indice);
            case "abandonar" -> abandonar(jugador, indice);
            case "amigo", "invitar" -> amigo(jugador, indice, args, true);
            case "quitar", "echar" -> amigo(jugador, indice, args, false);
            case "nombre" -> nombrar(jugador, indice, args);
            case "ofrecer" -> ofrecer(jugador, indice, args);
            case "banco" -> panel.confirmarBanco(jugador, indice.clave());
            case "unir" -> unirMandato(jugador, indice, args);
            case "separar" -> separarMandato(jugador, indice);
            default -> informar(jugador, indice);
        }
        return true;
    }

    private void informar(Player jugador, Indice indice) {
        if (indice == null) {
            int x = jugador.getLocation().getBlockX();
            int z = jugador.getLocation().getBlockZ();
            Cuadricula.Zona zona = cuadricula.zona(x, z);
            if (zona == Cuadricula.Zona.SPAWN) {
                if (administra(jugador)) {
                    jugador.sendMessage("§eEsto es el spawn. §fPuedes modificarlo. Los demás no.");
                } else {
                    jugador.sendMessage("§eEsto es el spawn. §fNo se modifica.");
                }
                jugador.sendMessage("§fLa tienda está en el panel. §e/puesto");
                return;
            }
            if (zona == Cuadricula.Zona.PASILLO) {
                if (administra(jugador)) {
                    jugador.sendMessage("§ePasillo del spawn. §fPuedes decorarlo. No se vende.");
                } else {
                    jugador.sendMessage("§ePasillo del spawn. §fNo se vende. Queda para decorar el barrio.");
                }
                jugador.sendMessage("§fLa libre tiene borde amarillo. Si el borde es rojo, ya tiene dueño.");
                return;
            }
            List<Indice> lados = tramo(x, z);
            if (lados != null) {
                Camino.Estado estado = estadoTramo(jugador, lados);
                if (estado == Camino.Estado.SUYO) {
                    jugador.sendMessage("§aEste tramo es tuyo. §fPuedes quitar el camino y unir la casa.");
                    return;
                }
                if (estado == Camino.Estado.BARRIO && jugador.hasPermission("parcelas.admin")) {
                    jugador.sendMessage("§eCamino del barrio. §fPuedes tocarlo.");
                    return;
                }
                if (lados.size() == 4) {
                    jugador.sendMessage("§eCruce entre parcelas. §fSe abre si las cuatro son de la misma persona, o si están unidas entre sí.");
                } else {
                    jugador.sendMessage("§eCamino entre dos parcelas. §fPasa a hierba si los dos lados son tuyos, o si los dos dueños aceptan unirlo.");
                }
                decirLados(jugador, lados);
                return;
            }
            jugador.sendMessage("§6Fuera de las parcelas. §fSe compra la hierba de borde amarillo. El rojo ya tiene dueño.");
            return;
        }
        Parcela parcela = parcelas.get(indice.clave());
        if (parcela == null) {
            jugador.sendMessage("§aParcela libre. §fLa pala de oro te cobra §e" + precioDeNuevas(jugador.getUniqueId()) + " duros§f y pasa a ser tuya.");
            return;
        }
        if (parcela.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§aEs tuya" + (parcela.titulo == null ? "." : ": §f" + parcela.titulo + "§a."));
            jugador.sendMessage("§fNegociar: §e/parcela ofrecer <jugador> <precio>§f. La otra persona: §e/parcela aceptar");
            jugador.sendMessage("§7Invitar, unir y el banco están en §f/parcela panel§7.");
            return;
        }
        decirDueno(jugador, parcela);
    }

    private void reclamar(Player jugador, Indice indice) {
        Parcela ya = parcelas.get(indice.clave());
        if (ya != null) {
            if (ya.dueno.equals(jugador.getUniqueId())) {
                jugador.sendMessage("§aYa es tuya.");
                return;
            }
            decirDueno(jugador, ya);
            return;
        }
        if (banco.enMora(jugador.getUniqueId())) {
            jugador.sendMessage("§cDebes al banco. No puedes comprar una parcela hasta saldar.");
            return;
        }
        int tope = topeDe(jugador.getUniqueId());
        if (cuantas(jugador.getUniqueId()) >= tope) {
            jugador.sendMessage("§cYa tienes " + tope + " parcelas.");
            return;
        }
        int precio = precioDeNuevas(jugador.getUniqueId());
        if (!cobrar(jugador, precio)) {
            jugador.sendMessage("§cTe faltan duros. Esta te cuesta " + precio + ". Tienes " + duros(jugador) + ".");
            return;
        }
        Parcela parcela = new Parcela();
        parcela.dueno = jugador.getUniqueId();
        parcela.nombre = jugador.getName();
        parcela.pagado = precio;
        parcelas.put(indice.clave(), parcela);
        guardar();
        recogerBanderas(indice);
        pintarBorde(indice, Material.RED_CONCRETE);
        int llevas = cuantas(jugador.getUniqueId());
        String siguiente = llevas >= topeDe(jugador.getUniqueId())
            ? "Ya tienes el tope."
            : "La siguiente cuesta " + precioDeNuevas(jugador.getUniqueId()) + " duros.";
        jugador.sendMessage("§aParcela tuya. §f−" + precio + " duros. Te quedan " + duros(jugador) + ". " + siguiente);
        jugador.sendMessage("§7El borde pasa a rojo.");
        if (vecinaPropia(jugador.getUniqueId(), indice)) {
            unirSuelo(jugador.getUniqueId(), indice);
            jugador.sendMessage("§7La de al lado también es tuya. El camino estrecho entre las dos ya es hierba.");
        } else {
            jugador.sendMessage("§7Si compras la de al lado, ese camino estrecho pasa a hierba. Los pasillos del spawn no se tocan.");
        }
    }

    private void nombrar(Player jugador, Indice indice, String[] args) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/parcela nombre <texto>");
            return;
        }
        String texto = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        parcela.titulo = recortar(texto);
        guardar();
        jugador.sendMessage("§aEsta parcela se llama §f" + parcela.titulo);
    }

    private void ofrecer(Player jugador, Indice indice, String[] args) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (parcelaBloqueada(jugador, indice.clave())) {
            return;
        }
        if (args.length < 3) {
            jugador.sendMessage("§e/parcela ofrecer <jugador> <precio>");
            return;
        }
        Player otro = Bukkit.getPlayerExact(args[1]);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar conectada.");
            return;
        }
        int precio;
        try {
            precio = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            jugador.sendMessage("§cEl precio es un número de duros.");
            return;
        }
        if (precio < precioMin || precio > precioMax) {
            jugador.sendMessage("§cEl precio tiene que estar entre " + precioMin + " y " + precioMax + " duros.");
            return;
        }
        Oferta oferta = new Oferta();
        oferta.clave = indice.clave();
        oferta.vendedor = jugador.getUniqueId();
        oferta.precio = precio;
        ofertas.put(otro.getUniqueId(), oferta);
        otro.sendMessage("§6" + jugador.getName() + " §fte ofrece esta parcela por §e" + precio + " duros§f. §e/parcela aceptar");
        jugador.sendMessage("§aOferta enviada a " + otro.getName() + ".");
        panel.pedirOferta(otro, jugador.getName(), tituloParcela(indice.clave()), precio);
    }

    private void aceptar(Player jugador) {
        Oferta oferta = ofertas.remove(jugador.getUniqueId());
        if (oferta == null) {
            jugador.sendMessage("§cNadie te ha ofrecido una parcela.");
            return;
        }
        Parcela parcela = parcelas.get(oferta.clave);
        if (parcela == null || !parcela.dueno.equals(oferta.vendedor)) {
            jugador.sendMessage("§cEsa oferta ya no vale.");
            return;
        }
        if (banco.enMora(jugador.getUniqueId())) {
            ofertas.put(jugador.getUniqueId(), oferta);
            jugador.sendMessage("§cDebes al banco. No puedes comprar una parcela hasta saldar.");
            return;
        }
        if (banco.avalada(oferta.clave)) {
            ofertas.put(jugador.getUniqueId(), oferta);
            jugador.sendMessage("§cEsa parcela está de aval. No se vende.");
            return;
        }
        if (aldeanos != null && !aldeanos.cabenEnCasa(oferta.clave, jugador.getUniqueId())) {
            ofertas.put(jugador.getUniqueId(), oferta);
            jugador.sendMessage("§cCon los aldeanos de esta casa pasarías de 8.");
            return;
        }
        if (!cobrar(jugador, oferta.precio)) {
            jugador.sendMessage("§cTe faltan duros. Pide " + oferta.precio + ".");
            return;
        }
        int tasa = oferta.precio * impuesto / 100;
        int paraDueno = oferta.precio - tasa;
        if (!banco.ingresar(Bukkit.getOfflinePlayer(parcela.dueno), paraDueno)) {
            economia.depositPlayer(jugador, oferta.precio);
            jugador.sendMessage("§cNo se ha podido pagar. Te he devuelto los duros.");
            return;
        }
        banco.sumarCaja(tasa);
        quitarUniones(clave(oferta.clave), jugador.getUniqueId());
        UUID anteriorId = parcela.dueno;
        String anterior = parcela.nombre;
        parcela.dueno = jugador.getUniqueId();
        parcela.nombre = jugador.getName();
        parcela.titulo = null;
        parcela.precio = null;
        parcela.pagado = oferta.precio;
        parcela.amigos.clear();
        if (aldeanos != null) {
            aldeanos.pasarCasa(oferta.clave, jugador.getUniqueId());
        }
        recogerBanderas(clave(oferta.clave));
        guardar();
        pintarBorde(clave(oferta.clave), Material.RED_CONCRETE);
        jugador.sendMessage("§aParcela tuya. §f" + anterior + " recibe " + paraDueno + " duros. El borde queda rojo.");
        soltarCaminos(jugador, anteriorId, clave(oferta.clave));
        Player vendedor = Bukkit.getPlayer(oferta.vendedor);
        if (vendedor != null) {
            vendedor.sendMessage("§a" + jugador.getName() + " ha aceptado. Recibes " + paraDueno + " duros.");
        }
    }

    private void vender(Player jugador, Indice indice, String[] args) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (parcelaBloqueada(jugador, indice.clave())) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/parcela vender <precio>§f, entre " + precioMin + " y " + precioMax + " duros.");
            return;
        }
        int precio;
        try {
            precio = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            jugador.sendMessage("§cEl precio es un número de duros.");
            return;
        }
        if (precio < precioMin || precio > precioMax) {
            jugador.sendMessage("§cEl precio tiene que estar entre " + precioMin + " y " + precioMax + " duros.");
            return;
        }
        parcela.precio = precio;
        guardar();
        jugador.sendMessage("§aEn venta por " + precio + " duros. §fQuien entre y escriba §e/parcela comprar§f se la queda.");
    }

    private void cancelar(Player jugador, Indice indice) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        parcela.precio = null;
        guardar();
        jugador.sendMessage("§aYa no está en venta.");
    }

    private void comprar(Player jugador, Indice indice) {
        Parcela parcela = parcelas.get(indice.clave());
        if (parcela == null) {
            jugador.sendMessage("§eEstá libre. §fPala de oro: §e" + precioDeNuevas(jugador.getUniqueId()) + " duros§f.");
            return;
        }
        if (parcela.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cYa es tuya.");
            return;
        }
        if (parcela.precio == null) {
            decirDueno(jugador, parcela);
            return;
        }
        if (parcela.precio > precioMax) {
            jugador.sendMessage("§cEse precio ya no vale. El máximo es " + precioMax + " duros.");
            return;
        }
        if (banco.enMora(jugador.getUniqueId())) {
            jugador.sendMessage("§cDebes al banco. No puedes comprar una parcela hasta saldar.");
            return;
        }
        if (banco.avalada(indice.clave())) {
            jugador.sendMessage("§cEsa parcela está de aval. No se vende.");
            return;
        }
        if (cuantas(jugador.getUniqueId()) >= topeDe(jugador.getUniqueId())) {
            jugador.sendMessage("§cYa tienes el tope de parcelas.");
            return;
        }
        if (aldeanos != null && !aldeanos.cabenEnCasa(indice.clave(), jugador.getUniqueId())) {
            jugador.sendMessage("§cCon los aldeanos de esta casa pasarías de 8.");
            return;
        }
        int precio = parcela.precio;
        if (!cobrar(jugador, precio)) {
            jugador.sendMessage("§cTe faltan duros. Pide " + precio + ".");
            return;
        }
        int tasa = precio * impuesto / 100;
        int paraDueno = precio - tasa;
        if (!banco.ingresar(Bukkit.getOfflinePlayer(parcela.dueno), paraDueno)) {
            economia.depositPlayer(jugador, precio);
            jugador.sendMessage("§cNo se ha podido pagar al dueño. Te he devuelto los duros.");
            return;
        }
        banco.sumarCaja(tasa);
        quitarUniones(indice, jugador.getUniqueId());
        UUID anteriorId = parcela.dueno;
        String anterior = parcela.nombre;
        parcela.dueno = jugador.getUniqueId();
        parcela.nombre = jugador.getName();
        parcela.precio = null;
        parcela.pagado = precio;
        parcela.amigos.clear();
        if (aldeanos != null) {
            aldeanos.pasarCasa(indice.clave(), jugador.getUniqueId());
        }
        recogerBanderas(indice);
        guardar();
        pintarBorde(indice, Material.RED_CONCRETE);
        jugador.sendMessage("§aParcela tuya. §f" + anterior + " recibe " + paraDueno + " duros. El barrio se queda " + tasa + ". El borde queda rojo.");
        soltarCaminos(jugador, anteriorId, indice);
    }

    private void abandonar(Player jugador, Indice indice) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (parcelaBloqueada(jugador, indice.clave())) {
            return;
        }
        UUID anteriorId = parcela.dueno;
        if (aldeanos != null) {
            aldeanos.devolverCasa(jugador, indice.clave());
        }
        recogerBanderas(indice);
        quitarUniones(indice, jugador.getUniqueId());
        pintarBorde(indice, Material.YELLOW_CONCRETE);
        parcelas.remove(indice.clave());
        guardar();
        jugador.sendMessage("§eParcela libre otra vez. §fLos duros no vuelven. Lo construido se queda. El borde vuelve a amarillo.");
        soltarCaminos(null, anteriorId, indice);
    }

    private void amigo(Player jugador, Indice indice, String[] args, boolean agregar) {
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage(agregar ? "§e/parcela amigo <nombre>" : "§e/parcela quitar <nombre>");
            return;
        }
        Player otro = Bukkit.getPlayerExact(args[1]);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar en el barrio ahora mismo.");
            return;
        }
        if (agregar) {
            sumarAmigo(jugador, indice.clave(), otro.getUniqueId());
        } else {
            restarAmigo(jugador, indice.clave(), otro.getUniqueId());
        }
    }

    private Parcela propia(Player jugador, Indice indice) {
        Parcela parcela = parcelas.get(indice.clave());
        if (parcela == null || !parcela.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsta parcela no es tuya.");
            return null;
        }
        return parcela;
    }

    private int precioDeNuevas(UUID id) {
        int precio = precioLibre;
        int llevas = cuantas(id);
        for (int i = 0; i < llevas; i++) {
            precio += 30 + 10 * i;
        }
        return precio;
    }

    int duros(Player jugador) {
        return durosDe(jugador);
    }

    public int durosDe(OfflinePlayer jugador) {
        if (economia == null || jugador == null) {
            return 0;
        }
        return (int) Math.floor(economia.getBalance(jugador));
    }

    boolean cobrar(Player jugador, int precio) {
        if (precio <= 0 || duros(jugador) < precio) {
            return false;
        }
        double antes = economia.getBalance(jugador);
        EconomyResponse respuesta = economia.withdrawPlayer(jugador, precio);
        if (!respuesta.transactionSuccess() || economia.getBalance(jugador) > antes - precio + 0.5) {
            if (respuesta.transactionSuccess()) {
                economia.depositPlayer(jugador, precio);
            }
            return false;
        }
        return true;
    }

    void devolver(Player jugador, int precio) {
        economia.depositPlayer(jugador, precio);
    }

    boolean abonar(OfflinePlayer jugador, int precio) {
        if (precio <= 0 || jugador == null) {
            return true;
        }
        EconomyResponse respuesta = economia.depositPlayer(jugador, precio);
        return respuesta.transactionSuccess();
    }

    boolean enMora(UUID id) {
        return banco != null && banco.enMora(id);
    }

    boolean cobrarA(OfflinePlayer jugador, int precio) {
        if (precio <= 0 || jugador == null) {
            return true;
        }
        if (durosDe(jugador) < precio) {
            return false;
        }
        double antes = economia.getBalance(jugador);
        EconomyResponse respuesta = economia.withdrawPlayer(jugador, precio);
        if (!respuesta.transactionSuccess() || economia.getBalance(jugador) > antes - precio + 0.5) {
            if (respuesta.transactionSuccess()) {
                economia.depositPlayer(jugador, precio);
            }
            return false;
        }
        return true;
    }

    private boolean parcelaBloqueada(Player jugador, String clave) {
        if (banco.avalada(clave)) {
            jugador.sendMessage("§cEsta parcela es el aval de tu préstamo.");
            return true;
        }
        if (venta.hayAlguno(clave)) {
            jugador.sendMessage("§cQuita el cofre de venta antes.");
            return true;
        }
        return false;
    }

    String motivoVenta(Player jugador, int x, int z) {
        if (jugador.getWorld() == null || !jugador.getWorld().getName().equals("world")) {
            return "El cofre de venta va en tu parcela del barrio.";
        }
        Cuadricula.Zona zona = cuadricula.zona(x, z);
        if (zona == Cuadricula.Zona.SPAWN) {
            return "En el spawn no se pone.";
        }
        if (zona == Cuadricula.Zona.PASILLO) {
            return "En el pasillo no se pone.";
        }
        Indice indice = parcelaEn(x, z);
        if (indice == null) {
            return "Eso no es una parcela.";
        }
        Parcela parcela = parcelas.get(indice.clave());
        if (parcela == null) {
            return "Esta parcela está libre. Primero, la pala de oro.";
        }
        if (!parcela.dueno.equals(jugador.getUniqueId())) {
            return "Esta parcela no es tuya.";
        }
        if (parcela.precio != null) {
            return "Quítala de la venta antes de poner el cofre.";
        }
        if (banco.avalada(indice.clave())) {
            return "Esta parcela es un aval. No admite un cofre nuevo.";
        }
        if (venta.cuantos(indice.clave()) >= 4) {
            return "Ya hay cuatro cofres de venta en esta parcela.";
        }
        return null;
    }

    Cuadricula rejilla() {
        return cuadricula;
    }

    int lado() {
        return lado;
    }

    private void recogerBanderas(Indice indice) {
        if (indice == null) {
            return;
        }
        Plugin viajes = getServer().getPluginManager().getPlugin("Viajes");
        if (viajes == null || !viajes.isEnabled()) {
            return;
        }
        int margen = Math.max(0, salto - lado);
        int minX = cuadricula.origen(indice.i) - margen;
        int minZ = cuadricula.origen(indice.j) - margen;
        int maxX = cuadricula.origen(indice.i) + lado - 1 + margen;
        int maxZ = cuadricula.origen(indice.j) + lado - 1 + margen;
        try {
            viajes.getClass().getMethod("recogerZona", String.class, int.class, int.class, int.class, int.class)
                .invoke(viajes, "world", minX, minZ, maxX, maxZ);
        } catch (ReflectiveOperationException e) {
            getLogger().warning("No pude devolver las banderas de la parcela: " + e.getMessage());
        }
    }

    Aldeanos aldeanos() {
        return aldeanos;
    }

    String claveEn(int x, int z) {
        Indice indice = parcelaEn(x, z);
        return indice == null ? "" : indice.clave();
    }

    List<String> clavesDe(UUID dueno) {
        List<String> lista = new ArrayList<>();
        for (Map.Entry<String, Parcela> entrada : parcelas.entrySet()) {
            if (dueno.equals(entrada.getValue().dueno)) {
                lista.add(entrada.getKey());
            }
        }
        return lista;
    }

    boolean esDueno(UUID dueno, String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela != null && dueno.equals(parcela.dueno);
    }

    boolean enVenta(String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela != null && parcela.precio != null;
    }

    String tituloParcela(String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null) {
            return "Parcela";
        }
        if (parcela.titulo != null && !parcela.titulo.isEmpty()) {
            return parcela.titulo;
        }
        return "Parcela " + clave;
    }

    void soltarAval(String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null) {
            return;
        }
        UUID anterior = parcela.dueno;
        Indice indice = clave(clave);
        recogerBanderas(indice);
        quitarUniones(indice, anterior);
        pintarBorde(indice, Material.YELLOW_CONCRETE);
        parcelas.remove(clave);
        guardar();
        soltarCaminos(null, anterior, indice);
    }

    public int ocupadas() {
        return parcelas.size();
    }

    public int parcelasBase() {
        return maxParcelas;
    }

    int topeDe(UUID id) {
        return maxParcelas;
    }

    public String textoParcela(org.bukkit.OfflinePlayer jugador) {
        if (jugador == null) {
            return "";
        }
        int tope = topeDe(jugador.getUniqueId());
        if (cuantas(jugador.getUniqueId()) >= tope) {
            return "llena";
        }
        return Integer.toString(precioDeNuevas(jugador.getUniqueId()));
    }

    public static void contar(Player jugador) {
        if (instancia != null) {
            instancia.contarCuenta(jugador);
        }
    }

    private void contarCuenta(Player jugador) {
        int tope = topeDe(jugador.getUniqueId());
        int llevas = cuantas(jugador.getUniqueId());
        jugador.sendMessage("§6Tienes §f" + duros(jugador) + " duros§6. Parcelas: §f" + llevas + "/" + tope + "§6.");
        if (llevas < tope) {
            jugador.sendMessage("§fLa siguiente cuesta §e" + precioDeNuevas(jugador.getUniqueId()) + " duros§f. Pala de oro en una libre. §e/parcela cerca");
        }
        jugador.sendMessage("§7El survival y los eventos se abren en el Panel Preguntones: Escape, el botón dorado de abajo.");
        jugador.sendMessage("§7Tienda, banco y parcelas están en el panel. §f/parcela panel");
        String clan = clanes.resumen(jugador.getUniqueId());
        if (!clan.isEmpty()) {
            jugador.sendMessage(clan);
        }
    }

    private void cerca(Player jugador) {
        if (jugador.getWorld() == null || !jugador.getWorld().getName().equals("world")) {
            jugador.sendMessage("§eEso se usa en el barrio.");
            return;
        }
        List<Indice> anclas = parcelasDe(jugador.getUniqueId());
        boolean deClan = false;
        if (anclas.isEmpty()) {
            deClan = true;
            for (UUID id : clanes.companeros(jugador.getUniqueId())) {
                anclas.addAll(parcelasDe(id));
            }
        }
        Indice mejor = anclas.isEmpty() ? libreCercana(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ()) : libreAlLado(anclas, jugador);
        if (mejor == null) {
            jugador.sendMessage("§eNo queda una parcela libre al lado.");
            return;
        }
        World mundo = jugador.getWorld();
        Location sitio = new Location(mundo, origen(mejor.i) + lado / 2.0 + 0.5, 66, origen(mejor.j) + lado / 2.0 + 0.5, jugador.getLocation().getYaw(), 0);
        jugador.teleport(sitio);
        jugador.setFallDistance(0f);
        String donde = deClan && !anclas.isEmpty() ? "al lado de tu clan" : "al lado";
        jugador.sendMessage("§aParcela libre " + donde + ". §fCuesta §e" + precioDeNuevas(jugador.getUniqueId()) + " duros§f. Pala de oro en un bloque.");
    }

    private List<Indice> parcelasDe(UUID dueno) {
        List<Indice> lista = new ArrayList<>();
        for (Map.Entry<String, Parcela> entrada : parcelas.entrySet()) {
            if (!dueno.equals(entrada.getValue().dueno)) {
                continue;
            }
            String[] partes = entrada.getKey().split(",");
            lista.add(new Indice(Integer.parseInt(partes[0]), Integer.parseInt(partes[1])));
        }
        return lista;
    }

    private Indice libreAlLado(List<Indice> anclas, Player jugador) {
        Indice mejor = null;
        int mejorDist = Integer.MAX_VALUE;
        int[][] lados = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (Indice ancla : anclas) {
            for (int[] ladoPaso : lados) {
                int ni = ancla.i + ladoPaso[0];
                int nj = ancla.j + ladoPaso[1];
                if (ni < desde || ni > hasta || nj < desde || nj > hasta) {
                    continue;
                }
                if (Math.abs(origen(ni) - origen(ancla.i)) > salto || Math.abs(origen(nj) - origen(ancla.j)) > salto) {
                    continue;
                }
                Indice vecino = new Indice(ni, nj);
                if (parcelas.containsKey(vecino.clave())) {
                    continue;
                }
                int dist = Math.abs(origen(ni) - jugador.getLocation().getBlockX()) + Math.abs(origen(nj) - jugador.getLocation().getBlockZ());
                if (dist < mejorDist) {
                    mejorDist = dist;
                    mejor = vecino;
                }
            }
        }
        return mejor != null ? mejor : libreCercana(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
    }

    private Indice libreCercana(int x, int z) {
        Indice mejor = null;
        int mejorDist = Integer.MAX_VALUE;
        for (int i = desde; i <= hasta; i++) {
            for (int j = desde; j <= hasta; j++) {
                Indice indice = new Indice(i, j);
                if (parcelas.containsKey(indice.clave())) {
                    continue;
                }
                int dist = Math.abs(origen(i) + lado / 2 - x) + Math.abs(origen(j) + lado / 2 - z);
                if (dist < mejorDist) {
                    mejorDist = dist;
                    mejor = indice;
                }
            }
        }
        return mejor;
    }

    private int cuantas(UUID dueno) {
        int n = 0;
        for (Parcela parcela : parcelas.values()) {
            if (dueno.equals(parcela.dueno)) {
                n++;
            }
        }
        return n;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alRomper(BlockBreakEvent event) {
        if (venta.romper(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        decidir(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alPoner(BlockPlaceEvent event) {
        if (venta.alColocar(event)) {
            return;
        }
        decidir(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alUsar(PlayerInteractEvent event) {
        Block bloque = event.getClickedBlock();
        if (bloque == null || fueraDelBarrio(bloque.getWorld())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && venta.esPuesto(bloque)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
            boolean dueno = event.getPlayer().isSneaking();
            venta.abrir(event.getPlayer(), bloque, dueno);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
            && Entrada.caja(bloque.getType().name())
            && !puede(event.getPlayer(), bloque.getX(), bloque.getZ())) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setCancelled(true);
            avisar(event.getPlayer(), bloque.getX(), bloque.getZ());
            return;
        }
        if (bloque.getType() == Material.STONE_BUTTON && bloque.getX() == 17 && bloque.getY() == 66 && bloque.getZ() == 8) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getItem() != null
                && event.getItem().getType() == Material.GOLDEN_SHOVEL) {
            event.setCancelled(true);
            Indice indice = parcelaEn(bloque.getX(), bloque.getZ());
            if (indice == null) {
                avisar(event.getPlayer(), bloque.getX(), bloque.getZ());
                return;
            }
            if (!parcelas.containsKey(indice.clave())) {
                reclamar(event.getPlayer(), indice);
                return;
            }
            Parcela parcela = parcelas.get(indice.clave());
            if (parcela.dueno.equals(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage("§aYa es tuya. §fLos pasillos del spawn no se rompen.");
                return;
            }
            decirDueno(event.getPlayer(), parcela);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && dejarEntrar(event, bloque)) {
            return;
        }
        decidir(event, event.getPlayer(), bloque);
    }

    private boolean dejarEntrar(PlayerInteractEvent event, Block bloque) {
        if (!Entrada.es(bloque.getType().name()) || parcelaEn(bloque.getX(), bloque.getZ()) == null) {
            return false;
        }
        if (puede(event.getPlayer(), bloque.getX(), bloque.getZ())) {
            return false;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            return true;
        }
        event.setCancelled(false);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        if (Entrada.hierro(bloque.getType().name())) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            alternarHierro(bloque);
        } else {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        }
        return true;
    }

    private void alternarHierro(Block bloque) {
        if (!(bloque.getBlockData() instanceof Openable abierto)) {
            return;
        }
        boolean abrir = !abierto.isOpen();
        ponerAbierta(bloque, abrir);
        if (bloque.getBlockData() instanceof Door puerta) {
            Block otra = bloque.getRelative(puerta.getHalf() == Door.Half.TOP ? BlockFace.DOWN : BlockFace.UP);
            if (otra.getType() == bloque.getType()) {
                ponerAbierta(otra, abrir);
            }
        }
        boolean trampilla = bloque.getType() == Material.IRON_TRAPDOOR;
        Sound sonido = abrir
            ? (trampilla ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_DOOR_OPEN)
            : (trampilla ? Sound.BLOCK_IRON_TRAPDOOR_CLOSE : Sound.BLOCK_IRON_DOOR_CLOSE);
        bloque.getWorld().playSound(bloque.getLocation().add(0.5, 0.5, 0.5), sonido, 1f, 1f);
    }

    private void ponerAbierta(Block bloque, boolean abrir) {
        if (bloque.getBlockData() instanceof Openable abierto) {
            abierto.setOpen(abrir);
            bloque.setBlockData(abierto, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alAbrir(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player jugador)) {
            return;
        }
        Location sitio = event.getInventory().getLocation();
        if (sitio == null || sitio.getWorld() == null || fueraDelBarrio(sitio.getWorld())) {
            return;
        }
        if (puede(jugador, sitio.getBlockX(), sitio.getBlockZ())) {
            return;
        }
        event.setCancelled(true);
        avisar(jugador, sitio.getBlockX(), sitio.getBlockZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alTrasvasar(InventoryMoveItemEvent event) {
        Location origen = event.getSource().getLocation();
        Location destino = event.getDestination().getLocation();
        if (origen == null || destino == null || origen.getWorld() == null || destino.getWorld() == null) {
            return;
        }
        if (fueraDelBarrio(origen.getWorld()) && fueraDelBarrio(destino.getWorld())) {
            return;
        }
        if (!mismoDueno(origen, destino)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alVaciar(PlayerBucketEmptyEvent event) {
        decidir(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alLlenar(PlayerBucketFillEvent event) {
        decidir(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alCosechar(PlayerHarvestBlockEvent event) {
        decidir(event, event.getPlayer(), event.getHarvestedBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alEntidad(PlayerInteractEntityEvent event) {
        decidir(event, event.getPlayer(), event.getRightClicked().getLocation().getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alArmadura(PlayerArmorStandManipulateEvent event) {
        decidir(event, event.getPlayer(), event.getRightClicked().getLocation().getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void alCuadro(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player jugador) {
            decidir(event, jugador, event.getEntity().getLocation().getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void alGolpear(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player atacante
            && event.getEntity() instanceof Player victima
            && karmaPermite(atacante, victima)) {
            return;
        }
        if (event.getDamager() instanceof Player jugador && rechazar(jugador, event.getEntity().getLocation().getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean karmaPermite(Player atacante, Player victima) {
        Plugin karma = getServer().getPluginManager().getPlugin("Karma");
        if (karma == null || !karma.isEnabled()) {
            return false;
        }
        try {
            Object valor = karma.getClass().getMethod("golpePermitido", Player.class, Player.class)
                .invoke(karma, atacante, victima);
            return Boolean.TRUE.equals(valor);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void alCambiar(EntityChangeBlockEvent event) {
        if (fueraDelBarrio(event.getBlock().getWorld())) {
            return;
        }
        int x = event.getBlock().getX();
        int z = event.getBlock().getZ();
        if (event.getEntity() instanceof Player jugador) {
            if (!puede(jugador, x, z)) {
                event.setCancelled(true);
            } else if (administra(jugador)) {
                Cuadricula.Zona zona = cuadricula.zona(x, z);
                if (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO) {
                    event.setCancelled(false);
                }
            }
            return;
        }
        Indice indice = parcelaEn(x, z);
        if (indice != null && parcelas.containsKey(indice.clave())) {
            return;
        }
        if (dentroDelBarrio(x, z)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alExplotar(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> esBarrio(block) && dentroDelBarrio(block.getX(), block.getZ()));
    }

    @EventHandler
    public void alExplotarBloque(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> esBarrio(block) && dentroDelBarrio(block.getX(), block.getZ()));
    }

    private void decidir(Cancellable event, Player jugador, Block bloque) {
        if (rechazar(jugador, bloque)) {
            event.setCancelled(true);
            return;
        }
        if (bloque == null || !administra(jugador)) {
            return;
        }
        Cuadricula.Zona zona = cuadricula.zona(bloque.getX(), bloque.getZ());
        if (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO) {
            event.setCancelled(false);
        }
    }

    private boolean rechazar(Player jugador, Block bloque) {
        if (bloque == null || fueraDelBarrio(bloque.getWorld()) || puede(jugador, bloque.getX(), bloque.getZ())) {
            return false;
        }
        avisar(jugador, bloque.getX(), bloque.getZ());
        return true;
    }

    private boolean mismoDueno(Location a, Location b) {
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        Indice ia = parcelaEn(a.getBlockX(), a.getBlockZ());
        Indice ib = parcelaEn(b.getBlockX(), b.getBlockZ());
        if (ia == null || ib == null) {
            return false;
        }
        if (ia.clave().equals(ib.clave())) {
            return true;
        }
        Parcela pa = parcelas.get(ia.clave());
        Parcela pb = parcelas.get(ib.clave());
        return pa != null && pb != null && pa.dueno.equals(pb.dueno);
    }

    private boolean administra(Player jugador) {
        return jugador.isOp() || jugador.hasPermission("parcelas.admin");
    }

    private boolean puede(Player jugador, int x, int z) {
        if (!dentroDelBarrio(x, z)) {
            return true;
        }
        Cuadricula.Zona zona = cuadricula.zona(x, z);
        if (administra(jugador) && (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO)) {
            return true;
        }
        Indice indice = parcelaEn(x, z);
        if (indice != null) {
            Parcela parcela = parcelas.get(indice.clave());
            if (parcela == null) {
                return false;
            }
            return parcela.dueno.equals(jugador.getUniqueId()) || parcela.amigos.contains(jugador.getUniqueId());
        }
        List<Indice> lados = tramo(x, z);
        if (lados != null) {
            return Camino.puede(estadoTramo(jugador, lados), jugador.hasPermission("parcelas.admin"));
        }
        return false;
    }

    private Camino.Estado estadoTramo(Player jugador, List<Indice> lados) {
        List<Camino.Lado> datos = new ArrayList<>();
        List<String> claves = new ArrayList<>();
        boolean completas = true;
        for (Indice lado : lados) {
            if (parcelas.get(lado.clave()) == null) {
                completas = false;
            }
            claves.add(lado.clave());
        }
        String enlace = completas ? Enlace.grupo(claves, paresUnidos(claves)) : null;
        boolean duenoDelGrupo = enlace != null && poseeAlguna(jugador.getUniqueId(), lados);
        UUID id = jugador.getUniqueId();
        for (Indice lado : lados) {
            Parcela parcela = parcelas.get(lado.clave());
            if (parcela == null) {
                datos.add(new Camino.Lado(null, false));
                continue;
            }
            boolean entra = parcela.dueno.equals(id) || parcela.amigos.contains(id) || duenoDelGrupo;
            String marca = enlace != null ? enlace : parcela.dueno.toString();
            datos.add(new Camino.Lado(marca, entra));
        }
        return Camino.estado(datos);
    }

    private void soltarCaminos(Player receptor, UUID anterior, Indice indice) {
        boolean unio = false;
        boolean solto = false;
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] paso : pasos) {
            int ni = indice.i + paso[0];
            int nj = indice.j + paso[1];
            if (paso[0] != 0 && !cuadricula.juntas(indice.i, ni)) {
                continue;
            }
            if (paso[1] != 0 && !cuadricula.juntas(indice.j, nj)) {
                continue;
            }
            Parcela vecina = parcelas.get(ni + "," + nj);
            if (vecina == null) {
                continue;
            }
            if (receptor != null && receptor.getUniqueId().equals(vecina.dueno)) {
                unio = true;
            } else if (anterior.equals(vecina.dueno)) {
                solto = true;
            }
        }
        if (unio && receptor != null) {
            unirSuelo(receptor.getUniqueId(), indice);
            receptor.sendMessage("§7La de al lado también es tuya. El camino estrecho entre las dos ya es hierba.");
        }
        if (solto) {
            String texto = "§7El camino estrecho vuelve al barrio. Lo construido se queda. Lo toca quien administra.";
            if (receptor != null) {
                receptor.sendMessage(texto);
            }
            Player viejo = Bukkit.getPlayer(anterior);
            if (viejo != null && viejo != receptor) {
                viejo.sendMessage(texto);
            }
        }
    }

    private void avisar(Player jugador, int x, int z) {
        long ahora = System.currentTimeMillis();
        Long previo = avisos.get(jugador.getUniqueId());
        if (previo != null && ahora - previo < 2500) {
            return;
        }
        avisos.put(jugador.getUniqueId(), ahora);
        Cuadricula.Zona zona = cuadricula.zona(x, z);
        if (zona == Cuadricula.Zona.SPAWN) {
            if (administra(jugador)) {
                jugador.sendMessage("§eEsto es el spawn. §fPuedes modificarlo. Los demás no.");
            } else {
                jugador.sendMessage("§eEsto es el spawn. §fNo se modifica.");
            }
            jugador.sendMessage("§fLa tienda está en el panel.");
            return;
        }
        if (zona == Cuadricula.Zona.PASILLO) {
            if (administra(jugador)) {
                jugador.sendMessage("§ePasillo del spawn. §fPuedes decorarlo. No se vende.");
            } else {
                jugador.sendMessage("§ePasillo del spawn. §fNo se vende y no se toca.");
            }
            return;
        }
        Indice indice = parcelaEn(x, z);
        if (indice != null) {
            Parcela parcela = parcelas.get(indice.clave());
            if (parcela == null) {
                jugador.sendMessage("§eParcela libre. §fPala de oro: §e" + precioDeNuevas(jugador.getUniqueId()) + " duros§f.");
                return;
            }
            decirDueno(jugador, parcela);
            jugador.sendMessage("§cAquí solo se abre la puerta. El resto no se toca.");
            return;
        }
        List<Indice> lados = tramo(x, z);
        if (lados != null) {
            jugador.sendMessage("§eEste camino no se rompe. §fPasa a hierba si los lados son tuyos, o si los dos dueños aceptan unirlo.");
            return;
        }
        jugador.sendMessage("§eEsto no es una parcela.");
    }

    private void decirDueno(Player jugador, Parcela parcela) {
        String titulo = parcela.titulo != null && !parcela.titulo.isEmpty() ? " §7(" + parcela.titulo + ")" : "";
        if (parcela.precio != null) {
            jugador.sendMessage("§6Es de §f" + parcela.nombre + titulo + "§6, en venta por §e" + parcela.precio + " duros§6. §f/parcela comprar");
            return;
        }
        jugador.sendMessage("§6Es de §f" + parcela.nombre + titulo + "§6.");
        jugador.sendMessage("§fPara negociar, §e" + parcela.nombre + " §fescribe §e/parcela ofrecer " + jugador.getName() + " <precio> §fy tú §e/parcela aceptar§f.");
    }

    private void decirLados(Player jugador, List<Indice> lados) {
        for (Indice lado : lados) {
            Parcela parcela = parcelas.get(lado.clave());
            if (parcela == null) {
                jugador.sendMessage("§7Un lado está libre.");
            } else {
                jugador.sendMessage("§7Un lado es de §f" + parcela.nombre + "§7.");
            }
        }
    }

    private boolean esBarrio(Block bloque) {
        return bloque.getWorld() != null && bloque.getWorld().getName().equals("world");
    }

    private boolean fueraDelBarrio(org.bukkit.World mundo) {
        return mundo == null || !mundo.getName().equals("world");
    }

    private boolean dentroDelBarrio(int x, int z) {
        int borde = primero + (hasta * salto) + lado + 16;
        return Math.abs(x) <= borde && Math.abs(z) <= borde;
    }

    private List<Indice> tramo(int x, int z) {
        List<Cuadricula.Casilla> lados = cuadricula.unir(x, z);
        if (lados == null) {
            return null;
        }
        List<Indice> indices = new ArrayList<>();
        for (Cuadricula.Casilla lado : lados) {
            indices.add(new Indice(lado.i, lado.j));
        }
        return indices;
    }

    private Indice parcelaEn(int x, int z) {
        Cuadricula.Casilla casilla = cuadricula.parcela(x, z);
        return casilla == null ? null : new Indice(casilla.i, casilla.j);
    }

    private int origen(int indice) {
        return cuadricula.origen(indice);
    }

    private boolean vecinaPropia(UUID dueno, Indice indice) {
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] paso : pasos) {
            int ni = indice.i + paso[0];
            int nj = indice.j + paso[1];
            if (paso[0] != 0 && !cuadricula.juntas(indice.i, ni)) {
                continue;
            }
            if (paso[1] != 0 && !cuadricula.juntas(indice.j, nj)) {
                continue;
            }
            Parcela vecina = parcelas.get(ni + "," + nj);
            if (vecina != null && dueno.equals(vecina.dueno)) {
                return true;
            }
        }
        return false;
    }

    private void unirSuelo(UUID dueno, Indice indice) {
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] paso : pasos) {
            int ni = indice.i + paso[0];
            int nj = indice.j + paso[1];
            if (Enlace.par(cuadricula, indice.i, indice.j, ni, nj) == null) {
                continue;
            }
            Parcela vecina = parcelas.get(ni + "," + nj);
            if (vecina == null || !dueno.equals(vecina.dueno)) {
                continue;
            }
            hierbaEntre(indice, new Indice(ni, nj));
        }
    }

    /** Solo el hueco estrecho entre dos parcelas vecinas. El spawn y los pasillos anchos no entran. */
    private void hierbaEntre(Indice indice, Indice vecino) {
        if (indice == null || vecino == null || Enlace.par(cuadricula, indice.i, indice.j, vecino.i, vecino.j) == null) {
            return;
        }
        World mundo = Bukkit.getWorld("world");
        if (mundo == null) {
            return;
        }
        if (indice.j == vecino.j) {
            int menor = Math.min(cuadricula.origen(indice.i), cuadricula.origen(vecino.i));
            int mayor = Math.max(cuadricula.origen(indice.i), cuadricula.origen(vecino.i));
            int z0 = cuadricula.origen(indice.j);
            for (int x = menor + lado; x < mayor; x++) {
                for (int z = z0; z < z0 + lado; z++) {
                    aHierba(mundo, x, z);
                }
            }
            for (int z = z0; z < z0 + lado; z++) {
                aHierba(mundo, menor + lado - 1, z);
                aHierba(mundo, mayor, z);
            }
            return;
        }
        if (indice.i != vecino.i) {
            return;
        }
        int menor = Math.min(cuadricula.origen(indice.j), cuadricula.origen(vecino.j));
        int mayor = Math.max(cuadricula.origen(indice.j), cuadricula.origen(vecino.j));
        int x0 = cuadricula.origen(indice.i);
        for (int z = menor + lado; z < mayor; z++) {
            for (int x = x0; x < x0 + lado; x++) {
                aHierba(mundo, x, z);
            }
        }
        for (int x = x0; x < x0 + lado; x++) {
            aHierba(mundo, x, menor + lado - 1);
            aHierba(mundo, x, mayor);
        }
    }

    private void aHierba(org.bukkit.World mundo, int x, int z) {
        Cuadricula.Zona zona = cuadricula.zona(x, z);
        if (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO) {
            if (!avisoReserva) {
                avisoReserva = true;
                getLogger().warning("Una unión iba a tocar el spawn o un pasillo en " + x + ", " + z + ". Ese bloque no se ha cambiado.");
            }
            return;
        }
        if (zona != Cuadricula.Zona.CAMINO && zona != Cuadricula.Zona.PARCELA) {
            return;
        }
        Block suelo = mundo.getBlockAt(x, 64, z);
        if (suelo.getType() == Material.DIRT_PATH
                || suelo.getType() == Material.YELLOW_CONCRETE
                || suelo.getType() == Material.RED_CONCRETE) {
            suelo.setType(Material.GRASS_BLOCK, false);
        }
    }

    private void pintarOcupadas() {
        if (Bukkit.getWorld("world") == null) {
            return;
        }
        for (String id : parcelas.keySet()) {
            pintarBorde(clave(id), Material.RED_CONCRETE);
        }
    }

    private void pintarBorde(Indice indice, Material color) {
        World mundo = Bukkit.getWorld("world");
        if (mundo == null || indice == null) {
            return;
        }
        Material puesto = color == Material.RED_CONCRETE ? Material.YELLOW_CONCRETE : Material.RED_CONCRETE;
        int x0 = origen(indice.i);
        int z0 = origen(indice.j);
        int x1 = x0 + lado - 1;
        int z1 = z0 + lado - 1;
        for (int x = x0; x <= x1; x++) {
            teñir(mundo, x, z0, puesto, color);
            teñir(mundo, x, z1, puesto, color);
        }
        for (int z = z0 + 1; z < z1; z++) {
            teñir(mundo, x0, z, puesto, color);
            teñir(mundo, x1, z, puesto, color);
        }
    }

    private void teñir(World mundo, int x, int z, Material puesto, Material color) {
        if (cuadricula.zona(x, z) != Cuadricula.Zona.PARCELA) {
            return;
        }
        Block suelo = mundo.getBlockAt(x, 64, z);
        if (suelo.getType() == puesto) {
            suelo.setType(color, false);
        }
    }

    private Indice clave(String texto) {
        String[] partes = texto.split(",");
        return new Indice(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));
    }

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= 15 ? texto : texto.substring(0, 15);
    }

    private void cargar() {
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        for (String clave : yaml.getKeys(false)) {
            Parcela parcela = new Parcela();
            parcela.dueno = UUID.fromString(yaml.getString(clave + ".dueno"));
            parcela.nombre = yaml.getString(clave + ".nombre", "?");
            parcela.titulo = yaml.getString(clave + ".titulo", null);
            if (yaml.contains(clave + ".precio")) {
                parcela.precio = yaml.getInt(clave + ".precio");
            }
            for (String amigo : yaml.getStringList(clave + ".amigos")) {
                parcela.amigos.add(UUID.fromString(amigo));
            }
            if (yaml.contains(clave + ".pagado")) {
                parcela.pagado = yaml.getInt(clave + ".pagado");
            }
            for (String unida : yaml.getStringList(clave + ".unidas")) {
                parcela.unidas.add(unida);
            }
            parcelas.put(clave, parcela);
        }
    }

    private void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Parcela> entrada : parcelas.entrySet()) {
            String clave = entrada.getKey();
            Parcela parcela = entrada.getValue();
            yaml.set(clave + ".dueno", parcela.dueno.toString());
            yaml.set(clave + ".nombre", parcela.nombre);
            if (parcela.titulo != null && !parcela.titulo.isEmpty()) {
                yaml.set(clave + ".titulo", parcela.titulo);
            }
            if (parcela.precio != null) {
                yaml.set(clave + ".precio", parcela.precio);
            }
            List<String> amigos = new ArrayList<>();
            for (UUID amigo : parcela.amigos) {
                amigos.add(amigo.toString());
            }
            yaml.set(clave + ".amigos", amigos);
            if (parcela.pagado != null && parcela.pagado > 0) {
                yaml.set(clave + ".pagado", parcela.pagado);
            }
            if (!parcela.unidas.isEmpty()) {
                yaml.set(clave + ".unidas", new ArrayList<>(parcela.unidas));
            }
        }
        try {
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            getLogger().warning("No pude guardar las parcelas: " + e.getMessage());
        }
    }

    boolean enBarrio(Player jugador) {
        return jugador != null && !fueraDelBarrio(jugador.getWorld());
    }

    Cuadricula.Zona zonaDe(Player jugador) {
        if (!enBarrio(jugador)) {
            return null;
        }
        return cuadricula.zona(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
    }

    String claveAqui(Player jugador) {
        if (!enBarrio(jugador)) {
            return null;
        }
        Indice indice = parcelaEn(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        return indice == null ? null : indice.clave();
    }

    boolean libre(String clave) {
        return parse(clave) != null && !parcelas.containsKey(clave);
    }

    boolean mia(Player jugador, String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela != null && jugador != null && jugador.getUniqueId().equals(parcela.dueno);
    }

    int llevasDe(UUID id) {
        return cuantas(id);
    }

    int topePara(UUID id) {
        return topeDe(id);
    }

    int precioNueva(UUID id) {
        return precioDeNuevas(id);
    }

    int precioMinimo() {
        return precioMin;
    }

    int precioMaximo() {
        return precioMax;
    }

    int precioPrimera() {
        return precioLibre;
    }

    int tasaDe(int precio) {
        return precio * impuesto / 100;
    }

    int porcentajeImpuesto() {
        return impuesto;
    }

    int cajaBanco() {
        return banco.cajaActual();
    }

    int recompraDe(String clave) {
        return Cuentas.recompra(baseDe(clave));
    }

    int pagadoDe(String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null || parcela.pagado == null || parcela.pagado <= 0) {
            return precioLibre;
        }
        return parcela.pagado;
    }

    boolean pagadoApuntado(String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela != null && parcela.pagado != null && parcela.pagado > 0;
    }

    Integer precioPuesto(String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela == null ? null : parcela.precio;
    }

    String duenoNombre(String clave) {
        Parcela parcela = parcelas.get(clave);
        return parcela == null ? "" : parcela.nombre;
    }

    String tituloDe(String clave) {
        return tituloParcela(clave);
    }

    String tituloSi(String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null || parcela.titulo == null) {
            return "";
        }
        return parcela.titulo;
    }

    String nombreDe(UUID id) {
        if (id == null) {
            return "alguien";
        }
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String nombre = Bukkit.getOfflinePlayer(id).getName();
        return nombre == null ? "alguien" : nombre;
    }

    String resumen(Player jugador, String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null) {
            return "Esta parcela está libre.";
        }
        List<String> lineas = new ArrayList<>();
        lineas.add(tituloParcela(clave) + ". De " + parcela.nombre + ".");
        if (parcela.precio != null) {
            lineas.add("En venta por " + parcela.precio + " duros.");
        }
        if (!parcela.amigos.isEmpty()) {
            List<String> nombres = new ArrayList<>();
            for (UUID amigo : parcela.amigos) {
                nombres.add(nombreDe(amigo));
            }
            lineas.add("Construyen: " + String.join(", ", nombres) + ".");
        }
        if (!parcela.unidas.isEmpty()) {
            List<String> nombres = new ArrayList<>();
            for (String otra : parcela.unidas) {
                Parcela vecina = parcelas.get(otra);
                nombres.add(vecina == null ? otra : vecina.nombre);
            }
            lineas.add("Camino unido con " + String.join(", ", nombres) + ".");
        }
        if (jugador != null && jugador.getUniqueId().equals(parcela.dueno)) {
            int pago = recompraDe(clave);
            if (pagadoApuntado(clave)) {
                lineas.add("Te costó " + pagadoDe(clave) + ". El banco paga " + pago + ".");
            } else {
                lineas.add("El banco paga " + pago + ", la quinta parte de la primera.");
            }
        }
        return String.join("\n", lineas);
    }

    String textoOferta(Player jugador) {
        Oferta oferta = ofertas.get(jugador.getUniqueId());
        if (oferta == null) {
            return null;
        }
        Parcela parcela = parcelas.get(oferta.clave);
        if (parcela == null || !parcela.dueno.equals(oferta.vendedor)) {
            ofertas.remove(jugador.getUniqueId());
            return null;
        }
        return nombreDe(oferta.vendedor) + " te ofrece " + tituloParcela(oferta.clave) + " por " + oferta.precio + " duros.";
    }

    String textoUnion(Player jugador) {
        Propuesta propuesta = propuestas.get(jugador.getUniqueId());
        if (propuesta == null) {
            return null;
        }
        if (System.currentTimeMillis() > propuesta.vence) {
            propuestas.remove(jugador.getUniqueId());
            return null;
        }
        return nombreDe(propuesta.de) + " quiere unir el camino con tu parcela.";
    }

    String textoTramo(Player jugador) {
        if (!enBarrio(jugador)) {
            return "Eso se usa en el barrio.";
        }
        List<Indice> lados = tramo(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        if (lados == null) {
            return "Esto no es una parcela.";
        }
        Camino.Estado estado = estadoTramo(jugador, lados);
        if (estado == Camino.Estado.SUYO) {
            return "Este tramo es tuyo. Puedes quitar el camino y unir la casa.";
        }
        if (lados.size() == 4) {
            return "Cruce entre parcelas. Se abre si las cuatro son de la misma persona, o si están unidas entre sí.";
        }
        return "Camino entre dos parcelas. Pasa a hierba si los dos lados son tuyos, o si los dos dueños aceptan unirlo.";
    }

    List<Opcion> misParcelas(UUID id) {
        List<Opcion> lista = new ArrayList<>();
        List<String> claves = clavesDe(id);
        claves.sort(String::compareTo);
        for (String clave : claves) {
            Parcela parcela = parcelas.get(clave);
            if (parcela == null) {
                continue;
            }
            String ayuda = parcela.precio != null
                ? "En venta por " + parcela.precio + " duros."
                : "El banco paga " + recompraDe(clave) + " duros.";
            lista.add(new Opcion(clave, null, parcela.dueno, tituloParcela(clave), ayuda));
        }
        return lista;
    }

    List<Opcion> gentePara(Player jugador, String clave, boolean echar) {
        List<Opcion> lista = new ArrayList<>();
        Parcela parcela = parcelas.get(clave);
        if (parcela == null || jugador == null || !jugador.getUniqueId().equals(parcela.dueno)) {
            return lista;
        }
        if (echar) {
            List<UUID> amigos = new ArrayList<>(parcela.amigos);
            amigos.sort(Comparator.comparing(this::nombreDe, String.CASE_INSENSITIVE_ORDER));
            for (UUID amigo : amigos) {
                lista.add(new Opcion(clave, null, amigo, nombreDe(amigo), "Deja de construir aquí."));
            }
            return lista;
        }
        List<Player> otros = new ArrayList<>();
        for (Player otro : Bukkit.getOnlinePlayers()) {
            if (!otro.getUniqueId().equals(jugador.getUniqueId()) && !parcela.amigos.contains(otro.getUniqueId())) {
                otros.add(otro);
            }
        }
        otros.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        for (Player otro : otros) {
            lista.add(new Opcion(clave, null, otro.getUniqueId(), otro.getName(), "Puede construir en esta parcela."));
        }
        return lista;
    }

    List<Opcion> paraUnir(String clave) {
        List<Opcion> lista = new ArrayList<>();
        Indice indice = parse(clave);
        Parcela parcela = indice == null ? null : parcelas.get(clave);
        if (indice == null || parcela == null) {
            return lista;
        }
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] paso : pasos) {
            int ni = indice.i + paso[0];
            int nj = indice.j + paso[1];
            if (Enlace.par(cuadricula, indice.i, indice.j, ni, nj) == null) {
                continue;
            }
            String otra = ni + "," + nj;
            Parcela vecina = parcelas.get(otra);
            if (vecina == null || vecina.dueno.equals(parcela.dueno)) {
                continue;
            }
            if (parcela.unidas.contains(otra) && vecina.unidas.contains(clave)) {
                continue;
            }
            Player online = Bukkit.getPlayer(vecina.dueno);
            if (online == null) {
                continue;
            }
            lista.add(new Opcion(clave, otra, vecina.dueno, online.getName(), tituloParcela(otra)));
        }
        return lista;
    }

    List<Opcion> unidasCon(String clave) {
        List<Opcion> lista = new ArrayList<>();
        Parcela parcela = parcelas.get(clave);
        if (parcela == null) {
            return lista;
        }
        for (String otra : parcela.unidas) {
            Parcela vecina = parcelas.get(otra);
            if (vecina == null || !vecina.unidas.contains(clave)) {
                continue;
            }
            lista.add(new Opcion(clave, otra, vecina.dueno, nombreDe(vecina.dueno), tituloParcela(otra)));
        }
        return lista;
    }

    List<Opcion> uniblesAqui(Player jugador) {
        String clave = claveAqui(jugador);
        if (clave != null && mia(jugador, clave)) {
            return paraUnir(clave);
        }
        List<Opcion> lista = new ArrayList<>();
        if (!enBarrio(jugador)) {
            return lista;
        }
        List<Indice> lados = tramo(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        if (lados == null) {
            return lista;
        }
        for (Indice lado : lados) {
            Parcela parcela = parcelas.get(lado.clave());
            if (parcela != null && jugador.getUniqueId().equals(parcela.dueno)) {
                lista.addAll(paraUnir(lado.clave()));
            }
        }
        return lista;
    }

    List<Opcion> uniblesMenu(Player jugador) {
        List<Opcion> todas = new ArrayList<>();
        boolean varias = cuantas(jugador.getUniqueId()) > 1;
        for (String clave : clavesDe(jugador.getUniqueId())) {
            for (Opcion opcion : paraUnir(clave)) {
                String etiqueta = varias ? tituloParcela(clave) + " · " + opcion.nombre : opcion.nombre;
                todas.add(new Opcion(opcion.clave, opcion.otra, opcion.id, etiqueta, opcion.ayuda));
            }
        }
        return todas;
    }

    void irLibre(Player jugador) {
        cerca(jugador);
    }

    void reclamarAqui(Player jugador) {
        if (!enBarrio(jugador)) {
            jugador.sendMessage("§eEso se usa en el barrio.");
            return;
        }
        Indice indice = parcelaEn(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        if (indice == null) {
            jugador.sendMessage("§eNo estás en una parcela.");
            return;
        }
        reclamar(jugador, indice);
    }

    void comprarAqui(Player jugador) {
        if (!enBarrio(jugador)) {
            jugador.sendMessage("§eEso se usa en el barrio.");
            return;
        }
        Indice indice = parcelaEn(jugador.getLocation().getBlockX(), jugador.getLocation().getBlockZ());
        if (indice == null) {
            jugador.sendMessage("§eNo estás en una parcela.");
            return;
        }
        comprar(jugador, indice);
    }

    void ponerEnVenta(Player jugador, String clave, int precio) {
        Indice indice = parse(clave);
        if (indice == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null || parcelaBloqueada(jugador, indice.clave())) {
            return;
        }
        if (precio < precioMin || precio > precioMax) {
            jugador.sendMessage("§cEl precio tiene que estar entre " + precioMin + " y " + precioMax + " duros.");
            return;
        }
        parcela.precio = precio;
        guardar();
        jugador.sendMessage("§aEn venta por " + precio + " duros. §fQuien entre puede comprarla en el panel.");
    }

    void quitarVenta(Player jugador, String clave) {
        Indice indice = parse(clave);
        if (indice != null) {
            cancelar(jugador, indice);
        }
    }

    void dejarParcela(Player jugador, String clave) {
        Indice indice = parse(clave);
        if (indice != null) {
            abandonar(jugador, indice);
        }
    }

    void ponerNombre(Player jugador, String clave, String texto) {
        Indice indice = parse(clave);
        if (indice == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        parcela.titulo = recortar(texto);
        guardar();
        jugador.sendMessage("§aEsta parcela se llama §f" + parcela.titulo);
    }

    void sumarAmigo(Player jugador, String clave, UUID otroId) {
        Indice indice = parse(clave);
        if (indice == null || otroId == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (otroId.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsa parcela ya es tuya.");
            return;
        }
        Player otro = Bukkit.getPlayer(otroId);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar en el barrio.");
            return;
        }
        if (!parcela.amigos.add(otroId)) {
            jugador.sendMessage("§e" + otro.getName() + " ya puede construir aquí.");
            return;
        }
        guardar();
        jugador.sendMessage("§a" + otro.getName() + " puede construir en esta parcela.");
        jugador.sendMessage("§7Para el hueco entre dos tuyas, invítalo en las dos. Si la de al lado es suya, unirla pide que aceptéis los dos.");
        otro.sendMessage("§a" + jugador.getName() + " te deja construir en su parcela.");
    }

    void restarAmigo(Player jugador, String clave, UUID otroId) {
        Indice indice = parse(clave);
        if (indice == null || otroId == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null) {
            return;
        }
        if (!parcela.amigos.remove(otroId)) {
            jugador.sendMessage("§eEsa persona ya no construye aquí.");
            return;
        }
        guardar();
        jugador.sendMessage("§e" + nombreDe(otroId) + " ya no construye aquí.");
        Player otro = Bukkit.getPlayer(otroId);
        if (otro != null) {
            otro.sendMessage("§e" + jugador.getName() + " ya no te deja construir en su parcela.");
        }
    }

    void hacerOferta(Player jugador, String clave, UUID destino, int precio) {
        Indice indice = parse(clave);
        if (indice == null || destino == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null || parcelaBloqueada(jugador, clave)) {
            return;
        }
        if (destino.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cNo puedes ofrecértela a ti.");
            return;
        }
        if (precio < precioMin || precio > precioMax) {
            jugador.sendMessage("§cEl precio tiene que estar entre " + precioMin + " y " + precioMax + " duros.");
            return;
        }
        Player otro = Bukkit.getPlayer(destino);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar conectada.");
            return;
        }
        Oferta oferta = new Oferta();
        oferta.clave = clave;
        oferta.vendedor = jugador.getUniqueId();
        oferta.precio = precio;
        ofertas.put(destino, oferta);
        otro.sendMessage("§6" + jugador.getName() + " §fte ofrece esta parcela por §e" + precio + " duros§f. §e/parcela aceptar");
        jugador.sendMessage("§aOferta enviada a " + otro.getName() + ".");
        panel.pedirOferta(otro, jugador.getName(), tituloParcela(clave), precio);
    }

    void aceptarOferta(Player jugador) {
        aceptar(jugador);
    }

    void rechazarOferta(Player jugador) {
        Oferta oferta = ofertas.remove(jugador.getUniqueId());
        if (oferta == null) {
            jugador.sendMessage("§cNadie te ha ofrecido una parcela.");
            return;
        }
        jugador.sendMessage("§eOferta rechazada.");
        Player vendedor = Bukkit.getPlayer(oferta.vendedor);
        if (vendedor != null) {
            vendedor.sendMessage("§e" + jugador.getName() + " no acepta la parcela.");
        }
    }

    void pedirUnion(Player jugador, String mia, String suya) {
        Indice a = parse(mia);
        Indice b = parse(suya);
        if (a == null || b == null) {
            return;
        }
        Parcela pa = propia(jugador, a);
        if (pa == null) {
            return;
        }
        Parcela pb = parcelas.get(suya);
        if (pb == null) {
            jugador.sendMessage("§cEsa parcela ya no tiene dueño.");
            return;
        }
        if (pb.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§eEsa también es tuya. El camino estrecho ya se une al comprarla.");
            return;
        }
        if (Enlace.par(cuadricula, a.i, a.j, b.i, b.j) == null) {
            jugador.sendMessage("§cEsas parcelas no están pegadas. El spawn y los pasillos anchos no se unen.");
            return;
        }
        if (pa.unidas.contains(suya) && pb.unidas.contains(mia)) {
            jugador.sendMessage("§eEse camino ya está unido.");
            return;
        }
        Player otro = Bukkit.getPlayer(pb.dueno);
        if (otro == null) {
            jugador.sendMessage("§c" + pb.nombre + " tiene que estar en el barrio para aceptar.");
            return;
        }
        long vence = System.currentTimeMillis() + 60_000L;
        propuestas.put(otro.getUniqueId(), new Propuesta(jugador.getUniqueId(), otro.getUniqueId(), mia, suya, vence));
        jugador.sendMessage("§aSe lo has pedido a " + otro.getName() + ". §fTiene un minuto para aceptar.");
        otro.sendMessage("§6" + jugador.getName() + " §fquiere unir el camino con tu parcela.");
        panel.pedirUnion(otro, jugador.getName(), tituloParcela(mia));
        Bukkit.getScheduler().runTaskLater(this, () -> caducarUnion(otro.getUniqueId(), vence), 20L * 60L);
    }

    void aceptarUnion(Player jugador) {
        Propuesta propuesta = propuestas.remove(jugador.getUniqueId());
        if (propuesta == null || System.currentTimeMillis() > propuesta.vence || !propuesta.para.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsa unión ya no vale.");
            return;
        }
        Parcela mia = parcelas.get(propuesta.mia);
        Parcela suya = parcelas.get(propuesta.suya);
        if (mia == null || suya == null || !mia.dueno.equals(propuesta.de) || !suya.dueno.equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsa unión ya no vale.");
            return;
        }
        Indice a = parse(propuesta.mia);
        Indice b = parse(propuesta.suya);
        if (a == null || b == null || Enlace.par(cuadricula, a.i, a.j, b.i, b.j) == null) {
            jugador.sendMessage("§cEsas parcelas no están pegadas.");
            return;
        }
        mia.unidas.add(propuesta.suya);
        suya.unidas.add(propuesta.mia);
        guardar();
        hierbaEntre(a, b);
        jugador.sendMessage("§aCamino unido con " + mia.nombre + ". §fEl hueco estrecho entre las dos ya es hierba.");
        Player de = Bukkit.getPlayer(propuesta.de);
        if (de != null) {
            de.sendMessage("§a" + jugador.getName() + " acepta. §fEl hueco estrecho entre las dos ya es hierba.");
        }
    }

    void rechazarUnion(Player jugador) {
        Propuesta propuesta = propuestas.remove(jugador.getUniqueId());
        if (propuesta == null) {
            jugador.sendMessage("§cNadie te ha pedido unir un camino.");
            return;
        }
        jugador.sendMessage("§eNo unes el camino.");
        Player de = Bukkit.getPlayer(propuesta.de);
        if (de != null) {
            de.sendMessage("§e" + jugador.getName() + " no une el camino.");
        }
    }

    void separarUnion(Player jugador, String mia, String suya) {
        Indice a = parse(mia);
        if (a == null) {
            return;
        }
        Parcela pa = propia(jugador, a);
        Parcela pb = parcelas.get(suya);
        if (pa == null) {
            return;
        }
        if (pb == null || !pa.unidas.contains(suya)) {
            jugador.sendMessage("§eEse camino ya no está unido.");
            return;
        }
        pa.unidas.remove(suya);
        pb.unidas.remove(mia);
        guardar();
        jugador.sendMessage("§eCamino separado. §fLo construido se queda. El hueco vuelve al barrio.");
        if (!jugador.getUniqueId().equals(pb.dueno)) {
            Player otro = Bukkit.getPlayer(pb.dueno);
            if (otro != null) {
                otro.sendMessage("§e" + jugador.getName() + " separa el camino. §fLo construido se queda.");
            }
        }
    }

    /** Cambia el dueño y el color del borde. No borra construcciones ni toca el spawn. */
    void venderBanco(Player jugador, String clave) {
        Indice indice = parse(clave);
        if (indice == null) {
            return;
        }
        Parcela parcela = propia(jugador, indice);
        if (parcela == null || parcelaBloqueada(jugador, indice.clave())) {
            return;
        }
        int pago = Cuentas.recompra(baseDe(clave));
        if (pago <= 0) {
            jugador.sendMessage("§cEl banco no compra esta parcela.");
            return;
        }
        if (banco.cajaActual() < pago) {
            jugador.sendMessage("§cLa caja tiene " + banco.cajaActual() + " duros. Esta venta pide " + pago + ". La parcela sigue tuya.");
            return;
        }
        if (!banco.comprarParcela(jugador, pago)) {
            jugador.sendMessage("§cEl banco no ha podido pagarte. La parcela sigue tuya.");
            return;
        }
        if (aldeanos != null) {
            aldeanos.devolverCasa(jugador, indice.clave());
        }
        recogerBanderas(indice);
        boolean apuntado = pagadoApuntado(clave);
        quitarUniones(indice, jugador.getUniqueId());
        UUID anterior = parcela.dueno;
        pintarBorde(indice, Material.YELLOW_CONCRETE);
        parcelas.remove(indice.clave());
        guardar();
        String extra = apuntado ? "" : " §7Se toma la primera, " + precioLibre + " duros.";
        jugador.sendMessage("§aEl banco compra la parcela por " + pago + " duros. §fLo construido se queda. El borde vuelve a amarillo." + extra);
        soltarCaminos(null, anterior, indice);
    }

    private void unirMandato(Player jugador, Indice indice, String[] args) {
        if (propia(jugador, indice) == null) {
            return;
        }
        if (args.length < 2) {
            jugador.sendMessage("§e/parcela unir <nombre>§f, o el panel.");
            return;
        }
        Player otro = Bukkit.getPlayerExact(args[1]);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar en el barrio.");
            return;
        }
        List<Opcion> opciones = new ArrayList<>();
        for (Opcion opcion : paraUnir(indice.clave())) {
            if (otro.getUniqueId().equals(opcion.id)) {
                opciones.add(opcion);
            }
        }
        if (opciones.isEmpty()) {
            jugador.sendMessage("§c" + otro.getName() + " no tiene una parcela pegada a esta.");
            return;
        }
        if (opciones.size() > 1) {
            jugador.sendMessage("§eTiene varias pegadas. Elige en el panel.");
            panel.abrirUnir(jugador, indice.clave());
            return;
        }
        pedirUnion(jugador, opciones.get(0).clave, opciones.get(0).otra);
    }

    private void separarMandato(Player jugador, Indice indice) {
        if (propia(jugador, indice) == null) {
            return;
        }
        List<Opcion> unidas = unidasCon(indice.clave());
        if (unidas.isEmpty()) {
            jugador.sendMessage("§eEsta parcela no tiene un camino unido.");
            return;
        }
        if (unidas.size() > 1) {
            jugador.sendMessage("§eHay varias. Elige en el panel.");
            panel.abrirSeparar(jugador, indice.clave());
            return;
        }
        separarUnion(jugador, unidas.get(0).clave, unidas.get(0).otra);
    }

    private int baseDe(String clave) {
        Parcela parcela = parcelas.get(clave);
        if (parcela == null || parcela.pagado == null || parcela.pagado <= 0) {
            return precioLibre;
        }
        return parcela.pagado;
    }

    private void quitarUniones(Indice indice, UUID callar) {
        Parcela parcela = parcelas.get(indice.clave());
        if (parcela == null || parcela.unidas.isEmpty()) {
            return;
        }
        List<String> copia = new ArrayList<>(parcela.unidas);
        parcela.unidas.clear();
        for (String otra : copia) {
            Parcela vecina = parcelas.get(otra);
            if (vecina == null) {
                continue;
            }
            vecina.unidas.remove(indice.clave());
            if (callar != null && callar.equals(vecina.dueno)) {
                continue;
            }
            Player dueno = Bukkit.getPlayer(vecina.dueno);
            if (dueno != null) {
                dueno.sendMessage("§e" + parcela.nombre + " ya no une el camino con tu parcela. §fLo construido se queda.");
            }
        }
    }

    private void caducarUnion(UUID para, long vence) {
        Propuesta propuesta = propuestas.get(para);
        if (propuesta == null || propuesta.vence != vence) {
            return;
        }
        propuestas.remove(para);
        Player quien = Bukkit.getPlayer(para);
        if (quien != null) {
            quien.sendMessage("§eLa unión del camino ha caducado.");
        }
        Player de = Bukkit.getPlayer(propuesta.de);
        if (de != null) {
            de.sendMessage("§eNo han aceptado la unión a tiempo.");
        }
    }

    private void podarUniones() {
        boolean cambio = false;
        for (Map.Entry<String, Parcela> entrada : parcelas.entrySet()) {
            Indice aqui = parse(entrada.getKey());
            Iterator<String> iter = entrada.getValue().unidas.iterator();
            while (iter.hasNext()) {
                String otra = iter.next();
                Indice alla = parse(otra);
                Parcela vecina = parcelas.get(otra);
                boolean vale = aqui != null && alla != null && vecina != null
                    && vecina.unidas.contains(entrada.getKey())
                    && Enlace.par(cuadricula, aqui.i, aqui.j, alla.i, alla.j) != null;
                if (!vale) {
                    iter.remove();
                    cambio = true;
                }
            }
        }
        if (cambio) {
            guardar();
        }
    }

    private Set<String> paresUnidos(List<String> claves) {
        Set<String> pares = new HashSet<>();
        for (int i = 0; i < claves.size(); i++) {
            for (int j = i + 1; j < claves.size(); j++) {
                if (!unidasMutuas(claves.get(i), claves.get(j))) {
                    continue;
                }
                String par = Enlace.canon(claves.get(i), claves.get(j));
                if (par != null) {
                    pares.add(par);
                }
            }
        }
        return pares;
    }

    private boolean unidasMutuas(String a, String b) {
        Parcela pa = parcelas.get(a);
        Parcela pb = parcelas.get(b);
        return pa != null && pb != null && pa.unidas.contains(b) && pb.unidas.contains(a);
    }

    private boolean poseeAlguna(UUID id, List<Indice> lados) {
        for (Indice lado : lados) {
            Parcela parcela = parcelas.get(lado.clave());
            if (parcela != null && id.equals(parcela.dueno)) {
                return true;
            }
        }
        return false;
    }

    private Indice parse(String texto) {
        if (texto == null || texto.indexOf(',') < 0) {
            return null;
        }
        try {
            return clave(texto);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final class Indice {
        final int i;
        final int j;

        Indice(int i, int j) {
            this.i = i;
            this.j = j;
        }

        String clave() {
            return i + "," + j;
        }
    }

    private static final class Parcela {
        UUID dueno;
        String nombre;
        String titulo;
        Integer precio;
        Integer pagado;
        final Set<UUID> amigos = new HashSet<>();
        final Set<String> unidas = new HashSet<>();
    }

    private static final class Oferta {
        String clave;
        UUID vendedor;
        int precio;
    }

    private static final class Propuesta {
        final UUID de;
        final UUID para;
        final String mia;
        final String suya;
        final long vence;

        Propuesta(UUID de, UUID para, String mia, String suya, long vence) {
            this.de = de;
            this.para = para;
            this.mia = mia;
            this.suya = suya;
            this.vence = vence;
        }
    }

    static final class Opcion {
        final String clave;
        final String otra;
        final UUID id;
        final String nombre;
        final String ayuda;

        Opcion(String clave, String otra, UUID id, String nombre, String ayuda) {
            this.clave = clave;
            this.otra = otra;
            this.id = id;
            this.nombre = nombre;
            this.ayuda = ayuda;
        }
    }
}
