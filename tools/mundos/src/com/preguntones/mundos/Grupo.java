package com.preguntones.mundos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/** Grupo del dragón, de hasta 4. Se arma en el panel y se guarda con el ciclo. */
final class Grupo {
    static final int MAXIMO = 4;

    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor MENTA = TextColor.color(0x8FD0C8);
    private static final TextColor SI = TextColor.color(0x8FE3A0);
    private static final TextColor NO = TextColor.color(0xE07A7A);

    private final MundosPlugin plugin;
    private final Marco marco;
    private final Map<UUID, UUID> liderDe = new HashMap<>();
    private final Map<UUID, Set<UUID>> equipoDe = new HashMap<>();
    private final Map<UUID, Invitacion> invitaGrupo = new HashMap<>();

    Grupo(MundosPlugin plugin) {
        this.plugin = plugin;
        this.marco = new Marco(plugin, this);
    }

    void arrancar() {
        marco.arrancar();
    }

    void cerrar() {
        marco.cerrar();
    }

    void cargar(YamlConfiguration yaml) {
        liderDe.clear();
        equipoDe.clear();
        invitaGrupo.clear();
        ConfigurationSection grupos = yaml.getConfigurationSection("grupos");
        if (grupos == null) {
            return;
        }
        for (String liderTexto : grupos.getKeys(false)) {
            try {
                UUID lider = UUID.fromString(liderTexto);
                Set<UUID> equipo = leer(yaml.getStringList("grupos." + liderTexto));
                equipo.add(lider);
                equipoDe.put(lider, equipo);
                for (UUID miembro : equipo) {
                    liderDe.put(miembro, lider);
                }
            } catch (IllegalArgumentException ignorado) {
                plugin.getLogger().warning("Ignoro un grupo " + liderTexto);
            }
        }
    }

    void escribir(YamlConfiguration yaml) {
        for (Map.Entry<UUID, Set<UUID>> entrada : equipoDe.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (UUID id : entrada.getValue()) {
                ids.add(id.toString());
            }
            yaml.set("grupos." + entrada.getKey(), ids);
        }
    }

    Set<UUID> miembros(UUID id) {
        UUID lider = liderDe.get(id);
        if (lider == null) {
            return Set.of(id);
        }
        Set<UUID> equipo = equipoDe.get(lider);
        if (equipo == null || equipo.isEmpty()) {
            return Set.of(id);
        }
        return Set.copyOf(equipo);
    }

    List<UUID> presentes(UUID id) {
        UUID lider = liderDe.get(id);
        if (lider == null) {
            return List.of();
        }
        Set<UUID> equipo = equipoDe.get(lider);
        if (equipo == null || equipo.isEmpty()) {
            return List.of();
        }
        return ordenar(lider, equipo, false);
    }

    List<UUID> marcoDe(UUID id) {
        UUID lider = liderDe.get(id);
        boolean enEquipo = lider != null;
        if (!enEquipo) {
            lider = id;
        }
        Set<UUID> equipo = equipoDe.get(lider);
        if (equipo != null && !equipo.contains(id) && enEquipo) {
            return List.of();
        }
        if (!enEquipo && !id.equals(lider)) {
            return List.of();
        }
        Set<UUID> base = equipo == null ? new HashSet<>() : equipo;
        if (!enEquipo && invitados(lider).isEmpty()) {
            return List.of();
        }
        if (!enEquipo) {
            base = new HashSet<>();
            base.add(id);
        }
        List<UUID> lista = ordenar(lider, base, true);
        if (lista.size() > MAXIMO) {
            lista = new ArrayList<>(lista.subList(0, MAXIMO));
        }
        return lista.size() < 2 ? List.of() : lista;
    }

    boolean pendiente(UUID id) {
        return invitaGrupo.containsKey(id);
    }

