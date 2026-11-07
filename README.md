# Better Combat Arsenal

A small, data-driven NeoForge weapon mod for Minecraft **1.21.1** whose melee weapons use
[Better Combat](https://modrinth.com/mod/better-combat) weapon presets for their attack
animations.

It currently ships six weapons — a one-handed **Sword**, two-handed **Claymore**, **Twin Blade**,
**Double Axe**, **Hengdao** and the one-handed **Rapier** — and is structured so more weapons can be
added without touching the registration code.

## Features

| Item | Id | Better Combat preset family | Base stats |
|---|---|---|---|
| Sword | `bettercombatarsenal:sword` | `bettercombat:sword` (one-handed: horizontal slashes + stab) | Iron tier, 6 attack damage, 1.6 attack speed |
| Claymore | `bettercombatarsenal:claymore` | `bettercombat:claymore` (two-handed: wide slash, thrust, overhead slam) | Iron tier, 8 attack damage, 0.8 attack speed, +0.35 attack range |
| Twin Blade | `bettercombatarsenal:twinblade` | `bettercombat:twin_blade` (two-handed: alternating switch-blade slashes) | Iron tier, 7 attack damage, 1.2 attack speed |
| Double Axe | `bettercombatarsenal:doubleaxe` | `bettercombat:double_axe` (two-handed: wide slash + 360° spin) | Iron tier, 9 attack damage, 0.6 attack speed |
| Hengdao | `bettercombatarsenal:hengdao` | `bettercombat:katana` (two-handed: alternating vertical/horizontal katana slashes) | Iron tier, 5.5 attack damage, 1.0 attack speed |
| Rapier | `bettercombatarsenal:rapier` | `bettercombat:rapier` (one-handed: quick horizontal slash + forward stab) | Iron tier, 3.5 attack damage, 2.2 attack speed |

- Dedicated **Better Combat Arsenal** creative tab, plus entries in the vanilla Combat tab.
- **2D GUI thumbnails**: the supplied 16×16 icons from `resourcepacks/item` are used *only* for the
  inventory/hotbar/creative-menu thumbnail; every in-world context renders the full 3D model.
- **Glowing blade**: every weapon ships a supplied `<weapon>_blade.png` mask (opaque only on the
  blade pixels). The mask is bound to an overlay model (`item/<id>_fx`) that re-uses the authored
  geometry, which is then drawn full-bright on top of the base model — only the blade glows.
  Everything is tunable in `config/bettercombatarsenal/weapon_glow.json`.
- **Glowing slash trail**: each weapon's `weapon_attributes` data declares its own Better Combat
  `trail_appearance` with `glows: true`, so Better Combat's built-in slash trail is self-lit too.
- **Trail sparks**: while a Better Combat weapon swing is running, small full-bright sparks are
  emitted along the blade and fade out over 0.5 s; normal/vanilla swings never emit them. While the
  player moves, a few longer-lived sparks (2–3 s) are left behind where the blade swept, and even
  when standing still a very light ambient trickle keeps the blade faintly glowing. Amount,
  size, lifetime, spread and velocity are configurable.
- **Glow everywhere**: first person, third person, dropped items, item frames and shelf/display
  contexts all show the glowing blade; the first-person overlay is drawn in the hand model's own
  render pass so it stays visible while the weapon is perfectly still.
- **Jump attack**: strict rule — only an attack performed **after an actual jump and at or before the
  jump apex** (vertical velocity still `>= 0`) is a jump attack; attacking after the apex (already
  falling) is a normal attack, and attacking first and jumping afterwards never becomes a jump attack.
  A jump attack plays the claymore's final overhead slam animation instead of the weapon's own attack,
  while the swing/impact **sound stays the weapon's own**. Each jump attack interrupts and resets the
  combo chain (the next ground attack starts at the first combo step). Hits from a jump
  attack knock the target back by a weapon-class dependent amount (sword 0.35, twin blade 0.45,
  hengdao 0.55, claymore 0.85, double axe 1.0). A jump attack (while the attack input is held/pressed)
  starts a client-side jump cooldown equal to the weapon's Better Combat attack cooldown (at least
  10 ticks); only the **first half** of it blocks jumping (client mixin cancels `jumpFromGround` for
  the local player holding an arsenal weapon), so the player can jump again during the second half.
  Just jumping around while holding the weapon never starts a cooldown, and attacks are never gated.
  This is a small optional mixin into Better Combat's attack selection plus a jump-blocking mixin, and
  is only active when Better Combat is installed.
