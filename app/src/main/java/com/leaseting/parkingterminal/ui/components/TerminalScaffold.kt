package com.leaseting.parkingterminal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.leaseting.parkingterminal.ui.theme.Spacing

/**
 * The frame every working screen sits in.
 *
 * The bar is the brand blue rather than the surface underneath it. That is the
 * one place the terminal states what it is, it survives direct sunlight, and it
 * gives the page a top edge to hang off, which a white bar on a white
 * background does not. Below it the page is the tinted container colour, so a
 * white card reads as a card without needing a shadow to prove it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScaffold(
    title: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    StatusBarIcons(onDarkBackground = true)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = actions,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = content,
    )
}

/**
 * A word-sized action in the top bar. It carries its own content colour because
 * a `TextButton` would otherwise draw brand blue on brand blue.
 */
@Composable
fun TerminalBarAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * The scrolling body of a screen, at the standard margin and rhythm. Scrolling
 * rather than fixed because the on-screen keyboard takes half of a handheld the
 * moment a plate or an amount is being typed.
 */
@Composable
fun ScreenColumn(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    verticalSpacing: androidx.compose.ui.unit.Dp = Spacing.Gutter,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Gutter),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}
