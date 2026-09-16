package com.nexuscraft.nexusfarming;

import java.util.Random;

/**
 * Pure bonus-yield math, deliberately kept free of any Bukkit type so it can be exercised
 * directly by the standalone test suite. Everything else (Fortune level, Fertility level,
 * config numbers) is resolved by the caller into a single "final chance" before calling here.
 */
public final class YieldRoller {

    private YieldRoller() {
    }

    /**
     * @param baseChance            0.0-1.0 chance before any bonuses.
     * @param fortuneLevel          Fortune level on the tool (0 if none/not applicable).
     * @param fortuneBonusPerLevel  added to chance per Fortune level.
     * @param fertilityBonusChance  already-resolved Fertility contribution (0.0 if disabled/no data).
     * @param chanceCap             hard ceiling on the combined chance.
     */
    public static double resolveChance(double baseChance, int fortuneLevel, double fortuneBonusPerLevel,
                                        double fertilityBonusChance, double chanceCap) {
        double chance = baseChance + (Math.max(0, fortuneLevel) * fortuneBonusPerLevel) + Math.max(0, fertilityBonusChance);
        return clamp01(Math.min(chance, chanceCap));
    }

    /**
     * Rolls whether a bonus triggers, and if so how much extra to grant.
     *
     * @return 0 if the bonus didn't trigger, otherwise a value in [minExtra, maxExtra].
     */
    public static int rollExtra(double chance, int minExtra, int maxExtra, Random random) {
        if (minExtra > maxExtra) {
            throw new IllegalArgumentException("minExtra (" + minExtra + ") > maxExtra (" + maxExtra + ")");
        }
        if (random.nextDouble() >= clamp01(chance)) {
            return 0;
        }
        if (minExtra == maxExtra) {
            return minExtra;
        }
        return minExtra + random.nextInt(maxExtra - minExtra + 1);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
