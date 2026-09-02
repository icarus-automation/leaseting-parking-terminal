package com.leaseting.parkingterminal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.ui.home.HomeScreen
import com.leaseting.parkingterminal.ui.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data object HomeRoute

/**
 * The terminal's destinations, gated by the session.
 *
 * Navigation follows the session rather than the other way round: signing in or
 * out is what moves the graph, so no screen can be reached with the wrong
 * session — including by pressing back.
 */
@Composable
fun ParkingTerminalNavHost(
    sessionState: SessionState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val isSignedIn = sessionState is SessionState.SignedIn

    LaunchedEffect(isSignedIn) {
        val target = if (isSignedIn) HomeRoute else LoginRoute
        val alreadyThere = navController.currentDestination?.hasRoute(target::class) == true
        if (alreadyThere) return@LaunchedEffect

        navController.navigate(target) {
            // Nothing from the previous session stays behind the new screen.
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = LoginRoute,
        modifier = modifier,
    ) {
        composable<LoginRoute> {
            LoginScreen()
        }
        composable<HomeRoute> {
            // Null only for the frame between sign-out and the redirect above.
            val attendant = (sessionState as? SessionState.SignedIn)?.attendant
            if (attendant != null) {
                HomeScreen(attendant = attendant, onSignOut = onSignOut)
            }
        }
    }
}
