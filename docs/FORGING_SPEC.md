# 锻刃系统规范（BetterCombat:NeonArsenal）

本规范描述锻造（锻刃）系统的数据、流程、颜色混合规则与每种材料的效果结算，供实现、测试与后续扩展使用。所有数值均在 `com.zszq.neonarsenal` 中实现。

## 1. 术语与数据

| 术语 | 说明 |
|---|---|
| 锻刃层（layer） | 一次成功锻造附加到武器上的永久效果记录，最多 3 层 |
| 层 id | `chorus`（紫颂果）、`creeper`（苦力怕头颅）、`rabbit`（兔子脚）、`echo`（回响碎片） |
| 混合色 | 多层锻刃时按 HSV 色相插值（短弧色相平均 + 饱和度/明度平均）得到的刀光/粒子/刀刃光效颜色 |
| 标记效果 | 服务端施加、客户端读取的隐藏效果，用于同步苦力怕闪烁进度 |

数据组件：`bettercombat_neonarsenal:forge`，值为逗号分隔的层 id（按添加顺序），例如 `chorus,echo`。无该组件 = 未锻刃。

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
   - `Forge.canAdd(武器, 层)`：当前层数 < 3 且不包含该层。
3. 校验通过：输出 = 武器副本 + 追加该层；经验花费按材料表（5/8/8）；消耗材料 1 个（`setMaterialCost(1)`）。
4. 校验失败：不设置输出，交给原版铁砧逻辑（不消耗、无提示）。
5. 取走输出后：锋利度、已有层、原有附魔全部保留；层顺序即添加顺序。

边界规则：

- 已满 3 层或已含该层：该材料无效（不可叠加同层）。
- 蜜脾与三种材料互斥：蜜脾走锋利度修复（`SharpnessEvents`），三种材料走锻刃（`ForgeAnvil`）。
- 锻刃**不写入真实附魔**（火焰效果在命中时结算），因此武器不会出现附魔光效。
- 锻刃不改变锋利度、不改变武器基础属性。

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

**Tooltip**：默认显示缩略信息（锋利度、颜色、`锻刃效果: 层名`、`按住 Shift 查看…` 提示）；**每层锻刃使物品稀有度 +1 阶**（白→黄→青→紫，名称颜色随之变化）；**按住 Shift** 时显示 `— 综合性能 —`（伤害、攻速、移速、跳跃高度、跃斩增伤、锐度伤害系数）与 `— 锻刃效果 —`（各层持有/效果说明；其中**不包含粒子效果相关内容**）。颜色行：有染料显示 `着色模块: #RRGGBB`；仅有锻刃层时显示 `锻刃颜色: #RRGGBB`。

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
3. 在命中位置生成一只**完全无敌**苦力怕（伤害事件置 0、取消击退、击退抗性拉满），向**远离玩家**的方向**快速**行走 **4 秒（80 tick）**（每 tick 直接位移 0.25 格，约 5 格/秒，不依赖寻路）；每 tick 重新设置 `setInvulnerable(true)` 并回满血，伤害事件强制置 0，任何生物都无法击杀；
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
- 同一武器最多 3 层，层组合不分职业/预设。

## 6. 组合、优先级与交互

### 6.1 叠加规则（全部效果同时生效、分别计算）

1. 每次命中都**逐层遍历**武器的全部锻刃层，每层独立判断触发条件与自身冷却；某一层不满足只跳过自身，不影响其他层。
2. **持有效果**（主手武器，按层相加后以 `ADD_MULTIPLIED_TOTAL` 应用）：紫颂果 增伤+10%/移速+15%；苦力怕头颅 增伤+5%/攻速+15%；兔子脚 攻速+5%/跳跃高度+150%（默认2.5倍）/跳劈增伤+100%；回响碎片 攻速−50%/增伤+100%。
3. 冷却**按效果独立**，并记录在**武器堆叠**的数据组件中（absolute game time）：紫颂果 10 秒；苦力怕头颅 30 秒；兔子脚 30 秒；回响碎片 30 秒。丢弃、切换物品、交换主副手、退出重进都不会重置冷却。
3. 效果执行相互隔离：每个效果包在独立异常处理中，某效果出错只会记录日志，不会阻止其他效果结算。
4. 同一目标可同时处于「被点燃 + 白色闪烁 + 被传送/僵直」状态；伤害、击退、传送、僵直分别独立结算、互不覆盖。
5. 苦力怕标记目标在引信结束前死亡时，立即在**死亡位置**爆炸（不会因为目标死亡而失效）。
6. 结算顺序为 **火焰点燃 → 苦力怕标记 → 紫颂传送**，但顺序不影响最终结果；冷却各自开始计时。
7. 跳劈击退（击退 V）与苦力怕爆炸击退（击退 III）分别结算，不互相覆盖。
8. **双持限制**：只有主手武器的锻刃层参与结算。副手攻击（Better Combat 判定为 off-hand）不触发任何锻刃效果，也不消耗/缩放主手锋利度；主手攻击不受副手武器影响。渲染与结算分离：刀光、火花粒子、刀刃光效按各自手持武器分别渲染（副手武器用副手自己的颜色，不套用主手）。
2. 锋利度：锻刃不改变锋利度；命中照常消耗 1 点锋利度；刀光亮度与粒子密度 = 锋利度系数（每损失 10% −20%，≥50% 归零）× 锻刃颜色（颜色只改色相，不改变亮度系数）。
3. 伤害：锋利度倍率作用于事件伤害（0.75–1.0）；锻刃本身不额外加成伤害（苦力怕爆炸为独立伤害）。
4. 跳劈击退（击退 V）与苦力怕爆炸击退（击退 III）互不叠加，各自结算。

