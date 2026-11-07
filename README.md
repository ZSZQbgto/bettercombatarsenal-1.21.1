# 更好的战斗：霓虹装备

[中文](#更好的战斗霓虹装备) | [English](#english)

> 极光工业新一代作战套件：6 把武器、会变钝的刀口、4 种锻刃材料、2 个效果开关、任意染料染色。
> 核心工艺由同一套工业铁砧完成，抹除交给砂轮。
>
> **背景**：极光工业（Aurora Industries）推出的新一代作战武器套件。

## 安装

- Minecraft 1.21.1、NeoForge 21.1.265、Java 21
- **必要前置**：Better Combat 2.4.0+、playeranimator、cloth_config
- 可选：JEI（配方查看）、Sodium/Iris（光影兼容）
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
- 铁砧里用蜜脾补，一个 +1000，不花经验。

### 2. 四种锻刃材料

铁砧中武器 + 材料，8 级经验、1:1 消耗；最多 3 层，同层不能重复，每层稀有度 +1。

| 材料 | 随身效果 | 命中效果 | 冷却 |
|---|---|---|---|
| 紫颂果 | 伤害 +10%、移速 +15% | 20 格内敌人拉进攻击范围，僵直 4 秒 | 10 秒 |
| 苦力怕头颅 | 伤害 +5%、攻速 +15% | 无敌苦力怕远离并吸怪，随后爆炸 | 30 秒 |
| 兔子脚 | 攻速 +5%、跳跃高度 +150%、跃斩增伤 +100% | 4 只无敌雷霆大兔子，每 0.5 秒咬 3 个目标各 4 伤害，10 秒后消失 | 30 秒 |
| 回响碎片 | 攻速 -50%、伤害 +100% | 8 格内敌人互相攻击 8 秒 | 30 秒 |

### 3. 两个开关

**发光墨囊**开启刀刃发光与 BC 刀光，**烈焰粉**开启粒子；两者独立，每把武器各只能锻一次。

| 锻入的开关 | 粒子 | BC 刀光 | 刀刃 |
|---|---|---|---|
| 只烈焰粉 | 不发光尘粒 | 原版、不发光 | 不发光 |
| 只发光墨囊 | 无 | 发光、着色 | 发光 |
| 两个都有 | 发光粒子 | 发光、着色 | 发光 |

### 4. 颜色

- 单层锻刃：该层纯色；多层锻刃：HSV 色相插值混合色。
- 颜色同时作用于刀刃光效、粒子、BC 刀光。
- 任意染料 1:1 染色、1 级经验、无限次；每次与当前颜色 50/50 混合，只留最终一色。
- 染料颜色优先于混合色。

### 5. 跳劈

- 先起跳，在最高点及之前挥刀才算；巨刃下劈动作、武器自身音效、击退 V、重置连招。
- 跳劈后攻击冷却的前半段禁止起跳；下落中挥刀不算跳劈。

## 武器

| 武器 | ID | 攻速 | 伤害 | DPS | 定位 |
|---|---|---|---|---|---|
| 长剑 | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | 单手，最稳 |
| 阔剑 | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | 双手，+0.35 距离 |
| 双刃剑 | `bettercombat_neonarsenal:twinblade` | 1.2 | 9.2 | 11 | 双手连段 |
| 双刃斧 | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | 双手旋转斩 |
| 横刀 | `bettercombat_neonarsenal:hengdao` | 1.0 | 11.0 | 11 | 双手拔刀斩 |
| 刺剑 | `bettercombat_neonarsenal:rapier` | 2.2 | 4.6 | 10 | 单手快刺 |

## 工艺速查

- **冷却跟武器走**：存在物品上按游戏时间计算，丢弃、换手、重进都不刷新。
- **双持**：锻刃与锋利度只由主手攻击触发；副手砍人不触发、不消耗；渲染各自独立。
- **砂轮**：一次清除锻刃、染料、开关、冷却、稀有度，保留锋利度。
- **Tooltip**：平时显示刀刃锐度、着色模块、锻刃效果；按住 Shift 看综合性能与每层说明。
- **配置**：模组列表滑条——粒子密度 0–3、刀刃发光强度 0–1。
- **JEI**：132 条锻刃配方，铁砧为催化剂。
- **多人**：锻刃、冷却、粒子判定在服务端，客户端只负责渲染。

## 构建

```
gradlew build
# build/libs/bettercombat_neonarsenal-1.0.0.jar
# 176,776 B · SHA256 56B06816…088B
```

## 许可

- 代码：MIT License（[LICENSE](LICENSE)）
- 美术资源：CC BY 4.0（[ASSETS-LICENSE.md](ASSETS-LICENSE.md)），再分发保留署名即可商用

---

# English

# BetterCombat:NeonArsenal

[中文](#更好的战斗霓虹装备) | [English](#english)

> Aurora Industries' new-generation combat kit: six weapons, edges that wear down, four forge materials, two effect
> switches and unlimited dyeing. Core processes run through one industrial anvil; wipes happen at the grindstone.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.265, Java 21
- **Required**: Better Combat 2.4.0+, playeranimator, cloth_config
- Optional: JEI (recipes), Sodium/Iris (shader compatibility)
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
- Repair at an anvil with honeycomb: +1000 each, no XP.

### 2. Four forge materials

Anvil weapon + material: 8 levels, 1:1, up to 3 layers, no duplicate layer, +1 rarity per layer.

| Material | While held | On hit | Cooldown |
|---|---|---|---|
| Chorus Fruit | +10% damage, +15% move speed | Pull mobs within 20 blocks into range, stun 4 s | 10 s |
| Creeper Head | +5% damage, +15% attack speed | An invulnerable creeper walks off, pulls mobs, then explodes | 30 s |
| Rabbit's Foot | +5% attack speed, +150% jump height, +100% jump-slam damage | Four invulnerable thunder rabbits bite 3 targets for 4 damage every 0.5 s, vanish after 10 s | 30 s |
| Echo Shard | -50% attack speed, +100% damage | Mobs within 8 blocks fight each other for 8 s | 30 s |

### 3. Two switches

**Glow Ink Sac** enables blade glow and the BC trail; **Blaze Powder** enables particles; independent, one forge each per weapon.

| Forged switch | Particles | BC trail | Blade |
|---|---|---|---|
| Blaze only | Non-glowing dust | Original, no glow | No glow |
| Glow ink only | None | Glowing, coloured | Glowing |
| Both | Glowing sparks | Glowing, coloured | Glowing |

### 4. Colour

- One layer: solid colour; several layers: HSV hue-interpolated mixed colour.
- The colour applies to blade glow, particles and the BC trail.
- Any dye, 1:1, 1 level, unlimited; each blends 50/50 with the current colour, keeping one final colour.
- Dye overrides the mixed colour.

### 5. Jump slam

- Jump first, hit at or before the apex; claymore slam animation, the weapon's own sound, Knockback V, combo reset.
- The first half of the attack cooldown blocks jumping; falling swings do not count.

## Weapons

| Weapon | ID | Speed | Damage | DPS | Role |
|---|---|---|---|---|---|
| Longsword | `bettercombat_neonarsenal:sword` | 1.6 | 6.3 | 10 | One-handed, steady |
| Broadsword | `bettercombat_neonarsenal:claymore` | 0.8 | 15.0 | 12 | Two-handed, +0.35 reach |
| Double-Edged Sword | `bettercombat_neonarsenal:twinblade` | 1.2 | 9.2 | 11 | Two-handed combo |
| Double-Bladed Axe | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 20.0 | 12 | Two-handed spin |
| Hengdao | `bettercombat_neonarsenal:hengdao` | 1.0 | 11.0 | 11 | Two-handed draw slashes |
| Rapier | `bettercombat_neonarsenal:rapier` | 2.2 | 4.6 | 10 | One-handed fast thrusts |

## Quick reference

- **Cooldowns follow the weapon**: stored on the item as game time; dropping, swapping or rejoining never resets them.
- **Dual wielding**: forge and sharpness effects trigger only from the main hand; off-hand hits trigger nothing; rendering is per hand.
- **Grindstone**: clears layers, dye, switches, cooldowns and rarity in one operation; sharpness stays.
- **Tooltip**: shows blade sharpness, colour module and forge effects; hold Shift for totals and per-layer text.
- **Config**: mod-list sliders — particle density 0–3, blade glow intensity 0–1.
- **JEI**: 132 forging recipes with the anvil as catalyst.
- **Multiplayer**: forging, cooldowns and particles are server-authoritative; clients only render.

## Build

```
gradlew build
# build/libs/bettercombat_neonarsenal-1.0.0.jar
# 176,776 B · SHA256 56B06816…088B
```

## License

- Code: MIT License ([LICENSE](LICENSE))
- Art assets: CC BY 4.0 ([ASSETS-LICENSE.md](ASSETS-LICENSE.md)); keep the attribution and commercial use is allowed
