package com.leaseting.parkingterminal.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.R
import com.leaseting.parkingterminal.domain.auth.Attendant
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme
import com.leaseting.parkingterminal.ui.theme.extendedColors

/**
 * Where an authenticated attendant lands. Holds the sign-out control and the
 * identity of the session; the parking transaction flow slots in below it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    attendant: Attendant,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isConfirmingSignOut by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    TextButton(onClick = { isConfirmingSignOut = true }) {
                        Text(stringResource(R.string.home_sign_out))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AttendantCard(attendant)
            ReadyCard()
        }
    }

    if (isConfirmingSignOut) {
        SignOutDialog(
            onConfirm = {
                isConfirmingSignOut = false
                onSignOut()
            },
            onDismiss = { isConfirmingSignOut = false },
        )
    }
}

@Composable
private fun AttendantCard(attendant: Attendant, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_signed_in_as),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = attendant.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = attendant.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadyCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_ready_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extendedColors.success,
            )
            Text(
                text = stringResource(R.string.home_ready_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_sign_out_title)) },
        text = { Text(stringResource(R.string.home_sign_out_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.home_sign_out)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LeasetingParkingTerminalTheme {
        HomeScreen(
            attendant = Attendant(
                id = "1",
                name = "Parking Attendant",
                email = "parkingattendant@leaseting.com",
            ),
            onSignOut = {},
        )
    }
}
