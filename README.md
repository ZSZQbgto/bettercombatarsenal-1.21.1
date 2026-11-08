# 更好的战斗：霓虹装备

[中文](#更好的战斗霓虹装备) | [English](#english)

> 极光工业新一代作战套件：7 把武器、会变钝的刀口、4 种锻刃材料、2 个效果开关、任意染料染色。
> 核心工艺由同一套工业铁砧完成，抹除交给砂轮。
>
> **背景**：极光工业（Aurora Industries）推出的新一代作战武器套件。

## 安装

- Minecraft 1.21.1、NeoForge 21.1.265、Java 21
- 开发服务器：`gradlew runServer` 使用独立的 `run-server` 目录，只含服务端必要模组（Better Combat、cloth_config、playeranimator）；`runClient` 继续使用 `run`（含 Sodium/Iris/JEI 等客户端模组）
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

**发光墨囊**开启刀刃发光与 BC 刀光，**烈焰粉**开启粒子；两者独立，每把武器各只能锻一次。未锻发光墨囊时，锻刃层与染料仍会给刀刃上色（不发光）。装有 LambDynamicLights 时，锻入发光墨囊的武器还会发出 **7 级动态光（等同发光地衣）**。

| 锻入的开关 | 粒子 | BC 刀光 | 刀刃 |
|---|---|---|---|
| 只烈焰粉 | 不发光尘粒 | 原版、不发光 | 有色不发光（有锻刃/染料时） |
| 只发光墨囊 | 无 | 发光、着色 | 发光、着色 |
| 两个都有 | 发光粒子 | 发光、着色 | 发光、着色 |

### 4. 颜色

- 单层锻刃：该层纯色；多层锻刃：HSV 色相插值混合色。
- 颜色同时作用于刀刃（含未发光墨囊时的非发光着色）、刀刃光效、粒子、BC 刀光。
- 任意染料 1:1 染色、1 级经验、无限次；每次与当前颜色 50/50 混合，只留最终一色。
- 染料颜色优先于混合色；整条颜色链在 Tooltip 的「着色模块/锻刃颜色」中显示。

### 5. 跳劈

- 起跳后至落地前均可触发（上升与下落都算）；沿用原版空中判定：水中、攀爬、骑乘、失明时不触发。
- 触发时播放巨刃下劈动作、使用武器自身音效、击退 V、重置连招。
- 跳劈后攻击冷却的前半段禁止起跳；落地后清空跳劈窗口。

### 6. 格挡

- 手持武器长按右键进入格挡（右键方块同样进入格挡，不触发方块交互）；松开恢复，进入/退出各有 **0.1 秒**过渡。
- 单持武器与双手武器使用**双手格挡**姿态；真正双持（副手也持本模组武器）使用**双持格挡**姿态（均来自资源包中的一帧动作）。
- 躯干/身体/头部/手部：**只依靠提供的动作文件**（不叠加其它玩家动画）；手臂使用动作文件中的 `right_arm` / `left_arm` 旋转；格挡时通过渲染前 overlay 把 `right_arm_bend` / `left_arm_bend` 的 **+X** 关节角以最高权重叠加到当前手臂 bend 上（修正之前角度反向）；非格挡不写 bend，不影响 BC / FancyPlayerAnimation；不再强制潜行腿/相机姿态；躯干旋转后重新贴合腰部（模型 y=12），头部吸附到躯干顶部并略微下移贴住身体，手臂按躯干完整变换定位；腿部保持原版渲染，身体底部与腿部对齐，不出现间隙。
- 腿部：完全不参与格挡动作，保持原版正常渲染。
- **格挡时屏蔽潜行**：格挡期间强制退出潜行（含清空本地输入 `shiftKeyDown`），右键格挡时不会进入潜行状态；松开后恢复普通潜行输入。
- **格挡期间不能疾跑**；若开始格挡时正在疾跑，会立即中断疾跑。
- **格挡优先级最高**：开始格挡会清除武器的物品冷却，不受攻击冷却与切换武器冷却影响；格挡期间左键攻击/挖掘会被取消，确保格挡动作优先。
- 格挡成功时**不显示受击红闪**。
- 格挡成功时在武器位置迸发**中等密度火星粒子**：无需烈焰粉锻刃即可出现；颜色跟随当前武器的粒子颜色；锻入发光墨囊时火星发光，否则为普通不着火火星。
- **两次格挡之间**的音效与粒子强制间隔 **随机 0.3–0.8 秒**（每次重新随机）；史莱姆等高频攻击生物不会逐 tick 触发；间隔内的格挡仍正常免伤/扣锋利度，但不再播放音效与粒子。
- 每次**触发格挡音效/粒子**时：手部与武器颤动约 2px、持续约 0.1 秒；视角轻微颤动；角色向后平移约 1/4 格。
- 宽窄皮肤适配：读取玩家 `PlayerSkin.Model`，slim 皮肤的手臂枢轴比 wide 高 0.5 模型单位，格挡姿态会补偿该偏移；手中武器位置沿用原版 slim 偏移。
- 皮肤外层同步：袖子与外套在姿态计算后重新同步，避免外层皮肤贴图停在 idle 姿态；裤腿保持原版腿部姿态。
- 格挡判定沿用原版受击路径，但只对**前方 210°（左右各 105°）**内来源的伤害生效；背后/侧面来源与环境伤害不受格挡。每次成功格挡消耗 **本次伤害 ×10% 向上取整**的锋利度，并播放一次格挡音效；格挡期间**免疫被格挡的伤害**，**松开右键后获得 0.2 秒（4 tick）无敌**。
- 格挡 **2 秒内**松开不打断连招；超过 2 秒再松开则打断连招。

## 武器

| 武器 | ID | 攻速 | 伤害 | DPS | 定位 |
|---|---|---|---|---|---|
| 长剑 | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | 单手，最稳 |
| 阔剑 | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | 双手，+0.35 距离 |
| 双刃剑 | `bettercombat_neonarsenal:twinblade` | 1.4 | 9.2 | 12.9 | 双手连段 |
| 双刃斧 | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | 双手旋转斩 |
| 横刀 | `bettercombat_neonarsenal:hengdao` | 1.1 | 11.0 | 12.1 | 双手拔刀斩 |
| 刺剑 | `bettercombat_neonarsenal:rapier` | 2.5 | 5.0 | 12.5 | 单手快刺 |
| 长枪 | `bettercombat_neonarsenal:spear` | 1.2 | 9.2 | 11.0 | 双手，+1 距离 |

## 工艺速查

- **冷却跟武器走**：存在物品上按游戏时间计算，丢弃、换手、重进都不刷新。
- **双持**：锻刃与锋利度只由主手攻击触发；副手砍人不触发、不消耗；渲染各自独立。
- **砂轮**：一次清除锻刃、染料、开关、冷却、稀有度，保留锋利度。
- **Tooltip**：平时显示刀刃锐度、颜色（染色显示为着色模块，仅锻刃层时显示锻刃颜色）、锻刃效果；按住 Shift 看综合性能与每层说明。
- **配置**：模组列表滑条——粒子密度 0–3、刀刃发光强度 0–1。
- **JEI**：154 条锻刃配方，铁砧为催化剂。
- **多人**：锻刃、冷却、粒子判定在服务端，客户端只负责渲染。

## 构建

```
gradlew build
# build/libs/bettercombat_neonarsenal-alpha1.jar
# 239,880 B · SHA256 8E6EA545…3055
```

## 许可

- 代码：MIT License（[LICENSE](LICENSE)）
- 美术资源：CC BY 4.0（[ASSETS-LICENSE.md](ASSETS-LICENSE.md)），再分发保留署名即可商用

---

# English

# BetterCombat:NeonArsenal

[中文](#更好的战斗霓虹装备) | [English](#english)

> Aurora Industries' new-generation combat kit: seven weapons, edges that wear down, four forge materials, two effect
> switches and unlimited dyeing. Core processes run through one industrial anvil; wipes happen at the grindstone.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.265, Java 21
- Dev server: `gradlew runServer` runs from its own `run-server` directory with only the server-side mods (Better Combat, cloth_config, playeranimator); `runClient` keeps using `run` (with Sodium/Iris/JEI etc.)
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

**Glow Ink Sac** enables blade glow and the BC trail; **Blaze Powder** enables particles; independent, one forge each per weapon.

| Forged switch | Particles | BC trail | Blade |
|---|---|---|---|
| Blaze only | Non-glowing dust | Original, no glow | Tinted, no glow (with layers/dye) |
| Glow ink only | None | Glowing, coloured | Glowing, coloured |
| Both | Glowing sparks | Glowing, coloured | Glowing, coloured |

### 4. Colour

- One layer: solid colour; several layers: HSV hue-interpolated mixed colour.
- The colour applies to the blade (including a non-glowing tint before glow ink), blade glow, particles and the BC trail.
- Any dye, 1:1, 1 level, unlimited; each blends 50/50 with the current colour, keeping one final colour.
- Dye overrides the mixed colour; the resulting colour is shown in the tooltip (colour module / forge colour).

### 5. Jump slam

- Any time after jumping and before landing counts (rising or falling), using vanilla airborne rules; in water, on ladders, riding or blinded it does not trigger.
- On trigger: claymore slam animation, the weapon's own sound, Knockback V, combo reset.
- The first half of the attack cooldown blocks jumping; the jump window is cleared on landing.

### 6. Blocking

- Hold right-click with a weapon to block (right-clicking a block also blocks instead of interacting); release to recover, with a **0.1 s** transition in and out.
- Single-wield and two-handed weapons use the **two-handed parry**; genuinely dual-wielded pairs (offhand also holds an arsenal weapon) use the **dual-wield parry** (both supplied one-frame animations).
- Torso/body/head/hands: driven **only by the supplied animation file** (no other player animation is stacked). Arms use the animation file's `right_arm` / `left_arm` rotations; while blocking, a render-time overlay applies the `right_arm_bend` / `left_arm_bend` **+X** joint angle with highest weight over the current arm bend (fixing the previous reversed angle); it writes no bend outside blocking, so normal BC / FancyPlayerAnimation animation is untouched; no forced sneak legs/camera pose. After posing, the torso is re-attached to the waist (model y=12); the head snaps to the torso top and is lowered slightly to touch the body, while arms follow the torso's full transform. Legs keep their vanilla rendering (no forced `model.crouching` and no camera-pose changes).
- Legs: do not take part in the block pose at all; they keep their vanilla rendering.
- **Sneaking is suppressed while blocking**: blocking forces the player out of the sneak state (including clearing the local `shiftKeyDown` input); releasing right-click restores normal sneak input.
- **Sprinting is disabled while blocking**; starting a block while sprinting interrupts the sprint immediately.
- **Blocking has the highest priority**: starting a block clears the item cooldown and is not gated by attack or weapon-switch cooldowns; left-click attacks/mining are cancelled while blocking so the block pose always wins.
- A successful block does **not flash the player red**.
- A successful block emits a **medium-density spark burst** at the weapon: it appears without blaze powder, uses the current weapon particle colour, and glows only when a glow ink sac was forged.
- Between two blocks the sound and particles have a forced **random 0.3–0.8 s gap**, rerolled every time; fast attackers such as slimes cannot retrigger them every tick. Blocks during the gap still negate damage and spend sharpness but do not play sound/particles.
- Whenever the parry sound/particles actually trigger: the hand and weapon shake by about 2 px for ~0.1 s, the camera shakes slightly, and the player is pushed back about 1/4 block.
- Slim/wide skins: reads `PlayerSkin.Model`; slim arm pivots sit 0.5 model units higher than wide, so the block pose compensates that offset (held-item placement keeps vanilla's slim offset).
- Outer skin layers: sleeves and jacket are resynced after posing so the skin texture layers follow the parry; pants keep the vanilla leg pose.
- Blocking follows the vanilla hurt path but only covers damage coming from the **frontal 210° arc (105° each side)**; damage from behind/sides and environmental damage are not blocked. Each successful block costs **ceil(10% of the incoming damage)** sharpness and plays one parry sound; blocked damage is negated, and **after releasing right-click a 0.2 s (4 tick) invulnerability window remains**.
- Releasing within **2 s** keeps the combo; blocking longer than 2 s and releasing breaks the combo.

## Weapons

| Weapon | ID | Speed | Damage | DPS | Role |
|---|---|---|---|---|---|
| Longsword | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | One-handed, steady |
| Broadsword | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | Two-handed, +0.35 reach |
| Double-Edged Sword | `bettercombat_neonarsenal:twinblade` | 1.4 | 9.2 | 12.9 | Two-handed combo |
| Double-Bladed Axe | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | Two-handed spin |
| Hengdao | `bettercombat_neonarsenal:hengdao` | 1.1 | 11.0 | 12.1 | Two-handed draw slashes |
| Rapier | `bettercombat_neonarsenal:rapier` | 2.5 | 5.0 | 12.5 | One-handed fast thrusts |
| Spear | `bettercombat_neonarsenal:spear` | 1.2 | 9.2 | 11.0 | Two-handed, +1 reach |

## Quick reference

- **Cooldowns follow the weapon**: stored on the item as game time; dropping, swapping or rejoining never resets them.
- **Dual wielding**: forge and sharpness effects trigger only from the main hand; off-hand hits trigger nothing; rendering is per hand.
- **Grindstone**: clears layers, dye, switches, cooldowns and rarity in one operation; sharpness stays.
- **Tooltip**: shows blade sharpness, colour (colour module when dyed, forge colour when only layers are present) and forge effects; hold Shift for totals and per-layer text.
- **Config**: mod-list sliders — particle density 0–3, blade glow intensity 0–1.
- **JEI**: 154 forging recipes with the anvil as catalyst.
- **Multiplayer**: forging, cooldowns and particles are server-authoritative; clients only render.

## Build

```
gradlew build
# build/libs/bettercombat_neonarsenal-alpha1.jar
# 239,880 B · SHA256 8E6EA545…3055
```

## License

- Code: MIT License ([LICENSE](LICENSE))
- Art assets: CC BY 4.0 ([ASSETS-LICENSE.md](ASSETS-LICENSE.md)); keep the attribution and commercial use is allowed
