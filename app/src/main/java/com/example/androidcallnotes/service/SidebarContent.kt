package com.example.androidcallnotes.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SidebarContent(
    isMinimized: Boolean,
    onClick: () -> Unit,
    onMinimize: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Glossy gradient: Primary color at bottom, slightly lighter/transparent at top
    val glossyGradient = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.8f), // Top highlight
            primaryColor.copy(alpha = 0.98f) // Solid bottom
        )
    )

    Box(
        modifier = Modifier
            .width(if (isMinimized) 12.dp else 64.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .background(glossyGradient)
            .border(
                BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 20f && !isMinimized) {
                        onMinimize()
                    }
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isMinimized) {
            // Shine effect overlay (top half)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.25f),
                            0.5f to Color.Transparent
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = "Open CallMemo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "CallMemo",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
