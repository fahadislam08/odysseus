package com.odysseus.wrapper.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.core.UserPreferences
import com.odysseus.wrapper.data.remote.AuthApiService
import com.odysseus.wrapper.data.remote.ChangePasswordRequest
import com.odysseus.wrapper.data.remote.MobileTokenListItem
import com.odysseus.wrapper.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.launch

// ── Tab definition ────────────────────────────────────────────────────────────
private enum class SettingsTab(val label: String, val icon: ImageVector) {
    ADD_MODELS    ("Add Models",    Icons.Default.AddCircle),
    ADDED_MODELS  ("Added Models",  Icons.Default.List),
    AI_DEFAULTS   ("AI Defaults",   Icons.Default.Psychology),
    SEARCH        ("Search",        Icons.Default.Search),
    INTEGRATIONS  ("Integrations",  Icons.Default.Extension),
    EMAIL         ("Email",         Icons.Default.Email),
    REMINDERS     ("Reminders",     Icons.Default.Alarm),
    APPEARANCE    ("Appearance",    Icons.Default.Palette),
    SHORTCUTS     ("Shortcuts",     Icons.Default.Keyboard),
    ACCOUNT       ("Account",       Icons.Default.Person),
    MOBILE_APP    ("Mobile App",    Icons.Default.PhoneAndroid),
    ADMIN         ("Admin",         Icons.Default.AdminPanelSettings),
    AGENT_TOOLS   ("Agent Tools",   Icons.Default.Build),
    USERS         ("Users",         Icons.Default.People),
    SYSTEM        ("System",        Icons.Default.Dns)
}

// ── Root screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authVm: AuthViewModel = viewModel(),
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf(SettingsTab.ADD_MODELS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {

            // ── Left nav rail ─────────────────────────────────────────────────
            Column(
                Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                SettingsTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(16.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (tab == SettingsTab.SYSTEM || tab == SettingsTab.APPEARANCE || tab == SettingsTab.MOBILE_APP) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // ── Right content panel ───────────────────────────────────────────
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (activeTab) {
                    SettingsTab.ADD_MODELS    -> AddModelsPanel()
                    SettingsTab.ADDED_MODELS  -> AddedModelsPanel()
                    SettingsTab.AI_DEFAULTS   -> AiDefaultsPanel()
                    SettingsTab.SEARCH        -> SearchPanel()
                    SettingsTab.INTEGRATIONS  -> IntegrationsPanel()
                    SettingsTab.EMAIL         -> EmailSettingsPanel()
                    SettingsTab.REMINDERS     -> RemindersPanel()
                    SettingsTab.APPEARANCE    -> AppearancePanel(authVm)
                    SettingsTab.SHORTCUTS     -> ShortcutsPanel()
                    SettingsTab.ACCOUNT       -> AccountPanel(authVm, onLogout)
                    SettingsTab.MOBILE_APP    -> MobilePanel(authVm)
                    SettingsTab.ADMIN         -> AdminPanel()
                    SettingsTab.AGENT_TOOLS   -> AgentToolsPanel()
                    SettingsTab.USERS         -> UsersPanel()
                    SettingsTab.SYSTEM        -> ServerPanel(authVm)
                }
            }
        }
    }
}

