package dev.dominikstahl.einkaufsliste

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.dominikstahl.einkaufsliste.ui.home.HomeScreen
import dev.dominikstahl.einkaufsliste.ui.home.HomeViewModel
import dev.dominikstahl.einkaufsliste.ui.details.ListDetailsScreen
import dev.dominikstahl.einkaufsliste.ui.details.ListDetailsViewModel
import dev.dominikstahl.einkaufsliste.data.auth.AuthManager
import dev.dominikstahl.einkaufsliste.ui.login.LoginScreen
import dev.dominikstahl.einkaufsliste.ui.login.LoginViewModel
import dev.dominikstahl.einkaufsliste.ui.theme.EinkaufslisteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    // Called when the PendingIntent fires after OAuth redirect (normal flow).
    // Also called if the app is already running and a new intent arrives (singleTask).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle auth response if MainActivity was recreated after process death
        // (the PendingIntent launches a fresh MainActivity with the response in the intent).
        handleAuthIntent(intent)

        setContent {
            EinkaufslisteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by authManager.isLoggedIn.collectAsStateWithLifecycle()
                    val startDest = remember { if (authManager.isLoggedIn.value) "home" else "login" }

                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("login") {
                            val viewModel: LoginViewModel = hiltViewModel()
                            LoginScreen(
                                viewModel = viewModel,
                                onStartLoginFlow = {
                                    val completionIntent = Intent(this@MainActivity, MainActivity::class.java)
                                    val cancelIntent = Intent(this@MainActivity, MainActivity::class.java)
                                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                    authManager.performAuthorizationRequest(
                                        PendingIntent.getActivity(this@MainActivity, 0, completionIntent, flags),
                                        PendingIntent.getActivity(this@MainActivity, 1, cancelIntent, flags)
                                    )
                                }
                            )
                        }
                        composable("home") {
                            val viewModel: HomeViewModel = hiltViewModel()
                            val lists by viewModel.activeLists.collectAsStateWithLifecycle()
                            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

                            HomeScreen(
                                lists = lists,
                                isOffline = isOffline,
                                onListClick = { listId ->
                                    navController.navigate("list_details/$listId")
                                },
                                onCreateList = { name, icon, color ->
                                    viewModel.createNewList(name, icon, color)
                                },
                                onUpdateList = { id, name, icon, color ->
                                    viewModel.updateList(id, name, icon, color)
                                },
                                onDeleteList = { id ->
                                    viewModel.deleteList(id)
                                },
                                onRefresh = {
                                    viewModel.triggerSync()
                                },
                                onLogout = {
                                    authManager.logout()
                                }
                            )
                        }
                        composable("list_details/{listId}") { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: ""
                            val viewModel: ListDetailsViewModel = hiltViewModel()

                            LaunchedEffect(listId) {
                                viewModel.setListId(listId)
                            }

                            ListDetailsScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleAuthIntent(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        Log.d("MainActivity", "handleAuthIntent: action=${intent.action}, " +
                "hasResponse=${response != null}, hasException=${ex != null}")
        if (response != null || ex != null) {
            lifecycleScope.launch {
                authManager.handleAuthorizationResponse(response, ex)
            }
        }
    }
}