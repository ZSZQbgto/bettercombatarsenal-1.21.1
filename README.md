# 更好的战斗：霓虹装备

[![Build](https://github.com/ZSZQbgto/bettercombatarsenal-1.21.1/actions/workflows/build.yml/badge.svg)](https://github.com/ZSZQbgto/bettercombatarsenal-1.21.1/actions/workflows/build.yml)

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

## 从源码构建

- 需要 **Java 21**；首次构建需要联网下载 Gradle、NeoForge 与 Minecraft 资源。
- 可选编译依赖（Better Combat / JEI / Player Animation Library）放在 `run/mods`，本地首次构建前需自备；CI 会自动下载固定版本。
- 构建：`./gradlew build`（Windows：`gradlew.bat build`），产物在 `build/libs/bettercombat_neonarsenal-alpha1.jar`。
- 仓库内的 `gradle.properties` **不包含代理配置**，GitHub Actions 会直连构建。本地需要代理时请写到用户级 `~/.gradle/gradle.properties`（Windows：`%USERPROFILE%\.gradle\gradle.properties`；不要提交到仓库）：

  ```properties
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  ```

  也可以改用 `GRADLE_OPTS` 传入代理参数。
- **GitHub Actions**（`.github/workflows/build.yml`）：push 到 `master`/`main`、PR 或手动触发时自动构建并上传 jar；使用 Gradle 依赖缓存，网络失败会自动重试（清理残缺下载、必要时重下 wrapper）。
- CI 会从 Modrinth CDN 下载固定版本的可选编译依赖（Better Combat 2.4.0+1.21.1、JEI 19.70.0.469、Player Animation Library 2.0.4+1.21.1）到 `run/mods` 后再编译；仓库不提交第三方 jar。下载带 SHA-1 校验与重试，CDN 被墙时请配置上面的代理 Secret。
- CI 需要代理时，在仓库 **Secrets** 中配置 `HTTPS_PROXY` 或 `HTTP_PROXY`（例如 `http://127.0.0.1:7890`）；工作流会转换成 Gradle `systemProp`，并同时用于 Gradle wrapper 下载。未配置时使用直连。

## 游戏特色

### 1. 刀会变钝

- 锋利度 3000 满值，每次命中或挖方块 -1。
- 伤害从 100% 起，每掉 20% 锋利度少 5%，全钝仍有 75%。
- 光效与粒子一起衰减：每损失 10% 暗 20%，损失过半直接熄灭。
- 铁砧里用蜜脾补，一个 +1000，不花经验；修复结果可直接从铁砧取出（修复操作不消耗经验等级）。

### 2. 四种锻刃材料

铁砧中武器 + 材料，8 级经验、1:1 消耗；武器最多 **6 层**，**同种材料最多锻 5 次**；同种材料锻入 **3 层**开启该材料的命中效果，满 **5 层**时该材料命中效果冷却减半；**每 2 层稀有度 +1 阶**（0–1 层普通/白，2–3 层罕见/黄，4–5 层稀有/青，6 层史诗/紫）。每锻入一层额外获得 **+2% 暴击率**（暴击伤害 ×1.5）。

| 材料 | 锻层加成（Lv1 → Lv5） | 命中效果 | 冷却 |
|---|---|---|---|
| 紫颂果 | 伤害 +10% → +115%、移速 +2% → +10% | 20 格内敌人拉进攻击范围，僵直 4 秒 | 10 秒（满 5 层 5 秒） |
| 苦力怕头颅 | 攻速 +5% → +40%、伤害 +5% → +40% | 无敌苦力怕（免疫伤害与击退）像原版躲避猫一样用 `AvoidEntityGoal` **真实寻路**远离玩家（4 秒，半径 24 格、1.2 冲刺速度），并强吸敌对生物，随后原地白色闪烁 1.4 秒并爆炸（不破坏地形、仅伤敌对，dummmmmmy 人偶除外） | 30 秒（满 5 层 15 秒） |
| 兔子脚 | 攻速 +5% → +40%、伤害 +4% → +35%、跳跃高度 +30% → +150%、跃斩增伤 +20% → +100% | 4 只无敌雷霆大兔子（免疫伤害与击退），每 0.5 秒咬 3 个目标各 4 伤害，10 秒后消失 | 30 秒（满 5 层 15 秒） |
| 回响碎片 | 攻速 -50%（固定）、伤害 +60% → +300% | 8 格内敌人互相攻击 8 秒 | 30 秒（满 5 层 15 秒） |

**成长规则**：材料层数（1–5 阶）按各自成长表成长，完整数值见 `docs/FORGING_SPEC.md` §4.5；每层额外 **+2% 暴击率**，暴击造成 **×1.5** 伤害（原版暴击的粒子/音效）。

**解锁规则**：每种材料锻入 1–2 层只生效「锻层加成」；**同种材料锻入第 3 层**才开启该材料的「命中效果」；同种材料满 5 层时该效果冷却减半。每种材料最多 5 层，武器总共最多 6 层，可混搭不同材料。Shift 详情在未达 3 层时显示「命中效果：同种材料锻入 3 层后开启」。

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

- 起跳后至落地前触发（整个跳跃窗口）；下落高度 **小于等于 5 格**（超过 5 格归下落斩击或普通攻击）。实际命中落在挥砍的落点、通常已处于下落阶段（对齐原版暴击）。水中、攀爬、骑乘、失明时不触发。
- 触发时播放阔剑第三段下劈动画、使用武器自身音效、击退 I、重置连招。
- 跳劈后攻击冷却的前半段禁止起跳；落地后清空跳劈窗口。
- 创造飞行时攻击使用平地出招，不触发跳劈。

### 6. 下落斩击

- 自由下落时按住攻击键（已累计下落超过 5 格，或离地 5 格以上且正在下降）；长按 0.15 秒进入下落斩击；短按则会在松开时打一次普通攻击。**拥有缓降效果时无法触发**。下落距离始终从**本次下落起点**累计，中途松开/重按不会重置。
- 起手动作停在下劈前 1/3 的蓄力姿势；下落过程中风声随下落速度增强——坠得越快，风越大；中途松手/再次按住只会中断动作，**不会中断或重置风声**，风一直响到本次下落结束。
- 距地 4 格时恢复动作，从 1/3 处继续劈下，并播放巨刃第三段斩击音效；无论落地前后，整段动作都以原速播完，不会被落地截断。
- 到最终帧用 0.3 秒淡出回站立姿态；这 0.3 秒内无法攻击，结束后恢复正常攻击。
- 落地有专属落地音效；落地时若武器锻入烈焰粉，会迸发大量粒子。
- **下落斩击的额外攻击力随实际下落高度提升，且没有上限**：第 1–10 格每格 +4 伤害，第 11–20 格每格 +2 伤害，之后每格 +1 伤害。
- 松开攻击键（尚未到 4 格）会在 0.2 秒内淡回下落姿态；**已累计的下落距离不会重置**，再次按住时仍按本次下落起点继续判定（只需重新长按 0.15 秒），不会要求再下落超过 5 格。
- 下落斩击的摔落结算按**本次下落起点**计算高度：下落 **<10 格免疫摔落伤害**；**>10 格每多 3 格扣 1 点血量**，最低保留 **1 点血**，不会摔死；扣血量会经过原版摔落保护（摔落保护/保护附魔，走原版 `damage_protection` 与魔法减伤公式）和 `FALL_DAMAGE_MULTIPLIER` 属性减免，护甲不参与（与原版摔落一致）。中途取消/重按斩击不重置该高度；最终没有触发下落斩击的普通下落仍按原版摔落伤害结算。
- **装备鞘翅时**：下落斩击蓄力过程中单击空格展开滑翔会**保持当前蓄力姿势**；此后不再按“距地 4 格/落地”触发，而是**撞击生物时触发斩击**。滑翔结束时若没有撞到生物，斩击不会触发，蓄力自动收回。
- **鞘翅滑翔中**（手持可鞘翅斩击的武器）**长按左键 0.15 秒**进入同样的滑行斩击蓄力（1/3 定格），撞到生物时触发斩击；**滑翔中长按/点击右键没有效果**。滑翔斩击的额外攻击力按**撞击瞬间的速度**计算：把速度换算成等效下落高度（`v = √(2·0.08·h)`）后套用同一分档，与下落斩击保持平衡。
- **鞘翅斩击是独立分配的技能**：仅**阔剑、双刃斧、横刀、双刃剑**可用（与下落斩击相同）；其他武器在滑翔和下落时保持**平地的攻击方式**，不会进入蓄力。

### 7. 格挡

- 手持武器长按右键进入格挡（对方块右键同样进入格挡，不触发方块交互）；松开恢复，进入/退出各有 **0.1 秒**过渡。
- 右键优先级：目标为工作方块（带 GUI，如工作台/熔炉/箱子）时，若**玩家与工作方块之间**有敌对生物 → 格挡；否则正常工作方块；非工作方块 → 格挡。
- 单持武器与双手武器使用**双手格挡**姿态；真正双持（副手也持本模组武器）使用**双持格挡**姿态。
- **格挡动画分两套**：动画文件里同时提供 `parry`/`dualwieldparry`（装 FPA 时使用，手臂和膝盖的弯曲由 FPA 的 bend 实现驱动）与 `parrywithoutfpa`/`dualwieldparrywithoutfpa`（不装 FPA 时使用：肩部角度已补偿、弯曲骨骼为 0，因此角色保持直臂直腿）。`BlockPose` 按 `FpaBend.available()` 自动选择对应 animation 键。
- 格挡期间**强制退出潜行**，且**无法疾跑**；若开始格挡时正在疾跑，会立即中断疾跑。
- 只格挡**前方 210°（左右各 105°）**内来源的伤害；背后/侧面来源与环境伤害不受格挡。
- 成功格挡**免疫本次伤害**；**触发格挡音效/粒子时**消耗 **本次伤害 ×10%（向上取整）**的锋利度，间隔内的格挡不消耗；**松开右键后获得 0.2 秒无敌**。
- 格挡成功时**不显示受击红闪**；武器处迸发中等密度火星粒子（无需烈焰粉，颜色跟随当前武器粒子色，锻入发光墨囊时发光）。
- 格挡的音效、粒子与锋利度消耗有 **0.3–0.8 秒随机间隔**（每次重新随机）；间隔内的格挡仍正常免伤，但不再播放音效与粒子，也**不扣锋利度**。
- 每次触发格挡音效/粒子时：手部与武器颤动约 2px、持续约 0.1 秒；视角轻微颤动；角色向后平移约 1/4 格。
- 格挡 **2 秒内**松开不打断连招；超过 2 秒再松开则打断连招。
- 宽窄皮肤均适配；格挡时袖子、外套、裤腿与**头部外层**（含 3D 皮肤层的 3D 网格）都会跟随动作；**格挡静止时全身只比移动格挡略低约 1 像素**（头部与身体一起下移）。
- **骑马 / 坐船 / 坐矿车等坐姿格挡时**，下半身保持原版坐姿（不套用动画文件的站姿腿），只有上半身进入格挡姿态，整体保持乘坐高度。
- **骑乘活体坐骑**（马、驴、骆驼等）行走/奔跑时，角色会随坐骑步态轻微上下起伏（幅度约 1 像素，速度越快越明显），武器与披风等一起跟随；船/矿车不应用。
- **穿戴鞘翅格挡时**，鞘翅会收拢为潜行时的姿态，并跟随躯干/肩膀一起运动，不会与身体分离。
- **格挡时的披风挂点（无 FPA）**：格挡姿态会旋转躯干，而原版 `CapeLayer` 在模型原点绘制披风，挂点会离开肩膀后沿。未安装 FancyPlayerAnimation 时，`CapeLayerBlockPoseMixin` 在格挡姿态激活期间（`BlockPose.progressOf > 0`）对披风渲染套用当前 `model.body` 的 `translateAndRotate`，让披风跟着旋转后的肩膀上沿/后沿；安装 FPA 时由 FPA 处理，跳过该变换。
- **披风（Better Combat）**：Better Combat 使用的 Player Animator Library 会把披风挂到被动画/弯曲的躯干上，导致披风紧贴后背。未安装 FancyPlayerAnimation 时，本模组在 `CapeLayer` 渲染期间把该库的动画状态临时判为非活动（`CapeLayerVanillaMixin` + `AnimationApplierCapeMixin`），让披风按**原版渲染**（移动摆动、步态晃动、下蹲倾斜）；安装 FPA 时保持 FPA 自己的披风处理不变。
- **格挡优先级最高**：开始格挡会清除武器的物品冷却，不受攻击冷却与切换武器冷却影响；格挡期间左键攻击/挖掘会被取消，确保格挡动作优先。
- **第一人称格挡**：格挡时武器切换到设计好的第一人称格挡姿态（_fpparry 模型的第一人称 display 变换），松开后平滑回到普通持握。
- **第三人称攻击姿态**：静置时使用现有静置放置；攻击动画（含普通攻击与下落斩击/冲锋前刺的手动 BC 攻击动画）期间改用 `bctppitemholding` 导出的攻击放置（`<id>_tppattack`，7 把有差异的武器，匕首不变），**0.10 s 淡入 / 0.15 s 淡出**，联机时其他玩家的攻击同样生效；左手/副手保持现有静置放置（资源包未提供左手攻击数据）。
- **双持格挡（长剑/刺剑）**：主手长剑或刺剑格挡且副手也持本模组武器时，副手使用该武器 `_fpparry` 的**左手格挡变换**（长剑/刺剑已提供），与主手共用同一 0.18 秒平滑过渡和视图上抬；未设计副手姿态的武器（如双手武器）保持普通副手持握，不会被错误上抬。

## 武器

| 武器 | ID | 攻速 | 伤害 | DPS | 跳劈 | 格挡 | 下落斩击 | 鞘翅斩击 | 定位 |
|---|---|---|---|---|---|---|---|---|---|
| 长剑 | `bettercombat_neonarsenal:sword` | 1.6 | 5.0 | 8.0 | ✓ | ✓ | — | — | 单手，最稳 |
| 阔剑 | `bettercombat_neonarsenal:claymore` | 0.8 | 11.5 | 9.2 | ✓ | ✓ | ✓ | ✓ | 双手，+0.35 距离 |
| 双刃剑 | `bettercombat_neonarsenal:twinblade` | 1.4 | 7.0 | 9.8 | ✓ | ✓ | ✓ | ✓ | 双手连段 |
| 双刃斧 | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 15.0 | 9.0 | ✓ | ✓ | ✓ | ✓ | 双手旋转斩 |
| 横刀 | `bettercombat_neonarsenal:hengdao` | 1.1 | 8.5 | 9.4 | ✓ | ✓ | ✓ | ✓ | 双手拔刀斩 |
| 刺剑 | `bettercombat_neonarsenal:rapier` | 2.5 | 4.0 | 10.0 | ✓ | ✓ | — | — | 单手快刺 |
| 匕首 | `bettercombat_neonarsenal:dagger` | 3.2 | 3.0 | 9.6 | — | — | — | — | 单手短刃，纯输出 |
| 长枪 | `bettercombat_neonarsenal:spear` | 1.2 | 7.0 | 8.4 | ✓ | ✓ | — | — | 双手，+1 距离，**冲锋前刺** |

（DPS = 基础伤害 × 攻速。）

### 8. 附魔

- 本模组武器可在**附魔台 / 铁砧（附魔书）/ `/enchant`** 附魔；三种伤害附魔（锋利、亡灵杀手、节肢杀手）**互斥**，只能选一（沿用原版 `exclusive_set`）。
- 天然可附魔列表：
  - **长剑、双刃剑、横刀、刺剑、匕首**：锋利 V、亡灵杀手 V、节肢杀手 V、击退 II、抢夺 III、火焰附加 II、耐久 III、经验修补 I、横扫之刃 III。
  - **阔剑、双刃斧、长枪**：锋利 V、亡灵杀手 V、节肢杀手 V、击退 II、抢夺 III、火焰附加 II、耐久 III、经验修补 I（**无横扫之刃**）。
  - 长枪不适用三叉戟附魔（忠诚、激流、引雷、穿刺）；**致密/破甲**为原版重锤专属，不能自然获得。`/enchant` 命令可强制附上，且仍按原版排斥（致密/破甲 与 锋利/亡灵杀手/节肢杀手/穿刺 互斥）。
- **附魔视觉效果**：未锻入**发光墨囊**时无效果；锻入后刀刃发光不再是单一颜色，而是**中等速度的流光**——颜色沿刀刃在武器当前颜色环（染料/锻刃混色）附近以**较窄的色相变化范围**（±12°、第二谐波权重 0.25）流动；未染色时表现为明暗流光。
- **不显示原版附魔光效**：附魔武器不会出现原版附魔闪烁，只保留本模组自己的刀刃发光/流光。
- **Tooltip**：默认显示**综合百分比**：伤害与攻速会把附魔提供的伤害/攻速数值**叠加**到锻刃百分比上（锋利/亡灵杀手/节肢杀手/火焰附加折算为相对武器基础攻击伤害的百分比；附魔数据里的攻速修饰符也计入攻速百分比），附魔部分**只列附魔名称**；按住 **Shift** 查看附魔名称、综合性能与锻刃说明。
- **经验修补（Mending）**：附在本模组武器上时不再修耐久（武器无耐久），改为**每 1 点经验恢复 20 点锋利度**（消耗对应经验，优先主手、其次副手；锋利度已满时经验正常获得，多余经验仍会正常进账/修其它带经验修补的装备）。
- **耐久（Unbreaking）**：武器无耐久，附魔改为降低**锋利度损耗率**：沿用原版耐久公式，每级 L 使每次命中/挖掘消耗 1 点锋利度的概率变为 1/(L+1)（I 50%、II 33.3%、III 25%），即期望损耗率降为 1/(L+1)。

### 9. 冲锋前刺（骑乘长枪）

- **条件**：主手持长枪（`CHARGE_THRUST` 技能），且正在**骑乘活体坐骑**（鞘翅滑行不触发）。
- **短点击左键**：触发一次普通长枪前刺攻击（与普通攻击一致）。
- **长按左键**：0.15 秒后进入蓄力，播放长枪前刺动作并**冻结在动画 1/3 处**；蓄力期间服务器每 tick 扫描玩家移动路径上位于攻击方向锥形范围内的所有生物并结算冲锋伤害；**松开**后继续播完整段前刺动画、动画结束后 0.3 秒淡出，并对松开瞬间前方未被命中的生物补一次前刺。
- **冲锋伤害**：按“攻击者与目标速度差沿攻击方向的投影”计算，阈值 **≥ 4.6 格/秒**才会造成冲锋伤害；伤害 = 武器攻击力 × 倍率，倍率 = 1 + (相对速度 − 4.6) × 0.15，上限 4×。迎面冲刺倍率最大、同向追击倍率低、侧向接近 90° 贡献接近 0。低于阈值的一次接触不结算伤害（之后更快经过仍可命中）。
- 命中时按现有粒子/声音规则：武器锻入烈焰粉时迸发武器色火星，播放长枪刺击音效，并把目标沿攻击方向击退。
- 蓄力期间屏蔽普通攻击与挖掘，服务器侧同时关闭 Better Combat 攻击通道（10 秒安全兜底，客户端松手/中断会立即恢复）。
- **BC 刀光跟随玩家**：只让 Better Combat 自己生成的挥砍刀光粒子随施放者移动（高速骑乘时不再相对地面静止）；本模组生成的粒子（火星、格挡迸发等）保持原样，不做任何修改。

## 工艺速查

- **持刀动作**：单持**长剑 / 阔剑 / 双刃剑 / 双刃斧 / 横刀 / 刺剑 / 长枪**时，主手使用设计好的持刀动作（`animations/weapon_holding_action_from_behind.json`，含手臂与肘弯），静止与移动都保持该设计动作；武器位置与朝向保持原版渲染不变；移动时只叠加微量前后摆动：行走中轴后 5°、扇面 4°，疾跑中轴后 10°、扇面 8°，静止时无摆动。与攻击招式之间平滑过渡（出招时淡出、收招后淡入），**双持时副手保持原有动作不变**（匕首保持原持刀动作）。
- **冷却跟武器走**：存在物品上按游戏时间计算，丢弃、换手、重进都不刷新。
- **双持**：锻刃与锋利度只由主手攻击触发；副手砍人不触发、不消耗；两手外观各自独立。
- **砂轮**：一次清除锻刃、染料、开关、冷却、稀有度，保留锋利度。
- **Tooltip**：平时显示刀刃锐度、颜色、附魔数值加成与锻刃百分比加成；按住 Shift 看附魔明细、综合性能与每层说明。
- **配置**：模组列表滑条——粒子密度 0–3、刀刃发光强度 0–1。
- **JEI**：272 条锻刃配方（展示第 1/2/3/5 次锻造示例；第 3–6 层可继续在铁砧中锻造），铁砧为催化剂。
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

## Building from source

- **Java 21** is required. The first build downloads Gradle, NeoForge and Minecraft artifacts.
- Optional compile-time dependencies (Better Combat / JEI / Player Animation Library) go into `run/mods`; a fresh local clone needs them, while CI downloads the pinned versions automatically.
- Build with `./gradlew build` (`gradlew.bat build` on Windows); the jar is written to `build/libs/bettercombat_neonarsenal-alpha1.jar`.
- The repository `gradle.properties` **does not contain proxy settings** (GitHub Actions runs without the local proxy). For a local proxy, put it in `~/.gradle/gradle.properties` (Windows: `%USERPROFILE%\.gradle\gradle.properties`), never in the repository:

  ```properties
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  ```

  Alternatively pass the proxy through `GRADLE_OPTS`.
- **GitHub Actions** (`.github/workflows/build.yml`): builds on pushes to `master`/`main`, pull requests and manual runs, then uploads the jar; Gradle dependency caches are used and transient network failures are retried (partial downloads are cleaned, the wrapper is re-downloaded when needed).
- CI downloads pinned optional compile-time dependencies (Better Combat 2.4.0+1.21.1, JEI 19.70.0.469, Player Animation Library 2.0.4+1.21.1) from the Modrinth CDN into `run/mods` before compiling; third-party jars are not committed. Downloads are SHA-1 verified and retried; configure the proxy Secret above if the CDN is blocked.
- If CI needs a proxy, set `HTTPS_PROXY` or `HTTP_PROXY` as a repository **Secret** (for example `http://127.0.0.1:7890`); the workflow converts it to Gradle `systemProp` settings and also uses it for the Gradle wrapper download. Without it the build connects directly.

## Features

### 1. The edge wears

- Sharpness starts at 3000; -1 per hit or broken block.
- Damage starts at 100%, -5% per 20% sharpness lost, still 75% when fully dull.
- Glow and particles fade too: -20% per 10% lost; past 50% lost they go dark.
- Repair at an anvil with honeycomb: +1000 each, no XP; the repaired weapon can be taken out immediately.

### 2. Four forge materials

Anvil weapon + material: 8 levels, 1:1, up to **6 layers**; **each material can be forged up to five times**; a material's on-hit effect unlocks at **3 layers** and its cooldown is **halved at the full 5 layers**; **+1 rarity per 2 layers** (layers 0-1 COMMON/white, 2-3 UNCOMMON/yellow, 4-5 RARE/aqua, 6 EPIC/light purple). Each forged layer also grants **+2% crit chance** (x1.5 crit damage).

| Material | Layer bonus (Lv1 -> Lv5) | On hit | Cooldown |
|---|---|---|---|
| Chorus Fruit | +10% -> +115% damage, +2% -> +10% move speed | Pull mobs within 20 blocks into range, stun 4 s | 10 s (5 s at 5 layers) |
| Creeper Head | +5% -> +40% attack speed, +5% -> +40% damage | An invulnerable creeper flees the player with **real pathfinding** using vanilla creeper-vs-cat `AvoidEntityGoal` (4 s, radius 24, sprint 1.2), pulls mobs, then flashes in place for 1.4 s and explodes | 30 s (15 s at 5 layers) |
| Rabbit's Foot | +5% -> +40% attack speed, +4% -> +35% damage, +30% -> +150% jump height, +20% -> +100% jump-slam damage | Four invulnerable thunder rabbits bite 3 targets for 4 damage every 0.5 s, vanish after 10 s | 30 s (15 s at 5 layers) |
| Echo Shard | -50% attack speed (fixed), +60% -> +300% damage | Mobs within 8 blocks fight each other for 8 s | 30 s (15 s at 5 layers) |

**Growth rule**: a material's level (1-5 = number of copies) follows the unified exponential table in `docs/FORGING_SPEC.md` section 4.5. Every layer also grants **+2% crit chance** for **x1.5** critical damage (vanilla crit particles/sound).

**Unlock rule**: one or two copies of a material only grant its layer bonus; **the 3rd copy** unlocks its "on hit" effect, and the full 5 copies halve that effect's cooldown. Each material can be forged up to five times, up to 6 layers per weapon, different materials can be mixed. Below 3 copies the Shift details show "On-hit effect: forge 3 copies of this material to unlock".

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
- On trigger: claymore slam animation, the weapon's own sound, Knockback I, combo reset.
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
- **Two block-animation variants**: each animation file ships `parry`/`dualwieldparry` (used with FancyPlayerAnimation, where FPA drives the bent arms and knees) and `parrywithoutfpa`/`dualwieldparrywithoutfpa` (used without FPA: the shoulder angles are compensated and the bend bones are zero, so the limbs stay straight). `BlockPose` selects the matching animation key from `FpaBend.available()`.
- While blocking, sneaking is suppressed and sprinting is disabled; starting a block while sprinting interrupts the sprint immediately.
- Only damage coming from the **frontal 210° arc (105° each side)** is blocked; damage from behind/sides and environmental damage are not.
- A successful block negates the incoming damage; the **parry sound/particles** trigger costs **ceil(10% of the incoming damage)** sharpness (blocks inside the gap cost none), and grants a **0.2 s invulnerability window after releasing right-click**.
- A successful block does **not flash the player red** and emits a **medium-density spark burst** at the weapon (no blaze powder needed; uses the current weapon particle colour and glows when glow ink was forged).
- Block sounds, particles and sharpness cost have a forced **random 0.3–0.8 s gap**, rerolled every time; blocks during the gap still negate damage but play no sound/particles and cost no sharpness.
- Whenever sound/particles actually trigger: the hand and weapon shake by about 2 px for ~0.1 s, the camera shakes slightly, and the player is pushed back about 1/4 block.
- Releasing within **2 s** keeps the combo; blocking longer than 2 s and releasing breaks the combo.
- Slim and wide skins both fit; sleeves and jacket follow the block pose; **while blocking stationary the whole figure sits only about 1 px lower than the moving block pose** (head and body move together).
- **Blocking while riding (horse, boat, minecart, ...)**: the lower body keeps the vanilla seated pose and the model keeps the seat height; only the upper body enters the block pose.
- **While riding a living mount** (horse, donkey, camel, ...) the rider heaves subtly with the mount's gait (about one pixel, stronger at higher speed); the weapon and layers follow. Boats and minecarts are unaffected.
- **Blocking with an elytra equipped** folds the wings into the sneaking elytra pose and keeps them attached to the posed torso/shoulders instead of floating apart.
- **Cape anchor while blocking (without FPA)**: the blocking pose rotates the torso, while vanilla `CapeLayer` draws the cape at the model origin, so the anchor leaves the shoulders' back edge. Without FancyPlayerAnimation, `CapeLayerBlockPoseMixin` applies the current `model.body` `translateAndRotate` to the cape render while the block pose is active (`BlockPose.progressOf > 0`), so the cape follows the posed shoulders; with FPA installed FPA handles it and the transform is skipped.
- **Cape (Better Combat)**: the Player Animator Library used by Better Combat attaches the cape to the animated/bent torso, which makes the cape stick to the back. Without FancyPlayerAnimation installed the mod makes that library report its animation as inactive while `CapeLayer` renders (`CapeLayerVanillaMixin` + `AnimationApplierCapeMixin`), so the cape renders exactly like **vanilla** (movement swing, walk bob, crouch tilt); with FPA installed FPA keeps its own cape handling.
- **Blocking has the highest priority**: starting a block clears the item cooldown and is not gated by attack or weapon-switch cooldowns; left-click attacks/mining are cancelled while blocking.
- **Third-person attack pose**: idle keeps the existing resting placement; during a Better Combat attack animation (normal attacks, plus the manual falling-slash / charge-thrust animations) the held weapon switches to the attack placement exported from `bctppitemholding` (`<id>_tppattack`, the seven weapons whose values differ; the dagger is unchanged) with a **0.10 s fade-in / 0.15 s fade-out**. Other players' attacks in multiplayer use it too; the left hand/offhand keeps the existing resting placement (the pack provides no left-hand attack data).
- **First-person parry**: while blocking, the weapon switches to the authored first-person parry pose (the `_fpparry` model's first-person display transform) with a **0.18 s smoothstep** blend to and from the normal grip; **dual-wield parry (sword/rapier main hand)**: when the off hand also holds an arsenal weapon, the off-hand blade uses its `_fpparry` **left-hand** parry transform with the same 0.18 s blend and view lift (sword/rapier provide authored left-hand poses; weapons without one keep their normal off-hand grip and are not lifted); a successful block bursts the same weapon-coloured sparks as third person (14 particles, glowing when glow ink was forged) at the first-person weapon position.

### 8. Enchantments

- Arsenal weapons are enchantable at the **enchanting table, the anvil (books) and `/enchant`**; the three damage enchantments (Sharpness, Smite, Bane of Arthropods) are **mutually exclusive**, exactly like vanilla.
- Natural enchantment lists:
  - **Longsword, Double-Edged Sword, Hengdao, Rapier, Dagger**: Sharpness V, Smite V, Bane of Arthropods V, Knockback II, Looting III, Fire Aspect II, Unbreaking III, Mending I, Sweeping Edge III.
  - **Broadsword, Double-Bladed Axe, Spear**: the same list **without Sweeping Edge**.
  - The spear accepts no trident enchantments (Loyalty, Riptide, Channeling, Impaling); **Density/Breach** are mace-only and cannot be obtained naturally. `/enchant` can force them and the vanilla exclusive sets are still enforced (Density/Breach vs Sharpness/Smite/Bane/Impaling).
- **Visual effect**: no glow ink sac → no effect; with it the blade glow is no longer a single colour but a **medium-speed flowing light** whose colours travel along the blade near the weapon's current colour-ring colour (dye/forge mix) inside a **narrower hue range** (±12°, second harmonic weight 0.25); an uncoloured blade shows a brightness flow.
- **No vanilla enchantment glint**: enchanted arsenal weapons never show the vanilla foil effect; only the mod's own blade glow/flow is used.
- **Tooltip**: the default view shows the **combined percentages**: damage and attack speed **add** the enchantment-provided damage/attack-speed values to the forge percentages (Sharpness/Smite/Bane of Arthropods/Fire Aspect as percentages of the weapon's base attack damage; attack-speed modifiers in the enchantment data are also added to the attack speed percentage), and the enchantments themselves are listed by **name only**. Hold **Shift** for the enchantment names, total stats and forge descriptions.
- **Mending** on an arsenal weapon restores **sharpness** instead of durability (the weapons are unbreakable): **1 experience point restores 20 sharpness** (that experience is consumed; the main hand is repaired first, then the off hand). When the held weapon is already full, experience is gained normally, and leftover experience still repairs other Mending gear.
- **Unbreaking** on an arsenal weapon reduces the **sharpness loss rate** instead of durability, with the vanilla durability formula: with level L each hit/broken block consumes one sharpness point with a probability of 1/(L+1) (I 50%, II 33.3%, III 25%), i.e. the expected loss rate becomes 1/(L+1).

## Weapons

| Weapon | ID | Speed | Damage | DPS | Jump slam | Block | Falling slash | Elytra slash | Role |
|---|---|---|---|---|---|---|---|---|---|
| Longsword | `bettercombat_neonarsenal:sword` | 1.6 | 5.0 | 8.0 | ✓ | ✓ | — | — | One-handed, steady |
| Broadsword | `bettercombat_neonarsenal:claymore` | 0.8 | 11.5 | 9.2 | ✓ | ✓ | ✓ | ✓ | Two-handed, +0.35 reach |
| Double-Edged Sword | `bettercombat_neonarsenal:twinblade` | 1.4 | 7.0 | 9.8 | ✓ | ✓ | ✓ | ✓ | Two-handed combo |
| Double-Bladed Axe | `bettercombat_neonarsenal:doubleaxe` | 0.6 | 15.0 | 9.0 | ✓ | ✓ | ✓ | ✓ | Two-handed spin |
| Hengdao | `bettercombat_neonarsenal:hengdao` | 1.1 | 8.5 | 9.4 | ✓ | ✓ | ✓ | ✓ | Two-handed draw slashes |
| Rapier | `bettercombat_neonarsenal:rapier` | 2.5 | 4.0 | 10.0 | ✓ | ✓ | — | — | One-handed fast thrusts |
| Dagger | `bettercombat_neonarsenal:dagger` | 3.2 | 3.0 | 9.6 | — | — | — | — | One-handed short blade, pure damage |
| Spear | `bettercombat_neonarsenal:spear` | 1.2 | 7.0 | 8.4 | ✓ | ✓ | — | — | Two-handed, +1 reach, **charge thrust** |

(Base damage is unified to **3/4** of the previous value and rounded **up to the next 0.5**; DPS = base damage x attack speed.)

### 9. Charge thrust (mounted spear)

- **Condition**: the main hand holds the spear (`CHARGE_THRUST` skill) and the player is **riding a living mount** (elytra gliding does not trigger it).
- **Short click**: one ordinary spear thrust attack (identical to a normal attack).
- **Hold**: after 0.15 s the charge starts — the spear thrust animation plays and is **frozen at one third** of its length. While charging, the server sweeps every creature along the player's movement inside the forward attack cone each tick and settles charge damage. **Releasing** resumes the whole thrust animation (fading out over 0.3 s once finished) and applies one more strike to creatures in front that were not hit yet.
- **Charge damage**: uses the attacker/target relative velocity projected onto the attack direction; charge damage only applies at **≥ 4.6 blocks/second**. Damage = weapon attack damage × multiplier where multiplier = 1 + (relative speed − 4.6) × 0.15, capped at 4×. Head-on runs hurt the most, chasing a fleeing target gives a low bonus, and moving sideways (about 90°) contributes almost nothing. A contact below the threshold deals no damage (a later faster pass can still hit).
- Hits use the existing particle/sound rules: weapon-coloured sparks when blaze powder was forged, the spear stab sound, and knockback along the attack direction.
- Normal attacks and mining are suppressed while charging, and Better Combat's attack channel is disabled server-side (10 s failsafe; released/aborted charges restore it immediately).
- **Better Combat's slash trail follows the player**: only Better Combat's own slash-trail particles are dragged along with their caster (so the trail no longer hangs still in the air while riding fast); particles generated by this mod (sparks, block burst, ...) are left completely untouched.

## Quick reference

- **Holding animation**: when single-wielding the **Longsword / Broadsword / Double-Edged Sword / Double-Bladed Axe / Hengdao / Rapier / Spear**, the main hand uses the authored holding action (`animations/weapon_holding_action_from_behind.json`, arm + elbow bend) for both idle and moving, and the weapon keeps the vanilla placement/orientation; moving only adds a tiny fore/back swing: walking 5° backward centre with a 4° fan, sprinting 10° backward centre with an 8° fan, standing still no swing. It cross-fades smoothly with attack animations (fades out while swinging, back in afterwards), and **the offhand keeps its original animation when dual wielding** (the dagger keeps its previous holding pose).
- **Cooldowns follow the weapon**: stored on the item as game time; dropping, swapping or rejoining never resets them.
- **Dual wielding**: forge and sharpness effects trigger only from the main hand; off-hand hits trigger nothing; each hand keeps its own appearance.
- **Grindstone**: clears layers, dye, switches, cooldowns and rarity in one operation; sharpness stays.
- **Tooltip**: shows blade sharpness, colour, numeric enchantment/forge bonuses by default; hold Shift for enchantment details, totals and per-layer text.
- **Config**: mod-list sliders — particle density 0–3, blade glow intensity 0–1.
- **JEI**: 272 forging recipes (first/second/third/fifth-copy examples; layers 3-6 are still forged at the anvil) with the anvil as catalyst.
- **Multiplayer**: forging, cooldowns and particle effects stay consistent in multiplayer.

## License

- Code: MIT License ([LICENSE](LICENSE))
- Art assets: CC BY 4.0 ([ASSETS-LICENSE.md](ASSETS-LICENSE.md)); keep the attribution and commercial use is allowed
