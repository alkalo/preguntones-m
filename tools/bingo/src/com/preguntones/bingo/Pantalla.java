package com.preguntones.bingo;

import java.util.List;
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
import org.bukkit.Sound;
import org.bukkit.entity.Player;

final class Pantalla {
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor HECHO = TextColor.color(0x8FE3A0);

    private final BingoPlugin plugin;

    Pantalla(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    void abrir(Player jugador) {
        plugin.intentarCobro(jugador);
        ((Audience) jugador).showDialog(carta(jugador));
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.15f);
    }

    private Dialog carta(Player jugador) {
        Component cuerpo = Component.text(plugin.queda(), SUAVE);
        for (Casilla casilla : plugin.carta()) {
            boolean hecha = plugin.hecha(jugador.getUniqueId(), casilla);
            cuerpo = cuerpo.append(Component.newline())
                .append(Component.text((hecha ? "● " : "○ ") + casilla.texto, hecha ? HECHO : SUAVE));
        }
        cuerpo = cuerpo.append(Component.newline()).append(Component.text(resumen(jugador), HECHO));
        Component texto = cuerpo;
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Bingo", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(texto, 320)))
                .build())
            .type(DialogType.notice(volver(jugador)))
        );
    }

    private String resumen(Player jugador) {
        int premio = plugin.duros();
        if (plugin.cobrado(jugador.getUniqueId())) {
            return "Ya tienes los " + premio + " duros.";
        }
        int van = plugin.hechas(jugador.getUniqueId());
        if (van >= Carta.TAMANO) {
            return "Los " + premio + " duros siguen pendientes.";
        }
        return van + " de " + Carta.TAMANO + ". Al llenarla, " + premio + " duros.";
    }

    private ActionButton volver(Player jugador) {
        return ActionButton.builder(Component.text("Volver", SUAVE))
            .tooltip(Component.text("Vuelve a los eventos."))
            .width(120)
            .action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player persona) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (persona.isOnline()) {
                            persona.performCommand("menu eventos");
                        }
                    });
                }
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();
    }
}
