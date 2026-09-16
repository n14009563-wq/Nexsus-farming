package com.nexuscraft.nexusfarming;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * Sugar cane, cactus, and bamboo all grow as a vertical column where only the bottom-most block
 * is the "anchor" that keeps producing more -- breaking anything above it doesn't slow it down at
 * all (that's exactly how an automatic cane farm works with water or pistons). A golden hoe
 * right-click does the same thing by hand, instantly: everything above the anchor gets collected,
 * the anchor itself is left standing so the column keeps growing right back. No bonus-yield roll
 * here -- these always drop 1-for-1 in vanilla with no Fortune interaction, so inventing a bonus
 * chance for them wouldn't be modeling anything real.
 */
public final class ColumnCropHarvestService {

    private static final Set<Material> COLUMN_MATERIALS = EnumSet.of(Material.SUGAR_CANE, Material.CACTUS, Material.BAMBOO);

    private final FarmingConfig config;
    private final EffectsUtil effects;
    private final Random random;

    public ColumnCropHarvestService(FarmingConfig config, EffectsUtil effects, Random random) {
        this.config = config;
        this.effects = effects;
        this.random = random;
    }

    public static boolean isColumnCrop(Material material) {
        return COLUMN_MATERIALS.contains(material);
    }

    /**
     * @return how many blocks above the anchor were collected.
     */
    public int harvest(Player player, Block clicked, ItemStack hoe) {
        if (!config.harvestEnabled) {
            return 0;
        }
        Material material = clicked.getType();
        if (!COLUMN_MATERIALS.contains(material)) {
            return 0;
        }

        World world = clicked.getWorld();
        Block bottom = clicked;
        while (world.getBlockAt(bottom.getX(), bottom.getY() - 1, bottom.getZ()).getType() == material) {
            bottom = world.getBlockAt(bottom.getX(), bottom.getY() - 1, bottom.getZ());
        }

        int harvested = 0;
        int y = bottom.getY() + 1;
        while (harvested < config.aoeMaxBlocks) {
            Block block = world.getBlockAt(bottom.getX(), y, bottom.getZ());
            if (block.getType() != material) {
                break;
            }
            Location dropLoc = block.getLocation().clone().add(0.5, 0.2, 0.5);
            world.dropItemNaturally(dropLoc, new ItemStack(material, 1));
            block.setType(Material.AIR);
            harvested++;
            y++;
        }

        if (harvested > 0) {
            effects.playHarvest(bottom.getLocation());
            if (config.durabilityEnabled && GoldenHoeUtil.damage(hoe, config.durabilityPerBlock, random)) {
                player.getInventory().setItemInMainHand(null);
            }
        }
        return harvested;
    }
}
