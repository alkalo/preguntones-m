package com.preguntones.parcelas;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

final class Banco implements Listener {
    private static final int CAJA_INICIAL = 150;
    private static final String CAMPO_DUROS = "duros";
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es-ES")).withZone(MADRID);
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor ORO = TextColor.color(0xE6B84D);
    private static final TextColor SI = TextColor.color(0x8FE3A0);
    private static final TextColor NO = TextColor.color(0xE07A7A);

    private final ParcelasPlugin plugin;
    private final File archivo;
    private final Map<UUID, Prestamo> prestamos = new HashMap<>();
    private final Map<UUID, Borrador> borradores = new HashMap<>();
    private final Map<UUID, Integer> vistos = new HashMap<>();
    private final Map<UUID, List<String>> avisos = new HashMap<>();
    private int caja;
    private boolean cajaLista;

    Banco(ParcelasPlugin plugin) {
        this.plugin = plugin;
        archivo = new File(plugin.getDataFolder(), "banco.yml");
        cargar();
    }

    void arrancar() {
        Bukkit.getScheduler().runTaskLater(plugin, this::vencerTodos, 60L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::vigilar, 40L, 40L);
    }

    boolean orden(Player jugador, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("condonar")) {
            if (!jugador.isOp() && !jugador.hasPermission("parcelas.admin")) {
                jugador.sendMessage("§cNo puedes condonar.");
                return true;
            }
            condonar(jugador, args[1]);
            return true;
        }
        abrir(jugador);
        return true;
    }

    boolean enMora(UUID id) {
        Prestamo prestamo = prestamos.get(id);
        return prestamo != null && prestamo.mora;
    }

    boolean avalada(String parcela) {
        if (parcela == null) {
            return false;
        }
        for (Prestamo prestamo : prestamos.values()) {
            if (parcela.equals(prestamo.parcela) && !prestamo.mora) {
                return true;
            }
        }
        return false;
    }

    boolean ingresar(OfflinePlayer quien, int cantidad) {
        if (cantidad <= 0 || quien == null) {
            return true;
        }
        Prestamo prestamo = prestamos.get(quien.getUniqueId());
        int queda = cantidad;
        if (prestamo != null && prestamo.mora && prestamo.restante > 0) {
            int toma = Math.min(queda, prestamo.restante);
            aplicar(prestamo, toma);
            queda -= toma;
            if (prestamo.restante <= 0) {
                prestamos.remove(quien.getUniqueId());
                avisar(quien.getUniqueId(), "§aHas saldado al banco.");
            }
            guardar();
        }
        if (queda <= 0) {
            mirar(quien.getUniqueId());
            return true;
        }
        if (!plugin.abonar(quien, queda)) {
            return false;
        }
        mirar(quien.getUniqueId());
        return true;
    }

    void sumarCaja(int cantidad) {
        if (cantidad <= 0) {
            return;
        }
        caja += cantidad;
        guardar();
    }

    int cajaActual() {
        return caja;
    }

    /** Paga la recompra desde la caja. Si el ingreso falla, la caja vuelve a donde estaba. */
    boolean comprarParcela(OfflinePlayer quien, int pago) {
        if (pago <= 0 || quien == null || caja < pago) {
            return false;
        }
        caja -= pago;
        guardar();
        if (!ingresar(quien, pago)) {
            caja += pago;
            guardar();
            return false;
        }
        plugin.getLogger().info(quien.getName() + " vende una parcela al banco por " + pago + ". Caja: " + caja);
        return true;
    }

    private void abrir(Player jugador) {
        borradores.remove(jugador.getUniqueId());
        Prestamo prestamo = prestamos.get(jugador.getUniqueId());
        String cuerpo = prestamo == null
            ? "Tienes " + plugin.duros(jugador) + " duros. La caja del barrio tiene " + caja + "."
            : "Debes " + prestamo.restante + " duros. El plazo acaba el " + fecha(prestamo.vence) + ". Tienes " + plugin.duros(jugador) + ".";
        List<ActionButton> botones = new ArrayList<>();
        if (prestamo == null) {
            botones.add(boton("Pedir", ORO, "Escribe cuántos duros y elige el plazo.", 160, () -> pedir(jugador)));
        } else {
            botones.add(boton("Pagar", SI, "Devuelves " + prestamo.restante + " duros, el total que aceptaste.", 160, () -> pagar(jugador)));
        }
        mostrar(jugador, "Banco", cuerpo, botones, boton("Volver", SUAVE, "Cierra el banco.", 120, null), 1);
    }

    private void pedir(Player jugador) {
        pedir(jugador, null, "");
    }

    private void pedir(Player jugador, String aviso, String previo) {
        if (prestamos.containsKey(jugador.getUniqueId())) {
            abrir(jugador);
            return;
        }
        Borrador borrador = new Borrador();
        borrador.parcelas = plugin.clavesDe(jugador.getUniqueId());
        borradores.put(jugador.getUniqueId(), borrador);
        if (caja < 1) {
            mostrar(jugador, "Banco", "La caja está vacía. Cuando alguien venda, habrá duros.", List.of(),
                boton("Volver", SUAVE, "Vuelve al banco.", 120, () -> abrir(jugador)), 1);
            return;
        }
        String cuerpo = aviso == null
            ? "La caja tiene " + caja + " duros. Escribe cuántos, de 1 a " + caja + "."
            : aviso;
        int largo = String.valueOf(caja).length();
        String inicial = previo == null ? "" : previo.trim();
        if (inicial.length() > largo) {
            inicial = inicial.substring(0, largo);
        }
        var campo = DialogInput.text(CAMPO_DUROS, Component.text("Duros"))
            .maxLength(largo)
            .width(240);
        if (!inicial.isEmpty()) {
            campo.initial(inicial);
        }
        mostrar(jugador, "Pedir", cuerpo, List.of(campo.build()),
            List.of(botonCantidad()),
            boton("Volver", SUAVE, "Vuelve al banco.", 120, () -> abrir(jugador)), 1);
    }

    private void tomarCantidad(DialogResponseView vista, Audience audiencia) {
        String escrito = campo(vista);
        ejecutar(audiencia, () -> {
            if (audiencia instanceof Player jugador) {
                aplicarCantidad(jugador, escrito);
            }
        });
    }

    private void aplicarCantidad(Player jugador, String escrito) {
        if (borradores.get(jugador.getUniqueId()) == null || prestamos.containsKey(jugador.getUniqueId())) {
            abrir(jugador);
            return;
        }
        if (caja < 1) {
            pedir(jugador, null, "");
            return;
        }
        Integer capital = entero(escrito);
        if (capital == null || capital < 1) {
            pedir(jugador, "Escribe un número entero, de 1 a " + caja + ".", escrito);
            return;
        }
        if (capital > caja) {
            pedir(jugador, "Como máximo, " + caja + " duros.", escrito);
            return;
        }
        plazo(jugador, capital);
    }

    private static Integer entero(String escrito) {
        if (escrito == null) {
            return null;
        }
        String limpio = escrito.trim();
        if (limpio.isEmpty() || limpio.length() > 9) {
            return null;
        }
        for (int i = 0; i < limpio.length(); i++) {
            char cifra = limpio.charAt(i);
            if (cifra < '0' || cifra > '9') {
                return null;
            }
        }
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String campo(DialogResponseView vista) {
        if (vista == null) {
            return "";
        }
        try {
            String valor = vista.getText(CAMPO_DUROS);
            return valor == null ? "" : valor;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void plazo(Player jugador, int capital) {
        Borrador borrador = borradores.get(jugador.getUniqueId());
        if (borrador == null) {
            abrir(jugador);
            return;
        }
        borrador.capital = capital;
        List<ActionButton> botones = List.of(
            plazoBoton(jugador, capital, 1, 10),
            plazoBoton(jugador, capital, 3, 20),
            plazoBoton(jugador, capital, 7, 30)
        );
        mostrar(jugador, "Plazo", "Pides " + capital + " duros. Cada botón dice lo que devuelves. El interés se redondea hacia arriba, y como mínimo es un duro.", botones,
            boton("Volver", SUAVE, "Vuelve a la cantidad.", 120, () -> pedir(jugador, null, String.valueOf(capital))), 1);
    }

    private ActionButton plazoBoton(Player jugador, int capital, int dias, int porcentaje) {
        int interes = Cuentas.interes(capital, porcentaje);
        int total = capital + interes;
        String dia = dias == 1 ? "1 día" : dias + " días";
        return boton(dia + " · " + total + " duros", ORO,
            "El " + porcentaje + " %. El interés son " + interes + " duros.", 220,
            () -> aval(jugador, dias, porcentaje));
    }

    private void aval(Player jugador, int dias, int porcentaje) {
        Borrador borrador = borradores.get(jugador.getUniqueId());
        if (borrador == null) {
            abrir(jugador);
            return;
        }
        borrador.dias = dias;
        borrador.porcentaje = porcentaje;
        borrador.vence = System.currentTimeMillis() + dias * 86_400_000L;
        List<ActionButton> botones = new ArrayList<>();
        if (borrador.capital <= 50) {
            botones.add(boton("Mis duros", ORO, "Si no pagas, te quitamos el saldo y lo que falte sigue debiendo.", 180,
                () -> confirmar(jugador, null)));
        }
        for (String clave : borrador.parcelas) {
            if (!plugin.esDueno(jugador.getUniqueId(), clave) || plugin.enVenta(clave) || avalada(clave)) {
                continue;
            }
            String nombre = plugin.tituloParcela(clave);
            String elegida = clave;
            botones.add(boton(nombre, ORO, "Esta parcela queda bloqueada hasta que pagues.", 180,
                () -> confirmar(jugador, elegida)));
        }
        if (botones.isEmpty()) {
            mostrar(jugador, "Aval", "Hace falta una parcela tuya, que no esté en venta.", List.of(),
                boton("Volver", SUAVE, "Vuelve al plazo.", 120, () -> plazo(jugador, borrador.capital)), 1);
            return;
        }
        mostrar(jugador, "Aval", "Pides " + borrador.capital + " duros. Elige qué se queda el banco si no pagas.", botones,
            boton("Volver", SUAVE, "Vuelve al plazo.", 120, () -> plazo(jugador, borrador.capital)), 1);
    }

    private void confirmar(Player jugador, String parcela) {
        Borrador borrador = borradores.get(jugador.getUniqueId());
        if (borrador == null) {
            abrir(jugador);
            return;
        }
        borrador.parcela = parcela;
        int interes = Cuentas.interes(borrador.capital, borrador.porcentaje);
        int total = borrador.capital + interes;
        boolean unica = parcela != null && plugin.clavesDe(jugador.getUniqueId()).size() <= 1;
        String cuerpo = textoContrato(borrador, interes, total, unica);
        Runnable aceptar = unica ? () -> confirmarUnica(jugador) : () -> conceder(jugador);
        mostrar(jugador, "Aceptar", cuerpo,
            List.of(boton("Aceptar", SI, "Sin esto no sale ningún duro.", 160, aceptar)),
            boton("Volver", SUAVE, "Vuelve al aval.", 120, () -> aval(jugador, borrador.dias, borrador.porcentaje)), 1);
    }

    private void confirmarUnica(Player jugador) {
        Borrador borrador = borradores.get(jugador.getUniqueId());
        if (borrador == null) {
            abrir(jugador);
            return;
        }
        mostrar(jugador, "Tu única parcela", "Es tu única parcela. Si no pagas, deja de ser tuya.",
            List.of(boton("Sí, esa parcela", NO, "La parcela vuelve al barrio si no pagas.", 180, () -> conceder(jugador))),
            boton("Volver", SUAVE, "No pido el préstamo.", 120, () -> abrir(jugador)), 1);
    }

    private String textoContrato(Borrador borrador, int interes, int total, boolean unica) {
        String base = "Pides " + borrador.capital + " duros. A " + borrador.dias + " días, el " + borrador.porcentaje
            + " %. El interés son " + interes + " duros. Devuelves " + total + " el " + fecha(borrador.vence) + ". ";
        if (borrador.parcela == null) {
            return base + "Si no pagas, te quitamos los duros que tengas y lo que falte sigue debiendo.";
        }
        String nombre = plugin.tituloParcela(borrador.parcela);
        String extra = unica ? " Es tu única parcela." : "";
        return base + "Aval: " + nombre + ". Si no pagas, primero se cobra de tu saldo. Si no llega, " + nombre + " vuelve al barrio." + extra;
    }

    private void conceder(Player jugador) {
        Borrador borrador = borradores.remove(jugador.getUniqueId());
        if (borrador == null || prestamos.containsKey(jugador.getUniqueId())) {
            jugador.sendMessage("§cEsa petición ya no vale.");
            return;
        }
        if (caja < borrador.capital) {
            jugador.sendMessage("§cLa caja ya no llega.");
            abrir(jugador);
            return;
        }
        if (borrador.parcela != null) {
            if (!borrador.parcelas.contains(borrador.parcela)
                || !plugin.esDueno(jugador.getUniqueId(), borrador.parcela)
                || plugin.enVenta(borrador.parcela)
                || avalada(borrador.parcela)) {
                jugador.sendMessage("§cEsa parcela ya no puede ser el aval.");
                return;
            }
        }
        caja -= borrador.capital;
        if (!plugin.abonar(jugador, borrador.capital)) {
            caja += borrador.capital;
            guardar();
            jugador.sendMessage("§cNo he podido darte los duros.");
            return;
        }
        Prestamo prestamo = new Prestamo();
        prestamo.dueno = jugador.getUniqueId();
        prestamo.capital = borrador.capital;
        prestamo.interes = Cuentas.interes(borrador.capital, borrador.porcentaje);
        prestamo.restante = prestamo.capital + prestamo.interes;
        prestamo.vence = System.currentTimeMillis() + borrador.dias * 86_400_000L;
        prestamo.parcela = borrador.parcela;
        prestamos.put(jugador.getUniqueId(), prestamo);
        vistos.put(jugador.getUniqueId(), plugin.duros(jugador));
        guardar();
        jugador.sendMessage("§aEl banco te deja " + borrador.capital + " duros. §fDevuelves " + prestamo.restante + " el " + fecha(prestamo.vence) + ".");
    }

    private void pagar(Player jugador) {
        Prestamo prestamo = prestamos.get(jugador.getUniqueId());
        if (prestamo == null) {
            jugador.sendMessage("§eNo debes nada.");
            return;
        }
        if (!plugin.cobrar(jugador, prestamo.restante)) {
            jugador.sendMessage("§cSe paga entero. Debes " + prestamo.restante + " y tienes " + plugin.duros(jugador) + ".");
            return;
        }
        aplicar(prestamo, prestamo.restante);
        String parcela = prestamo.parcela;
        prestamos.remove(jugador.getUniqueId());
        vistos.put(jugador.getUniqueId(), plugin.duros(jugador));
        guardar();
        jugador.sendMessage(parcela == null
            ? "§aHas pagado al banco."
            : "§aHas pagado al banco. §fEl aval queda libre.");
    }

    private void condonar(Player staff, String nombre) {
        OfflinePlayer quien = Bukkit.getOfflinePlayer(nombre);
        Prestamo prestamo = null;
        UUID id = null;
        if (quien.getUniqueId() != null) {
            prestamo = prestamos.get(quien.getUniqueId());
            id = quien.getUniqueId();
        }
        if (prestamo == null) {
            for (Prestamo candidato : prestamos.values()) {
                OfflinePlayer otro = Bukkit.getOfflinePlayer(candidato.dueno);
                if (nombre.equalsIgnoreCase(otro.getName())) {
                    prestamo = candidato;
                    id = candidato.dueno;
                    break;
                }
            }
        }
        if (prestamo == null || id == null) {
            staff.sendMessage("§cEsa persona no debe al banco.");
            return;
        }
        prestamos.remove(id);
        guardar();
        String texto = "§eEl banco condona la deuda. La caja no recupera ese préstamo.";
        avisar(id, texto);
        staff.sendMessage("§eDeuda condonada. La caja sigue en " + caja + " duros.");
        plugin.getLogger().info(staff.getName() + " condona el préstamo de " + nombre + ". Caja: " + caja);
    }

    private void vencerTodos() {
        for (Prestamo prestamo : new ArrayList<>(prestamos.values())) {
            if (!prestamo.mora && System.currentTimeMillis() >= prestamo.vence) {
                vencer(prestamo);
            }
        }
    }

    private void vencer(Prestamo prestamo) {
        OfflinePlayer quien = Bukkit.getOfflinePlayer(prestamo.dueno);
        int saldo = plugin.durosDe(quien);
        int toma = Math.min(saldo, prestamo.restante);
        if (toma > 0 && plugin.cobrarA(quien, toma)) {
            aplicar(prestamo, toma);
        }
        if (prestamo.restante <= 0) {
            prestamos.remove(prestamo.dueno);
            guardar();
            avisar(prestamo.dueno, "§aHas pagado al banco. El aval queda libre.");
            mirar(prestamo.dueno);
            return;
        }
        if (prestamo.parcela != null && plugin.esDueno(prestamo.dueno, prestamo.parcela)) {
            String nombre = plugin.tituloParcela(prestamo.parcela);
            plugin.venta.recogerParcela(prestamo.parcela);
            plugin.soltarAval(prestamo.parcela);
            prestamos.remove(prestamo.dueno);
            guardar();
            avisar(prestamo.dueno, "§cNo has pagado. " + nombre + " vuelve al barrio.");
            return;
        }
        prestamo.mora = true;
        prestamo.parcela = null;
        guardar();
        if (toma > 0) {
            avisar(prestamo.dueno, "§cNo has pagado. Te he quitado " + toma + " duros. Sigues debiendo " + prestamo.restante + ".");
        } else {
            avisar(prestamo.dueno, "§cNo has pagado. Sigues debiendo " + prestamo.restante + " duros.");
        }
        mirar(prestamo.dueno);
    }

    private void aplicar(Prestamo prestamo, int cantidad) {
        int aInteres = Math.min(cantidad, prestamo.interes);
        prestamo.interes -= aInteres;
        cantidad -= aInteres;
        int aCapital = Math.min(cantidad, prestamo.capital);
        prestamo.capital -= aCapital;
        if (aCapital > 0) {
            caja += aCapital;
        }
        prestamo.restante = prestamo.capital + prestamo.interes;
    }

    private void vigilar() {
        vencerTodos();
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            Prestamo prestamo = prestamos.get(jugador.getUniqueId());
            int ahora = plugin.duros(jugador);
            if (prestamo == null || !prestamo.mora) {
                vistos.put(jugador.getUniqueId(), ahora);
                continue;
            }
            int antes = vistos.getOrDefault(jugador.getUniqueId(), ahora);
            if (ahora > antes) {
                int toma = Math.min(ahora - antes, prestamo.restante);
                if (toma > 0 && plugin.cobrar(jugador, toma)) {
                    aplicar(prestamo, toma);
                    if (prestamo.restante <= 0) {
                        prestamos.remove(jugador.getUniqueId());
                        jugador.sendMessage("§aHas saldado al banco.");
                    }
                    guardar();
                }
            }
            vistos.put(jugador.getUniqueId(), plugin.duros(jugador));
        }
    }

    private void mirar(UUID id) {
        Player jugador = Bukkit.getPlayer(id);
        if (jugador != null) {
            vistos.put(id, plugin.duros(jugador));
        }
    }

    private void avisar(UUID id, String texto) {
        Player jugador = Bukkit.getPlayer(id);
        if (jugador != null) {
            jugador.sendMessage(texto);
            return;
        }
        avisos.computeIfAbsent(id, clave -> new ArrayList<>()).add(texto);
        guardar();
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        Prestamo prestamo = prestamos.get(jugador.getUniqueId());
        if (prestamo != null && !prestamo.mora && System.currentTimeMillis() >= prestamo.vence) {
            vencer(prestamo);
            prestamo = prestamos.get(jugador.getUniqueId());
        }
        if (prestamo != null && prestamo.mora) {
            int toma = Math.min(plugin.duros(jugador), prestamo.restante);
            if (toma > 0 && plugin.cobrar(jugador, toma)) {
                aplicar(prestamo, toma);
                if (prestamo.restante <= 0) {
                    prestamos.remove(jugador.getUniqueId());
                    jugador.sendMessage("§aHas saldado al banco.");
                }
                guardar();
            }
        }
        vistos.put(jugador.getUniqueId(), plugin.duros(jugador));
        List<String> pendientes = avisos.remove(jugador.getUniqueId());
        if (pendientes != null) {
            for (String texto : pendientes) {
                jugador.sendMessage(texto);
            }
            guardar();
        }
    }

    @EventHandler
    public void alMandar(PlayerCommandPreprocessEvent event) {
        if (!enMora(event.getPlayer().getUniqueId())) {
            return;
        }
        String texto = event.getMessage().toLowerCase(Locale.ROOT);
        String[] partes = texto.split(" ");
        String orden = partes[0];
        int puntos = orden.indexOf(':');
        if (puntos >= 0) {
            orden = orden.substring(puntos + 1);
        }
        if (orden.equals("/pay") || orden.equals("/pagar")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDebes al banco. No puedes pasar duros hasta saldar.");
        }
    }

    private String fecha(long instante) {
        return FECHA.format(Instant.ofEpochMilli(instante));
    }

    private void mostrar(Player jugador, String titulo, String cuerpo, List<ActionButton> botones, ActionButton volver, int columnas) {
        mostrar(jugador, titulo, cuerpo, List.of(), botones, volver, columnas);
    }

    private void mostrar(Player jugador, String titulo, String cuerpo, List<DialogInput> campos, List<ActionButton> botones, ActionButton volver, int columnas) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(false)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, SUAVE), 320)))
                .inputs(campos)
                .build())
            .type(DialogType.multiAction(botones, volver, columnas))
        );
        ((Audience) jugador).showDialog(dialog);
    }

    private ActionButton botonCantidad() {
        return ActionButton.builder(Component.text("Seguir", ORO))
            .tooltip(Component.text("El interés y lo que devuelves salen de este número."))
            .width(160)
            .action(DialogAction.customClick(this::tomarCantidad, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }

    private ActionButton boton(String etiqueta, TextColor color, String ayuda, int ancho, Runnable accion) {
        ActionButton.Builder builder = ActionButton.builder(Component.text(etiqueta, color))
            .tooltip(Component.text(ayuda))
            .width(ancho);
        if (accion != null) {
            builder.action(DialogAction.customClick((view, audience) -> ejecutar(audience, accion),
                ClickCallback.Options.builder().uses(1).build()));
        }
        return builder.build();
    }

    private void ejecutar(Audience audience, Runnable accion) {
        if (!(audience instanceof Player jugador)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline()) {
                accion.run();
            }
        });
    }

    private void cargar() {
        if (!archivo.exists()) {
            caja = CAJA_INICIAL;
            cajaLista = true;
            guardar();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        cajaLista = yaml.getBoolean("caja-lista");
        caja = cajaLista ? yaml.getInt("caja") : CAJA_INICIAL;
        if (!cajaLista) {
            cajaLista = true;
            caja = CAJA_INICIAL;
        }
        ConfigurationSection seccion = yaml.getConfigurationSection("prestamos");
        if (seccion != null) {
            for (String id : seccion.getKeys(false)) {
                Prestamo prestamo = new Prestamo();
                prestamo.dueno = UUID.fromString(id);
                prestamo.capital = seccion.getInt(id + ".capital");
                prestamo.interes = seccion.getInt(id + ".interes");
                prestamo.restante = seccion.getInt(id + ".restante");
                prestamo.vence = seccion.getLong(id + ".vence");
                prestamo.mora = seccion.getBoolean(id + ".mora");
                prestamo.parcela = seccion.getString(id + ".parcela");
                if (prestamo.parcela != null && prestamo.parcela.isEmpty()) {
                    prestamo.parcela = null;
                }
                prestamos.put(prestamo.dueno, prestamo);
            }
        }
        ConfigurationSection pendientes = yaml.getConfigurationSection("avisos");
        if (pendientes != null) {
            for (String id : pendientes.getKeys(false)) {
                avisos.put(UUID.fromString(id), new ArrayList<>(pendientes.getStringList(id)));
            }
        }
    }

    void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("caja", caja);
        yaml.set("caja-lista", true);
        for (Prestamo prestamo : prestamos.values()) {
            String id = prestamo.dueno.toString();
            yaml.set("prestamos." + id + ".capital", prestamo.capital);
            yaml.set("prestamos." + id + ".interes", prestamo.interes);
            yaml.set("prestamos." + id + ".restante", prestamo.restante);
            yaml.set("prestamos." + id + ".vence", prestamo.vence);
            yaml.set("prestamos." + id + ".mora", prestamo.mora);
            if (prestamo.parcela != null) {
                yaml.set("prestamos." + id + ".parcela", prestamo.parcela);
            }
        }
        for (Map.Entry<UUID, List<String>> entrada : avisos.entrySet()) {
            yaml.set("avisos." + entrada.getKey(), entrada.getValue());
        }
        try {
            if (archivo.getParentFile() != null && !archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude guardar el banco: " + e.getMessage());
        }
    }

    private static final class Prestamo {
        UUID dueno;
        int capital;
        int interes;
        int restante;
        long vence;
        boolean mora;
        String parcela;
    }

    private static final class Borrador {
        int capital;
        int dias;
        int porcentaje;
        long vence;
        String parcela;
        List<String> parcelas = List.of();
    }
}
