package com.odysseus.wrapper.ui.screens.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.data.remote.MemoryItem
import com.odysseus.wrapper.data.remote.SkillItem
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(vm: MemoryViewModel = viewModel()) {
    val memories  by vm.memories.collectAsStateWithLifecycle()
    val skills    by vm.skills.collectAsStateWithLifecycle()
    val loading   by vm.loading.collectAsStateWithLifecycle()
    val error     by vm.error.collectAsStateWithLifecycle()
    val tab       by vm.tab.collectAsStateWithLifecycle()
    val catFilter by vm.categoryFilter.collectAsStateWithLifecycle()

    var showAddMemory by remember { mutableStateOf(false) }
    var showAddSkill  by remember { mutableStateOf(false) }

    if (showAddMemory) {
        AddMemoryDialog(
            onDismiss = { showAddMemory = false },
            onSave    = { text, cat -> vm.addMemory(text, cat); showAddMemory = false }
        )
    }
    if (showAddSkill) {
        AddSkillDialog(
            onDismiss = { showAddSkill = false },
            onSave    = { title, problem, solution, tags -> vm.addSkill(title, problem, solution, tags); showAddSkill = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { vm.loadAll() }) { Icon(Icons.Default.Refresh, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (tab == 0) showAddMemory = true else showAddSkill = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { vm.setTab(0) },
                    text = { Text("Memories (${memories.size})") })
                Tab(selected = tab == 1, onClick = { vm.setTab(1) },
                    text = { Text("Skills (${skills.size})") })
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(it, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            when (tab) {
                0 -> {
                    // Category filter row
                    val cats = listOf(null, "fact", "contact", "task", "preference", "identity", "project", "goal")
                    LazyRow(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cats) { cat ->
                            FilterChip(selected = catFilter == cat, onClick = { vm.setCategory(cat) },
                                label = { Text(cat ?: "All") })
                        }
                    }
                    when {
                        loading           -> LoadingBox()
                        memories.isEmpty() -> EmptyBox("No memories. Tap + to add one.")
                        else -> LazyColumn(Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(memories, key = { it.id }) { mem ->
                                MemoryCard(mem,
                                    onPin    = { vm.pinMemory(mem.id) },
                                    onDelete = { vm.deleteMemory(mem.id) })
                            }
                        }
                    }
                }
                1 -> when {
                    loading         -> LoadingBox()
                    skills.isEmpty() -> EmptyBox("No skills. Tap + to add one.")
                    else -> LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(skills, key = { it.id }) { skill ->
                            SkillCard(skill, onDelete = { vm.deleteSkill(skill.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryCard(mem: MemoryItem, onPin: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(mem.text, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(mem.category, MaterialTheme.colorScheme.primary)
                    StatusChip(mem.source, MaterialTheme.colorScheme.secondary)
                    if (mem.pinned) StatusChip("📌 pinned", MaterialTheme.colorScheme.tertiary)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(if (mem.pinned) "Unpin" else "Pin") },
                        onClick = { onPin(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PushPin, null) })
                    DropdownMenuItem(text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: SkillItem, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(skill.title, fontWeight = FontWeight.SemiBold)
                    Text(skill.problem, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 1)
                }
                IconButton(onClick = { expanded = !expanded }, Modifier.size(24.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Text("Problem: ${skill.problem}", fontSize = 13.sp)
                Text("Solution: ${skill.solution}", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (skill.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(skill.tags) { tag -> SuggestionChip(onClick = {}, label = { Text(tag, fontSize = 11.sp) }) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemoryDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var text     by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("fact") }
    val cats = listOf("fact","contact","task","preference","identity","project","goal")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it },
                    label = { Text("Memory text *") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(cats) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat },
                            label = { Text(cat) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, category) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddSkillDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var problem  by remember { mutableStateOf("") }
    var solution by remember { mutableStateOf("") }
    var tags     by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Skill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = problem, onValueChange = { problem = it },
                    label = { Text("When to use") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = solution, onValueChange = { solution = it },
                    label = { Text("How — approach/steps") }, modifier = Modifier.fillMaxWidth().height(80.dp))
                OutlinedTextField(value = tags, onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, problem, solution, tags) }, enabled = title.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
