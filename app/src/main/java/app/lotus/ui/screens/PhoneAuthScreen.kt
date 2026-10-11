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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lotus.data.LotusBackend
import kotlinx.coroutines.launch

@Composable
fun PhoneAuthScreen(onAuthenticated: (String, String?) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
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
                if (codeSent) "Введите код из SMS" else "Вход по номеру телефона",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                if (codeSent) "Мы отправили код, если SMS-провайдер настроен для Lotus." else "Ваш номер нужен для подтверждения аккаунта. Он не станет публичным именем профиля.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!codeSent) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' || ch == '(' || ch == ')' }; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Номер телефона") },
                    placeholder = { Text("+39 333 123 4567") },
                    singleLine = true,
                    shape = RoundedCornerShape(17.dp)
                )
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(8); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Код из SMS") },
                    singleLine = true,
                    shape = RoundedCornerShape(17.dp)
                )
                TextButton(onClick = { codeSent = false; code = ""; error = null }, enabled = !loading) {
                    Text("Изменить номер")
                }
            }
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        if (!codeSent) {
                            LotusBackend.sendOtp(phone.trim()).onSuccess {
                                codeSent = true
                            }.onFailure {
                                error = friendlyError(it.message)
                            }
                        } else {
                            LotusBackend.verifyOtp(phone.trim(), code.trim()).onSuccess { session ->
                                onAuthenticated(session.first, session.second)
                            }.onFailure {
                                error = friendlyError(it.message)
                            }
                        }
                        loading = false
                    }
                },
                enabled = !loading && if (codeSent) code.length >= 4 else phone.trim().length >= 8,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (codeSent) "Подтвердить номер" else "Отправить SMS")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Сервер Lotus подключён. Для отправки SMS необходимо включить и настроить провайдера SMS в Supabase Auth.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun friendlyError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("phone provider", true) || text.contains("sms", true) && text.contains("not", true) ->
            "SMS-провайдер ещё не настроен в Supabase. Нужно включить его в настройках Auth."
        text.contains("invalid", true) || text.contains("phone", true) ->
            "Проверьте номер в международном формате, например +393331234567."
        text.isBlank() -> "Не удалось связаться с сервером. Попробуйте ещё раз."
        else -> text.take(220)
    }
}
