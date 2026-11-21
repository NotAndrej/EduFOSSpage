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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
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
import org.edufosspage.R
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.wear.InfoBlock

@Composable
fun LoginScreen(vm: LoginViewModel) {
    val listState = rememberScalingLazyListState()
    val haptics   = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }

    val canSubmit = !vm.isLoading && vm.username.isNotBlank() && vm.password.isNotBlank()

    fun submit() {
        if (!canSubmit) return
        // Dropping focus hides the IME; otherwise it stays up over the next screen.
        focusManager.clearFocus()
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.login()
    }

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            // Scaffold does not inset its content for the time text, so the top
            // padding here is what keeps the heading clear of the clock. Kept
            // tight because the whole form has to fit a round ~233 dp screen.
            contentPadding      = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "EduFOSSpage",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                    )
                    Text(
                        stringResource(R.string.wear_login_subtitle),
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }

            item {
                WearTextField(
                    value         = vm.username,
                    onValueChange = { vm.username = it },
                    label         = stringResource(R.string.wear_login_username_label),
                    placeholder   = stringResource(R.string.wear_login_username_hint),
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Next,
                    focusRequester = usernameFocus,
                    // The IME's Next key had no handler, so it did nothing and the
                    // password field could only be reached by dismissing the keyboard.
                    // moveFocus also scrolls the newly focused field into view, which a
                    // bare FocusRequester would not do (and would throw if that item
                    // happened to be outside the lazy list's composed window).
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
            }

            item {
                WearTextField(
                    value                 = vm.password,
                    onValueChange         = { vm.password = it },
                    label                 = stringResource(R.string.wear_login_password_label),
                    placeholder           = stringResource(R.string.wear_login_password_hint),
                    keyboardType          = KeyboardType.Password,
                    imeAction             = ImeAction.Done,
                    focusRequester        = passwordFocus,
                    keyboardActions       = KeyboardActions(onDone = { submit() }),
                    visualTransformation  =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailing              = {
                        Text(
                            if (passwordVisible) stringResource(R.string.wear_login_hide)
                            else stringResource(R.string.wear_login_show),
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
                Button(
                    onClick  = { submit() },
                    enabled  = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (vm.isLoading) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.wear_login_action))
                    }
                }
            }

            if (vm.errorMessage.isNotBlank()) {
                item { InfoBlock(vm.errorMessage, isError = true) }
            }
        }
    }
}
