package com.preguntones.viajes;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

final class Cuaderno {
    private static final byte VERSION = 19;
    private static final String TITULO = "Cuaderno del barrio";
    private static final TextColor TINTA = TextColor.color(0x2C2416);
    private static final TextColor SUAVE = TextColor.color(0x5C5346);
    private static final TextColor ORO = TextColor.color(0x8A5A12);
    private static final TextColor ENLACE = TextColor.color(0x1A4F8B);

    private Cuaderno() {
    }

    static void entregar(ViajesPlugin plugin, Player jugador) {
        NamespacedKey clave = new NamespacedKey(plugin, "cuaderno");
        Byte tengo = jugador.getPersistentDataContainer().get(clave, PersistentDataType.BYTE);
        if (tengo != null && tengo >= VERSION) {
            return;
        }
        if (tengo != null) {
            quitarViejo(jugador);
        }
        ItemStack nuevo = crear();
        ItemStack abierto = nuevo.clone();
        Map<Integer, ItemStack> sobra = jugador.getInventory().addItem(nuevo);
        for (ItemStack item : sobra.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), item);
        }
        jugador.openBook(abierto);
        jugador.playSound(jugador.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.45f, 1.05f);
        jugador.getPersistentDataContainer().set(clave, PersistentDataType.BYTE, VERSION);
    }

    private static ItemStack crear() {
        ItemStack libro = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) libro.getItemMeta();
        meta.setTitle(TITULO);
        meta.setAuthor("Preguntones");
        meta.title(Component.text(TITULO, ORO).decoration(TextDecoration.ITALIC, false));
        meta.author(Component.text("Preguntones", TINTA).decoration(TextDecoration.ITALIC, false));
        meta.displayName(Component.text(TITULO, ORO).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
            Component.text("Veinte páginas.", SUAVE).decoration(TextDecoration.ITALIC, false),
            Component.text("Lo azul se pulsa.", SUAVE).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.addPages(
            portada(),
            indice(),
            parcela(),
            precio(),
            camino(),
            trato(),
            duros(),
            cofre(),
            banco(),
            panel(),
            bandera(),
            viajes(),
            survival(),
            dragon(),
            xo(),
            xoJuego(),
            clan(),
            pelea(),
            duelo(),
            normas()
        );
        libro.setItemMeta(meta);
        return libro;
    }

    private static Component portada() {
        return unir(
            titulo("Preguntones"),
            salto(),
            tinta("Estás en el barrio.\nLo de aquí se queda."),
            salto(),
            tinta("El survival, otro\nmundo, se borra\na las 48 horas\ntras el dragón.\nLas parcelas, no."),
            salto(),
            tinta("La primera parcela\ncuesta 25 duros.\nSale de una tarde."),
            nl(),
            mando("Panel Preguntones", "/menu", "También puedes escribir /menu.")
        );
    }

    private static Component indice() {
        return unir(
            titulo("Índice"),
            salto(),
            fila("Parcela", 3, "Precio", 4), nl(),
            fila("Camino", 5, "Vender", 6), nl(),
            fila("Duros", 7, "Cofre", 8), nl(),
            fila("Banco", 9, "Panel", 10), nl(),
            fila("Bandera", 11, "Viajes", 12), nl(),
            fila("Survival", 13, "Dragón", 14), nl(),
            fila("La XO", 15, "Clan", 17), nl(),
            fila("Pelea", 18, "Duelo", 19), nl(),
            pagina("Normas", 20)
        );
    }

    private static Component parcela() {
        return unir(
            titulo("La parcela"),
            salto(),
            tinta("Amarillo si está libre.\nRojo si tiene dueño."),
            salto(),
            tinta("Pala de oro.\nClic derecho ahí.\nUn lingote de oro\ny dos palos."),
            salto(),
            tinta("La primera: 25.\nEs entera tuya."),
            nl(),
            mando("Ir a una libre", "/parcela cerca", "Te lleva a una parcela libre. El comando es /parcela cerca."),
            nl(),
            volver()
        );
    }

    private static Component precio() {
        return unir(
            titulo("El precio"),
            salto(),
            tinta("1ª  25    2ª  55\n3ª  95    4ª 145\n5ª 205    6ª 275"),
            salto(),
            tinta("Tope: 6 por persona.\nUn clan no lo sube."),
            salto(),
            tinta("Al banco, la quinta\nparte de lo que costó.\n25 da 5. 275 da 55.\nAbandonar no devuelve.\nLo construido se queda."),
            nl(),
            mando("Abrir parcelas", "/parcela panel", "También en el Panel Preguntones, botón Parcelas."),
            nl(),
            volver()
        );
    }

    private static Component camino() {
        return unir(
            titulo("El camino"),
            salto(),
            tinta("El hueco, 4 bloques.\nEs de todos."),
            salto(),
            tinta("Si los dos lados\nson tuyos, pasa\na hierba y la unes."),
            salto(),
            tinta("Con un amigo:\nlos dos aceptáis\nen el panel, Parcelas.\nEse hueco pasa a hierba."),
            salto(),
            tinta("Cuatro en cruz:\nel centro también."),
            nl(),
            sugerir("Dejar construir", "/parcela invitar ", "Escribe su nombre. Tiene que estar en el barrio. Para el hueco, invítalo en las dos. El comando es /parcela invitar."),
            nl(),
            volver()
        );
    }

    private static Component trato() {
        return unir(
            titulo("Vender"),
            salto(),
            tinta("Dentro, para vender."),
            nl(),
            sugerir("/parcela ofrecer", "/parcela ofrecer ", "Escribe su nombre y el precio. Tiene que estar conectado. Tú, dentro de tu parcela."),
            nl(),
            tinta("nombre y precio."),
            nl(),
            mando("/parcela aceptar", "/parcela aceptar", "Acepta la oferta que te han hecho."),
            nl(),
            sugerir("/parcela vender", "/parcela vender ", "Escribe el precio, entre 20 y 2000 duros. Tú, dentro."),
            nl(),
            tinta("y un precio."),
            nl(),
            mando("/parcela comprar", "/parcela comprar", "Compra la parcela en venta donde estás."),
            nl(),
            tinta("quien esté dentro."),
            salto(),
            tinta("Vendedor: el 90 %.\nBarrio: el 10 %."),
            nl(),
            volver()
        );
    }

    private static Component duros() {
        return unir(
            titulo("Los duros"),
            salto(),
            tinta("Te los dan solos\nen el survival:\ntalar, picar y más.\ny al vender a otro.\nEl euro no los crea."),
            salto(),
            tinta("Escape, Progresos.\nCada uno dice\nlos duros que da."),
            salto(),
            tinta("La web es aspecto:\ncolor, estela, frase.\nNi terreno ni vuelo."),
            nl(),
            sugerir("Dar duros", "/pay ", "Escribe el nombre y cuántos duros. El comando es /pay."),
            nl(),
            mando("Tienda", "/puesto", "Bloques, luces y muebles. El botón Aldeano, en el centro, cuesta 40 duros. La bandera, aparte."),
            nl(),
            volver()
        );
    }

    private static Component cofre() {
        return unir(
            titulo("El cofre"),
            salto(),
            tinta("Se craftea:\ncuatro lingotes\nde oro, un cofre\ny un cartel de roble."),
            salto(),
            tinta("En tu parcela.\nHasta cuatro.\nShift: metes género\ny le pones precio."),
            salto(),
            tinta("Quien compra\nelige cuántos.\nEl barrio se queda\nel 10 %."),
            nl(),
            volver()
        );
    }

    private static Component banco() {
        return unir(
            titulo("El banco"),
            salto(),
            tinta("Está en el panel.\nEscribes los duros,\nhasta lo que haya\nen la caja."),
            salto(),
            tinta("1 día, el 10 %.\n3 días, el 20 %.\n7 días, el 30 %.\nAceptas el total."),
            salto(),
            tinta("Hasta 50, si no\npagas, te quita\nduros. Más: una\nparcela de aval."),
            nl(),
            mando("Abrir el banco", "/menu banco", "También está en el panel. El comando /banco abre lo mismo."),
            nl(),
            volver()
        );
    }

    private static void quitarViejo(Player jugador) {
        ItemStack[] contenido = jugador.getInventory().getContents();
        for (int i = 0; i < contenido.length; i++) {
            ItemStack item = contenido[i];
            if (item != null && item.getType() == Material.WRITTEN_BOOK && item.getItemMeta() instanceof BookMeta meta
                && TITULO.equals(meta.getTitle())) {
                jugador.getInventory().setItem(i, null);
            }
        }
        ItemStack mano = jugador.getInventory().getItemInOffHand();
        if (mano.getType() == Material.WRITTEN_BOOK && mano.getItemMeta() instanceof BookMeta meta && TITULO.equals(meta.getTitle())) {
            jugador.getInventory().setItemInOffHand(null);
        }
    }

    private static Component panel() {
        return unir(
            titulo("El panel"),
            salto(),
            tinta("Pulsa Escape.\nAbajo, botón dorado,\naparte del menú."),
            salto(),
            tinta("Spawn, un jugador,\nsurvival, muerte,\neventos, PVP,\nparcelas, tienda,\nbandera, banco\ny grupo."),
            salto(),
            tinta("Quince segundos.\nSi hay peligro, no."),
            nl(),
            mando("Abrir el panel", "/menu", "El mismo botón dorado de Escape. El comando es /menu."),
            nl(),
            volver()
        );
    }

    private static Component bandera() {
        return unir(
            titulo("La bandera"),
            salto(),
            tinta("Cuesta 40 duros.\nBotón Bandera\ndel panel."),
            nl(),
            mando("Comprar bandera", "/bandera", "También está en el panel, botón Bandera."),
            salto(),
            tinta("Donde construyas.\nPonla: sale en Spawn."),
            salto(),
            tinta("Hasta cinco.\nSolo tú la quitas."),
            salto(),
            tinta("Barrio: se queda."),
            nl(),
            pagina("Fuera, vuelve.", 13, "En el survival, el Nether o el End, al borrarse ese mundo, vuelve a ti."),
            nl(),
            volver()
        );
    }

    private static Component viajes() {
        return unir(
            titulo("Los viajes"),
            salto(),
            tinta("Spawn, cama y bandera.\nTardan 15 segundos."),
            salto(),
            tinta("Ir junto a alguien:\nesa persona acepta.\nLuego, 15 segundos."),
            salto(),
            tinta("Se cancelan en lava,\nsi te ahogas\no si te atacan."),
            nl(),
            mando("Volver al barrio", "/volver", "Tarda 15 segundos. Se cancela en lava, si te ahogas o si te atacan."),
            nl(),
            volver()
        );
    }

    private static Component survival() {
        return unir(
            titulo("El survival"),
            salto(),
            tinta("Explorar: al azar.\nA tu lado vienen\nsolo si aceptan."),
            salto(),
            tinta("Tu cama: duerme\nallí, Nether o End.\nLuego tarda 15 s.\nLa muerte: " + Huella.PRECIO + " duros,\nuna vez."),
            salto(),
            tinta("Ese mundo se borra.\nEl barrio sigue."),
            nl(),
            mando("Cuándo se borra", "/ciclo", "Dice el día en que se borra el survival y cuándo se puede entrar al End."),
            nl(),
            volver()
        );
    }

    private static Component dragon() {
        return unir(
            titulo("El dragón"),
            salto(),
            tinta("Cada ciclo. A las 48 h\nse borra ese mapa."),
            salto(),
            tinta("1: 14 días, 12 blazes.\n2: bastión y ciudad.\nLuego se turnan."),
            salto(),
            tinta("Sin explosiones\nni élitros. Más vida.\nCon cristales, poco daño."),
            salto(),
            tinta("Hasta 4, en el panel.\n20 duros y Cazador.\nLa ficha, en /puesto.\n40 duros."),
            nl(),
            mando("Abrir el grupo", "/grupo", "Botón Grupo del panel. Hasta 4. La cara y la vida se quedan arriba a la izquierda."),
            nl(),
            volver()
        );
    }

    private static Component xo() {
        return unir(
            titulo("La XO"),
            salto(),
            tinta("Sábado, 21:30.\nHasta las 22:30.\nHora de España.\nEmpieza a las 21:35."),
            salto(),
            tinta("Entra por el panel\nsi está abierta."),
            salto(),
            tinta("Cada 10 minutos\nempieza una ronda.\nHacen falta dos."),
            nl(),
            pagina("Cómo se juega", 16, "La frase, dónde ponerse y los 8 duros. Si usas Discord, la voz es voz-sabado.")
        );
    }

    private static Component xoJuego() {
        return unir(
            titulo("En la ronda"),
            salto(),
            tinta("Lees una frase.\nPonte en X o en O.\nX es falso.\nO es verdadero.\nCinco segundos.\nSe te ve, sin golpes."),
            salto(),
            tinta("Si fallas, miras.\nSi aciertas, sigue.\nEl último: 8 duros."),
            nl(),
            volver()
        );
    }

    private static Component clan() {
        return unir(
            titulo("El clan"),
            salto(),
            tinta("Hasta 8 personas.\nNo da más parcelas.\nSigues con seis."),
            salto(),
            sugerir("/clan crear", "/clan crear ", "Escribe el nombre del clan."),
            nl(),
            sugerir("/clan invitar", "/clan invitar ", "Escribe el nombre de alguien conectado."),
            nl(),
            mando("/clan aceptar", "/clan aceptar", "Acepta la invitación."),
            nl(),
            mando("/clan voz", "/clan voz", "El jefe abre una sala de voz. No hace falta Discord para jugar."),
            salto(),
            tinta("Se juega igual\nsin Discord."),
            nl(),
            volver()
        );
    }

    private static Component pelea() {
        return unir(
            titulo("La pelea"),
            salto(),
            tinta("No golpeas a nadie,\nsalvo karma negativo\no modo agresivo."),
            salto(),
            tinta("El modo, a los 3 días,\nen PVP. Entonces\npuedes, y te pueden.\nSi no lo lleva,\nbaja tu karma."),
            nl(),
            pagina("El duelo", 19, "Cómo retar, y qué quiere decir la palabra junto al nombre.")
        );
    }

    private static Component duelo() {
        return unir(
            titulo("El duelo"),
            salto(),
            tinta("Se reta en PVP.\nTarda 15 segundos.\nNi karma ni objetos.\nLuego, a su sitio.\nEn la XO, no hay."),
            salto(),
            tinta("Bueno paga menos.\nNegativo suelta\nlo que lleva.\nCruel pierde duros."),
            nl(),
            volver()
        );
    }

    private static Component normas() {
        return unir(
            titulo("Normas"),
            salto(),
            tinta("Español y de tú.\nEl insulto se corta."),
            salto(),
            tinta("No toques lo ajeno.\nNi el spawn."),
            nl(),
            tinta("A las 05:00 reinicia\ny no borra parcelas."),
            nl(),
            tinta("Soporte en Discord,\ncon tu nombre.\nNo mandes claves."),
            nl(),
            volver()
        );
    }

    private static Component titulo(String texto) {
        return Component.text(texto, ORO, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static Component tinta(String texto) {
        return plano(texto, TINTA);
    }

    private static Component plano(String texto, TextColor color) {
        return Component.text(texto, color)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false);
    }

    private static Component mando(String visible, String comando, String ayuda) {
        return enlace(visible, ayuda).clickEvent(ClickEvent.runCommand(comando));
    }

    private static Component sugerir(String visible, String comando, String ayuda) {
        return enlace(visible, ayuda).clickEvent(ClickEvent.suggestCommand(comando));
    }

    private static Component fila(String izquierda, int paginaIzquierda, String derecha, int paginaDerecha) {
        return pagina(izquierda, paginaIzquierda)
            .append(plano("    ", SUAVE))
            .append(pagina(derecha, paginaDerecha));
    }

    private static Component pagina(String visible, int numero) {
        return pagina(visible, numero, "Página " + numero);
    }

    private static Component pagina(String visible, int numero, String ayuda) {
        return enlace(visible, ayuda).clickEvent(ClickEvent.changePage(numero));
    }

    private static TextComponent enlace(String visible, String ayuda) {
        return Component.text(visible, ENLACE)
            .decorate(TextDecoration.UNDERLINED)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false)
            .hoverEvent(HoverEvent.showText(Component.text(ayuda, ORO).decoration(TextDecoration.ITALIC, false)));
    }

    private static Component volver() {
        return pagina("Índice", 2, "Vuelve a la lista.");
    }

    private static Component salto() {
        return Component.newline().append(Component.newline());
    }

    private static Component nl() {
        return Component.newline();
    }

    private static Component unir(Component... partes) {
        Component pagina = Component.empty();
        for (Component parte : partes) {
            pagina = pagina.append(parte);
        }
        return pagina;
    }
}
