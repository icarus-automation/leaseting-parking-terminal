package com.leaseting.parkingterminal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.ui.entry.EntryScreen
import com.leaseting.parkingterminal.ui.exit.ExitScreen
import com.leaseting.parkingterminal.ui.find.FindVehicleScreen
import com.leaseting.parkingterminal.ui.gate.GateScreen
import com.leaseting.parkingterminal.ui.login.LoginScreen
import com.leaseting.parkingterminal.ui.shift.EndShiftScreen
import com.leaseting.parkingterminal.ui.terminal.TerminalPickerScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

/** No shift open: pick the gate to work. */
@Serializable
data object TerminalPickerRoute

/** The working gate. Everything during a shift starts here. */
@Serializable
data object GateRoute

@Serializable
data object EntryRoute

@Serializable
data object FindVehicleRoute

@Serializable
data class ExitRoute(val sessionId: String)

@Serializable
data object EndShiftRoute

/**
 * The terminal's destinations, gated by the session and the shift.
 *
 * Navigation follows state rather than the other way round. Signing in or out
 * moves the graph, and so does opening or declaring a shift, so no screen can
 * be reached in a state that cannot serve it — including by pressing back.
 * Everything below the gate is ordinary forward navigation.
 */
@Composable
fun ParkingTerminalNavHost(
    sessionState: SessionState,
    shiftState: ShiftState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val attendant = (sessionState as? SessionState.SignedIn)?.attendant
    val shift = (shiftState as? ShiftState.Open)?.shift

    // The root a given state belongs at. A shift that is still unknown reads as
    // no shift, which sends the guard to the picker — the caller holds the
    // splash until the API has been asked, and picking the same gate again
    // hands back a shift they already hold, so a failed check costs one tap.
    val root = when {
        attendant == null -> LoginRoute
        shift == null -> TerminalPickerRoute
        else -> GateRoute
    }

    LaunchedEffect(root) {
        val current = navController.currentDestination
        val alreadyThere = when (root) {
            LoginRoute -> current?.hasRoute(LoginRoute::class) == true
            TerminalPickerRoute -> current?.hasRoute(TerminalPickerRoute::class) == true
            // Any screen inside a shift counts as being at the gate: returning
            // from an entry must not bounce the guard out of it.
            else -> current != null && current.hasRoute(LoginRoute::class).not() &&
                current.hasRoute(TerminalPickerRoute::class).not()
        }
        if (alreadyThere) return@LaunchedEffect

        navController.navigate(root) {
            // Nothing from the previous state stays behind the new screen.
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

        composable<TerminalPickerRoute> {
            TerminalPickerScreen(onSignOut = onSignOut)
        }

        composable<GateRoute> {
            // Null only for the frame between a shift ending and the redirect.
            if (attendant != null && shift != null) {
                GateScreen(
                    attendant = attendant,
                    shift = shift,
                    onRecordEntry = { navController.navigate(EntryRoute) },
                    onVehicleOut = { navController.navigate(FindVehicleRoute) },
                    onEndShift = { navController.navigate(EndShiftRoute) },
                    onSignOut = onSignOut,
                )
            }
        }

        composable<EntryRoute> {
            EntryScreen(onDone = { navController.popBackStack() })
        }

        composable<FindVehicleRoute> {
            FindVehicleScreen(
                onSessionSelected = { sessionId -> navController.navigate(ExitRoute(sessionId)) },
            )
        }

        composable<ExitRoute> { backStackEntry ->
            val route: ExitRoute = backStackEntry.toRoute()
            ExitScreen(
                sessionId = route.sessionId,
                // Back to the finder, not the gate: the next vehicle is behind
                // this one and the guard is still working the exit lane.
                onDone = { navController.popBackStack() },
            )
        }

        composable<EndShiftRoute> {
            EndShiftScreen(onCancel = { navController.popBackStack() })
        }
    }
}
