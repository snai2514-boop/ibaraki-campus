# iOS 署名・インストール / 签名与安装 / Signing and installation

[日本語](#日本語) · [中文](#中文) · [English](#english)

> **無料の個人署名は通常 7 日ごとに更新 / 免费个人账号签名通常每 7 天续签一次 / Free personal signing normally needs renewal every 7 days.** 这是续签，不要求每周下载新版应用；付费开发者账号期限不同，以描述文件为准。[Apple](https://developer.apple.com/help/account/basics/about-your-developer-account)

## 工具与文件 / ツール / Tools

| 用途 / Purpose | 官方入口 / Official link |
| --- | --- |
| 本项目未签名 IPA / Unsigned app | [下载 IPA](https://github.com/snai2514-boop/ibaraki-campus/releases/download/v1.0.81/CampusAssistant-1.0.81-unsigned.ipa) · [发布页及 SHA256SUMS](https://github.com/snai2514-boop/ibaraki-campus/releases/tag/v1.0.81) |
| Windows / Mac 签名工具 | [Sideloadly 官网及各平台下载](https://sideloadly.io/) · [Windows 64-bit 安装器](https://sideloadly.io/SideloadlySetup64.exe) |
| Windows 所需 Apple 组件 | [Apple 网页版 iTunes 64-bit](https://www.apple.com/itunes/download/win64) · [Apple iCloud 安装器](https://updates.cdn-apple.com/2020/windows/001-39935-20200911-1A70AA56-F448-11EA-8CC0-99D41950005E/iCloudSetup.exe)；链接来自 Sideloadly 官网，请先阅读其依赖说明。 |
| Apple Account | [Apple 账号管理与创建入口](https://account.apple.com/) |
| Mac 开发工具 | [Apple Xcode 官方下载入口](https://developer.apple.com/xcode/) |
| Mac 源码安装 | [最新源码 ZIP](https://github.com/snai2514-boop/ibaraki-campus/archive/refs/heads/main.zip) |
| 开发者模式 / Developer Mode | [Apple 官方步骤](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device) |
| 续签、Wi-Fi 与报错 / Refresh and help | [Sideloadly 官方 FAQ](https://sideloadly.io/faq) |

工具与依赖链接核对于 2026-09-18；若直接下载地址变更，从相应官网重新进入。Windows 与 Mac 任选一种方法，不必安装两套工具。

## 日本語

対象は iOS / iPadOS 16 以降です。[配布ページ](https://github.com/snai2514-boop/ibaraki-campus/releases/tag/v1.0.81)から `CampusAssistant-1.0.81-unsigned.ipa` をダウンロードしてください。Safari で開くだけではインストールできません。`CampusAssistant-Simulator.zip` は Mac のシミュレーター用で、実機には使えません。

### Windows：配布 IPA を署名

1. [Sideloadly 公式サイト](https://sideloadly.io/)からツールを入手し、Windows 用依存ソフトの案内に従います。
2. iPhone / iPad を USB 接続し、ロックを解除して自分のコンピューターを信頼します。
3. Sideloadly で対象デバイスを選び、IPA を読み込み、自分の Apple Account で署名・インストールします。認証要求はツール内で確認してください。
4. 初回起動時に求められた場合は「設定 → 一般 → VPN とデバイス管理」で自分の開発者アカウントを確認します。
5. 「設定 → プライバシーとセキュリティ → デベロッパモード」を有効にし、案内に従って再起動・確認してから起動します。[Apple の説明](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device)

無料署名は通常 7 日で期限切れになります。同じ Apple Account と Bundle ID で再署名して上書きしてください。自動更新にも稼働中のコンピューターとデバイス接続が必要です。[更新・制限・エラーの公式 FAQ](https://sideloadly.io/faq)

### Mac：Xcode からインストール

1. Xcode と[最新ソース](https://github.com/snai2514-boop/ibaraki-campus/archive/refs/heads/main.zip)を用意し、展開した `CampusAssistant.xcodeproj` を開きます。
2. Xcode の Settings → Accounts に自分の Apple Account を追加します。
3. アプリのターゲット `CampusAssistant` → Signing & Capabilities で Automatically manage signing を選び、自分の Team / Personal Team を指定します。
4. Bundle Identifier が使用できない場合は自分用の一意な値に変更し、更新時も同じ値を使います。
5. 接続した実機を実行先に選び、デベロッパモードを有効にして Run（▶）を実行します。無料 Personal Team のプロファイルは 7 日で期限切れになります。[Apple のアカウント説明](https://developer.apple.com/help/account/basics/about-your-developer-account)

実機での署名・学校認証は未検証です。アプリ削除で端末内データを失う可能性があるため、更新前に必要な記録を保存してください。パスワード、認証コード、証明書を Issue や管理者へ送らないでください。

## 中文

需要 iOS / iPadOS 16 或以上。在[发布页](https://github.com/snai2514-boop/ibaraki-campus/releases/tag/v1.0.81)下载 `CampusAssistant-1.0.81-unsigned.ipa`。它尚未签名，不能在 Safari 点开直接安装；`CampusAssistant-Simulator.zip` 只用于 Mac 模拟器。

### 方法一：Windows 使用 Sideloadly

1. 从 [Sideloadly 官网](https://sideloadly.io/)下载软件。按官网 Windows 说明安装所需的 iTunes、iCloud 组件；官网目前要求 Apple 网页版组件，勿把其他下载站当作官网。
2. 用 USB 连接 iPhone / iPad，解锁手机，确认“信任此电脑”，在工具中选择正确设备。
3. 将上述 IPA 拖入工具，填写你自己的 Apple Account，点击 Start。按工具提示完成认证，等待签名和安装结束。Apple 账号和学校账号是两套账号。
4. 若提示“未受信任的开发者”，在手机“设置 → 通用 → VPN 与设备管理”中核对并信任自己的开发者账号。
5. 若要求开发者模式，在“设置 → 隐私与安全性 → 开发者模式”开启，按系统提示重启并确认，再打开应用。[Apple 开发者模式说明](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device)

免费账号通常需要每 7 天续签。同一 Apple Account、同一 Bundle ID 上覆盖安装；不要先卸载应用。开启自动续签后，也需要电脑运行工具后台并通过 USB 或已配置的 Wi-Fi 连接设备，不能保证离线自动续签。[Sideloadly 官方 FAQ](https://sideloadly.io/faq)

### 方法二：Mac 使用 Xcode

1. 安装 Xcode，下载并解压[最新源码](https://github.com/snai2514-boop/ibaraki-campus/archive/refs/heads/main.zip)，打开根目录的 `CampusAssistant.xcodeproj`。
2. Xcode → Settings → Accounts 添加你自己的 Apple Account。
3. 选择项目中的应用 Target `CampusAssistant` → Signing & Capabilities，勾选 Automatically manage signing，Team 选自己的账号或 Personal Team。
4. 如果 Bundle Identifier 提示被占用，换成自己的唯一标识，例如 `com.yourname.campusassistant`；后续更新保持一致。
5. 连接、解锁并信任 iPhone，在运行目标中选这台手机，开启开发者模式，点击 Run（▶）。这里是从源码编译签名，不是拖入发布的 IPA。免费 Personal Team 的描述文件有效期为 7 天，过期后重新运行安装。[Apple 账号说明](https://developer.apple.com/help/account/basics/about-your-developer-account)

### 常见问题

| 现象 | 处理 |
| --- | --- |
| 工具找不到设备 | 解锁、重新连接 USB，确认信任此电脑，并核对官网要求的驱动组件。 |
| 已达免费应用数量上限 | 免费账号通常最多同时安装 3 个此类应用；先备份再移除不需要的应用。[官方限制说明](https://sideloadly.io/faq) |
| 几天后打不开 | 检查签名有效期，使用原账号和原 Bundle ID 续签。 |
| 看不到开发者模式 | 先完成设备配对或开发安装流程，再按 Apple 文档检查；不要安装来历不明的配置文件。 |
| 学校登录或同步失败 | 签名成功只代表能安装；学校连接问题需要单独排查。反馈系统版本、应用版本和去除私人信息的错误文字。 |

本项目没有苹果实机，以上依据官方文档整理，未完成真机签名验证。卸载可能删除本地日程、手工成绩等数据，更新前请自行保存重要记录。不要将 Apple 密码、验证码、签名私钥、学校账号或成绩发到公开 Issue；项目不代收账号或证书。

## English

Requires iOS / iPadOS 16+. Download `CampusAssistant-1.0.81-unsigned.ipa` from the [release page](https://github.com/snai2514-boop/ibaraki-campus/releases/tag/v1.0.81). Opening it in Safari does not install it. `CampusAssistant-Simulator.zip` is for the Mac simulator only.

### Windows: sign the IPA

1. Get [Sideloadly](https://sideloadly.io/) from its official website and follow its Windows dependency instructions.
2. Connect and unlock your device over USB; trust your computer. Select that device, load the IPA, enter your own Apple Account and start signing. Complete authentication in the tool.
3. If prompted, trust your own developer account under Settings → General → VPN & Device Management.
4. Enable Settings → Privacy & Security → Developer Mode; restart and confirm as instructed. See [Apple's guide](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device).

Free signing normally expires after seven days. Refresh using the same account and bundle ID without uninstalling. Automatic refresh requires a running computer and a device connection. See the [official FAQ](https://sideloadly.io/faq).

### Mac: build with Xcode

1. Install Xcode, extract the [source](https://github.com/snai2514-boop/ibaraki-campus/archive/refs/heads/main.zip), and open `CampusAssistant.xcodeproj`.
2. Add your Apple Account in Xcode Settings → Accounts.
3. Select the `CampusAssistant` app target → Signing & Capabilities → Automatically manage signing; choose your Team / Personal Team. Use a unique Bundle Identifier if required, keeping it consistent for updates.
4. Choose your connected device as the destination, enable Developer Mode and click Run. This builds from source. Free Personal Team profiles expire after seven days; run again to renew. See [Apple's account documentation](https://developer.apple.com/help/account/basics/about-your-developer-account) and [automatic signing](https://developer.apple.com/documentation/xcode/distributing-your-app-to-registered-devices).

No physical Apple device was available to verify these instructions. Installation does not establish school-service compatibility. Preserve important local records before updating; uninstalling can erase them. Never post passwords, verification codes, signing keys or school records in public issues.

