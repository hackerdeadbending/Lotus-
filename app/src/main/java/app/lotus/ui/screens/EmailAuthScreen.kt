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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lotus.data.LotusBackend
import kotlinx.coroutines.launch

@Composable
fun EmailAuthScreen(onAuthenticated: (String, String?) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val validEmail = email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
    val validPassword = password.length >= 6

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
                    Icon(
                        if (createAccount) Icons.Rounded.MailOutline else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer
                    )
                }
            }
            Text(
                "Lotus",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground
            )
            Text(
                if (createAccount) "Создайте свой Lotus" else "С возвращением",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                if (createAccount) "Зарегистрируйтесь по электронной почте, чтобы начать общаться."
                else "Войдите в свой аккаунт Lotus с помощью электронной почты.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(254); error = null; info = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Электронная почта") },
                placeholder = { Text("you@example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(17.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(128); error = null; info = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                placeholder = { Text("Минимум 6 символов") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(17.dp)
            )

            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            if (info != null) {
                Text(info!!, color = colors.primary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    loading = true
                    error = null
                    info = null
                    scope.launch {
                        val result = if (createAccount) {
                            LotusBackend.signUpWithEmail(email.trim(), password)
                        } else {
                            LotusBackend.signInWithEmail(email.trim(), password)
                        }
                        result.onSuccess { session ->
                            onAuthenticated(session.first, session.second)
                        }.onFailure { failure ->
                            val message = failure.message.orEmpty()
                            error = when {
                                message.contains("already registered", true) ||
                                    message.contains("already exists", true) ->
                                    "Этот email уже зарегистрирован. Переключитесь на вход."
                                message.contains("invalid login credentials", true) ||
                                    message.contains("invalid email or password", true) ->
                                    "Неверная почта или пароль."
                                message.contains("email not confirmed", true) ||
                                    message.contains("confirm your email", true) ->
                                    "Подтвердите email по письму из почты, затем войдите снова."
                                message.contains("password", true) && message.contains("least", true) ->
                                    "Пароль слишком короткий. Используйте не менее 6 символов."
                                message.contains("empty session", true) ||
                                    message.contains("требуется подтверждение email", true) ->
                                    "Проверьте почту и подтвердите регистрацию. Если письмо не пришло, проверьте папку «Спам»."
                                message.isBlank() ->
                                    "Не удалось связаться с сервером. Проверьте интернет и попробуйте ещё раз."
                                else -> message.take(220)
                            }
                        }
                        loading = false
                    }
                },
                enabled = !loading && validEmail && validPassword,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (createAccount) "Создать аккаунт" else "Войти")
                }
            }

            if (!createAccount) {
                TextButton(
                    onClick = {
                        loading = true
                        error = null
                        info = null
                        scope.launch {
                            LotusBackend.sendPasswordRecovery(email.trim())
                                .onSuccess {
                                    info = "Если аккаунт с такой почтой существует, письмо для восстановления скоро придёт. Проверьте также папку «Спам»."
                                }
                                .onFailure { failure ->
                                    error = when {
                                        failure.message.orEmpty().contains("rate", true) ->
                                            "Слишком много запросов. Подождите немного и попробуйте снова."
                                        else ->
                                            "Не удалось отправить письмо. Проверьте адрес и интернет-соединение."
                                    }
                                }
                            loading = false
                        }
                    },
                    enabled = !loading && validEmail
                ) {
                    Text("Забыли пароль?")
                }
            }

            TextButton(
                onClick = {
                    createAccount = !createAccount
                    error = null
                    info = null
                },
                enabled = !loading
            ) {
                Text(if (createAccount) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Почта используется для входа. Ваш адрес не отображается другим пользователям как публичный идентификатор.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
