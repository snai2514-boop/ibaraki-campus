package com.tyust.course.account

import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Dedicated app identity. Never calls a school login endpoint or stores passwords. */
class EmailAuthClient internal constructor(private val baseUrl: String, private val publicKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(25, TimeUnit.SECONDS).build()) {
    val configured: Boolean get() = baseUrl.toHttpUrlOrNull()?.let {
        it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.query == null &&
            it.fragment == null && it.encodedPath == "/" && publicKey.startsWith("sb_publishable_")
    } == true

    fun signUp(email: String, password: String) {
        validateEmail(email)
        require(password.length in 12..128) { "密码请使用 12–128 个字符" }
        val result = post("signup", JSONObject().put("email", email.trim()).put("password", password))
        check(result.optString("access_token").isBlank()) { "账户服务未开启邮箱验证，请联系维护者" }
    }

    fun signIn(email: String, password: String): String {
        validateEmail(email)
        require(password.isNotEmpty()) { "请输入密码" }
        return verifiedEmail(post("token?grant_type=password", JSONObject().put("email", email.trim()).put("password", password)))
    }

    fun resend(email: String) {
        validateEmail(email)
        post("resend", JSONObject().put("email", email.trim()).put("type", "signup"))
    }

    fun verify(email: String, code: String): String {
        validateEmail(email)
        require(code.matches(Regex("[0-9]{6}"))) { "请输入邮件中的 6 位验证码" }
        return verifiedEmail(post("verify", JSONObject().put("email", email.trim()).put("token", code).put("type", "signup")))
    }

    private fun verifiedEmail(response: JSONObject): String {
        val user = response.optJSONObject("user")
        check(response.optString("access_token").isNotBlank() && user != null &&
            !user.isNull("email_confirmed_at") && user.optString("email_confirmed_at").isNotBlank() &&
            user.optString("email").isNotBlank()) { "服务未返回已验证的邮箱账户" }
        // No cloud data APIs yet: do not persist or log unused bearer/refresh tokens.
        return user!!.getString("email").also(::validateEmail)
    }

    private fun post(path: String, payload: JSONObject): JSONObject {
        check(configured) { "邮箱账户服务尚未开通，请先使用本机功能" }
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/auth/v1/" + path)
            .header("apikey", publicKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException(when (response.code) {
                    429 -> "操作过于频繁，请稍后重试"
                    400, 401, 403, 422 -> "验证未通过，请检查邮箱、密码或验证码；验证码可能已过期"
                    else -> "账户服务暂时不可用，请稍后重试"
                })
                return JSONObject(response.body?.string().orEmpty().ifBlank { "{}" })
            }
        } catch (e: IOException) { throw IllegalStateException("无法连接账户服务，请检查网络后重试") }
    }

    companion object {
        fun validateEmail(email: String) {
            require(email.trim().length <= 254 && email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "请输入有效邮箱地址" }
            val domain = email.trim().substringAfterLast('@').lowercase(java.util.Locale.ROOT)
            require(domain in setOf("vc.ibaraki.ac.jp", "g.ibaraki.ac.jp")) {
                "仅支持茨城大学邮箱：@vc.ibaraki.ac.jp 或 @g.ibaraki.ac.jp"
            }
        }
    }
}
