package org.edufosspage

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.launch
import org.edufosspage.data.*
import org.edufosspage.ui.auth.PinLockScreen
import org.edufosspage.ui.login.LoginScreen
import org.woheller69.freeDroidWarn.FreeDroidWarn

val BlueLightScheme = lightColorScheme(
    primary              = Color(0xFF1565C0), onPrimary            = Color.White,
    primaryContainer     = Color(0xFFD3E4FF), onPrimaryContainer   = Color(0xFF001C3D),
    secondary            = Color(0xFF535F70), onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD7E3F7), onSecondaryContainer = Color(0xFF101C2B),
    tertiary             = Color(0xFF6B5778), onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFF2DAFF), onTertiaryContainer  = Color(0xFF251431),
    background           = Color(0xFFF8F9FF), onBackground         = Color(0xFF191C20),
    surface              = Color(0xFFF8F9FF), onSurface            = Color(0xFF191C20),
    surfaceVariant       = Color(0xFFDFE2EB), onSurfaceVariant     = Color(0xFF43474E),
)

val BlueDarkScheme = darkColorScheme(
    primary              = Color(0xFFA4C8FF), onPrimary            = Color(0xFF003063),
    primaryContainer     = Color(0xFF00479A), onPrimaryContainer   = Color(0xFFD3E4FF),
    secondary            = Color(0xFFBBC7DB), onSecondary          = Color(0xFF253140),
    secondaryContainer   = Color(0xFF3B4858), onSecondaryContainer = Color(0xFFD7E3F7),
    background           = Color(0xFF111318), onBackground         = Color(0xFFE2E2E9),
    surface              = Color(0xFF111318), onSurface            = Color(0xFFE2E2E9),
    surfaceVariant       = Color(0xFF43474E), onSurfaceVariant     = Color(0xFFC3C7CF),
)

val AmoledScheme = darkColorScheme(
    primary              = Color(0xFFA4C8FF), onPrimary            = Color(0xFF003063),
    primaryContainer     = Color(0xFF00347A), onPrimaryContainer   = Color(0xFFD3E4FF),
    secondary            = Color(0xFFBBC7DB), onSecondary          = Color(0xFF253140),
    secondaryContainer   = Color(0xFF1A1A1A), onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary             = Color(0xFFD6BEE4), onTertiary           = Color(0xFF3B2948),
    tertiaryContainer    = Color(0xFF1A1A1A), onTertiaryContainer  = Color(0xFFF2DAFF),
    background           = Color(0xFF000000), onBackground         = Color(0xFFE2E2E9),
    surface              = Color(0xFF000000), onSurface            = Color(0xFFE2E2E9),
    surfaceVariant       = Color(0xFF1A1A1A), onSurfaceVariant     = Color(0xFFC3C7CF),
    outline              = Color(0xFF2A2A2A), outlineVariant       = Color(0xFF1F1F1F),
)

private const val MIN_DENSITY = 2.0f

@Composable
fun AdaptiveDensity(content: @Composable () -> Unit) {
    val current = LocalDensity.current
    val clamped = if (current.density < MIN_DENSITY) Density(MIN_DENSITY, current.fontScale) else current
    CompositionLocalProvider(LocalDensity provides clamped, content = content)
}

private object AuthState {
    var authenticated      = false
    var lastBackgroundTime = 0L
}

