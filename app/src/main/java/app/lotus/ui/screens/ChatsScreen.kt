package app.lotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lotus.data.LotusBackend
import app.lotus.data.LotusCrypto
import app.lotus.data.LotusChat
import app.lotus.data.LotusMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private data class ChatPreview(
    val id: String,
    val peerId: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val subtitle: String = "Lotus",
    val peerPublicKey: String? = null
)

private data class LocalMessage(
    val id: String,
    val text: String,
    val mine: Boolean,
    val time: String
)

@Composable
fun ChatsScreen(
    accessToken: String,
    encryptionReady: Boolean,
    encryptionError: String?,
    onRetryEncryption: () -> Unit,
    onLogout: () -> Unit
) {
    var chats by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }
    var selectedChat by remember { mutableStateOf<ChatPreview?>(null) }
    var messages by remember { mutableStateOf<List<LocalMessage>>(emptyList()) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewChat by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var profileNameSaving by remember { mutableStateOf(false) }
    var profileStatus by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var loadingChats by remember { mutableStateOf(true) }
    var loadingMessages by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var backendError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    fun toPreview(chat: LotusChat): ChatPreview {
        val lastMessage = chat.lastMessage.ifBlank { "Начните общение" }
        val readableLastMessage = if (lastMessage.startsWith("LOTUS1:")) {
            runCatching { LotusCrypto.decrypt(context, chat.id, chat.peerPublicKey, lastMessage) }
                .getOrElse { "Зашифрованное сообщение" }
        } else lastMessage
        return ChatPreview(
            id = chat.id,
            peerId = chat.peerId,
            name = chat.displayName,
            lastMessage = readableLastMessage,
            time = displayTime(chat.lastMessageAt),
            subtitle = "Личный чат",
            peerPublicKey = chat.peerPublicKey
        )
    }

    LaunchedEffect(accessToken) {
        while (true) {
            LotusBackend.listChats(accessToken)
                .onSuccess { result ->
                    chats = result.map(::toPreview)
                    backendError = null
                    loadingChats = false
                }
                .onFailure { failure ->
                    backendError = friendlyMessagingError(failure.message)
                    loadingChats = false
                }
            delay(5000)
        }
    }

    LaunchedEffect(accessToken, selectedChat?.id) {
        val conversationId = selectedChat?.id ?: return@LaunchedEffect
        loadingMessages = true
        while (true) {
            LotusBackend.listMessages(accessToken, conversationId)
                .onSuccess { result ->
                    messages = result.map { message ->
                        val readableBody = runCatching {
                            LotusCrypto.decrypt(context, conversationId, selectedChat?.peerPublicKey, message.body)
                        }.getOrElse { "Не удалось расшифровать сообщение" }
                        LocalMessage(message.id, readableBody, message.mine, displayTime(message.createdAt))
                    }
                    backendError = null
                    loadingMessages = false
                }
                .onFailure { failure ->
                    backendError = friendlyMessagingError(failure.message)
                    loadingMessages = false
                }
            delay(3000)
        }
    }

    val activeChat = selectedChat
    if (activeChat != null) {
        ConversationScreen(
            chat = activeChat,
            messages = messages,
            draft = draft,
            sending = sending,
            loading = loadingMessages,
            encryptionReady = encryptionReady,
            encryptionError = encryptionError,
            onRetryEncryption = onRetryEncryption,
            onDraftChange = { draft = it },
            onBack = {
                selectedChat = null
                draft = ""
                messages = emptyList()
            },
            onSend = {
                val text = draft.trim()
                if (text.isNotEmpty() && !sending) {
                    sending = true
                    scope.launch {
                        val encryptedResult = runCatching {
                            withContext(Dispatchers.IO) {
                                LotusCrypto.encrypt(context, activeChat.id, activeChat.peerPublicKey, text)
                            }
                        }
                        if (encryptedResult.isFailure) {
                            backendError = encryptedResult.exceptionOrNull()?.message
                                ?: "Не удалось зашифровать сообщение."
                            sending = false
                            return@launch
                        }
                        LotusBackend.sendMessage(accessToken, activeChat.id, encryptedResult.getOrThrow())
                            .onSuccess { sent ->
                                messages = messages + LocalMessage(
                                    sent.id, text, true, displayTime(sent.createdAt)
                                )
                                chats = chats.map { chat ->
                                    if (chat.id == activeChat.id) chat.copy(
                                        lastMessage = text,
                                        time = displayTime(sent.createdAt)
                                    ) else chat
                                }
                                draft = ""
                                backendError = null
                            }
                            .onFailure { failure ->
                                backendError = friendlyMessagingError(failure.message)
                            }
                        sending = false
                    }
                }
            }
        )
        if (backendError != null) {
            Surface(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = colors.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    backendError!!,
                    modifier = Modifier.padding(10.dp),
                    color = colors.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "Новый чат") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✿", color = colors.onPrimaryContainer, fontSize = 25.sp)
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                        Text(
                            "Lotus",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.8).sp
                            ),
                            color = colors.onBackground
                        )
                    }
                    Text(
                        "ВАШЕ ЛИЧНОЕ ПРОСТРАНСТВО",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) searchQuery = "" }) {
                    Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Поиск", tint = colors.onSurfaceVariant)
                }
                IconButton(onClick = {
                    showSettings = true
                    profileStatus = null
                    scope.launch {
                        LotusBackend.getDisplayName(accessToken).onSuccess { profileName = it }
                            .onFailure { profileStatus = "Не удалось загрузить профиль." }
                    }
                }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Настройки", tint = colors.onSurfaceVariant)
                }
            }

            if (searchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Имя или сообщение") }
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.secondary)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ваше пространство", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Text("Спокойное общение без лишнего", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Сообщения", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.onBackground, modifier = Modifier.weight(1f))
                Text("${chats.size} чатов", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }

            val visibleChats = chats.filter { it.name.contains(searchQuery, true) || it.lastMessage.contains(searchQuery, true) }
            when {
                loadingChats && visibleChats.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                visibleChats.isEmpty() -> Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(70.dp).clip(CircleShape).background(colors.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(30.dp))
                        }
                        Text(if (searchQuery.isBlank()) "Здесь пока тихо" else "Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                        Text(
                            if (backendError != null) backendError!! else if (searchQuery.isBlank()) "Добавьте человека, чтобы начать личный чат." else "Попробуйте другое имя или сообщение.",
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (backendError == null && searchQuery.isBlank()) {
                            Button(onClick = { showNewChat = true }, shape = RoundedCornerShape(16.dp)) { Text("Добавить человека") }
                        }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(visibleChats, key = { it.id }) { chat ->
                        ChatListItem(chat) {
                            selectedChat = chat
                            messages = emptyList()
                            draft = ""
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Настройки") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Профиль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it.take(40); profileStatus = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ваше имя") },
                        placeholder = { Text("Как вас будут видеть") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = {
                            profileNameSaving = true
                            profileStatus = null
                            scope.launch {
                                LotusBackend.updateDisplayName(accessToken, profileName).onSuccess {
                                    profileName = it
                                    profileStatus = "Имя профиля сохранено."
                                }.onFailure { failure ->
                                    profileStatus = when {
                                        failure.message.orEmpty().contains("display_name_length_invalid", true) ->
                                            "Имя должно содержать от 2 до 40 символов."
                                        else -> "Не удалось сохранить имя. Проверьте подключение и попробуйте снова."
                                    }
                                }
                                profileNameSaving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !profileNameSaving && profileName.trim().length in 2..40,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (profileNameSaving) "Сохраняем…" else "Сохранить имя")
                    }
                    if (profileStatus != null) {
                        Text(
                            profileStatus!!,
                            color = if (profileStatus!!.startsWith("Не удалось")) colors.error else colors.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Lotus · личные сообщения", color = colors.onSurfaceVariant)
                    Text(
                        "Сейчас сообщения передаются через сервер без сквозного шифрования. Не отправляйте секретные данные до подключения и проверки E2EE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Готово") } },
            dismissButton = {
                TextButton(onClick = {
                    showSettings = false
                    onLogout()
                }) { Text("Выйти из аккаунта") }
            }
        )
    }

    if (showNewChat) {
        ContactDiscoveryDialog(
            accessToken = accessToken,
            onDismiss = { showNewChat = false },
            onChatStarted = { chat ->
                val preview = toPreview(chat)
                chats = (listOf(preview) + chats.filterNot { it.id == preview.id })
                selectedChat = preview
                messages = emptyList()
                draft = ""
            }
        )
    }
}

