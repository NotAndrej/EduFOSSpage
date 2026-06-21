package org.edufosspage.ui.login

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.edufosspage.data.*

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    var username      by mutableStateOf("")
    var password      by mutableStateOf("")
    var isLoading     by mutableStateOf(false)
    var isAutoLogging by mutableStateOf(false)
    var errorMessage  by mutableStateOf("")

    var loginSuccess: LoginResult.Success? by mutableStateOf(null)
    var sessionReady  by mutableStateOf(false)

    init { tryAutoLogin() }

    private fun tryAutoLogin() {
        val (savedUser, savedPass) = CredentialStore.load(getApplication()) ?: return
        username      = savedUser
        isAutoLogging = true

        viewModelScope.launch {
            val ctx               = getApplication<Application>()
            EduHttp.appContext    = ctx
	    val restoredFromCache = AppCache.loadSession(ctx)
            if (restoredFromCache && EduSession.isReady) {
                sessionReady  = true
                isAutoLogging = false
                refreshSessionInBackground(savedUser, savedPass)
                return@launch
            }

            when (val result = EdupageLogin.login(savedUser, savedPass)) {
                is LoginResult.Success -> loginSuccess = result
                is LoginResult.Failure -> {}
            }
            isAutoLogging = false
        }
    }

    private fun refreshSessionInBackground(user: String, pass: String) {
        viewModelScope.launch {
            val result = EdupageLogin.login(user, pass)
            if (result is LoginResult.Success) {
                EduApi.initSession(result.schoolUrl, result.cookies)
                AppCache.saveSession(getApplication())
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            isLoading    = true
            errorMessage = ""
            when (val result = EdupageLogin.login(username, password)) {
                is LoginResult.Success -> {
                    CredentialStore.save(getApplication(), username, password)
                    loginSuccess = result
                }
                is LoginResult.Failure -> errorMessage = result.error
            }
            isLoading = false
        }
    }

    fun initSession() {
        val success = loginSuccess ?: return
	EduHttp.appContext = getApplication()
        viewModelScope.launch {
            EduApi.initSession(success.schoolUrl, success.cookies)
            AppCache.saveSession(getApplication())
            sessionReady = true
        }
    }

    fun logout() {
        val ctx = getApplication<Application>()
        viewModelScope.launch { AppCache.clearAll(ctx) }
        CredentialStore.clear(ctx)
        EduSession.clear()
        sessionReady = false
        loginSuccess = null
        username     = ""
        password     = ""
        errorMessage = ""
    }
}
