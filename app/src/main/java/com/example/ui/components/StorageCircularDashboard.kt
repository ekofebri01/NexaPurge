package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StorageStats
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class StorageDisplayFocus {
    USED,
    FREE
}

/**
 * Format bytes into human-readable string.
 */
fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}

/**
 * High-precision circular dashboard component that visualizes storage usage (used vs. free space)
 * with animated dual-segment circular progress arcs, ambient glowing core, interactive focus toggle,
 * side-by-side metric cards, and linear proportional breakdown.
 */
@Composable
fun StorageCircularDashboard(
    stats: StorageStats,
    onOptimizeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayFocus by remember { mutableStateOf(StorageDisplayFocus.USED) }

    val totalBytes = stats.totalBytes.coerceAtLeast(1L)
    val usedRatio = (stats.occupiedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
    val freeRatio = (stats.freeBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()

    val usedPercentage = (usedRatio * 100).toInt()
    val freePercentage = (freeRatio * 100).toInt()

    // Smooth animation for the circular progress indicator
    val animatedUsedRatio by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "animatedUsedRatio"
    )

    val animatedFreeRatio by animateFloatAsState(
        targetValue = freeRatio,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "animatedFreeRatio"
    )

    val usedStr = formatStorageSize(stats.occupiedBytes)
    val freeStr = formatStorageSize(stats.freeBytes)
    val totalStr = formatStorageSize(stats.totalBytes)

    // Palette aligned with "Sophisticated Dark" Obsidian aesthetic
    val emeraldColor = Color(0xFF10B981)
    val cyanColor = Color(0xFF06B6D4)
    val amberWarning = Color(0xFFF59E0B)
    val roseAlert = Color(0xFFEF4444)
    val trackBgColor = Color(0xFF1E293B).copy(alpha = 0.5f)

    val isAlert = stats.thresholdAlert || usedRatio >= 0.85f

    val usedArcColors = if (isAlert) {
        listOf(amberWarning, roseAlert)
    } else {
        listOf(emeraldColor, cyanColor)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_circular_dashboard")
            .clip(RoundedCornerShape(36.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(36.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E0F12).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header: Component Title and Focus Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isAlert) amberWarning.copy(alpha = 0.15f) else emeraldColor.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = "Storage Status",
                            tint = if (isAlert) amberWarning else emeraldColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STATUS PENYIMPANAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF94A3B8), // Slate-400
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Internal Storage (Digunakan vs Bebas)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Focus Toggle Chip
                Surface(
                    onClick = {
                        displayFocus = if (displayFocus == StorageDisplayFocus.USED) StorageDisplayFocus.FREE else StorageDisplayFocus.USED
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.testTag("storage_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Toggle Focus",
                            tint = if (displayFocus == StorageDisplayFocus.USED) emeraldColor else cyanColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (displayFocus == StorageDisplayFocus.USED) "Lihat Bebas" else "Lihat Terpakai",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================================
            // MAIN CIRCULAR PROGRESS INDICATOR (Visualizes Used vs. Free Space)
            // =========================================================================
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .drawBehind {
                        // Ambient radial glow behind the circular meter
                        val glowColor = if (isAlert) amberWarning.copy(alpha = 0.12f) else emeraldColor.copy(alpha = 0.12f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.7f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(220.dp)
                        .testTag("storage_gauge_canvas")
                ) {
                    val canvasSize = size.minDimension
                    val strokeWidthPx = 22.dp.toPx()
                    val freeStrokeWidthPx = 18.dp.toPx()
                    val arcSize = Size(canvasSize - strokeWidthPx, canvasSize - strokeWidthPx)
                    val arcTopLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                    // 1. Subtle background track
                    drawArc(
                        color = trackBgColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Angle geometry: 360 degree ring
                    // To show distinct used vs free segments with high clarity,
                    // we allocate angle proportional to used vs free space.
                    val sweepAngleUsed = (animatedUsedRatio * 360f).coerceIn(4f, 356f)
                    val sweepAngleFree = (360f - sweepAngleUsed).coerceAtLeast(4f)
                    val startAngle = -90f // Starts at 12 o'clock

                    // 2. Free Space Segment (Cyan/Sky glow arc)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF0891B2).copy(alpha = 0.5f),
                                cyanColor.copy(alpha = 0.7f),
                                Color(0xFF0891B2).copy(alpha = 0.5f)
                            )
                        ),
                        startAngle = startAngle + sweepAngleUsed,
                        sweepAngle = sweepAngleFree,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = freeStrokeWidthPx, cap = StrokeCap.Round)
                    )

                    // 3. Used Space Segment (Emerald-to-Cyan or Amber-to-Rose gradient)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = usedArcColors + usedArcColors.first()
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngleUsed,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // 4. Subtle separator indicators at boundary points
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    val radius = (canvasSize - strokeWidthPx) / 2f

                    // Indicator pin at current used transition
                    val transitionRad = Math.toRadians((startAngle + sweepAngleUsed).toDouble())
                    val markerX = centerOffset.x + radius * cos(transitionRad).toFloat()
                    val markerY = centerOffset.y + radius * sin(transitionRad).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(markerX, markerY)
                    )
                }

                // Interactive Center Core
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            displayFocus = if (displayFocus == StorageDisplayFocus.USED) StorageDisplayFocus.FREE else StorageDisplayFocus.USED
                        }
                        .padding(16.dp)
                ) {
                    val activePercent = if (displayFocus == StorageDisplayFocus.USED) {
                        (animatedUsedRatio * 100).toInt()
                    } else {
                        (animatedFreeRatio * 100).toInt()
                    }

                    // Main readout percentage
                    Text(
                        text = "$activePercent%",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (displayFocus == StorageDisplayFocus.USED) {
                            if (isAlert) amberWarning.copy(alpha = 0.2f) else emeraldColor.copy(alpha = 0.18f)
                        } else {
                            cyanColor.copy(alpha = 0.18f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (displayFocus == StorageDisplayFocus.USED) {
                                if (isAlert) amberWarning.copy(alpha = 0.4f) else emeraldColor.copy(alpha = 0.35f)
                            } else {
                                cyanColor.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (displayFocus == StorageDisplayFocus.USED) {
                                            if (isAlert) amberWarning else emeraldColor
                                        } else {
                                            cyanColor
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (displayFocus == StorageDisplayFocus.USED) "TERPAKAI" else "TERSEDIA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (displayFocus == StorageDisplayFocus.USED) {
                                    if (isAlert) amberWarning else emeraldColor
                                } else {
                                    cyanColor
                                },
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (displayFocus == StorageDisplayFocus.USED) "$usedStr digunakan" else "$freeStr tersisa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // USED VS. FREE SPACE METRIC CARDS (Side-by-Side)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Used Space (Terpakai)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, if (displayFocus == StorageDisplayFocus.USED) emeraldColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("storage_used_card")
                        .clickable { displayFocus = StorageDisplayFocus.USED }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (isAlert) amberWarning else emeraldColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Terpakai",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "$usedPercentage%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAlert) amberWarning else emeraldColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = usedStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("storage_used_text")
                        )

                        Text(
                            text = "Sistem & Aplikasi",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Card 2: Free Space (Tersedia)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, if (displayFocus == StorageDisplayFocus.FREE) cyanColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("storage_free_card")
                        .clickable { displayFocus = StorageDisplayFocus.FREE }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(cyanColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tersedia",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "$freePercentage%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = cyanColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = freeStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("storage_free_text")
                        )

                        Text(
                            text = "Bebas Digunakan",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // LINEAR PROPORTIONAL SPLIT BAR (Used vs. Free)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Kapasitas: $totalStr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${usedPercentage}% Terpakai • ${freePercentage}% Bebas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dual-color progress bar showing exact ratio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    // Used fraction
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(animatedUsedRatio.coerceAtLeast(0.02f))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (isAlert) listOf(amberWarning, roseAlert) else listOf(emeraldColor, Color(0xFF059669))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    // Free fraction
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(animatedFreeRatio.coerceAtLeast(0.02f))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(cyanColor.copy(alpha = 0.4f), cyanColor)
                                )
                            )
                    )
                }
            }

            // Health / Advisory Pill
            if (isAlert) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = amberWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, amberWarning.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = amberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Penyimpanan hampir penuh. Bersihkan cache & berkas sementara untuk melegakan ruang ponsel.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            if (onOptimizeClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onOptimizeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emeraldColor,
                        contentColor = Color(0xFF0A0A0B)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("dashboard_optimize_button")
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = emeraldColor.copy(alpha = 0.4f),
                            spotColor = emeraldColor.copy(alpha = 0.4f)
                        ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Scan Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPTIMIZE NOW",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
