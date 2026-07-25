package com.odysseus.wrapper.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.data.remote.NoteItem
import com.odysseus.wrapper.ui.components.EmptyBox
import com.odysseus.wrapper.ui.components.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(vm: NotesViewModel = viewModel()) {
    val notes   by vm.notes.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error   by vm.error.collectAsStateWithLifecycle()
    val filter  by vm.filter.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<NoteItem?>(null) }

    if (showCreate) {
        NoteEditorDialog(
            onDismiss = { showCreate = false },
            onSave = { title, content, type, color, pinned ->
                vm.create(title, content, type, color, pinned)
                showCreate = false
            }
        )
    }

    editNote?.let { note ->
        NoteEditorDialog(
            initial = note,
            onDismiss = { editNote = null },
            onSave = { title, content, _, color, pinned ->
                vm.update(note.id, title, content, color, pinned)
                editNote = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Filter chips
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all", "note", "todo", "reminder").forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick  = { vm.setFilter(f) },
                        label    = { Text(f.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Text(it, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            when {
                loading          -> LoadingBox()
                notes.isEmpty()  -> EmptyBox("No notes yet. Tap + to create one.")
                else -> LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note,
                            onTap    = { editNote = note },
                            onPin    = { vm.pin(note.id) },
                            onDelete = { vm.delete(note.id) },
                            onArchive = { vm.archive(note.id) },
                            onToggleItem = { idx -> vm.toggleItem(note.id, idx) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: NoteItem,
    onTap: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onToggleItem: (Int) -> Unit
) {
    val bgColor = when (note.color) {
        "red"    -> Color(0x22E06C75)
        "yellow" -> Color(0x22E5C07B)
        "green"  -> Color(0x2298C379)
        "blue"   -> Color(0x2261AFEF)
        "purple" -> Color(0x22C678DD)
        else     -> MaterialTheme.colorScheme.surface
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.pinned) Icon(Icons.Default.PushPin, null,
                    Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                if (note.note_type == "todo")
                    Icon(Icons.Default.CheckBox, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                if (note.title.isNotBlank()) {
                    Text(note.title, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Box {
                    IconButton(onClick = { showMenu = true }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(if (note.pinned) "Unpin" else "Pin") },
                            onClick = { onPin(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) })
                        DropdownMenuItem(text = { Text("Archive") },
                            onClick = { onArchive(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Archive, null) })
                        DropdownMenuItem(text = { Text("Delete") },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }

            // Todo items
            if (note.note_type == "todo" && !note.items.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                note.items.take(5).forEachIndexed { idx, item ->
                    Row(Modifier.fillMaxWidth().clickable { onToggleItem(idx) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.done, onCheckedChange = { onToggleItem(idx) },
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(item.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (item.done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (note.items.size > 5)
                    Text("+${note.items.size - 5} more", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            } else if (!note.content.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.content, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f), lineHeight = 18.sp)
            }

            note.due_date?.let { due ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Spacer(Modifier.width(4.dp))
                    Text(due, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    initial: NoteItem? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, type: String, color: String?, pinned: Boolean) -> Unit
) {
    var title   by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var type    by remember { mutableStateOf(initial?.note_type ?: "note") }
    var color   by remember { mutableStateOf(initial?.color) }
    var pinned  by remember { mutableStateOf(initial?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Note" else "Edit Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it },
                    label = { Text("Content") }, modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6)
                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("note", "todo", "reminder").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) })
                    }
                }
                // Color chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null, "red", "yellow", "green", "blue", "purple").forEach { c ->
                        val bg = when (c) {
                            "red" -> Color(0xFFE06C75); "yellow" -> Color(0xFFE5C07B)
                            "green" -> Color(0xFF98C379); "blue" -> Color(0xFF61AFEF)
                            "purple" -> Color(0xFFC678DD); else -> MaterialTheme.colorScheme.outline
                        }
                        Box(Modifier.size(24.dp).background(bg, RoundedCornerShape(50))
                            .clickable { color = c }
                            .then(if (color == c) Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(50)) else Modifier))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pinned")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, content, type, color, pinned) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