@Composable
private fun ConversationScreen(
    chat: ChatPreview,
    messages: List<LocalMessage>,
    draft: String,
    sending: Boolean,
    loading: Boolean,
    encryptionReady: Boolean,
    encryptionError: String?,
    onRetryEncryption: () -> Unit,
    onDraftChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = colors.background, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onSend,
                        enabled = draft.isNotBlank() && !sending && encryptionReady,
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(colors.primary)
                    ) {
                        if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить", tint = colors.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад") }
                Avatar(chat.name, Modifier.size(44.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(chat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Личный чат", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "Ещё", tint = colors.onSurfaceVariant) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.35f)))
            if (loading && messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(chat.name, Modifier.size(72.dp))
                        Text(chat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Это начало вашей истории", color = colors.onSurfaceVariant)
                    }
                }
            } else {
                val messageListState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) messageListState.animateScrollToItem(messages.lastIndex)
                }
                LazyColumn(
                    state = messageListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start) {
                            Column(Modifier.fillMaxWidth(0.84f), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
                                Text(
                                    message.text,
                                    modifier = Modifier.clip(RoundedCornerShape(
                                        topStart = 19.dp, topEnd = 19.dp,
                                        bottomStart = if (message.mine) 19.dp else 5.dp,
                                        bottomEnd = if (message.mine) 5.dp else 19.dp
                                    )).background(if (message.mine) colors.primaryContainer else colors.surface)
                                        .padding(horizontal = 15.dp, vertical = 11.dp),
                                    color = if (message.mine) colors.onPrimaryContainer else colors.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(message.time, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    if (message.mine) Icon(Icons.Rounded.Check, contentDescription = "Отправлено", tint = colors.primary, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "Сообщения передаются через сервер; сквозное шифрование пока не реализовано.",
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatListItem(chat: ChatPreview, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.name, Modifier.size(54.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                chat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(chat.time, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Box(
                Modifier.size(7.dp).clip(CircleShape).background(colors.secondary.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun Avatar(name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val palettes = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.surfaceVariant to colors.onSurfaceVariant
    )
    val index = (name.hashCode() and Int.MAX_VALUE) % palettes.size
    Box(modifier.clip(CircleShape).background(palettes[index].first), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "✿", color = palettes[index].second, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

private fun displayTime(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrElse {
        val time = value.substringAfter('T', value).take(5)
        if (time.matches(Regex("\\d{2}:\\d{2}"))) time else ""
    }
}

private fun friendlyMessagingError(message: String?): String {
    val value = message.orEmpty()
    return when {
        value.contains("verified_phone_required", true) -> "Сначала войдите с подтверждённым номером телефона."
        value.contains("mutual_contact_required", true) -> "Сначала оба пользователя должны добавить друг друга в контакты Lotus."
        value.contains("server_not_configured", true) || value.contains("HTTP 404", true) ->
            "Сервер чатов ещё не развёрнут. Служебная часть Lotus требует настройки."
        value.isBlank() -> "Нет связи с сервером. Проверьте интернет и попробуйте ещё раз."
        else -> value.take(180)
    }
}
