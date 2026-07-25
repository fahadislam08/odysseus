package com.odysseus.wrapper.ui.screens.email

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.data.remote.EmailItem
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailScreen(vm: EmailViewModel = viewModel()) {
    val emails   by vm.emails.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val loading  by vm.loading.collectAsStateWithLifecycle()
    val error    by vm.error.collectAsStateWithLifecycle()
    val folder   by vm.folder.collectAsStateWithLifecycle()
    val folders  by vm.folders.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val aiReply  by vm.aiReply.collectAsStateWithLifecycle()

    var showCompose   by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }
    var showSearch    by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }

    // If an email is selected, show detail
    if (selected != null) {
        EmailDetailScreen(
            detail   = selected!!,
            aiReply  = aiReply,
            accounts = accounts,
            onBack   = { vm.close() },
            onReply  = { accountId, to, subject, body ->
                vm.send(accountId, to, subject, body, replyTo = selected!!.uid)
                vm.close()
            },
            onAiReply    = { vm.generateAiReply(selected!!.uid) },
            onDelete     = { vm.delete(selected!!.uid) },
            onArchive    = { vm.archive(selected!!.uid) },
            onMarkUnread = { vm.markUnread(selected!!.uid) }
        )
        return
    }

    if (showCompose) {
        ComposeEmailDialog(
            accounts  = accounts,
            onDismiss = { showCompose = false },
            onSend    = { accountId, to, subject, body, cc ->
                vm.send(accountId, to, subject, body, cc)
                showCompose = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search email…") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.3f))
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Email", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box {
                                TextButton(onClick = { showFolderMenu = true }) {
                                    Text(folder, style = MaterialTheme.typography.labelMedium)
                                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = showFolderMenu,
                                    onDismissRequest = { showFolderMenu = false }) {
                                    listOf("INBOX", "Sent", "Archive", "Trash").forEach { f ->
                                        DropdownMenuItem(text = { Text(f) },
                                            onClick = { vm.setFolder(f); showFolderMenu = false })
                                    }
                                    folders.filter { f -> listOf("INBOX","Sent","Archive","Trash").none {
                                        it.equals(f.full_name, ignoreCase = true) } }.forEach { f ->
                                        DropdownMenuItem(text = { Text(f.name) },
                                            onClick = { vm.setFolder(f.full_name); showFolderMenu = false })
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) { searchQuery = ""; vm.load() } }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null)
                    }
                    if (showSearch) {
                        IconButton(onClick = { vm.search(searchQuery) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    } else {
                        IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCompose = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Create, "Compose")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            error?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(it, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            when {
                loading         -> LoadingBox()
                emails.isEmpty() -> EmptyBox("No messages in $folder.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(emails, key = { it.uid }) { email ->
                        EmailListItem(email, onTap = { vm.open(email.uid) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun EmailListItem(email: EmailItem, onTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onTap() }
            .background(if (!email.is_read) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center) {
            Text(email.sender_name.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                Text(email.sender_name.ifBlank { email.sender },
                    fontWeight = if (!email.is_read) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(email.date.take(10), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
            }
            Text(email.subject.ifBlank { "(no subject)" }, maxLines = 1,
                fontWeight = if (!email.is_read) FontWeight.SemiBold else FontWeight.Normal,
                overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(email.snippet, maxLines = 1, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                overflow = TextOverflow.Ellipsis)
        }
        if (email.is_flagged)
            Icon(Icons.Default.Flag, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        if (email.has_attachments)
            Icon(Icons.Default.AttachFile, null, Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(0.4f))
        if (!email.is_read)
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    detail: com.odysseus.wrapper.data.remote.EmailDetail,
    aiReply: String?,
    accounts: List<com.odysseus.wrapper.data.remote.EmailAccountItem>,
    onBack: () -> Unit,
    onReply: (Int, String, String, String) -> Unit,
    onAiReply: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onMarkUnread: () -> Unit
) {
    var showReply by remember { mutableStateOf(false) }
    var replyBody by remember { mutableStateOf("") }

    LaunchedEffect(aiReply) {
        if (!aiReply.isNullOrBlank() && aiReply != "Generating…") replyBody = aiReply
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.subject.ifBlank { "(no subject)" }, maxLines = 1,
                    overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAiReply) { Icon(Icons.Default.AutoAwesome, "AI Reply") }
                    IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, "Archive") }
                    IconButton(onClick = onMarkUnread) { Icon(Icons.Default.Email, "Mark unread") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f).padding(16.dp)) {
                item {
                    Text("From: ${detail.sender_name.ifBlank { detail.sender }}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f))
                    if (detail.to.isNotEmpty())
                        Text("To: ${detail.to.joinToString(", ")}", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.7f))
                    Text("Date: ${detail.date}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(detail.body_text.ifBlank { detail.body_html.replace(Regex("<[^>]*>"), "") },
                        style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    if (detail.attachments.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Attachments:", fontWeight = FontWeight.SemiBold)
                        detail.attachments.forEach { att ->
                            Row(Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(att.filename, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Reply area
            if (showReply || !aiReply.isNullOrBlank()) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)) {
                    if (aiReply == "Generating…") {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = replyBody, onValueChange = { replyBody = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        label = { Text("Reply") })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showReply = false; replyBody = "" }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val accId = accounts.firstOrNull()?.id ?: 0
                            onReply(accId, detail.sender, "Re: ${detail.subject}", replyBody)
                        }, enabled = replyBody.isNotBlank()) { Text("Send") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showReply = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reply")
                    }
                    OutlinedButton(onClick = onAiReply, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Reply")
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeEmailDialog(
    accounts: List<com.odysseus.wrapper.data.remote.EmailAccountItem>,
    onDismiss: () -> Unit,
    onSend: (accountId: Int, to: String, subject: String, body: String, cc: String) -> Unit
) {
    var to      by remember { mutableStateOf("") }
    var cc      by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body    by remember { mutableStateOf("") }
    val accountId = accounts.firstOrNull()?.id ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compose Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = to, onValueChange = { to = it },
                    label = { Text("To *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cc, onValueChange = { cc = it },
                    label = { Text("CC") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it },
                    label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it },
                    label = { Text("Body") }, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSend(accountId, to, subject, body, cc) },
                enabled = to.isNotBlank()) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
