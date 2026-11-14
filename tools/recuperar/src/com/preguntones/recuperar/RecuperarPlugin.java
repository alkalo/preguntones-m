package com.preguntones.recuperar;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import fr.xephi.authme.events.RestoreSessionEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RecuperarPlugin extends JavaPlugin implements Listener, TabCompleter {

    private Cuentas cuentas;
    private Puerta puerta;
    private Invitacion invitacion;
    private final Set<UUID> avisados = new HashSet<>();
    private final Set<String> cambiando = new HashSet<>();
    private final Set<String> pendientes = new LinkedHashSet<>();
    private final Set<String> avisoPendiente = new HashSet<>();
    private final Map<String, Long> proximoIntento = new HashMap<>();
    private BukkitTask pulso;

    @Override
    public void onEnable() {
        try {
            cuentas = new Cuentas(new File(getDataFolder(), "codigos.yml"));
        } catch (RuntimeException e) {
            getLogger().severe("No pude leer los códigos. El plugin no arranca, para no perderlos.");
            setEnabled(false);
            return;
        }
        if (getCommand("recuperar") != null) {
            getCommand("recuperar").setExecutor(this);
            getCommand("recuperar").setTabCompleter(this);
        }
        invitacion = new Invitacion(this);
        puerta = new Puerta(this);
        puerta.prepararSesiones();
        getServer().getPluginManager().registerEvents(this, this);
        invitacion.escuchar();
        try {
            DiscordSRV.api.subscribe(this);
        } catch (RuntimeException e) {
            getLogger().warning("No pude enganchar el enlace de Discord.");
        }
        cargarPendientes();
        if (!pendientes.isEmpty()) {
            pulso = getServer().getScheduler().runTaskTimer(this, this::pulsar, 40L, 100L);
        }
        getLogger().info("Cuentas con código: " + cuentas.cuantas());
    }

    @Override
    public void onDisable() {
        if (pulso != null) {
            pulso.cancel();
        }
        if (invitacion != null) {
            invitacion.cerrar();
        }
        try {
            DiscordSRV.api.unsubscribe(this);
        } catch (RuntimeException ignorada) {
            // Discord ya no está.
        }
    }

    @EventHandler
    public void alCortar(PlayerConnectionCloseEvent event) {
        puerta.cortar(event.getPlayerUniqueId());
    }

    @EventHandler
    public void alPreentrar(AsyncPlayerPreLoginEvent event) {
        if (event.getAddress() != null) {
            puerta.recordarIp(event.getName(), event.getAddress().getHostAddress());
        }
    }

    @EventHandler
    public void alConfigurar(AsyncPlayerConnectionConfigureEvent event) {
        puerta.atender(event.getConnection());
    }

    @EventHandler
    public void alPulsar(PlayerCustomClickEvent event) {
        if (event.getIdentifier() != null && "cancelar".equals(event.getIdentifier().value())
                && event.getCommonConnection() instanceof PlayerGameConnection juego && juego.getPlayer() != null) {
            echar(juego.getPlayer(), "Has cancelado la entrada.");
        }
        if (event.getCommonConnection() instanceof PlayerConfigurationConnection conexion) {
            puerta.alPulsar(event.getIdentifier(), event.getDialogResponseView(), conexion);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alIntentarEntrar(PlayerLoginEvent event) {
        String motivo = puerta.rechazo(event.getPlayer().getName());
        if (motivo != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(motivo));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void alUnirse(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        String motivo = puerta.rechazo(jugador.getName());
        if (motivo != null) {
            echar(jugador, motivo);
            return;
        }
        if (puerta.consumirEntrada(jugador.getName())) {
            try {
                AuthMeApi.getInstance().forceLogin(jugador);
            } catch (RuntimeException e) {
                getLogger().warning("No pude abrir la sesión de " + jugador.getName());
            }
            if (puerta.consumirAviso(jugador.getName())) {
                jugador.sendMessage("§aEl código de recuperación está en tu Discord.");
            }
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!jugador.isOnline()) {
                return;
            }
            if (AuthMeApi.getInstance().isAuthenticated(jugador)) {
                entregarCodigo(jugador);
            } else {
                avisoOlvido(jugador);
            }
        }, 40L);
    }

    @EventHandler
    public void alEntrar(LoginEvent event) {
        programarCodigo(event.getPlayer());
    }

    @EventHandler
    public void alRegistrar(RegisterEvent event) {
        if (event.getPlayer() != null) {
            programarCodigo(event.getPlayer());
        }
    }

    @EventHandler
    public void alSesion(RestoreSessionEvent event) {
        if (!event.isCancelled()) {
            programarCodigo(event.getPlayer());
        }
    }

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void alVincular(AccountLinkedEvent event) {
        if (event.getPlayer() == null || event.getUser() == null) {
            return;
        }
        puerta.conectado(event.getPlayer().getUniqueId());
        String nombre = event.getPlayer().getName();
        if (nombre == null) {
            return;
        }
        String discord = event.getUser().getId();
        getServer().getScheduler().runTask(this, () -> {
            Player jugador = getServer().getPlayerExact(nombre);
            if (jugador == null) {
                return;
            }
            if (cuentas.entregado(nombre)) {
                decir(jugador, "§aDiscord vinculado. Si perdiste el código: §f/recuperar nuevo");
                return;
            }
            enviarCodigo(jugador, nombre, discord, cuentas.tiene(nombre), false, false);
        });
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        avisados.remove(event.getPlayer().getUniqueId());
    }

    private void programarCodigo(Player jugador) {
        getServer().getScheduler().runTaskLater(this, () -> entregarCodigo(jugador), 20L);
    }

    private void entregarCodigo(Player jugador) {
        if (!jugador.isOnline() || !AuthMeApi.getInstance().isAuthenticated(jugador)) {
            return;
        }
        String discord = Correo.discordDe(jugador.getUniqueId());
        if (discord == null) {
            if (avisados.add(jugador.getUniqueId())) {
                jugador.sendMessage("§eVincula Discord en la ventana de entrada. Sal y vuelve a entrar.");
            }
            return;
        }
        if (cuentas.entregado(jugador.getName()) || cambiando.contains(clave(jugador.getName())) || pendiente(jugador.getName())) {
            return;
        }
        enviarCodigo(jugador, jugador.getName(), discord, cuentas.tiene(jugador.getName()), false, false);
    }

    private void avisoOlvido(Player jugador) {
        if (!AuthMeApi.getInstance().isRegistered(jugador.getName())) {
            return;
        }
        if (!avisados.add(jugador.getUniqueId())) {
            return;
        }
        jugador.sendMessage("§eLa ventana de entrada no llegó a abrirse. Sal y vuelve a entrar.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String etiqueta, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Entra al servidor para usar esto.");
            return true;
        }
        Pedido pedido = Pedido.leer(args);
        switch (pedido.tipo) {
            case AYUDA -> ayuda(jugador);
            case NUEVO -> nuevo(jugador);
            case CAMBIAR -> cambiar(jugador, pedido);
            case USO -> jugador.sendMessage("§eUso: §f/recuperar nuevo §eo §f/recuperar código clave clave");
            default -> jugador.sendMessage("§eUso: §f/recuperar nuevo §eo §f/recuperar código clave clave");
        }
        return true;
    }

    private void ayuda(Player jugador) {
        if (AuthMeApi.getInstance().isAuthenticated(jugador)) {
            jugador.sendMessage("§fEl código de recuperación llega por privado a Discord.");
            jugador.sendMessage("§fSi lo perdiste: §e/recuperar nuevo");
            jugador.sendMessage("§7El anterior deja de valer.");
            return;
        }
        jugador.sendMessage("§fSi recuerdas la contraseña: §e/login contraseña");
        jugador.sendMessage("§fSi no: §e/recuperar nuevo");
        jugador.sendMessage("§7El código llega por privado a tu Discord. Luego: §f/recuperar código clave clave");
        jugador.sendMessage("§7Si todavía no vinculaste Discord: §f/discord link");
    }

    private void nuevo(Player jugador) {
        String nombre = jugador.getName();
        String id = clave(nombre);
        if (cambiando.contains(id)) {
            jugador.sendMessage("§7Espera un momento.");
            return;
        }
        String discord = Correo.discordDe(jugador.getUniqueId());
        if (discord == null) {
            jugador.sendMessage("§cVincula Discord para poder recuperar la contraseña.");
            jugador.sendMessage("§fEscribe §e/discord link §fy sigue lo que te diga. El código llega por privado.");
            return;
        }
        String bloqueo = cuentas.esperaReenvio(nombre, System.currentTimeMillis());
        if (bloqueo != null) {
            jugador.sendMessage("§c" + bloqueo);
            return;
        }
        boolean dentro = AuthMeApi.getInstance().isAuthenticated(jugador);
        enviarCodigo(jugador, nombre, discord, cuentas.tiene(nombre), !dentro, false);
    }

    private void cambiar(Player jugador, Pedido pedido) {
        String nombre = jugador.getName();
        String id = clave(nombre);
        if (cambiando.contains(id)) {
            jugador.sendMessage("§7Espera un momento.");
            return;
        }
        if (!AuthMeApi.getInstance().isRegistered(nombre)) {
            jugador.sendMessage("§cEsta cuenta aún no tiene contraseña. Créala con §f/register clave clave");
            return;
        }
        if (!cuentas.tiene(nombre)) {
            jugador.sendMessage("§cEsta cuenta no tiene código. Escribe §f/recuperar nuevo §csi ya vinculaste Discord.");
            return;
        }
        long ahora = System.currentTimeMillis();
        String bloqueo = cuentas.espera(nombre, ahora);
        if (bloqueo != null) {
            jugador.sendMessage("§c" + bloqueo);
            return;
        }
        String codigo = Codigo.normalizar(pedido.codigo);
        if (codigo == null) {
            jugador.sendMessage("§cEse no parece un código. Tiene esta forma: §fABCD-EFGH");
            return;
        }
        if (!cuentas.coincide(nombre, codigo)) {
            jugador.sendMessage("§c" + cuentas.fallar(nombre, ahora));
            return;
        }
        if (!pedido.clave.equals(pedido.repetida)) {
            jugador.sendMessage("§cLas dos contraseñas no coinciden.");
            return;
        }
        String rechazo = Politica.rechazo(nombre, pedido.clave, codigo);
        if (rechazo != null) {
            jugador.sendMessage("§c" + rechazo);
            return;
        }
        cuentas.acertar(nombre);
        cambiando.add(id);
        boolean estabaDentro = AuthMeApi.getInstance().isAuthenticated(jugador);
        try {
            AuthMeApi.getInstance().changePassword(nombre, pedido.clave);
        } catch (RuntimeException e) {
            cambiando.remove(id);
            getLogger().warning("AuthMe no aceptó el cambio de contraseña de " + nombre);
            jugador.sendMessage("§cNo he podido cambiar la contraseña. Prueba otra vez dentro de un momento. El código sigue valiendo.");
            return;
        }
        jugador.sendMessage("§7Un momento, estoy cambiando la contraseña.");
        comprobar(jugador, nombre, pedido.clave, estabaDentro, 0);
    }

    private void comprobar(Player jugador, String nombre, String nueva, boolean estabaDentro, int intento) {
        long espera = intento == 0 ? 10L : 20L;
        getServer().getScheduler().runTaskLater(this, () -> {
            boolean vale = false;
            try {
                vale = AuthMeApi.getInstance().checkPassword(nombre, nueva);
            } catch (RuntimeException e) {
                getLogger().warning("No pude comprobar el cambio de contraseña de " + nombre);
            }
            if (vale) {
                cambiando.remove(clave(nombre));
                if (!jugador.isOnline()) {
                    try {
                        cuentas.borrar(nombre);
                    } catch (RuntimeException e) {
                        getLogger().warning("No pude retirar el código usado de " + nombre);
                    }
                    return;
                }
                try {
                    cuentas.borrar(nombre);
                } catch (RuntimeException e) {
                    getLogger().warning("No pude retirar el código usado de " + nombre);
                }
                boolean entro = estabaDentro;
                if (!estabaDentro) {
                    try {
                        AuthMeApi.getInstance().forceLogin(jugador);
                        entro = true;
                    } catch (RuntimeException e) {
                        getLogger().warning("No pude abrir la sesión de " + nombre + " tras cambiar la contraseña");
                    }
                }
                if (entro) {
                    jugador.sendMessage("§aContraseña cambiada.");
                } else {
                    jugador.sendMessage("§aContraseña cambiada. Entra con §f/login §ay la nueva.");
                }
                String discord = Correo.discordDe(jugador.getUniqueId());
                if (discord == null) {
                    jugador.sendMessage("§eVincula Discord con §f/discord link §ey luego escribe §f/recuperar nuevo§e.");
                    jugador.sendMessage("§7El código que acabas de usar ya no vale.");
                    return;
                }
                enviarCodigo(jugador, nombre, discord, true, false, false);
                return;
            }
            if (intento < 4) {
                comprobar(jugador, nombre, nueva, estabaDentro, intento + 1);
                return;
            }
            cambiando.remove(clave(nombre));
            if (jugador.isOnline()) {
                jugador.sendMessage("§cNo he podido usar esa contraseña. Tiene que ser distinta de tu nombre, de 5 a 30 caracteres, y no una clave muy fácil. El código sigue valiendo.");
            }
        }, espera);
    }

    private void enviarCodigo(Player jugador, String nombre, String discordId, boolean sustituye, boolean desdePuerta, boolean pendiente) {
        String id = clave(nombre);
        if (!cambiando.add(id)) {
            if (!pendiente) {
                decir(jugador, "§7Espera un momento.");
            }
            return;
        }
        if (!Correo.listo()) {
            cambiando.remove(id);
            decir(jugador, "§eDiscord todavía no está conectado. Espera un momento y vuelve a escribirlo.");
            return;
        }
        Cuentas.Borrador borrador = cuentas.preparar();
        String texto = Aviso.texto(nombre, Codigo.mostrar(borrador.codigo), sustituye);
        Correo.enviar(discordId, texto, () -> getServer().getScheduler().runTask(this, () -> {
            try {
                long reenvio = System.currentTimeMillis() + (desdePuerta ? Cuentas.REENVIO_PUERTA_MS : Cuentas.REENVIO_DENTRO_MS);
                cuentas.confirmar(nombre, borrador, reenvio);
            } catch (RuntimeException e) {
                cambiando.remove(id);
                getLogger().warning("No pude guardar el código de " + nombre + " después de enviarlo.");
                decir(jugador, "§cEl mensaje salió, pero no he podido guardarlo. Escribe §f/recuperar nuevo §cotra vez.");
                if (pendiente) {
                    proximoIntento.put(id, System.currentTimeMillis() + 120_000L);
                }
                return;
            }
            cambiando.remove(id);
            if (pendiente) {
                pendientes.removeIf(item -> item.equalsIgnoreCase(nombre));
                proximoIntento.remove(id);
                avisoPendiente.remove(id);
                guardarPendientes();
            }
            getLogger().info("Código de " + nombre + " enviado por Discord.");
            avisarEnJuego(jugador, sustituye, desdePuerta);
        }), error -> getServer().getScheduler().runTask(this, () -> {
            cambiando.remove(id);
            String porque = Correo.explicar(error);
            getLogger().warning("No pude enviar el privado de " + nombre + ": " + porque);
            decir(jugador, "§c" + porque);
            if (pendiente) {
                proximoIntento.put(id, System.currentTimeMillis() + 120_000L);
            }
        }));
    }

    private void avisarEnJuego(Player jugador, boolean sustituye, boolean desdePuerta) {
        if (jugador == null || !jugador.isOnline()) {
            return;
        }
        jugador.showTitle(Title.title(
                Component.text("Míralo en Discord", NamedTextColor.GOLD),
                Component.text("Mensaje privado", NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(6), Duration.ofMillis(600))));
        if (jugador.getWorld() != null) {
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.4f);
        }
        jugador.sendMessage("§aTe he enviado el código por privado en Discord.");
        jugador.sendMessage("§fÁbrelo y guárdalo. Aquí no se enseña.");
        if (sustituye) {
            jugador.sendMessage("§eEl código anterior ya no vale.");
        }
        if (desdePuerta || !AuthMeApi.getInstance().isAuthenticated(jugador)) {
            jugador.sendMessage(Component.text("/recuperar ", NamedTextColor.YELLOW)
                    .append(Component.text("código clave clave", NamedTextColor.YELLOW))
                    .clickEvent(ClickEvent.suggestCommand("/recuperar "))
                    .hoverEvent(HoverEvent.showText(Component.text("Pulsa para preparar el comando. El código está en Discord."))));
            jugador.sendMessage("§7Las dos últimas palabras son la contraseña nueva, repetida.");
        }
    }

    private void pulsar() {
        if (pendientes.isEmpty()) {
            if (pulso != null) {
                pulso.cancel();
                pulso = null;
            }
            return;
        }
        if (!Correo.listo()) {
            return;
        }
        long ahora = System.currentTimeMillis();
        for (String nombre : List.copyOf(pendientes)) {
            String id = clave(nombre);
            Long proximo = proximoIntento.get(id);
            if (proximo != null && ahora < proximo) {
                continue;
            }
            if (cambiando.contains(id)) {
                continue;
            }
            UUID uuid = getServer().getOfflinePlayer(nombre).getUniqueId();
            String discord = Correo.discordDe(uuid);
            if (discord == null) {
                if (avisoPendiente.add(id)) {
                    getLogger().warning(nombre + " no tiene Discord vinculado. El código nuevo espera a que lo vincule.");
                }
                proximoIntento.put(id, ahora + 120_000L);
                continue;
            }
            Player jugador = getServer().getPlayerExact(nombre);
            enviarCodigo(jugador, nombre, discord, cuentas.tiene(nombre), true, true);
        }
    }

    private void cargarPendientes() {
        File archivo = new File(getDataFolder(), "reenviar.yml");
        if (!archivo.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        for (String nombre : yaml.getStringList("nombres")) {
            if (nombre != null && !nombre.isBlank()) {
                pendientes.add(nombre);
            }
        }
    }

    private void guardarPendientes() {
        File archivo = new File(getDataFolder(), "reenviar.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("nombres", new ArrayList<>(pendientes));
        try {
            yaml.save(archivo);
        } catch (IOException e) {
            getLogger().warning("No pude guardar la lista de reenvío.");
        }
    }

    Cuentas cuentas() {
        return cuentas;
    }

    Invitacion invitacion() {
        return invitacion;
    }

    Puerta puerta() {
        return puerta;
    }

    void encolar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        pendientes.add(nombre);
        guardarPendientes();
        if (pulso == null || pulso.isCancelled()) {
            pulso = getServer().getScheduler().runTaskTimer(this, this::pulsar, 40L, 100L);
        }
    }

    private boolean pendiente(String nombre) {
        for (String item : pendientes) {
            if (item.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private void echar(Player jugador, String motivo) {
        puerta.cancelar(jugador.getName());
        try {
            AuthMeApi.getInstance().forceLogout(jugador);
        } catch (RuntimeException ignorada) {
            // No había sesión.
        }
        jugador.kick(Component.text(motivo));
    }

    private void decir(Player jugador, String mensaje) {
        if (jugador != null && jugador.isOnline()) {
            jugador.sendMessage(mensaje);
        }
    }

    private static String clave(String nombre) {
        return nombre.toLowerCase(Locale.ROOT);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String etiqueta, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String escrito = args[0].toLowerCase(Locale.ROOT);
        List<String> opciones = new ArrayList<>();
        if ("nuevo".startsWith(escrito)) {
            opciones.add("nuevo");
        }
        if ("discord".startsWith(escrito)) {
            opciones.add("discord");
        }
        return opciones;
    }
}
