package com.preguntones.karma;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

final class Duelos implements Listener {
    private final KarmaPlugin plugin;
    private final Map<UUID, Reto> retos = new HashMap<>();
    private final Map<UUID, Viaje> viajes = new HashMap<>();
    private final Map<UUID, Pelea> peleas = new HashMap<>();

    Duelos(KarmaPlugin plugin) {
        this.plugin = plugin;
    }

    void latido() {
        long ahora = System.currentTimeMillis();
        Iterator<Reto> retosPendientes = new ArrayList<>(retos.values()).iterator();
        while (retosPendientes.hasNext()) {
            Reto reto = retosPendientes.next();
            if (reto.expira > ahora) {
                continue;
            }
            retos.remove(reto.para);
            avisar(reto.de, "§7" + reto.nombrePara + " no ha contestado.");
            avisar(reto.para, "§7Se ha pasado el reto de " + reto.nombreDe + ".");
        }
        for (UUID id : new ArrayList<>(viajes.keySet())) {
            Viaje viaje = viajes.get(id);
            if (viaje != null) {
                avanzarViaje(viaje);
            }
        }
        for (Pelea pelea : new ArrayList<>(new java.util.HashSet<>(peleas.values()))) {
            if (!pelea.activa) {
                avanzarCuenta(pelea);
            }
        }
    }

    void cerrar() {
        for (Viaje viaje : new ArrayList<>(viajes.values())) {
            cortarViaje(viaje, "Se ha cortado. El servidor se cierra.");
        }
        for (Pelea pelea : new ArrayList<>(new java.util.HashSet<>(peleas.values()))) {
            terminar(pelea, null);
            avisar(pelea.reta, "§7El duelo se ha cortado.");
            avisar(pelea.acepta, "§7El duelo se ha cortado.");
        }
        retos.clear();
    }

    boolean ocupado(Player jugador) {
        if (jugador == null) {
            return false;
        }
        UUID id = jugador.getUniqueId();
        return viajes.containsKey(id) || peleas.containsKey(id);
    }

    boolean golpeEntre(Player atacante, Player victima) {
        Pelea pelea = peleas.get(atacante.getUniqueId());
        return pelea != null && pelea.activa && pelea == peleas.get(victima.getUniqueId());
    }

    void retar(Player de, Player para) {
        if (para == null || !para.isOnline() || de.equals(para)) {
            de.sendMessage("§cEsa persona ya no está.");
            return;
        }
        if (plugin.esXo(de) || plugin.esXo(para)) {
            de.sendMessage("§cEn la XO no hay duelos.");
            return;
        }
        if (ocupado(de) || ocupado(para) || retoDe(de.getUniqueId()) != null || retos.containsKey(para.getUniqueId())) {
            de.sendMessage("§cAhora no se puede retar. Hay un duelo o un reto pendiente.");
            return;
        }
        int segundos = Math.max(5, plugin.entero("reto-segundos", 30));
        Reto reto = new Reto(de.getUniqueId(), de.getName(), para.getUniqueId(), para.getName(),
            System.currentTimeMillis() + segundos * 1000L);
        retos.put(para.getUniqueId(), reto);
        de.sendMessage("§aHas retado a " + para.getName() + ". §7Tiene " + segundos + " segundos.");
        para.sendMessage(Component.text(de.getName() + " te reta a un duelo. ", TextColor.color(0xE6B84D))
            .append(Component.text("Aceptar", TextColor.color(0x8FE3A0))
                .clickEvent(ClickEvent.runCommand("/karma aceptar")))
            .append(Component.text(" · ", TextColor.color(0xC5CED8)))
            .append(Component.text("Rechazar", TextColor.color(0xE07A7A))
                .clickEvent(ClickEvent.runCommand("/karma rechazar"))));
        plugin.ajustes().mostrarReto(para, de.getName());
    }

