# 锻刃系统规范（BetterCombat:NeonArsenal）

本规范描述锻造（锻刃）系统的数据、流程、颜色混合规则与每种材料的效果结算，供实现、测试与后续扩展使用。所有数值均在 `com.zszq.neonarsenal` 中实现。

## 1. 术语与数据

| 术语 | 说明 |
|---|---|
| 锻刃层（layer） | 一次成功锻造附加到武器上的永久效果记录，最多 6 层 |
| 层 id | `chorus`（紫颂果）、`creeper`（苦力怕头颅）、`rabbit`（兔子脚）、`echo`（回响碎片） |
| 混合色 | 多层锻刃时按 HSV 色相插值（短弧色相平均 + 饱和度/明度平均）得到的刀光/粒子/刀刃光效颜色 |
| 标记效果 | 服务端施加、客户端读取的隐藏效果，用于同步苦力怕闪烁进度 |

数据组件：`bettercombat_neonarsenal:forge`，值为逗号分隔的层 id（按添加顺序），例如 `chorus,echo`。无该组件 = 未锻刃。同种材料第 2 次锻造会出现重复 id（如 `chorus,chorus`），统计用 `Forge.level`、去重用 `Forge.distinctLayers`（颜色与持有效果按材料去重）。

层定义（`item/Forge.java`）：

| 层 id | 名称 | 颜色 | 材料 | 经验 |
|---|---|---|---|---|
| `chorus` | 紫颂 | `#E64CFF`（原版传送紫） | 紫颂果 | 8 级 |
| `creeper` | 苦力怕 | `#4CFF4C`（苦力怕绿） | 苦力怕头颅 | 8 级 |
| `rabbit` | 兔子脚 | `#D2B48C`（兔毛棕） | 兔子脚 | 8 级 |
| `echo` | 回响 | `#0E7C7B`（深暗之域青） | 回响碎片 | 8 级 |

## 2. 通用锻造流程（铁砧）

1. 玩家把武器放入铁砧左槽，材料放入右槽。
2. 服务端在 `AnvilUpdateEvent` 中校验：
   - 左槽物品是本模组武器（`SharpnessWeaponItem`）；
   - 右槽是三种材料之一；
   - `Forge.canAdd(武器, 层)`：当前总层数 < 3 且该材料已锻次数 < 2（同种材料最多锻 2 次，第 2 次开启该材料命中效果）。
3. 校验通过：输出 = 武器副本 + 追加该层；经验花费按材料表（5/8/8）；消耗材料 1 个（`setMaterialCost(1)`）。
4. 校验失败：不设置输出，交给原版铁砧逻辑（不消耗、无提示）。
5. 取走输出后：锋利度、已有层、原有附魔全部保留；层顺序即添加顺序。

边界规则：

- 已满 6 层或该材料已锻 2 次：该材料无效。同种材料允许且仅允许第 2 次，用于开启该材料的命中效果（层 id 可重复，`Forge.level` 统计次数、`Forge.distinctLayers` 供颜色/持有效果去重）。
- 蜜脾与三种材料互斥：蜜脾走锋利度修复（`SharpnessEvents`），三种材料走锻刃（`ForgeAnvil`）。
- 锻刃**不写入真实附魔**（火焰效果在命中时结算），因此武器不会出现附魔光效。
- 锻刃不改变锋利度、不改变武器基础属性。
- 稀有度：按 `layers.size()/2` 提升（每 2 层 +1 阶：0–1 层普通/白，2–3 层罕见/黄，4–5 层稀有/青，6 层史诗/紫），追加层时由 `Forge.add` → `Forge.updateRarity` 写入；砂轮抹除会移除该组件。
- **解锁规则**：只锻入 1 层时仅结算该层的持有效果，命中效果不触发；锻入第 2 层后，全部已锻层的命中效果开启（`Forge.hitEffectsActive` 以 `layers.size() >= 2` 判定）。

## 3. 颜色混合规范

### 3.1 单层

直接使用该层颜色。

### 3.2 多层

按添加顺序做 **HSV 色相插值**：

1. 每层颜色 RGB → HSV；
2. 色相：把各层色相当作单位圆向量（cos/sin）求和，再 `atan2` 取角（自动走色相环短弧）；
3. 饱和度、明度：算术平均；
4. HSV → RGB 作为混合色。

这保证混合结果始终落在色相环上，不会像屏幕混合那样三层直接变白，也不会像 RGB 平均那样发灰。

| 层组合 | 混合结果 |
|---|---|
| 紫颂 | `#E64CFF` |
| 苦力怕 | `#4CFF4C` |
| 兔子脚 | `#D2B48C` |
| 回响 | `#0E7C7B` |
| 紫颂 + 苦力怕 | `#4CB2FF`（青蓝） |

### 3.3 应用范围

混合色统一用于：

1. 刀刃发光叠加层（`WeaponGlow` tint）；
2. Better Combat 自带刀光（`TrailAppearance`：primary = `0xRRGGBBFF`，secondary = `0xRRGGBB99`，通过可选 mixin 覆盖 `SlashParticleUtil.appearanceFor`）；
3. 火花粒子（`GlowSparkParticle` 逐次生成时着色）。

tooltip 的 `锻刃: xxx` 每行仍显示**该层自身颜色**，不显示混合色。未锻刃武器使用配置中的默认颜色（默认白）。

**颜色优先级**：若武器有染料颜色，则刀刃发光、粒子与 BC 刀光都使用染料颜色（优先级高于锻刃混合色；无锻刃层时也生效）。所有锻刃层（紫颂/苦力怕/兔子/回响）的混合色同样统一作用于这三处渲染。

**Tooltip**：默认显示缩略信息（锋利度、颜色、`锻刃效果: 层名（II）`、`按住 Shift 查看…` 提示）；**每 2 层锻刃使物品稀有度 +1 阶**（`layers.size()/2`：0–1 层普通/白，2–3 层罕见/黄，4–5 层稀有/青，6 层史诗/紫；名称颜色随之变化；由 `Forge.updateRarity` 写入 `DataComponents.RARITY`，砂轮清除）；**按住 Shift** 时显示 `— 综合性能 —`（伤害、攻速、移速、跳跃高度、跃斩增伤、锐度伤害系数）与 `— 锻刃效果 —`（各层持有/效果说明；其中**不包含粒子效果相关内容**）。颜色行：有染料显示 `着色模块: #RRGGBB`；仅有锻刃层时显示 `锻刃颜色: #RRGGBB`。

**刀刃发光混合色**：单层锻刃显示该层纯色；**两层及以上**时刀刃发光使用各层颜色的**混合色**（HSV 色相插值，见 3.2），不再做渐变/滚动。

**粒子/刀光/BC 刀光混合色**：单层锻刃用纯色；存在两种及以上锻刃层时，粒子颜色、刀刃发光颜色、Better Combat 刀光颜色统一使用**混合色**（同一颜色；BC 刀光 primary 不透明、secondary 半透明），不再使用渐变。

## 4. 材料效果流程

