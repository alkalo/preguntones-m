package com.preguntones.mundos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Caras y vida del grupo, fijas arriba a la izquierda. Los duros siguen en la lista de la derecha. */
final class Marco implements Listener {
    private static final String MARCA = "grupo-hud";

    private static final TextColor TEAL = TextColor.color(0x8FD0C8);
    private static final TextColor BLANCO = TextColor.color(0xF4F7FB);
    private static final TextColor ORO = TextColor.color(0xF0D080);
    private static final TextColor ROJO = TextColor.color(0xE14B4B);
    private static final TextColor GRIS = TextColor.color(0x9AA3AD);
    private static final TextColor VACIO = TextColor.color(0x5C534C);

    private final MundosPlugin plugin;
    private final Grupo grupo;
    private final Map<UUID, Vista> vistas = new HashMap<>();
    private BukkitTask tarea;

    Marco(MundosPlugin plugin, Grupo grupo) {
        this.plugin = plugin;
        this.grupo = grupo;
    }

    void arrancar() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (World mundo : Bukkit.getWorlds()) {
            for (Entity entidad : mundo.getEntitiesByClass(Display.class)) {
                if (entidad.getScoreboardTags().contains(MARCA)) {
                    entidad.remove();
                }
            }
        }
        tarea = plugin.getServer().getScheduler().runTaskTimer(plugin, this::pulso, 1L, 1L);
    }

    void cerrar() {
        if (tarea != null) {
            tarea.cancel();
            tarea = null;
        }
        for (UUID id : new ArrayList<>(vistas.keySet())) {
            quitar(id);
        }
    }

    void pulso() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            List<UUID> gente = grupo.marcoDe(jugador.getUniqueId());
            if (gente.size() < 2) {
                quitar(jugador.getUniqueId());
                continue;
            }
            asegurar(jugador, gente);
            colocar(jugador);
        }
        List<UUID> huérfanos = new ArrayList<>();
        for (UUID id : vistas.keySet()) {
            if (Bukkit.getPlayer(id) == null) {
                huérfanos.add(id);
            }
        }
        for (UUID id : huérfanos) {
            quitar(id);
        }
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        grupo.olvidarInvitacion(event.getPlayer());
        quitar(event.getPlayer().getUniqueId());
    }

    private void asegurar(Player jugador, List<UUID> gente) {
        Vista vista = vistas.get(jugador.getUniqueId());
        if (vista != null && vista.sirve(jugador.getWorld(), gente)) {
            return;
        }
        quitar(jugador.getUniqueId());
        Vista nueva = crear(jugador, gente);
        if (nueva != null) {
            vistas.put(jugador.getUniqueId(), nueva);
        }
    }

    private Vista crear(Player jugador, List<UUID> gente) {
        World mundo = jugador.getWorld();
        if (mundo == null) {
            return null;
        }
        Vista vista = new Vista(mundo, List.copyOf(gente));
        Location ojo = ancla(jugador);
        for (int i = 0; i < gente.size() && i < Grupo.MAXIMO; i++) {
            UUID id = gente.get(i);
            int indice = i;
            ItemDisplay cara = mundo.spawn(ojo, ItemDisplay.class, entidad -> preparar(entidad, Esquina.ESCALA_CARA, Esquina.cara(indice)));
            TextDisplay texto = mundo.spawn(ojo, TextDisplay.class, entidad -> preparar(entidad, Esquina.ESCALA_TEXTO, Esquina.texto(indice)));
            cara.setItemStack(Grupo.cara(id));
            cara.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            texto.text(rotulo(jugador, id, i == 0));
            texto.setLineWidth(140);
            texto.setAlignment(TextDisplay.TextAlignment.LEFT);
            texto.setDefaultBackground(false);
            texto.setBackgroundColor(Color.fromARGB(210, 16, 20, 28));
            texto.setShadowed(false);
            texto.setSeeThrough(false);
            Fila fila = new Fila(id, cara, texto, Bukkit.getPlayer(id) != null);
            fila.vida = clave(id);
            vista.filas.add(fila);
        }
        return vista;
    }

    private void preparar(Display entidad, float escala, float[] puesto) {
        entidad.setVisibleByDefault(false);
        entidad.setPersistent(false);
        entidad.setInvulnerable(true);
        entidad.setSilent(true);
        entidad.setGravity(false);
        entidad.addScoreboardTag(MARCA);
        entidad.setBillboard(Display.Billboard.CENTER);
        entidad.setBrightness(new Display.Brightness(15, 15));
        entidad.setInterpolationDuration(0);
        entidad.setInterpolationDelay(0);
        entidad.setTeleportDuration(2);
        entidad.setShadowRadius(0);
        entidad.setShadowStrength(0);
        entidad.setDisplayWidth(0);
        entidad.setDisplayHeight(0);
        entidad.setViewRange(0.5f);
        entidad.setTransformation(forma(escala, puesto));
    }

    private void mostrar(Player jugador, Entity entidad) {
        jugador.showEntity(plugin, entidad);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (jugador.isOnline() && entidad.isValid()) {
                jugador.showEntity(plugin, entidad);
            }
        });
    }

    private void colocar(Player jugador) {
        Vista vista = vistas.get(jugador.getUniqueId());
        if (vista == null || vista.filas.isEmpty()) {
            return;
        }
        Location ojo = ancla(jugador);
        int tope = Math.min(vista.filas.size(), Grupo.MAXIMO);
        for (int i = 0; i < tope; i++) {
            Fila fila = vista.filas.get(i);
            if (!fila.cara.isValid() || !fila.texto.isValid()) {
                continue;
            }
            acercar(fila.cara, ojo);
            acercar(fila.texto, ojo);
            boolean enLinea = Bukkit.getPlayer(fila.miembro) != null;
            if (enLinea != fila.enLinea) {
                fila.enLinea = enLinea;
                fila.cara.setItemStack(Grupo.cara(fila.miembro));
            }
            String ahora = clave(fila.miembro);
            if (!ahora.equals(fila.vida)) {
                fila.vida = ahora;
                fila.texto.text(rotulo(jugador, fila.miembro, i == 0));
            }
            if (!fila.mostrada) {
                fila.mostrada = true;
                mostrar(jugador, fila.cara);
                mostrar(jugador, fila.texto);
            }
        }
    }

    private Component rotulo(Player viewer, UUID id, boolean primero) {
        Player persona = Bukkit.getPlayer(id);
        boolean yo = viewer.getUniqueId().equals(id);
        TextColor color = persona == null ? GRIS : yo ? ORO : BLANCO;
        Component nombre = plano(Grupo.nombre(id), color);
        if (primero) {
            nombre = plano("Grupo  ", TEAL).append(nombre);
        }
        return nombre.append(Component.newline()).append(barra(persona, id));
    }

    private Component barra(Player persona, UUID id) {
        if (grupo.pendiente(id)) {
            return plano(persona == null ? "Fuera" : "Invitado", TEAL);
        }
        if (persona == null) {
            return plano("Fuera", GRIS);
        }
        if (persona.isDead() || persona.getHealth() <= 0) {
            return plano("Caído", ROJO);
        }
        double max = 20;
        AttributeInstance atributo = persona.getAttribute(Attribute.MAX_HEALTH);
        if (atributo != null && atributo.getValue() > 0) {
            max = atributo.getValue();
        }
        double salud = Math.max(0, Math.min(max, persona.getHealth()));
        int llenos = (int) Math.round(10.0 * salud / max);
        llenos = Math.max(0, Math.min(10, llenos));
        int puntos = (int) Math.ceil(salud);
        Component linea = plano("❤".repeat(llenos), ROJO)
            .append(plano("❤".repeat(10 - llenos), VACIO))
            .append(plano(" " + puntos, BLANCO));
        int extra = (int) persona.getAbsorptionAmount();
        if (extra > 0) {
            linea = linea.append(plano(" +" + extra, ORO));
        }
        return linea;
    }

    private String clave(UUID id) {
        if (grupo.pendiente(id)) {
            return Bukkit.getPlayer(id) == null ? "invitado-fuera" : "invitado";
        }
        Player persona = Bukkit.getPlayer(id);
        if (persona == null) {
            return "fuera";
        }
        if (persona.isDead() || persona.getHealth() <= 0) {
            return "caido";
        }
        double max = 20;
        AttributeInstance atributo = persona.getAttribute(Attribute.MAX_HEALTH);
        if (atributo != null && atributo.getValue() > 0) {
            max = atributo.getValue();
        }
        double salud = Math.max(0, Math.min(max, persona.getHealth()));
        int llenos = (int) Math.round(10.0 * salud / max);
        return llenos + ":" + (int) Math.ceil(salud) + ":" + (int) persona.getAbsorptionAmount();
    }

    private static Component plano(String texto, TextColor color) {
        return Component.text(texto, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Location ancla(Player jugador) {
        Location ojo = jugador.getEyeLocation();
        ojo.setYaw(0);
        ojo.setPitch(0);
        return ojo;
    }

    private static void acercar(Entity entidad, Location ojo) {
        Location actual = entidad.getLocation();
        if (actual.getWorld() == null || ojo.getWorld() == null || !actual.getWorld().equals(ojo.getWorld())) {
            entidad.teleport(ojo);
            return;
        }
        if (actual.distanceSquared(ojo) < 0.0004) {
            return;
        }
        entidad.teleport(ojo);
    }

    private static Transformation forma(float escala, float[] puesto) {
        return new Transformation(
            new Vector3f(puesto[0], puesto[1], puesto[2]),
            new Quaternionf(),
            new Vector3f(escala, escala, escala),
            new Quaternionf()
        );
    }

    private void quitar(UUID id) {
        Vista vista = vistas.remove(id);
        if (vista == null) {
            return;
        }
        for (Fila fila : vista.filas) {
            borrar(fila.cara);
            borrar(fila.texto);
        }
    }

    private static void borrar(Entity entidad) {
        if (entidad != null && entidad.isValid()) {
            entidad.remove();
        }
    }

    private static final class Vista {
        final World mundo;
        final List<UUID> firma;
        final List<Fila> filas = new ArrayList<>();

        Vista(World mundo, List<UUID> firma) {
            this.mundo = mundo;
            this.firma = firma;
        }

        boolean sirve(World actual, List<UUID> gente) {
            if (actual == null || !actual.equals(mundo) || !firma.equals(gente)) {
                return false;
            }
            for (Fila fila : filas) {
                if (fila.cara == null || !fila.cara.isValid() || fila.texto == null || !fila.texto.isValid()) {
                    return false;
                }
            }
            return filas.size() == gente.size();
        }
    }

    private static final class Fila {
        final UUID miembro;
        final ItemDisplay cara;
        final TextDisplay texto;
        boolean enLinea;
        String vida = "";
        boolean mostrada;

        Fila(UUID miembro, ItemDisplay cara, TextDisplay texto, boolean enLinea) {
            this.miembro = miembro;
            this.cara = cara;
            this.texto = texto;
            this.enLinea = enLinea;
        }
    }
}