- **Sodium / Iris friendly**: without shaders the overlay uses an additive, full-bright layer; as
  soon as an Iris shader pack is active it automatically switches to the vanilla
  `entity_translucent_emissive` layer (a self-lit render type shader packs already handle). Iris is
  detected through its API reflectively, so Iris is not a required dependency.
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

The `builtin/entity` model intentionally has **no `gui` display entry** (only `thirdperson_*`,
`firstperson_*`, `ground` and `fixed`): the GUI transform is applied by vanilla before the renderer
runs, so a `gui` entry would rotate/scale the supplied 16×16 icon as well.

## Blade glow

Each weapon gets a second, mask-based overlay model (`models/item/<id>_fx.json`): it parents the
same geometry and overrides texture key `1` with the supplied `<weapon>_blade.png` mask. The mask is
transparent everywhere except the blade pixels, so drawing the overlay with full-bright light and a
glow blend only lights up the blade. The overlay is registered as an additional model and drawn by
`client/ArsenalItemRenderer` right after the base model in every non-GUI display context.

Client configuration (`config/bettercombatarsenal/weapon_glow.json`, created on first launch and
re-applied within about a second after editing):

```json
{
  "enabled": true,
  "glow": {
    "enabled": true,
    "alpha": 0.85,
    "additive": true,
    "mode": "auto",
    "tint": "#FFFFFF"
  },
  "trail": {
    "particles": true,
    "particle": "bettercombatarsenal:glow_spark",
    "particlesPerTick": 5,
    "particleLifetimeTicks": 10,
    "particleScale": 0.05,
    "spread": 0.08,
    "speed": 0.015
  },
  "motion": {
    "particles": true,
    "particlesPerTick": 1,
    "intervalTicks": 2,
    "particleLifetimeTicks": 50,
    "particleScale": 0.04,
    "minSpeed": 0.05,
    "spread": 0.04,
    "speed": 0.0
  },
  "idle": {
    "particles": true,
    "intervalTicks": 8,
    "particlesPerEmit": 1,
    "particleLifetimeTicks": 40,
    "particleScale": 0.04,
    "spread": 0.03,
    "speed": 0.0
  }
}
```

| Key | Meaning |
|---|---|
| `enabled` | Master switch for the glow and the trail sparks |
| `glow.enabled` | Glow layer switch |
| `glow.alpha` | Overlay opacity (0–1) |
| `glow.additive` | Additive blend when no shader pack is active (brighter, self-lit look) |
| `glow.mode` | `auto` (default), or force `additive` / `translucent` / `emissive` |
| `glow.tint` | Colour multiplied with the mask (`#RRGGBB`) |
| `trail.particles` | Emit sparks along the blade during **Better Combat swings only** |
| `trail.particle` | Particle id; default is the mod's own small full-bright spark `bettercombatarsenal:glow_spark` |
| `trail.particlesPerTick` | Sparks per client tick during a swing (denser = higher) |
| `trail.particleLifetimeTicks` | Spark lifetime; `10` = 0.5 s |
| `trail.particleScale` | Spark size (`0.05` is small; applies to `glow_spark`) |
| `trail.spread` / `trail.speed` | Random position jitter / velocity per spark |
| `motion.particles` | Leave a few lingering sparks where the blade sweeps **while the player moves** |
| `motion.particlesPerTick` / `motion.intervalTicks` | Amount and cadence (1 spark every 2 ticks by default) |
| `motion.particleLifetimeTicks` | Lingering spark lifetime; `50` = 2.5 s |
| `motion.particleScale` | Lingering spark size |
| `motion.minSpeed` | Minimum horizontal speed (blocks/tick) before sparks appear |
| `motion.spread` / `motion.speed` | Jitter / velocity (`0` = the spark stays where the blade passed) |
| `idle.particles` | Very light ambient trickle at the blade **also while standing still** |
| `idle.intervalTicks` / `idle.particlesPerEmit` | One spark every 8 ticks by default (subtle) |
| `idle.particleLifetimeTicks` | Idle spark lifetime; `40` = 2 s |
| `idle.particleScale` | Idle spark size |
| `idle.spread` / `idle.speed` | Jitter / velocity (`0` = stays at the blade) |

