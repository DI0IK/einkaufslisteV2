package dev.dominikstahl.einkaufsliste.data.auth

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AuthManager"

    private val ISSUER_URI = "https://sso.dominikstahl.dev/application/o/einkaufsliste-pub/"
    private val CLIENT_ID = "zDHMQhYvPGeLH2mg5Jq8qWr9kI6WH5QAMdKRoNFG"
    private val REDIRECT_URI = "dev.dominikstahl.einkaufsliste://oauth2callback"

    private val sharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            Log.d(TAG, "EncryptedSharedPreferences initialized successfully")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to standard", e)
        context.getSharedPreferences("auth_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val authService = AuthorizationService(context)
    private var authState: AuthState = loadAuthState()

    private val _isLoggedIn = MutableStateFlow(authState.isAuthorized)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow(getUsernameFromState())
    val username = _username.asStateFlow()

    init {
        Log.d(TAG, "AuthManager initialized. isAuthorized=${authState.isAuthorized}, " +
                "hasAccessToken=${authState.accessToken != null}, " +
                "hasRefreshToken=${authState.refreshToken != null}")
    }

    private fun loadAuthState(): AuthState {
        val json = sharedPreferences.getString("auth_state", null)
        if (json == null) {
            Log.d(TAG, "loadAuthState: no saved state, returning empty AuthState")
            return AuthState()
        }
        return try {
            val state = AuthState.jsonDeserialize(json)
            Log.d(TAG, "loadAuthState: deserialized OK. isAuthorized=${state.isAuthorized}, " +
                    "needsTokenRefresh=${state.needsTokenRefresh}")
            state
        } catch (e: Exception) {
            Log.e(TAG, "loadAuthState: failed to deserialize, returning empty AuthState", e)
            AuthState()
        }
    }

    private fun saveAuthState() {
        sharedPreferences.edit().putString("auth_state", authState.jsonSerializeString()).apply()
        val wasLoggedIn = _isLoggedIn.value
        _isLoggedIn.value = authState.isAuthorized
        _username.value = getUsernameFromState()
        Log.d(TAG, "saveAuthState: isAuthorized=${authState.isAuthorized} " +
                "(was=$wasLoggedIn), username=${_username.value}")
    }

    private fun getUsernameFromState(): String? {
        val idToken = authState.idToken ?: return null
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT))
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val map = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(payload)
                map["preferred_username"]?.let {
                    val content = it.toString()
                    if (content.startsWith("\"") && content.endsWith("\"")) {
                        content.substring(1, content.length - 1)
                    } else {
                        content
                    }
                }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "getUsernameFromState: failed to parse JWT idToken", e)
            null
        }
    }

    fun performAuthorizationRequest(
        completionPendingIntent: PendingIntent,
        cancelPendingIntent: PendingIntent
    ) {
        Log.d(TAG, "performAuthorizationRequest: fetching OIDC config from issuer=$ISSUER_URI")
        AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(ISSUER_URI)) { config, ex ->
            if (ex != null || config == null) {
                Log.e(TAG, "performAuthorizationRequest: OIDC discovery FAILED — ${ex?.message}", ex)
                return@fetchFromIssuer
            }
            Log.d(TAG, "performAuthorizationRequest: OIDC discovery OK — " +
                    "authEndpoint=${config.authorizationEndpoint}, " +
                    "tokenEndpoint=${config.tokenEndpoint}")

            val authRequest = AuthorizationRequest.Builder(
                config,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(REDIRECT_URI)
            ).setScopes("openid", "profile", "email", "offline_access").build()

            Log.d(TAG, "performAuthorizationRequest: launching auth request, auth URL=${authRequest.toUri()}")
            authService.performAuthorizationRequest(
                authRequest,
                completionPendingIntent,
                cancelPendingIntent
            )
        }
    }

    suspend fun handleAuthorizationResponse(response: AuthorizationResponse?, exception: AuthorizationException?): Boolean {
        Log.d(TAG, "handleAuthorizationResponse: response=${response != null}, exception=${exception?.message}")

        if (exception != null) {
            Log.e(TAG, "handleAuthorizationResponse: auth exception type=${exception.type}, " +
                    "code=${exception.code}, message=${exception.message}")
            return false
        }
        if (response == null) {
            Log.e(TAG, "handleAuthorizationResponse: response is null with no exception — " +
                    "PendingIntent may not have delivered auth data correctly")
            return false
        }

        Log.d(TAG, "handleAuthorizationResponse: received auth code, updating state and exchanging for tokens")
        authState.update(response, exception)
        saveAuthState()

        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "handleAuthorizationResponse: starting token exchange request")
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                if (tokenException != null) {
                    Log.e(TAG, "handleAuthorizationResponse: token exchange FAILED — " +
                            "type=${tokenException.type}, code=${tokenException.code}, " +
                            "message=${tokenException.message}")
                } else {
                    Log.d(TAG, "handleAuthorizationResponse: token exchange SUCCESS — " +
                            "accessToken=${tokenResponse?.accessToken?.take(20)}..., " +
                            "expiresIn=${tokenResponse?.accessTokenExpirationTime}")
                }
                authState.update(tokenResponse, tokenException)
                if (tokenResponse != null) {
                    saveAuthState()
                    continuation.resume(true)
                } else {
                    saveAuthState()
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun getValidAccessToken(): String? {
        if (!authState.isAuthorized) {
            Log.d(TAG, "getValidAccessToken: not authorized, returning null")
            return null
        }

        val needsRefresh = authState.needsTokenRefresh ||
            (authState.accessTokenExpirationTime ?: 0L) - System.currentTimeMillis() < 60000

        if (!needsRefresh) {
            Log.d(TAG, "getValidAccessToken: token still valid, returning existing token")
            return authState.accessToken
        }

        val refreshToken = authState.refreshToken
        if (refreshToken == null) {
            Log.e(TAG, "getValidAccessToken: needs refresh but no refresh token available — logging out")
            logout()
            return null
        }

        Log.d(TAG, "getValidAccessToken: token expired/near-expiry, refreshing...")
        return suspendCancellableCoroutine { continuation ->
            val request = authState.createTokenRefreshRequest()
            authService.performTokenRequest(request) { tokenResponse, tokenException ->
                authState.update(tokenResponse, tokenException)
                saveAuthState()
                if (tokenResponse != null) {
                    Log.d(TAG, "getValidAccessToken: refresh SUCCESS")
                    continuation.resume(tokenResponse.accessToken)
                } else {
                    Log.e(TAG, "getValidAccessToken: refresh FAILED — ${tokenException?.message}")
                    if (tokenException != null && tokenException.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                        Log.e(TAG, "getValidAccessToken: oauth token error, logging out")
                        logout()
                    }
                    continuation.resume(null)
                }
            }
        }
    }

    fun logout() {
        Log.d(TAG, "logout: clearing auth state")
        authState = AuthState()
        saveAuthState()
    }
}
