package com.preguntones.parcelas;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/**
 * El precio sigue la receta y es el atajo caro: talar sigue saliendo mejor.
 * 32 troncos de roble son 48 duros. Sus tablones salen a 12, porque un tronco da cuatro.
 * La madera de corteza sale a 64, porque cuatro troncos dan tres.
 * Escaleras, losas y muros cuestan los bloques que gastan.
 * La bandera no está aquí: sigue a 40, es un punto de viaje.
 */
final class Catalogo {
    private static final int ESCALA = 2;
    private static final List<String> SALTADOS = new ArrayList<>();

    private Catalogo() {
    }

    static List<Seccion> secciones() {
        return List.of(
                new Seccion("Luces", Material.LANTERN),
                new Seccion("Madera", Material.OAK_LOG),
                new Seccion("Piedra", Material.STONE_BRICKS),
                new Seccion("Color", Material.CYAN_WOOL),
                new Seccion("Cristal", Material.GLASS),
                new Seccion("Plantas", Material.POPPY),
                new Seccion("Tierra", Material.MOSS_BLOCK),
                new Seccion("Nether", Material.NETHER_BRICKS),
                new Seccion("End", Material.PURPUR_BLOCK),
                new Seccion("Cobre", Material.COPPER_BLOCK),
                new Seccion("Mar", Material.PRISMARINE),
                new Seccion("Muebles", Material.BOOKSHELF),
                new Seccion("Mecanismos", Material.REPEATER)
        );
    }

