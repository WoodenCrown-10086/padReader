# 翻译透镜（Translens）

一个「全局翻译透镜」Android 应用：屏幕上常驻一个可拖动、可缩放的 1px 高亮框，框内实时显示对应屏幕区域内容的译文，框外屏幕原样。适合在平板/手机上阅读文献、网页或其他 App 时随时翻译。

> 目标设备：华为 MatePad 11（HarmonyOS 4.2.0，Android 兼容层）。可扩展到其他安卓平板（见「跨设备兼容」）。

---

## 当前进度（Phase 0 骨架）

**已可运行：**

- 可拖动、可缩放的透镜悬浮窗（窗口本身即框，1px 边框）；
- 无障碍取字（直读其他 App 的文本节点，零截图）；
- 事件驱动流水线（内容变化才翻译，静止时 CPU≈0）；
- 翻译结果 LRU 缓存；
- 占位翻译器 `NoopTranslator`（原样返回，先跑通整条链路）。

**暂未接入（后续 Phase）：**

- HMS ML Kit 翻译 / OCR（Phase 1，见下）；
- 截图 OCR 兜底（Phase 1）；
- 原位覆盖的精细排版（Phase 2）；
- 整屏快照翻译、朗读、复制（Phase 3）。

> 因此当前安装后，框内显示的是**原文本身**（因为翻译器还是占位实现），用于验证「取字 → 翻译 → 渲染」链路已经打通。

---

## 环境要求

| 项 | 要求 |
|---|---|
| Android Studio | 最新稳定版（自带 JDK 17） |
| Android SDK | Platform 34 |
| 设备 | Android 7.0+（本机 HarmonyOS 4.2 兼容） |
| 华为开发者账号 | 仅 Phase 1 接入 HMS 时需要 |

## 打开与构建

1. 用 Android Studio 打开本目录（`D:\padView`）。
2. 首次打开会自动下载 Gradle 8.9 与依赖，等待 Sync 完成。
3. 连接 MatePad（需开启开发者模式 + USB 调试）。
4. 点击 Run，安装到设备。

> 命令行构建需要 `gradle-wrapper.jar`。若目录里没有，用 Android Studio 打开一次后它会生成，或在 Studio 里执行一次 `gradle wrapper`。

## 真机使用步骤

1. 打开 App → 点「开启悬浮窗权限」授权（首次点「开启翻译透镜」也会引导）。
2. 点「开启无障碍服务」，在系统无障碍设置里打开「翻译透镜取字服务」。
3. 回到 App 点「开启翻译透镜」。
4. 回到任意 App，拖动/缩放蓝色透镜框，框内即显示读取到的文本。

## 华为设备额外设置（防止被杀后台）

- 设置 → 应用 → 翻译透镜 → 允许「自启动」「关联启动」「后台弹窗」；
- 设置 → 电池 → 更多设置，关闭对本应用的省电限制（或点 App 内「电池优化白名单」）。

---

## 目录结构

```
app/src/main/java/com/translens/app/
├── core/                    # 平台无关算法（可移植到 ArkTS）
│   ├── model/               # TextSpan / Lang / RenderMode
│   ├── translate/           # Translator 接口 + 各实现
│   └── layout/              # LayoutEngine + RenderPlan
├── source/                  # 取字：TextSource + 无障碍 + 截图OCR兜底
├── ocr/                     # OcrEngine 抽象 + HMS/Paddle 实现
├── pipeline/                # TranslationPipeline + RenderSink
├── overlay/                 # LensView + LensOverlayService
├── detect/ChangeDetector.kt # 内容变化检测
├── cache/TranslationCache.kt# LRU 缓存
├── compat/DeviceCompat.kt   # 厂商 ROM 差异
├── settings/SettingsRepo.kt # DataStore 配置
└── ui/MainActivity.kt       # 引导/配置入口
```

## 设计要点（健壮性与可维护性）

- **接口抽象**：取字、翻译、OCR 全部走接口，实现可替换、可测试。
- **错误处理**：取字返回 `Result`，翻译异常由 `TranslationPipeline` 统一捕获降级，不崩溃。
- **并发安全**：`Mutex` 串行化刷新，避免渲染乱序；缓存内部加锁。
- **事件驱动**：无障碍内容变化 + 手势松手才触发刷新，静止零开销。
- **手动依赖注入**：`AppContainer` 集中管理单例，无反射、无代码生成。

---

## 接入 HMS ML Kit（Phase 1）

1. 注册华为开发者账号，在 [AppGallery Connect](https://developer.huawei.com/consumer/cn/service/josp/agc/index.html) 创建应用，包名填 `com.translens.app`。
2. 下载 `agconnect-services.json` 放到 `app/` 目录（已在 `.gitignore` 中忽略）。
3. `settings.gradle.kts` 取消注释华为 maven 仓库：
   ```kotlin
   maven { url = uri("https://developer.huawei.com/repo/") }
   ```
4. 根 `build.gradle.kts` 接入 agconnect 插件；`app/build.gradle.kts` 取消注释 HMS ML Kit 依赖。
5. 实现 `HmsTranslator` / `HmsOcr`（文件内已给出示意代码）。
6. 在 `TranslensApp` 的 `AppContainer` 中把 `translator` 换成 `HmsTranslator()`。

## 跨设备兼容（其他安卓平板）

- 无 GMS 也无 HMS 的设备：用 `PaddleOcr`（纯开源本地 OCR）+ `CloudTranslator` 兜底；
- 厂商差异（悬浮窗/后台白名单/电池优化路径）统一走 `DeviceCompat`；
- 若未来升级 HarmonyOS NEXT（纯血鸿蒙），`core/` 算法层为平台无关设计，可移植 ArkTS。

---

## 许可与说明

- 本项目为学习/自用骨架，图标为临时占位，上线前需替换。
- 无障碍服务仅用于读取文本，不修改内容、不上传数据；云翻译为显式可选，开启前需向用户明示。
