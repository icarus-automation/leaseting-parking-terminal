package com.leaseting.parkingterminal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leaseting.parkingterminal.ui.theme.Sizes
import com.leaseting.parkingterminal.ui.theme.Spacing
import com.leaseting.parkingterminal.ui.theme.extendedColors

/**
 * What a card is saying, which decides what colour it is.
 *
 * A guard glancing down mid-transaction should be able to tell an outcome from
 * a form without reading either.
 */
enum class CardTone {
    /** Ordinary content: details, forms, lists. */
    Plain,

    /** The running total for the shift, and other figures the terminal owns. */
    Brand,

    /** Something worked: a stub recorded, money taken, a shift submitted. */
    Confirmed,
}

/**
 * The single card in the app.
 *
 * Flat, with a hairline border rather than a shadow. Elevation on a white page
 * under a bright sky reads as nothing at all, and a border holds its edge at any
 * angle the handheld is tilted to.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Plain,
    contentPadding: Dp = Spacing.Gutter,
    verticalSpacing: Dp = Spacing.Medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = MaterialTheme.extendedColors
    val container: Color
    val onContainer: Color
    val border: Color

    when (tone) {
        CardTone.Plain -> {
            container = MaterialTheme.colorScheme.surface
            onContainer = MaterialTheme.colorScheme.onSurface
            border = MaterialTheme.colorScheme.outlineVariant
        }

        CardTone.Brand -> {
            container = MaterialTheme.colorScheme.primaryContainer
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
            border = MaterialTheme.colorScheme.primaryContainer
        }

        CardTone.Confirmed -> {
            container = extended.successContainer
            onContainer = extended.onSuccessContainer
            border = extended.successContainer
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = onContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Sizes.Hairline, border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

/**
 * The tracked-out caption that names the figure below it. Uppercase, because at
 * this size letterforms carry the heading better than weight alone.
 */
@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = if (color == Color.Unspecified) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            color
        },
        modifier = modifier,
    )
}

/**
 * `Label                      value`, the shape the receipt prints in and the
 * shape a guard checks a stay against before taking money.
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = true),
        )
    }
}

/** The line above a total. Lighter than a full divider, because it separates
 * rows inside one card rather than one card from the next. */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
