package org.edufosspage.wear

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.data.DeepLinkState
import org.edufosspage.data.EduHttp
import org.edufosspage.data.NotificationChannels
import org.edufosspage.data.NotificationScheduler
import org.edufosspage.data.NotificationStore
import org.edufosspage.data.NotificationWorker
import org.edufosspage.data.TamperStore
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.theme.EduWearTheme

class MainActivity : ComponentActivity() {
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("WearMainActivity", "POST_NOTIFICATIONS granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // EduHttp gates every request on connectivity, so it needs a context.
        EduHttp.appContext = applicationContext

        applyScreenshotFlag()
        NotificationChannels.createAll(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (NotificationStore.isMasterEnabled(this)) {
            NotificationScheduler.schedule(this)
        }

        handleNotificationIntent(intent)

        setContent {
            EduWearTheme {
                val loginVm: LoginViewModel = viewModel()
                WearApp(loginVm)
            }
        }
    }

    override fun onResume() { super.onResume(); applyScreenshotFlag() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(NotificationWorker.EXTRA_TAB, -1) ?: -1
        if (tab >= 0) DeepLinkState.request(tab)
    }

    private fun applyScreenshotFlag() {
        if (TamperStore.isScreenshotBlocked(this)) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