The blade glow and the idle trickle are drawn for **every** display context: first person,
third person (left and right hand), dropped items, item frames and shelf/display mods. In first person, drawing from inside the hand renderer cannot work under Iris: the shader pack owns
blending in that pass, so an additive overlay comes out flat grey or ends up behind the hand model.
The Iris adaptation therefore captures the item pose, modelview and projection while each hand is
drawn and paints the glow **after** the hands (GUI pre phase) with the normal additive, full-bright
layer. Main hand and offhand are tracked separately, so both glow. The deferred layer keeps depth
testing and back-face culling (no polygon offset), so the glow only appears on the visible blade
surface and never shines through the weapon model. No extra model or texture files are needed —
the supplied assets are used unchanged.

### Better Combat slash trail

Better Combat's own slash trail is re-coloured and lit through the weapon data, not through code:
each `data/bettercombatarsenal/weapon_attributes/<id>.json` carries

```json
"trail_appearance": {
  "default_appearance": {
    "primary": { "color_rgba": 4294967193, "glows": true },
    "secondary": { "color_rgba": 2576980326, "glows": true }
  },
  "conditional": {
    "is_enchanted": {
      "primary": { "color_rgba": 1725562777, "glows": true },
      "secondary": { "color_rgba": 2582052684, "glows": true }
    }
  }
}
```

`glows: true` is what makes Better Combat render the slash trail self-lit (its particles use the
glow render path); the colours are Better Combat's default trail colours, so only the glow differs
from the default look. Because this is datapack data, it keeps working without any code hook and
can be tuned per weapon.

The extra sparks are client-side only. There are five full-bright spark styles, all pure white (no tint)
with the same fade-out behaviour but different vanilla sprites:

| Particle id | Sprite | Look |
|---|---|---|
| `bettercombatarsenal:glow_spark` | vanilla `glow` | plain star spark |
| `bettercombatarsenal:glow_glitter` | vanilla `glitter_*` | animated end-rod twinkle, white |
| `bettercombatarsenal:glow_sparkle` | vanilla `spark_*` | animated firework spark, white |
| `bettercombatarsenal:glow_crit` | vanilla `critical_hit` | crisp crit flash, white |
| `bettercombatarsenal:glow_magic` | vanilla `enchanted_hit` | soft glint, white |

Each emitter (`trail`, `motion`, `idle`) has a `variants` list; when it is not empty a random entry
is picked for every spark, so the styles mix. Empty list = fall back to the single `particle` id. The blade position is captured from the
`_fx` overlay model while any arsenal weapon is rendered, so the sparks follow the real blade in
first and third person, and also for dropped items / display contexts. Three emitters exist:

- **Attack sparks**: only while Better Combat reports a weapon swing in progress
  (`MinecraftClient_BetterCombat.isWeaponSwingInProgress()`, looked up reflectively so Better Combat
  stays optional). Because it follows the animation state instead of the attack key, normal/vanilla
  swings never emit sparks and the emission covers the whole Better Combat animation, including its
  latter half. Default: 5 sparks per tick, 0.5 s lifetime.
- **Movement sparks**: while the player is moving, a few longer-lived sparks (default 1 every 2 ticks,
  2.5 s lifetime) stay where the blade swept and fade out naturally.
- **Idle sparks**: a very light trickle (default one spark every 8 ticks, 2 s lifetime) at the blade
  even when nothing is moving — including when the weapon is lying on the ground, in an item frame
  or on a shelf.

Compatibility notes:

- **Sodium**: the overlay is plain quad rendering through the normal `MultiBufferSource`; Sodium does
  not replace item/entity rendering, so nothing special is needed.
