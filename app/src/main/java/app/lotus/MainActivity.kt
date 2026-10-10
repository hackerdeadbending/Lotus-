package app.lotus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101310)
private val Panel = Color(0xFF191E19)
private val PanelSoft = Color(0xFF222922)
private val LotusGreen = Color(0xFFC8E6B2)
private val Muted = Color(0xFF9CA79B)
private val Paper = Color(0xFFF2F2E8)

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
    val initials: String,
    val name: String,
    val message: String,
    val time: String,
    val unread: Int = 0,
    val tint: Color
)

@Composable
private fun LotusApp() {
    var destination by remember { mutableStateOf(Destination.Chats) }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink,
        surface = Panel,
        primary = LotusGreen,
        onPrimary = Ink,
        onBackground = Paper,
        onSurface = Paper,
        secondary = Muted
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                when (destination) {
                    Destination.Chats -> ChatsScreen()
                    Destination.Browser -> SimpleScreen(
                        icon = { Icon(Icons.Outlined.Explore, null, tint = LotusGreen, modifier = Modifier.size(30.dp)) },
                        eyebrow = "A quieter way to explore",
                        title = "Your space,
your pace.",
                        description = "A private browsing space is planned for Lotus. Network protection is not active in this preview."
                    )
                    Destination.Privacy -> SimpleScreen(
                        icon = { Icon(Icons.Outlined.Shield, null, tint = LotusGreen, modifier = Modifier.size(30.dp)) },
                        eyebrow = "Privacy centre",
                        title = "Privacy by design.",
                        description = "Security controls will appear here as they are implemented and tested. This preview does not yet provide encrypted messaging."
                    )
                    Destination.Settings -> SimpleScreen(
                        icon = { Icon(Icons.Outlined.Settings, null, tint = LotusGreen, modifier = Modifier.size(30.dp)) },
                        eyebrow = "Make it yours",
                        title = "Your Lotus.",
                        description = "Appearance, notifications, identity and local data settings will live here."
                    )
                }
                Spacer(Modifier.weight(1f))
                BottomNavigation(destination = destination, onSelect = { destination = it })
            }
        }
    }
}

@Composable
private fun ChatsScreen() {
    val chats = listOf(
        ChatPreview("M", "Mira", "That sounds good. Talk soon 🌿", "12:42", 2, Color(0xFF465B43)),
        ChatPreview("A", "Alex", "Sent you the photos", "Yesterday", 0, Color(0xFF665547)),
        ChatPreview("L", "Luca", "Are we still on for tomorrow?", "Tue", 0, Color(0xFF4C526A)),
        ChatPreview("S", "Sofia", "Thank you ✨", "Mon", 0, Color(0xFF705263))
    )
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LotusMark()
            Spacer(Modifier.width(10.dp))
            Column {
                Text("lotus", fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = Paper, letterSpacing = (-0.7).sp)
                Text("A little more private.", fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(PanelSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = "Privacy preview", tint = LotusGreen, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text("YOUR SPACE", fontSize = 10.sp, color = Muted, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                Text("Messages", fontSize = 31.sp, fontWeight = FontWeight.Medium, color = Paper, letterSpacing = (-0.8).sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(30.dp)).background(Panel).padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = Muted, modifier = Modifier.size(13.dp))
                    Text("Preview", fontSize = 11.sp, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF29382A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = LotusGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Privacy starts here", fontWeight = FontWeight.Medium, color = Paper, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text("A first look at your new space.", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(25.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text("4 conversations", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(chats) { chat -> ChatRow(chat) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Demo conversations · no messages are sent or stored",
            color = Color(0xFF707B70),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun ChatRow(chat: ChatPreview) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).clickable { }.padding(vertical = 11.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(chat.tint),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.initials, fontSize = 19.sp, color = Paper, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.name, color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(chat.message, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unread > 0) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(LotusGreen).padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chat.unread.toString(), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SimpleScreen(
    icon: @Composable () -> Unit,
    eyebrow: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.Center
    ) {
        LotusMark()
        Spacer(Modifier.height(42.dp))
        Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Panel), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(22.dp))
        Text(eyebrow.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Paper, fontSize = 35.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp)
        Spacer(Modifier.height(15.dp))
        Text(description, color = Muted, fontSize = 15.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = LotusGreen, contentColor = Ink),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("Coming into focus", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LotusMark() {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(LotusGreen),
        contentAlignment = Alignment.Center
    ) {
        Text("✳", fontSize = 24.sp, color = Ink)
    }
}

@Composable
private fun BottomNavigation(destination: Destination, onSelect: (Destination) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF141814)).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Destination.values().forEach { item ->
            val selected = item == destination
            val icon = when (item) {
                Destination.Chats -> Icons.Outlined.ChatBubbleOutline
                Destination.Browser -> Icons.Outlined.Explore
                Destination.Privacy -> Icons.Outlined.Shield
                Destination.Settings -> Icons.Outlined.Settings
            }
            Column(
                modifier = Modifier.clip(RoundedCornerShape(15.dp)).clickable { onSelect(item) }.padding(horizontal = 15.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = item.label, tint = if (selected) LotusGreen else Muted, modifier = Modifier.size(21.dp))
                Text(item.label, color = if (selected) LotusGreen else Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
