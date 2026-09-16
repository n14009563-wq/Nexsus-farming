package com.nexuscraft.nexusfarming;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Melon and pumpkin grow as a separate fruit block off a stem, not a self-replanting Ageable
 * like the row crops, so there's nothing on this block to "reset" -- vanilla already regrows the
 * stem another fruit on its own once this one's broken. What the golden hoe adds here is bonus
 * yield, Golden Crop rolls, and durability, same as everywhere else. Deliberately does NOT try to
 * force-finish the attached stem's growth: reaching into a neighboring stem block's own state
 * from here risks desyncing it (double-attaching, skipped growth ticks) for a gimmick that
 * doesn't add much over just breaking it again a little later -- see README.
 */
public final class StemFruitHarvestService {

    private static final Map<Material, int[]> YIELD_RANGE = new EnumMap<>(Material.class);
    private static final Map<Material, Material> DROP_MATERIAL = new EnumMap<>(Material.class);

    static {
        YIELD_RANGE.put(Material.MELON, new int[]{3, 7});
        YIELD_RANGE.put(Material.PUMPKIN, new int[]{1, 1});
        DROP_MATERIAL.put(Material.MELON, Material.MELON_SLICE);
        DROP_MATERIAL.put(Material.PUMPKIN, Material.PUMPKIN);
    }

    private final FarmingConfig config;
    private final GoldenCropFactory goldenCropFactory;
    private final EffectsUtil effects;
    private final Random random;

    public StemFruitHarvestService(FarmingConfig config, GoldenCropFactory goldenCropFactory,
                                    EffectsUtil effects, Random random) {
        this.config = config;
        this.goldenCropFactory = goldenCropFactory;
        this.effects = effects;
        this.random = random;
    }

    public static boolean isStemFruit(Material material) {
        return YIELD_RANGE.containsKey(material);
    }

    public boolean harvest(Player player, Block block, ItemStack hoe) {
        if (!config.harvestEnabled) {
            return false;
        }
        Material material = block.getType();
        int[] range = YIELD_RANGE.get(material);
        if (range == null) {
            return false;
        }
        Material dropMaterial = DROP_MATERIAL.get(material);

        World world = block.getWorld();
        Location blockLoc = block.getLocation();
        Location dropLoc = blockLoc.clone().add(0.5, 0.2, 0.5);

        int base = range[0] + (range[1] > range[0] ? random.nextInt(range[1] - range[0] + 1) : 0);
        double chance = YieldRoller.resolveChance(config.bonusYieldBaseChance, GoldenHoeUtil.fortuneLevel(hoe),
                config.fortuneBonusPerLevel, 0.0, config.bonusYieldChanceCap);
        int extra = YieldRoller.rollExtra(chance, config.bonusYieldMinExtra, config.bonusYieldMaxExtra, random);
        int total = base + extra;

        boolean golden = config.goldenCropsEnabled && total > 0 && random.nextDouble() < config.goldenCropChance;
        if (golden) {
            world.dropItemNaturally(dropLoc, goldenCropFactory.create(dropMaterial, 1));
            if (total > 1) {
                world.dropItemNaturally(dropLoc, new ItemStack(dropMaterial, total - 1));
            }
        } else if (total > 0) {
            world.dropItemNaturally(dropLoc, new ItemStack(dropMaterial, total));
        }

        block.setType(Material.AIR);

        effects.playHarvest(blockLoc);
        if (extra > 0) {
            effects.playBonusYield(blockLoc);
        }
        if (golden) {
            effects.playGoldenCropFound(blockLoc);
        }

        if (config.durabilityEnabled && GoldenHoeUtil.damage(hoe, config.durabilityPerBlock, random)) {
            player.getInventory().setItemInMainHand(null);
        }

        return true;
    }
}
