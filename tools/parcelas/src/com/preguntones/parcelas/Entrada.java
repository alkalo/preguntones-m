package com.preguntones.parcelas;

final class Entrada {
    private Entrada() {
    }

    static boolean es(String material) {
        return material != null
            && (material.endsWith("_DOOR") || material.endsWith("_FENCE_GATE") || material.endsWith("_TRAPDOOR"));
    }

    static boolean hierro(String material) {
        return "IRON_DOOR".equals(material) || "IRON_TRAPDOOR".equals(material);
    }

    /** Cofre, barril, horno y el resto de cajas. La puerta no entra. */
    static boolean caja(String material) {
        if (material == null || es(material)) {
            return false;
        }
        return material.contains("CHEST")
            || material.contains("SHULKER")
            || material.contains("BARREL")
            || material.contains("FURNACE")
            || "SMOKER".equals(material)
            || material.contains("HOPPER")
            || "DROPPER".equals(material)
            || "DISPENSER".equals(material)
            || "CRAFTER".equals(material)
            || "BREWING_STAND".equals(material)
            || "LECTERN".equals(material)
            || "DECORATED_POT".equals(material)
            || "CHISELED_BOOKSHELF".equals(material)
            || "JUKEBOX".equals(material)
            || "BEACON".equals(material);
    }
}
