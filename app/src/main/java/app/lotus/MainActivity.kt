package app.lotus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val Ink = Color(0xFF101310)
private val Panel = Color(0xFF191E19)
private val PanelSoft = Color(0xFF222922)
private val LotusGreen = Color(0xFFC8E6B2)
private val Muted = Color(0xFF9CA79B)
private val Paper = Color(0xFFF2F2E8)
private val Border = Color(0xFF343D33)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LotusApp() }
    }
}

private enum class Destination(val label: String) {
    Chats("Chats"), Browser("Explore"), Privacy("Privacy"), Settings("Settings")
}

private data class ChatPreview(
    val id: String,
    val initials: String,
    val name: String,
    val message: String,
    val time: String,
    val unread: Int = 0,
    val tint: Color
)

private data class LocalMessage(val text: String, val outgoing: Boolean)

@Composable
private fun LotusApp() {
    var profileName by rememberSaveable { mutableStateOf("") }
    var profileId by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf("welcome") }
    var destinationName by rememberSaveable { mutableStateOf("Chats") }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var contactDraft by rememberSaveable { mutableStateOf("") }
    var messageDraft by rememberSaveable { mutableStateOf("") }
    val chats = remember {
        mutableStateListOf(
            ChatPreview("mira", "M", "Mira", "That sounds good. Talk soon 🌿", "12:42", 2, Color(0xFF465B43)),
            ChatPreview("alex", "A", "Alex", "Sent you the photos", "Yesterday", 0, Color(0xFF665547)),
            ChatPreview("luca", "L", "Luca", "Are we still on for tomorrow?", "Tue", 0, Color(0xFF4C526A)),
            ChatPreview("sofia", "S", "Sofia", "Thank you ✨", "Mon", 0, Color(0xFF705263))
        )
    }
    val messages = remember { mutableStateListOf<LocalMessage>() }
    val selectedChat = chats.firstOrNull { it.id == selectedId }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = LotusGreen,
        onPrimary = Ink, onBackground = Paper, onSurface = Paper, secondary = Muted
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
                when (page) {
                    "welcome" -> WelcomeScreen(profileName, { profileName = it }) {
                        if (profileName.trim().isNotEmpty()) {
                            profileName = profileName.trim().take(32)
                            profileId = "LOTUS-" + UUID.randomUUID().toString().take(8).uppercase()
                            page = "chats"
                        }
                    }
                    "chats" -> ChatsScreen(chats, profileName, profileId, {
                        selectedId = it.id
                        messages.clear()
                        messages.add(LocalMessage("This is a local preview conversation.", false))
                        page = "conversation"
                    }, {
                        contactDraft = ""
                        page = "new"
                    })
                    "conversation" -> ConversationScreen(
                        selectedChat, messages, messageDraft, { messageDraft = it },
                        { messageDraft = ""; page = "chats" },
                        {
                            val text = messageDraft.trim()
                            if (text.isNotEmpty()) {
                                messages.add(LocalMessage(text, true))
                                messageDraft = ""
                            }
                        }
                    )
                    "new" -> NewContactScreen(contactDraft, { contactDraft = it }, { page = "chats" }) {
                        val name = contactDraft.trim().take(32)
                        if (name.isNotEmpty()) {
                            val id = UUID.randomUUID().toString()
                            chats.add(0, ChatPreview(id, name.take(1).uppercase(), name, "Local demo contact", "Now", 0, Color(0xFF52624A)))
                            selectedId = id
                            messages.clear()
                            messages.add(LocalMessage("Demo conversation created locally. Nothing was sent.", false))
                            contactDraft = ""
                            page = "conversation"
                        }
                    }
                    "browser" -> SimpleScreen({ Icon(Icons.Outlined.Explore, null, tint = LotusGreen, modifier = Modifier.size(30.dp)) }, "A quieter way to explore", "Your space,\nyour pace.", "A private browsing space is planned for Lotus. Tor and network protection are not active in this preview.")
                    "privacy" -> SimpleScreen({ Icon(Icons.Outlined.Shield, null, tint = LotusGreen, modifier = Modifier.size(30.dp)) }, "Privacy centre", "Privacy by design.", "End-to-end encryption, protected networking and secure local storage are not implemented yet.")
                    "settings" -> SettingsScreen(profileName, profileId) {
                        profileName = ""
                        profileId = ""
                        page = "welcome"
                    }
                }
                if (page == "chats" || page == "browser" || page == "privacy" || page == "settings") {
                    Spacer(Modifier.weight(1f))
                    BottomNavigation(destinationName) { dest ->
                        destinationName = dest.label
                        page = when (dest) {
                            Destination.Chats -> "chats"
                            Destination.Browser -> "browser"
                            Destination.Privacy -> "privacy"
                            Destination.Settings -> "settings"
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(name: String, onName: (String) -> Unit, onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp), verticalArrangement = Arrangement.Center) {
        LotusMark()
        Spacer(Modifier.height(34.dp))
        Text("A SPACE OF YOUR OWN", color = Muted, fontSize = 10.sp, letterSpacing = 2.2.sp)
        Spacer(Modifier.height(12.dp))
        Text("Meet Lotus.", color = Paper, fontSize = 39.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text("A calmer place for your conversations.", color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(32.dp))
        Text("Choose a display name", color = Paper, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        LotusTextField(name, onName, "What should we call you?")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Lock, null, tint = LotusGreen, modifier = Modifier.padding(top = 2.dp).size(16.dp))
            Spacer(Modifier.width(9.dp))
            Text("No phone number or email is requested. This preview creates a temporary profile, not a cryptographic identity.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = LotusGreen, contentColor = Ink), shape = RoundedCornerShape(16.dp)) {
            Text("Create local profile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Text("EARLY PREVIEW · MESSAGES ARE NOT ENCRYPTED OR TRANSMITTED", color = Color(0xFF778174), fontSize = 9.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun ChatsScreen(chats: List<ChatPreview>, profileName: String, profileId: String, onOpen: (ChatPreview) -> Unit, onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LotusMark()
            Spacer(Modifier.width(10.dp))
            Column {
                Text("lotus", fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = Paper, letterSpacing = (-0.7).sp)
                Text("A little more private.", fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNew, modifier = Modifier.size(44.dp).clip(CircleShape).background(PanelSoft)) {
                Icon(Icons.Outlined.Add, "New conversation", tint = LotusGreen)
            }
        }
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("YOUR SPACE", fontSize = 10.sp, color = Muted, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Messages", fontSize = 31.sp, fontWeight = FontWeight.Medium, color = Paper, letterSpacing = (-0.8).sp)
            }
            Row(Modifier.clip(RoundedCornerShape(30.dp)).background(Panel).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WifiOff, null, tint = Muted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("Preview", fontSize = 11.sp, color = Muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF29382A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Shield, null, tint = LotusGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Welcome, $profileName", fontWeight = FontWeight.Medium, color = Paper, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text("Temporary profile · $profileId", color = Muted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text(chats.size.toString() + " conversations", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(chats, key = { it.id }) { chat -> ChatRow(chat) { onOpen(chat) } }
        }
        Text("Demo contacts · nothing is sent over a network", color = Color(0xFF707B70), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 10.dp))
    }
}

@Composable
private fun ChatRow(chat: ChatPreview, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(vertical = 11.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(chat.tint), contentAlignment = Alignment.Center) {
            Text(chat.initials, fontSize = 19.sp, color = Paper, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.name, color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(chat.message, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unread > 0) Box(Modifier.clip(CircleShape).background(LotusGreen).padding(horizontal = 7.dp, vertical = 3.dp)) {
                Text(chat.unread.toString(), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConversationScreen(chat: ChatPreview?, messages: List<LocalMessage>, draft: String, onDraft: (String) -> Unit, onBack: () -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Paper) }
            Box(Modifier.size(40.dp).clip(CircleShape).background(chat?.tint ?: PanelSoft), contentAlignment = Alignment.Center) {
                Text(chat?.initials ?: "?", color = Paper, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(chat?.name ?: "Conversation", color = Paper, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Local preview only", color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.Lock, "Encryption not active", tint = Muted, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        }
        Text("Preview only: messages stay in temporary app memory. They are not sent or end-to-end encrypted.", Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(10.dp), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(14.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
                    Box(Modifier.fillMaxWidth(0.84f).clip(RoundedCornerShape(16.dp)).background(if (message.outgoing) Color(0xFF2B3A2A) else Panel).padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text(message.text, color = Paper, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            LotusTextField(draft, onDraft, "Write a demo message…", Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = draft.trim().isNotEmpty(), modifier = Modifier.size(48.dp).clip(CircleShape).background(if (draft.trim().isNotEmpty()) LotusGreen else PanelSoft)) {
                Icon(Icons.Outlined.Send, "Add local demo message", tint = if (draft.trim().isNotEmpty()) Ink else Muted)
            }
        }
    }
}

@Composable
private fun NewContactScreen(name: String, onName: (String) -> Unit, onBack: () -> Unit, onCreate: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Paper) }
        Spacer(Modifier.height(30.dp))
        Text("NEW CONVERSATION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("Start with a name.", color = Paper, fontSize = 32.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text("Contact discovery and invitations are not connected yet. Add a sample contact to explore the conversation interface.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(24.dp))
        LotusTextField(name, onName, "Contact display name")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreate, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = LotusGreen, contentColor = Ink), shape = RoundedCornerShape(15.dp)) {
            Text("Create demo conversation", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsScreen(profileName: String, profileId: String, onReset: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp)) {
        LotusMark()
        Spacer(Modifier.height(30.dp))
        Text("SETTINGS", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your Lotus.", color = Paper, fontSize = 34.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(24.dp))
        Text("LOCAL PROFILE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp)) {
            Text(profileName, color = Paper, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(profileId, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("PROTECTION STATUS", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        listOf("End-to-end encryption", "Tor network protection", "Encrypted local database", "Offline message transport").forEach { label ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(label, color = Paper, fontSize = 13.sp)
                Text("Not implemented", color = Color(0xFFE0B38B), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PanelSoft, contentColor = Paper), shape = RoundedCornerShape(15.dp)) {
            Text("Reset temporary profile")
        }
        Spacer(Modifier.height(10.dp))
        Text("The profile exists only in UI state and may disappear when the app process is recreated. This ID is not a cryptographic key.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun SimpleScreen(icon: @Composable () -> Unit, eyebrow: String, title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 26.dp), verticalArrangement = Arrangement.Center) {
        LotusMark()
        Spacer(Modifier.height(42.dp))
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Panel), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(22.dp))
        Text(eyebrow.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Paper, fontSize = 35.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(15.dp))
        Text(description, color = Muted, fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = LotusGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Not active in this preview", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LotusTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Muted, fontSize = 13.sp) }, singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Paper, unfocusedTextColor = Paper, cursorColor = LotusGreen,
            focusedBorderColor = LotusGreen, unfocusedBorderColor = Border,
            focusedContainerColor = Panel, unfocusedContainerColor = Panel,
            focusedPlaceholderColor = Muted, unfocusedPlaceholderColor = Muted
        )
    )
}

@Composable
private fun LotusMark() {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(LotusGreen), contentAlignment = Alignment.Center) {
        Text("✳", fontSize = 24.sp, color = Ink)
    }
}

@Composable
private fun BottomNavigation(current: String, onSelect: (Destination) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF141814)).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        Destination.values().forEach { item ->
            val selected = item.label == current
            val icon = when (item) {
                Destination.Chats -> Icons.Outlined.ChatBubbleOutline
                Destination.Browser -> Icons.Outlined.Explore
                Destination.Privacy -> Icons.Outlined.Shield
                Destination.Settings -> Icons.Outlined.Settings
            }
            Column(Modifier.clip(RoundedCornerShape(15.dp)).clickable { onSelect(item) }.padding(horizontal = 15.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, item.label, tint = if (selected) LotusGreen else Muted, modifier = Modifier.size(21.dp))
                Text(item.label, color = if (selected) LotusGreen else Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
