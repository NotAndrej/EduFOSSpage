package org.edufosspage.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.data.EduHttp
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.theme.EduWearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // EduHttp gates every request on connectivity, so it needs a context.
        EduHttp.appContext = applicationContext

        setContent {
            EduWearTheme {
                val loginVm: LoginViewModel = viewModel()
                WearApp(loginVm)
            }
        }
    }
}
