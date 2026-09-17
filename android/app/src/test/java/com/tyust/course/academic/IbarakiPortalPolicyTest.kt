package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class IbarakiPortalPolicyTest {
    @Test fun schoolAndObservedSsoHostsCanNavigate() {
        assertTrue(IbarakiPortalPolicy.allowsNavigation(IbarakiPortalPolicy.START_URL))
        assertTrue(IbarakiPortalPolicy.allowsNavigation("https://login.microsoftonline.com/tenant/oauth2/authorize"))
        assertTrue(IbarakiPortalPolicy.allowsNavigation("https://sidp.ibaraki.ac.jp/idp/"))
    }
    @Test fun readingIsLimitedToSchoolPortal() {
        assertTrue(IbarakiPortalPolicy.allowsReading("https://csweb.ibaraki.ac.jp/campusweb/page"))
        assertFalse(IbarakiPortalPolicy.allowsReading("https://login.microsoftonline.com/"))
        assertFalse(IbarakiPortalPolicy.allowsReading("https://sidp.ibaraki.ac.jp/"))
        assertFalse(IbarakiPortalPolicy.allowsReading("https://csweb.ibaraki.ac.jp/"))
    }
    @Test fun rejectUnsafeAndLookalikeNavigation() {
        listOf("http://csweb.ibaraki.ac.jp/campusweb/", "https://csweb.ibaraki.ac.jp.evil.com/",
            "https://evil.com@csweb.ibaraki.ac.jp/", "javascript:alert(1)", "file:///tmp/file",
            "https://csweb.ibaraki.ac.jp:444/campusweb/").forEach { assertFalse(IbarakiPortalPolicy.allowsNavigation(it)) }
    }
}
