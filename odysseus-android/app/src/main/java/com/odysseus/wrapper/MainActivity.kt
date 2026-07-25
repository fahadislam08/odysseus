package com.odysseus.wrapper

import android.os.Bundle
import android.os.Build
import java.io.File
import java.io.StringWriter
import java.io.PrintWriter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.core.UserPreferences
import com.odysseus.wrapper.ui.screens.auth.AuthViewModel
import com.odysseus.wrapper.ui.screens.auth.FirstRunScreen
import com.odysseus.wrapper.ui.screens.auth.LoginScreen
import com.odysseus.wrapper.ui.screens.calendar.CalendarScreen
import com.odysseus.wrapper.ui.screens.chat.ChatScreen
import com.odysseus.wrapper.ui.screens.documents.DocumentsScreen
import com.odysseus.wrapper.ui.screens.email.EmailScreen
import com.odysseus.wrapper.ui.screens.gallery.GalleryScreen
import com.odysseus.wrapper.ui.screens.memory.MemoryScreen
import com.odysseus.wrapper.ui.screens.notes.NotesScreen
import com.odysseus.wrapper.ui.screens.research.ResearchScreen
import com.odysseus.wrapper.ui.screens.settings.SettingsScreen
import com.odysseus.wrapper.ui.screens.tasks.TasksScreen
import com.odysseus.wrapper.ui.theme.OdysseusTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
                File(filesDir, "last_crash.txt").writeText(
                    Build.MANUFACTURER + " " + Build.MODEL + "\n" + Build.VERSION.SDK_INT + "\n\n" + trace
                )
            } catch (_: Exception) { }
            previousHandler?.uncaughtException(thread, throwable)
        }
        enableEdgeToEdge()
        val prefs = UserPreferences(this)
        val savedUrl = runBlocking { prefs.serverUrl.first() }
        if (savedUrl.isNotEmpty()) NetworkClient.baseUrl = savedUrl
        val savedToken = runBlocking { prefs.bearerToken.first() }
        if (savedToken.isNotEmpty()) NetworkClient.bearerToken = savedToken
        setContent {
            val authVm: AuthViewModel = viewModel()
            val darkMode by authVm.darkMode.collectAsStateWithLifecycle()
            OdysseusTheme(darkTheme = darkMode) {
                OdysseusApp(authVm)
            }
        }
    }
}

// ── Navigation routes ─────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object FirstRun  : Screen("first_run",  "Setup",      Icons.Default.Settings)
    object Login     : Screen("login",      "Login",      Icons.AutoMirrored.Filled.Login)
    object NewChat   : Screen("new_chat",   "New Chat",   Icons.Default.Add)
    object Search    : Screen("search",     "Search",     Icons.Default.Search)
    object Chat      : Screen("chat",       "Chats",      Icons.AutoMirrored.Filled.Chat)
    object Brain     : Screen("brain",      "Brain",      Icons.Default.Psychology)
    object Calendar  : Screen("calendar",   "Calendar",   Icons.Default.CalendarMonth)
    object Compare   : Screen("compare",    "Compare",    Icons.Default.Compare)
    object Cookbook  : Screen("cookbook",   "Cookbook",   Icons.Default.MenuBook)
    object Research  : Screen("research",   "Deep Research", Icons.Default.TravelExplore)
    object Gallery   : Screen("gallery",    "Gallery",    Icons.Default.PhotoLibrary)
    object Library   : Screen("library",    "Library",    Icons.Default.LocalLibrary)
    object Documents : Screen("documents",  "Documents",  Icons.Default.Description)
    object Notes     : Screen("notes",      "Notes",      Icons.AutoMirrored.Filled.Notes)
    object Tasks     : Screen("tasks",      "Tasks",      Icons.Default.Schedule)
    object Email     : Screen("email",      "Email",      Icons.Default.Email)
    object Theme     : Screen("theme",      "Theme",      Icons.Default.Palette)
    object Settings  : Screen("settings",   "Settings",   Icons.Default.Settings)
}

// Same order as Odysseus webapp sidebar sections
private val sidebarItems = listOf(
    Screen.NewChat,
    Screen.Search,
    Screen.Brain,
    Screen.Calendar,
    Screen.Compare,
    Screen.Cookbook,
    Screen.Research,
    Screen.Gallery,
    Screen.Library,
    Screen.Documents,
    Screen.Notes,
    Screen.Tasks,
    Screen.Theme
)

