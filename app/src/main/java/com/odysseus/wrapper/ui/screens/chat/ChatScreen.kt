package com.odysseus.wrapper.ui.screens.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val sessions       by vm.sessions.collectAsStateWithLifecycle()
    val currentSession by vm.currentSession.collectAsStateWithLifecycle()
    val messages       by vm.messages.collectAsStateWithLifecycle()
    val sending        by vm.sending.collectAsStateWithLifecycle()
    val loading        by vm.loading.collectAsStateWithLifecycle()
    val error          by vm.error.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var inputText   by remember { mutableStateOf("") }
    var useWeb      by remember { mutableStateOf(false) }
    var showRename  by remember { mutableStateOf<SessionItem?>(null) }

    // Rename dialog
    showRename?.let { session ->
        var name by remember(session.id) { mutableStateOf(session.name) }
        AlertDialog(
            onDismissRequest = { showRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { vm.renameSession(session.id, name); showRename = null }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = null }) { Text("Cancel") } }
        )
    }

    // Sessions drawer (slide from left — matches webapp sidebar)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(290.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chats", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    FilledTonalIconButton(onClick = {
                        vm.newSession()
                        scope.launch { drawerState.close() }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, "New chat", Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))

                if (loading && sessions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(sessions, key = { it.id }) { session ->
                            val selected = currentSession?.id == session.id
                            NavigationDrawerItem(
                                label = {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(session.name.ifBlank { "Untitled" }, maxLines = 1,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                            Text("${session.message_count} msgs · ${session.model.substringAfterLast("/").take(20)}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                                        }
                                    }
                                },
                                selected = selected,
                                onClick = {
                                    vm.selectSession(session)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    Row {
                                        IconButton(onClick = { showRename = session }, Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, null, Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                        }
                                        IconButton(onClick = { vm.deleteSession(session.id) }, Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp).height(52.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor   = MaterialTheme.colorScheme.primary.copy(0.12f),
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor        = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor      = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // ── Chat sub-topbar: session name + actions ───────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sessions list button
                TextButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.UnfoldMore, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(currentSession?.name?.ifBlank { "Select chat" } ?: "Select chat",
                        fontSize = 13.sp, maxLines = 1,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                // Web search toggle
                IconButton(onClick = { useWeb = !useWeb }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Language, "Web search",
                        Modifier.size(16.dp),
                        tint = if (useWeb) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
                // New chat
                IconButton(onClick = { vm.newSession() }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "New chat", Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))

            // ── Error banner ──────────────────────────────────────────────────
            error?.let {
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    IconButton(onClick = { vm.clearError() }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                    }
                }
            }

            // ── Messages ──────────────────────────────────────────────────────
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
            }

            if (messages.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Odysseus",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(if (currentSession == null) "Select or create a chat" else "Start the conversation",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.4f))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.hashCode() }) { msg ->
                        OdysseusChatBubble(msg)
                    }
                    if (sending) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                // Pulsing dots — like webapp thinking indicator
                                Box(
                                    Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        repeat(3) {
                                            Box(Modifier.size(6.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(0.5f), CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Input area — matches webapp send bar ──────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Message Odysseus…",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                            fontSize = 14.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.outline.copy(0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                        cursorColor          = MaterialTheme.colorScheme.primary
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(6.dp))
                if (sending) {
                    IconButton(
                        onClick = { vm.stopGeneration() },
                        modifier = Modifier.size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.Stop, "Stop",
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(
                        onClick = {
                            val t = inputText.trim()
                            if (t.isNotBlank()) { vm.sendMessage(t, useWeb); inputText = "" }
                        },
                        modifier = Modifier.size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isBlank()) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.primary
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, "Send",
                            Modifier.size(18.dp),
                            tint = if (inputText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                   else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OdysseusChatBubble(message: MessageItem) {
    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // AI avatar — red circle with "O" like webapp
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("O", color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd   = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .combinedClickable(onClick = {}, onLongClick = { showMenu = true })
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text      = message.content,
                color     = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                fontSize  = 14.sp,
                lineHeight = 21.sp
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}