- **Iris shader packs**: shader packs replace the blend state of custom render types and re-shade
  translucent geometry in their deferred passes, which can leave a custom additive overlay only
  partially lit. With `mode: "auto"` the renderer therefore switches to the vanilla
  `entity_translucent_emissive` render type (bound to the item atlas) whenever
  `IrisApi.isShaderPackInUse()` reports an active shader pack; that vanilla render type is emissive
  and handled by every shader pack. The Iris API is looked up via reflection, so no Iris dependency
  is declared.

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
| Hengdao | horizontal right slash | `HORIZONTAL_PLANE` | 0.80 | 120° | 0.5 | `bettercombat:two_handed_slash_horizontal_right` / `katana_slash` |
| Hengdao | vertical left slash | `VERTICAL_PLANE` | 1.20 | 120° | 0.5 | `bettercombat:two_handed_slash_vertical_left` / `katana_slash` (pitch 1.05) |
| Hengdao | vertical right slash | `VERTICAL_PLANE` | 1.20 | 120° | 0.5 | `bettercombat:two_handed_slash_vertical_right` / `katana_slash` (pitch 0.95) |
| Hengdao | horizontal left slash | `HORIZONTAL_PLANE` | 0.80 | 120° | 0.5 | `bettercombat:two_handed_slash_horizontal_left` / `katana_slash` |
| Rapier | horizontal slash | `HORIZONTAL_PLANE` | 0.90 | 90° | 0.5 | `bettercombat:one_handed_slash_horizontal_left` / `rapier_slash` |
| Rapier | forward stab | `FORWARD_BOX` | 1.10 | - | 0.5 | `bettercombat:one_handed_stab` / `rapier_stab` (pitch 1.05) |

Sword: one-handed, category `sword`, no range bonus. Claymore: two-handed, category `claymore`,
`bettercombat:pose_two_handed_sword`, +0.35 attack range. Twin blade: two-handed, category
`twin_blade`, `bettercombat:pose_two_handed_katana`, no range bonus; two alternating 150°
switch-blade slashes. Double axe: two-handed, category `double_axe`, no range bonus; 180° slash
plus a 360° spin. Hengdao: two-handed, category `katana`, pose `bettercombat:pose_two_handed_katana`, no
range bonus; alternating vertical/horizontal katana slashes (it uses Better Combat's katana preset but
keeps its own name/id `hengdao`). Rapier: one-handed, category `rapier`, no range bonus; a quick 90°
horizontal slash followed by a forward stab. Tuning any of these values is a pure data-pack change.

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

One small exception to the pure-data rule is the airborne jump attack: Better Combat has no
airborne attack condition, so `compat/JumpAttackHandler` (all Better Combat types are reached
reflectively) plus `mixin/bettercombat/PlayerAttackHelperMixin` replace the selected attack with
the claymore's last attack while the player is not on the ground. `ArsenalMixinPlugin` only applies
that mixin when Better Combat is loaded, so the mod still compiles, loads and runs without it.

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
│   ├── ModParticles.java             # glow_spark particle type used by the trail sparks
│   └── ModCreativeTabs.java          # arsenal tab + vanilla Combat tab entries
├── client/                           # client-only (never loaded on a dedicated server)
│   ├── ArsenalClient.java            # models, client item extensions, geometry loaders, particle provider
│   ├── ArsenalItemRenderer.java      # GUI thumbnail vs in-world 3D renderer + blade glow call
│   ├── PermissiveModelLoader.java    # loads unmodified exports with non-vanilla rotation angles
│   └── fx/
│       ├── WeaponGlow.java           # full-bright blade-mask overlay (additive / emissive for Iris)
│       ├── WeaponTrailFx.java        # Better Combat swing / movement spark emitters
│       ├── GlowSparkParticle.java    # small full-bright spark (0.5 s attack, 2.5 s movement)
│       └── WeaponGlowConfig.java     # config/bettercombatarsenal/weapon_glow.json loading + reload
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
   - `models/item/<id>_fx.json` — blade glow overlay: same parent/loader as `_3d` but texture key
     `1` bound to `bettercombatarsenal:item/<id>_blade` (the supplied mask);
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

All supplied weapon textures are organised into three folders under
`assets/bettercombatarsenal/textures/`:

