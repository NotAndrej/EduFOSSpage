package org.edufosspage.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.edufosspage.data.PinStore

@Composable
fun PinSetupScreen(
    onPinSet:   () -> Unit,
    onDismiss:  () -> Unit,
    isChanging: Boolean = false,
) {
    val ctx        = LocalContext.current
    val scope      = rememberCoroutineScope()
    val haptics    = LocalHapticFeedback.current
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    var step     by rememberSaveable { mutableStateOf(1) }
    var firstPin by rememberSaveable { mutableStateOf("") }
    var input    by rememberSaveable { mutableStateOf("") }
    var isError  by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf("") }

    val shakeOffset = remember { Animatable(0f) }

    fun shake(msg: String) {
        isError  = true
        errorMsg = msg
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(0f, keyframes {
                durationMillis = 400
                10f at 50; -10f at 100; 10f at 150; -10f at 200
                6f at 250; -6f at 300; 0f at 400
            })
            delay(500)
            input = ""; firstPin = ""; step = 1; isError = false; errorMsg = ""
        }
    }

    fun onDigit(d: String) {
        if (input.length >= 6 || isError) return
        input += d
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (input.length == 6) {
            scope.launch {
                delay(80)
                when (step) {
                    1 -> { firstPin = input; input = ""; step = 2 }
                    2 -> {
                        if (input == firstPin) { PinStore.setPin(ctx, input); onPinSet() }
                        else shake("PINs don't match. Try again.")
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (input.isNotEmpty() && !isError) {
            input = input.dropLast(1)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val title = when {
        isChanging && step == 1 -> "Enter new PIN"
        isChanging && step == 2 -> "Confirm new PIN"
        step == 1               -> "Set a PIN"
        else                    -> "Confirm PIN"
    }
    val subtitle = if (step == 1) "Enter a 6-digit PIN" else "Enter the same PIN again"

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
        }

        if (isLandscape) {
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
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SetupDots(input = input, isError = isError, shakeOffset = shakeOffset.value)
                    if (isError) {
                        Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SetupNumpad(onDigit = ::onDigit, onBackspace = ::onBackspace, buttonSize = 64.dp, spacing = 8.dp)
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SetupDots(input = input, isError = isError, shakeOffset = shakeOffset.value)
                SetupNumpad(onDigit = ::onDigit, onBackspace = ::onBackspace)
                if (isError) {
                    Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SetupDots(input: String, isError: Boolean, shakeOffset: Float) {
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
private fun SetupNumpad(
    onDigit:     (String) -> Unit,
    onBackspace: () -> Unit,
    buttonSize:  androidx.compose.ui.unit.Dp = 80.dp,
    spacing:     androidx.compose.ui.unit.Dp = 12.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally) {
        // Rows 1-9
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { key ->
                    PinPadButton(onClick = { onDigit(key) }, size = buttonSize) {
                        Text(key, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        // Last row - Box placeholder forces 0 centered regardless of constraints
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Box(Modifier.size(buttonSize)) // empty, same size as a button
            PinPadButton(onClick = { onDigit("0") }, size = buttonSize) {
                Text("0", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            }
            PinPadButton(onClick = onBackspace, size = buttonSize) {
                Text("⌫", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