## 6.2 客户端设置（模组列表滑条）

模组列表 → Better Combat Arsenal → 配置：

- `particle_density`（粒子浓度，0–3，默认 1）：全局乘算所有火花/锻刃粒子数量；
- `blade_glow_intensity`（刀刃发光强度，0–1，默认 1）：全局乘算刀刃发光叠加层亮度。

两项与 `config/bettercombat_neonarsenal/weapon_glow.json` 的细分配置相乘生效。模组列表的「配置」按钮通过自定义配置界面（`IConfigScreenFactory`）打开，包含两个可拖动滑条，关闭时保存。

## 7. 兼容与回退

- **多人游戏**：锻刃、冷却、粒子判定均为服务端结算（组件同步），客户端只负责渲染；专用服务器已通过启动验证。

- **JEI**：安装后提供「锻刃」配方分类（154 条：锻刃层、基础开关、染料），铁砧为催化剂；未安装时该插件不会被加载。

- **Better Combat 为必需前置**（2.4.0+，需 playeranimator、cloth_config）：缺失时模组不会加载。
- 光影/Iris：刀刃发光沿用既有 Iris 适配路径；锻刃颜色仍生效。
- 旧存档：无 `forge` 组件的武器视为未锻刃；已满 3 层的旧武器不可继续锻造。
- 无指令入口：锻造只能通过铁砧进行（锋利度指令已移除）。

## 8. 验收用例

- [ ] 紫颂果锻刃：经验 8、20 格内敌对生物被拉到身边、末影人粒子/音效、僵直 4 秒、冷却 10 秒。
- [ ] 苦力怕头颅锻刃：经验 8、目标自身膨胀白色闪烁、吸引 10 格、1.4 秒后爆炸、无地形破坏、半 TNT 伤害、击退 III、冷却 8 秒。
- [ ] 兔子脚锻刃：持有攻速 +30%/移速 +20%，命中召唤 4 只 2 格高兔子（完全无敌、彼此不攻击、不攻击锻刃苦力怕、移速 2 倍）、主动索敌、每 0.5 秒最多 3 个目标各 4 伤害、10 秒白色粒子消失、30 秒武器冷却。
- [ ] 冷却绑定武器：丢弃/切换/重进后剩余冷却仍在（按游戏时间）。
- [ ] 回响碎片锻刃：命中后 8 格内 ≥2 敌对生物互相残杀 8 秒、深暗之域粒子、30 秒武器冷却。
- [ ] 各层颜色：单层与混合结果符合第 3 节表格（刀刃光效、BC 刀光、粒子一致）。
- [ ] 满 3 层与重复层材料无效；锋利度与已有层保留。
- [ ] 锋利度 ≤1500 时刀光与粒子完全消失，修复后恢复。


## 8. 格挡（Blocking）