所有效果均在**服务端**结算，客户端只负责渲染。触发点统一为 `LivingIncomingDamageEvent`（主手武器为本模组武器且包含对应层）。

### 4.1 紫颂果（`chorus`）

1. **持有效果**：增伤 +10%、移速 +15%（`ADD_MULTIPLIED_TOTAL` 属性修饰符，多层相加）；
2. 冷却：200 tick（10 秒），按武器堆叠记录；
2. 命中时若冷却结束：选取玩家 **20 格**内、视线无遮挡、存活的**敌对生物（非 Boss）**；正在被苦力怕锻刃吸引的生物（直到该苦力怕爆炸前）**不受传送影响**；
3. 逐个传送到玩家周围**半径 1.6 格**；
4. 在起点与终点各生成 32 个原版传送门粒子（`ParticleTypes.PORTAL`），并播放原版末影人传送音效；
5. 被传送生物获得 **4 秒（80 tick）僵直**：清除目标、停止寻路；僵直期间其造成的伤害被置 0；
6. 冷却开始计时。

边界：Boss（末影龙/凋灵/最大生命 ≥ 200）排除；玩家自身排除；无视线不传送；苦力怕/其他层效果与紫颂互不影响。

### 4.2 苦力怕头颅（`creeper`）

1. 冷却：600 tick（30 秒），按武器堆叠记录；
2. **持有效果**：增伤 +5%、攻速 +15%；
3. 触发：命中任意生物（含副手限制：仅主手攻击）；
3. 在命中位置生成一只**完全无敌**苦力怕（伤害事件置 0、取消击退、击退抗性拉满），用**原版躲避猫的方式**远离玩家：给它添加与 vanilla `Creeper` 逃跑猫相同的 `AvoidEntityGoal<Player>`（真实寻路、1.0/1.2 速度、半径 24 格以覆盖 4 秒撤退），清空其目标选择器，移动完全交给 AI 寻路（不再直接位移）；每 tick 重新设置 `setInvulnerable(true)` 并回满血，伤害事件强制置 0，任何生物都无法击杀；
4. 行走期间每 tick 强吸引 **6 格**内的**敌对生物**（不含玩家、Boss、本模组召唤物，以及 dummmmmmy 人偶）朝苦力怕靠拢（0.45 格/步）；一旦被吸引即**锁定至爆炸**，即使被推出 6 格范围也不会脱锁（距离 >16 格时加速拉回），并持续清除其目标/寻路；
5. 4 秒后停止行走，开始原版苦力怕 **白色闪烁膨胀** 1.4 秒（28 tick）；
6. 1.4 秒后苦力怕消失并执行自定义爆炸（vanilla `Creeper#explodeCreeper` 已由 mixin 取消，绝不破坏地形）：
   - 不破坏地形（不使用真实爆炸破坏）；
   - 仅伤害 4 格内非 Boss 敌对生物：`伤害 = 20 * max(0.25, 1 - 距离/4 * 0.75)`（半 TNT 强度）；
   - 击退 III（1.5）；原版爆炸粒子/音效；
7. 苦力怕天生无敌，不会被玩家或其他生物提前击杀；引信结束前由模组自行引爆（28 tick < 自然 30 tick）。

### 4.3 兔子脚（`rabbit`）

1. **持有效果**（主手持有该武器时，每 tick 维护）：
   - 攻速 +5%；
   - 跳跃高度 +150%（默认 2.5 倍，跳跃初速度 ×√2.5）；
   - 跳劈（跳跃下砸攻击）伤害 +100%；
   - 放下/切换武器后效果立即移除。
2. **召唤**：命中时若武器冷却就绪（30 秒，按武器堆叠记录）：
   - 在玩家周围召唤 **4 只大型兔子**（`SCALE = 4.0`，约 2 格高），持久化；
   - **完全无敌**：`setInvulnerable(true)` + 每 tick 回满血 + 伤害事件置 0，任何生物都无法杀死；
   - **彼此不攻击**：目标筛选排除所有兔子；也**不会攻击苦力怕锻刃生成的苦力怕**（按标签/状态排除）；
   - **主动寻找敌对生物**：每 tick 搜索 24 格内最近的敌对生物（非 Boss）并移动靠近（移动速度为普通兔子的 2 倍），进入 2.5 格范围后每 **0.5 秒**最多攻击 **3 个**敌对生物、每个 **4 点伤害**（`mobAttack` + 挥手动画）；
   - 生成后 **10 秒（200 tick）** 必定自动消失并生成白色云粒子（`ParticleTypes.CLOUD`）：到期时间写入兔子实体自身的持久化数据，即使区块卸载/重载或内存状态丢失也会按时消失；
   - 冷却开始计时（存于武器）。

### 4.4 回响碎片（`echo`）

1. **持有效果**：攻速 −50%、增伤 +100%；
2. 冷却：600 tick（30 秒），按武器堆叠记录；
3. 触发：命中时以被命中生物为中心，收集 **8 格**内存活、非 Boss、非兔子/非锻刃苦力怕的敌对生物；
3. 目标数量 ≥ 2 才生效并开始冷却，满足条件的生物进入 **8 秒互相残杀**状态：
   - 每秒 tick 让每只暴走生物锁定最近的其它敌对生物（优先同样暴走的目标），设置目标并靠近；
   - 它们会实际互相攻击、互相扣血，直到有人死亡或状态结束；
4. 触发与暴走期间持续散发 **深暗之域色（幽匿灵魂 `ParticleTypes.SCULK_SOUL`）** 粒子；
5. 边界：数量不足 2 不触发、不消耗冷却；Boss、兔子、锻刃苦力怕不参与；与其它锻刃层独立结算。

### 4.5 染料（任意染料）

1. **材料**：任意原版染料（`DyeItem`），铁砧中与武器 **1:1**（消耗 1 个染料、1 级经验）；
2. **结果**：输出武器副本，写入永久染料组件 `bettercombat_neonarsenal:dye`（0xRRGGBB，取染料纹理色）；
3. **无限次 + 颜色叠加**：可在铁砧反复染色，每次新染料与当前颜色 **50/50** 混合，只写入最终的单一颜色值（覆盖旧值）；**不保留历史、不生成列表/嵌套数据**；
4. **不新增锻刃层**：不影响层数上限、稀有度、持有效果与冷却；
5. **颜色作用范围**：染料颜色**覆盖锻刃混合色**，用于刀刃（未锻发光墨囊时为**非发光着色**，锻刃层或染料都生效）、刀刃发光、火花粒子/刀光弧粒子、Better Combat 刀光（primary 不透明、secondary 半透明）；**无锻刃层的武器单独染色同样生效**；
6. **tooltip**：有染料时显示 `着色模块: #RRGGBB`（用该颜色着色）；仅有锻刃层时显示 `锻刃颜色: #RRGGBB`（混合色）；
7. 与蜜脾修复、锻刃材料互斥（按右槽物品类型判定）；染料不影响锋利度。

### 4.6 基础效果开关（发光墨囊 / 烈焰粉）

这两个物品不产生战斗效果，只负责开启渲染：

