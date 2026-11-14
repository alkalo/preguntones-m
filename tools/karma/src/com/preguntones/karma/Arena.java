package com.preguntones.karma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/** Coliseo del mundo duelo. No toca el barrio. */
final class Arena implements Listener {
    static final String MUNDO = "duelo";
    private static final int VERSION = 1;
    private static final String[] NOMBRES = {"Público", "Grada", "Palco"};

    private final KarmaPlugin plugin;
    private boolean lista;

    Arena(KarmaPlugin plugin) {
        this.plugin = plugin;
    }

    boolean lista() {
        return lista && Bukkit.getWorld(MUNDO) != null;
    }

    Location oeste() {
        World mundo = Bukkit.getWorld(MUNDO);
        return mundo == null ? null : new Location(mundo, -4.5, 65, 0.5, -90f, 0f);
    }

    Location este() {
        World mundo = Bukkit.getWorld(MUNDO);
        return mundo == null ? null : new Location(mundo, 5.5, 65, 0.5, 90f, 0f);
    }

    void preparar() {
        World mundo = crear();
        if (mundo == null) {
            plugin.getLogger().warning("No he podido crear el mundo del duelo.");
            return;
        }
        reglas(mundo);
        File archivo = new File(plugin.getDataFolder(), "arena.yml");
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(archivo);
        boolean hecha = yaml.getInt("version") == VERSION
            && mundo.getBlockAt(0, 64, 0).getType() == Material.GOLD_BLOCK;
        if (!hecha) {
            construir(mundo);
            limpiarPublico(mundo);
            publico(mundo);
            yaml.set("version", VERSION);
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                yaml.save(archivo);
            } catch (IOException e) {
                plugin.getLogger().warning("No pude guardar la marca de la arena: " + e.getMessage());
            }
        } else if (mundo.getEntitiesByClass(Mannequin.class).size() < 20) {
            limpiarPublico(mundo);
            publico(mundo);
        }
        lista = mundo.getBlockAt(0, 64, 0).getType() == Material.GOLD_BLOCK;
        if (lista) {
            plugin.getLogger().info("Arena de duelo lista.");
        }
    }

    private World crear() {
        if (Bukkit.getWorld(MUNDO) == null) {
            WorldCreator creador = new WorldCreator(MUNDO);
            creador.type(WorldType.FLAT);
            creador.generateStructures(false);
            creador.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}");
            Bukkit.createWorld(creador);
        }
        return Bukkit.getWorld(MUNDO);
    }

    private void reglas(World mundo) {
        mundo.setPVP(true);
        mundo.setDifficulty(Difficulty.NORMAL);
        mundo.setSpawnFlags(false, false);
        mundo.setGameRule(GameRules.SPAWN_MOBS, false);
        mundo.setGameRule(GameRules.SPAWN_MONSTERS, false);
        mundo.setGameRule(GameRules.SPAWN_PATROLS, false);
        mundo.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        mundo.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        mundo.setGameRule(GameRules.ADVANCE_TIME, false);
        mundo.setGameRule(GameRules.ADVANCE_WEATHER, false);
        mundo.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        mundo.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        mundo.setGameRule(GameRules.MOB_GRIEFING, false);
        mundo.setTime(18000);
        mundo.setStorm(false);
        mundo.setSpawnLocation(0, 65, 0);
    }

    private void construir(World mundo) {
        rellenar(mundo, -16, 63, -14, 16, 63, 14, Material.BLACKSTONE);
        for (int x = -12; x <= 12; x += 4) {
            for (int z = -8; z <= 8; z += 4) {
                mundo.getBlockAt(x, 63, z).setType(Material.SEA_LANTERN, false);
            }
        }
        rellenar(mundo, -10, 64, -7, 10, 64, 7, Material.POLISHED_DEEPSLATE);
        rellenar(mundo, -10, 64, 0, 10, 64, 0, Material.DEEPSLATE_TILES);
        rellenar(mundo, 0, 64, -7, 0, 64, 7, Material.DEEPSLATE_TILES);
        mundo.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK, false);
        mundo.getBlockAt(-5, 64, 0).setType(Material.RED_CONCRETE, false);
        mundo.getBlockAt(5, 64, 0).setType(Material.BLUE_CONCRETE, false);
        rellenar(mundo, -11, 64, -8, 11, 64, -8, Material.GOLD_BLOCK);
        rellenar(mundo, -11, 64, 8, 11, 64, 8, Material.GOLD_BLOCK);
        rellenar(mundo, -11, 64, -7, -11, 64, 7, Material.GOLD_BLOCK);
        rellenar(mundo, 11, 64, -7, 11, 64, 7, Material.GOLD_BLOCK);

        rellenar(mundo, -12, 65, -9, 12, 67, -9, Material.GLASS);
        rellenar(mundo, -12, 65, 9, 12, 67, 9, Material.GLASS);
        rellenar(mundo, -12, 65, -8, -12, 67, 8, Material.GLASS);
        rellenar(mundo, 12, 65, -8, 12, 67, 8, Material.GLASS);
        rellenar(mundo, -11, 69, -8, 11, 69, 8, Material.BARRIER);

        rellenar(mundo, -10, 65, 10, 10, 65, 10, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -10, 66, 11, 10, 66, 11, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -10, 67, 12, 10, 67, 12, Material.QUARTZ_BLOCK);
        rellenar(mundo, -10, 65, -10, 10, 65, -10, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -10, 66, -11, 10, 66, -11, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -10, 67, -12, 10, 67, -12, Material.QUARTZ_BLOCK);
        rellenar(mundo, 13, 65, -6, 13, 65, 6, Material.SMOOTH_QUARTZ);
        rellenar(mundo, 14, 66, -6, 14, 66, 6, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -13, 65, -6, -13, 65, 6, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -14, 66, -6, -14, 66, 6, Material.SMOOTH_QUARTZ);

        rellenar(mundo, -12, 65, 13, 12, 70, 13, Material.POLISHED_BLACKSTONE_BRICKS);
        rellenar(mundo, -12, 65, -13, 12, 70, -13, Material.POLISHED_BLACKSTONE_BRICKS);
        rellenar(mundo, 15, 65, -10, 15, 70, 10, Material.POLISHED_BLACKSTONE_BRICKS);
        rellenar(mundo, -15, 65, -10, -15, 70, 10, Material.POLISHED_BLACKSTONE_BRICKS);
        pilar(mundo, -15, -13);
        pilar(mundo, 15, -13);
        pilar(mundo, -15, 13);
        pilar(mundo, 15, 13);
        bandera(mundo, -14, 69, 0, Material.RED_WALL_BANNER, BlockFace.EAST);
        bandera(mundo, 14, 69, 0, Material.BLUE_WALL_BANNER, BlockFace.WEST);
        bandera(mundo, 0, 68, -12, Material.RED_WALL_BANNER, BlockFace.SOUTH);
        bandera(mundo, 0, 68, 12, Material.BLUE_WALL_BANNER, BlockFace.NORTH);

        rellenar(mundo, -16, 64, -14, -16, 74, 14, Material.BARRIER);
        rellenar(mundo, 16, 64, -14, 16, 74, 14, Material.BARRIER);
        rellenar(mundo, -15, 64, -14, 15, 74, -14, Material.BARRIER);
        rellenar(mundo, -15, 64, 14, 15, 74, 14, Material.BARRIER);
        rellenar(mundo, -16, 74, -14, 16, 74, 14, Material.BARRIER);
    }

    private void pilar(World mundo, int x, int z) {
        rellenar(mundo, x, 64, z, x, 70, z, Material.CHISELED_POLISHED_BLACKSTONE);
        mundo.getBlockAt(x, 71, z).setType(Material.SEA_LANTERN, false);
    }

    private void bandera(World mundo, int x, int y, int z, Material tipo, BlockFace cara) {
        Block bloque = mundo.getBlockAt(x, y, z);
        bloque.setType(tipo, false);
        if (bloque.getBlockData() instanceof Directional dir) {
            dir.setFacing(cara);
            bloque.setBlockData(dir, false);
        }
    }

    private void publico(World mundo) {
        int i = 0;
        int[] xs = {-9, -6, -3, 0, 3, 6, 9};
        for (int x : xs) {
            sentar(mundo, x, 67, 11, i++);
            sentar(mundo, x, 67, -11, i++);
        }
        int[] zs = {-4, -2, 0, 2, 4};
        for (int z : zs) {
            sentar(mundo, 14, 67, z, i++);
            sentar(mundo, -14, 67, z, i++);
        }
    }

    private void sentar(World mundo, int x, int y, int z, int orden) {
        Location sitio = new Location(mundo, x + 0.5, y, z + 0.5);
        sitio.setDirection(new Location(mundo, 0.5, y, 0.5).toVector().subtract(sitio.toVector()));
        float giro = sitio.getYaw();
        boolean rojo = orden % 2 == 0;
        String nombre = NOMBRES[orden % NOMBRES.length];
        Consumer<Mannequin> vestir = npc -> {
            npc.setImmovable(true);
            npc.setInvulnerable(true);
            npc.setGravity(false);
            npc.setCollidable(false);
            npc.setPersistent(true);
            npc.setRemoveWhenFarAway(false);
            npc.setAI(false);
            npc.setSilent(true);
            npc.setBodyYaw(giro);
            npc.customName(Component.text(nombre, NamedTextColor.GOLD));
            npc.setCustomNameVisible(true);
            npc.setDescription(Component.text("Mira el duelo"));
            EntityEquipment equipo = npc.getEquipment();
            if (equipo != null) {
                equipo.setChestplate(cuero(Material.LEATHER_CHESTPLATE, rojo));
                equipo.setHelmet(cuero(Material.LEATHER_HELMET, rojo));
            }
        };
        mundo.spawn(sitio, Mannequin.class, vestir);
    }

    private ItemStack cuero(Material tipo, boolean rojo) {
        ItemStack pieza = new ItemStack(tipo);
        if (pieza.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(rojo ? Color.fromRGB(0xB33A3A) : Color.fromRGB(0x3A5FB3));
            pieza.setItemMeta(meta);
        }
        return pieza;
    }

    private void limpiarPublico(World mundo) {
        for (Mannequin npc : new ArrayList<>(mundo.getEntitiesByClass(Mannequin.class))) {
            npc.remove();
        }
    }

    private void rellenar(World mundo, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int xa = Math.min(x1, x2);
        int xb = Math.max(x1, x2);
        int ya = Math.min(y1, y2);
        int yb = Math.max(y1, y2);
        int za = Math.min(z1, z2);
        int zb = Math.max(z1, z2);
        for (int x = xa; x <= xb; x++) {
            for (int y = ya; y <= yb; y++) {
                for (int z = za; z <= zb; z++) {
                    mundo.getBlockAt(x, y, z).setType(material, false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alRomper(BlockBreakEvent event) {
        if (MUNDO.equals(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alPoner(BlockPlaceEvent event) {
        if (MUNDO.equals(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alExplotar(EntityExplodeEvent event) {
        if (event.getEntity() != null && MUNDO.equals(event.getEntity().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alExplotarBloque(BlockExplodeEvent event) {
        if (MUNDO.equals(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alDano(EntityDamageEvent event) {
        if (event.getEntity() instanceof Mannequin npc && MUNDO.equals(npc.getWorld().getName())) {
            event.setCancelled(true);
        }
    }
}