- 手持任意本模组武器长按右键进入格挡；`SharpnessWeaponItem#use` 开始物品使用，松开即停止。
- 动画：客户端 `BlockPose` 读取 `assets/bettercombat_neonarsenal/animations/two_handed_parry.json` / `dual_wield_parry.json`（已同步 resourcepacks 最新版本），在 `PlayerModel#setupAnim` 末尾混合；进入/退出各 0.1 秒。单持武器与双手武器使用双手姿态；仅当副手也持本模组武器（真正双持）时使用双持姿态。slim 皮肤补偿手臂枢轴 0.5 单位偏移；躯干/身体/头部/手部只使用动作文件（不叠加其它动画）；手臂应用动作文件的 `right_arm/left_arm` 旋转；格挡时在 `AgeableListModel.renderToBuffer` 前由 `BlockBendRenderMixin` 调用 `FpaBend.blend`，读取 `right_arm_bend/left_arm_bend` 第一分量（**正 X**，修正先前反向）作为关节角，以进度作为权重叠加到当前 bend；非格挡不写 bend；躯干旋转后重新贴合腰部（y=12），头部吸附到躯干顶部并下移 0.5 单位贴住身体；格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；腿部按同一 progress 插值并同步 pants；格挡静止时腿部使用动画文件 `right_leg/left_leg`，腿根 z 从文件 +2 收回 0 与 body 接洽；移动时保持原版行走腿；不强制 crouch、不改相机；，手臂按躯干旋转 + 最终位移的完整变换定位，避免身体、头手与腿的间隙；腿部完全不参与格挡、保持原版；袖子与外套在姿态后重新 `copyFrom`（裤腿不覆盖）。格挡时屏蔽潜行：格挡期间每 tick 强制 `setShiftKeyDown(false)` 且客户端 setPose(STANDING)（服务端/客户端），相机/眼高保持站立，松开后恢复普通潜行输入。格挡期间禁止疾跑，正在疾跑会立即中断；同时屏蔽潜行。格挡优先级最高：`use` 清除物品冷却，不受攻击/切换武器冷却影响；格挡时取消左键攻击与挖掘事件。格挡判定沿用原版受击路径但限定前方 210°（±105°）：每次该范围内会生效的伤害事件触发一次成功格挡，消耗 ceil(伤害×10%) 锋利度、播放音效、格挡期间取消所有伤害（持续无敌）；松开格挡结束后额外给予 4 tick（0.2 秒）无敌；清除受击红闪、在武器位置发送 14 颗火星粒子（无需烈焰粉；颜色取 `Forge.displayColor`，锻入发光墨囊时粒子 full-bright 发光，否则使用世界光照）；音效与粒子按玩家持久数据记录下一次可用时间，两次之间强制随机 0.3–0.8 秒间隔（6–16 tick，每次重掷），避免史莱姆等高频率攻击逐 tick 触发。无额外击退抗性属性；环境伤害与背后/侧面伤害不受格挡。姿态 mixin 使用较低优先级，确保在其它玩家动画的 TAIL 注入之后应用，手部动作为**加法叠加**（不替换其它动作），格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；腿部按同一 progress 插值并同步 pants；格挡静止时腿部使用动画文件 `right_leg/left_leg`，腿根 z 从文件 +2 收回 0 与 body 接洽；移动时保持原版行走腿；不强制 crouch、不改相机；渲染。
- 判定：服务端 `LivingIncomingDamageEvent` 中若玩家正在使用本模组武器，则取消本次攻击伤害，调用 `Sharpness.consume` 消耗 1 点，并播放 `bettercombat_neonarsenal:parry`；播放时客户端通过监听本地 parry 音效触发手部/武器 2px、0.1s 颤动与视角轻微颤动（不再使用自定义网络包），服务端给玩家一个约 1/4 格的后退推力；取消伤害的同时给予 4 tick 受击无敌，避免同一攻击逐 tick 重复触发音效。
- 连招：`PlayerTickEvent` 记录格挡开始时间；松开时若持续 >40 tick（2 秒）则通过反射调用 `PlayerAttackProperties.setComboCount(player, -1)` 打断连招，≤40 tick 保持连招。

格挡姿态进入/退出 0.1 秒，idle↔格挡腿部切换 0.1 秒（文件腿权重 = progress × legBlend）；格挡静止↔移动的腿部姿态切换 0.1 秒；格挡静止时腿部使用动画文件 right_leg/left_leg 并把腿根 z 从 +2 收到 0 与 body 接洽，移动时保持原版行走腿。

右键优先级：工作方块（getMenuProvider != null）且玩家与方块之间（水平线段投影 t∈[0,1]、横向距离≤1.5、垂直≤2.5）存在 Enemy → 格挡；否则工作方块正常交互；非工作方块格挡。

跳劈：创造飞行不触发；下落>5格时首次攻击输入被 FallSlamClient 拦截，距地面<=4格自动 startAttack；FallSlamHandler 对命中伤害乘 1+fallDistance*0.05，落地时烈焰粉锻刃迸发 80 颗粒子并播放 parry 音效。

下落跳劈需持续按住攻击键；松开重置 fallStartY 与 pending，必须再次按住重新累计 >5 格。BC 攻击请求到达时 FallSlamHandler.markSlam；LivingFallEvent：有 slam 标记时取消原版摔落，<10格免伤，>10格按 floor((d-10)/3)*3 扣血并 clamp 到 1 HP；正常下落无标记不受影响。

格挡姿态整体接地：groundOffset = 24 - min(leg.y + 12*cos(xRot) - 2*|sin(xRot)|)，随后将 body/head/arms/legs/sleeves/jacket/pants 全部 y += offset，使脚底接触模型地面 y=24。

修复：等待期间 FallSlamState.waitingForSlam=true，BC getCurrentAttack 返回 null，避免按住攻击键每 tick 重播跳劈；首次按下 fallStartY=Y+fallDistance，松手后 afterRelease=true，再按下从当前 Y 起算。

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