| 材料 | 消耗 | 开启 |
|---|---|---|
| 发光墨囊（Glow Ink Sac） | 1:1、1 级经验 | 刀刃发光 + BC 刀光颜色覆盖/发光 |
| 烈焰粉（Blaze Powder） | 1:1、1 级经验 | 粒子效果（刀刃火花、BC 刀光弧粒子、移动/待机） |

规则：

1. 两个开关各自独立：
   - **只锻烈焰粉**：粒子存在但**不发光**（使用更大、更清晰、受光照影响的 dust 粒子），刀刃不发光、BC 刀光保持原版外观且不发光；
   - **只锻发光墨囊**：刀刃发光、BC 刀光颜色覆盖并发光，**无粒子**；
   - **两者都有**：发光粒子（glow 火花）+ 发光 BC 刀光 + 发光刀刃。
2. 开关**不占锻刃层数**、不改变稀有度/持有效果/冷却；**每种开关每把武器只能锻一次**，已锻后铁砧不再接受该材料（组件只会是 0/1，不会重复消耗）。
3. **颜色仍由染料/锻刃混合色决定**，开关只决定是否渲染；无颜色来源时使用默认白色。
4. 未锻入发光墨囊：刀刃不发光但会显示锻刃/染料的非发光着色，BC 刀光保持原版颜色且不发光；未锻入烈焰粉：任何粒子都不生成。

### 4.7 砂轮抹除

1. 已锻刃（有锻刃层 / 染料 / 发光开关 / 粒子开关 / 冷却记录任一）的武器**单独**放入砂轮（底部槽为空）时，输出为**抹除全部锻刃**的副本：
   - 移除锻刃层、染料、发光墨囊开关、烈焰粉开关、武器冷却记录；
   - 移除锻造带来的稀有度加成（恢复普通稀有度）；
   - **保留锋利度**（不属于锻刃数据）；
2. 取走输出即完成一次抹除，操作方式与原版砂轮去除附魔一致；
3. 底部槽非空时不介入，交给原版砂轮逻辑；武器通过覆写 `canGrindstoneRepair` 允许放入砂轮输入槽。

## 5. 每把武器的锻刃流程

七把武器的**锻刃流程完全一致**（见第 2 节），差异仅在于武器自身的攻击预设/命中频率，从而影响效果触发频率：

| 武器 | id | Better Combat 预设 | 攻击速度 | 基础 DPS | 攻击距离加成 | 锻刃层上限 | 触发特点 |
|---|---|---|---|---|---|---|---|
| 长剑 | `sword` | `bettercombat:sword` | 1.6 | 10.1 | 0 | 3 | 单手，频率高，冷却覆盖快 |
| 阔剑 | `claymore` | `bettercombat:claymore` | 0.8 | 12.0 | +0.35 | 3 | 双手，范围大，单次伤害高 |
| 双刃剑 | `twinblade` | `bettercombat:twin_blade` | 1.4 | 12.9 | 0 | 3 | 双手交替连段 |
| 双刃斧 | `doubleaxe` | `bettercombat:double_axe` | 0.6 | 12.0 | 0 | 3 | 双手旋转斩，命中面广 |
| 横刀 | `hengdao` | `bettercombat:katana` | 1.1 | 12.1 | 0 | 3 | 双手拔刀斩 |
| 刺剑 | `rapier` | `bettercombat:rapier` | 2.5 | 12.5 | 0 | 3 | 单手刺击，触发最频繁 |
| 长枪 | `spear` | `bettercombat:spear` | 1.2 | 11.0 | +1 | 3 | 双手长刺，距离远 |

说明：

- 颜色与效果只由**层组合**决定，与武器种类无关；
- 每把武器独立保存自己的层（同一物品类型的不同堆叠也各自独立）；
- 同一武器最多 6 层，层组合不分职业/预设。

## 6. 组合、优先级与交互

### 6.1 叠加规则（全部效果同时生效、分别计算）

1. **解锁规则**：每种材料锻入 1 次只结算其持有效果；同种材料锻入第 2 次后该材料的命中效果才开启（`Forge.hitEffectActive` 以该材料层数 ≥2 判定）。开启后每次命中遍历全部已锻材料，每种材料独立判断触发条件与自身冷却；某一材料不满足只跳过自身，不影响其他材料。
2. **持有效果**（主手武器，按层相加后以 `ADD_MULTIPLIED_TOTAL` 应用）：紫颂果 增伤+10%/移速+15%；苦力怕头颅 增伤+5%/攻速+15%；兔子脚 攻速+5%/跳跃高度+150%（默认2.5倍）/跳劈增伤+100%；回响碎片 攻速−50%/增伤+100%。
3. 冷却**按效果独立**，并记录在**武器堆叠**的数据组件中（absolute game time）：紫颂果 10 秒；苦力怕头颅 30 秒；兔子脚 30 秒；回响碎片 30 秒。丢弃、切换物品、交换主副手、退出重进都不会重置冷却。
3. 效果执行相互隔离：每个效果包在独立异常处理中，某效果出错只会记录日志，不会阻止其他效果结算。
4. 同一目标可同时处于「被点燃 + 白色闪烁 + 被传送/僵直」状态；伤害、击退、传送、僵直分别独立结算、互不覆盖。
5. 苦力怕标记目标在引信结束前死亡时，立即在**死亡位置**爆炸（不会因为目标死亡而失效）。
6. 结算顺序为 **火焰点燃 → 苦力怕标记 → 紫颂传送**，但顺序不影响最终结果；冷却各自开始计时。
7. 跳劈击退（击退 I）与苦力怕爆炸击退（击退 III）分别结算，不互相覆盖。
8. **双持限制**：只有主手武器的锻刃层参与结算。副手攻击（Better Combat 判定为 off-hand）不触发任何锻刃效果，也不消耗/缩放主手锋利度；主手攻击不受副手武器影响。渲染与结算分离：刀光、火花粒子、刀刃光效按各自手持武器分别渲染（副手武器用副手自己的颜色，不套用主手）。
2. 锋利度：锻刃不改变锋利度；命中消耗 1 点锋利度（耐久附魔按原版公式降低损耗率：等级 L 时消耗概率 1/(L+1)）；刀光亮度与粒子密度 = 锋利度系数（每损失 10% −20%，≥50% 归零）× 锻刃颜色（颜色只改色相，不改变亮度系数）。
3. 伤害：锋利度倍率作用于事件伤害（0.75–1.0）；锻刃本身不额外加成伤害（苦力怕爆炸为独立伤害）。
4. 跳劈击退（击退 I）与苦力怕爆炸击退（击退 III）互不叠加，各自结算。

## 6.2 客户端设置（模组列表滑条）

模组列表 → Better Combat Arsenal → 配置：

- `particle_density`（粒子浓度，0–3，默认 1）：全局乘算所有火花/锻刃粒子数量；
- `blade_glow_intensity`（刀刃发光强度，0–1，默认 1）：全局乘算刀刃发光叠加层亮度。

