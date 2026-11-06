# Better Combat Arsenal

A small, data-driven NeoForge weapon mod for Minecraft **1.21.1** whose melee weapons use
[Better Combat](https://modrinth.com/mod/better-combat) weapon presets for their attack
animations.

It currently ships two weapons — a one-handed **Sword** and a two-handed **Claymore** — and is
structured so more weapons can be added without touching the registration code.

## Features

| Item | Id | Better Combat preset family | Base stats |
|---|---|---|---|
| Sword | `bettercombatarsenal:sword` | `bettercombat:sword` (one-handed: horizontal slashes + stab) | Iron tier, 6 attack damage, 1.6 attack speed |
| Claymore | `bettercombatarsenal:claymore` | `bettercombat:claymore` (two-handed: wide slash, thrust, overhead slam) | Iron tier, 8 attack damage, 0.8 attack speed, +0.35 attack range |
| Twin Blade | `bettercombatarsenal:twinblade` | `bettercombat:twin_blade` (two-handed: alternating switch-blade slashes) | Iron tier, 7 attack damage, 1.2 attack speed |
| Double Axe | `bettercombatarsenal:doubleaxe` | `bettercombat:double_axe` (two-handed: wide slash + 360° spin) | Iron tier, 9 attack damage, 0.6 attack speed |

- Dedicated **Better Combat Arsenal** creative tab, plus entries in the vanilla Combat tab.
- **2D GUI thumbnails**: the supplied 16×16 icons from `resourcepacks/item` are used *only* for the
  inventory/hotbar/creative-menu thumbnail; every in-world context renders the full 3D model.
- Sword, claymore and twin blade are in the `minecraft:swords` item tag; the double axe is in
  `minecraft:axes`. The matching enchantment tags (Sharpness, Fire Aspect where applicable,
  durability, ...) apply.
- Survival crafting recipes using iron ingots and sticks.
- English (`en_us`) and Simplified Chinese (`zh_cn`) translations.
- Better Combat is **optional** at runtime: without it the weapons keep vanilla combat behaviour.
  With Better Combat 2.x installed, the preset attack animations, hitboxes and two-handed
  behaviour are applied automatically.

## Rendering: GUI thumbnail vs 3D model

Minecraft's item-model format has no per-display-context model selection, so a small client-side
renderer (`client/ArsenalItemRenderer`) switches between two baked variants:

| Context | Model used |
|---|---|
| GUI / inventory / hotbar / creative menu / recipe viewers | `item/<id>_thumbnail` — the supplied 16×16 icon from `resourcepacks/item`, used **only** here |
| First person, third person, ground, item frames, ... | `item/<id>_3d` — the full Blockbench export (unchanged geometry) |

The visible item model (`models/item/<id>.json`) is a `builtin/entity` model carrying the exported
display transforms, so vanilla applies the hand/ground transform before the renderer draws. The two
additional models are registered generically for every catalogue weapon by `client/ArsenalClient`,
which means future weapons need no Java changes.

## Weapon attributes

Every weapon owns an explicit Better Combat weapon-attribute definition under
`data/bettercombatarsenal/weapon_attributes/<item id>.json`. The numbers below are the per-swing
`damage_multiplier` values (multiplied with the item's base attack damage); every swing references
a Better Combat preset player animation.

| Weapon | Swing | Hitbox | Damage × | Angle | Upswing | Animation / sound |
|---|---|---|---|---|---|---|
| Sword | horizontal right slash | `HORIZONTAL_PLANE` | 1.00 | 120° | 0.5 | `bettercombat:one_handed_slash_horizontal_right` / `sword_slash` |
| Sword | horizontal left slash | `HORIZONTAL_PLANE` | 1.00 | 120° | 0.5 | `bettercombat:one_handed_slash_horizontal_left` / `sword_slash` (pitch 1.05) |
| Sword | thrust | `FORWARD_BOX` | 1.15 | 0° | 0.4 | `bettercombat:one_handed_stab` / `sword_slash` (pitch 1.2) |
| Claymore | wide horizontal slash | `HORIZONTAL_PLANE` | 0.80 | 150° | 0.5 | `bettercombat:two_handed_slash_horizontal_right` / `claymore_swing` |
| Claymore | thrust | `FORWARD_BOX` | 1.00 | 0° | 0.5 | `bettercombat:two_handed_stab_left` / `claymore_stab` |
| Claymore | overhead slam | `VERTICAL_PLANE` | 1.30 | 150° | 0.5 | `bettercombat:two_handed_slam` / `claymore_slam` |
| Twin Blade | switch-blade slash right | `HORIZONTAL_PLANE` | 1.00 | 150° | 0.5 | `bettercombat:one_handed_slash_switch_blade_right` / `sword_slash` |
| Twin Blade | switch-blade slash left | `HORIZONTAL_PLANE` | 1.00 | 150° | 0.5 | `bettercombat:one_handed_slash_switch_blade_left` / `sword_slash` |
| Double Axe | wide horizontal slash | `HORIZONTAL_PLANE` | 1.00 | 180° | 0.5 | `bettercombat:two_handed_slash_horizontal_right` / `double_axe_swing` |
| Double Axe | 360° spin | `HORIZONTAL_PLANE` | 1.00 | 360° | 0.5 | `bettercombat:two_handed_spin` / `double_axe_swing` (pitch 1.1) |

Sword: one-handed, category `sword`, no range bonus. Claymore: two-handed, category `claymore`,
`bettercombat:pose_two_handed_sword`, +0.35 attack range. Twin blade: two-handed, category
`twin_blade`, `bettercombat:pose_two_handed_katana`, no range bonus; two alternating 150°
switch-blade slashes. Double axe: two-handed, category `double_axe`, no range bonus; 180° slash
plus a 360° spin. Tuning any of these values is a pure data-pack change.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265+ (21.1.x) |
| Better Combat (optional, recommended) | 2.0.0+ for NeoForge 1.21.1 |
| Cloth Config + playerAnimator (Better Combat dependencies) | as required by Better Combat |

## Build

```powershell
.\gradlew.bat build
# jar: build\libs\bettercombatarsenal-1.0.0.jar
```

Drop the jar into `mods/`. For the full experience also install Better Combat (and its
dependencies Cloth Config and playerAnimator) for Minecraft 1.21.1 NeoForge.

> **IDE run buttons after a `clean`:** `.\gradlew.bat clean` deletes `build/moddev/`, including the
> `*RunVmArgs.txt` / `*RunProgramArgs.txt` files that the IntelliJ **Client/Server/Data/
> GameTestServer** run configurations point at. If a run then fails with
> `could not open ...\build\moddev\clientRunVmArgs.txt`, regenerate the launch files with
>
> ```powershell
> .\gradlew.bat prepareClientRun prepareServerRun prepareDataRun prepareGameTestServerRun
> ```
>
> (or just run `.\gradlew.bat runClient` once) before launching from the IDE.

## How the Better Combat link works

The mod has **no compile-time or runtime code dependency** on Better Combat. The integration is
pure datapack data: each weapon gets its own fully written weapon-attribute definition at

```
data/bettercombatarsenal/weapon_attributes/<item id>.json
```

The definition contains the values that make the weapon feel like itself — `range_bonus`,
`two_handed`, `pose`, `category`, and the per-swing `attacks` array (hitbox shape, damage
multiplier, angle, upswing, swing sound). Every `animation` entry references one of Better
Combat's built-in preset player animations, e.g.:

```json
{
  "attributes": {
    "range_bonus": 0.35,
    "two_handed": true,
    "pose": "bettercombat:pose_two_handed_sword",
    "category": "claymore",
    "attacks": [
      {
        "hitbox": "HORIZONTAL_PLANE",
        "damage_multiplier": 0.8,
        "angle": 150.0,
        "upswing": 0.5,
        "animation": "bettercombat:two_handed_slash_horizontal_right",
        "swing_sound": { "id": "bettercombat:claymore_swing" }
      }
    ]
  }
}
```

Better Combat scans `data/<namespace>/weapon_attributes/*.json` from every datapack/mod, keys each
file by `<namespace>:<path>` and, when that id matches a registered item, resolves the definition
into the runtime weapon attributes (hitboxes, attack range, two-handed rules, animation playback,
swing sounds, ...).

Because the file path doubles as the item id, **the file name must always match the item id**
(`weapon_attributes/claymore.json` for `bettercombatarsenal:claymore`).

The optional dependency is declared in `META-INF/neoforge.mods.toml` with `type="optional"` and
`ordering="AFTER"`, so load order is correct when Better Combat is present and nothing breaks
when it is absent. At startup the mod logs each weapon's preset mapping; a warning is printed when
Better Combat is missing.

## Architecture / adding a weapon

```
src/main/java/com/zszq/bettercombatarsenal/
├── BetterCombatArsenal.java          # @Mod entry point: registries, creative tabs, startup log
├── weapon/
│   ├── WeaponDefinition.java         # immutable weapon data + fluent builder + optional custom item factory
│   ├── WeaponCatalog.java            # ★ THE weapon list — add new weapons here
│   └── BetterCombatPresets.java      # opaque Better Combat preset ids (no Better Combat classes)
├── registry/
│   ├── ModItems.java                 # loops over WeaponCatalog, registers every item
│   └── ModCreativeTabs.java          # arsenal tab + vanilla Combat tab entries
├── client/                           # client-only (never loaded on a dedicated server)
│   ├── ArsenalClient.java            # registers additional models + client item extensions + geometry loaders
│   ├── ArsenalItemRenderer.java      # GUI thumbnail vs in-world 3D renderer
│   └── PermissiveModelLoader.java    # loads unmodified exports with non-vanilla rotation angles
└── compat/
    └── BetterCombatIntegration.java  # optional presence check + startup logging only
```

`WeaponDefinition` is deliberately decoupled:

- it does not know about models, translations, recipes or Better Combat;
- the Better Combat preset is a plain `ResourceLocation` naming the preset family/base preset the
  weapon is modelled on; it is used for logging/documentation, while the concrete weapon attributes
  are written out per weapon in the `weapon_attributes` JSON;
- a custom `ItemFactory` hook allows future non-sword weapon classes to reuse the same catalogue
  and registration pipeline.

### Adding a weapon — checklist

1. **Catalogue**: append one entry to `WeaponCatalog.WEAPONS`, e.g.

   ```java
   WeaponDefinition.builder("katana")
           .tier(Tiers.IRON)
           .attackDamage(3.0F)
           .attackSpeed(-2.0F)
           .betterCombatPreset(BetterCombatPresets.of("katana"))
           .build()
   ```

2. **Models** (all files are our own except the byte-identical geometry):
   - `models/item/<id>_geometry.json` — the Blockbench export copied **unchanged** (normal case);
   - `models/item/<id>_3d.json` — thin wrapper that parents to the geometry and fixes the texture
     namespace, e.g. `{"parent":"bettercombatarsenal:item/<id>_geometry","textures":{"1":"bettercombatarsenal:block/<texture>"}}`;
   - **exceptional rotations**: 1.21.1's vanilla parser only accepts element angles
     `-45/-22.5/0/22.5/45`, and `ModelManager` eagerly parses every file under `models/`. If an
     export uses other angles (the twin blade uses ±15°, the double axe uses ±25° plus Euler
     `x/y/z` rotations), put it unchanged under `assets/bettercombatarsenal/geometry/<id>.json`
     instead and use the permissive loader:

     ```json
     // models/item/twinblade_3d.json (same pattern for doubleaxe_3d.json)
     {
       "loader": "bettercombatarsenal:permissive_model",
       "source": "bettercombatarsenal:geometry/twinblade",
       "textures": { "particle": "bettercombatarsenal:block/twinblade",
                     "1": "bettercombatarsenal:block/twinblade" }
     }
     ```
   - `models/item/<id>_thumbnail.json` — 2D GUI icon, e.g.
     `{"parent":"minecraft:item/generated","textures":{"layer0":"bettercombatarsenal:item/<icon>"}}`;
   - `models/item/<id>.json` — `builtin/entity` model with the display transforms copied from the
     export (first/third person, ground, fixed) for the custom renderer;
   - the thumbnail icon may be the only user of a supplied `resourcepacks/item` PNG.
3. **Better Combat attributes**: create `data/bettercombatarsenal/weapon_attributes/<id>.json` with
   a full, explicit `attributes` block — `range_bonus`, `two_handed`, `pose`, `category` and the
   `attacks` array. Base the numbers on the matching Better Combat preset (`sword`, `claymore`,
   `twin_blade`, `double_axe`, `katana`, `dagger`, `axe`, `mace`, `spear`, `halberd`, `scythe`, `battlestaff`, ...)
   and keep its `bettercombat:*` preset `animation` ids so the swings still play Better Combat
   animations; tune `damage_multiplier`, `angle`, `upswing` and `swing_sound` per weapon.
4. **Translation**: `assets/bettercombatarsenal/lang/en_us.json` (and `zh_cn.json`) with
   `item.bettercombatarsenal.<id>` and, if needed, a creative tab icon update.
5. **Tags/recipes**: add the item to `data/minecraft/tags/item/swords.json` (swords) or
   `data/minecraft/tags/item/axes.json` (axes; create it if absent) and add a recipe under
   `data/bettercombatarsenal/recipe/`.

`ModItems`, the creative tab and the startup diagnostics pick the weapon up automatically.

## Asset provenance

All weapon models, textures and item icons were supplied in `C:\DSHhome\resourcepacks` and are
included **byte-for-byte** (no model or texture content was edited):

| Source | Mod destination | Note |
|---|---|---|
| `resourcepacks/block/claymore.json` | `assets/bettercombatarsenal/models/item/claymore_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/sword.json` | `assets/bettercombatarsenal/models/item/sword_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/twinblade.json` | `assets/bettercombatarsenal/geometry/twinblade.json` | unchanged geometry; uses ±15° element rotations, loaded by `permissive_model` |
| `resourcepacks/block/doubleaxe.json` | `assets/bettercombatarsenal/geometry/doubleaxe.json` | unchanged geometry; uses ±25° and Euler x/y/z rotations, loaded by `permissive_model` |
| `resourcepacks/texture/claymore.png` | `assets/bettercombatarsenal/textures/block/claymore.png` | 128×128 3D model texture |
| `resourcepacks/texture/sword.png` | `assets/bettercombatarsenal/textures/item/sword.png` | 64×64 3D model texture |
| `resourcepacks/texture/twinblade.png` | `assets/bettercombatarsenal/textures/block/twinblade.png` | 64×64 3D model texture |
| `resourcepacks/texture/doubleaxe.png` | `assets/bettercombatarsenal/textures/block/doubleaxe.png` | 128×128 3D model texture |
| `resourcepacks/item/claymore.png` | `assets/bettercombatarsenal/textures/item/claymore.png` | 16×16 claymore GUI thumbnail (used only by `item/claymore_thumbnail`) |
| `resourcepacks/item/model.png` | `assets/bettercombatarsenal/textures/item/model.png` | 16×16 sword GUI thumbnail (used only by `item/sword_thumbnail`) |
| `resourcepacks/item/twinblade.png` | `assets/bettercombatarsenal/textures/item/twinblade.png` | 16×16 twin blade GUI thumbnail (used only by `item/twinblade_thumbnail`) |
| `resourcepacks/item/doubleaxe.png` | `assets/bettercombatarsenal/textures/item/doubleaxe.png` | 16×16 double axe GUI thumbnail (used only by `item/doubleaxe_thumbnail`) |

The supplied Blockbench exports reference their textures with un-namespaced paths
(`"1": "block/claymore"` etc.), which Minecraft resolves in the `minecraft:` namespace. To keep the
provided files untouched, the geometry files are never edited and are wrapped for baking by
`models/item/<id>_3d.json`, which only overrides texture key `1` with a correctly namespaced,
atlas-stitched path:

```json
// models/item/claymore_3d.json (3D model used in hand)
{
  "parent": "bettercombatarsenal:item/claymore_geometry",
  "textures": { "1": "bettercombatarsenal:block/claymore" }
}

// models/item/sword_thumbnail.json (2D GUI thumbnail, supplied icon)
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "bettercombatarsenal:item/model" }
}
```

Special cases: the twin-blade export uses ±15° element rotations and the double-axe export uses
±25° plus Euler x/y/z rotations. Minecraft 1.21.1's vanilla model parser rejects all of these, and
`ModelManager` eagerly parses every file under `models/`. The unmodified exports are therefore
shipped under `geometry/<id>.json` and read by `client/PermissiveModelLoader`, so they render
exactly as authored without editing the assets. Euler x/y/z rotations are interpreted in
Blockbench's `java_block` order (`ZYX`, i.e. {@code qz * qy * qx}); this matters for the double-axe
blade symmetry:

```json
// models/item/twinblade_3d.json (same pattern for doubleaxe_3d.json)
{
  "loader": "bettercombatarsenal:permissive_model",
  "source": "bettercombatarsenal:geometry/twinblade",
  "textures": { "particle": "bettercombatarsenal:block/twinblade",
                "1": "bettercombatarsenal:block/twinblade" }
}
```

Both model textures live under `textures/block/` or `textures/item/` because the vanilla
`minecraft:blocks` atlas only stitches those directories. The provided `resourcepacks/item` PNGs are
referenced exclusively by the `*_thumbnail.json` models (GUI only). Every supplied asset keeps its
original bytes; the geometry wrapper, thumbnail, `builtin/entity` display models and the permissive
loader wrapper are the only model files written by this project.

## Verification

Verified on this checkout with Minecraft 1.21.1 + NeoForge 21.1.265:

- `.\gradlew.bat clean build` → `BUILD SUCCESSFUL`; jar
  `build/libs/bettercombatarsenal-1.0.0.jar` (SHA256 `F9BDE453…CE93`).
- All four supplied geometry files in the jar are byte-identical to the current provided
  `resourcepacks/block/claymore.json` / `sword.json` / `twinblade.json` / `doubleaxe.json` (SHA256
  `E738BBB3…9259` / `45DA30BC…BAE5` / `50E72887…F03E` / `DC2E03C3…856`); no file under
  `C:\DSHhome\resourcepacks` is written to and no supplied model was edited.
- Client resource reload with the updated double-axe asset: no `Failed to load model`,
  `Invalid rotation`, `Missing axis` or `Missing textures` lines for
  `bettercombatarsenal:*`; the supplied 16×16 icons are used only by the `*_thumbnail` models and
  the 3D weapons render in hand.
- Dedicated server with Better Combat 2.4.0 (+ Cloth Config 15.0.140, playerAnimator 2.0.4):
  Better Combat logged `Loaded container: bettercombatarsenal:sword` / `...claymore` /
  `...twinblade` / `...doubleaxe`, encoded its weapon registry with no parse/resolve errors. Its
  decoded registry contains all four explicitly defined weapons, e.g. claymore
  `category=claymore`, `two_handed=true`, `range_bonus=0.35`, attacks `0.8 / 1.0 / 1.3`; sword
  `category=sword`, `range_bonus=0.0`, attacks `1.0 / 1.0 / 1.15`; twinblade
  `category=twin_blade`, `two_handed=true`, `pose=bettercombat:pose_two_handed_katana`,
  attacks `switch_blade_right / switch_blade_left` at 150°; doubleaxe `category=double_axe`,
  `two_handed=true`, attacks `180° slash / 360° spin`.

## License

The project currently uses the template's `All Rights Reserved` license; adjust `mod_license` in
`gradle.properties` if you intend to publish under another license.
