import XCTest

final class CampusUITests: XCTestCase {
    func testPreviewRegistrationAndQuarterNavigation() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--preview"]
        app.launch()
        let web = app.webViews.firstMatch
        XCTAssertTrue(web.buttons["查看 →"].waitForExistence(timeout: 15))
        if !web.buttons["查看 →"].isHittable { web.swipeUp() }
        web.buttons["查看 →"].tap()
        let multiple = web.buttons.containing(NSPredicate(format: "label CONTAINS %@", "2 门可选")).firstMatch
        XCTAssertTrue(multiple.waitForExistence(timeout: 10))
        multiple.tap()
        let choice = web.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "统计学基础 火2")).firstMatch
        XCTAssertTrue(choice.waitForExistence(timeout: 10))
        choice.tap()
        web.buttons["完成"].tap()
        XCTAssertTrue(web.staticTexts["已选 1 门"].waitForExistence(timeout: 10))
        web.buttons["一键登录已勾选课程"].tap()
        XCTAssertTrue(web.staticTexts["预览：二次确认"].waitForExistence(timeout: 10))
        XCTAssertTrue(web.staticTexts["统计学基础 · 火2"].exists)
        web.buttons["知道了"].tap()
        web.buttons.containing(NSPredicate(format: "label CONTAINS %@", "时间表")).firstMatch.tap()
        XCTAssertTrue(web.staticTexts["2026 · Q3"].waitForExistence(timeout: 10))
        web.buttons["上一周"].tap()
        XCTAssertTrue(web.staticTexts["2026 · Q2"].waitForExistence(timeout: 10))
        web.buttons["下一周"].tap()
        XCTAssertTrue(web.staticTexts["2026 · Q3"].waitForExistence(timeout: 10))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Preview calendar - fictional data"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
    func testBundledInterfaceAndNavigation() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launch()
        let web = app.webViews.firstMatch
        XCTAssertTrue(web.waitForExistence(timeout: 15))
        XCTAssertTrue(web.staticTexts["教务助手"].waitForExistence(timeout: 15))
        XCTAssertTrue(web.buttons["登录学校账户"].exists)
        web.buttons.containing(NSPredicate(format: "label CONTAINS %@", "时间表")).firstMatch.tap()
        XCTAssertTrue(web.staticTexts["日历"].waitForExistence(timeout: 10))
        web.buttons.containing(NSPredicate(format: "label CONTAINS %@", "设置")).firstMatch.tap()
        XCTAssertTrue(web.staticTexts["学校账户"].waitForExistence(timeout: 10))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