    void olvidarInvitacion(Player jugador) {
        Invitacion invitacion = invitaGrupo.remove(jugador.getUniqueId());
        if (invitacion == null) {
            return;
        }
        Player quien = Bukkit.getPlayer(invitacion.de);
        if (quien != null && !quien.equals(jugador)) {
            quien.sendMessage("§e" + jugador.getName() + " se ha ido. La invitación se cancela.");
        }
    }

    private List<UUID> invitados(UUID lider) {
        List<UUID> lista = new ArrayList<>();
        for (Map.Entry<UUID, Invitacion> entrada : invitaGrupo.entrySet()) {
            if (lider.equals(entrada.getValue().lider)) {
                lista.add(entrada.getKey());
            }
        }
        return lista;
    }

    private List<UUID> ordenar(UUID lider, Set<UUID> equipo, boolean conInvitados) {
        Set<UUID> juntos = new HashSet<>(equipo);
        if (conInvitados) {
            juntos.addAll(invitados(lider));
        }
        List<UUID> lista = new ArrayList<>(juntos);
        lista.sort(Comparator
            .comparing((UUID miembro) -> !miembro.equals(lider))
            .thenComparing(this::pendiente)
            .thenComparing(Grupo::nombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    private String espera(UUID lider) {
        List<String> nombres = new ArrayList<>();
        for (UUID id : invitados(lider)) {
            nombres.add(nombre(id));
        }
        if (nombres.isEmpty()) {
            return "";
        }
        return "Esperando a " + String.join(", ", nombres) + ".";
    }

    static String nombre(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String guardado = Bukkit.getOfflinePlayer(id).getName();
        return guardado == null || guardado.isBlank() ? "Alguien" : guardado;
    }

    static ItemStack cara(UUID id) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (!(item.getItemMeta() instanceof SkullMeta meta)) {
            return item;
        }
        Player online = Bukkit.getPlayer(id);
        meta.setPlayerProfile(online != null ? online.getPlayerProfile() : Bukkit.createProfile(id));
        item.setItemMeta(meta);
        return item;
    }

    boolean orden(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Entra al juego para hacer un grupo.");
            return true;
        }
        if (args.length == 0) {
            abrir(jugador, null);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invitar" -> {
                if (args.length < 2) {
                    jugador.sendMessage("§e/grupo invitar <nombre>");
                    break;
                }
                Player otro = Bukkit.getPlayerExact(args[1]);
                if (otro == null) {
                    jugador.sendMessage("§cEsa persona tiene que estar conectada.");
                    break;
                }
                invitar(jugador, otro.getUniqueId(), false);
            }
            case "aceptar" -> aceptar(jugador);
            case "salir" -> salir(jugador, false);
            case "rechazar", "no" -> rechazar(jugador);
            default -> jugador.sendMessage("§eEstá en el panel, botón Grupo. §fO /grupo invitar, aceptar o salir.");
        }
        return true;
    }

