package com.leaseting.parkingterminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.ui.navigation.ParkingTerminalNavHost
import com.leaseting.parkingterminal.ui.session.SessionViewModel
import com.leaseting.parkingterminal.ui.shift.ShiftHostViewModel

/**
 * Application root. Waits for the stored session to be read before drawing
 * anything, so a signed-in guard never sees the login screen flash past on a
 * cold start, and asks the API about the shift the moment a session appears.
 */
@Composable
fun ParkingTerminalApp(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory),
    shiftHostViewModel: ShiftHostViewModel = viewModel(factory = ShiftHostViewModel.Factory),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val shiftState by shiftHostViewModel.shiftState.collectAsStateWithLifecycle()

    val isSignedIn = sessionState is SessionState.SignedIn

    // Covers both sign-in and a cold start with a stored session: a handheld
    // that restarted mid-shift comes back to the same till.
    LaunchedEffect(isSignedIn, shiftState is ShiftState.Unknown) {
        if (isSignedIn && shiftState is ShiftState.Unknown) shiftHostViewModel.refresh()
    }

    when (sessionState) {
        SessionState.Restoring -> RestoringSessionScreen(modifier)
        else -> ParkingTerminalNavHost(
            sessionState = sessionState,
            shiftState = shiftState,
            onSignOut = sessionViewModel::signOut,
            modifier = modifier,
        )
    }
}

@Composable
private fun RestoringSessionScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.session_restoring),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
