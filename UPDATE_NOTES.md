# Frosty 1.3.0+26.1.2 更新说明

## 🎯 本次更新

### ✅ 已修复
- **Nametags（名字标签）功能**
  - ✅ 强制显示所有玩家名字标签（即使潜行状态）
  - ✅ 自定义缩放控制（0.05x - 5.0x）
  - ✅ 距离衰减算法（10格以上自动缩放）

- **Xray（透视）功能**
  - ✅ 原版渲染器完全支持
  - ✅ 矿物 ESP 正常工作
  - ✅ 透明度控制正常

### ⚠️ 已知限制
- **Sodium 支持暂时禁用**
  - 原因：Minecraft 26.1.2 暂无兼容的 Sodium 版本
  - 影响：Xray 只能使用原版渲染器（功能完整，性能略低）
  - 备注：代码已保留，等待未来 Sodium 版本发布时启用

## 📦 下载与安装

1. 下载 `Frosty-1.3.0+26.1.2.jar` （位于 `build/libs/`）
2. 将 jar 文件放入 `.minecraft/mods/` 文件夹
3. 确保安装了 Fabric Loader 和 Fabric API
4. 启动游戏

## 🎮 使用方法

### Nametags（名字标签）
```
/toggle nametags          # 开关功能
设置 > Render > Nametags  # 在 GUI 中调整缩放
```

### Xray（透视）
```
/toggle xray              # 开关功能
设置 > Render > Xray      # 在 GUI 中选择矿物和透明度
```

## 🔧 技术实现

### 新增文件
- `SubmitNodeCollectorMixin.java` - 拦截名字标签缩放

### 修改文件
- `FrostyMixinPlugin.java` - 添加 Sodium 运行时检测
- `frosty.mixins.json` - 注册新 mixin
- `build.gradle` - 添加 Modrinth 仓库

### 禁用文件
- `SodiumBlockRendererXrayMixin.java.disabled` - 保留代码，等待 Sodium 兼容

## 📝 详细文档

完整技术文档请参阅 [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)

## 🐛 报告问题

如遇到问题，请提供：
1. 具体复现步骤
2. Minecraft 版本（应为 26.1.2）
3. 是否安装了 Sodium（如有，请提供版本）
4. 游戏日志（`.minecraft/logs/latest.log`）

---

**版本：** 1.3.0+26.1.2  
**构建日期：** 2026-09-04  
**适配版本：** Minecraft 26.1.2
