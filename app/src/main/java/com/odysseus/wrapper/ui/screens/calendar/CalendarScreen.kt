package com.odysseus.wrapper.ui.screens.calendar

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
    val events     by vm.events.collectAsStateWithLifecycle()
    val loading    by vm.loading.collectAsStateWithLifecycle()
    val error      by vm.error.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()

    var showCreate  by remember { mutableStateOf(false) }
    var editEvent   by remember { mutableStateOf<CalendarEvent?>(null) }

    if (showCreate) {
        EventEditorDialog(onDismiss = { showCreate = false },
            onSave = { req -> vm.createEvent(req); showCreate = false })
    }
    editEvent?.let { ev ->
        EventEditorDialog(initial = ev, onDismiss = { editEvent = null },
            onSave = { req ->
                vm.updateEvent(ev.uid, EventUpdateRequest(
                    summary = req.summary, dtstart = req.dtstart, dtend = req.dtend,
                    all_day = req.all_day, description = req.description,
                    location = req.location, rrule = req.rrule, color = req.color))
                editEvent = null
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                actions = {
                    syncStatus?.let {
                        Text(it, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { vm.sync() }) { Icon(Icons.Default.Sync, "Sync CalDAV") }
                    IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Refresh") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "New event")
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
                events.isEmpty() -> EmptyBox("No events. Tap + to add one.")
                else -> {
                    // Group by date prefix
                    val grouped = events.groupBy { it.dtstart.take(10) }
                    LazyColumn(Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        grouped.forEach { (date, dayEvents) ->
                            item(key = "header_$date") {
                                Text(date, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            }
                            items(dayEvents, key = { it.uid }) { event ->
                                EventCard(event,
                                    onTap    = { editEvent = event },
                                    onDelete = { vm.deleteEvent(event.uid) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: CalendarEvent, onTap: () -> Unit, onDelete: () -> Unit) {
    val color = when (event.color) {
        "red"    -> Color(0xFFE06C75)
        "green"  -> Color(0xFF98C379)
        "blue"   -> Color(0xFF61AFEF)
        "yellow" -> Color(0xFFE5C07B)
        else     -> MaterialTheme.colorScheme.primary
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().clickable { onTap() },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(40.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(event.summary.ifBlank { "No title" }, fontWeight = FontWeight.SemiBold)
                val timeStr = if (event.all_day) "All day"
                    else "${event.dtstart.take(16).replace("T", " ")} – ${event.dtend?.take(16)?.replace("T", " ") ?: ""}"
                Text(timeStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                if (event.location.isNotBlank())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text(event.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                event.rrule?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("Recurring", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") },
                        onClick = { onTap(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@Composable
fun EventEditorDialog(
    initial: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (EventCreateRequest) -> Unit
) {
    var summary     by remember { mutableStateOf(initial?.summary ?: "") }
    var dtstart     by remember { mutableStateOf(initial?.dtstart?.take(16) ?: "") }
    var dtend       by remember { mutableStateOf(initial?.dtend?.take(16) ?: "") }
    var allDay      by remember { mutableStateOf(initial?.all_day ?: false) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location    by remember { mutableStateOf(initial?.location ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Event" else "Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = summary, onValueChange = { summary = it },
                    label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("All day")
                }
                OutlinedTextField(value = dtstart, onValueChange = { dtstart = it },
                    label = { Text("Start (YYYY-MM-DDTHH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!allDay)
                    OutlinedTextField(value = dtend, onValueChange = { dtend = it },
                        label = { Text("End (YYYY-MM-DDTHH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(80.dp))
                OutlinedTextField(value = location, onValueChange = { location = it },
                    label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(EventCreateRequest(
                        summary     = summary,
                        dtstart     = if (dtstart.length == 16) "${dtstart}:00" else dtstart,
                        dtend       = dtend.ifBlank { null }?.let { if (it.length == 16) "${it}:00" else it },
                        all_day     = allDay,
                        description = description,
                        location    = location
                    ))
                },
                enabled = summary.isNotBlank() && dtstart.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
