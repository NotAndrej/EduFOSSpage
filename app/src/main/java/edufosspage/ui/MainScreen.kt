package org.edufosspage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.data.AppCacheState
import org.edufosspage.data.DeepLinkState
import org.edufosspage.ui.about.AboutScreen
import org.edufosspage.ui.grades.GradesScreen
import org.edufosspage.ui.home.HomeScreen
import org.edufosspage.ui.home.HomeViewModel
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.ui.messages.MessagesScreen
import org.edufosspage.ui.other.OtherScreen
import org.edufosspage.ui.settings.NotificationsScreen
import org.edufosspage.ui.settings.PrivacyScreen
import org.edufosspage.ui.settings.SettingsScreen
import org.edufosspage.ui.settings.ThemeScreen
import org.edufosspage.ui.timetable.TimetableScreen

data class NavItem(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

val navItems = listOf(
    NavItem("Home",      Icons.Filled.Home,         Icons.Outlined.Home),
    NavItem("Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavItem("Grades",    Icons.Filled.Grade,         Icons.Outlined.Grade),
    NavItem("Messages",  Icons.Filled.Email,         Icons.Outlined.Email),
    NavItem("Other",     Icons.Filled.MoreHoriz,     Icons.Outlined.MoreHoriz),
)

@Composable
fun MainScreen(startTab: Int = 0, vm: LoginViewModel = viewModel()) {
    MainTabsScreen(loginVm = vm, startTab = startTab)
}

@Composable
private fun CachedDataBanner() {
    val showingCached by AppCacheState.showingCached.collectAsState()
    AnimatedVisibility(
        visible = showingCached,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut()
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiaryContainer) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CloudOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    "Showing cached data - updating...",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier   = Modifier.weight(1f)
                )
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun MainTabsScreen(loginVm: LoginViewModel, startTab: Int = 0) {
    var selectedTab by rememberSaveable { mutableStateOf(startTab) }
    var route by rememberSaveable { mutableStateOf("tabs") }

    // Handle notification tap deep-links fired while the app is already running.
    val pendingTab by DeepLinkState.pendingTab.collectAsState()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { tab ->
            selectedTab = tab
            route = "tabs"
            DeepLinkState.consume()
        }
    }
    val sharedVm: HomeViewModel = viewModel()

    BackHandler(enabled = route != "tabs") {
        route = when {
            route.startsWith("settings/") -> "settings"
            else                          -> "tabs"
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier       = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                navItems.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = selectedTab == i && route == "tabs",
                        onClick  = { selectedTab = i; route = "tabs" },
                        icon     = {
                            Icon(
                                if (selectedTab == i && route == "tabs") item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            CachedDataBanner()
            Box(Modifier.weight(1f)) {
                Crossfade(targetState = route, label = "route") { r ->
                    when (r) {
                        "tabs" -> when (selectedTab) {
                            0 -> HomeScreen(vm = sharedVm, onNavigateToTab = { selectedTab = it })
                            1 -> TimetableScreen(vm = sharedVm)
                            2 -> GradesScreen()
                            3 -> MessagesScreen()
                            4 -> OtherScreen(
                                onNavigateToSettings = { route = "settings" },
                                onNavigateToAbout    = { route = "about" }
                            )
                        }
                        "settings"           -> SettingsScreen(
                            loginVm              = loginVm,
                            onBack               = { route = "tabs" },
                            onLoggedOut          = { route = "tabs" },
                            onOpenThemes         = { route = "settings/themes" },
                            onOpenPrivacy        = { route = "settings/privacy" },
                            onOpenNotifications  = { route = "settings/notifications" },
                        )
                        "settings/themes"         -> ThemeScreen(onBack = { route = "settings" })
                        "settings/privacy"        -> PrivacyScreen(onBack = { route = "settings" })
                        "settings/notifications"  -> NotificationsScreen(onBack = { route = "settings" })
                        "about"              -> AboutScreen(onBack = { route = "tabs" })
                    }
                }
            }
        }
    }
}
