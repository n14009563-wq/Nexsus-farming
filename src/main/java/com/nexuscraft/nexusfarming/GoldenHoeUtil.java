package com.nexuscraft.nexusfarming;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

/**
 * Small stateless helpers around the item this whole plugin keys off of: a plain vanilla golden
 * hoe. No NBT tag or custom recipe -- any GOLDEN_HOE works, so it stays enchantable, renameable,
 * anvil-repairable, and tradeable exactly like vanilla.
 */
public final class GoldenHoeUtil {

    private GoldenHoeUtil() {
    }

    public static boolean isGoldenHoe(ItemStack item) {
        return item != null && item.getType() == Material.GOLDEN_HOE;
    }

    public static int fortuneLevel(ItemStack item) {
        return item == null ? 0 : item.getEnchantmentLevel(Enchantment.FORTUNE);
    }

    public static int unbreakingLevel(ItemStack item) {
        return item == null ? 0 : item.getEnchantmentLevel(Enchantment.UNBREAKING);
    }

    /**
     * Applies durability damage the same way vanilla tools do: each of the {@code points}
     * damage points independently has only a {@code 1/(unbreakingLevel+1)} chance to actually
     * count, so higher Unbreaking makes the hoe last proportionally longer instead of a flat
     * damage-reduction. An unbreakable item (anvil "Unbreakable" tag) is never damaged.
     *
     * @return true if this call broke the item -- caller is responsible for actually removing
     *         it from the player's hand, since a stub/real ItemStack held locally can't reach
     *         back into the inventory slot that's holding it.
     */
    public static boolean damage(ItemStack item, int points, Random random) {
        if (item == null || points <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta.isUnbreakable()) {
            return false;
        }
        // ItemMeta itself has no notion of durability -- that's the separate Damageable
        // sub-interface (org.bukkit.inventory.meta.Damageable extends ItemMeta). Every real tool's
        // meta implements it, but the plain ItemMeta reference getItemMeta() hands back doesn't
        // expose it without this cast.
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }

        int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        int actualDamage = 0;
        for (int i = 0; i < points; i++) {
            if (unbreaking <= 0 || random.nextInt(unbreaking + 1) == 0) {
                actualDamage++;
            }
        }
        if (actualDamage <= 0) {
            return false;
        }

        int newDamage = damageable.getDamage() + actualDamage;
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability > 0 && newDamage >= maxDurability) {
            item.setAmount(0);
            return true;
        }

        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }
}
