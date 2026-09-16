package com.nexuscraft.nexusfarming;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The single dispatch point for everything the golden hoe does: routes a right-click to whichever
 * harvest service matches the block, or to the Growth Nudge service if it's a row crop that isn't
 * mature yet.
 */
public final class HarvestListener implements Listener {

    private final FarmingConfig config;
    private final RowCropHarvestService rowCropService;
    private final GrowthNudgeService growthNudgeService;
    private final StemFruitHarvestService stemFruitService;
    private final ColumnCropHarvestService columnCropService;

    public HarvestListener(FarmingConfig config, RowCropHarvestService rowCropService,
                            GrowthNudgeService growthNudgeService, StemFruitHarvestService stemFruitService,
                            ColumnCropHarvestService columnCropService) {
        this.config = config;
        this.rowCropService = rowCropService;
        this.growthNudgeService = growthNudgeService;
        this.stemFruitService = stemFruitService;
        this.columnCropService = columnCropService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasBlock()) {
            return;
        }
        // Real Bukkit fires PlayerInteractEvent for both hands on some interactions; only ever
        // act on the main-hand copy so a single right-click can't double-harvest / double-damage
        // the hoe.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack hoe = event.getItem();
        if (!GoldenHoeUtil.isGoldenHoe(hoe) || hoe.getAmount() <= 0) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("nexusfarming.use")) {
            return;
        }

        Block block = event.getClickedBlock();
        Material material = block.getType();
        boolean handled = dispatch(player, block, material, hoe);

        if (handled) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    private boolean dispatch(Player player, Block block, Material material, ItemStack hoe) {
        if (RowCropType.isRowCrop(material)) {
            if (player.isSneaking() && config.aoeEnabled) {
                return rowCropService.harvestAoe(player, block, hoe) > 0;
            }
            if (rowCropService.harvestOne(player, block, hoe)) {
                return true;
            }
            return growthNudgeService.nudge(player, block, hoe);
        }
        if (StemFruitHarvestService.isStemFruit(material)) {
            return stemFruitService.harvest(player, block, hoe);
        }
        if (ColumnCropHarvestService.isColumnCrop(material)) {
            return columnCropService.harvest(player, block, hoe) > 0;
        }
        return false;
    }
}
