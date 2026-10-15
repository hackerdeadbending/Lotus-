package app.lotus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
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
import android.graphics.Bitmap
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import app.lotus.data.LotusBackend
import app.lotus.data.LotusChat
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ContactDiscoveryDialog(accessToken: String, onDismiss: () -> Unit, onChatStarted: (LotusChat) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var personalInviteCode by remember { mutableStateOf<String?>(null) }
    var creatingInvite by remember { mutableStateOf(false) }
    var acceptingInvite by remember { mutableStateOf(false) }
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

    fun createPersonalInvite(share: Boolean) {
        if (creatingInvite) return
        creatingInvite = true
        statusMessage = null
        scope.launch {
            LotusBackend.createInvite(accessToken).onSuccess { code ->
                personalInviteCode = code
                if (share) {
                    val inviteText = "Присоединяйся ко мне в Lotus! Код приглашения: $code. Он действует 7 дней. Установи приложение: https://github.com/hackerdeadbending/Lotus-/releases, затем введи код в разделе «Добавить человека»."
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Приглашение в Lotus")
                        putExtra(Intent.EXTRA_TEXT, inviteText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Пригласить в Lotus"))
                    statusMessage = "Код создан. Он действует 7 дней и может быть использован один раз."
                } else {
                    showQr = true
                }
            }.onFailure { failure ->
                statusMessage = "Ошибка: " + (failure.message ?: "не удалось создать приглашение")
            }
            creatingInvite = false
        }
    }

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { value ->
                        phone = value.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
                        statusMessage = null
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase().filter { ch -> ch in '0'..'9' || ch in 'A'..'F' }.take(24) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Код приглашения") },
                        placeholder = { Text("24 символа") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    Button(
                        onClick = {
                            acceptingInvite = true
                            statusMessage = null
                            scope.launch {
                                LotusBackend.acceptInvite(accessToken, inviteCode).onSuccess { chat ->
                                    onChatStarted(chat)
                                    onDismiss()
                                }.onFailure { failure ->
                                    statusMessage = when {
                                        failure.message?.contains("invite_invalid_or_expired", true) == true ->
                                            "Код недействителен, уже использован или истёк."
                                        failure.message?.contains("invite_cannot_accept_own", true) == true ->
                                            "Нельзя принять собственное приглашение. Отправьте код другому человеку."
                                        else -> "Ошибка: " + (failure.message ?: "не удалось принять приглашение")
                                    }
                                }
                                acceptingInvite = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inviteCode.length == 24 && !acceptingInvite,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (acceptingInvite) "Принимаем…" else "Принять приглашение")
                    }
                    OutlinedButton(
                        onClick = { createPersonalInvite(share = true) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !creatingInvite,
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (creatingInvite) "Создаём код…" else "Создать личное приглашение")
                    }
                    personalInviteCode?.let { code ->
                        Surface(
                            color = colors.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Ваш код приглашения", fontWeight = FontWeight.SemiBold)
                                Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Одноразовый · действует 7 дней", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                TextButton(onClick = { createPersonalInvite(share = true) }, enabled = !creatingInvite) {
                                    Text("Создать новый код и поделиться")
                                }
                            }
                        }
                    }
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
                        title = "QR-код",
                        subtitle = "QR-код приглашения",
                        onClick = {
                            if (personalInviteCode != null) showQr = true
                            else createPersonalInvite(share = false)
                        }
                    )
                    DiscoveryOption(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        title = "Пригласить",
                        subtitle = "Поделиться ссылкой",
                        onClick = { createPersonalInvite(share = true) }
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
                        val lookup = LotusBackend.lookupContact(accessToken, phone.trim())
                        lookup.onSuccess { result ->
                            if (result.mutual && !result.contactId.isNullOrBlank()) {
                                LotusBackend.startChat(accessToken, result.contactId).onSuccess { chat ->
                                    onChatStarted(chat)
                                    onDismiss()
                                }.onFailure { failure ->
                                    statusMessage = "Не удалось открыть чат: " + (failure.message ?: "ошибка сервера")
                                }
                            } else {
                                statusMessage = "Запрос обработан. Контакт появится, когда другой пользователь Lotus тоже добавит ваш номер."
                            }
                        }.onFailure { failure ->
                            statusMessage = when (failure.message) {
                                "verified_phone_required" ->
                                    "Для поиска контактов нужно отдельно подтвердить номер телефона. Вход по email при этом остаётся доступен."
                                "phone_must_be_e164" ->
                                    "Введите номер в международном формате, например +39 123 456 7890."
                                "rate_limited" ->
                                    "Слишком много попыток. Подождите минуту и попробуйте снова."
                                else ->
                                    "Ошибка: " + (failure.message ?: "не удалось проверить контакт")
                            }
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

    if (showQr) {
        val inviteUrl = "https://github.com/hackerdeadbending/Lotus-/releases"
        val qrPayload = personalInviteCode?.let { "LOTUS-INVITE:$it" } ?: inviteUrl
        val qrBitmap = remember(qrPayload) {
            runCatching {
                val matrix = QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 640, 640)
                Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).also { bitmap ->
                    for (x in 0 until 640) {
                        for (y in 0 until 640) {
                            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        }
                    }
                }
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("Приглашение в Lotus") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR-код приглашения в Lotus",
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    } else {
                        Text("Не удалось создать QR-код. Используйте кнопку «Пригласить».")
                    }
                    Text(
                        if (personalInviteCode != null) "Это личный одноразовый код. Получатель должен установить Lotus и ввести код в поле «Код приглашения»." else "Отсканируйте код, чтобы открыть страницу загрузки Lotus.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            personalInviteCode?.let { code -> "Код приглашения Lotus: $code. Введите его в разделе «Добавить человека». Скачать приложение: $inviteUrl" } ?: inviteUrl
                        )
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Поделиться Lotus"))
                }) { Text("Поделиться приглашением") }
            },
            dismissButton = { TextButton(onClick = { showQr = false }) { Text("Готово") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun DiscoveryOption(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
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
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
