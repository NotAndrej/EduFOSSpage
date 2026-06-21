package org.edufosspage.ui.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.edufosspage.data.AppCache
import org.edufosspage.data.BiometricStore
import org.edufosspage.data.PinStore
import org.edufosspage.data.TamperStore
import org.edufosspage.ui.auth.PinSetupScreen

@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    // Biometric
    val biometricEnabled by BiometricStore.enabledFlow(ctx).collectAsState(initial = false)
    val biometricTimeout by BiometricStore.timeoutFlow(ctx).collectAsState(initial = 0)
    val bioAvailable = BiometricManager.from(ctx)
        .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    // PIN
    var pinEnabled    by remember { mutableStateOf(PinStore.isEnabled(ctx)) }
    var hasPin        by remember { mutableStateOf(PinStore.hasPin(ctx)) }
    var showPinSetup  by remember { mutableStateOf(false) }
    var isChangingPin by remember { mutableStateOf(false) }

    val locked = !biometricEnabled && !pinEnabled

    // Other toggles
    val screenshotBlocked = remember { mutableStateOf(TamperStore.isScreenshotBlocked(ctx)) }
    val cacheEnabled      = remember { mutableStateOf(AppCache.isCacheEnabled(ctx)) }

    if (showPinSetup) {
        PinSetupScreen(
            isChanging = isChangingPin,
            onPinSet   = {
                pinEnabled    = true
                hasPin        = true
                showPinSetup  = false
                isChangingPin = false
                PinStore.setEnabled(ctx, true)
            },
            onDismiss = {
                if (!hasPin) pinEnabled = false
                showPinSetup  = false
                isChangingPin = false
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Privacy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (locked) {
                Surface(
                    shape    = RoundedCornerShape(20.dp),
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "Enable biometric lock or PIN below to change privacy settings",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Biometric
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Biometric lock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (bioAvailable) "Require fingerprint or face to open app"
                                else "No biometric hardware available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = biometricEnabled && bioAvailable,
                            onCheckedChange = { if (bioAvailable) scope.launch { BiometricStore.setEnabled(ctx, it) } },
                            enabled         = bioAvailable
                        )
                    }

                    if (biometricEnabled && bioAvailable) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("Lock after being in background for:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(0 to "Every open", 1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes", 30 to "30 minutes")
                            .forEach { (mins, label) ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = biometricTimeout == mins,
                                        onClick  = { if (!locked) scope.launch { BiometricStore.setTimeout(ctx, mins) } },
                                        enabled  = !locked
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium,
                                        color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // PIN
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Pin, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("PIN lock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (hasPin) "6-digit PIN is set - toggle to remove"
                                else "Toggle on to set a 6-digit PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = pinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (hasPin) { pinEnabled = true; PinStore.setEnabled(ctx, true) }
                                    else        { pinEnabled = false; showPinSetup = true }
                                } else {
                                    PinStore.clearPin(ctx); pinEnabled = false; hasPin = false
                                }
                            }
                        )
                    }

                    if (hasPin) {
                        TextButton(
                            onClick  = { isChangingPin = true; showPinSetup = true },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Change PIN") }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Screenshot blocking
                    PrivacyRow(
                        icon      = Icons.Outlined.Screenshot,
                        label     = "Block screenshots",
                        sub       = "Prevent screenshots and screen recording of the app",
                        checked   = screenshotBlocked.value,
                        locked    = locked,
                        onChanged = { enabled -> screenshotBlocked.value = enabled; TamperStore.setScreenshotBlocked(ctx, enabled) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Cache
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storage, null, Modifier.size(20.dp),
                            tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Local data cache",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                                color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (cacheEnabled.value) "Grades, messages and timetable are saved to disk for instant launch"
                                else "No data is written to disk - app starts fresh every launch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (locked) 0.4f else 1f)
                            )
                        }
                        Switch(
                            checked         = cacheEnabled.value,
                            enabled         = !locked,
                            onCheckedChange = { enabled -> cacheEnabled.value = enabled; AppCache.setCacheEnabled(ctx, enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    label:     String,
    sub:       String,
    checked:   Boolean,
    locked:    Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp),
            tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                   else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (locked) 0.4f else 1f))
        }
        Switch(checked = checked, enabled = !locked, onCheckedChange = onChanged)
    }
}
