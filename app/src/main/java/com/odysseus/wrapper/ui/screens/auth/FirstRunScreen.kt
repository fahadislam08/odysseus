package com.odysseus.wrapper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun FirstRunScreen(onDone: () -> Unit, vm: AuthViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var serverUrl   by remember { mutableStateOf("http://") }
    var apiToken    by remember { mutableStateOf("") }
    var testing     by remember { mutableStateOf(false) }
    var testResult  by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo / header
        Icon(Icons.Default.TravelExplore, contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Odysseus", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("AI Workspace", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(Modifier.height(40.dp))

        // Step card
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Connect to your server", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    "Enter the public URL of your Odysseus server. " +
                    "This is typically your EC2 public IP or domain name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value       = serverUrl,
                    onValueChange = { serverUrl = it; testResult = null; testSuccess = false },
                    label       = { Text("Server URL") },
                    placeholder = { Text("http://your-ec2-ip/  or  https://odysseus.yourdomain.com/") },
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    isError = serverUrl.isNotBlank() && !serverUrl.startsWith("http")
                )

                // Example hints
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "http://YOUR_EC2_PUBLIC_IP/" to "EC2 public IP (HTTP)",
                        "https://odysseus.example.com/" to "Custom domain (HTTPS)"
                    ).forEach { (example, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Circle, null, Modifier.size(6.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { serverUrl = example; testResult = null; testSuccess = false },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) { Text(example, fontSize = 10.sp) }
                        }
                    }
                }

                // Test button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            testing = true; testResult = null; testSuccess = false
                            focus.clearFocus()
                            val url = serverUrl.trim().let { if (!it.endsWith("/")) "$it/" else it }
                            com.odysseus.wrapper.core.NetworkClient.baseUrl = url
                            try {
                                val svc = com.odysseus.wrapper.core.NetworkClient
                                    .create<com.odysseus.wrapper.data.remote.AuthApiService>()
                                val r = svc.status()
                                testSuccess  = r.isSuccessful
                                testResult   = if (r.isSuccessful)
                                    "✅ Server reached! ${if (r.body()?.authenticated == true) "Logged in as ${r.body()?.username}" else "Ready to login."}"
                                else
                                    "⚠️ Server responded HTTP ${r.code()} — check the URL."
                            } catch (e: Exception) {
                                testSuccess = false
                                testResult  = "❌ Cannot reach server.\n${e.message}\n\nMake sure:\n• Port 80 is open in EC2 security group\n• Docker containers are running"
                            }
                            testing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = serverUrl.startsWith("http") && !testing
                ) {
                    if (testing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing…")
                    } else {
                        Icon(Icons.Default.NetworkCheck, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Connection")
                    }
                }

                testResult?.let {
                    Card(colors = CardDefaults.cardColors(
                        if (testSuccess) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(it, Modifier.padding(10.dp), fontSize = 12.sp,
                            color = if (testSuccess) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Info card
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("EC2 Setup Checklist", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium)
                }
                listOf(
                    "Security Group: inbound port 80 (HTTP) open to 0.0.0.0/0",
                    "Docker running: docker compose up -d",
                    "Use the EC2 Public IPv4 address, not Private IP"
                ).forEach { tip ->
                    Row(verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(tip, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Spacer(Modifier.height(24.dp))

        // ── Optional: paste mobile token from webapp settings ─────────────────
        Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mobile App Token (optional)", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    "In the Odysseus webapp go to Settings → Mobile App, " +
                    "generate a token and paste it here. The app will use it " +
                    "so you don't need to enter your password each time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value         = apiToken,
                    onValueChange = { apiToken = it.trim() },
                    label         = { Text("Paste token (ody_…)") },
                    placeholder   = { Text("ody_xxxxxxxxxxxxxxxxxxxx") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    leadingIcon   = { Icon(Icons.Default.Key, null) },
                    isError       = apiToken.isNotBlank() && !apiToken.startsWith("ody_")
                )
                if (apiToken.isNotBlank() && !apiToken.startsWith("ody_")) {
                    Text("Token should start with ody_", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Continue button
        Button(
            onClick = {
                val url = serverUrl.trim().let { if (!it.endsWith("/")) "$it/" else it }
                vm.completeFirstRun(url, apiToken.trim())
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = serverUrl.startsWith("http")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Continue to Login", style = MaterialTheme.typography.titleMedium)
        }

        if (!testSuccess && testResult == null) {
            Spacer(Modifier.height(8.dp))
            Text("Test the connection first to make sure everything works",
                fontSize = 11.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(0.4f))
        }

        Spacer(Modifier.height(24.dp))
    }
}
