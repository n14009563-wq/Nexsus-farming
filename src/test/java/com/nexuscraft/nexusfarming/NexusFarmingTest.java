package com.nexuscraft.nexusfarming;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Standalone test suite (no JUnit -- same house pattern as the rest of the Nexus plugin family):
 * exercises every class in this plugin that's pure logic, or logic plus only the stub Bukkit
 * types that behave deterministically (Material, ItemStack/ItemMeta, Enchantment). Bukkit
 * glue that's genuinely no-op in the stub (FileConfiguration/YamlConfiguration persistence,
 * scheduler timing, event dispatch) is NOT exercised here -- it's covered by the -Xlint:all
 * -Werror compile check against the stub API instead, same division of labor as this whole
 * Nexus ecosystem uses.
 */
public final class NexusFarmingTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testYieldRoller();
        testFertilityService();
        testGoldenHoeUtil();
        testRowCropType();
        testBlockKey();
        testFarmingConfigDefaults();
        testFarmingConfigClamping();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- YieldRoller ---

    private static void testYieldRoller() {
        section("YieldRoller");

        double chance = YieldRoller.resolveChance(0.20, 2, 0.10, 0.05, 0.90);
        checkApprox("resolveChance combines base + fortune + fertility", chance, 0.45, 1e-9);

        double capped = YieldRoller.resolveChance(0.50, 10, 0.10, 0.50, 0.90);
        checkApprox("resolveChance clamps to chanceCap", capped, 0.90, 1e-9);

        double floor = YieldRoller.resolveChance(-5.0, 0, 0.0, 0.0, 0.90);
        checkApprox("resolveChance never goes below 0", floor, 0.0, 1e-9);

        Random alwaysHigh = fixedDoubleRandom(0.99);
        check("rollExtra never triggers at chance 0.0", YieldRoller.rollExtra(0.0, 1, 3, alwaysHigh) == 0);

        Random alwaysLow = fixedDoubleRandom(0.0);
        int extra = YieldRoller.rollExtra(1.0, 1, 3, alwaysLow);
        check("rollExtra always triggers at chance 1.0", extra >= 1 && extra <= 3);

        int fixedExtra = YieldRoller.rollExtra(1.0, 2, 2, alwaysLow);
        checkEquals("rollExtra returns exactly minExtra when minExtra == maxExtra", fixedExtra, 2);

        boolean threw = false;
        try {
            YieldRoller.rollExtra(0.5, 5, 2, new Random());
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check("rollExtra rejects minExtra > maxExtra", threw);

        // Statistical sanity: over many trials at chance 0.30, the trigger rate should land
        // reasonably close to 0.30 with a fixed seed (deterministic, generous tolerance).
        Random seeded = new Random(42);
        int trials = 20_000;
        int triggered = 0;
        for (int i = 0; i < trials; i++) {
            if (YieldRoller.rollExtra(0.30, 1, 1, seeded) > 0) {
                triggered++;
            }
        }
        double rate = triggered / (double) trials;
        check("rollExtra trigger rate ~0.30 over " + trials + " trials (got " + rate + ")",
                Math.abs(rate - 0.30) < 0.02);
    }

    // --- FertilityService ---

    private static void testFertilityService() {
        section("FertilityService");

        FertilityService fertility = new FertilityService(5, 3);
        BlockKey tile = new BlockKey("world", 10, 64, 10);

        checkEquals("fresh tile starts at level 0", fertility.levelFor(tile, 0), 0);

        fertility.recordHarvest(tile, 0);
        checkEquals("first harvest brings tile to level 1", fertility.levelFor(tile, 0), 1);

        for (int i = 0; i < 10; i++) {
            fertility.recordHarvest(tile, 0);
        }
        checkEquals("level never exceeds configured maxLevel", fertility.levelFor(tile, 0), 5);

        // No decay before decayAfterDays has fully elapsed.
        checkEquals("no decay before the idle threshold", fertility.levelFor(tile, 2), 5);

        // Exactly one decay step at the threshold.
        checkEquals("one decay step right at the idle threshold", fertility.levelFor(tile, 3), 4);

        // Long idle: decays all the way to 0 and the tile is dropped from tracking.
        fertility.recordHarvest(tile, 3);
        checkEquals("re-harvesting resets the level upward again", fertility.levelFor(tile, 3), 5);
        checkEquals("a very long idle period decays fully to 0", fertility.levelFor(tile, 1000), 0);
        checkEquals("a fully-decayed tile is dropped from tracking", fertility.trackedTileCount(), 0);

        FertilityService bonusService = new FertilityService(5, 3);
        bonusService.recordHarvest(tile, 0);
        bonusService.recordHarvest(tile, 0);
        checkApprox("bonusChanceFor multiplies level by bonusPerLevel",
                bonusService.bonusChanceFor(tile, 0, 0.05), 0.10, 1e-9);

        FertilityService disabled = new FertilityService(0, 3);
        disabled.recordHarvest(tile, 0);
        checkEquals("maxLevel=0 means Fertility never accumulates", disabled.levelFor(tile, 0), 0);
    }

    // --- GoldenHoeUtil ---

    private static void testGoldenHoeUtil() {
        section("GoldenHoeUtil");

        check("isGoldenHoe true for a golden hoe", GoldenHoeUtil.isGoldenHoe(new ItemStack(Material.GOLDEN_HOE)));
        check("isGoldenHoe false for an iron hoe", !GoldenHoeUtil.isGoldenHoe(new ItemStack(Material.IRON_HOE)));
        check("isGoldenHoe false for null", !GoldenHoeUtil.isGoldenHoe(null));

        ItemStack plainHoe = new ItemStack(Material.GOLDEN_HOE);
        checkEquals("fortuneLevel is 0 with no enchant", GoldenHoeUtil.fortuneLevel(plainHoe), 0);

        ItemStack fortuneHoe = new ItemStack(Material.GOLDEN_HOE);
        fortuneHoe.addUnsafeEnchantment(Enchantment.FORTUNE, 3);
        checkEquals("fortuneLevel reads back the applied enchant", GoldenHoeUtil.fortuneLevel(fortuneHoe), 3);

        // No Unbreaking: every point of damage always counts.
        ItemStack noUnbreaking = new ItemStack(Material.GOLDEN_HOE);
        boolean broke = GoldenHoeUtil.damage(noUnbreaking, 5, fixedIntRandom(0));
        checkEquals("damage with no Unbreaking applies every point", noUnbreaking.getItemMeta().getDamage(), 5);
        check("5 damage on a 32-durability hoe doesn't break it", !broke);

        // Damage right up to the break threshold.
        ItemStack nearBroken = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = nearBroken.getItemMeta();
        meta.setDamage(30);
        nearBroken.setItemMeta(meta);
        boolean brokeNow = GoldenHoeUtil.damage(nearBroken, 5, fixedIntRandom(0));
        check("damage past max durability (32) breaks the hoe", brokeNow);
        checkEquals("a broken hoe's amount is set to 0", nearBroken.getAmount(), 0);

        // Unbreakable items never take damage.
        ItemStack unbreakable = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta unbreakableMeta = unbreakable.getItemMeta();
        unbreakableMeta.setUnbreakable(true);
        unbreakable.setItemMeta(unbreakableMeta);
        GoldenHoeUtil.damage(unbreakable, 10, fixedIntRandom(0));
        checkEquals("an Unbreakable hoe never takes damage", unbreakable.getItemMeta().getDamage(), 0);

        // Unbreaking I's chance-to-actually-damage is 1/(level+1) = 50% per point. Hit a fresh
        // hoe with exactly 1 point of damage many times over (fresh each time so it never nears
        // the break threshold) and check how often that single point actually landed.
        Random seeded = new Random(7);
        int trials = 20_000;
        int accepted = 0;
        for (int i = 0; i < trials; i++) {
            ItemStack freshHoe = new ItemStack(Material.GOLDEN_HOE);
            freshHoe.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            GoldenHoeUtil.damage(freshHoe, 1, seeded);
            if (freshHoe.getItemMeta().getDamage() > 0) {
                accepted++;
            }
        }
        double hitRate = accepted / (double) trials;
        check("Unbreaking I lands close to a 50% per-point hit rate over " + trials + " trials (got " + hitRate + ")",
                Math.abs(hitRate - 0.5) < 0.02);
    }

    // --- RowCropType ---

    private static void testRowCropType() {
        section("RowCropType");

        checkEquals("fromBlockMaterial finds WHEAT", RowCropType.fromBlockMaterial(Material.WHEAT), RowCropType.WHEAT);
        check("fromBlockMaterial returns null for a non-crop", RowCropType.fromBlockMaterial(Material.STONE) == null);
        check("isRowCrop true for CARROTS", RowCropType.isRowCrop(Material.CARROTS));
        check("isRowCrop false for MELON (stem fruit, not a row crop)", !RowCropType.isRowCrop(Material.MELON));

        check("WHEAT drops extra seeds", RowCropType.WHEAT.dropsExtraSeeds());
        check("BEETROOTS drops extra seeds", RowCropType.BEETROOTS.dropsExtraSeeds());
        check("CARROTS does not drop a separate seed item", !RowCropType.CARROTS.dropsExtraSeeds());
        check("POTATOES does not drop a separate seed item", !RowCropType.POTATOES.dropsExtraSeeds());
        check("NETHER_WART does not drop a separate seed item", !RowCropType.NETHER_WART.dropsExtraSeeds());

        checkEquals("WHEAT matures at age 7", RowCropType.WHEAT.maxAge(), 7);
        checkEquals("BEETROOTS matures at age 3", RowCropType.BEETROOTS.maxAge(), 3);
    }

    // --- BlockKey ---

    private static void testBlockKey() {
        section("BlockKey");

        BlockKey a = new BlockKey("world", 1, 2, 3);
        BlockKey b = new BlockKey("world", 1, 2, 3);
        BlockKey c = new BlockKey("world", 4, 2, 3);
        checkEquals("identical coordinates are equal", a, b);
        check("different coordinates are not equal", !a.equals(c));
        checkEquals("equal keys share a hash code", a.hashCode(), b.hashCode());

        checkEquals("chebyshevDistance is the max axis delta", a.chebyshevDistance(new BlockKey("world", 4, 2, 9)), 6);
        checkEquals("chebyshevDistance to self is 0", a.chebyshevDistance(a), 0);
        check("cross-world distance is effectively infinite",
                a.chebyshevDistance(new BlockKey("nether", 1, 2, 3)) == Integer.MAX_VALUE);

        BlockKey withUnderscoreWorld = new BlockKey("my_world_nether", -5, 70, 12);
        String segment = withUnderscoreWorld.toPathSegment();
        checkEquals("path-segment round-trips through a world name containing underscores",
                BlockKey.fromPathSegment(segment), withUnderscoreWorld);
    }

    // --- FarmingConfig ---

    private static void testFarmingConfigDefaults() {
        section("FarmingConfig (defaults)");

        FarmingConfig config = new FarmingConfig(new FakePlugin());
        config.load();

        check("harvest enabled by default", config.harvestEnabled);
        checkApprox("default bonus yield base chance", config.bonusYieldBaseChance, 0.20, 1e-9);
        checkEquals("default AoE radius", config.aoeRadius, 3);
        checkEquals("default Fertility max level", config.fertilityMaxLevel, 5);
        checkApprox("default Golden Crop chance", config.goldenCropChance, 0.03, 1e-9);
        checkEquals("default Scarecrow radius", config.scarecrowRadius, 5);
    }

    private static void testFarmingConfigClamping() {
        section("FarmingConfig (clamping)");

        FakePlugin outOfRangeCap = new FakePlugin();
        outOfRangeCap.config.values.put("harvest.bonusYield.chanceCap", 5.0);
        FarmingConfig capConfig = new FarmingConfig(outOfRangeCap);
        capConfig.load();
        checkApprox("chanceCap above 1.0 is clamped to 1.0", capConfig.bonusYieldChanceCap, 1.0, 1e-9);

        FakePlugin invertedYield = new FakePlugin();
        invertedYield.config.values.put("harvest.bonusYield.minExtra", 5);
        invertedYield.config.values.put("harvest.bonusYield.maxExtra", 2);
        FarmingConfig yieldConfig = new FarmingConfig(invertedYield);
        yieldConfig.load();
        checkEquals("maxExtra is pulled up to minExtra when config has them inverted",
                yieldConfig.bonusYieldMaxExtra, 5);

        FakePlugin bigRadius = new FakePlugin();
        bigRadius.config.values.put("harvest.aoe.maxRadius", 4);
        bigRadius.config.values.put("harvest.aoe.radius", 10);
        FarmingConfig radiusConfig = new FarmingConfig(bigRadius);
        radiusConfig.load();
        checkEquals("aoeRadius is clamped down to aoeMaxRadius", radiusConfig.aoeRadius, 4);

        FakePlugin tinySaveInterval = new FakePlugin();
        tinySaveInterval.config.values.put("fertility.saveIntervalSeconds", 5);
        FarmingConfig saveConfig = new FarmingConfig(tinySaveInterval);
        saveConfig.load();
        checkEquals("fertilitySaveIntervalSeconds has a 30-second floor", saveConfig.fertilitySaveIntervalSeconds, 30);
    }

    // --- test doubles ---

    /** A FileConfiguration whose getX(path, def) calls return a caller-supplied override map, or the default. */
    private static final class FakeConfig extends FileConfiguration {
        final Map<String, Object> values = new HashMap<>();

        @Override
        public boolean getBoolean(String path, boolean def) {
            return values.containsKey(path) ? (Boolean) values.get(path) : def;
        }

        @Override
        public int getInt(String path, int def) {
            return values.containsKey(path) ? (Integer) values.get(path) : def;
        }

        @Override
        public double getDouble(String path, double def) {
            return values.containsKey(path) ? (Double) values.get(path) : def;
        }
    }

    private static final class FakePlugin implements Plugin {
        final FakeConfig config = new FakeConfig();

        @Override public String getName() { return "NexusFarmingTest"; }
        @Override public FileConfiguration getConfig() { return config; }
        @Override public Logger getLogger() { return Logger.getLogger("NexusFarmingTest"); }
        @Override public File getDataFolder() { return new File("."); }
        @Override public void reloadConfig() { }
        @Override public void saveDefaultConfig() { }
        @Override public void saveConfig() { }
        @Override public void saveResource(String resourcePath, boolean replace) { }
        @Override public PluginDescriptionFile getDescription() { return new PluginDescriptionFile(); }
        @Override public Server getServer() { return null; }
    }

    /** A Random whose nextDouble() always returns the same fixed value -- for deterministic threshold tests. */
    private static Random fixedDoubleRandom(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    /** A Random whose nextInt(bound) always returns the same fixed value -- for deterministic threshold tests. */
    private static Random fixedIntRandom(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    // --- tiny assertion framework ---

    private static void section(String name) {
        System.out.println("-- " + name + " --");
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL: " + description);
        }
    }

    private static void checkEquals(String description, Object actual, Object expected) {
        check(description + " (expected " + expected + ", got " + actual + ")",
                actual == null ? expected == null : actual.equals(expected));
    }

    private static void checkApprox(String description, double actual, double expected, double epsilon) {
        check(description + " (expected " + expected + ", got " + actual + ")", Math.abs(actual - expected) <= epsilon);
    }
}
