package com.leaseting.parkingterminal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.auth.SessionState
import com.leaseting.parkingterminal.domain.parking.ShiftState
import com.leaseting.parkingterminal.ui.components.StatusBarIcons
import com.leaseting.parkingterminal.ui.navigation.ParkingTerminalNavHost
import com.leaseting.parkingterminal.ui.session.SessionViewModel
import com.leaseting.parkingterminal.ui.shift.ShiftHostViewModel
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.motion

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
    val hasCheckedShift by shiftHostViewModel.hasCheckedShift.collectAsStateWithLifecycle()

    val isSignedIn = sessionState is SessionState.SignedIn
    val isShiftUnknown = shiftState is ShiftState.Unknown

    // Covers both sign-in and a cold start with a stored session: a handheld
    // that restarted mid-shift comes back to the same till.
    LaunchedEffect(isSignedIn, isShiftUnknown) {
        if (isSignedIn && isShiftUnknown) shiftHostViewModel.refresh()
    }

    // Hold the splash until both questions are answered. Drawing the graph with
    // the shift still in flight would land a signed-in guard on the login
    // screen, because that is where the graph starts.
    val isPreparing = sessionState is SessionState.Restoring ||
        (isSignedIn && isShiftUnknown && !hasCheckedShift)

    if (isPreparing) {
        RestoringSessionScreen(modifier)
    } else {
        ParkingTerminalNavHost(
            sessionState = sessionState,
            shiftState = shiftState,
            onSignOut = sessionViewModel::signOut,
            modifier = modifier,
        )
    }
}

/**
 * The first thing on the handheld in the morning, so it is the product's name
 * on the product's colour rather than a spinner on a white page. The wordmark
 * fades in over the brand field, which covers the moment the stored session is
 * being decrypted.
 */
@Composable
private fun RestoringSessionScreen(modifier: Modifier = Modifier) {
    StatusBarIcons(onDarkBackground = true)

    val wordmark = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { wordmark.targetState = true }
    val wordmarkFade = motion.emphasised

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = wordmark,
            enter = fadeIn(tween(wordmarkFade)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.padding(Spacing.Large),
            ) {
                Text(
                    text = stringResource(R.string.app_brand),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.app_product).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = Spacing.Large)
                        .size(SPINNER_SIZE),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.session_restoring),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = QUIET_ON_PRIMARY,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.Small),
                )
            }
        }
    }
}

private val SPINNER_SIZE = 20.dp

/** Enough to sit back from the wordmark, still clear of AA on the brand blue. */
private const val QUIET_ON_PRIMARY = 0.85f
