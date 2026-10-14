/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors

/** A translucent panel so the horizon stays visible behind the content. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalPaletteColors.current
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = palette.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, (accent ?: palette.outline).copy(alpha = 0.45f)),
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

/** Small uppercase section header with a hairline, used across the app. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Color? = null) {
    val palette = LocalPaletteColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent ?: LocalAccents.current.mint)
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = palette.onSurfaceMuted,
        )
        Box(
            Modifier
                .weight(1f)
                .size(1.dp)
                .background(palette.outline.copy(alpha = 0.6f))
        )
    }
}

/** A single headline number with its caption. */
@Composable
fun StatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    val palette = LocalPaletteColors.current
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.04f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.35f), MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.onSurfaceMuted,
            )
            if (footnote != null) {
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceMuted.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Rounded selectable pill used for ranges, flags and tags. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPaletteColors.current
    val tint = accent ?: LocalAccents.current.lilac
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.22f) else palette.surface.copy(alpha = 0.6f))
            .border(1.dp, if (selected) tint else palette.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) leading()
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else palette.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    val palette = LocalPaletteColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = palette.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}