两项与 `config/bettercombat_neonarsenal/weapon_glow.json` 的细分配置相乘生效。模组列表的「配置」按钮通过自定义配置界面（`IConfigScreenFactory`）打开，包含两个可拖动滑条，关闭时保存。

## 7. 兼容与回退

- **多人游戏**：锻刃、冷却、粒子判定均为服务端结算（组件同步），客户端只负责渲染；专用服务器已通过启动验证。

- **JEI**：安装后提供「锻刃」配方分类（208 条：锻刃层首层与第二层、基础开关、染料，每把武器 26 条），铁砧为催化剂；未安装时该插件不会被加载。武器最多 6 层，JEI 只展示第 1/2 次锻造的示例，第 3–6 层可继续在铁砧中锻造。

- **Better Combat 为必需前置**（2.4.0+，需 playeranimator、cloth_config）：缺失时模组不会加载。
- 光影/Iris：刀刃发光沿用既有 Iris 适配路径；锻刃颜色仍生效。
- 旧存档：无 `forge` 组件的武器视为未锻刃；已满 6 层的旧武器不可继续锻造。
- 无指令入口：锻造只能通过铁砧进行（锋利度指令已移除）。

## 8. 验收用例

- [ ] 紫颂果锻刃：经验 8、20 格内敌对生物被拉到身边、末影人粒子/音效、僵直 4 秒、冷却 10 秒。
- [ ] 苦力怕头颅锻刃：经验 8、目标自身膨胀白色闪烁、吸引 10 格、1.4 秒后爆炸、无地形破坏、半 TNT 伤害、击退 III、冷却 8 秒。
- [ ] 兔子脚锻刃：持有攻速 +30%/移速 +20%，命中召唤 4 只 2 格高兔子（完全无敌、彼此不攻击、不攻击锻刃苦力怕、移速 2 倍）、主动索敌、每 0.5 秒最多 3 个目标各 4 伤害、10 秒白色粒子消失、30 秒武器冷却。
- [ ] 冷却绑定武器：丢弃/切换/重进后剩余冷却仍在（按游戏时间）。
- [ ] 回响碎片锻刃：命中后 8 格内 ≥2 敌对生物互相残杀 8 秒、深暗之域粒子、30 秒武器冷却。
- [ ] 各层颜色：单层与混合结果符合第 3 节表格（刀刃光效、BC 刀光、粒子一致）。
- [ ] 满 6 层与重复层材料无效；锋利度与已有层保留。
- [ ] 锋利度 ≤1500 时刀光与粒子完全消失，修复后恢复。


## 8. 格挡（Blocking）

