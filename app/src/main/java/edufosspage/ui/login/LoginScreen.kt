package org.edufosspage.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.ui.MainScreen
import org.edufosspage.ui.login.LoginViewModel

@Composable
fun LoginScreen(startTab: Int = 0, vm: LoginViewModel = viewModel()) {
    if (vm.sessionReady) { MainScreen(startTab = startTab, vm = vm); return }

    // Init session when login succeeds
    if (vm.loginSuccess != null) {
        LaunchedEffect(vm.loginSuccess) { vm.initSession() }
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading your school…", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // Auto-login splash
    if (vm.isAutoLogging) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("EduFOSSpage", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                CircularProgressIndicator()
                Text("Signing in…", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // Credentials screen
    var passwordVisible by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center) {
        Text("EduFOSSpage", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Sign in to your EduPage account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(value = vm.username, onValueChange = { vm.username = it },
            label = { Text("Username/Email") }, singleLine = true,
            shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = vm.password, onValueChange = { vm.password = it },
            label = { Text("Password") }, singleLine = true,
            shape = MaterialTheme.shapes.large,
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff
                         else Icons.Default.Visibility, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))

        Button(onClick = { if (!vm.isLoading) vm.login() }, enabled = !vm.isLoading,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (vm.isLoading) CircularProgressIndicator(Modifier.size(20.dp),
                strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Log in", style = MaterialTheme.typography.labelLarge)
        }

        if (vm.errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(vm.errorMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

