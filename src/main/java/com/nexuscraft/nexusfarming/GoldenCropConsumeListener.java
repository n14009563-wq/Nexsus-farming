package com.nexuscraft.nexusfarming;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Eating a Golden Crop grants a real, temporary buff -- Regeneration to actually feel like a
 * "farmer's feast", plus Saturation so it also functions as genuinely great food. Detection is
 * entirely via {@link GoldenCropFactory}'s PersistentDataContainer tag, never the display name or
 * lore, so a player renaming/relearing the item in an anvil can't spoof (or lose) the buff.
 */
public final class GoldenCropConsumeListener implements Listener {

    private final FarmingConfig config;
    private final GoldenCropFactory goldenCropFactory;
    private final EffectsUtil effects;

    public GoldenCropConsumeListener(FarmingConfig config, GoldenCropFactory goldenCropFactory, EffectsUtil effects) {
        this.config = config;
        this.goldenCropFactory = goldenCropFactory;
        this.effects = effects;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!config.goldenCropsEnabled) {
            return;
        }
        ItemStack item = event.getItem();
        if (!goldenCropFactory.isGolden(item)) {
            return;
        }

        Player player = event.getPlayer();
        int durationTicks = Math.max(1, config.goldenCropBuffDurationSeconds) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, config.goldenCropBuffAmplifier));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, durationTicks, config.goldenCropBuffAmplifier));
        effects.playGoldenCropEaten(player.getLocation());
    }
}
