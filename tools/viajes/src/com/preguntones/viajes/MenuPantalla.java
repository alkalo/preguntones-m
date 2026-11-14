package com.preguntones.viajes;

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
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

final class MenuPantalla {
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SPAWN = TextColor.color(0x8FE3A0);
    private static final TextColor JUGADOR = TextColor.color(0x8FD0E8);
    private static final TextColor SURVIVAL = TextColor.color(0xF0D080);
    private static final TextColor MUERTE = TextColor.color(0xE08A8A);
    private static final TextColor XO = TextColor.color(0xF0A0A8);
    private static final TextColor EVENTOS = TextColor.color(0xC9A0F0);
    private static final TextColor BINGO = TextColor.color(0xF0C36A);
    private static final TextColor PVP = TextColor.color(0xE6B84D);
    private static final TextColor TIENDA = TextColor.color(0xF4F7FB);
    private static final TextColor PARCELAS = TextColor.color(0x8FCB6B);
    private static final TextColor BANCO = TextColor.color(0xE6B84D);
    private static final TextColor BANDERA = TextColor.color(0xE6B84D);
    private static final TextColor GRUPO = TextColor.color(0x8FD0C8);

    private final ViajesPlugin plugin;

    MenuPantalla(ViajesPlugin plugin) {
        this.plugin = plugin;
    }