    List<String> completar(String[] args) {
        if (args.length == 1) {
            return filtrar(List.of("invitar", "aceptar", "salir"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invitar")) {
            List<String> nombres = new ArrayList<>();
            for (Player jugador : Bukkit.getOnlinePlayers()) {
                nombres.add(jugador.getName());
            }
            return filtrar(nombres, args[1]);
        }
        return List.of();
    }

    private void abrir(Player jugador, String aviso) {
        if (!jugador.isOnline()) {
            return;
        }
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null && !aviso.isEmpty()) {
            cuerpo.add(DialogBody.plainMessage(Component.text(aviso, TextColor.color(0xE6B84D)), 300));
        }
        List<UUID> gente = presentes(jugador.getUniqueId());
        Invitacion pendiente = invitaGrupo.get(jugador.getUniqueId());
        boolean dentro = liderDe.containsKey(jugador.getUniqueId());
        UUID lider = liderDe.getOrDefault(jugador.getUniqueId(), jugador.getUniqueId());
        String intro = gente.isEmpty()
            ? "Invita a alguien que esté conectado. Hasta 4. Al cazar el dragón, cada uno que esté conectado cobra 20 duros."
            : "Sois " + gente.size() + " de 4. La cara y la vida se quedan arriba a la izquierda, aunque mires a otro sitio. Los duros siguen a la derecha.";
        String espera = espera(lider);
        if (!espera.isEmpty()) {
            intro = espera + " " + intro;
        }
        if (pendiente != null && !dentro) {
            intro = pendiente.nombre + " te invita. " + intro;
        }
        cuerpo.add(DialogBody.plainMessage(Component.text(intro, SUAVE), 300));
        for (UUID id : gente) {
            Player persona = Bukkit.getPlayer(id);
            String linea = persona == null
                ? nombre(id) + " · fuera"
                : nombre(id) + " · " + (int) Math.ceil(persona.getHealth()) + " de vida";
            cuerpo.add(DialogBody.item(cara(id))
                .description(DialogBody.plainMessage(Component.text(linea, TEXTO), 240))
                .showDecorations(false)
                .showTooltip(false)
                .width(20)
                .height(20)
                .build());
        }
        List<ActionButton> botones = new ArrayList<>();
        if (ocupados(lider, null) < MAXIMO) {
            botones.add(boton("Invitar", MENTA, "Alguien que esté conectado. Hasta 4.", 160, () -> elegir(jugador)));
        }
        if (pendiente != null && !dentro) {
            botones.add(boton("Aceptar", SI, "Entras en el grupo.", 160, () -> aceptar(jugador)));
            botones.add(boton("Ahora no", NO, "Dejas la invitación.", 160, () -> rechazar(jugador)));
        }
        if (dentro) {
            botones.add(boton("Salir", NO, "Si eres quien lo abrió, el grupo se disuelve.", 160, () -> salir(jugador, true)));
        }
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Grupo", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(cuerpo)
                .build())
            .type(DialogType.multiAction(
                botones,
                boton("Volver", SUAVE, "Vuelve al panel.", 120, () -> volver(jugador)),
                1
            ))
        ));
    }

    private void elegir(Player jugador) {
        UUID lider = liderDe.getOrDefault(jugador.getUniqueId(), jugador.getUniqueId());
        List<Player> candidatos = new ArrayList<>();
        boolean hayMas = false;
        for (Player otro : Bukkit.getOnlinePlayers()) {
            if (otro.equals(jugador)) {
                continue;
            }
            hayMas = true;
            if (liderDe.get(otro.getUniqueId()) == null) {
                candidatos.add(otro);
            }
        }
        candidatos.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (candidatos.isEmpty()) {
            String texto = hayMas
                ? "Quien está conectado ya tiene grupo."
                : "No hay nadie más conectado.";
            ((Audience) jugador).showDialog(aviso(jugador, "Invitar", texto));
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        for (Player otro : candidatos) {
            UUID id = otro.getUniqueId();
            String nombre = otro.getName();
            botones.add(boton(nombre, TEXTO, "Invitar a " + nombre + ".", 160, () -> invitar(jugador, id, true)));
        }
        int columnas = candidatos.size() > 8 ? 2 : 1;
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Invitar", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("Elige a alguien que esté conectado. Hasta 4.", SUAVE),
                    300
                )))
                .build())
            .type(DialogType.multiAction(
                botones,
                boton("Volver", SUAVE, "Vuelve al grupo.", 120, () -> abrir(jugador, null)),
                columnas
            ))
        ));
    }

    private void invitar(Player jugador, UUID id, boolean panel) {
        Player otro = Bukkit.getPlayer(id);
        if (otro == null) {
            jugador.sendMessage("§cEsa persona tiene que estar conectada.");
            if (panel) {
                abrir(jugador, null);
            }
            return;
        }
        if (otro.getUniqueId().equals(jugador.getUniqueId())) {
            jugador.sendMessage("§cNo puedes invitarte.");
            return;
        }
        UUID lider = liderDe.getOrDefault(jugador.getUniqueId(), jugador.getUniqueId());
        UUID suyo = liderDe.get(otro.getUniqueId());
        if (suyo != null) {
            jugador.sendMessage(suyo.equals(lider) ? "§cYa está en el grupo." : "§cYa está en un grupo.");
            if (panel) {
                abrir(jugador, null);
            }
            return;
        }
        if (ocupados(lider, otro.getUniqueId()) >= MAXIMO) {
            jugador.sendMessage("§cEse grupo ya tiene 4 personas.");
            if (panel) {
                abrir(jugador, null);
            }
            return;
        }
        invitaGrupo.put(otro.getUniqueId(), new Invitacion(lider, jugador.getUniqueId(), jugador.getName()));
        jugador.sendMessage("§aInvitación enviada.");
        pedir(otro, jugador.getName());
        marco.pulso();
        if (panel) {
            abrir(jugador, "Invitación enviada a " + otro.getName() + ".");
        }
    }

    private void pedir(Player quien, String de) {
        ((Audience) quien).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Grupo", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text(de + " te invita a su grupo. Hasta 4. Si aceptas, la cara y la vida se quedan arriba a la izquierda.", SUAVE),
                    300
                )))
                .build())
            .type(DialogType.confirmation(
                boton("Aceptar", SI, "Entras en el grupo.", 140, () -> aceptar(quien)),
                boton("Ahora no", NO, "Dejas la invitación.", 140, () -> rechazar(quien))
            ))
        ));
        quien.sendMessage("§6" + de + " §fte invita a su grupo.");
        quien.playSound(quien.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
    }

    private void aceptar(Player jugador) {
        Invitacion invitacion = invitaGrupo.get(jugador.getUniqueId());
        if (invitacion == null) {
            jugador.sendMessage("§cNadie te ha invitado.");
            return;
        }
        if (liderDe.containsKey(jugador.getUniqueId())) {
            invitaGrupo.remove(jugador.getUniqueId());
            jugador.sendMessage("§cYa estás en un grupo. Sal primero.");
            return;
        }
        UUID ya = liderDe.get(invitacion.lider);
        if (ya != null && !ya.equals(invitacion.lider)) {
            invitaGrupo.remove(jugador.getUniqueId());
            jugador.sendMessage("§cEse grupo ya no está.");
            return;
        }
        Set<UUID> equipo = equipoDe.computeIfAbsent(invitacion.lider, id -> {
            Set<UUID> nuevo = new HashSet<>();
            nuevo.add(id);
            liderDe.put(id, id);
            return nuevo;
        });
        if (equipo.size() >= MAXIMO) {
            invitaGrupo.remove(jugador.getUniqueId());
            jugador.sendMessage("§cEse grupo ya tiene 4 personas.");
            return;
        }
        invitaGrupo.remove(jugador.getUniqueId());
        equipo.add(jugador.getUniqueId());
        liderDe.put(jugador.getUniqueId(), invitacion.lider);
        for (UUID id : equipo) {
            if (id.equals(jugador.getUniqueId())) {
                continue;
            }
            Player otro = Bukkit.getPlayer(id);
            if (otro != null) {
                otro.sendMessage("§a" + jugador.getName() + " entra en el grupo.");
            }
        }
        jugador.sendMessage("§aEstás en el grupo. Máximo 4.");
        plugin.guardarEstado();
        marco.pulso();
        abrir(jugador, "Estás en el grupo.");
    }

    private void rechazar(Player jugador) {
        Invitacion invitacion = invitaGrupo.remove(jugador.getUniqueId());
        if (invitacion == null) {
            jugador.sendMessage("§eNo tenías invitación.");
            return;
        }
        jugador.sendMessage("§eHas dejado la invitación.");
        Player quien = Bukkit.getPlayer(invitacion.de);
        if (quien != null && !quien.equals(jugador)) {
            quien.sendMessage("§e" + jugador.getName() + " no entra ahora.");
        }
        marco.pulso();
    }

    private void salir(Player jugador, boolean panel) {
        UUID lider = liderDe.get(jugador.getUniqueId());
        if (lider == null) {
            jugador.sendMessage("§eNo estás en un grupo.");
            if (panel) {
                abrir(jugador, null);
            }
            return;
        }
        if (lider.equals(jugador.getUniqueId())) {
            disolver(lider, jugador, "§eHas disuelto el grupo.", "§e" + jugador.getName() + " ha disuelto el grupo.");
            if (panel) {
                abrir(jugador, null);
            }
            return;
        }
        Set<UUID> equipo = equipoDe.get(lider);
        if (equipo != null) {
            equipo.remove(jugador.getUniqueId());
            if (equipo.isEmpty()) {
                equipoDe.remove(lider);
            }
        }
        liderDe.remove(jugador.getUniqueId());
        jugador.sendMessage("§eHas salido del grupo.");
        if (equipo != null) {
            for (UUID id : equipo) {
                Player otro = Bukkit.getPlayer(id);
                if (otro != null) {
                    otro.sendMessage("§e" + jugador.getName() + " sale del grupo.");
                }
            }
        }
        plugin.guardarEstado();
        marco.pulso();
        if (panel) {
            abrir(jugador, null);
        }
    }

    private void disolver(UUID lider, Player quien, String paraQuien, String paraResto) {
        Set<UUID> equipo = equipoDe.remove(lider);
        if (equipo != null) {
            for (UUID id : equipo) {
                liderDe.remove(id);
            }
            for (UUID id : equipo) {
                if (quien != null && id.equals(quien.getUniqueId())) {
                    continue;
                }
                Player otro = Bukkit.getPlayer(id);
                if (otro != null) {
                    otro.sendMessage(paraResto);
                }
            }
        }
        liderDe.remove(lider);
        invitaGrupo.entrySet().removeIf(entrada -> lider.equals(entrada.getValue().lider));
        if (quien != null) {
            quien.sendMessage(paraQuien);
        }
        plugin.guardarEstado();
        marco.pulso();
    }

    private int ocupados(UUID lider, UUID salvo) {
        Set<UUID> equipo = equipoDe.get(lider);
        int n = equipo == null ? 1 : equipo.size();
        for (Map.Entry<UUID, Invitacion> entrada : invitaGrupo.entrySet()) {
            if (entrada.getKey().equals(salvo)) {
                continue;
            }
            if (lider.equals(entrada.getValue().lider)) {
                n++;
            }
        }
        return n;
    }

    private void volver(Player jugador) {
        if (!jugador.performCommand("menu")) {
            jugador.sendMessage("§eEl panel no está listo.");
        }
    }

    private Dialog aviso(Player jugador, String titulo, String texto) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(texto, SUAVE), 280)))
                .build())
            .type(DialogType.notice(boton("Volver", SUAVE, "Vuelve al grupo.", 120, () -> abrir(jugador, null))))
        );
    }

    private ActionButton boton(String etiqueta, TextColor color, String ayuda, int ancho, Runnable accion) {
        return ActionButton.builder(Component.text(etiqueta, color))
            .tooltip(Component.text(ayuda))
            .width(ancho)
            .action(DialogAction.customClick((view, audience) -> ejecutar(audience, accion), unUso()))
            .build();
    }

    private ClickCallback.Options unUso() {
        return ClickCallback.Options.builder().uses(1).build();
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

    private static List<String> filtrar(List<String> opciones, String escrito) {
        String prefijo = escrito == null ? "" : escrito.toLowerCase(Locale.ROOT);
        List<String> sale = new ArrayList<>();
        for (String opcion : opciones) {
            if (opcion.toLowerCase(Locale.ROOT).startsWith(prefijo)) {
                sale.add(opcion);
            }
        }
        return sale;
    }

    private Set<UUID> leer(List<String> textos) {
        Set<UUID> ids = new HashSet<>();
        for (String texto : textos) {
            ids.add(UUID.fromString(texto));
        }
        return ids;
    }

    private static final class Invitacion {
        final UUID lider;
        final UUID de;
        final String nombre;

        Invitacion(UUID lider, UUID de, String nombre) {
            this.lider = lider;
            this.de = de;
            this.nombre = nombre;
        }
    }
}