- 手持任意本模组武器长按右键进入格挡；`SharpnessWeaponItem#use` 开始物品使用，松开即停止。
- 格挡动画的两套变体：`two_handed_parry.json` / `dual_wield_parry.json` 各包含两个 animation 键——`parry`/`dualwieldparry`（FPA 存在时使用）与 `parrywithoutfpa`/`dualwieldparrywithoutfpa`（FPA 不存在时使用）。`BlockPose.load(file, animationKey)` 按 `FpaBend.available()` 选择：装 FPA 时用前者，`applyRenderBends` 通过 `FpaBend` 把 `right_arm_bend`/`left_arm_bend`（肘，肩下 4 模型单位）和 `left_leg_bend`（膝，胯下 6 模型单位）交给 FPA 的 bend 实现，袖子/裤腿外层用同一角度；不装 FPA 时用后者：肩部旋转已补偿、`*_bend` 为 0，且 `FpaBend` 无 `BendHooks` 时直接返回，因此保持直臂直腿，不做任何网格弯曲。
- 动画：客户端 `BlockPose` 读取 `assets/bettercombat_neonarsenal/animations/two_handed_parry.json` / `dual_wield_parry.json`（已同步 resourcepacks 最新版本），在 `PlayerModel#setupAnim` 末尾混合；进入/退出各 0.1 秒。单持武器与双手武器使用双手姿态；仅当副手也持本模组武器（真正双持）时使用双持姿态。slim 皮肤补偿手臂枢轴 0.5 单位偏移；躯干/身体/头部/手部只使用动作文件（不叠加其它动画）；手臂应用动作文件的 `right_arm/left_arm` 旋转；格挡时在 `AgeableListModel.renderToBuffer` 前由 `BlockBendRenderMixin` 调用 `FpaBend.blend`，读取 `right_arm_bend/left_arm_bend` 第一分量（**正 X**，修正先前反向）作为关节角，以进度作为权重叠加到当前 bend；非格挡不写 bend；躯干旋转后重新贴合腰部（y=12），头部吸附到躯干顶部并下移 0.5 单位贴住身体；格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；腿部按同一 progress 插值并同步 pants；格挡静止时腿部使用动画文件 `right_leg/left_leg`，腿根 z 从文件 +2 收回 0 与 body 接洽；移动时保持原版行走腿；静止时全身在接地偏移基础上再上抬 1 个模型单位（约 1 像素）；不强制 crouch、不改相机；，手臂按躯干旋转 + 最终位移的完整变换定位，避免身体、头手与腿的间隙；腿部完全不参与格挡、保持原版；外层皮肤部件（袖子、外套、裤腿与头部外层 `hat`）在姿态与接地偏移之后重新 `copyFrom` 同步——头部外层此前漏同步，导致格挡时外层皮肤（含 3D 皮肤层 compatibilityMode 注入到 `hat` 的 3D 网格）停在原地，现已修正；格挡时屏蔽潜行：格挡期间每 tick 强制 `setShiftKeyDown(false)` 且客户端 setPose(STANDING)（服务端/客户端），相机/眼高保持站立，松开后恢复普通潜行输入。格挡期间禁止疾跑，正在疾跑会立即中断；同时屏蔽潜行。格挡优先级最高：`use` 清除物品冷却，不受攻击/切换武器冷却影响；格挡时取消左键攻击与挖掘事件。格挡判定沿用原版受击路径但限定前方 210°（±105°）：每次该范围内会生效的伤害事件触发一次成功格挡，消耗 ceil(伤害×10%) 锋利度、播放音效、格挡期间取消所有伤害（持续无敌）；松开格挡结束后额外给予 4 tick（0.2 秒）无敌；清除受击红闪、在武器位置发送 14 颗火星粒子（无需烈焰粉；颜色取 `Forge.displayColor`，锻入发光墨囊时粒子 full-bright 发光，否则使用世界光照）；音效与粒子按玩家持久数据记录下一次可用时间，两次之间强制随机 0.3–0.8 秒间隔（6–16 tick，每次重掷），避免史莱姆等高频率攻击逐 tick 触发。无额外击退抗性属性；环境伤害与背后/侧面伤害不受格挡。姿态 mixin 使用较低优先级，确保在其它玩家动画的 TAIL 注入之后应用，手部动作为**加法叠加**（不替换其它动作），格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；腿部按同一 progress 插值并同步 pants；格挡静止时腿部使用动画文件 `right_leg/left_leg`，腿根 z 从文件 +2 收回 0 与 body 接洽；移动时保持原版行走腿；静止时全身在接地偏移基础上再上抬 1 个模型单位（约 1 像素）；不强制 crouch、不改相机；渲染。
- 第三人称攻击姿态：`ThirdPersonAttackPose`（由 `NeonArsenalItemRenderer` 在 `THIRD_PERSON_RIGHT_HAND` 上下文调用，实体由 `ItemInHandLayerContextMixin` 在 `ItemInHandLayer#render` HEAD/RETURN 捕获，支持联机其他玩家）读取 `assets/bettercombat_neonarsenal/models/item/<id>_tppattack.json`（从 `resourcepacks/bctppitemholding` 提取的 `thirdperson_righthand` 攻击放置；仅 7 把差异武器，匕首无差异）。触发条件：`AttackAnimationTracker.isAnimating(player)`（所有玩家的 BC 攻击动画）或本地玩家 `FallSlamAnimation.isControlling()`/`ChargeThrustAnimation.isControlling()`（手动动画冻结时长超过 tracker 时仍保持）；用 `Centre⁻¹ · Normal⁻¹ · Attack · Centre` 把现有静置放置替换为攻击放置（与原版 display 后 `translate(-0.5,-0.5,-0.5)` 居中偏移共轭），权重 **0.10 s 淡入 / 0.15 s 淡出**，只处理右手上下文（包内无左手攻击数据，左手/副手保持静置）；修正矩阵按 itemId 缓存，模型重载/断线时清空。
- 第一人称格挡：`FirstPersonParryPose`（由 `NeonArsenalItemRenderer` 在第一人称上下文调用）读取 `assets/bettercombat_neonarsenal/models/item/<id>_fpparry.json`（来自 `resourcepacks/firstpersonparry`，只包含 `firstperson_righthand`/`firstperson_lefthand` 的 display 变换）。格挡时按 `BlockPose.isBlocking(player)` 且渲染的 `ItemStack` 与 `player.getUseItem()` 相同来判定；**双持格挡**时（`BlockPose.isDualWieldBlock(player)`：主手为可格挡单体武器且副手也是本模组武器），若渲染的是 `player.getOffhandItem()`，副手也进入格挡姿态判定，并使用其 `_fpparry` 的 `firstperson_lefthand` 变换（与主手共用同一 `updateProgress` 进度与 smoothstep，0.18 秒）；仅当 `_fpparry` 的左手变换与普通左手变换不同（当前为长剑/刺剑）时才应用，避免给未设计副手格挡姿态的武器（双手武器等）错误叠加 `VIEW_LIFT` 上抬。第三人称双持姿态已由 `dual_wield_parry` 的 `right_arm/left_arm/right_arm_bend/left_arm_bend` 驱动，无需额外代码；通过 `inverse(正常手持变换) × 格挡变换` 把第一人称手部矩阵替换成格挡姿态，**0.18 秒**线性进度 + smoothstep 缓动淡入淡出（进出两端速度为 0，不跳变；左撇子用 `firstperson_lefthand`，`leftHand` 标志与 `ItemRenderer` 一致）；替换时必须把原版物品渲染在 display 变换后追加的 `translate(-0.5,-0.5,-0.5)` 居中偏移共轭消掉（否则修正矩阵会作用在被旋转过的坐标系里，把武器推到屏幕左下角外），即修正矩阵为 `Centre⁻¹ · Normal⁻¹ · Lift · Parry · Centre`；作者的第一人称 display 变换是按 BlockBench 预览锚点调的，因此再在视图空间额外上抬 `VIEW_LIFT = 0.22` 格（1 格 = 16 显示单位）后与正常持握插值，使格挡刀身出现在视野下半部；未提供 `_fpparry` 模型的武器（如匕首）保持原版第一人称持握。`_fpparry` 模型由 `NeonArsenalClient#registerAdditionalModels` 按约定注册。第一人称成功格挡的迸发粒子：`ClientLevelParrySoundMixin` 收到 parry 音效时，若本地玩家正在格挡、处于第一人称且音效距离自身 ≤3 格，由 `FirstPersonBlockBurst` 在相机前方偏右下（近似持刀位置）按现有粒子规则迸发 14 颗 `BlockSparkOptions`（颜色取 `Forge.displayColor`、锻入发光墨囊时 full-bright，与第三人称世界坐标迸发一致）；世界坐标迸发仍由服务端 `BlockHandler` 发送给所有附近玩家。
- 判定：服务端 `LivingIncomingDamageEvent` 中若玩家正在使用本模组武器，则取消本次攻击伤害，调用 `Sharpness.consume` 消耗 1 点，并播放 `bettercombat_neonarsenal:parry`；播放时客户端通过监听本地 parry 音效触发手部/武器 2px、0.1s 颤动与视角轻微颤动（不再使用自定义网络包），服务端给玩家一个约 1/4 格的后退推力；取消伤害的同时给予 4 tick 受击无敌，避免同一攻击逐 tick 重复触发音效。
- 连招：`PlayerTickEvent` 记录格挡开始时间；松开时若持续 >40 tick（2 秒）则通过反射调用 `PlayerAttackProperties.setComboCount(player, -1)` 打断连招，≤40 tick 保持连招。

格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；格挡静止时腿部使用动画文件 right_leg/left_leg 并把腿根 z 从 +2 收到 0 与 body 接洽，移动时保持原版行走腿。

右键优先级：工作方块（getMenuProvider != null）且玩家与方块之间（水平线段投影 t∈[0,1]、横向距离≤1.5、垂直≤2.5）存在 Enemy → 格挡；否则工作方块正常交互；非工作方块格挡。

跳劈：起跳后至落地前触发（整个跳跃窗口，保证阔剑第三段下劈动画在挥砍开始时即播放），下落高度 **≤5 格**（超过 5 格归下落斩击或普通攻击）。Better Combat 在客户端立即开始挥砍并播放动画，但攻击请求要等挥摆结束才发到服务端；服务端按客户端上报的跳劈动画选择同一套跳劈攻击，避免“客户端播下劈动画、服务端结算普通攻击”导致伤害与音效和动画不同步。创造飞行不触发；下落>5格时首次攻击输入被 FallSlamClient 拦截，距地面<=4格自动 startAttack；FallSlamHandler 对命中伤害乘 1+fallDistance*0.05，落地时烈焰粉锻刃迸发 80 颗粒子并播放 parry 音效。

下落斩击需在本次下落中按住攻击键（长按 0.15 秒）；**下落高度始终从本次下落起点累计**（客户端直接使用原版 `fallDistance`，不再使用 `fallStartY`/`afterRelease`），中途松开只取消当前蓄力、不重置已累计高度，再次按住即按同一次下落继续判定。风声音效在本次下落期间持续播放：取消/重按斩击或完成斩击都不会中断或重启风声，只在落地/入水/缓降/滑翔等真正结束下落时停止。BC 攻击请求到达时 FallSlamHandler.markSlam；LivingFallEvent：有 slam 标记时取消原版摔落，<10 格免伤，>10 格按 `floor((d-10)/3)` 扣血（每多 3 格 1 点）并 clamp 到 1 HP；扣血量先乘原版 `FALL_DAMAGE_MULTIPLIER` 属性，再用原版摔落伤害源查询 `EnchantmentHelper.getDamageProtection`（摔落保护/保护等 `damage_protection` 附魔）并套 `CombatRules.getDamageAfterMagicAbsorb`，与原版摔落一致（护甲不参与，因 fall 伤害类型在 `bypasses_armor` 中）；最终未触发下落斩击的普通下落按原版摔落结算。注意服务端可能先标记落地、`LivingFallEvent` 晚 1~2 tick 才到，PENDING 会保留到事件到达或超时，落地伤害只结算一次。

