package com.nexuscraft.nexusfarming;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Wires block place/break up to {@link ScarecrowService}'s anchor registration, and enforces the
 * actual trample protection on {@link EntityChangeBlockEvent}.
 */
public final class ScarecrowListener implements Listener {

    private final FarmingConfig config;
    private final ScarecrowService scarecrow;
    private final EffectsUtil effects;

    public ScarecrowListener(FarmingConfig config, ScarecrowService scarecrow, EffectsUtil effects) {
        this.config = config;
        this.scarecrow = scarecrow;
        this.effects = effects;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!config.scarecrowEnabled) {
            return;
        }
        Block block = event.getBlock();
        if (!isPumpkinLike(block.getType())) {
            return;
        }
        Block below = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        if (below.getType() != Material.HAY_BLOCK) {
            return;
        }
        scarecrow.registerAnchor(block);
        effects.playScarecrowPlaced(block.getLocation());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        if (isPumpkinLike(type) && scarecrow.isAnchor(block)) {
            scarecrow.unregisterAnchor(block);
            return;
        }

        if (type == Material.HAY_BLOCK) {
            Block above = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
            if (isPumpkinLike(above.getType()) && scarecrow.isAnchor(above)) {
                scarecrow.unregisterAnchor(above);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrample(EntityChangeBlockEvent event) {
        if (!config.scarecrowEnabled || event.getTo() != Material.DIRT) {
            return;
        }
        if (scarecrow.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private static boolean isPumpkinLike(Material material) {
        return material == Material.CARVED_PUMPKIN || material == Material.JACK_O_LANTERN;
    }
}
