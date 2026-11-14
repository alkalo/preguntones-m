package com.preguntones.karma;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.bukkit.entity.Player;

final class Ajustes {
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor ORO = TextColor.color(0xE6B84D);

    private final KarmaPlugin plugin;

    Ajustes(KarmaPlugin plugin) {
        this.plugin = plugin;
    }

    void abrir(Player jugador) {
        Etiqueta etiqueta = plugin.etiqueta(jugador.getUniqueId());
        int puntos = plugin.puntos(jugador.getUniqueId());
        String cifra = puntos > 0 ? "+" + puntos : Integer.toString(puntos);
        boolean agresivo = plugin.agresivo(jugador.getUniqueId());
        String modo = agresivo ? "Modo agresivo: sí" : "Modo agresivo: no";
        String ayudaModo = plugin.cuentaVieja(jugador.getUniqueId())
            ? (agresivo
                ? "Quítalo. Mientras esté activo, cualquiera puede matarte."
                : "Actívalo para poder matar. Cualquiera podrá matarte a ti. El karma negativo se puede matar sin esto.")
            : "Se abre cuando lleves " + plugin.entero("dias-cuenta", 3) + " días en el servidor.";
        List<ActionButton> botones = List.of(
            boton(modo, agresivo ? TextColor.color(0xE05050) : TextColor.color(0x8FE3A0), ayudaModo, 180, () -> {
                plugin.cambiarAgresivo(jugador);
                abrir(jugador);
            }),
            boton("Retar a duelo", ORO, "Los dos vais a la arena. El karma no cambia.", 180, () -> jugadores(jugador))
        );
        Component cuerpo = Component.text("Ahora eres ", SUAVE)
            .append(Component.text(etiqueta.visible + " (" + cifra + ")", etiqueta.color))
            .append(Component.text(". Bueno abarata a los aldeanos. Con karma negativo te pueden matar y, si mueres, sueltas los objetos. Cruel pierde "
                + plugin.entero("cruel-duros", 8) + " duros.", SUAVE));
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("PVP", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(cuerpo, 320)))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", SUAVE, "Vuelve al panel.", 120, () -> jugador.performCommand("menu")), 1))
        ));
    }

    void mostrarReto(Player destino, String nombre) {
        List<ActionButton> botones = List.of(
            boton("Aceptar", TextColor.color(0x8FE3A0), "Vais a la arena en 15 segundos, si no hay peligro.", 140,
                () -> plugin.duelos().aceptar(destino)),
            boton("Rechazar", TextColor.color(0xE07A7A), "El duelo no se hace.", 140,
                () -> plugin.duelos().rechazar(destino))
        );
        ((Audience) destino).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Un duelo", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text(nombre + " te reta. No cambia el karma y no pierdes objetos. Al acabar, los dos volvéis a vuestro sitio.", SUAVE),
                    320
                )))
                .build())
            .type(DialogType.multiAction(botones, cerrar("Cerrar"), 2))
        ));
    }

    private void jugadores(Player jugador) {
        List<Player> otros = new ArrayList<>();
        for (Player otro : Bukkit.getOnlinePlayers()) {
            if (!otro.equals(jugador)) {
                otros.add(otro);
            }
        }
        otros.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (otros.isEmpty()) {
            ((Audience) jugador).showDialog(aviso("Un duelo", "No hay nadie más conectado.", () -> abrir(jugador)));
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        for (Player otro : otros) {
            UUID id = otro.getUniqueId();
            Etiqueta etiqueta = plugin.etiqueta(id);
            Component nombre = Component.text(otro.getName(), TEXTO)
                .append(Component.text(" · " + etiqueta.visible, etiqueta.color));
            String texto = otro.getName();
            botones.add(boton(nombre, "Retar a " + texto + ". Si acepta, vais a la arena en 15 segundos.", 180, () -> {
                Player destino = Bukkit.getPlayer(id);
                if (destino == null) {
                    jugador.sendMessage("§cEsa persona ya no está.");
                    return;
                }
                plugin.duelos().retar(jugador, destino);
            }));
        }
        int columnas = otros.size() > 8 ? 2 : 1;
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Retar", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("El combate es en la arena. Al acabar, los dos volvéis a vuestro sitio.", SUAVE),
                    320
                )))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", SUAVE, "Vuelve al PVP.", 120, () -> abrir(jugador)), columnas))
        ));
    }

    private Dialog aviso(String titulo, String texto, Runnable volver) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(texto, SUAVE), 280)))
                .build())
            .type(DialogType.notice(boton("Volver", SUAVE, "Vuelve al PVP.", 120, volver)))
        );
    }

    private ActionButton boton(String etiqueta, TextColor color, String ayuda, int ancho, Runnable accion) {
        return boton(Component.text(etiqueta, color), ayuda, ancho, accion);
    }

    private ActionButton boton(Component etiqueta, String ayuda, int ancho, Runnable accion) {
        return ActionButton.builder(etiqueta)
            .tooltip(Component.text(ayuda))
            .width(ancho)
            .action(DialogAction.customClick((view, audience) -> ejecutar(audience, accion), unUso()))
            .build();
    }

    private ActionButton cerrar(String etiqueta) {
        return ActionButton.builder(Component.text(etiqueta, SUAVE))
            .width(120)
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
}
