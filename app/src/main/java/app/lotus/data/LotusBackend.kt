package app.lotus.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LotusBackend {
    const val BASE_URL = "https://roehppogklmxztpcsfft.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_uLnqdXmGDVE2Xixpvo6yew_08qFjvM7"
    private const val PREFS = "lotus_auth"
    private const val ACCESS = "access_token"
    private const val REFRESH = "refresh_token"

    fun accessToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACCESS, null)

    fun saveSession(context: Context, accessToken: String, refreshToken: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS, accessToken)
            .putString(REFRESH, refreshToken)
            .apply()
    }

    suspend fun sendOtp(phone: String): Result<Unit> = request(
        path = "/auth/v1/otp",
        method = "POST",
        body = JSONObject().put("phone", phone).put("create_user", true)
    ) { Unit }

    suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, String?>> = request(
        path = "/auth/v1/verify",
        method = "POST",
        body = JSONObject().put("phone", phone).put("token", code).put("type", "sms")
    ) { json ->
        val access = json.optString("access_token")
        require(access.isNotBlank()) { "Сервер не вернул токен авторизации" }
        access to json.optString("refresh_token").takeIf { it.isNotBlank() }
    }

    suspend fun lookupContact(accessToken: String, phone: String): Result<ContactResult> = request(
        path = "/functions/v1/lookup-contact",
        method = "POST",
        body = JSONObject().put("phone", phone),
        bearer = accessToken
    ) { json ->
        ContactResult(
            mutual = json.optBoolean("mutual", false),
            displayName = json.optJSONObject("contact")?.optString("displayName")
        )
    }

    private suspend fun <T> request(
        path: String,
        method: String = "POST",
        body: JSONObject,
        bearer: String? = null,
        parse: (JSONObject) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("apikey", PUBLISHABLE_KEY)
                setRequestProperty("Content-Type", "application/json")
                if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
                if (code !in 200..299) {
                    val reason = json.optString("msg").ifBlank {
                        json.optString("message").ifBlank {
                            json.optString("error_description").ifBlank { json.optString("error").ifBlank { "HTTP $code" } }
                        }
                    }
                    throw IllegalStateException(reason)
                }
                parse(json)
            } finally {
                connection.disconnect()
            }
        }
    }
}

data class ContactResult(val mutual: Boolean, val displayName: String?)
