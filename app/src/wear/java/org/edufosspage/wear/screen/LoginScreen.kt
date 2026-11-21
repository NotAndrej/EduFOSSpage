package org.edufosspage.wear.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.InfoBlock

@Composable
fun LoginScreen(vm: LoginViewModel) {
    val listState = rememberScalingLazyListState()
    val haptics   = LocalHapticFeedback.current
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "EduFOSS",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                    )
                    Text(
                        "Sign in to EduPage",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }

            item {
                WearTextField(
                    value         = vm.username,
                    onValueChange = { vm.username = it },
                    label         = "Username or email",
                    placeholder   = "name@school.edu",
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Next,
                )
            }

            item {
                WearTextField(
                    value                 = vm.password,
                    onValueChange         = { vm.password = it },
                    label                 = "Password",
                    placeholder           = "••••••••",
                    keyboardType          = KeyboardType.Password,
                    imeAction             = ImeAction.Done,
                    visualTransformation  =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailing              = {
                        Text(
                            if (passwordVisible) "Hide" else "Show",
                            style    = MaterialTheme.typography.caption1,
                            color    = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .clickable { passwordVisible = !passwordVisible }
                                .padding(4.dp),
                        )
                    },
                )
            }

            item {
                val canSubmit = !vm.isLoading && vm.username.isNotBlank() && vm.password.isNotBlank()
                Button(
                    onClick  = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.login()
                    },
                    enabled  = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (vm.isLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Log in")
                    }
                }
            }

            if (vm.errorMessage.isNotBlank()) {
                item { InfoBlock(vm.errorMessage, isError = true) }
            }
        }
    }
}