格挡姿态高度：移动格挡保持原版身体高度；静止格挡把 body/head/arms/legs/sleeves/jacket/pants 整体下移 `STATIONARY_DROP = 1` 个模型单位（约 1px），只比移动格挡略低，避免停下时明显下沉。`player.isPassenger()`（骑马/坐船/坐矿车等）时跳过动画文件的站姿腿（下半身保持原版坐姿）并跳过全部垂直偏移，整体保持乘坐高度，仅上半身进入格挡姿态；潜行过渡的腿部插值在骑乘时也跳过。

修复：等待期间 FallSlamState.waitingForSlam=true，BC getCurrentAttack 返回 null，避免按住攻击键每 tick 重播跳劈；下落高度改用原版 `fallDistance`（从本次下落起点累计），取消/重按不重置。

修复重复跳劈：ClientTickEvent.Pre 中提前更新 FallSlamState.waitingForSlam，确保 BC 在玩家 tick 查询攻击时已看到等待标记；自动触发后 triggered 期间继续抑制，直到落地/松手。

重复跳劈修复：ClientPlayerAttackAnimationMixin 拦截 BC AbstractClientPlayer.playAttackAnimation；FallSlamAnimation 手动启动 bettercombat:two_handed_slam，tick 到 endTick/3 后把 attackAnimation.speed.speed 设为 0 冻结；松手 stopAttackAnimation(0.2) 淡出；4 格时按剩余下落时间设置 speed 恢复，使动画落地前完成。

长按判定：FallSlamClient pressTicks>=3 才 longPress，短于 3 tick 松开走 shortTapAttack（allowNormalAttack 让 BC 选普通攻击）；下落期间 startAttack/playAttackAnimation 全过滤；冻结时清空 TransmissionSpeedModifier.gears 并 speed=0，避免 setupAnim 用 gears 覆盖。

底层分析：BC 持键攻击走 Minecraft.continueAttack(Z)（mixin pre_handleBlockBreaking），点击走 startAttack(Z)（pre_doAttack）；动画由 MinecraftClientInject.startUpswing 与 ClientNetwork.handleAttackAnimation 直接调用 playAttackAnimation；TransmissionSpeedModifier.setupAnim 每帧用 gears 覆盖 speed。修复：preTick 建立 pending/longPress；MinecraftContinueAttackMixin 取消 continueAttack；startAttack 检查 waiting；playAttackAnimation 过滤；FallSlamAnimation 冻结时 speed=0 且 gears 清空，1/3 暂停，4格恢复。

自动触发修复：MinecraftFallSlamMixin 仅在不处于 triggeringAttack 时拦 startAttack，确保 4 格自动触发能真正发出攻击；此前 waitingForSlam 连自动触发也拦掉，导致无伤害/无落地逻辑。

语义修正：长按后不屏蔽 BC 连招/音效/刀光（blockAttacks=false），只过滤可见动画；动作冻结在 1/3，4 格时 resumeOriginal 原速恢复；落地 lockTicks=6（0.3s），期间 blockAttacks/attackLock=true，结束后释放；不再用自动 startAttack 触发。

最终语义：长按后 blockAttacks=true，continueAttack/startAttack/playAttackAnimation 全部过滤；1/3 冻结；4 格 resumeOriginal(speed=1) 且 committed=true；committed 后 key release 不取消；落地 lockTicks=6，结束后清 waiting/block。

落地过渡：FallSlamClient 在 onGround 时把 BC 动画 currentTick 设为 endTick（最终帧），再 stopAttackAnimation(0.3) 淡出到 idle；0.3s 锁定内 blockAttacks=true，结束后释放。

修复连招未屏蔽：FallSlamClient.tick 的 lockTicks==0 分支之前每 tick 清空 blockAttacks/waitingForSlam，导致长按期间 BC 连招恢复；现在仅 pending/longPress 都结束时才清空，否则保持 blockAttacks=waiting=true。

最终连招语义：长按后 blockAttacks=false（continueAttack/startAttack 放行，BC 音效/刀光/攻击继续），waitingForSlam=true（仅 playAttackAnimation 过滤，可见动画保持手动 1/3 动画）；4 格 resumeOriginal；落地 finishAndFade(0.3)+lockTicks=6。

动画修复：不再手动调用 playAttackAnimation（参数/资源可能失败导致无动画），改为 allowFirstAnimation 放行 BC 自身第一次跳劈动画，ClientPlayerAttackAnimationMixin 调用 FallSlamAnimation.onExternalStart，tickPause 冻在 1/3；后续 BC 动画过滤，音效/刀光/攻击继续。

动画启动修复：FallSlamAnimation.onExternalStart 手动调用 BC playAttackAnimation("bettercombat:two_handed_slam", TWO_HANDED, length=玩家攻击冷却 tick 数, upswing=0.5)；length 不是秒，之前传 1 导致动画瞬间播完/不可见。ClientPlayerAttackAnimationMixin 只放行该手动调用，BC 自身动画、攻击、音效、刀光全部过滤；1/3 冻结、4 格 resumeOriginal(speed=1)、落地 finishAndFade(0.3)。

硬屏蔽：长按开始发送 FallSlamWindupPayload(true)，服务端 FallSlamHandler.setWindup 调用 BC CombatFlags.setAttacksDisabled(player,true)；落地/取消发 false。客户端 blockAttacks=true，startAttack/continueAttack/playAttackAnimation 全过滤；只有 onExternalStart 手动启动的动画放行。

硬屏蔽补充：ClientNetworkFallSlamMixin 在 waitingForSlam 时取消 BC handleAttackAnimation 与 handleAttackSound；加上 MinecraftFallSlamMixin/MinecraftContinueAttackMixin 与 CombatFlags，形成客户端+服务端全链路屏蔽。仅 FallSlamAnimation.onExternalStart 手动动画不受影响。