    void abrir(Player jugador) {
        ((Audience) jugador).showDialog(menu(jugador));
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.15f);
    }

    void jugadores(Player jugador) {
        abrirJugadores(jugador);
    }

    void spawn(Player jugador) {
        abrirSpawn(jugador);
    }

    void survival(Player jugador) {
        abrirSurvival(jugador);
    }

    private ClickCallback.Options unUso() {
        return ClickCallback.Options.builder().uses(1).build();
    }

    private Dialog menu(Player jugador) {
        List<ActionButton> botones = List.of(
            boton("Spawn", SPAWN, "El barrio o una bandera tuya. Tarda 15 segundos, si no hay peligro.", 74, () -> abrirSpawn(jugador)),
            boton("Jugador", JUGADOR, "Ir junto a alguien. Esa persona tiene que aceptar.", 74, () -> abrirJugadores(jugador)),
            boton("Survival", SURVIVAL, "Explorar un sitio al azar, o volver a tu cama.", 74, () -> abrirSurvival(jugador)),
            boton("Muerte", MUERTE, "Tu última muerte. Cuesta " + Huella.PRECIO + " duros. Una vez.", 74, () -> abrirMuerte(jugador)),
            boton("Eventos", EVENTOS, "La XO y el bingo.", 74, () -> abrirEventos(jugador)),
            boton("PVP", PVP, "Modo agresivo y retar a un duelo.", 74, () -> jugador.performCommand("karma")),
            boton("Parcelas", PARCELAS, "Invitar, unir el camino con un amigo y venderle al banco.", 74, () -> abrirParcelas(jugador)),
            boton("Tienda", TIENDA, "Bloques para la parcela. Se pagan con duros.", 74, () -> abrirTienda(jugador)),
            boton("Bandera", BANDERA, "Cuesta 40 duros. Al ponerla, sale en Spawn.", 74, () -> abrirBandera(jugador)),
            boton("Banco", BANCO, "Pedir duros. Se devuelven con intereses.", 74, () -> abrirBanco(jugador)),
            boton("Grupo", GRUPO, "Hasta 4 personas. La cara y la vida se quedan arriba a la izquierda.", 74, () -> abrirGrupo(jugador))
        );
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Panel Preguntones", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .build())
            .type(DialogType.multiAction(botones, cerrar("Cerrar"), 4))
        );
    }

    void eventos(Player jugador) {
        abrirEventos(jugador);
    }

    private void abrirEventos(Player jugador) {
        List<ActionButton> botones = List.of(
            boton("XO", XO, "Se abre sola el sábado a las 21:30.", 150, () -> plugin.irXo(jugador)),
            boton("Bingo", BINGO, "Una hora, cuando está abierto. Si la llenas, te llevas duros.", 150, () -> abrirBingo(jugador))
        );
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Eventos", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("La XO se abre sola el sábado a las 21:30. El bingo lo abre un admin y dura una hora. Nueve cosas, y al llenarlas hay duros.", TextColor.color(0xC5CED8)),
                    320
                )))
                .build())
            .type(DialogType.multiAction(
                botones,
                boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)),
                1
            ))
        ));
    }

    private void abrirBingo(Player jugador) {
        if (!jugador.performCommand("bingo")) {
            jugador.sendMessage("§eEl bingo no está listo.");
        }
    }

    private void abrirTienda(Player jugador) {
        if (!jugador.performCommand("puesto")) {
            jugador.sendMessage("§eLa tienda no está lista.");
        }
    }

    private void abrirParcelas(Player jugador) {
        if (!jugador.performCommand("parcela panel")) {
            jugador.sendMessage("§eLas parcelas no están listas.");
        }
    }

    private void abrirGrupo(Player jugador) {
        if (!jugador.performCommand("grupo")) {
            jugador.sendMessage("§eEl grupo no está listo.");
        }
    }

    private void abrirBanco(Player jugador) {
        if (!jugador.performCommand("banco")) {
            jugador.sendMessage("§eEl banco no está listo.");
        }
    }

    void muerte(Player jugador) {
        abrirMuerte(jugador);
    }

    private void abrirMuerte(Player jugador) {
        Huella huella = plugin.huella(jugador);
        if (huella == null) {
            ((Audience) jugador).showDialog(aviso(jugador, "Tu muerte", "Todavía no has muerto."));
            return;
        }
        Location aqui = jugador.getLocation();
        String mundo = aqui.getWorld() == null ? "" : aqui.getWorld().getName();
        if (Huella.junto(huella, mundo, aqui.getBlockX(), aqui.getBlockY(), aqui.getBlockZ())) {
            ((Audience) jugador).showDialog(aviso(jugador, "Tu muerte", "Ya estás junto a esa muerte."));
            return;
        }
        String donde = Huella.sitio(huella.mundo) + ", " + huella.x + " " + huella.y + " " + huella.z;
        String saldo = plugin.economiaLista()
            ? " Tienes " + plugin.duros(jugador) + "."
            : " La economía no está lista.";
        String cuerpo = "Tu última muerte: " + donde + ". Cuesta " + Huella.PRECIO
            + " duros. Tarda 15 segundos y se gasta. Si se cancela, no se cobra." + saldo;
        List<ActionButton> botones = List.of(
            boton("Pagar " + Huella.PRECIO, MUERTE, "Al llegar, −" + Huella.PRECIO + " duros.", 160, () -> plugin.irMuerte(jugador))
        );
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Tu muerte", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, TextColor.color(0xC5CED8)), 320)))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)), 1))
        ));
    }

    void bandera(Player jugador) {
        abrirBandera(jugador);
    }

    private void abrirBandera(Player jugador) {
        List<ActionButton> botones = List.of(
            boton("Comprar", SPAWN, "Te cobra 40 duros y te da la bandera.", 150, () -> {
                if (!jugador.performCommand("bandera")) {
                    jugador.sendMessage("§eLa bandera no está lista.");
                }
            })
        );
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Bandera", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("Cuesta 40 duros. La pones donde quieras y sale en Spawn. Hasta cinco.", TextColor.color(0xC5CED8)),
                    280
                )))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)), 1))
        ));
    }

    private void abrirSpawn(Player jugador) {
        List<Banderas.Punto> puntos = plugin.puntos(jugador);
        List<ActionButton> botones = new ArrayList<>();
        botones.add(boton("El barrio", SPAWN, "Volver al barrio. Tarda 15 segundos, si no hay peligro.", 180,
            () -> plugin.volverSpawn(jugador)));
        for (Banderas.Punto punto : puntos) {
            UUID id = punto.id;
            botones.add(boton(punto.nombre, BANDERA, ayudaBandera(punto), 180, () -> plugin.irBandera(jugador, id)));
        }
        String cuerpo = puntos.isEmpty()
            ? "El barrio es el de siempre. La bandera cuesta 40 duros, en el panel. Al ponerla, sale aquí."
            : "El barrio, o una bandera tuya. Tarda 15 segundos, si no hay peligro.";
        int columnas = botones.size() > 6 ? 2 : 1;
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Spawn", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, TextColor.color(0xC5CED8)), 320)))
                .build())
            .type(DialogType.multiAction(
                botones,
                boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)),
                columnas
            ))
        ));
    }

    private String ayudaBandera(Banderas.Punto punto) {
        return mundo(punto.mundo) + " · " + punto.bx + " " + punto.by + " " + punto.bz + ". Tarda 15 segundos.";
    }

    private String mundo(String nombre) {
        return switch (nombre) {
            case "world" -> "barrio";
            case "survival" -> "survival";
            case "survival_nether" -> "nether";
            case "fin" -> "end";
            default -> nombre;
        };
    }

    private void abrirSurvival(Player jugador) {
        List<ActionButton> botones = List.of(
            boton("Explorar", SURVIVAL, "Un sitio al azar. Quien esté a tu lado viene si acepta.", 150, () -> plugin.irSurvival(jugador)),
            boton("Tu cama", SPAWN, "La cama del survival. Tarda 15 segundos, si no hay peligro.", 150, () -> plugin.irCama(jugador))
        );
        ((net.kyori.adventure.audience.Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Survival", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("Explorar suelta en un sitio nuevo. Quien esté a tu lado viene si acepta. Tu cama vuelve donde dormiste.", TextColor.color(0xC5CED8)),
                    320
                )))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)), 1))
        ));
    }

    private void abrirJugadores(Player jugador) {
        List<Player> otros = new ArrayList<>();
        for (Player otro : Bukkit.getOnlinePlayers()) {
            if (!otro.equals(jugador)) {
                otros.add(otro);
            }
        }
        otros.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (otros.isEmpty()) {
            ((Audience) jugador).showDialog(aviso(jugador, "Un jugador", "No hay nadie más conectado."));
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        for (Player otro : otros) {
            UUID id = otro.getUniqueId();
            String nombre = otro.getName();
            botones.add(boton(nombre, TEXTO, "Pedirle a " + nombre + " que acepte.", 160, () -> plugin.irCon(jugador, id)));
        }
        int columnas = otros.size() > 8 ? 2 : 1;
        ((Audience) jugador).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Un jugador", TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(
                    Component.text("Esa persona tiene que aceptar. Luego tardas 15 segundos. No vale en lava, ahogándote o peleando.", TextColor.color(0xC5CED8)),
                    320
                )))
                .build())
            .type(DialogType.multiAction(botones, boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador)), columnas))
        ));
    }

    private Dialog aviso(Player jugador, String titulo, String texto) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(texto, TextColor.color(0xC5CED8)), 280)))
                .build())
            .type(DialogType.notice(boton("Volver", TextColor.color(0xC5CED8), "Vuelve al panel.", 120, () -> abrir(jugador))))
        );
    }

    private ActionButton boton(String etiqueta, TextColor color, String ayuda, int ancho, Runnable accion) {
        return ActionButton.builder(Component.text(etiqueta, color))
            .tooltip(Component.text(ayuda))
            .width(ancho)
            .action(DialogAction.customClick((view, audience) -> ejecutar(audience, accion), unUso()))
            .build();
    }

    void pedir(Player quien, String titulo, String cuerpo, String si, String no, String ayudaNo) {
        ((Audience) quien).showDialog(Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, TextColor.color(0xC5CED8)), 320)))
                .build())
            .type(DialogType.confirmation(
                boton(si, SPAWN, "Aceptar.", 140, () -> plugin.aceptar(quien)),
                boton(no, TextColor.color(0xE07A7A), ayudaNo, 140, () -> plugin.rechazar(quien))
            ))
        ));
        quien.playSound(quien.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
    }

    private ActionButton cerrar(String etiqueta) {
        return ActionButton.builder(Component.text(etiqueta, TextColor.color(0xC5CED8)))
            .width(120)
            .build();
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
