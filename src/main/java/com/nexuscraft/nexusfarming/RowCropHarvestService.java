package com.nexuscraft.nexusfarming;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * The headline mechanic: right-click a MATURE row crop with a golden hoe and it harvests AND
 * replants itself, instantly, on the exact same block -- no re-tilling, no re-seeding. Bonus
 * yield, Fertility, Golden Crops, and hoe durability all resolve here too, since they all key
 * off the same single harvest event. Immature crops are declined here (return false) so the
 * caller can route them to {@link GrowthNudgeService} instead.
 */
public final class RowCropHarvestService {

    private final FarmingConfig config;
    private final FertilityService fertility;
    private final GoldenCropFactory goldenCropFactory;
    private final EffectsUtil effects;
    private final Random random;

    public RowCropHarvestService(FarmingConfig config, FertilityService fertility,
                                  GoldenCropFactory goldenCropFactory, EffectsUtil effects, Random random) {
        this.config = config;
        this.fertility = fertility;
        this.goldenCropFactory = goldenCropFactory;
        this.effects = effects;
        this.random = random;
    }

    /**
     * @return true if this block was a mature row crop and was harvested+replanted; false if it
     *         wasn't a row crop at all, or was a row crop that isn't mature yet.
     */
    public boolean harvestOne(Player player, Block block, ItemStack hoe) {
        if (!config.harvestEnabled) {
            return false;
        }
        RowCropType type = RowCropType.fromBlockMaterial(block.getType());
        if (type == null) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return false;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return false;
        }

        World world = block.getWorld();
        Location blockLoc = block.getLocation();
        Location dropLoc = blockLoc.clone().add(0.5, 0.2, 0.5);
        BlockKey key = BlockKey.of(block);
        long currentDay = currentDay();

        double fertilityBonus = config.fertilityEnabled
                ? fertility.bonusChanceFor(key, currentDay, config.fertilityBonusPerLevel)
                : 0.0;
        double chance = YieldRoller.resolveChance(config.bonusYieldBaseChance, GoldenHoeUtil.fortuneLevel(hoe),
                config.fortuneBonusPerLevel, fertilityBonus, config.bonusYieldChanceCap);
        int extra = YieldRoller.rollExtra(chance, config.bonusYieldMinExtra, config.bonusYieldMaxExtra, random);

        int baseYield = type.baseMinYield() + rollSpread(type.baseMaxYield() - type.baseMinYield());
        int totalDrop = baseYield + extra;

        boolean golden = config.goldenCropsEnabled && totalDrop > 0 && random.nextDouble() < config.goldenCropChance;
        dropCrops(world, dropLoc, type, totalDrop, golden);
        if (type.dropsExtraSeeds()) {
            int seeds = rollSpread(3); // 0-3, matching vanilla's own wheat/beetroot seed roll
            if (seeds > 0) {
                world.dropItemNaturally(dropLoc, new ItemStack(type.seedMaterial(), seeds));
            }
        }

        // Auto-replant: reset the SAME block straight back to age 0 rather than breaking it, so
        // there's no farmland exposed and no re-seeding step -- it just starts growing again.
        ageable.setAge(0);
        block.setBlockData(data);

        effects.playHarvest(blockLoc);
        effects.playReplant(blockLoc);
        if (extra > 0) {
            effects.playBonusYield(blockLoc);
        }
        if (golden) {
            effects.playGoldenCropFound(blockLoc);
        }

        if (config.fertilityEnabled) {
            fertility.recordHarvest(key, currentDay);
        }

        if (config.durabilityEnabled && GoldenHoeUtil.damage(hoe, config.durabilityPerBlock, random)) {
            player.getInventory().setItemInMainHand(null);
        }

        return true;
    }

    /**
     * Sneak + right-click: reaps every mature crop of the SAME type within {@code aoe.radius}
     * blocks of the clicked one (same horizontal plane, Chebyshev distance -- real farm rows are
     * flat), capped at {@code aoe.maxBlocksPerUse} so a big field can't turn one click into a
     * lag spike. Stops early if the hoe breaks partway through.
     *
     * @return how many blocks were actually harvested.
     */
    public int harvestAoe(Player player, Block origin, ItemStack hoe) {
        if (!config.aoeEnabled) {
            return 0;
        }
        RowCropType type = RowCropType.fromBlockMaterial(origin.getType());
        if (type == null) {
            return 0;
        }

        int radius = config.aoeRadius;
        World world = origin.getWorld();
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        int processed = 0;

        for (int dx = -radius; dx <= radius && processed < config.aoeMaxBlocks; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (hoe.getAmount() <= 0) {
                    return processed;
                }
                Block block = world.getBlockAt(originX + dx, originY, originZ + dz);
                if (block.getType() != type.blockMaterial()) {
                    continue;
                }
                if (harvestOne(player, block, hoe)) {
                    processed++;
                    if (processed >= config.aoeMaxBlocks) {
                        break;
                    }
                }
            }
        }
        return processed;
    }

    private void dropCrops(World world, Location dropLoc, RowCropType type, int totalDrop, boolean golden) {
        if (totalDrop <= 0) {
            return;
        }
        if (golden) {
            world.dropItemNaturally(dropLoc, goldenCropFactory.create(type.dropMaterial(), 1));
            if (totalDrop > 1) {
                world.dropItemNaturally(dropLoc, new ItemStack(type.dropMaterial(), totalDrop - 1));
            }
        } else {
            world.dropItemNaturally(dropLoc, new ItemStack(type.dropMaterial(), totalDrop));
        }
    }

    private int rollSpread(int spread) {
        return spread <= 0 ? 0 : random.nextInt(spread + 1);
    }

    private static long currentDay() {
        return System.currentTimeMillis() / 86_400_000L;
    }
}
