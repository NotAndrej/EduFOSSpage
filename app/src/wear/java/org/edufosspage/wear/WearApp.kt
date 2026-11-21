package org.edufosspage.wear

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.edufosspage.data.EduMessage
import org.edufosspage.data.EduSession
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.screen.AccountScreen
import org.edufosspage.wear.screen.GradesScreen
import org.edufosspage.wear.screen.LoginScreen
import org.edufosspage.wear.screen.MenuScreen
import org.edufosspage.wear.screen.MessageDetailScreen
import org.edufosspage.wear.screen.MessagesScreen
import org.edufosspage.wear.screen.TimetableScreen

private const val ROUTE_MENU      = "menu"
private const val ROUTE_TIMETABLE = "timetable"
private const val ROUTE_GRADES    = "grades"
private const val ROUTE_MESSAGES  = "messages"
private const val ROUTE_MESSAGE   = "message"
private const val ROUTE_ACCOUNT   = "account"

@Composable
fun WearApp(loginVm: LoginViewModel) {
    when {
        loginVm.sessionReady -> WearNavigation(loginVm)

        // Login succeeded but the session (subjects, teachers, user) is still initialising.
        loginVm.loginSuccess != null -> {
            LaunchedEffect(loginVm.loginSuccess) { loginVm.initSession() }
            LoadingState("Loading your school…")
        }

        loginVm.isAutoLogging -> LoadingState("Signing in…")

        else -> LoginScreen(loginVm)
    }
}

@Composable
private fun WearNavigation(loginVm: LoginViewModel) {
    var route by rememberSaveable { mutableStateOf(ROUTE_MENU) }
    var openedMessage by remember { mutableStateOf<EduMessage?>(null) }

    // The watch's system back gesture pops sub-screens; the detail screen pops to the list.
    BackHandler(enabled = route != ROUTE_MENU) {
        route = if (route == ROUTE_MESSAGE) ROUTE_MESSAGES else ROUTE_MENU
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

        else -> MenuScreen(
            userName        = EduSession.userFullName.ifBlank { "Signed in" },
            userClass       = EduSession.userClass,
            onOpenTimetable = { route = ROUTE_TIMETABLE },
            onOpenGrades    = { route = ROUTE_GRADES },
            onOpenMessages  = { route = ROUTE_MESSAGES },
            onOpenAccount   = { route = ROUTE_ACCOUNT },
        )
    }
}
