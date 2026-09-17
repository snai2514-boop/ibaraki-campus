# 教务助手 · iPhone / iPad

独立苹果兼容客户端，最低 iOS 16。UIKit / WKWebView 容器搭配本地打包界面，学校登录使用独立 WKWebView。个人数据保存在设备，没有数据上传服务。

支持首页、课程、学分/GPA、周/月日历、公告、学校前台同步、线上授课识别和可登录课程。选课只提交勾选项，必须二次确认，结果不明时防止重复提交。仅使用已核对校历，不猜测未知日期。

初始版本尚未覆盖 Android 全部功能，验证边界见 [VALIDATION.md](VALIDATION.md)。

## Mac 构建

安装 Xcode 并完成首次启动，准备 Python 3 与 Node.js 后运行：

```sh
bash tools/build.sh
```

产物在 build/dist/：
- CampusAssistant-1.0.80-unsigned.ipa：ARM64 设备包，未签名，不能直接安装。
- CampusAssistant-Simulator.zip：Mac iOS 模拟器专用，不可装到 iPhone。
- SHA256SUMS.txt：校验值。

安装到自己的 iPhone：在 Xcode 打开 CampusAssistant.xcodeproj，在 Signing & Capabilities 选择自己的 Team，必要时使用自己的 Bundle Identifier，连接设备运行；或使用可信个人签名工具对 IPA 签名。不要上传 Apple 密码、证书或学校会话。

云构建配置 ci/ios-build.yml 在独立仓库映射为 .github/workflows/ios-build.yml。使用有时间上限的 macOS runner，无需学校或 Apple 密钥，不开启定时任务。

## 本地预览和测试

```sh
node --test --test-isolation=none Tests/core.test.cjs
python3 tools/audit.py
python3 -m http.server 8765 --bind 127.0.0.1 --directory CampusAssistant/Web
```

打开 http://127.0.0.1:8765/?preview=1 。预览数据完全虚构，不连接学校，不提交登记。浏览器预览不等于 iOS 模拟器验证。

## 隐私和源码

仅 tools/audit.py 允许的源码、资源、测试、说明进入上传包。学校会话、学生信息、本机日志、签名密钥、构建缓存和 Android 仓库历史不上传。

见 [NOTICE.md](NOTICE.md)、[LICENSE](LICENSE)、[DATA-SOURCES.md](DATA-SOURCES.md)。非学校官方应用。
