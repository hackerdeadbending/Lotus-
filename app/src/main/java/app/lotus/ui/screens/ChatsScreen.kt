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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen() {
    var chats by remember {
        mutableStateOf(
            listOf(
                ChatPreview("1", "Аня", "Всё хорошо, не волнуйся", "12:41", 2),
                ChatPreview("2", "Группа \"Дом\"", "Мама: приезжайте аккуратно", "11:18"),
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

    val visibleChats = chats.filter { chat ->
        !searchOpen ||
            chat.name.contains(searchQuery, ignoreCase = true) ||
            chat.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Поиск чатов") }
                        )
                    } else {
                        Text(
                            text = "Lotus",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchOpen) {
                            searchQuery = ""
                            searchOpen = false
                        } else {
                            searchOpen = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = if (searchOpen) "Закрыть поиск" else "Поиск",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChat = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Новый чат")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (visibleChats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isBlank()) "Чатов пока нет" else "Ничего не найдено",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(visibleChats, key = { _, chat -> chat.id }) { index, chat ->
                    var visible by remember(chat.id) { mutableStateOf(false) }
                    LaunchedEffect(chat.id) {
                        delay(index * 60L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 5 }
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

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Настройки") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lotus · демонстрационная версия интерфейса")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Уведомления")
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                    Text(
                        "Сообщения пока хранятся только в памяти приложения. Защищённая доставка ещё не подключена.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Готово") }
            }
        )
    }

    if (showNewChat) {
        AlertDialog(
            onDismissRequest = {
                showNewChat = false
                newChatName = ""
            },
            title = { Text("Новый чат") },
            text = {
                OutlinedTextField(
                    value = newChatName,
                    onValueChange = { newChatName = it },
                    label = { Text("Имя контакта или группы") },
                    singleLine = true
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
                TextButton(onClick = {
                    showNewChat = false
                    newChatName = ""
                }) { Text("Отмена") }
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
                                "Это локальный макет чата. Отправленные здесь сообщения не доставляются другим людям.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            messages.takeLast(8).forEach { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Сообщение") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val text = draft.trim()
                        val updatedMessages = (conversationMessages[chat.id]
                            ?: if (chat.lastMessage == "Начните общение") emptyList() else listOf(chat.lastMessage)) + text
                        conversationMessages = conversationMessages + (chat.id to updatedMessages)
                        chats = chats.map {
                            if (it.id == chat.id) it.copy(lastMessage = text, time = "сейчас", unread = 0) else it
                        }
                        selectedChat = chat.copy(lastMessage = text, time = "сейчас", unread = 0)
                        draft = ""
                    }
                ) { Text("Отправить") }
            },
            dismissButton = {
                TextButton(onClick = { selectedChat = null }) { Text("Закрыть") }
            }
        )
    }
}

@Composable
private fun ChatListItem(chat: ChatPreview, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = chat.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unread.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
