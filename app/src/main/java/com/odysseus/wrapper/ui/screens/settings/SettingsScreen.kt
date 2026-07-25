package com.odysseus.wrapper.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.data.remote.AuthApiService
import com.odysseus.wrapper.data.remote.ChangePasswordRequest
import com.odysseus.wrapper.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authVm: AuthViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val savedUrl  by authVm.savedServerUrl.collectAsStateWithLifecycle()
    val darkMode  by authVm.darkMode.collectAsStateWithLifecycle()
    val scope     = rememberCoroutineScope()

    var serverUrl     by remember(savedUrl) { mutableStateOf(savedUrl) }
    var editingUrl    by remember { mutableStateOf(false) }
    var testResult    by remember { mutableStateOf<String?>(null) }
    var testing       by remember { mutableStateOf(false) }

    var oldPw         by remember { mutableStateOf("") }
    var newPw         by remember { mutableStateOf("") }
    var pwMsg         by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Server URL ──────────────────────────────────────────────────
            SectionCard(title = "Server") {
                if (editingUrl) {
                    OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it },
                        label = { Text("Server URL") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { serverUrl = savedUrl; editingUrl = false }) { Text("Cancel") }
                        Button(onClick = {
                            authVm.setServerUrl(serverUrl)
                            editingUrl = false
                            testResult = null
                        }) { Text("Save") }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(savedUrl, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { serverUrl = savedUrl; editingUrl = true }) {
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            testing = true; testResult = null
                            try {
                                val r = NetworkClient.create<AuthApiService>().status()
                                testResult = if (r.isSuccessful) "✅ ${r.body()?.username ?: "Connected"}"
                                else "⚠️ HTTP ${r.code()}"
                            } catch (e: Exception) { testResult = "❌ ${e.message}" }
                            testing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !testing
                ) {
                    if (testing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Test Connection")
                }
                testResult?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("✅")) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error)
                }
            }

            // ── Appearance ──────────────────────────────────────────────────
            SectionCard(title = "Appearance") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Dark Mode")
                    }
                    Switch(checked = darkMode, onCheckedChange = {
                        scope.launch { authVm.prefs.setDarkMode(it) }
                    })
                }
            }

            // ── Change Password ─────────────────────────────────────────────
            SectionCard(title = "Account") {
                Text("Change Password", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = oldPw, onValueChange = { oldPw = it },
                    label = { Text("Current password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPw, onValueChange = { newPw = it },
                    label = { Text("New password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                pwMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val r = NetworkClient.create<AuthApiService>()
                                    .changePassword(ChangePasswordRequest(oldPw, newPw))
                                pwMsg = if (r.isSuccessful) "✅ Password changed" else "❌ Failed (${r.code()})"
                                if (r.isSuccessful) { oldPw = ""; newPw = "" }
                            } catch (e: Exception) { pwMsg = "❌ ${e.message}" }
                        }
                    },
                    enabled = oldPw.isNotBlank() && newPw.length >= 6,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Change Password") }
            }

            // ── Sign Out ────────────────────────────────────────────────────
            SectionCard(title = "Session") {
                OutlinedButton(
                    onClick = { authVm.logout(); onLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }

            // ── App info ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onBackground.copy(0.4f))
                Spacer(Modifier.width(8.dp))
                Text("Odysseus Android v2.0", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.4f))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
