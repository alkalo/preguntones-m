package com.preguntones.impulso;

import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.events.CompletePredictionEvent;
import ac.grim.grimac.api.events.FlagEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"deprecation", "removal"})
public final class ImpulsoPlugin extends JavaPlugin implements Listener {

    private final ConcurrentHashMap<UUID, Foto> fotos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Tridente.Memoria> memorias = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::retratar, 1L, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void alImpulso(PlayerRiptideEvent event) {
        Tridente.Memoria memoria = memorias.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new Tridente.Memoria());
        synchronized (memoria) {
            memoria.giroHasta = System.currentTimeMillis() + Tridente.VENTANA_MS;
        }
        fotos.put(event.getPlayer().getUniqueId(), Foto.de(event.getPlayer()));
    }

    @EventHandler
    public void alPrediccion(CompletePredictionEvent event) {
        if (dejar(event.getUser(), event.getCheck().getCheckName(), event.getOffset())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alAviso(FlagEvent event) {
        if (dejar(event.getUser(), event.getCheck().getCheckName(), Double.NaN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        fotos.remove(id);
        memorias.remove(id);
    }

    private void retratar() {
        Set<UUID> vistos = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            vistos.add(player.getUniqueId());
            fotos.put(player.getUniqueId(), Foto.de(player));
        }
        fotos.keySet().removeIf(id -> !vistos.contains(id));
    }

    private boolean dejar(GrimUser user, String chequeo, double desfase) {
        if (user == null || chequeo == null) {
            return false;
        }
        Foto foto = fotos.get(user.getUniqueId());
        Tridente.Memoria memoria = memorias.computeIfAbsent(user.getUniqueId(), id -> new Tridente.Memoria());
        synchronized (memoria) {
            boolean puede = foto != null && foto.puede;
            int nivel = foto == null ? 0 : foto.nivel;
            boolean girando = foto != null && foto.girando;
            return Tridente.ignorar(puede, nivel, girando, System.currentTimeMillis(), memoria, chequeo, desfase);
        }
    }

    private static int nivel(ItemStack stack) {
        if (stack == null || stack.getType() != Material.TRIDENT) {
            return 0;
        }
        return stack.getEnchantmentLevel(Enchantment.RIPTIDE);
    }

    private static final class Foto {
        private final int nivel;
        private final boolean puede;
        private final boolean girando;

        private Foto(int nivel, boolean puede, boolean girando) {
            this.nivel = nivel;
            this.puede = puede;
            this.girando = girando;
        }

        private static Foto de(Player player) {
            int nivel = Math.max(
                    ImpulsoPlugin.nivel(player.getInventory().getItemInMainHand()),
                    ImpulsoPlugin.nivel(player.getInventory().getItemInOffHand()));
            return new Foto(nivel, player.isInWaterOrRain(), player.getPose() == Pose.SPIN_ATTACK);
        }
    }
}