    static boolean conocida(String nombre) {
        for (Seccion seccion : secciones()) {
            if (seccion.nombre.equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    static List<String> saltados() {
        return SALTADOS;
    }

    static List<Fila> filas() {
        SALTADOS.clear();
        List<Fila> lista = new ArrayList<>();
        luces(lista);
        maderas(lista);
        piedra(lista);
        color(lista);
        cristal(lista);
        plantas(lista);
        tierra(lista);
        nether(lista);
        end(lista);
        cobre(lista);
        mar(lista);
        muebles(lista);
        mecanismos(lista);
        return lista;
    }

    private static void luces(List<Fila> lista) {
        add(lista, "Luces", Material.TORCH, 32, 6, "Antorchas");
        add(lista, "Luces", Material.SOUL_TORCH, 16, 12, "Antorchas de alma");
        add(lista, "Luces", Material.COPPER_TORCH, 16, 12, "Antorchas de cobre");
        add(lista, "Luces", Material.LANTERN, 4, 16, "Faroles");
        add(lista, "Luces", Material.SOUL_LANTERN, 4, 20, "Faroles de alma");
        add(lista, "Luces", Material.CANDLE, 8, 8, "Velas");
        colores(lista, "Luces", "_CANDLE", "Vela ", 8, 12, true);
        add(lista, "Luces", Material.JACK_O_LANTERN, 4, 12, "Calabazas");
        add(lista, "Luces", Material.REDSTONE_LAMP, 4, 24, "Lámparas");
        add(lista, "Luces", Material.GLOWSTONE, 16, 16, "Piedra luminosa");
        add(lista, "Luces", Material.SEA_PICKLE, 8, 6, "Pepinos de mar");
        add(lista, "Luces", Material.GLOW_LICHEN, 16, 8, "Liquen luminoso");
        add(lista, "Luces", Material.END_ROD, 4, 24, "Varas del End");
        add(lista, "Luces", Material.SEA_LANTERN, 4, 24, "Lámparas de mar");
        add(lista, "Luces", Material.SHROOMLIGHT, 4, 20, "Luz de hongo");
        add(lista, "Luces", Material.OCHRE_FROGLIGHT, 2, 24, "Luz de rana");
        add(lista, "Luces", Material.PEARLESCENT_FROGLIGHT, 2, 24, "Luz perlada");
        add(lista, "Luces", Material.VERDANT_FROGLIGHT, 2, 24, "Luz verde");
        String[] oxido = {"", "EXPOSED_", "WEATHERED_", "OXIDIZED_"};
        String[] femenino = {"", " expuesta", " erosionada", " oxidada"};
        String[] masculino = {"", " expuesto", " erosionado", " oxidado"};
        for (int i = 0; i < oxido.length; i++) {
            poner(lista, "Luces", "WAXED_" + oxido[i] + "COPPER_BULB", 4, 16, "Bombilla de cobre" + femenino[i] + " encerada");
            poner(lista, "Luces", "WAXED_" + oxido[i] + "COPPER_LANTERN", 4, 16, "Farol de cobre" + masculino[i] + " encerado");
        }
    }

    private static void maderas(List<Fila> lista) {
        madera(lista, "roble", "OAK", false, 24);
        madera(lista, "abeto", "SPRUCE", false, 24);
        madera(lista, "abedul", "BIRCH", false, 24);
        madera(lista, "selva", "JUNGLE", false, 24);
        madera(lista, "acacia", "ACACIA", false, 24);
        madera(lista, "roble oscuro", "DARK_OAK", false, 24);
        madera(lista, "manglar", "MANGROVE", false, 28);
        poner(lista, "Madera", "MANGROVE_ROOTS", 32, 16, "Raíces de manglar");
        poner(lista, "Madera", "MUDDY_MANGROVE_ROOTS", 32, 20, "Raíces embarradas");
        madera(lista, "cerezo", "CHERRY", false, 28);
        madera(lista, "roble pálido", "PALE_OAK", false, 32);
        madera(lista, "bambú", "BAMBOO", false, 80);
        poner(lista, "Madera", "BAMBOO_BLOCK", 16, 20, "Bloque de bambú");
        poner(lista, "Madera", "STRIPPED_BAMBOO_BLOCK", 16, 20, "Bambú pelado");
        poner(lista, "Madera", "BAMBOO_MOSAIC", 16, 20, "Mosaico de bambú");
        poner(lista, "Madera", "BAMBOO_MOSAIC_STAIRS", 16, 30, "Escalera de mosaico");
        poner(lista, "Madera", "BAMBOO_MOSAIC_SLAB", 32, 20, "Losa de mosaico");
        madera(lista, "carmesí", "CRIMSON", true, 36);
        madera(lista, "distorsionado", "WARPED", true, 36);
    }

    private static void piedra(List<Fila> lista) {
        obra(lista, "Piedra", "STONE", "STONE_STAIRS", "STONE_SLAB", null, "piedra", 32, 16);
        obra(lista, "Piedra", "COBBLESTONE", "COBBLESTONE_STAIRS", "COBBLESTONE_SLAB", "COBBLESTONE_WALL", "roca", 32, 16);
        obra(lista, "Piedra", "MOSSY_COBBLESTONE", "MOSSY_COBBLESTONE_STAIRS", "MOSSY_COBBLESTONE_SLAB", "MOSSY_COBBLESTONE_WALL", "roca musgosa", 32, 48);
        obra(lista, "Piedra", "STONE_BRICKS", "STONE_BRICK_STAIRS", "STONE_BRICK_SLAB", "STONE_BRICK_WALL", "ladrillos de piedra", 32, 20);
        obra(lista, "Piedra", "MOSSY_STONE_BRICKS", "MOSSY_STONE_BRICK_STAIRS", "MOSSY_STONE_BRICK_SLAB", "MOSSY_STONE_BRICK_WALL", "ladrillos musgosos", 32, 52);
        poner(lista, "Piedra", "CRACKED_STONE_BRICKS", 32, 20, "Ladrillos agrietados");
        poner(lista, "Piedra", "CHISELED_STONE_BRICKS", 16, 12, "Ladrillos cincelados");
        poner(lista, "Piedra", "SMOOTH_STONE", 32, 20, "Piedra lisa");
        poner(lista, "Piedra", "SMOOTH_STONE_SLAB", 32, 10, "Losa de piedra lisa");
        obra(lista, "Piedra", "GRANITE", "GRANITE_STAIRS", "GRANITE_SLAB", "GRANITE_WALL", "granito", 32, 16);
        obra(lista, "Piedra", "POLISHED_GRANITE", "POLISHED_GRANITE_STAIRS", "POLISHED_GRANITE_SLAB", null, "granito pulido", 32, 20);
        obra(lista, "Piedra", "DIORITE", "DIORITE_STAIRS", "DIORITE_SLAB", "DIORITE_WALL", "diorita", 32, 16);
        obra(lista, "Piedra", "POLISHED_DIORITE", "POLISHED_DIORITE_STAIRS", "POLISHED_DIORITE_SLAB", null, "diorita pulida", 32, 20);
        obra(lista, "Piedra", "ANDESITE", "ANDESITE_STAIRS", "ANDESITE_SLAB", "ANDESITE_WALL", "andesita", 32, 16);
        obra(lista, "Piedra", "POLISHED_ANDESITE", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_SLAB", null, "andesita pulida", 32, 20);
        poner(lista, "Piedra", "DEEPSLATE", 32, 16, "Pizarra");
        obra(lista, "Piedra", "COBBLED_DEEPSLATE", "COBBLED_DEEPSLATE_STAIRS", "COBBLED_DEEPSLATE_SLAB", "COBBLED_DEEPSLATE_WALL", "pizarra rocosa", 32, 16);
        obra(lista, "Piedra", "POLISHED_DEEPSLATE", "POLISHED_DEEPSLATE_STAIRS", "POLISHED_DEEPSLATE_SLAB", "POLISHED_DEEPSLATE_WALL", "pizarra pulida", 32, 20);
        obra(lista, "Piedra", "DEEPSLATE_BRICKS", "DEEPSLATE_BRICK_STAIRS", "DEEPSLATE_BRICK_SLAB", "DEEPSLATE_BRICK_WALL", "ladrillos de pizarra", 32, 20);
        obra(lista, "Piedra", "DEEPSLATE_TILES", "DEEPSLATE_TILE_STAIRS", "DEEPSLATE_TILE_SLAB", "DEEPSLATE_TILE_WALL", "baldosas de pizarra", 32, 20);
        poner(lista, "Piedra", "CRACKED_DEEPSLATE_BRICKS", 32, 20, "Ladrillos de pizarra agrietados");
        poner(lista, "Piedra", "CRACKED_DEEPSLATE_TILES", 32, 20, "Baldosas agrietadas");
        poner(lista, "Piedra", "CHISELED_DEEPSLATE", 16, 12, "Pizarra cincelada");
        obra(lista, "Piedra", "TUFF", "TUFF_STAIRS", "TUFF_SLAB", "TUFF_WALL", "toba", 32, 16);
        obra(lista, "Piedra", "POLISHED_TUFF", "POLISHED_TUFF_STAIRS", "POLISHED_TUFF_SLAB", "POLISHED_TUFF_WALL", "toba pulida", 32, 20);
        obra(lista, "Piedra", "TUFF_BRICKS", "TUFF_BRICK_STAIRS", "TUFF_BRICK_SLAB", "TUFF_BRICK_WALL", "ladrillos de toba", 32, 20);
        poner(lista, "Piedra", "CHISELED_TUFF", 16, 12, "Toba cincelada");
        poner(lista, "Piedra", "CHISELED_TUFF_BRICKS", 16, 12, "Ladrillos de toba cincelados");
        poner(lista, "Piedra", "CALCITE", 32, 16, "Calcita");
        poner(lista, "Piedra", "DRIPSTONE_BLOCK", 32, 16, "Bloque de caliza");
        poner(lista, "Piedra", "POINTED_DRIPSTONE", 16, 12, "Estalactita");
        obra(lista, "Piedra", "SANDSTONE", "SANDSTONE_STAIRS", "SANDSTONE_SLAB", "SANDSTONE_WALL", "arenisca", 16, 24);
        poner(lista, "Piedra", "CUT_SANDSTONE", 16, 24, "Arenisca cortada");
        poner(lista, "Piedra", "CUT_SANDSTONE_SLAB", 32, 24, "Losa de arenisca cortada");
        poner(lista, "Piedra", "CHISELED_SANDSTONE", 16, 28, "Arenisca cincelada");
        obra(lista, "Piedra", "SMOOTH_SANDSTONE", "SMOOTH_SANDSTONE_STAIRS", "SMOOTH_SANDSTONE_SLAB", null, "arenisca lisa", 16, 28);
        obra(lista, "Piedra", "RED_SANDSTONE", "RED_SANDSTONE_STAIRS", "RED_SANDSTONE_SLAB", "RED_SANDSTONE_WALL", "arenisca roja", 16, 24);
        poner(lista, "Piedra", "CUT_RED_SANDSTONE", 16, 24, "Arenisca roja cortada");
        poner(lista, "Piedra", "CUT_RED_SANDSTONE_SLAB", 32, 24, "Losa de arenisca roja cortada");
        poner(lista, "Piedra", "CHISELED_RED_SANDSTONE", 16, 28, "Arenisca roja cincelada");
        obra(lista, "Piedra", "SMOOTH_RED_SANDSTONE", "SMOOTH_RED_SANDSTONE_STAIRS", "SMOOTH_RED_SANDSTONE_SLAB", null, "arenisca roja lisa", 16, 28);
        obra(lista, "Piedra", "BRICKS", "BRICK_STAIRS", "BRICK_SLAB", "BRICK_WALL", "ladrillos", 16, 20);
        obra(lista, "Piedra", "MUD_BRICKS", "MUD_BRICK_STAIRS", "MUD_BRICK_SLAB", "MUD_BRICK_WALL", "ladrillos de barro", 16, 16);
        poner(lista, "Piedra", "RESIN_BLOCK", 16, 24, "Resina");
        obra(lista, "Piedra", "RESIN_BRICKS", "RESIN_BRICK_STAIRS", "RESIN_BRICK_SLAB", "RESIN_BRICK_WALL", "ladrillos de resina", 16, 28);
        poner(lista, "Piedra", "CHISELED_RESIN_BRICKS", 16, 32, "Resina cincelada");
        poner(lista, "Piedra", "RESIN_CLUMP", 16, 12, "Grumo de resina");
        add(lista, "Piedra", Material.AMETHYST_BLOCK, 16, 24, "Amatista");
        add(lista, "Piedra", Material.AMETHYST_CLUSTER, 4, 20, "Racimo de amatista");
        add(lista, "Piedra", Material.REINFORCED_DEEPSLATE, 8, 36, "Pizarra reforzada");
        poner(lista, "Piedra", "STONE_BUTTON", 16, 10, "Botón de piedra");
    }

    private static void color(List<Fila> lista) {
        add(lista, "Color", Material.TERRACOTTA, 32, 16, "Terracota");
        colores(lista, "Color", "_WOOL", "Lana ", 16, 16, true);
        colores(lista, "Color", "_CARPET", "Alfombra ", 32, 24, true);
        colores(lista, "Color", "_TERRACOTTA", "Terracota ", 32, 20, true);
        colores(lista, "Color", "_GLAZED_TERRACOTTA", "Azulejo ", 16, 24, false);
        colores(lista, "Color", "_CONCRETE_POWDER", "Polvo ", 32, 24, false);
        colores(lista, "Color", "_CONCRETE", "Hormigón ", 32, 24, false);
        colores(lista, "Color", "_BED", "Cama ", 1, 8, true);
        colores(lista, "Color", "_BANNER", "Estandarte ", 1, 12, false);
        colores(lista, "Color", "_DYE", "Tinte ", 16, 8, false);
    }

    private static void cristal(List<Fila> lista) {
        add(lista, "Cristal", Material.GLASS, 32, 16, "Cristal");
        add(lista, "Cristal", Material.GLASS_PANE, 32, 8, "Panel de cristal");
        add(lista, "Cristal", Material.TINTED_GLASS, 16, 28, "Cristal opaco");
        colores(lista, "Cristal", "_STAINED_GLASS", "Cristal ", 16, 16, false);
        colores(lista, "Cristal", "_STAINED_GLASS_PANE", "Panel ", 32, 12, false);
    }

    private static void plantas(List<Fila> lista) {
        varias(lista, "Plantas", 16, 6,
                Material.DANDELION, "Diente de león",
                Material.POPPY, "Amapola",
                Material.BLUE_ORCHID, "Orquídea",
                Material.ALLIUM, "Allium",
                Material.AZURE_BLUET, "Houstonia",
                Material.RED_TULIP, "Tulipán rojo",
                Material.ORANGE_TULIP, "Tulipán naranja",
                Material.WHITE_TULIP, "Tulipán blanco",
                Material.PINK_TULIP, "Tulipán rosa",
                Material.OXEYE_DAISY, "Margarita",
                Material.CORNFLOWER, "Aciano",
                Material.LILY_OF_THE_VALLEY, "Lirio",
                Material.PINK_PETALS, "Pétalos",
                Material.LEAF_LITTER, "Hojarasca",
                Material.SHORT_GRASS, "Hierba",
                Material.FERN, "Helecho",
                Material.DEAD_BUSH, "Arbusto seco");
        varias(lista, "Plantas", 8, 6,
                Material.TALL_GRASS, "Hierba alta",
                Material.LARGE_FERN, "Helecho grande",
                Material.BUSH, "Arbusto",
                Material.VINE, "Enredadera",
                Material.TWISTING_VINES, "Enredadera distorsionada",
                Material.WEEPING_VINES, "Enredadera llorona");
        varias(lista, "Plantas", 4, 10,
                Material.SUNFLOWER, "Girasol",
                Material.LILAC, "Lila",
                Material.ROSE_BUSH, "Rosal",
                Material.PEONY, "Peonía",
                Material.AZALEA, "Azalea",
                Material.FLOWERING_AZALEA, "Azalea en flor",
                Material.TORCHFLOWER, "Antorcha floral",
                Material.CACTUS_FLOWER, "Flor de cactus",
                Material.OPEN_EYEBLOSSOM, "Flor de ojo abierta",
                Material.CLOSED_EYEBLOSSOM, "Flor de ojo cerrada",
                Material.BIG_DRIPLEAF, "Hoja grande",
                Material.SMALL_DRIPLEAF, "Hoja pequeña",
                Material.CRIMSON_FUNGUS, "Hongo carmesí",
                Material.WARPED_FUNGUS, "Hongo distorsionado",
                Material.CRIMSON_ROOTS, "Raíces carmesí",
                Material.WARPED_ROOTS, "Raíces distorsionadas",
                Material.NETHER_SPROUTS, "Brotes del Nether",
                Material.RED_MUSHROOM, "Champiñón rojo",
                Material.BROWN_MUSHROOM, "Champiñón marrón",
                Material.HANGING_ROOTS, "Raíces colgantes");
        varias(lista, "Plantas", 32, 8,
                Material.OAK_LEAVES, "Hojas de roble",
                Material.SPRUCE_LEAVES, "Hojas de abeto",
                Material.BIRCH_LEAVES, "Hojas de abedul",
                Material.JUNGLE_LEAVES, "Hojas de selva",
                Material.ACACIA_LEAVES, "Hojas de acacia",
                Material.DARK_OAK_LEAVES, "Hojas de roble oscuro",
                Material.MANGROVE_LEAVES, "Hojas de manglar",
                Material.CHERRY_LEAVES, "Hojas de cerezo",
                Material.PALE_OAK_LEAVES, "Hojas de roble pálido",
                Material.AZALEA_LEAVES, "Hojas de azalea",
                Material.FLOWERING_AZALEA_LEAVES, "Hojas de azalea en flor");
        varias(lista, "Plantas", 4, 12,
                Material.OAK_SAPLING, "Brote de roble",
                Material.SPRUCE_SAPLING, "Brote de abeto",
                Material.BIRCH_SAPLING, "Brote de abedul",
                Material.JUNGLE_SAPLING, "Brote de selva",
                Material.ACACIA_SAPLING, "Brote de acacia",
                Material.DARK_OAK_SAPLING, "Brote de roble oscuro",
                Material.CHERRY_SAPLING, "Brote de cerezo",
                Material.PALE_OAK_SAPLING, "Brote de roble pálido",
                Material.MANGROVE_PROPAGULE, "Propágulo");
        varias(lista, "Plantas", 16, 6,
                Material.SUGAR_CANE, "Caña de azúcar",
                Material.LILY_PAD, "Nenúfar");
        add(lista, "Plantas", Material.BAMBOO, 64, 8, "Bambú");
        add(lista, "Plantas", Material.CACTUS, 8, 10, "Cactus");
        add(lista, "Plantas", Material.PUMPKIN, 8, 10, "Calabaza");
        add(lista, "Plantas", Material.CARVED_PUMPKIN, 4, 12, "Calabaza tallada");
        add(lista, "Plantas", Material.WILDFLOWERS, 16, 10, "Flores silvestres");
        add(lista, "Plantas", Material.GLOW_BERRIES, 8, 12, "Bayas luminosas");
        add(lista, "Plantas", Material.PALE_HANGING_MOSS, 16, 20, "Musgo pálido colgante");
        add(lista, "Plantas", Material.SPORE_BLOSSOM, 1, 24, "Espora");
        add(lista, "Plantas", Material.PITCHER_PLANT, 4, 20, "Planta odre");
        add(lista, "Plantas", Material.FIREFLY_BUSH, 4, 20, "Arbusto de luciérnagas");
        add(lista, "Plantas", Material.CREAKING_HEART, 1, 24, "Corazón del creaking");
    }

    private static void tierra(List<Fila> lista) {
        varias(lista, "Tierra", 32, 8,
                Material.DIRT, "Tierra",
                Material.ROOTED_DIRT, "Tierra enraizada",
                Material.MUD, "Barro");
        add(lista, "Tierra", Material.COARSE_DIRT, 32, 12, "Tierra estéril");
        varias(lista, "Tierra", 32, 12,
                Material.CLAY, "Arcilla",
                Material.GRAVEL, "Grava",
                Material.SAND, "Arena");
        add(lista, "Tierra", Material.RED_SAND, 32, 16, "Arena roja");
        add(lista, "Tierra", Material.PACKED_MUD, 32, 16, "Barro compacto");
        add(lista, "Tierra", Material.DIRT_PATH, 32, 10, "Camino de tierra");
        add(lista, "Tierra", Material.GRASS_BLOCK, 16, 16, "Césped");
        add(lista, "Tierra", Material.PODZOL, 16, 16, "Podzol");
        add(lista, "Tierra", Material.MYCELIUM, 16, 16, "Micelio");
        add(lista, "Tierra", Material.MOSS_BLOCK, 32, 12, "Musgo");
        add(lista, "Tierra", Material.MOSS_CARPET, 32, 8, "Tapiz de musgo");
        add(lista, "Tierra", Material.PALE_MOSS_BLOCK, 16, 24, "Musgo pálido");
        add(lista, "Tierra", Material.PALE_MOSS_CARPET, 16, 16, "Tapiz de musgo pálido");
        add(lista, "Tierra", Material.SNOW_BLOCK, 32, 8, "Nieve");
        add(lista, "Tierra", Material.ICE, 16, 12, "Hielo");
        add(lista, "Tierra", Material.PACKED_ICE, 16, 128, "Hielo compacto");
        add(lista, "Tierra", Material.BLUE_ICE, 4, 360, "Hielo azul");
    }

    private static void nether(List<Fila> lista) {
        add(lista, "Nether", Material.NETHERRACK, 32, 8, "Piedra del Nether");
        obra(lista, "Nether", "NETHER_BRICKS", "NETHER_BRICK_STAIRS", "NETHER_BRICK_SLAB", "NETHER_BRICK_WALL", "ladrillos del Nether", 32, 24);
        poner(lista, "Nether", "NETHER_BRICK_FENCE", 16, 16, "Valla del Nether");
        poner(lista, "Nether", "CRACKED_NETHER_BRICKS", 32, 24, "Ladrillos del Nether agrietados");
        poner(lista, "Nether", "CHISELED_NETHER_BRICKS", 16, 16, "Ladrillos del Nether cincelados");
        obra(lista, "Nether", "RED_NETHER_BRICKS", "RED_NETHER_BRICK_STAIRS", "RED_NETHER_BRICK_SLAB", "RED_NETHER_BRICK_WALL", "ladrillos rojos del Nether", 32, 28);
        add(lista, "Nether", Material.BASALT, 32, 16, "Basalto");
        add(lista, "Nether", Material.POLISHED_BASALT, 32, 20, "Basalto pulido");
        add(lista, "Nether", Material.SMOOTH_BASALT, 32, 20, "Basalto liso");
        obra(lista, "Nether", "BLACKSTONE", "BLACKSTONE_STAIRS", "BLACKSTONE_SLAB", "BLACKSTONE_WALL", "piedra negra", 32, 20);
        obra(lista, "Nether", "POLISHED_BLACKSTONE", "POLISHED_BLACKSTONE_STAIRS", "POLISHED_BLACKSTONE_SLAB", "POLISHED_BLACKSTONE_WALL", "piedra negra pulida", 32, 24);
        obra(lista, "Nether", "POLISHED_BLACKSTONE_BRICKS", "POLISHED_BLACKSTONE_BRICK_STAIRS", "POLISHED_BLACKSTONE_BRICK_SLAB", "POLISHED_BLACKSTONE_BRICK_WALL", "ladrillos de piedra negra", 32, 28);
        poner(lista, "Nether", "CRACKED_POLISHED_BLACKSTONE_BRICKS", 32, 28, "Ladrillos negros agrietados");
        poner(lista, "Nether", "CHISELED_POLISHED_BLACKSTONE", 16, 20, "Piedra negra cincelada");
        poner(lista, "Nether", "POLISHED_BLACKSTONE_BUTTON", 8, 6, "Botón de piedra negra");
        poner(lista, "Nether", "POLISHED_BLACKSTONE_PRESSURE_PLATE", 8, 8, "Placa de piedra negra");
        add(lista, "Nether", Material.SOUL_SAND, 32, 16, "Arena de almas");
        add(lista, "Nether", Material.SOUL_SOIL, 32, 16, "Tierra de almas");
        add(lista, "Nether", Material.MAGMA_BLOCK, 16, 16, "Magma");
        add(lista, "Nether", Material.NETHER_WART_BLOCK, 16, 20, "Bloque de verruga");
        add(lista, "Nether", Material.WARPED_WART_BLOCK, 16, 20, "Bloque de verruga distorsionada");
        add(lista, "Nether", Material.CRIMSON_NYLIUM, 16, 16, "Nicelio carmesí");
        add(lista, "Nether", Material.WARPED_NYLIUM, 16, 16, "Nicelio distorsionado");
        add(lista, "Nether", Material.BONE_BLOCK, 16, 16, "Bloque de hueso");
        add(lista, "Nether", Material.CRYING_OBSIDIAN, 16, 28, "Obsidiana llorosa");
        obra(lista, "Nether", "QUARTZ_BLOCK", "QUARTZ_STAIRS", "QUARTZ_SLAB", null, "cuarzo", 16, 32);
        add(lista, "Nether", Material.QUARTZ_PILLAR, 16, 32, "Pilar de cuarzo");
        add(lista, "Nether", Material.QUARTZ_BRICKS, 16, 32, "Ladrillos de cuarzo");
        add(lista, "Nether", Material.CHISELED_QUARTZ_BLOCK, 16, 36, "Cuarzo cincelado");
        obra(lista, "Nether", "SMOOTH_QUARTZ", "SMOOTH_QUARTZ_STAIRS", "SMOOTH_QUARTZ_SLAB", null, "cuarzo liso", 16, 36);
    }

    private static void end(List<Fila> lista) {
        add(lista, "End", Material.END_STONE, 32, 28, "Piedra del End");
        obra(lista, "End", "END_STONE_BRICKS", "END_STONE_BRICK_STAIRS", "END_STONE_BRICK_SLAB", "END_STONE_BRICK_WALL", "ladrillos del End", 32, 36);
        obra(lista, "End", "PURPUR_BLOCK", "PURPUR_STAIRS", "PURPUR_SLAB", null, "púrpur", 16, 36);
        add(lista, "End", Material.PURPUR_PILLAR, 16, 36, "Pilar de púrpur");
        add(lista, "End", Material.CHORUS_FLOWER, 4, 24, "Flor de chorus");
    }

    private static void cobre(List<Fila> lista) {
        add(lista, "Cobre", Material.COPPER_BLOCK, 16, 20, "Cobre");
        String[] oxido = {"", "EXPOSED_", "WEATHERED_", "OXIDIZED_"};
        String[] nombre = {"", "expuesto", "erosionado", "oxidado"};
        for (int i = 0; i < oxido.length; i++) {
            String extra = nombre[i].isEmpty() ? "" : " " + nombre[i];
            String cera = "WAXED_" + oxido[i];
            if (i == 0) {
                poner(lista, "Cobre", "WAXED_COPPER_BLOCK", 16, 24, "Cobre encerado");
            } else {
                poner(lista, "Cobre", cera + "COPPER", 16, 24, "Cobre" + extra + " encerado");
            }
            poner(lista, "Cobre", cera + "CUT_COPPER", 16, 24, "Cobre cortado" + extra + " encerado");
            poner(lista, "Cobre", cera + "CUT_COPPER_STAIRS", 16, 36, "Escalera de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "CUT_COPPER_SLAB", 32, 24, "Losa de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "CHISELED_COPPER", 16, 28, "Cobre cincelado" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_GRATE", 16, 24, "Reja de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_DOOR", 4, 12, "Puerta de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_TRAPDOOR", 8, 12, "Trampilla de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_BARS", 16, 16, "Barrotes de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_CHAIN", 8, 12, "Cadena de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "COPPER_GOLEM_STATUE", 1, 20, "Estatua de cobre" + extra + " encerado");
            poner(lista, "Cobre", cera + "LIGHTNING_ROD", 1, 12, "Pararrayos" + extra + " encerado");
        }
    }

    private static void mar(List<Fila> lista) {
        obra(lista, "Mar", "PRISMARINE", "PRISMARINE_STAIRS", "PRISMARINE_SLAB", "PRISMARINE_WALL", "prismarina", 16, 32);
        obra(lista, "Mar", "PRISMARINE_BRICKS", "PRISMARINE_BRICK_STAIRS", "PRISMARINE_BRICK_SLAB", null, "ladrillos de prismarina", 16, 36);
        obra(lista, "Mar", "DARK_PRISMARINE", "DARK_PRISMARINE_STAIRS", "DARK_PRISMARINE_SLAB", null, "prismarina oscura", 16, 36);
        coral(lista, "TUBE", "de tubo");
        coral(lista, "BRAIN", "de cerebro");
        coral(lista, "BUBBLE", "de burbuja");
        coral(lista, "FIRE", "de fuego");
        coral(lista, "HORN", "de cuerno");
        add(lista, "Mar", Material.SPONGE, 2, 24, "Esponja");
        add(lista, "Mar", Material.WET_SPONGE, 4, 16, "Esponja mojada");
        add(lista, "Mar", Material.KELP, 16, 6, "Alga");
        add(lista, "Mar", Material.DRIED_KELP_BLOCK, 16, 12, "Bloque de alga seca");
        add(lista, "Mar", Material.SEAGRASS, 16, 6, "Hierba marina");
    }

    private static void muebles(List<Fila> lista) {
        add(lista, "Muebles", Material.ITEM_FRAME, 8, 12, "Marcos");
        add(lista, "Muebles", Material.GLOW_ITEM_FRAME, 4, 20, "Marcos luminosos");
        add(lista, "Muebles", Material.PAINTING, 4, 12, "Cuadros");
        add(lista, "Muebles", Material.FLOWER_POT, 8, 6, "Maceta");
        add(lista, "Muebles", Material.DECORATED_POT, 2, 12, "Maceta decorada");
        add(lista, "Muebles", Material.BOOKSHELF, 4, 16, "Estanterías");
        add(lista, "Muebles", Material.CHISELED_BOOKSHELF, 1, 20, "Estantería cincelada");
        add(lista, "Muebles", Material.LECTERN, 1, 12, "Atril");
        add(lista, "Muebles", Material.BELL, 1, 24, "Campana");
        add(lista, "Muebles", Material.ARMOR_STAND, 1, 12, "Maniquí");
        add(lista, "Muebles", Material.LADDER, 32, 8, "Escalera de mano");
        add(lista, "Muebles", Material.SCAFFOLDING, 32, 12, "Andamio");
        add(lista, "Muebles", Material.CAMPFIRE, 1, 12, "Hoguera");
        add(lista, "Muebles", Material.SOUL_CAMPFIRE, 1, 16, "Hoguera de alma");
        add(lista, "Muebles", Material.HAY_BLOCK, 8, 12, "Fardos");
        add(lista, "Muebles", Material.NOTE_BLOCK, 4, 12, "Bloque de notas");
        add(lista, "Muebles", Material.JUKEBOX, 1, 36, "Gramola");
        add(lista, "Muebles", Material.LOOM, 1, 12, "Telar");
        add(lista, "Muebles", Material.CARTOGRAPHY_TABLE, 1, 12, "Mesa de mapas");
        add(lista, "Muebles", Material.FLETCHING_TABLE, 1, 12, "Mesa de flechas");
        add(lista, "Muebles", Material.SMITHING_TABLE, 1, 12, "Mesa de herrería");
        add(lista, "Muebles", Material.STONECUTTER, 1, 12, "Cortapiedras");
        add(lista, "Muebles", Material.GRINDSTONE, 1, 12, "Afiladora");
        add(lista, "Muebles", Material.CAULDRON, 1, 12, "Caldero");
        add(lista, "Muebles", Material.CRAFTING_TABLE, 1, 2, "Mesa de trabajo");
        add(lista, "Muebles", Material.FURNACE, 1, 8, "Horno");
        add(lista, "Muebles", Material.SMOKER, 1, 16, "Ahumador");
        add(lista, "Muebles", Material.BLAST_FURNACE, 1, 24, "Alto horno");
        add(lista, "Muebles", Material.COMPOSTER, 1, 6, "Compostador");
        add(lista, "Muebles", Material.IRON_BARS, 16, 24, "Barrotes");
        add(lista, "Muebles", Material.IRON_CHAIN, 4, 20, "Cadenas");
        add(lista, "Muebles", Material.IRON_DOOR, 1, 24, "Puerta de hierro");
        add(lista, "Muebles", Material.IRON_TRAPDOOR, 1, 20, "Trampilla de hierro");
        add(lista, "Muebles", Material.LIGHTNING_ROD, 1, 12, "Pararrayos");
        add(lista, "Muebles", Material.BEEHIVE, 1, 24, "Colmena");
        add(lista, "Muebles", Material.HONEYCOMB_BLOCK, 16, 16, "Bloque de panal");
        discos(lista);
        fragmentos(lista);
        patrones(lista);
        add(lista, "Muebles", Material.SKELETON_SKULL, 1, 20, "Cabeza de esqueleto");
        add(lista, "Muebles", Material.ZOMBIE_HEAD, 1, 20, "Cabeza de zombi");
        add(lista, "Muebles", Material.CREEPER_HEAD, 1, 20, "Cabeza de creeper");
        add(lista, "Muebles", Material.PIGLIN_HEAD, 1, 20, "Cabeza de piglin");
        add(lista, "Muebles", Material.DRAGON_HEAD, 1, 48, "Cabeza de dragón");
    }

    private static void mecanismos(List<Fila> lista) {
        add(lista, "Mecanismos", Material.REDSTONE, 32, 12, "Redstone");
        add(lista, "Mecanismos", Material.REDSTONE_TORCH, 16, 6, "Antorchas de redstone");
        add(lista, "Mecanismos", Material.REPEATER, 4, 12, "Repetidores");
        add(lista, "Mecanismos", Material.COMPARATOR, 2, 20, "Comparadores");
        add(lista, "Mecanismos", Material.LEVER, 8, 6, "Palancas");
        add(lista, "Mecanismos", Material.DAYLIGHT_DETECTOR, 1, 12, "Sensor de luz");
        add(lista, "Mecanismos", Material.PISTON, 4, 32, "Pistones");
        add(lista, "Mecanismos", Material.STICKY_PISTON, 2, 24, "Pistones pegajosos");
        add(lista, "Mecanismos", Material.OBSERVER, 2, 24, "Observadores");
        add(lista, "Mecanismos", Material.DISPENSER, 2, 20, "Dispensadores");
        add(lista, "Mecanismos", Material.DROPPER, 4, 20, "Soltadores");
        add(lista, "Mecanismos", Material.HOPPER, 1, 20, "Tolva");
        add(lista, "Mecanismos", Material.REDSTONE_BLOCK, 4, 24, "Bloques de redstone");
        add(lista, "Mecanismos", Material.SLIME_BLOCK, 4, 12, "Bloques de slime");
        add(lista, "Mecanismos", Material.HONEY_BLOCK, 4, 16, "Bloques de miel");
        add(lista, "Mecanismos", Material.TARGET, 4, 12, "Dianas");
        add(lista, "Mecanismos", Material.RAIL, 16, 28, "Raíles");
        add(lista, "Mecanismos", Material.POWERED_RAIL, 8, 24, "Raíles propulsores");
        add(lista, "Mecanismos", Material.DETECTOR_RAIL, 4, 16, "Raíles detectores");
        add(lista, "Mecanismos", Material.ACTIVATOR_RAIL, 4, 16, "Raíles activadores");
        add(lista, "Mecanismos", Material.MINECART, 1, 20, "Vagoneta");
        add(lista, "Mecanismos", Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 2, 16, "Placa de hierro");
        add(lista, "Mecanismos", Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 2, 16, "Placa de oro");
    }

    private static void discos(List<Fila> lista) {
        varias(lista, "Muebles", 1, 12,
                Material.MUSIC_DISC_13, "Disco 13",
                Material.MUSIC_DISC_CAT, "Disco cat",
                Material.MUSIC_DISC_BLOCKS, "Disco blocks",
                Material.MUSIC_DISC_CHIRP, "Disco chirp",
                Material.MUSIC_DISC_FAR, "Disco far",
                Material.MUSIC_DISC_MALL, "Disco mall",
                Material.MUSIC_DISC_MELLOHI, "Disco mellohi",
                Material.MUSIC_DISC_STAL, "Disco stal",
                Material.MUSIC_DISC_STRAD, "Disco strad",
                Material.MUSIC_DISC_WARD, "Disco ward",
                Material.MUSIC_DISC_11, "Disco 11",
                Material.MUSIC_DISC_WAIT, "Disco wait",
                Material.MUSIC_DISC_LAVA_CHICKEN, "Disco Lava Chicken");
        varias(lista, "Muebles", 1, 28,
                Material.MUSIC_DISC_5, "Disco 5",
                Material.MUSIC_DISC_OTHERSIDE, "Disco otherside",
                Material.MUSIC_DISC_PIGSTEP, "Disco Pigstep",
                Material.MUSIC_DISC_RELIC, "Disco Relic",
                Material.MUSIC_DISC_CREATOR, "Disco Creator",
                Material.MUSIC_DISC_CREATOR_MUSIC_BOX, "Disco Creator, caja de música",
                Material.MUSIC_DISC_PRECIPICE, "Disco Precipice",
                Material.MUSIC_DISC_TEARS, "Disco Tears");
    }

    private static void fragmentos(List<Fila> lista) {
        varias(lista, "Muebles", 1, 12,
                Material.ANGLER_POTTERY_SHERD, "Fragmento pescador",
                Material.ARCHER_POTTERY_SHERD, "Fragmento arquero",
                Material.ARMS_UP_POTTERY_SHERD, "Fragmento brazos en alto",
                Material.BLADE_POTTERY_SHERD, "Fragmento hoja",
                Material.BREWER_POTTERY_SHERD, "Fragmento cervecero",
                Material.BURN_POTTERY_SHERD, "Fragmento quema",
                Material.DANGER_POTTERY_SHERD, "Fragmento peligro",
                Material.EXPLORER_POTTERY_SHERD, "Fragmento explorador",
                Material.FLOW_POTTERY_SHERD, "Fragmento flujo",
                Material.FRIEND_POTTERY_SHERD, "Fragmento amigo",
                Material.GUSTER_POTTERY_SHERD, "Fragmento ráfaga",
                Material.HEART_POTTERY_SHERD, "Fragmento corazón",
                Material.HEARTBREAK_POTTERY_SHERD, "Fragmento desamor",
                Material.HOWL_POTTERY_SHERD, "Fragmento aullido",
                Material.MINER_POTTERY_SHERD, "Fragmento minero",
                Material.MOURNER_POTTERY_SHERD, "Fragmento doliente",
                Material.PLENTY_POTTERY_SHERD, "Fragmento abundancia",
                Material.PRIZE_POTTERY_SHERD, "Fragmento premio",
                Material.SCRAPE_POTTERY_SHERD, "Fragmento raspado",
                Material.SHEAF_POTTERY_SHERD, "Fragmento haz",
                Material.SHELTER_POTTERY_SHERD, "Fragmento refugio",
                Material.SKULL_POTTERY_SHERD, "Fragmento calavera",
                Material.SNORT_POTTERY_SHERD, "Fragmento resoplido");
    }

    private static void patrones(List<Fila> lista) {
        varias(lista, "Muebles", 1, 12,
                Material.FLOWER_BANNER_PATTERN, "Patrón flor",
                Material.FIELD_MASONED_BANNER_PATTERN, "Patrón mampostería",
                Material.BORDURE_INDENTED_BANNER_PATTERN, "Patrón borde dentado");
        varias(lista, "Muebles", 1, 20,
                Material.CREEPER_BANNER_PATTERN, "Patrón creeper",
                Material.SKULL_BANNER_PATTERN, "Patrón calavera",
                Material.GLOBE_BANNER_PATTERN, "Patrón globo",
                Material.PIGLIN_BANNER_PATTERN, "Patrón piglin",
                Material.FLOW_BANNER_PATTERN, "Patrón flujo",
                Material.GUSTER_BANNER_PATTERN, "Patrón ráfaga");
    }

    private static void madera(List<Fila> lista, String nombre, String prefijo, boolean hongo, int troncos) {
        int tablones = paso(troncos / 4.0);
        int corteza = paso(troncos * 4.0 / 3.0);
        if (hongo) {
            poner(lista, "Madera", prefijo + "_STEM", 32, troncos, "Tallo de " + nombre);
            poner(lista, "Madera", prefijo + "_HYPHAE", 32, corteza, "Hifas de " + nombre);
            poner(lista, "Madera", "STRIPPED_" + prefijo + "_STEM", 32, troncos, "Tallo pelado de " + nombre);
            poner(lista, "Madera", "STRIPPED_" + prefijo + "_HYPHAE", 32, corteza, "Hifas peladas de " + nombre);
        } else if (!"BAMBOO".equals(prefijo)) {
            poner(lista, "Madera", prefijo + "_LOG", 32, troncos, "Tronco de " + nombre);
            poner(lista, "Madera", prefijo + "_WOOD", 32, corteza, "Madera de " + nombre);
            poner(lista, "Madera", "STRIPPED_" + prefijo + "_LOG", 32, troncos, "Tronco pelado de " + nombre);
            poner(lista, "Madera", "STRIPPED_" + prefijo + "_WOOD", 32, corteza, "Madera pelada de " + nombre);
        }
        poner(lista, "Madera", prefijo + "_PLANKS", 32, tablones, "Tablones de " + nombre);
        poner(lista, "Madera", prefijo + "_STAIRS", 16, paso(troncos * 3.0 / 16.0), "Escalera de " + nombre);
        poner(lista, "Madera", prefijo + "_SLAB", 64, tablones, "Losa de " + nombre);
        poner(lista, "Madera", prefijo + "_FENCE", 16, paso(troncos * 5.0 / 24.0), "Valla de " + nombre);
        poner(lista, "Madera", prefijo + "_FENCE_GATE", 8, paso(troncos / 4.0), "Puerta de valla de " + nombre);
        poner(lista, "Madera", prefijo + "_DOOR", 6, paso(troncos / 8.0), "Puerta de " + nombre);
        poner(lista, "Madera", prefijo + "_TRAPDOOR", 8, paso(troncos / 4.0), "Trampilla de " + nombre);
        poner(lista, "Madera", prefijo + "_BUTTON", 16, paso(troncos / 8.0), "Botón de " + nombre);
        poner(lista, "Madera", prefijo + "_PRESSURE_PLATE", 8, paso(troncos / 6.0), "Placa de " + nombre);
        poner(lista, "Madera", prefijo + "_SIGN", 8, paso(troncos / 6.0), "Cartel de " + nombre);
        if ("BAMBOO".equals(prefijo)) {
            poner(lista, "Madera", prefijo + "_HANGING_SIGN", 4, 16, "Cartel colgante de " + nombre);
        } else {
            poner(lista, "Madera", prefijo + "_HANGING_SIGN", 4, paso(troncos / 2.0), "Cartel colgante de " + nombre);
        }
        poner(lista, "Madera", prefijo + "_SHELF", 4, paso(troncos / 4.0), "Estante de " + nombre);
    }

    private static void coral(List<Fila> lista, String tipo, String nombre) {
        poner(lista, "Mar", tipo + "_CORAL", 4, 12, "Coral " + nombre);
        poner(lista, "Mar", tipo + "_CORAL_FAN", 4, 12, "Abanico " + nombre);
        poner(lista, "Mar", tipo + "_CORAL_BLOCK", 16, 28, "Bloque de coral " + nombre);
        poner(lista, "Mar", "DEAD_" + tipo + "_CORAL_BLOCK", 16, 16, "Coral muerto " + nombre);
    }

    private static void obra(List<Fila> lista, String categoria, String bloque, String escalera, String losa, String muro, String nombre, int bloques, int precio) {
        poner(lista, categoria, bloque, bloques, precio, capitalizar(nombre));
        if (escalera != null) {
            poner(lista, categoria, escalera, 16, paso(precio * 24.0 / bloques), "Escalera de " + nombre);
        }
        if (losa != null) {
            poner(lista, categoria, losa, 32, paso(precio * 16.0 / bloques), "Losa de " + nombre);
        }
        if (muro != null) {
            poner(lista, categoria, muro, 16, paso(precio * 16.0 / bloques), "Muro de " + nombre);
        }
    }

    private static int paso(double valor) {
        return Math.max(2, (int) Math.ceil(valor / 2.0) * 2);
    }

    private static void colores(List<Fila> lista, String categoria, String sufijo, String pieza, int cantidad, int precio, boolean femenino) {
        String[] nombres = femenino
                ? new String[] {
                    "blanca", "naranja", "magenta", "celeste", "amarilla", "lima", "rosa", "gris",
                    "gris claro", "cian", "morada", "azul", "marrón", "verde", "roja", "negra"
                }
                : new String[] {
                    "blanco", "naranja", "magenta", "celeste", "amarillo", "lima", "rosa", "gris",
                    "gris claro", "cian", "morado", "azul", "marrón", "verde", "rojo", "negro"
                };
        String[] materiales = {
                "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
                "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
        };
        for (int i = 0; i < nombres.length; i++) {
            poner(lista, categoria, materiales[i] + sufijo, cantidad, precio, pieza + nombres[i]);
        }
    }

    private static void varias(List<Fila> lista, String categoria, int cantidad, int precio, Object... pares) {
        if (pares.length % 2 != 0) {
            throw new IllegalStateException("el catálogo está desparejo");
        }
        for (int i = 0; i < pares.length; i += 2) {
            add(lista, categoria, (Material) pares[i], cantidad, precio, (String) pares[i + 1]);
        }
    }

    private static void add(List<Fila> lista, String categoria, Material material, int cantidad, int precio, String nombre) {
        lista.add(new Fila(categoria, material, cantidad, precio * ESCALA, nombre));
    }

    private static void poner(List<Fila> lista, String categoria, String material, int cantidad, int precio, String nombre) {
        try {
            add(lista, categoria, Material.valueOf(material), cantidad, precio, nombre);
        } catch (IllegalArgumentException e) {
            SALTADOS.add(material);
        }
    }

    private static String capitalizar(String nombre) {
        if (nombre.isEmpty()) {
            return nombre;
        }
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }

    static final class Seccion {
        final String nombre;
        final Material icono;

        Seccion(String nombre, Material icono) {
            this.nombre = nombre;
            this.icono = icono;
        }
    }

    static final class Fila {
        final String categoria;
        final Material material;
        final int cantidad;
        final int precio;
        final String nombre;

        Fila(String categoria, Material material, int cantidad, int precio, String nombre) {
            this.categoria = categoria;
            this.material = material;
            this.cantidad = cantidad;
            this.precio = precio;
            this.nombre = nombre;
        }
    }
}
