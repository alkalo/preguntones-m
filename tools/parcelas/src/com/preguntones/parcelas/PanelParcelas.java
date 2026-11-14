package com.preguntones.parcelas;

import java.util.ArrayList;
import java.util.List;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

final class PanelParcelas {
    private static final String CAMPO = "escrito";
    private static final TextColor TEXTO = TextColor.color(0xF4F7FB);
    private static final TextColor SUAVE = TextColor.color(0xC5CED8);
    private static final TextColor HIERBA = TextColor.color(0x8FCB6B);
    private static final TextColor ORO = TextColor.color(0xE6B84D);
    private static final TextColor NO = TextColor.color(0xE07A7A);

    private final ParcelasPlugin plugin;

    PanelParcelas(ParcelasPlugin plugin) {
        this.plugin = plugin;
    }

    void abrir(Player jugador) {
        List<ActionButton> botones = new ArrayList<>();
        List<ParcelasPlugin.Opcion> mias = plugin.misParcelas(jugador.getUniqueId());
        botones.add(boton("Aquí", HIERBA, "Lo que hay donde estás.", 150, () -> aqui(jugador)));
        if (!mias.isEmpty()) {
            botones.add(boton("Las mías", TEXTO, "Cada parcela tuya.", 150, () -> elegir(jugador, "Las mías", "Elige una.", mias, opcion -> ficha(jugador, opcion.clave))));
            botones.add(boton("Invitar", TEXTO, "Alguien construye en la tuya.", 150, () -> invitar(jugador)));
            botones.add(boton("Unir", HIERBA, "El hueco con la parcela de un amigo, si los dos aceptáis.", 150, () -> unir(jugador)));
            botones.add(boton("Al banco", ORO, "Te paga la quinta parte de lo que te costó.", 150, () -> banco(jugador)));
        }
        botones.add(boton("Una libre", HIERBA, "Te lleva a una parcela libre.", 150, () -> plugin.irLibre(jugador)));
        if (plugin.textoOferta(jugador) != null) {
            botones.add(boton("Aceptar oferta", ORO, "Te quedas la parcela que te han ofrecido.", 160, () -> {
                plugin.aceptarOferta(jugador);
                abrir(jugador);
            }));
            botones.add(boton("Rechazar oferta", NO, "La oferta se cancela.", 160, () -> {
                plugin.rechazarOferta(jugador);
                abrir(jugador);
            }));
        }
        if (plugin.textoUnion(jugador) != null) {
            botones.add(boton("Aceptar unión", HIERBA, "El hueco entre las dos pasa a hierba.", 160, () -> {
                plugin.aceptarUnion(jugador);
                abrir(jugador);
            }));
            botones.add(boton("No unir", NO, "El camino se queda como está.", 150, () -> {
                plugin.rechazarUnion(jugador);
                abrir(jugador);
            }));
        }
        int llevas = plugin.llevasDe(jugador.getUniqueId());
        int tope = plugin.topePara(jugador.getUniqueId());
        String cuerpo = "Tienes " + llevas + " de " + tope + ".";
        if (llevas < tope) {
            cuerpo += " La siguiente cuesta " + plugin.precioNueva(jugador.getUniqueId()) + " duros.";
        }
        cuerpo += "\nEl banco paga la quinta parte de lo que costó.";
        String oferta = plugin.textoOferta(jugador);
        if (oferta != null) {
            cuerpo += "\n" + oferta;
        }
        String union = plugin.textoUnion(jugador);
        if (union != null) {
            cuerpo += "\n" + union;
        }
        mostrar(jugador, "Parcelas", cuerpo, botones, boton("Volver", SUAVE, "Vuelve al panel.", 120, () -> volverMenu(jugador)), botones.size() > 6 ? 2 : 1);
    }

    void confirmarBanco(Player jugador, String clave) {
        if (!plugin.mia(jugador, clave)) {
            jugador.sendMessage("§cEsta parcela no es tuya.");
            abrir(jugador);
            return;
        }
        int pago = plugin.recompraDe(clave);
        int caja = plugin.cajaBanco();
        if (caja < pago) {
            aviso(jugador, "Al banco", "La caja tiene " + caja + " duros. Esta venta pide " + pago + ". La parcela sigue tuya y el terreno no cambia.");
            return;
        }
        String cuerpo = plugin.pagadoApuntado(clave)
            ? "Te costó " + plugin.pagadoDe(clave) + " duros. El banco paga " + pago + ", la quinta parte."
            : "No quedó apuntado lo que costó. El banco paga " + pago + ", la quinta parte de la primera.";
        cuerpo += " Lo construido se queda. El borde pasa a amarillo. La caja tiene " + caja + ".";
        preguntar(jugador, "Al banco", cuerpo, "Vender", () -> {
            plugin.venderBanco(jugador, clave);
            abrir(jugador);
        }, "Volver", () -> ficha(jugador, clave));
    }

