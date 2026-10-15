package app.lotus.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
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

    suspend fun signUpWithEmail(email: String, password: String): Result<Pair<String, String?>> = request(
        path = "/auth/v1/signup",
        method = "POST",
        body = JSONObject().put("email", email.trim()).put("password", password)
    ) { json ->
        val access = json.optString("access_token")
        require(access.isNotBlank()) {
            "Supabase вернул пустую сессию. Возможно, требуется подтверждение email."
        }
        access to json.optString("refresh_token").takeIf { it.isNotBlank() }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Pair<String, String?>> = request(
        path = "/auth/v1/token?grant_type=password",
        method = "POST",
        body = JSONObject().put("email", email.trim()).put("password", password)
    ) { json ->
        val access = json.optString("access_token")
        require(access.isNotBlank()) { "Сервер не вернул токен авторизации" }
        access to json.optString("refresh_token").takeIf { it.isNotBlank() }
    }

    suspend fun sendEmailOtp(email: String): Result<Unit> = request(
        path = "/auth/v1/otp",
        method = "POST",
        body = JSONObject().put("email", email.trim()).put("create_user", true)
    ) { Unit }

    suspend fun verifyEmailOtp(email: String, code: String): Result<Pair<String, String?>> = request(
        path = "/auth/v1/verify",
        method = "POST",
        body = JSONObject().put("email", email.trim()).put("token", code.trim()).put("type", "email")
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

    suspend fun listChats(accessToken: String): Result<List<LotusChat>> = request(
        path = "/functions/v1/lotus-messaging",
        body = JSONObject().put("action", "list_chats"),
        bearer = accessToken
    ) { json ->
        val rows = json.optJSONArray("chats") ?: JSONArray()
        (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("id")
            if (id.isBlank()) return@mapNotNull null
            LotusChat(
                id = id,
                peerId = row.optString("peerId"),
                displayName = row.optString("displayName").ifBlank { "Lotus contact" },
                lastMessage = row.optString("lastMessage"),
                lastMessageAt = row.optString("lastMessageAt")
            )
        }
    }

    suspend fun startChat(accessToken: String, contactId: String): Result<LotusChat> = request(
        path = "/functions/v1/lotus-messaging",
        body = JSONObject().put("action", "start_chat").put("contactId", contactId),
        bearer = accessToken
    ) { json ->
        val row = json.optJSONObject("chat") ?: error("Сервер не вернул чат")
        LotusChat(
            id = row.optString("id").also { require(it.isNotBlank()) { "Сервер не вернул ID чата" } },
            peerId = row.optString("peerId"),
            displayName = row.optString("displayName").ifBlank { "Lotus contact" },
            lastMessage = row.optString("lastMessage"),
            lastMessageAt = row.optString("lastMessageAt")
        )
    }

    suspend fun listMessages(accessToken: String, conversationId: String): Result<List<LotusMessage>> = request(
        path = "/functions/v1/lotus-messaging",
        body = JSONObject().put("action", "list_messages").put("conversationId", conversationId),
        bearer = accessToken
    ) { json ->
        val rows = json.optJSONArray("messages") ?: JSONArray()
        (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("id")
            if (id.isBlank()) return@mapNotNull null
            LotusMessage(
                id = id,
                senderId = row.optString("sender_id"),
                body = row.optString("body"),
                createdAt = row.optString("created_at")
            )
        }
    }

    suspend fun sendMessage(accessToken: String, conversationId: String, text: String): Result<LotusMessage> = request(
        path = "/functions/v1/lotus-messaging",
        body = JSONObject()
            .put("action", "send_message")
            .put("conversationId", conversationId)
            .put("text", text),
        bearer = accessToken
    ) { json ->
        val row = json.optJSONObject("message") ?: error("Сервер не вернул сообщение")
        LotusMessage(
            id = row.optString("id").also { require(it.isNotBlank()) { "Сервер не вернул ID сообщения" } },
            senderId = row.optString("sender_id"),
            body = row.optString("body"),
            createdAt = row.optString("created_at")
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

data class ContactResult(val mutual: Boolean, val displayName: String?, val contactId: String? = null)

data class LotusChat(
    val id: String,
    val peerId: String,
    val displayName: String,
    val lastMessage: String,
    val lastMessageAt: String
)

data class LotusMessage(
    val id: String,
    val senderId: String,
    val body: String,
    val createdAt: String
)
