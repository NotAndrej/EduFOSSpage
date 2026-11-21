package org.edufosspage.wear

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.edufosspage.R
import org.edufosspage.data.DeepLinkState
import org.edufosspage.data.EduMessage
import org.edufosspage.data.EduSession
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.auth.WearAuthGate
import org.edufosspage.wear.auth.WearPinSetupScreen
import org.edufosspage.wear.screen.AboutScreen
import org.edufosspage.wear.screen.AccountScreen
import org.edufosspage.wear.screen.GradesScreen
import org.edufosspage.wear.screen.LoginScreen
import org.edufosspage.wear.screen.MenuScreen
import org.edufosspage.wear.screen.MessageDetailScreen
import org.edufosspage.wear.screen.MessagesScreen
import org.edufosspage.wear.screen.NotificationsScreen
import org.edufosspage.wear.screen.PrivacyScreen
import org.edufosspage.wear.screen.SettingsScreen
import org.edufosspage.wear.screen.ThemeScreen
import org.edufosspage.wear.screen.TimetableScreen

private const val ROUTE_MENU      = "menu"
private const val ROUTE_TIMETABLE = "timetable"
private const val ROUTE_GRADES    = "grades"
private const val ROUTE_MESSAGES  = "messages"
private const val ROUTE_MESSAGE   = "message"
private const val ROUTE_ACCOUNT   = "account"

private const val ROUTE_SETTINGS      = "settings"
private const val ROUTE_SET_THEME     = "settings/theme"
private const val ROUTE_SET_PRIVACY   = "settings/privacy"
private const val ROUTE_SET_NOTIF     = "settings/notifications"
private const val ROUTE_SET_ABOUT     = "settings/about"
private const val ROUTE_PIN           = "pin"

@Composable
fun WearApp(loginVm: LoginViewModel) {
    // The PIN gate wraps everything, so the login screen is covered as well as the content.
    WearAuthGate {
        when {
            loginVm.sessionReady -> WearNavigation(loginVm)

            // Login succeeded but the session (subjects, teachers, user) is still initialising.
            loginVm.loginSuccess != null -> {
                LaunchedEffect(loginVm.loginSuccess) { loginVm.initSession() }
                LoadingState(stringResource(R.string.wear_loading_school))
            }

            loginVm.isAutoLogging -> LoadingState(stringResource(R.string.wear_signing_in))

            else -> LoginScreen(loginVm)
        }
    }
}

@Composable
private fun WearNavigation(loginVm: LoginViewModel) {
    var route by rememberSaveable { mutableStateOf(ROUTE_MENU) }
    var openedMessage by remember { mutableStateOf<EduMessage?>(null) }
    val signedInLabel = stringResource(R.string.wear_signed_in)

    // A notification tap deep-links to the matching screen while the watch app is already running.
    val pendingTab by DeepLinkState.pendingTab.collectAsState()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { tab ->
            route = when (tab) {
                DeepLinkState.TAB_TIMETABLE -> ROUTE_TIMETABLE
                DeepLinkState.TAB_GRADES    -> ROUTE_GRADES
                DeepLinkState.TAB_MESSAGES  -> ROUTE_MESSAGES
                else                        -> ROUTE_MENU
            }
            DeepLinkState.consume()
        }
    }

    // The watch's system back gesture pops sub-screens; the detail screen pops to the list and the
    // PIN setup returns to the privacy screen it was opened from.
    BackHandler(enabled = route != ROUTE_MENU) {
        route = when (route) {
            ROUTE_MESSAGE -> ROUTE_MESSAGES
            ROUTE_PIN     -> ROUTE_SET_PRIVACY
            ROUTE_SET_THEME, ROUTE_SET_PRIVACY, ROUTE_SET_NOTIF, ROUTE_SET_ABOUT -> ROUTE_SETTINGS
            else -> ROUTE_MENU
        }
    }

    when (route) {
        ROUTE_TIMETABLE -> TimetableScreen(onBack = { route = ROUTE_MENU })

        ROUTE_GRADES -> GradesScreen(onBack = { route = ROUTE_MENU })

        ROUTE_MESSAGES -> MessagesScreen(
            onBack        = { route = ROUTE_MENU },
            onOpenMessage = { message ->
                openedMessage = message
                route         = ROUTE_MESSAGE
            },
        )

        ROUTE_MESSAGE -> MessageDetailScreen(
            message = openedMessage,
            onBack  = { route = ROUTE_MESSAGES },
        )

        ROUTE_ACCOUNT -> AccountScreen(
            userName = EduSession.userFullName,
            userClass = EduSession.userClass,
            onBack   = { route = ROUTE_MENU },
            onLogout = { loginVm.logout() },
        )

        ROUTE_SETTINGS -> SettingsScreen(
            onBack               = { route = ROUTE_MENU },
            onOpenTheme          = { route = ROUTE_SET_THEME },
            onOpenPrivacy        = { route = ROUTE_SET_PRIVACY },
            onOpenNotifications  = { route = ROUTE_SET_NOTIF },
            onOpenAbout          = { route = ROUTE_SET_ABOUT },
        )

        ROUTE_SET_THEME   -> ThemeScreen(onBack = { route = ROUTE_SETTINGS })

        ROUTE_SET_PRIVACY -> PrivacyScreen(
            onBack   = { route = ROUTE_SETTINGS },
            onSetPin = { route = ROUTE_PIN },
        )

        ROUTE_SET_NOTIF   -> NotificationsScreen(onBack = { route = ROUTE_SETTINGS })

        ROUTE_SET_ABOUT   -> AboutScreen(onBack = { route = ROUTE_SETTINGS })

        ROUTE_PIN -> WearPinSetupScreen(
            onDone   = { route = ROUTE_SET_PRIVACY },
            onCancel = { route = ROUTE_SET_PRIVACY },
        )

        else -> MenuScreen(
            userName        = EduSession.userFullName.ifBlank { signedInLabel },
            userClass       = EduSession.userClass,
            onOpenTimetable = { route = ROUTE_TIMETABLE },
            onOpenGrades    = { route = ROUTE_GRADES },
            onOpenMessages  = { route = ROUTE_MESSAGES },
            onOpenAccount   = { route = ROUTE_ACCOUNT },
            onOpenSettings  = { route = ROUTE_SETTINGS },
        )
    }
}
