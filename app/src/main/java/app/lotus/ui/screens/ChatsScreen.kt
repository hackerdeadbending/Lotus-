package app.lotus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class ChatPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unread: Int = 0
)

@Composable
fun ChatsScreen() {
    var chats by remember {
        mutableStateOf(
            listOf(
                ChatPreview("1", "Аня", "Всё хорошо, не волнуйся", "12:41", 2),
                ChatPreview("2", "Группа «Дом»", "Мама: приезжайте аккуратно", "11:18"),
                ChatPreview("3", "Игорь", "Документы отправил", "вчера"),
                ChatPreview("4", "Сестра", "Сообщение удалено", "пн"),
                ChatPreview("5", "Коллеги", "Встреча перенесена", "вс"),
            )
        )
    }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showNewChat by remember { mutableStateOf(false) }
    var newChatName by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<ChatPreview?>(null) }
    var draft by remember { mutableStateOf("") }
    var conversationMessages by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val colors = MaterialTheme.colorScheme
    val visibleChats = chats.filter { chat ->
        !searchOpen || chat.name.contains(searchQuery, ignoreCase = true) ||
            chat.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Новый чат")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❀", color = colors.onPrimaryContainer, fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Lotus",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.7).sp
                            ),
                            color = colors.onBackground
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "ВАШЕ ЛИЧНОЕ ПРОСТРАНСТВО",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) searchQuery = ""
                    }
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = if (searchOpen) "Закрыть поиск" else "Поиск",
                        tint = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Настройки", tint = colors.onSurfaceVariant)
                }
            }

            AnimatedVisibility(visible = searchOpen, enter = fadeIn() + slideInVertically()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Поиск по чатам") }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.secondary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Тишина — это тоже защита",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Личный интерфейс Lotus",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Сообщения",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${chats.size} чатов",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }

            if (visibleChats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "Чатов пока нет" else "Ничего не найдено",
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(visibleChats, key = { _, chat -> chat.id }) { index, chat ->
                        var visible by remember(chat.id) { mutableStateOf(false) }
                        LaunchedEffect(chat.id) {
                            delay(index * 55L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { it / 8 }
                            )
                        ) {
                            ChatListItem(chat = chat, onClick = {
                                selectedChat = chat
                                draft = ""
                            })
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
                    Text("Lotus · демонстрационная версия интерфейса", color = colors.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Уведомления")
                        Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                    }
                    Text(
                        "Сообщения пока хранятся только в памяти приложения. Защищённая доставка ещё не подключена.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Готово") } }
        )
    }

    if (showNewChat) {
        AlertDialog(
            onDismissRequest = { showNewChat = false; newChatName = "" },
            title = { Text("Новый чат") },
            text = {
                OutlinedTextField(
                    value = newChatName,
                    onValueChange = { newChatName = it },
                    label = { Text("Имя контакта или группы") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    enabled = newChatName.isNotBlank(),
                    onClick = {
                        val name = newChatName.trim()
                        if (name.isNotEmpty()) {
                            val id = "local-${System.currentTimeMillis()}"
                            val newChat = ChatPreview(id, name, "Начните общение", "сейчас")
                            chats = listOf(newChat) + chats
                            conversationMessages = conversationMessages + (id to emptyList())
                            selectedChat = newChat
                        }
                        showNewChat = false
                        newChatName = ""
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showNewChat = false; newChatName = "" }) { Text("Отмена") }
            }
        )
    }

    selectedChat?.let { chat ->
        val messages = conversationMessages[chat.id] ?: listOf(chat.lastMessage)
        AlertDialog(
            onDismissRequest = { selectedChat = null },
            title = { Text(chat.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (messages.isEmpty()) {
                            Text(
                                "Это локальный макет чата. Сообщения не доставляются другим людям.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        } else {
                            messages.takeLast(8).forEachIndexed { index, message ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End
                                ) {
                                    Text(
                                        text = message,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (index % 2 == 0) colors.surfaceVariant else colors.secondaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        color = if (index % 2 == 0) colors.onSurface else colors.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Сообщение") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val message = draft.trim()
                        val previous = conversationMessages[chat.id]
                            ?: if (chat.lastMessage == "Начните общение") emptyList() else listOf(chat.lastMessage)
                        val updated = previous + message
                        conversationMessages = conversationMessages + (chat.id to updated)
                        chats = chats.map {
                            if (it.id == chat.id) it.copy(lastMessage = message, time = "сейчас", unread = 0) else it
                        }
                        selectedChat = chat.copy(lastMessage = message, time = "сейчас", unread = 0)
                        draft = ""
                    }
                ) { Text("Отправить") }
            },
            dismissButton = { TextButton(onClick = { selectedChat = null }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun ChatListItem(chat: ChatPreview, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val avatarBackgrounds = listOf(colors.primaryContainer, colors.secondaryContainer, colors.surfaceVariant)
    val avatarColor = listOf(colors.onPrimaryContainer, colors.onSecondaryContainer, colors.onSurfaceVariant)
    val avatarIndex = (chat.id.hashCode().and(Int.MAX_VALUE)) % avatarBackgrounds.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(avatarBackgrounds[avatarIndex]),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = avatarColor[avatarIndex]
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.time,
                style = MaterialTheme.typography.labelSmall,
                color = if (chat.unread > 0) colors.secondary else colors.onSurfaceVariant
            )
            if (chat.unread > 0) {
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unread.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}
