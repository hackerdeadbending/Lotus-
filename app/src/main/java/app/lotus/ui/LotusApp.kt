package app.lotus.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.lotus.data.LotusBackend
import app.lotus.data.LotusCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import app.lotus.ui.screens.ChatsScreen
import app.lotus.ui.screens.EmailAuthScreen
import app.lotus.ui.screens.SplashScreen

@Composable
fun LotusApp() {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var accessToken by remember { mutableStateOf(LotusBackend.accessToken(context)) }
    var encryptionReady by remember { mutableStateOf(false) }
    var encryptionError by remember { mutableStateOf<String?>(null) }
    var encryptionRetry by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val refresh = LotusBackend.refreshToken(context)
        if (!refresh.isNullOrBlank()) {
            LotusBackend.refreshSession(refresh).onSuccess { session ->
                LotusBackend.saveSession(context, session.first, session.second ?: refresh)
                accessToken = session.first
            }
        }
    }

    LaunchedEffect(accessToken) {
        while (!accessToken.isNullOrBlank()) {
            delay(45 * 60 * 1000L)
            val refresh = LotusBackend.refreshToken(context) ?: continue
            LotusBackend.refreshSession(refresh).onSuccess { session ->
                LotusBackend.saveSession(context, session.first, session.second ?: refresh)
                accessToken = session.first
            }
        }
    }

    LaunchedEffect(accessToken, encryptionRetry) {
        val token = accessToken
        if (token.isNullOrBlank()) {
            encryptionReady = false
            encryptionError = null
        } else {
            encryptionReady = false
            encryptionError = null
            val userId = LotusBackend.userIdFromAccessToken(token)
            if (userId.isNullOrBlank()) {
                encryptionError = "Не удалось определить аккаунт для ключа шифрования."
                return@LaunchedEffect
            }
            val keyResult = runCatching {
                withContext(Dispatchers.IO) { LotusCrypto.getOrCreatePublicKey(context, userId) }
            }
            if (keyResult.isSuccess) {
                val publicKey = keyResult.getOrThrow()
                val publishResult = LotusBackend.publishEncryptionPublicKey(token, publicKey)
                publishResult.fold(
                    onSuccess = { encryptionReady = true },
                    onFailure = { failure ->
                        encryptionError = when {
                            failure.message.orEmpty().contains("encryption_public_key_invalid", true) ->
                                "Не удалось опубликовать ключ шифрования. Обновите приложение."
                            else -> "Не удалось подключить защищённый обмен ключами. Проверьте интернет и повторите."
                        }
                    }
                )
            } else {
                encryptionError = keyResult.exceptionOrNull()?.message
                    ?: "Не удалось подготовить локальный ключ шифрования."
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    if (targetState) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (fadeIn() + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialOffsetY = { it / 8 }
                        )) togetherWith (fadeOut() + slideOutVertically(
                            targetOffsetY = { -it / 12 }
                        ))
                    }
                },
                label = "main_transition"
            ) { isSplash ->
                if (isSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (accessToken.isNullOrBlank()) {
                    EmailAuthScreen { access, refresh ->
                        LotusBackend.saveSession(context, access, refresh)
                        accessToken = access
                    }
                } else {
                    ChatsScreen(
                        accessToken = accessToken.orEmpty(),
                        encryptionReady = encryptionReady,
                        encryptionError = encryptionError,
                        onRetryEncryption = { encryptionRetry += 1 },
                        onLogout = {
                            LotusBackend.clearSession(context)
                            accessToken = null
                        }
                    )
                }
            }
        }
    }
}
