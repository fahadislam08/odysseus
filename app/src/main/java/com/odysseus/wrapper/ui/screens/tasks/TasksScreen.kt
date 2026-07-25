package com.odysseus.wrapper.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val tasks     by vm.tasks.collectAsStateWithLifecycle()
    val runs      by vm.runs.collectAsStateWithLifecycle()
    val loading   by vm.loading.collectAsStateWithLifecycle()
    val error     by vm.error.collectAsStateWithLifecycle()
    val actionMsg by vm.actionMsg.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (showCreate) {
        TaskCreateDialog(onDismiss = { showCreate = false }, onSave = { vm.create(it); showCreate = false })
    }

    actionMsg?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2000)
            vm.clearMsg()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Scheduled") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Recent Runs") })
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(it, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            when (selectedTab) {
                0 -> when {
                    loading        -> LoadingBox()
                    tasks.isEmpty() -> EmptyBox("No tasks yet. Tap + to schedule one.")
                    else -> LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(task,
                                onRun    = { vm.run(task.id) },
                                onPause  = { vm.pause(task.id) },
                                onResume = { vm.resume(task.id) },
                                onDelete = { vm.delete(task.id) },
                                onViewRuns = { vm.loadRuns(task.id) }
                            )
                        }
                    }
                }
                1 -> when {
                    runs.isEmpty() -> EmptyBox("No recent runs.")
                    else -> LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(runs, key = { it.id }) { run -> RunCard(run) }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onRun: () -> Unit, onPause: () -> Unit, onResume: () -> Unit,
    onDelete: () -> Unit, onViewRuns: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(task.name.ifBlank { "Unnamed Task" }, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val schedText = when (task.schedule) {
                        "daily"   -> "Daily ${task.scheduled_time}"
                        "weekly"  -> "Weekly ${task.scheduled_time}"
                        "monthly" -> "Monthly ${task.scheduled_time}"
                        "once"    -> "Once ${task.scheduled_date ?: task.scheduled_time}"
                        "cron"    -> task.cron_expression ?: "Cron"
                        else      -> task.trigger_type
                    }
                    StatusChip(schedText,
                        if (task.status == "paused") MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary)
                    StatusChip(task.task_type, MaterialTheme.colorScheme.secondary)
                }
                task.last_run?.let {
                    Text("Last: $it", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
            IconButton(onClick = onRun) { Icon(Icons.Default.PlayArrow, "Run", tint = MaterialTheme.colorScheme.primary) }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(if (task.status == "paused") "Resume" else "Pause") },
                        onClick = { if (task.status == "paused") onResume() else onPause(); showMenu = false },
                        leadingIcon = { Icon(if (task.status == "paused") Icons.Default.PlayArrow else Icons.Default.Pause, null) })
                    DropdownMenuItem(text = { Text("View Runs") },
                        onClick = { onViewRuns(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.History, null) })
                    DropdownMenuItem(text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@Composable
fun RunCard(run: TaskRun) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = when (run.status) {
                    "success" -> MaterialTheme.colorScheme.secondary
                    "error"   -> MaterialTheme.colorScheme.error
                    "running" -> MaterialTheme.colorScheme.primary
                    else      -> MaterialTheme.colorScheme.outline
                }
                StatusChip(run.status, color)
                Spacer(Modifier.width(8.dp))
                Text(run.started_at, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            run.result?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
            }
            run.error?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, maxLines = 2, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun TaskCreateDialog(onDismiss: () -> Unit, onSave: (TaskCreateRequest) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var prompt   by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("daily") }
    var time     by remember { mutableStateOf("09:00") }
    var taskType by remember { mutableStateOf("llm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Scheduled Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = prompt, onValueChange = { prompt = it },
                    label = { Text("Prompt / Instruction") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                // Schedule type
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("once", "daily", "weekly", "monthly").forEach { s ->
                        FilterChip(selected = schedule == s, onClick = { schedule = s },
                            label = { Text(s.replaceFirstChar { it.uppercase() }) })
                    }
                }
                OutlinedTextField(value = time, onValueChange = { time = it },
                    label = { Text("Time (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("llm", "action", "research").forEach { t ->
                        FilterChip(selected = taskType == t, onClick = { taskType = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(TaskCreateRequest(name = name, prompt = prompt.ifBlank { null },
                    task_type = taskType, schedule = schedule, scheduled_time = time))
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