    void aceptar(Player jugador) {
        Reto reto = retos.remove(jugador.getUniqueId());
        if (reto == null) {
            jugador.sendMessage("§7No tienes ningún reto.");
            return;
        }
        if (reto.expira < System.currentTimeMillis()) {
            jugador.sendMessage("§7Se ha pasado el reto.");
            return;
        }
        Player de = Bukkit.getPlayer(reto.de);
        if (de == null || !de.isOnline()) {
            jugador.sendMessage("§cEsa persona ya no está.");
            return;
        }
        if (plugin.esXo(de) || plugin.esXo(jugador)) {
            avisar(de.getUniqueId(), "§cEn la XO no hay duelos.");
            jugador.sendMessage("§cEn la XO no hay duelos.");
            return;
        }
        if (ocupado(de) || ocupado(jugador)) {
            avisar(de.getUniqueId(), "§cEl duelo no ha podido empezar.");
            jugador.sendMessage("§cEl duelo no ha podido empezar.");
            return;
        }
        empezarViaje(de, jugador);
    }

    void rechazar(Player jugador) {
        Reto reto = retos.remove(jugador.getUniqueId());
        if (reto == null) {
            jugador.sendMessage("§7No tienes ningún reto.");
            return;
        }
        jugador.sendMessage("§7Has rechazado el duelo.");
        avisar(reto.de, "§7" + jugador.getName() + " no acepta el duelo.");
    }

