package dev.dominikstahl.einkaufsliste.ui.login

import androidx.lifecycle.ViewModel
import dev.dominikstahl.einkaufsliste.data.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    val isLoggedIn = authManager.isLoggedIn

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun onLoginClicked(startLogin: () -> Unit) {
        _loading.value = true
        _error.value = null
        startLogin()
        // Loading is cleared in onLoginComplete or when isLoggedIn changes
        _loading.value = false
    }

    fun onLoginError(message: String) {
        _loading.value = false
        _error.value = message
    }
}