    void pedirOferta(Player quien, String nombre, String parcela, int precio) {
        int tasa = plugin.tasaDe(precio);
        String cuerpo = nombre + " te ofrece " + parcela + " por " + precio + " duros. Recibe " + (precio - tasa) + ". El barrio se queda " + tasa + ".";
        preguntar(quien, "Oferta", cuerpo, "Aceptar", () -> plugin.aceptarOferta(quien), "No", () -> plugin.rechazarOferta(quien));
        quien.playSound(quien.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
    }

    void pedirUnion(Player quien, String nombre, String parcela) {
        String cuerpo = nombre + " quiere unir " + parcela + " con la tuya. Si aceptas, el hueco estrecho entre las dos pasa a hierba. El spawn no entra.";
        preguntar(quien, "Unir", cuerpo, "Aceptar", () -> plugin.aceptarUnion(quien), "No", () -> plugin.rechazarUnion(quien));
        quien.playSound(quien.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.4f);
    }

    void abrirUnir(Player jugador, String clave) {
        elegir(jugador, "Unir", "Elige con quién. Tiene que aceptar.", plugin.paraUnir(clave), opcion -> plugin.pedirUnion(jugador, opcion.clave, opcion.otra));
    }

    void abrirSeparar(Player jugador, String clave) {
        elegir(jugador, "Separar", "Lo construido se queda. El hueco vuelve al barrio.", plugin.unidasCon(clave), opcion -> confirmarSeparar(jugador, opcion));
    }

    private void aqui(Player jugador) {
        if (!plugin.enBarrio(jugador)) {
            aviso(jugador, "Parcelas", "Eso se usa en el barrio.");
            return;
        }
        Cuadricula.Zona zona = plugin.zonaDe(jugador);
        if (zona == Cuadricula.Zona.SPAWN || zona == Cuadricula.Zona.PASILLO) {
            aviso(jugador, zona == Cuadricula.Zona.SPAWN ? "Spawn" : "Pasillo", "Esto no es una parcela. La libre tiene borde amarillo.");
            return;
        }
        String clave = plugin.claveAqui(jugador);
        if (clave != null) {
            if (plugin.libre(clave)) {
                confirmarReclamar(jugador);
                return;
            }
            if (plugin.mia(jugador, clave)) {
                ficha(jugador, clave);
                return;
            }
            if (plugin.enVenta(clave)) {
                confirmarCompra(jugador, clave);
                return;
            }
            aviso(jugador, "Parcela", "Es de " + plugin.duenoNombre(clave) + ".");
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        for (ParcelasPlugin.Opcion opcion : plugin.uniblesAqui(jugador)) {
            ParcelasPlugin.Opcion fija = opcion;
            botones.add(boton(fija.nombre, HIERBA, fija.ayuda, 170, () -> plugin.pedirUnion(jugador, fija.clave, fija.otra)));
        }
        String texto = plugin.textoTramo(jugador);
        if (botones.isEmpty()) {
            aviso(jugador, "Camino", texto);
            return;
        }
        mostrar(jugador, "Camino", texto, botones, volver(jugador), 1);
    }

    private void ficha(Player jugador, String clave) {
        if (!plugin.mia(jugador, clave)) {
            jugador.sendMessage("§cEsta parcela no es tuya.");
            abrir(jugador);
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        botones.add(boton("Invitar", TEXTO, "Alguien construye aquí.", 150, () -> elegirGente(jugador, clave, false)));
        if (!plugin.gentePara(jugador, clave, true).isEmpty()) {
            botones.add(boton("Quitar", NO, "Alguien deja de construir aquí.", 150, () -> elegirGente(jugador, clave, true)));
        }
        botones.add(boton("Nombre", TEXTO, "Hasta 15 letras.", 140, () -> pedirNombre(jugador, clave, null, null)));
        botones.add(boton("Ofrecer", ORO, "Se la ofreces a alguien que esté en el barrio.", 150, () -> ofrecer(jugador, clave)));
        if (plugin.enVenta(clave)) {
            botones.add(boton("Quitar venta", SUAVE, "Ya no se compra al entrar.", 150, () -> {
                plugin.quitarVenta(jugador, clave);
                ficha(jugador, clave);
            }));
        } else {
            botones.add(boton("En venta", ORO, "Entre " + plugin.precioMinimo() + " y " + plugin.precioMaximo() + " duros.", 150, () -> pedirPrecio(jugador, clave, "En venta", "Escribe el precio.", null, null, false)));
        }
        if (!plugin.paraUnir(clave).isEmpty()) {
            botones.add(boton("Unir", HIERBA, "El hueco con un vecino, si acepta.", 140, () -> abrirUnir(jugador, clave)));
        }
        if (!plugin.unidasCon(clave).isEmpty()) {
            botones.add(boton("Separar", NO, "Lo construido se queda.", 140, () -> abrirSeparar(jugador, clave)));
        }
        botones.add(boton("Al banco", ORO, "La quinta parte de lo que te costó.", 150, () -> confirmarBanco(jugador, clave)));
        botones.add(boton("Abandonar", NO, "No devuelve duros. Lo construido se queda.", 150, () -> confirmarAbandonar(jugador, clave)));
        mostrar(jugador, plugin.tituloDe(clave), plugin.resumen(jugador, clave), botones, volver(jugador), 2);
    }

    private void invitar(Player jugador) {
        String aqui = plugin.claveAqui(jugador);
        if (plugin.mia(jugador, aqui)) {
            elegirGente(jugador, aqui, false);
            return;
        }
        List<ParcelasPlugin.Opcion> mias = plugin.misParcelas(jugador.getUniqueId());
        if (mias.isEmpty()) {
            aviso(jugador, "Invitar", "Todavía no tienes una parcela.");
            return;
        }
        if (mias.size() == 1) {
            elegirGente(jugador, mias.get(0).clave, false);
            return;
        }
        elegir(jugador, "Invitar", "Elige la parcela.", mias, opcion -> elegirGente(jugador, opcion.clave, false));
    }

    private void unir(Player jugador) {
        List<ParcelasPlugin.Opcion> opciones = plugin.uniblesMenu(jugador);
        if (opciones.isEmpty()) {
            aviso(jugador, "Unir", "Nadie con una parcela pegada a la tuya está en el barrio, o ya está unida. El spawn y los pasillos anchos no se unen.");
            return;
        }
        elegir(jugador, "Unir", "Elige con quién. Tiene que aceptar.", opciones, opcion -> plugin.pedirUnion(jugador, opcion.clave, opcion.otra));
    }

    private void banco(Player jugador) {
        String aqui = plugin.claveAqui(jugador);
        if (plugin.mia(jugador, aqui)) {
            confirmarBanco(jugador, aqui);
            return;
        }
        List<ParcelasPlugin.Opcion> mias = plugin.misParcelas(jugador.getUniqueId());
        if (mias.isEmpty()) {
            aviso(jugador, "Al banco", "Todavía no tienes una parcela.");
            return;
        }
        if (mias.size() == 1) {
            confirmarBanco(jugador, mias.get(0).clave);
            return;
        }
        elegir(jugador, "Al banco", "Elige cuál. El banco paga la quinta parte.", mias, opcion -> confirmarBanco(jugador, opcion.clave));
    }

    private void elegirGente(Player jugador, String clave, boolean echar) {
        List<ParcelasPlugin.Opcion> gente = plugin.gentePara(jugador, clave, echar);
        if (gente.isEmpty()) {
            aviso(jugador, echar ? "Quitar" : "Invitar", echar ? "Nadie más construye aquí." : "No hay nadie más en el barrio.");
            return;
        }
        String titulo = echar ? "Quitar" : "Invitar";
        String cuerpo = echar ? "Deja de construir en esta parcela." : "Puede construir aquí. Para el hueco, los dos dueños aceptan unirlo.";
        elegir(jugador, titulo, cuerpo, gente, opcion -> {
            if (echar) {
                plugin.restarAmigo(jugador, clave, opcion.id);
            } else {
                plugin.sumarAmigo(jugador, clave, opcion.id);
            }
            ficha(jugador, clave);
        });
    }

    private void ofrecer(Player jugador, String clave) {
        List<ParcelasPlugin.Opcion> gente = plugin.gentePara(jugador, clave, false);
        if (gente.isEmpty()) {
            aviso(jugador, "Ofrecer", "No hay nadie más en el barrio.");
            return;
        }
        elegir(jugador, "Ofrecer", "Elige a quién.", gente, opcion -> pedirPrecio(jugador, clave, "Precio", "Duros que paga " + opcion.nombre + ".", null, opcion.id, true));
    }

    private void confirmarReclamar(Player jugador) {
        if (plugin.enMora(jugador.getUniqueId())) {
            aviso(jugador, "Parcela libre", "Debes al banco. No puedes comprar una parcela hasta saldar.");
            return;
        }
        int tope = plugin.topePara(jugador.getUniqueId());
        int llevas = plugin.llevasDe(jugador.getUniqueId());
        if (llevas >= tope) {
            aviso(jugador, "Parcela libre", "Ya tienes " + tope + " parcelas.");
            return;
        }
        int precio = plugin.precioNueva(jugador.getUniqueId());
        preguntar(jugador, "Reclamar", "Cuesta " + precio + " duros. Tienes " + plugin.duros(jugador) + ".", "Comprar", () -> {
            plugin.reclamarAqui(jugador);
            abrir(jugador);
        }, "Volver", () -> abrir(jugador));
    }

    private void confirmarCompra(Player jugador, String clave) {
        Integer precio = plugin.precioPuesto(clave);
        if (precio == null) {
            aviso(jugador, "Parcela", "Ya no está en venta.");
            return;
        }
        int tasa = plugin.tasaDe(precio);
        String cuerpo = "Pagas " + precio + " duros. " + plugin.duenoNombre(clave) + " recibe " + (precio - tasa) + ". El barrio se queda " + tasa + ". Tienes " + plugin.duros(jugador) + ".";
        preguntar(jugador, "Comprar", cuerpo, "Comprar", () -> {
            plugin.comprarAqui(jugador);
            abrir(jugador);
        }, "Volver", () -> abrir(jugador));
    }

    private void confirmarAbandonar(Player jugador, String clave) {
        preguntar(jugador, "Abandonar", "No devuelve duros. Lo construido se queda. Los aldeanos vuelven a ti en fichas. El borde pasa a amarillo.", "Abandonar", () -> {
            plugin.dejarParcela(jugador, clave);
            abrir(jugador);
        }, "Volver", () -> ficha(jugador, clave));
    }

    private void confirmarSeparar(Player jugador, ParcelasPlugin.Opcion opcion) {
        preguntar(jugador, "Separar", "El hueco con " + opcion.nombre + " vuelve al barrio. Lo construido se queda.", "Separar", () -> {
            plugin.separarUnion(jugador, opcion.clave, opcion.otra);
            ficha(jugador, opcion.clave);
        }, "Volver", () -> ficha(jugador, opcion.clave));
    }

    private void pedirNombre(Player jugador, String clave, String aviso, String previo) {
        String inicial = previo != null ? previo : plugin.tituloSi(clave);
        String cuerpo = aviso == null ? "Hasta 15 letras." : aviso;
        pedirTexto(jugador, "Nombre", cuerpo, "Nombre", 15, inicial, "Poner", escrito -> {
            String limpio = escrito == null ? "" : escrito.trim().replace('§', ' ').replace('\n', ' ').trim();
            if (limpio.isEmpty()) {
                pedirNombre(jugador, clave, "Escribe un nombre.", escrito);
                return;
            }
            plugin.ponerNombre(jugador, clave, limpio);
            ficha(jugador, clave);
        }, () -> ficha(jugador, clave));
    }

    private void pedirPrecio(Player jugador, String clave, String titulo, String cuerpo, String aviso, java.util.UUID destino, boolean oferta) {
        String texto = aviso == null ? cuerpo : aviso;
        pedirTexto(jugador, titulo, texto, "Duros", 4, null, oferta ? "Ofrecer" : "Poner", escrito -> {
            Integer precio = leerEntero(escrito, plugin.precioMinimo(), plugin.precioMaximo());
            if (precio == null) {
                pedirPrecio(jugador, clave, titulo, cuerpo, "Tiene que ser un número entre " + plugin.precioMinimo() + " y " + plugin.precioMaximo() + ".", destino, oferta);
                return;
            }
            if (oferta) {
                plugin.hacerOferta(jugador, clave, destino, precio);
            } else {
                plugin.ponerEnVenta(jugador, clave, precio);
            }
            ficha(jugador, clave);
        }, () -> ficha(jugador, clave));
    }

    private void pedirTexto(Player jugador, String titulo, String cuerpo, String etiqueta, int largo, String inicial, String poner, java.util.function.Consumer<String> alPoner, Runnable alVolver) {
        String previo = inicial == null ? "" : inicial.trim();
        if (previo.length() > largo) {
            previo = previo.substring(0, largo);
        }
        var campo = DialogInput.text(CAMPO, Component.text(etiqueta)).maxLength(largo).width(240);
        if (!previo.isEmpty()) {
            campo.initial(previo);
        }
        DialogInput entrada = campo.build();
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, SUAVE), 300)))
                .inputs(List.of(entrada))
                .build())
            .type(DialogType.confirmation(
                botonVista(poner, ORO, (vista, audiencia) -> {
                    String escrito = texto(vista);
                    ejecutar(audiencia, () -> alPoner.accept(escrito));
                }),
                boton("Volver", SUAVE, "Vuelves sin guardar.", 120, alVolver)
            )));
        ((Audience) jugador).showDialog(dialog);
    }

    private void elegir(Player jugador, String titulo, String cuerpo, List<ParcelasPlugin.Opcion> opciones, java.util.function.Consumer<ParcelasPlugin.Opcion> alElegir) {
        if (opciones == null || opciones.isEmpty()) {
            aviso(jugador, titulo, "No hay nada que elegir.");
            return;
        }
        List<ActionButton> botones = new ArrayList<>();
        for (ParcelasPlugin.Opcion opcion : opciones) {
            ParcelasPlugin.Opcion fija = opcion;
            botones.add(boton(fija.nombre, TEXTO, fija.ayuda == null ? "" : fija.ayuda, 170, () -> alElegir.accept(fija)));
        }
        mostrar(jugador, titulo, cuerpo, botones, volver(jugador), botones.size() > 6 ? 2 : 1);
    }

    private void aviso(Player jugador, String titulo, String texto) {
        mostrar(jugador, titulo, texto, List.of(), volver(jugador), 1);
    }

    private void preguntar(Player jugador, String titulo, String cuerpo, String si, Runnable alSi, String no, Runnable alNo) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, SUAVE), 320)))
                .build())
            .type(DialogType.confirmation(
                boton(si, HIERBA, si + ".", 140, alSi),
                boton(no, NO, no + ".", 140, alNo)
            )));
        ((Audience) jugador).showDialog(dialog);
    }

    private ActionButton volver(Player jugador) {
        return boton("Volver", SUAVE, "Vuelve a Parcelas.", 120, () -> abrir(jugador));
    }

    private void volverMenu(Player jugador) {
        if (!jugador.performCommand("menu")) {
            jugador.sendMessage("§eEl panel no está listo.");
        }
    }

    private void mostrar(Player jugador, String titulo, String cuerpo, List<ActionButton> botones, ActionButton salida, int columnas) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(titulo, TEXTO))
                .canCloseWithEscape(true)
                .pause(true)
                .afterAction(DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(Component.text(cuerpo, SUAVE), 320)))
                .build())
            .type(botones.isEmpty()
                ? DialogType.notice(salida)
                : DialogType.multiAction(botones, salida, columnas)));
        ((Audience) jugador).showDialog(dialog);
    }

    private ActionButton boton(String etiqueta, TextColor color, String ayuda, int ancho, Runnable accion) {
        return ActionButton.builder(Component.text(etiqueta, color))
            .tooltip(Component.text(ayuda == null ? "" : ayuda))
            .width(ancho)
            .action(DialogAction.customClick((vista, audiencia) -> ejecutar(audiencia, accion), unUso()))
            .build();
    }

    private ActionButton botonVista(String etiqueta, TextColor color, DialogActionCallback accion) {
        return ActionButton.builder(Component.text(etiqueta, color))
            .width(150)
            .action(DialogAction.customClick(accion, unUso()))
            .build();
    }

    private ClickCallback.Options unUso() {
        return ClickCallback.Options.builder().uses(1).build();
    }

    private void ejecutar(Audience audiencia, Runnable accion) {
        if (!(audiencia instanceof Player jugador) || accion == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline()) {
                accion.run();
            }
        });
    }

    private static String texto(DialogResponseView vista) {
        if (vista == null) {
            return "";
        }
        try {
            String valor = vista.getText(CAMPO);
            return valor == null ? "" : valor;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static Integer leerEntero(String escrito, int minimo, int maximo) {
        if (escrito == null) {
            return null;
        }
        String limpio = escrito.trim();
        if (limpio.isEmpty() || limpio.length() > 4) {
            return null;
        }
        for (int i = 0; i < limpio.length(); i++) {
            char cifra = limpio.charAt(i);
            if (cifra < '0' || cifra > '9') {
                return null;
            }
        }
        try {
            int precio = Integer.parseInt(limpio);
            if (precio < minimo || precio > maximo) {
                return null;
            }
            return precio;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
