package org.edufosspage.wear.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import org.edufosspage.data.PinStore

/**
 * How long the watch may stay in the background before it asks for the PIN again. A watch is
 * stopped and restarted constantly (every wrist-down and screen-off), so re-locking immediately
 * would be unbearable; a minute covers a glance or a notification without leaving the inbox open
 * to whoever picks the watch up next.
 */
private const val RELOCK_AFTER_MS = 60_000L

/**
 * In-memory unlock flag, deliberately not persisted. A cold start must always ask for the PIN,
 * while an activity recreation (rotation, a theme change, a resize) must not.
 */
internal object WearAuthState {
    var authenticated = false
    var backgroundedAt = 0L
}

/**
 * Keeps the watch UI behind the PIN when one is set. The phone app also offers biometrics and a
 * user-configurable timeout; on the watch there is a 6-digit PIN and the fixed grace period above.
 * The check runs on the whole app, so the login screen is covered too.
 */
@Composable
fun WearAuthGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current

    // Synchronous read of the stored setting; the PIN is a boolean flag plus a hash, no I/O cost.
    var pinEnabled by remember { mutableStateOf(PinStore.isEnabled(ctx)) }
    var unlocked by remember { mutableStateOf(!pinEnabled || WearAuthState.authenticated) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (PinStore.isEnabled(ctx)) {
                        WearAuthState.backgroundedAt = System.currentTimeMillis()
                    }
                }

                Lifecycle.Event.ON_START -> {
                    // The setting may have changed while the app was away, so re-read it.
                    pinEnabled = PinStore.isEnabled(ctx)
                    val backgroundedAt = WearAuthState.backgroundedAt
                    WearAuthState.backgroundedAt = 0L

                    val expired = backgroundedAt != 0L &&
                        System.currentTimeMillis() - backgroundedAt >= RELOCK_AFTER_MS

                    if (pinEnabled && (!WearAuthState.authenticated || expired)) {
                        WearAuthState.authenticated = false
                        unlocked = false
                    }
                }

                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    if (unlocked) {
        content()
    } else {
        WearPinLockScreen(
            onUnlocked = {
                WearAuthState.authenticated = true
                unlocked = true
            },
        )
    }
}