附魔支持：arsenal 武器不可损坏，原版 `Item.isEnchantable` 依赖 MAX_DAMAGE 组件，因此 `SharpnessWeaponItem.isEnchantable` 恒真。天然可附魔列表由 `WeaponEnchantments` 按武器 id 白名单给出（剑类：锋利/亡灵杀手/节肢杀手/击退/抢夺/火焰附加/耐久/经验修补/横扫；阔剑、双刃斧、长枪不含横扫），通过 NeoForge `IItemExtension.supportsEnchantment`（铁砧与附魔书）和 `isPrimaryItemFor`（附魔台候选）生效；三种伤害附魔继续走原版 `exclusive_set/damage` 互斥。致密/破甲/穿刺不在任何武器白名单内，附魔台与铁砧无法获得；`EnchantCommandArsenalMixin` 用 `@Redirect` 在 `/enchant` 命令里跳过 `supportsEnchantment`（仅 arsenal 武器），因此命令可强制附上，原版排斥仍由命令自身的 `isEnchantmentCompatible` 保证。Tooltip：附魔**只列名称**，其数值并入综合百分比——`EnchantmentTips.damageFraction` 以武器显示攻击伤害（玩家基础 1 + 主手 `ATTACK_DAMAGE` 修饰符）为分母，把锋利（首级 +1、之后每级 +0.5）、亡灵/节肢（2.5×等级）、火焰附加（4×等级）求和后一次性折算并与锻刃伤害百分比相加；`EnchantmentTips.attackSpeedFraction` 读取 `EnchantmentEffectComponents.ATTRIBUTES` 中的 `ATTACK_SPEED` 修饰符（`ADD_VALUE` 除以 4.0 基础攻速，`ADD_MULTIPLIED_BASE/TOTAL` 直接作为比例）并与锻刃攻速百分比相加；Shift 视图同样只列附魔名称。**耐久（Unbreaking）**：武器无耐久，改为按原版耐久公式降低锋利度损耗率——`SharpnessEvents` 在命中/挖掘时调用 `Sharpness.rollConsume`，等级 L 时每次使用以 1/(L+1) 概率消耗 1 点（I 50%、II 33.3%、III 25%），期望损耗率为 1/(L+1)。原版附魔名称行在 `ItemTooltipEvent` 中从 arsenal 武器 tooltip 移除，避免与自定义明细重复；`SharpnessWeaponItem.isFoil` 恒 false，附魔武器不显示原版附魔光效。

持刀动作：单持长剑/阔剑/双刃剑/双刃斧/横刀/刺剑/长枪时，`HoldPose` 读取 `assets/bettercombat_neonarsenal/animations/weapon_holding_action_from_behind.json`（geckolib 格式：`right_arm` 旋转、`right_arm_bend` 肘弯 -22.5°），在 `PlayerModel#setupAnim` TAIL（`PlayerBlockPoseMixin`）按 0.1s 权重混合到主手手臂（左撇子镜像到左手），肘弯经 `FpaBend` 在 `AgeableListModel#renderToBuffer` 前施加；**武器位置与朝向沿用原版第三人称手持渲染**（不读取 `right_item`，不替换原版手部链）。权重按 0.1s 混合，格挡/出招时淡出；下落斩击与冲锋前刺的主动画控制期间（`FallSlamAnimation.isControlling()`/`FallSlamState.blockAttacks`、`ChargeThrustClient.isCharging()`，仅对本地玩家）也保持让位，避免冻结动画超过 `AttackAnimationTracker` 计时后持刀姿势在斩击中途回弹。移动摆动叠加在手臂 xRot：行走中轴 +5°、扇面 4°，疾跑中轴 +10°、扇面 8°，静止 0°，用 `sin(limbSwing*0.6662)` 振荡并按 8/s 平滑中轴与扇面。招式过渡：`PoseForPlayerHoldPoseMixin` 把 BC `poseForPlayer` 的主手 pose 置空（副手保留原 pose），攻击动画由 BC 播放，期间 `AttackAnimationTracker`（在 `ClientPlayerAttackAnimationMixin` 的 play/stop 中记录）让持刀姿势淡出、收招后淡入；格挡姿态优先，格挡期间持刀姿势淡出且不施加。双持时副手手臂与副手物品完全不处理。附魔流光：锻入发光墨囊后 `WeaponGlow` 不再用单色 putBulkData，而是逐顶点颜色——按 overlay 模型（刀刃遮罩）最长轴归一化得到刀刃位置，颜色相位 = 位置×1.4 周期 + 时间×0.45 周期/秒，色相在武器当前颜色环（染料/锻刃混色 RGB→HSV）**±12°** 附近波动（第二谐波权重 0.25，颜色变化域收窄），并叠加亮度波动使未染色刀刃也有明暗流光；additive/translucent/deferred/emissive 四种渲染路径与 Iris 第一人称延迟 pass 共用同一发光函数（NEW_ENTITY 格式补 overlay/normal，其余补 lightmap）。

## 9. 骑乘起伏（RiderBob）

骑乘**活体坐骑**（马、驴、骡、骆驼、羊驼、猪、炽足兽等）时，渲染中的玩家角色会随坐骑步态轻微上下起伏：`PlayerRendererRiderBobMixin` 在 `PlayerRenderer#render` 开头对 `PoseStack` 做世界空间竖直平移 `translate(0, bob, 0)`，`RiderBob` 取坐骑 `walkAnimation.speed(partialTicks)`（0–1 并钳制）与 `walkAnimation.position(partialTicks)`，`bob = sin(position × 0.6662 × 2) × 0.08 × speed`（单位格，约 1.3 像素峰值，频率为步态两倍，走/跑越快越明显）；平移发生在模型、手持物与各图层之前，因此角色、武器、披风/鞘翅会整体一起起伏，不会与坐骑分离。船、矿车等非生物载具不应用；创造旁观者不应用。原版骑乘位置（含地形与跳跃）本来已跟随坐骑，这里只补坐骑动画本身没有的躯干起伏。幅度/频率常量在 `RiderBob`（`AMPLITUDE`、`FREQUENCY`）。

## 10. 冲锋前刺（Charge Thrust）

- 技能声明：`WeaponDefinition.Ability.CHARGE_THRUST`，仅长枪（`spear`）拥有；条件为主手持该武器且（`player.getVehicle() instanceof LivingEntity` 骑乘活体坐骑 或 `player.isFallFlying()` 鞘翅滑行），且未在格挡/旁观/创造飞行/死亡。
- 客户端 `ChargeThrustClient`（`ClientTickEvent.Pre/Post`，由 `NeonArsenalClient` 注册）：检测左键按下；按下后立即置 `ChargeThrustState.blockAttacks = true` 并用 `MinecraftContinueAttackMixin` / `FallSlamBetterCombatGateMixin`（同时检查 `FallSlamState` 与 `ChargeThrustState`）拦截 Better Combat 的 startUpswing/performAttack/continueAttack，避免普通挥砍插入；同时 `FallSlamAnimation.clearBetterCombatSwing()` 清掉已排队的挥砍。
  - **短点击**（按住不足 3 tick = 0.15s）：恢复过滤状态并通过 `MinecraftAttackAccessor` 调一次 `Minecraft#startAttack`，让 Better Combat 正常执行一次长枪前刺。
  - **长按**（≥3 tick）：`ChargeThrustAnimation.onExternalStart` 直接调用 `PlayerAttackAnimatable#playAttackAnimation("bettercombat:two_handed_stab_right", AnimatedHand.TWO_HANDED, 冷却 tick 数, 0.5)`，并以 `tickPause` 在 `(endTick+2)/3`（即 1/3 处）把该动画层速度置 0 冻结；同时发送 `ChargeThrustWindupPayload(true)` 让服务端关闭 Better Combat 攻击通道（`CombatFlags.setAttacksDisabled`）。
  - **松开**：`ChargeThrustAnimation.resumeOriginal` 以原速继续整段动画并发送 `ChargeThrustStrikePayload(true)`；`isFinished` 判定到最后关键帧后 `finishAndFade(6 tick)` 淡出，随后 6 tick 攻击锁再恢复过滤。中途失去条件（下马、落地、收鞘翅、切武器、死亡等）走 `abort`：0.2 秒淡出并发送 windup(false) 恢复。
  - 动画层保护沿用 `FallSlamStopAnimationMixin`：本地玩家手动播放的动画只允许 `ChargeThrustAnimation` 自己停止；`ClientPlayerAttackAnimationMixin` 额外放行 `ChargeThrustState.allowAnimation` 并拦截蓄力期间的普通攻击动画。持刀动作 `HoldPose` 在 `isCharging()`（蓄力/收招/淡出阶段）期间不施加，否则其平滑权重会在 `AttackAnimationTracker` 记录的手动动画窗口（约 1 秒）结束后重新覆盖主手，造成蓄力约 2 秒后“回弹”到持刀姿态。
