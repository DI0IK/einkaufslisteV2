package dev.dominikstahl.einkaufsliste.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authManagerProvider: Provider<AuthManager>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val authManager = authManagerProvider.get()
        val token = runBlocking {
            authManager.getValidAccessToken()
        }

        val request = chain.request()
        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        // On 401, attempt one token refresh and retry before giving up.
        // Do NOT immediately logout — the backend may be temporarily unavailable
        // or the token may just need refreshing. Logging out on every backend 401
        // would kick the user out whenever the backend is unreachable.
        if (response.code == 401) {
            response.close()
            val freshToken = runBlocking {
                authManager.getValidAccessToken()
            }
            if (freshToken != null && freshToken != token) {
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer $freshToken")
                    .build()
                return chain.proceed(retryRequest)
            }
            // Still 401 after refresh — re-proceed with the original authenticated request
            // so the caller sees the 401 and can decide how to handle it.
            return chain.proceed(authenticatedRequest)
        }

        return response
    }
}
