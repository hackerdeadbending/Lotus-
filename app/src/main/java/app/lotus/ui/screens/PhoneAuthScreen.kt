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
import androidx.compose.material.icons.rounded.PhoneIphone
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
    val normalizedPhone = phone.filter { it.isDigit() || it == '+' }
    val phoneDigits = phone.count { it.isDigit() }
    val validPhone = normalizedPhone.startsWith("+") && phoneDigits in 8..15

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
                        if (codeSent) Icons.Rounded.Lock else Icons.Rounded.PhoneIphone,
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
                if (codeSent) "Подтвердите номер" else "Вход по номеру телефона",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                if (codeSent) "Введите код из SMS, отправленного на $normalizedPhone."
                else "Номер нужен для входа и приватного поиска взаимных контактов. Он не будет отображаться публично.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!codeSent) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value ->
                        phone = value.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }.take(22)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Номер телефона") },
                    placeholder = { Text("+393331234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(17.dp)
                )
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value.filter(Char::isDigit).take(8); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("SMS-код") },
                    placeholder = { Text("123456") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(17.dp)
                )
            }

            if (error != null) {
                Text(
                    error!!,
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        if (!codeSent) {
                            LotusBackend.sendOtp(normalizedPhone).onSuccess {
                                codeSent = true
                            }.onFailure { failure ->
                                error = friendlyPhoneError(failure.message)
                            }
                        } else {
                            LotusBackend.verifyOtp(normalizedPhone, code.trim()).onSuccess { session ->
                                onAuthenticated(session.first, session.second)
                            }.onFailure { failure ->
                                error = friendlyPhoneError(failure.message)
                            }
                        }
                        loading = false
                    }
                },
                enabled = !loading && if (codeSent) code.length in 4..8 else validPhone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (codeSent) "Подтвердить и войти" else "Получить SMS-код")
                }
            }

            if (codeSent) {
                TextButton(
                    onClick = { codeSent = false; code = ""; error = null },
                    enabled = !loading
                ) {
                    Text("Изменить номер")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "SMS-авторизация требует включённого Phone provider и настроенного SMS-провайдера в Supabase.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun friendlyPhoneError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("phone provider", true) || text.contains("sms provider", true) ||
            text.contains("messagebird", true) || text.contains("not configured", true) ->
            "SMS-авторизация ещё не настроена в Supabase. Включите Phone provider и настройте SMS-провайдера."
        text.contains("invalid", true) && (text.contains("phone", true) || text.contains("number", true)) ->
            "Проверьте номер в международном формате, например +393331234567."
        text.contains("token", true) || text.contains("otp", true) || text.contains("code", true) ->
            "Код неверный или истёк. Проверьте SMS и запросите новый код."
        text.isBlank() -> "Не удалось связаться с сервером. Проверьте интернет и попробуйте ещё раз."
        else -> text.take(220)
    }
}
