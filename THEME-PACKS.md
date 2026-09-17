# 美化包制作 / Theme packs / テーマパック

适用于教务助手 Android / iOS **1.0.82 及以后版本**，仅兼容本项目的 `ibaraki-campus-theme` v1 格式。

## 中文

下载 [青空示例 ZIP](https://github.com/snai2514-boop/ibaraki-campus/releases/download/v1.0.82/CampusAssistant-Sky-theme.zip)，解压后修改 `theme.json`，替换可选背景图，再把文件本身压缩为 ZIP。不要把外层文件夹一起压缩，也不要加入 `.DS_Store`、`__MACOSX` 等文件。

```text
my-theme.zip
├── theme.json
└── background.png       # 可选，也可以使用 background.jpg
```

模板：[theme.json](examples/themes/sky/theme.json)。使用 UTF-8 编码，必须保留 `format: "ibaraki-campus-theme"`、`version: 1`；`name` 为 1–60 字符，`author` 可选，最长 80 字符。

`light` 和 `dark` 都必须提供以下七个颜色，格式为 `#RRGGBB`：

| 字段 | 用途 |
| --- | --- |
| `primary` | 主色、选中态、主要按钮 |
| `onPrimary` | 主要按钮上的文字 |
| `background` | 页面底色 |
| `surface` | 卡片背景 |
| `text` | 正文 |
| `muted` | 次要文字 |
| `accent` | 强调背景、部分选中背景 |
| `dialog` | 弹窗背景（可选，未填写时使用 `surface`） |

背景图可选：设置 `"background": "background.png"` 或 `"background": "background.jpg"`。不要背景时删除这个字段和图片文件。ZIP 根目录只能包含配置和明确引用的背景、启动动画图片。

限制：ZIP ≤ 2,000,000 字节，JSON ≤ 16,384 字节，图片 ≤ 1,000,000 字节；图片每边 ≤ 4096 像素、总像素 ≤ 8,000,000。PNG/JPEG 静态背景以 22% 不透明度、居中裁切显示。浅深色随 App / 系统主题选择；请分别检查正文和按钮对比度。

导入：Android「设置 → 主题设置 → 导入美化包 ZIP」；iOS「设置 → 主题与美化包 → 导入 ZIP」。预览后点击应用；取消不会替换当前主题，恢复默认可撤销。

支持任意配色、静态页面背景、独立弹窗背景、内置桌面图标选择、PNG 帧启动动画、名称和作者。**不支持**页面布局、导航图标替换、自定义字体、任意 CSS/HTML/JavaScript、网络图片和学校网页美化。课程区块、成绩合格/不合格等语义色仍由 App 管理。美化包不能读取课程、账户或改变选课操作。

### 桌面图标和启动动画

`"appIcon": "default"`、`"sky"`、`"night"` 选择随安装包提供的三种桌面图标。iOS 只允许使用编译时内置的备用图标，因此 ZIP 中的任意图片不能直接成为桌面图标。Android 也使用相同清单，便于同一个包跨平台。手机桌面可能需要短暂时间刷新；iOS 可能显示系统更换图标提示。

```json
"appIcon": "sky",
"startup": {
  "frames": ["startup-1.png", "startup-2.png"],
  "frameDurationMs": 150
}
```

将对应 PNG 放在 ZIP 根目录。1–8 帧，文件名只能为 `startup-1.png` 到 `startup-8.png`，每帧 ≤128,000 字节、每边 ≤1024 像素，帧间隔为 80–350 毫秒整数。播放一次，最长 2.8 秒，支持跳过；关闭系统动画或启用减少动态效果时不播放。动画是系统静态启动屏之后的 App 内动画，不能覆盖操作系统的启动屏。ZIP 和所有文件解压后的合计都不能超过 2,000,000 字节。

需要新的桌面图标时，请修改源代码：Android 在 `android/app/src/main/res/drawable/` 添加图标并配置 `AndroidManifest.xml` 的 `activity-alias`；iOS 在 `CampusAssistant/Assets.xcassets/` 添加 App Icon 集合并更新 `tools/generate_project.py` 的备用图标列表。两端主题解析器及切换接口的图标白名单也要一起增加，然后重新构建安装包。[Apple 备用图标说明](https://developer.apple.com/documentation/xcode/configuring-your-app-to-use-alternate-app-icons) · [Android activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)。

制作顺序：复制示例 → 修改两套颜色（不限蓝色）→ 替换背景或删除该字段 → 选择桌面图标 → 可选绘制 PNG 动画帧 → 压缩文件 → 导入预览 → 分别检查浅色和深色下文字、卡片、按钮与弹窗。

## English

For this app only, Android/iOS 1.0.82+. Start with the [Sky ZIP](https://github.com/snai2514-boop/ibaraki-campus/releases/download/v1.0.82/CampusAssistant-Sky-theme.zip) or [JSON template](examples/themes/sky/theme.json). Edit the UTF-8 `theme.json`, keep format/version, and ZIP the files directly without a containing folder. Both `light` and `dark` require all seven `#RRGGBB` colors listed above. An optional `background.png` or `background.jpg` must match the `background` property exactly.

Limits: ZIP 2,000,000 bytes; JSON 16,384; image 1,000,000 bytes, 4096 pixels per edge and 8 million pixels total. Background opacity is 22%, centered and cropped. Import from Settings → Theme, preview, then apply. Cancel preserves the old pack; restore defaults removes the custom appearance. Supports arbitrary light/dark colors, static backgrounds, a separate optional `dialog` color (defaults to `surface`), bundled desktop icons (`default`, `sky`, `night`) and PNG startup frames. The `startup` object above lists 1–8 root-level PNG files, each ≤128,000 bytes and ≤1024 pixels per edge, at an integer 80–350 ms per frame. The one-shot animation is skippable and respects reduced motion. Total expanded bytes must also be ≤2,000,000. Arbitrary imported desktop icons are unavailable on iOS; add bundled icons in source and rebuild. No layouts, navigation icon/font replacements, executable code, URLs, or school-page styling. Semantic course/grade colors remain app-controlled.

## 日本語

このアプリ専用です（Android / iOS 1.0.82 以降）。[青空サンプル](https://github.com/snai2514-boop/ibaraki-campus/releases/download/v1.0.82/CampusAssistant-Sky-theme.zip)を展開し、UTF-8 の `theme.json` を編集してください。`format` と `version` は変更せず、`light` / `dark` の七色を `#RRGGBB` 形式で指定します。外側のフォルダーを含めず、ファイルを直接 ZIP にしてください。背景は省略可能で、`background` の名前と PNG/JPEG ファイル名を一致させます。

上限：ZIP 2,000,000 バイト、JSON 16,384 バイト、画像 1,000,000 バイト・一辺 4096 ピクセル・合計 800 万ピクセル。背景は不透明度 22%、中央配置・トリミング表示です。設定のテーマ画面で読み込み、プレビュー後に適用します。キャンセルで従来のテーマを維持し、初期設定に戻すこともできます。任意の配色、静止背景、独立した `dialog` 色（省略時 `surface`）、内蔵ホーム画面アイコン（`default` / `sky` / `night`）、PNG 起動アニメーションに対応します。上記 `startup` 設定に 1〜8 枚の PNG を指定し、各128,000バイト・一辺1024px以下、間隔80〜350msの整数とします。1回再生・スキップ可能で、動きを減らす設定に従います。展開後も合計2,000,000バイト以下です。iOSでは任意の画像をホーム画面アイコンとして読み込めず、ソースへの追加と再ビルドが必要です。レイアウト、ナビゲーションアイコン、フォント、スクリプト、外部URL、大学サイトの変更は非対応です。