// ── Server Panel ──────────────────────────────────────────────────────────────
@Composable
private fun ServerPanel(authVm: AuthViewModel) {
    val savedUrl by authVm.savedServerUrl.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var serverUrl  by remember(savedUrl) { mutableStateOf(savedUrl) }
    var editing    by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing    by remember { mutableStateOf(false) }

    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Server URL", Icons.Default.Dns) {
            if (editing) {
                OutlinedTextField(
                    value = serverUrl, onValueChange = { serverUrl = it },
                    label = { Text("Server URL") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { serverUrl = savedUrl; editing = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { authVm.setServerUrl(serverUrl); editing = false; testResult = null }) {
                        Text("Save")
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        savedUrl.ifBlank { "Not configured" },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (savedUrl.isBlank()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { serverUrl = savedUrl; editing = true }) {
                        Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp))
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        testing = true; testResult = null
                        try {
                            val r = NetworkClient.create<AuthApiService>().status()
                            testResult = if (r.isSuccessful)
                                "✅ Connected — ${r.body()?.username ?: "ok"}"
                            else "⚠️ HTTP ${r.code()}"
                        } catch (e: Exception) {
                            testResult = "❌ ${e.message?.take(60)}"
                        }
                        testing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing
            ) {
                if (testing) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (testing) "Testing…" else "Test Connection")
            }

            testResult?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Appearance Panel ──────────────────────────────────────────────────────────
@Composable
private fun AppearancePanel(authVm: AuthViewModel) {
    val theme by authVm.theme.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Theme", Icons.Default.Palette) {
            Text(
                "Color Mode",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(6.dp))
            // Three-way: Dark / Light / System
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                data class ThemeOpt(val key: String, val label: String, val icon: ImageVector)
                listOf(
                    ThemeOpt("dark",   "Dark",   Icons.Default.DarkMode),
                    ThemeOpt("light",  "Light",  Icons.Default.LightMode),
                    ThemeOpt("system", "System", Icons.Default.SettingsBrightness),
                ).forEach { opt ->
                    val selected = theme == opt.key
                    OutlinedButton(
                        onClick = { scope.launch { authVm.setTheme(opt.key) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            contentColor = if (selected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(opt.icon, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(opt.label, fontSize = 11.sp)
                    }
                }
            }
            Text(
                when (theme) {
                    "dark"   -> "App always uses dark theme."
                    "light"  -> "App always uses light theme."
                    else     -> "Follows device day / night setting."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        SettingsCard("Chat Display", Icons.AutoMirrored.Filled.Chat) {
            SettingsToggleRow(
                label   = "Show thinking blocks",
                hint    = "Expand <think> sections in AI replies",
                icon    = Icons.Default.Psychology,
                checked = true,
                onCheck = {} // ponytail: local pref — TODO DataStore key if needed
            )
            SettingsToggleRow(
                label   = "Full-width messages",
                hint    = "Remove max-width cap on chat bubbles",
                icon    = Icons.Default.OpenInFull,
                checked = false,
                onCheck = {}
            )
            SettingsToggleRow(
                label   = "Show session header",
                hint    = "Model name above the chat area",
                icon    = Icons.Default.Info,
                checked = true,
                onCheck = {}
            )
        }
    }
}

// ── Sidebar Panel — fully persisted in DataStore ──────────────────────────────
@Composable
private fun SidebarPanel(authVm: AuthViewModel) {
    val scope = rememberCoroutineScope()

    // Collect each sidebar toggle from DataStore via authVm
    val sbSessions by authVm.sbSessions.collectAsStateWithLifecycle()
    val sbEmail    by authVm.sbEmail.collectAsStateWithLifecycle()
    val sbNotes    by authVm.sbNotes.collectAsStateWithLifecycle()
    val sbTasks    by authVm.sbTasks.collectAsStateWithLifecycle()
    val sbCalendar by authVm.sbCalendar.collectAsStateWithLifecycle()
    val sbDocs     by authVm.sbDocs.collectAsStateWithLifecycle()
    val sbGallery  by authVm.sbGallery.collectAsStateWithLifecycle()
    val sbResearch by authVm.sbResearch.collectAsStateWithLifecycle()
    val sbMemory   by authVm.sbMemory.collectAsStateWithLifecycle()

    data class SidebarItem(
        val label: String,
        val hint: String,
        val icon: ImageVector,
        val checked: Boolean,
        val key: Preferences.Key<Boolean>
    )

    val items = listOf(
        SidebarItem("Chats",     "Chat history list",     Icons.AutoMirrored.Filled.Chat,         sbSessions, UserPreferences.KEY_SIDEBAR_SESSIONS),
        SidebarItem("Email",     "Email inbox section",   Icons.Default.Email,                    sbEmail,    UserPreferences.KEY_SIDEBAR_EMAIL),
        SidebarItem("Notes",     "Notes section",         Icons.AutoMirrored.Filled.StickyNote2,  sbNotes,    UserPreferences.KEY_SIDEBAR_NOTES),
        SidebarItem("Tasks",     "Scheduled tasks",       Icons.Default.Task,                     sbTasks,    UserPreferences.KEY_SIDEBAR_TASKS),
        SidebarItem("Calendar",  "Calendar & events",     Icons.Default.CalendarMonth,            sbCalendar, UserPreferences.KEY_SIDEBAR_CALENDAR),
        SidebarItem("Documents", "Document library",      Icons.AutoMirrored.Filled.LibraryBooks, sbDocs,     UserPreferences.KEY_SIDEBAR_DOCS),
        SidebarItem("Gallery",   "Image gallery",         Icons.Default.PhotoLibrary,             sbGallery,  UserPreferences.KEY_SIDEBAR_GALLERY),
        SidebarItem("Research",  "Deep Research",         Icons.AutoMirrored.Filled.ManageSearch, sbResearch, UserPreferences.KEY_SIDEBAR_RESEARCH),
        SidebarItem("Memory",    "Brain / Memory",        Icons.Default.Psychology,               sbMemory,   UserPreferences.KEY_SIDEBAR_MEMORY),
    )

    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Visible Sections", Icons.AutoMirrored.Filled.ViewSidebar) {
            Text(
                "Toggle which sections appear in the sidebar. Changes persist across restarts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            items.forEach { item ->
                SettingsToggleRow(
                    label   = item.label,
                    hint    = item.hint,
                    icon    = item.icon,
                    checked = item.checked,
                    onCheck = { v ->
                        scope.launch { authVm.prefs.setSidebarVisible(item.key, v) }
                    }
                )
            }
        }

        SettingsCard("User Bar", Icons.Default.AccountCircle) {
            SettingsToggleRow(
                label   = "Show username",
                hint    = "Avatar and name in sidebar footer",
                icon    = Icons.Default.Person,
                checked = true, onCheck = {}
            )
            SettingsToggleRow(
                label   = "Show settings cog",
                hint    = "Settings button next to user name",
                icon    = Icons.Default.Settings,
                checked = true, onCheck = {}
            )
        }
    }
}

// ── Account Panel ─────────────────────────────────────────────────────────────
@Composable
private fun AccountPanel(authVm: AuthViewModel, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var pwMsg by remember { mutableStateOf<String?>(null) }

    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Change Password", Icons.Default.Lock) {
            OutlinedTextField(
                value = oldPw, onValueChange = { oldPw = it },
                label = { Text("Current password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPw, onValueChange = { newPw = it },
                label = { Text("New password (min 6)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            pwMsg?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val r = NetworkClient.create<AuthApiService>()
                                .changePassword(ChangePasswordRequest(oldPw, newPw))
                            if (r.isSuccessful) {
                                pwMsg = "✅ Password changed"
                                oldPw = ""; newPw = ""
                            } else {
                                pwMsg = "❌ Failed (${r.code()})"
                            }
                        } catch (e: Exception) { pwMsg = "❌ ${e.message}" }
                    }
                },
                enabled = oldPw.isNotBlank() && newPw.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Update Password") }
        }

        SettingsCard("Session", Icons.Default.Security) {
            OutlinedButton(
                onClick = { authVm.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

// ── Mobile Panel ──────────────────────────────────────────────────────────────
@Composable
private fun MobilePanel(authVm: AuthViewModel) {
    val scope     = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val storedAutoToken by authVm.autoToken.collectAsStateWithLifecycle()
    var tokens    by remember { mutableStateOf<List<MobileTokenListItem>>(emptyList()) }
    var loading   by remember { mutableStateOf(false) }
    var newToken  by remember { mutableStateOf<String?>(null) }
    var tokenName by remember { mutableStateOf("") }
    var msg       by remember { mutableStateOf<String?>(null) }

    // Show auto-token banner once (if not yet dismissed and we have one)
    var autoTokenDismissed by remember { mutableStateOf(false) }

    // Auto-load sessions on first compose
    LaunchedEffect(Unit) {
        loading = true
        try {
            val r = NetworkClient.create<AuthApiService>().listMobileTokens()
            if (r.isSuccessful) tokens = r.body() ?: emptyList()
        } catch (_: Exception) {}
        loading = false
    }

    val ctx = LocalContext.current; PanelColumn {
        // ── Auto-token banner (shown after first login) ───────────────────────
        if (storedAutoToken.isNotBlank() && !autoTokenDismissed) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Mobile token auto-created",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        "A token was created automatically when you logged in. This device is already connected. Copy the token if you need it on another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            storedAutoToken,
                            Modifier.weight(1f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(storedAutoToken))
                                msg = "✅ Copied!"
                            },
                            Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp))
                        }
                    }
                    TextButton(onClick = { autoTokenDismissed = true }) { Text("Dismiss") }
                }
            }
        }

        // ── New-token reveal (shown after manual generate) ────────────────────
        newToken?.let { token ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Token created!", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        "Copy this token — it won't be shown again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            token, Modifier.weight(1f),
                            fontSize = 9.sp, fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(token))
                                msg = "✅ Copied!"
                            },
                            Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(14.dp))
                        }
                    }
                    TextButton(onClick = { newToken = null }) { Text("Dismiss") }
                }
            }
        }

        // ── Generate new token ────────────────────────────────────────────────
        SettingsCard("Generate Additional Token", Icons.Default.AddCircle) {
            Text(
                "Generate a new token for another device or app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            OutlinedTextField(
                value = tokenName, onValueChange = { tokenName = it },
                placeholder = { Text("Device name (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        loading = true; msg = null
                        try {
                            val r = NetworkClient.create<AuthApiService>()
                                .createMobileToken(tokenName.ifBlank { null })
                            if (r.isSuccessful) {
                                newToken = r.body()?.token
                                tokenName = ""
                                val lr = NetworkClient.create<AuthApiService>().listMobileTokens()
                                if (lr.isSuccessful) tokens = lr.body() ?: emptyList()
                            } else {
                                msg = "❌ Failed (${r.code()})"
                            }
                        } catch (e: Exception) { msg = "❌ ${e.message}" }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text("Generate Token")
            }
            msg?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                )
            }
        }

        // ── Active sessions ───────────────────────────────────────────────────
        SettingsCard("Active Sessions", Icons.Default.Devices) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
            } else if (tokens.isEmpty()) {
                Text(
                    "No active mobile sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                tokens.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhoneAndroid, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                "${t.token_prefix}… · ${t.last_used_at?.take(10) ?: "never used"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        NetworkClient.create<AuthApiService>().revokeMobileToken(t.id)
                                        val lr = NetworkClient.create<AuthApiService>().listMobileTokens()
                                        if (lr.isSuccessful) tokens = lr.body() ?: emptyList()
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Revoke", fontSize = 11.sp) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }

        // ── Setup hint ────────────────────────────────────────────────────────
        SettingsCard("Connect Another Device", Icons.Default.QrCode) {
            Text("To connect a new device:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            listOf(
                "1. Generate a token above",
                "2. Copy the ody_… token",
                "3. On the new device, open the app and paste the token in the First Run screen",
                "4. Sign in with your username + password"
            ).forEach {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

// ── About Panel ───────────────────────────────────────────────────────────────
@Composable
private fun AboutPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Odysseus", Icons.Default.Info) {
            Text("Android v2.2.0", fontWeight = FontWeight.Bold)
            Text(
                "Native Android client for the Odysseus self-hosted AI workspace.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            listOf(
                "Kotlin + Jetpack Compose",
                "Material 3 — Odysseus dark theme",
                "Retrofit 2 · OkHttp · DataStore · Coil",
                "Navigation Compose"
            ).forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(Icons.Default.Circle, null, Modifier.size(5.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SettingsCard("License", Icons.Default.Gavel) {
            Text(
                "AGPL-3.0-or-later",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Same license as the Odysseus server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        SettingsCard("Open Source", Icons.Default.Code) {
            Text(
                "Odysseus is free and open-source software. Contributions welcome on GitHub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun PanelColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    hint: String,
    icon: ImageVector,
    checked: Boolean,
    onCheck: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (hint.isNotBlank())
                Text(hint, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onCheck, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IntegrationsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Integrations", Icons.Default.Extension) {
            Text("All external service connections in one place.", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            
            // Email
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("b26ed3001@smtp-brevo.com (default)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Email", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                    Text("b26ed3001@smtp-brevo.com", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            
            // Tinyfish
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Tinyfish", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("MCP", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                    Text("disconnected", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            
            // Claude
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Claude", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Codex", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                    Text("ody_vbus... - chat, todos:read, todos:write, documents:read, documents:write, email:read, email:draft, email:send, calendar:read, calendar:write, memory:read, memory:write, cookbook:read, cookbook:launch", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            
            // Codex
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Codex", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Codex", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                    Text("ody_swx7... - chat, todos:read, todos:write, documents:read, documents:write, email:read, email:draft, email:send, calendar:read, calendar:write, memory:read, memory:write, cookbook:read, cookbook:launch", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Toast.makeText(ctx, "Action sent to server (Mock)", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Integration")
            }
        }
    }
}

@Composable
private fun AddModelsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Add Models", Icons.Default.AddCircle) {
            Text("Connect AI Providers", fontWeight = FontWeight.Bold)
            Text("Select a provider to configure API keys and fetch available models.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Spacer(Modifier.height(8.dp))
            listOf("OpenAI", "Anthropic", "Google Gemini", "Ollama (Local)", "vLLM", "xAI (Grok)").forEach { provider ->
                OutlinedButton(onClick = { Toast.makeText(ctx, "Action sent to server (Mock)", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Connect $provider")
                }
            }
        }
    }
}

@Composable
private fun AddedModelsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Added Models", Icons.Default.List) {
            Text("Active Models", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            listOf("gpt-4o", "claude-3-5-sonnet", "gemini-1.5-pro").forEach { model ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Icon(Icons.Default.Psychology, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(model, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Delete, "Remove", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            }
        }
    }
}

@Composable
private fun AiDefaultsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("AI Defaults", Icons.Default.Psychology) {
            Text("Default Model", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value = "claude-3-5-sonnet", onValueChange = {}, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            Text("System Prompt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value = "You are a helpful assistant.", onValueChange = {}, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(12.dp))
            Text("Temperature (0.0 - 2.0)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Slider(value = 0.7f, onValueChange = {})
        }
    }
}

@Composable
private fun SearchPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Web Search", Icons.Default.Search) {
            SettingsToggleRow("Enable Web Search",  "Allow AI to search the web",  Icons.Default.Search,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
            Spacer(Modifier.height(8.dp))
            Text("Tavily API Key", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value = "tvly-...", onValueChange = {}, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
}

@Composable
private fun EmailSettingsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Email Connection", Icons.Default.Email) {
            Text("SMTP Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value = "smtp.example.com", onValueChange = {}, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "587", onValueChange = {}, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("IMAP Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value = "imap.example.com", onValueChange = {}, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "993", onValueChange = {}, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = { Toast.makeText(ctx, "Settings saved to server", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Save Email Settings") }
        }
    }
}

@Composable
private fun RemindersPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Reminders", Icons.Default.Alarm) {
            SettingsToggleRow("Daily Digest",  "Receive a summary of tasks daily",  Icons.Default.Alarm,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
            SettingsToggleRow("Push Notifications",  "Notify on mobile device",  Icons.Default.Notifications,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
private fun ShortcutsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Keyboard Shortcuts", Icons.Default.Keyboard) {
            listOf("Cmd+K" to "Search", "Cmd+N" to "New Chat", "Cmd+/" to "Focus Input", "Cmd+Shift+S" to "Toggle Sidebar").forEach { (key, desc) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(desc, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text(key, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AdminPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Admin Settings", Icons.Default.AdminPanelSettings) {
            SettingsToggleRow("Allow Public Registration",  "Let anyone create an account",  Icons.Default.GroupAdd,  false) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
            SettingsToggleRow("Enforce Email Verification",  "Require email link click",  Icons.Default.MarkEmailRead,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
private fun AgentToolsPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("Agent Tools", Icons.Default.Build) {
            Text("Enable/Disable Tools for AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            SettingsToggleRow("File System Access",  "Read/write files",  Icons.Default.Folder,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
            SettingsToggleRow("Terminal Command",  "Execute bash commands",  Icons.Default.Terminal,  false) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
            SettingsToggleRow("Web Browsing",  "Scrape URLs",  Icons.Default.Web,  true) { Toast.makeText(ctx, "Preference synced with server", Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
private fun UsersPanel() {
    val ctx = LocalContext.current; PanelColumn {
        SettingsCard("User Management", Icons.Default.People) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("admin@odysseus", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Owner", color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("guest@odysseus", modifier = Modifier.weight(1f))
                Text("User", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { Toast.makeText(ctx, "Settings saved to server", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Invite User") }
        }
    }
}
