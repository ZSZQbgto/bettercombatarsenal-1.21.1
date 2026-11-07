# 更好的战斗：霓虹装备

[中文](#更好的战斗霓虹装备) | [English](#english)

一套面向高等工业军械的锻造方案：武器谱系、攻击序列、刃光与锻刃工序全部集中在少量数据与通用代码中，扩展新型号无需改动注册逻辑。Better Combat 为**必需前置**（2.4.0+，并需其依赖 playeranimator、cloth_config）：武器的攻击动作、命中框与双手规则全部由 Better Combat 预设驱动。

## 特性

| 武器 | 物品 id | Better Combat 预设 | 基础属性 |
|---|---|---|---|
| 长剑 | `bettercombat_neonarsenal:sword` | `bettercombat:sword`（单手：横斩 + 突刺） | 铁级，8.75 攻击伤害，1.6 攻击速度（基础 DPS 14） |
| 阔剑 | `bettercombat_neonarsenal:claymore` | `bettercombat:claymore`（双手：横扫、突刺、下劈） | 铁级，20 攻击伤害，0.8 攻击速度，+0.35 攻击距离（基础 DPS 16） |
| 双刃剑 | `bettercombat_neonarsenal:twinblade` | `bettercombat:twin_blade`（双手：交替双刃斩） | 铁级，10 攻击伤害，1.2 攻击速度（基础 DPS 12） |
| 双刃斧 | `bettercombat_neonarsenal:doubleaxe` | `bettercombat:double_axe`（双手：横扫 + 360° 旋转斩） | 铁级，26.67 攻击伤害，0.6 攻击速度（基础 DPS 16） |
| 横刀 | `bettercombat_neonarsenal:hengdao` | `bettercombat:katana`（双手：交替垂直/水平拔刀斩） | 铁级，14 攻击伤害，1.0 攻击速度（基础 DPS 14） |
| 刺剑 | `bettercombat_neonarsenal:rapier` | `bettercombat:rapier`（单手：快速横斩 + 前刺） | 铁级，6.36 攻击伤害，2.2 攻击速度（基础 DPS 14） |

- 独立的「更好的战斗：霓虹装备」创造模式标签页（仅此栏；武器不可堆叠，一格只能放一把）。
- **2D GUI 缩略图**：`resourcepacks/item` 提供的 16×16 图标只用于背包/快捷栏/创造栏，世界内始终渲染完整 3D 模型。
- **刀刃发光**：每把武器提供 `<weapon>_blade.png` 遮罩，绑定到复用原几何的叠加模型（`item/<id>_fx`），以全亮方式仅照亮刀刃像素。
- **刀光发光**：每把武器的 `weapon_attributes` 数据里声明 `trail_appearance` 且 `glows: true`。
- **火花粒子**：BC 挥砍期间沿刀刃生成全亮火花（0.5 s），移动时留下 2–3 s 长寿命火花，站立时也有极轻的环境火花；普通原版挥动不产生粒子。共 5 种粒子样式可混合。
- **锻刃混合色**：单层锻刃显示该层纯色；两层及以上时刀刃发光、粒子、Better Combat 刀光统一使用各层颜色的**混合色**（HSV 色相插值，见规范 3.2），不再使用渐变。
- **锻刃粒子**：粒子颜色使用武器装备的**混合色**（单层即该层纯色）。
- **全场景发光**：第一/第三人称、掉落物、物品展示框、置物架等均发光；第一人称在 Iris 下通过“手部之后补画”的特殊适配保持常亮。
- **Sodium / Iris 友好**：无光影用加色全亮层；有光影自动切换自发光层，Iris 通过 API 反射检测。
- **跳劈**：严格规则——只有“先起跳、在最高点及之前攻击”才算跳劈；跳劈播放阔剑最后一段下劈动作、使用武器自身音效、重置连招、跳砸造成击退 V 效果（强度 2.5）。跳劈后进入武器攻击冷却（≥10 tick），冷却**前半段**禁止起跳。
- **Tooltip**：悬浮显示缩略信息（刀刃锐度、`锻刃效果: 层名`、Shift 提示）；每增加一层锻刃，物品**稀有度 +1 阶**（名字颜色随之提升：白→黄→青→紫）；**按住 Shift** 显示 `— 综合性能 —`（伤害/攻速/移速/跳跃高度/跃斩增伤/锐度伤害系数）与 `— 锻刃效果 —`（各层效果说明，**不含粒子效果相关内容**）；中英文均已本地化。
- **锋利度**：武器无耐久、永不损坏；锋利度 0–3000，显示在 tooltip；满值 100% 伤害，每损失 20% 伤害 −5%（0 时 75%）。每次命中/破坏方块 −1。铁砧中用蜜脾修复（每蜜脾 +1000，**不消耗经验**）。
- **锻刃**：铁砧中武器 + 特殊材料锻刃，消耗经验，最多 3 层，同层不可重复，刀光/刀刃光效/粒子颜色取各层HSV 色相插值混合色。
- **JEI 支持**：安装 JEI 后新增「军械库锻刃」配方分类，展示每把武器的锻刃层/基础开关/染料配方、结果与经验消耗，铁砧作为催化剂；未安装 JEI 时自动不加载该集成。
- **砂轮抹除**：已锻刃的武器单独放入砂轮（底部空），可**一次性抹除全部锻刃**（锻刃层、染料、发光/粒子开关、冷却、稀有度加成），保留锋利度；操作方式同原版砂轮去附魔。
- **效果开关（基础锻刃）**：铁砧中武器 + **发光墨囊**（1:1、1 级经验）才开启**刀刃发光 + BC 刀光发光/颜色**；武器 + **烈焰粉**（1:1、1 级经验）才开启**粒子效果**。两个开关各自独立，且**每种开关每把武器只能锻一次**（已锻后铁砧不再接受该材料）：**只锻烈焰粉**→有（大颗粒、清晰可见的）不发光粒子、BC 刀光保持原版且不发光、刀刃不发光；**只锻发光墨囊**→刀刃与 BC 刀光发光、无粒子；**两者都有**→发光粒子 + 发光 BC 刀光 + 发光刀刃。颜色仍由染料/锻刃混合色决定，开关只负责开启。
- **染料锻刃**：铁砧中武器 + 任意染料（1 个染料 : 1 把武器、消耗 1 级经验）→ **永久染色**；可无限次染色，每次新染料与当前颜色 **50/50 混合叠加**（只保留最终一个颜色值，不累积历史、不套娃数据）；染料颜色作用于**刀刃光效、粒子、Better Combat 刀光**（覆盖锻刃混合色；需先锻入发光墨囊才显示于刀刃发光/BC 刀光，需烈焰粉才显示于粒子），tooltip 显示 `染色: #RRGGBB`。
  - 锻刃效果冷却按**武器堆叠**记录（绝对游戏时间，存于物品数据组件），丢弃、切换、移动物品都不会重置冷却。
  - 双持时**仅主手武器**的锻刃/锋利度效果生效：副手攻击不触发锻刃效果，也不消耗主手锋利度。
  - 双持渲染分开：刀光、火花粒子、刀刃光效各自使用**对应手持武器**的颜色/状态渲染（不会套用主手），只有锻刃战斗效果只取主手。
- 生存合成配方（铁锭 + 木棍）、`minecraft:swords` 标签、中英文翻译。
- Better Combat 为必要前置（2.4.0+，需 playeranimator、cloth_config）。

## 渲染：GUI 缩略图与 3D 模型

游戏内物品模型（`models/item/<id>.json`）为 `builtin/entity` 模型，携带导出的显示变换；`NeonArsenalItemRenderer` 根据显示上下文选择：

| 上下文 | 使用模型 |
|---|---|
| GUI / 背包 / 快捷栏 / 创造栏 | `item/<id>_thumbnail`（16×16 图标） |
| 第一/第三人称、地面、展示框等 | `item/<id>_3d`（完整 Blockbench 几何） |

`builtin/entity` 模型**不包含 `gui` 显示项**，避免 2D 图标被旋转/缩放。两个变体由 `NeonArsenalClient` 通用注册，新增武器无需 Java 改动。

## 刀刃发光

每把武器的 `item/<id>_fx` 复用原几何、纹理键绑定 `<weapon>_blade.png` 遮罩，以全亮叠加绘制；颜色来自配置或锻刃色。第一人称使用延迟补画（GUI 前）以避开 Iris 手部 pass 的混合限制。

纹理组织为三个目录（`assets/bettercombat_neonarsenal/textures/`）：

| 目录 | 内容 |
|---|---|
| `thumbnail/` | 16×16 GUI 图标 |
| `weapon/` | 3D 模型贴图 |
| `blade/` | 刀刃发光遮罩 |

另保留 `particle/`（粒子图集要求），并通过 `assets/minecraft/atlases/blocks.json` 的 `neoforge:namespaced_directory` 源把三个目录拼入物品图集。

配置 `config/bettercombat_neonarsenal/weapon_glow.json`（约 1 秒热重载）控制发光透明度、混合模式、颜色、拖尾/移动/待机粒子等。

## 锋利度

- 上限 3000，tooltip 显示（绿/黄/红）；武器无耐久条、不会消失。
- 伤害：3000 = 100%，每损失 20%（600）−5%，0 时 75%。
- 消耗：每次命中（同 tick 多目标只扣一次）/ 破坏方块 −1。
- 修复：铁砧中武器 + 蜜脾，每蜜脾 +1000（按需消耗，未用完保留在铁砧），不消耗经验等级。
- 光效：每损失 10% 锋利度，刀光亮度与粒子密度 −20%；损失 ≥50%（≤1500）后完全不发光、不产生粒子。

## 锻刃

铁砧中武器 + 材料，消耗经验，最多 3 层（同层不可重复）：

| 材料 | 经验 | 效果 | 颜色 |
|---|---|---|---|
| 紫颂果 | 8 级 | 持有：增伤 +10%、移速 +15%。命中时把 20 格内、视线无遮挡的敌对生物（非 Boss）拉到身边并僵直 4 秒（无法造成伤害），冷却 10 秒；使用原版末影人传送粒子与音效；正被苦力怕锻刃吸引的生物在苦力怕爆炸前**不受传送影响** | `#E64CFF`（原版传送紫） |
| 苦力怕头颅 | 8 级 | 持有：增伤 +5%、攻速 +15%。命中后生成一只**完全无敌**的苦力怕，向**远离玩家**的方向快速行走 4 秒（约 5 格/秒），沿途吸引 6 格内的生物；随后 1.4 秒白色闪烁膨胀并爆炸：不破坏地形、仅伤害敌对生物（半 TNT 伤害，附带击退 III），冷却 30 秒 | `#4CFF4C`（苦力怕绿） |
| 兔子脚 | 8 级 | 持有：攻速 +5%、跳跃高度 +150%（默认 2.5 倍）、跳劈增伤 +100%；攻击时召唤 4 只 2 格高大型兔子：**完全无敌**、彼此不攻击、主动追踪最近的敌对生物（不会攻击苦力怕锻刃生成的苦力怕），每 0.5 秒最多攻击 3 个目标、每个 4 伤害（移动速度为普通兔子 2 倍），10 秒后必定消失（白色粒子效果，到期时间存于实体自身）；30 秒武器冷却 | `#D2B48C`（兔毛棕） |
| 回响碎片 | 8 级 | 持有：攻速 −50%、增伤 +100%。命中时让目标周围 8 格内至少 2 个敌对生物**互相残杀 8 秒**（互相锁定攻击），散发深暗之域色（幽匿灵魂）粒子；30 秒武器冷却 | `#0E7C7B`（深暗之域青） |


完整规范见 [docs/FORGING_SPEC.md](docs/FORGING_SPEC.md)。

刀光、刀刃光效、火花粒子与 Better Combat 自带刀光均使用混合色（Better Combat 刀光通过 mixin 覆盖 `SlashParticleUtil.appearanceFor`）。

## 模组列表设置

模组列表 → BetterCombat:NeonArsenal → 配置（点击打开模组自带的设置界面，两个滑条可拖动）：

- **粒子浓度**（particle_density，0–3，默认 1）：全局乘算所有武器火花/锻刃粒子的数量；
- **刀刃发光强度**（blade_glow_intensity，0–1，默认 1）：全局乘算刀刃发光叠加层亮度。

## 需求

- Minecraft 1.21.1、NeoForge 21.1.265、Java 21
- Better Combat 2.4+（必需，含依赖 playeranimator、cloth_config）
- Sodium/Iris、Leawind 第三人称等模组可共存

## 构建

```
gradlew build
# 产物: build/libs/bettercombat_neonarsenal-1.0.0.jar
```

## 架构 / 新增武器

武器集中在 `weapon/WeaponCatalog`；`ModItems`、创造栏、模型注册、光效、粒子、跳劈、锋利度与锻刃均为通用逻辑。新增武器只需：

1. 在 `WeaponCatalog` 添加条目（id、铁级、伤害/速度、Better Combat 预设）；
2. 添加 `models/item/<id>.json`（builtin/entity + 显示变换）、`<id>_3d.json`、`<id>_fx.json`、`<id>_thumbnail.json`；
3. 添加 `textures/thumbnail|weapon|blade` 对应贴图；
4. 添加 `data/bettercombat_neonarsenal/weapon_attributes/<id>.json`、配方、`minecraft:swords` 标签、语言条目。

## 资源来源

`resourcepacks` 为唯一资源来源，模组内文件均为其字节一致的副本（几何、3D 贴图、刀刃遮罩、缩略图），通过脚本按目录映射复制；不修改提供的模型/贴图。

## 验证

- `gradlew build` 成功；最终 jar：`build/libs/bettercombat_neonarsenal-1.0.0.jar`（SHA256 `3E7C1CF9…B447`）。
- 6 把武器注册、Better Combat 预设绑定、跳劈/刀光 mixin、锋利度与锻刃逻辑均通过离线校验与实机启动验证；无缺失模型/贴图警告。
- JEI 插件实机验证：插件加载并注册 132 条锻刃配方。
- 专用服务器验证：仅安装必要前置（Better Combat + 依赖）时服务器可正常启动（Done），无客户端类加载错误；所有锻刃/冷却/粒子逻辑均为服务端结算，多人游戏各客户端独立渲染。

## 许可证

见仓库许可文件。

---

# English

# BetterCombat:NeonArsenal

A small, data-driven NeoForge weapon mod for Minecraft **1.21.1** whose melee weapons use
[Better Combat](https://modrinth.com/mod/better-combat) weapon presets for their attack
animations.

It currently ships six weapons — a one-handed **Sword**, two-handed **Claymore**, **Twin Blade**,
**Double Axe**, **Hengdao** and the one-handed **Rapier** — and is structured so more weapons can be
added without touching the registration code.

## Features

| Item | Id | Better Combat preset family | Base stats |
|---|---|---|---|
| Longsword | `bettercombat_neonarsenal:sword` | `bettercombat:sword` (one-handed: horizontal slashes + stab) | Iron tier, 8.75 attack damage, 1.6 attack speed (base DPS 14) |
| Broadsword | `bettercombat_neonarsenal:claymore` | `bettercombat:claymore` (two-handed: wide slash, thrust, overhead slam) | Iron tier, 20 attack damage, 0.8 attack speed, +0.35 attack range (base DPS 16) |
| Double-Edged Sword | `bettercombat_neonarsenal:twinblade` | `bettercombat:twin_blade` (two-handed: alternating switch-blade slashes) | Iron tier, 10 attack damage, 1.2 attack speed (base DPS 12) |
| Double-Bladed Axe | `bettercombat_neonarsenal:doubleaxe` | `bettercombat:double_axe` (two-handed: wide slash + 360° spin) | Iron tier, 26.67 attack damage, 0.6 attack speed (base DPS 16) |
| Hengdao | `bettercombat_neonarsenal:hengdao` | `bettercombat:katana` (two-handed: alternating vertical/horizontal katana slashes) | Iron tier, 14 attack damage, 1.0 attack speed (base DPS 14) |
| Rapier | `bettercombat_neonarsenal:rapier` | `bettercombat:rapier` (one-handed: quick horizontal slash + forward stab) | Iron tier, 6.36 attack damage, 2.2 attack speed (base DPS 14) |

- A dedicated **BetterCombat:NeonArsenal** creative tab only (no vanilla combat tab entries);
  weapons do not stack, so each slot holds exactly one weapon.
- **2D GUI thumbnails**: the supplied 16×16 icons from `resourcepacks/item` are used *only* for the
  inventory/hotbar/creative-menu thumbnail; every in-world context renders the full 3D model.
- **Glowing blade**: every weapon ships a supplied `<weapon>_blade.png` mask (opaque only on the
  blade pixels). The mask is bound to an overlay model (`item/<id>_fx`) that re-uses the authored
  geometry, which is then drawn full-bright on top of the base model — only the blade glows.
  Everything is tunable in `config/bettercombat_neonarsenal/weapon_glow.json`.
- **Glowing slash trail**: each weapon's `weapon_attributes` data declares its own Better Combat
  `trail_appearance` with `glows: true`, so Better Combat's built-in slash trail is self-lit too.
- **Trail sparks**: while a Better Combat weapon swing is running, small full-bright sparks are
- **Forge mixed colour**: one layer shows that solid colour; two or more layers use the **mixed colour** (HSV hue interpolation, spec 3.2) for the blade glow, particles and BC slash trail.
- **Forge particles**: particles use the weapon's **mixed forge colour** (single layer = its solid colour).
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
  attack knock the target back with Knockback V strength (2.5 velocity). A jump attack (while the attack input is held/pressed)
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
renderer (`client/NeonArsenalItemRenderer`) switches between two baked variants:

| Context | Model used |
|---|---|
| GUI / inventory / hotbar / creative menu / recipe viewers | `item/<id>_thumbnail` — the supplied 16×16 icon from `resourcepacks/item`, used **only** here |
| First person, third person, ground, item frames, ... | `item/<id>_3d` — the full Blockbench export (unchanged geometry) |

The visible item model (`models/item/<id>.json`) is a `builtin/entity` model carrying the exported
display transforms, so vanilla applies the hand/ground transform before the renderer draws. The two
additional models are registered generically for every catalogue weapon by `client/NeonArsenalClient`,
which means future weapons need no Java changes.

The `builtin/entity` model intentionally has **no `gui` display entry** (only `thirdperson_*`,
`firstperson_*`, `ground` and `fixed`): the GUI transform is applied by vanilla before the renderer
runs, so a `gui` entry would rotate/scale the supplied 16×16 icon as well.

## Blade glow

Each weapon gets a second, mask-based overlay model (`models/item/<id>_fx.json`): it parents the
same geometry and overrides texture key `1` with the supplied `<weapon>_blade.png` mask. The mask is
transparent everywhere except the blade pixels, so drawing the overlay with full-bright light and a
glow blend only lights up the blade. The overlay is registered as an additional model and drawn by
`client/NeonArsenalItemRenderer` right after the base model in every non-GUI display context.

Client configuration (`config/bettercombat_neonarsenal/weapon_glow.json`, created on first launch and
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
    "particle": "bettercombat_neonarsenal:glow_spark",
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
| `trail.particle` | Particle id; default is the mod's own small full-bright spark `bettercombat_neonarsenal:glow_spark` |
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
each `data/bettercombat_neonarsenal/weapon_attributes/<id>.json` carries

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
| `bettercombat_neonarsenal:glow_spark` | vanilla `glow` | plain star spark |
| `bettercombat_neonarsenal:glow_glitter` | vanilla `glitter_*` | animated end-rod twinkle, white |
| `bettercombat_neonarsenal:glow_sparkle` | vanilla `spark_*` | animated firework spark, white |
| `bettercombat_neonarsenal:glow_crit` | vanilla `critical_hit` | crisp crit flash, white |
| `bettercombat_neonarsenal:glow_magic` | vanilla `enchanted_hit` | soft glint, white |

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
`data/bettercombat_neonarsenal/weapon_attributes/<item id>.json`. The numbers below are the per-swing
`damage_multiplier` values (multiplied with the item's base attack damage); every swing references
a Better Combat preset player animation.

| Weapon | Swing | Hitbox | Damage × | Angle | Upswing | Animation / sound |
|---|---|---|---|---|---|---|
| Longsword | horizontal right slash | `HORIZONTAL_PLANE` | 1.00 | 120° | 0.5 | `bettercombat:one_handed_slash_horizontal_right` / `sword_slash` |
| Longsword | horizontal left slash | `HORIZONTAL_PLANE` | 1.00 | 120° | 0.5 | `bettercombat:one_handed_slash_horizontal_left` / `sword_slash` (pitch 1.05) |
| Longsword | thrust | `FORWARD_BOX` | 1.15 | 0° | 0.4 | `bettercombat:one_handed_stab` / `sword_slash` (pitch 1.2) |
| Broadsword | wide horizontal slash | `HORIZONTAL_PLANE` | 0.80 | 150° | 0.5 | `bettercombat:two_handed_slash_horizontal_right` / `claymore_swing` |
| Broadsword | thrust | `FORWARD_BOX` | 1.00 | 0° | 0.5 | `bettercombat:two_handed_stab_left` / `claymore_stab` |
| Broadsword | overhead slam | `VERTICAL_PLANE` | 1.30 | 150° | 0.5 | `bettercombat:two_handed_slam` / `claymore_slam` |
| Double-Edged Sword | switch-blade slash right | `HORIZONTAL_PLANE` | 1.00 | 150° | 0.5 | `bettercombat:one_handed_slash_switch_blade_right` / `sword_slash` |
| Double-Edged Sword | switch-blade slash left | `HORIZONTAL_PLANE` | 1.00 | 150° | 0.5 | `bettercombat:one_handed_slash_switch_blade_left` / `sword_slash` |
| Double-Bladed Axe | wide horizontal slash | `HORIZONTAL_PLANE` | 1.00 | 180° | 0.5 | `bettercombat:two_handed_slash_horizontal_right` / `double_axe_swing` |
| Double-Bladed Axe | 360° spin | `HORIZONTAL_PLANE` | 1.00 | 360° | 0.5 | `bettercombat:two_handed_spin` / `double_axe_swing` (pitch 1.1) |
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

## Sharpness (tooltip index + anvil repair)

Arsenal weapons have **no durability and never break**. They carry a sharpness index (0–3000)
shown in the item **tooltip** (`锋利度: 3000 / 3000`):

- **Damage**: 3000 = 100%; every 20% lost (600 points) reduces damage by 5%, so 0 sharpness still
  deals 75%.
- **Consumption**: each attack hit (once per player tick, so a multi-target swing costs one point)
  and each broken block costs 1 sharpness, down to 0.
- **Repair**: exactly like vanilla repairing an iron tool with iron ingots — put the weapon and
  honeycombs in an **anvil**. Each consumed honeycomb restores 1000 sharpness; as many honeycombs as
  needed are consumed (up to full sharpness) and unused honeycombs stay in the anvil. The
  sharpening operation costs **no experience levels**.
- **Glow/particles**: every 10% lost reduces blade glow brightness and spark density by 20%; at 50%
  or more lost (sharpness ≤ 1500) the blade no longer glows and no sparks appear.

Implemented through the `bettercombat_neonarsenal:sharpness` data component, a non-damageable item
class with tooltip, damage/consumption events and an `AnvilUpdateEvent` handler.


## Blade forging (锻刃)

When dual wielding, only the **main-hand** weapon's forge/sharpness effects apply: off-hand
attacks neither trigger forge effects nor consume main-hand sharpness.
Dual-wield rendering is separate: each hand's slash trail, sparks and blade glow use that weapon's own colour/state (never the main hand's); only forge combat effects are main-hand only.

All forge-effect cooldowns (chorus, creeper, rabbit) are stored on the weapon stack as absolute
game time; dropping, switching or moving the weapon never resets them.

Put an arsenal weapon and one special item in an **anvil**. Forging costs experience levels and
adds one forge layer; a weapon supports at most **3 layers** and the same layer cannot be added
twice. Already forged weapons can be forged further.

| Item | Cost | Effect | Colour |
|---|---|---|---|
| Creeper Head (苦力怕头颅) | 8 levels | While held: +5% damage, +15% attack speed. On hit: spawns a **completely invulnerable** creeper that quickly walks **away from the player** for 4 s (~5 blocks/s), pulling living mobs within 6 blocks along its path; it then swells/flashes white for 1.4 s and explodes. The explosion breaks no blocks, only hurts hostile mobs (half TNT-style damage, Knockback III) and has a 30 s cooldown. | `#4CFF4C` creeper green |
| Rabbit's Foot (兔子脚) | 8 levels | While held: +5% attack speed, +150% jump height (2.5x default), +100% jump-slam damage. On hit: summons four 2-block-tall rabbits that are **completely invulnerable**, never attack each other, actively hunt the nearest hostile mob (never the creepers spawned by the creeper-head forge) and bite **up to 3** hostile mobs every **0.5 s** for **4 damage each** (movement speed doubled); they always vanish after 10 s with white particles (expiry stored on the entity itself). 30 s weapon cooldown. | `#D2B48C` tan |
| Echo Shard (回响碎片) | 8 levels | While held: -50% attack speed, +100% damage. On hit: at least 2 hostile mobs within 8 blocks of the target are driven to **fight each other for 8 s** (mutually targeted), emitting Deep Dark sculk-soul particles; 30 s weapon cooldown. | `#0E7C7B` Deep Dark teal |
| Chorus Fruit (紫颂果) | 8 levels | While held: +10% damage, +15% movement speed. On hit: hostile non-boss mobs within 20 blocks with line of sight are teleported into a tight attack-range cluster and stunned for 4 s (cannot deal damage); 5 s cooldown. Teleport uses the vanilla enderman effect: purple portal particles and the enderman teleport sound at both the origin and the destination. | `#E64CFF` (vanilla teleport purple) |


See [docs/FORGING_SPEC.md](docs/FORGING_SPEC.md) for the full forging specification.

The blade glow overlay and the spark particles use the **HSV hue-interpolated colour of all forge layers**,
e.g. chorus + creeper = `#4CB2FF` (teal blue). Better Combat's own slash trail also uses the weapon's uniform mixed forge colour (primary opaque, secondary semi-transparent).
overrides `SlashParticleUtil.appearanceFor` for forged weapons (when Better Combat is installed).
Forged layers are listed in the weapon tooltip (`锻刃: 紫颂`, ...).

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
# jar: build\libs\bettercombat_neonarsenal-1.0.0.jar
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
data/bettercombat_neonarsenal/weapon_attributes/<item id>.json
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
(`weapon_attributes/claymore.json` for `bettercombat_neonarsenal:claymore`).

The optional dependency is declared in `META-INF/neoforge.mods.toml` with `type="optional"` and
`ordering="AFTER"`, so load order is correct when Better Combat is present and nothing breaks
when it is absent. At startup the mod logs each weapon's preset mapping; a warning is printed when
Better Combat is missing.

One small exception to the pure-data rule is the airborne jump attack: Better Combat has no
airborne attack condition, so `compat/JumpAttackHandler` (all Better Combat types are reached
reflectively) plus `mixin/bettercombat/PlayerAttackHelperMixin` replace the selected attack with
the claymore's last attack while the player is not on the ground. `NeonArsenalMixinPlugin` only applies
that mixin when Better Combat is loaded, so the mod still compiles, loads and runs without it.

## Architecture / adding a weapon

```
src/main/java/com/zszq/bettercombat_neonarsenal/
├── NeonArsenal.java          # @Mod entry point: registries, creative tabs, startup log
├── weapon/
│   ├── WeaponDefinition.java         # immutable weapon data + fluent builder + optional custom item factory
│   ├── WeaponCatalog.java            # ★ THE weapon list — add new weapons here
│   └── NeonArsenalPresets.java      # opaque Better Combat preset ids (no Better Combat classes)
├── registry/
│   ├── ModItems.java                 # loops over WeaponCatalog, registers every item
│   ├── ModParticles.java             # glow_spark particle type used by the trail sparks
│   └── ModCreativeTabs.java          # arsenal tab + vanilla Combat tab entries
├── client/                           # client-only (never loaded on a dedicated server)
│   ├── NeonArsenalClient.java            # models, client item extensions, geometry loaders, particle provider
│   ├── NeonArsenalItemRenderer.java      # GUI thumbnail vs in-world 3D renderer + blade glow call
│   ├── PermissiveModelLoader.java    # loads unmodified exports with non-vanilla rotation angles
│   └── fx/
│       ├── WeaponGlow.java           # full-bright blade-mask overlay (additive / emissive for Iris)
│       ├── WeaponTrailFx.java        # Better Combat swing / movement spark emitters
│       ├── GlowSparkParticle.java    # small full-bright spark (0.5 s attack, 2.5 s movement)
│       └── WeaponGlowConfig.java     # config/bettercombat_neonarsenal/weapon_glow.json loading + reload
└── compat/
    └── NeonArsenalIntegration.java  # optional presence check + startup logging only
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
           .betterCombatPreset(NeonArsenalPresets.of("katana"))
           .build()
   ```

2. **Models** (all files are our own except the byte-identical geometry):
   - `models/item/<id>_geometry.json` — the Blockbench export copied **unchanged** (normal case);
   - `models/item/<id>_3d.json` — thin wrapper that parents to the geometry and fixes the texture
     namespace, e.g. `{"parent":"bettercombat_neonarsenal:item/<id>_geometry","textures":{"1":"bettercombat_neonarsenal:block/<texture>"}}`;
   - **exceptional rotations**: 1.21.1's vanilla parser only accepts element angles
     `-45/-22.5/0/22.5/45`, and `ModelManager` eagerly parses every file under `models/`. If an
     export uses other angles (the twin blade uses ±15°, the double axe uses ±25° plus Euler
     `x/y/z` rotations), put it unchanged under `assets/bettercombat_neonarsenal/geometry/<id>.json`
     instead and use the permissive loader:

     ```json
     // models/item/twinblade_3d.json (same pattern for doubleaxe_3d.json)
     {
       "loader": "bettercombat_neonarsenal:permissive_model",
       "source": "bettercombat_neonarsenal:geometry/twinblade",
       "textures": { "particle": "bettercombat_neonarsenal:block/twinblade",
                     "1": "bettercombat_neonarsenal:block/twinblade" }
     }
     ```
   - `models/item/<id>_thumbnail.json` — 2D GUI icon, e.g.
     `{"parent":"minecraft:item/generated","textures":{"layer0":"bettercombat_neonarsenal:item/<icon>"}}`;
   - `models/item/<id>_fx.json` — blade glow overlay: same parent/loader as `_3d` but texture key
     `1` bound to `bettercombat_neonarsenal:item/<id>_blade` (the supplied mask);
   - `models/item/<id>.json` — `builtin/entity` model with the display transforms copied from the
     export (first/third person, ground, fixed) for the custom renderer;
   - the thumbnail icon may be the only user of a supplied `resourcepacks/item` PNG.
3. **Better Combat attributes**: create `data/bettercombat_neonarsenal/weapon_attributes/<id>.json` with
   a full, explicit `attributes` block — `range_bonus`, `two_handed`, `pose`, `category` and the
   `attacks` array. Base the numbers on the matching Better Combat preset (`sword`, `claymore`,
   `twin_blade`, `double_axe`, `katana`, `dagger`, `axe`, `mace`, `spear`, `halberd`, `scythe`, `battlestaff`, ...)
   and keep its `bettercombat:*` preset `animation` ids so the swings still play Better Combat
   animations; tune `damage_multiplier`, `angle`, `upswing` and `swing_sound` per weapon.
4. **Translation**: `assets/bettercombat_neonarsenal/lang/en_us.json` (and `zh_cn.json`) with
   `item.bettercombat_neonarsenal.<id>` and, if needed, a creative tab icon update.
5. **Tags/recipes**: add the item to `data/minecraft/tags/item/swords.json` (swords) or
   `data/minecraft/tags/item/axes.json` (axes; create it if absent) and add a recipe under
   `data/bettercombat_neonarsenal/recipe/`.

`ModItems`, the creative tab and the startup diagnostics pick the weapon up automatically.

## Asset provenance

All supplied weapon textures are organised into three folders under
`assets/bettercombat_neonarsenal/textures/`:

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
| `resourcepacks/block/claymore.json` | `assets/bettercombat_neonarsenal/models/item/claymore_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/sword.json` | `assets/bettercombat_neonarsenal/models/item/sword_geometry.json` | unchanged geometry + display states |
| `resourcepacks/block/twinblade.json` | `assets/bettercombat_neonarsenal/geometry/twinblade.json` | unchanged geometry; uses ±15° element rotations, loaded by `permissive_model` |
| `resourcepacks/block/doubleaxe.json` | `assets/bettercombat_neonarsenal/geometry/doubleaxe.json` | unchanged geometry; uses ±25° and Euler x/y/z rotations, loaded by `permissive_model` |
| `resourcepacks/texture/claymore.png` | `assets/bettercombat_neonarsenal/textures/weapon/claymore.png` | 128×128 3D model texture |
| `resourcepacks/texture/sword.png` | `assets/bettercombat_neonarsenal/textures/weapon/sword.png` | 64×64 3D model texture |
| `resourcepacks/texture/twinblade.png` | `assets/bettercombat_neonarsenal/textures/weapon/twinblade.png` | 64×64 3D model texture |
| `resourcepacks/texture/doubleaxe.png` | `assets/bettercombat_neonarsenal/textures/weapon/doubleaxe.png` | 128×128 3D model texture |
| `resourcepacks/block/hengdao.json` | `assets/bettercombat_neonarsenal/models/item/hengdao_geometry.json` | unchanged geometry + display states |
| `resourcepacks/texture/hengdao.png` | `assets/bettercombat_neonarsenal/textures/weapon/hengdao.png` | 3D model texture |
| `resourcepacks/texture/hengdao_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/hengdao_blade.png` | blade glow mask (used only by `item/hengdao_fx`) |
| `resourcepacks/item/hengdao.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/hengdao.png` | 16×16 GUI thumbnail (used by `item/hengdao_thumbnail`) |
| `resourcepacks/block/rapier.json` | `assets/bettercombat_neonarsenal/geometry/rapier.json` | unchanged geometry (read by the `permissive_model` loader) |
| `resourcepacks/texture/rapier.png` | `assets/bettercombat_neonarsenal/textures/weapon/rapier.png` | 3D model texture |
| `resourcepacks/texture/rapier_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/rapier_blade.png` | blade glow mask (used only by `item/rapier_fx`) |
| `resourcepacks/item/rapier.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/rapier.png` | 16×16 GUI thumbnail (used by `item/rapier_thumbnail`) |
| `resourcepacks/item/claymore.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/claymore.png` | 16×16 claymore GUI thumbnail (used only by `item/claymore_thumbnail`) |
| `resourcepacks/item/sword.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/sword.png` | 16×16 sword GUI thumbnail (used only by `item/sword_thumbnail`) |
| `resourcepacks/item/twinblade.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/twinblade.png` | 16×16 twin blade GUI thumbnail (used only by `item/twinblade_thumbnail`) |
| `resourcepacks/item/doubleaxe.png` | `assets/bettercombat_neonarsenal/textures/thumbnail/doubleaxe.png` | 16×16 double axe GUI thumbnail (used only by `item/doubleaxe_thumbnail`) |
| `resourcepacks/texture/sword_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/sword_blade.png` | blade glow mask (used only by `item/sword_fx`) |
| `resourcepacks/texture/claymore_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/claymore_blade.png` | blade glow mask (used only by `item/claymore_fx`) |
| `resourcepacks/texture/twinblade_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/twinblade_blade.png` | blade glow mask (used only by `item/twinblade_fx`) |
| `resourcepacks/texture/doubleaxe_blade.png` | `assets/bettercombat_neonarsenal/textures/blade/doubleaxe_blade.png` | blade glow mask (used only by `item/doubleaxe_fx`) |

The supplied Blockbench exports reference their textures with un-namespaced paths
(`"1": "block/claymore"` etc.), which Minecraft resolves in the `minecraft:` namespace. To keep the
provided files untouched, the geometry files are never edited and are wrapped for baking by
`models/item/<id>_3d.json`, which only overrides texture key `1` with a correctly namespaced,
atlas-stitched path:

```json
// models/item/claymore_3d.json (3D model used in hand)
{
  "parent": "bettercombat_neonarsenal:item/claymore_geometry",
  "textures": { "1": "bettercombat_neonarsenal:block/claymore" }
}

// models/item/sword_thumbnail.json (2D GUI thumbnail, supplied icon)
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "bettercombat_neonarsenal:item/model" }
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
  "loader": "bettercombat_neonarsenal:permissive_model",
  "source": "bettercombat_neonarsenal:geometry/twinblade",
  "textures": { "particle": "bettercombat_neonarsenal:block/twinblade",
                "1": "bettercombat_neonarsenal:block/twinblade" }
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
  `build/libs/bettercombat_neonarsenal-1.0.0.jar` (SHA256 `3E7C1CF9…B447`).
- Updated supplied assets are included byte-identically: `resourcepacks/block/claymore.json` →
  `assets/bettercombat_neonarsenal/models/item/claymore_geometry.json` (SHA256 `DB27C17E…`, latest re-export; display transforms kept in sync in `models/item/claymore.json`),
  `resourcepacks/block/twinblade.json` → `assets/bettercombat_neonarsenal/geometry/twinblade.json`
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
  world joins, Better Combat logs `Loaded container: bettercombat_neonarsenal:sword` / `...claymore` /
  `...twinblade` / `...doubleaxe`, our log line `Registered 6 arsenal weapon(s)` is printed, the
  attack hook reports `Better Combat attack hook installed; blade trail particles are active.`, the
  jump-attack mixin reports `Jump-attack mixin applied: true`, and
  no `Missing textures in model bettercombat_neonarsenal:*`, `Failed to load model` or render-type errors
  are emitted for this mod (verified for all six weapons including `Hengdao` and `Rapier`).
- In-game the six weapons render with the supplied 16×16 icons in the GUI only, the full 3D model
  in hand, and the `<weapon>_blade.png` mask lights up the blade (additive layer normally, vanilla
  emissive layer while an Iris shader pack is in use); swings emit the configured glowing particles
  along the blade and Better Combat's slash trail is self-lit.
- Dedicated server with Better Combat 2.4.0 (+ Cloth Config 15.0.140, playerAnimator 2.0.4):
  Better Combat logged `Loaded container: bettercombat_neonarsenal:sword` / `...claymore` /
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
