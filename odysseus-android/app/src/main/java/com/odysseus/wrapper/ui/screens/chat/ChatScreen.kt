package com.odysseus.wrapper.ui.screens.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.data.remote.MessageItem
import com.odysseus.wrapper.data.remote.SessionItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val sessions       by vm.sessions.collectAsStateWithLifecycle()
    val currentSession by vm.currentSession.collectAsStateWithLifecycle()
    val messages       by vm.messages.collectAsStateWithLifecycle()
    val sending        by vm.sending.collectAsStateWithLifecycle()
    val loading        by vm.loading.collectAsStateWithLifecycle()
    val error          by vm.error.collectAsStateWithLifecycle()

    var inputText   by remember { mutableStateOf("") }
    var useWeb      by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ChatGPT style Top Bar
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            // Model Selector Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(currentSession?.model?.substringAfterLast("/")?.take(15) ?: "Odysseus AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.newSession() }) {
                Icon(Icons.Default.Edit, "New Chat")
            }
        }

        // Error banner
        error?.let {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                IconButton(onClick = { vm.clearError() }, Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
            }
        }

        // Messages
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        if (messages.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("How can I help you today?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(messages) { msg ->
                    ChatGPTStyleBubble(msg)
                }
                if (sending) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        // ChatGPT Style Input Bar
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.08f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { useWeb = !useWeb }, Modifier.size(36.dp)) {
                    Icon(if(useWeb) Icons.Default.Language else Icons.Default.Add, null, 
                        tint = if (useWeb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (inputText.isEmpty()) {
                                Text("Message Odysseus...", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (sending) {
                    IconButton(
                        onClick = { vm.stopGeneration() },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(
                        onClick = {
                            val t = inputText.trim()
                            if (t.isNotBlank()) { vm.sendMessage(t, useWeb); inputText = "" }
                        },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (inputText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(0.2f) else MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.ArrowUpward, "Send", tint = if(inputText.isBlank()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatGPTStyleBubble(message: MessageItem) {
    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // AI Logo
            Box(Modifier.size(28.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
            }
            Spacer(Modifier.width(16.dp))
        }

        Box(
            Modifier
                .weight(1f, fill = false)
                .background(
                    if (isUser) MaterialTheme.colorScheme.onSurface.copy(0.08f) else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .padding(if (isUser) PaddingValues(horizontal = 16.dp, vertical = 10.dp) else PaddingValues(top = 4.dp))
        ) {
            Text(
                text = message.content,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = { clipboard.setText(AnnotatedString(message.content)); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}
