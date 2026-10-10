package app.lotus.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.86f) }
    val alpha = remember { Animatable(0f) }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(650))
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(1050)
        alpha.animateTo(0f, animationSpec = tween(350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❀",
                        fontSize = 52.sp,
                        color = colors.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = "Lotus",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp
                ),
                color = colors.onBackground
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "ТИШЕ. БЛИЖЕ. ЛИЧНОЕ.",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(34.dp))
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 3.dp)
                    .background(colors.secondary, CircleShape)
            )
        }
    }
}
