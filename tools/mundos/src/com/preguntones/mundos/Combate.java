package com.preguntones.mundos;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

/** La pelea del primer dragón del ciclo, y las marcas del bastión y la ciudad. */
final class Combate implements Listener {

    private final MundosPlugin plugin;

    Combate(MundosPlugin plugin) {
        this.plugin = plugin;
    }

    void pulso() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            World mundo = jugador.getWorld();
            if (mundo == null) {
                continue;
            }
            if ("survival".equals(mundo.getName())
                    && plugin.faltaCiudad(jugador)
                    && Sitio.salaAntigua(jugador.getLocation())) {
                plugin.marcarCiudad(jugador);
            }
            if (plugin.peleaEn(mundo) && jugador.isGliding()) {
                jugador.setGliding(false);
            }
        }
        World fin = Bukkit.getWorld("fin");
        if (fin == null || !plugin.peleaEn(fin)) {
            return;
        }
        for (EnderDragon dragon : fin.getEntitiesByClass(EnderDragon.class)) {
            asegurarVida(dragon);
        }
    }

    @EventHandler
    public void alMorirBruto(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof PiglinBrute bruto)) {
            return;
        }
        if (bruto.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        Player asesino = bruto.getKiller();
        if (asesino == null || !Sitio.dentro(asesino.getLocation(), Structure.BASTION_REMNANT)) {
            return;
        }
        plugin.marcarBastion(asesino);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alDano(EntityDamageEvent event) {
        EnderDragon dragon = dragonDe(event.getEntity());
        if (dragon == null || !plugin.peleaEn(dragon.getWorld())) {
            return;
        }
        asegurarVida(dragon);
        Player golpeador = golpeador(event);
        if (golpeador != null) {
            plugin.recordarGolpe(golpeador);
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            if (!hayCredito(golpeador)) {
                event.setCancelled(true);
                llenar(dragon);
                dragon.teleport(new Location(dragon.getWorld(), 0.5, 80, 0.5));
            }
            return;
        }
        boolean cristales = quedanCristales(dragon.getWorld());
        boolean posado = Pelea.posado(dragon.getPhase().name());
        boolean explosion = event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        double nuevo = Pelea.dano(explosion, cristales, posado, event.getDamage());
        if (nuevo <= 0) {
            event.setCancelled(true);
            return;
        }
        event.setDamage(nuevo);
        if (golpeador != null && cristales) {
            plugin.avisarPelea(golpeador, "§ePrimero los cristales de las torres.");
        } else if (golpeador != null && !posado) {
            plugin.avisarPelea(golpeador, "§eEn el aire casi no entra. Espera a que se pose.");
        }
        if (dragon.getHealth() > nuevo || hayCredito(golpeador)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean hayCredito(Player golpeador) {
        if (golpeador != null && plugin.puertaLista(golpeador)) {
            return true;
        }
        return plugin.acreditable() != null;
    }

    @EventHandler(ignoreCancelled = true)
    public void alPlanear(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player jugador)) {
            return;
        }
        if (!plugin.peleaEn(jugador.getWorld())) {
            return;
        }
        event.setCancelled(true);
        plugin.avisarPelea(jugador, "§eEn esta pelea los élitros no vuelan.");
    }

    @EventHandler(ignoreCancelled = true)
    public void alUsar(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!plugin.peleaEn(event.getPlayer().getWorld())) {
            return;
        }
        Material tipo = event.getClickedBlock().getType();
        if (!Tag.BEDS.isTagged(tipo) && tipo != Material.RESPAWN_ANCHOR) {
            return;
        }
        event.setCancelled(true);
        plugin.avisarPelea(event.getPlayer(), "§eEso no explota contra el dragón.");
    }

    @EventHandler(ignoreCancelled = true)
    public void alPoner(BlockPlaceEvent event) {
        if (!plugin.peleaEn(event.getPlayer().getWorld())) {
            return;
        }
        Material tipo = event.getBlockPlaced().getType();
        if (tipo != Material.END_CRYSTAL && tipo != Material.TNT && tipo != Material.TNT_MINECART) {
            return;
        }
        event.setCancelled(true);
        plugin.avisarPelea(event.getPlayer(), "§eEso no se usa en esta pelea.");
    }

    static void llenar(EnderDragon dragon) {
        asegurarVida(dragon);
        AttributeInstance vida = dragon.getAttribute(Attribute.MAX_HEALTH);
        dragon.setHealth(vida == null ? Pelea.VIDA : Math.min(Pelea.VIDA, vida.getBaseValue()));
    }

    private static void asegurarVida(EnderDragon dragon) {
        AttributeInstance vida = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (vida == null || vida.getBaseValue() >= Pelea.VIDA - 1) {
            return;
        }
        double vieja = vida.getBaseValue();
        double actual = dragon.getHealth();
        vida.setBaseValue(Pelea.VIDA);
        dragon.setHealth(Math.min(Pelea.VIDA, vieja <= 0 ? Pelea.VIDA : actual * (Pelea.VIDA / vieja)));
    }

    private static boolean quedanCristales(World mundo) {
        for (EnderCrystal cristal : mundo.getEntitiesByClass(EnderCrystal.class)) {
            if (!cristal.isDead()) {
                return true;
            }
        }
        return false;
    }

    private static EnderDragon dragonDe(Entity entidad) {
        if (entidad instanceof EnderDragon dragon) {
            return dragon;
        }
        if (entidad instanceof EnderDragonPart parte && parte.getParent() instanceof EnderDragon dragon) {
            return dragon;
        }
        return null;
    }

    private static Player golpeador(EntityDamageEvent event) {
        if (!(event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent golpe)) {
            return null;
        }
        Entity origen = golpe.getDamager();
        if (origen instanceof Player jugador) {
            return jugador;
        }
        if (origen instanceof Projectile proyectil) {
            ProjectileSource fuente = proyectil.getShooter();
            if (fuente instanceof Player jugador) {
                return jugador;
            }
        }
        return null;
    }
}
