package app.lotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import app.lotus.data.LotusBackend
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ContactDiscoveryDialog(accessToken: String, onDismiss: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            val number = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (!number.isNullOrBlank()) {
                phone = number
                statusMessage = null
            }
        }
    }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Добавить человека", fontWeight = FontWeight.SemiBold)
                Text(
                    "Найдите знакомого безопасным способом",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value ->
                        phone = value.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
                        showStatus = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Номер телефона") },
                    placeholder = { Text("+39 000 000 0000") },
                    singleLine = true,
                    shape = RoundedCornerShape(17.dp)
                )

                OutlinedButton(
                    onClick = {
                        contactPicker.launch(
                            Intent(
                                Intent.ACTION_PICK,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Rounded.Contacts, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Выбрать из контактов")
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.surface.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = colors.primary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Взаимные контакты", fontWeight = FontWeight.SemiBold, color = colors.onPrimaryContainer)
                            Text(
                                "Профиль появится, только если вы сохранили номера друг друга. Номер не будет публичным именем профиля.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DiscoveryOption(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) },
                        title = "QR-код"
                    )
                    DiscoveryOption(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        title = "Приглашение"
                    )
                }

                if (statusMessage != null) {
                    Text(
                        statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusMessage!!.startsWith("Ошибка")) colors.error else colors.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    checking = true
                    statusMessage = null
                    scope.launch {
                        LotusBackend.lookupContact(accessToken, phone.trim())
                            .onSuccess { result ->
                                statusMessage = if (result.mutual) "Взаимный контакт найден. Подключение чатов — следующий этап."
                                else "Запрос обработан. Контакт появится, когда другой пользователь Lotus тоже добавит ваш номер."
                            }
                            .onFailure { failure ->
                                statusMessage = "Ошибка: " + (failure.message ?: "не удалось проверить контакт")
                            }
                        checking = false
                    }
                },
                enabled = phone.trim().length >= 5 && !checking,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(if (checking) "Проверяем…" else "Проверить контакт")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun DiscoveryOption(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = colors.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides colors.secondary
                ) { icon() }
            }
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text("Скоро", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}
