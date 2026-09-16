package com.nexuscraft.nexusfarming;

import org.bukkit.Material;

/**
 * The row-crop family: grows in place via an {@code Ageable} block (age 0 -> max age = mature),
 * harvested and replanted on the exact same block. Deliberately excludes melon/pumpkin (they
 * grow as a separate fruit block off a stem -- see StemFruitHarvestService) and sugar
 * cane/cactus/bamboo (they grow as a vertical column -- see ColumnCropHarvestService).
 */
public enum RowCropType {
    WHEAT(Material.WHEAT, Material.WHEAT_SEEDS, Material.WHEAT, 7, 1, 1, true),
    CARROTS(Material.CARROTS, Material.CARROT, Material.CARROT, 7, 2, 4, false),
    POTATOES(Material.POTATOES, Material.POTATO, Material.POTATO, 7, 2, 4, false),
    BEETROOTS(Material.BEETROOTS, Material.BEETROOT_SEEDS, Material.BEETROOT, 3, 1, 1, true),
    NETHER_WART(Material.NETHER_WART, Material.NETHER_WART, Material.NETHER_WART, 3, 2, 4, false);

    private final Material blockMaterial;
    private final Material seedMaterial;
    private final Material dropMaterial;
    private final int maxAge;
    private final int baseMinYield;
    private final int baseMaxYield;
    private final boolean dropsExtraSeeds;

    RowCropType(Material blockMaterial, Material seedMaterial, Material dropMaterial, int maxAge,
                int baseMinYield, int baseMaxYield, boolean dropsExtraSeeds) {
        this.blockMaterial = blockMaterial;
        this.seedMaterial = seedMaterial;
        this.dropMaterial = dropMaterial;
        this.maxAge = maxAge;
        this.baseMinYield = baseMinYield;
        this.baseMaxYield = baseMaxYield;
        this.dropsExtraSeeds = dropsExtraSeeds;
    }

    public Material blockMaterial() {
        return blockMaterial;
    }

    /** What you'd need in hand to plant this crop -- used for golden-crop naming/lore, not for planting logic. */
    public Material seedMaterial() {
        return seedMaterial;
    }

    public Material dropMaterial() {
        return dropMaterial;
    }

    public int maxAge() {
        return maxAge;
    }

    public int baseMinYield() {
        return baseMinYield;
    }

    public int baseMaxYield() {
        return baseMaxYield;
    }

    /** Wheat and beetroot are the odd ones out: the plant material isn't also the seed, so a
     *  mature block of either additionally rolls 0-3 loose seeds on top of its guaranteed 1
     *  crop, same as vanilla. Carrots/potatoes/nether wart are their own seed, so no extra roll. */
    public boolean dropsExtraSeeds() {
        return dropsExtraSeeds;
    }

    public static RowCropType fromBlockMaterial(Material material) {
        for (RowCropType type : values()) {
            if (type.blockMaterial == material) {
                return type;
            }
        }
        return null;
    }

    public static boolean isRowCrop(Material material) {
        return fromBlockMaterial(material) != null;
    }
}