    boolean alMorir(PlayerDeathEvent event) {
        Player muerto = event.getEntity();
        Pelea pelea = peleas.get(muerto.getUniqueId());
        if (pelea == null) {
            return false;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        if (!pelea.activa) {
            terminar(pelea, muerto);
            avisar(pelea.reta, "§7El duelo se ha cortado.");
            avisar(pelea.acepta, "§7El duelo se ha cortado.");
            return true;
        }
        UUID ganador = muerto.getUniqueId().equals(pelea.reta) ? pelea.acepta : pelea.reta;
        String nombreGanador = ganador.equals(pelea.reta) ? pelea.nombreReta : pelea.nombreAcepta;
        String nombrePerdedor = muerto.getName();
        avisar(ganador, "§aHas ganado el duelo contra " + nombrePerdedor + ".");
        titulo(ganador, "Has ganado");
        muerto.sendMessage("§6Has perdido el duelo contra " + nombreGanador + ".");
        terminar(pelea, muerto);
        return true;
    }

    boolean llevar(Player jugador, Location sitio) {
        if (jugador == null || sitio == null || sitio.getWorld() == null) {
            return false;
        }
        jugador.leaveVehicle();
        jugador.setFallDistance(0f);
        plugin.marcarTeleporte(jugador.getUniqueId());
        boolean bien;
        try {
            bien = jugador.teleport(sitio);
        } finally {
            plugin.soltarTeleporte(jugador.getUniqueId());
        }
        if (bien) {
            jugador.setFallDistance(0f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
        return bien;
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        Player jugador = event.getPlayer();
        UUID id = jugador.getUniqueId();
        Reto entrante = retos.remove(id);
        if (entrante != null) {
            avisar(entrante.de, "§7" + jugador.getName() + " ya no está.");
        }
        Reto saliente = retoDe(id);
        if (saliente != null) {
            retos.remove(saliente.para);
            avisar(saliente.para, "§7" + jugador.getName() + " ya no está.");
        }
        Viaje viaje = viajes.get(id);
        if (viaje != null) {
            cortarViaje(viaje, "§7" + jugador.getName() + " ya no está.");
        } else {
            for (Viaje otro : new ArrayList<>(viajes.values())) {
                if (otro.anfitrion.equals(id)) {
                    cortarViaje(otro, "§7" + jugador.getName() + " ya no está.");
                }
            }
        }
        Pelea pelea = peleas.get(id);
        if (pelea == null) {
            return;
        }
        UUID ganador = id.equals(pelea.reta) ? pelea.acepta : pelea.reta;
        String nombre = jugador.getName();
        boolean activa = pelea.activa;
        terminar(pelea, jugador);
        if (activa) {
            avisar(ganador, "§aHas ganado el duelo contra " + nombre + ".");
            titulo(ganador, "Has ganado");
        } else {
            avisar(ganador, "§7El duelo se ha cortado.");
        }
    }

    @EventHandler
    public void alRevivir(PlayerRespawnEvent event) {
        if (!plugin.tieneVuelta(event.getPlayer().getUniqueId()) || ocupado(event.getPlayer())) {
            return;
        }
        Location sitio = plugin.tomarVuelta(event.getPlayer().getUniqueId());
        if (sitio != null) {
            event.setRespawnLocation(sitio);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alTeleport(PlayerTeleportEvent event) {
        if (plugin.esTeleportePropio(event.getPlayer().getUniqueId())) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        Viaje viaje = viajes.get(id);
        if (viaje != null) {
            cortarViaje(viaje, "§cSe ha cortado. Te has movido.");
            return;
        }
        if (peleas.containsKey(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDurante el duelo no puedes irte.");
        }
    }

    private void empezarViaje(Player anfitrion, Player viajero) {
        String motivo = plugin.motivoDe(viajero);
        if (motivo != null) {
            viajero.sendMessage("§c" + plugin.textoAhora(motivo));
            viajero.playSound(viajero.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            anfitrion.sendMessage("§c" + viajero.getName() + " no puede salir ahora.");
            return;
        }
        int espera = Math.max(1, plugin.entero("espera-segundos", 15));
        BossBar barra = Bukkit.createBossBar("Al duelo · " + espera + " s", BarColor.GREEN, BarStyle.SOLID);
        barra.addPlayer(viajero);
        barra.addPlayer(anfitrion);
        barra.setProgress(1.0);
        Viaje viaje = new Viaje(
            viajero.getUniqueId(),
            viajero.getName(),
            anfitrion.getUniqueId(),
            anfitrion.getName(),
            viajero.getLocation().clone(),
            barra,
            espera * 2
        );
        viajes.put(viajero.getUniqueId(), viaje);
        viajero.sendMessage("§aLa arena os espera en " + espera + " segundos. §7Si entras en lava, te ahogas o te ataca un mob, se cancela.");
        anfitrion.sendMessage("§aLa arena os espera en " + espera + " segundos.");
        viajero.playSound(viajero.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    private void avanzarViaje(Viaje viaje) {
        Player viajero = Bukkit.getPlayer(viaje.viajero);
        Player anfitrion = Bukkit.getPlayer(viaje.anfitrion);
        if (viajero == null || !viajero.isOnline() || anfitrion == null || !anfitrion.isOnline()) {
            cortarViaje(viaje, "§cEsa persona ya no está.");
            return;
        }
        if (viajero.isDead() || anfitrion.isDead()) {
            cortarViaje(viaje, "§cEl duelo no ha podido empezar.");
            return;
        }
        if (plugin.esXo(viajero) || plugin.esXo(anfitrion)) {
            cortarViaje(viaje, "§cEn la XO no hay duelos.");
            return;
        }
        String motivo = plugin.motivoDe(viajero);
        if (motivo != null) {
            cortarViaje(viaje, "§c" + plugin.textoCorte(motivo));
            return;
        }
        viaje.medios++;
        if (viaje.medios < viaje.totalMedios) {
            int quedan = (int) Math.ceil((viaje.totalMedios - viaje.medios) / 2.0);
            String texto = "Al duelo · " + Math.max(quedan, 1) + " s";
            viaje.barra.setTitle(texto);
            viaje.barra.setProgress(Math.max(0.0, (viaje.totalMedios - viaje.medios) / (double) viaje.totalMedios));
            viajero.sendActionBar("§b" + texto);
            return;
        }
        viajes.remove(viaje.viajero);
        viaje.barra.removeAll();
        String peligro = plugin.motivoDe(anfitrion);
        if (peligro != null) {
            String texto = "§c" + plugin.textoCorteAjeno(anfitrion.getName(), peligro);
            viajero.sendMessage(texto);
            anfitrion.sendMessage(texto);
            viajero.playSound(viajero.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (anfitrion.isDead() || viajero.isDead()) {
            viajero.sendMessage("§cEl duelo no ha podido empezar.");
            anfitrion.sendMessage("§cEl duelo no ha podido empezar.");
            return;
        }
        if (!plugin.arena().lista() || plugin.arena().oeste() == null || plugin.arena().este() == null) {
            viajero.sendMessage("§cLa arena no está lista.");
            anfitrion.sendMessage("§cLa arena no está lista.");
            return;
        }
        Location origenViajero = viajero.getLocation().clone();
        Location origenAnfitrion = anfitrion.getLocation().clone();
        plugin.guardarVuelta(viajero.getUniqueId(), origenViajero);
        plugin.guardarVuelta(anfitrion.getUniqueId(), origenAnfitrion);
        boolean uno = llevar(viajero, plugin.arena().oeste());
        boolean dos = uno && llevar(anfitrion, plugin.arena().este());
        if (!uno || !dos) {
            deshacer(viajero, origenViajero, uno);
            deshacer(anfitrion, origenAnfitrion, dos);
            viajero.sendMessage("§cNo he podido llevaros a la arena.");
            anfitrion.sendMessage("§cNo he podido llevaros a la arena.");
            return;
        }
        Pelea pelea = new Pelea(
            viaje.anfitrion, viaje.nombreAnfitrion, viaje.viajero, viaje.nombreViajero, origenAnfitrion, origenViajero);
        peleas.put(pelea.reta, pelea);
        peleas.put(pelea.acepta, pelea);
        titulo(pelea.reta, "3");
        titulo(pelea.acepta, "3");
        viajero.sendMessage("§6El duelo empieza en 3 segundos. §7El karma no cambia y no pierdes objetos.");
        anfitrion.sendMessage("§6El duelo empieza en 3 segundos. §7El karma no cambia y no pierdes objetos.");
    }

    private void avanzarCuenta(Pelea pelea) {
        Player reta = Bukkit.getPlayer(pelea.reta);
        Player acepta = Bukkit.getPlayer(pelea.acepta);
        if (reta == null || acepta == null || !reta.isOnline() || !acepta.isOnline()) {
            terminar(pelea, null);
            avisar(pelea.reta, "§7El duelo se ha cortado.");
            avisar(pelea.acepta, "§7El duelo se ha cortado.");
            return;
        }
        if (reta.isDead() || acepta.isDead()) {
            Player muerto = reta.isDead() ? reta : acepta;
            UUID ganador = muerto.getUniqueId().equals(pelea.reta) ? pelea.acepta : pelea.reta;
            boolean activa = pelea.activa;
            terminar(pelea, muerto);
            if (activa) {
                avisar(ganador, "§aHas ganado el duelo contra " + muerto.getName() + ".");
                titulo(ganador, "Has ganado");
                muerto.sendMessage("§6Has perdido el duelo contra " + (ganador.equals(pelea.reta) ? pelea.nombreReta : pelea.nombreAcepta) + ".");
            } else {
                avisar(pelea.reta, "§7El duelo se ha cortado.");
                avisar(pelea.acepta, "§7El duelo se ha cortado.");
            }
            return;
        }
        pelea.medios++;
        if (pelea.medios == 2) {
            titulo(pelea.reta, "2");
            titulo(pelea.acepta, "2");
        } else if (pelea.medios == 4) {
            titulo(pelea.reta, "1");
            titulo(pelea.acepta, "1");
        } else if (pelea.medios >= 6) {
            pelea.activa = true;
            titulo(pelea.reta, "Duelo");
            titulo(pelea.acepta, "Duelo");
            reta.sendMessage("§aEl duelo ha empezado.");
            acepta.sendMessage("§aEl duelo ha empezado.");
        }
    }

    private void cortarViaje(Viaje viaje, String texto) {
        viajes.remove(viaje.viajero);
        viaje.barra.removeAll();
        avisar(viaje.viajero, texto);
        avisar(viaje.anfitrion, texto);
        Player viajero = Bukkit.getPlayer(viaje.viajero);
        if (viajero != null && viajero.isOnline()) {
            viajero.playSound(viajero.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void deshacer(Player jugador, Location origen, boolean movido) {
        if (!movido) {
            plugin.tomarVuelta(jugador.getUniqueId());
            return;
        }
        if (llevar(jugador, origen)) {
            plugin.tomarVuelta(jugador.getUniqueId());
        }
    }

    private void terminar(Pelea pelea, Player fuera) {
        peleas.remove(pelea.reta);
        peleas.remove(pelea.acepta);
        devolver(pelea.reta, pelea.origenReta, fuera);
        devolver(pelea.acepta, pelea.origenAcepta, fuera);
    }

    private void devolver(UUID id, Location origen, Player fuera) {
        if (fuera != null && fuera.getUniqueId().equals(id)) {
            if (!plugin.tieneVuelta(id)) {
                plugin.guardarVuelta(id, origen);
            }
            return;
        }
        Player jugador = Bukkit.getPlayer(id);
        if (jugador != null && jugador.isOnline() && !jugador.isDead()) {
            Location sitio = plugin.tomarVuelta(id);
            if (sitio == null) {
                sitio = origen;
            }
            if (llevar(jugador, sitio)) {
                jugador.sendMessage("§aHas vuelto a tu sitio.");
            } else {
                plugin.guardarVuelta(id, sitio);
            }
            return;
        }
        if (!plugin.tieneVuelta(id)) {
            plugin.guardarVuelta(id, origen);
        }
    }

    private Reto retoDe(UUID id) {
        for (Reto reto : retos.values()) {
            if (reto.de.equals(id)) {
                return reto;
            }
        }
        return null;
    }

    private void titulo(UUID id, String texto) {
        Player jugador = Bukkit.getPlayer(id);
        if (jugador == null || !jugador.isOnline() || jugador.isDead()) {
            return;
        }
        jugador.showTitle(Title.title(
            Component.text(texto),
            Component.empty(),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(150))
        ));
    }

    private void avisar(UUID id, String texto) {
        Player jugador = Bukkit.getPlayer(id);
        if (jugador != null && jugador.isOnline()) {
            jugador.sendMessage(texto);
        }
    }

    private static final class Reto {
        private final UUID de;
        private final String nombreDe;
        private final UUID para;
        private final String nombrePara;
        private final long expira;

        private Reto(UUID de, String nombreDe, UUID para, String nombrePara, long expira) {
            this.de = de;
            this.nombreDe = nombreDe;
            this.para = para;
            this.nombrePara = nombrePara;
            this.expira = expira;
        }
    }

    private static final class Viaje {
        private final UUID viajero;
        private final String nombreViajero;
        private final UUID anfitrion;
        private final String nombreAnfitrion;
        private final Location origen;
        private final BossBar barra;
        private final int totalMedios;
        private int medios;

        private Viaje(UUID viajero, String nombreViajero, UUID anfitrion, String nombreAnfitrion,
                      Location origen, BossBar barra, int totalMedios) {
            this.viajero = viajero;
            this.nombreViajero = nombreViajero;
            this.anfitrion = anfitrion;
            this.nombreAnfitrion = nombreAnfitrion;
            this.origen = origen;
            this.barra = barra;
            this.totalMedios = totalMedios;
        }
    }

    private static final class Pelea {
        private final UUID reta;
        private final String nombreReta;
        private final UUID acepta;
        private final String nombreAcepta;
        private final Location origenReta;
        private final Location origenAcepta;
        private boolean activa;
        private int medios;

        private Pelea(UUID reta, String nombreReta, UUID acepta, String nombreAcepta,
                      Location origenReta, Location origenAcepta) {
            this.reta = reta;
            this.nombreReta = nombreReta;
            this.acepta = acepta;
            this.nombreAcepta = nombreAcepta;
            this.origenReta = origenReta;
            this.origenAcepta = origenAcepta;
        }
    }
}
