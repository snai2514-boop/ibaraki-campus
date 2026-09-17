package com.tyust.course.academic

import java.net.URI

object IbarakiPortalPolicy {
    const val START_URL = "https://csweb.ibaraki.ac.jp/campusweb/"
    private val loginHosts = setOf("csweb.ibaraki.ac.jp", "sidp.ibaraki.ac.jp", "login.microsoftonline.com")
    fun allowsNavigation(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.userInfo == null && uri.port in setOf(-1, 443) &&
            uri.host?.lowercase() in loginHosts
    }.getOrDefault(false)
    fun allowsReading(url: String): Boolean = allowsNavigation(url) && runCatching {
        val uri = URI(url)
        uri.host.equals("csweb.ibaraki.ac.jp", true) && uri.path.startsWith("/campusweb/")
    }.getOrDefault(false)
}