- 服务端 `ChargeThrustHandler`：`ChargeThrustWindupPayload(true)` 建立蓄力状态并关闭 BC 攻击；`PlayerTickEvent.Post` 每 tick 以 `lastPosition → position` 为扫掠段、攻击方向 = `getLookAngle()` 归一化、前向锥半角 60°、距离 ≤ 4 格（+目标宽度/2）收集活体生物（排除玩家/盔甲架/自身），每个蓄力对每个目标最多结算一次。
  - **冲锋伤害**：`relativeSpeed = ((攻击者速度 − 目标速度) · 攻击方向) × 20`（格/秒）；攻击者速度取自 `Charge` 保存的最近 6 个位置的平均每 tick 位移（**不用坐骑 deltaMovement**——玩家控制的坐骑是客户端权威，服务端 delta 通常为 0，会导致骑马时永远达不到阈值），否则取自身位置位移。仅当 `relativeSpeed >= 4.6` 才结算；伤害 = `Attribute.ATTACK_DAMAGE` × `min(4.0, 1 + (relativeSpeed − 4.6) × 0.15)`。低于阈值时不结算也不记入已命中集合，之后更快经过仍可命中。迎面（同向相对）倍率最高，同向追击接近目标速度时低于阈值，侧向 90° 投影≈0。
  - **松开** `ChargeThrustStrikePayload`：对前方锥内尚未命中的生物以同样规则补一次前刺、播放刺击音效（`bettercombat:spear_stab`，经 `BuiltInRegistries.SOUND_EVENT` 解析，BC 缺失则跳过），命中时按 `Forge.particlesEnabled/displayColor/glowEnabled` 迸发 8 颗武器色火星并沿攻击方向击退 0.4 格，随后清理状态并恢复 BC 攻击通道。
  - 安全兜底：单次蓄力最长 10 秒（客户端正常会提前发送 windup(false)）；玩家退出/死亡时清理状态并恢复攻击通道。

- **BC 刀光跟随玩家**：Better Combat 的挥砍刀光本身是世界坐标粒子，高速骑乘时会留在原地。TrailFollow 在 SlashParticleUtil.spawnParticles 调用期间（SlashTrailFollowMixin 的 HEAD/RETURN）标记上下文；ParticleEngineTrailFollowMixin 在 ParticleEngine.add 时只记录**类名以 
et.bettercombat. 开头**的粒子（即 BC 自己的刀光粒子）及其施放者；每个客户端 tick 用 ParticleAccessor 把这些粒子的世界坐标与上一帧坐标按施放者位移整体平移。本模组生成的粒子（WeaponTrailFx 火星、FirstPersonBlockBurst 等）类名不属于 BC，永远不会被记录/移动，保持原样。


## 11. 经验修补（Mending）→ 锋利度

arsenal 武器无耐久（SharpnessWeaponItem 不可损坏），原版经验修补没有可修的耐久。MendingSharpness（注册在 NeoForge.EVENT_BUS 的 PlayerXpEvent.PickupXp）在玩家拾取经验球时把经验转换为武器锋利度：**每 1 点经验恢复 20 点锋利度**（SHARPNESS_PER_XP = 20）。只处理主手/副手中带 EnchantmentEffectComponents.REPAIR_WITH_XP 且 Sharpness.get < Sharpness.MAX 的 arsenal 武器，优先主手、其次副手；按缺少的锋利度算出所需经验 ceil(missing / 20)，直接扣减 ExperienceOrb.value（消耗经验），剩余经验仍由原版 ExperienceOrb.playerTouch 流程发放（可正常进账，或修其它带经验修补的装备）。武器锋利度已满时不扣经验，经验正常获得。锋利度上限仍为 Sharpness.MAX = 3000。

## 12. 披风渲染（Better Combat / PAL）

Better Combat 使用的 Player Animator Library（PAL）在动画激活时，会在 `CapeLayer#render` 里把披风挂到被动画和弯曲的躯干上（`dev.kosmx.playerAnim.mixin.CapeLayerMixin`：先按 `body` 的位置/旋转和 `torso`/`body` 弯曲量变换 pose stack，再把 cloak 部件姿势清零），因此披风会紧贴后背而不是按原版摆动。未安装 FancyPlayerAnimation 时，本模组让披风回到**原版渲染**：

- `CapeLayerVanillaMixin`（priority 500，早于 PAL 的默认 1000）在 `CapeLayer#render` HEAD 置 `CapeRenderState.suppressPalCape = true`，RETURN 清掉；
- `pal.AnimationApplierCapeMixin` 在该标记为真时让 PAL 的 `AnimationApplier#isActive()` 返回 false，PAL 的披风分支因此走非活动路径（只把 cloak 弯曲清零，不动 pose stack），披风由原版 `CapeLayer` 正常渲染（移动摆动/步态晃动/下蹲倾斜）；
- 标记只在这一个渲染调用内为真，正常动画播放不受影响；
- 安装 FancyPlayerAnimation 时 `CapeRenderState.suppressPalCape` 保持 false，FPA 自己的披风处理照常生效；
- 格挡姿态的披风挂点：原版 `CapeLayer` 在模型原点绘制披风，而格挡姿态会旋转躯干并重新贴接腿部，挂点会离开肩膀后沿。未安装 FPA 时 `CapeLayerBlockPoseMixin` 在 `BlockPose.progressOf(player) > 0.001` 的披风渲染期间 HEAD `pushPose()` 并执行 `PlayerModel.body.translateAndRotate(poseStack)`，RETURN 处 pop（`@Unique` 标记保证成对）；这样披风挂点跟随旋转后的肩膀上沿/后沿，淡入淡出与格挡姿态进度一致。装 FPA 时 `FpaBend.available()` 为真，跳过该变换由 FPA 处理披风。
