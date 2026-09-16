package com.nexuscraft.nexusfarming;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * A plain, immutable identity for a single block position, deliberately NOT built on top of
 * {@link Location} (Location compares world/x/y/z as doubles with no equals()/hashCode()
 * override in real Bukkit either, so it's a poor map/set key for a block-granular cache). A
 * record gets correct equals()/hashCode() for free, which is all Fertility and Scarecrow need.
 */
public record BlockKey(String world, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /** Chebyshev (chessboard) distance -- cheap, no sqrt, and matches how AoE/Scarecrow radii read in config. */
    public int chebyshevDistance(BlockKey other) {
        if (!world.equals(other.world)) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(x - other.x), Math.max(Math.abs(y - other.y), Math.abs(z - other.z)));
    }

    /** Serialization-friendly form for config/YAML keys, which can't contain most punctuation safely. */
    public String toPathSegment() {
        return world + "_" + x + "_" + y + "_" + z;
    }

    public static BlockKey fromPathSegment(String segment) {
        String[] parts = segment.split("_");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Malformed BlockKey segment: " + segment);
        }
        int z = Integer.parseInt(parts[parts.length - 1]);
        int y = Integer.parseInt(parts[parts.length - 2]);
        int x = Integer.parseInt(parts[parts.length - 3]);
        String world = String.join("_", java.util.Arrays.copyOfRange(parts, 0, parts.length - 3));
        return new BlockKey(world, x, y, z);
    }
}
