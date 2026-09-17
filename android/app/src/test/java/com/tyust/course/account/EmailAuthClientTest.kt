package com.tyust.course.account

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import okio.Buffer

class EmailAuthClientTest {
    @Test fun onlyExactUniversityEmailDomainsAreAccepted() {
        EmailAuthClient.validateEmail("student@vc.ibaraki.ac.jp")
        EmailAuthClient.validateEmail(" student@G.IBARAKI.AC.JP ")
        listOf("student@gmail.com", "student@ibaraki.ac.jp.evil.com", "student@evilvc.ibaraki.ac.jp",
            "student@sub.vc.ibaraki.ac.jp", "student@@vc.ibaraki.ac.jp", "student@ibaraki.ac.jp").forEach {
            assertThrows(IllegalArgumentException::class.java) { EmailAuthClient.validateEmail(it) }
        }
    }
    @Test fun externalEmailIsRejectedBeforeNetworkAndAfterLogin() {
        val api = auth()
        assertThrows(IllegalArgumentException::class.java) { api.signIn("student@gmail.com", "password") }
        assertThrows(IllegalArgumentException::class.java) { api.resend("student@gmail.com") }
        assertTrue(requests.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            auth("""{"access_token":"test-token","user":{"email":"student@gmail.com","email_confirmed_at":"2026-09-15"}}""")
                .signIn("test@vc.ibaraki.ac.jp", "password")
        }
    }
    private var requests = mutableListOf<Request>()
    private fun auth(body: String = "{}", status: Int = 200): EmailAuthClient {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("test")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return EmailAuthClient("https://example.supabase.co", "sb_publishable_test", client)
    }
    @Test fun unconfiguredOrInsecureEndpointsAreDisabled() {
        assertFalse(EmailAuthClient("", "").configured)
        assertFalse(EmailAuthClient("http://example.com", "sb_publishable_test").configured)
        assertFalse(EmailAuthClient("https://example.com", "sb_secret_not_for_app").configured)
        assertFalse(EmailAuthClient("https://example.com?redirect=x", "sb_publishable_test").configured)
        assertThrows(IllegalStateException::class.java) { EmailAuthClient("", "").signIn("test@vc.ibaraki.ac.jp", "password") }
    }
    @Test fun registrationSendsOwnPasswordToDedicatedAuthService() {
        auth().signUp(" test@vc.ibaraki.ac.jp ", "my-test-password")
        val request = requests.single()
        assertEquals("/auth/v1/signup", request.url.encodedPath)
        val buffer = Buffer(); request.body!!.writeTo(buffer)
        val payload = JSONObject(buffer.readUtf8())
        assertEquals("test@vc.ibaraki.ac.jp", payload.getString("email"))
        assertEquals("my-test-password", payload.getString("password"))
        assertEquals("sb_publishable_test", request.header("apikey"))
    }
    @Test fun invalidRegistrationDoesNotSendRequest() {
        val api = auth()
        assertThrows(IllegalArgumentException::class.java) { api.signUp("bad", "long-enough-password") }
        assertThrows(IllegalArgumentException::class.java) { api.signUp("test@vc.ibaraki.ac.jp", "short") }
        assertTrue(requests.isEmpty())
    }
    @Test fun loginRequiresConfirmedServerUserAndToken() {
        assertThrows(IllegalStateException::class.java) { auth("""{"user":{"email":"test@vc.ibaraki.ac.jp"}}""").signIn("test@vc.ibaraki.ac.jp", "secret") }
        val response = """{"access_token":"test-token","user":{"email":"test@vc.ibaraki.ac.jp","email_confirmed_at":"2026-09-15T00:00:00Z"}}"""
        assertEquals("test@vc.ibaraki.ac.jp", auth(response).signIn("test@vc.ibaraki.ac.jp", "secret"))
        assertEquals("grant_type=password", requests.last().url.query)
    }
    @Test fun verificationMustUseServerAndRejectsExpiredCode() {
        val api = auth("{}", 403)
        assertThrows(IllegalArgumentException::class.java) { api.verify("test@vc.ibaraki.ac.jp", "123") }
        assertTrue(requests.isEmpty())
        assertThrows(IllegalStateException::class.java) { api.verify("test@vc.ibaraki.ac.jp", "123456") }
        assertEquals("/auth/v1/verify", requests.single().url.encodedPath)
    }
    @Test fun signupRefusesAutoConfirmedConfiguration() {
        assertThrows(IllegalStateException::class.java) { auth("""{"access_token":"test"}""").signUp("test@vc.ibaraki.ac.jp", "long-enough-password") }
    }
    @Test fun rateLimitDoesNotReportSent() {
        val error = assertThrows(IllegalStateException::class.java) { auth("{}", 429).resend("test@vc.ibaraki.ac.jp") }
        assertTrue(error.message!!.contains("频繁"))
    }
}
