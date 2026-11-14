package com.preguntones.reinicio;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ReinicioPlugin extends JavaPlugin implements Listener {
    private final Random azar = new Random();
    private final AtomicBoolean senalPendiente = new AtomicBoolean();
    private BukkitTask tarea;
    private int quedan;
    private String broma = "";
    private volatile boolean avisado;
    private volatile boolean dejarPasar;
    private boolean reiniciar;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        enganchar();
        Bukkit.getScheduler().runTaskLater(this, this::enganchar, 20L);
        getLogger().info("Antes de parar, título en el centro durante " + Cartel.SEGUNDOS + " s.");
    }

    @Override
    public void onDisable() {
        if (tarea != null) {
            tarea.cancel();
            tarea = null;
        }
        if (!Decidir.alApagar(avisado, getServer().isStopping(), Bukkit.getOnlinePlayers().size())) {
            return;
        }
        getLogger().info("El apagado ya empezó. Aviso antes de echar a nadie.");
        avisarQuieto();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alConsola(ServerCommandEvent event) {
        atacar(event.getSender(), event.getCommand(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alJugador(PlayerCommandPreprocessEvent event) {
        atacar(event.getPlayer(), event.getMessage(), event);
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        if (tarea == null) {
            return;
        }
        Player jugador = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (tarea == null || !jugador.isOnline()) {
                return;
            }
            jugador.showTitle(cartel(3000L));
            decir(jugador);
        });
    }

    private void enganchar() {
        List<String> puestas = Senal.instalar(this::alSenal);
        if (puestas.isEmpty()) {
            getLogger().warning("No pude enganchar la señal de apagado. Avisaré al parar.");
            return;
        }
        getLogger().info("Enganchado el apagado: " + String.join(", ", puestas));
    }

    private void alSenal() {
        if (dejarPasar || avisado) {
            return;
        }
        if (!senalPendiente.compareAndSet(false, true)) {
            return;
        }
        getLogger().info("Señal de apagado. Aviso primero.");
        try {
            Bukkit.getScheduler().runTask(this, () -> {
                if (!avisado && !dejarPasar) {
                    empezar(false);
                }
            });
        } catch (Throwable error) {
            getLogger().warning("No pude dejar el aviso para el siguiente tick.");
        }
    }

    private void atacar(CommandSender sender, String linea, Cancellable event) {
        if (dejarPasar) {
            return;
        }
        Orden.Tipo tipo = Orden.tipo(linea);
        if (tipo == null || !puede(sender, tipo)) {
            return;
        }
        event.setCancelled(true);
        if (tarea != null || avisado) {
            sender.sendMessage("Ya está avisado. Faltan " + Math.max(quedan, 0) + " s.");
            return;
        }
        empezar(tipo == Orden.Tipo.REINICIAR);
    }

    private boolean puede(CommandSender sender, Orden.Tipo tipo) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (sender.isOp()) {
            return true;
        }
        if (tipo == Orden.Tipo.PARAR) {
            return sender.hasPermission("minecraft.command.stop");
        }
        return sender.hasPermission("minecraft.command.restart")
            || sender.hasPermission("bukkit.command.restart");
    }

    private void empezar(boolean quiereReiniciar) {
        if (avisado || dejarPasar) {
            return;
        }
        avisado = true;
        reiniciar = quiereReiniciar;
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().info("Nadie dentro. Paro ya.");
            cerrar();
            return;
        }
        prepararBroma();
        quedan = Cartel.SEGUNDOS;
        pintar(3000L);
        decir(null);
        sonar(true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
        tarea = Bukkit.getScheduler().runTaskTimer(this, this::pulso, 20L, 20L);
    }

    private void pulso() {
        quedan--;
        if (quedan <= 0) {
            if (tarea != null) {
                tarea.cancel();
                tarea = null;
            }
            pintar(5000L);
            cerrar();
            return;
        }
        pintar(3000L);
        if (quedan <= 3) {
            sonar(false);
        }
    }

    private void avisarQuieto() {
        avisado = true;
        prepararBroma();
        for (int segundo = Cartel.SEGUNDOS; segundo >= 1; segundo--) {
            quedan = segundo;
            pintar(2000L);
            if (segundo == Cartel.SEGUNDOS) {
                decir(null);
                sonar(true);
            } else if (segundo <= 3) {
                sonar(false);
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interrumpido) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        quedan = 0;
        pintar(5000L);
    }

    private void prepararBroma() {
        broma = Bromas.elegir(azar, getConfig().getString("ultima", ""));
        getConfig().set("ultima", broma);
        saveConfig();
    }

    private void pintar(long estancia) {
        Title titulo = cartel(estancia);
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            if (!jugador.isOnline()) {
                continue;
            }
            try {
                jugador.showTitle(titulo);
            } catch (Throwable ignored) {
                // Si uno falla, el resto sigue viendo el aviso.
            }
        }
    }

    private Title cartel(long estancia) {
        return Title.title(
            plano(Cartel.titulo(quedan), NamedTextColor.GOLD),
            plano(broma, NamedTextColor.WHITE),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(estancia), Duration.ZERO)
        );
    }

    private void decir(Player solo) {
        Component mensaje = plano("Perdón. ", NamedTextColor.GOLD)
            .append(plano(Cartel.frase(quedan) + " ", NamedTextColor.WHITE))
            .append(plano(broma, NamedTextColor.YELLOW));
        if (solo != null) {
            solo.sendMessage(mensaje);
            return;
        }
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.sendMessage(mensaje);
        }
        getLogger().info(Cartel.frase(quedan) + " " + broma);
    }

    private void sonar(boolean inicio) {
        Sound sonido = inicio ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_NOTE_BLOCK_PLING;
        float tono = inicio ? 1.0f : quedan == 3 ? 1.0f : quedan == 2 ? 1.3f : 1.6f;
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.playSound(jugador, sonido, SoundCategory.MASTER, 0.45f, tono);
        }
    }

    private void cerrar() {
        dejarPasar = true;
        if (reiniciar && Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart")) {
            return;
        }
        Bukkit.shutdown();
    }

    private static Component plano(String texto, NamedTextColor color) {
        return Component.text(texto, color).decoration(TextDecoration.ITALIC, false);
    }
}
