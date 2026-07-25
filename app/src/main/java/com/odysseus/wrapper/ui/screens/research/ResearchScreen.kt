package com.odysseus.wrapper.ui.screens.research

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.odysseus.wrapper.data.remote.ResearchSession
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchScreen(vm: ResearchViewModel = viewModel()) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val active   by vm.active.collectAsStateWithLifecycle()
    val status   by vm.status.collectAsStateWithLifecycle()
    val report   by vm.report.collectAsStateWithLifecycle()
    val loading  by vm.loading.collectAsStateWithLifecycle()
    val starting by vm.starting.collectAsStateWithLifecycle()
    val error    by vm.error.collectAsStateWithLifecycle()

    // Show active session / report view
    if (active != null) {
        ResearchDetailScreen(
            session  = active!!,
            status   = status,
            report   = report,
            onBack   = { vm.close() },
            onCancel = { vm.cancel(active!!.id) },
            onDelete = { vm.delete(active!!.id) },
            onArchive = { vm.archive(active!!.id) }
        )
        return
    }

    var query      by remember { mutableStateOf("") }
    var maxSources by remember { mutableIntStateOf(10) }
    var showStart  by remember { mutableStateOf(false) }

    if (showStart) {
        AlertDialog(
            onDismissRequest = { showStart = false },
            title = { Text("Deep Research") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = query, onValueChange = { query = it },
                        label = { Text("Research query *") },
                        modifier = Modifier.fillMaxWidth().height(100.dp))
                    Text("Max sources: $maxSources")
                    Slider(value = maxSources.toFloat(), onValueChange = { maxSources = it.toInt() },
                        valueRange = 5f..30f, steps = 4)
                }
            },
            confirmButton = {
                Button(onClick = { vm.start(query, maxSources); showStart = false },
                    enabled = query.isNotBlank() && !starting) {
                    if (starting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Start")
                }
            },
            dismissButton = { TextButton(onClick = { showStart = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Research", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { vm.loadLibrary() }) { Icon(Icons.Default.Refresh, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showStart = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Search, "New research")
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
                loading          -> LoadingBox()
                sessions.isEmpty() -> EmptyBox("No research sessions yet.\nTap 🔍 to start one.")
                else -> LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        ResearchSessionCard(session,
                            onTap    = { vm.openSession(session) },
                            onDelete = { vm.delete(session.id) },
                            onArchive = { vm.archive(session.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ResearchSessionCard(session: ResearchSession, onTap: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val statusColor = when (session.status) {
        "done"      -> MaterialTheme.colorScheme.secondary
        "running"   -> MaterialTheme.colorScheme.primary
        "error"     -> MaterialTheme.colorScheme.error
        "cancelled" -> MaterialTheme.colorScheme.outline
        else        -> MaterialTheme.colorScheme.outline
    }
    Card(Modifier.fillMaxWidth().clickable { onTap() },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.query, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(session.status, statusColor)
                    if (session.source_count > 0)
                        Text("${session.source_count} sources", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date((session.started_at * 1000).toLong())), fontSize = 11.sp,
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
fun ResearchDetailScreen(
    session: ResearchSession,
    status: com.odysseus.wrapper.data.remote.ResearchStatus?,
    report: com.odysseus.wrapper.data.remote.ResearchReport?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.query, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (session.status == "running")
                        IconButton(onClick = onCancel) { Icon(Icons.Default.Stop, "Cancel", tint = MaterialTheme.colorScheme.error) }
                    IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, null) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Status / progress bar
            if (session.status == "running" || status?.status == "running") {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(status?.message ?: "Researching…", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (status?.progress ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${status?.progress ?: 0}% · ${status?.sources_found ?: 0} sources found",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (report != null) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(report.report, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    if (report.sources.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Sources (${report.sources.size})", fontWeight = FontWeight.Bold)
                        report.sources.forEachIndexed { i, src ->
                            Spacer(Modifier.height(6.dp))
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("${i + 1}. ${src.title.ifBlank { src.url }}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(src.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    if (src.snippet.isNotBlank())
                                        Text(src.snippet, fontSize = 12.sp, maxLines = 3,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                }
                            }
                        }
                    }
                }
            } else if (session.status != "running") {
                EmptyBox("No report available yet.")
            }
        }
    }
}
