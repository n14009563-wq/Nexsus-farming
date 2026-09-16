package com.nexuscraft.nexusfarming;

import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Carved Pumpkin (or Jack o'Lantern) on top of a Hay Bale is a Scarecrow: everything within
 * its radius, at the Hay Bale's own height -- the same layer the farmland sits at -- is protected
 * from trampling. The protected-tile set is precomputed ONCE, when the anchor is placed, and kept
 * as a ref-counted reverse index; a trample check is then a single O(1) HashMap lookup no matter
 * how large the radius is, instead of walking every anchor's radius on every single trample event.
 */
public final class ScarecrowService {

    private final FarmingConfig config;
    private final Map<BlockKey, Set<BlockKey>> anchors = new HashMap<>();
    private final Map<BlockKey, Integer> protectedTileRefCount = new HashMap<>();

    public ScarecrowService(FarmingConfig config) {
        this.config = config;
    }

    public void registerAnchor(Block pumpkinBlock) {
        if (!config.scarecrowEnabled) {
            return;
        }
        BlockKey anchorKey = BlockKey.of(pumpkinBlock);
        if (anchors.containsKey(anchorKey)) {
            return;
        }
        Set<BlockKey> protectedTiles = computeProtectedTiles(pumpkinBlock);
        anchors.put(anchorKey, protectedTiles);
        for (BlockKey tile : protectedTiles) {
            protectedTileRefCount.merge(tile, 1, Integer::sum);
        }
    }

    public void unregisterAnchor(Block pumpkinBlock) {
        Set<BlockKey> protectedTiles = anchors.remove(BlockKey.of(pumpkinBlock));
        if (protectedTiles == null) {
            return;
        }
        for (BlockKey tile : protectedTiles) {
            protectedTileRefCount.computeIfPresent(tile, (key, count) -> count <= 1 ? null : count - 1);
        }
    }

    public boolean isAnchor(Block block) {
        return anchors.containsKey(BlockKey.of(block));
    }

    public boolean isProtected(Block block) {
        return protectedTileRefCount.containsKey(BlockKey.of(block));
    }

    public int anchorCount() {
        return anchors.size();
    }

    private Set<BlockKey> computeProtectedTiles(Block pumpkinBlock) {
        int radius = Math.max(0, config.scarecrowRadius);
        String world = pumpkinBlock.getWorld().getName();
        // Pumpkin sits on the Hay Bale, and the Hay Bale sits at the same height as the field's
        // farmland (it's just another ground block dropped into the row) -- one below the pumpkin.
        int farmlandY = pumpkinBlock.getY() - 1;
        int pumpkinX = pumpkinBlock.getX();
        int pumpkinZ = pumpkinBlock.getZ();

        Set<BlockKey> tiles = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                tiles.add(new BlockKey(world, pumpkinX + dx, farmlandY, pumpkinZ + dz));
            }
        }
        return tiles;
    }
}
