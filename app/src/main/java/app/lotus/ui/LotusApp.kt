package app.lotus.ui

import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.lotus.data.LotusBackend
import app.lotus.ui.screens.ChatsScreen
import app.lotus.ui.screens.EmailAuthScreen
import app.lotus.ui.screens.SplashScreen

@Composable
fun LotusApp() {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var accessToken by remember { mutableStateOf(LotusBackend.accessToken(context)) }

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
                    ChatsScreen(accessToken = accessToken.orEmpty())
                }
            }
        }
    }
}
