package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SignalEntity
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CyberAqua
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SubtitleWhite

/**
 * Top Market Dashboard Stats & Control Bar.
 */
@Composable
fun DashboardStatsBar(
    signals: List<SignalEntity>,
    isScanningActive: Boolean,
    isAnalyzing: Boolean,
    accentColor: Color,
    onToggleScanning: () -> Unit,
    onBatchScanAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buyCount = signals.count { it.direction.uppercase() == "BUY" }
    val sellCount = signals.count { it.direction.uppercase() == "SELL" }
    val totalCount = signals.size.coerceAtLeast(1)
    val bullishPct = (buyCount.toFloat() / totalCount.toFloat() * 100f).toInt().coerceIn(10, 90)
    val bearishPct = 100 - bullishPct

    val infiniteTransition = rememberInfiniteTransition(label = "StatsPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(22.dp))
            .testTag("dashboard_stats_bar"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141414), Color(0xFF0C0C0C))
                    )
                )
                .padding(16.dp)
        ) {
            // Header Row: Status Indicator & Quick Batch AI Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Active Scanner Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleScanning)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isScanningActive) CyberNeonGreen.copy(alpha = glowAlpha) else Color.Gray)
                    )
                    Column {
                        Text(
                            text = if (isScanningActive) "SCANNER ACTIVE" else "SCANNER IDLE",
                            color = if (isScanningActive) CyberNeonGreen else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tap to toggle live ticker",
                            color = SubtitleWhite,
                            fontSize = 8.5.sp
                        )
                    }
                }

                // Right: Batch Scan AI Button
                Button(
                    onClick = onBatchScanAll,
                    enabled = !isAnalyzing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = Color(0xFF222222)
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("batch_ai_scan_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAnalyzing) "SCANNING..." else "BATCH SCAN",
                            color = Color.Black,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Market Sentiment Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MARKET SENTIMENT BIAS",
                    color = SubtitleWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Bullish: $bullishPct% ($buyCount BUY)",
                        color = CyberNeonGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Bearish: $bearishPct% ($sellCount SELL)",
                        color = CyberCrimson,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Split Sentiment Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF202020))
            ) {
                Box(
                    modifier = Modifier
                        .weight(bullishPct.toFloat())
                        .height(6.dp)
                        .background(CyberNeonGreen)
                )
                Box(
                    modifier = Modifier
                        .weight(bearishPct.toFloat())
                        .height(6.dp)
                        .background(CyberCrimson)
                )
            }
        }
    }
}