| Folder | Contents |
|---|---|
| `thumbnail/` | 16×16 GUI icons used only by `item/<id>_thumbnail` |
| `weapon/` | model textures used by `item/<id>_3d` and the particle key of `item/<id>` |
| `blade/` | `<id>_blade.png` glow masks used only by `item/<id>_fx` |

The particle sprite stays in `textures/particle/`, which Minecraft's particle atlas requires.

Minecraft only stitches `textures/item` and `textures/block` into the block/item atlas by default, so the
mod ships `assets/minecraft/atlases/blocks.json` with three `neoforge:namespaced_directory` sources that
stitch `weapon/`, `thumbnail/` and `blade/` from this mod's namespace into the same atlas.

### Source files

All weapon models, textures and item icons were supplied in `C:\DSHhome\resourcepacks` and are
included **byte-for-byte** (no model or texture content was edited):

| Source | Mod destination | Note |
|---|---|---|
| `resourcepacks/block/claymore.json` | `assets/bettercombatarsenal/models/item/claymore_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/sword.json` | `assets/bettercombatarsenal/models/item/sword_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/twinblade.json` | `assets/bettercombatarsenal/geometry/twinblade.json` | unchanged geometry; uses ±15° element rotations, loaded by `permissive_model` |
| `resourcepacks/block/doubleaxe.json` | `assets/bettercombatarsenal/geometry/doubleaxe.json` | unchanged geometry; uses ±25° and Euler x/y/z rotations, loaded by `permissive_model` |
| `resourcepacks/texture/claymore.png` | `assets/bettercombatarsenal/textures/weapon/claymore.png` | 128×128 3D model texture |
| `resourcepacks/texture/sword.png` | `assets/bettercombatarsenal/textures/weapon/sword.png` | 64×64 3D model texture |
| `resourcepacks/texture/twinblade.png` | `assets/bettercombatarsenal/textures/weapon/twinblade.png` | 64×64 3D model texture |
| `resourcepacks/texture/doubleaxe.png` | `assets/bettercombatarsenal/textures/weapon/doubleaxe.png` | 128×128 3D model texture |
| `resourcepacks/block/hengdao.json` | `assets/bettercombatarsenal/models/item/hengdao_geometry.json` | unchanged geometry + display states |
| `resourcepacks/texture/hengdao.png` | `assets/bettercombatarsenal/textures/weapon/hengdao.png` | 3D model texture |
| `resourcepacks/texture/hengdao_blade.png` | `assets/bettercombatarsenal/textures/blade/hengdao_blade.png` | blade glow mask (used only by `item/hengdao_fx`) |
| `resourcepacks/item/hengdao.png` | `assets/bettercombatarsenal/textures/thumbnail/hengdao.png` | 16×16 GUI thumbnail (used by `item/hengdao_thumbnail`) |
| `resourcepacks/block/rapier.json` | `assets/bettercombatarsenal/geometry/rapier.json` | unchanged geometry (read by the `permissive_model` loader) |
| `resourcepacks/texture/rapier.png` | `assets/bettercombatarsenal/textures/weapon/rapier.png` | 3D model texture |
| `resourcepacks/texture/rapier_blade.png` | `assets/bettercombatarsenal/textures/blade/rapier_blade.png` | blade glow mask (used only by `item/rapier_fx`) |
| `resourcepacks/item/rapier.png` | `assets/bettercombatarsenal/textures/thumbnail/rapier.png` | 16×16 GUI thumbnail (used by `item/rapier_thumbnail`) |
| `resourcepacks/item/claymore.png` | `assets/bettercombatarsenal/textures/thumbnail/claymore.png` | 16×16 claymore GUI thumbnail (used only by `item/claymore_thumbnail`) |
| `resourcepacks/item/sword.png` | `assets/bettercombatarsenal/textures/thumbnail/sword.png` | 16×16 sword GUI thumbnail (used only by `item/sword_thumbnail`) |
| `resourcepacks/item/twinblade.png` | `assets/bettercombatarsenal/textures/thumbnail/twinblade.png` | 16×16 twin blade GUI thumbnail (used only by `item/twinblade_thumbnail`) |
| `resourcepacks/item/doubleaxe.png` | `assets/bettercombatarsenal/textures/thumbnail/doubleaxe.png` | 16×16 double axe GUI thumbnail (used only by `item/doubleaxe_thumbnail`) |
| `resourcepacks/texture/sword_blade.png` | `assets/bettercombatarsenal/textures/blade/sword_blade.png` | blade glow mask (used only by `item/sword_fx`) |
| `resourcepacks/texture/claymore_blade.png` | `assets/bettercombatarsenal/textures/blade/claymore_blade.png` | blade glow mask (used only by `item/claymore_fx`) |
| `resourcepacks/texture/twinblade_blade.png` | `assets/bettercombatarsenal/textures/blade/twinblade_blade.png` | blade glow mask (used only by `item/twinblade_fx`) |
| `resourcepacks/texture/doubleaxe_blade.png` | `assets/bettercombatarsenal/textures/blade/doubleaxe_blade.png` | blade glow mask (used only by `item/doubleaxe_fx`) |

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

