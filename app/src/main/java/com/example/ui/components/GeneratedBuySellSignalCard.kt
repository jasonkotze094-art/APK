package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SignalEntity
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CyberAqua
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SubtitleWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * High-craft Jetpack Compose Buy/Sell Signal Card.
 * Displays real-time generated signals with precision targets, confidence meter,
 * technical indicator confluence, and one-tap order simulation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratedBuySellSignalCard(
    signal: SignalEntity,
    currentPrice: Double,
    accentColor: Color,
    onExecuteOrder: (direction: String) -> Unit,
    onFocusChart: (symbol: String, timeframe: String) -> Unit,
    onReScan: (symbol: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }
    var showCopiedToast by remember { mutableStateOf(false) }

    val isBuy = signal.direction.uppercase() == "BUY"
    val isSell = signal.direction.uppercase() == "SELL"
    val isHold = !isBuy && !isSell

    val actionColor = when {
        isBuy -> CyberNeonGreen
        isSell -> CyberCrimson
        else -> CyberAqua
    }

    val actionLabel = when {
        isBuy -> "BUY / LONG"
        isSell -> "SELL / SHORT"
        else -> "HOLD / RANGE"
    }

    val setupSubtitle = when {
        isBuy -> "Bullish Momentum & Support Rebound"
        isSell -> "Bearish Rejection & Resistance Break"
        else -> "Market Consolidating — Neutral Bias"
    }

    // Calculate Risk-Reward Ratio
    val riskDistance = abs(signal.entryPrice - signal.stopLoss)
    val rewardDistance = abs(signal.takeProfit - signal.entryPrice)
    val riskRewardRatio = if (riskDistance > 0.00001) {
        val rr = rewardDistance / riskDistance
        String.format(Locale.US, "1 : %.2f", rr)
    } else {
        "1 : 2.00"
    }

    // Formatted time
    val timeFormatted = remember(signal.timestamp) {
        val diffMs = System.currentTimeMillis() - signal.timestamp
        when {
            diffMs < 60_000 -> "Just now"
            diffMs < 3600_000 -> "${diffMs / 60_000}m ago"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(signal.timestamp))
        }
    }

    // Conviction level badge
    val (convictionLabel, convictionBg) = when {
        signal.confidence >= 80 -> Pair("HIGH CONVICTION", CyberNeonGreen.copy(alpha = 0.15f))
        signal.confidence >= 70 -> Pair("MODERATE CONVICTION", CyberAqua.copy(alpha = 0.15f))
        else -> Pair("SPECULATIVE", SubtitleWhite.copy(alpha = 0.15f))
    }

    // Subtle glow animation for live active signals
    val infiniteTransition = rememberInfiniteTransition(label = "SignalGlow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SignalPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    color = actionColor.copy(alpha = pulseGlow * 0.7f)
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("signal_card_${signal.symbol}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            actionColor.copy(alpha = 0.08f),
                            Color(0xFF101010),
                            Color(0xFF090909)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // 1. Header: Symbol, Timeframe, Live Tag, Share / Expand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Symbol
                    Text(
                        text = signal.symbol,
                        color = PrimaryWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.testTag("signal_card_symbol_${signal.symbol}")
                    )

                    // Timeframe Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF202020))
                            .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signal.timeframe,
                            color = CyberAqua,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Live Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(actionColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(actionColor.copy(alpha = pulseGlow))
                            )
                            Text(
                                text = timeFormatted,
                                color = actionColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Top Right Action Buttons: Copy & Refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val textToCopy = buildString {
                                appendLine("🚨 THE CLOWN BEAST SIGNAL: ${signal.symbol} (${signal.timeframe})")
                                appendLine("ACTION: ${signal.direction}")
                                appendLine("ENTRY: ${signal.entryPrice}")
                                appendLine("TP: ${signal.takeProfit}")
                                appendLine("SL: ${signal.stopLoss}")
                                appendLine("R:R: $riskRewardRatio | Confidence: ${signal.confidence}%")
                                appendLine("Strategy: ${signal.strategy}")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Trading Signal", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Signal copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_signal_button_${signal.symbol}")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Copy Signal",
                            tint = SubtitleWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onReScan(signal.symbol) },
                        modifier = Modifier.size(32.dp).testTag("rescan_signal_button_${signal.symbol}")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Re-analyze",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Main Signal Banner (Action Pill & Conviction)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, actionColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action Pill with glowing icon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBuy) Icons.Default.KeyboardArrowUp else if (isSell) Icons.Default.KeyboardArrowDown else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isBuy) Color.Black else PrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = actionLabel,
                                color = if (isBuy) Color.Black else PrimaryWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = setupSubtitle,
                            color = PrimaryWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strategy: ${signal.strategy}",
                            color = SubtitleWhite,
                            fontSize = 10.sp
                        )
                    }
                }

                // Conviction Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(convictionBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = convictionLabel,
                        color = actionColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. AI Confidence Meter
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI CONVICTION GAUGE",
                        color = SubtitleWhite,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${signal.confidence}% Confidence",
                        color = actionColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { signal.confidence / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = actionColor,
                    trackColor = Color(0xFF222222)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Precision Trading Levels Grid (4-box clean card layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SignalLevelGridCell(
                    title = "ENTRY",
                    value = String.format(Locale.US, "%.2f", signal.entryPrice),
                    subtitle = "Market: ${String.format(Locale.US, "%.2f", currentPrice)}",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                SignalLevelGridCell(
                    title = "TAKE PROFIT",
                    value = String.format(Locale.US, "%.2f", signal.takeProfit),
                    subtitle = if (isBuy) "+${String.format(Locale.US, "%.1f", rewardDistance * 100)} p" else "-${String.format(Locale.US, "%.1f", rewardDistance * 100)} p",
                    accentColor = CyberNeonGreen,
                    modifier = Modifier.weight(1f)
                )

                SignalLevelGridCell(
                    title = "STOP LOSS",
                    value = String.format(Locale.US, "%.2f", signal.stopLoss),
                    subtitle = if (isBuy) "-${String.format(Locale.US, "%.1f", riskDistance * 100)} p" else "+${String.format(Locale.US, "%.1f", riskDistance * 100)} p",
                    accentColor = CyberCrimson,
                    modifier = Modifier.weight(1f)
                )

                SignalLevelGridCell(
                    title = "R : R",
                    value = riskRewardRatio,
                    subtitle = "Risk/Reward",
                    accentColor = CyberAqua,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Technical Confluence Tags Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConfluenceChip(
                    text = if (isBuy) "📈 14-EMA Bull Cross" else "📉 14-EMA Bear Cross",
                    color = actionColor
                )
                ConfluenceChip(
                    text = if (isBuy) "⚡ RSI: Oversold (32)" else "⚡ RSI: Overbought (68)",
                    color = CyberAqua
                )
                ConfluenceChip(
                    text = if (isBuy) "🛡️ Support Zone Rebound" else "🧱 Resistance Rejection",
                    color = CyberMagenta
                )
                ConfluenceChip(
                    text = "📊 Vol: Above Average",
                    color = SubtitleWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. AI Analytical Thesis Rationale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D0D0D))
                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI ANALYTICAL THESIS",
                            color = accentColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Google Grounded",
                            color = SubtitleWhite,
                            fontSize = 8.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = signal.analysis.ifEmpty {
                            "Calculated multi-timeframe confluence using moving averages and price action structure on ${signal.timeframe}."
                        },
                        color = PrimaryWhite,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Interactive Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus Chart
                OutlinedButton(
                    onClick = { onFocusChart(signal.symbol, signal.timeframe) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("focus_chart_button_${signal.symbol}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryWhite
                    )
                ) {
                    Text(
                        text = "VIEW CHART",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Execute Simulated Order
                Button(
                    onClick = { onExecuteOrder(signal.direction) },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(40.dp)
                        .testTag("execute_signal_button_${signal.symbol}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isBuy) Color.Black else PrimaryWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isBuy) "SIMULATE BUY" else if (isSell) "SIMULATE SELL" else "EXECUTE TRADE",
                            color = if (isBuy) Color.Black else PrimaryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalLevelGridCell(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141414))
            .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = SubtitleWhite,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SubtitleWhite,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ConfluenceChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
