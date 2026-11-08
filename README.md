# 更好的战斗：霓虹装备

[中文](#更好的战斗霓虹装备) | [English](#english)

> 极光工业新一代作战套件：8 把武器、会变钝的刀口、4 种锻刃材料、2 个效果开关、任意染料染色。
> 核心工艺由同一套工业铁砧完成，抹除交给砂轮。
>
> **背景**：极光工业（Aurora Industries）推出的新一代作战武器套件。

## 安装

- Minecraft 1.21.1、NeoForge 21.1.265、Java 21
- **必要前置**：Better Combat 2.4.0+、playeranimator、cloth_config
- 可选：JEI（配方查看）、Sodium/Iris（光影兼容）、**LambDynamicLights**（动态光照）
- **推荐同时安装 Sodium + LambDynamicLights**：锻入发光墨囊的武器会像发光地衣一样发出 7 级动态光
- 推荐同步安装：**Fancy Player Animation**（玩家动作动画增强，与本模组兼容）

## 快速开始

1. 装好前置，进游戏做铁砧。
2. **武器 + 材料**：锻刃；**武器 + 蜜脾**：修锋利度；**武器 + 染料**：染色。
3. 想换工艺配置：把武器放进砂轮，一次抹干净，锋利度保留。

## 游戏特色

### 1. 刀会变钝

- 锋利度 3000 满值，每次命中或挖方块 -1。
- 伤害从 100% 起，每掉 20% 锋利度少 5%，全钝仍有 75%。
- 光效与粒子一起衰减：每损失 10% 暗 20%，损失过半直接熄灭。
- 铁砧里用蜜脾补，一个 +1000，不花经验；修复结果可直接从铁砧取出（修复操作不消耗经验等级）。

### 2. 四种锻刃材料

铁砧中武器 + 材料，8 级经验、1:1 消耗；最多 3 层，同层不能重复，每层稀有度 +1。

| 材料 | 随身效果 | 命中效果 | 冷却 |
|---|---|---|---|
| 紫颂果 | 伤害 +10%、移速 +15% | 20 格内敌人拉进攻击范围，僵直 4 秒 | 10 秒 |
| 苦力怕头颅 | 伤害 +5%、攻速 +15% | 无敌苦力怕（免疫伤害与击退）远离并强吸敌对生物，目标锁定至爆炸无法脱锁，随后爆炸（不破坏地形、仅伤敌对，dummmmmmy 人偶除外） | 30 秒 |
| 兔子脚 | 攻速 +5%、跳跃高度 +150%、跃斩增伤 +100% | 4 只无敌雷霆大兔子（免疫伤害与击退），每 0.5 秒咬 3 个目标各 4 伤害，10 秒后消失 | 30 秒 |
| 回响碎片 | 攻速 -50%、伤害 +100% | 8 格内敌人互相攻击 8 秒 | 30 秒 |

### 3. 两个开关

**发光墨囊**开启刀刃发光与挥砍刀光，**烈焰粉**开启粒子；两者独立，每把武器各只能锻一次。未锻发光墨囊时，锻刃层与染料仍会给刀刃上色（不发光）。装有 LambDynamicLights 时，锻入发光墨囊的武器还会发出 **7 级动态光（等同发光地衣）**。

| 锻入的开关 | 粒子 | 挥砍刀光 | 刀刃 |
|---|---|---|---|
| 只烈焰粉 | 不发光尘粒 | 原版、不发光 | 有色不发光（有锻刃/染料时） |
| 只发光墨囊 | 无 | 发光、着色 | 发光、着色 |
| 两个都有 | 发光粒子 | 发光、着色 | 发光、着色 |

### 4. 颜色

- 单层锻刃：该层纯色；多层锻刃：取各层颜色之间的过渡混色。
- 颜色同时作用于刀刃（含未发光墨囊时的非发光着色）、刀刃光效、粒子与挥砍刀光。
- 任意染料 1:1 染色、1 级经验、无限次；每次与当前颜色各半混合，只留最终一色。
- 染料颜色优先于混合色；整条颜色链在 Tooltip 的「着色模块/锻刃颜色」中显示。

### 5. 跳劈

- 起跳后至落地前均可触发（上升与下落都算）；水中、攀爬、骑乘、失明时不触发。
- 触发时播放巨刃下劈动作、使用武器自身音效、击退 V、重置连招。
- 跳劈后攻击冷却的前半段禁止起跳；落地后清空跳劈窗口。
- 创造飞行时攻击使用平地出招，不触发跳劈。

### 6. 下落斩击

- 自由下落时按住攻击键（已累计下落超过 5 格，或离地 5 格以上且正在下降）；长按 0.15 秒进入下落斩击；短按则会在松开时打一次普通攻击。**拥有缓降效果时无法触发**。
- 起手动作停在下劈前 1/3 的蓄力姿势；下落过程中风声随下落速度增强——坠得越快，风越大。
- 距地 4 格时恢复动作，从 1/3 处继续劈下，并播放巨刃第三段斩击音效；无论落地前后，整段动作都以原速播完，不会被落地截断。
- 到最终帧用 0.3 秒淡出回站立姿态；这 0.3 秒内无法攻击，结束后恢复正常攻击。
- 落地有专属落地音效；落地时若武器锻入烈焰粉，会迸发大量粒子。
- **下落斩击的额外攻击力随实际下落高度提升，且没有上限**：第 1–10 格每格 +4 伤害，第 11–20 格每格 +2 伤害，之后每格 +1 伤害。
- 松开攻击键（尚未到 4 格）会在 0.2 秒内淡回下落姿态，并重置已累计的下落高度，需要重新按住再下落超过 5 格才会再次触发。
- 下落斩击：下落 **<10 格免疫摔落伤害**；**>10 格每多 3 格扣 1 点血量**，最低保留 **1 点血**，不会摔死；普通下落不受影响。
- **装备鞘翅时**：下落斩击蓄力过程中单击空格展开滑翔会**保持当前蓄力姿势**；此后不再按“距地 4 格/落地”触发，而是**撞击生物时触发斩击**。滑翔结束时若没有撞到生物，斩击不会触发，蓄力自动收回。
- **鞘翅滑翔中**（手持可鞘翅斩击的武器）**长按左键 0.15 秒**进入同样的滑行斩击蓄力（1/3 定格），撞到生物时触发斩击；**滑翔中长按/点击右键没有效果**。滑翔斩击的额外攻击力按**撞击瞬间的速度**计算：把速度换算成等效下落高度（`v = √(2·0.08·h)`）后套用同一分档，与下落斩击保持平衡。
- **鞘翅斩击是独立分配的技能**：仅**阔剑、双刃斧、横刀、双刃剑**可用（与下落斩击相同）；其他武器在滑翔和下落时保持**平地的攻击方式**，不会进入蓄力。

### 7. 格挡

- 手持武器长按右键进入格挡（对方块右键同样进入格挡，不触发方块交互）；松开恢复，进入/退出各有 **0.1 秒**过渡。
- 右键优先级：目标为工作方块（带 GUI，如工作台/熔炉/箱子）时，若**玩家与工作方块之间**有敌对生物 → 格挡；否则正常工作方块；非工作方块 → 格挡。
- 单持武器与双手武器使用**双手格挡**姿态；真正双持（副手也持本模组武器）使用**双持格挡**姿态。
- 格挡期间**强制退出潜行**，且**无法疾跑**；若开始格挡时正在疾跑，会立即中断疾跑。
- 只格挡**前方 210°（左右各 105°）**内来源的伤害；背后/侧面来源与环境伤害不受格挡。
- 成功格挡**免疫本次伤害**；**触发格挡音效/粒子时**消耗 **本次伤害 ×10%（向上取整）**的锋利度，间隔内的格挡不消耗；**松开右键后获得 0.2 秒无敌**。
- 格挡成功时**不显示受击红闪**；武器处迸发中等密度火星粒子（无需烈焰粉，颜色跟随当前武器粒子色，锻入发光墨囊时发光）。
- 格挡的音效、粒子与锋利度消耗有 **0.3–0.8 秒随机间隔**（每次重新随机）；间隔内的格挡仍正常免伤，但不再播放音效与粒子，也**不扣锋利度**。
- 每次触发格挡音效/粒子时：手部与武器颤动约 2px、持续约 0.1 秒；视角轻微颤动；角色向后平移约 1/4 格。
- 格挡 **2 秒内**松开不打断连招；超过 2 秒再松开则打断连招。
- 宽窄皮肤均适配；格挡时袖子与外套会跟随动作。
- **穿戴鞘翅格挡时**，鞘翅会收拢为潜行时的姿态，并跟随躯干/肩膀一起运动，不会与身体分离。
- **格挡优先级最高**：开始格挡会清除武器的物品冷却，不受攻击冷却与切换武器冷却影响；格挡期间左键攻击/挖掘会被取消，确保格挡动作优先。

## 武器

| 武器 | ID | 攻速 | 伤害 | DPS | 跳劈 | 格挡 | 下落斩击 | 鞘翅斩击 | 定位 |
|---|---|---|---|---|---|---|---|---|---|
| 长剑 | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | ✓ | ✓ | — | — | 单手，最稳 |
| 阔剑 | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | ✓ | ✓ | ✓ | ✓ | 双手，+0.35 距离 |
| 双刃剑 | `bettercombat_neonarsenal:twinblade` | 1.4 | 9.2 | 12.9 | ✓ | ✓ | ✓ | ✓ | 双手连段 |
| 双刃斧 | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | ✓ | ✓ | ✓ | ✓ | 双手旋转斩 |
| 横刀 | `bettercombat_neonarsenal:hengdao` | 1.1 | 11.0 | 12.1 | ✓ | ✓ | ✓ | ✓ | 双手拔刀斩 |
| 刺剑 | `bettercombat_neonarsenal:rapier` | 2.5 | 5.0 | 12.5 | ✓ | ✓ | — | — | 单手快刺 |
| 匕首 | `bettercombat_neonarsenal:dagger` | 3.2 | 4.0 | 12.8 | — | — | — | — | 单手短刃，纯输出 |
| 长枪 | `bettercombat_neonarsenal:spear` | 1.2 | 9.2 | 11.0 | ✓ | ✓ | — | — | 双手，+1 距离 |

## 工艺速查

- **冷却跟武器走**：存在物品上按游戏时间计算，丢弃、换手、重进都不刷新。
- **双持**：锻刃与锋利度只由主手攻击触发；副手砍人不触发、不消耗；两手外观各自独立。
- **砂轮**：一次清除锻刃、染料、开关、冷却、稀有度，保留锋利度。
- **Tooltip**：平时显示刀刃锐度、颜色（染色显示为着色模块，仅锻刃层时显示锻刃颜色）、锻刃效果；按住 Shift 看综合性能与每层说明。
- **配置**：模组列表滑条——粒子密度 0–3、刀刃发光强度 0–1。
- **JEI**：176 条锻刃配方，铁砧为催化剂。
- **多人**：锻刃、冷却与粒子效果在多人联机下保持一致。

## 许可

- 代码：MIT License（[LICENSE](LICENSE)）
- 美术资源：CC BY 4.0（[ASSETS-LICENSE.md](ASSETS-LICENSE.md)），再分发保留署名即可商用

---

# English

# BetterCombat:NeonArsenal

[中文](#更好的战斗霓虹装备) | [English](#english)

> Aurora Industries' new-generation combat kit: eight weapons, edges that wear down, four forge materials, two effect
> switches and unlimited dyeing. Core processes run through one industrial anvil; wipes happen at the grindstone.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.265, Java 21
- **Required**: Better Combat 2.4.0+, playeranimator, cloth_config
- Optional: JEI (recipes), Sodium/Iris (shader compatibility), **LambDynamicLights** (dynamic lighting)
- **Sodium + LambDynamicLights recommended**: a weapon forged with a glow ink sac emits light level 7, the same as glow lichen
- Recommended alongside: **Fancy Player Animation** (extra player animations; compatible with this mod)

## Quick start

1. Install the requirements and craft an anvil.
2. **Weapon + material**: forge; **weapon + honeycomb**: repair sharpness; **weapon + dye**: colour.
3. To reroll: put the weapon in a grindstone; one wipe clears everything but sharpness.

## Features

### 1. The edge wears

- Sharpness starts at 3000; -1 per hit or broken block.
- Damage starts at 100%, -5% per 20% sharpness lost, still 75% when fully dull.
- Glow and particles fade too: -20% per 10% lost; past 50% lost they go dark.
- Repair at an anvil with honeycomb: +1000 each, no XP; the repaired weapon can be taken out immediately.

### 2. Four forge materials

Anvil weapon + material: 8 levels, 1:1, up to 3 layers, no duplicate layer, +1 rarity per layer.

| Material | While held | On hit | Cooldown |
|---|---|---|---|
| Chorus Fruit | +10% damage, +15% move speed | Pull mobs within 20 blocks into range, stun 4 s | 10 s |
| Creeper Head | +5% damage, +15% attack speed | An invulnerable creeper walks off, pulls mobs, then explodes | 30 s |
| Rabbit's Foot | +5% attack speed, +150% jump height, +100% jump-slam damage | Four invulnerable thunder rabbits (immune to damage and knockback) bite 3 targets for 4 damage every 0.5 s, vanish after 10 s | 30 s |
| Echo Shard | -50% attack speed, +100% damage | Mobs within 8 blocks fight each other for 8 s | 30 s |

### 3. Two switches

**Glow Ink Sac** enables blade glow and the slash trail; **Blaze Powder** enables particles; independent, one forge each per weapon. Without glow ink, forge layers and dye still tint the blade (without glowing). With LambDynamicLights installed, a weapon forged with a glow ink sac also emits **light level 7 (the same as glow lichen)**.

| Forged switch | Particles | Slash trail | Blade |
|---|---|---|---|
| Blaze only | Non-glowing dust | Original, no glow | Tinted, no glow (with layers/dye) |
| Glow ink only | None | Glowing, coloured | Glowing, coloured |
| Both | Glowing sparks | Glowing, coloured | Glowing, coloured |

### 4. Colour

- One layer: solid colour; several layers: a blended transition colour between them.
- The colour applies to the blade (including a non-glowing tint before glow ink), blade glow, particles and the slash trail.
- Any dye, 1:1, 1 level, unlimited; each blends 50/50 with the current colour, keeping one final colour.
- Dye overrides the mixed colour; the resulting colour is shown in the tooltip (colour module / forge colour).

### 5. Jump slam

- Any time after jumping and before landing counts (rising or falling); in water, on ladders, riding or blinded it does not trigger.
- On trigger: claymore slam animation, the weapon's own sound, Knockback V, combo reset.
- The first half of the attack cooldown blocks jumping; the jump window is cleared on landing.
- Creative-flight attacks use the normal ground attack instead of the jump slam.

### 6. Falling slash

- Hold attack during a free fall (more than 5 blocks already fallen, or more than 5 blocks above the ground while descending); holding for 0.15 s enters the falling slash, while a short tap performs one normal attack on release. **The Slow Falling effect prevents it entirely.**
- The wind-up pose freezes at 1/3, just before the downward chop; during the fall a wind sound grows with your falling speed.
- Four blocks above the ground the animation resumes from the 1/3 pose and the claymore's third attack swing sound plays; the whole animation always finishes at original speed, before or after landing, and is never cut by landing.
- The final frame fades back to idle over 0.3 s; attacks are locked for those 0.3 s and restored afterwards.
- Landing plays its own landing sound; if the weapon was forged with blaze powder, landing emits a large spark burst.
- **Extra attack power grows with the actual fall height and has no cap**: +4 damage per block for the first 10 blocks, +2 per block for blocks 11-20, and +1 per block beyond 20.
- Releasing attack before the 4-block trigger fades back over 0.2 s and resets the measured fall height, so a new press must accumulate more than 5 blocks again.
- Falling slash: **<10 blocks is fall-damage immune**; above 10 blocks lose **1 HP per extra 3 blocks**, clamped to **1 HP** so it cannot kill. Normal falls are unaffected.
- **With an elytra**: deploying glide during the windup keeps the frozen pose; the slash then no longer triggers by ground distance/landing but **only when hitting a creature**. If the glide ends without hitting anything, no strike is made and the windup is retracted.
- **While elytra gliding** (holding a weapon with the elytra-slash skill), **hold left-click for 0.15 s** to enter the same glide windup (frozen at 1/3); hitting a creature triggers the slash. **Right-click does nothing while gliding.** Its extra attack power follows the **impact speed**: the speed is converted to the equivalent free-fall height (`v = √(2·0.08·h)`) and the same height tiers are applied, keeping it balanced with the normal falling slash.
- **Elytra slash is a separately assigned skill**: only the **Broadsword, Double-Bladed Axe, Hengdao and Double-Edged Sword** can use it (same list as the falling slash); all other weapons keep their **normal ground-style attacks** while gliding and falling and never enter the windup.

### 7. Blocking

- Hold right-click with a weapon to block (right-clicking a block also blocks instead of interacting); release to recover, with a **0.1 s** transition in and out.
- Right-click priority: on a work block (workbench/furnace/chest etc.), if a hostile mob stands between you and that block → block; otherwise use the block normally; other blocks → block.
- Single-wield and two-handed weapons use the **two-handed parry**; genuinely dual-wielded pairs (offhand also holds an arsenal weapon) use the **dual-wield parry**.
- While blocking, sneaking is suppressed and sprinting is disabled; starting a block while sprinting interrupts the sprint immediately.
- Only damage coming from the **frontal 210° arc (105° each side)** is blocked; damage from behind/sides and environmental damage are not.
- A successful block negates the incoming damage; the **parry sound/particles** trigger costs **ceil(10% of the incoming damage)** sharpness (blocks inside the gap cost none), and grants a **0.2 s invulnerability window after releasing right-click**.
- A successful block does **not flash the player red** and emits a **medium-density spark burst** at the weapon (no blaze powder needed; uses the current weapon particle colour and glows when glow ink was forged).
- Block sounds, particles and sharpness cost have a forced **random 0.3–0.8 s gap**, rerolled every time; blocks during the gap still negate damage but play no sound/particles and cost no sharpness.
- Whenever sound/particles actually trigger: the hand and weapon shake by about 2 px for ~0.1 s, the camera shakes slightly, and the player is pushed back about 1/4 block.
- Releasing within **2 s** keeps the combo; blocking longer than 2 s and releasing breaks the combo.
- Slim and wide skins both fit; sleeves and jacket follow the block pose.
- **Blocking with an elytra equipped** folds the wings into the sneaking elytra pose and keeps them attached to the posed torso/shoulders instead of floating apart.
- **Blocking has the highest priority**: starting a block clears the item cooldown and is not gated by attack or weapon-switch cooldowns; left-click attacks/mining are cancelled while blocking.

## Weapons

| Weapon | ID | Speed | Damage | DPS | Jump slam | Block | Falling slash | Elytra slash | Role |
|---|---|---|---|---|---|---|---|---|---|
| Longsword | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | ✓ | ✓ | — | — | One-handed, steady |
| Broadsword | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | ✓ | ✓ | ✓ | ✓ | Two-handed, +0.35 reach |
| Double-Edged Sword | `bettercombat_neonarsenal:twinblade` | 1.4 | 9.2 | 12.9 | ✓ | ✓ | ✓ | ✓ | Two-handed combo |
| Double-Bladed Axe | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | ✓ | ✓ | ✓ | ✓ | Two-handed spin |
| Hengdao | `bettercombat_neonarsenal:hengdao` | 1.1 | 11.0 | 12.1 | ✓ | ✓ | ✓ | ✓ | Two-handed draw slashes |
| Rapier | `bettercombat_neonarsenal:rapier` | 2.5 | 5.0 | 12.5 | ✓ | ✓ | — | — | One-handed fast thrusts |
| Dagger | `bettercombat_neonarsenal:dagger` | 3.2 | 4.0 | 12.8 | — | — | — | — | One-handed short blade, pure damage |
| Spear | `bettercombat_neonarsenal:spear` | 1.2 | 9.2 | 11.0 | ✓ | ✓ | — | — | Two-handed, +1 reach |

## Quick reference

- **Cooldowns follow the weapon**: stored on the item as game time; dropping, swapping or rejoining never resets them.
- **Dual wielding**: forge and sharpness effects trigger only from the main hand; off-hand hits trigger nothing; each hand keeps its own appearance.
- **Grindstone**: clears layers, dye, switches, cooldowns and rarity in one operation; sharpness stays.
- **Tooltip**: shows blade sharpness, colour (colour module when dyed, forge colour when only layers are present) and forge effects; hold Shift for totals and per-layer text.
- **Config**: mod-list sliders — particle density 0–3, blade glow intensity 0–1.
- **JEI**: 176 forging recipes with the anvil as catalyst.
- **Multiplayer**: forging, cooldowns and particle effects stay consistent in multiplayer.

## License

- Code: MIT License ([LICENSE](LICENSE))
- Art assets: CC BY 4.0 ([ASSETS-LICENSE.md](ASSETS-LICENSE.md)); keep the attribution and commercial use is allowed