Model textures live under `textures/weapon/`, GUI icons under `textures/thumbnail/` and glow masks
under `textures/blade/`, because the vanilla
`minecraft:blocks` atlas only stitches those directories. The provided `resourcepacks/item` PNGs are
referenced exclusively by the `*_thumbnail.json` models (GUI only). Every supplied asset keeps its
original bytes; the geometry wrapper, thumbnail, `builtin/entity` display models and the permissive
loader wrapper are the only model files written by this project.

## Verification

Verified on this checkout with Minecraft 1.21.1 + NeoForge 21.1.265:

- `.\gradlew.bat build` → `BUILD SUCCESSFUL`; jar
  `build/libs/bettercombatarsenal-1.0.0.jar` (SHA256 `3795BF79…F336`).
- Updated supplied assets are included byte-identically: `resourcepacks/block/claymore.json` →
  `assets/bettercombatarsenal/models/item/claymore_geometry.json` (SHA256 `DB27C17E…`, latest re-export; display transforms kept in sync in `models/item/claymore.json`),
  `resourcepacks/block/twinblade.json` → `assets/bettercombatarsenal/geometry/twinblade.json`
  (SHA256 `C45F1CCC…`), the re-exported `resourcepacks/texture/twinblade.png` →
  `textures/block/twinblade.png` (`64B9BD06…`), and the four `<weapon>_blade.png` masks →
  `textures/item/<weapon>_blade.png`; no file under `C:\DSHhome\resourcepacks` is written to and no
  supplied model or texture was edited.
- Trail data validated against Better Combat 2.4.0 itself: `WeaponAttributesHelper.decode(...)`
  accepts all four updated `weapon_attributes/*.json`, and after Better Combat's own
  `ConditionalTrailAppearance.merge(...)` the resolved appearance is
  `Part[color_rgba=4294967193, glows=true]` / `Part[color_rgba=2576980326, glows=true]` — i.e. the
  built-in slash trail takes Better Combat's glow render path.
- Client (dev run) with Better Combat 2.4.0 + Cloth Config 15.0.140 + playerAnimator 2.0.4 **and**
  the bundled optimization/shader stack (Sodium 0.8.13, Sodium Extra 0.9.5, Iris 1.8.14-beta.1,
  Complementary Reimagined active, LambDynamicLights, Leawind third person, ETF/EMF, JEI, ...):
  world joins, Better Combat logs `Loaded container: bettercombatarsenal:sword` / `...claymore` /
  `...twinblade` / `...doubleaxe`, our log line `Registered 6 arsenal weapon(s)` is printed, the
  attack hook reports `Better Combat attack hook installed; blade trail particles are active.`, the
  jump-attack mixin reports `Jump-attack mixin applied: true`, and
  no `Missing textures in model bettercombatarsenal:*`, `Failed to load model` or render-type errors
  are emitted for this mod (verified for all six weapons including `Hengdao` and `Rapier`).
- In-game the six weapons render with the supplied 16×16 icons in the GUI only, the full 3D model
  in hand, and the `<weapon>_blade.png` mask lights up the blade (additive layer normally, vanilla
  emissive layer while an Iris shader pack is in use); swings emit the configured glowing particles
  along the blade and Better Combat's slash trail is self-lit.
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
