package com.nexuscraft.nexusfarming;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/**
 * Builds (and later recognizes) the rare "Golden Crop" variant: no resource pack involved --
 * just a gold-colored display name, descriptive lore, and an enchant glint borrowed from a
 * hidden Unbreaking I (the enchant itself does nothing on a food item; ItemFlag.HIDE_ENCHANTS
 * keeps the "Unbreaking I" tooltip line from ever showing, so all that's visible is the glint).
 * A PersistentDataContainer tag is what actually marks the item as golden for the consume
 * listener -- the name/lore/glint are purely cosmetic and never inspected by plugin logic.
 */
public final class GoldenCropFactory {

    private final NamespacedKey goldenKey;

    public GoldenCropFactory(Plugin plugin) {
        this.goldenKey = new NamespacedKey(plugin, "golden_crop");
    }

    public ItemStack create(Material baseMaterial, int amount) {
        ItemStack item = new ItemStack(baseMaterial, amount);
        ItemMeta meta = item.getItemMeta();

        // Component-based displayName()/lore(), not the legacy String setDisplayName()/setLore()
        // -- both still exist on real Paper's ItemMeta, but the legacy pair is deprecated in
        // favor of these Adventure equivalents.
        meta.displayName(Component.text("✦ Golden " + prettyName(baseMaterial) + " ✦", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Harvested from an exceptionally", NamedTextColor.GRAY),
                Component.text("fertile patch of soil.", NamedTextColor.GRAY),
                Component.text("Eat for a burst of vigor.", NamedTextColor.YELLOW)
        ));

        // Direct meta.addEnchant, not item.addUnsafeEnchantment -- the latter would create and
        // attach a *second*, separate ItemMeta instance on the item, which item.setItemMeta(meta)
        // below would then immediately overwrite, silently dropping the enchant.
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(goldenKey, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isGolden(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Boolean flag = item.getItemMeta().getPersistentDataContainer().get(goldenKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(flag);
    }

    private static String prettyName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
