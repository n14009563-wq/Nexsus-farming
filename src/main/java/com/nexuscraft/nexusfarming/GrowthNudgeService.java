package com.nexuscraft.nexusfarming;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Right-clicking a row crop that ISN'T mature yet with a golden hoe nudges it forward a growth
 * stage instead of harvesting it -- a small, durability-costed "hurry up", not an instant free
 * grow. A per-player cooldown (not per-block) is what actually keeps this in check: a per-block
 * cooldown would just mean spamming across a whole field, while a per-player one caps how much
 * total growth one player can force regardless of field size.
 */
public final class GrowthNudgeService {

    private final FarmingConfig config;
    private final EffectsUtil effects;
    private final Random random;
    private final Map<UUID, Long> nextAllowedMillis = new HashMap<>();

    public GrowthNudgeService(FarmingConfig config, EffectsUtil effects, Random random) {
        this.config = config;
        this.effects = effects;
        this.random = random;
    }

    /**
     * @return true if this block was an immature row crop and got nudged; false if it wasn't a
     *         row crop, was already mature, or the player is still on cooldown.
     */
    public boolean nudge(Player player, Block block, ItemStack hoe) {
        if (!config.growthNudgeEnabled) {
            return false;
        }
        RowCropType type = RowCropType.fromBlockMaterial(block.getType());
        if (type == null) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return false;
        }
        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return false;
        }

        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long allowedAt = nextAllowedMillis.get(playerId);
        if (allowedAt != null && now < allowedAt) {
            return false;
        }

        int newAge = Math.min(ageable.getMaximumAge(), ageable.getAge() + config.growthNudgeStages);
        ageable.setAge(newAge);
        block.setBlockData(data);

        nextAllowedMillis.put(playerId, now + config.growthNudgeCooldownSeconds * 1000L);

        if (config.durabilityEnabled && GoldenHoeUtil.damage(hoe, config.growthNudgeDurabilityCost, random)) {
            player.getInventory().setItemInMainHand(null);
        }

        effects.playReplant(block.getLocation());
        return true;
    }

    public void clearCooldown(UUID playerId) {
        nextAllowedMillis.remove(playerId);
    }
}
