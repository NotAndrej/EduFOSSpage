package org.edufosspage.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.edufosspage.R
import org.edufosspage.data.PinStore

@Composable
fun PinLockScreen(
    onUnlocked:      () -> Unit,
    onUseBiometrics: (() -> Unit)? = null,
) {
    val ctx        = LocalContext.current
    val scope      = rememberCoroutineScope()
    val haptics    = LocalHapticFeedback.current
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    var input   by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    // Loaded from SharedPreferences so a force-kill between attempts cannot reset the counter.
    var attempts by remember { mutableIntStateOf(PinStore.getAttempts(ctx)) }

    val shakeOffset = remember { Animatable(0f) }

    fun onWrong() {
        attempts++
        PinStore.setAttempts(ctx, attempts)
        isError = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(0f, keyframes {
                durationMillis = 400
                10f at 50; -10f at 100; 10f at 150; -10f at 200
                6f at 250; -6f at 300; 0f at 400
            })
            delay(100)
            input   = ""
            isError = false
        }
    }

    fun onDigit(d: String) {
        if (input.length >= 6 || isError) return
        input += d
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (input.length == 6) {
            scope.launch {
                delay(80)
                if (PinStore.verify(ctx, input)) { PinStore.clearAttempts(ctx); onUnlocked() }
                else onWrong()
            }
        }
    }

    fun onBackspace() {
        if (isError || input.isEmpty()) return
        input = input.dropLast(1)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun onClearAll() {
        if (input.isEmpty()) return
        input = ""
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val subtitleText = if (attempts == 0) stringResource(R.string.core_enter_pin_to_continue)
                       else stringResource(R.string.core_incorrect_pin)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        if (isLandscape) {
            // Landscape: header left, numpad right
            Row(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lock, null, Modifier.size(32.dp),
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.core_enter_pin), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (attempts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PinDots(input = input, isError = isError, shakeOffset = shakeOffset.value)
                }

                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PinNumpad(
                        onDigit         = ::onDigit,
                        onBackspace     = ::onBackspace,
                        onClearAll      = ::onClearAll,
                        onUseBiometrics = onUseBiometrics,
                        buttonSize      = 64.dp,
                        spacing         = 8.dp
                    )
                }
            }
        } else {
            // Portrait: stacked
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Outlined.Lock, null, Modifier.size(40.dp),
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.core_enter_pin), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (attempts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PinDots(input = input, isError = isError, shakeOffset = shakeOffset.value)
                PinNumpad(
                    onDigit         = ::onDigit,
                    onBackspace     = ::onBackspace,
                    onClearAll      = ::onClearAll,
                    onUseBiometrics = onUseBiometrics,
                )
            }
        }
    }
}

@Composable
private fun PinDots(input: String, isError: Boolean, shakeOffset: Float) {
    Row(
        modifier              = Modifier.absoluteOffset(x = shakeOffset.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(6) { i ->
            val filled   = i < input.length
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError && filled -> MaterialTheme.colorScheme.error
                    filled            -> MaterialTheme.colorScheme.primary
                    else              -> MaterialTheme.colorScheme.surfaceVariant
                }, label = "dot$i"
            )
            Box(Modifier.size(16.dp).clip(CircleShape).background(dotColor))
        }
    }
}

@Composable
private fun PinNumpad(
    onDigit:         (String) -> Unit,
    onBackspace:     () -> Unit,
    onClearAll:      () -> Unit,
    onUseBiometrics: (() -> Unit)?,
    buttonSize:      androidx.compose.ui.unit.Dp = 80.dp,
    spacing:         androidx.compose.ui.unit.Dp = 12.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "del"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> if (onUseBiometrics != null) {
                            PinPadButton(onClick = onUseBiometrics, size = buttonSize) {
                                Icon(Icons.Outlined.Fingerprint, stringResource(R.string.core_use_biometrics), Modifier.size(24.dp))
                            }
                        } else {
                            Spacer(Modifier.size(buttonSize))
                        }
                        "del" -> Surface(
                            shape    = CircleShape,
                            color    = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(buttonSize)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap       = { onBackspace() },
                                        onLongPress = { onClearAll() }
                                    )
                                }
                        ) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("⌫", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        else -> PinPadButton(onClick = { onDigit(key) }, size = buttonSize) {
                            Text(key, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PinPadButton(
    onClick: () -> Unit,
    size:    androidx.compose.ui.unit.Dp = 80.dp,
    content: @Composable () -> Unit,
) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(size)) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { content() }
    }
}
