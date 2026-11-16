package com.inventory.barcodetracker.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerticalScrollbarContainer(
    scrollState: ScrollState,
    maxHeight: Dp = 380.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (scrollState.maxValue > 0) 12.dp else 0.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )

        if (scrollState.maxValue > 0) {
            val density = LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(8.dp)
            ) {
                val viewportHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
                val maxScrollPx = scrollState.maxValue.toFloat()
                val currentScrollPx = scrollState.value.toFloat()
                val totalContentHeightPx = viewportHeightPx + maxScrollPx

                val rawThumbHeightPx = viewportHeightPx * (viewportHeightPx / totalContentHeightPx)
                val minThumbHeightPx = with(density) { 32.dp.toPx() }
                val thumbHeightPx = rawThumbHeightPx.coerceIn(minThumbHeightPx, viewportHeightPx * 0.8f)

                val maxThumbOffsetPx = maxOf(0f, viewportHeightPx - thumbHeightPx)
                val scrollRatio = (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)
                val currentOffsetYDp = with(density) { (maxThumbOffsetPx * scrollRatio).toDp() }
                val currentThumbHeightDp = with(density) { thumbHeightPx.toDp() }

                // 1. Faint Background Track Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                )

                // 2. Natural Proportional Moving Thumb Pill
                Box(
                    modifier = Modifier
                        .offset(y = currentOffsetYDp)
                        .size(width = 6.dp, height = currentThumbHeightDp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