// ── Root composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdysseusApp(authVm: AuthViewModel) {
    val navController  = rememberNavController()
    val savedUrl       by authVm.savedServerUrl.collectAsStateWithLifecycle()
    val isLoggedIn     by authVm.prefs.isLoggedIn.collectAsStateWithLifecycle(false)
    val firstRunDone   by authVm.firstRunDone.collectAsStateWithLifecycle()
    val currentEntry   by navController.currentBackStackEntryAsState()
    val currentRoute   = currentEntry?.destination?.route
    val startDest = when {
        !firstRunDone || savedUrl.isEmpty() -> Screen.FirstRun.route
        !isLoggedIn                         -> Screen.Login.route
        else                                -> Screen.Chat.route
    }

    val authRoutes = setOf(Screen.FirstRun.route, Screen.Login.route)
    val showDrawer = currentRoute != null && currentRoute !in authRoutes

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    if (showDrawer) {
        // ── Odysseus sidebar layout: left modal drawer ────────────────────────
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                OdysseusSidebar(
                    currentRoute  = currentRoute,
                    authVm        = authVm,
                    onNavigate    = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    onSettings = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    onLogout = {
                        authVm.logout()
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Login.route) { popUpTo(Screen.Chat.route) { inclusive = true } }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    // Odysseus-style top bar: hamburger + current section title
                    val screen = sidebarItems.find { it.route == currentRoute }
                        ?: if (currentRoute == Screen.Settings.route) Screen.Settings else null
                    TopAppBar(
                        title = {
                            Text(
                                screen?.label ?: "Odysseus",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                MainNavHost(navController, startDest, authVm, Modifier.padding(innerPadding))
            }
        }
    } else {
        // Auth screens — no drawer, no top bar
        MainNavHost(navController, startDest, authVm, Modifier)
    }
}

// ── Sidebar matching Odysseus webapp ─────────────────────────────────────────

@Composable
fun OdysseusSidebar(
    currentRoute: String?,
    authVm: AuthViewModel,
    onNavigate: (Screen) -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val username by authVm.prefs.username.collectAsStateWithLifecycle("")

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor  = MaterialTheme.colorScheme.onSurface
    ) {
        // ── Brand header (same as webapp sidebar-brand) ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Odysseus",
                style     = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.primary,
                fontSize  = 20.sp
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(4.dp))

        // ── Nav items — same order as webapp sidebar ──────────────────────────
        sidebarItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationDrawerItem(
                icon    = {
                    Icon(
                        screen.icon,
                        contentDescription = screen.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                label   = {
                    Text(
                        screen.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize  = 14.sp
                    )
                },
                selected = selected,
                onClick  = { onNavigate(screen) },
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                    .height(44.dp),
                colors  = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    selectedTextColor        = MaterialTheme.colorScheme.primary,
                    unselectedTextColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    selectedIconColor        = MaterialTheme.colorScheme.primary,
                    unselectedIconColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // ── Bottom user bar (same as webapp sidebar-user-bar) ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    username.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                username.ifBlank { "admin" },
                modifier  = Modifier.weight(1f),
                fontSize  = 13.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            IconButton(onClick = onSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

// ── NavHost ───────────────────────────────────────────────────────────────────

@Composable
fun MainNavHost(
    navController: androidx.navigation.NavHostController,
    startDest: String,
    authVm: AuthViewModel,
    modifier: Modifier
) {
    NavHost(navController = navController, startDestination = startDest, modifier = modifier) {
        composable(Screen.FirstRun.route) {
            FirstRunScreen(vm = authVm, onDone = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.FirstRun.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(vm = authVm, onLoggedIn = {
                navController.navigate(Screen.Chat.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.NewChat.route)   { ChatScreen() }
        composable(Screen.Search.route)    { ChatScreen() }
        composable(Screen.Chat.route)      { ChatScreen() }
        composable(Screen.Brain.route)     { MemoryScreen() }
        composable(Screen.Compare.route)   { ChatScreen() }
        composable(Screen.Cookbook.route)  { ChatScreen() }
        composable(Screen.Library.route)   { ChatScreen() }
        composable(Screen.Theme.route)     { ChatScreen() }
        composable(Screen.Notes.route)     { NotesScreen() }
        composable(Screen.Tasks.route)     { TasksScreen() }
        composable(Screen.Calendar.route)  { CalendarScreen() }
        composable(Screen.Email.route)     { EmailScreen() }
        composable(Screen.Documents.route) { DocumentsScreen() }
        composable(Screen.Gallery.route)   { GalleryScreen() }
        composable(Screen.Research.route)  { ResearchScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(authVm = authVm, onLogout = {
                navController.navigate(Screen.Login.route) { popUpTo(Screen.Chat.route) { inclusive = true } }
            })
        }
    }
}
