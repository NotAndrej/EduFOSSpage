package org.edufosspage.wear.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.edufosspage.R
import org.edufosspage.data.PinStore

/** The phone app's PIN is six digits; the watch keeps the same length so the two are familiar. */
private const val PIN_LENGTH = 6

/** ⌫ — the watch font has no dedicated backspace glyph in the core icon set. */
private const val BACKSPACE = "\u232B"

/**
 * Full-screen PIN prompt for the round display. On a wrong code the dots shake, the attempt count
 * (which survives a force-kill, because it lives in SharedPreferences) is bumped, and the prompt
 * switches to an error message. The layout is sized so the whole pad clears the round bezel.
 */
@Composable
fun WearPinLockScreen(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var input by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(PinStore.getAttempts(ctx)) }

    val shake = remember { Animatable(0f) }

    fun onWrong() {
        attempts++
        PinStore.setAttempts(ctx, attempts)
        isError = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 400
                8f at 50; -8f at 100; 8f at 150; -8f at 200
                5f at 250; -5f at 300; 0f at 400
            })
            delay(120)
            input = ""
            isError = false
        }
    }

    fun onDigit(digit: String) {
        if (input.length >= PIN_LENGTH || isError) return
        input += digit
        if (input.length == PIN_LENGTH) {
            scope.launch {
                delay(80)
                if (PinStore.verify(ctx, input)) {
                    PinStore.clearAttempts(ctx)
                    onUnlocked()
                } else {
                    onWrong()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (attempts == 0) stringResource(R.string.wear_pin_enter)
                    else stringResource(R.string.wear_pin_incorrect),
                    style = MaterialTheme.typography.title3,
                    color = if (attempts > 0) MaterialTheme.colors.error else MaterialTheme.colors.onBackground,
                )
            }
            WearPinDots(input, isError, shake.value)
            WearPinPad(onDigit = ::onDigit, onBackspace = { if (!isError) input = input.dropLast(1) })
        }
    }
}

/**
 * Two-step PIN creation (choose, then confirm). Reached from the Account screen; [onDone] is
 * called only after the PIN is stored, [onCancel] backs out without changing anything.
 */
@Composable
fun WearPinSetupScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    isChanging: Boolean = false,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var step by rememberSaveable { mutableStateOf(1) }
    var firstPin by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf("") }

    val shake = remember { Animatable(0f) }
    val mismatchMsg = stringResource(R.string.wear_pin_mismatch)

    fun onMismatch(msg: String) {
        isError = true
        errorMsg = msg
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 400
                8f at 50; -8f at 100; 8f at 150; -8f at 200
                5f at 250; -5f at 300; 0f at 400
            })
            delay(650)
            input = ""
            firstPin = ""
            step = 1
            isError = false
            errorMsg = ""
        }
    }

    fun onDigit(digit: String) {
        if (input.length >= PIN_LENGTH || isError) return
        input += digit
        if (input.length == PIN_LENGTH) {
            scope.launch {
                delay(80)
                when (step) {
                    1 -> {
                        firstPin = input
                        input = ""
                        step = 2
                    }
                    else -> if (input == firstPin) {
                        PinStore.setPin(ctx, input)
                        onDone()
                    } else {
                        onMismatch(mismatchMsg)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CompactChip(
            onClick = onCancel,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.wear_cd_cancel)) },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                when {
                    isError -> errorMsg
                    step == 2 -> stringResource(R.string.wear_pin_confirm)
                    isChanging -> stringResource(R.string.wear_pin_new)
                    else -> stringResource(R.string.wear_pin_set)
                },
                style = MaterialTheme.typography.title3,
                color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                if (step == 1) stringResource(R.string.wear_pin_choose)
                else stringResource(R.string.wear_pin_enter_again),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
            WearPinDots(input, isError, shake.value)
            WearPinPad(onDigit = ::onDigit, onBackspace = { if (!isError) input = input.dropLast(1) })
        }
    }
}

@Composable
private fun WearPinDots(input: String, isError: Boolean, shakeOffset: Float) {
    Row(
        modifier = Modifier.absoluteOffset(x = shakeOffset.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PIN_LENGTH) { index ->
            val filled = index < input.length
            val color by animateColorAsState(
                targetValue = when {
                    isError && filled -> MaterialTheme.colors.error
                    filled -> MaterialTheme.colors.primary
                    else -> MaterialTheme.colors.surface
                },
                label = "dot$index",
            )
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
    }
}

/**
 * Compact 3x4 keypad. The buttons are 38dp — below the 48dp accessibility minimum, but the round
 * 233dp display has no room for larger targets once six dots and a label are stacked above the pad,
 * and a watch tap is a deliberate gesture with a wide finger contact.
 */
@Composable
private fun WearPinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    size: Dp = 38.dp,
    spacing: Dp = 3.dp,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", BACKSPACE),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { key ->
                    when (key) {
                        // Empty cell keeps "0" in the middle column.
                        "" -> Spacer(Modifier.size(size))
                        BACKSPACE -> WearPinKey(BACKSPACE, size) { onBackspace() }
                        else -> WearPinKey(key, size) { onDigit(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WearPinKey(label: String, size: Dp, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.title2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface,
        )
    }
}
