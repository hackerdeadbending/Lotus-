package app.lotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unread: Int = 0,
    val subtitle: String = "Lotus"
)

private data class LocalMessage(val text: String, val mine: Boolean, val time: String)

@Composable
fun ChatsScreen() {
    var chats by remember {
        mutableStateOf(
            listOf(
                ChatPreview("1", "Аня", "Всё хорошо, не волнуйся", "12:41", 2, "была недавно"),
                ChatPreview("2", "Дом", "Мама: приезжайте аккуратно", "11:18", 0, "3 участника"),
                ChatPreview("3", "Игорь", "Документы отправил", "Вчера", 0, "был сегодня"),
                ChatPreview("4", "Сестра", "Сообщение удалено", "Пн", 0, "была вчера"),
                ChatPreview("5", "Коллеги", "Встреча перенесена", "Вс", 0, "5 участников")
            )
        )
    }
    var selectedChat by remember { mutableStateOf<ChatPreview?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewChat by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            mapOf(
                "1" to listOf(
                    LocalMessage("Привет! Как ты?", false, "12:38"),
                    LocalMessage("Всё хорошо, не волнуйся", false, "12:41"),
                    LocalMessage("Хорошо 💚 Напиши, когда будешь дома", true, "12:42")
                ),
                "2" to listOf(
                    LocalMessage("В воскресенье собираемся у нас", false, "11:10"),
                    LocalMessage("Мама: приезжайте аккуратно", false, "11:18")
                )
            )
        )
    }
    val colors = MaterialTheme.colorScheme
    val activeChat = selectedChat

    if (activeChat != null) {
        ConversationScreen(
            chat = activeChat,
            messages = messages[activeChat.id].orEmpty(),
            draft = draft,
            onDraftChange = { draft = it },
            onBack = { selectedChat = null; draft = "" },
            onSend = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    val updated = messages[activeChat.id].orEmpty() + LocalMessage(text, true, "сейчас")
                    messages = messages + (activeChat.id to updated)
                    chats = chats.map {
                        if (it.id == activeChat.id) it.copy(lastMessage = text, time = "сейчас", unread = 0) else it
                    }
                    selectedChat = activeChat.copy(lastMessage = text, time = "сейчас", unread = 0)
                    draft = ""
                }
            }
        )
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
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                            Text("✿", color = colors.onPrimaryContainer, fontSize = 25.sp)
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                        Text("Lotus", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp), color = colors.onBackground)
                    }
                    Text("ВАШЕ ЛИЧНОЕ ПРОСТРАНСТВО", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp), color = colors.onSurfaceVariant)
                }
                IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) searchQuery = "" }) {
                    Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Поиск", tint = colors.onSurfaceVariant)
                }
                IconButton(onClick = { showSettings = true }) {
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
                Text(chats.size.toString() + " чатов", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }

            val visibleChats = chats.filter { it.name.contains(searchQuery, true) || it.lastMessage.contains(searchQuery, true) }
            if (visibleChats.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(70.dp).clip(CircleShape).background(colors.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(30.dp))
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                        Text("Попробуйте другое имя или сообщение", color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(visibleChats, key = { it.id }) { chat ->
                        ChatListItem(chat) {
                            selectedChat = chat
                            draft = ""
                            chats = chats.map { if (it.id == chat.id) it.copy(unread = 0) else it }
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Lotus · предварительная версия", color = colors.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Уведомления")
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                    Text("Чаты пока работают как локальный макет. Регистрация, защищённая доставка и синхронизация ещё не подключены.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Готово") } }
        )
    }

    if (showNewChat) {
        ContactDiscoveryDialog(onDismiss = { showNewChat = false })
    }
}

@Composable
private fun ConversationScreen(
    chat: ChatPreview,
    messages: List<LocalMessage>,
    draft: String,
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
                    IconButton(onClick = onSend, enabled = draft.isNotBlank(), modifier = Modifier.size(50.dp).clip(CircleShape).background(colors.primary)) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить", tint = colors.onPrimary)
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
                    Text(chat.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = "Ещё", tint = colors.onSurfaceVariant) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.35f)))
            if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(chat.name, Modifier.size(72.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                        Text(chat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Это начало вашей истории", color = colors.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start) {
                            Column(Modifier.fillMaxWidth(0.84f), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
                                Text(
                                    message.text,
                                    modifier = Modifier.clip(RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp, bottomStart = if (message.mine) 19.dp else 5.dp, bottomEnd = if (message.mine) 5.dp else 19.dp))
                                        .background(if (message.mine) colors.primaryContainer else colors.surface)
                                        .padding(horizontal = 15.dp, vertical = 11.dp),
                                    color = if (message.mine) colors.onPrimaryContainer else colors.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(message.time, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    if (message.mine) Icon(Icons.Rounded.Check, contentDescription = "Локально", tint = colors.primary, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
            Text("Демо-режим · сообщения пока не доставляются другим пользователям", Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChatListItem(chat: ChatPreview, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.name, Modifier.size(56.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.foundation.layout.Spacer(Modifier.height(5.dp))
            Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.time, style = MaterialTheme.typography.labelSmall, color = if (chat.unread > 0) colors.secondary else colors.onSurfaceVariant)
            if (chat.unread > 0) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(7.dp))
                Box(Modifier.size(22.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                    Text(chat.unread.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                }
            }
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
