package app.lotus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lotus.data.LotusBackend
import kotlinx.coroutines.launch

@Composable
fun EmailAuthScreen(onAuthenticated: (String, String?) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creatingAccount by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.primaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(Modifier.padding(22.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.onPrimaryContainer)
                }
            }
            Text(
                "Lotus",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground
            )
            Text(
                if (creatingAccount) "Создать тестовый аккаунт" else "Войти в Lotus",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Пока вход по телефону не настроен, используйте email и пароль для тестирования приложения.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trimStart(); error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль (минимум 6 символов)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(17.dp)
            )
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val result = if (creatingAccount) {
                            LotusBackend.signUpWithEmail(email.trim(), password)
                        } else {
                            LotusBackend.signInWithEmail(email.trim(), password)
                        }
                        result.onSuccess { session ->
                            onAuthenticated(session.first, session.second)
                        }.onFailure { failure ->
                            error = friendlyEmailError(failure.message)
                        }
                        loading = false
                    }
                },
                enabled = !loading && email.contains("@") && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (creatingAccount) "Создать аккаунт" else "Войти")
                }
            }
            androidx.compose.material3.TextButton(
                onClick = { creatingAccount = !creatingAccount; error = null },
                enabled = !loading
            ) {
                Text(if (creatingAccount) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Это временный способ входа для разработки. Не используйте здесь пароль от других сервисов.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun friendlyEmailError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("confirm", true) || text.contains("email", true) && text.contains("not", true) ->
            "Проверьте настройки Supabase Auth: для теста без писем отключите Confirm email. Если аккаунт уже создан, переключитесь на «Войти»."
        text.contains("invalid", true) && text.contains("credentials", true) ->
            "Неверный email или пароль. Проверьте данные или создайте аккаунт."
        text.contains("already", true) && text.contains("registered", true) ->
            "Этот email уже зарегистрирован. Нажмите «Уже есть аккаунт? Войти»."
        text.isBlank() -> "Не удалось связаться с сервером. Проверьте интернет и попробуйте ещё раз."
        else -> text.take(220)
    }
}
