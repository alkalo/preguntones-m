package com.preguntones.mundos;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;

/** Estar dentro de la estructura. El borde de un chunk suelto no cuenta. */
final class Sitio {

    private Sitio() {
    }

    static boolean dentro(Location sitio, Structure tipo) {
        World mundo = sitio.getWorld();
        if (mundo == null || tipo == null) {
            return false;
        }
        int cx = sitio.getBlockX() >> 4;
        int cz = sitio.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!mundo.isChunkLoaded(cx + dx, cz + dz)) {
                    continue;
                }
                Chunk chunk = mundo.getChunkAt(cx + dx, cz + dz);
                for (GeneratedStructure generada : chunk.getStructures(tipo)) {
                    if (generada.getBoundingBox().contains(sitio.getX(), sitio.getY(), sitio.getZ())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean salaAntigua(Location sitio) {
        if (sitio.getY() > 0 || !dentro(sitio, Structure.ANCIENT_CITY)) {
            return false;
        }
        World mundo = sitio.getWorld();
        int x0 = sitio.getBlockX();
        int y0 = sitio.getBlockY();
        int z0 = sitio.getBlockZ();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    int y = y0 + dy;
                    if (y < mundo.getMinHeight() || y >= mundo.getMaxHeight()) {
                        continue;
                    }
                    if (mundo.getBlockAt(x0 + dx, y, z0 + dz).getType() == Material.REINFORCED_DEEPSLATE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
