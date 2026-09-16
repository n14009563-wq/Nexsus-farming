package com.nexuscraft.nexusfarming;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a per-farmland-tile "Fertility" level that rises the more you harvest that tile and
 * slowly decays if you leave it alone. Deliberately NOT a global per-tick scan (that's the exact
 * class of lag bug NexusDimensions shipped with, see its CHANGES.md v0.1.1) -- every tile's decay
 * is computed lazily, only when that tile is actually read, and the decayed value is written
 * straight back so the next read on that same tile is O(1) again.
 */
public final class FertilityService {

    /** One entry per tracked tile. Package-private + mutable on purpose: FertilityService owns it entirely. */
    static final class Record {
        int level;
        long lastHarvestDay;

        Record(int level, long lastHarvestDay) {
            this.level = level;
            this.lastHarvestDay = lastHarvestDay;
        }
    }

    private final Map<BlockKey, Record> records = new HashMap<>();
    private final int maxLevel;
    private final int decayAfterDays;

    public FertilityService(int maxLevel, int decayAfterDays) {
        this.maxLevel = Math.max(0, maxLevel);
        this.decayAfterDays = Math.max(0, decayAfterDays);
    }

    public int trackedTileCount() {
        return records.size();
    }

    /**
     * Effective Fertility level for a tile right now, applying any decay owed since its last
     * harvest first (and persisting that decay so it isn't recomputed on the next read).
     */
    public int levelFor(BlockKey key, long currentDay) {
        Record record = records.get(key);
        if (record == null) {
            return 0;
        }
        applyDecay(record, currentDay);
        if (record.level <= 0) {
            records.remove(key);
            return 0;
        }
        return record.level;
    }

    public double bonusChanceFor(BlockKey key, long currentDay, double bonusPerLevel) {
        return levelFor(key, currentDay) * Math.max(0, bonusPerLevel);
    }

    /** Call on every successful harvest of that tile: bumps Fertility by one, capped, and resets its decay clock. */
    public void recordHarvest(BlockKey key, long currentDay) {
        if (maxLevel <= 0) {
            return;
        }
        Record record = records.get(key);
        if (record == null) {
            records.put(key, new Record(1, currentDay));
            return;
        }
        applyDecay(record, currentDay);
        record.level = Math.min(maxLevel, record.level + 1);
        record.lastHarvestDay = currentDay;
    }

    private void applyDecay(Record record, long currentDay) {
        if (decayAfterDays <= 0) {
            return;
        }
        long idleDays = currentDay - record.lastHarvestDay;
        if (idleDays < decayAfterDays) {
            return;
        }
        long decaySteps = idleDays / decayAfterDays;
        if (decaySteps <= 0) {
            return;
        }
        record.level = (int) Math.max(0, record.level - decaySteps);
        // Advance the clock by whole steps consumed, not all the way to currentDay, so a tile
        // that's been idle a long time doesn't get an unfair "free" partial-step credit later.
        record.lastHarvestDay += decaySteps * decayAfterDays;
    }

    // --- persistence (real Bukkit only -- not covered by the standalone test suite, see README) ---

    public void loadFrom(ConfigurationSection section) {
        records.clear();
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection tile = section.getConfigurationSection(key);
            if (tile == null) {
                continue;
            }
            try {
                BlockKey blockKey = BlockKey.fromPathSegment(key);
                int level = tile.getInt("level", 0);
                long lastHarvestDay = tile.getLong("lastHarvestDay", 0L);
                if (level > 0) {
                    records.put(blockKey, new Record(level, lastHarvestDay));
                }
            } catch (RuntimeException ignoredMalformedEntry) {
                // A hand-edited or corrupted entry shouldn't take the whole file down with it.
            }
        }
    }

    public void saveTo(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (Map.Entry<BlockKey, Record> entry : records.entrySet()) {
            ConfigurationSection tile = section.createSection(entry.getKey().toPathSegment());
            tile.set("level", entry.getValue().level);
            tile.set("lastHarvestDay", entry.getValue().lastHarvestDay);
        }
    }
}
