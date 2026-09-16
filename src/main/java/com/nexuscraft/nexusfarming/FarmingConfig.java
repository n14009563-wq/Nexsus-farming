package com.nexuscraft.nexusfarming;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Parses config.yml into typed fields. Every field has a hardcoded default matching
 * config.yml's own shipped values, so a missing/deleted key degrades gracefully rather than
 * throwing -- same pattern this whole Nexus ecosystem uses (see NexusCreativeSurvival).
 */
public final class FarmingConfig {

    private final Plugin plugin;

    // --- harvest.* ---
    public boolean harvestEnabled = true;
    public double bonusYieldBaseChance = 0.20;
    public int bonusYieldMinExtra = 1;
    public int bonusYieldMaxExtra = 3;
    public double fortuneBonusPerLevel = 0.10;
    public double bonusYieldChanceCap = 0.90;

    // --- harvest.aoe.* ---
    public boolean aoeEnabled = true;
    public int aoeRadius = 3;
    public int aoeMaxRadius = 6;
    public int aoeMaxBlocks = 64;

    // --- harvest.durability.* ---
    public boolean durabilityEnabled = true;
    public int durabilityPerBlock = 1;

    // --- growthNudge.* ---
    public boolean growthNudgeEnabled = true;
    public int growthNudgeStages = 1;
    public int growthNudgeCooldownSeconds = 8;
    public int growthNudgeDurabilityCost = 1;

    // --- fertility.* ---
    public boolean fertilityEnabled = true;
    public int fertilityMaxLevel = 5;
    public double fertilityBonusPerLevel = 0.05;
    public int fertilityDecayAfterDays = 3;
    public int fertilitySaveIntervalSeconds = 300;

    // --- goldenCrops.* ---
    public boolean goldenCropsEnabled = true;
    public double goldenCropChance = 0.03;
    public int goldenCropBuffDurationSeconds = 30;
    public int goldenCropBuffAmplifier = 1;

    // --- scarecrow.* ---
    public boolean scarecrowEnabled = true;
    public int scarecrowRadius = 5;

    // --- effects.* ---
    public boolean effectsEnabled = true;

    public FarmingConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();

        harvestEnabled = c.getBoolean("harvest.enabled", harvestEnabled);
        bonusYieldBaseChance = clamp01(c.getDouble("harvest.bonusYield.baseChance", bonusYieldBaseChance));
        bonusYieldMinExtra = Math.max(0, c.getInt("harvest.bonusYield.minExtra", bonusYieldMinExtra));
        bonusYieldMaxExtra = Math.max(bonusYieldMinExtra, c.getInt("harvest.bonusYield.maxExtra", bonusYieldMaxExtra));
        fortuneBonusPerLevel = Math.max(0, c.getDouble("harvest.bonusYield.fortuneBonusPerLevel", fortuneBonusPerLevel));
        bonusYieldChanceCap = clamp01(c.getDouble("harvest.bonusYield.chanceCap", bonusYieldChanceCap));

        aoeEnabled = c.getBoolean("harvest.aoe.enabled", aoeEnabled);
        aoeMaxRadius = Math.max(1, c.getInt("harvest.aoe.maxRadius", aoeMaxRadius));
        aoeRadius = clampInt(c.getInt("harvest.aoe.radius", aoeRadius), 1, aoeMaxRadius);
        aoeMaxBlocks = Math.max(1, c.getInt("harvest.aoe.maxBlocksPerUse", aoeMaxBlocks));

        durabilityEnabled = c.getBoolean("harvest.durability.enabled", durabilityEnabled);
        durabilityPerBlock = Math.max(0, c.getInt("harvest.durability.perBlock", durabilityPerBlock));

        growthNudgeEnabled = c.getBoolean("growthNudge.enabled", growthNudgeEnabled);
        growthNudgeStages = Math.max(1, c.getInt("growthNudge.stages", growthNudgeStages));
        growthNudgeCooldownSeconds = Math.max(0, c.getInt("growthNudge.cooldownSeconds", growthNudgeCooldownSeconds));
        growthNudgeDurabilityCost = Math.max(0, c.getInt("growthNudge.durabilityCost", growthNudgeDurabilityCost));

        fertilityEnabled = c.getBoolean("fertility.enabled", fertilityEnabled);
        fertilityMaxLevel = Math.max(0, c.getInt("fertility.maxLevel", fertilityMaxLevel));
        fertilityBonusPerLevel = Math.max(0, c.getDouble("fertility.bonusChancePerLevel", fertilityBonusPerLevel));
        fertilityDecayAfterDays = Math.max(0, c.getInt("fertility.decayAfterDays", fertilityDecayAfterDays));
        fertilitySaveIntervalSeconds = Math.max(30, c.getInt("fertility.saveIntervalSeconds", fertilitySaveIntervalSeconds));

        goldenCropsEnabled = c.getBoolean("goldenCrops.enabled", goldenCropsEnabled);
        goldenCropChance = clamp01(c.getDouble("goldenCrops.chance", goldenCropChance));
        goldenCropBuffDurationSeconds = Math.max(1, c.getInt("goldenCrops.buffDurationSeconds", goldenCropBuffDurationSeconds));
        goldenCropBuffAmplifier = Math.max(0, c.getInt("goldenCrops.buffAmplifier", goldenCropBuffAmplifier));

        scarecrowEnabled = c.getBoolean("scarecrow.enabled", scarecrowEnabled);
        scarecrowRadius = Math.max(1, c.getInt("scarecrow.radius", scarecrowRadius));

        effectsEnabled = c.getBoolean("effects.enabled", effectsEnabled);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
