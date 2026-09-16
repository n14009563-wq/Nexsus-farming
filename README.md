# NexusFarming

A massive, vanilla-only farming overhaul built around one item: a plain golden hoe. No custom
recipe, no NBT tag, no resource pack -- any golden hoe already works, and it stays enchantable,
renameable, and anvil-repairable like normal. Hold it, right-click a crop, and this plugin takes
over.

## What it does

**Auto-replant.** Right-click a *mature* row crop (wheat, carrots, potatoes, beetroot, nether
wart) with the golden hoe and it harvests AND replants itself instantly, on the exact same block.
No re-tilling, no re-seeding -- it just starts growing again.

**Bonus yield.** Every harvest rolls a chance at extra crops on top of the normal drop. Base
chance and the extra-crop range are both configurable; Fortune on the hoe raises the chance
further, and so does that tile's Fertility (below).

**Sneak-harvest (AoE Reap).** Sneak + right-click a mature crop and it reaps every mature crop of
that same type in a radius around it -- a whole patch in one click. Hard-capped both in radius and
in total blocks processed per use, so a big field can't turn one click into a lag spike.

**Growth Nudge.** Right-click a crop that ISN'T mature yet and it nudges its growth forward a
stage instead -- a small, durability-costed "hurry up", not a free instant-grow. Cooldown is
per-player, not per-block, so it can't be used to insta-max an entire field.

**Soil Fertility.** Farmland you keep actively harvesting builds up a Fertility level over time
(per exact tile), adding to that tile's bonus-yield chance. Leave it alone and it slowly decays
back down. Fertility data is saved to `fertility.yml` in the plugin's data folder and reloaded on
startup.

**Golden Crops.** A small chance on every harvest produces a shimmering Golden Crop instead of a
plain one -- no resource pack, just a gold-colored name, descriptive lore, and an enchant glint
(borrowed from a hidden Unbreaking I whose tooltip line is hidden, so only the glint shows). Eat
one for a real, temporary Regeneration + Saturation buff.

**Scarecrow.** Place a Carved Pumpkin (or Jack o'Lantern) on top of a Hay Bale in a field and it
becomes a Scarecrow: every crop within its radius is protected from being trampled back into dirt,
by players, villagers, or mobs alike. Break either block to remove it.

**Stem fruit and column crops, covered too.** Melon and pumpkin blocks get the same bonus-yield
and Golden Crop treatment when broken with the golden hoe. Sugar cane, cactus, and bamboo get an
instant "reap the column" right-click: everything above the bottom-most block is collected, the
bottom block is left standing so the column keeps growing, exactly like a manual version of an
automatic cane farm.

## Config

See `config.yml` for every number above -- it's heavily commented. `/nexusfarming reload` picks up
changes without a restart.

## Commands & permissions

- `/nexusfarming status` (alias `/nfarm`, `/farming`) -- a quick summary of your server's current
  farming numbers. Permission: `nexusfarming.use` (default: everyone).
- `/nexusfarming reload` -- reloads `config.yml`. Permission: `nexusfarming.admin` (default: op).
- `nexusfarming.use` also gates the golden hoe's powers themselves, in case you want to restrict
  them to a rank.

## What this deliberately doesn't do

- **No instant stem regrowth on melon/pumpkin.** Reaching into a neighboring stem block's own
  growth state from the fruit-harvest listener risks desyncing it (double-attaching, skipped
  growth ticks) for a gimmick that doesn't add much over vanilla's own automatic regrow. Skipped
  on purpose.
- **No bonus yield on sugar cane/cactus/bamboo.** They always drop 1-for-1 in vanilla with no
  Fortune interaction, so a bonus-yield roll there wouldn't be modeling anything real.
- **No Fertility on melon/pumpkin/cane/cactus/bamboo tiles.** Fertility is framed as "worked
  soil" -- these grow on grass/dirt/sand, not farmland, so it doesn't fit.
- **No NexusEconomy integration for Golden Crops.** A Golden Crop and its plain counterpart share
  the same base `Material` (only ItemMeta differs), and Material-keyed pricing can't tell them
  apart -- so no special sell price, on purpose, rather than a half-working one.

## If anything fails to compile against a real Paper 1.21.x jar

This was written and compile-verified against a hand-written stub of the Paper API (this sandbox
has no network access to Maven Central / repo.papermc.io), modeled closely on the exact API shape
already confirmed correct while building NexusDimensions and NexusCreativeSurvival in this same
plugin family. Everything below is flagged because it wasn't checked against a real jar:

- `Particle.HAPPY_VILLAGER` -- Paper reworked several Particle names around 1.20.5/1.21 (the old
  `VILLAGER_HAPPY` became `HAPPY_VILLAGER`); this uses the post-rework name as best recalled.
- `Sound.ITEM_CROP_PLANT`, `Sound.BLOCK_CROP_BREAK`, `Sound.ENTITY_VILLAGER_YES`,
  `Sound.ENTITY_PLAYER_LEVELUP`, `Sound.ENTITY_EXPERIENCE_ORB_PICKUP` -- all used as their current
  best-recalled names, not verified against a real jar.
- `Enchantment.FORTUNE` / `Enchantment.UNBREAKING` -- Paper 1.20.5+ made `Enchantment` a
  Registry-backed interface (the same kind of shift `Attribute` went through, see
  NexusDimensions' `CHANGES.md` v0.1.1). The constant names themselves weren't renamed, but how
  you reach them (`Enchantment.FORTUNE` as a static field vs. a `Registry` lookup) may need
  adjusting for the exact Paper version you build against.
- `ItemMeta#addEnchant(Enchantment, int, boolean)` and `ItemStack#addUnsafeEnchantment` -- used
  here to apply a purely cosmetic glint via a hidden Unbreaking I; the exact bypass semantics
  (level cap vs. item-type applicability) can differ slightly by API version.
- `PlayerInteractEvent` firing once per hand -- this plugin guards with
  `event.getHand() == EquipmentSlot.HAND` to avoid double-processing a single right-click; this is
  real, documented Bukkit/Paper behavior, but worth double-checking against your exact version if
  a harvest ever seems to fire twice (or damage the hoe twice) per click.
- Explicitly calling `player.getInventory().setItemInMainHand(null)` when a hoe breaks, rather
  than relying on the event's item reference auto-syncing back to the inventory slot -- the safer,
  more explicit choice, but the auto-sync guarantee itself wasn't verified against a specific
  Paper version.

None of the above affects the plugin's own logic (yield math, Fertility decay, cooldowns,
Scarecrow protection) -- see `CHANGES.md` for how the pure-logic pieces were verified instead.
