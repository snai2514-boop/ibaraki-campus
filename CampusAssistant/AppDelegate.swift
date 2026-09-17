import UIKit
import WebKit
import UserNotifications

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?
    private var controller: CampusController?
    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let root = CampusController()
        controller = root
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = root
        window?.makeKeyAndVisible()
        return true
    }
    func applicationDidBecomeActive(_ application: UIApplication) { controller?.foreground() }
    func applicationDidEnterBackground(_ application: UIApplication) { controller?.background() }
}

/// Only bundled UI has a native bridge. School documents never receive a message handler.
final class CampusController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate, WKUIDelegate {
    private var interface: WKWebView!
    private var school: WKWebView!
    private var toolbar: UIToolbar!
    private var loaded = false
    private var showingLogin = false
    private var homeRequest: String?
    private var privacyCover = UIView()
    private let portal = URL(string: "https://csweb.ibaraki.ac.jp/campusweb/")!
    private let hosts: Set<String> = ["csweb.ibaraki.ac.jp", "sidp.ibaraki.ac.jp", "login.microsoftonline.com"]
    private let allowedScripts = Set(["extract", "school-registration", "school-sync-navigation", "school-course-modes", "school-notices", "school-syllabus", "school-classrooms"])
    private var dataURL: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("campus-state.json")
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.945, green: 0.969, blue: 0.992, alpha: 1)
        let schoolConfig = WKWebViewConfiguration()
        schoolConfig.websiteDataStore = .default()
        school = WKWebView(frame: .zero, configuration: schoolConfig)
        school.navigationDelegate = self
        school.uiDelegate = self
        school.accessibilityElementsHidden = true
        school.alpha = 0.01
        view.addSubview(school)
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .nonPersistent()
        config.userContentController.add(self, name: "campus")
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--preview") {
            config.userContentController.addUserScript(WKUserScript(source: "window.__CAMPUS_PREVIEW__ = true;", injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        #endif
        interface = WKWebView(frame: .zero, configuration: config)
        interface.navigationDelegate = self
        interface.isOpaque = true
        interface.backgroundColor = view.backgroundColor
        view.addSubview(interface)
        toolbar = UIToolbar()
        toolbar.items = [UIBarButtonItem(title: "学校登录", style: .plain, target: nil, action: nil), UIBarButtonItem(systemItem: .flexibleSpace), UIBarButtonItem(title: "完成登录并同步", style: .done, target: self, action: #selector(closeLogin))]
        toolbar.isHidden = true
        view.addSubview(toolbar)
        privacyCover.backgroundColor = view.backgroundColor
        privacyCover.isHidden = true
        view.addSubview(privacyCover)
        guard let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "Web") else { return }
        interface.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
    }
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let safe = view.safeAreaInsets
        interface.frame = view.bounds
        school.frame = CGRect(x: 0, y: safe.top + (showingLogin ? 44 : 0), width: view.bounds.width, height: view.bounds.height - safe.top - safe.bottom - (showingLogin ? 44 : 0))
        toolbar.frame = CGRect(x: 0, y: safe.top, width: view.bounds.width, height: 44)
        privacyCover.frame = view.bounds
    }
    func foreground() {
        privacyCover.isHidden = true
        if loaded && !showingLogin { interface.evaluateJavaScript("window.nativeForeground && window.nativeForeground()", completionHandler: nil) }
    }
    func background() {
        privacyCover.isHidden = false
        school.stopLoading()
        if let id = homeRequest { homeRequest = nil; reply(id, error: "应用已进入后台，同步已暂停") }
        interface.evaluateJavaScript("window.nativeBackground && window.nativeBackground()", completionHandler: nil)
    }
    private func allowed(_ url: URL?) -> Bool {
        guard let u = url else { return false }
        return u.scheme == "https" && u.user == nil && u.password == nil && (u.port == nil || u.port == 443) && hosts.contains(u.host?.lowercased() ?? "")
    }
    private func readable(_ url: URL?) -> Bool { allowed(url) && url?.host == "csweb.ibaraki.ac.jp" && (url?.path.hasPrefix("/campusweb/") ?? false) }
    private func json(_ value: Any) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: value, options: [.fragmentsAllowed]), let text = String(data: data, encoding: .utf8) else { return "null" }
        return text
    }
    private func reply(_ id: String, result: Any? = nil, error: String? = nil) {
        interface.evaluateJavaScript("window.nativeReply(\(json(id)),\(json(result ?? NSNull())),\(json(error ?? NSNull())))", completionHandler: nil)
    }
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.webView === interface, message.frameInfo.isMainFrame,
              let source = message.frameInfo.request.url, source.isFileURL,
              source.standardizedFileURL.path == Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "Web")?.standardizedFileURL.path,
              let body = message.body as? [String: Any], let id = body["id"] as? String, let action = body["action"] as? String else { return }
        let payload = body["payload"] as? [String: Any] ?? [:]
        switch action {
        case "load":
            do {
                if FileManager.default.fileExists(atPath: dataURL.path) {
                    let data = try Data(contentsOf: dataURL)
                    reply(id, result: try JSONSerialization.jsonObject(with: data))
                } else { reply(id) }
            } catch { reply(id, error: "无法读取本机记录，原文件已保留") }
        case "save":
            do {
                guard let state = payload["state"] as? [String: Any] else { throw NSError(domain: "InvalidState", code: 1) }
                let data = try JSONSerialization.data(withJSONObject: state)
                guard data.count <= 8_000_000 else { throw NSError(domain: "StateTooLarge", code: 1) }
                let dir = dataURL.deletingLastPathComponent()
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
                try data.write(to: dataURL, options: [.atomic, .completeFileProtection])
                var file = dataURL
                var values = URLResourceValues(); values.isExcludedFromBackup = true
                try file.setResourceValues(values)
                reply(id, result: true)
            } catch { reply(id, error: "保存失败，请保留应用并重试") }
        case "home":
            guard homeRequest == nil else { reply(id, error: "学校页面正在加载"); return }
            homeRequest = id; school.load(URLRequest(url: portal))
            DispatchQueue.main.asyncAfter(deadline: .now() + 18) { [weak self] in
                guard let self = self, self.homeRequest == id else { return }
                self.homeRequest = nil; self.school.stopLoading(); self.reply(id, error: "学校响应超时，保留原记录")
            }
        case "login":
            showingLogin = true; toolbar.isHidden = false; interface.isHidden = true
            school.alpha = 1; school.accessibilityElementsHidden = false
            view.setNeedsLayout()
            if school.url == nil { school.load(URLRequest(url: portal)) }
            reply(id, result: true)
        case "read":
            guard readable(school.url), let name = payload["script"] as? String, allowedScripts.contains(name),
                  let url = Bundle.main.url(forResource: name, withExtension: "js", subdirectory: "SchoolScripts"), var script = try? String(contentsOf: url, encoding: .utf8) else {
                reply(id, error: "请先完成学校登录"); return
            }
            let request = payload["request"] as? [String: Any] ?? [:]
            script = script.replacingOccurrences(of: "__REQUEST__", with: json(request))
            script = script.replacingOccurrences(of: "__TARGET__", with: json(request["target"] ?? ""))
            script = script.replacingOccurrences(of: "__DETAIL__", with: json(request["detail"] ?? NSNull()))
            school.evaluateJavaScript(script) { [weak self] result, error in
                guard let self = self else { return }
                guard self.readable(self.school.url) else { self.reply(id, error: "学校登录已变化，请重新登录"); return }
                if error != nil { self.reply(id, error: "学校页面正在变化，请重试") } else { self.reply(id, result: result) }
            }
        case "permission":
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in DispatchQueue.main.async { self.reply(id, result: granted) } }
        case "notify":
            let content = UNMutableNotificationContent()
            content.title = "教务助手"; content.body = String((payload["message"] as? String ?? "").prefix(1000)); content.sound = .default
            UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil))
            reply(id, result: true)
        case "logout":
            school.stopLoading()
            do { if FileManager.default.fileExists(atPath: dataURL.path) { try FileManager.default.removeItem(at: dataURL) } }
            catch { reply(id, error: "本机记录清除失败"); return }
            UNUserNotificationCenter.current().removeAllDeliveredNotifications()
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            WKWebsiteDataStore.default().removeData(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince: .distantPast) {
                self.school.load(URLRequest(url: self.portal)); self.reply(id, result: true)
            }
        default: reply(id, error: "不支持的操作")
        }
    }
    @objc private func closeLogin() {
        showingLogin = false; toolbar.isHidden = true; interface.isHidden = false
        school.alpha = 0.01; school.accessibilityElementsHidden = true; view.setNeedsLayout()
        interface.evaluateJavaScript("window.nativeLoginClosed()", completionHandler: nil)
    }
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if webView === interface {
            let root = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "Web")
            decisionHandler(navigationAction.request.url?.standardizedFileURL.path == root?.standardizedFileURL.path ? .allow : .cancel)
        } else { decisionHandler(allowed(navigationAction.request.url) ? .allow : .cancel) }
    }
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        if webView === interface { loaded = true }
        else if let id = homeRequest { homeRequest = nil; reply(id, result: true) }
    }
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) { failNavigation(webView, error) }
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) { failNavigation(webView, error) }
    private func failNavigation(_ web: WKWebView, _ error: Error) {
        if web === school, let id = homeRequest { homeRequest = nil; reply(id, error: "学校页面加载失败，请检查网络或重新登录") }
    }
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        if webView === interface { loaded = false; interface.reload() }
        else { interface.evaluateJavaScript("window.nativeBackground()", completionHandler: nil) }
    }
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if webView === school && allowed(navigationAction.request.url) { school.load(navigationAction.request) }
        return nil
    }
    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        let alert = UIAlertController(title: "学校确认", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "取消", style: .cancel) { _ in completionHandler(false) })
        alert.addAction(UIAlertAction(title: "确认", style: .default) { _ in completionHandler(true) })
        present(alert, animated: true)
    }
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: "学校提示", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default) { _ in completionHandler() }); present(alert, animated: true)
    }
}