@Composable
fun FragmentActivity.AuthGate(content: @Composable () -> Unit) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    val bioEnabled  by BiometricStore.enabledFlow(ctx).collectAsState(initial = false)
    val pinEnabled  = PinStore.isEnabled(ctx)
    val timeoutMins by BiometricStore.timeoutFlow(ctx).collectAsState(initial = 0)

    val authRequired = bioEnabled || pinEnabled
    var authenticated by remember { mutableStateOf(AuthState.authenticated || !authRequired) }
    var showPin       by remember { mutableStateOf(false) }

    val bioAvailable = BiometricManager.from(ctx)
        .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    fun showBiometricPrompt() {
        if (!bioEnabled || !bioAvailable) {
            if (pinEnabled) { showPin = true } else { authenticated = true; AuthState.authenticated = true }
            return
        }
        BiometricPrompt(
            this@AuthGate,
            ContextCompat.getMainExecutor(ctx),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated = true; AuthState.authenticated = true; showPin = false
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    Log.w("AuthGate", "bio error $code: $msg")
                    if (pinEnabled) showPin = true
                }
                override fun onAuthenticationFailed() { Log.w("AuthGate", "bio failed") }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("EduFOSSpage")
                .setSubtitle("Verify your identity to continue")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        )
    }

    LaunchedEffect(authRequired) {
        if (!authRequired || AuthState.authenticated) return@LaunchedEffect
        when {
            bioEnabled && bioAvailable -> showBiometricPrompt()
            pinEnabled                 -> showPin = true
            else                       -> { authenticated = true; AuthState.authenticated = true }
        }
    }

    DisposableEffect(bioEnabled, pinEnabled, timeoutMins) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (authRequired) {
                        AuthState.lastBackgroundTime = System.currentTimeMillis()
                        scope.launch { BiometricStore.setLastBackground(ctx, AuthState.lastBackgroundTime) }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (NotificationStore.isMasterEnabled(ctx)) {
                        NotificationScheduler.runNow(ctx)
                    }
                    if (authRequired) {
                        val lastBg = AuthState.lastBackgroundTime
                        if (lastBg == 0L) return@LifecycleEventObserver
                        val elapsed   = System.currentTimeMillis() - lastBg
                        val timeoutMs = timeoutMins * 60_000L
                        AuthState.lastBackgroundTime = 0L
                        if (timeoutMins == 0 || elapsed >= timeoutMs) {
                            authenticated = false; AuthState.authenticated = false; showPin = false
                            when {
                                bioEnabled && bioAvailable -> showBiometricPrompt()
                                pinEnabled                 -> showPin = true
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    when {
        authenticated -> content()
        showPin       -> PinLockScreen(
            onUnlocked      = { authenticated = true; AuthState.authenticated = true; showPin = false },
            onUseBiometrics = if (bioEnabled && bioAvailable) {{ showPin = false; showBiometricPrompt() }} else null
        )
        else -> {}
    }
}

class MainActivity : FragmentActivity() {
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("MainActivity", "POST_NOTIFICATIONS granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        FreeDroidWarn.showWarningOnUpgrade(this, BuildConfig.VERSION_CODE)
        applyScreenshotFlag()

        NotificationChannels.createAll(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (NotificationStore.isMasterEnabled(this)) {
            NotificationScheduler.schedule(this)
        }

        handleNotificationIntent(intent)

        val startTab = intent.getIntExtra(NotificationWorker.EXTRA_TAB, -1)
            .takeIf { it >= 0 } ?: 0
        setContent {
            val systemDark = isSystemInDarkTheme()
            val theme      by ThemeStore.themeFlow(this).collectAsState(initial = AppTheme.SYSTEM)
            val colorScheme = when (theme) {
                AppTheme.LIGHT  -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(this) else BlueLightScheme
                AppTheme.DARK   -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(this)  else BlueDarkScheme
                AppTheme.AMOLED -> {
                    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(this) else BlueDarkScheme
                    base.copy(
                        background = Color(0xFF000000), surface = Color(0xFF000000),
                        surfaceVariant = Color(0xFF1A1A1A), outline = Color(0xFF2A2A2A), outlineVariant = Color(0xFF1F1F1F),
                    )
                }
                AppTheme.SYSTEM -> when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                        if (systemDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
                    systemDark -> BlueDarkScheme
                    else       -> BlueLightScheme
                }
            }

            // Update check on launch
            var pendingUpdate by remember { mutableStateOf<org.edufosspage.data.UpdateInfo?>(null) }
            val updateScope   = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                updateScope.launch {
                    pendingUpdate = org.edufosspage.data.UpdateChecker.check()
                }
            }

            pendingUpdate?.let { info ->
                org.edufosspage.ui.UpdateDialog(
                    info      = info,
                    onDismiss = { pendingUpdate = null },
                )
            }

            AdaptiveDensity {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AuthGate {
                                LoginScreen(startTab = startTab)
                            }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); applyScreenshotFlag() }

    /** Called when the app is already running and a notification is tapped. */
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
