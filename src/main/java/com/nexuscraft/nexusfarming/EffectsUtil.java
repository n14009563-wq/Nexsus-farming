package com.nexuscraft.nexusfarming;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

/**
 * All player-facing feedback funnels through here, using only vanilla sounds/particles -- no
 * resource pack, ever. Every method is a no-op if {@code enabled} is false, so call sites don't
 * need their own config checks.
 */
public final class EffectsUtil {

    private final boolean enabled;

    public EffectsUtil(boolean enabled) {
        this.enabled = enabled;
    }

    public void playHarvest(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.BLOCK_CROP_BREAK, 0.8f, 1.0f);
    }

    public void playReplant(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.ITEM_CROP_PLANT, 0.6f, 1.2f);
    }

    public void playBonusYield(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.CRIT, location, 8, 0.3, 0.3, 0.3);
        world.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
    }

    public void playGoldenCropFound(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, 12, 0.4, 0.4, 0.4);
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
    }

    public void playGoldenCropEaten(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, 6, 0.3, 0.5, 0.3);
    }

    public void playScarecrowPlaced(Location location) {
        if (!enabled || location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);
    }
}
