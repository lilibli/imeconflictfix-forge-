# ImeConflictFix — 输入法冲突修复

Minecraft 模组，在需要输入文字时自动开启中文输入法，不需要时自动关闭，彻底告别"按空格跳起来""按 E 打开背包却打字"的烦恼。

## 版本信息

| 项目 | 版本 |
|------|------|
| Minecraft | 1.21.10 |
| Forge | 60.1.0 |
| Java | JDK 21 |
| 模组加载器 | Forge（非 NeoForge） |

## 灵感来源

本模组参考了 [IMBlocker](https://github.com/reserveword/IMBlocker) 的 imm32 方案，通过 JNA 调用 Windows 输入法接口，在全屏模式下自绘 IME 组合文字和候选列表。

## 支持的游戏界面

| 界面 | 自动开中文 | 说明 |
|------|:---:|------|
| 聊天框 | ✅ | 命令模式自动切英文 |
| 告示牌 / 悬挂告示牌 | ✅ | |
| 书与笔 | ✅ | |
| 铁砧 | ✅ | |
| 创造模式搜索 | ✅ | 点搜索框自动开，离开自动关 |
| 任意有输入框的界面 | ✅ | 帧级自动检测 |

关闭任何界面 → 强制切回英文，Shift 等按键不影响游戏操作。

## 使用方法

1. 下载 `imeconflictfix-x.x.x.jar`
2. 放入 Minecraft 的 `mods` 文件夹
3. 启动游戏，无需任何配置

> ⚠️ 仅支持 Windows 系统（依赖 imm32.dll）。

## 构建

```bash
./gradlew build
# 输出：build/libs/imeconflictfix-x.x.x.jar
```

## 作者

| 角色 | 名字 |
|------|------|
| 作者 | bigshua |
| 调试 & 测试 | 狸狸本狸 |

## 许可证

GNU Affero General Public License v3.0 (AGPL-3.0)
