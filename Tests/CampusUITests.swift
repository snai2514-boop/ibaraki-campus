import XCTest

final class CampusUITests: XCTestCase {
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
