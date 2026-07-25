package com.odysseus.wrapper.ui.screens.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.odysseus.wrapper.data.remote.DocumentItem
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(vm: DocumentsViewModel = viewModel()) {
    val docs    by vm.docs.collectAsStateWithLifecycle()
    val current by vm.current.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val saving  by vm.saving.collectAsStateWithLifecycle()
    val error   by vm.error.collectAsStateWithLifecycle()

    // Show editor if a doc is open
    if (current != null) {
        DocumentEditor(
            doc     = current!!,
            saving  = saving,
            onBack  = { vm.close() },
            onSave  = { t, c -> vm.save(current!!.id, t, c) },
            onDelete = { vm.delete(current!!.id) },
            onArchive = { vm.archive(current!!.id); vm.close() }
        )
        return
    }

    var showCreate  by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch  by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateDocDialog(
            onDismiss = { showCreate = false },
            onCreate  = { title, type -> vm.create(title, "", type); showCreate = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(value = searchQuery, onValueChange = {
                            searchQuery = it; vm.load(it.ifBlank { null })
                        }, placeholder = { Text("Search docs…") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                    } else { Text("Documents", fontWeight = FontWeight.Bold) }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) { searchQuery = ""; vm.load() } }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null)
                    }
                    IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "New document")
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
                loading        -> LoadingBox()
                docs.isEmpty() -> EmptyBox("No documents. Tap + to create one.")
                else -> LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(docs, key = { it.id }) { doc ->
                        DocCard(doc, onTap = { vm.open(doc.id) }, onDelete = { vm.delete(doc.id) },
                            onArchive = { vm.archive(doc.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DocCard(doc: DocumentItem, onTap: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { onTap() },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary.copy(0.7f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(doc.content_type, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text("${doc.word_count} words", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text(doc.updated_at.take(10), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Archive") }, onClick = { onArchive(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Archive, null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditor(
    doc: com.odysseus.wrapper.data.remote.DocumentDetail,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var title   by remember(doc.id) { mutableStateOf(doc.title) }
    var content by remember(doc.id) { mutableStateOf(doc.content) }
    var changed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(value = title, onValueChange = { title = it; changed = true },
                        placeholder = { Text("Document title") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.3f)))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { onSave(title, content); changed = false },
                            enabled = changed) {
                            Icon(Icons.Default.Save, "Save",
                                tint = if (changed) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onBackground.copy(0.3f))
                        }
                    }
                    IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, "Archive") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        OutlinedTextField(
            value = content, onValueChange = { content = it; changed = true },
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            placeholder = { Text("Start writing…", color = MaterialTheme.colorScheme.onBackground.copy(0.3f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.outline.copy(0.3f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.1f)),
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
        )
    }
}

@Composable
fun CreateDocDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type  by remember { mutableStateOf("markdown") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("markdown", "html", "plain").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title, type) }, enabled = title.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
