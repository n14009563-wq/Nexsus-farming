# NexusFarming changelog

## v0.1.1 -- real-build fixes (found by an actual `mvn clean package` against Paper 1.21.4)

The v0.1.0 sandbox stub modeled `ItemMeta` as a concrete class that directly implemented
`Damageable` (durability get/set right there on it). The real Paper API doesn't work that way:
`ItemMeta` is an interface with no durability methods at all, and `Damageable` is a *separate*
sub-interface (`Damageable extends ItemMeta`) that a caller has to `instanceof`-check/cast to --
`ItemStack#getItemMeta()` only ever hands back the plain `ItemMeta` type. That mismatch compiled
fine against the sandbox stub and then failed for real:

```
GoldenHoeUtil.java:[62,29] cannot find symbol: method getDamage() location: variable meta of type org.bukkit.inventory.meta.ItemMeta
GoldenHoeUtil.java:[69,13] cannot find symbol: method setDamage(int) location: variable meta of type org.bukkit.inventory.meta.ItemMeta
```

Fixed by making `GoldenHoeUtil#damage` do exactly what real plugin code has to: `if (!(meta
instanceof Damageable damageable)) return false;` before touching `getDamage()`/`setDamage()`.
The sandbox stub was corrected to match -- `ItemMeta` is now genuinely an interface, `Damageable`
genuinely extends it rather than standing alone, and a new `ItemMetaImpl` concrete class (mirroring
the same "interface + one concrete stand-in" pattern `Ageable`/`AgeableBlockData` already used for
crop age) is what `ItemStack#getItemMeta()` actually hands back -- so this exact class of bug would
now be caught by the stub compile check too, not just a real build.

Also cleaned up a real (non-fatal) compiler warning from the same build log: `GoldenCropFactory`
was using `ItemMeta#setDisplayName(String)`/`#setLore(List<String>)`, both deprecated on real
Paper in favor of the Adventure `Component`-based `displayName(Component)`/`lore(List<Component>)`.
Switched to those; the stub's `ItemMeta` interface now declares both the legacy and modern pairs,
matching the real API (deprecated, not removed).

No behavior changes from a player's perspective -- same mechanics, same numbers, same commands.
Re-verified: `javac -Xlint:all -Werror` against the corrected stub, 0 errors/warnings; all 57
standalone test-suite checks still pass (two of them needed the same `instanceof Damageable` cast
added, since they were driving `GoldenHoeUtil` through real `ItemStack`/`ItemMeta` calls directly).

## v0.1.0 -- initial release

Brand-new plugin, built from scratch. Golden hoe in hand: harvesting a mature row crop
auto-replants it instantly, with a rolled chance of bonus yield; sneak-harvest reaps a whole
patch at once; off-tempo crops get nudged forward a growth stage instead; worked farmland builds
up a Fertility level that raises the bonus-yield odds over time; a rare Golden Crop variant gives
a real buff when eaten; and a pumpkin-on-hay-bale Scarecrow protects nearby crops from trampling.
Melon/pumpkin and sugar cane/cactus/bamboo get their own matching (but scoped-down) treatment. See
`README.md` for the full feature list and what was deliberately left out.

### Design choices worth recording

- **No global per-tick scans anywhere.** This is a direct, deliberate reaction to the exact class
  of lag bug diagnosed and fixed in NexusDimensions (unthrottled mob spawning across an
  entirely-ocean dimension preset -- see that plugin's own `CHANGES.md` v0.1.1). Concretely here:
  - AoE Reap is hard-capped by both `harvest.aoe.maxRadius` and `harvest.aoe.maxBlocksPerUse`, so
    a single right-click can never process an unbounded number of blocks.
  - Soil Fertility decay is lazy and read-triggered (`FertilityService.levelFor`): a tile's decay
    is only computed the moment that tile is actually read, and the decayed value is written
    straight back so the next read is O(1) again. There's no background task scanning every
    tracked tile on a timer.
  - Scarecrow protection precomputes each anchor's full protected-tile set once, at placement
    time, into a ref-counted reverse index. A trample check is a single `HashMap` lookup
    regardless of radius, instead of iterating every anchor's radius on every single
    `EntityChangeBlockEvent`.
- **`BlockKey`, not `Location`, as the map/set key for Fertility and Scarecrow state.** Real
  Bukkit's `Location` compares world/x/y/z as `double`s and doesn't override `equals()`/
  `hashCode()` in a way that's safe for exact-block identity lookups. `BlockKey` is a plain
  record over `(world, x, y, z)` built specifically for this.
- **Durability damage is Unbreaking-aware via the real per-point formula**, not a flat reduction:
  each point of configured damage independently has a `1/(unbreakingLevel+1)` chance to actually
  count, matching vanilla tool-damage behavior. Verified statistically in the test suite (Unbreaking
  I lands within 2 points of a 50% hit rate over 20,000 trials).
- **Golden Crop identification is entirely PersistentDataContainer-based**, never the display name
  or lore -- so renaming or relearing the item in an anvil can't spoof or strip the buff.
- **Deliberately scoped down in a few places** rather than half-implementing something fragile:
  see the "What this deliberately doesn't do" section of `README.md` for the reasoning on each
  (no forced instant stem regrowth, no bonus yield on column crops, no Fertility on non-farmland
  tiles, no NexusEconomy pricing hook for Golden Crops).

### Verification

No network access in this sandbox to Maven Central or repo.papermc.io, so this couldn't be built
against a real Paper jar directly (same constraint noted throughout NexusDimensions' and
NexusCreativeSurvival's own changelogs). Instead:

- A hand-written stub subset of the Paper/Bukkit API was built under `stubs/`, modeled on the
  exact API shapes already confirmed correct while building the rest of this plugin family
  (including fixing the same `Bukkit.getPluginManager()` "new instance every call" stub bug here
  proactively, first found in NexusCreativeSurvival's stub set).
- All 17 real plugin source classes compile clean against that stub with
  `javac -Xlint:all -Werror` -- zero warnings, zero errors.
- A standalone test suite (`src/test/java/.../NexusFarmingTest.java`, plain `main()`-based, no
  JUnit dependency) exercises every pure-logic class directly: `YieldRoller`'s chance math and
  extra-yield rolling (including a 20,000-trial statistical check against the configured
  chance), `FertilityService`'s level-cap and decay-on-read math, `GoldenHoeUtil`'s
  Unbreaking-aware durability damage (including the break threshold and the "Unbreakable" flag),
  `RowCropType`'s per-crop data, `BlockKey`'s equality/distance/serialization, and
  `FarmingConfig`'s defaults and clamping behavior (driven through a small in-memory fake
  `Plugin`/`FileConfiguration` pair so genuinely different config values -- not just the
  defaults -- exercise the actual clamping logic). All 57 checks pass.
- See `README.md`'s "if anything fails to compile against a real Paper 1.21.x jar" section for
  the specific API surface that's uncertain because it couldn't be checked against a real jar
  (particle/sound/enchantment naming, mainly), same house style as NexusDimensions used.
