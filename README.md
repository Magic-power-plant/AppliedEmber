# AppliedEmber

Minecraft 1.20.1（Forge 47.x）mod：Applied Energistics 2 与 Embers Rekindled 的联动。它把 Embers 的余烬（Ember）能量注册为 ME 网络中的一种 AEKey，从而可以在 ME 网络里存储、传输和自动搬运余烬。

- mod id：`appliedember`
- 当前版本：0.1.7
- 协议：GNU GPL 3.0

## 依赖

| 依赖 | 版本 | 必需性 |
|---|---|---|
| Minecraft / Forge | 1.20.1 / 47.x | 必需 |
| Applied Energistics 2 | 15.4.x | 必需（硬依赖） |
| Embers Rekindled | CurseMaven file 7279674 | 必需（硬依赖） |
| MEGA Cells | CurseMaven file 6175008 | 可选（加载时注册 MEGA 级余烬元件） |

开发环境中 AE2 的 GuideME 与 MEGA Cells 的 Cloth Config 作为运行时依赖参与 dev 运行（用于数据生成），不随本 mod 分发。

## 功能

**ME 余烬元件方块（ME Ember Cell）**
接入 ME 网格，作为网络侧的 Ember capability：相邻的 Embers 机器可以像访问普通余烬储罐一样向网络读取/写入余烬。方块每 10 tick 向六邻方块主动推送余烬（带 remainder 回传，不会凭空丢失能量），并提供比较器信号。

**余烬存储元件（Ember Storage Cell）**
1k / 4k / 16k / 64k / 256k 五档，使用 AE2 的存储组件和元件外壳（`ember_cell_housing`）合成，可直接放入 AE2 驱动器（Drive），并带驱动器面板贴图。

**便携余烬元件（Portable Ember Cell）**
每个档位各一个，复用 AE2 的便携元件界面，以 AE 能量驱动。

**P2P 余烬通道（Ember P2P Tunnel）**
在 AE2 P2P 通道上搬运余烬。

**ME总线**
- 外部存储总线：把 Embers 储罐接入 ME 网络
- 输入/输出总线：在 Embers 设备与网络之间自动搬运余烬
- 容器物品策略：支持含 Ember capability 的物品容器

**MEGA Cells 联动（可选）**
加载 MEGA Cells 时注册 MEGA 级余烬元件。

## 开发

基于 ForgeGradle 6 + official mappings（Java 17）：

```bash
./gradlew build      # 构建（产物在 build/libs/）
./gradlew test       # 纯逻辑单元测试（JUnit 5，无需启动游戏）
./gradlew runData    # 数据生成（语言/模型/战利品表/标签，输出到 src/generated/resources）
./gradlew runClient  # 客户端 dev 运行
./gradlew runServer  # 服务端 dev 运行
./gradlew runGameTestServer  # GameTest 服务器
```

项目自带一组 JUnit 5 单元测试（`src/test`）和游戏内 GameTest（`gametest.AppliedEmberGameTests`）；资源（模型、语言、方块状态、战利品表、标签）全部由 datagen 生成，请勿手改 `src/generated/resources/` 下的产物。

代码结构：

- `me/` — EmberKey / EmberKeyType（AE2 能量类型）、存储策略（外部存储/输入/输出）、数值换算工具
- `content/ember/` — ME 余烬元件方块与实体、capability 桥接
- `content/cell/` — 存储元件、便携元件、MEGA 级元件
- `content/p2p/` — P2P 通道
- `datagen/` — 数据生成器
- `gametest/`、`src/test/` — 游戏内测试与单元测试

## 已知事项

- AE2 的若干扩展点（`ExternalStorageStrategy`、`GenericInternalInventory` 等）标注为 `@ApiStatus.Experimental`，AE2 升级小版本时需回归验证。
