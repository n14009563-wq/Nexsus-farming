package com.nexuscraft.nexusfarming;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public final class NexusFarmingPlugin extends JavaPlugin {

    private FarmingConfig config;
    private FertilityService fertility;
    private ScarecrowService scarecrow;
    private BukkitTask fertilitySaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new FarmingConfig(this);
        config.load();

        Random random = new Random();
        EffectsUtil effects = new EffectsUtil(config.effectsEnabled);

        this.fertility = new FertilityService(config.fertilityMaxLevel, config.fertilityDecayAfterDays);
        loadFertility();

        GoldenCropFactory goldenCropFactory = new GoldenCropFactory(this);
        this.scarecrow = new ScarecrowService(config);

        RowCropHarvestService rowCropService = new RowCropHarvestService(config, fertility, goldenCropFactory, effects, random);
        GrowthNudgeService growthNudgeService = new GrowthNudgeService(config, effects, random);
        StemFruitHarvestService stemFruitService = new StemFruitHarvestService(config, goldenCropFactory, effects, random);
        ColumnCropHarvestService columnCropService = new ColumnCropHarvestService(config, effects, random);

        getServer().getPluginManager().registerEvents(
                new HarvestListener(config, rowCropService, growthNudgeService, stemFruitService, columnCropService), this);
        getServer().getPluginManager().registerEvents(new ScarecrowListener(config, scarecrow, effects), this);
        getServer().getPluginManager().registerEvents(new GoldenCropConsumeListener(config, goldenCropFactory, effects), this);

        getCommand("nexusfarming").setExecutor(new NexusFarmingCommand(this, config, fertility, scarecrow));

        startFertilitySaveTask();

        getLogger().info("NexusFarming enabled -- golden hoe in hand, right-click a mature crop to auto-replant it. "
                + "Sneak + right-click to reap a whole patch. Bonus yield " + Math.round(config.bonusYieldBaseChance * 100)
                + "% base, Golden Crops " + Math.round(config.goldenCropChance * 100) + "% chance.");
    }

    @Override
    public void onDisable() {
        if (fertilitySaveTask != null) {
            fertilitySaveTask.cancel();
        }
        saveFertility();
        getLogger().info("NexusFarming disabled.");
    }

    private void startFertilitySaveTask() {
        if (fertilitySaveTask != null) {
            fertilitySaveTask.cancel();
        }
        if (!config.fertilityEnabled) {
            return;
        }
        long intervalTicks = 20L * Math.max(30, config.fertilitySaveIntervalSeconds);
        this.fertilitySaveTask = getServer().getScheduler().runTaskTimer(this, this::saveFertility, intervalTicks, intervalTicks);
    }

    private void loadFertility() {
        File file = new File(getDataFolder(), "fertility.yml");
        if (!file.exists()) {
            return;
        }
        fertility.loadFrom(YamlConfiguration.loadConfiguration(file));
    }

    private void saveFertility() {
        getDataFolder().mkdirs();
        YamlConfiguration data = new YamlConfiguration();
        fertility.saveTo(data);
        try {
            data.save(new File(getDataFolder(), "fertility.yml"));
        } catch (IOException e) {
            getLogger().warning("Failed to save fertility.yml: " + e.getMessage());
        }
    }
}
