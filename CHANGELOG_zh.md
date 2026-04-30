## AE2Enhanced 1.2.0

**适用版本**: Minecraft 1.12.2 / Forge 14.23.5.2768+ / AE2-UEL v0.56.7+

---

### ✨ 新增：超维度仓储中枢（第二阶段）

本次更新的核心内容——**超维度仓储中枢**，一个 21 格平面多方块存储结构，专为解决后期 AE2 网络容量瓶颈而设计。

- **BigInteger 级容量**：单结构存储容量理论上无上限，彻底绕过 `int`/`long` 限制
- **多类型兼容**：原生支持物品与流体；可选支持 Mekanism 气体与 Thaumcraft 源质（安装对应模组后自动启用）
- **外部文件持久化**：数据存储于 `<world>/ae2enhanced/storage/<uuid>.dat`，使用压缩 NBT + 原子写入，从根本上避免 NBT 溢出和存档卡顿
- **安全模式**：文件异常时自动进入只读状态，防止数据损坏；支持安全的 Mod 版本更新，不会因更新丢失物品
- **第三方扩展 API**：其他模组可通过 `registerExternalAdapter()` 接入自定义存储类型
- **全息投影渲染**：结构上方渲染超立方体 TESR（双旋转立方体 + 光环 + 脉冲核心）；可配置渲染开关与最大距离
- **能量粒子**：结构运行时生成能量粒子向中心汇聚

### ⚙️ 新增：配置文件系统

位于 `config/ae2enhanced.cfg`：

| 参数 | 说明 |
|---|---|
| `flushIntervalSeconds` | 存储文件自动刷盘间隔（秒，默认 5） |
| `enableHyperdimensionalRenderer` | 是否启用特效渲染（默认 true） |
| `renderDistance` | 最大渲染距离（方块，默认 64） |
| `damageMode` | 黑洞伤害模式：ALL / NON_CREATIVE / NONE（默认 ALL） |

### 🔧 改进与修复

- **配方**：为超维度仓储中枢的 4 种方块添加合成配方
- **本地化**：补全 en_us / zh_cn / ru_ru 语言文件；修复速度升级卡名称缺失的问题
- **黑洞**：移除装配枢纽事件视界的"16 次未击杀则传送放逐"保底逻辑；新增 `damageMode` 配置，创造模式玩家可设为免疫
- **渲染修复**：修正超维度控制器 TESR 的渲染距离计算错误；粒子现在围绕多方块几何中心而非控制器方块生成

---

### 📥 依赖

**必需**：Minecraft 1.12.2 · Forge 14.23.5.2768+ · AE2-UEL v0.56.7+ · MixinBooter 8.9+

**可选**：Mekanism + MekanismEnergistics（气体存储）· Thaumcraft + Thaumic Energistics（源质存储）
