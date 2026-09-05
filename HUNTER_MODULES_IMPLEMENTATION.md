# Hunter Modules Implementation Summary

## 完成时间
2026-09-05

## 实现的模块

### 1. FishHunter (鱼类猎手)
**文件**: `src/main/java/xyz/whatsyouss/frosty/modules/impl/beta/FishHunter.java`

**功能**:
- 自动扫描并狩猎附近的鱼类（鳕鱼、鲑鱼、热带鱼）
- 智能追击系统（扫描 -> 追击 -> 攻击）
- 距离控制（最小距离和最大范围）
- 自动武器选择
- 黑名单系统（避免重复攻击无法击杀的目标）

**设置**:
- 范围: 1-32方块（默认16）
- 最小距离: 1-10方块（默认3）
- 自动选择武器: 开关（默认开启）

**状态机**:
- IDLE: 空闲
- SCANNING: 扫描目标
- CHASING: 追击目标
- ATTACKING: 攻击目标

### 2. TurtleHunter (海龟猎手)
**文件**: `src/main/java/xyz/whatsyouss/frosty/modules/impl/beta/TurtleHunter.java`

**功能**:
- 自动扫描并狩猎附近的海龟
- 可选仅攻击幼体海龟
- 智能追击系统
- 距离控制
- 自动武器选择
- 黑名单系统

**设置**:
- 范围: 1-32方块（默认20）
- 最小距离: 1-10方块（默认2）
- 自动选择武器: 开关（默认开启）
- 仅攻击幼体: 开关（默认关闭）

**状态机**:
- IDLE: 空闲
- SCANNING: 扫描目标
- CHASING: 追击目标
- ATTACKING: 攻击目标

## 技术实现

### 核心特性

1. **实体识别**
   - 使用 `EntityType` 进行类型检查
   - FishHunter: `COD`, `SALMON`, `TROPICAL_FISH`
   - TurtleHunter: `TURTLE` (可选过滤幼体)

2. **寻路与追击**
   - 使用 `RotationUtils.getYawPitchTo()` 计算视角
   - 平滑旋转到目标
   - 自动前进/后退控制

3. **武器选择**
   - 自动扫描快捷栏
   - 选择攻击力最高的物品
   - 使用 `InventoryAccessor` mixin 访问选中槽位

4. **黑名单机制**
   - 攻击超时后将目标加入黑名单
   - 自动清理过期的黑名单条目
   - 防止卡在无法击杀的目标上

5. **距离控制**
   - 最小距离: 避免过近
   - 最大范围: 扫描和攻击限制
   - 动态调整（允许1.5倍超出）

### Mixin 实现

**InventoryAccessor**
```java
@Mixin(Inventory.class)
public interface InventoryAccessor {
    @Accessor("selected")
    int getSelected();
    
    @Accessor("selected")
    void setSelected(int slot);
}
```

用途: 访问玩家当前选中的快捷栏槽位

### 模块注册

两个模块已注册到 `ModuleManager`:
- 位于 Beta 类别
- 在游戏启动时自动加载

## 源自 Booter Client

这些模块是基于 Booter Client 1.0.0 的字节码分析重新实现的：
- 保留了核心逻辑和状态机设计
- 适配了 Frosty 的架构和 API
- 使用了 Minecraft 1.21.4 的实体系统

## 构建验证

✅ 编译成功
✅ 模块已打包到 jar
✅ 无编译错误或警告（除了标准的 unchecked 警告）

生成的文件:
- `build/libs/Frosty-1.3.0-beta.2+26.1.2.jar` (2.6 MB)

## 使用说明

1. 安装 mod 到 Minecraft 1.21.4 (Fabric)
2. 进入游戏后打开 mod 菜单
3. 找到 Beta 类别
4. 启用 "FishHunter" 或 "TurtleHunter"
5. 调整范围和距离设置
6. 模块将自动寻找并攻击目标

## 注意事项

⚠️ **服务器使用警告**
- 这些模块在多人服务器上可能违反服务器规则
- 可能被反作弊插件检测
- 建议仅在单人或允许 mod 的服务器使用

## 后续改进建议

1. 添加目标优先级（优先攻击稀有鱼类）
2. 实现路径规划避障
3. 添加水下寻路支持
4. 实现更智能的武器选择（考虑耐久度）
5. 添加统计信息（击杀数量、收集物品等）
